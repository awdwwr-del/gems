# Elemental Gems (Fabric 1.21.1)

A Bliss-SMP-style mod: every player is bound to a random elemental gem the first time they join.
Gems are soulbound (they stay with you on death), glow, are fireproof, and each has a passive effect plus two active abilities.

## Controls
- **Right-click** the gem: ability 1
- **Sneak + right-click**: ability 2
- Every ability has its **own cooldown**. Hold a gem to see the HUD above your hotbar: gem, energy, and each ability as READY or a countdown.

## Gems
| Gem | Passive | Right-click | Sneak + Right-click |
|---|---|---|---|
| Sculk | Night Vision, Darkness immunity | Sonic Boom (piercing beam) | Sculk Pulse (blind, slow, glow nearby) |
| Wealth | Luck II, Hero of the Village | Pockets (portable 27 slots = Ender Chest) | Gold Rush (Haste III + Luck III) |
| Fire | Fire Resistance | Fireball | Fire Ring (ignite + blast away) |
| Water | Water Breathing, Dolphin's Grace | Water Cannon | Whirlpool (pull + slow) |
| Strength | Strength I | Shockwave (ground slam) | Berserk (Strength III + Resistance) |
| Speed | Speed I | Dash (slashes everything in your path) | Blur (Speed IV, Jump, Haste, Strength) |
| Life | Health Boost I | Mend (heal + Regen) | Life Aura (heals you and trusted players) |
| Puff | Double jump (triple at Tier II) + no fall damage (no Slow Falling, so crits and mace smash work) | Cloud Leap | Gust (blow everything away) |
| Flux | Haste I | Lightning Strike | Static Discharge (zap, weaken, slow) |
| Astra | Jump Boost I | Starfall (meteor on the spot you look at) | Gravity Warp (levitate + glow nearby) |
| Crimson | Fire Resistance, Strength I in the Nether | Hoglin Charge (trample forward) | Spore Burst (poison, nausea, slow) |
| End | Saturation (never hungry) | Ender Swap (swap places with your target) | Void Rift (roots, blinds and damages enemies) |
| Blood | Bloodlust: Strength + Speed below half health | Blood Drain (steal health from a target) | Blood Pact (pay 4 hearts for Strength III + Speed II) |
| Divine | Resistance I | Smite (holy beam, double damage to undead) | Divine Shield (Resistance + Regen for you and trusted players) |

Abilities that need a target (Ender Swap, Blood Drain, Smite) don't use up their cooldown if nothing is in sight.
Blood Pact can never kill you; it needs more than 5 hearts.

Textures are 32x32: each gem has its own shape and symbol, and Tier II gets a gold frame. To use your own art, replace the PNGs in `src/main/resources/assets/elementalgems/textures/item/` (keep the same names).

## Tiers
Gems come in **Tier I** and **Tier II** (gold outline and sparkles). Tier II gives:
- +50% damage / healing / knockback
- +50% effect duration, +30% range and radius
- -25% cooldowns
- Stronger passive (one level higher)
- Extras: Fireball explodes bigger, Lightning Strike chains a second bolt

## Ultimates (Tier II only)
Every Tier II gem has a third move. Press **R** (rebindable in Controls > Elemental Gems) or run `/gems ultimate`.
- Costs **no energy** (your gem just has to be awake, so above 0 energy) and has a 150 second cooldown. The HUD shows `Ult[R]` READY or a countdown.
- Sculk **Deafening Roar**, Wealth **Golden Aegis**, Fire **Inferno**, Water **Tsunami**, Strength **Titan's Wrath**, Speed **Time Rush**,
  Life **Tree of Life**, Puff **Cyclone**, Flux **Thunderstorm**, Astra **Black Hole**, Crimson **Crimson Rage**, End **Dragon's Wrath**,
  Blood **Crimson Harvest**, Divine **Heaven's Judgment**.
- Hold a Tier II gem and read its tooltip for what its ultimate does.

## Items
| Item | What it does | Recipe |
|---|---|---|
| **Gem Upgrader** | Right-click: upgrades your Tier I gem to Tier II. Consumed. | Nether Star in the centre, 4 Netherite Ingots (edges), 4 Diamond Blocks (corners) |
| **Gem Trader** | Right-click: swaps your gem for a different random one, free. Sneak + right-click: opens a menu to **choose** your gem for 2 energy (you need more than 2). Tier kept, Trader consumed. | Netherite Ingot in the centre, 4 Diamond Blocks (edges), 4 Eyes of Ender (corners) |
| **Energy Bottle** | Right-click: +1 energy. | Experience Bottle in the centre, 4 Gold Blocks (edges), 4 Diamonds (corners) |

## Mythical items
All four weapons are unbreakable (they never lose durability), have netherite-beating damage and a right-click ability with a cooldown shown on the item.

| Item | On hit | Right-click | Signature ingredient |
|---|---|---|---|
| **Excalibur** | Bonus damage to undead | Holy Wave: damages and weakens enemies in front, heals you and trusted allies | Diamond Block |
| **Thunder Hammer** | 15% chance to strike lightning | Thunder Strike: lightning on your target | Lightning Rod |
| **Soul Reaper** | Heals you a little | Reap: drains everything close to you | Wither Skeleton Skull |
| **Frostbite** | Slows and freezes | Flash Freeze: freezes and damages everything close | Blue Ice |
| **Earthbreaker** (mace) | Keeps the vanilla smash attack, plus cleaves enemies around the target and slows it | Skyward Leap: launch yourself up, then crash down for a smash (12s) | A vanilla Mace |
| **Phoenix Feather** | - | Carry it: when a killing blow lands it burns up, brings you back at half health and scorches nearby enemies (not the void or `/kill`) | Totem of Undying |

Weapon recipe: Netherite Ingots on the sides and top corners, signature ingredient top centre, Nether Star in the middle, Blaze Rod handle.
Earthbreaker recipe: Nether Star top centre, a vanilla Mace in the middle, Netherite Ingots either side of both, Blaze Rod handle.
Phoenix Feather recipe: Blaze Rods on the corners, Netherite Ingots on the edges, Totem of Undying in the middle.

Recipes are the files in `src/main/resources/data/elementalgems/recipe/` - edit them to change the cost.

## Energy
- Everyone starts with **5 energy** (max 10).
- **Killing a player: +1.** **Dying: -1.**
- At **0 energy your gem goes dormant**: no passive, no abilities, until you drink an Energy Bottle.
- Tweak `START_ENERGY` / `MAX_ENERGY` in `GemManager.java`.

## Trust
- `/trust add <player>`, `/trust remove <player>`, `/trust list`
- Your abilities skip players you trust (no damage, debuffs or knockback) and your Life Aura heals them.
- Trust is one-way, so your friend has to `/trust` you back.
- Fireball and Lightning Strike are vanilla entities, so they can still hurt trusted players.

## Commands
- `/gems info` - anyone: list gems and abilities
- `/gems ultimate` - anyone with a Tier II gem: use your ultimate (same as the R key)
- `/gems give <player> <gem> [tier]` - op: replace a player's gem (tier 1 or 2)
- `/gems reroll <player>` - op: swap to a different random gem (tier kept)
- `/gems energy <player> <0-10>` - op: set energy
- Items for testing: `/give @s elementalgems:gem_trader`, `gem_upgrader`, `energy_bottle`, `excalibur`, `thunder_hammer`, `soul_reaper`, `frostbite`, `earthbreaker`, `phoenix_feather`

## Getting the jar (pick one)

**A. No Java needed - let GitHub build it**
1. Make a free GitHub account, create a new repository, and upload everything in this folder (including the hidden `.github` folder).
2. Open the repo's **Actions** tab, wait for the "build" run to go green (a few minutes), open it and download the **elemental-gems-jar** artifact.
3. Unzip it: `elemental-gems-1.0.0.jar` is the mod.

**B. Build locally** (needs JDK 21)
- Windows: `gradlew build`  |  Mac/Linux: `./gradlew build`
- The jar appears in `build/libs/elemental-gems-1.0.0.jar` (not the `-sources` one).

## Using it in Lunar Client
1. Open Lunar Client and pick **1.21.1** (mod loading works on 1.16.5+).
2. Bottom right, open **Mods**, then click the folder button (or drag and drop the files into the window).
3. Add **elemental-gems-1.0.0.jar**. If the game complains about a missing `fabric-api`, add the Fabric API 1.21.1 jar from modrinth.com/mod/fabric-api too.
4. Multiplayer: the **server** needs the same jar plus Fabric API (Fabric loader 1.21.1), and every player needs the jar in their client, because the gems are new items.
5. When you update the mod, remove the old jar first so you don't load two copies.

## Using it with the normal launcher
Install Fabric Loader for 1.21.1 (fabricmc.net/use), then put the jar and Fabric API into `.minecraft/mods`.

## Known limitations
- Gems can still be thrown away manually (Q / dropping from the inventory). Death never drops them, but a player can toss one on purpose.
- Gems in the death stash are held in memory, so they would be lost if the server crashes in the few seconds between death and respawn.
- Cooldowns are kept in memory, so they reset when the server restarts.
- Players who join before they have a gem get one; if you already had a gem from the old version it still works (it is Tier I).
- Adding a new gem: add an entry to `GemType`, a passive + two abilities in `GemAbilities` (a `case` in both `applyPassive` and `activate`), and `<id>_gem.png`, `<id>_gem_2.png`, models and lang entries (copy an existing gem's files and rename).
