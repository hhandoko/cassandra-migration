/**
 * File     : MigrationConfigs.kt
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

import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.internal.util.StringUtils

/**
 * Main Cassandra migration configuration.
 */
// TODO: To be merged with `CassandraMigrationConfiguration`
class MigrationConfiguration {

    /**
     * Cassandra migration configuration properties.
     */
    enum class MigrationProperty constructor(val prefix: String, val description: String) {
        SCRIPTS_ENCODING("cassandra.migration.scripts.encoding", "Encoding for CQL scripts"),
        SCRIPTS_LOCATIONS("cassandra.migration.scripts.locations", "Locations of the migration scripts in CSV format"),
        ALLOW_OUTOFORDER("cassandra.migration.scripts.allowoutoforder", "Allow out of order migration"),
        TARGET_VERSION("cassandra.migration.version.target", "The target version. Migrations with a higher version number will be ignored.")
    }

    /**
     * The encoding of Cql migration scripts.
     * (default: UTF-8)
     */
    var encoding = "UTF-8"

    /**
     * Locations of the migration scripts in CSV format.
     * (default: db/migration)
     */
    var scriptsLocations = arrayOf("db/migration")

    /**
     * The target version. Migrations with a higher version number will be ignored.
     * (default: the latest version)
     */
    var target = MigrationVersion.LATEST
        private set

    /**
     * Set the target version property from String value.
     *
     * @param target Target version as String.
     */
    fun setTargetFromString(target: String) {
        this.target = MigrationVersion.fromVersion(target)
    }

    /**
     * Allow out of order migrations.
     * (default: false)
     */
    var isAllowOutOfOrder = false
        set

    /**
     * Set allow out of order migration property from String value.
     *
     * @param allowOutOfOrder Allow out of order as String.
     */
    fun setIsAllowOutOfOrderFromString(allowOutOfOrder: String) {
        this.isAllowOutOfOrder = allowOutOfOrder.toBoolean()
    }

    /**
     * MigrationConfig initialization.
     */
    init {
        val scriptsEncodingP = System.getProperty(MigrationProperty.SCRIPTS_ENCODING.prefix)
        if (null != scriptsEncodingP && scriptsEncodingP.trim { it <= ' ' }.length != 0)
            this.encoding = scriptsEncodingP

        val targetVersionP = System.getProperty(MigrationProperty.TARGET_VERSION.prefix)
        if (null != targetVersionP && targetVersionP.trim { it <= ' ' }.length != 0)
            setTargetFromString(targetVersionP)

        val locationsProp = System.getProperty(MigrationProperty.SCRIPTS_LOCATIONS.prefix)
        if (locationsProp != null && locationsProp.trim { it <= ' ' }.length != 0) {
            scriptsLocations = StringUtils.tokenizeToStringArray(locationsProp, ",")
        }

        val allowOutOfOrderProp = System.getProperty(MigrationProperty.ALLOW_OUTOFORDER.prefix)
        if (allowOutOfOrderProp != null && allowOutOfOrderProp.trim { it <= ' ' }.length != 0) {
            setIsAllowOutOfOrderFromString(allowOutOfOrderProp)
        }
    }

}
