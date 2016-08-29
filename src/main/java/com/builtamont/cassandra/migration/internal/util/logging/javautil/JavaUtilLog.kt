/**
 * File     : JavaUtilLog.kt
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
package com.builtamont.cassandra.migration.internal.util.logging.javautil

import com.builtamont.cassandra.migration.internal.util.logging.Log

import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
 * Wrapper for a java.util.Logger.
 *
 * @param logger The original java.util Logger.
 */
class JavaUtilLog(private val logger: Logger) : Log {

    override fun debug(message: String) {
        log(Level.FINE, message, null)
    }

    override fun info(message: String) {
        log(Level.INFO, message, null)
    }

    override fun warn(message: String) {
        log(Level.WARNING, message, null)
    }

    override fun error(message: String) {
        log(Level.SEVERE, message, null)
    }

    override fun error(message: String, e: Exception) {
        log(Level.SEVERE, message, e)
    }

    /**
     * Log the message at the specified level with the specified exception if any.
     *
     * @param level The level to log at.
     * @param message The message to log.
     * @param e The exception, if any.
     */
    private fun log(level: Level, message: String, e: Exception?) {
        // millis and thread are filled by the constructor
        val record = LogRecord(level, message)
        record.loggerName = logger.name
        record.thrown = e
        record.sourceClassName = logger.name
        record.sourceMethodName = methodName
        logger.log(record)
    }

    /**
     * Computes the source method name for the log output.
     */
    private val methodName: String?
        get() {
            return Throwable().stackTrace.first { it.className == logger.name }.methodName
        }
}
