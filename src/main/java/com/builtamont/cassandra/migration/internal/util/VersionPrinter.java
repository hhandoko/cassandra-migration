/**
 * Copyright 2010-2015 Axel Fontaine
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.builtamont.cassandra.migration.internal.util;

import com.builtamont.cassandra.migration.internal.util.logging.Log;
import com.builtamont.cassandra.migration.internal.util.logging.LogFactory;
import com.builtamont.cassandra.migration.internal.util.scanner.classpath.ClassPathResource;

/**
 * Prints the Cassandra Migration version.
 */
public class VersionPrinter {
    private static final Log LOG = LogFactory.INSTANCE.getLog(VersionPrinter.class);
    private static boolean printed;

    private VersionPrinter() {
        // Do nothing
    }

    public static void printVersion(ClassLoader classLoader) {
        if (printed) {
            return;
        }
        printed = true;
        String version = new ClassPathResource("version.txt", classLoader).loadAsString("UTF-8");
        LOG.info("Cassandra Migration " + version);
    }
}
