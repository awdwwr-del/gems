package com.elementalgems;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

/**
 * /gems info                              - everyone: lists every gem and what it does
 * /gems give <player> <gem> [tier]        - op: replaces the player's gem with the given one
 * /gems reroll <player>                   - op: swaps the player's gem for a different random one (tier kept)
 * /gems energy <player> <0-10>            - op: sets a player's energy
 * /trust add|remove <player>, /trust list - everyone: manage who your abilities ignore
 *
 * Utility items can be handed out with vanilla /give:
 *   elementalgems:gem_trader, elementalgems:gem_upgrader, elementalgems:energy_bottle
 */
public final class GemCommand {
	private GemCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("gems")
					.then(CommandManager.literal("info").executes(GemCommand::info))
					.then(CommandManager.literal("ultimate").executes(ctx ->
							GemUltimates.tryUse(ctx.getSource().getPlayerOrThrow()) ? 1 : 0))
					.then(CommandManager.literal("give").requires(s -> s.hasPermissionLevel(2))
							.then(CommandManager.argument("player", EntityArgumentType.player())
									.then(CommandManager.argument("gem", StringArgumentType.word())
											.suggests((ctx, builder) -> CommandSource.suggestMatching(
													Arrays.stream(GemType.values()).map(GemType::id), builder))
											.executes(ctx -> give(ctx, 1))
											.then(CommandManager.argument("tier", IntegerArgumentType.integer(1, 2))
													.executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "tier")))))))
					.then(CommandManager.literal("reroll").requires(s -> s.hasPermissionLevel(2))
							.then(CommandManager.argument("player", EntityArgumentType.player())
									.executes(GemCommand::reroll)))
					.then(CommandManager.literal("energy").requires(s -> s.hasPermissionLevel(2))
							.then(CommandManager.argument("player", EntityArgumentType.player())
									.then(CommandManager.argument("amount", IntegerArgumentType.integer(0, GemManager.MAX_ENERGY))
											.executes(GemCommand::energy)))));

			dispatcher.register(CommandManager.literal("trust")
					.then(CommandManager.literal("add")
							.then(CommandManager.argument("player", EntityArgumentType.player())
									.executes(GemCommand::trustAdd)))
					.then(CommandManager.literal("remove")
							.then(CommandManager.argument("player", EntityArgumentType.player())
									.executes(GemCommand::trustRemove)))
					.then(CommandManager.literal("list").executes(GemCommand::trustList)));
		});
	}

	// ------------------------------------------------------------------ /gems

	private static int info(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		for (GemType gem : GemType.values()) {
			source.sendFeedback(() -> Text.literal(gem.displayName() + " Gem").styled(s -> s.withColor(gem.color()).withBold(true))
					.append(Text.literal(" - " + gem.passiveDesc()).formatted(Formatting.GRAY)), false);
			source.sendFeedback(() -> Text.literal("   Right-click: " + gem.ability1Name() + " | Sneak + Right-click: " + gem.ability2Name())
					.formatted(Formatting.DARK_GRAY), false);
		}
		return 1;
	}

	private static int give(CommandContext<ServerCommandSource> ctx, int tier) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
		GemType gem = GemType.byId(StringArgumentType.getString(ctx, "gem"));
		if (gem == null) {
			ctx.getSource().sendError(Text.literal("Unknown gem. Use /gems info to see the list."));
			return 0;
		}
		GemManager.removeAllGems(target);
		GemManager.assign(target, gem, tier);
		ctx.getSource().sendFeedback(() -> Text.literal("Gave " + gem.displayName() + " Gem (tier " + tier + ") to ")
				.append(target.getDisplayName()), true);
		return 1;
	}

	private static int reroll(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
		GemType current = GemType.byId(target.getAttached(GemManager.ASSIGNED_GEM));
		GemType gem;
		do {
			gem = GemType.values()[target.getRandom().nextInt(GemType.values().length)];
		} while (gem == current);

		int tier = Math.max(1, GemManager.carriedTier(target));
		GemManager.removeAllGems(target);
		GemManager.assign(target, gem, tier);
		GemType finalGem = gem;
		ctx.getSource().sendFeedback(() -> Text.literal("Rerolled ")
				.append(target.getDisplayName())
				.append(Text.literal(" into the " + finalGem.displayName() + " Gem")), true);
		return 1;
	}

	private static int energy(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
		int amount = IntegerArgumentType.getInteger(ctx, "amount");
		GemManager.setEnergy(target, amount);
		ctx.getSource().sendFeedback(() -> Text.literal("Set energy of ")
				.append(target.getDisplayName())
				.append(Text.literal(" to " + amount)), true);
		return 1;
	}

	// ------------------------------------------------------------------ /trust

	private static int trustAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity owner = ctx.getSource().getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
		if (target == owner) {
			ctx.getSource().sendError(Text.literal("You can't trust yourself - you are always safe from your own gem."));
			return 0;
		}
		if (!GemTrust.add(owner, target)) {
			ctx.getSource().sendError(Text.literal("You already trust that player."));
			return 0;
		}
		ctx.getSource().sendFeedback(() -> Text.literal("Your gem abilities will no longer hit ")
				.append(target.getDisplayName()).append(Text.literal(". They should /trust you back.")), false);
		return 1;
	}

	private static int trustRemove(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity owner = ctx.getSource().getPlayerOrThrow();
		ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
		if (!GemTrust.remove(owner, target)) {
			ctx.getSource().sendError(Text.literal("You don't trust that player."));
			return 0;
		}
		ctx.getSource().sendFeedback(() -> Text.literal("You no longer trust ").append(target.getDisplayName()), false);
		return 1;
	}

	private static int trustList(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity owner = ctx.getSource().getPlayerOrThrow();
		List<String> entries = GemTrust.entries(owner);
		if (entries.isEmpty()) {
			ctx.getSource().sendFeedback(() -> Text.literal("You don't trust anyone."), false);
			return 0;
		}
		String names = String.join(", ", entries.stream().map(GemTrust::nameOf).toList());
		ctx.getSource().sendFeedback(() -> Text.literal("Trusted: " + names), false);
		return entries.size();
	}
}
