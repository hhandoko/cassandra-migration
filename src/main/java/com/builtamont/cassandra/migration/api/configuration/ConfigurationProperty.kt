/**
 * File     : ConfigurationProperty.kt
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
 * Cassandra migration configuration properties.
 *
 * @param namespace The property namespace.
 * @param description The property description.
 */
enum class ConfigurationProperty(val namespace: String, val description: String) {

    // Scripts configuration properties
    // ~~~~~~
    SCRIPTS_ENCODING(
            "cassandra.migration.scripts.encoding",
            "Encoding for CQL scripts"
    ),

    SCRIPTS_LOCATIONS(
            "cassandra.migration.scripts.locations",
            "Locations of the migration scripts in CSV format"
    ),

    TABLE_PREFIX(
            "cassandra.migration.table.prefix",
            "Prefix to be prepended to cassandra_migration_version* table names"
    ),

    ALLOW_OUT_OF_ORDER(
            "cassandra.migration.scripts.allowoutoforder",
            "Allow out of order migration"
    ),

    // Version target configuration properties
    // ~~~~~~
    TARGET_VERSION(
            "cassandra.migration.version.target",
            "The target version. Migrations with a higher version number will be ignored."
    ),

    // Cluster configuration properties
    // ~~~~~~
    CONTACT_POINTS(
            "cassandra.migration.cluster.contactpoints",
            "Comma separated values of node IP addresses"
    ),

    PORT(
            "cassandra.migration.cluster.port",
            "CQL native transport port"
    ),

    USERNAME(
            "cassandra.migration.cluster.username",
            "Username for password authenticator"
    ),

    PASSWORD(
            "cassandra.migration.cluster.password",
            "Password for password authenticator"
    ),

    TRUSTSTORE(
            "cassandra.migration.cluster.truststore",
            "Path to the truststore for client SSL"
    ),

    TRUSTSTORE_PASSWORD(
            "cassandra.migration.cluster.truststore_password",
            "Password for the truststore"
    ),

    KEYSTORE(
            "cassandra.migration.cluster.keystore",
            "Path to the keystore for client SSL certificate authentication"
    ),

    KEYSTORE_PASSWORD(
            "cassandra.migration.cluster.keystore_password",
            "Password for the keystore"
    ),

    // Keyspace name configuration properties
    // ~~~~~~
    KEYSPACE_NAME(
            "cassandra.migration.keyspace.name",
            "Name of Cassandra keyspace"
    )

}
