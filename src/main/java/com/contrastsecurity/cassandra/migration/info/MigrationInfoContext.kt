/**
 * Copyright 2010-2015 Axel Fontaine
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrastsecurity.cassandra.migration.info

import com.contrastsecurity.cassandra.migration.api.MigrationVersion

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
        result = 31 * result + target!!.hashCode()
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
         * @return {@code true} if this version instance is not the same as the given object.
         */
        fun isNotSame(): Boolean {
            return other == null || javaClass != other.javaClass
        }

        if (this === other) return true
        if (isNotSame()) return false

        val that = other as MigrationInfoContext?

        if (outOfOrder != that!!.outOfOrder) return false
        if (pendingOrFuture != that.pendingOrFuture) return false
        if (if (schema != null) schema != that.schema else that.schema != null) return false
        if (if (baseline != null) baseline != that.baseline else that.baseline != null) return false
        if (lastApplied != that.lastApplied) return false
        if (lastResolved != that.lastResolved) return false
        return target == that.target
    }

}
