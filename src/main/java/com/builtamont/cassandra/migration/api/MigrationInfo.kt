/**
 * File     : MigrationInfo.kt
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

import java.util.*

/**
 * Info about a migration.
 */
// TODO: Implement additional properties from Flyway 4.x
interface MigrationInfo : Comparable<MigrationInfo> {

    /**
     * The type of migration (CQL, Java, ...)
     */
    val type: MigrationType
        get

    /**
     * The target version of this migration.
     */
    val checksum: Int?
        get

    /**
     * The schema version after the migration is complete.
     */
    val version: MigrationVersion
        get

    /**
     * The description of the migration.
     */
    val description: String
        get

    /**
     * The name of the script to execute for this migration, relative to its classpath or filesystem location.
     */
    val script: String
        get

    /**
     * The state of the migration (PENDING, SUCCESS, ...)
     */
    val state: MigrationState
        get

    /**
     * The timestamp when this migration was installed. (Only for applied migrations)
     */
    val installedOn: Date?
        get

//    /**
//     * The user that installed this migration. (Only for applied migrations)
//     */
//    val installedBy: String?
//        get
//
//    /**
//     * The rank of this installed migration. This is the most precise way to sort applied migrations by installation order.
//     * Migrations that were applied later have a higher rank. (Only for applied migrations)
//     */
//    val installedRank: Int?
//        get

    /**
     * The execution time (in millis) of this migration. (Only for applied migrations)
     */
    val executionTime: Int?
        get

}
