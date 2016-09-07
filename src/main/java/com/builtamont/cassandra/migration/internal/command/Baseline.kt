/**
 * File     : Baseline.kt
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
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO

/**
 * Handles the baseline command.
 *
 * @param migrationResolver The Cassandra migration resolver.
 * @param baselineVersion The baseline version of the migration.
 * @param schemaVersionDAO The Cassandra migration schema version DAO.
 * @param baselineDescription The baseline version description / comments.
 * @param user The user to execute the migration as.
 */
class Baseline(
    private val migrationResolver: MigrationResolver,
    private val baselineVersion: MigrationVersion,
    private val schemaVersionDAO: SchemaVersionDAO,
    private val baselineDescription: String,
    private val user: String
) {

    /**
     * Runs the migration baselining.
     *
     * @return The number of successfully applied migration baselining.
     * @throws CassandraMigrationException when migration baselining failed for any reason.
     */
    @Throws(CassandraMigrationException::class)
    fun run() {
        val baselineMigration = schemaVersionDAO.baselineMarker
        if (schemaVersionDAO.hasAppliedMigrations()) {
            val msg = "Unable to baseline metadata table ${schemaVersionDAO.tableName} as it already contains migrations"
            throw CassandraMigrationException(msg)
        }

        if (schemaVersionDAO.hasBaselineMarker()) {
            val isNotBaselineByVersion = !(baselineMigration.version?.equals(baselineVersion) ?: false)
            val isNotBaselineByDescription = !baselineMigration.description.equals(baselineDescription)
            if (isNotBaselineByVersion || isNotBaselineByDescription) {
                val msg = "Unable to baseline metadata table ${schemaVersionDAO.tableName} with ($baselineVersion, $baselineDescription)" +
                        " as it has already been initialized with (${baselineMigration.version}, ${baselineMigration.description})"
                throw CassandraMigrationException(msg)
            }
        } else {
            if (baselineVersion.equals(MigrationVersion.fromVersion("0"))) {
                val msg = "Unable to baseline metadata table ${schemaVersionDAO.tableName} with version 0 as this version was used for schema creation"
                throw CassandraMigrationException(msg)
            }
            schemaVersionDAO.addBaselineMarker(baselineVersion, baselineDescription, user)
        }
    }

}