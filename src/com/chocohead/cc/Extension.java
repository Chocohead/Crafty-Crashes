package com.chocohead.cc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

public class Extension implements IMixinConfigPlugin, IExtension {
	@Override
	public void onLoad(String mixinPackage) {
		Object transformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
		if (!(transformer instanceof IMixinTransformer)) throw new IllegalStateException("Running with an odd transformer: " + transformer);

		IExtensionRegistry extensions = ((IMixinTransformer) transformer).getExtensions();
		if (!(extensions instanceof Extensions)) throw new IllegalStateException("Running with odd extensions: " + extensions);

		((Extensions) extensions).add(this);
	}

	@Override
	public boolean checkActive(MixinEnvironment environment) {
		return true;
	}


	@Override
	public List<String> getMixins() {
		return Collections.emptyList();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}


	@Override
	public void preApply(ITargetClassContext context) {
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(ITargetClassContext context) {
	}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, ClassNode node) {
		SourcePool.add(name, node.sourceDebug);
	}
}