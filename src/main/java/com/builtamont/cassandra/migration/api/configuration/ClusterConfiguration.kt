/**
 * File     : ClusterConfiguration.kt
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
 * Cluster configuration.
 */
class ClusterConfiguration {

    /**
     * Cluster configuration properties.
     */
    enum class ClusterProperty constructor(val prefix: String, val description: String) {
        CONTACTPOINTS(PROPERTY_PREFIX + "contactpoints", "Comma separated values of node IP addresses"),
        PORT(PROPERTY_PREFIX + "port", "CQL native transport port"),
        USERNAME(PROPERTY_PREFIX + "username", "Username for password authenticator"),
        PASSWORD(PROPERTY_PREFIX + "password", "Password for password authenticator")
    }

    /**
     * Cluster contact points.
     * (default: ["localhost"])
     */
    var contactpoints = arrayOf("localhost")
      get set

    /**
     * Cluster connection port.
     * (default: 9042)
     */
    var port = 9042
      get set

    /**
     * The username to connect to the cluster.
     */
    var username: String? = null
      get set

    /**
     * The password to connect to the cluster.
     */
    var password: String? = null
      get set

    init {
        val contactpointsP = System.getProperty(ClusterProperty.CONTACTPOINTS.prefix)
        if (null != contactpointsP && contactpointsP.trim { it <= ' ' }.length != 0)
            this.contactpoints = contactpointsP.replace("\\s+".toRegex(), "").split("[,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val portP = System.getProperty(ClusterProperty.PORT.prefix)
        if (null != portP && portP.trim { it <= ' ' }.length != 0)
            this.port = Integer.parseInt(portP)

        val usernameP = System.getProperty(ClusterProperty.USERNAME.prefix)
        if (null != usernameP && usernameP.trim { it <= ' ' }.length != 0)
            this.username = usernameP

        val passwordP = System.getProperty(ClusterProperty.PASSWORD.prefix)
        if (null != passwordP && passwordP.trim { it <= ' ' }.length != 0)
            this.password = passwordP
    }

    /**
     * ClusterConfiguration companion object.
     */
    companion object {
        private val PROPERTY_PREFIX = "cassandra.migration.cluster."
    }

}
