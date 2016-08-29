/**
 * File     : ResolvedMigration.kt
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
package com.builtamont.cassandra.migration.api.resolver

import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion

/**
 * Migration resolved through a MigrationResolver. Can be applied against a database.
 */
interface ResolvedMigration {

    /**
     * The target version of this migration.
     */
    var version: MigrationVersion? get

    /**
     * The description of the migration.
     */
    var description: String?
        get

    /**
     * The name of the script to execute for this migration, relative to its classpath location.
     */
    var script: String?
        get

    /**
     * The checksum of the migration.
     */
    var checksum: Int?
        get

    /**
     * The type of migration (CQL, JAVA_DRIVER)
     */
    var type: MigrationType?
        get

    /**
     * The physical location of the migration on disk.
     */
    var physicalLocation: String?
        get

    /**
     * The executor to run this migration.
     */
    var executor: MigrationExecutor?
        get

}
