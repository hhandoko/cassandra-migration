/**
 * Copyright 2010-2016 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.builtamont.cassandra.migration.internal.info;

import com.builtamont.cassandra.migration.api.MigrationInfo;
import com.builtamont.cassandra.migration.api.MigrationInfoService;
import com.builtamont.cassandra.migration.api.MigrationType;
import com.builtamont.cassandra.migration.api.MigrationVersion;
import com.builtamont.cassandra.migration.api.resolver.MigrationResolver;
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration;
import com.builtamont.cassandra.migration.internal.dbsupport.SchemaVersionDAO;
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration;
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationImpl;
import com.builtamont.cassandra.migration.internal.util.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for MigrationInfoDumper.
 */
public class MigrationInfoDumperTest {
    @Test
    public void dumpEmpty() {
        String table = MigrationInfoDumper.INSTANCE.dumpToAsciiTable(new MigrationInfo[0]);
        String[] lines = StringUtils.tokenizeToStringArray(table, "\n");

        assertEquals(5, lines.length);
        for (String line : lines) {
            assertEquals(lines[0].length(), line.length());
        }
    }

    @Test
    public void dump2pending() {
        MigrationInfoService migrationInfoService =
                new MigrationInfoServiceImpl(
                        createMigrationResolver(createAvailableMigration("1"), createAvailableMigration("2.2014.09.11.55.45613")),
                        createSchemaVersionDAO(), MigrationVersion.Companion.getLATEST(), false, true);
        migrationInfoService.refresh();

        String table = MigrationInfoDumper.INSTANCE.dumpToAsciiTable(migrationInfoService.all());
        String[] lines = StringUtils.tokenizeToStringArray(table, "\n");

        assertEquals(6, lines.length);
        for (String line : lines) {
            assertEquals(lines[0].length(), line.length());
        }
    }

    /**
     * Creates a new available migration with this version.
     *
     * @param version The version of the migration.
     * @return The available migration.
     */
    private ResolvedMigration createAvailableMigration(String version) {
        ResolvedMigration migration = new ResolvedMigrationImpl();
        migration.setVersion(MigrationVersion.Companion.fromVersion(version));
        migration.setDescription("abc very very very very very very very very very very long");
        migration.setScript("x");
        migration.setType(MigrationType.CQL);
        return migration;
    }

    /**
     * Creates a migrationResolver for testing.
     *
     * @param resolvedMigrations The resolved migrations.
     * @return The migration resolver.
     */
    private MigrationResolver createMigrationResolver(final ResolvedMigration... resolvedMigrations) {
        return new MigrationResolver() {
            public List<ResolvedMigration> resolveMigrations() {
                return Arrays.asList(resolvedMigrations);
            }
        };
    }

    /**
     * Creates a metadata table for testing.
     *
     * @return The metadata table.
     */
    private SchemaVersionDAO createSchemaVersionDAO() {
        SchemaVersionDAO metaDataTable = mock(SchemaVersionDAO.class);
        when(metaDataTable.findAppliedMigrations()).thenReturn(new ArrayList<AppliedMigration>());
        return metaDataTable;
    }
}
