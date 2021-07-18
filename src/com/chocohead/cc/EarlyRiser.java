package com.chocohead.cc;

import org.objectweb.asm.tree.ClassNode;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

public class EarlyRiser implements Runnable, IExtension {
	@Override
	public void run() {
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
	public void preApply(ITargetClassContext context) {
	}

	@Override
	public void postApply(ITargetClassContext context) {
	}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, ClassNode node) {
		SourcePool.add(name, node.sourceDebug);
	}
}