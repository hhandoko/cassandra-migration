/**
 * File     : MigrationInfoImpl.kt
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
package com.builtamont.cassandra.migration.internal.info

import com.builtamont.cassandra.migration.api.MigrationInfo
import com.builtamont.cassandra.migration.api.MigrationState
import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration
import java.util.*

/**
 * Default implementation of MigrationInfoService.
 *
 * @param resolvedMigration The resolved migration to aggregate the info from.
 * @param appliedMigration The applied migration to aggregate the info from.
 * @param context The current context.
 */
class MigrationInfoImpl(
    val resolvedMigration: ResolvedMigration?,
    val appliedMigration: AppliedMigration?,
    private val context: MigrationInfoContext
) : MigrationInfo {

    /**
     * The type of migration (CQL, Java, ...)
     */
    override val type: MigrationType
        get() {
            if (appliedMigration != null) {
                return appliedMigration.type!!
            }
            return resolvedMigration?.type!!
        }

    /**
     * The target version of this migration.
     */
    override val checksum: Int?
        get() {
            if (appliedMigration != null) {
                return appliedMigration.checksum
            }
            return resolvedMigration!!.checksum
        }

    /**
     * The schema version after the migration is complete.
     */
    override val version: MigrationVersion
        get() {
            if (appliedMigration != null) {
                return appliedMigration.version!!
            }
            return resolvedMigration!!.version!!
        }

    /**
     * The description of the migration.
     */
    override val description: String
        get() {
            if (appliedMigration != null) {
                return appliedMigration.description!!
            }
            return resolvedMigration!!.description!!
        }

    /**
     * The name of the script to execute for this migration, relative to its classpath or filesystem location.
     */
    override val script: String
        get() {
            if (appliedMigration != null) {
                return appliedMigration.script!!
            }
            return resolvedMigration!!.script!!
        }

    /**
     * The state of the migration (PENDING, SUCCESS, ...)
     */
    override val state: MigrationState
        get() {
            if (appliedMigration == null) {
                if (resolvedMigration!!.version!!.compareTo(context.baseline) < 0) {
                    return MigrationState.BELOW_BASELINE
                }
                if (resolvedMigration.version!!.compareTo(context.target) > 0) {
                    return MigrationState.ABOVE_TARGET
                }
                if (resolvedMigration.version!!.compareTo(context.lastApplied) < 0 && !context.outOfOrder) {
                    return MigrationState.IGNORED
                }
                return MigrationState.PENDING
            }

            if (resolvedMigration == null) {
                if (MigrationType.SCHEMA === appliedMigration.type) {
                    return MigrationState.SUCCESS
                }
                if (MigrationType.BASELINE === appliedMigration.type) {
                    return MigrationState.BASELINE
                }
                if (version.compareTo(context.lastResolved) < 0) {
                    if (appliedMigration.isSuccess) {
                        return MigrationState.MISSING_SUCCESS
                    }
                    return MigrationState.MISSING_FAILED
                }
                if (version.compareTo(context.lastResolved) > 0) {
                    if (appliedMigration.isSuccess) {
                        return MigrationState.FUTURE_SUCCESS
                    }
                    return MigrationState.FUTURE_FAILED
                }
            }

            if (appliedMigration.isSuccess) {
                if (appliedMigration.versionRank == appliedMigration.installedRank) {
                    return MigrationState.SUCCESS
                }
                return MigrationState.OUT_OF_ORDER
            }
            return MigrationState.FAILED
        }

    /**
     * The timestamp when this migration was installed. (Only for applied migrations)
     */
    override val installedOn: Date?
        get() {
            if (appliedMigration != null) {
                return appliedMigration.installedOn
            }
            return null
        }

    /**
     * The execution time (in millis) of this migration. (Only for applied migrations)
     */
    override val executionTime: Int?
        get() {
            if (appliedMigration != null) {
                return appliedMigration.executionTime
            }
            return null
        }

    /**
     * Validates this migrationInfo for consistency.
     *
     * @return The error message, or {@code null} if everything is fine.
     */
    fun validate(): String? {
        if (!context.pendingOrFuture
                && resolvedMigration == null
                && appliedMigration!!.type !== MigrationType.SCHEMA
                && appliedMigration!!.type !== MigrationType.BASELINE) {
            return "Detected applied migration not resolved locally: " + version
        }

        if (!context.pendingOrFuture && MigrationState.PENDING === state || MigrationState.IGNORED === state) {
            return "Detected resolved migration not applied to database: " + version
        }

        if (resolvedMigration != null && appliedMigration != null) {
            if (version.compareTo(context.baseline) > 0) {
                if (resolvedMigration.type !== appliedMigration.type) {
                    return createMismatchMessage("Type", appliedMigration.version!!,
                            appliedMigration.type!!, resolvedMigration.type!!)
                }
                if (!com.builtamont.cassandra.migration.internal.util.ObjectUtils.nullSafeEquals(resolvedMigration.checksum, appliedMigration.checksum)) {
                    return createMismatchMessage("Checksum", appliedMigration.version!!,
                            appliedMigration.checksum!!, resolvedMigration.checksum!!)
                }
                if (resolvedMigration.description != appliedMigration.description) {
                    return createMismatchMessage("Description", appliedMigration.version!!,
                            appliedMigration.description!!, resolvedMigration.description!!)
                }
            }
        }
        return null
    }

    /**
     * Creates a message for a mismatch.
     *
     * @param mismatch The type of mismatch.
     * @param version The offending version.
     * @param applied The applied value.
     * @param resolved The resolved value.
     * @return The message.
     */
    private fun createMismatchMessage(mismatch: String, version: MigrationVersion, applied: Any, resolved: Any): String {
        val message = "Migration $mismatch mismatch for migration $version\n-> Applied to database : $applied\n-> Resolved locally    : $resolved"
        return String.format(message)
    }

    /**
     * @return The computed info instance hash value.
     */
    override fun hashCode(): Int {
        var result = resolvedMigration?.hashCode() ?: 0
        result = 31 * result + (appliedMigration?.hashCode() ?: 0)
        result = 31 * result + context.hashCode()
        return result
    }

    /**
     * @return {@code true} if this info instance is the same as the given object.
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
         * @return {@code true} if this context instance applied migration property is not the same as the given object applied migration property.
         */
        fun isNotSameAppliedMigration(that: MigrationInfoImpl): Boolean {
            return if (appliedMigration != null) appliedMigration != that.appliedMigration else that.appliedMigration != null
        }

        /**
         * @return {@code true} if this context instance resolved migration property is not the same as the given object resolved migration property.
         */
        fun isNotSameResolvedMigration(that: MigrationInfoImpl): Boolean {
            return if (resolvedMigration != null) resolvedMigration != that.resolvedMigration else that.resolvedMigration != null
        }

        val that = other as MigrationInfoImpl? ?: return false

        return when {
            this === other                  -> true
            isNotSame()                     -> false
            isNotSameAppliedMigration(that) -> false
            context != that.context         -> false
            else                            -> !isNotSameResolvedMigration(that) // Note the double negative
        }
    }

    /**
     * @return {@code true} if this info instance is comparable to the given object.
     */
    @SuppressWarnings("NullableProblems")
    override fun compareTo(other: MigrationInfo): Int {
        return version.compareTo(other.version)
    }

}
