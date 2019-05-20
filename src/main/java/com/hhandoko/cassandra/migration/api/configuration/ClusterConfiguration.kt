/**
 * File     : ClusterConfiguration.kt
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
package com.hhandoko.cassandra.migration.api.configuration

import com.hhandoko.cassandra.migration.internal.util.StringUtils
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Cluster configuration.
 */
class ClusterConfiguration {

    /**
     * Cluster node IP address(es).
     * (default: ["localhost"])
     */
    var contactpoints = arrayOf("localhost")
      get set

    /**
     * Cluster CQL native transport port.
     * (default: 9042)
     */
    var port = 9042
      get set

    /**
     * The username for password authenticator.
     */
    var username: String? = null
      get set

    /**
     * The password for password authenticator.
     */
    var password: String? = null
      get set

    /**
     * True to enable SSL.
     */
    var enableSsl = false
      get set

    /**
     * The path to the truststore.
     */
    var truststore: Path? = null
      get set

    /**
     * The password for the truststore.
     */
    var truststorePassword: String? = null
      get set

    /**
     * The path to the keystore.
     */
    var keystore: Path? = null
        get set

    /**
     * The password for the keystore.
     */
    var keystorePassword: String? = null
        get set

    /**
     * ClusterConfiguration initialization.
     */
    init {
        ConfigFactory.invalidateCaches()
        ConfigFactory.load().let {
            it.extract<String?>(ConfigurationProperty.CONTACT_POINTS.namespace)?.let {
                this.contactpoints = StringUtils.tokenizeToStringArray(it, ",")
            }

            it.extract<Int?>(ConfigurationProperty.PORT.namespace)?.let {
                this.port = it
            }

            it.extract<String?>(ConfigurationProperty.USERNAME.namespace)?.let {
                this.username = it.trim()
            }

            it.extract<String?>(ConfigurationProperty.PASSWORD.namespace)?.let {
                this.password = it.trim()
            }

            it.extract<Boolean?>(ConfigurationProperty.ENABLE_SSL.namespace)?.let {
                this.enableSsl = it
            }

            it.extract<String?>(ConfigurationProperty.TRUSTSTORE.namespace)?.let {
                this.truststore = Paths.get(it.trim())
            }

            it.extract<String?>(ConfigurationProperty.TRUSTSTORE_PASSWORD.namespace)?.let {
                this.truststorePassword = it.trim()
            }

            it.extract<String?>(ConfigurationProperty.KEYSTORE.namespace)?.let {
                this.keystore = Paths.get(it.trim())
            }

            it.extract<String?>(ConfigurationProperty.KEYSTORE_PASSWORD.namespace)?.let {
                this.keystorePassword = it.trim()
            }
        }
    }

}
