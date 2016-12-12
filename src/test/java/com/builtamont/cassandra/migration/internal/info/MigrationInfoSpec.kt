/**
 * File     : MigrationInfoSpec.kt
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
package com.builtamont.cassandra.migration.internal.info

import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationImpl
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.should.shouldMatch
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

/**
 * MigrationInfoSpec unit tests.
 */
@RunWith(JUnitPlatform::class)
class MigrationInfoSpec : Spek({

    val version = MigrationVersion.fromVersion("1")
    val description = "test"
    val type = MigrationType.CQL

    /**
     * Creates a new resolved migration.
     *
     * @return The resolved migration.
     */
    fun createResolvedMigration(): ResolvedMigration {
        val resolvedMigration = ResolvedMigrationImpl()
        resolvedMigration.version = version
        resolvedMigration.description = description
        resolvedMigration.type = type
        resolvedMigration.checksum = 456
        return resolvedMigration
    }

    /**
     * Creates a new applied migration.
     *
     * @return The applied migration.
     */
    fun createAppliedMigration(): AppliedMigration {
        return AppliedMigration(version, description, type, null, 123, "testUser", 0, success = true)
    }

    describe("MigrationInfo") {

        it("should be able to validate migrations info") {
            val migrationInfo = MigrationInfoImpl(
                    createResolvedMigration(),
                    createAppliedMigration(),
                    MigrationInfoContext()
            )
            val message = migrationInfo.validate()

            message!! shouldMatch containsSubstring("123")
            message!! shouldMatch containsSubstring("456")
        }

    }

})
