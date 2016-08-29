/**
 * File     : LogFactory.kt
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
package com.builtamont.cassandra.migration.internal.util.logging

import com.builtamont.cassandra.migration.internal.util.logging.apachecommons.ApacheCommonsLogCreator
import com.builtamont.cassandra.migration.internal.util.logging.javautil.JavaUtilLogCreator
import com.builtamont.cassandra.migration.internal.util.logging.slf4j.Slf4jLogCreator

object LogFactory {

    /**
     * Factory for implementation-specific loggers.
     */
    private var _logCreator: LogCreator? = null

    /**
     * The factory for implementation-specific loggers to be used as a fallback when no other suitable loggers were found.
     */
    private var _fallbackLogCreator: LogCreator? = null

    /**
     * @param logCreator The factory for implementation-specific loggers.
     */
    fun setLogCreator(logCreator: LogCreator) {
        LogFactory._logCreator = logCreator
    }
    /**
     * @param fallbackLogCreator The factory for implementation-specific loggers
     *                           to be used as a fallback when no other suitable loggers were found.
     */
    fun setFallbackLogCreator(fallbackLogCreator: LogCreator) {
        LogFactory._fallbackLogCreator = fallbackLogCreator
    }

    /**
     * Retrieves the matching logger for this class.
     *
     * @param clazz The class to get the logger for.
     * @return The logger.
     */
    fun getLog(clazz: Class<*>): Log {
        if (_logCreator == null) {
            val classLoader = Thread.currentThread().contextClassLoader
            val featureDetector = com.builtamont.cassandra.migration.internal.util.FeatureDetector(classLoader)

            _logCreator = when {
                featureDetector.isSlf4jAvailable                -> Slf4jLogCreator()
                featureDetector.isApacheCommonsLoggingAvailable -> ApacheCommonsLogCreator()
                _fallbackLogCreator == null                     -> JavaUtilLogCreator()
                else                                            -> _fallbackLogCreator
            }
        }

        return _logCreator!!.createLogger(clazz)
    }

}
