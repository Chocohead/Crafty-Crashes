package com.chocohead.cc.smap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceSortedMap;

class StratumBuilder {
	private interface DelayedLine {
		void append(Int2ReferenceMap<LineInfo> lineMapping) throws IOException;
	}
	public final String id;
	private final Int2ReferenceSortedMap<FileInfo> files = new Int2ReferenceLinkedOpenHashMap<>();
	private final List<DelayedLine> lines = new ArrayList<>();

	public StratumBuilder(String id) {
		this.id = id;
	}

	void addFile(int id, String name, String path, IMixinInfo mixin) throws IOException {
		if (files.put(id, new FileInfo(name, path, mixin)) != null) {
			throw new InvalidFormat("Duplicate file ID " + id + " for " + this.id + " stratum");
		}
	}

	void addLines(int input, int inputFile, int repeat, int output, int increment) {
		lines.add(lineMapping -> {
			FileInfo file = files.get(inputFile < 0 ? files.lastIntKey() : inputFile);
			if (file == null) throw new InvalidFormat("Line mapping used unknown file ID: " + inputFile + " for " + id + " stratum");

			for (int n = 0; n < repeat; n++) {
				LineInfo info = new LineInfo(file, input + n);

				//input + n => [output + (n * increment), output + ((n + 1) * increment) - 1]
				for (int line = output + (n * increment), end = line + increment; line < end; line++) {
					if (!lineMapping.containsKey(line)) {
						lineMapping.put(line, info);
					}
				}
			}
		});
	}

	Stratum validate(boolean seenFile, boolean seenLines) throws IOException {
		if (!seenFile || !seenLines) {
			String missed;
			if (!seenFile) {
				if (!seenLines) {
					missed = "file and line sections";
				} else {
					missed = "file section";
				}
			} else {
				assert !seenLines;
				missed = "line section";
			}

			throw new InvalidFormat("Missed " + missed + " for " + id + " stratum");
		}

		Int2ReferenceMap<LineInfo> lineMapping = new Int2ReferenceOpenHashMap<>();

		for (DelayedLine delayedLine : lines) {
			delayedLine.append(lineMapping);
		}

		return new Stratum(id, lineMapping);
	}
}