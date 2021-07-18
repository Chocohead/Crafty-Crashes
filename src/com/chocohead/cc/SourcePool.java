package com.chocohead.cc;

import java.util.HashMap;
import java.util.Map;

public class SourcePool {
	private static final Map<String, String> SOURCES = new HashMap<>();

	static void add(String owner, String source) {
		if (SOURCES.put(owner, source) != null) {
			throw new IllegalArgumentException("Duplicate source mapping for " + owner);
		}
	}

	public static String get(String owner) {
		return SOURCES.get(owner);
	}
}