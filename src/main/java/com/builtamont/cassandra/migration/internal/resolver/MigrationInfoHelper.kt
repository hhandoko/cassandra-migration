/**
 * File     : MigrationInfoHelper.kt
 * License  :
 *   Original   - Copyright (c) 2010 - 2016 Boxfuse GmbH
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
package com.builtamont.cassandra.migration.internal.resolver

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.internal.util.Pair

/**
 * Parsing support for migrations that use the standard Cassandra migration version + description embedding in their name.
 * These migrations have names like 1_2__Description.
 */
object MigrationInfoHelper {

    /**
     * Extracts the schema version and the description from a migration name formatted as 1_2__Description.
     *
     * @param migrationName The migration name to parse. Should not contain any folders or packages.
     * @param prefix The migration prefix.
     * @param separator The migration separator.
     * @param suffix The migration suffix.
     * @return The extracted schema version.
     * @throws CassandraMigrationException if the migration name does not follow the standard conventions.
     */
    fun extractVersionAndDescription(
        migrationName: String,
        prefix: String,
        separator: String,
        suffix: String
    ): Pair<MigrationVersion, String> {
        val cleanMigrationName = migrationName.substring(prefix.length, migrationName.length - suffix.length)

        // Handle the description
        val descriptionPos = cleanMigrationName.indexOf(separator)
        if (descriptionPos < 0) {
            val suggestedFormatMsg = "${prefix}1_2${separator}Description${suffix}"
            val migrationNameErrorMsg = "Wrong migration name format: $migrationName (It should look like this: $suggestedFormatMsg)"
            throw CassandraMigrationException(migrationNameErrorMsg)
        }

        val version = cleanMigrationName.substring(0, descriptionPos)
        val description = cleanMigrationName.substring(descriptionPos + separator.length).replace("_".toRegex(), " ")
        return Pair.of(MigrationVersion.fromVersion(version), description)
    }

}
