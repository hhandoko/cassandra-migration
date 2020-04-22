/**
 * File     : CompositeMigrationResolver.kt
 * License  :
 *   Original   - Copyright (c) 2010 - 2016 Boxfuse GmbH
 *   Derivative - Copyright (c) 2016 - 2018 cassandra-migration Contributors
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
package com.hhandoko.cassandra.migration.internal.resolver

import com.hhandoko.cassandra.migration.api.CassandraMigrationException
import com.hhandoko.cassandra.migration.api.resolver.MigrationResolver
import com.hhandoko.cassandra.migration.api.resolver.ResolvedMigration
import java.util.*

/**
 * Facility for retrieving and sorting the available migrations from the classpath through the various migration
 * resolvers.
 *
 * @param customMigrationResolvers Custom Migration Resolvers.
 */
class CompositeMigrationResolver(
    vararg customMigrationResolvers: MigrationResolver
) : MigrationResolver {

    /**
     * The migration resolvers to use internally.
     */
    public val migrationResolvers = ArrayList<MigrationResolver>()

    /**
     * The available migrations, sorted by version, newest first. An empty list is returned when no migrations can be
     * found.
     */
    private var availableMigrations: List<ResolvedMigration>? = null

    /**
     * CompositeMigrationResolver initialization.
     */
    init {
        migrationResolvers.addAll(listOf(*customMigrationResolvers))
    }

    /**
     * Finds all available migrations using all migration resolvers (CQL, Java, ...).
     *
     * @return The available migrations, sorted by version, oldest first. An empty list is returned when no migrations
     *         can be found.
     * @throws CassandraMigrationException when the available migrations have overlapping versions.
     */
    override fun resolveMigrations(): List<ResolvedMigration> {
        if (availableMigrations == null) {
            availableMigrations = doFindAvailableMigrations()
        }

        return availableMigrations as List<ResolvedMigration>
    }

    /**
     * Finds all available migrations using all migration resolvers (CQL, Java, ...).
     *
     * @return The available migrations, sorted by version, oldest first. An empty list is returned when no migrations
     *         can be found.
     * @throws CassandraMigrationException when the available migrations have overlapping versions.
     */
    @Throws(CassandraMigrationException::class)
    private fun doFindAvailableMigrations(): List<ResolvedMigration> {
        val migrations = ArrayList(collectMigrations(migrationResolvers))
        migrations.sortWith(ResolvedMigrationComparator())

        checkForIncompatibilities(migrations)

        return migrations
    }

    /**
     * CompositeMigrationResolver companion object.
     */
    companion object {

        /**
         * Collects all the migrations for all migration resolvers.
         *
         * @param migrationResolvers The migration resolvers to check.
         * @return All migrations.
         */
        fun collectMigrations(migrationResolvers: Collection<MigrationResolver>): Collection<ResolvedMigration> {
            return migrationResolvers.flatMap { it.resolveMigrations() }.distinct()
        }

        /**
         * Checks for incompatible migrations.
         *
         * @param migrations The migrations to check.
         * @throws CassandraMigrationException when two different migration with the same version number are found.
         */
        fun checkForIncompatibilities(migrations: List<ResolvedMigration>) {

            /**
             * Returns incompatible migration error message.
             *
             * @param current The current migration run.
             * @param next The next migration to run.
             * @return Incompatible migration error message.
             */
            fun incompatibleErrorMsg(current: ResolvedMigration, next: ResolvedMigration): String {
                return "Found more than one migration with version ${current.version}\nOffenders:\n-> ${current.physicalLocation} (${current.type})\n-> ${next.physicalLocation} (${next.type})"
            }

            // Check for more than one migration with same version
            for (i in 0..migrations.size - 1 - 1) {
                val current = migrations[i]
                val next = migrations[i + 1]
                if (current.version!!.compareTo(next.version) == 0) {
                    throw CassandraMigrationException(incompatibleErrorMsg(current, next))
                }
            }
        }

    }

}
