package com.chocohead.cc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

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
					Set<IMixinInfo> mixins = Mixins.getMixinsForClass(className);

					if (!mixins.isEmpty()) {
						try (InputStream in = SMAPper.class.getResourceAsStream('/' + className.replace('.', '/') + ".class")) {
							String value = findSource(in);

							if (value != null && value.startsWith("SMAP")) {
								smap = SMAP.forResolved(value, mixins.stream().collect(Collectors.toMap(IMixinInfo::getClassRef, Function.identity())));
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
					elements[i] = new StackTraceElement(element.getClassName(), element.getMethodName(), realLine.file.getBestName("Unspecified?"), realLine.line);
					modified = true;
				}
			}
		}

		return modified;
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