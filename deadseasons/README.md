# Dead Seasons — Zombie Apocalypse mod (Forge 1.20.1)

## Building the mod on Windows 11

1. **Install Java 17 (one time only).** Open *Command Prompt* and run:
   ```
   winget install EclipseAdoptium.Temurin.17.JDK
   ```
   Close Command Prompt and open a new one afterwards.
2. **Unzip** `deadseasons-project.zip` somewhere easy, e.g. `C:\mods\deadseasons`.
3. **Open a terminal in that folder.** In File Explorer, open the `deadseasons` folder (the one with `gradlew.bat` in it), click the address bar, type `cmd`, and press Enter.
4. **Build:**
   ```
   gradlew.bat build
   ```
   The first build downloads Minecraft and Forge (about 1–2 GB) and can take 10–20 minutes. Later builds take about a minute.
5. When it says `BUILD SUCCESSFUL`, the mod is at:
   ```
   build\libs\deadseasons-1.20.1-1.0.5.jar
   ```
   If the build fails, copy the red error lines and send them to Claude to fix.

## Installing it with the Modrinth App

1. Open your Forge 1.20.1 instance in the Modrinth App.
2. Click the **⋮** (three dots) menu → **Open folder**.
3. Open the `mods` folder and copy `deadseasons-1.20.1-1.0.5.jar` into it.
4. Launch the game and **create a new world** (towns only generate in new chunks).

It works alongside Create and Create: Big Cannons; there are no shared IDs.

---

## What's in the mod

### Zombies (spawn alongside vanilla mobs)
| Zombie | Burns in sun? | What it does |
|---|---|---|
| Walker | Yes | The common shambler |
| Runner | Yes | Fast and fragile, hunts at night |
| Brute | No | 60 HP, smashes doors and barricades on any difficulty |
| Screamer | Yes | Shrieks when it sees you: every zombie within 48 blocks comes for you, plus reinforcements |
| Soldier Zombie | No | Military gear, drops ammo |
| Riot Zombie | No | Bullets and arrows from the front do 70% less damage. Flank it or use melee |
| Spitter | Yes | Spits poisonous acid from range |
| Bloater | No | Bursts into a poison cloud when close or killed (gas mask blocks it) |
| Crawler | No | Low to the ground, slows you when it grabs you |
| Frostbitten | No | Cold biomes and everywhere in Winter; freezes you |
| Abomination | No | Blood Moon boss, 260 HP |

Zombies that kill villagers can turn them into zombie villagers. Zombies get slowly tougher over 150 days.

### Seasons (35 days each, every world starts in Summer)
- **Summer:** hot, thirst drains faster. **Autumn:** orange leaves, crops grow slower, fewer animals.
- **Winter:** snow piles up, water freezes, outdoor crops **stop growing**, far fewer animals, Frostbitten everywhere.
- **Spring:** snow melts, crops grow normally.
- **Greenhouses:** crops with any glass, glass pane or Grow Lamp above them (within 12 blocks) grow all year. A Grow Lamp within 2 blocks also works for underground farms.

### Survival
- **Thirst** (water drops above your hunger bar). Water bottles are dirty and can make you sick. Boil them in a furnace or campfire, filter them (bottle + charcoal + paper), or use a **Rain Collector**.
- **Body temperature** (shown right of the hotbar). Affected by season, biome, night, rain, shelter, water, fires and clothing. Winter clothes keep you warm; summer clothes keep you cool.
- **Infection:** zombie hits have a 10% chance to infect you. You have **7 days** to take **Antibiotics** or you die. Milk does not cure it.

### Weapons
- **Guns:** Pistol, Revolver, SMG, Pump Shotgun, Hunting Rifle, Assault Rifle. Right-click to fire (hold for SMG/Assault Rifle). **R** or **Sneak + right-click** to reload. Headshots do extra damage. Guns wear out; repair them with Gun Parts in an anvil.
- **Ammo:** Handgun Ammo, Shotgun Shells, Rifle Ammo.
- **Melee:** Baseball Bat, Spiked Bat, Machete, Crowbar, Fire Axe, Katana, Sledgehammer.
- **Noise:** gunshots, explosions, alarms, sprinting and engines attract zombies.

### Armor & clothing
Military, Riot, Winter (warmth), Summer (cooling), and a Gas Mask.

### Base defenses
Barbed Wire, Spike Trap, Reinforced Wall, Reinforced Door (zombies can't break it), Wooden and Metal Barricades (zombies claw through them; repair with planks or iron), Landmine, Alarm Bell (rings and gives a redstone signal), Floodlight (zombies can't spawn in bright light), and Auto Turret (load it by right-clicking with any ammo).

### Events
- **Blood Moon** every 60 days: red fog, waves of zombies, an Abomination at midnight, and no sleeping.
- **Fog Nights:** thick fog and extra Crawlers and Runners.
- **Supply Airdrops** every 3–7 days: coordinates in chat, smoke you can follow, and zombie guards.
- **Migrating Herds:** a large herd walks through your area.
- **Horde Raids:** a horde tracks you down at night every 8–14 days.

### Towns
- **Ruined towns:** houses, stores, police stations, clinics, gas stations and military camps, with loot and zombies.
- **Survivor settlements:** walled camps with traders, guards, greenhouse farms and a radio.

### Survivors
Give a survivor food to try to recruit them (35% chance each try).
- **Right-click** (empty hand): switch between Follow / Guard / Farm / Wander.
- **Sneak + right-click** with a weapon or armor: give it to them. They can use guns.
- **Sneak + right-click** (empty hand): take their weapon back, or dismiss them if they're unarmed.
- **Right-click with food:** heal them.

### Box Truck (very rare — found in about 4% of ruined towns)
- Right-click to drive: **W/S** to move, **A/D** to steer. Fill it with **Gas Cans** and repair it with iron or scrap.
- Runs over zombies.
- **Sneak + right-click** to enter the cargo area, a room you can build in. Use the cargo door to step out.
- Moving your truck around makes horde raids much less likely to find you.

### Emergency Radio
Right-click to see the day, season, next Blood Moon, last airdrop, and incoming raids or herds.

### Commands (cheats on)
`/deadseasons status`, `/deadseasons season <summer|autumn|winter|spring>`, `/deadseasons bloodmoon`, `/deadseasons fog`, `/deadseasons airdrop`, `/deadseasons herd`, `/deadseasons raid`, `/deadseasons infect`, `/deadseasons cure`, `/deadseasons thirst <0-20>`

### Config
`config/deadseasons-common.toml` (created after the first launch) controls season length, Blood Moon interval, infection chance and days, thirst rate, spawn pressure, event frequency, truck rarity and more.

### Changing textures and sounds
Edit `tools/make_assets.py` and `tools/sprites.py`, then run `python tools/make_assets.py`. Or just replace the PNG files in `src/main/resources/assets/deadseasons/textures`.
