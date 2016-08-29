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
package com.builtamont.cassandra.migration.internal.resolver.java.dummy;

import com.builtamont.cassandra.migration.api.MigrationVersion;
import com.builtamont.cassandra.migration.api.migration.MigrationChecksumProvider;
import com.builtamont.cassandra.migration.api.migration.MigrationInfoProvider;
import com.datastax.driver.core.Session;

/**
 * Test migration.
 */
public class Version3dot5 extends DummyAbstractJavaMigration implements MigrationInfoProvider, MigrationChecksumProvider {
    public void doMigrate(Session session) throws Exception {
        //Do nothing.
    }

    public Integer getChecksum() {
        return 35;
    }

    public MigrationVersion getVersion() {
        return MigrationVersion.Companion.fromVersion("3.5");
    }

    public String getDescription() {
        return "Three Dot Five";
    }
}
