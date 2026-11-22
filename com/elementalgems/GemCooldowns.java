package com.elementalgems;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Separate cooldown for every ability of every gem, per player.
 * Stored in memory against the server tick counter (resets when the server restarts).
 */
public final class GemCooldowns {
	private static final Map<UUID, Map<String, Integer>> EXPIRY = new HashMap<>();

	private GemCooldowns() {
	}

	private static String key(GemType gem, boolean second) {
		return gem.id() + (second ? ":2" : ":1");
	}

	private static int now(ServerPlayerEntity player) {
		return player.server.getTicks();
	}

	/** Ticks left before this ability can be used again (0 = ready). */
	public static int remaining(ServerPlayerEntity player, GemType gem, boolean second) {
		Map<String, Integer> map = EXPIRY.get(player.getUuid());
		if (map == null) return 0;
		Integer expiry = map.get(key(gem, second));
		if (expiry == null) return 0;
		return Math.max(0, expiry - now(player));
	}

	public static void start(ServerPlayerEntity player, GemType gem, boolean second, int ticks) {
		EXPIRY.computeIfAbsent(player.getUuid(), id -> new HashMap<>()).put(key(gem, second), now(player) + ticks);
	}

	public static void clear(ServerPlayerEntity player) {
		EXPIRY.remove(player.getUuid());
	}

	/** Cooldown length for a gem ability, shortened by 25% for Tier II gems. */
	public static int cooldownFor(GemType gem, int tier, boolean second) {
		int base = second ? gem.ability2Cooldown() : gem.ability1Cooldown();
		return tier >= 2 ? (int) (base * 0.75) : base;
	}

	// ------------------------------------------------------------------ ultimates (third move)

	public static int remainingUltimate(ServerPlayerEntity player, GemType gem) {
		Map<String, Integer> map = EXPIRY.get(player.getUuid());
		if (map == null) return 0;
		Integer expiry = map.get(gem.id() + ":3");
		if (expiry == null) return 0;
		return Math.max(0, expiry - now(player));
	}

	public static void startUltimate(ServerPlayerEntity player, GemType gem, int ticks) {
		EXPIRY.computeIfAbsent(player.getUuid(), id -> new HashMap<>()).put(gem.id() + ":3", now(player) + ticks);
	}
}
