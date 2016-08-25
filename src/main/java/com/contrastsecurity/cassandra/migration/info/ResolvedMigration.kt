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
package com.contrastsecurity.cassandra.migration.info

import com.contrastsecurity.cassandra.migration.api.MigrationType
import com.contrastsecurity.cassandra.migration.api.MigrationVersion
import com.contrastsecurity.cassandra.migration.api.resolver.MigrationExecutor

/**
 * A migration available on the classpath.
 */
class ResolvedMigration {

    /**
     * The target version of this migration.
     */
    var version: MigrationVersion? = null

    /**
     * The description of the migration.
     */
    var description: String? = null

    /**
     * The name of the script to execute for this migration, relative to its classpath location.
     */
    var script: String? = null

    /**
     * The checksum of the migration.
     */
    var checksum: Int? = null

    /**
     * The type of migration (CQL, JAVA_DRIVER)
     */
    var type: MigrationType? = null

    /**
     * The physical location of the migration on disk.
     */
    var physicalLocation: String? = null

    /**
     * The executor to run this migration.
     */
    var executor: MigrationExecutor? = null

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

        if (this === other) return true
        if (isNotSame()) return false

        val migration = other as ResolvedMigration?

        if (if (checksum != null) checksum != migration!!.checksum else migration!!.checksum != null) return false
        if (if (description != null) description != migration.description else migration.description != null) return false
        if (if (physicalLocation != null) physicalLocation != migration.physicalLocation else migration.physicalLocation != null) return false
        if (if (script != null) script != migration.script else migration.script != null) return false
        if (type !== migration.type) return false
        return version == migration.version
    }

    /**
     * @return {@code true} if this migration instance is comparable to the given object.
     */
    @SuppressWarnings("NullableProblems")
    operator fun compareTo(other: ResolvedMigration): Int {
        return version!!.compareTo(other.version)
    }

}
