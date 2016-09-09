/**
 * File     : Migrate.kt
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
package com.builtamont.cassandra.migration.internal.command

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationInfo
import com.builtamont.cassandra.migration.api.MigrationState
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO
import com.builtamont.cassandra.migration.internal.info.MigrationInfoImpl
import com.builtamont.cassandra.migration.internal.info.MigrationInfoServiceImpl
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration
import com.builtamont.cassandra.migration.internal.util.StopWatch
import com.builtamont.cassandra.migration.internal.util.TimeFormat
import com.builtamont.cassandra.migration.internal.util.logging.LogFactory
import com.datastax.driver.core.Session

/**
 * Main workflow for migrating the database.
 *
 * @param migrationResolver The Cassandra migration resolver.
 * @param migrationTarget The target version of the migration.
 * @param schemaVersionDAO The Cassandra migration schema version DAO.
 * @param session The Cassandra session connection to use to execute the migration.
 * @param user The user to execute the migration as.
 * @param allowOutOfOrder True to allow migration to be run "out of order".
 */
class Migrate(
    private val migrationResolver: MigrationResolver,
    private val migrationTarget: MigrationVersion,
    private val schemaVersionDAO: SchemaVersionDAO,
    private val session: Session,
    private val user: String,
    private val allowOutOfOrder: Boolean
) {

    /** Keyspace name lensing */
    private val keyspaceName = schemaVersionDAO.keyspaceConfig.name

    /**
     * Runs the actual migration.
     *
     * @return The number of successfully applied migrations.
     * @throws CassandraMigrationException when migration execution failed for any reason.
     */
    @Throws(CassandraMigrationException::class)
    fun run(): Int {
        val stopWatch = StopWatch()
        stopWatch.start()

        var migrationSuccessCount = 0
        while (true) {
            val infoService = MigrationInfoServiceImpl(migrationResolver, schemaVersionDAO, migrationTarget, allowOutOfOrder, true)
            infoService.refresh()

            // Initialise `firstRun` and `currentSchemaVersion` variables
            val firstRun = migrationSuccessCount == 0
            val currentSchemaVersion = infoService.current()?.version ?: MigrationVersion.CURRENT

            // First run only
            // ~~~~~
            // Log first run message and warn the user if `out-of-order` is enabled
            if (firstRun) {
                LOG.info("Current version of keyspace $keyspaceName: $currentSchemaVersion")

                if (allowOutOfOrder) {
                    LOG.warn("'outOfOrder' mode is active. Migration of keyspace $keyspaceName may not be reproducible.")
                }
            }

            // Future migrations
            // ~~~~~
            // Log future migrations and warn users if there are no resolved migrations, or
            // if there current version migration is newer than what is available
            val future = infoService.future()
            if (future.isNotEmpty()) {
                val resolvedLogMsg = "Keyspace $keyspaceName has version $currentSchemaVersion"
                val resolved = infoService.resolved()
                if (resolved.size == 0) {
                    LOG.warn("$resolvedLogMsg, but no migration could be resolved in the configured locations!")
                } else {
                    val latestVersion = resolved[resolved.size - 1].version
                    LOG.warn("$resolvedLogMsg that is newer than the latest available migration ($latestVersion)!")
                }
            }

            // Failed migrations
            // ~~~~~
            // Log failed future migrations and throw `CassandraMigrationException` for everything else
            val failed = infoService.failed()
            if (failed.isNotEmpty()) {
                val isFutureFailed = failed[0].state === MigrationState.FUTURE_FAILED
                val failedVersion = failed[0].version
                if (failed.size == 1 && isFutureFailed) {
                    val failedLogMsg = "Keyspace $keyspaceName contains a failed future migration to version $failedVersion!"
                    LOG.warn(failedLogMsg)
                } else {
                    val failedLogMsg = "Keyspace $keyspaceName contains a failed migration to version $failedVersion!"
                    throw CassandraMigrationException(failedLogMsg)
                }
            }

            // Pending migrations
            // ~~~~~
            // Apply pending migrations
            val pendingMigrations = infoService.pending()
            if (pendingMigrations.isNotEmpty()) {
                if (pendingMigrations[0] is MigrationInfoImpl) {
                    val isOutOfOrder = pendingMigrations[0].version.compareTo(currentSchemaVersion) < 0
                    applyMigration(pendingMigrations[0] as MigrationInfoImpl, isOutOfOrder) ?: break
                }
            } else {
                // Exit if there are no more pending migrations
                break
            }

            migrationSuccessCount++
        }

        stopWatch.stop()
        logSummary(migrationSuccessCount, stopWatch.totalTimeMillis)

        return migrationSuccessCount
    }

    /**
     * Applies this migration to the database. The migration state and the execution time are updated accordingly.
     *
     * @param migration The migration to apply.
     * @param isOutOfOrder If this migration is being applied out of order.
     * @return The result of the migration.
     * @throws CassandraMigrationException when migration cannot be applied.
     */
    @Throws(CassandraMigrationException::class)
    private fun applyMigration(migration: MigrationInfoImpl, isOutOfOrder: Boolean): MigrationVersion? {

        /**
         * Add applied migration into the Cassandra migration versioning table.
         *
         * @param version The migration version currently being applied.
         * @param migration The migration to apply.
         * @param executionTime The total time taken to perform this migration run (in ms).
         * @param success True to denote successful migration application.
         */
        fun addAppliedMigration(version: MigrationVersion, migration: MigrationInfo, executionTime: Long, success: Boolean = true) {
            schemaVersionDAO.addAppliedMigration(
                AppliedMigration(
                    version,
                    migration.description,
                    migration.type,
                    migration.script,
                    migration.checksum,
                    user,
                    executionTime.toInt(),
                    success
                )
            )
        }

        val version = migration.version
        val logMsg = "Migration of keyspace $keyspaceName to version $version"

        val oooLogMsg = if (isOutOfOrder) " (out of order)" else ""
        LOG.info("$logMsg  - ${migration.description}$oooLogMsg")

        val stopWatch = StopWatch()
        stopWatch.start()

        var isMigrationSuccess = false
        try {
            val executor = migration.resolvedMigration!!.executor!!
            executor.execute(session)
            isMigrationSuccess = true
            LOG.debug("$logMsg success!")
        } catch (e: Exception) {
            LOG.error("$logMsg failed! Please restore backups and roll back database and code!")
            throw CassandraMigrationException("Unable to apply migration", e)
        } finally {
            stopWatch.stop()
            addAppliedMigration(version, migration, stopWatch.totalTimeMillis, isMigrationSuccess)
        }

        return version
    }

    /**
     * Logs the summary of this migration run.
     *
     * @param count The number of successfully applied migrations.
     * @param executionTime The total time taken to perform this migration run (in ms).
     */
    private fun logSummary(count: Int, executionTime: Long) {

        /**
         * @return No migration is run log message (schema is up-to-date).
         */
        fun noMigrationLogMsg(): String {
            return "Keyspace $keyspaceName is up to date, no migration necessary"
        }

        /**
         * @return The migration success log message.
         */
        fun successLogMsg(): String {
            return "Successfully applied $count migration(s) to keyspace $keyspaceName (execution time ${TimeFormat.format(executionTime)})"
        }

        when (count) {
            0    -> LOG.info(noMigrationLogMsg())
            1    -> LOG.info(successLogMsg())
            else -> LOG.info(successLogMsg())
        }

        if (count == 0) return
    }

    /**
     * Migrate command companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(Migrate::class.java)
    }

}
