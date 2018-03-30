/**
 * File     : MigrationVersionSpec.kt
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
package com.hhandoko.cassandra.migration.internal.info

import com.hhandoko.cassandra.migration.api.CassandraMigrationException
import com.hhandoko.cassandra.migration.api.MigrationVersion
import io.kotlintest.specs.FreeSpec

/**
 * MigrationVersionSpec unit tests.
 */
class MigrationVersionSpec : FreeSpec() {

    init {

        "MigrationVersion comparator" - {

            "for SemVer version comparison" - {

                val v1 = MigrationVersion.fromVersion("1")
                val v10 = MigrationVersion.fromVersion("1.0")
                val v11 = MigrationVersion.fromVersion("1.1")
                val v1100 = MigrationVersion.fromVersion("1.1.0.0")
                val v1101 = MigrationVersion.fromVersion("1.1.0.1")
                val v2 = MigrationVersion.fromVersion("2")

                "should compare v1 == v1.0" { v1 shouldEqual v10 }
                "should compare v1.0 == v1" { v10 shouldEqual v1 }

                "should compare v1.1 < v1" { (v1 < v11) shouldBe true }
                "should compare v1 > v1.1" { (v11 > v1) shouldBe true }

                "should compare v1.1 == v1.1.0.0" { v11 shouldEqual v1100 }
                "should compare v1.1.0.0 == v1.1" { v1100 shouldEqual v11 }

                "should compare v1.1 < v1.1.0.1" { (v11 < v1101) shouldBe true }
                "should compare v1.1.0.1 > v1.1" { (v1101 > v11) shouldBe true }

                "should compare v1.1.0.1 < v2" { (v1101 < v2) shouldBe true }
                "should compare v2 > v1.1.0.1" { (v2 > v1101) shouldBe true }

                "should compare v2 < LATEST" { (v2 < MigrationVersion.LATEST) shouldBe true }
                "should compare LATEST > v2" { (MigrationVersion.LATEST > v2) shouldBe true }

            }

            "timestamp version comparison" - {

                val v201004171859 = MigrationVersion.fromVersion("201004171859")
                val v201004180000 = MigrationVersion.fromVersion("201004180000")

                "should compare v201004171859 < v201004180000" { (v201004171859 < v201004180000) shouldBe true }
                "should compare v201004180000 > v201004171859" { (v201004180000 > v201004171859) shouldBe true }

                "should compare v201004180000 < LATEST" { (v201004180000 < MigrationVersion.LATEST) shouldBe true }
                "should compare LATEST > v201004180000" { (MigrationVersion.LATEST > v201004180000) shouldBe true }

            }

        }

        "MigrationVersion" - {

            "provided with two SemVer version inputs" - {

                "should compare equal" {
                    val a1 = MigrationVersion.fromVersion("1.2.3.3")
                    val a2 = MigrationVersion.fromVersion("1.2.3.3")
                    a1 shouldEqual a2
                    a1.hashCode() shouldEqual a2.hashCode()
                }

                "should compare patch version differences" {
                    val a1 = MigrationVersion.fromVersion("1.2.13.3")
                    val a2 = MigrationVersion.fromVersion("1.2.3.3")
                    (a1 > a2) shouldBe true
                }

                "should compare build version differences" {
                    val a1 = MigrationVersion.fromVersion("1.2.3.3")
                    val a2 = MigrationVersion.fromVersion("1.2.3")
                    (a1 > a2) shouldBe true
                    (a2 < a1) shouldBe true
                }

                "should compare equals with leading zeroes" {
                    val a1 = MigrationVersion.fromVersion("1.0")
                    val a2 = MigrationVersion.fromVersion("001.0")
                    a1 shouldEqual a2
                    a1.hashCode() shouldEqual a2.hashCode()
                }

                "should compare equals with trailing zeroes" {
                    val a1 = MigrationVersion.fromVersion("1")
                    val a2 = MigrationVersion.fromVersion("1.00")
                    a1 shouldEqual a2
                    a1.hashCode() shouldEqual a2.hashCode()
                }

                "should compare equals version zeroes" {
                    val a1 = MigrationVersion.fromVersion("0.0")
                    val a2 = MigrationVersion.fromVersion("0")
                    a1 shouldEqual a2
                    a1.hashCode() shouldEqual a2.hashCode()
                }

            }

            "provided overflowing Long version input" - {

                val raw = "9999999999999999999999999999999999.8888888231231231231231298797298789132.22"

                "should compare equals" {
                    MigrationVersion.fromVersion(raw).version shouldEqual raw
                }

            }

            "provided invalid constructor version input" - {

                "should throw exception on missing minor version number" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion("1..1.1") }
                }

                "should throw exception on missing patch version number" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion("1.1..1") }
                }

                "should throw exception on dot only character in version input" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion(".") }
                }

                "should throw exception on dot as first character in version input" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion(".1") }
                }

                "should throw exception on trailing dot in version input" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion("1.") }
                }

                "should throw exception on alphabet first character in version input" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion("abc1.0") }
                }

                "should throw exception on dash character in version input" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion("1.2.1-3") }
                }

                "should throw exception on alphanumeric character in version input" {
                    shouldThrow<CassandraMigrationException> { MigrationVersion.fromVersion("1.2.1a") }
                }

            }

            "provided MigrationVersion enumeration values" - {

                "should compare `fromVersion()` factory method" {
                    MigrationVersion.fromVersion(MigrationVersion.LATEST.version) shouldBe MigrationVersion.LATEST
                    MigrationVersion.fromVersion(MigrationVersion.EMPTY.version) shouldBe MigrationVersion.EMPTY
                    MigrationVersion.fromVersion("1.2.3").version shouldBe "1.2.3"
                }

                "should compare equals EMPTY value" { MigrationVersion.EMPTY shouldEqual MigrationVersion.EMPTY }
                "should compare equals LATEST value" { MigrationVersion.LATEST shouldEqual MigrationVersion.LATEST }
                "should compare equals CURRENT value" { MigrationVersion.CURRENT shouldEqual MigrationVersion.CURRENT }

            }

        }

    }

}
