/**
 * File     : BaseIT.java
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

import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class BaseIT {

    public static final String CASSANDRA_KEYSPACE =
            System.getProperty("cassandra.migration.keyspace.name") != null
                    ? System.getProperty("cassandra.migration.keyspace.name")
                    : "cassandra_migration_test";
    public static final String CASSANDRA_CONTACT_POINT =
            System.getProperty("cassandra.migration.cluster.contactpoints") != null
                    ? System.getProperty("cassandra.migration.cluster.contactpoints")
                    : "localhost";
    public static final int CASSANDRA_PORT =
            System.getProperty("cassandra.migration.cluster.port") != null
                    ? Integer.parseInt(System.getProperty("cassandra.migration.cluster.port"))
                    : 9147;
    public static final String CASSANDRA_USERNAME =
            System.getProperty("cassandra.migration.cluster.username") != null
                    ? System.getProperty("cassandra.migration.cluster.username")
                    : "cassandra";
    public static final String CASSANDRA_PASSWORD =
            System.getProperty("cassandra.migration.cluster.password") != null
                    ? System.getProperty("cassandra.migration.cluster.password")
                    : "cassandra";
    public static final boolean DISABLE_EMBEDDED =
            System.getProperty("cassandra.migration.disable_embedded") != null
                    && Boolean.parseBoolean(System.getProperty("cassandra.migration.disable_embedded"));

    private Session session;

    @BeforeClass
    public static void beforeSuite() throws Exception {
        if (DISABLE_EMBEDDED) return;

        EmbeddedCassandraServerHelper.startEmbeddedCassandra(
                "cassandra-unit.yaml",
                "target/embeddedCassandra",
                200000L);
    }

    @AfterClass
    public static void afterSuite() {
        if (DISABLE_EMBEDDED) return;

        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Before
    public void createKeyspace() {
        Statement statement = new SimpleStatement(
                "CREATE KEYSPACE " + CASSANDRA_KEYSPACE +
                        "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };"
        );
        getSession(getKeyspace()).execute(statement);

        // Reconnect session to the keyspace
        session = session.getCluster().connect(CASSANDRA_KEYSPACE);
    }

    @After
    public void dropKeyspace() {
        Statement statement = new SimpleStatement(
                "DROP KEYSPACE " + CASSANDRA_KEYSPACE + ";"
        );
        getSession(getKeyspace()).execute(statement);
    }

    protected KeyspaceConfiguration getKeyspace() {
        KeyspaceConfiguration ks = new KeyspaceConfiguration();
        ks.setName(CASSANDRA_KEYSPACE);
        ks.getClusterConfig().setContactpoints(new String[] { CASSANDRA_CONTACT_POINT });
        ks.getClusterConfig().setPort(CASSANDRA_PORT);
        ks.getClusterConfig().setUsername(CASSANDRA_USERNAME);
        ks.getClusterConfig().setPassword(CASSANDRA_PASSWORD);
        return ks;
    }

    protected Session getSession(KeyspaceConfiguration keyspaceConfig) {
        if (session != null && !session.isClosed())
            return session;

        String username = keyspaceConfig.getClusterConfig().getUsername();
        String password = keyspaceConfig.getClusterConfig().getPassword();
        Cluster.Builder builder = new Cluster.Builder();
        builder.addContactPoints(CASSANDRA_CONTACT_POINT)
                .withPort(CASSANDRA_PORT)
                .withCredentials(username, password);
        Cluster cluster = builder.build();
        session = cluster.connect();
        return session;
    }

    protected Session getSession() {
        return session;
    }
}
