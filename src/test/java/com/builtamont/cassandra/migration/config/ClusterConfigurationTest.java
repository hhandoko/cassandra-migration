package com.builtamont.cassandra.migration.config;

import com.builtamont.cassandra.migration.api.configuration.ClusterConfiguration;
import com.builtamont.cassandra.migration.api.configuration.ConfigurationProperty;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ClusterConfigurationTest {

    @Test
    public void shouldHaveDefaultConfigValues() {
        ClusterConfiguration clusterConfig = new ClusterConfiguration();

        // NOTE: Properties below are checked before any assertions, as integration tests allows these properties to be
        //       overridden (thus assertion will fail).
        //       It should be OK as Travis' test matrix should cover these different conditions (e.g. where System
        //       properties are provided, and where default values should be used instead).

        if (hasProperty("cassandra.migration.keyspace.name"))
            assertThat(clusterConfig.getContactpoints()[0], is("localhost"));

        if (hasProperty("cassandra.migration.cluster.port"))
            assertThat(clusterConfig.getPort(), is(9042));

        if (hasProperty("cassandra.migration.cluster.username"))
            assertThat(clusterConfig.getUsername(), is(nullValue()));

        if (hasProperty("cassandra.migration.cluster.password"))
            assertThat(clusterConfig.getPassword(), is(nullValue()));
    }

    @Test
    public void systemPropsShouldOverrideDefaultConfigValues() {
        System.setProperty(ConfigurationProperty.CONTACT_POINTS.getNamespace(), "192.168.0.1,192.168.0.2, 192.168.0.3");
        System.setProperty(ConfigurationProperty.PORT.getNamespace(), "9144");
        System.setProperty(ConfigurationProperty.USERNAME.getNamespace(), "user");
        System.setProperty(ConfigurationProperty.PASSWORD.getNamespace(), "pass");

        ClusterConfiguration clusterConfig = new ClusterConfiguration();
        assertThat(clusterConfig.getContactpoints()[0], is("192.168.0.1"));
        assertThat(clusterConfig.getContactpoints()[1], is("192.168.0.2"));
        assertThat(clusterConfig.getContactpoints()[2], is("192.168.0.3"));
        assertThat(clusterConfig.getPort(), is(9144));
        assertThat(clusterConfig.getUsername(), is("user"));
        assertThat(clusterConfig.getPassword(), is("pass"));
    }

    /**
     * Checks whether the given System property is defined.
     *
     * @param key The property key.
     * @return True if the System property is defined.
     */
    private boolean hasProperty(String key) {
        return System.getProperty(key) != null;
    }

}
