# The Impossible Pack (Minecraft 26.3 — NeoForge)

**"With this treasure, I summon..."**

### 📥 1-Click Downloads (Minecraft 26.3 / NeoForge)
- 👉 **[Standalone Mod JAR (`the_impossible_pack-26.3-2.0.0.jar`)](https://github.com/landrcy/wither-skull-megumi/raw/main/releases/the_impossible_pack-26.3-2.0.0.jar)** *(Drop directly into `.minecraft/mods/`)*
- 👉 **[Modrinth / Prism Modpack (`The-Impossible-Pack-26.3.mrpack`)](https://github.com/landrcy/wither-skull-megumi/raw/main/releases/The-Impossible-Pack-26.3.mrpack)** *(Import directly as "The Impossible Pack")*
- 👉 **[CurseForge Modpack (`The-Impossible-Pack-26.3-Modpack.zip`)](https://github.com/landrcy/wither-skull-megumi/raw/main/releases/The-Impossible-Pack-26.3-Modpack.zip)**

---

## Features & Ritual Choreography

1. **5% Coated Wither Skeletons**:
   - **5% (`1 in 20`)** of natural Wither Skeleton spawns wear **Megumi Fushiguro's tattered Jujutsu High coat** (`the_impossible_pack:megumi_wither_skeleton`) and trigger the Mahoraga Wither summon when attacked. The remaining **95%** stay normal Wither Skeletons without the coat.
   - Commands:
     ```mcfunction
     /summon the_impossible_pack:megumi_wither_skeleton
     /summon the_impossible_pack:mahoraga_wither
     ```
   - Creative Spawn Eggs are also included in the **Spawn Eggs** tab.

2. **Custom Mahoraga Summon Audio (`0:00 – 0:16`) & Pose**:
   - Attacking a Coated Wither Skeleton immediately starts the **16-second Mahoraga Summon audio** (`the_impossible_pack:mahoraga_summon`) and snaps him into Megumi's hand-sign pose.
   - Everything in the world cuts to **100% pitch black** leaving **only you and him** visible, surrounded by **12 Wither Skulls**.

3. **Wither Charges Up Behind Him & Explodes (`0:16`)**:
   - Across the summon build-up, a towering Wither crowned with the **3D Golden Eight-Handled Mahoraga Divine Wheel** rises and powers up directly behind him.
   - Right as the summon completes (`16.0s`), **the Wither explodes, killing the Wither Skeleton and launching his skull, coated torso, arms, and legs** spinning past you through the darkness!

4. **Divine Wheel Adapting Sound & Boss Mechanic**:
   - You snap out of the illusion facing the real **Eight-Handled Mahoraga Wither** (`the_impossible_pack:mahoraga_wither`), which **exclusively targets only you** and wears the **3D Golden Mahoraga Divine Wheel** floating & tilted above its center head.
   - Immediately upon completing the summon—and every time it adapts to your attacks in combat—its **Divine Wheel clicks `45°`**, plays the **Divine Wheel Adapting sound effect** (`the_impossible_pack:divine_wheel_adapting`), heals slightly, and builds damage resistance against that attack type!

## Building from Source (Java 25 + NeoForge 26.3)

```bash
./gradlew build
```
