/**
 * File     : CqlStatementBuilder.kt
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

import com.builtamont.cassandra.migration.internal.util.StringUtils
import java.util.*

/**
 * Builds a CQL statement, one line at a time.
 */
class CqlStatementBuilder {

    /**
     * The current statement, as it is being built.
     */
    private val statement = StringBuilder()

    /**
     * The initial line number of this statement.
     */
    private var lineNumber: Int = 0

    /**
     * Flag indicating whether the current statement is still empty.
     */
    var isEmpty = true
        private set

    /**
     * Flag indicating whether the current statement is properly terminated.
     */
    var isTerminated: Boolean = false
        private set

    /**
     * Are we currently inside a ' multi-line string literal.
     */
    private var insideQuoteStringLiteral = false

    /**
     * Are we currently inside an alternate multi-line string literal.
     */
    private var insideAlternateQuoteStringLiteral = false

    /**
     * The alternate quote that is expected to close the string literal.
     */
    private var alternateQuote: String? = null

    /**
     * Whether the last processed line ended with a single line -- comment.
     */
    private var lineEndsWithSingleLineComment = false

    /**
     * Are we inside a multi-line / ** * / comment.
     */
    private var insideMultiLineComment = false

    /**
     * Whether a non-comment part of a statement has already been seen.
     */
    private var nonCommentStatementPartSeen = false

    /**
     * The current delimiter to look for to terminate the statement.
     */
    private var delimiter = defaultDelimiter

    /**
     * @return The default delimiter for this database.
     */
    protected val defaultDelimiter: Delimiter
        get() = Delimiter(";", false)

    /**
     * @param lineNumber The initial line number of this statement.
     */
    fun setLineNumber(lineNumber: Int) {
        this.lineNumber = lineNumber
    }

    /**
     * @param delimiter The current delimiter to look for to terminate the statement.
     */
    fun setDelimiter(delimiter: Delimiter) {
        this.delimiter = delimiter
    }

    /**
     * @return The assembled statement, with the delimiter stripped off.
     */
    val cqlStatement: String
        get() = statement.toString()

    /**
     * Analyses this line and extracts the new default delimiter.
     * This method is only called between statements and looks for explicit delimiter change directives.
     *
     * @param line Line to analyze.
     * @return The new delimiter. `null` if it is the same as the current one.
     */
    @SuppressWarnings("UnusedParameters")
    fun extractNewDelimiterFromLine(line: String): Delimiter? {
        return null
    }

    /**
     * Checks whether this line is in fact a directive disguised as a comment.
     *
     * @param line The line to analyse.
     * @return {@code true} if it is a directive that should be processed by the database, {@code false} if not.
     */
    fun isCommentDirective(line: String): Boolean {
        return false
    }

    /**
     * Checks whether this line is just a single-line comment outside a statement or not.
     *
     * @param line The line to analyse.
     * @return {@code true} if it is, {@code false} if not.
     */
    protected fun isSingleLineComment(line: String): Boolean {
        return line.startsWith("--")
    }

    /**
     * Adds this line to the current statement being built.
     *
     * @param line The line to add.
     */
    fun addLine(line: String) {
        if (isEmpty) {
            isEmpty = false
        } else {
            statement.append("\n")
        }

        val lineSimplified = simplifyLine(line)

        applyStateChanges(lineSimplified)
        if (endWithOpenMultilineStringLiteral() || insideMultiLineComment) {
            statement.append(line)
            return
        }

        delimiter = changeDelimiterIfNecessary(lineSimplified, delimiter)

        statement.append(line)

        if (isCommentDirective(lineSimplified)) {
            nonCommentStatementPartSeen = true
        }

        if (!lineEndsWithSingleLineComment && lineTerminatesStatement(lineSimplified, delimiter)) {
            stripDelimiter(statement, delimiter)
            isTerminated = true
        }
    }

    /**
     * Checks whether the statement currently ends with an open multiline string literal.
     *
     * @return {@code true} if it does, {@code false} if it doesn't.
     */
    fun endWithOpenMultilineStringLiteral(): Boolean {
        return insideQuoteStringLiteral || insideAlternateQuoteStringLiteral
    }

    /**
     * @return Whether the current statement is only closed comments so far and can be discarded.
     */
    fun canDiscard(): Boolean {
        return !insideAlternateQuoteStringLiteral && !insideQuoteStringLiteral && !insideMultiLineComment && !nonCommentStatementPartSeen
    }

    /**
     * Simplifies this line to make it easier to parse.
     *
     * @param line The line to simplify.
     * @return The simplified line.
     */
    protected fun simplifyLine(line: String): String {
        return removeEscapedQuotes(line).replace("--", " -- ").replace("\\s+".toRegex(), " ").trim { it <= ' ' }.toUpperCase()
    }

    /**
     * Checks whether this line in the CQL script indicates that the statement delimiter will be different from the
     * current one. Useful for database-specific stored procedures and block constructs.
     *
     * @param line The simplified line to analyse.
     * @param delimiter The current delimiter.
     * @return The new delimiter to use (can be the same as the current one) or `null` for no delimiter.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected fun changeDelimiterIfNecessary(line: String, delimiter: Delimiter): Delimiter {
        return delimiter
    }

    /**
     * Checks whether this line terminates the current statement.
     *
     * @param line The line to check.
     * @param delimiter The current delimiter.
     * @return {@code true} if it does, {@code false} if it doesn't.
     */
    private fun lineTerminatesStatement(line: String, delimiter: Delimiter?): Boolean {
        if (delimiter == null) {
            return false
        }

        val upperCaseDelimiter = delimiter.delimiter.toUpperCase()

        if (delimiter.isAloneOnLine) {
            return line == upperCaseDelimiter
        }

        return line.endsWith(upperCaseDelimiter)
    }

    /**
     * Extracts the alternate open quote from this token (if any).
     *
     * @param token The token to check.
     * @return The alternate open quote. `null` if none.
     */
    protected fun extractAlternateOpenQuote(token: String): String? {
        return null
    }

    /**
     * Computes the alternate closing quote for this open quote.
     *
     * @param openQuote The alternate open quote.
     * @return The close quote.
     */
    protected fun computeAlternateCloseQuote(openQuote: String): String {
        return openQuote
    }

    /**
     * Applies any state changes resulting from this line being added.
     *
     * @param line The line that was just added to the statement.
     */
    protected fun applyStateChanges(line: String) {
        // Ignore all special characters that naturally occur in CQL, but are not opening or closing string literals
        val tokens = StringUtils.tokenizeToStringArray(line, " @<>;:=|(),+{}")

        val delimitingTokens = extractStringLiteralDelimitingTokens(tokens)

        lineEndsWithSingleLineComment = false
        for (delimitingToken in delimitingTokens) {
            if (!insideQuoteStringLiteral && !insideAlternateQuoteStringLiteral
                    && TokenType.MULTI_LINE_COMMENT == delimitingToken) {
                insideMultiLineComment = !insideMultiLineComment
            }

            if (!insideQuoteStringLiteral && !insideAlternateQuoteStringLiteral && !insideMultiLineComment
                    && TokenType.SINGLE_LINE_COMMENT == delimitingToken) {
                lineEndsWithSingleLineComment = true
                return
            }

            if (!insideMultiLineComment && !insideQuoteStringLiteral &&
                    TokenType.ALTERNATE_QUOTE == delimitingToken) {
                insideAlternateQuoteStringLiteral = !insideAlternateQuoteStringLiteral
            }

            if (!insideMultiLineComment && !insideAlternateQuoteStringLiteral &&
                    TokenType.QUOTE == delimitingToken) {
                insideQuoteStringLiteral = !insideQuoteStringLiteral
            }

            if (!insideMultiLineComment && !insideQuoteStringLiteral && !insideAlternateQuoteStringLiteral &&
                    TokenType.OTHER == delimitingToken) {
                nonCommentStatementPartSeen = true
            }
        }
    }

    /**
     * Extract the type of all tokens that potentially delimit string literals.
     *
     * @param tokens The tokens to analyse.
     * @return The list of potentially delimiting string literals token types per token.
     *         Tokens that do not have any impact on string delimiting are discarded.
     */
    private fun extractStringLiteralDelimitingTokens(tokens: Array<String>): List<TokenType> {
        val delimitingTokens = ArrayList<TokenType>()
        for (token in tokens) {
            val cleanToken = cleanToken(token)
            var handled = false

            if (alternateQuote == null) {
                val alternateQuoteFromToken = extractAlternateOpenQuote(cleanToken)
                if (alternateQuoteFromToken != null) {
                    val closeQuote = computeAlternateCloseQuote(alternateQuoteFromToken)
                    if (cleanToken.length >= alternateQuoteFromToken.length + closeQuote.length
                            && cleanToken.startsWith(alternateQuoteFromToken) && cleanToken.endsWith(closeQuote)) {
                        //Skip $$abc$$, ...
                        continue
                    }

                    alternateQuote = closeQuote
                    delimitingTokens.add(TokenType.ALTERNATE_QUOTE)

                    continue
                }
            }
            if (alternateQuote != null && cleanToken.endsWith(alternateQuote!!)) {
                alternateQuote = null
                delimitingTokens.add(TokenType.ALTERNATE_QUOTE)
                continue
            }

            if (cleanToken.length >= 2 && cleanToken.startsWith("'") && cleanToken.endsWith("'")) {
                //Skip '', 'abc', ...
                continue
            }
            if (cleanToken.length >= 4 && cleanToken.startsWith("/*") && cleanToken.endsWith("*/")) {
                //Skip /**/, /*comment*/, ...
                continue
            }

            if (isSingleLineComment(cleanToken)) {
                delimitingTokens.add(TokenType.SINGLE_LINE_COMMENT)
                handled = true
            }

            if (cleanToken.startsWith("/*")) {
                delimitingTokens.add(TokenType.MULTI_LINE_COMMENT)
                handled = true
            } else if (cleanToken.startsWith("'")) {
                delimitingTokens.add(TokenType.QUOTE)
                handled = true
            }

            if (!cleanToken.startsWith("/*") && cleanToken.endsWith("*/")) {
                delimitingTokens.add(TokenType.MULTI_LINE_COMMENT)
                handled = true
            } else if (!cleanToken.startsWith("'") && cleanToken.endsWith("'")) {
                delimitingTokens.add(TokenType.QUOTE)
                handled = true
            }

            if (!handled) {
                delimitingTokens.add(TokenType.OTHER)
            }
        }

        return delimitingTokens
    }

    /**
     * Removes escaped quotes from this token.
     *
     * @param token The token to parse.
     * @return The cleaned token.
     */
    protected fun removeEscapedQuotes(token: String): String {
        return StringUtils.replaceAll(token, "''", "")
    }

    /**
     * Performs additional cleanup on this token, such as removing charset casting that prefixes string literals.
     * Must be implemented in dialect specific sub classes.
     *
     * @param token The token to clean.
     * @return The cleaned token.
     */
    protected fun cleanToken(token: String): String {
        return token
    }

    /**
     * The types of tokens relevant for string delimiter related parsing.
     */
    private enum class TokenType {

        /** Some other token. */
        OTHER,

        /** Token opens or closes a ' string literal. */
        QUOTE,

        /** Token opens or closes an alternate string literal. */
        ALTERNATE_QUOTE,

        /** Token starts end of line comment. */
        SINGLE_LINE_COMMENT,

        /** Token opens or closes multi-line comment. */
        MULTI_LINE_COMMENT

    }

    /**
     * CqlStatementBuilder companion object.
     */
    companion object {

        /**
         * Strips this delimiter from this cql statement.
         *
         * @param cql The statement to parse.
         * @param delimiter The delimiter to strip.
         */
        internal fun stripDelimiter(cql: StringBuilder, delimiter: Delimiter) {
            var last: Int = cql.length
            while (last > 0) {
                if (!Character.isWhitespace(cql[last - 1])) {
                    break
                }
                last--
            }

            cql.delete(last - delimiter.delimiter.length, cql.length)
        }

    }

}
