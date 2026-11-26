package com.landrcy.witherscullmegumi.ritual;

public final class RitualConstants {
    private RitualConstants() {}

    /** 0.1% chance (1 in 1000) for a natural Wither Skeleton spawn to become Wither Skull Megumi. */
    public static final float NATURAL_REPLACE_CHANCE = 0.001F;

    /** Ticks to snap from combat stance into Megumi's Mahoraga summoning hand-sign pose. */
    public static final int POSE_SNAP_TICKS = 8;

    /** Tick when the world finishes fading into 100% pitch-black void around player & Megumi. */
    public static final int BLACKOUT_FULL_TICK = 14;

    /** Tick when surrounding Mahoraga Wither Skulls & 8-skull wheel begin manifesting in the darkness. */
    public static final int SKULLS_START_TICK = 18;

    /** Tick when all surrounding Wither Skulls have manifested around the ritual arena. */
    public static final int SKULLS_FULL_TICK = 52;

    /** Tick when the Wither emerges directly behind Megumi Wither Skeleton and starts powering up. */
    public static final int WITHER_APPEAR_TICK = 56;

    /** Tick when the Wither behind him detonates and launches Megumi's skeletal body parts outward. */
    public static final int WITHER_EXPLODE_TICK = 106;

    /** Tick when the player snaps out of the illusion and faces the real single-target Wither. */
    public static final int ILLUSION_END_TICK = 128;

    /** Number of Wither Skulls in the outer ceremonial perimeter surrounding player & Megumi. */
    public static final int PERIMETER_SKULL_COUNT = 12;

    /** Number of Wither Skulls in the Mahoraga Eight-Handled Dharmachakra wheel behind/above Megumi. */
    public static final int WHEEL_SKULL_COUNT = 8;
}
