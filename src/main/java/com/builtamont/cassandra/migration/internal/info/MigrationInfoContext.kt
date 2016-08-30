/**
 * File     : MigrationInfoContext.kt
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

import com.builtamont.cassandra.migration.api.MigrationVersion

/**
 * The current context of the migrations.
 */
class MigrationInfoContext {

    /**
     * Whether out of order migrations are allowed.
     */
    var outOfOrder: Boolean = false

    /**
     * Whether pending or future migrations are allowed.
     */
    var pendingOrFuture: Boolean = false

    /**
     * The migration target.
     */
    var target: MigrationVersion? = null

    /**
     * The SCHEMA migration version that was applied.
     */
    var schema: MigrationVersion? = null

    /**
     * The BASELINE migration version that was applied.
     */
    var baseline: MigrationVersion? = null

    /**
     * The last resolved migration.
     */
    var lastResolved = MigrationVersion.EMPTY

    /**
     * The last applied migration.
     */
    var lastApplied = MigrationVersion.EMPTY

    /**
     * @return The computed context instance hash value.
     */
    override fun hashCode(): Int {
        var result = if (outOfOrder) 1 else 0
        result = 31 * result + if (pendingOrFuture) 1 else 0
        result = 31 * result + (target?.hashCode() ?: 0)
        result = 31 * result + (schema?.hashCode() ?: 0)
        result = 31 * result + (baseline?.hashCode() ?: 0)
        result = 31 * result + lastResolved.hashCode()
        result = 31 * result + lastApplied.hashCode()
        return result
    }

    /**
     * @return {@code true} if this context instance is the same as the given object.
     */
    @SuppressWarnings("SimplifiableIfStatement")
    override fun equals(other: Any?): Boolean {

        /**
         * @return {@code true} if this context instance is not the same as the given object.
         */
        fun isNotSame(): Boolean {
            return other == null || javaClass != other.javaClass
        }

        /**
         * @return {@code true} if this context instance schema property is not the same as the given object schema property.
         */
        fun isNotSameSchema(that: MigrationInfoContext): Boolean {
            return if (schema != null) schema != that.schema else that.schema != null
        }

        /**
         * @return {@code true} if this context instance baseline property is not the same as the given object baseline property.
         */
        fun isNotSameBaseline(that: MigrationInfoContext): Boolean {
            return if (baseline != null) baseline != that.baseline else that.baseline != null
        }

        val that = other as MigrationInfoContext? ?: return false

        return when {
            this === other                          -> true
            isNotSame()                             -> false
            outOfOrder != that.outOfOrder           -> false
            pendingOrFuture != that.pendingOrFuture -> false
            isNotSameSchema(that)                   -> false
            isNotSameBaseline(that)                 -> false
            lastApplied != that.lastApplied         -> false
            lastResolved != that.lastResolved       -> false
            else                                    -> target == that.target
        }
    }

}
