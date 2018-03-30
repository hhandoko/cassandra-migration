/**
 * File     : CassandraMigrationCommandLineKIT.kt
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
package com.hhandoko.cassandra.migration

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Command line-based migration integration test.
 */
class CassandraMigrationCommandLineKIT : BaseKIT() {

    // Flags for command-line with system properties JVM args
    internal var testWithArgsCompleted = false
    internal var testWithArgsSuccess = false

    // Flags for command-line with HOCON config file
    internal var testWithConfCompleted = false
    internal var testWithConfSuccess = false

    init {

        "Command line migration" - {

            /**
             * Watch the process.
             */
            fun watch(process: Process, success: () -> Unit, completed: () -> Unit) {
                Thread(Runnable {
                    val input = BufferedReader(InputStreamReader(process.inputStream))
                    try {
                        input.forEachLine {
                            if (it.contains("Successfully applied 4 migration(s)"))
                                success()
                            println(it)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    completed()
                }).start()
            }

            /**
             * @return True if OS name contains `win` for Windows.
             */
            fun isWindows(): Boolean {
                return System.getProperty("os.name").toLowerCase().contains("win")
            }

            "should run successfully provided system properties arguments" {
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

                watch(process, { testWithArgsSuccess = true }, { testWithArgsCompleted = true })

                while (!testWithArgsCompleted)
                    Thread.sleep(1000L)

                testWithArgsSuccess shouldBe true
            }

            "should run successfully provided HOCON configuration file" {
                // NOTE: Workaround for Travis CI, config file location as passed-in args:
                //       - Typical `mvn test` / `mvn integration-test` / `mvn verify` will use `application.test.conf` config
                //       - In Travis CI, it will use `application.it-test.conf` for this test only
                val file = System.getProperty("it.config.file") ?: "src/test/resources/application.test.conf"
                val shell =
                        """java -jar
                         | -Dconfig.file=${file}
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

                watch(process, { testWithConfSuccess = true }, { testWithConfCompleted = true })

                while (!testWithConfCompleted)
                    Thread.sleep(1000L)

                testWithConfSuccess shouldBe true
            }

        }

    }

}