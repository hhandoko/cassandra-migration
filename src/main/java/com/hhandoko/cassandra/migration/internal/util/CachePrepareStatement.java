/**
 * File     : CachePrepareStatement.java
 * License  :
 *   Original   - Copyright (c) 2015 - 2016 Contrast Security
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

import java.util.concurrent.ConcurrentHashMap;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

/**
 * Cassandra prepared statement cache utility.
 */
public class CachePrepareStatement {
    /**
     * Prepared statement cache.
     */
    private ConcurrentHashMap<Integer, PreparedStatement> cacheStatement = new ConcurrentHashMap<>();

    /**
     * Current connection session.
     */
    private CqlSession session;

    /**
     * Creates a new instance of this class.
     *
     * @param session The Cassandra driver connection session.
     */
    public CachePrepareStatement(CqlSession session) {
        this.session = session;
    }

    /**
     * Prepare the given prepared statement string.
     * Retrieve it from cache if exists, or prepare and store it in cache for later use.
     *
     * @param s The prepared statement string.
     * @return PreparedStatement.
     */
    public PreparedStatement prepare(String s) {
        Integer hash = s.hashCode();
        PreparedStatement ps = cacheStatement.get(hash);
        if (ps == null) {
            ps = session.prepare(s);
            cacheStatement.put(hash, ps);
        }
        return ps;
    }
}
