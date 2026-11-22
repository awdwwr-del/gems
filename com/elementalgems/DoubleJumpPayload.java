package com.elementalgems;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Sent from the client to the server when the jump key is pressed in mid-air while carrying a Puff gem. */
public record DoubleJumpPayload() implements CustomPayload {
	public static final CustomPayload.Id<DoubleJumpPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ElementalGems.MOD_ID, "double_jump"));
	public static final PacketCodec<RegistryByteBuf, DoubleJumpPayload> CODEC =
			PacketCodec.unit(new DoubleJumpPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
