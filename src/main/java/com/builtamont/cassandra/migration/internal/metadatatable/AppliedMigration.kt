/**
 * File     : AppliedMigration.kt
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
package com.builtamont.cassandra.migration.internal.metadatatable

import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion
import java.util.*

/**
 * A migration applied to the database (maps to a row in the metadata table).
 */
class AppliedMigration : Comparable<AppliedMigration> {

    /**
     * The position of this version amongst all others. (For easy order by sorting)
     */
    var versionRank: Int? = null
        private set

    /**
     * The order in which this migration was applied amongst all others. (For out of order detection)
     */
    var installedRank: Int? = null
        private set

    /**
     * The target version of this migration.
     */
    var version: MigrationVersion? = null
        private set

    /**
     * The description of the migration.
     */
    var description: String? = null
        private set

    /**
     * The type of migration (CQL, JAVA_DRIVER, ...)
     */
    var type: MigrationType? = null
        private set

    /**
     * The name of the script to execute for this migration, relative to its classpath location.
     */
    var script: String? = null
        private set

    /**
     * The checksum of the migration. (Optional)
     */
    var checksum: Int? = null
        private set

    /**
     * The timestamp when this migration was installed.
     */
    var installedOn: Date? = null
        private set

    /**
     * The user that installed this migration.
     */
    var installedBy: String? = null
        private set

    /**
     * The execution time (in millis) of this migration.
     */
    var executionTime: Int = 0
        private set

    /**
     * Flag indicating whether the migration was successful or not.
     */
    var isSuccess: Boolean = false
        private set

    /**
     * Creates a new applied migration. Only called from the RowMapper.
     *
     * @param versionRank The position of this version amongst all others. (For easy order by sorting)
     * @param installedRank The order in which this migration was applied amongst all others. (For out of order detection)
     * @param version The target version of this migration.
     * @param description The description of the migration.
     * @param type The type of migration (INIT, CQL, ...)
     * @param script The name of the script to execute for this migration, relative to its classpath location.
     * @param checksum The checksum of the migration. (Optional)
     * @param installedOn The timestamp when this migration was installed.
     * @param installedBy The user that installed this migration.
     * @param executionTime The execution time (in millis) of this migration.
     * @param success Flag indicating whether the migration was successful or not.
     */
    constructor(
        versionRank: Int,
        installedRank: Int,
        version: MigrationVersion,
        description: String,
        type: MigrationType,
        script: String,
        checksum: Int?,
        installedOn: Date,
        installedBy: String,
        executionTime: Int,
        success: Boolean
    ) {
        this.versionRank = versionRank
        this.installedRank = installedRank
        this.version = version
        this.description = description
        this.type = type
        this.script = script
        this.checksum = checksum
        this.installedOn = installedOn
        this.installedBy = installedBy
        this.executionTime = executionTime
        this.isSuccess = success
    }

    /**
     * Creates a new applied migration.
     *
     * @param version The target version of this migration.
     * @param description The description of the migration.
     * @param type The type of migration (INIT, CQL, ...)
     * @param script The name of the script to execute for this migration, relative to its classpath location.
     * @param checksum The checksum of the migration. (Optional)
     * @param installedBy The user that installed this migration.
     * @param executionTime The execution time (in millis) of this migration.
     * @param success Flag indicating whether the migration was successful or not.
     */
    constructor(
        version: MigrationVersion,
        description: String,
        type: MigrationType,
        script: String?,
        checksum: Int?,
        installedBy: String,
        executionTime: Int,
        success: Boolean
    ) {
        this.version = version
        this.description = abbreviateDescription(description)
        this.type = type
        this.script = abbreviateScript(script)
        this.checksum = checksum
        this.installedBy = installedBy
        this.executionTime = executionTime
        this.isSuccess = success
    }

    /**
     * Abbreviates this description to a length that will fit in the database.
     *
     * @param description The description to process.
     * @return The abbreviated version.
     */
    private fun abbreviateDescription(description: String?): String? {
        return when {
            description == null       -> null
            description.length <= 200 -> description
            else                      -> description.substring(0, 197) + "..."
        }
    }

    /**
     * Abbreviates this script to a length that will fit in the database.
     *
     * @param script The script to process.
     * @return The abbreviated version.
     */
    private fun abbreviateScript(script: String?): String? {
        return when {
            script == null        -> null
            script.length <= 1000 -> script
            else                  -> "..." + script.substring(3, 1000)
        }
    }

    /**
     * @return The computed applied instance hash value.
     */
    override fun hashCode(): Int {
        var result = versionRank ?: 0
        result = 31 * result + (installedRank ?: 0)
        result = 31 * result + version!!.hashCode()
        result = 31 * result + description!!.hashCode()
        result = 31 * result + type!!.hashCode()
        result = 31 * result + script!!.hashCode()
        result = 31 * result + (checksum?.hashCode() ?: 0)
        result = 31 * result + (installedOn?.hashCode() ?: 0)
        result = 31 * result + (installedBy?.hashCode() ?: 0)
        result = 31 * result + executionTime
        result = 31 * result + if (isSuccess) 1 else 0
        return result
    }

    /**
     * @return {@code true} if this applied instance is the same as the given object.
     */
    @SuppressWarnings("SimplifiableIfStatement")
    override fun equals(other: Any?): Boolean {

        /**
         * @return {@code true} if this applied instance is not the same as the given object.
         */
        fun isNotSame(): Boolean {
            return other == null || javaClass != other.javaClass
        }

        /**
         * @return {@code true} if this context instance checksum property is not the same as the given object checksum property.
         */
        fun isNotSameChecksum(that: AppliedMigration): Boolean {
            return if (checksum != null) checksum != that.checksum else that.checksum != null
        }

        /**
         * @return {@code true} if this context instance installed by property is not the same as the given object installed by property.
         */
        fun isNotSameInstalledBy(that: AppliedMigration): Boolean {
            return if (installedBy != null) installedBy != that.installedBy else that.installedBy != null
        }

        /**
         * @return {@code true} if this context instance installed on property is not the same as the given object resolved installed on property.
         */
        fun isNotSameInstalledOn(that: AppliedMigration): Boolean {
            return if (installedOn != null) installedOn != that.installedOn else that.installedOn != null
        }

        val that = other as AppliedMigration? ?: return false

        return when {
            this === other                      -> true
            isNotSame()                         -> false
            executionTime != that.executionTime -> false
            installedRank != that.installedRank -> false
            versionRank != that.versionRank     -> false
            isSuccess != that.isSuccess         -> false
            isNotSameChecksum(that)             -> false
            description != that.description     -> false
            isNotSameInstalledBy(that)          -> false
            isNotSameInstalledOn(that)          -> false
            script != that.script               -> false
            type !== that.type                  -> false
            else                                -> version == that.version
        }
    }


    /**
     * @return {@code true} if this applied instance is comparable to the given object.
     */
    @SuppressWarnings("NullableProblems")
    override fun compareTo(other: AppliedMigration): Int {
        return version!!.compareTo(other.version)
    }

}
