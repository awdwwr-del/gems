package com.elementalgems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Client side: the Ultimate key (default R, rebindable in Controls). */
public class ElementalGemsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyBinding ultimateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.elementalgems.ultimate",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				"key.categories.elementalgems"));

		// Double jump: a fresh jump press while in the air, carrying a Puff gem.
		boolean[] jumpHeld = {false};
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			ClientPlayerEntity player = client.player;
			if (player == null) {
				jumpHeld[0] = false;
				return;
			}
			boolean pressed = client.options.jumpKey.isPressed();
			if (pressed && !jumpHeld[0] && client.currentScreen == null
					&& !player.isOnGround() && !player.isTouchingWater() && !player.isClimbing()
					&& !player.isFallFlying() && !player.getAbilities().flying
					&& PuffMovement.puffTier(player) > 0) {
				ClientPlayNetworking.send(new DoubleJumpPayload());
			}
			jumpHeld[0] = pressed;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (ultimateKey.wasPressed()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new UltimatePayload());
				}
			}
		});
	}
}
