/**
 * File     : MigrationVersionSpec.kt
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

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.lessThan
import com.natpryce.hamkrest.should.shouldMatch
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

/**
 * MigrationVersionSpec unit tests.
 */
@RunWith(JUnitPlatform::class)
class MigrationVersionSpec : Spek({

    describe("MigrationVersion comparator") {

        context("SemVer version comparison") {

            val v1 = MigrationVersion.fromVersion("1")
            val v10 = MigrationVersion.fromVersion("1.0")
            val v11 = MigrationVersion.fromVersion("1.1")
            val v1100 = MigrationVersion.fromVersion("1.1.0.0")
            val v1101 = MigrationVersion.fromVersion("1.1.0.1")
            val v2 = MigrationVersion.fromVersion("2")

            it("should compare v1 == v1.0") { v1 shouldMatch equalTo(v10) }
            it("should compare v1.0 == v1") { v10 shouldMatch equalTo(v1) }

            it("should compare v1.1 < v1") { v1 shouldMatch lessThan(v11) }
            it("should compare v1 > v1.1") { v11 shouldMatch greaterThan(v1) }

            it("should compare v1.1 == v1.1.0.0") { v11 shouldMatch equalTo(v1100) }
            it("should compare v1.1.0.0 == v1.1") { v1100 shouldMatch equalTo(v11) }

            it("should compare v1.1 < v1.1.0.1") { v11 shouldMatch lessThan(v1101) }
            it("should compare v1.1.0.1 > v1.1") { v1101 shouldMatch greaterThan(v11) }

            it("should compare v1.1.0.1 < v2") { v1101 shouldMatch lessThan(v2) }
            it("should compare v2 > v1.1.0.1") { v2 shouldMatch greaterThan(v1101) }

            it("should compare v2 < LATEST") { v2 shouldMatch lessThan(MigrationVersion.LATEST) }
            it("should compare LATEST > v2") { MigrationVersion.LATEST shouldMatch greaterThan(v2) }

        }

        context("timestamp version comparison") {

            val v201004171859 = MigrationVersion.fromVersion("201004171859")
            val v201004180000 = MigrationVersion.fromVersion("201004180000")

            it("should compare v201004171859 < v201004180000") { v201004171859 shouldMatch lessThan(v201004180000) }
            it("should compare v201004180000 > v201004171859") { v201004180000 shouldMatch greaterThan(v201004171859) }

            it("should compare v201004180000 < LATEST") { v201004180000 shouldMatch lessThan(MigrationVersion.LATEST) }
            it("should compare LATEST > v201004180000") { MigrationVersion.LATEST shouldMatch greaterThan(v201004180000) }

        }

    }

    describe("MigrationVersion") {

        context("provided with two SemVer version inputs") {

            it("should compare equal") {
                val a1 = MigrationVersion.fromVersion("1.2.3.3")
                val a2 = MigrationVersion.fromVersion("1.2.3.3")
                a1 shouldMatch equalTo(a2)
                a1.hashCode() shouldMatch equalTo(a2.hashCode())
            }

            it("should compare patch version differences") {
                val a1 = MigrationVersion.fromVersion("1.2.13.3")
                val a2 = MigrationVersion.fromVersion("1.2.3.3")
                a1 shouldMatch greaterThan(a2)
            }

            it("should compare build version differences") {
                val a1 = MigrationVersion.fromVersion("1.2.3.3")
                val a2 = MigrationVersion.fromVersion("1.2.3")
                a1 shouldMatch greaterThan(a2)
                a2 shouldMatch lessThan(a1)
            }

            it("should compare equals with leading zeroes") {
                val a1 = MigrationVersion.fromVersion("1.0")
                val a2 = MigrationVersion.fromVersion("001.0")
                a1 shouldMatch equalTo(a2)
                a1.hashCode() shouldMatch equalTo(a2.hashCode())
            }

            it("should compare equals with trailing zeroes") {
                val a1 = MigrationVersion.fromVersion("1")
                val a2 = MigrationVersion.fromVersion("1.00")
                a1 shouldMatch equalTo(a2)
                a1.hashCode() shouldMatch equalTo(a2.hashCode())
            }

            it("should compare equals version zeroes") {
                val a1 = MigrationVersion.fromVersion("0.0")
                val a2 = MigrationVersion.fromVersion("0")
                a1 shouldMatch equalTo(a2)
                a1.hashCode() shouldMatch equalTo(a2.hashCode())
            }

        }

        context("provided overflowing Long version input") {

            val raw = "9999999999999999999999999999999999.8888888231231231231231298797298789132.22"

            it("should compare equals") {
                MigrationVersion.fromVersion(raw).version shouldMatch equalTo(raw)
            }

        }

        context("provided invalid constructor version input") {

            it("should throw exception on missing minor version number") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion("1..1.1") }
            }

            it("should throw exception on missing patch version number") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion("1.1..1") }
            }

            it("should throw exception on dot only character in version input") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion(".") }
            }

            it("should throw exception on dot as first character in version input") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion(".1") }
            }

            it("should throw exception on trailing dot in version input") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion("1.") }
            }

            it("should throw exception on alphabet first character in version input") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion("abc1.0") }
            }

            it("should throw exception on dash character in version input") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion("1.2.1-3") }
            }

            it("should throw exception on alphanumeric character in version input") {
                assertFailsWith<CassandraMigrationException> { MigrationVersion.fromVersion("1.2.1a") }
            }

        }

        context("provided MigrationVersion enumeration values") {

            it("should compare `fromVersion()` factory method") {
                MigrationVersion.fromVersion(MigrationVersion.LATEST.version) shouldMatch equalTo(MigrationVersion.LATEST)
                MigrationVersion.fromVersion(MigrationVersion.EMPTY.version) shouldMatch equalTo(MigrationVersion.EMPTY)
                MigrationVersion.fromVersion("1.2.3").version shouldMatch equalTo("1.2.3")
            }

            it("should compare equals EMPTY value") { MigrationVersion.EMPTY shouldMatch equalTo(MigrationVersion.EMPTY) }
            it("should compare equals LATEST value") { MigrationVersion.LATEST shouldMatch equalTo(MigrationVersion.LATEST) }
            it("should compare equals CURRENT value") { MigrationVersion.CURRENT shouldMatch equalTo(MigrationVersion.CURRENT) }

        }

    }

})
