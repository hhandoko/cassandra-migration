/**
 * File     : KeyspaceConfiguration.kt
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
package com.builtamont.cassandra.migration.api.configuration

/**
 * Keyspace configuration.
 */
class KeyspaceConfiguration {

    /**
     * Keyspace configuration properties.
     */
    enum class KeyspaceProperty constructor(val prefix: String, val description: String) {
        NAME(PROPERTY_PREFIX + "name", "Name of Cassandra keyspace")
    }

    /**
     * Cluster configuration.
     */
    lateinit var clusterConfig: ClusterConfiguration

    /**
     * Keyspace name.
     */
    var name: String? = null
      get set

    init {
        clusterConfig = ClusterConfiguration()
        val keyspaceP = System.getProperty(KeyspaceProperty.NAME.prefix)
        if (null != keyspaceP && keyspaceP.trim { it <= ' ' }.length != 0)
            this.name = keyspaceP
    }

    /**
     * KeyspaceConfiguration companion object.
     */
    companion object {
        private val PROPERTY_PREFIX = "cassandra.migration.keyspace."
    }

}
