/**
 * File     : SchemaVersionDAO.Java
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
package com.builtamont.cassandra.migration.internal.dbsupport;

import com.builtamont.cassandra.migration.api.MigrationType;
import com.builtamont.cassandra.migration.api.MigrationVersion;
import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration;
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration;
import com.builtamont.cassandra.migration.internal.util.CachePrepareStatement;
import com.builtamont.cassandra.migration.internal.util.logging.Log;
import com.builtamont.cassandra.migration.internal.util.logging.LogFactory;
import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

/**
 * Schema migrations table Data Access Object.
 */
// TODO: Convert to Kotlin code... Some challenges with Mockito mocking :)
public class SchemaVersionDAO {

    private static final Log LOG = LogFactory.INSTANCE.getLog(SchemaVersionDAO.class);
    private static final String COUNTS_TABLE_NAME_SUFFIX = "_counts";

    private Session session;
    private KeyspaceConfiguration keyspaceConfig;
    private String tableName;
    private CachePrepareStatement cachePs;
    private ConsistencyLevel consistencyLevel;

    /**
     * Creates a new schema version DAO.
     *
     * @param session The Cassandra session connection to use to execute the migration.
     * @param keyspaceConfig The Cassandra keyspace to connect to.
     * @param tableName The Cassandra migration version table name.
     */
    public SchemaVersionDAO(Session session, KeyspaceConfiguration keyspaceConfig, String tableName) {
        this.session = session;
        this.keyspaceConfig = keyspaceConfig;
        this.tableName = tableName;
        this.cachePs = new CachePrepareStatement(session);

        // If running on a single host, don't force ConsistencyLevel.ALL
        boolean isClustered = session.getCluster().getMetadata().getAllHosts().size() > 1;
        this.consistencyLevel = isClustered ? ConsistencyLevel.ALL : ConsistencyLevel.ONE;
    }

    public String getTableName() {
        return tableName;
    }

    public KeyspaceConfiguration getKeyspaceConfig() {
        return this.keyspaceConfig;
    }

    /**
     * Create schema migration version table if it does not exists.
     */
    public void createTablesIfNotExist() {
        if (tablesExist()) {
            return;
        }

        Statement statement = new SimpleStatement(
                "CREATE TABLE IF NOT EXISTS " + keyspaceConfig.getName() + "." + tableName + "(" +
                        "  version_rank int," +
                        "  installed_rank int," +
                        "  version text," +
                        "  description text," +
                        "  script text," +
                        "  checksum int," +
                        "  type text," +
                        "  installed_by text," +
                        "  installed_on timestamp," +
                        "  execution_time int," +
                        "  success boolean," +
                        "  PRIMARY KEY (version)" +
                        ");");
        statement.setConsistencyLevel(this.consistencyLevel);
        session.execute(statement);

        statement = new SimpleStatement(
                "CREATE TABLE IF NOT EXISTS " + keyspaceConfig.getName() + "." + tableName + COUNTS_TABLE_NAME_SUFFIX + " (" +
                        "  name text," +
                        "  count counter," +
                        "  PRIMARY KEY (name)" +
                        ");");
        statement.setConsistencyLevel(this.consistencyLevel);
        session.execute(statement);
    }

    /**
     * Check if schema migration version table has already been created.
     *
     * @return {@code true} if schema migration version table exists in the keyspace.
     */
    public boolean tablesExist() {
        boolean schemaVersionTableExists = false;
        boolean schemaVersionCountsTableExists = false;

        // ISSUE #17
        // ~~~~~~
        // Ref    : https://github.com/builtamont-oss/cassandra-migration/issues/17
        // Summary:
        //   Table check query fails following a `DROP TABLE` statement when run against embedded Cassandra and Apache
        //   Cassandra 3.7 and earlier.
        // Fix    :
        //   Use `SELECT *` rather than `SELECT count(*)` as a workaround, less efficient but universal.
        // Notes  :
        //   Can be reverted (to use count) once the affected Cassandra version has been superseded by another major
        //   version (e.g. 4.x).
        // ~~~~~~
        Statement schemaVersionStatement = QueryBuilder
                .select()
                //.countAll()
                .from(keyspaceConfig.getName(), tableName);

        Statement schemaVersionCountsStatement = QueryBuilder
                .select()
                //.countAll()
                .from(keyspaceConfig.getName(), tableName + COUNTS_TABLE_NAME_SUFFIX);

        schemaVersionStatement.setConsistencyLevel(this.consistencyLevel);
        schemaVersionCountsStatement.setConsistencyLevel(this.consistencyLevel);

        try {
            ResultSet resultsSchemaVersion = session.execute(schemaVersionStatement);
            if (resultsSchemaVersion.one() != null) {
                schemaVersionTableExists = true;
            }
        } catch (InvalidQueryException e) {
            LOG.debug("No schema version table found with a name of " + tableName);
        }

        try {
            ResultSet resultsSchemaVersionCounts = session.execute(schemaVersionCountsStatement);
            if (resultsSchemaVersionCounts.one() != null) {
                schemaVersionCountsTableExists = true;
            }
        } catch (InvalidQueryException e) {
            LOG.debug("No schema version counts table found with a name of " + tableName + COUNTS_TABLE_NAME_SUFFIX);
        }

        return schemaVersionTableExists && schemaVersionCountsTableExists;
    }

    /**
     * Add applied migration record into the schema migration version table.
     *
     * @param appliedMigration The applied migration.
     */
    public void addAppliedMigration(AppliedMigration appliedMigration) {
        createTablesIfNotExist();

        MigrationVersion version = appliedMigration.getVersion();

        int versionRank = calculateVersionRank(version);
        PreparedStatement statement = cachePs.prepare(
                "INSERT INTO " + keyspaceConfig.getName() + "." + tableName + "(" +
                        "  version_rank, installed_rank, version," +
                        "  description, type, script," +
                        "  checksum, installed_on, installed_by," +
                        "  execution_time, success" +
                        ") VALUES (" +
                        "  ?, ?, ?," +
                        "  ?, ?, ?," +
                        "  ?, dateOf(now()), ?," +
                        "  ?, ?" +
                        ");"
        );

        statement.setConsistencyLevel(this.consistencyLevel);
        session.execute(statement.bind(
                versionRank,
                calculateInstalledRank(),
                version.toString(),
                appliedMigration.getDescription(),
                appliedMigration.getType().name(),
                appliedMigration.getScript(),
                appliedMigration.getChecksum(),
                appliedMigration.getInstalledBy(),
                appliedMigration.getExecutionTime(),
                appliedMigration.isSuccess()
        ));
        LOG.debug("Schema version table " + tableName + " successfully updated to reflect changes");
    }

    /**
     * Retrieve the applied migrations from the schema migration version table.
     *
     * @return The applied migrations.
     */
    public List<AppliedMigration> findAppliedMigrations() {
        if (!tablesExist()) {
            return new ArrayList<>();
        }

        Select select = QueryBuilder
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
                .from(keyspaceConfig.getName(), tableName);

        select.setConsistencyLevel(this.consistencyLevel);
        ResultSet results = session.execute(select);
        List<AppliedMigration> resultsList = new ArrayList<>();
        for (Row row : results) {
            resultsList.add(new AppliedMigration(
                    row.getInt("version_rank"),
                    row.getInt("installed_rank"),
                    MigrationVersion.Companion.fromVersion(row.getString("version")),
                    row.getString("description"),
                    MigrationType.valueOf(row.getString("type")),
                    row.getString("script"),
                    row.isNull("checksum") ? null : row.getInt("checksum"),
                    row.getTimestamp("installed_on"),
                    row.getString("installed_by"),
                    row.getInt("execution_time"),
                    row.getBool("success")
            ));
        }

        // NOTE: Order by `version_rank` not necessary here, as it eventually gets saved in TreeMap
        //       that uses natural ordering
        return resultsList;
    }

    /**
     * Retrieve the applied migrations from the metadata table.
     *
     * @param migrationTypes The migration types to find.
     * @return The applied migrations.
     */
    public List<AppliedMigration> findAppliedMigrations(MigrationType... migrationTypes) {
        if (!tablesExist()) {
            return new ArrayList<>();
        }

        Select select = QueryBuilder
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
                .from(keyspaceConfig.getName(), tableName);

        select.setConsistencyLevel(ConsistencyLevel.ALL);
        ResultSet results = session.execute(select);
        List<AppliedMigration> resultsList = new ArrayList<>();
        List<MigrationType> migTypeList = Arrays.asList(migrationTypes);
        for (Row row : results) {
            MigrationType migType = MigrationType.valueOf(row.getString("type"));
            if(migTypeList.contains(migType)){
                resultsList.add(new AppliedMigration(
                        row.getInt("version_rank"),
                        row.getInt("installed_rank"),
                        MigrationVersion.Companion.fromVersion(row.getString("version")),
                        row.getString("description"),
                        migType,
                        row.getString("script"),
                        row.getInt("checksum"),
                        row.getTimestamp("installed_on"),
                        row.getString("installed_by"),
                        row.getInt("execution_time"),
                        row.getBool("success")
                ));
            }
        }

        // NOTE: Order by `version_rank` not necessary here, as it eventually gets saved in TreeMap
        //       that uses natural ordering
        return resultsList;
    }

    /**
     * Check if the keyspace has applied migrations.
     *
     * @return {@code true} if the keyspace has applied migrations.
     */
    public boolean hasAppliedMigrations() {
        if (!tablesExist()) {
            return false;
        }

        createTablesIfNotExist();
        List<AppliedMigration> filteredMigrations = new ArrayList<>();
        List<AppliedMigration> appliedMigrations = findAppliedMigrations();
        for (AppliedMigration appliedMigration : appliedMigrations) {
            if (!appliedMigration.getType().equals(MigrationType.BASELINE)) {
                filteredMigrations.add(appliedMigration);
            }
        }
        return !filteredMigrations.isEmpty();
    }

    /**
     * Add a baseline version marker.
     *
     * @param baselineVersion The baseline version.
     * @param baselineDescription the baseline version description.
     * @param user The user's username executing the baselining.
     */
    public void addBaselineMarker(final MigrationVersion baselineVersion, final String baselineDescription, final String user) {
        addAppliedMigration(
            new AppliedMigration(
                baselineVersion,
                baselineDescription,
                MigrationType.BASELINE,
                baselineDescription,
                0,
                user,
                0,
                true
            )
        );
    }

    /**
     * Get the baseline marker's applied migration.
     *
     * @return The baseline marker's applied migration.
     */
    public AppliedMigration getBaselineMarker() {
        List<AppliedMigration> appliedMigrations = findAppliedMigrations(MigrationType.BASELINE);
        return appliedMigrations.isEmpty() ? null : appliedMigrations.get(0);
    }

    /**
     * Check if schema migration version table has a baseline marker.
     *
     * @return {@code true} if the schema migration version table has a baseline marker.
     */
    public boolean hasBaselineMarker() {
        if (!tablesExist()) {
            return false;
        }

        createTablesIfNotExist();

        return !findAppliedMigrations(MigrationType.BASELINE).isEmpty();
    }

    /**
     * Calculates the installed rank for the new migration to be inserted.
     *
     * @return The installed rank.
     */
    private int calculateInstalledRank() {
        Statement statement = new SimpleStatement(
                "UPDATE " + keyspaceConfig.getName() + "." + tableName + COUNTS_TABLE_NAME_SUFFIX +
                        " SET count = count + 1" +
                        " WHERE name = 'installed_rank'" +
                        ";");

        session.execute(statement);

        Select select = QueryBuilder
                .select("count")
                .from(keyspaceConfig.getName(), tableName + COUNTS_TABLE_NAME_SUFFIX);
        select.where(eq("name", "installed_rank"));

        select.setConsistencyLevel(this.consistencyLevel);
        ResultSet result = session.execute(select);

        return (int) result.one().getLong("count");
    }

    /**
     * Calculate the rank for this new version about to be inserted.
     *
     * @param version The version to calculated for.
     * @return The rank.
     */
    private int calculateVersionRank(MigrationVersion version) {
        Statement statement = QueryBuilder
                .select()
                .column("version")
                .column("version_rank")
                .from(keyspaceConfig.getName(), tableName);

        statement.setConsistencyLevel(this.consistencyLevel);
        ResultSet versionRows = session.execute(statement);

        List<MigrationVersion> migrationVersions = new ArrayList<>();
        HashMap<String, MigrationMetaHolder> migrationMetaHolders = new HashMap<>();
        for (Row versionRow : versionRows) {
            migrationVersions.add(MigrationVersion.Companion.fromVersion(versionRow.getString("version")));
            migrationMetaHolders.put(versionRow.getString("version"), new MigrationMetaHolder(
                    versionRow.getInt("version_rank")
            ));
        }

        Collections.sort(migrationVersions);

        BatchStatement batchStatement = new BatchStatement();
        PreparedStatement preparedStatement = cachePs.prepare(
                "UPDATE " + keyspaceConfig.getName() + "." + tableName +
                        " SET version_rank = ?" +
                        " WHERE version = ?" +
                        ";");

        for (int i = 0; i < migrationVersions.size(); i++) {
            if (version.compareTo(migrationVersions.get(i)) < 0) {
                for (int z = i; z < migrationVersions.size(); z++) {
                    String migrationVersionStr = migrationVersions.get(z).getVersion();
                    batchStatement.add(preparedStatement.bind(
                            migrationMetaHolders.get(migrationVersionStr).getVersionRank() + 1,
                            migrationVersionStr));
                    batchStatement.setConsistencyLevel(this.consistencyLevel);
                }
                return i + 1;
            }
        }
        session.execute(batchStatement);

        return migrationVersions.size() + 1;
    }

    /**
     * Schema migration (transient) metadata.
     */
    class MigrationMetaHolder {
        private int versionRank;

        public MigrationMetaHolder(int versionRank) {
            this.versionRank = versionRank;
        }

        public int getVersionRank() {
            return versionRank;
        }
    }

}
