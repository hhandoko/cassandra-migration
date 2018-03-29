/**
 * File     : CommandLine.kt
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
package com.hhandoko.cassandra.migration

import com.hhandoko.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.hhandoko.cassandra.migration.internal.util.logging.Log
import com.hhandoko.cassandra.migration.internal.util.logging.LogFactory
import com.hhandoko.cassandra.migration.internal.util.logging.console.ConsoleLog
import com.hhandoko.cassandra.migration.internal.util.logging.console.ConsoleLogCreator

/**
 * Cassandra migration command line runner.
 */
object CommandLine {

    /** Debug-level flag */
    val DEBUG_FLAG = "-X"

    /** Output suppression flag */
    val QUIET_FLAG = "-q"

    /** Command to trigger migrate action */
    val MIGRATE = "migrate"

    /** Command to trigger validate action */
    val VALIDATE = "validate"

    /** Command to trigger baseline action */
    val BASELINE = "baseline"

    /**
     * Logging support.
     */
    lateinit private var LOG: Log

    /**
     * Main method body.
     *
     * @param args The command line arguments.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val logLevel = getLogLevel(args)
        initLogging(logLevel)

        val cm = CassandraMigration()
        val ks = KeyspaceConfiguration()
        cm.keyspaceConfig = ks

        val operations = getOperations(args).map(String::toLowerCase)
        when {
            operations.contains(MIGRATE)  -> cm.migrate()
            operations.contains(VALIDATE) -> cm.validate()
            operations.contains(BASELINE) -> cm.baseline()
            else                          -> printUsage()
        }
    }

    /**
     * Initialize logger.
     */
    internal fun initLogging(level: ConsoleLog.Level) {
        LogFactory.setLogCreator(ConsoleLogCreator(level))
        LOG = LogFactory.getLog(CommandLine::class.java)
    }

    /**
     * Get logging level.
     */
    private fun getLogLevel(args: Array<String>): ConsoleLog.Level {
        return when {
            args.contains(DEBUG_FLAG) -> ConsoleLog.Level.DEBUG
            args.contains(QUIET_FLAG) -> ConsoleLog.Level.WARN
            else                      -> ConsoleLog.Level.INFO
        }
    }

    /**
     * Get all applicable operations.
     */
    private fun getOperations(args: Array<String>): List<String> {
        return args.filterNot { it.startsWith("-") }
    }

    /**
     * Print command line runner info.
     */
    private fun printUsage() {
        LOG.info("*********")
        LOG.info("* Usage *")
        LOG.info("*********")
        LOG.info("")
        LOG.info("cassandra-migration [options] command")
        LOG.info("")
        LOG.info("Commands")
        LOG.info("========")
        LOG.info("migrate  : Migrates the database")
        LOG.info("validate : Validates the applied migrations against the available ones")
        LOG.info("baseline : Baselines an existing database, excluding all migrations up to, and including baselineVersion")
        LOG.info("")
        LOG.info("Add ${DEBUG_FLAG} to print debug output")
        LOG.info("Add ${QUIET_FLAG} to suppress all output, except for errors and warnings")
        LOG.info("")
    }

}
