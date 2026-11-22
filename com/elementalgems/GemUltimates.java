package com.elementalgems;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The third move ("ultimate") of Tier II gems. Triggered by the Ultimate key (default R) or /gems ultimate.
 * Needs a Tier II gem that is awake (energy above 0). Costs no energy, but has a long cooldown.
 */
public final class GemUltimates {
	private GemUltimates() {
	}

	/** Returns true if the ultimate fired. */
	public static boolean tryUse(ServerPlayerEntity p) {
		if (!p.isAlive() || p.isSpectator()) return false;
		ServerWorld w = p.getServerWorld();

		GemItem best = null;
		PlayerInventory inv = p.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).getItem() instanceof GemItem gemItem) {
				if (best == null || gemItem.getTier() > best.getTier()) best = gemItem;
			}
		}
		if (best == null || best.getTier() < 2) {
			p.sendMessage(Text.literal("Ultimates need a Tier II gem. Use a Gem Upgrader.").formatted(Formatting.RED), true);
			return false;
		}

		GemType gem = best.getGem();
		if (GemManager.getEnergy(p) <= 0) {
			p.sendMessage(Text.literal("Your gem is dormant - you have no energy.").formatted(Formatting.RED), true);
			return false;
		}
		int left = GemCooldowns.remainingUltimate(p, gem);
		if (left > 0) {
			p.sendMessage(Text.literal(gem.ultName() + " is on cooldown: " + ((left + 19) / 20) + "s").formatted(Formatting.RED), true);
			return false;
		}

		if (!run(gem, p, w)) return false;

		GemCooldowns.startUltimate(p, gem, gem.ultCooldown());
		p.sendMessage(Text.literal(gem.ultName() + "!").styled(s -> s.withColor(gem.color()).withBold(true)), true);
		return true;
	}

	private static boolean run(GemType gem, ServerPlayerEntity p, ServerWorld w) {
		return switch (gem) {
			case SCULK -> deafeningRoar(p, w);
			case WEALTH -> goldenAegis(p, w);
			case FIRE -> inferno(p, w);
			case WATER -> tsunami(p, w);
			case STRENGTH -> titansWrath(p, w);
			case SPEED -> timeRush(p, w);
			case LIFE -> treeOfLife(p, w);
			case PUFF -> cyclone(p, w);
			case FLUX -> thunderstorm(p, w);
			case ASTRA -> blackHole(p, w);
			case CRIMSON -> crimsonRage(p, w);
			case END -> dragonsWrath(p, w);
			case BLOOD -> crimsonHarvest(p, w);
			case DIVINE -> heavensJudgment(p, w);
		};
	}

	// ------------------------------------------------------------------ the ultimates

	private static boolean deafeningRoar(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.7f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_WARDEN_ROAR, 1.5f, 0.8f);
		GemAbilities.ring(w, ParticleTypes.SCULK_SOUL, p.getPos().add(0, 0.5, 0), 14, 100);
		GemAbilities.ring(w, ParticleTypes.SONIC_BOOM, p.getPos().add(0, 1, 0), 5, 8);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 14)) {
			e.damage(src, 14.0f);
			Vec3d away = flatAway(e, p).multiply(1.6);
			GemAbilities.knock(e, away.x, 0.6, away.z);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 200, 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2));
		}
		return true;
	}

	private static boolean goldenAegis(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.2f, 1.2f);
		GemAbilities.ring(w, ParticleTypes.HAPPY_VILLAGER, p.getPos().add(0, 0.5, 0), 10, 80);
		for (ServerPlayerEntity ally : allies(p, w, 10)) {
			boolean self = ally == p;
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 400, self ? 1 : 0));
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, self ? 1 : 0));
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, self ? 3 : 1));
			if (self) {
				ally.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 400, 3));
				ally.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 400, 3));
			}
			w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, ally.getX(), ally.getBodyY(0.5), ally.getZ(), 30, 0.5, 0.9, 0.5, 0.2);
		}
		return true;
	}

	private static boolean inferno(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
		for (int r = 3; r <= 11; r += 4) {
			GemAbilities.ring(w, ParticleTypes.FLAME, p.getPos().add(0, 0.3, 0), r, 60);
		}
		GemAbilities.ring(w, ParticleTypes.LAVA, p.getPos().add(0, 0.5, 0), 6, 24);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 11)) {
			e.setOnFireFor(10);
			e.damage(src, 12.0f);
			Vec3d away = flatAway(e, p).multiply(1.8);
			GemAbilities.knock(e, away.x, 0.7, away.z);
		}
		return true;
	}

	private static boolean tsunami(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ENTITY_GENERIC_SPLASH, 1.5f, 0.5f);
		for (int r = 4; r <= 12; r += 4) {
			GemAbilities.ring(w, ParticleTypes.SPLASH, p.getPos().add(0, 1, 0), r, 70);
			GemAbilities.ring(w, ParticleTypes.BUBBLE_POP, p.getPos().add(0, 0.6, 0), r, 40);
		}
		Vec3d look = GemAbilities.look(p).multiply(1, 0, 1).normalize();
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 14)) {
			e.extinguish();
			e.damage(src, 8.0f);
			Vec3d push = flatAway(e, p).multiply(1.4).add(look.multiply(1.2));
			GemAbilities.knock(e, push.x, 0.6, push.z);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2));
		}
		return true;
	}

	private static boolean titansWrath(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 0.5f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.2f, 0.7f);
		GemAbilities.ring(w, ParticleTypes.EXPLOSION, p.getPos().add(0, 0.2, 0), 4, 14);
		GemAbilities.ring(w, ParticleTypes.EXPLOSION, p.getPos().add(0, 0.2, 0), 8, 22);
		GemAbilities.ring(w, ParticleTypes.CRIT, p.getPos().add(0, 0.5, 0), 9, 70);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 120, 2));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 2));
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 9)) {
			e.damage(src, 14.0f);
			Vec3d away = flatAway(e, p).multiply(1.2);
			GemAbilities.knock(e, away.x, 1.4, away.z);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
		}
		return true;
	}

	private static boolean timeRush(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.5f, 1.4f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.0f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 4));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1));
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 8)) {
			e.damage(src, 14.0f);
			GemAbilities.knock(e, 0, 0.5, 0);
			GemAbilities.particleLine(w, ParticleTypes.SWEEP_ATTACK, p.getEyePos(), e.getEyePos(), 1.0);
			w.spawnParticles(ParticleTypes.CRIT, e.getX(), e.getBodyY(0.5), e.getZ(), 25, 0.4, 0.6, 0.4, 0.4);
		}
		GemAbilities.ring(w, ParticleTypes.END_ROD, p.getPos().add(0, 1, 0), 3, 40);
		return true;
	}

	private static boolean treeOfLife(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.0f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
		for (int r = 4; r <= 16; r += 6) {
			GemAbilities.ring(w, ParticleTypes.HAPPY_VILLAGER, p.getPos().add(0, 0.5, 0), r, 80);
		}
		for (ServerPlayerEntity ally : allies(p, w, 16)) {
			ally.heal(20.0f);
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2));
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 1));
			w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getBodyY(0.5), ally.getZ(), 40, 0.6, 1.0, 0.6, 0.1);
		}
		return true;
	}

	private static boolean cyclone(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.6f);
		for (int r = 3; r <= 12; r += 3) {
			GemAbilities.ring(w, ParticleTypes.CLOUD, p.getPos().add(0, 0.5 + r * 0.2, 0), r, 60);
		}
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 12)) {
			e.damage(src, 4.0f);
			Vec3d away = flatAway(e, p).multiply(1.0);
			GemAbilities.knock(e, away.x, 1.9, away.z);
		}
		return true;
	}

	private static boolean thunderstorm(ServerPlayerEntity p, ServerWorld w) {
		List<LivingEntity> targets = new ArrayList<>(GemAbilities.nearby(p, w, 20));
		if (targets.isEmpty()) return noEnemies(p);
		targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
		for (int i = 0; i < Math.min(6, targets.size()); i++) {
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
			if (bolt != null) {
				bolt.refreshPositionAfterTeleport(targets.get(i).getPos());
				bolt.setChanneler(p);
				w.spawnEntity(bolt);
			}
		}
		return true;
	}

	private static boolean blackHole(ServerPlayerEntity p, ServerWorld w) {
		Vec3d center = GemAbilities.rayEnd(p, w, 12).add(0, 1, 0);
		GemAbilities.sound(w, p, SoundEvents.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.6f);
		for (int r = 2; r <= 12; r += 3) {
			GemAbilities.ring(w, ParticleTypes.PORTAL, center, r, 50);
		}
		w.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 60, 0.5, 0.5, 0.5, 0.2);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.aroundPoint(p, w, center, 12)) {
			Vec3d pull = center.subtract(e.getPos()).multiply(0.22);
			GemAbilities.knock(e, pull.x, Math.max(0.2, pull.y), pull.z);
			e.damage(src, 10.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
		}
		return true;
	}

	private static boolean crimsonRage(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ENTITY_HOGLIN_ANGRY, 1.6f, 0.6f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.2f, 0.8f);
		for (int r = 3; r <= 10; r += 3) {
			GemAbilities.ring(w, ParticleTypes.CRIMSON_SPORE, p.getPos().add(0, 1, 0), r, 70);
		}
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 240, 2));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 240, 1));
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 10)) {
			e.damage(src, 8.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 1));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 0));
		}
		return true;
	}

	private static boolean dragonsWrath(ServerPlayerEntity p, ServerWorld w) {
		Vec3d look = GemAbilities.look(p);
		Vec3d eye = p.getEyePos();
		GemAbilities.sound(w, p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.9f);
		for (int i = -5; i <= 5; i++) {
			double angle = Math.toRadians(i * 8);
			Vec3d dir = new Vec3d(look.x * Math.cos(angle) - look.z * Math.sin(angle), look.y,
					look.x * Math.sin(angle) + look.z * Math.cos(angle));
			GemAbilities.particleLine(w, ParticleTypes.PORTAL, eye, eye.add(dir.multiply(12)), 0.7);
			GemAbilities.particleLine(w, ParticleTypes.END_ROD, eye, eye.add(dir.multiply(12)), 2.0);
		}
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 12)) {
			Vec3d to = e.getBoundingBox().getCenter().subtract(eye).normalize();
			if (to.dotProduct(look) < 0.45) continue;
			e.damage(src, 14.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
		}
		return true;
	}

	private static boolean crimsonHarvest(ServerPlayerEntity p, ServerWorld w) {
		List<LivingEntity> targets = GemAbilities.nearby(p, w, 10);
		if (targets.isEmpty()) return noEnemies(p);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.5f);
		GemAbilities.ring(w, ParticleTypes.DAMAGE_INDICATOR, p.getPos().add(0, 1, 0), 10, 60);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		float healed = 0;
		for (LivingEntity e : targets) {
			e.damage(src, 8.0f);
			healed += 3.0f;
			GemAbilities.particleLine(w, ParticleTypes.DAMAGE_INDICATOR, e.getEyePos(), p.getEyePos(), 0.6);
		}
		p.heal(Math.min(healed, 20.0f));
		return true;
	}

	private static boolean heavensJudgment(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.6f, 1.5f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.8f);
		for (int r = 4; r <= 14; r += 5) {
			GemAbilities.ring(w, ParticleTypes.END_ROD, p.getPos().add(0, 0.5, 0), r, 70);
		}
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 14)) {
			float damage = isUndead(e) ? 24.0f : 12.0f;
			e.damage(src, damage);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
			Vec3d pos = e.getPos();
			GemAbilities.particleLine(w, ParticleTypes.END_ROD, pos.add(0, 16, 0), pos, 0.6);
			w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1, pos.z, 25, 0.4, 0.8, 0.4, 0.2);
		}
		for (ServerPlayerEntity ally : allies(p, w, 14)) {
			ally.heal(16.0f);
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 1));
			w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, ally.getX(), ally.getBodyY(0.5), ally.getZ(), 30, 0.5, 0.9, 0.5, 0.2);
		}
		return true;
	}

	// ------------------------------------------------------------------ helpers

	static boolean isUndead(LivingEntity e) {
		return e instanceof net.minecraft.entity.mob.ZombieEntity
				|| e instanceof net.minecraft.entity.mob.AbstractSkeletonEntity
				|| e instanceof net.minecraft.entity.mob.PhantomEntity
				|| e instanceof net.minecraft.entity.mob.ZoglinEntity
				|| e instanceof net.minecraft.entity.boss.WitherEntity;
	}

	private static Vec3d flatAway(LivingEntity e, ServerPlayerEntity p) {
		Vec3d away = e.getPos().subtract(p.getPos()).multiply(1, 0, 1);
		return away.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 0) : away.normalize();
	}

	/** You plus the players you trust within `radius`. */
	private static List<ServerPlayerEntity> allies(ServerPlayerEntity p, ServerWorld w, double radius) {
		return w.getEntitiesByClass(ServerPlayerEntity.class, p.getBoundingBox().expand(radius),
				e -> e.isAlive() && e.squaredDistanceTo(p) <= radius * radius && (e == p || GemTrust.isTrusted(p, e)));
	}

	private static boolean noEnemies(ServerPlayerEntity p) {
		p.sendMessage(Text.literal("No enemies nearby.").formatted(Formatting.RED), true);
		return false;
	}
}
