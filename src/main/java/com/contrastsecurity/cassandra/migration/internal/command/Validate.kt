/**
 * File     : Validate.kt
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
package com.contrastsecurity.cassandra.migration.internal.command

import com.contrastsecurity.cassandra.migration.api.MigrationVersion
import com.contrastsecurity.cassandra.migration.api.resolver.MigrationResolver
import com.contrastsecurity.cassandra.migration.internal.dbsupport.SchemaVersionDAO
import com.contrastsecurity.cassandra.migration.api.MigrationInfoService
import com.contrastsecurity.cassandra.migration.internal.info.MigrationInfoServiceImpl
import com.contrastsecurity.cassandra.migration.internal.util.logging.LogFactory
import com.contrastsecurity.cassandra.migration.utils.StopWatch
import com.contrastsecurity.cassandra.migration.utils.TimeFormat

/**
 * Handles the validate command.
 * Validates the applied migrations against the available ones.
 *
 * @param migrationResolver The Cassandra migration resolver.
 * @param migrationTarget The target version of the migration.
 * @param schemaVersionDAO The Cassandra migration schema version DAO.
 * @param outOfOrder True to allow migration to be run "out of order".
 * @param pendingOrFuture True to allow pending or Future<T> migration to be run.
 */
class Validate(
        private val migrationResolver: MigrationResolver,
        private val migrationTarget: MigrationVersion,
        private val schemaVersionDAO: SchemaVersionDAO,
        private val outOfOrder: Boolean,
        private val pendingOrFuture: Boolean
) {

    /**
     * Runs the actual migration.
     *
     * @return The validation error, if any.
     */
    fun run(): String? {
        val stopWatch = StopWatch()
        stopWatch.start()

        val infoService = MigrationInfoServiceImpl(migrationResolver, schemaVersionDAO, migrationTarget, outOfOrder, pendingOrFuture)
        infoService.refresh()
        val count = infoService.all().size
        val validationError = infoService.validate()

        stopWatch.stop()

        logSummary(count, stopWatch.totalTimeMillis)

        return validationError
    }

    /**
     * Logs the summary of this migration run.
     *
     * @param count The number of successfully applied migrations.
     * @param executionTime The total time taken to perform this migration run (in ms).
     */
    private fun logSummary(count: Int, executionTime: Long) {
        LOG.info("Validated %d migrations (execution time %s)".format(count, TimeFormat.format(executionTime)))
    }

    /**
     * Validate command companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(Validate::class.java)
    }

}
