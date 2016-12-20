/**
 * File     : ClusterConfigurationSpec.kt
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
package com.builtamont.cassandra.migration.config

import com.builtamont.cassandra.migration.api.configuration.ClusterConfiguration
import com.builtamont.cassandra.migration.api.configuration.ConfigurationProperty
import io.kotlintest.specs.FreeSpec
import java.nio.file.Paths
import java.util.*

/**
 * ClusterConfiguration unit tests.
 */
class ClusterConfigurationSpec : FreeSpec() {

    val defaultProperties: Properties? = System.getProperties()

    /**
     * Clear test-related System properties.
     */
    fun clearTestProperties() {
        System.clearProperty(ConfigurationProperty.CONTACT_POINTS.namespace)
        System.clearProperty(ConfigurationProperty.PORT.namespace)
        System.clearProperty(ConfigurationProperty.USERNAME.namespace)
        System.clearProperty(ConfigurationProperty.PASSWORD.namespace)
        System.clearProperty(ConfigurationProperty.TRUSTSTORE.namespace)
        System.clearProperty(ConfigurationProperty.TRUSTSTORE_PASSWORD.namespace)
        System.clearProperty(ConfigurationProperty.KEYSTORE.namespace)
        System.clearProperty(ConfigurationProperty.KEYSTORE_PASSWORD.namespace)
    }

    override fun beforeEach() {
        clearTestProperties()
    }

    override fun afterEach() {
        clearTestProperties()
        System.setProperties(defaultProperties)
    }

    init {

        "ClusterConfiguration" - {

            "given default values" - {

                val clusterConfig = ClusterConfiguration()
                // NOTE: Two hosts for contact points unit test, as in Travis CI the Cassandra hosts are set to
                //       127.0.0.1 for standalone DSE Community and Apache Cassandra, and localhost for embedded
                //       Cassandra.
                val hosts = arrayOf("localhost", "127.0.0.1")

                "should have default contact points" {
                    forAny(hosts) { host ->
                        clusterConfig.contactpoints.toList() should contain(host)
                    }
                }

                "should have default port" {
                    clusterConfig.port shouldBe 9042
                }

                "should have no default username" {
                    clusterConfig.username shouldBe null
                }

                "should have no default password" {
                    clusterConfig.password shouldBe null
                }

                "should have no default truststore" {
                    clusterConfig.truststore shouldBe null
                }

                "should have no default truststore password" {
                    clusterConfig.truststorePassword shouldBe null
                }

                "should have no default keystore" {
                    clusterConfig.keystore shouldBe null
                }

                "should have no default keystore password" {
                    clusterConfig.keystorePassword shouldBe null
                }
            }

            "provided System properties values" - {

                val contactPoints = arrayOf("192.168.0.1", "192.168.0.2", "192.168.0.3")
                val port = 9144
                val username = "user"
                val password = "pass"
                val truststore = "truststore.jks"
                val truststorePassword = "pass"
                val keystore = "keystore.jks"
                val keystorePassword = "pass"

                "should allow contact points override" {
                    System.setProperty(ConfigurationProperty.CONTACT_POINTS.namespace, contactPoints.joinToString())
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.contactpoints.size shouldBe contactPoints.size
                    clusterConfig.contactpoints.toSet() shouldBe contactPoints.toSet()
                }

                "should allow port override" {
                    System.setProperty(ConfigurationProperty.PORT.namespace, port.toString())
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.port shouldBe port
                }

                "should allow username override" {
                    System.setProperty(ConfigurationProperty.USERNAME.namespace, username)
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.username shouldBe username
                }

                "should allow password override" {
                    System.setProperty(ConfigurationProperty.PASSWORD.namespace, password)
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.password shouldBe password
                }

                "should allow truststore override" {
                    System.setProperty(ConfigurationProperty.TRUSTSTORE.namespace, truststore)
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.truststore shouldBe Paths.get(truststore)
                }

                "should allow truststore password override" {
                    System.setProperty(ConfigurationProperty.TRUSTSTORE_PASSWORD.namespace, truststorePassword)
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.truststorePassword shouldBe truststorePassword
                }

                "should allow keystore override" {
                    System.setProperty(ConfigurationProperty.KEYSTORE.namespace, keystore)
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.keystore shouldBe Paths.get(keystore)
                }

                "should allow keystore password override" {
                    System.setProperty(ConfigurationProperty.KEYSTORE_PASSWORD.namespace, keystorePassword)
                    val clusterConfig = ClusterConfiguration()
                    clusterConfig.keystorePassword shouldBe keystorePassword
                }

            }

        }

    }

}
