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

import com.builtamont.cassandra.migration.api.MigrationType;
import com.builtamont.cassandra.migration.api.MigrationVersion;
import com.builtamont.cassandra.migration.api.resolver.ResolvedMigration;
import com.builtamont.cassandra.migration.internal.metadatatable.AppliedMigration;
import com.builtamont.cassandra.migration.internal.resolver.ResolvedMigrationImpl;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MigrationInfoTest {
    @Test
    public void validate() {
        MigrationVersion version = MigrationVersion.Companion.fromVersion("1");
        String description = "test";
        String user = "testUser";
        MigrationType type = MigrationType.CQL;

        ResolvedMigration resolvedMigration = new ResolvedMigrationImpl();
        resolvedMigration.setVersion(version);
        resolvedMigration.setDescription(description);
        resolvedMigration.setType(type);
        resolvedMigration.setChecksum(456);

        AppliedMigration appliedMigration = new AppliedMigration(version, description, type, null, 123, user, 0, true);

        MigrationInfoImpl migrationInfo =
                new MigrationInfoImpl(resolvedMigration, appliedMigration, new MigrationInfoContext());
        String message = migrationInfo.validate();

        assertTrue(message.contains("123"));
        assertTrue(message.contains("456"));
    }
}
