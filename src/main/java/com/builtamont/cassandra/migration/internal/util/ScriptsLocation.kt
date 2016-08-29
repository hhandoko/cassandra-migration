/**
 * File     : ScriptsLocation.kt
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
package com.builtamont.cassandra.migration.internal.util

import com.builtamont.cassandra.migration.api.CassandraMigrationException

/**
 * A location to load migrations from.
 *
 * @param descriptor The location descriptor.
 */
class ScriptsLocation(descriptor: String) : Comparable<ScriptsLocation> {

    /**
     * The prefix part of the location. Can be either classpath: or filesystem:.
     */
    var prefix: String? = null
        private set

    /**
     * The path part of the location.
     */
    var path: String? = null
        private set

    /**
     * ScriptsLocation initialization.
     */
    init {
        // Initialize `prefix` and `path`
        val normalizedDescriptor = descriptor.trim { it <= ' ' }.replace("\\", "/")
        if (normalizedDescriptor.contains(":")) {
            prefix = normalizedDescriptor.substring(0, normalizedDescriptor.indexOf(":") + 1)
            path = normalizedDescriptor.substring(normalizedDescriptor.indexOf(":") + 1)
        } else {
            prefix = CLASSPATH_PREFIX
            path = normalizedDescriptor
        }

        // Check that location is either classpath or filesystem,
        // otherwise throw `CassandraMigrationException`
        if (isClassPath) {
            path = path!!.replace(".", "/")
            if (path!!.startsWith("/")) {
                path = path!!.substring(1)
            }
        } else if (!isFileSystem) {
            val unknownPrefixLogMsg = "Unknown prefix for location. Must be ${CLASSPATH_PREFIX} or ${FILESYSTEM_PREFIX}.$normalizedDescriptor"
            throw CassandraMigrationException(unknownPrefixLogMsg)
        }

        // Trim trailing slash in path
        if (path!!.endsWith("/")) {
            path = path!!.substring(0, path!!.length - 1)
        }
    }

    /**
     * Denotes whether this location is on the classpath.
     */
    val isClassPath: Boolean
        get() = CLASSPATH_PREFIX == prefix

    /**
     * Denotes whether this location is on the filesystem.
     */
    val isFileSystem: Boolean
        get() = FILESYSTEM_PREFIX == prefix

    /**
     * The complete location descriptor.
     */
    val descriptor: String
        get() = prefix!! + path!!

    /**
     * Checks whether this location is a parent of the other location.
     *
     * @return {@code true} if it is a parent of the other location.
     */
    fun isParentOf(other: ScriptsLocation): Boolean {
        return (other.descriptor + "/").startsWith(descriptor + "/")
    }

    /**
     * @return The complete location descriptor.
     */
    override fun toString(): String {
        return descriptor
    }

    /**
     * @return The computed location instance hash value.
     */
    override fun hashCode(): Int {
        return descriptor.hashCode()
    }

    /**
     * @return {@code true} if this location instance is the same as the given object.
     */
    override fun equals(other: Any?): Boolean {

        /**
         * @return {@code true} if this version instance is not the same as the given object.
         */
        fun isNotSame(): Boolean {
            return other == null || javaClass != other.javaClass
        }

        return when {
            this === other -> true
            isNotSame()    -> false
            else           -> descriptor == (other as ScriptsLocation?)!!.descriptor
        }
    }

    /**
     * @return {@code true} if this location instance is comparable to the given object.
     */
    @SuppressWarnings("NullableProblems")
    override fun compareTo(other: ScriptsLocation): Int {
        return descriptor.compareTo(other.descriptor)
    }

    /**
     * ScriptsLocation companion object.
     */
    companion object {

        /** The prefix for classpath locations. */
        private val CLASSPATH_PREFIX = "classpath:"

        /** The prefix for filesystem locations. */
        val FILESYSTEM_PREFIX = "filesystem:"

    }

}
