package com.builtamont.cassandra.migration.config;

import com.builtamont.cassandra.migration.api.configuration.ClusterConfiguration;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ClusterConfigurationTest {
    @Test
    public void shouldHaveDefaultConfigValues() {
        ClusterConfiguration clusterConfig = new ClusterConfiguration();
        assertThat(clusterConfig.getContactpoints()[0], is("localhost"));
        assertThat(clusterConfig.getPort(), is(9042));
        assertThat(clusterConfig.getUsername(), is(nullValue()));
        assertThat(clusterConfig.getPassword(), is(nullValue()));
    }

    @Test
    public void systemPropsShouldOverrideDefaultConfigValues() {
        System.setProperty(ClusterConfiguration.ClusterProperty.CONTACTPOINTS.getName(), "192.168.0.1,192.168.0.2, 192.168.0.3");
        System.setProperty(ClusterConfiguration.ClusterProperty.PORT.getName(), "9144");
        System.setProperty(ClusterConfiguration.ClusterProperty.USERNAME.getName(), "user");
        System.setProperty(ClusterConfiguration.ClusterProperty.PASSWORD.getName(), "pass");

        ClusterConfiguration clusterConfig = new ClusterConfiguration();
        assertThat(clusterConfig.getContactpoints()[0], is("192.168.0.1"));
        assertThat(clusterConfig.getContactpoints()[1], is("192.168.0.2"));
        assertThat(clusterConfig.getContactpoints()[2], is("192.168.0.3"));
        assertThat(clusterConfig.getPort(), is(9144));
        assertThat(clusterConfig.getUsername(), is("user"));
        assertThat(clusterConfig.getPassword(), is("pass"));
    }
}
