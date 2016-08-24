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
package com.contrastsecurity.cassandra.migration.action

import com.contrastsecurity.cassandra.migration.api.CassandraMigrationException
import com.contrastsecurity.cassandra.migration.api.MigrationState
import com.contrastsecurity.cassandra.migration.api.MigrationVersion
import com.contrastsecurity.cassandra.migration.api.resolver.MigrationResolver
import com.contrastsecurity.cassandra.migration.dao.SchemaVersionDAO
import com.contrastsecurity.cassandra.migration.info.AppliedMigration
import com.contrastsecurity.cassandra.migration.info.MigrationInfo
import com.contrastsecurity.cassandra.migration.info.MigrationInfoService
import com.contrastsecurity.cassandra.migration.logging.LogFactory
import com.contrastsecurity.cassandra.migration.utils.StopWatch
import com.contrastsecurity.cassandra.migration.utils.TimeFormat
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

    /**
     * Runs the actual migration.
     *
     * @return The number of successfully applied migrations.
     */
    fun run(): Int {
        val stopWatch = StopWatch()
        stopWatch.start()

        var migrationSuccessCount = 0
        while (true) {
            val firstRun = migrationSuccessCount == 0

            val infoService = MigrationInfoService(migrationResolver, schemaVersionDAO, migrationTarget, allowOutOfOrder, true)
            infoService.refresh()

            var currentSchemaVersion = MigrationVersion.EMPTY
            if (infoService.current() != null) {
                currentSchemaVersion = infoService.current().version
            }
            if (firstRun) {
                LOG.info("Current version of keyspace " + schemaVersionDAO.keyspace.name + ": " + currentSchemaVersion)
            }

            val future = infoService.future()
            if (future.size > 0) {
                val resolved = infoService.resolved()
                if (resolved.size == 0) {
                    LOG.warn("Keyspace " + schemaVersionDAO.keyspace.name + " has version " + currentSchemaVersion
                            + ", but no migration could be resolved in the configured locations !")
                } else {
                    LOG.warn("Keyspace " + schemaVersionDAO.keyspace.name + " has a version (" + currentSchemaVersion
                            + ") that is newer than the latest available migration ("
                            + resolved[resolved.size - 1].version + ") !")
                }
            }

            val failed = infoService.failed()
            if (failed.size > 0) {
                if (failed.size == 1 && failed[0].state === MigrationState.FUTURE_FAILED) {
                    LOG.warn("Keyspace " + schemaVersionDAO.keyspace.name + " contains a failed future migration to version " + failed[0].version + " !")
                } else {
                    throw CassandraMigrationException("Keyspace " + schemaVersionDAO.keyspace.name + " contains a failed migration to version " + failed[0].version + " !")
                }
            }

            val pendingMigrations = infoService.pending()

            if (pendingMigrations.size == 0) {
                break
            }

            val isOutOfOrder = pendingMigrations[0].version.compareTo(currentSchemaVersion) < 0
            val mv = applyMigration(pendingMigrations[0], isOutOfOrder) ?: //no more migrations
                    break

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
     */
    private fun applyMigration(migration: MigrationInfo, isOutOfOrder: Boolean): MigrationVersion? {
        val version = migration.version
        LOG.info("Migrating keyspace " + schemaVersionDAO.keyspace.name + " to version " + version + " - " + migration.description +
                if (isOutOfOrder) " (out of order)" else "")

        val stopWatch = StopWatch()
        stopWatch.start()

        try {
            val migrationExecutor = migration.resolvedMigration.executor
            try {
                migrationExecutor.execute(session)
            } catch (e: Exception) {
                throw CassandraMigrationException("Unable to apply migration", e)
            }

            LOG.debug("Successfully completed and committed migration of keyspace " +
                    schemaVersionDAO.keyspace.name + " to version " + version)
        } catch (e: CassandraMigrationException) {
            val failedMsg = "Migration of keyspace " + schemaVersionDAO.keyspace.name +
                    " to version " + version + " failed!"

            LOG.error(failedMsg + " Please restore backups and roll back database and code!")

            stopWatch.stop()
            val executionTime = stopWatch.totalTimeMillis.toInt()
            val appliedMigration = AppliedMigration(version, migration.description,
                    migration.type, migration.script, migration.checksum, user, executionTime, false)
            schemaVersionDAO.addAppliedMigration(appliedMigration)
            throw e
        }

        stopWatch.stop()
        val executionTime = stopWatch.totalTimeMillis.toInt()

        val appliedMigration = AppliedMigration(version, migration.description,
                migration.type, migration.script, migration.checksum, user, executionTime, true)
        schemaVersionDAO.addAppliedMigration(appliedMigration)

        return version
    }

    /**
     * Logs the summary of this migration run.
     *
     * @param migrationSuccessCount The number of successfully applied migrations.
     * @param executionTime The total time taken to perform this migration run (in ms).
     */
    private fun logSummary(migrationSuccessCount: Int, executionTime: Long) {
        if (migrationSuccessCount == 0) {
            LOG.info("Keyspace " + schemaVersionDAO.keyspace.name + " is up to date. No migration necessary.")
            return
        }

        if (migrationSuccessCount == 1) {
            LOG.info("Successfully applied 1 migration to keyspace " + schemaVersionDAO.keyspace.name + " (execution time " + TimeFormat.format(executionTime) + ").")
        } else {
            LOG.info("Successfully applied " + migrationSuccessCount + " migrations to keyspace " + schemaVersionDAO.keyspace.name + " (execution time " + TimeFormat.format(executionTime) + ").")
        }
    }

    /**
     * Migrate command companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(Migrate::class.java)
    }

}
