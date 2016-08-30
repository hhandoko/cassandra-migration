/**
 * File     : CqlMigrationResolver.kt
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
package com.builtamont.cassandra.migration.internal.resolver.cql

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import com.builtamont.cassandra.migration.internal.resolver.MigrationInfoHelper
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationComparator
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationImpl
import com.builtamont.cassandra.migration.internal.util.ScriptsLocation
import com.builtamont.cassandra.migration.internal.util.scanner.Resource
import com.builtamont.cassandra.migration.internal.util.scanner.Scanner
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.zip.CRC32

/**
 * Migration resolver for CQL files on the classpath.
 * The CQL files must have names like V1__Description.cql or V1_1__Description.cql.
 *
 * @param classLoader The ClassLoader for loading migrations on the classpath.
 * @param location The location on the classpath where the migrations are located.
 * @param encoding The encoding of the .cql file.
 */
class CqlMigrationResolver(
    classLoader: ClassLoader,
    private val location: ScriptsLocation,
    private val encoding: String
) : MigrationResolver {

    /** The scanner to use. */
    private val scanner: Scanner

    /**
     * CqlMigrationResolver initialization.
     */
    init {
        this.scanner = Scanner(classLoader)
    }

    /**
     * Resolves the available migrations.
     *
     * @return The available migrations.
     */
    override fun resolveMigrations(): List<ResolvedMigration> {
        val resources = scanner.scanForResources(location, CQL_MIGRATION_PREFIX, CQL_MIGRATION_SUFFIX)

        return resources.map { resource ->
            val resolvedMigration = extractMigrationInfo(resource)
            resolvedMigration.physicalLocation = resource.locationOnDisk
            resolvedMigration.executor = CqlMigrationExecutor(resource, encoding)
            resolvedMigration
        }.sortedWith(ResolvedMigrationComparator())
    }

    /**
     * Extracts the migration info for this resource.
     *
     * @param resource The resource to analyse.
     * @return The migration info.
     */
    private fun extractMigrationInfo(resource: Resource): ResolvedMigration {
        val info = MigrationInfoHelper.extractVersionAndDescription(
            resource.filename,
            CQL_MIGRATION_PREFIX,
            CQL_MIGRATION_SEPARATOR,
            CQL_MIGRATION_SUFFIX
        )

        val migration = ResolvedMigrationImpl()
        migration.version = info.left
        migration.description = info.right
        migration.script = extractScriptName(resource)
        migration.checksum = calculateChecksum(resource, resource.loadAsString("UTF-8"))
        migration.type = MigrationType.CQL
        return migration
    }

    /**
     * Extracts the script name from this resource.
     *
     * @param resource The resource to process.
     * @return The script name.
     */
    fun extractScriptName(resource: Resource): String {
        return if (location.path!!.isEmpty()) {
            resource.location
        } else {
            resource.location.substring(location.path!!.length + 1)
        }
    }

    /**
     * CqlMigrationResolver companion object.
     */
    companion object {

        /** The prefix for CQL migrations. */
        private val CQL_MIGRATION_PREFIX = "V"

        /** The separator for CQL migrations. */
        private val CQL_MIGRATION_SEPARATOR = "__"

        /** The suffix for cql migrations. */
        private val CQL_MIGRATION_SUFFIX = ".cql"

        /**
         * Calculates the checksum of these bytes.
         *
         * @param bytes The bytes to calculate the checksum for.
         * @return The crc-32 checksum of the bytes.
         */
        @Deprecated("Replaced with `calculateChecksum(resource: Resource, str: String)` as per Flyway 4.x")
        private fun calculateChecksum(bytes: ByteArray): Int {
            val crc32 = CRC32()
            crc32.update(bytes)
            return crc32.value.toInt()
        }

        /**
         * Calculates the checksum of this string.
         *
         * @param resource The resource to process.
         * @param str The string to calculate the checksum for.
         * @return The crc-32 checksum of the bytes.
         */
        private fun calculateChecksum(resource: Resource, str: String): Int {
            val crc32 = CRC32()

            try {
                BufferedReader(StringReader(str)).forEachLine { line ->
                    crc32.update(line.toByteArray(charset("UTF-8")))
                }
            } catch (e: IOException) {
                val message = "Unable to calculate checksum for ${resource.location} (${resource.locationOnDisk})"
                throw CassandraMigrationException(message, e)
            }

            return crc32.value.toInt()
        }

    }

}
