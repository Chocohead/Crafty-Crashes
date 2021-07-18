package com.chocohead.cc;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.google.common.collect.Iterables;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import com.chocohead.cc.smap.FileInfo;

public class SourcePool {
	private static final Map<String, String> SOURCES = new HashMap<>();
	private static final Map<FileInfo, IMixinInfo> MIXINS = new IdentityHashMap<>();

	static void add(String owner, String source) {
		if (SOURCES.put(owner, source) != null) {
			throw new IllegalArgumentException("Duplicate source mapping for " + owner);
		}
	}

	public static String get(String owner) {
		return SOURCES.get(owner);
	}

	public static IMixinInfo findFor(FileInfo file) {
		if (!MIXINS.containsKey(file)) {
			if (file.path != null && file.path.endsWith(".java")) {
				ClassInfo info = ClassInfo.fromCache(file.path.substring(0, file.path.length() - 5));

				if (info != null && info.isMixin()) {//This is very silly but also useful
					IMixinInfo out = Iterables.getOnlyElement(info.getAppliedMixins());
					MIXINS.put(file, out);
					return out;
				}
			}

			MIXINS.put(file, null);
			return null;			
		} else {
			return MIXINS.get(file);
		}
	}
}