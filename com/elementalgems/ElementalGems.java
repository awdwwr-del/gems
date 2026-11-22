package com.elementalgems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementalGems implements ModInitializer {
	public static final String MOD_ID = "elementalgems";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();
		PayloadTypeRegistry.playC2S().register(UltimatePayload.ID, UltimatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(UltimatePayload.ID,
				(payload, context) -> GemUltimates.tryUse(context.player()));
		PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.ID,
				(payload, context) -> PuffMovement.tryDoubleJump(context.player()));
		PuffMovement.register();
		GemManager.register();
		GemCommand.register();
		LOGGER.info("Elemental Gems loaded with {} gems.", GemType.values().length);
	}
}
