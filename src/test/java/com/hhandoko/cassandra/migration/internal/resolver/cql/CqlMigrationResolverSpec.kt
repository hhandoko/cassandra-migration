/**
 * File     : CqlMigrationResolverSpec.kt
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
package com.hhandoko.cassandra.migration.internal.resolver.cql

import com.hhandoko.cassandra.migration.api.CassandraMigrationException
import com.hhandoko.cassandra.migration.internal.util.Location
import com.hhandoko.cassandra.migration.internal.util.scanner.classpath.ClassPathResource
import com.hhandoko.cassandra.migration.internal.util.scanner.filesystem.FileSystemResource
import io.kotlintest.specs.FreeSpec

/**
 * CqlMigrationResolverSpec unit tests.
 */
class CqlMigrationResolverSpec : FreeSpec() {

    /**
     * Create the CQL migration resolver given its location.
     *
     * @param location The scripts location.
     * @return The CQL migration resolver.
     */
    fun createMigrationResolver(location: String): CqlMigrationResolver {
        return CqlMigrationResolver(
                Thread.currentThread().contextClassLoader,
                Location(location),
                "UTF-8",
                timeout = 0
        )
    }

    init {

        "CqlMigrationResolver" - {

            "provided valid migration folders" - {

                "should extract script name" {
                    val resolver = createMigrationResolver("db/migration")
                    val resource = ClassPathResource("db/migration/db_0__init.cql", Thread.currentThread().contextClassLoader)
                    resolver.extractScriptName(resource) shouldBe "db_0__init.cql"
                }

                "should extract script name root location" {
                    val resolver = createMigrationResolver("")
                    val resource = ClassPathResource("db_0__init.cql", Thread.currentThread().contextClassLoader)
                    resolver.extractScriptName(resource) shouldBe "db_0__init.cql"
                }

                "should extract script name from filesystem prefix" {
                    val resolver = createMigrationResolver("filesystem:/some/dir")
                    val resource = FileSystemResource("/some/dir/V3.171__patch.cql")
                    resolver.extractScriptName(resource) shouldBe "V3.171__patch.cql"
                }

            }

            "provided valid migration with sub-folders" - {

                "should resolve version number and script names in sub-folders" {
                    val resolver = createMigrationResolver("migration/subdir")
                    val migrations = resolver.resolveMigrations()

                    migrations.size shouldBe 3

                    migrations[0].version.toString() shouldBe "1"
                    migrations[1].version.toString() shouldBe "1.1"
                    migrations[2].version.toString() shouldBe "2.0"

                    migrations[0].script shouldBe "dir1/V1__First.cql"
                    migrations[1].script shouldBe "V1_1__Populate_table.cql"
                    migrations[2].script shouldBe "dir2/V2_0__Add_contents_table.cql"
                }
            }

            "provided non-existing migration folder" - {

                "should throw an exception" {
                    shouldThrow<CassandraMigrationException> {
                        val resolver = createMigrationResolver("non/existing")
                        resolver.resolveMigrations()
                    }
                }

            }

        }

    }

}
