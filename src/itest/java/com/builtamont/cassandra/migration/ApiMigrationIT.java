/**
 * File     : ApiMigrationIT.java
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

import com.builtamont.cassandra.migration.api.MigrationInfo;
import com.builtamont.cassandra.migration.api.MigrationInfoService;
import com.builtamont.cassandra.migration.api.MigrationType;
import com.builtamont.cassandra.migration.internal.info.MigrationInfoDumper;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ApiMigrationIT extends BaseIT {

    @Test
    public void migrations_should_run_succesfully_from_api_methods_invocation() {
        String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
        CassandraMigration cm = new CassandraMigration();
        cm.setLocations(scriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate();

        MigrationInfoService infoService = cm.info();
        System.out.println("Initial migration");
        System.out.println(MigrationInfoDumper.INSTANCE.dumpToAsciiTable(infoService.all()));
        assertThat(infoService.all().length, is(6));
        for (MigrationInfo info : infoService.all()) {
            assertThat(info.getVersion().getVersion(), anyOf(is("1.0.0"), is("1.1.0"), is("1.2.0"), is("2.0.0"), is("3.0"), is("3.0.1")));
            if (info.getVersion().equals("3.0.1")) {
                assertThat(info.getDescription(), is("Three point zero one"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.JAVA_DRIVER.name()));
                assertThat(info.getScript().contains(".java"), is(true));

                Select select = QueryBuilder.select().column("value").from("test1");
                select.where(eq("space", "web")).and(eq("key", "facebook"));
                ResultSet result = getSession().execute(select);
                assertThat(result.one().getString("value"), is("facebook.com"));
            } else if (info.getVersion().equals("3.0")) {
                assertThat(info.getDescription(), is("Third"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.JAVA_DRIVER.name()));
                assertThat(info.getScript().contains(".java"), is(true));

                Select select = QueryBuilder.select().column("value").from("test1");
                select.where(eq("space", "web")).and(eq("key", "google"));
                ResultSet result = getSession().execute(select);
                assertThat(result.one().getString("value"), is("google.com"));
            } else if (info.getVersion().equals("2.0.0")) {
                assertThat(info.getDescription(), is("Second"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.CQL.name()));
                assertThat(info.getScript().contains(".cql"), is(true));

                Select select = QueryBuilder.select().column("title").column("message").from("contents");
                select.where(eq("id", 1));
                Row row = getSession().execute(select).one();
                assertThat(row.getString("title"), is("foo"));
                assertThat(row.getString("message"), is("bar"));
            } else if (info.getVersion().equals("1.2.0")) {
                assertThat(info.getDescription(), is("First delete temp"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.CQL.name()));
                assertThat(info.getScript().contains(".cql"), is(true));

                Select select = QueryBuilder.select().from("test2");
                ResultSet result = getSession().execute(select);
                assertThat(result.one(), is(null));
            } else if (info.getVersion().equals("1.1.0")) {
                assertThat(info.getDescription(), is("First create temp"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.CQL.name()));
                assertThat(info.getScript().contains(".cql"), is(true));

                Select select = QueryBuilder.select().column("value").from("test2");
                select.where(eq("space", "foo")).and(eq("key", "bar"));
                ResultSet result = getSession().execute(select);
                assertThat(result.one().getString("value"), is("profit!"));
            } else if (info.getVersion().equals("1.0.0")) {
                assertThat(info.getDescription(), is("First"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.CQL.name()));
                assertThat(info.getScript().contains(".cql"), is(true));

                Select select = QueryBuilder.select().column("value").from("test1");
                select.where(eq("space", "foo")).and(eq("key", "bar"));
                ResultSet result = getSession().execute(select);
                assertThat(result.one().getString("value"), is("profit!"));
            }

            assertThat(info.getState().isApplied(), is(true));
            assertThat(info.getInstalledOn(), notNullValue());
        }

        // test out of order when out of order is not allowed
        String[] outOfOrderScriptsLocations = { "migration/integ_outoforder", "migration/integ/java" };
        cm = new CassandraMigration();
        cm.setLocations(outOfOrderScriptsLocations);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate();

        infoService = cm.info();
        System.out.println("Out of order migration with out-of-order ignored");
        System.out.println(MigrationInfoDumper.INSTANCE.dumpToAsciiTable(infoService.all()));
        assertThat(infoService.all().length, is(7));
        for (MigrationInfo info : infoService.all()) {
            assertThat(info.getVersion().getVersion(),
                    anyOf(is("1.0.0"), is("1.1.0"), is("1.2.0"), is("2.0.0"), is("3.0"), is("3.0.1"), is("1.1.1")));
            if (info.getVersion().equals("1.1.1")) {
                assertThat(info.getDescription(), is("Late arrival"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.CQL.name()));
                assertThat(info.getScript().contains(".cql"), is(true));
                assertThat(info.getState().isApplied(), is(false));
                assertThat(info.getInstalledOn(), nullValue());
            }
        }

        // test out of order when out of order is allowed
        String[] outOfOrder2ScriptsLocations = { "migration/integ_outoforder2", "migration/integ/java" };
        cm = new CassandraMigration();
        cm.setLocations(outOfOrder2ScriptsLocations);
        cm.setAllowOutOfOrder(true);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate();

        infoService = cm.info();
        System.out.println("Out of order migration with out-of-order allowed");
        System.out.println(MigrationInfoDumper.INSTANCE.dumpToAsciiTable(infoService.all()));
        assertThat(infoService.all().length, is(8));
        for (MigrationInfo info : infoService.all()) {
            assertThat(info.getVersion().getVersion(),
                    anyOf(is("1.0.0"), is("1.1.0"), is("1.2.0"), is("2.0.0"), is("3.0"), is("3.0.1"), is("1.1.1"), is("1.1.2")));
            if (info.getVersion().equals("1.1.2")) {
                assertThat(info.getDescription(), is("Late arrival2"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.CQL.name()));
                assertThat(info.getScript().contains(".cql"), is(true));
                assertThat(info.getState().isApplied(), is(true));
                assertThat(info.getInstalledOn(), notNullValue());
            }
        }

        // test out of order when out of order is allowed again
        String[] outOfOrder3ScriptsLocations = { "migration/integ_outoforder3", "migration/integ/java" };
        cm = new CassandraMigration();
        cm.setLocations(outOfOrder3ScriptsLocations);
        cm.setAllowOutOfOrder(true);
        cm.setKeyspaceConfig(getKeyspace());
        cm.migrate();

        infoService = cm.info();
        System.out.println("Out of order migration with out-of-order allowed");
        System.out.println(MigrationInfoDumper.INSTANCE.dumpToAsciiTable(infoService.all()));
        assertThat(infoService.all().length, is(9));
        for (MigrationInfo info : infoService.all()) {
            assertThat(info.getVersion().getVersion(),
                    anyOf(is("1.0.0"), is("1.1.0"), is("1.2.0"), is("2.0.0"), is("3.0"), is("3.0.1"), is("1.1.1"), is("1.1.2"), is("1.1.3")));
            if (info.getVersion().equals("1.1.3")) {
                assertThat(info.getDescription(), is("Late arrival3"));
                assertThat(info.getType().name(), Matchers.is(MigrationType.CQL.name()));
                assertThat(info.getScript().contains(".cql"), is(true));
                assertThat(info.getState().isApplied(), is(true));
                assertThat(info.getInstalledOn(), notNullValue());
            }
        }
    }

}
