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
import com.datastax.driver.core.NettySSLOptions
import com.datastax.driver.core.Session
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy
import com.datastax.driver.core.policies.TokenAwarePolicy
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

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
     * @param extSession The external session if provided, defaults to null.
     * @param T The action result type.
     * @return The action result.
     */
    internal fun <T> execute(action: Action<T>, extSession: Session? = null): T {
        val result: T

        VersionPrinter.printVersion(classLoader)

        val useExternalSession = extSession != null
        var cluster: Cluster? = null
        var session: Session? = null
        try {
            if (extSession != null) {
                // TODO: Refactor KeyspaceConfiguration, it's referenced indirectly in many places in this class (code smell)
                keyspaceConfig.name = extSession.loggedKeyspace
                session = extSession
                cluster = extSession.cluster
            } else {
                // Guard clauses: Cluster and Keyspace must be defined
                val errorMsg = "Unable to establish Cassandra session"
                if (keyspaceConfig == null) throw IllegalArgumentException("$errorMsg. Keyspace is not configured.")
                if (keyspaceConfig.clusterConfig == null) throw IllegalArgumentException("$errorMsg. Cluster is not configured.")
                if (keyspaceConfig.name.isNullOrEmpty()) throw IllegalArgumentException("$errorMsg. Keyspace is not specified.")

                // Build the Cluster
                val builder = Cluster.Builder()
                builder.addContactPoints(*keyspaceConfig.clusterConfig.contactpoints).withPort(keyspaceConfig.clusterConfig.port)

                // Use TokenAware & DCAware load balancing policies
                builder.withLoadBalancingPolicy(TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().build()))

                if (!keyspaceConfig.clusterConfig.username.isNullOrBlank()) {
                    if (!keyspaceConfig.clusterConfig.password.isNullOrBlank()) {
                        builder.withCredentials(keyspaceConfig.clusterConfig.username, keyspaceConfig.clusterConfig.password)
                    } else {
                        throw IllegalArgumentException("Password must be provided with username.")
                    }
                }

                // Add SSL options to cluster builder
                if (keyspaceConfig.clusterConfig.truststore != null) {
                    FileInputStream(keyspaceConfig.clusterConfig.truststore?.toFile()).use {

                        val sslCtxBuilder = SslContextBuilder.forClient()
                                .sslProvider(SslProvider.JDK)
                                // The Java cryptographic extensions (JCE) are required for AES 256
                                .ciphers(listOf("TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA"))

                        val truststore = KeyStore.getInstance("JKS")
                        truststore.load(it, keyspaceConfig.clusterConfig.truststorePassword?.toCharArray() ?:
                                throw IllegalArgumentException("Truststore password must be provided with truststore."))

                        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                        tmf.init(truststore)
                        sslCtxBuilder.trustManager(tmf)

                        if (keyspaceConfig.clusterConfig.keystore != null) {
                            FileInputStream(keyspaceConfig.clusterConfig.keystore?.toFile()).use {

                                val keystore = KeyStore.getInstance("JKS")
                                val keystorePass = keyspaceConfig.clusterConfig.keystorePassword?.toCharArray() ?:
                                        throw IllegalArgumentException("Keystore password must be provided with keystore.")
                                keystore.load(it, keystorePass)

                                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                                kmf.init(keystore, keystorePass)
                                sslCtxBuilder.keyManager(kmf)
                            }
                        }
                        builder.withSSL(NettySSLOptions(sslCtxBuilder.build()))
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

                if (cluster != null && !cluster.isClosed)
                    try {
                        cluster.close()
                    } catch (e: Exception) {
                        LOG.warn("Error closing Cassandra cluster")
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
        return tablePrefix.orEmpty() + MigrationVersion.CURRENT.table
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
