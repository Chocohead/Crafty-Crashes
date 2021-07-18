package com.chocohead.cc.smap;

public class FileInfo {
	public final String name, path;

	public FileInfo(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public String getBestName() {
		return getBestName("?");
	}

	public String getBestName(String finalOption) {
		return path != null ? path : name != null && !"null".equals(name) ? name : finalOption;
	}

	@Override
	public String toString() {
		return "File[" + name + (path != null ? ", " + path : "") + ']';
	}
}