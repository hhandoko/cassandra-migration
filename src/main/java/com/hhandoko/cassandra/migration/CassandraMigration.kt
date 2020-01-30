/**
 * File     : CassandraMigration.kt
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
 *   Derivative - Copyright (c) 2016 - 2018 cassandra-migration Contributors
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
package com.hhandoko.cassandra.migration

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader
import com.hhandoko.cassandra.migration.api.CassandraMigrationException
import com.hhandoko.cassandra.migration.api.MigrationInfoService
import com.hhandoko.cassandra.migration.api.MigrationVersion
import com.hhandoko.cassandra.migration.api.configuration.CassandraMigrationConfiguration
import com.hhandoko.cassandra.migration.api.configuration.ConfigurationProperty
import com.hhandoko.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.hhandoko.cassandra.migration.api.resolver.MigrationResolver
import com.hhandoko.cassandra.migration.internal.command.Baseline
import com.hhandoko.cassandra.migration.internal.command.Initialize
import com.hhandoko.cassandra.migration.internal.command.Migrate
import com.hhandoko.cassandra.migration.internal.command.Validate
import com.hhandoko.cassandra.migration.internal.dbsupport.SchemaVersionDAO
import com.hhandoko.cassandra.migration.internal.info.MigrationInfoServiceImpl
import com.hhandoko.cassandra.migration.internal.resolver.CompositeMigrationResolver
import com.hhandoko.cassandra.migration.internal.util.Locations
import com.hhandoko.cassandra.migration.internal.util.StringUtils
import com.hhandoko.cassandra.migration.internal.util.VersionPrinter
import com.hhandoko.cassandra.migration.internal.util.logging.LogFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.util.function.Supplier

/**
 * This is the centre point of Cassandra migration, and for most users, the only class they will ever have to deal with.
 * It is THE public API from which all important Cassandra migration functions such as clean, validate and migrate can be called.
 */
class CassandraMigration : CassandraMigrationConfiguration {

    /**
     * The Cassandra keyspace configuration.
     */
    var keyspaceConfig: KeyspaceConfiguration

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
     * Migration script execution timeout in seconds.
     * (default: 60)
     */
    override var timeout = 60

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

        ConfigFactory.invalidateCaches()
        ConfigFactory.load().let {
            it.extract<String?>(ConfigurationProperty.TARGET_VERSION.namespace)?.let {
                this.target = MigrationVersion.fromVersion(it.trim().toUpperCase())
            }

            it.extract<String?>(ConfigurationProperty.BASELINE_VERSION.namespace)?.let {
                this.baselineVersion = MigrationVersion.fromVersion(it.trim().toUpperCase())
            }

            it.extract<String?>(ConfigurationProperty.BASELINE_DESCRIPTION.namespace)?.let {
                this.baselineDescription = it.trim()
            }

            it.extract<String?>(ConfigurationProperty.SCRIPTS_ENCODING.namespace)?.let {
                this.encoding = it.trim()
            }

            it.extract<String?>(ConfigurationProperty.SCRIPTS_LOCATIONS.namespace)?.let {
                this.locations = StringUtils.tokenizeToStringArray(it, ",")
            }

            it.extract<Int?>(ConfigurationProperty.SCRIPTS_TIMEOUT.namespace)?.let {
                this.timeout = it
            }

            it.extract<Boolean?>(ConfigurationProperty.ALLOW_OUT_OF_ORDER.namespace)?.let {
                this.allowOutOfOrder = it
            }

            it.extract<String?>(ConfigurationProperty.TABLE_PREFIX.namespace)?.let {
                this.tablePrefix = it.trim()
            }
        }
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
    fun migrate(session: CqlSession): Int {
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
    fun info(session: CqlSession): MigrationInfoService {
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
    fun validate(session: CqlSession) {
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
    fun baseline(session: CqlSession) {
        execute(baselineAction(), session)
    }

    /**
     * Executes this command with proper resource handling and cleanup.
     *
     * @param action The action to execute.
     * @param extSession The external session if provided, defaults to null.
     * @param T The action result type.
     * @return The action result.
     */
    internal fun <T> execute(action: Action<T>, extSession: CqlSession? = null): T {
        val result: T

        VersionPrinter.printVersion()

        val useExternalSession = extSession != null
//        var cluster: Cluster? = null
        var session: CqlSession? = null
        try {
            if (extSession != null) {
                session = extSession
            } else {
                // Guard clauses: Cluster and Keyspace must be defined
                val errorMsg = "Unable to establish Cassandra session"
                if (keyspaceConfig == null) throw IllegalArgumentException("$errorMsg. Keyspace is not configured.")
//                if (keyspaceConfig.clusterConfig == null) throw IllegalArgumentException("$errorMsg. Cluster is not configured.")
                if (keyspaceConfig.name.isNullOrEmpty()) throw IllegalArgumentException("$errorMsg. Keyspace is not specified.")

                // Build the Cluster
                val builder = CqlSession.builder()

                keyspaceConfig.prefix?.let {
                    builder.withConfigLoader(DefaultDriverConfigLoader(Supplier {
                        ConfigFactory.invalidateCaches()
                        val ref = ConfigFactory.load().getConfig("datastax-java-driver")
                        val app: Config = ref.getConfig(it);
                        app.withFallback(ref)
                    }));
                }
                builder.withKeyspace(keyspaceConfig.name)
                session = builder.build()

                LOG.info(getConnectionInfo(session))
            }
            result = action.execute(session!!)
        } finally {
            // NOTE: We don't close external sessions, and let those sessions be managed outside the Cassandra Migration
            //       lifecycle.
            if (!useExternalSession) {
                if (session != null && !session.isClosed)
                    try {
                        session.close()
                    } catch (e: Exception) {
                        LOG.warn("Error closing Cassandra session")
                    }
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
    private fun getConnectionInfo(session: CqlSession): String {

        val sb = StringBuilder()
        sb.append("Connected to cluster: ")
        sb.append(session.name)
        sb.append("\n")
        for (host in session.metadata.nodes.values) {
            sb.append("Data center: ")
            sb.append(host.datacenter)
            sb.append("; Host: ")
            sb.append(host.listenAddress)
        }
        return sb.toString()
    }

    /**
     * Creates the MigrationResolver.
     *
     * @return A new, fully configured, MigrationResolver instance.
     */
    private fun createMigrationResolver(): MigrationResolver {
        return CompositeMigrationResolver(classLoader, Locations(*locations), encoding, timeout)
    }

    private fun migrationTableName(): String{
        return tablePrefix.orEmpty() + MigrationVersion.CURRENT.table
    }

    /**
     * Creates the SchemaVersionDAO.
     *
     * @return A configured SchemaVersionDAO instance.
     */
    private fun createSchemaVersionDAO(session: CqlSession): SchemaVersionDAO {
        return SchemaVersionDAO(session, keyspaceConfig, migrationTableName())
    }

    /**
     * @return The database migration action.
     */
    private fun migrateAction(): Action<Int> {
        return object: Action<Int> {
            override fun execute(session: CqlSession): Int {
                Initialize().run(session, keyspaceConfig, migrationTableName())

                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = createSchemaVersionDAO(session)
                val migrate = Migrate(
                        migrationResolver,
                        target,
                        schemaVersionDAO,
                        session,
                        session.name ?: "",
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
            override fun execute(session: CqlSession): MigrationInfoService {
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
            override fun execute(session: CqlSession): String? {
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
            override fun execute(session: CqlSession): Unit {
                val migrationResolver = createMigrationResolver()
                val schemaVersionDAO = createSchemaVersionDAO(session)
                val baseline = Baseline(
                        migrationResolver,
                        baselineVersion,
                        schemaVersionDAO,
                        baselineDescription,
                        session.name ?: ""
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
        fun execute(session: CqlSession): T

    }

    /**
     * CassandraMigration companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(CassandraMigration::class.java)
    }

}
