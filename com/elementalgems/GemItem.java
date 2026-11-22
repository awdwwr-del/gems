package com.elementalgems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * A gem item (Tier I or Tier II). Right-click = ability 1, sneak + right-click = ability 2.
 * Every ability has its own cooldown, tracked in {@link GemCooldowns}.
 */
public class GemItem extends Item {
	private final GemType gem;
	private final int tier;

	public GemItem(GemType gem, int tier, Settings settings) {
		super(settings);
		this.gem = gem;
		this.tier = tier;
	}

	public GemType getGem() {
		return gem;
	}

	public int getTier() {
		return tier;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (!(world instanceof ServerWorld serverWorld) || !(user instanceof ServerPlayerEntity player)) {
			return TypedActionResult.success(stack);
		}

		if (GemManager.getEnergy(player) <= 0) {
			player.sendMessage(Text.literal("You are out of energy - your gem is dormant. Use an Energy Bottle.")
					.formatted(Formatting.RED), true);
			return TypedActionResult.fail(stack);
		}

		boolean second = player.isSneaking();
		int left = GemCooldowns.remaining(player, gem, second);
		if (left > 0) {
			String name = second ? gem.ability2Name() : gem.ability1Name();
			player.sendMessage(Text.literal(name + " is on cooldown: " + ((left + 19) / 20) + "s")
					.formatted(Formatting.RED), true);
			return TypedActionResult.fail(stack);
		}

		if (!GemAbilities.activate(gem, tier, second, player, serverWorld)) {
			return TypedActionResult.fail(stack); // couldn't fire (no target etc.): no cooldown spent
		}
		GemCooldowns.start(player, gem, second, GemCooldowns.cooldownFor(gem, tier, second));
		return TypedActionResult.success(stack);
	}

	@Override
	public Text getName(ItemStack stack) {
		return Text.literal(gem.displayName() + " Gem" + (tier >= 2 ? " II" : ""))
				.styled(s -> s.withColor(gem.color()));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal(tier >= 2 ? "Tier II (+50% power, -25% cooldowns)" : "Tier I - upgrade with a Gem Upgrader")
				.formatted(tier >= 2 ? Formatting.GOLD : Formatting.GRAY));
		tooltip.add(Text.literal("Passive: ").formatted(Formatting.GRAY)
				.append(Text.literal(gem.passiveDesc()).formatted(Formatting.WHITE)));
		tooltip.add(Text.literal("Right-click: ").formatted(Formatting.GRAY)
				.append(Text.literal(gem.ability1Name()).styled(s -> s.withColor(gem.color())))
				.append(Text.literal(" (" + GemCooldowns.cooldownFor(gem, tier, false) / 20 + "s)").formatted(Formatting.DARK_GRAY)));
		tooltip.add(Text.literal("  " + gem.ability1Desc()).formatted(Formatting.DARK_GRAY));
		tooltip.add(Text.literal("Sneak + Right-click: ").formatted(Formatting.GRAY)
				.append(Text.literal(gem.ability2Name()).styled(s -> s.withColor(gem.color())))
				.append(Text.literal(" (" + GemCooldowns.cooldownFor(gem, tier, true) / 20 + "s)").formatted(Formatting.DARK_GRAY)));
		tooltip.add(Text.literal("  " + gem.ability2Desc()).formatted(Formatting.DARK_GRAY));
		if (tier >= 2) {
			tooltip.add(Text.literal("Ultimate [R]: ").formatted(Formatting.GOLD)
					.append(Text.literal(gem.ultName()).styled(s -> s.withColor(gem.color())))
					.append(Text.literal(" (" + gem.ultCooldown() / 20 + "s)").formatted(Formatting.DARK_GRAY)));
			tooltip.add(Text.literal("  " + gem.ultDesc()).formatted(Formatting.DARK_GRAY));
		}
		tooltip.add(Text.literal("Soulbound - keeps on death").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
	}
}
