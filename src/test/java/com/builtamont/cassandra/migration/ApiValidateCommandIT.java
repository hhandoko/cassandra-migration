/**
 * File     : ApiValidateCmdMigrationIT.java
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
import com.builtamont.cassandra.migration.api.MigrationInfoService;
import com.datastax.driver.core.Session;
import org.junit.Assert;
import org.junit.Test;

public class ApiValidateCommandIT extends BaseIT {

    @Test
    public void validate_cmd_should_throw_exception_when_invalid_migration_scripts_are_provided() {
        // apply migration scripts
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate();

        MigrationInfoService infoService = cm.info();
        String validationError = infoService.validate();
        Assert.assertNull(validationError);

        cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());

        cm.validate();

        cm = new CassandraMigration();
        cm.setLocations(new String[] { "migration/integ/java" });
        cm.setKeyspaceConfig(getKeyspace());

        try {
            cm.validate();
            Assert.fail("The expected CassandraMigrationException was not raised");
        } catch (CassandraMigrationException e) {
            Assert.assertTrue("expected CassandraMigrationException", true);
        }
    }

    @Test
    public void validate_cmd_with_ext_session_should_throw_exception_when_invalid_migration_scripts_are_provided() {
        // apply migration scripts
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession();
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate(session);

        MigrationInfoService infoService = cm.info(session);
        String validationError = infoService.validate();
        Assert.assertNull(validationError);

        cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());

        cm.validate(session);

        cm = new CassandraMigration();
        cm.setLocations(new String[] { "migration/integ/java" });
        cm.setKeyspaceConfig(getKeyspace());

        try {
            cm.validate(session);
            Assert.fail("The expected CassandraMigrationException was not raised");
        } catch (CassandraMigrationException e) {
        }

        Assert.assertFalse(session.isClosed());
    }

    @Test
    public void validate_cmd_with_ext_session_and_defaulted_keyspace_config_should_throw_exception_when_invalid_migration_scripts_are_provided() {
        // apply migration scripts
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        Session session = getSession();
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.migrate(session);

        MigrationInfoService infoService = cm.info(session);
        String validationError = infoService.validate();
        Assert.assertNull(validationError);

        cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);

        cm.validate(session);

        cm = new CassandraMigration();
        cm.setLocations(new String[] { "migration/integ/java" });

        try {
            cm.validate(session);
            Assert.fail("The expected CassandraMigrationException was not raised");
        } catch (CassandraMigrationException e) {
        }

        Assert.assertFalse(session.isClosed());
    }

}
