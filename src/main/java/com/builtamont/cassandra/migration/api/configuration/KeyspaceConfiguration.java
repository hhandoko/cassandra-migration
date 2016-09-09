package com.builtamont.cassandra.migration.api.configuration;

public class KeyspaceConfiguration {
    private static final String PROPERTY_PREFIX = "cassandra.migration.keyspace.";

    public enum KeyspaceProperty {
        NAME(PROPERTY_PREFIX + "name", "Name of Cassandra keyspace");

        private String name;
        private String description;

        KeyspaceProperty(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    private ClusterConfiguration clusterConfig;
    private String name;

    public KeyspaceConfiguration() {
        clusterConfig = new ClusterConfiguration();
        String keyspaceP = System.getProperty(KeyspaceProperty.NAME.getName());
        if (null != keyspaceP && keyspaceP.trim().length() != 0)
            this.name = keyspaceP;
    }

    public ClusterConfiguration getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(ClusterConfiguration clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
