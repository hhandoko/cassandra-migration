/**
 * File     : CassandraMigration.java
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
package com.contrastsecurity.cassandra.migration;

import com.contrastsecurity.cassandra.migration.api.CassandraMigrationException;
import com.contrastsecurity.cassandra.migration.api.MigrationVersion;
import com.contrastsecurity.cassandra.migration.api.configuration.CassandraMigrationConfiguration;
import com.contrastsecurity.cassandra.migration.api.resolver.MigrationResolver;
import com.contrastsecurity.cassandra.migration.config.Keyspace;
import com.contrastsecurity.cassandra.migration.config.MigrationConfigs;
import com.contrastsecurity.cassandra.migration.internal.info.MigrationInfoServiceImpl;
import com.contrastsecurity.cassandra.migration.internal.util.ScriptsLocations;
import com.contrastsecurity.cassandra.migration.internal.dbsupport.SchemaVersionDAO;
import com.contrastsecurity.cassandra.migration.api.MigrationInfoService;
import com.contrastsecurity.cassandra.migration.internal.command.Initialize;
import com.contrastsecurity.cassandra.migration.internal.command.Migrate;
import com.contrastsecurity.cassandra.migration.internal.command.Validate;
import com.contrastsecurity.cassandra.migration.internal.util.logging.Log;
import com.contrastsecurity.cassandra.migration.internal.util.logging.LogFactory;
import com.contrastsecurity.cassandra.migration.internal.resolver.CompositeMigrationResolver;
import com.contrastsecurity.cassandra.migration.utils.VersionPrinter;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 * This is the centre point of Cassandra migration, and for most users, the only class they will ever have to deal with.
 *
 * It is THE public API from which all important Cassandra migration functions such as clean, validate and migrate can be called.
 */
public class CassandraMigration implements CassandraMigrationConfiguration {

    private static final Log LOG = LogFactory.INSTANCE.getLog(CassandraMigration.class);

    /**
     * The ClassLoader to use for resolving migrations on the classpath.
     * (default: Thread.currentThread().getContextClassLoader())
     */
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    /**
     * The Cassandra keyspace to connect to.
     */
    private Keyspace keyspace;

    /**
     * The Cassandra migration configuration.
     */
    private MigrationConfigs configs;

    /**
     * Creates a new Cassandra migration.
     */
    public CassandraMigration() {
        this.keyspace = new Keyspace();
        this.configs = new MigrationConfigs();
    }

    /**
     * Gets the ClassLoader to use for resolving migrations on the classpath.
     *
     * @return The ClassLoader to use for resolving migrations on the classpath.
     *         (default: Thread.currentThread().getContextClassLoader())
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the ClassLoader to use for resolving migrations on the classpath.
     *
     * @param classLoader The ClassLoader to use for resolving migrations on the classpath.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Gets the Cassandra keyspace to connect to.
     *
     * @return The Cassandra keyspace to connect to.
     */
    public Keyspace getKeyspace() {
        return keyspace;
    }

    /**
     * Sets the Cassandra keyspace to connect to.
     *
     * @param keyspace The Cassandra keyspace to connect to.
     */
    public void setKeyspace(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    /**
     * Gets the Cassandra migration configuration.
     *
     * @return The Cassandra migration configuration.
     */
    public MigrationConfigs getConfigs() {
        return configs;
    }

    /**
     * Starts the database migration. All pending migrations will be applied in order.
     * Calling migrate on an up-to-date database has no effect.
     *
     * @return The number of successfully applied migrations.
     */
    public int migrate() {
        return execute(new Action<Integer>() {
            public Integer execute(Session session) {
                new Initialize().run(session, keyspace, MigrationVersion.Companion.getCURRENT().getTable());

                MigrationResolver migrationResolver = createMigrationResolver();
                SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(session, keyspace, MigrationVersion.Companion.getCURRENT().getTable());
                Migrate migrate = new Migrate(migrationResolver, configs.getTarget(), schemaVersionDAO, session,
                        keyspace.getCluster().getUsername(), configs.isAllowOutOfOrder());

                return migrate.run();
            }
        });
    }

    /**
     * Retrieves the complete information about all the migrations including applied, pending and current migrations with
     * details and status.
     *
     * @return All migrations sorted by version, oldest first.
     */
    public MigrationInfoService info() {
        return execute(new Action<MigrationInfoService>() {
            public MigrationInfoService execute(Session session) {
                MigrationResolver migrationResolver = createMigrationResolver();
                SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(session, keyspace, MigrationVersion.Companion.getCURRENT().getTable());
                MigrationInfoService migrationInfoService = new MigrationInfoServiceImpl(migrationResolver, schemaVersionDAO, configs.getTarget(), false, true);
                migrationInfoService.refresh();

                return migrationInfoService;
            }
        });
    }

    /**
     * Validate applied migrations against resolved ones (on the filesystem or classpath)
     * to detect accidental changes that may prevent the schema(s) from being recreated exactly.
     *
     * Validation fails if:
     *  * differences in migration names, types or checksums are found
     *  * versions have been applied that aren't resolved locally anymore
     *  * versions have been resolved that haven't been applied yet
     */
    public void validate() {
        String validationError = execute(new Action<String>() {
            @Override
            public String execute(Session session) {
                MigrationResolver migrationResolver = createMigrationResolver();
                SchemaVersionDAO schemaVersionDao = new SchemaVersionDAO(session, keyspace, MigrationVersion.Companion.getCURRENT().getTable());
                Validate validate = new Validate(migrationResolver, configs.getTarget(), schemaVersionDao, true, false);
                return validate.run();
            }
        });
    
        if (validationError != null) {
            throw new CassandraMigrationException("Validation failed. " + validationError);
        }
    }

    /**
     * Baselines an existing database, excluding all migrations up to and including baselineVersion.
     */
    public void baseline() {
        // TODO: Create the Cassandra migration implementation, refer to existing PR: https://github.com/Contrast-Security-OSS/cassandra-migration/pull/17
        throw new NotImplementedException();
    }

    /**
     * Executes this command with proper resource handling and cleanup.
     *
     * @param action The action to execute.
     * @param <T> The action result type.
     * @return The action result.
     */
    <T> T execute(Action<T> action) {
        T result;

        VersionPrinter.printVersion(classLoader);

        com.datastax.driver.core.Cluster cluster = null;
        Session session = null;
        try {
            if (null == keyspace)
                throw new IllegalArgumentException("Unable to establish Cassandra session. Keyspace is not configured.");

            if (null == keyspace.getCluster())
                throw new IllegalArgumentException("Unable to establish Cassandra session. Cluster is not configured.");

            com.datastax.driver.core.Cluster.Builder builder = new com.datastax.driver.core.Cluster.Builder();
            builder.addContactPoints(keyspace.getCluster().getContactpoints()).withPort(keyspace.getCluster().getPort());
            if (null != keyspace.getCluster().getUsername() && !keyspace.getCluster().getUsername().trim().isEmpty()) {
                if (null != keyspace.getCluster().getPassword() && !keyspace.getCluster().getPassword().trim().isEmpty()) {
                    builder.withCredentials(keyspace.getCluster().getUsername(),
                            keyspace.getCluster().getPassword());
                } else {
                    throw new IllegalArgumentException("Password must be provided with username.");
                }
            }
            cluster = builder.build();

            Metadata metadata = cluster.getMetadata();
            LOG.info(getConnectionInfo(metadata));

            session = cluster.newSession();
            if (null == keyspace.getName() || keyspace.getName().trim().length() == 0)
                throw new IllegalArgumentException("Keyspace not specified.");
            List<KeyspaceMetadata> keyspaces = metadata.getKeyspaces();
            boolean keyspaceExists = false;
            for (KeyspaceMetadata keyspaceMetadata : keyspaces) {
                if (keyspaceMetadata.getName().equalsIgnoreCase(keyspace.getName()))
                    keyspaceExists = true;
            }
            if (keyspaceExists)
                session.execute("USE " + keyspace.getName());
            else
                throw new CassandraMigrationException("Keyspace: " + keyspace.getName() + " does not exist.");

            result = action.execute(session);
        } finally {
            if (null != session && !session.isClosed())
                try {
                    session.close();
                } catch(Exception e) {
                    LOG.warn("Error closing Cassandra session");
                }
            if (null != cluster && !cluster.isClosed())
                try {
                    cluster.close();
                } catch(Exception e) {
                    LOG.warn("Error closing Cassandra cluster");
                }
        }
        return result;
    }

    /**
     * Get Cassandra connection information.
     *
     * @param metadata The connected cluster metadata.
     * @return Cluster connection information.
     */
    private String getConnectionInfo(Metadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("Connected to cluster: ");
        sb.append(metadata.getClusterName());
        sb.append("\n");
        for (Host host : metadata.getAllHosts()) {
            sb.append("Data center: ");
            sb.append(host.getDatacenter());
            sb.append("; Host: ");
            sb.append(host.getAddress());
        }
        return sb.toString();
    }

    /**
     * Creates the MigrationResolver.
     *
     * @return A new, fully configured, MigrationResolver instance.
     */
    private MigrationResolver createMigrationResolver() {
        return new CompositeMigrationResolver(classLoader, new ScriptsLocations(configs.getScriptsLocations()), configs.getEncoding());
    }

    /**
     * A Cassandra migration action that can be executed.
     *
     * @param <T> The action result type.
     */
    interface Action<T> {

        /**
         * Execute the operation.
         *
         * @param session The Cassandra session connection to use to execute the migration.
         * @return The action result.
         */
        T execute(Session session);

    }

}
