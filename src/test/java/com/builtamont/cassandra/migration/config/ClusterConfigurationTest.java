package com.builtamont.cassandra.migration.config;

import com.builtamont.cassandra.migration.api.configuration.ClusterConfiguration;
import com.builtamont.cassandra.migration.api.configuration.ConfigurationProperty;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ClusterConfigurationTest {

    @Test
    public void shouldHaveDefaultConfigValues() {
        ClusterConfiguration clusterConfig = new ClusterConfiguration();

        // NOTE: Properties below are checked before any assertions, as integration tests allows these properties to be
        //       overridden (thus assertion will fail).
        //       It should be OK as Travis' test matrix should cover these different conditions (e.g. where System
        //       properties are provided, and where default values should be used instead).

        if (hasProperty(ConfigurationProperty.CONTACT_POINTS.getNamespace()))
            assertThat(clusterConfig.getContactpoints()[0], either(is("localhost")).or(is("127.0.0.1")));

        if (hasProperty(ConfigurationProperty.PORT.getNamespace()))
            assertThat(clusterConfig.getPort(), is(9042));

        if (hasProperty(ConfigurationProperty.USERNAME.getNamespace()))
            assertThat(clusterConfig.getUsername(), is(nullValue()));

        if (hasProperty(ConfigurationProperty.PASSWORD.getNamespace()))
            assertThat(clusterConfig.getPassword(), is(nullValue()));

        if (hasProperty("cassandra.migration.cluster.truststore"))
            assertThat(clusterConfig.getTruststore(), is(nullValue()));

        if (hasProperty("cassandra.migration.cluster.truststorePassword"))
            assertThat(clusterConfig.getTruststorePassword(), is(nullValue()));

        if (hasProperty("cassandra.migration.cluster.keystore"))
            assertThat(clusterConfig.getKeystore(), is(nullValue()));

        if (hasProperty("cassandra.migration.cluster.keystorePassword"))
            assertThat(clusterConfig.getKeystorePassword(), is(nullValue()));
    }

    @Test
    public void systemPropsShouldOverrideDefaultConfigValues() {
        System.setProperty(ConfigurationProperty.CONTACT_POINTS.getNamespace(), "192.168.0.1,192.168.0.2,192.168.0.3");
        System.setProperty(ConfigurationProperty.PORT.getNamespace(), "9144");
        System.setProperty(ConfigurationProperty.USERNAME.getNamespace(), "user");
        System.setProperty(ConfigurationProperty.PASSWORD.getNamespace(), "pass");
        System.setProperty(ConfigurationProperty.TRUSTSTORE.getNamespace(), "truststore.jks");
        System.setProperty(ConfigurationProperty.TRUSTSTORE_PASSWORD.getNamespace(), "pass");
        System.setProperty(ConfigurationProperty.KEYSTORE.getNamespace(), "keystore.jks");
        System.setProperty(ConfigurationProperty.KEYSTORE_PASSWORD.getNamespace(), "pass");

        ClusterConfiguration clusterConfig = new ClusterConfiguration();
        assertThat(clusterConfig.getContactpoints()[0], is("192.168.0.1"));
        assertThat(clusterConfig.getContactpoints()[1], is("192.168.0.2"));
        assertThat(clusterConfig.getContactpoints()[2], is("192.168.0.3"));
        assertThat(clusterConfig.getPort(), is(9144));
        assertThat(clusterConfig.getUsername(), is("user"));
        assertThat(clusterConfig.getPassword(), is("pass"));
        assertThat(clusterConfig.getTruststore(), is(Paths.get("truststore.jks")));
        assertThat(clusterConfig.getTruststorePassword(), is("pass"));
        assertThat(clusterConfig.getKeystore(), is(Paths.get("keystore.jks")));
        assertThat(clusterConfig.getKeystorePassword(), is("pass"));
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
