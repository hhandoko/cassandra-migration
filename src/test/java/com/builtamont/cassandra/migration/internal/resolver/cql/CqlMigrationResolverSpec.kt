/**
 * File     : CqlMigrationResolverSpec.kt
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
package com.builtamont.cassandra.migration.internal.resolver.cql

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.internal.util.ScriptsLocation
import com.builtamont.cassandra.migration.internal.util.scanner.classpath.ClassPathResource
import com.builtamont.cassandra.migration.internal.util.scanner.filesystem.FileSystemResource
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

/**
 * CqlMigrationResolverSpec unit tests.
 */
@RunWith(JUnitPlatform::class)
class CqlMigrationResolverSpec : Spek({

    /**
     * Create the CQL migration resolver given its location.
     *
     * @param location The scripts location.
     * @return The CQL migration resolver.
     */
    fun createMigrationResolver(location: String): CqlMigrationResolver {
        return CqlMigrationResolver(
                Thread.currentThread().contextClassLoader,
                ScriptsLocation(location),
                "UTF-8"
        )
    }

    describe("CqlMigrationResolver") {

        context("provided valid migration folders") {

            it("should extract script name") {
                val resolver = createMigrationResolver("db/migration")
                val resource = ClassPathResource("db/migration/db_0__init.cql", Thread.currentThread().contextClassLoader)
                resolver.extractScriptName(resource) shouldMatch equalTo("db_0__init.cql")
            }

            it("should extract script name root location") {
                val resolver = createMigrationResolver("")
                val resource = ClassPathResource("db_0__init.cql", Thread.currentThread().contextClassLoader)
                resolver.extractScriptName(resource) shouldMatch equalTo("db_0__init.cql")
            }

            it("should extract script name from filesystem prefix") {
                val resolver = createMigrationResolver("filesystem:/some/dir")
                val resource = FileSystemResource("/some/dir/V3.171__patch.cql")
                resolver.extractScriptName(resource) shouldMatch equalTo("V3.171__patch.cql")
            }

        }

        context("provided valid migration with sub-folders") {

            it("should resolve version number and script names in sub-folders") {
                val resolver = createMigrationResolver("migration/subdir")
                val migrations = resolver.resolveMigrations()

                migrations.size shouldMatch equalTo(3)

                migrations[0].version.toString() shouldMatch equalTo("1")
                migrations[1].version.toString() shouldMatch equalTo("1.1")
                migrations[2].version.toString() shouldMatch equalTo("2.0")

                migrations[0].script shouldMatch equalTo("dir1/V1__First.cql")
                migrations[1].script shouldMatch equalTo("V1_1__Populate_table.cql")
                migrations[2].script shouldMatch equalTo("dir2/V2_0__Add_contents_table.cql")
            }
        }

        context("provided non-existing migration folder") {

            it("should throw an exception") {
                assertFailsWith<CassandraMigrationException> {
                    val resolver = createMigrationResolver("non/existing")
                    resolver.resolveMigrations()
                }
            }

        }

    }

})
