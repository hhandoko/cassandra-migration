/**
 * File     : JavaUtilLogCreator.kt
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
import com.builtamont.cassandra.migration.internal.util.logging.LogCreator

import java.util.logging.Logger

/**
 * Log Creator for java.util.logging.
 */
class JavaUtilLogCreator : LogCreator {
    override fun createLogger(clazz: Class<*>): Log {
        return JavaUtilLog(Logger.getLogger(clazz.name))
    }
}
