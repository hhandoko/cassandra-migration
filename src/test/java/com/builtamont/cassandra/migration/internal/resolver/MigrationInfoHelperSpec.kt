/**
 * File     : MigrationInfoHelperSpec.kt
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
package com.builtamont.cassandra.migration.internal.resolver

import com.builtamont.cassandra.migration.api.CassandraMigrationException
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
 * MigrationInfoHelperSpec unit tests.
 */
@RunWith(JUnitPlatform::class)
class MigrationInfoHelperSpec : Spek({

    describe("MigrationInfoHelper") {

        context("provided valid helper object") {

            it("should extract schema version and description with defaults config") {
                val info = MigrationInfoHelper.extractVersionAndDescription("V9_4__EmailDev.cql", "V", "__", ".cql")
                info.left.version shouldMatch equalTo("9.4")
                info.right shouldMatch equalTo("EmailDev")
            }

            it("should extract schema version and description with spaces in description") {
                val info = MigrationInfoHelper.extractVersionAndDescription("V9_4__Email_Dev.cql", "V", "__", ".cql")
                info.left.version shouldMatch equalTo("9.4")
                info.right shouldMatch equalTo("Email Dev")
            }

            it("should extract schema version and description with custom separator") {
                val info = MigrationInfoHelper.extractVersionAndDescription("V9_4-EmailDev.cql", "V", "-", ".cql")
                info.left.version shouldMatch equalTo("9.4")
                info.right shouldMatch equalTo("EmailDev")
            }

            it("should extract schema version and description with custom prefix") {
                val info = MigrationInfoHelper.extractVersionAndDescription("V_9_4__EmailDev.cql", "V_", "__", ".cql")
                info.left.version shouldMatch equalTo("9.4")
                info.right shouldMatch equalTo("EmailDev")
            }

            it("should extract schema version and description with custom suffix") {
                val info = MigrationInfoHelper.extractVersionAndDescription("V9_4__EmailDev", "V", "__", "")
                info.left.version shouldMatch equalTo("9.4")
                info.right shouldMatch equalTo("EmailDev")
            }

            it("should extract schema version and description with leading zero in version") {
                val info = MigrationInfoHelper.extractVersionAndDescription("V009_4__EmailDev.cql", "V", "__", ".cql")
                info.left.version shouldMatch equalTo("009.4")
                info.right shouldMatch equalTo("EmailDev")
            }

        }

        context("provided invalid helper object") {

            it("should throw exception with missing description") {
                assertFailsWith<CassandraMigrationException> {
                    MigrationInfoHelper.extractVersionAndDescription("9_4", "", "__", "")
                }
            }

            it("should throw exception with leading underscore") {
                assertFailsWith<CassandraMigrationException> {
                    MigrationInfoHelper.extractVersionAndDescription("_9_4__Description", "", "__", "")
                }
            }

            it("should throw exception with leading underscore after prefix") {
                assertFailsWith<CassandraMigrationException> {
                    MigrationInfoHelper.extractVersionAndDescription("V_9_4__Description", "V", "__", "")
                }
            }

        }

    }

})
