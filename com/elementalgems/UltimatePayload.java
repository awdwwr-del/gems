package com.elementalgems;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Sent from the client to the server when the Ultimate key is pressed. Carries no data. */
public record UltimatePayload() implements CustomPayload {
	public static final CustomPayload.Id<UltimatePayload> ID =
			new CustomPayload.Id<>(Identifier.of(ElementalGems.MOD_ID, "ultimate"));
	public static final PacketCodec<RegistryByteBuf, UltimatePayload> CODEC =
			PacketCodec.unit(new UltimatePayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
