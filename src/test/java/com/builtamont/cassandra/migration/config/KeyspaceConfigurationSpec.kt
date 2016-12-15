/**
 * File     : KeyspaceConfigurationSpec.kt
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
import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration
import io.kotlintest.matchers.be
import io.kotlintest.specs.FreeSpec
import java.util.*

/**
 * KeyspaceConfiguration unit tests.
 */
class KeyspaceConfigurationSpec : FreeSpec() {

    val defaultProperties: Properties? = System.getProperties()

    /**
     * Clear test-related System properties.
     */
    fun clearTestProperties() {
        System.clearProperty(ConfigurationProperty.KEYSPACE_NAME.namespace)
    }

    override fun beforeEach() {
        clearTestProperties()
    }

    override fun afterEach() {
        clearTestProperties()
        System.setProperties(defaultProperties)
    }

    init {

        "KeyspaceConfiguration" - {

            "given default values" - {

                val keyspaceConfig = KeyspaceConfiguration()

                "should have no keyspace name as the default" {
                    keyspaceConfig.name shouldBe null
                }

                "should have default cluster object" {
                    keyspaceConfig.clusterConfig should be a ClusterConfiguration::class
                }

            }

            "provided System properties values" - {

                "should allow keyspace name override" {
                    System.setProperty(ConfigurationProperty.KEYSPACE_NAME.namespace, "myspace")
                    KeyspaceConfiguration().name shouldBe "myspace"
                }

            }

        }

    }

}