/**
 * File     : CqlMigrationResolverTest.java
 * License  :
 *   Copyright (c) 2015 - 2016 Contrast Security
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
package com.builtamont.cassandra.migration.internal.resolver.cql;

import com.builtamont.cassandra.migration.api.CassandraMigrationException;
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration;
import com.builtamont.cassandra.migration.internal.util.ScriptsLocation;
import com.builtamont.cassandra.migration.internal.util.scanner.classpath.ClassPathResource;
import com.builtamont.cassandra.migration.internal.util.scanner.filesystem.FileSystemResource;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Testcase for CqlMigration.
 */
public class CqlMigrationResolverTest {
    @Test
    public void resolveMigrations() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("migration/subdir"), "UTF-8");
        Collection<ResolvedMigration> migrations = cqlMigrationResolver.resolveMigrations();

        assertEquals(3, migrations.size());

        List<ResolvedMigration> migrationList = new ArrayList<ResolvedMigration>(migrations);

        Assert.assertEquals("1", migrationList.get(0).getVersion().toString());
        Assert.assertEquals("1.1", migrationList.get(1).getVersion().toString());
        Assert.assertEquals("2.0", migrationList.get(2).getVersion().toString());

        Assert.assertEquals("dir1/V1__First.cql", migrationList.get(0).getScript());
        Assert.assertEquals("V1_1__Populate_table.cql", migrationList.get(1).getScript());
        Assert.assertEquals("dir2/V2_0__Add_contents_table.cql", migrationList.get(2).getScript());
    }

    @Test(expected = CassandraMigrationException.class)
    public void resolveMigrationsNonExisting() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("non/existing"), "UTF-8");

        cqlMigrationResolver.resolveMigrations();
    }

    @Test
    public void extractScriptName() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("db/migration"), "UTF-8");

        assertEquals("db_0__init.cql", cqlMigrationResolver.extractScriptName(
                new ClassPathResource("db/migration/db_0__init.cql", Thread.currentThread().getContextClassLoader())));
    }

    @Test
    public void extractScriptNameRootLocation() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(), new ScriptsLocation(""), "UTF-8");

        assertEquals("db_0__init.cql", cqlMigrationResolver.extractScriptName(
                new ClassPathResource("db_0__init.cql", Thread.currentThread().getContextClassLoader())));
    }

    @Test
    public void extractScriptNameFileSystemPrefix() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("filesystem:/some/dir"), "UTF-8");

        assertEquals("V3.171__patch.cql", cqlMigrationResolver.extractScriptName(new FileSystemResource("/some/dir/V3.171__patch.cql")));
    }
}
