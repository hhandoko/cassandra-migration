/**
 * File     : MigrationInfoServiceImpl.kt
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
package com.builtamont.cassandra.migration.internal.info

import com.builtamont.cassandra.migration.api.*
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration
import java.util.*

/**
 * Default implementation of MigrationInfoService.
 *
 * @param migrationResolver The migration resolver for available migrations.
 * @param schemaVersionDAO The schema version table DAO implementation.
 * @param target The target version up to which to retrieve the info.
 * @param outOfOrder Allows migrations to be run "out of order".
 *                   If you already have versions 1 and 3 applied, and now a version 2 is found,
 *                   it will be applied too instead of being ignored.
 *                   (default: `false`)
 * @param pendingOrFuture Allows pending or future migrations to be run.
 */
class MigrationInfoServiceImpl(
    private val migrationResolver: MigrationResolver,
    private val schemaVersionDAO: SchemaVersionDAO,
    private var target: MigrationVersion?,
    private val outOfOrder: Boolean,
    private val pendingOrFuture: Boolean
) : MigrationInfoService {

    /**
     * The migrations infos calculated at the last refresh.
     */
    private var migrationInfos: List<MigrationInfoImpl> = emptyList()

    /**
     * Refreshes the info about all known migrations from both the classpath and the DB.
     */
    override fun refresh() {
        val availableMigrations = migrationResolver.resolveMigrations()
        val appliedMigrations = schemaVersionDAO.findAppliedMigrations()

        migrationInfos = mergeAvailableAndAppliedMigrations(availableMigrations, appliedMigrations)

        if (MigrationVersion.CURRENT === target) {
            target = current()?.version
        }
    }

    /**
     * Validate all migrations for consistency.
     *
     * @return The error message, or `null` if everything is fine.
     */
    override fun validate(): String? {
        migrationInfos.forEach { it.validate()?.let { return it } }
        return null
    }

    /**
     * Retrieves the full set of infos about the migrations.
     *
     * @return The migrations.
     */
    override fun all(): Array<MigrationInfo> {
        return migrationInfos.toTypedArray()
    }

    /**
     * @return Current migration to be run.
     */
    override fun current(): MigrationInfo? {
        return migrationInfos.lastOrNull { it.state.isApplied }
    }

    /**
     * Retrieves the full set of infos about the pending migrations.
     *
     * @return The pending migrations. An empty array if none.
     */
    override fun pending(): Array<MigrationInfo> {
        return migrationInfos.filter { it.state === MigrationState.PENDING }.orEmpty().toTypedArray()
    }

    /**
     * Retrieves the full set of infos about the migrations applied on the DB.
     *
     * @return The applied migrations. An empty array if none.
     */
    override fun applied(): Array<MigrationInfo> {
        return migrationInfos.filter { it.state.isApplied }.orEmpty().toTypedArray()
    }

    /**
     * Retrieves the full set of infos about the migrations resolved on the classpath.
     *
     * @return The resolved migrations. An empty array if none.
     */
    override fun resolved(): Array<MigrationInfo> {
        return migrationInfos.filter { it.state.isResolved }.orEmpty().toTypedArray()
    }

    /**
     * Retrieves the full set of infos about the migrations that failed.
     *
     * @return The failed migrations. An empty array if none.
     */
    override fun failed(): Array<MigrationInfo> {
        return migrationInfos.filter { it.state.isFailed }.orEmpty().toTypedArray()
    }

    /**
     * Retrieves the full set of infos about future migrations applied to the DB.
     *
     * @return The future migrations. An empty array if none.
     */
    override fun future(): Array<MigrationInfo> {
        return migrationInfos.filter { it.state === MigrationState.FUTURE_SUCCESS }.orEmpty().toTypedArray()
    }

    /**
     * Retrieves the full set of infos about out of order migrations applied to the DB.
     *
     * @return The out of order migrations. An empty array if none.
     */
    override fun outOfOrder(): Array<MigrationInfo> {
        return migrationInfos.filter { it.state === MigrationState.OUT_OF_ORDER }.orEmpty().toTypedArray()
    }

    /**
     * Merges the available and the applied migrations to produce one fully aggregated and consolidated list.
     *
     * @param resolvedMigrations The available migrations.
     * @param appliedMigrations The applied migrations.
     * @return The complete list of migrations.
     */
    fun mergeAvailableAndAppliedMigrations(resolvedMigrations: Collection<ResolvedMigration>, appliedMigrations: List<AppliedMigration>): List<MigrationInfoImpl> {
        val context = MigrationInfoContext()
        context.outOfOrder = outOfOrder
        context.pendingOrFuture = pendingOrFuture
        context.target = target

        val resolvedMigrationsMap = TreeMap<MigrationVersion, ResolvedMigration>()
        for (resolvedMigration in resolvedMigrations) {
            val version = resolvedMigration.version
            if (version!!.compareTo(context.lastResolved) > 0) {
                context.lastResolved = version
            }
            resolvedMigrationsMap.put(version, resolvedMigration)
        }

        val appliedMigrationsMap = TreeMap<MigrationVersion, AppliedMigration>()
        for (appliedMigration in appliedMigrations) {
            val version = appliedMigration.version
            if (version!!.compareTo(context.lastApplied) > 0) {
                context.lastApplied = version
            }
            if (appliedMigration.type === MigrationType.SCHEMA) {
                context.schema = version
            }
            if (appliedMigration.type === MigrationType.BASELINE) {
                context.baseline = version
            }
            appliedMigrationsMap.put(version, appliedMigration)
        }

        val allVersions = HashSet<MigrationVersion>()
        allVersions.addAll(resolvedMigrationsMap.keys)
        allVersions.addAll(appliedMigrationsMap.keys)

        val migrationInfos = ArrayList<MigrationInfoImpl>()
        for (version in allVersions) {
            val resolvedMigration = resolvedMigrationsMap[version]
            val appliedMigration = appliedMigrationsMap[version]
            migrationInfos.add(MigrationInfoImpl(resolvedMigration, appliedMigration, context))
        }

        Collections.sort(migrationInfos)

        return migrationInfos
    }

}
