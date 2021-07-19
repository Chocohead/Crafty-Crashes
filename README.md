# Crafty Crashes
A Fabric mod which modifies stack traces in crash reports to include the relevant Mixin and source line number where appropriate, acting as a super [Mixin Trace](//github.com/comp500/mixintrace) whilst still working alongside it. Includes an [SMAP](https://jcp.org/en/jsr/detail?id=45) reader in the off chance you need one too.

Tested on 1.16.5 and 1.17.1, likely to work on many other versions which have similar crash reporting logic

### Example
Using the following Mixin as an example of a Mixin which causes issues at runtime:
```java
@Mixin(Keyboard.class)
abstract class BadMixin {
	@Dynamic("1.16 Lambda")
	@Group(min = 1, max = 1)
	@Inject(method = "method_1454(I[ZLnet/minecraft/client/gui/ParentElement;III)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ParentElement;keyPressed(III)Z"))
	private void beforeKeyPressedEvent(int code, boolean[] resultHack, ParentElement parentElement, int key, int scancode, int modifiers, CallbackInfo call) {
		if (key == GLFW.GLFW_KEY_T) {
			throw new RuntimeException("Oh no a crash");
		}
	}

	@Dynamic("1.17 Lambda")
	@Group(min = 1, max = 1)
	@Inject(method = "method_1454(ILnet/minecraft/client/gui/screen/Screen;[ZIII)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;keyPressed(III)Z"))
	private void beforeKeyPressedEvent(int code, Screen screen, boolean[] resultHack, int key, int scancode, int modifiers, CallbackInfo call) {
		beforeKeyPressedEvent(code, resultHack, screen, key, scancode, modifiers, call);
	}
}
```
Normally when this is run you'd get the following crash report section (which is effectively the top frames of the full stack trace):
```
-- Head --
Thread: Render thread
Stacktrace:
	at net.minecraft.class_309.handler$zzg001$beforeKeyPressedEvent(class_309.java:1075)
	at net.minecraft.class_309.handler$zzg001$beforeKeyPressedEvent(class_309.java:1084)
	at net.minecraft.class_309.method_1454(class_309.java:374)
```
`Keyboard` only has around 520 lines, so 1075 and 1084 are entirely unhelpful to finding what is wrong. The only knowledge gained is that the Mixin which crashed had handlers called `beforeKeyPressedEvent`.

In contrast, running with Crafty Crashes:
```
-- Head --
Thread: Render thread
Stacktrace:
	at net.minecraft.class_309.handler$zzg001$beforeKeyPressedEvent(com/chocohead/example/mixin/BadMixin.java [cc-example.mixins.json]:24)
	at net.minecraft.class_309.handler$zzg001$beforeKeyPressedEvent(com/chocohead/example/mixin/BadMixin.java [cc-example.mixins.json]:33)
	at net.minecraft.class_309.method_1454(class_309.java:374)
```
Now we have the fully qualified name of the Mixin which has gone wrong, the Mixin config which registered it, and the correct line numbers for the Mixin. This makes finding and debugging the Mixin much easier, especially as searching for a Mixin config by name on Github is more likely to return a unique result than searching the handler or Mixin class's name.
