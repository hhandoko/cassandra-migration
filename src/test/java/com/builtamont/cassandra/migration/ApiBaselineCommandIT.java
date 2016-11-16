/**
 * File     : ApiBaselineCommandIT.java
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
package com.builtamont.cassandra.migration;

import com.builtamont.cassandra.migration.api.CassandraMigrationException;
import com.builtamont.cassandra.migration.api.MigrationVersion;
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO;
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration;
import com.datastax.driver.core.Session;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ApiBaselineCommandIT extends BaseIT {

    @Test
    public void baseline_cmd_before_migration_should_mark_at_first_migration_script() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.baseline();

        SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.Companion.getCURRENT().getTable());
        AppliedMigration baselineMarker = schemaVersionDAO.getBaselineMarker();
        assertThat(baselineMarker.getVersion(), is(MigrationVersion.Companion.fromVersion("1")));
    }

    @Test
    public void baseline_cmd_with_table_prefix_before_migration_should_mark_at_first_migration_script() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.setTablePrefix("test1_");
        cm.baseline();

        SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(getSession(), getKeyspace(), cm.getTablePrefix() + MigrationVersion.Companion.getCURRENT().getTable());
        AppliedMigration baselineMarker = schemaVersionDAO.getBaselineMarker();
        assertThat(baselineMarker.getVersion(), is(MigrationVersion.Companion.fromVersion("1")));
    }

    @Test
    public void baseline_cmd_with_ext_session_before_migration_should_mark_at_first_migration_script() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession();
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.baseline(session);

        SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.Companion.getCURRENT().getTable());
        AppliedMigration baselineMarker = schemaVersionDAO.getBaselineMarker();
        assertThat(baselineMarker.getVersion(), is(MigrationVersion.Companion.fromVersion("1")));
    }

    @Test
    public void baseline_cmd_with_ext_session_and_defaulted_keyspace_config_before_migration_should_mark_at_first_migration_script() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession(getKeyspace());
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.baseline(session);

        SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(getSession(), getKeyspace(), MigrationVersion.Companion.getCURRENT().getTable());
        AppliedMigration baselineMarker = schemaVersionDAO.getBaselineMarker();
        assertThat(baselineMarker.getVersion(), is(MigrationVersion.Companion.fromVersion("1")));
    }

    @Test
    public void baseline_cmd_with_ext_session_and_table_prefix_before_migration_should_mark_at_first_migration_script() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession();
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.setTablePrefix("test2_");
        cm.baseline(session);

        SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(getSession(), getKeyspace(), cm.getTablePrefix() + MigrationVersion.Companion.getCURRENT().getTable());
        AppliedMigration baselineMarker = schemaVersionDAO.getBaselineMarker();
        assertThat(baselineMarker.getVersion(), is(MigrationVersion.Companion.fromVersion("1")));
    }

    @Test
    public void baseline_cmd_with_ext_session_and_defaulted_keyspace_config_and_table_prefix_before_migration_should_mark_at_first_migration_script() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession(getKeyspace());
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setTablePrefix("test2_");
        cm.baseline(session);

        SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(getSession(), getKeyspace(), cm.getTablePrefix() + MigrationVersion.Companion.getCURRENT().getTable());
        AppliedMigration baselineMarker = schemaVersionDAO.getBaselineMarker();
        assertThat(baselineMarker.getVersion(), is(MigrationVersion.Companion.fromVersion("1")));
    }

    @Test(expected = CassandraMigrationException.class)
    public void baseline_cmd_should_throw_exception_when_baselining_after_successful_migration() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate();

        cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.baseline();
    }

    @Test(expected = CassandraMigrationException.class)
    public void baseline_cmd_with_ext_session_should_throw_exception_when_baselining_after_successful_migration() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession();
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate(session);

        cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.baseline(session);
    }

    @Test(expected = CassandraMigrationException.class)
    public void baseline_cmd_with_ext_session_and_defaulted_keyspace_config_should_throw_exception_when_baselining_after_successful_migration() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession();
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.migrate(session);

        cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.baseline(session);
    }

}
