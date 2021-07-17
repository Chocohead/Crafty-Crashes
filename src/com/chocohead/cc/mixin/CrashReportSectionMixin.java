package com.chocohead.cc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.crash.CrashReportSection;

import com.chocohead.cc.SMAPper;

@Mixin(CrashReportSection.class)
abstract class CrashReportSectionMixin {
	@Shadow
	private StackTraceElement[] stackTrace;

	@Inject(method = "initStackTrace",
			at = @At(value = "INVOKE", target = "Ljava/lang/System;arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V", remap = false, shift = Shift.AFTER))
	private void fixCause(CallbackInfoReturnable<Integer> call) {
		SMAPper.apply(stackTrace, "java.");
	}
}