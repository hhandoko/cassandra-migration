/**
 * File     : CassandraMigrationCommandLineKIT.kt
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
package com.builtamont.cassandra.migration

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Command line-based migration integration test.
 */
class CassandraMigrationCommandLineKIT : BaseKIT() {

    internal var runCmdTestCompleted = false
    internal var runCmdTestSuccess = false

    init {

        "Command line migration" - {

            /**
             * Watch the process.
             */
            fun watch(process: Process) {
                Thread(Runnable {
                    val input = BufferedReader(InputStreamReader(process.inputStream))
                    try {
                        input.forEachLine {
                            if (it.contains("Successfully applied 4 migration(s)"))
                                runCmdTestSuccess = true
                            println(it)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    runCmdTestCompleted = true
                }).start()
            }

            /**
             * @return True if OS name contains `win` for Windows.
             */
            fun isWindows(): Boolean {
                return System.getProperty("os.name").toLowerCase().contains("win")
            }

            "should run successfully" {
                val shell =
                        """java -jar
                         | -Dcassandra.migration.scripts.locations=filesystem:target/test-classes/migration/integ
                         | -Dcassandra.migration.cluster.contactpoints=${this.CASSANDRA_CONTACT_POINT}
                         | -Dcassandra.migration.cluster.port=${this.CASSANDRA_PORT}
                         | -Dcassandra.migration.cluster.username=${this.CASSANDRA_USERNAME}
                         | -Dcassandra.migration.cluster.password=${this.CASSANDRA_PASSWORD}
                         | -Dcassandra.migration.keyspace.name=${this.CASSANDRA_KEYSPACE}
                         | target/*-jar-with-dependencies.jar
                         | migrate
                        """.trimMargin().replace("\n", "").replace("  ", " ")
                println(shell)
                val builder: ProcessBuilder
                if (isWindows()) {
                    throw IllegalStateException()
                } else {
                    builder = ProcessBuilder("bash", "-c", shell)
                }
                builder.redirectErrorStream(true)
                val process = builder.start()

                watch(process)

                while (!runCmdTestCompleted)
                    Thread.sleep(1000L)

                runCmdTestSuccess shouldBe true
            }

        }

    }

}