/**
 * File     : CommandLine.kt
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

import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.builtamont.cassandra.migration.internal.util.logging.Log
import com.builtamont.cassandra.migration.internal.util.logging.LogFactory
import com.builtamont.cassandra.migration.internal.util.logging.console.ConsoleLog
import com.builtamont.cassandra.migration.internal.util.logging.console.ConsoleLogCreator

/**
 * Cassandra migration command line runner.
 */
object CommandLine {

    /**
     * Command to trigger migrate action.
     */
    val MIGRATE = "migrate"

    /**
     * Command to trigger validate action.
     */
    val VALIDATE = "validate"

    /**
     * Command to trigger baseline action.
     */
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
    @JvmStatic fun main(args: Array<String>) {
        val logLevel = getLogLevel(args)
        initLogging(logLevel)

        val operations = determineOperations(args)
        if (operations.isEmpty()) {
            printUsage()
            return
        }

        val operation = operations[0]

        val cm = CassandraMigration()
        val ks = KeyspaceConfiguration()
        cm.keyspaceConfig = ks
        if (MIGRATE.equals(operation, ignoreCase = true)) {
            cm.migrate()
        } else if (VALIDATE.equals(operation, ignoreCase = true)) {
            cm.validate()
        } else if (BASELINE.equals(operation, ignoreCase = true)) {
            cm.baseline()
        }
    }

    /**
     * Get a list of applicable operations.
     */
    private fun determineOperations(args: Array<String>): List<String> {
        return args.filterNot { it.startsWith("-") }
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
        for (arg in args) {
            if ("-X" == arg) {
                return ConsoleLog.Level.DEBUG
            }
            if ("-q" == arg) {
                return ConsoleLog.Level.WARN
            }
        }
        return ConsoleLog.Level.INFO
    }

    /**
     * Print command line runner info.
     */
    private fun printUsage() {
        LOG.info("********")
        LOG.info("* Usage")
        LOG.info("********")
        LOG.info("")
        LOG.info("cassandra-migration [options] command")
        LOG.info("")
        LOG.info("Commands")
        LOG.info("========")
        LOG.info("migrate  : Migrates the database")
        LOG.info("validate : Validates the applied migrations against the available ones")
        LOG.info("baseline : Baselines an existing database, excluding all migrations up to, and including baselineVersion")
        LOG.info("")
        LOG.info("Add -X to print debug output")
        LOG.info("Add -q to suppress all output, except for errors and warnings")
        LOG.info("")
    }

}
