package com.elementalgems;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * All gem behaviour lives here: passives (applied once a second) and the two active abilities per gem.
 * Tier II gems hit 50% harder, last 50% longer, reach 30% further and have a stronger passive.
 * Trusted players (see {@link GemTrust}) are never hit by your abilities.
 */
public final class GemAbilities {
	private GemAbilities() {
	}

	// ------------------------------------------------------------------ tier scaling

	/** Damage / healing / knockback strength. */
	private static double sc(int tier, double v) {
		return tier >= 2 ? v * 1.5 : v;
	}

	/** Range / radius. */
	private static double rg(int tier, double v) {
		return tier >= 2 ? v * 1.3 : v;
	}

	/** Effect duration in ticks. */
	private static int dur(int tier, int ticks) {
		return tier >= 2 ? (int) (ticks * 1.5) : ticks;
	}

	/** Effect amplifier. */
	private static int amp(int tier, int amplifier) {
		return tier >= 2 ? amplifier + 1 : amplifier;
	}

	// ------------------------------------------------------------------ passives

	/** Called about once per second for every gem a player carries (not while out of energy). */
	public static void applyPassive(GemType gem, int tier, ServerPlayerEntity p) {
		int b = tier >= 2 ? 1 : 0;
		switch (gem) {
			case SCULK -> {
				p.removeStatusEffect(StatusEffects.DARKNESS);
				passive(p, StatusEffects.NIGHT_VISION, 0, 400);
			}
			case WEALTH -> {
				passive(p, StatusEffects.LUCK, 1 + b, 100);
				passive(p, StatusEffects.HERO_OF_THE_VILLAGE, b, 100);
			}
			case FIRE -> passive(p, StatusEffects.FIRE_RESISTANCE, 0, 100);
			case WATER -> {
				passive(p, StatusEffects.WATER_BREATHING, 0, 100);
				passive(p, StatusEffects.DOLPHINS_GRACE, b, 100);
			}
			case STRENGTH -> passive(p, StatusEffects.STRENGTH, b, 100);
			case SPEED -> passive(p, StatusEffects.SPEED, b, 100);
			case LIFE -> passive(p, StatusEffects.HEALTH_BOOST, b, 100);
			case PUFF -> { } // double jump and no fall damage live in PuffMovement
			case FLUX -> passive(p, StatusEffects.HASTE, b, 100);
			case ASTRA -> passive(p, StatusEffects.JUMP_BOOST, b, 100);
			case CRIMSON -> {
				passive(p, StatusEffects.FIRE_RESISTANCE, 0, 100);
				if (p.getWorld().getRegistryKey() == World.NETHER) {
					passive(p, StatusEffects.STRENGTH, b, 100);
				}
			}
			case END -> passive(p, StatusEffects.SATURATION, 0, 100);
			case BLOOD -> {
				if (p.getHealth() <= p.getMaxHealth() * 0.5f) {
					passive(p, StatusEffects.STRENGTH, b, 100);
					passive(p, StatusEffects.SPEED, 0, 100);
				}
			}
			case DIVINE -> {
				passive(p, StatusEffects.RESISTANCE, 0, 100);
				if (tier >= 2) passive(p, StatusEffects.FIRE_RESISTANCE, 0, 100);
			}
		}
	}

	private static void passive(ServerPlayerEntity p, RegistryEntry<StatusEffect> effect, int amp, int ticks) {
		p.addStatusEffect(new StatusEffectInstance(effect, ticks, amp, true, false, true));
	}

	// ------------------------------------------------------------------ dispatch

	/** Runs an ability. Returns false if it could not fire (no target, not enough health), so no cooldown is spent. */
	public static boolean activate(GemType gem, int tier, boolean second, ServerPlayerEntity p, ServerWorld w) {
		switch (gem) {
			case SCULK -> { if (second) sculkPulse(p, w, tier); else sonicBoom(p, w, tier); }
			case WEALTH -> { if (second) goldRush(p, w, tier); else pockets(p); }
			case FIRE -> { if (second) fireRing(p, w, tier); else fireball(p, w, tier); }
			case WATER -> { if (second) whirlpool(p, w, tier); else waterCannon(p, w, tier); }
			case STRENGTH -> { if (second) berserk(p, w, tier); else shockwave(p, w, tier); }
			case SPEED -> { if (second) blur(p, w, tier); else dash(p, w, tier); }
			case LIFE -> { if (second) lifeAura(p, w, tier); else mend(p, w, tier); }
			case PUFF -> { if (second) gust(p, w, tier); else cloudLeap(p, w, tier); }
			case FLUX -> { if (second) staticDischarge(p, w, tier); else lightningStrike(p, w, tier); }
			case ASTRA -> { if (second) gravityWarp(p, w, tier); else starfall(p, w, tier); }
			case CRIMSON -> { if (second) sporeBurst(p, w, tier); else hoglinCharge(p, w, tier); }
			case END -> { return second ? voidRift(p, w, tier) : enderSwap(p, w, tier); }
			case BLOOD -> { return second ? bloodPact(p, w, tier) : bloodDrain(p, w, tier); }
			case DIVINE -> { return second ? divineShield(p, w, tier) : smite(p, w, tier); }
		}
		return true;
	}

	// ------------------------------------------------------------------ SCULK

	private static void sonicBoom(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d start = p.getEyePos();
		Vec3d end = rayEnd(p, w, rg(tier, 16));
		sound(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
		particleLine(w, ParticleTypes.SONIC_BOOM, start.add(look(p)), end, 2.0);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity target : alongRay(p, w, start, end, 1.2)) {
			target.damage(src, (float) sc(tier, 12.0));
			Vec3d push = look(p).multiply(sc(tier, 1.2));
			knock(target, push.x, 0.4, push.z);
		}
	}

	private static void sculkPulse(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 10);
		sound(w, p, SoundEvents.ENTITY_WARDEN_ROAR, 1.0f, 1.2f);
		ring(w, ParticleTypes.SCULK_SOUL, p.getPos(), 8, 40);
		for (LivingEntity e : nearby(p, w, radius)) {
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, dur(tier, 160), 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(tier, 100), amp(tier, 1)));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, dur(tier, 200), 0));
		}
	}

	// ------------------------------------------------------------------ WEALTH

	private static void pockets(ServerPlayerEntity p) {
		p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inv, player) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inv, player.getEnderChestInventory()),
				Text.literal("Pockets")));
	}

	private static void goldRush(ServerPlayerEntity p, ServerWorld w, int tier) {
		sound(w, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, dur(tier, 600), amp(tier, 2)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, dur(tier, 600), amp(tier, 2)));
		w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getBodyY(0.5), p.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
	}

	// ------------------------------------------------------------------ FIRE

	private static void fireball(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d look = look(p);
		FireballEntity ball = new FireballEntity(w, p, look.multiply(1.2), tier >= 2 ? 3 : 2);
		ball.setPosition(p.getEyePos().add(look.multiply(1.5)));
		w.spawnEntity(ball);
		sound(w, p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
	}

	private static void fireRing(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 6);
		sound(w, p, SoundEvents.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
		ring(w, ParticleTypes.FLAME, p.getPos(), radius - 1, 48);
		ring(w, ParticleTypes.LAVA, p.getPos().add(0, 0.5, 0), radius - 3, 16);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : nearby(p, w, radius)) {
			e.setOnFireFor((int) sc(tier, 8));
			e.damage(src, (float) sc(tier, 4.0));
			Vec3d away = e.getPos().subtract(p.getPos()).multiply(1, 0, 1).normalize().multiply(sc(tier, 1.2));
			knock(e, away.x, 0.4, away.z);
		}
	}

	// ------------------------------------------------------------------ WATER

	private static void waterCannon(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d start = p.getEyePos();
		Vec3d end = rayEnd(p, w, rg(tier, 20));
		sound(w, p, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f);
		particleLine(w, ParticleTypes.SPLASH, start.add(look(p)), end, 0.5);
		particleLine(w, ParticleTypes.BUBBLE, start.add(look(p)), end, 1.0);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity target : alongRay(p, w, start, end, 1.4)) {
			target.extinguish();
			target.damage(src, (float) sc(tier, 4.0));
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(tier, 60), 1));
			Vec3d push = look(p).multiply(sc(tier, 1.8));
			knock(target, push.x, 0.3, push.z);
		}
	}

	private static void whirlpool(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 8);
		sound(w, p, SoundEvents.ENTITY_GENERIC_SPLASH, 1.0f, 0.6f);
		ring(w, ParticleTypes.BUBBLE_POP, p.getPos().add(0, 1, 0), radius, 60);
		ring(w, ParticleTypes.SPLASH, p.getPos().add(0, 1, 0), radius - 3, 40);
		for (LivingEntity e : nearby(p, w, radius)) {
			Vec3d pull = p.getPos().subtract(e.getPos()).normalize().multiply(sc(tier, 0.9));
			knock(e, pull.x, 0.2, pull.z);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(tier, 80), 2));
		}
	}

	// ------------------------------------------------------------------ STRENGTH

	private static void shockwave(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 5);
		sound(w, p, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.6f);
		ring(w, ParticleTypes.EXPLOSION, p.getPos().add(0, 0.2, 0), radius - 1, 12);
		ring(w, ParticleTypes.CRIT, p.getPos().add(0, 0.5, 0), radius, 50);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : nearby(p, w, radius)) {
			e.damage(src, (float) sc(tier, 8.0));
			Vec3d away = e.getPos().subtract(p.getPos()).multiply(1, 0, 1).normalize().multiply(sc(tier, 0.8));
			knock(e, away.x, sc(tier, 0.9), away.z);
		}
	}

	private static void berserk(ServerPlayerEntity p, ServerWorld w, int tier) {
		sound(w, p, SoundEvents.ENTITY_RAVAGER_ROAR, 1.0f, 0.9f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, dur(tier, 300), amp(tier, 2)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, dur(tier, 300), amp(tier, 0)));
		w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getBodyY(1.0), p.getZ(), 12, 0.5, 0.5, 0.5, 0.0);
	}

	// ------------------------------------------------------------------ SPEED

	private static void dash(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d look = look(p);
		Vec3d start = p.getPos().add(0, 1, 0);
		Vec3d end = start.add(look.multiply(rg(tier, 8)));
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : alongRay(p, w, start, end, 1.5)) {
			e.damage(src, (float) sc(tier, 7.0));
			knock(e, look.x * 0.6, 0.5, look.z * 0.6);
			w.spawnParticles(ParticleTypes.CRIT, e.getX(), e.getBodyY(0.5), e.getZ(), 15, 0.3, 0.5, 0.3, 0.3);
		}
		particleLine(w, ParticleTypes.SWEEP_ATTACK, start, end, 2.0);
		particleLine(w, ParticleTypes.CRIT, start, end, 0.5);
		double force = sc(tier, 2.2);
		p.setVelocity(look.x * force, Math.max(look.y * 1.2, 0.25), look.z * force);
		p.velocityModified = true;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur(tier, 100), 2));
		sound(w, p, SoundEvents.ENTITY_PHANTOM_FLAP, 1.2f, 1.4f);
		sound(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
		w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 20, 0.3, 0.1, 0.3, 0.05);
	}

	private static void blur(ServerPlayerEntity p, ServerWorld w, int tier) {
		sound(w, p, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur(tier, 300), amp(tier, 3)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, dur(tier, 300), amp(tier, 1)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, dur(tier, 300), amp(tier, 0)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, dur(tier, 300), amp(tier, 1)));
		w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getBodyY(0.5), p.getZ(), 25, 0.4, 0.8, 0.4, 0.05);
	}

	// ------------------------------------------------------------------ LIFE

	private static void mend(ServerPlayerEntity p, ServerWorld w, int tier) {
		sound(w, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
		p.heal((float) sc(tier, 10.0));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, dur(tier, 100), amp(tier, 1)));
		w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getBodyY(0.5), p.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
	}

	private static void lifeAura(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 12);
		sound(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
		ring(w, ParticleTypes.HAPPY_VILLAGER, p.getPos().add(0, 0.5, 0), radius, 80);
		List<ServerPlayerEntity> players = w.getEntitiesByClass(ServerPlayerEntity.class,
				p.getBoundingBox().expand(radius),
				e -> e.isAlive() && e.squaredDistanceTo(p) <= radius * radius && (e == p || GemTrust.isTrusted(p, e)));
		for (ServerPlayerEntity target : players) {
			target.heal((float) sc(tier, 6.0));
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, dur(tier, 160), amp(tier, 2)));
		}
	}

	// ------------------------------------------------------------------ PUFF

	private static void cloudLeap(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d look = look(p);
		p.setVelocity(look.x * 1.0, sc(tier, 1.1), look.z * 1.0);
		p.velocityModified = true;
		p.fallDistance = 0;
		sound(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
		w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 40, 0.5, 0.1, 0.5, 0.1);
	}

	private static void gust(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 7);
		sound(w, p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
		ring(w, ParticleTypes.CLOUD, p.getPos().add(0, 0.5, 0), radius, 60);
		for (LivingEntity e : nearby(p, w, radius)) {
			Vec3d away = e.getPos().subtract(p.getPos()).multiply(1, 0, 1).normalize().multiply(sc(tier, 1.8));
			knock(e, away.x, 0.6, away.z);
		}
	}

	// ------------------------------------------------------------------ FLUX

	private static void lightningStrike(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d start = p.getEyePos();
		Vec3d end = rayEnd(p, w, rg(tier, 30));
		List<LivingEntity> hits = alongRay(p, w, start, end, 1.5);

		int bolts = tier >= 2 ? 2 : 1; // Tier II chains a second bolt onto another target
		for (int i = 0; i < bolts; i++) {
			Vec3d target;
			if (i < hits.size()) {
				target = hits.get(i).getPos();
			} else if (i == 0) {
				target = end;
			} else {
				break;
			}
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
			if (bolt != null) {
				bolt.refreshPositionAfterTeleport(target);
				bolt.setChanneler(p);
				w.spawnEntity(bolt);
			}
		}
	}

	private static void staticDischarge(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 8);
		sound(w, p, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);
		ring(w, ParticleTypes.ELECTRIC_SPARK, p.getPos().add(0, 1, 0), radius, 80);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : nearby(p, w, radius)) {
			e.damage(src, (float) sc(tier, 6.0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(tier, 80), 1));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, dur(tier, 100), 0));
			particleLine(w, ParticleTypes.ELECTRIC_SPARK, p.getEyePos(), e.getEyePos(), 0.5);
		}
	}

	// ------------------------------------------------------------------ ASTRA

	private static void blink(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d look = look(p);
		Vec3d eyeEnd = rayEnd(p, w, rg(tier, 14));
		Vec3d feet = eyeEnd.subtract(0, p.getStandingEyeHeight(), 0);
		Vec3d delta = feet.subtract(p.getPos());

		// Back off along the look direction until the player's hitbox actually fits.
		for (int i = 0; i < 12; i++) {
			Box box = p.getBoundingBox().offset(delta);
			if (w.isSpaceEmpty(p, box)) break;
			delta = delta.subtract(look.multiply(0.5));
		}

		Vec3d from = p.getPos();
		Vec3d to = from.add(delta);
		w.spawnParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 40, 0.3, 0.6, 0.3, 0.2);
		p.requestTeleport(to.x, to.y, to.z);
		p.fallDistance = 0;
		w.spawnParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 40, 0.3, 0.6, 0.3, 0.2);
		sound(w, p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
	}

	private static void gravityWarp(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 10);
		sound(w, p, SoundEvents.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.6f);
		ring(w, ParticleTypes.END_ROD, p.getPos().add(0, 1, 0), radius, 70);
		for (LivingEntity e : nearby(p, w, radius)) {
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, dur(tier, 60), 1));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, dur(tier, 200), 0));
		}
	}

	// ------------------------------------------------------------------ CRIMSON

	private static void hoglinCharge(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d look = look(p);
		Vec3d start = p.getPos().add(0, 1, 0);
		Vec3d end = start.add(look.multiply(rg(tier, 7)));
		sound(w, p, SoundEvents.ENTITY_HOGLIN_ATTACK, 1.2f, 0.8f);
		particleLine(w, ParticleTypes.CRIMSON_SPORE, start, end, 0.5);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : alongRay(p, w, start, end, 1.6)) {
			e.damage(src, (float) sc(tier, 8.0));
			knock(e, look.x * sc(tier, 1.2), 0.8, look.z * sc(tier, 1.2));
		}
		double force = sc(tier, 1.8);
		p.setVelocity(look.x * force, 0.2, look.z * force);
		p.velocityModified = true;
	}

	private static void sporeBurst(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 8);
		sound(w, p, SoundEvents.ENTITY_HOGLIN_ANGRY, 1.2f, 0.7f);
		ring(w, ParticleTypes.CRIMSON_SPORE, p.getPos().add(0, 1, 0), radius, 70);
		ring(w, ParticleTypes.CRIMSON_SPORE, p.getPos().add(0, 1, 0), radius * 0.5, 40);
		for (LivingEntity e : nearby(p, w, radius)) {
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, dur(tier, 100), 1));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, dur(tier, 140), 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(tier, 100), 1));
		}
	}

	// ------------------------------------------------------------------ END

	private static boolean enderSwap(ServerPlayerEntity p, ServerWorld w, int tier) {
		LivingEntity target = firstTarget(p, w, rg(tier, 24));
		if (target == null) return noTarget(p);

		Vec3d a = p.getPos();
		Vec3d b = target.getPos();
		w.spawnParticles(ParticleTypes.PORTAL, a.x, a.y + 1, a.z, 40, 0.3, 0.6, 0.3, 0.3);
		w.spawnParticles(ParticleTypes.PORTAL, b.x, b.y + 1, b.z, 40, 0.3, 0.6, 0.3, 0.3);
		target.requestTeleport(a.x, a.y, a.z);
		p.requestTeleport(b.x, b.y, b.z);
		p.fallDistance = 0;
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(tier, 60), 1));
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, dur(tier, 40), 0));
		sound(w, p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
		return true;
	}

	private static boolean voidRift(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d center = rayEnd(p, w, rg(tier, 14));
		double radius = rg(tier, 4);
		sound(w, p, SoundEvents.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.6f);
		ring(w, ParticleTypes.PORTAL, center.add(0, 0.2, 0), radius, 60);
		ring(w, ParticleTypes.PORTAL, center.add(0, 0.2, 0), radius * 0.5, 30);
		w.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1, center.z, 60, 0.6, 1.0, 0.6, 0.2);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : aroundPoint(p, w, center, radius)) {
			e.damage(src, (float) sc(tier, 6.0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(tier, 80), 6));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, dur(tier, 60), 0));
			Vec3d pull = center.subtract(e.getPos()).multiply(0.3);
			knock(e, pull.x, 0.1, pull.z);
		}
		return true;
	}

	// ------------------------------------------------------------------ ASTRA

	private static void starfall(ServerPlayerEntity p, ServerWorld w, int tier) {
		Vec3d target = rayEnd(p, w, rg(tier, 30));
		double radius = rg(tier, 4.5);
		particleLine(w, ParticleTypes.END_ROD, target.add(0, 18, 0), target, 0.6);
		particleLine(w, ParticleTypes.FLAME, target.add(0, 18, 0), target, 1.2);
		sound(w, p, SoundEvents.ITEM_FIRECHARGE_USE, 1.0f, 0.6f);
		sound(w, p, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.6f);
		w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y + 0.5, target.z, 1, 0, 0, 0, 0);
		ring(w, ParticleTypes.FLAME, target.add(0, 0.2, 0), radius, 50);
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		for (LivingEntity e : aroundPoint(p, w, target, radius)) {
			e.damage(src, (float) sc(tier, 10.0));
			Vec3d away = e.getPos().subtract(target).multiply(1, 0, 1);
			if (away.lengthSquared() > 1.0E-4) away = away.normalize().multiply(sc(tier, 0.9));
			knock(e, away.x, 0.8, away.z);
		}
	}

	// ------------------------------------------------------------------ BLOOD

	private static boolean bloodDrain(ServerPlayerEntity p, ServerWorld w, int tier) {
		LivingEntity target = firstTarget(p, w, rg(tier, 10));
		if (target == null) return noTarget(p);

		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		target.damage(src, (float) sc(tier, 8.0));
		p.heal((float) sc(tier, 6.0));
		sound(w, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.8f);
		particleLine(w, ParticleTypes.DAMAGE_INDICATOR, target.getEyePos(), p.getEyePos(), 0.6);
		return true;
	}

	private static boolean bloodPact(ServerPlayerEntity p, ServerWorld w, int tier) {
		if (p.getHealth() <= 10.0f) {
			p.sendMessage(Text.literal("Not enough health for a Blood Pact.").formatted(Formatting.RED), true);
			return false;
		}
		p.setHealth(p.getHealth() - 8.0f); // pay 4 hearts, can never kill you
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, dur(tier, 300), amp(tier, 2)));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, dur(tier, 300), amp(tier, 1)));
		sound(w, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.4f, 0.6f);
		w.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getBodyY(0.5), p.getZ(), 20, 0.5, 0.8, 0.5, 0.1);
		return true;
	}

	// ------------------------------------------------------------------ DIVINE

	private static boolean smite(ServerPlayerEntity p, ServerWorld w, int tier) {
		LivingEntity target = firstTarget(p, w, rg(tier, 24));
		if (target == null) return noTarget(p);

		float damage = (float) sc(tier, 10.0);
		if (target instanceof net.minecraft.entity.mob.ZombieEntity
				|| target instanceof net.minecraft.entity.mob.AbstractSkeletonEntity
				|| target instanceof net.minecraft.entity.mob.PhantomEntity
				|| target instanceof net.minecraft.entity.mob.ZoglinEntity
				|| target instanceof net.minecraft.entity.boss.WitherEntity) {
			damage *= 2.0f; // holy damage vs undead
		}
		DamageSource src = w.getDamageSources().indirectMagic(p, p);
		target.damage(src, damage);
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, dur(tier, 100), 0));

		Vec3d pos = target.getPos();
		particleLine(w, ParticleTypes.END_ROD, pos.add(0, 14, 0), pos, 0.5);
		w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1, pos.z, 30, 0.4, 0.8, 0.4, 0.2);
		sound(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
		sound(w, p, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
		return true;
	}

	private static boolean divineShield(ServerPlayerEntity p, ServerWorld w, int tier) {
		double radius = rg(tier, 8);
		sound(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.6f);
		ring(w, ParticleTypes.END_ROD, p.getPos().add(0, 0.5, 0), radius, 70);
		List<ServerPlayerEntity> allies = w.getEntitiesByClass(ServerPlayerEntity.class,
				p.getBoundingBox().expand(radius),
				e -> e.isAlive() && e.squaredDistanceTo(p) <= radius * radius && (e == p || GemTrust.isTrusted(p, e)));
		for (ServerPlayerEntity ally : allies) {
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, dur(tier, 160), amp(tier, 1)));
			ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, dur(tier, 100), 1));
			w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, ally.getX(), ally.getBodyY(0.5), ally.getZ(), 20, 0.4, 0.8, 0.4, 0.2);
		}
		return true;
	}

	// ------------------------------------------------------------------ helpers

	/** The nearest living entity (not you, not trusted) along your line of sight, or null. */
	/** Living entities (not you, not trusted) within `radius` of a point. */
	static List<LivingEntity> aroundPoint(ServerPlayerEntity p, ServerWorld w, Vec3d center, double radius) {
		return w.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(radius),
				e -> e != p && e.isAlive() && !e.isSpectator() && !GemTrust.isTrusted(p, e)
						&& e.getPos().distanceTo(center) <= radius);
	}

	static LivingEntity firstTarget(ServerPlayerEntity p, ServerWorld w, double range) {
		Vec3d start = p.getEyePos();
		Vec3d end = rayEnd(p, w, range);
		List<LivingEntity> hits = alongRay(p, w, start, end, 1.3);
		return hits.isEmpty() ? null : hits.get(0);
	}

	static boolean noTarget(ServerPlayerEntity p) {
		p.sendMessage(Text.literal("No target in sight.").formatted(Formatting.RED), true);
		return false;
	}

	static Vec3d look(ServerPlayerEntity p) {
		return p.getRotationVec(1.0f);
	}

	static void sound(ServerWorld w, ServerPlayerEntity p, SoundEvent sound, float volume, float pitch) {
		w.playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
	}

	static void knock(LivingEntity e, double x, double y, double z) {
		e.addVelocity(x, y, z);
		e.velocityModified = true;
	}

	/** Where a ray from the player's eyes ends: the first solid block, or max range. */
	static Vec3d rayEnd(ServerPlayerEntity p, ServerWorld w, double range) {
		Vec3d start = p.getEyePos();
		Vec3d end = start.add(look(p).multiply(range));
		BlockHitResult hit = w.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
	}

	/** Living entities (not the caster, not trusted players) within `radius` of the segment, nearest first. */
	static List<LivingEntity> alongRay(ServerPlayerEntity p, ServerWorld w, Vec3d from, Vec3d to, double radius) {
		Box area = new Box(from, to).expand(radius + 1.0);
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, area,
				x -> x != p && x.isAlive() && !x.isSpectator() && !GemTrust.isTrusted(p, x))) {
			Vec3d center = e.getBoundingBox().getCenter();
			if (distanceToSegment(center, from, to) <= radius + e.getWidth() / 2.0) {
				out.add(e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(from)));
		return out;
	}

	/** Living entities around the caster, excluding the caster and anyone they trust. */
	static List<LivingEntity> nearby(ServerPlayerEntity p, ServerWorld w, double radius) {
		return w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(radius),
				e -> e != p && e.isAlive() && !e.isSpectator() && !GemTrust.isTrusted(p, e)
						&& e.squaredDistanceTo(p) <= radius * radius);
	}

	private static double distanceToSegment(Vec3d point, Vec3d a, Vec3d b) {
		Vec3d ab = b.subtract(a);
		double lengthSq = ab.lengthSquared();
		if (lengthSq < 1.0E-6) return point.distanceTo(a);
		double t = Math.max(0.0, Math.min(1.0, point.subtract(a).dotProduct(ab) / lengthSq));
		return point.distanceTo(a.add(ab.multiply(t)));
	}

	static void particleLine(ServerWorld w, ParticleEffect particle, Vec3d from, Vec3d to, double step) {
		Vec3d dir = to.subtract(from);
		double length = dir.length();
		if (length < 0.01) return;
		Vec3d unit = dir.multiply(1.0 / length);
		for (double d = 0; d <= length; d += step) {
			Vec3d pos = from.add(unit.multiply(d));
			w.spawnParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
		}
	}

	static void ring(ServerWorld w, ParticleEffect particle, Vec3d center, double radius, int count) {
		if (radius <= 0) return;
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0 * i) / count;
			w.spawnParticles(particle, center.x + Math.cos(angle) * radius, center.y + 0.1,
					center.z + Math.sin(angle) * radius, 1, 0, 0.1, 0, 0.0);
		}
	}
}
