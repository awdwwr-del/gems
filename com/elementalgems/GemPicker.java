package com.elementalgems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Chest-style menu opened by sneak + right-clicking a Gem Trader.
 * Clicking a gem swaps yours for it, costs COST energy and uses up the Trader. Nothing can be taken out.
 */
public class GemPicker extends GenericContainerScreenHandler {
	public static final int COST = 2;

	private final ItemStack trader;

	private GemPicker(int syncId, PlayerInventory playerInv, SimpleInventory menu, ItemStack trader) {
		super(ScreenHandlerType.GENERIC_9X2, syncId, playerInv, menu, 2);
		this.trader = trader;
	}

	public static void open(ServerPlayerEntity player, ItemStack trader) {
		int tier = Math.max(1, GemManager.carriedTier(player));
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
			SimpleInventory menu = new SimpleInventory(18);
			GemType[] types = GemType.values();
			for (int i = 0; i < types.length; i++) {
				menu.setStack(i, new ItemStack(ModItems.gem(types[i], tier)));
			}
			return new GemPicker(syncId, inv, menu, trader);
		}, Text.literal("Choose your gem (" + COST + " energy)")));
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		GemType[] types = GemType.values();
		if (slotIndex >= 0 && slotIndex < types.length && player instanceof ServerPlayerEntity serverPlayer) {
			choose(serverPlayer, types[slotIndex]);
			return;
		}
		this.syncState(); // ignore every other click and resync the client
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	private void choose(ServerPlayerEntity player, GemType type) {
		if (trader.isEmpty() || trader.getItem() != ModItems.GEM_TRADER) {
			player.closeHandledScreen();
			return;
		}
		if (GemManager.carriedGem(player) == type) {
			player.sendMessage(Text.literal("You already have that gem.").formatted(Formatting.RED), true);
			return;
		}
		int energy = GemManager.getEnergy(player);
		if (energy <= COST) {
			player.sendMessage(Text.literal("You need more than " + COST + " energy to choose a gem.").formatted(Formatting.RED), true);
			player.closeHandledScreen();
			return;
		}
		if (!GemManager.swapGem(player, type)) {
			player.sendMessage(Text.literal("You have no gem to trade.").formatted(Formatting.RED), true);
			player.closeHandledScreen();
			return;
		}

		GemManager.setEnergy(player, energy - COST);
		trader.decrement(1);
		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.3f);
		player.sendMessage(Text.literal("You chose the ").formatted(Formatting.GRAY)
				.append(Text.literal(type.displayName() + " Gem").styled(s -> s.withColor(type.color()).withBold(true)))
				.append(Text.literal(" (-" + COST + " energy).").formatted(Formatting.GRAY)), false);
		player.closeHandledScreen();
	}
}
