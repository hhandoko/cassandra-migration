/**
 * File     : CompositeMigrationResolverSpec.kt
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
package com.builtamont.cassandra.migration.internal.resolver

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import com.builtamont.cassandra.migration.internal.util.Locations
import io.kotlintest.matchers.have
import io.kotlintest.specs.FreeSpec

/**
 * CompositeMigrationResolverSpec unit tests.
 */
class CompositeMigrationResolverSpec : FreeSpec() {

    /**
     * Creates a resolved migration for testing.
     *
     * @param type The migration type.
     * @param version The resolved migration version.
     * @param description The description.
     * @param script The migration script.
     * @param checksum The migration script checksum.
     * @return Resolved migration.
     */
    fun createResolvedMigration(type: MigrationType, version: String, description: String, script: String, checksum: Int): ResolvedMigration {
        val migration = ResolvedMigrationImpl()
        migration.type = type
        migration.version = MigrationVersion.fromVersion(version)
        migration.description = description
        migration.script = script
        migration.checksum = checksum
        return migration
    }

    init {

        "CompositeMigrationResolver" - {

            "should resolve migrations in multiple locations" {
                val resolver = CompositeMigrationResolver(
                        Thread.currentThread().contextClassLoader,
                        Locations("migration/subdir/dir2", "migration.outoforder", "migration/subdir/dir1"),
                        "UTF-8",
                        timeout = 0
                )
                val migrations = resolver.resolveMigrations()

                migrations.size shouldBe 3

                migrations[0].description shouldBe "First"
                migrations[1].description shouldBe "Late arrival"
                migrations[2].description shouldBe "Add contents table"
            }

            "should collect migrations and eliminate duplicates" {
                val resolver = object : MigrationResolver {
                    override fun resolveMigrations(): List<ResolvedMigration> {
                        return arrayListOf(
                                createResolvedMigration(MigrationType.JAVA_DRIVER, "1", "Description", "Migration1", 123),
                                createResolvedMigration(MigrationType.JAVA_DRIVER, "1", "Description", "Migration1", 123),
                                createResolvedMigration(MigrationType.CQL, "2", "Description2", "Migration2", 1234)
                        )
                    }
                }
                val resolvers = arrayListOf(resolver)
                val migrations = CompositeMigrationResolver.collectMigrations(resolvers)

                migrations.size shouldBe 2
            }

            "should check for incompatibilities provided no conflict" {
                val migrations = arrayListOf(
                        createResolvedMigration(MigrationType.JAVA_DRIVER, "1", "Description", "Migration1", 123),
                        createResolvedMigration(MigrationType.CQL, "2", "Description2", "Migration2", 1234)
                )

                CompositeMigrationResolver.checkForIncompatibilities(migrations)
            }

            "should check for incompatibilities error message in conflicted migrations" {
                val migration1 = createResolvedMigration(MigrationType.CQL, "1", "First", "V1__First.cql", 123)
                migration1.physicalLocation = "target/test-classes/migration/validate/V1__First.cql"
                val migration2 = createResolvedMigration(MigrationType.JAVA_DRIVER, "1", "Description", "Migration1", 123)
                migration2.physicalLocation = "Migration1"

                val migrations = arrayListOf(migration1, migration2)

                try {
                    CompositeMigrationResolver.checkForIncompatibilities(migrations)
                } catch (e: CassandraMigrationException) {
                    e.message!! should have substring "target/test-classes/migration/validate/V1__First.cql"
                    e.message!! should have substring "Migration1"
                }
            }

        }

    }

}
