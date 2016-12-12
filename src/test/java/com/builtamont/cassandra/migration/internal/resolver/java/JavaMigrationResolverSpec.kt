/**
 * File     : JavaMigrationResolverSpec.kt
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
package com.builtamont.cassandra.migration.internal.resolver.java

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.internal.resolver.java.dummy.V2__InterfaceBasedMigration
import com.builtamont.cassandra.migration.internal.resolver.java.dummy.Version3dot5
import com.builtamont.cassandra.migration.internal.util.ScriptsLocation
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
 * JavaMigrationResolverSpec unit tests.
 */
@RunWith(JUnitPlatform::class)
class JavaMigrationResolverSpec : Spek({

    /**
     * Create the Java migration resolver given its location.
     *
     * @param location The Java migration class location.
     * @return The Java migration resolver.
     */
    fun createMigrationResolver(location: String): JavaMigrationResolver {
        return JavaMigrationResolver(
                Thread.currentThread().contextClassLoader,
                ScriptsLocation(location)
        )
    }

    describe("JavaMigrationResolver") {

        context("provided valid migration classes") {

            it("should resolve migrations") {
                val resolver = createMigrationResolver("com/builtamont/cassandra/migration/internal/resolver/java/dummy")
                val migrations = resolver.resolveMigrations()

                migrations.size shouldMatch equalTo(3)

                migrations[0].version.toString() shouldMatch equalTo("2")
                migrations[1].version.toString() shouldMatch equalTo("3.5")
                migrations[2].version.toString() shouldMatch equalTo("4")

                migrations[0].description shouldMatch equalTo("InterfaceBasedMigration")
                migrations[1].description shouldMatch equalTo("Three Dot Five")

                migrations[0].checksum shouldMatch equalTo(0)
                migrations[1].checksum shouldMatch equalTo(35)
            }

        }

        context("provided migration info as input argument(s)") {

            it("should extract migration info on classes directly inheriting from JavaMigration interface") {
                val resolver = JavaMigrationResolver(Thread.currentThread().contextClassLoader, null)
                val migration = resolver.extractMigrationInfo(V2__InterfaceBasedMigration())

                migration.version.toString() shouldMatch equalTo("2")
                migration.description shouldMatch equalTo("InterfaceBasedMigration")
                migration.checksum shouldMatch equalTo(0)
            }

            it("should extract migration info on classes inheriting JavaMigration through abstract classes") {
                val resolver = JavaMigrationResolver(Thread.currentThread().contextClassLoader, null)
                val migration = resolver.extractMigrationInfo(Version3dot5())

                migration.version.toString() shouldMatch equalTo("3.5")
                migration.description shouldMatch equalTo("Three Dot Five")
                migration.checksum shouldMatch equalTo(35)
            }
        }

        context("provided a migration class with broken functionality") {

            it("should throw exception") {
                assertFailsWith<CassandraMigrationException> {
                    val resolver = createMigrationResolver("com/builtamont/cassandra/migration/internal/resolver/java/error")
                    resolver.resolveMigrations()
                }
            }

        }

    }

})
