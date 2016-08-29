package com.builtamont.cassandra.migration.internal.util;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import java.util.concurrent.ConcurrentHashMap;

public class CachePrepareStatement {
	private ConcurrentHashMap<Integer, PreparedStatement> cacheStatement = new ConcurrentHashMap<>();

	private Session session;

	public CachePrepareStatement(Session session) {
		this.session = session;
	}

	public PreparedStatement prepare(String s){
		PreparedStatement ps = cacheStatement.get(s.hashCode());
		if(ps == null){
			ps = session.prepare(s);
			cacheStatement.put(s.hashCode(), ps);
		}
		return ps;
	}
}
