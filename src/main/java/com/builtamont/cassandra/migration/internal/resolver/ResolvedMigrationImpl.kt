/**
 * File     : ResolvedMigrationImpl.kt
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

import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.resolver.MigrationExecutor
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration

/**
 * A migration available on the classpath.
 */
class ResolvedMigrationImpl : ResolvedMigration {

    /**
     * The target version of this migration.
     */
    override var version: MigrationVersion? = null
        get set

    /**
     * The description of the migration.
     */
    override var description: String? = null
        get set

    /**
     * The name of the script to execute for this migration, relative to its classpath location.
     */
    override var script: String? = null
        get set

    /**
     * The checksum of the migration.
     */
    override var checksum: Int? = null
        get set

    /**
     * The type of migration (CQL, JAVA_DRIVER)
     */
    override var type: MigrationType? = null
        get set

    /**
     * The physical location of the migration on disk.
     */
    override var physicalLocation: String? = null
        get set

    /**
     * The executor to run this migration.
     */
    override var executor: MigrationExecutor? = null
        get set

    /**
     * @return The computed migration instance hash value.
     */
    override fun hashCode(): Int {
        var result = version?.hashCode() ?: 0
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (script?.hashCode() ?: 0)
        result = 31 * result + (checksum?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (physicalLocation?.hashCode() ?: 0)
        return result
    }

    /**
     * @return {@code true} if this migration instance is the same as the given object.
     */
    @SuppressWarnings("SimplifiableIfStatement")
    override fun equals(other: Any?): Boolean {

        /**
         * @return {@code true} if this version instance is not the same as the given object.
         */
        fun isNotSame(): Boolean {
            return other == null || javaClass != other.javaClass
        }

        /**
         * @return {@code true} if this context instance checksum property is not the same as the given object checksum property.
         */
        fun isNotSameChecksum(that: ResolvedMigrationImpl): Boolean {
            return if (checksum != null) checksum != that.checksum else that.checksum != null
        }

        /**
         * @return {@code true} if this context instance description property is not the same as the given object description property.
         */
        fun isNotSameDescription(that: ResolvedMigrationImpl): Boolean {
            return if (description != null) description != that.description else that.description != null
        }

        /**
         * @return {@code true} if this context instance physical location property is not the same as the given object physical location property.
         */
        fun isNotSamePhysicalLocation(that: ResolvedMigrationImpl): Boolean {
            return if (physicalLocation != null) physicalLocation != that.physicalLocation else that.physicalLocation != null
        }

        /**
         * @return {@code true} if this context instance script property is not the same as the given object script property.
         */
        fun isNotSameScript(that: ResolvedMigrationImpl): Boolean {
            return if (script != null) script != that.script else that.script != null
        }

        val that = other as ResolvedMigrationImpl? ?: return false

        return when {
            this === other                  -> true
            isNotSame()                     -> false
            isNotSameChecksum(that)         -> false
            isNotSameDescription(that)      -> false
            isNotSamePhysicalLocation(that) -> false
            isNotSameScript(that)           -> false
            type !== that.type              -> false
            else                            -> version == that.version
        }
    }

    /**
     * @return {@code true} if this migration instance is comparable to the given object.
     */
    @SuppressWarnings("NullableProblems")
    operator fun compareTo(other: ResolvedMigrationImpl): Int {
        return version!!.compareTo(other.version)
    }

}