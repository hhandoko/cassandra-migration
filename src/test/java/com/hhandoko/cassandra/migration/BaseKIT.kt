/**
 * File     : BaseKIT.kt
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
 *   Derivative - Copyright (c) 2016 - 2018 cassandra-migration Contributors
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
package com.hhandoko.cassandra.migration

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.hhandoko.cassandra.migration.api.configuration.ConfigurationProperty
import com.hhandoko.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.kotlintest.specs.FreeSpec
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import java.net.InetSocketAddress

/**
 * (K)otlin (I)ntegration (T)est fixture base class.
 */
open class BaseKIT : FreeSpec() {

    /** Configuration reader instance */
    private val conf = ConfigFactory.load()

    /** Keyspace name from configuration */
    val CASSANDRA_KEYSPACE: String =
            conf.extract<String?>(ConfigurationProperty.KEYSPACE_NAME.namespace)
                ?: "cassandra_migration_test"

    /** Cluster contact point(s) from configuration */
    val CASSANDRA_CONTACT_POINT: String =
            conf.extract<String?>(ConfigurationProperty.CONTACT_POINTS.namespace)
                ?: "localhost"

    /** Cluster connection port from configuration */
    val CASSANDRA_PORT: Int =
            conf.extract<Int?>(ConfigurationProperty.PORT.namespace)
                ?: 9147

    /** Cluster credentials username */
    val CASSANDRA_USERNAME: String =
            conf.extract<String?>(ConfigurationProperty.USERNAME.namespace)
                ?: "cassandra"

    /** Cluster credentials password */
    val CASSANDRA_PASSWORD: String =
            conf.extract<String?>(ConfigurationProperty.PASSWORD.namespace)
                ?: "cassandra"

    /**
     * Flag to disable embedded Cassandra.
     *
     * Used when running integration tests against DSE Community or Apache Cassandra instances.
     */
    val DISABLE_EMBEDDED: Boolean =
            conf.extract<Boolean?>("cassandra.migration.disable_embedded")
                ?: false

    /** Cluster connection session */
    private var session: CqlSession? = null

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
        val statement = SimpleStatement.builder(cql)
        getSession(CASSANDRA_USERNAME,CASSANDRA_PASSWORD).execute(statement.build())

        // Reconnect session to the keyspace
//        session = session!!.cluster.connect(CASSANDRA_KEYSPACE)

    }

    /**
     * Drop test keyspace after each test.
     */
    override fun afterEach() {
        super.afterEach()

        val cql = """DROP KEYSPACE ${CASSANDRA_KEYSPACE};"""
        val statement = SimpleStatement.builder(cql)
        getSession(CASSANDRA_USERNAME,CASSANDRA_PASSWORD).execute(statement.build())
    }

    /**
     * Get the keyspace configuration.
     */
    protected fun getKeyspace(): KeyspaceConfiguration {
        val ks = KeyspaceConfiguration()
        ks.name = CASSANDRA_KEYSPACE
//        ks.clusterConfig.contactpoints = arrayOf(CASSANDRA_CONTACT_POINT)
//        ks.clusterConfig.port = CASSANDRA_PORT
//        ks.clusterConfig.username = CASSANDRA_USERNAME
//        ks.clusterConfig.password = CASSANDRA_PASSWORD
        return ks
    }

    /**
     * Get the active connection session.
     */
    protected fun getSession(username: String, password: String): CqlSession {
        if (session != null && !session!!.isClosed)
            return session!!


//        val username = keyspaceConfig.clusterConfig.username
//        val password = keyspaceConfig.clusterConfig.password
        val builder = CqlSession.builder()
                .withAuthCredentials(username,password)
                .addContactPoint(InetSocketAddress(CASSANDRA_CONTACT_POINT,CASSANDRA_PORT))

//        builder.addContactPoints(CASSANDRA_CONTACT_POINT)
//                .withPort(CASSANDRA_PORT)
//                .withCredentials(username, password)
//        val cluster = builder.build()
//        session = cluster.connect()
        return builder.build()
    }

    /**
     * Get the active connection session.
     */
    protected fun getSession(): CqlSession {
        return session!!
    }

}
