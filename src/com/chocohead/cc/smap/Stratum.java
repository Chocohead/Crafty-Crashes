package com.chocohead.cc.smap;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.IntSet;

public class Stratum {
	public final String id;
	//private final Int2ReferenceMap<FileInfo> files;
	private final Int2ReferenceMap<LineInfo> lineMapping;

	Stratum(String id, Int2ReferenceMap<LineInfo> lineMapping) {
		this.id = id;
		this.lineMapping = lineMapping;
	}

	/*public FileInfo getFile(int id) {
		return files.get(id);
	}

	public Int2ReferenceMap<FileInfo> getFiles() {
		return Int2ReferenceMaps.unmodifiable(files);
	}*/

	public boolean hasLine(int line) {
		return lineMapping.containsKey(line);
	}

	public LineInfo mapLine(int line) {
		return lineMapping.get(line);
	}

	public IntSet mappedLines() {
		return lineMapping.keySet();
	}

	@Override
	public String toString() {
		return "Stratum[" + id + ']';
	}
}