package com.elementalgems;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
	private static final Map<GemType, GemItem> TIER_1 = new EnumMap<>(GemType.class);
	private static final Map<GemType, GemItem> TIER_2 = new EnumMap<>(GemType.class);

	public static Item GEM_TRADER;
	public static Item GEM_UPGRADER;
	public static Item ENERGY_BOTTLE;

	private ModItems() {
	}

	/** The gem item for a type and tier (1 or 2). */
	public static GemItem gem(GemType type, int tier) {
		return tier >= 2 ? TIER_2.get(type) : TIER_1.get(type);
	}

	public static void register() {
		for (GemType type : GemType.values()) {
			for (int tier = 1; tier <= 2; tier++) {
				GemItem item = new GemItem(type, tier, new Item.Settings()
						.maxCount(1)
						.rarity(tier == 2 ? Rarity.EPIC : Rarity.RARE)
						.fireproof());
				String path = type.id() + (tier == 2 ? "_gem_2" : "_gem");
				Registry.register(Registries.ITEM, Identifier.of(ElementalGems.MOD_ID, path), item);
				(tier == 2 ? TIER_2 : TIER_1).put(type, item);
			}
		}

		GEM_TRADER = Registry.register(Registries.ITEM, Identifier.of(ElementalGems.MOD_ID, "gem_trader"),
				new GemUtilityItems.Trader(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
		GEM_UPGRADER = Registry.register(Registries.ITEM, Identifier.of(ElementalGems.MOD_ID, "gem_upgrader"),
				new GemUtilityItems.Upgrader(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
		ENERGY_BOTTLE = Registry.register(Registries.ITEM, Identifier.of(ElementalGems.MOD_ID, "energy_bottle"),
				new GemUtilityItems.EnergyBottle(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));

		MythicItems.register();

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			TIER_1.values().forEach(entries::add);
			TIER_2.values().forEach(entries::add);
			entries.add(GEM_TRADER);
			entries.add(GEM_UPGRADER);
			entries.add(ENERGY_BOTTLE);
			MythicItems.WEAPONS.values().forEach(entries::add);
			entries.add(MythicItems.EARTHBREAKER);
			entries.add(MythicItems.PHOENIX_FEATHER);
		});
	}
}
