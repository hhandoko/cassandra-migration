/**
 * File     : CqlScript.kt
 * License  :
 *   Original   - Copyright (c) 2010 - 2016 Boxfuse GmbH
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
package com.builtamont.cassandra.migration.internal.dbsupport

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.internal.util.StringUtils
import com.builtamont.cassandra.migration.internal.util.logging.LogFactory
import com.builtamont.cassandra.migration.internal.util.scanner.Resource
import com.datastax.driver.core.Session
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.util.*

/**
 * CQL script containing a series of statements terminated by a delimiter (eg: ;).
 * Single-line (--) and multi-line (/ ** * /) comments are stripped and ignored.
 */
class CqlScript {

    /**
     * The CQL statements contained in this script.
     */
    val cqlStatements: List<String>

    /**
     * The resource containing the statements.
     */
    val resource: Resource?

    /**
     * Creates a new CQL script from this source.
     *
     * @param cqlScriptSource The cql script as a text block with all placeholders already replaced.
     */
    constructor(cqlScriptSource: String) {
        this.cqlStatements = parse(cqlScriptSource)
        this.resource = null
    }

    /**
     * Creates a new CQL script from this resource.
     *
     * @param cqlScriptResource The resource containing the statements.
     * @param encoding The encoding to use.
     */
    constructor(cqlScriptResource: Resource, encoding: String) {
        val cqlScriptSource = cqlScriptResource.loadAsString(encoding)
        this.cqlStatements = parse(cqlScriptSource)
        this.resource = cqlScriptResource
    }

    /**
     * Executes this script against the database.
     *
     * @param session The Cassandra session connection to use to execute the migration.
     */
    fun execute(session: Session) {
        cqlStatements.forEach {
            LOG.debug("Executing CQL: $it")
            session.execute(it)
        }
    }

    /**
     * Parses this script's source into statements.
     *
     * @param cqlScriptSource The script source to parse.
     * @return The parsed statements.
     */
    private fun parse(cqlScriptSource: String): List<String> {
        return linesToStatements(readLines(StringReader(cqlScriptSource)))
    }

    /**
     * Turns these lines in a series of statements.
     *
     * @param lines The lines to analyse.
     * @return The statements contained in these lines (in order).
    */
    private fun linesToStatements(lines: List<String>): List<String> {
        val statements = ArrayList<String>()

        var nonStandardDelimiter: Delimiter? = null
        var cqlStatementBuilder = CqlStatementBuilder()

        for (lineNumber in 1..lines.size) {
            val line = lines[lineNumber - 1]

            if (cqlStatementBuilder.isEmpty) {
                if (!StringUtils.hasText(line)) {
                    // Skip empty line between statements.
                    continue
                }

                val newDelimiter = cqlStatementBuilder.extractNewDelimiterFromLine(line)
                if (newDelimiter != null) {
                    nonStandardDelimiter = newDelimiter
                    // Skip this line as it was an explicit delimiter change directive outside of any statements.
                    continue
                }

                cqlStatementBuilder.setLineNumber(lineNumber)

                // Start a new statement, marking it with this line number.
                if (nonStandardDelimiter != null) {
                    cqlStatementBuilder.setDelimiter(nonStandardDelimiter)
                }
            }

            cqlStatementBuilder.addLine(line)

            if (cqlStatementBuilder.canDiscard()) {
                cqlStatementBuilder = CqlStatementBuilder()
            } else if (cqlStatementBuilder.isTerminated) {
                val cqlStatement = cqlStatementBuilder.cqlStatement
                statements.add(cqlStatement)
                LOG.debug("Found statement: $cqlStatement")

                cqlStatementBuilder = CqlStatementBuilder()
            }
        }

        // Catch any statements not followed by delimiter.
        if (!cqlStatementBuilder.isEmpty) {
            statements.add(cqlStatementBuilder.cqlStatement)
        }

        return statements
    }

    /**
     * Parses the textual data provided by this reader into a list of lines.
     *
     * @param reader The reader for the textual data.
     * @return The list of lines (in order).
     * @throws IllegalStateException when the textual data parsing failed.
     * @throws CassandraMigrationException when textual data parsing failed.
     */
    private fun readLines(reader: Reader): List<String> {
        try {
            return BufferedReader(reader).lineSequence().toList()
        } catch (e: IOException) {
            val message = if (resource != null) { "${resource.location} (${resource.locationOnDisk})" } else "lines"
            throw CassandraMigrationException("Unable to parse $message", e)
        }
    }

    /**
     * CqlScript companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(CqlScript::class.java)
    }

}
