package com.builtamont.cassandra.migration.config;

import com.builtamont.cassandra.migration.api.configuration.ConfigurationProperty;
import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class KeyspaceConfigurationTest {
    @Test
    public void shouldDefaultToNoKeyspaceButCanBeOverridden() {
        assertThat(new KeyspaceConfiguration().getName(), is(nullValue()));
        System.setProperty(ConfigurationProperty.KEYSPACE_NAME.getNamespace(), "myspace");
        assertThat(new KeyspaceConfiguration().getName(), is("myspace"));
    }

    @Test
    public void shouldHaveDefaultClusterObject() {
        assertThat(new KeyspaceConfiguration().getClusterConfig(), is(notNullValue()));
    }
}
