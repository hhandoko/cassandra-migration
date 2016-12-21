/**
 * File     : BaseKIT.kt
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
package com.builtamont.cassandra.migration

import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session
import com.datastax.driver.core.SimpleStatement
import io.kotlintest.specs.FreeSpec
import org.cassandraunit.utils.EmbeddedCassandraServerHelper

/**
 * (K)otlin (I)ntegration (T)est fixture base class.
 */
open class BaseKIT : FreeSpec() {

    /** Keyspace name from configuration */
    val CASSANDRA_KEYSPACE =
        if (System.getProperty("cassandra.migration.keyspace.name") != null)
            System.getProperty("cassandra.migration.keyspace.name")
        else
            "cassandra_migration_test"

    /** Cluster contact point(s) from configuration */
    val CASSANDRA_CONTACT_POINT =
        if (System.getProperty("cassandra.migration.cluster.contactpoints") != null)
            System.getProperty("cassandra.migration.cluster.contactpoints")
        else
            "localhost"

    /** Cluster connection port from configuration */
    val CASSANDRA_PORT =
        if (System.getProperty("cassandra.migration.cluster.port") != null)
            Integer.parseInt(System.getProperty("cassandra.migration.cluster.port"))
        else
            9147

    /** Cluster credentials username */
    val CASSANDRA_USERNAME =
        if (System.getProperty("cassandra.migration.cluster.username") != null)
            System.getProperty("cassandra.migration.cluster.username")
        else
            "cassandra"

    /** Cluster credentials password */
    val CASSANDRA_PASSWORD =
        if (System.getProperty("cassandra.migration.cluster.password") != null)
            System.getProperty("cassandra.migration.cluster.password")
        else
            "cassandra"

    /**
     * Flag to disable embedded Cassandra.
     *
     * Used when running integration tests against DSE Community or Apache Cassandra instances.
     */
    val DISABLE_EMBEDDED =
        System.getProperty("cassandra.migration.disable_embedded") != null &&
                java.lang.Boolean.parseBoolean(System.getProperty("cassandra.migration.disable_embedded"))

    /** Cluster connection session */
    private var session: Session? = null

    /**
     * Start embedded Cassandra before all tests are run.
     */
    override fun beforeAll() {
        super.beforeAll()

        // GUARD: Early return when testing against DSE Community and Apache Cassandra
        if (DISABLE_EMBEDDED) return

        EmbeddedCassandraServerHelper.startEmbeddedCassandra(
                "cassandra-unit.yaml",
                "target/embeddedCassandra",
                200000L
        )
    }

    /**
     * Cleans embedded Cassandra after all tests are run.
     */
    override fun afterAll() {
        super.afterAll()

        // GUARD: Early return when testing against DSE Community and Apache Cassandra
        if (DISABLE_EMBEDDED) return

        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
    }

    /**
     * Create required keyspace before each test.
     */
    override fun beforeEach() {
        super.beforeEach()

        val cql =
                """
                 | CREATE KEYSPACE ${CASSANDRA_KEYSPACE}
                 | WITH REPLICATION = {
                 |     'class' : 'SimpleStrategy',
                 |     'replication_factor' : 1
                 | };
                """.trimMargin()
        val statement = SimpleStatement(cql)
        getSession(getKeyspace()).execute(statement)

        // Reconnect session to the keyspace
        session = session!!.cluster.connect(CASSANDRA_KEYSPACE)
    }

    /**
     * Drop test keyspace after each test.
     */
    override fun afterEach() {
        super.afterEach()

        val cql = """DROP KEYSPACE ${CASSANDRA_KEYSPACE};"""
        val statement = SimpleStatement(cql)
        getSession(getKeyspace()).execute(statement)
    }

    /**
     * Get the keyspace configuration.
     */
    protected fun getKeyspace(): KeyspaceConfiguration {
        val ks = KeyspaceConfiguration()
        ks.name = CASSANDRA_KEYSPACE
        ks.clusterConfig.contactpoints = arrayOf(CASSANDRA_CONTACT_POINT)
        ks.clusterConfig.port = CASSANDRA_PORT
        ks.clusterConfig.username = CASSANDRA_USERNAME
        ks.clusterConfig.password = CASSANDRA_PASSWORD
        return ks
    }

    /**
     * Get the active connection session.
     */
    protected fun getSession(keyspaceConfig: KeyspaceConfiguration): Session {
        if (session != null && !session!!.isClosed())
            return session!!

        val username = keyspaceConfig.clusterConfig.username
        val password = keyspaceConfig.clusterConfig.password
        val builder = Cluster.Builder()
        builder.addContactPoints(CASSANDRA_CONTACT_POINT)
                .withPort(CASSANDRA_PORT)
                .withCredentials(username, password)
        val cluster = builder.build()
        session = cluster.connect()
        return session!!
    }

    /**
     * Get the active connection session.
     */
    protected fun getSession(): Session {
        return session!!
    }

}