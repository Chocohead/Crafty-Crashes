package com.chocohead.cc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import com.chocohead.cc.smap.FileInfo;
import com.chocohead.cc.smap.LineInfo;
import com.chocohead.cc.smap.SMAP;

public class SMAPper {
	private static final Field MIXINS = FieldUtils.getDeclaredField(ClassInfo.class, "mixins", true);

	private static boolean hasMixins(ClassInfo type) {
		try {
			return !((Set<?>) MIXINS.get(type)).isEmpty();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to find mixins for " + type.getName(), e);
		}
	}

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
					ClassInfo info = ClassInfo.fromCache(className);

					if (info != null && hasMixins(info)) {
						try (InputStream in = SMAPper.class.getResourceAsStream('/' + className.replace('.', '/') + ".class")) {
							String value = findSource(in);

							if (value != null && value.startsWith("SMAP")) {
								smap = SMAP.forResolved(value);
							}
						} catch (IOException e) {
							throw new RuntimeException("Error finding class source of " + className, e);
						}
					}
				}

				cache.put(className, smap);
			} else {
				smap = cache.get(className);
			}

			if (smap != null) {
				LineInfo realLine = smap.getDefaultStratum().mapLine(element.getLineNumber());

				if (realLine != null && smap.generatedFileName.equals(element.getFileName())) {
					elements[i] = new StackTraceElement(element.getClassName(), element.getMethodName(), realLine.file.getBestName("Unspecified?").concat(findMixin(realLine.file)), realLine.line);
					modified = true;
				}
			}
		}

		return modified;
	}

	private static String findMixin(FileInfo info) {
		if (info.path != null && info.path.endsWith(".java")) {//This is very silly but also useful
			IMixinInfo mixin = Iterables.getOnlyElement(Mixins.getMixinsForClass(info.path.substring(0, info.path.length() - 5)));

			if (mixin != null) return " [" + mixin.getConfig().getName() + ']';
		}

		return "";
	}

	private static String findSource(InputStream in) throws IOException {
		String[] out = new String[1];

		new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM7) {
			@Override
			public void visitSource(String source, String debug) {
				out[0] = debug;
			}
		}, ClassReader.SKIP_CODE);

		return out[0];
	}
}