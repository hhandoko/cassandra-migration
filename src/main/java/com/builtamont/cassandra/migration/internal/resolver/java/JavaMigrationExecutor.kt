/**
 * File     : JavaMigrationExecutor.kt
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
package com.builtamont.cassandra.migration.internal.resolver.java

import com.builtamont.cassandra.migration.api.CassandraMigrationException
import com.builtamont.cassandra.migration.api.migration.java.JavaMigration
import com.builtamont.cassandra.migration.api.resolver.MigrationExecutor
import com.datastax.driver.core.Session

/**
 * Adapter for executing migrations implementing JavaMigration.
 *
 * @param javaMigration The Java-based migration to execute.
 */
class JavaMigrationExecutor(private val javaMigration: JavaMigration) : MigrationExecutor {

    /**
     * Execute the Java driver-based migration.
     *
     * @param session The Cassandra session connection to use to execute the migration.
     * @throws CassandraMigrationException when the execution of the migration failed.
     */
    @Throws(CassandraMigrationException::class)
    override fun execute(session: Session) {
        try {
            javaMigration.migrate(session)
        } catch (e: Exception) {
            throw CassandraMigrationException("Migration failed !", e)
        }
    }

}
