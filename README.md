# Wither Scull Megumi (Minecraft 1.21.1 - NeoForge)

**"With this treasure, I summon..."**

A Jujutsu Kaisen–inspired **NeoForge 1.21.1** mod that introduces **Wither Skull Megumi** (`wither_scull_megumi:megumi_wither_skeleton`) — a cursed Wither Skeleton wearing a tattered Jujutsu High uniform.

## Features & Ritual Sequence

1. **Spawning**:
   - **0.1% Natural Replacement (`1 in 1,000`)**: Whenever a Wither Skeleton naturally spawns (e.g. in Nether Fortresses), there is a `0.1%` chance it spawns as **Wither Skull Megumi**.
   - **Command**:
     ```mcfunction
     /summon wither_scull_megumi:megumi_wither_skeleton
     ```
   - **Creative Spawn Egg**: Available in the **Spawn Eggs** Creative Tab (`Wither Skull Megumi Spawn Egg`).

2. **The Mahoraga Summoning Pose**:
   - The moment you attack **Wither Skull Megumi**, he stows his blade, locks eyes with you, and snaps into Megumi Fushiguro's Eight-Handled Sword Divergent Sila Divine General Mahoraga summoning hand-sign pose.

3. **Pitch-Black Domain Illusion**:
   - Everything in the world fades and cuts into **100% pitch-black emptiness** — sky, blocks, terrain, lava, and all other mobs vanish completely, leaving **only you and him** visible in stark contrast.

4. **Mahoraga Wither Skull Formation**:
   - **12 Wither Skulls** manifest around both of you in a ceremonial ring while an **8-Skull Dharmachakra Wheel** materializes behind/above his shoulders, clicking clockwise in discrete `45°` steps.

5. **The Wither Powers Up & Explodes**:
   - A massive **Wither** rises out of the darkness directly behind him, pulsing and charging with swirling invulnerable energy before **detonating** — violently launching his skull, ribcage, arms, and legs tumbling past you through the black void.

6. **Reality Returns — Single-Target Wither**:
   - You snap out of the illusion back into the real world, where the summoned **Eight-Handled Wither** (`wither_scull_megumi:megumi_wither`) stands fully charged and **exclusively targets ONLY you** (ignoring all other players, pets, villagers, golems, and mobs, and refusing to switch aggro).

## Installation

1. Install **Minecraft 1.21.1** with **NeoForge** (`21.1.x`).
2. Download `wither_scull_megumi-1.0.0.jar` from [Releases](https://github.com/landrcy/wither-skull-megumi/releases).
3. Drop the `.jar` into your `.minecraft/mods/` folder and launch!

## Building from Source

```bash
./gradlew build
```

The built mod jar is output to `build/libs/wither_scull_megumi-1.0.0.jar`.
