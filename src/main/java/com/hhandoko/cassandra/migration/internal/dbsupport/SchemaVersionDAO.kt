/**
 * File     : SchemaVersionDAO.kt
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
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
package com.hhandoko.cassandra.migration.internal.dbsupport

import com.datastax.driver.core.*
import com.datastax.driver.core.exceptions.InvalidQueryException
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.eq
import com.datastax.driver.core.querybuilder.Select
import com.hhandoko.cassandra.migration.api.MigrationType
import com.hhandoko.cassandra.migration.api.MigrationVersion
import com.hhandoko.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.hhandoko.cassandra.migration.internal.metadatatable.AppliedMigration
import com.hhandoko.cassandra.migration.internal.util.CachePrepareStatement
import com.hhandoko.cassandra.migration.internal.util.logging.LogFactory
import java.util.*

/**
 * Schema migrations table Data Access Object.
 *
 * @param session The Cassandra session connection to use to execute the migration.
 * @param keyspaceConfig The Cassandra keyspace to connect to.
 * @param tableName The Cassandra migration version table name.
 */
open class SchemaVersionDAO(private val session: Session, val keyspaceConfig: KeyspaceConfiguration, val tableName: String) {

    private val cachePs: CachePrepareStatement
    private val consistencyLevel: ConsistencyLevel

    // TODO: Break SchemaVersionDAO into service and table-specific mappings.
    // NOTE: SchemaVersionDAO might be able to be broken down further into service (i.e. logic) and actual data access /
    //       persistence. Right now it seems to be doing too much.
    //       Once it is refactored, the query and statements build methods can be kept inside the lazy val bodies rather
    //       than separate private methods.
    private val createSchemaMigrationTableStmt: SimpleStatement by lazy { buildCreateSchemaMigrationTableStmt() }
    private val createSchemaMigrationCounterTableStmt: SimpleStatement by lazy { buildCreateSchemaMigrationCounterTableStmt() }
    private val countSchemaMigrationTableQuery: Select by lazy { buildCountSchemaMigrationTableQuery() }
    private val countSchemaMigrationCounterTableQuery: Select by lazy { buildCountSchemaMigrationCounterTableQuery() }
    private val insertSchemaMigrationTableStmt: PreparedStatement by lazy { buildInsertSchemaMigrationRecordStmt() }
    private val findAppliedMigrationsQuery: Select by lazy { buildFindAppliedMigrationsQuery() }
    private val incrementInstalledRankStmt: SimpleStatement by lazy { buildIncrementInstalledRankStmt() }
    private val findInstalledRankCountColQuery: Select by lazy { buildFindInstalledRankCountColQuery() }
    private val findVersionRankQuery: Select by lazy { buildFindVersionRankQuery() }
    private val updateVersionRankStmt: PreparedStatement by lazy { buildUpdateVersionRankStmt() }

    init {
        this.cachePs = CachePrepareStatement(session)

        // If running on a single host, don't force ConsistencyLevel.ALL
        val isClustered = session.cluster.metadata.allHosts.size > 1
        // Use configuration consistency if provided, otherwise default to `ALL` on cluster or `ONE` on single host
        val consistencyDefined = keyspaceConfig.consistency != null

        this.consistencyLevel = when {
            consistencyDefined -> keyspaceConfig.consistency!!
            isClustered        -> ConsistencyLevel.ALL
            else               -> ConsistencyLevel.ONE
        }
    }

    /**
     * Create schema migration version table if it does not exists.
     */
    fun createTablesIfNotExist() {
        // GUARD: Skip table creation if already exists
        if (tablesExist()) return

        session.execute(createSchemaMigrationTableStmt)
        session.execute(createSchemaMigrationCounterTableStmt)
    }

    /**
     * Check if schema migration version table has already been created.
     *
     * @return `true` if schema migration version table exists in the keyspace.
     */
    open fun tablesExist(): Boolean {
        var schemaVersionTableExists = false
        var schemaVersionCountsTableExists = false

        try {
            val resultsSchemaVersion = session.execute(countSchemaMigrationTableQuery)
            if (resultsSchemaVersion.one() != null) {
                schemaVersionTableExists = true
            }
        } catch (e: InvalidQueryException) {
            LOG.debug("No schema version table found with a name of " + tableName)
        }

        try {
            val resultsSchemaVersionCounts = session.execute(countSchemaMigrationCounterTableQuery)
            if (resultsSchemaVersionCounts.one() != null) {
                schemaVersionCountsTableExists = true
            }
        } catch (e: InvalidQueryException) {
            LOG.debug("No schema version counts table found with a name of " + tableName + COUNTS_TABLE_NAME_SUFFIX)
        }

        return schemaVersionTableExists && schemaVersionCountsTableExists
    }

    /**
     * Add applied migration record into the schema migration version table.
     *
     * @param appliedMigration The applied migration.
     */
    fun addAppliedMigration(appliedMigration: AppliedMigration) {
        createTablesIfNotExist()

        val versionRank = calculateVersionRank(appliedMigration.version!!)
        val installedRank = calculateInstalledRank()

        val statement = boundInsertSchemaMigrationRecordStmt(versionRank, installedRank, appliedMigration)

        session.execute(statement)

        LOG.debug("Schema version table $tableName successfully updated to reflect changes")
    }

    /**
     * Retrieve the applied migrations from the schema migration version table.
     *
     * @return The applied migrations.
     */
    open fun findAppliedMigrations(): List<AppliedMigration> {
        // GUARD: Return empty array if tables does not exists
        if (!tablesExist()) return ArrayList()

        val results = session.execute(findAppliedMigrationsQuery)
        // TODO: Refactor to idiomatic Kotlin collections method
        val resultsList = ArrayList<AppliedMigration>()
        for (row in results) {
            resultsList.add(
                    AppliedMigration(
                            row.getInt("version_rank"),
                            row.getInt("installed_rank"),
                            MigrationVersion.fromVersion(row.getString("version")),
                            row.getString("description"),
                            MigrationType.valueOf(row.getString("type")),
                            row.getString("script"),
                            if (row.isNull("checksum")) null else row.getInt("checksum"),
                            row.getTimestamp("installed_on"),
                            row.getString("installed_by"),
                            row.getInt("execution_time"),
                            row.getBool("success")
                    )
            )
        }

        // NOTE: Order by `version_rank` not necessary here, as it eventually gets saved in TreeMap
        //       that uses natural ordering
        return resultsList
    }

    /**
     * Retrieve the applied migrations from the metadata table.
     *
     * @param migrationTypes The migration types to find.
     * @return The applied migrations.
     */
    open fun findAppliedMigrations(vararg migrationTypes: MigrationType): List<AppliedMigration> {
        // GUARD: Return empty array if tables does not exists
        if (!tablesExist()) return ArrayList()

        val results = session.execute(findAppliedMigrationsQuery)
        // TODO: Refactor to idiomatic Kotlin collections method
        val resultsList = ArrayList<AppliedMigration>()
        val migTypeList = Arrays.asList(*migrationTypes)
        for (row in results) {
            val migType = MigrationType.valueOf(row.getString("type"))
            if (migTypeList.contains(migType)) {
                resultsList.add(
                        AppliedMigration(
                                row.getInt("version_rank"),
                                row.getInt("installed_rank"),
                                MigrationVersion.fromVersion(row.getString("version")),
                                row.getString("description"),
                                migType,
                                row.getString("script"),
                                row.getInt("checksum"),
                                row.getTimestamp("installed_on"),
                                row.getString("installed_by"),
                                row.getInt("execution_time"),
                                row.getBool("success")
                        )
                )
            }
        }

        // NOTE: Order by `version_rank` not necessary here, as it eventually gets saved in TreeMap
        //       that uses natural ordering
        return resultsList
    }

    /**
     * Check if the keyspace has applied migrations.
     *
     * @return `true` if the keyspace has applied migrations.
     */
    open fun hasAppliedMigrations(): Boolean {
        // GUARD: Mark as no migrations if tables don't exists
        if (!tablesExist()) return false

        createTablesIfNotExist()

        // TODO: Refactor to idiomatic Kotlin collections method
        val filteredMigrations = ArrayList<AppliedMigration>()
        val appliedMigrations = findAppliedMigrations()
        for (appliedMigration in appliedMigrations) {
            if (appliedMigration.type != MigrationType.BASELINE) {
                filteredMigrations.add(appliedMigration)
            }
        }
        return filteredMigrations.isNotEmpty()
    }

    /**
     * Add a baseline version marker.
     *
     * @param baselineVersion The baseline version.
     * @param baselineDescription the baseline version description.
     * @param user The user's username executing the baselining.
     */
    fun addBaselineMarker(baselineVersion: MigrationVersion, baselineDescription: String, user: String) {
        addAppliedMigration(
                AppliedMigration(
                        baselineVersion,
                        baselineDescription,
                        MigrationType.BASELINE,
                        baselineDescription,
                        checksum = 0,
                        installedBy = user,
                        executionTime = 0,
                        success = true
                )
        )
    }

    /**
     * Get the baseline marker's applied migration.
     *
     * @return The baseline marker's applied migration.
     */
    val baselineMarker: AppliedMigration?
        get() {
            val appliedMigrations = findAppliedMigrations(MigrationType.BASELINE)
            return if (appliedMigrations.isEmpty()) null else appliedMigrations[0]
        }

    /**
     * Check if schema migration version table has a baseline marker.
     *
     * @return `true` if the schema migration version table has a baseline marker.
     */
    open fun hasBaselineMarker(): Boolean {
        // GUARD: Mark as no baseline marker if tables don't exists
        if (!tablesExist()) return false

        createTablesIfNotExist()

        return findAppliedMigrations(MigrationType.BASELINE).isNotEmpty()
    }

    /**
     * Calculates the installed rank for the new migration to be inserted.
     *
     * @return The installed rank.
     */
    private fun calculateInstalledRank(): Int {
        session.execute(incrementInstalledRankStmt)
        val result = session.execute(findInstalledRankCountColQuery)

        return result.one().getLong("count").toInt()
    }

    /**
     * Calculate the rank for this new version about to be inserted.
     *
     * @param version The version to calculated for.
     * @return The rank.
     */
    private fun calculateVersionRank(version: MigrationVersion): Int {
        val versionRows = session.execute(findVersionRankQuery)

        // TODO: Refactor to idiomatic Kotlin collections method
        val migrationVersions = ArrayList<MigrationVersion>()
        val migrationMetaHolders = HashMap<String, MigrationMetaHolder>()
        for (versionRow in versionRows) {
            migrationVersions.add(MigrationVersion.fromVersion(versionRow.getString("version")))
            migrationMetaHolders.put(
                    versionRow.getString("version"),
                    MigrationMetaHolder(versionRow.getInt("version_rank"))
            )
        }

        if (migrationVersions.size == 0) {
            return 1;
        }

        Collections.sort(migrationVersions)

        // TODO: Refactor for loop with idiomatic Kotlin collection methods
        for (i in migrationVersions.indices) {
            if (version.compareTo(migrationVersions[i]) < 0) {
                return i + 1
            }
        }

        return migrationVersions.size + 1
    }

    /**
     * Schema Migration table CQL statement builder.
     *
     * @return Schema Migration table create statement.
     */
    private fun buildCreateSchemaMigrationTableStmt(): SimpleStatement {
        val stmt = SimpleStatement(
                """
                 | CREATE TABLE IF NOT EXISTS "${keyspaceConfig.name}"."${tableName}"
                 | (
                 |   version_rank   INT,
                 |   installed_rank INT,
                 |   version        TEXT,
                 |   description    TEXT,
                 |   script         TEXT,
                 |   checksum       INT,
                 |   type           TEXT,
                 |   installed_by   TEXT,
                 |   installed_on   TIMESTAMP,
                 |   execution_time INT,
                 |   success        BOOLEAN,
                 |   PRIMARY KEY (version)
                 | );
                """.trimMargin()
        )
        stmt.consistencyLevel = this.consistencyLevel
        return stmt
    }

    /**
     * Schema Migration Counter table CQL statement builder.
     *
     * @return Schema Migration Counter table create statement.
     */
    private fun buildCreateSchemaMigrationCounterTableStmt(): SimpleStatement {
        val stmt = SimpleStatement(
                """
                 | CREATE TABLE IF NOT EXISTS "${keyspaceConfig.name}"."${tableName}${COUNTS_TABLE_NAME_SUFFIX}"
                 | (
                 |   name  TEXT,
                 |   count COUNTER,
                 |   PRIMARY KEY (name)
                 | );
                """.trimMargin()
        )
        stmt.consistencyLevel = this.consistencyLevel
        return stmt
    }

    // ISSUE #17
    // ~~~~~~
    // Ref    : https://github.com/hhandoko/cassandra-migration/issues/17
    // Summary:
    //   Table check query fails following a `DROP TABLE` statement when run against embedded Cassandra and Apache
    //   Cassandra 3.7 and earlier.
    // Fix    :
    //   Use `SELECT *` rather than `SELECT count(*)` as a workaround, less efficient but universal.
    // Notes  :
    //   Can be reverted (to use count) once the affected Cassandra version has been superseded by another major
    //   version (e.g. 4.x).
    // ~~~~~~

    /**
     * Schema Migration table count / exist CQL query builder.
     *
     * @return Schema Migration table count query.
     */
    private fun buildCountSchemaMigrationTableQuery(): Select {
        val query = QueryBuilder
                .select()
                //.countAll()
                .from(keyspaceConfig.name, tableName)
        query.consistencyLevel = this.consistencyLevel
        return query
    }

    /**
     * Schema Migration Counter table count / exist CQL query builder.
     *
     * @return Schema Migration Counter table count query.
     */
    private fun buildCountSchemaMigrationCounterTableQuery(): Select {
        val query = QueryBuilder
                .select()
                //.countAll()
                .from(keyspaceConfig.name, tableName + COUNTS_TABLE_NAME_SUFFIX)
        query.consistencyLevel = this.consistencyLevel
        return query
    }

    /**
     * Insert Schema Migration record CQL statement builder.
     *
     * @return Schema Migration record insert statement.
     */
    private fun buildInsertSchemaMigrationRecordStmt(): PreparedStatement {
        val stmt = this.cachePs.prepare(
                """
                 | INSERT INTO "${keyspaceConfig.name}"."${tableName}"
                 | (
                 |   version_rank, installed_rank, version,
                 |   description, type, script,
                 |   checksum, installed_on, installed_by,
                 |   execution_time, success
                 | ) VALUES (
                 |   ?, ?, ?,
                 |   ?, ?, ?,
                 |   ?, dateOf(now()), ?,
                 |   ?, ?
                 | );
                """.trimMargin()
        )
        stmt.consistencyLevel = this.consistencyLevel
        return stmt
    }

    /**
     * Bind Schema Migration table insert CQL statement with the given params.
     *
     * @param versionRank The current version rank.
     * @param installedRank The current installed rank.
     * @param appliedMigration The current applied migration.
     * @return Bound Schema Migration record insert statement.
     */
    private fun boundInsertSchemaMigrationRecordStmt(versionRank: Int, installedRank: Int, appliedMigration: AppliedMigration): BoundStatement {
        return insertSchemaMigrationTableStmt.bind(
                versionRank,
                installedRank,
                appliedMigration.version.toString(),
                appliedMigration.description,
                appliedMigration.type!!.name,
                appliedMigration.script,
                appliedMigration.checksum,
                appliedMigration.installedBy,
                appliedMigration.executionTime,
                appliedMigration.isSuccess
        )
    }

    /**
     * Find Schema Migration table applied migrations CQL query.
     *
     * @return Schema Migration table applied migrations select query.
     */
    private fun buildFindAppliedMigrationsQuery(): Select {
        val query = QueryBuilder
                .select()
                .column("version_rank")
                .column("installed_rank")
                .column("version")
                .column("description")
                .column("type")
                .column("script")
                .column("checksum")
                .column("installed_on")
                .column("installed_by")
                .column("execution_time")
                .column("success")
                .from(keyspaceConfig.name, tableName)

        query.consistencyLevel = this.consistencyLevel
        return query
    }

    /**
     * Increment Schema Migration table installed rank CQL statement.
     *
     * @return Schema Migration table increment installed rank update statement.
     */
    private fun buildIncrementInstalledRankStmt(): SimpleStatement {
        val stmt = SimpleStatement(
                """
                 | UPDATE "${keyspaceConfig.name}"."${tableName}${COUNTS_TABLE_NAME_SUFFIX}"
                 |    SET count = count + 1
                 |  WHERE name = 'installed_rank';
                """.trimMargin()
        )
        stmt.consistencyLevel = this.consistencyLevel
        return stmt
    }

    /**
     * Find Schema Migration table installed rank count CQL query.
     *
     * @return Schema Migration table installed rank count select query.
     */
    private fun buildFindInstalledRankCountColQuery(): Select {
        val query = QueryBuilder
                .select("count")
                .from(keyspaceConfig.name, tableName + COUNTS_TABLE_NAME_SUFFIX)
        query.where(eq("name", "installed_rank"))

        query.consistencyLevel = this.consistencyLevel
        return query
    }

    /**
     * Find Schema Migration table version rank CQL query.
     *
     * @return Schema Migration table version rank select query.
     */
    private fun buildFindVersionRankQuery(): Select {
        val query = QueryBuilder
                .select()
                .column("version")
                .column("version_rank")
                .from(keyspaceConfig.name, tableName)

        query.consistencyLevel = this.consistencyLevel
        return query
    }

    /**
     * Update Schema Migration table version rank CQL query.
     *
     * @return Schema Migration table version rank update query.
     */
    private fun buildUpdateVersionRankStmt(): PreparedStatement {
        val stmt = this.cachePs.prepare(
                """
                 | UPDATE "${keyspaceConfig.name}"."${tableName}"
                 |    SET version_rank = ?
                 |  WHERE version = ?;
                """.trimMargin()
        )
        stmt.consistencyLevel = this.consistencyLevel
        return stmt
    }

    /**
     * Bind Schema Migration table version update CQL statement with the given params.
     *
     * @param versionRank The current version rank.
     * @param version The current version.
     * @return Bound Schema Migration version record update statement.
     */
    private fun boundUpdateVersionRankStmt(versionRank: Int, version: String?): BoundStatement {
        return updateVersionRankStmt.bind(versionRank, version)
    }

    /**
     * Schema migration (transient) metadata.
     *
     * @param versionRank The applied migrations version rank.
     */
    internal inner class MigrationMetaHolder(val versionRank: Int)

    /**
     * SchemaVersionDAO companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(SchemaVersionDAO::class.java)
        private val COUNTS_TABLE_NAME_SUFFIX = "_counts"
    }

}
