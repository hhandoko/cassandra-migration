/**
 * File     : VersionPrinter.java
 * License  :
 *   Original   - Copyright (c) 2010 - 2016 Boxfuse GmbH
 *   Derivative - Copyright (c) 2016 - 2018 cassandra-migration Contributors
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
package com.hhandoko.cassandra.migration.internal.util;

import com.hhandoko.cassandra.migration.internal.util.logging.Log;
import com.hhandoko.cassandra.migration.internal.util.logging.LogFactory;
import com.hhandoko.cassandra.migration.internal.util.scanner.classpath.ClassPathResource;

/**
 * Prints the Cassandra Migration version.
 */
public class VersionPrinter {
    /**
     * Logger instance.
     */
    private static final Log LOG = LogFactory.INSTANCE.getLog(VersionPrinter.class);

    /**
     * Flag to denote if version has been printed.
     */
    private static boolean printed;

    /**
     * Prevents instantiation.
     */
    private VersionPrinter() {
        // Do nothing
    }

    /**
     * Prints the Cassandra Migration version.
     */
    public static void printVersion() {
        if (printed) {
            return;
        }
        printed = true;
        String version = new ClassPathResource("version.txt", VersionPrinter.class.getClassLoader()).loadAsString("UTF-8");
        LOG.info("Cassandra Migration " + version);
    }
}
