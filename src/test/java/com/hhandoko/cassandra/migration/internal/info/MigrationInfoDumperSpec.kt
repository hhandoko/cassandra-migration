/**
 * File     : MigrationInfoDumperSpec.kt
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
 *   Derivative - Copyright (c) 2016 - 2017 Citadel Technology Solutions Pte Ltd
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
import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationImpl
import com.builtamont.cassandra.migration.internal.util.StringUtils
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.kotlintest.specs.FreeSpec

/**
 * MigrationInfoDumperSpec unit tests.
 */
class MigrationInfoDumperSpec : FreeSpec() {

    /**
     * Create a new available migration with the given version.
     *
     * @param version The migration version.
     * @return The available migration.
     */
    fun createAvailableMigration(version: String): ResolvedMigration {
        val migration = ResolvedMigrationImpl()
        migration.version = MigrationVersion.fromVersion(version)
        migration.description = "abc very very very very very very very very very very long"
        migration.script = "x"
        migration.type = MigrationType.CQL
        return migration
    }

    /**
     * Creates a MigrationResolver for testing.
     *
     * @param resolvedMigrations The resolved migrations.
     * @return The migration resolver.
     */
    fun createMigrationResolver(vararg resolvedMigrations: ResolvedMigration): MigrationResolver {
        return object : MigrationResolver {
            override fun resolveMigrations(): List<ResolvedMigration> {
                return resolvedMigrations.toList()
            }
        }
    }

    /**
     * Create mocked SchemaVersionDAO for testing.
     *
     * @return The mocked SchemaVersionDAO.
     */
    fun createSchemaVersionDAO(): SchemaVersionDAO {
        return mock {
            on { findAppliedMigrations() } doReturn arrayListOf<AppliedMigration>()
        }
    }

    init {

        "MigrationInfoDumper" - {

            "given no / empty migrations" - {

                val table = MigrationInfoDumper.dumpToAsciiTable(arrayOf<MigrationInfo>())
                val lines = StringUtils.tokenizeToStringArray(table, "\n")

                "should print migrations ASCII table with no data rows" {
                    lines.size shouldBe 5
                    lines.forEach {
                        // NOTE: Not sure if this test is necessary, checks if all lines are justified-aligned
                        it.length shouldBe lines.first().length
                    }
                }

            }

            "given some migrations" - {

                val migrationInfoService = MigrationInfoServiceImpl(
                        createMigrationResolver(createAvailableMigration("1"), createAvailableMigration("2.2014.09.11.55.45613")),
                        createSchemaVersionDAO(),
                        MigrationVersion.LATEST,
                        outOfOrder = false,
                        pendingOrFuture = true
                )
                migrationInfoService.refresh()

                val table = MigrationInfoDumper.dumpToAsciiTable(migrationInfoService.all())
                val lines = StringUtils.tokenizeToStringArray(table, "\n")

                "should print pending migrations ASCII table" {
                    lines.size shouldBe 6
                    lines.forEach {
                        // NOTE: Not sure if this test is necessary, checks if all lines are justified-aligned
                        it.length shouldBe lines.first().length
                    }
                }

            }

        }

    }

}
