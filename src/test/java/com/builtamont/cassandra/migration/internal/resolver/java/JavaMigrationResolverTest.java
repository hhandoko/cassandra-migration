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
package com.builtamont.cassandra.migration.internal.resolver.java;

import com.builtamont.cassandra.migration.api.CassandraMigrationException;
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration;
import com.builtamont.cassandra.migration.internal.resolver.java.dummy.V2__InterfaceBasedMigration;
import com.builtamont.cassandra.migration.internal.resolver.java.dummy.Version3dot5;
import com.builtamont.cassandra.migration.internal.util.ScriptsLocation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for JavaMigrationResolver.
 */
public class JavaMigrationResolverTest {
    @Test(expected = CassandraMigrationException.class)
    public void broken() {
        new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), new ScriptsLocation("com/builtamont/cassandra/migration/internal/resolver/java/error")).resolveMigrations();
    }

    @Test
    public void resolveMigrations() {
        JavaMigrationResolver jdbcMigrationResolver =
                new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), new ScriptsLocation("com/builtamont/cassandra/migration/internal/resolver/java/dummy"));
        Collection<ResolvedMigration> migrations = jdbcMigrationResolver.resolveMigrations();

        assertEquals(3, migrations.size());

        List<ResolvedMigration> migrationList = new ArrayList<ResolvedMigration>(migrations);

        ResolvedMigration migrationInfo = migrationList.get(0);
        Assert.assertEquals("2", migrationInfo.getVersion().toString());
        Assert.assertEquals("InterfaceBasedMigration", migrationInfo.getDescription());
        Assert.assertEquals(new Integer(0), migrationInfo.getChecksum());

        ResolvedMigration migrationInfo1 = migrationList.get(1);
        Assert.assertEquals("3.5", migrationInfo1.getVersion().toString());
        Assert.assertEquals("Three Dot Five", migrationInfo1.getDescription());
        Assert.assertEquals(35, migrationInfo1.getChecksum().intValue());

        ResolvedMigration migrationInfo2 = migrationList.get(2);
        Assert.assertEquals("4", migrationInfo2.getVersion().toString());
    }

    @Test
    public void conventionOverConfiguration() {
        JavaMigrationResolver jdbcMigrationResolver = new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), null);
        ResolvedMigration migrationInfo = jdbcMigrationResolver.extractMigrationInfo(new V2__InterfaceBasedMigration());
        Assert.assertEquals("2", migrationInfo.getVersion().toString());
        Assert.assertEquals("InterfaceBasedMigration", migrationInfo.getDescription());
        Assert.assertEquals(new Integer(0), migrationInfo.getChecksum());
    }

    @Test
    public void explicitInfo() {
        JavaMigrationResolver jdbcMigrationResolver = new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), null);
        ResolvedMigration migrationInfo = jdbcMigrationResolver.extractMigrationInfo(new Version3dot5());
        Assert.assertEquals("3.5", migrationInfo.getVersion().toString());
        Assert.assertEquals("Three Dot Five", migrationInfo.getDescription());
        Assert.assertEquals(35, migrationInfo.getChecksum().intValue());
    }
}
