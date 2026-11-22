package com.elementalgems;

import java.util.Arrays;
import java.util.Locale;

/**
 * Every gem in the mod: its colour, passive description, two active abilities and the Tier II ultimate.
 * Cooldowns are in ticks (20 ticks = 1 second).
 */
public enum GemType {
	SCULK("sculk", "Sculk", 0x167C8C,
			"Night Vision and immunity to Darkness",
			"Sonic Boom", "Fires a piercing sonic beam that hurts everything in a line", 20 * 20,
			"Sculk Pulse", "Blinds, slows and reveals everything nearby", 20 * 45,
			"Deafening Roar", "A massive sonic shockwave that blasts and blinds everything around you", 20 * 150),

	WEALTH("wealth", "Wealth", 0xF5C518,
			"Luck II and Hero of the Village",
			"Pockets", "Opens a portable 27-slot stash (your Ender Chest)", 20 * 5,
			"Gold Rush", "Haste III and Luck III for 30 seconds", 20 * 60,
			"Golden Aegis", "Huge Resistance, Regeneration, Haste and Absorption for you and trusted allies", 20 * 150),

	FIRE("fire", "Fire", 0xFF5A1F,
			"Fire Resistance",
			"Fireball", "Launches an explosive fireball", 20 * 12,
			"Fire Ring", "Ignites and blasts away everything around you", 20 * 40,
			"Inferno", "A huge firestorm that burns and blasts away everything nearby", 20 * 150),

	WATER("water", "Water", 0x2F7BFF,
			"Water Breathing and Dolphin's Grace",
			"Water Cannon", "Blasts a jet of water that knocks back and slows targets", 20 * 15,
			"Whirlpool", "Drags nearby enemies toward you and slows them", 20 * 40,
			"Tsunami", "A crushing wave that slams everything nearby away from you", 20 * 150),

	STRENGTH("strength", "Strength", 0xC62828,
			"Strength I",
			"Shockwave", "Ground slam that damages and launches nearby enemies", 20 * 20,
			"Berserk", "Strength III and Resistance I for 15 seconds", 20 * 60,
			"Titan's Wrath", "A devastating slam that launches and weakens everything nearby", 20 * 150),

	SPEED("speed", "Speed", 0x7CF2D0,
			"Speed I",
			"Dash", "Dash forward, slashing every enemy in your path", 20 * 8,
			"Blur", "Speed IV, Jump Boost, Haste and Strength for 15 seconds", 20 * 50,
			"Time Rush", "Hits everything nearby and boosts you to Speed V and Strength II", 20 * 150),

	LIFE("life", "Life", 0x3CD35B,
			"Health Boost I (+2 hearts)",
			"Mend", "Heals you and grants Regeneration", 20 * 20,
			"Life Aura", "Heals you and trusted players within 12 blocks", 20 * 50,
			"Tree of Life", "Massively heals you and trusted allies within 16 blocks", 20 * 150),

	PUFF("puff", "Puff", 0xDCEBFF,
			"Double jump (triple jump at Tier II) and no fall damage",
			"Cloud Leap", "Launches you high into the air", 20 * 10,
			"Gust", "Blows everything around you away", 20 * 30,
			"Cyclone", "Launches everything nearby high into the sky", 20 * 150),

	FLUX("flux", "Flux", 0xC24BFF,
			"Haste I",
			"Lightning Strike", "Calls lightning on the target you're looking at", 20 * 20,
			"Static Discharge", "Zaps, weakens and slows everyone nearby", 20 * 45,
			"Thunderstorm", "Calls lightning down on up to 6 nearby enemies", 20 * 150),

	ASTRA("astra", "Astra", 0x3B3FA8,
			"Jump Boost I",
			"Starfall", "Calls a meteor down on the spot you're looking at", 20 * 20,
			"Gravity Warp", "Makes nearby enemies float helplessly and glow", 20 * 45,
			"Black Hole", "Drags everything into a crushing singularity", 20 * 150),

	CRIMSON("crimson", "Crimson", 0xC2336B,
			"Fire Resistance, plus Strength I while in the Nether",
			"Hoglin Charge", "Charge forward, trampling and launching everything in your path", 20 * 14,
			"Spore Burst", "Poisons, sickens and slows everything nearby", 20 * 40,
			"Crimson Rage", "Withers everything nearby while you enter a frenzy", 20 * 150),

	END("end", "End", 0x8A63D2,
			"Saturation (you never go hungry)",
			"Ender Swap", "Swap places with the target you're looking at", 20 * 18,
			"Void Rift", "Opens a rift that roots, blinds and damages enemies", 20 * 30,
			"Dragon's Wrath", "A cone of dragon's breath that withers and blinds", 20 * 150),

	BLOOD("blood", "Blood", 0xC4142A,
			"Bloodlust: Strength and Speed while below half health",
			"Blood Drain", "Rip health from your target and heal yourself", 20 * 16,
			"Blood Pact", "Pay 4 hearts for Strength III and Speed II", 20 * 50,
			"Crimson Harvest", "Drain everything nearby to heal yourself", 20 * 150),

	DIVINE("divine", "Divine", 0xFFF3B0,
			"Resistance I",
			"Smite", "Call down holy light (double damage to undead)", 20 * 18,
			"Divine Shield", "Shields you and trusted players with Resistance and Regeneration", 20 * 55,
			"Heaven's Judgment", "Smites enemies and heals you and your trusted allies", 20 * 150);

	private final String id;
	private final String displayName;
	private final int color;
	private final String passiveDesc;
	private final String ability1Name;
	private final String ability1Desc;
	private final int ability1Cooldown;
	private final String ability2Name;
	private final String ability2Desc;
	private final int ability2Cooldown;
	private final String ultName;
	private final String ultDesc;
	private final int ultCooldown;

	GemType(String id, String displayName, int color, String passiveDesc,
			String ability1Name, String ability1Desc, int ability1Cooldown,
			String ability2Name, String ability2Desc, int ability2Cooldown,
			String ultName, String ultDesc, int ultCooldown) {
		this.id = id;
		this.displayName = displayName;
		this.color = color;
		this.passiveDesc = passiveDesc;
		this.ability1Name = ability1Name;
		this.ability1Desc = ability1Desc;
		this.ability1Cooldown = ability1Cooldown;
		this.ability2Name = ability2Name;
		this.ability2Desc = ability2Desc;
		this.ability2Cooldown = ability2Cooldown;
		this.ultName = ultName;
		this.ultDesc = ultDesc;
		this.ultCooldown = ultCooldown;
	}

	public String id() { return id; }
	public String displayName() { return displayName; }
	public int color() { return color; }
	public String passiveDesc() { return passiveDesc; }
	public String ability1Name() { return ability1Name; }
	public String ability1Desc() { return ability1Desc; }
	public int ability1Cooldown() { return ability1Cooldown; }
	public String ability2Name() { return ability2Name; }
	public String ability2Desc() { return ability2Desc; }
	public int ability2Cooldown() { return ability2Cooldown; }
	public String ultName() { return ultName; }
	public String ultDesc() { return ultDesc; }
	public int ultCooldown() { return ultCooldown; }

	public static GemType byId(String id) {
		if (id == null) return null;
		String needle = id.toLowerCase(Locale.ROOT);
		return Arrays.stream(values()).filter(g -> g.id.equals(needle)).findFirst().orElse(null);
	}
}
