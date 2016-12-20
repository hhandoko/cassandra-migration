package com.builtamont.cassandra.migration

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Command line-based migration integration test.
 */
class CommandLineMigrationKIT : BaseKIT() {

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
                         | -Dcassandra.migration.cluster.contactpoints=${BaseIT.CASSANDRA_CONTACT_POINT}
                         | -Dcassandra.migration.cluster.port=${BaseIT.CASSANDRA_PORT}
                         | -Dcassandra.migration.cluster.username=${BaseIT.CASSANDRA_USERNAME}
                         | -Dcassandra.migration.cluster.password=${BaseIT.CASSANDRA_PASSWORD}
                         | -Dcassandra.migration.keyspace.name=${BaseIT.CASSANDRA_KEYSPACE}
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