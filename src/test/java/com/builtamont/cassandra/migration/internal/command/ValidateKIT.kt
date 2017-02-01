/**
 * File     : ValidateKIT.kt
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
package com.builtamont.cassandra.migration.internal.command

import com.builtamont.cassandra.migration.BaseKIT
import com.builtamont.cassandra.migration.CassandraMigration
import com.builtamont.cassandra.migration.api.CassandraMigrationException

/**
 * Validate command unit tests.
 */
class ValidateKIT : BaseKIT() {

    init {

        "Validate command API" - {

            "should throw exception when invalid migration scripts are provided" - {

                "for session and keyspace setup via configuration" {
                    // apply migration scripts
                    val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                    var cm = CassandraMigration()
                    cm.locations = scriptsLocations
                    cm.keyspaceConfig = getKeyspace()
                    cm.migrate()

                    val infoService = cm.info()
                    val validationError = infoService.validate()
                    validationError shouldBe null

                    cm = CassandraMigration()
                    cm.locations = scriptsLocations
                    cm.keyspaceConfig = getKeyspace()

                    cm.validate()

                    cm = CassandraMigration()
                    cm.locations = arrayOf("migration/integ/java")
                    cm.keyspaceConfig = getKeyspace()

                    shouldThrow<CassandraMigrationException> {
                        cm.validate()
                    }
                }

                "for external session, but keyspace setup via configuration" {
                    // apply migration scripts
                    val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                    val session = getSession()
                    var cm = CassandraMigration()
                    cm.locations = scriptsLocations
                    cm.keyspaceConfig = getKeyspace()
                    cm.migrate(session)

                    val infoService = cm.info(session)
                    val validationError = infoService.validate()
                    validationError shouldBe null

                    cm = CassandraMigration()
                    cm.locations = scriptsLocations
                    cm.keyspaceConfig = getKeyspace()

                    cm.validate(session)

                    cm = CassandraMigration()
                    cm.locations = arrayOf("migration/integ/java")
                    cm.keyspaceConfig = getKeyspace()

                    shouldThrow<CassandraMigrationException> {
                        cm.validate(session)
                    }

                    session.isClosed shouldBe false
                }

                "for external session and defaulted keyspace" {
                    // apply migration scripts
                    val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                    val session = getSession()
                    var cm = CassandraMigration()
                    cm.locations = scriptsLocations
                    cm.migrate(session)

                    val infoService = cm.info(session)
                    val validationError = infoService.validate()
                    validationError shouldBe null

                    cm = CassandraMigration()
                    cm.locations = scriptsLocations

                    cm.validate(session)

                    cm = CassandraMigration()
                    cm.locations = arrayOf("migration/integ/java")

                    shouldThrow<CassandraMigrationException> {
                        cm.validate(session)
                    }

                    session.isClosed shouldBe false
                }

            }

        }

    }

}
