package com.chocohead.cc;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.chocohead.cc.smap.FileInfo;
import com.chocohead.cc.smap.LineInfo;
import com.chocohead.cc.smap.SMAP;

public class SMAPper {
	public static void apply(Throwable t, String... skippedPackages) {
		apply(t, new HashMap<String, SMAP>(), skippedPackages);
	}

	private static void apply(Throwable t, Map<String, SMAP> cache, String... skippedPackages) {
		StackTraceElement[] elements = t.getStackTrace();
		if (apply(elements, cache, skippedPackages)) t.setStackTrace(elements);

		if (t.getCause() != null) apply(t.getCause(), cache, skippedPackages);
		for (Throwable suppressed : t.getSuppressed()) {
			apply(suppressed, cache, skippedPackages);
		}
	}

	public static boolean apply(StackTraceElement[] elements, String... skippedPackages) {
		return apply(elements, new HashMap<>(), skippedPackages);
	}

	private static boolean apply(StackTraceElement[] elements, Map<String, SMAP> cache, String... skippedPackages) {
		boolean modified = false;

		for (int i = 0, end = elements.length; i < end; i++) {
			StackTraceElement element = elements[i];
			if (element.isNativeMethod() || element.getLineNumber() < 0) continue;

			String className = element.getClassName();

			SMAP smap;
			if (!cache.containsKey(className)) {
				boolean skip = false;

				for (String packageName : skippedPackages) {
					if (className.startsWith(packageName)) {
						skip = true;
						break;
					}
				}

				smap = null;
				if (!skip) {
					String source = SourcePool.get(className);

					if (source != null && source.startsWith("SMAP")) {
						smap = SMAP.forResolved(source);
					}
				}

				cache.put(className, smap);
			} else {
				smap = cache.get(className);
			}

			if (smap != null && smap.generatedFileName.equals(element.getFileName())) {
				LineInfo realLine = smap.getDefaultStratum().mapLine(element.getLineNumber());

				if (realLine != null && !realLine.file.name.equals(element.getFileName())) {
					elements[i] = new StackTraceElement(element.getClassName(), element.getMethodName(), realLine.file.getBestName("Unspecified?").concat(findMixin(realLine.file)), realLine.line);
					modified = true;
				}
			}
		}

		return modified;
	}

	private static String findMixin(FileInfo file) {
		IMixinInfo mixin = SourcePool.findFor(file);
		return mixin != null ? " [" + mixin.getConfig().getName() + ']' : "";
	}
}