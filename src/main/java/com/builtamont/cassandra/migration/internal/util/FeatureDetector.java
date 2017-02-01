/**
 * File     : FeatureDetector.java
 * License  :
 *   Original   - Copyright (c) 2010 - 2016 Boxfuse GmbH
 *   Derivative - Copyright (c) 2016 - 2017 Citadel Technology Solutions Pte Ltd
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
package com.builtamont.cassandra.migration.internal.util;

/**
 * Detects whether certain features are available or not.
 */
public class FeatureDetector {
    /**
     * The ClassLoader to use.
     */
    private ClassLoader classLoader;

    /**
     * Flag indicating availability of slf4j.
     */
    private Boolean slf4jAvailable;

    /**
     * Flag indicating availability of Apache Commons logging.
     */
    private Boolean apacheCommonsLoggingAvailable;

    /**
     * Creates a new FeatureDetector.
     *
     * @param classLoader The ClassLoader to use.
     */
    public FeatureDetector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Checks whether slf4j is available.
     *
     * @return {@code true} if it is, {@code false} if it is not.
     */
    public boolean isSlf4jAvailable() {
        if (slf4jAvailable == null) {
            slf4jAvailable = ClassUtils.isPresent("org.slf4j.Logger", classLoader);
        }

        return slf4jAvailable;
    }

    /**
     * Checks whether Apache Commons Logging is available.
     *
     * @return {@code true} if it is, {@code false} if it is not.
     */
    public boolean isApacheCommonsLoggingAvailable() {
        if (apacheCommonsLoggingAvailable == null) {
            apacheCommonsLoggingAvailable = ClassUtils.isPresent("org.apache.commons.logging.Log", classLoader);
        }

        return apacheCommonsLoggingAvailable;
    }
}
