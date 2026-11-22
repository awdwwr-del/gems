package com.elementalgems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** The three utility items: Gem Trader, Gem Upgrader and Energy Bottle. */
public final class GemUtilityItems {
	private GemUtilityItems() {
	}

	/**
	 * Right-click: swap your gem for a random different one (free). Consumed.
	 * Sneak + right-click: open a menu and choose the gem yourself for GemPicker.COST energy.
	 */
	public static class Trader extends Item {
		public Trader(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
				return TypedActionResult.success(stack);
			}

			GemType current = GemManager.carriedGem(player);
			if (current == null) {
				player.sendMessage(Text.literal("You have no gem to trade.").formatted(Formatting.RED), true);
				return TypedActionResult.fail(stack);
			}

			if (player.isSneaking()) {
				GemPicker.open(player, stack);
				return TypedActionResult.success(stack);
			}

			GemType next;
			do {
				next = GemType.values()[player.getRandom().nextInt(GemType.values().length)];
			} while (next == current);

			GemManager.swapGem(player, next);
			stack.decrement(1);

			GemType chosen = next;
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.3f);
			player.sendMessage(Text.literal("Your gem became the ").formatted(Formatting.GRAY)
					.append(Text.literal(chosen.displayName() + " Gem").styled(s -> s.withColor(chosen.color()).withBold(true)))
					.append(Text.literal("!").formatted(Formatting.GRAY)), false);
			return TypedActionResult.success(stack);
		}

		@Override
		public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
			tooltip.add(Text.literal("Right-click: swap to a random different gem (free).").formatted(Formatting.GRAY));
			tooltip.add(Text.literal("Sneak + right-click: choose your gem (" + GemPicker.COST + " energy).").formatted(Formatting.GRAY));
			tooltip.add(Text.literal("Your gem's tier is kept. Consumed on use.").formatted(Formatting.DARK_GRAY));
		}
	}

	/** Upgrades a Tier I gem to Tier II. Consumed on use. */
	public static class Upgrader extends Item {
		public Upgrader(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
				return TypedActionResult.success(stack);
			}

			net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
			boolean hasGem = false;
			for (int i = 0; i < inv.size(); i++) {
				if (inv.getStack(i).getItem() instanceof GemItem gemItem) {
					hasGem = true;
					if (gemItem.getTier() < 2) {
						inv.setStack(i, new ItemStack(ModItems.gem(gemItem.getGem(), 2)));
						stack.decrement(1);
						world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.4f);
						player.sendMessage(Text.literal("Your gem has been upgraded to Tier II! Press your Ultimate key (default R) to use its third move.")
								.formatted(Formatting.GOLD, Formatting.BOLD), false);
						return TypedActionResult.success(stack);
					}
				}
			}

			player.sendMessage(Text.literal(hasGem ? "Your gem is already Tier II." : "You have no gem to upgrade.")
					.formatted(Formatting.RED), true);
			return TypedActionResult.fail(stack);
		}

		@Override
		public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
			tooltip.add(Text.literal("Right-click to upgrade your gem to Tier II.").formatted(Formatting.GRAY));
			tooltip.add(Text.literal("+50% power, -25% cooldowns, stronger passive, plus an Ultimate.").formatted(Formatting.DARK_GRAY));
		}
	}

	/** Restores one point of energy. */
	public static class EnergyBottle extends Item {
		public EnergyBottle(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
				return TypedActionResult.success(stack);
			}

			int energy = GemManager.getEnergy(player);
			if (energy >= GemManager.MAX_ENERGY) {
				player.sendMessage(Text.literal("Your energy is already full.").formatted(Formatting.RED), true);
				return TypedActionResult.fail(stack);
			}

			GemManager.setEnergy(player, energy + 1);
			stack.decrement(1);
			world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6f, 1.6f);
			player.sendMessage(Text.literal("Energy: " + (energy + 1) + "/" + GemManager.MAX_ENERGY).formatted(Formatting.YELLOW), true);
			return TypedActionResult.success(stack);
		}

		@Override
		public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
			tooltip.add(Text.literal("Right-click to gain 1 energy.").formatted(Formatting.GRAY));
			tooltip.add(Text.literal("At 0 energy your gem stops working.").formatted(Formatting.DARK_GRAY));
		}
	}
}
