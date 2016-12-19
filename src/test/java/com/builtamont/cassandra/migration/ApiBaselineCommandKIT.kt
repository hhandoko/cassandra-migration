/**
 * File     : ApiBaselineCommandKIT.kt
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
package com.builtamont.cassandra.migration

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO

/**
 * Validate command unit tests.
 */
class ApiBaselineCommandKIT : BaseKIT() {

    init {

        "Baseline command API" - {

            "should mark at first migration script" - {

                "with default table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.baseline()

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession()
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for external session and defaulted keyspace" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession(getKeyspace())
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker.version shouldBe MigrationVersion.fromVersion("1")
                    }

                }

                "with user-defined table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.baseline()

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), cm.tablePrefix + MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession()
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), cm.tablePrefix + MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker.version shouldBe MigrationVersion.fromVersion("1")
                    }

                    "for external session and defaulted keyspace" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession(getKeyspace())
                        val cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.tablePrefix = "test1_"
                        cm.baseline(session)

                        val schemaVersionDAO = SchemaVersionDAO(getSession(), getKeyspace(), cm.tablePrefix + MigrationVersion.CURRENT.table)
                        val baselineMarker = schemaVersionDAO.baselineMarker

                        baselineMarker.version shouldBe MigrationVersion.fromVersion("1")
                    }

                }

            }

            "should throw exception when baselining after successful migration" - {

                "with default table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.migrate()

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()

                        shouldThrow<CassandraMigrationException> { cm.baseline() }
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                    "for external session and defaulted keyspace" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                }

                "with user-defined table prefix" - {

                    "for session and keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.migrate()

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"

                        shouldThrow<CassandraMigrationException> { cm.baseline() }
                    }

                    "for external session, but keyspace setup via configuration" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.keyspaceConfig = getKeyspace()
                        cm.tablePrefix = "test1_"

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                    "for external session and defaulted keyspace" {
                        val scriptsLocations = arrayOf("migration/integ", "migration/integ/java")
                        val session = getSession()
                        var cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.tablePrefix = "test1_"
                        cm.migrate(session)

                        cm = CassandraMigration()
                        cm.locations = scriptsLocations
                        cm.tablePrefix = "test1_"

                        shouldThrow<CassandraMigrationException> { cm.baseline(session) }
                    }

                }

            }

        }

    }

}
