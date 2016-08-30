/**
 * File     : CqlMigrationExecutor.kt
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
package com.builtamont.cassandra.migration.internal.resolver.cql

import com.builtamont.cassandra.migration.api.resolver.MigrationExecutor
import com.builtamont.cassandra.migration.internal.dbsupport.CqlScript
import com.builtamont.cassandra.migration.internal.util.scanner.Resource
import com.datastax.driver.core.Session

/**
 * Database migration based on a cql file.
 *
 * @param cqlScriptResource The resource containing the cql script.
 *                          The complete CQL script is not held as a member field here because this would use the total
 *                          size of all CQL migrations files in heap space during db migration.
 * @param encoding The encoding of this CQL migration.
*/
class CqlMigrationExecutor(
    private val cqlScriptResource: Resource,
    private val encoding: String
) : MigrationExecutor {

    /**
     * Execute the CQL-based migration.
     *
     * @param session The Cassandra session connection to use to execute the migration.
     */
    override fun execute(session: Session) {
        val cqlScript = CqlScript(cqlScriptResource, encoding)
        cqlScript.execute(session)
    }

}
