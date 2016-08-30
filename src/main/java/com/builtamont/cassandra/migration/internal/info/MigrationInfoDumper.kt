/**
 * File     : MigrationInfoDumper.kt
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
package com.builtamont.cassandra.migration.internal.info

import com.builtamont.cassandra.migration.api.MigrationInfo
import com.builtamont.cassandra.migration.internal.util.DateUtils
import com.builtamont.cassandra.migration.internal.util.StringUtils

/**
 * Dumps migrations in an ASCII-art table in the logs and the console.
 */
object MigrationInfoDumper {

    private val VERSION_TITLE = "Version"
    private val DESCRIPTION_TITLE = "Description"

    /**
     * Dumps the info about all migrations into an ASCII table.
     *
     * @param migrationInfos The list of migrationInfos to dump.
     * @return The ASCII table, as one big multi-line string.
     */
    fun dumpToAsciiTable(migrationInfos: Array<MigrationInfo>): String {
        var versionWidth = VERSION_TITLE.length
        var descriptionWidth = DESCRIPTION_TITLE.length

        for (migrationInfo in migrationInfos) {
            versionWidth = Math.max(versionWidth, migrationInfo.version.toString().length)
            descriptionWidth = Math.max(descriptionWidth, migrationInfo.description.length)
        }

        val ruler = "+-" + StringUtils.trimOrPad("", versionWidth, '-') +
            "-+-" + StringUtils.trimOrPad("", descriptionWidth, '-') + "-+---------------------+---------+\n"

        val table = StringBuilder()
        table.append(ruler)
        table.append("| ").append(StringUtils.trimOrPad(VERSION_TITLE, versionWidth, ' ')).append(" | ").append(StringUtils.trimOrPad(DESCRIPTION_TITLE, descriptionWidth)).append(" | Installed on        | State   |\n")
        table.append(ruler)

        if (migrationInfos.size == 0) {
            table.append(StringUtils.trimOrPad("| No migrations found", ruler.length - 2, ' ')).append("|\n")
        } else {
            for (migrationInfo in migrationInfos) {
                table.append("| ").append(StringUtils.trimOrPad(migrationInfo.version.toString(), versionWidth))
                table.append(" | ").append(StringUtils.trimOrPad(migrationInfo.description, descriptionWidth))
                table.append(" | ").append(StringUtils.trimOrPad(DateUtils.formatDateAsIsoString(migrationInfo.installedOn), 19))
                table.append(" | ").append(StringUtils.trimOrPad(migrationInfo.state.displayName, 7))
                table.append(" |\n")
            }
        }

        table.append(ruler)
        return table.toString()
    }
}
