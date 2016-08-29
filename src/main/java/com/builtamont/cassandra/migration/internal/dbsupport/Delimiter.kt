/**
 * File     : Delimiter.kt
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

/**
 * Represents a CQL statement delimiter.
 *
 * @param delimiter The actual delimiter string.
 * @param isAloneOnLine Whether the delimiter sits alone on a new line or not.
 */
class Delimiter(val delimiter: String, val isAloneOnLine: Boolean) {

    /**
     * @return The computed delimiter instance hash value.
     */
    override fun hashCode(): Int {
        var result = delimiter.hashCode()
        result = 31 * result + if (isAloneOnLine) 1 else 0
        return result
    }

    /**
     * @return {@code true} if this delimiter instance is the same as the given object.
     */
    override fun equals(other: Any?): Boolean {

        /**
         * @return {@code true} if this version instance is not the same as the given object.
         */
        fun isNotSame(): Boolean {
            return other == null || javaClass != other.javaClass
        }

        /**
         * @return {@code true} if delimiter is alone on line.
         */
        fun isDelimiterAloneOnLine(): Boolean {
            val delimiter1 = other as Delimiter?
            return isAloneOnLine == delimiter1!!.isAloneOnLine && delimiter == delimiter1.delimiter
        }

        return when {
            this === other -> true
            isNotSame()    -> false
            else           -> isDelimiterAloneOnLine()
        }
    }

}
