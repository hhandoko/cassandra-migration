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

import com.builtamont.cassandra.migration.api.configuration.ConfigurationProperty
import com.builtamont.cassandra.migration.api.configuration.KeyspaceConfiguration
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

/**
 * KeyspaceConfiguration unit tests.
 */
@RunWith(JUnitPlatform::class)
class KeyspaceConfigurationSpec : Spek({

    val defaultProperties = System.getProperties()

    /**
     * Clear test-related System properties.
     */
    fun clearTestProperties() {
        System.clearProperty(ConfigurationProperty.KEYSPACE_NAME.namespace)
    }

    beforeEach {
        clearTestProperties()
    }

    afterEach {
        clearTestProperties()
        System.setProperties(defaultProperties)
    }

    describe("KeyspaceConfiguration") {

        context("default values") {

            val keyspaceConfig = KeyspaceConfiguration()

            it("should have no keyspace name as the default") {
                keyspaceConfig.name shouldMatch absent()
            }

            it("should have default cluster object") {
                keyspaceConfig.clusterConfig shouldMatch present()
            }

        }

        context("provided System properties values") {

            it("should allow keyspace name override") {
                System.setProperty(ConfigurationProperty.KEYSPACE_NAME.namespace, "myspace")
                KeyspaceConfiguration().name shouldMatch equalTo("myspace")
            }

        }

    }

})