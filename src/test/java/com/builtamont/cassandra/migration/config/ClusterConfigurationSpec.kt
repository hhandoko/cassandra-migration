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
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.nio.file.Paths

/**
 * ClusterConfiguration unit tests.
 */
@RunWith(JUnitPlatform::class)
class ClusterConfigurationSpec : Spek({

    val defaultProperties = System.getProperties()

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

    beforeEach {
        clearTestProperties()
    }

    afterEach {
        clearTestProperties()
        System.setProperties(defaultProperties)
    }

    describe("ClusterConfiguration") {

        context("default values") {

            val clusterConfig = ClusterConfiguration()

            it("should have default contact points") {
                clusterConfig.contactpoints.toList() shouldMatch anyElement(equalTo("localhost"))
            }

            it("should have default port") {
                clusterConfig.port shouldMatch equalTo(9042)
            }

            it("should have no default username") {
                clusterConfig.username shouldMatch absent()
            }

            it("should have no default password") {
                clusterConfig.password shouldMatch absent()
            }

            it("should have no default truststore") {
                clusterConfig.truststore shouldMatch absent()
            }

            it("should have no default truststore password") {
                clusterConfig.truststorePassword shouldMatch absent()
            }

            it("should have no default keystore") {
                clusterConfig.keystore shouldMatch absent()
            }

            it("should have no default keystore password") {
                clusterConfig.keystorePassword shouldMatch absent()
            }
        }

        context("provided System properties values") {

            val contactPoints = arrayOf("192.168.0.1", "192.168.0.2", "192.168.0.3")
            val port = 9144
            val username = "user"
            val password = "pass"
            val truststore = "truststore.jks"
            val truststorePassword = "pass"
            val keystore = "keystore.jks"
            val keystorePassword = "pass"

            it("should allow contact points override") {
                System.setProperty(ConfigurationProperty.CONTACT_POINTS.namespace, contactPoints.joinToString())
                val clusterConfig = ClusterConfiguration()
                clusterConfig.contactpoints.size shouldMatch equalTo(contactPoints.size)
                clusterConfig.contactpoints.toSet() shouldMatch equalTo(contactPoints.toSet())
            }

            it("should allow port override") {
                System.setProperty(ConfigurationProperty.PORT.namespace, port.toString())
                val clusterConfig = ClusterConfiguration()
                clusterConfig.port shouldMatch equalTo(port)
            }

            it("should allow username override") {
                System.setProperty(ConfigurationProperty.USERNAME.namespace, username)
                val clusterConfig = ClusterConfiguration()
                clusterConfig.username shouldMatch equalTo(username)
            }

            it("should allow password override") {
                System.setProperty(ConfigurationProperty.PASSWORD.namespace, password)
                val clusterConfig = ClusterConfiguration()
                clusterConfig.password shouldMatch equalTo(password)
            }

            it("should allow truststore override") {
                System.setProperty(ConfigurationProperty.TRUSTSTORE.namespace, truststore)
                val clusterConfig = ClusterConfiguration()
                clusterConfig.truststore shouldMatch equalTo(Paths.get(truststore))
            }

            it("should allow truststore password override") {
                System.setProperty(ConfigurationProperty.TRUSTSTORE_PASSWORD.namespace, truststorePassword)
                val clusterConfig = ClusterConfiguration()
                clusterConfig.truststorePassword shouldMatch equalTo(truststorePassword)
            }

            it("should allow keystore override") {
                System.setProperty(ConfigurationProperty.KEYSTORE.namespace, keystore)
                val clusterConfig = ClusterConfiguration()
                clusterConfig.keystore shouldMatch equalTo(Paths.get(keystore))
            }

            it("should allow keystore password override") {
                System.setProperty(ConfigurationProperty.KEYSTORE_PASSWORD.namespace, keystorePassword)
                val clusterConfig = ClusterConfiguration()
                clusterConfig.keystorePassword shouldMatch equalTo(keystorePassword)
            }

        }

    }

})
