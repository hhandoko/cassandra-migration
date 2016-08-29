/**
 * File     : MigrationResolver.kt
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
package com.builtamont.cassandra.migration.api.resolver

/**
 * Resolves available migrations. This interface can be implemented to create custom resolvers. A custom resolver
 * can be used to create additional types of migrations not covered by the standard resolvers (jdbc, sql, spring-jdbc).
 */
interface MigrationResolver {

    /**
     * Resolves the available migrations.
     *
     * @return The available migrations.
     */
    fun resolveMigrations(): Collection<ResolvedMigration>

}
