/**
 * File     : MigrationConfiguration.kt
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
package com.builtamont.cassandra.migration.api.configuration

/**
 * Main Cassandra migration configuration.
 */
// TODO: To be merged with `CassandraMigrationConfiguration`
class MigrationConfiguration : Configuration() {

    /**
     * Cassandra migration configuration properties.
     *
     * @param namespace The property namespace.
     * @param description The property description.
     */
    enum class MigrationProperty constructor(val namespace: String, val description: String) {
        SCRIPTS_ENCODING(BASE_PREFIX + "scripts.encoding", "Encoding for CQL scripts"),
        SCRIPTS_LOCATIONS(BASE_PREFIX + "scripts.locations", "Locations of the migration scripts in CSV format"),
        ALLOW_OUT_OF_ORDER(BASE_PREFIX + "scripts.allowoutoforder", "Allow out of order migration"),
        TARGET_VERSION(BASE_PREFIX + "version.target", "The target version. Migrations with a higher version number will be ignored.")
    }

}
