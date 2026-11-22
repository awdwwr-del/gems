package com.elementalgems;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mythical items: four unbreakable weapons with a right-click ability, and the Phoenix Feather
 * (carry it and you are brought back from a killing blow, once).
 */
public final class MythicItems {
	public enum Kind {
		EXCALIBUR("excalibur", "Excalibur", 0xFFE07A, 6, -2.4f, 20 * 25,
				"Holy Wave", "Right-click: a wave of light damages and weakens enemies in front of you and heals you and trusted allies.",
				"Deals bonus damage to undead."),
		THUNDER_HAMMER("thunder_hammer", "Thunder Hammer", 0x7FB8FF, 8, -3.0f, 20 * 20,
				"Thunder Strike", "Right-click: calls lightning on the target you're looking at.",
				"15% chance to strike lightning on hit."),
		SOUL_REAPER("soul_reaper", "Soul Reaper", 0x6BE0C0, 6, -2.6f, 20 * 22,
				"Reap", "Right-click: drains the life of everything close to you and heals you.",
				"Heals you a little on every hit."),
		FROSTBITE("frostbite", "Frostbite", 0x9FE8FF, 5, -2.2f, 20 * 30,
				"Flash Freeze", "Right-click: freezes and damages everything close to you.",
				"Slows and freezes enemies on hit.");

		final String id;
		final String displayName;
		final int color;
		final int attackBonus;
		final float attackSpeed;
		final int cooldown;
		final String abilityName;
		final String abilityDesc;
		final String passiveDesc;

		Kind(String id, String displayName, int color, int attackBonus, float attackSpeed, int cooldown,
				String abilityName, String abilityDesc, String passiveDesc) {
			this.id = id;
			this.displayName = displayName;
			this.color = color;
			this.attackBonus = attackBonus;
			this.attackSpeed = attackSpeed;
			this.cooldown = cooldown;
			this.abilityName = abilityName;
			this.abilityDesc = abilityDesc;
			this.passiveDesc = passiveDesc;
		}
	}

	public static final Map<Kind, Item> WEAPONS = new EnumMap<>(Kind.class);
	public static Item EARTHBREAKER;
	public static Item PHOENIX_FEATHER;

	private MythicItems() {
	}

	public static void register() {
		for (Kind kind : Kind.values()) {
			Item weapon = new Weapon(kind, new Item.Settings());
			Registry.register(Registries.ITEM, Identifier.of(ElementalGems.MOD_ID, kind.id), weapon);
			WEAPONS.put(kind, weapon);
		}
		EARTHBREAKER = Registry.register(Registries.ITEM, Identifier.of(ElementalGems.MOD_ID, "earthbreaker"),
				new Earthbreaker(new Item.Settings()
						.maxDamage(500)
						.rarity(Rarity.EPIC)
						.fireproof()
						.component(DataComponentTypes.TOOL, MaceItem.createToolComponent())
						.attributeModifiers(MaceItem.createAttributeModifiers())));
		PHOENIX_FEATHER = Registry.register(Registries.ITEM, Identifier.of(ElementalGems.MOD_ID, "phoenix_feather"),
				new PhoenixFeather(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()));
	}

	// ------------------------------------------------------------------ weapons

	public static class Weapon extends SwordItem {
		private final Kind kind;

		public Weapon(Kind kind, Item.Settings settings) {
			super(ToolMaterials.NETHERITE, settings
					.attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, kind.attackBonus, kind.attackSpeed))
					.rarity(Rarity.EPIC)
					.fireproof());
			this.kind = kind;
		}

		/** Not calling super.postHit means the weapon never loses durability. */
		@Override
		public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			if (!(attacker.getWorld() instanceof ServerWorld world)) return true;
			switch (kind) {
				case EXCALIBUR -> {
					if (GemUltimates.isUndead(target)) {
						target.damage(world.getDamageSources().magic(), 4.0f);
						world.spawnParticles(ParticleTypes.END_ROD, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
					}
				}
				case THUNDER_HAMMER -> {
					if (attacker.getRandom().nextFloat() < 0.15f) {
						spawnBolt(world, target.getPos(), attacker instanceof ServerPlayerEntity sp ? sp : null);
					}
				}
				case SOUL_REAPER -> {
					attacker.heal(2.0f);
					world.spawnParticles(ParticleTypes.SOUL, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.3, 0.5, 0.3, 0.05);
				}
				case FROSTBITE -> {
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
					target.setFrozenTicks(160);
					world.spawnParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
				}
			}
			return true;
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!(world instanceof ServerWorld serverWorld) || !(user instanceof ServerPlayerEntity player)) {
				return TypedActionResult.pass(stack);
			}
			if (player.getItemCooldownManager().isCoolingDown(this)) {
				return TypedActionResult.fail(stack);
			}

			switch (kind) {
				case EXCALIBUR -> holyWave(player, serverWorld);
				case THUNDER_HAMMER -> thunderStrike(player, serverWorld);
				case SOUL_REAPER -> reap(player, serverWorld);
				case FROSTBITE -> flashFreeze(player, serverWorld);
			}
			player.getItemCooldownManager().set(this, kind.cooldown);
			player.sendMessage(Text.literal(kind.abilityName + "!").styled(s -> s.withColor(kind.color).withBold(true)), true);
			return TypedActionResult.success(stack);
		}

		@Override
		public boolean hasGlint(ItemStack stack) {
			return true;
		}

		@Override
		public Text getName(ItemStack stack) {
			return Text.literal(kind.displayName).styled(s -> s.withColor(kind.color));
		}

		@Override
		public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
			tooltip.add(Text.literal("Mythical weapon - never breaks").formatted(Formatting.GOLD, Formatting.ITALIC));
			tooltip.add(Text.literal(kind.passiveDesc).formatted(Formatting.GRAY));
			tooltip.add(Text.literal(kind.abilityDesc).formatted(Formatting.DARK_GRAY));
			tooltip.add(Text.literal("Cooldown: " + kind.cooldown / 20 + "s").formatted(Formatting.DARK_GRAY));
		}
	}

	private static void holyWave(ServerPlayerEntity p, ServerWorld w) {
		Vec3d look = GemAbilities.look(p);
		Vec3d eye = p.getEyePos();
		GemAbilities.sound(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.2f, 1.6f);
		GemAbilities.sound(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
		for (int i = -6; i <= 6; i++) {
			double angle = Math.toRadians(i * 6);
			Vec3d dir = new Vec3d(look.x * Math.cos(angle) - look.z * Math.sin(angle), look.y,
					look.x * Math.sin(angle) + look.z * Math.cos(angle));
			GemAbilities.particleLine(w, ParticleTypes.END_ROD, eye.add(dir), eye.add(dir.multiply(9)), 1.0);
		}
		DamageSource src = w.getDamageSources().playerAttack(p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 9)) {
			Vec3d to = e.getBoundingBox().getCenter().subtract(eye).normalize();
			if (to.dotProduct(look) < 0.5) continue;
			e.damage(src, GemUltimates.isUndead(e) ? 18.0f : 9.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
			GemAbilities.knock(e, look.x * 0.8, 0.3, look.z * 0.8);
		}
		List<ServerPlayerEntity> allies = w.getEntitiesByClass(ServerPlayerEntity.class, p.getBoundingBox().expand(9),
				e -> e.isAlive() && e.squaredDistanceTo(p) <= 81 && (e == p || GemTrust.isTrusted(p, e)));
		for (ServerPlayerEntity ally : allies) {
			ally.heal(4.0f);
		}
	}

	private static void thunderStrike(ServerPlayerEntity p, ServerWorld w) {
		LivingEntity target = GemAbilities.firstTarget(p, w, 30);
		Vec3d pos = target != null ? target.getPos() : GemAbilities.rayEnd(p, w, 30);
		spawnBolt(w, pos, p);
	}

	private static void reap(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.ENTITY_PHANTOM_SWOOP, 1.4f, 0.6f);
		GemAbilities.ring(w, ParticleTypes.SOUL_FIRE_FLAME, p.getPos().add(0, 0.5, 0), 6, 50);
		DamageSource src = w.getDamageSources().playerAttack(p);
		float healed = 0;
		for (LivingEntity e : GemAbilities.nearby(p, w, 6)) {
			e.damage(src, 6.0f);
			healed += 2.0f;
			GemAbilities.particleLine(w, ParticleTypes.SOUL, e.getEyePos(), p.getEyePos(), 0.6);
		}
		p.heal(Math.min(healed, 12.0f));
	}

	private static void flashFreeze(ServerPlayerEntity p, ServerWorld w) {
		GemAbilities.sound(w, p, SoundEvents.BLOCK_GLASS_BREAK, 1.4f, 0.6f);
		GemAbilities.ring(w, ParticleTypes.SNOWFLAKE, p.getPos().add(0, 1, 0), 7, 70);
		GemAbilities.ring(w, ParticleTypes.SNOWFLAKE, p.getPos().add(0, 1, 0), 3.5, 35);
		DamageSource src = w.getDamageSources().playerAttack(p);
		for (LivingEntity e : GemAbilities.nearby(p, w, 7)) {
			e.damage(src, 4.0f);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 4));
			e.setFrozenTicks(220);
		}
	}

	private static void spawnBolt(ServerWorld w, Vec3d pos, ServerPlayerEntity channeler) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt != null) {
			bolt.refreshPositionAfterTeleport(pos);
			if (channeler != null) bolt.setChanneler(channeler);
			w.spawnEntity(bolt);
		}
	}

	// ------------------------------------------------------------------ Earthbreaker (mythic mace)

	/**
	 * A mace that keeps the vanilla smash attack. Never breaks, cleaves nearby enemies on every hit,
	 * and right-click launches you skyward so you can come crashing down for a smash.
	 */
	public static class Earthbreaker extends MaceItem {
		private static final int LEAP_COOLDOWN = 20 * 12;
		private static final int COLOR = 0xFF9A3C;

		public Earthbreaker(Item.Settings settings) {
			super(settings);
		}

		@Override
		public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			boolean result = super.postHit(stack, target, attacker);
			stack.setDamage(0); // never breaks

			if (attacker instanceof ServerPlayerEntity player && player.getWorld() instanceof ServerWorld world) {
				DamageSource src = world.getDamageSources().playerAttack(player);
				for (LivingEntity e : GemAbilities.aroundPoint(player, world, target.getPos(), 3.5)) {
					if (e == target) continue;
					e.damage(src, 4.0f);
					Vec3d away = e.getPos().subtract(target.getPos()).multiply(1, 0, 1);
					if (away.lengthSquared() > 1.0E-4) away = away.normalize().multiply(0.8);
					GemAbilities.knock(e, away.x, 0.5, away.z);
				}
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
				GemAbilities.ring(world, ParticleTypes.CRIT, target.getPos().add(0, 0.2, 0), 3.0, 30);
			}
			return result;
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!(world instanceof ServerWorld serverWorld) || !(user instanceof ServerPlayerEntity player)) {
				return TypedActionResult.pass(stack);
			}
			if (player.getItemCooldownManager().isCoolingDown(this)) {
				return TypedActionResult.fail(stack);
			}

			Vec3d look = GemAbilities.look(player);
			player.setVelocity(look.x * 0.8, 1.5, look.z * 0.8);
			player.velocityModified = true;
			player.fallDistance = 0;
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 1));
			GemAbilities.sound(serverWorld, player, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.6f);
			serverWorld.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 30, 0.5, 0.1, 0.5, 0.1);

			player.getItemCooldownManager().set(this, LEAP_COOLDOWN);
			player.sendMessage(Text.literal("Skyward Leap!").styled(s -> s.withColor(COLOR).withBold(true)), true);
			return TypedActionResult.success(stack);
		}

		@Override
		public boolean hasGlint(ItemStack stack) {
			return true;
		}

		@Override
		public Text getName(ItemStack stack) {
			return Text.literal("Earthbreaker").styled(s -> s.withColor(COLOR));
		}

		@Override
		public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
			tooltip.add(Text.literal("Mythical weapon - never breaks").formatted(Formatting.GOLD, Formatting.ITALIC));
			tooltip.add(Text.literal("Keeps the mace smash attack. Every hit also cleaves enemies around the target.").formatted(Formatting.GRAY));
			tooltip.add(Text.literal("Right-click: Skyward Leap - launch yourself up, then smash down on someone.").formatted(Formatting.DARK_GRAY));
			tooltip.add(Text.literal("Cooldown: " + LEAP_COOLDOWN / 20 + "s").formatted(Formatting.DARK_GRAY));
		}
	}

	// ------------------------------------------------------------------ Phoenix Feather

	public static class PhoenixFeather extends Item {
		public PhoenixFeather(Settings settings) {
			super(settings);
		}

		@Override
		public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
			tooltip.add(Text.literal("Carry it in your inventory.").formatted(Formatting.GRAY));
			tooltip.add(Text.literal("When you would die, it burns up, brings you back to half health").formatted(Formatting.DARK_GRAY));
			tooltip.add(Text.literal("and scorches enemies around you. Does not save you from the void.").formatted(Formatting.DARK_GRAY));
		}
	}

	/** Called before a player dies. Returns true if a Phoenix Feather saved them. */
	public static boolean tryPhoenix(ServerPlayerEntity player) {
		PlayerInventory inv = player.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			if (stack.isOf(PHOENIX_FEATHER)) {
				stack.decrement(1);
				ServerWorld w = player.getServerWorld();
				player.clearStatusEffects();
				player.setHealth(player.getMaxHealth() * 0.5f);
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 1));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 400, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));

				w.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
				w.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getBodyY(0.5), player.getZ(), 80, 0.6, 1.0, 0.6, 0.2);
				GemAbilities.ring(w, ParticleTypes.LAVA, player.getPos().add(0, 0.5, 0), 4, 20);

				DamageSource src = w.getDamageSources().indirectMagic(player, player);
				for (LivingEntity e : GemAbilities.nearby(player, w, 5)) {
					e.setOnFireFor(6);
					e.damage(src, 6.0f);
					Vec3d away = e.getPos().subtract(player.getPos()).multiply(1, 0, 1);
					if (away.lengthSquared() > 1.0E-4) away = away.normalize().multiply(1.2);
					GemAbilities.knock(e, away.x, 0.5, away.z);
				}
				player.sendMessage(Text.literal("The Phoenix Feather burns up and brings you back!").formatted(Formatting.GOLD, Formatting.BOLD), false);
				return true;
			}
		}
		return false;
	}
}
