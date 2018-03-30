/**
 * File     : JavaMigrationResolverSpec.kt
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
 *   Derivative - Copyright (c) 2016 - 2018 cassandra-migration Contributors
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
package com.hhandoko.cassandra.migration.internal.resolver.java

import com.hhandoko.cassandra.migration.api.CassandraMigrationException
import com.hhandoko.cassandra.migration.internal.resolver.java.dummy.V2__InterfaceBasedMigration
import com.hhandoko.cassandra.migration.internal.resolver.java.dummy.Version3dot5
import com.hhandoko.cassandra.migration.internal.util.Location
import io.kotlintest.specs.FreeSpec

/**
 * JavaMigrationResolverSpec unit tests.
 */
class JavaMigrationResolverSpec : FreeSpec() {

    /**
     * Create the Java migration resolver given its location.
     *
     * @param location The Java migration class location.
     * @return The Java migration resolver.
     */
    fun createMigrationResolver(location: String): JavaMigrationResolver {
        return JavaMigrationResolver(
                Thread.currentThread().contextClassLoader,
                Location(location)
        )
    }

    init {

        "JavaMigrationResolver" - {

            "provided valid migration classes" - {

                "should resolve migrations" {
                    val resolver = createMigrationResolver("com/hhandoko/cassandra/migration/internal/resolver/java/dummy")
                    val migrations = resolver.resolveMigrations()

                    migrations.size shouldBe 3

                    migrations[0].version.toString() shouldBe "2"
                    migrations[1].version.toString() shouldBe "3.5"
                    migrations[2].version.toString() shouldBe "4"

                    migrations[0].description shouldBe "InterfaceBasedMigration"
                    migrations[1].description shouldBe "Three Dot Five"

                    migrations[0].checksum shouldBe 0
                    migrations[1].checksum shouldBe 35
                }

            }

            "provided migration info as input argument(s)" - {

                "should extract migration info on classes directly inheriting from JavaMigration interface" {
                    val resolver = JavaMigrationResolver(Thread.currentThread().contextClassLoader, null)
                    val migration = resolver.extractMigrationInfo(V2__InterfaceBasedMigration())

                    migration.version.toString() shouldBe "2"
                    migration.description shouldBe "InterfaceBasedMigration"
                    migration.checksum shouldBe 0
                }

                "should extract migration info on classes inheriting JavaMigration through abstract classes" {
                    val resolver = JavaMigrationResolver(Thread.currentThread().contextClassLoader, null)
                    val migration = resolver.extractMigrationInfo(Version3dot5())

                    migration.version.toString() shouldBe "3.5"
                    migration.description shouldBe "Three Dot Five"
                    migration.checksum shouldBe 35
                }
            }

            "provided a migration class with broken functionality" - {

                "should throw exception" {
                    shouldThrow<CassandraMigrationException> {
                        val resolver = createMigrationResolver("com/hhandoko/cassandra/migration/internal/resolver/java/error")
                        resolver.resolveMigrations()
                    }
                }

            }

        }

    }

}
