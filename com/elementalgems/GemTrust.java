package com.elementalgems;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Trust list, like the Bliss SMP /trust command.
 * Players you trust are skipped by your gem abilities (damage, debuffs, knockback)
 * and are healed by your Life Aura. Trust is one-way: they have to trust you back.
 * Entries are stored as "uuid;name".
 */
public final class GemTrust {
	public static final AttachmentType<List<String>> TRUSTED = AttachmentRegistry.create(
			Identifier.of(ElementalGems.MOD_ID, "trusted"),
			builder -> builder.persistent(Codec.STRING.listOf()).copyOnDeath());

	private GemTrust() {
	}

	public static List<String> entries(ServerPlayerEntity owner) {
		List<String> list = owner.getAttached(TRUSTED);
		return list == null ? new ArrayList<>() : new ArrayList<>(list);
	}

	public static boolean isTrusted(ServerPlayerEntity owner, Entity other) {
		if (!(other instanceof ServerPlayerEntity otherPlayer)) return false;
		if (otherPlayer == owner) return false;
		String id = otherPlayer.getUuidAsString();
		for (String entry : entries(owner)) {
			if (entry.split(";", 2)[0].equals(id)) return true;
		}
		return false;
	}

	public static boolean add(ServerPlayerEntity owner, ServerPlayerEntity target) {
		if (isTrusted(owner, target)) return false;
		List<String> list = entries(owner);
		list.add(target.getUuidAsString() + ";" + target.getGameProfile().getName());
		owner.setAttached(TRUSTED, list);
		return true;
	}

	public static boolean remove(ServerPlayerEntity owner, ServerPlayerEntity target) {
		List<String> list = entries(owner);
		String id = target.getUuidAsString();
		boolean removed = list.removeIf(entry -> entry.split(";", 2)[0].equals(id));
		if (removed) owner.setAttached(TRUSTED, list);
		return removed;
	}

	public static String nameOf(String entry) {
		String[] parts = entry.split(";", 2);
		return parts.length > 1 ? parts[1] : parts[0];
	}
}
