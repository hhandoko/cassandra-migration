/**
 * File     : MigrationInfoService.kt
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
package com.builtamont.cassandra.migration.api

interface MigrationInfoService {

    /**
     * Refreshes the info about all known migrations from both the classpath and the DB.
     */
    fun refresh()

    /**
     * Validate all migrations for consistency.
     *
     * @return The error message, or `null` if everything is fine.
     */
    fun validate(): String?

    /**
     * Retrieves the full set of infos about the migrations.
     *
     * @return The migrations.
     */
    fun all(): Array<MigrationInfo>

    /**
     * @return Current migration to be run.
     */
    fun current(): MigrationInfo?

    /**
     * Retrieves the full set of infos about the pending migrations.
     *
     * @return The pending migrations. An empty array if none.
     */
    fun pending(): Array<MigrationInfo>

    /**
     * Retrieves the full set of infos about the migrations applied on the DB.
     *
     * @return The applied migrations. An empty array if none.
     */
    fun applied(): Array<MigrationInfo>

    /**
     * Retrieves the full set of infos about the migrations resolved on the classpath.
     *
     * @return The resolved migrations. An empty array if none.
     */
    fun resolved(): Array<MigrationInfo>

    /**
     * Retrieves the full set of infos about the migrations that failed.
     *
     * @return The failed migrations. An empty array if none.
     */
    fun failed(): Array<MigrationInfo>

    /**
     * Retrieves the full set of infos about future migrations applied to the DB.
     *
     * @return The future migrations. An empty array if none.
     */
    fun future(): Array<MigrationInfo>

    /**
     * Retrieves the full set of infos about out of order migrations applied to the DB.
     *
     * @return The out of order migrations. An empty array if none.
     */
    fun outOfOrder(): Array<MigrationInfo>

}
