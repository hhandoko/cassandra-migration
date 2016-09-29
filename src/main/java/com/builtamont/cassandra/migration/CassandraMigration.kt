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
package com.builtamont.cassandra.migration

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationInfoService
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.configuration.CassandraMigrationConfiguration
import com.builtamont.cassandra.migration.api.configuration.ConfigurationProperty
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.builtamont.cassandra.migration.internal.command.Baseline
import com.builtamont.cassandra.migration.internal.command.Initialize
import com.builtamont.cassandra.migration.internal.command.Migrate
import com.builtamont.cassandra.migration.internal.command.Validate
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO
import com.builtamont.cassandra.migration.internal.info.MigrationInfoServiceImpl
import com.builtamont.cassandra.migration.internal.resolver.CompositeMigrationResolver
import com.builtamont.cassandra.migration.internal.util.ScriptsLocations
import com.builtamont.cassandra.migration.internal.util.StringUtils
import com.builtamont.cassandra.migration.internal.util.VersionPrinter
import com.builtamont.cassandra.migration.internal.util.logging.LogFactory
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Metadata
import com.datastax.driver.core.Session

/**
 * This is the centre point of Cassandra migration, and for most users, the only class they will ever have to deal with.
 * It is THE public API from which all important Cassandra migration functions such as clean, validate and migrate can be called.
 */
class CassandraMigration : CassandraMigrationConfiguration {

    /**
     * The Cassandra keyspace configuration.
     */
    lateinit var keyspaceConfig: KeyspaceConfiguration

    /**
     * The ClassLoader to use for resolving migrations on the classpath.
     * (default: Thread.currentThread().getContextClassLoader())
     */
    override var classLoader = Thread.currentThread().contextClassLoader

    /**
     * The target version. Migrations with a higher version number will be ignored.
     * (default: MigrationVersion.LATEST)
     */
    override var target = MigrationVersion.LATEST

    /**
     * The baseline version.
     * (default: MigrationVersion.fromVersion("1"))
     */
    override var baselineVersion = MigrationVersion.fromVersion("1")

    /**
     * The baseline description.
     * (default: "<< Cassandra Baseline >>")
     */
    override var baselineDescription = "<< Cassandra Baseline >>"

    /**
     * The encoding of CQL migrations script encoding.
     * (default: "UTF-8")
     */
    override var encoding = "UTF-8"

    /**
     * Locations to scan recursively for migrations.
     * (default: new String[] {"db/migration"})
     */
    override var locations = arrayOf("db/migration")

    /**
     * The prefix to be prepended to `cassandra_migration_version*` table names.
     * (default: "")
     */
    override var tablePrefix = ""

    /**
     * Allow out of order migrations.
     * (default: false)
     */
    var allowOutOfOrder = false

    /**
     * CassandraMigration initialization.
     */
    init {
        this.keyspaceConfig = KeyspaceConfiguration()

        val targetVersionProp = System.getProperty(ConfigurationProperty.TARGET_VERSION.namespace)
        if (!targetVersionProp.isNullOrBlank()) target = MigrationVersion.fromVersion(targetVersionProp)

        val encodingProp = System.getProperty(ConfigurationProperty.SCRIPTS_ENCODING.namespace)
        if (!encodingProp.isNullOrBlank()) encoding = encodingProp.trim()

        val locationsProp = System.getProperty(ConfigurationProperty.SCRIPTS_LOCATIONS.namespace)
        if (!locationsProp.isNullOrBlank()) locations = StringUtils.tokenizeToStringArray(locationsProp, ",")

        val tablePrefixProp = System.getProperty(ConfigurationProperty.TABLE_PREFIX.namespace)
        if (!tablePrefixProp.isNullOrBlank()) tablePrefix = tablePrefixProp.trim()

        val allowOutOfOrderProp = System.getProperty(ConfigurationProperty.ALLOW_OUT_OF_ORDER.namespace)
        if (!allowOutOfOrderProp.isNullOrBlank()) allowOutOfOrder = allowOutOfOrderProp.toBoolean()
    }

    /**
     * Starts the database migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.
     *
     * @return The number of successfully applied migrations.
     */
    fun migrate(): Int {
        return execute(migrateAction())
    }

    /**
     * Starts the database migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.
     *
     * @param session The Cassandra connection session.
     * @return The number of successfully applied migrations.
     */
    fun migrate(session: Session): Int {
        return execute(migrateAction(), session)
    }

    /**
     * Retrieves the complete information about all the migrations including applied, pending and current migrations with
     * details and status.
     *
     * @return All migrations sorted by version, oldest first.
     */
    fun info(): MigrationInfoService {
        return execute(infoAction())
    }

    /**
     * Retrieves the complete information about all the migrations including applied, pending and current migrations with
     * details and status.
     *
     * @param session The Cassandra connection session.
     * @return All migrations sorted by version, oldest first.
     */
    fun info(session: Session): MigrationInfoService {
        return execute(infoAction(), session)
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
        val validationError = execute(validateAction())

        if (validationError != null) {
            throw CassandraMigrationException("Validation failed. $validationError")
        }
    }

    /**
     * Validate applied migrations against resolved ones (on the filesystem or classpath)
     * to detect accidental changes that may prevent the schema(s) from being recreated exactly.
     *
     * Validation fails if:
     *  * differences in migration names, types or checksums are found
     *  * versions have been applied that aren't resolved locally anymore
     *  * versions have been resolved that haven't been applied yet
     *
     * @param session The Cassandra connection session.
     */
    fun validate(session: Session) {
        val validationError = execute(validateAction(), session)

        if (validationError != null) {
            throw CassandraMigrationException("Validation failed. $validationError")
        }
    }

    /**
     * Baselines an existing database, excluding all migrations up to and including baselineVersion.
     */
    fun baseline() {
        execute(baselineAction())
    }

    /**
     * Baselines an existing database, excluding all migrations up to and including baselineVersion.
     *
     * @param session The Cassandra connection session.
     */
    fun baseline(session: Session) {
        execute(baselineAction(), session)
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

        var cluster: Cluster? = null
        var session: Session? = null
        try {
            // Guard clauses: Cluster and Keyspace must be defined
            val errorMsg = "Unable to establish Cassandra session"
            if (keyspaceConfig == null) throw IllegalArgumentException("$errorMsg. Keyspace is not configured.")
            if (keyspaceConfig.clusterConfig == null) throw IllegalArgumentException("$errorMsg. Cluster is not configured.")
            if (keyspaceConfig.name.isNullOrEmpty()) throw IllegalArgumentException("$errorMsg. Keyspace is not specified.")

            // Build the Cluster
            val builder = Cluster.Builder()
            builder.addContactPoints(*keyspaceConfig.clusterConfig.contactpoints).withPort(keyspaceConfig.clusterConfig.port)
            if (!keyspaceConfig.clusterConfig.username.isNullOrBlank()) {
                if (!keyspaceConfig.clusterConfig.password.isNullOrBlank()) {
                    builder.withCredentials(keyspaceConfig.clusterConfig.username, keyspaceConfig.clusterConfig.password)
                } else {
                    throw IllegalArgumentException("Password must be provided with username.")
                }
            }
            cluster = builder.build()

            LOG.info(getConnectionInfo(cluster.metadata))

            // Create a new Session
            session = cluster.newSession()

            // Connect to the specific Keyspace context (if already defined)
            val keyspaces = cluster.metadata.keyspaces.map { it.name }
            val keyspaceExists = keyspaces.first { it.equals(keyspaceConfig.name, ignoreCase = true) }.isNotEmpty()
            if (keyspaceExists) {
                session = cluster.connect(keyspaceConfig.name)
            } else {
                throw CassandraMigrationException("Keyspace: ${keyspaceConfig.name} does not exist.")
            }

            result = action.execute(session)
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
     * Executes this command with an existing session, with proper resource handling and cleanup.
     *
     * @param action The action to execute.
     * @param session The Cassandra connection session.
     * @param T The action result type.
     * @return The action result.
     */
    internal fun <T> execute(action: Action<T>, session: Session): T {
        return action.execute(session)
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
        return CompositeMigrationResolver(classLoader, ScriptsLocations(*locations), encoding)
    }

    private fun migrationTableName(): String{
        return tablePrefix + MigrationVersion.CURRENT.table
    }

    /**
     * Creates the SchemaVersionDAO.
     *
     * @return A configured SchemaVersionDAO instance.
     */
    private fun createSchemaVersionDAO(session: Session): SchemaVersionDAO {
        return SchemaVersionDAO(session, keyspaceConfig, migrationTableName())
    }

    /**
     * @return The database migration action.
     */
    private fun migrateAction(): Action<Int> {
        return object: Action<Int> {
            override fun execute(session: Session): Int {
                Initialize().run(session, keyspaceConfig, migrationTableName())

                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = createSchemaVersionDAO(session)
                val migrate = Migrate(
                        migrationResolver,
                        target,
                        schemaVersionDAO,
                        session,
                        keyspaceConfig.clusterConfig.username ?: "",
                        allowOutOfOrder
                )

                return migrate.run()
            }
        }
    }

    /**
     * @return The migration info service action.
     */
    private fun infoAction(): Action<MigrationInfoService> {
        return object : Action<MigrationInfoService> {
            override fun execute(session: Session): MigrationInfoService {
                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = createSchemaVersionDAO(session)
                val migrationInfoService = MigrationInfoServiceImpl(
                        migrationResolver,
                        schemaVersionDAO,
                        target,
                        outOfOrder = false,
                        pendingOrFuture = true
                )
                migrationInfoService.refresh()

                return migrationInfoService
            }
        }
    }

    /**
     * @return The migration validation action.
     */
    private fun validateAction(): Action<String?> {
        return object : Action<String?> {
            override fun execute(session: Session): String? {
                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = createSchemaVersionDAO(session)
                val validate = Validate(
                        migrationResolver,
                        target,
                        schemaVersionDAO,
                        outOfOrder = true,
                        pendingOrFuture = false
                )

                return validate.run()
            }
        }
    }

    /**
     * @return The migration baselining action.
     */
    private fun baselineAction(): Action<Unit> {
        return object : Action<Unit> {
            override fun execute(session: Session): Unit {
                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = createSchemaVersionDAO(session)
                val baseline = Baseline(
                        migrationResolver,
                        baselineVersion,
                        schemaVersionDAO,
                        baselineDescription,
                        keyspaceConfig.clusterConfig.username ?: ""
                )
                baseline.run()
            }
        }
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
        fun execute(session: Session): T

    }

    /**
     * CassandraMigration companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(CassandraMigration::class.java)
    }

}
