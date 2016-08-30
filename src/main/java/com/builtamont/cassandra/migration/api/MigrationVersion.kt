/**
 * File     : MigrationChecksumProvider.kt
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
package com.builtamont.cassandra.migration.api

import java.math.BigInteger
import java.util.*
import java.util.regex.Pattern

/**
 * A version of a migration.
 */
class MigrationVersion : Comparable<MigrationVersion?> {

    /**
     * The version in one of the following formats: 6, 6.0, 005, 1.2.3.4, 201004200021.
     * {@code null} means that this version refers to an empty schema.
     */
    lateinit private var versionParts: List<BigInteger>

    /**
     * The alternative text to display instead of the version number.
     */
    lateinit private var displayText: String

    /**
     * Creates a Version using this version string.
     *
     * @param version The version in one of the following formats: 6, 6.0, 005, 1.2.3.4, 201004200021.
     *                {@code null} means that this version refers to an empty schema.
     */
    private constructor(version: String) {
        val normalizedVersion = version.replace('_', '.')
        init(tokenize(normalizedVersion), normalizedVersion)
    }

    /**
     * Creates a Version using this version string.
     *
     * @param version     The version in one of the following formats: 6, 6.0, 005, 1.2.3.4, 201004200021.
     *                    {@code null} means that this version refers to an empty schema.
     * @param displayText The alternative text to display instead of the version number.
     */
    private constructor(version: BigInteger?, displayText: String) {
        val versionParts = version?.let { listOf(it) }.orEmpty()
        init(versionParts, displayText)
    }

    /** Migration version table name */
    val table = "cassandra_migration_version"

    /** Migration numeric version as String */
    var version: String?
        get() {
            return when {
                this == EMPTY  -> null
                this == LATEST -> java.lang.Long.toString(java.lang.Long.MAX_VALUE)
                else           -> displayText
            }
        }
        private set(version) {
            val normalizedVersion = version!!.replace('_', '.')
            this.versionParts = tokenize(normalizedVersion)
            this.displayText = normalizedVersion
        }

    /**
     * @return The textual representation of the version instance.
     */
    override fun toString(): String {
        return displayText
    }

    /**
     * @return The computed version instance hash value.
     */
    override fun hashCode(): Int {
        return versionParts.hashCode()
    }

    /**
     * @return {@code true} if this version instance is the same as the given object.
     */
    override fun equals(other: Any?): Boolean {

        /**
         * @return {@code true} if this version instance is not the same as the given object.
         */
        fun isNotSame(): Boolean {
            return other == null || javaClass != other.javaClass
        }

        return when {
            other === this -> true
            isNotSame()    -> false
            else           -> compareTo(other as? MigrationVersion) == 0
        }
    }

    /**
     * @return {@code true} if this version instance is comparable to the given object.
     */
    override fun compareTo(other: MigrationVersion?): Int {

        /**
         * @return Element value at given index or zero.
         */
        fun getOrZero(elements: List<BigInteger>, i: Int): BigInteger {
            return if (i < elements.size) elements[i] else BigInteger.ZERO
        }

        // Guard clauses, early return
        when {
            other == null     -> return 1
            this === EMPTY    -> return if (other === EMPTY) 0 else Integer.MIN_VALUE
            this === CURRENT  -> return if (other === CURRENT) 0 else Integer.MIN_VALUE
            this === LATEST   -> return if (other === LATEST) 0 else Integer.MAX_VALUE
            other === EMPTY   -> return Integer.MAX_VALUE
            other === CURRENT -> return Integer.MAX_VALUE
            other === LATEST  -> return Integer.MIN_VALUE
        }

        val elements1 = versionParts
        val elements2 = other?.versionParts
        val largestNumberOfElements = Math.max(elements1.size, elements2!!.size)

        // Iterate through the version parts to compare version numbers
        for (i in 0..largestNumberOfElements - 1) {
            val compared = getOrZero(elements1, i).compareTo(getOrZero(elements2, i))
            if (compared != 0) {
                return compared
            }
        }

        return 0
    }

    /**
     * Class initialisation helper.
     *
     * @param versionParts The migration version parts.
     * @param displayText
     */
    private fun init(versionParts: List<BigInteger>, displayText: String) {
        this.versionParts = versionParts
        this.displayText = displayText
    }

    /**
     * Splits this string into list of Long.
     *
     * @param str The string to split.
     * @return The resulting array.
     * @throws NumberFormatException when input string contains invalid characters.
     */
    @Throws(NumberFormatException::class)
    private fun tokenize(str: String): List<BigInteger> {
        val numbers = ArrayList(splitPattern.split(str).map { x ->
            try {
                BigInteger(x)
            } catch (e: NumberFormatException) {
                throw CassandraMigrationException("Invalid version containing non-numeric characters. Only 0..9 and . are allowed. Invalid version: $str")
            }
        })

        // TODO: Refactor to functional loop
        // NOTE: Currently there's no idiomatic way to break out of a loop in Kotlin
        for (i in numbers.size - 1 downTo 1) {
            if (numbers[i] != BigInteger.ZERO) break
            numbers.removeAt(i)
        }

        return numbers
    }

    /**
     * MigrationVersion companion object.
     */
    companion object {

        /** Version for an empty schema. */
        val EMPTY = MigrationVersion(null, "<< Empty Schema >>")

        /** Latest version. */
        val LATEST = MigrationVersion(BigInteger.valueOf(-1), "<< Latest Version >>")

        /** Current version. Only a marker. For the real version use CassandraMigration.info().current() instead. */
        val CURRENT = MigrationVersion(BigInteger.valueOf(-2), "<< Current Version >>")

        /** Compiled pattern for matching proper version format. */
        private val splitPattern = Pattern.compile("\\.(?=\\d)")

        /**
         * Factory for creating a MigrationVersion from a version String.
         *
         * @param version The version String. The value {@code current} will be interpreted as MigrationVersion.CURRENT,
         *                a marker for the latest version that has been applied to the database.
         * @return The MigrationVersion.
         */
        fun fromVersion(version: String?): MigrationVersion {

            /**
             * @return {@code true} if version is "CURRENT".
             */
            fun isCurrent(): Boolean {
                return "current".equals(version, ignoreCase = true)
            }

            return when {
                version == null           -> EMPTY
                isCurrent()               -> CURRENT
                LATEST.version == version -> LATEST
                else                      -> MigrationVersion(version)
            }
        }

    }

}
