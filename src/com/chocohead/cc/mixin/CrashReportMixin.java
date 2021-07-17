package com.chocohead.cc.mixin;

import org.objectweb.asm.Opcodes;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.crash.CrashReport;

import com.chocohead.cc.SMAPper;

@Mixin(CrashReport.class)
abstract class CrashReportMixin {
	@Shadow
	private @Final Throwable cause;

	@Inject(method = "<init>",
			at = @At(value = "FIELD", target = "Lnet/minecraft/util/crash/CrashReport;cause:Ljava/lang/Throwable;", opcode = Opcodes.PUTFIELD, shift = Shift.AFTER))
	private void fixCause(CallbackInfo call) {
		SMAPper.apply(cause, "java.");
	}
}