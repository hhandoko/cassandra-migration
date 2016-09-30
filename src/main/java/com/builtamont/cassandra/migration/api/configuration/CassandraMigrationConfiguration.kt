/**
 * File     : CassandraMigrationConfiguration.kt
 * License  :
 *   Original   - Copyright (c) 2010 - 2016 Boxfuse GmbH
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

import com.builtamont.cassandra.migration.api.MigrationVersion

/**
 * Readonly interface for main Cassandra migration configuration.
 * Can be used to provide configuration data to migrations and callbacks.
 */
// TODO: Provide Cassandra migration implementation (if applicable)
interface CassandraMigrationConfiguration {

    /**
     * Retrieves the ClassLoader to use for resolving migrations on the classpath.
     *
     * @return The ClassLoader to use for resolving migrations on the classpath.
     *         (default: Thread.currentThread().getContextClassLoader())
     */
    val classLoader: ClassLoader

//    /**
//     * Retrieves the dataSource to use to access the database. Must have the necessary privileges to execute ddl.
//     *
//     * @return The dataSource to use to access the database. Must have the necessary privileges to execute ddl.
//     */
//    val dataSource: DataSource

    /**
     * Retrieves the version to tag an existing schema with when executing baseline.
     *
     * @return The version to tag an existing schema with when executing baseline.
     *         (default: 1)
     */
    val baselineVersion: MigrationVersion

    /**
     * Retrieves the description to tag an existing schema with when executing baseline.
     *
     * @return The description to tag an existing schema with when executing baseline.
     *         (default: << Cassandra Baseline >>)
     */
    val baselineDescription: String

//    /**
//     * Retrieves the the custom MigrationResolvers to be used in addition to the built-in ones for resolving migrations to apply.
//     *
//     * @return The custom MigrationResolvers to be used in addition to the built-in ones for resolving migrations to apply.
//     *         An empty array if none.
//     *         (default: none)
//     */
//    val resolvers: Array<MigrationResolver>

//    /**
//     * Whether Cassandra migration should skip the default resolvers. If true, only custom resolvers are used.
//     *
//     * @return Whether default built-in resolvers should be skipped.
//     *         (default: false)
//     */
//    val isSkipDefaultResolvers: Boolean

//    /**
//     * Gets the callbacks for lifecycle notifications.
//     *
//     * @return The callbacks for lifecycle notifications. An empty array if none.
//     *         (default: none)
//     */
//    CassandraMigrationCallback[] getCallbacks();

//    /**
//     * Whether Cassandra migration should skip the default callbacks. If true, only custom callbacks are used.
//     *
//     * @return Whether default built-in callbacks should be skipped.
//     *         (default: false)
//     */
//    val isSkipDefaultCallbacks: Boolean

//    /**
//     * Retrieves the file name suffix for CQL migrations.
//     *
//     * CQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,
//     * which using the defaults translates to V1_1__My_description.cql
//     *
//     * @return The file name suffix for CQL migrations.
//     *         (default: .cql)
//     */
//    val cqlMigrationSuffix: String

//    /**
//     * Retrieves the file name prefix for repeatable CQL migrations.
//     *
//     * Repeatable CQL migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix ,
//     * which using the defaults translates to R__My_description.cql
//     *
//     * @return The file name prefix for repeatable CQL migrations.
//     *         (default: R)
//     */
//    val repeatableCqlMigrationPrefix: String

//    /**
//     * Retrieves the file name separator for CQL migrations.
//     *
//     * CQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,
//     * which using the defaults translates to V1_1__My_description.cql
//     *
//     * @return The file name separator for CQL migrations.
//     *         (default: __)
//     */
//    val cqlMigrationSeparator: String

//    /**
//     * Retrieves the file name prefix for CQL migrations.
//     *
//     * CQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix ,
//     * which using the defaults translates to V1_1__My_description.cql
//     *
//     * @return The file name prefix for CQL migrations.
//     *         (default: V)
//     */
//    val cqlMigrationPrefix: String

//    /**
//     * Checks whether placeholders should be replaced.
//     *
//     * @return Whether placeholders should be replaced.
//     *         (default: true)
//     */
//    val isPlaceholderReplacement: Boolean

//    /**
//     * Retrieves the suffix of every placeholder.
//     *
//     * @return The suffix of every placeholder.
//     *         (default: } )
//     */
//    val placeholderSuffix: String

//    /**
//     * Retrieves the prefix of every placeholder.
//     *
//     * @return The prefix of every placeholder.
//     *         (default: ${ )
//     */
//    val placeholderPrefix: String

//    /**
//     * Retrieves the map of <placeholder, replacementValue> to apply to CQL migration scripts.
//     *
//     * @return The map of <placeholder, replacementValue> to apply to CQL migration scripts.
//     */
//    val placeholders: Map<String, String>

    /**
     * Retrieves the target version up to which Cassandra migration should consider migrations.
     * Migrations with a higher version number will be ignored.
     *
     * The special value `current` designates the current version of the schema.
     *
     * @return The target version up to which Cassandra migration should consider migrations.
     *         (default: the latest version)
     */
    val target: MigrationVersion

//    /**
//     * Retrieves the name of the schema metadata table that will be used by Cassandra migration.
//     * By default (single-schema mode) the metadata table is placed in the default schema for the connection provided by the datasource.
//     *
//     * When the *cassandramigration.schemas* property is set (multi-schema mode), the metadata table is placed in the first
//     * schema of the list.
//     *
//     * @return The name of the schema metadata table that will be used by Cassandra migration.
//     *         (default: schema_version)
//     */
//    val table: String

//    /**
//     * Retrieves the schemas managed by Cassandra migration. These schema names are case-sensitive.
//     *
//     * Consequences:
//     *  * The first schema in the list will be automatically set as the default one during the migration.
//     *  * The first schema in the list will also be the one containing the metadata table.
//     *  * The schemas will be cleaned in the order of this list.
//     *
//     * @return The schemas managed by Cassandra migration.
//     *         (default: The default schema for the datasource connection)
//     */
//    val schemas: Array<String>

    /**
     * Retrieves the encoding of CQL migrations.
     *
     * @return The encoding of CQL migrations.
     *         (default: UTF-8)
     */
    val encoding: String

    /**
     * Retrieves the locations to scan recursively for migrations.
     *
     * The location type is determined by its prefix.
     *
     * Unprefixed locations or locations starting with `classpath:` point to a package on the classpath and may
     * contain both CQL and Java-based migrations.
     *
     * Locations starting with `filesystem:` point to a directory on the filesystem and may only contain sql
     * migrations.
     *
     * @return Locations to scan recursively for migrations.
     *         (default: db/migration)
     */
    val locations: Array<String>

    /**
     * Retrieves the migration table prefix.
     *
     * The prefix will be prepended to `cassandra_migration_version*` table names.
     *
     * By providing the prefix, multiple applications can have their own migration tables tracking the migration of their
     * own cassandra database assets without interfering with each other.
     *
     * @return the prefix to be prepended to `cassandra_migration_version*` table names
     */
    val tablePrefix: String
}
