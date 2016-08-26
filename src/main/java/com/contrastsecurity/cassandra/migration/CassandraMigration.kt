/**
 * File     : CassandraMigration.kt
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
 *   Derivative - Copyright (c) 2016 Citadel Technology Solutions Pte Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.contrastsecurity.cassandra.migration

import com.contrastsecurity.cassandra.migration.api.CassandraMigrationException
import com.contrastsecurity.cassandra.migration.api.MigrationInfoService
import com.contrastsecurity.cassandra.migration.api.MigrationVersion
import com.contrastsecurity.cassandra.migration.api.configuration.CassandraMigrationConfiguration
import com.contrastsecurity.cassandra.migration.api.configuration.MigrationConfigs
import com.contrastsecurity.cassandra.migration.api.resolver.MigrationResolver
import com.contrastsecurity.cassandra.migration.config.Keyspace
import com.contrastsecurity.cassandra.migration.internal.command.Initialize
import com.contrastsecurity.cassandra.migration.internal.command.Migrate
import com.contrastsecurity.cassandra.migration.internal.command.Validate
import com.contrastsecurity.cassandra.migration.internal.dbsupport.SchemaVersionDAO
import com.contrastsecurity.cassandra.migration.internal.info.MigrationInfoServiceImpl
import com.contrastsecurity.cassandra.migration.internal.resolver.CompositeMigrationResolver
import com.contrastsecurity.cassandra.migration.internal.util.ScriptsLocations
import com.contrastsecurity.cassandra.migration.internal.util.logging.LogFactory
import com.contrastsecurity.cassandra.migration.utils.VersionPrinter
import com.datastax.driver.core.Metadata
import com.datastax.driver.core.Session
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * This is the centre point of Cassandra migration, and for most users, the only class they will ever have to deal with.
 * It is THE public API from which all important Cassandra migration functions such as clean, validate and migrate can be called.
 */
class CassandraMigration : CassandraMigrationConfiguration {

    /**
     * The ClassLoader to use for resolving migrations on the classpath.
     * (default: Thread.currentThread().getContextClassLoader())
     */
    override var classLoader = Thread.currentThread().contextClassLoader

    /**
     * The Cassandra keyspace to connect to.
     */
    var keyspace: Keyspace? = null

    /**
     * The Cassandra migration configuration.
     */
    val configs: MigrationConfigs

    /**
     * CassandraMigration initialization.
     */
    init {
        this.keyspace = Keyspace()
        this.configs = MigrationConfigs()
    }

    /**
     * Starts the database migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.
     *
     * @return The number of successfully applied migrations.
     */
    fun migrate(): Int {
        return execute(object : Action<Int> {
            override fun execute(session: Session): Int {
                Initialize().run(session, keyspace!!, MigrationVersion.CURRENT.table)

                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = SchemaVersionDAO(session, keyspace, MigrationVersion.CURRENT.table)
                val migrate = Migrate(migrationResolver, configs.target, schemaVersionDAO, session,
                        keyspace!!.cluster.username, configs.isAllowOutOfOrder)

                return migrate.run()
            }
        })
    }

    /**
     * Retrieves the complete information about all the migrations including applied, pending and current migrations with
     * details and status.
     *
     * @return All migrations sorted by version, oldest first.
     */
    fun info(): MigrationInfoService {
        return execute(object : Action<MigrationInfoService> {
            override fun execute(session: Session): MigrationInfoService? {
                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = SchemaVersionDAO(session, keyspace, MigrationVersion.CURRENT.table)
                val migrationInfoService = MigrationInfoServiceImpl(migrationResolver, schemaVersionDAO, configs.target, false, true)
                migrationInfoService.refresh()

                return migrationInfoService
            }
        })
    }

    /**
     * Validate applied migrations against resolved ones (on the filesystem or classpath)
     * to detect accidental changes that may prevent the schema(s) from being recreated exactly.
     *
     * Validation fails if:
     *  * differences in migration names, types or checksums are found
     *  * versions have been applied that aren't resolved locally anymore
     *  * versions have been resolved that haven't been applied yet
     */
    fun validate() {
        val validationError = execute(object : Action<String?> {
            override fun execute(session: Session): String? {
                val migrationResolver = createMigrationResolver()
                val schemaVersionDao = SchemaVersionDAO(session, keyspace, MigrationVersion.CURRENT.table)
                val validate = Validate(migrationResolver, configs.target, schemaVersionDao, true, false)
                return validate.run()
            }
        })

        if (validationError != null) {
            throw CassandraMigrationException("Validation failed. " + validationError)
        }
    }

    /**
     * Baselines an existing database, excluding all migrations up to and including baselineVersion.
     */
    fun baseline() {
        // TODO: Create the Cassandra migration implementation, refer to existing PR: https://github.com/Contrast-Security-OSS/cassandra-migration/pull/17
        throw NotImplementedException()
    }

    /**
     * Executes this command with proper resource handling and cleanup.
     *
     * @param action The action to execute.
     * @param T The action result type.
     * @return The action result.
     */
    internal fun <T> execute(action: Action<T>): T {
        val result: T

        VersionPrinter.printVersion(classLoader)

        var cluster: com.datastax.driver.core.Cluster? = null
        var session: Session? = null
        try {
            if (null == keyspace)
                throw IllegalArgumentException("Unable to establish Cassandra session. Keyspace is not configured.")

            if (null == keyspace!!.cluster)
                throw IllegalArgumentException("Unable to establish Cassandra session. Cluster is not configured.")

            val builder = com.datastax.driver.core.Cluster.Builder()
            builder.addContactPoints(*keyspace!!.cluster.contactpoints).withPort(keyspace!!.cluster.port)
            if (null != keyspace!!.cluster.username && !keyspace!!.cluster.username.trim { it <= ' ' }.isEmpty()) {
                if (null != keyspace!!.cluster.password && !keyspace!!.cluster.password.trim { it <= ' ' }.isEmpty()) {
                    builder.withCredentials(keyspace!!.cluster.username,
                            keyspace!!.cluster.password)
                } else {
                    throw IllegalArgumentException("Password must be provided with username.")
                }
            }
            cluster = builder.build()

            val metadata = cluster!!.metadata
            LOG.info(getConnectionInfo(metadata))

            session = cluster.newSession()
            if (null == keyspace!!.name || keyspace!!.name.trim { it <= ' ' }.length == 0)
                throw IllegalArgumentException("Keyspace not specified.")
            val keyspaces = metadata.keyspaces
            var keyspaceExists = false
            for (keyspaceMetadata in keyspaces) {
                if (keyspaceMetadata.name.equals(keyspace!!.name, ignoreCase = true))
                    keyspaceExists = true
            }
            if (keyspaceExists)
                session!!.execute("USE " + keyspace!!.name)
            else
                throw CassandraMigrationException("Keyspace: " + keyspace!!.name + " does not exist.")

            result = action.execute(session)!!
        } finally {
            if (null != session && !session.isClosed)
                try {
                    session.close()
                } catch (e: Exception) {
                    LOG.warn("Error closing Cassandra session")
                }

            if (null != cluster && !cluster.isClosed)
                try {
                    cluster.close()
                } catch (e: Exception) {
                    LOG.warn("Error closing Cassandra cluster")
                }

        }
        return result
    }

    /**
     * Get Cassandra connection information.
     *
     * @param metadata The connected cluster metadata.
     * @return Cluster connection information.
     */
    private fun getConnectionInfo(metadata: Metadata): String {
        val sb = StringBuilder()
        sb.append("Connected to cluster: ")
        sb.append(metadata.clusterName)
        sb.append("\n")
        for (host in metadata.allHosts) {
            sb.append("Data center: ")
            sb.append(host.datacenter)
            sb.append("; Host: ")
            sb.append(host.address)
        }
        return sb.toString()
    }

    /**
     * Creates the MigrationResolver.
     *
     * @return A new, fully configured, MigrationResolver instance.
     */
    private fun createMigrationResolver(): MigrationResolver {
        return CompositeMigrationResolver(classLoader, ScriptsLocations(*configs.scriptsLocations), configs.encoding)
    }

    /**
     * A Cassandra migration action that can be executed.
     *
     * @param T The action result type.
     */
    internal interface Action<out T> {

        /**
         * Execute the operation.
         *
         * @param session The Cassandra session connection to use to execute the migration.
         * @return The action result.
         */
        fun execute(session: Session): T?

    }

    /**
     * CassandraMigration companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(CassandraMigration::class.java)
    }

}
