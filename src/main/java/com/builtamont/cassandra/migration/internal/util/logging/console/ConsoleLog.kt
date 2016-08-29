/**
 * File     : ConsoleLog.kt
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
package com.builtamont.cassandra.migration.internal.util.logging.console

import com.builtamont.cassandra.migration.internal.util.logging.Log

/**
 * Wrapper around a simple Console output.
 *
 * @param level the log level.
 */
class ConsoleLog(private val level: ConsoleLog.Level) : Log {

    /**
     * The log level.
     */
    enum class Level {
        DEBUG, INFO, WARN
    }

    override fun debug(message: String) {
        if (level == Level.DEBUG) {
            println("DEBUG: " + message)
        }
    }

    override fun info(message: String) {
        if (level.compareTo(Level.INFO) <= 0) {
            println(message)
        }
    }

    override fun warn(message: String) {
        println("WARNING: " + message)
    }

    override fun error(message: String) {
        println("ERROR: " + message)
    }

    override fun error(message: String, e: Exception) {
        println("ERROR: " + message)
        e.printStackTrace()
    }

}
