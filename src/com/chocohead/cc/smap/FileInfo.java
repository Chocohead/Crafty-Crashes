package com.chocohead.cc.smap;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class FileInfo {
	public final String name, path;
	public final IMixinInfo mixin;

	public FileInfo(String name, String path, IMixinInfo mixin) {
		this.name = name;
		this.path = path;
		this.mixin = mixin;
	}

	public String getBestName() {
		return getBestName("?");
	}

	public String getBestName(String finalOption) {
		if (path != null) {
			return mixin != null ? path + " [" + mixin.getConfig().getName() + ']' : path;
		} else {
			return name != null && !"null".equals(name) ? name : finalOption;
		}
	}

	@Override
	public String toString() {
		return "File[" + name + (path != null ? ", " + path : "") + ']';
	}
}