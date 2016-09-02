/**
 * File     : JavaMigrationResolver.kt
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
package com.builtamont.cassandra.migration.internal.resolver.java

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.MigrationType
import com.builtamont.cassandra.migration.api.MigrationVersion
import com.builtamont.cassandra.migration.api.migration.MigrationChecksumProvider
import com.builtamont.cassandra.migration.api.migration.MigrationInfoProvider
import com.builtamont.cassandra.migration.api.migration.java.JavaMigration
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import com.builtamont.cassandra.migration.internal.resolver.MigrationInfoHelper
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationComparator
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationImpl
import com.builtamont.cassandra.migration.internal.util.ClassUtils
import com.builtamont.cassandra.migration.internal.util.ScriptsLocation
import com.builtamont.cassandra.migration.internal.util.StringUtils
import com.builtamont.cassandra.migration.internal.util.scanner.Scanner
import java.util.*

/**
 * Migration resolver for Java migrations.
 * The classes must have a name like V1 or V1_1_3 or V1__Description or V1_1_3__Description.
 *
 * @param classLoader The ClassLoader for loading migrations on the classpath.
 * @param location The base package on the classpath where to migrations are located.
 */
class JavaMigrationResolver(
    private val classLoader: ClassLoader,
    private val location: ScriptsLocation?
) : MigrationResolver {

    /**
     * Resolves the available migrations.
     *
     * @return The available migrations.
     * @throws CassandraMigrationException when unable to resolve JavaMigrator with the given location.
     */
    @Throws(CassandraMigrationException::class)
    override fun resolveMigrations(): List<ResolvedMigration> {
        // Guard: If location  is not defined and not a classpath,
        //        then return an empty JavaMigration resolvers
        if (location != null && !location.isClassPath) {
            return ArrayList()
        }

        try {
            val classes = Scanner(classLoader).scanForClasses(location, JavaMigration::class.java)

            return classes.map { clazz ->
                val javaMigration = ClassUtils.instantiate<JavaMigration>(clazz.name, classLoader)

                val resolvedMigration = extractMigrationInfo(javaMigration)
                resolvedMigration.physicalLocation = ClassUtils.getLocationOnDisk(clazz)
                resolvedMigration.executor = JavaMigrationExecutor(javaMigration)
                resolvedMigration
            }.sortedWith(ResolvedMigrationComparator())
        } catch (e: Exception) {
            throw CassandraMigrationException("Unable to resolve Java migrations in location: " + location, e)
        }
    }

    /**
     * Extracts the migration info from this migration.
     *
     * @param javaMigration The migration to analyse.
     * @return The migration info.
     * @throws CassandraMigrationException when JavaMigration is missing its description.
     */
    @Throws(CassandraMigrationException::class)
    fun extractMigrationInfo(javaMigration: JavaMigration): ResolvedMigration {
        val checksum: Int?
        if (javaMigration is MigrationChecksumProvider) {
            checksum = javaMigration.checksum
        } else {
            checksum = 0
        }

        val version: MigrationVersion
        val description: String
        if (javaMigration is MigrationInfoProvider) {
            version = javaMigration.version
            description = javaMigration.description
            if (!StringUtils.hasText(description)) {
                throw CassandraMigrationException("Missing description for migration " + version)
            }
        } else {
            val className = ClassUtils.getShortName(javaMigration.javaClass)
            val info = MigrationInfoHelper.extractVersionAndDescription(className, "V", "__", "")
            version = info.left
            description = info.right
        }

        val script = javaMigration.javaClass.name

        val resolvedMigration = ResolvedMigrationImpl()
        resolvedMigration.version = version
        resolvedMigration.description = description
        resolvedMigration.script = script
        resolvedMigration.checksum = checksum
        resolvedMigration.type = MigrationType.JAVA_DRIVER
        return resolvedMigration
    }
}
