/**
 * File     : ResolvedMigrationComparator.kt
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

import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import java.util.*

/**
 * Comparator for ResolvedMigration.
 */
class ResolvedMigrationComparator : Comparator<ResolvedMigration> {
    override fun compare(o1: ResolvedMigration, o2: ResolvedMigration): Int {
        val isO1Defined = o1.version != null
        val isO2Defined = o2.version != null

        return when {
            isO1Defined && isO2Defined -> o1.version!!.compareTo(o2.version)
            isO1Defined                -> Int.MIN_VALUE
            isO2Defined                -> Int.MAX_VALUE
            else                       -> o1.description!!.compareTo(o2.description!!)
        }
    }
}
