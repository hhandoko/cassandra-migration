/**
 * File     : ScriptsLocations.kt
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
package com.builtamont.cassandra.migration.internal.util

import com.builtamont.cassandra.migration.internal.util.logging.LogFactory
import java.util.*

/**
 * The locations to scan recursively for migrations.
 *
 * The location type is determined by its prefix:
 *  * Unprefixed locations or locations starting with `classpath:` point to a package on the classpath and may
 *    contain both cql and java-based migrations.
 *  * Locations starting with `filesystem:` point to a directory on the filesystem and may only contain cql
 *    migrations.
 *
 * @param rawLocations The raw locations to process.
 *                     (default: db/migration)
 */
class ScriptsLocations(vararg rawLocations: String) {

    /**
     * The backing list.
     */
    private val locations = ArrayList<ScriptsLocation>()

    /**
     * ScriptsLocations initialization.
     */
    init {
        for (location in rawLocations.map { ScriptsLocation(it) }.sorted()) {
            if (locations.contains(location)) {
                LOG.warn("Discarding duplicate location '$location'")
                continue
            }

            val parentLocation = getParentLocationIfExists(location, locations)
            if (parentLocation != null) {
                LOG.warn("Discarding location '$location' as it is a sub-location of '$parentLocation'")
                continue
            }

            locations.add(location)
        }
    }

    /**
     * @return The locations.
     */
    fun getLocations(): List<ScriptsLocation> {
        return locations
    }

    /**
     * Retrieves this location's parent within this list, if any.
     *
     * @param location The location to check.
     * @param finalLocations The list to search.
     * @return The parent location. `null` if none.
     */
    private fun getParentLocationIfExists(location: ScriptsLocation, finalLocations: List<ScriptsLocation>): ScriptsLocation? {
        for (finalLocation in finalLocations) {
            if (finalLocation.isParentOf(location)) {
                return finalLocation
            }
        }
        return null
    }

    /**
     * ScriptLocations companion object.
     */
    companion object {
        private val LOG = LogFactory.getLog(ScriptsLocations::class.java)
    }

}
