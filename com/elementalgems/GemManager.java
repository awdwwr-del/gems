package com.elementalgems;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles: giving each player their gem on first join, keeping gems on death (soulbound),
 * passive effects, the energy system and the cooldown HUD.
 *
 * Energy (Bliss-style): everyone starts with START_ENERGY. Killing a player gives +1,
 * dying costs 1. At 0 energy the gem goes dormant (no passive, no abilities) until you
 * use an Energy Bottle.
 */
public final class GemManager {
	public static final int START_ENERGY = 5;
	public static final int MAX_ENERGY = 10;

	/** Persistent per-player data: the id of the gem they were assigned. */
	public static final AttachmentType<String> ASSIGNED_GEM = AttachmentRegistry.create(
			Identifier.of(ElementalGems.MOD_ID, "assigned_gem"),
			builder -> builder.persistent(Codec.STRING).copyOnDeath());

	/** Persistent per-player energy. */
	public static final AttachmentType<Integer> ENERGY = AttachmentRegistry.create(
			Identifier.of(ElementalGems.MOD_ID, "energy"),
			builder -> builder.persistent(Codec.INT).copyOnDeath());

	/** Gems taken off players at the moment of death, handed back as soon as they are alive again. */
	private static final Map<UUID, List<ItemStack>> DEATH_STASH = new HashMap<>();

	private static int tickCounter = 0;

	private GemManager() {
	}

	// ------------------------------------------------------------------ energy

	public static int getEnergy(ServerPlayerEntity player) {
		Integer energy = player.getAttached(ENERGY);
		return energy == null ? START_ENERGY : energy;
	}

	public static void setEnergy(ServerPlayerEntity player, int value) {
		player.setAttached(ENERGY, Math.max(0, Math.min(MAX_ENERGY, value)));
	}

	// ------------------------------------------------------------------ setup

	public static void register() {
		// First join -> random Tier I gem and starting energy.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			if (player.getAttached(ASSIGNED_GEM) == null) {
				GemType gem = GemType.values()[player.getRandom().nextInt(GemType.values().length)];
				setEnergy(player, START_ENERGY);
				assign(player, gem, 1);
				player.sendMessage(Text.literal("You have been bound to the ")
						.formatted(Formatting.GRAY)
						.append(Text.literal(gem.displayName() + " Gem").styled(s -> s.withColor(gem.color()).withBold(true)))
						.append(Text.literal("! Right-click to use it, sneak + right-click for its second ability.")
								.formatted(Formatting.GRAY)));
			}
		});

		// Phoenix Feather: brings you back from a killing blow (registered first so it can cancel the death).
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player
					&& !source.isOf(DamageTypes.OUT_OF_WORLD)
					&& !source.isOf(DamageTypes.GENERIC_KILL)
					&& MythicItems.tryPhoenix(player)) {
				return false;
			}
			return true;
		});

		// Soulbound: lift gems out of the inventory just before death drops happen.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player) {
				stashGems(player);
			}
			return true;
		});

		// Energy: -1 for dying, +1 for killing another player.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof ServerPlayerEntity victim)) return;

			int left = getEnergy(victim) - 1;
			setEnergy(victim, left);
			victim.sendMessage(Text.literal("You lost 1 energy (" + Math.max(0, left) + "/" + MAX_ENERGY + ").")
					.formatted(left <= 0 ? Formatting.RED : Formatting.YELLOW), false);
			if (left <= 0) {
				victim.sendMessage(Text.literal("Your gem is dormant. Use an Energy Bottle or kill a player to wake it.")
						.formatted(Formatting.RED), false);
			}

			if (source.getAttacker() instanceof ServerPlayerEntity killer && killer != victim) {
				int gained = Math.min(MAX_ENERGY, getEnergy(killer) + 1);
				setEnergy(killer, gained);
				killer.sendMessage(Text.literal("+1 energy (" + gained + "/" + MAX_ENERGY + ")").formatted(Formatting.GREEN), true);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(GemManager::onServerTick);
	}

	// ------------------------------------------------------------------ gem handling

	/** Records the gem as the player's own and puts it in their inventory (or drops it at their feet if full). */
	public static void assign(ServerPlayerEntity player, GemType gem, int tier) {
		player.setAttached(ASSIGNED_GEM, gem.id());
		giveGem(player, gem, tier);
	}

	public static void giveGem(ServerPlayerEntity player, GemType gem, int tier) {
		player.getInventory().offerOrDrop(new ItemStack(ModItems.gem(gem, tier)));
	}

	/** Removes every gem item the player carries. Returns how many stacks were removed. */
	public static int removeAllGems(ServerPlayerEntity player) {
		PlayerInventory inv = player.getInventory();
		int removed = 0;
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).getItem() instanceof GemItem) {
				inv.setStack(i, ItemStack.EMPTY);
				removed++;
			}
		}
		return removed;
	}

	/** Highest tier among the gems the player carries (0 if none). */
	public static int carriedTier(ServerPlayerEntity player) {
		int best = 0;
		PlayerInventory inv = player.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).getItem() instanceof GemItem gemItem) {
				best = Math.max(best, gemItem.getTier());
			}
		}
		return best;
	}

	/** The gem type the player currently carries (first one found), or null. */
	public static GemType carriedGem(ServerPlayerEntity player) {
		PlayerInventory inv = player.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).getItem() instanceof GemItem gemItem) return gemItem.getGem();
		}
		return null;
	}

	/** Replaces every gem the player carries with `next` (each keeps its tier). Returns false if they had none. */
	public static boolean swapGem(ServerPlayerEntity player, GemType next) {
		PlayerInventory inv = player.getInventory();
		boolean any = false;
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).getItem() instanceof GemItem old) {
				inv.setStack(i, new ItemStack(ModItems.gem(next, old.getTier())));
				any = true;
			}
		}
		if (any) player.setAttached(ASSIGNED_GEM, next.id());
		return any;
	}

	private static void stashGems(ServerPlayerEntity player) {
		PlayerInventory inv = player.getInventory();
		List<ItemStack> gems = new ArrayList<>();
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			if (stack.getItem() instanceof GemItem) {
				gems.add(stack.copy());
				inv.setStack(i, ItemStack.EMPTY);
			}
		}
		if (!gems.isEmpty()) {
			DEATH_STASH.computeIfAbsent(player.getUuid(), id -> new ArrayList<>()).addAll(gems);
		}
	}

	// ------------------------------------------------------------------ tick

	private static void onServerTick(MinecraftServer server) {
		// Return stashed gems as soon as the player is alive again (respawned, or saved by a totem).
		if (!DEATH_STASH.isEmpty()) {
			Iterator<Map.Entry<UUID, List<ItemStack>>> it = DEATH_STASH.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, List<ItemStack>> entry = it.next();
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
				if (player != null && player.isAlive() && !player.isDead()) {
					for (ItemStack stack : entry.getValue()) {
						player.getInventory().offerOrDrop(stack);
					}
					it.remove();
				}
			}
		}

		// Cooldown / energy HUD, twice a second.
		if (server.getTicks() % 10 == 0) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (player.isAlive() && !player.isSpectator()) showHud(player);
			}
		}

		// Passives, once per second.
		if (++tickCounter < 20) return;
		tickCounter = 0;

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!player.isAlive() || player.isSpectator()) continue;
			if (getEnergy(player) <= 0) continue; // dormant gem

			Map<GemType, Integer> best = new EnumMap<>(GemType.class);
			PlayerInventory inv = player.getInventory();
			for (int i = 0; i < inv.size(); i++) {
				if (inv.getStack(i).getItem() instanceof GemItem gemItem) {
					best.merge(gemItem.getGem(), gemItem.getTier(), Math::max);
				}
			}
			for (Map.Entry<GemType, Integer> e : best.entrySet()) {
				GemAbilities.applyPassive(e.getKey(), e.getValue(), player);
			}
		}
	}

	private static void showHud(ServerPlayerEntity player) {
		GemItem item = null;
		if (player.getMainHandStack().getItem() instanceof GemItem main) {
			item = main;
		} else if (player.getOffHandStack().getItem() instanceof GemItem off) {
			item = off;
		}
		if (item == null) return;

		GemType gem = item.getGem();
		int energy = getEnergy(player);

		MutableText text = Text.literal(gem.displayName() + (item.getTier() >= 2 ? " II" : ""))
				.styled(s -> s.withColor(gem.color()).withBold(true));
		text.append(Text.literal("   Energy " + energy).formatted(energy <= 0 ? Formatting.RED : Formatting.YELLOW));
		text.append(Text.literal("   "));
		text.append(cooldownText(gem.ability1Name(), GemCooldowns.remaining(player, gem, false)));
		text.append(Text.literal("   "));
		text.append(cooldownText(gem.ability2Name(), GemCooldowns.remaining(player, gem, true)));
		if (item.getTier() >= 2) {
			text.append(Text.literal("   "));
			text.append(cooldownText("Ult[R] " + gem.ultName(), GemCooldowns.remainingUltimate(player, gem)));
		}
		player.sendMessage(text, true);
	}

	private static Text cooldownText(String name, int ticks) {
		if (ticks <= 0) {
			return Text.literal(name + " READY").formatted(Formatting.GREEN);
		}
		return Text.literal(name + " " + ((ticks + 19) / 20) + "s").formatted(Formatting.RED);
	}
}
