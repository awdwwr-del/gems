package com.elementalgems;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Puff gem's passive: a double jump (a triple jump at Tier II) and no fall damage.
 * There is deliberately no Slow Falling, so critical hits and the mace smash still work while you fall.
 */
public final class PuffMovement {
	private static final Map<UUID, Integer> JUMPS_USED = new HashMap<>();

	private PuffMovement() {
	}

	public static void register() {
		// No fall damage while you carry an awake Puff gem.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.isOf(DamageTypes.FALL)
					&& entity instanceof ServerPlayerEntity player
					&& puffTier(player) > 0
					&& GemManager.getEnergy(player) > 0) {
				return false;
			}
			return true;
		});

		// Jumps come back once you touch the ground (or water).
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (JUMPS_USED.isEmpty()) return;
			Iterator<Map.Entry<UUID, Integer>> it = JUMPS_USED.entrySet().iterator();
			while (it.hasNext()) {
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(it.next().getKey());
				if (player == null || !player.isAlive() || player.isOnGround() || player.isTouchingWater()) {
					it.remove();
				}
			}
		});
	}

	/** Highest tier of Puff gem the player carries (0 = none). Safe to call on the client too. */
	public static int puffTier(PlayerEntity player) {
		int best = 0;
		PlayerInventory inv = player.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).getItem() instanceof GemItem gemItem && gemItem.getGem() == GemType.PUFF) {
				best = Math.max(best, gemItem.getTier());
			}
		}
		return best;
	}

	/** Called when the client reports a jump press in mid-air. Returns true if the extra jump happened. */
	public static boolean tryDoubleJump(ServerPlayerEntity p) {
		int tier = puffTier(p);
		if (tier == 0 || !p.isAlive() || p.isSpectator() || GemManager.getEnergy(p) <= 0) return false;
		if (p.isOnGround() || p.isTouchingWater() || p.isClimbing() || p.isFallFlying() || p.getAbilities().flying) return false;

		int allowed = tier >= 2 ? 2 : 1;
		int used = JUMPS_USED.getOrDefault(p.getUuid(), 0);
		if (used >= allowed) return false;
		JUMPS_USED.put(p.getUuid(), used + 1);

		Vec3d v = p.getVelocity();
		p.setVelocity(v.x, 0.6, v.z);
		p.velocityModified = true;
		p.fallDistance = 0;

		ServerWorld w = p.getServerWorld();
		w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 15, 0.3, 0.05, 0.3, 0.05);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.8f, 1.6f);
		return true;
	}
}
