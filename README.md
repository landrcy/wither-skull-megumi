# The Impossible Pack (Minecraft 26.3 — NeoForge)

**"With this treasure, I summon..."**

### 📥 1-Click Downloads (v2.2.0 — Minecraft 26.3 / NeoForge)
- 👉 **[Standalone Mod JAR (`the_impossible_pack-26.3-2.2.0.jar`)](https://github.com/landrcy/wither-skull-megumi/raw/main/releases/the_impossible_pack-26.3-2.2.0.jar)** *(Drop directly into `.minecraft/mods/`)*
- 👉 **[Modrinth / Prism Modpack (`The-Impossible-Pack-26.3.mrpack`)](https://github.com/landrcy/wither-skull-megumi/raw/main/releases/The-Impossible-Pack-26.3.mrpack)** *(Import directly as "The Impossible Pack")*
- 👉 **[CurseForge Modpack (`The-Impossible-Pack-26.3-Modpack.zip`)](https://github.com/landrcy/wither-skull-megumi/raw/main/releases/The-Impossible-Pack-26.3-Modpack.zip)**

---

## Features

### 1. Coated Wither Skeletons & Mahoraga Wither Ritual
- **5% (`1 in 20`)** of natural Wither Skeleton spawns wear **Megumi Fushiguro's tattered Jujutsu High coat** (`the_impossible_pack:megumi_wither_skeleton`) and trigger the 16-second Mahoraga Wither summon when attacked (the remaining 95% remain normal uncoated Wither Skeletons).
- **Custom Audio**: Plays `mahoraga_summon.ogg` (`16.17s`) as the world cuts to **100% pitch black** leaving only you and him surrounded by **12 Wither Skulls**, while a towering Wither crowned with the **3D Golden Eight-Handled Mahoraga Divine Wheel** charges up behind him.
- **Explosion & Adaptation**: At `16.0s`, the Wither explodes behind him—killing him and launching all 6 of his skeletal parts past you—then snaps you out of the illusion facing the real **Eight-Handled Mahoraga Wither** (`the_impossible_pack:mahoraga_wither`), which **exclusively targets only you**, turns its 3D Divine Wheel `45°`, and plays `divine_wheel_adapting.ogg` on summon and every time it adapts to your attacks!
- Commands: `/summon the_impossible_pack:megumi_wither_skeleton` and `/summon the_impossible_pack:mahoraga_wither` (plus Creative Spawn Eggs).

### 2. 0.5-Second Lightning-Charged Creepers
- The moment any Creeper begins its fuse, **0.5 seconds (`10` ticks) later** a lightning bolt strikes it and turns it into a **Charged Creeper** before it detonates (shielded from dying to its own lightning bolt!).

### 3. Homing / Curving Skeleton Arrows
- Arrows fired by Skeletons (including Strays, Bogged, and Wither Skeletons) smoothly **curve mid-flight toward the player**, compensating for strafing and gravity so they almost always hit unless blocked or covered.

### 4. Guaranteed + Tiered Armor on Zombies & Skeletons (No Equipment Drops)
- Every Zombie and Skeleton spawns with **at least 1 piece of armor**, weighted heavily toward fuller sets (and **never drops** its equipped armor or weapons):
  - **1 piece**: `12%` | **2 pieces**: `23%` | **3 pieces**: `30%` | **4 pieces (Full Set)**: `35%`
- Higher armor tiers are progressively rarer (including Minecraft 26.3 **Copper Armor**):
  - **Leather** (`34%`) → **Copper** (`22%`) → **Gold** (`16%`) → **Chainmail** (`12%`) → **Iron** (`10%`) → **Diamond** (`4.5%`) → **Netherite** (`1.5%`)

### 5. Enderman — Boogie Woogie Position Swap (Custom VFX & Particles)
- Aggroed Endermen clap with custom **Boogie Woogie** cursed-energy VFX & layered clap audio and **instantly swap positions with you**—even faster if you try hiding in water or towering up!
- **Custom Particles**: Concentric expanding cyan-to-magenta cursed-energy shockwave rings (`boogie_woogie_ring`) at both swap points plus high-speed helical cursed-energy sparks (`boogie_woogie_spark`) streaming directly between Point A and Point B.

### 6. Spider & Cave Spider — Rear-Spring Silky Web Shot (Custom Model Animation & Silk Physics)
- Aggroed Spiders and Cave Spiders crouch and **physically spring their abdomen/butt upward** (`SpringSpiderModel`) like a real spider firing silk, launching a trailing web shot that applies brief Slowness and traps your feet in a temporary 5-second cobweb.
- **Custom Particles**: 4 custom gossamer silk strand sprites (`silky_web_strand`) with **real-world spider-silk physics**—strong air drag, near-weightless float, gentle sinusoidal wind billowing, and sticking on contact with surfaces.

### 7. Zombie — Look-Down Bridging, Block Breaking & Baby-Zombie Fastball (Custom Debris Particles)
- **Look Down & Bridge UP**: When you tower up, Zombies equip cobblestone in their offhand (0% drop), **physically look straight down**, jump, and pillar up toward you—even breaking ceilings above their head!
- **Look Down & Bridge OVER**: Across ravines, moats, or pillars, Zombies **look down at the gap edge** and bridge straight across to you.
- **Block Breaking**: Zombies visibly crack and shatter walls/doors blocking their path (never breaking bedrock, obsidian, or containers/block-entities).
- **Baby-Zombie Fastball**: Adult Zombies can hurl nearby Baby Zombies in a high arc straight onto your ledge!
- **Custom Particles**: Directional stone/earth shatter chips (`siege_debris`) spraying outward from broken and placed blocks with real gravity, tumble spin, and floor bounce.

## Building from Source (Java 25 + NeoForge 26.3)

```bash
./gradlew build
```
