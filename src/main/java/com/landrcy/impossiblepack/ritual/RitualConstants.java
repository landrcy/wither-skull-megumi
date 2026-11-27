package com.landrcy.impossiblepack.ritual;

public final class RitualConstants {
    private RitualConstants() {}

    /** 5% of natural Wither Skeleton spawns wear Megumi's coat and trigger the summon ritual when attacked. */
    public static final float NATURAL_COATED_SPAWN_CHANCE = 0.05F;

    /** Ticks to snap into Megumi's Mahoraga summoning hand-sign pose. */
    public static final int POSE_SNAP_TICKS = 10;

    /** Tick when the world finishes fading into 100% pitch-black void around player & Megumi (0.8s). */
    public static final int BLACKOUT_FULL_TICK = 16;

    /** Tick when the 12 surrounding Mahoraga Wither Skulls begin manifesting (2.0s). */
    public static final int SKULLS_START_TICK = 40;

    /** Tick when all 12 surrounding Wither Skulls have manifested (6.0s). */
    public static final int SKULLS_FULL_TICK = 120;

    /** Tick when the Mahoraga-Wheeled Wither emerges behind Megumi and charges up with the summon audio (6.0s). */
    public static final int WITHER_APPEAR_TICK = 120;

    /**
     * Exact tick when the 16-second mahoraga_summon audio peaks/finishes (16.0s = 320 ticks):
     * the Wither behind Megumi EXPLODES, killing the Wither Skeleton and launching his body parts!
     */
    public static final int WITHER_EXPLODE_TICK = 320;

    /**
     * Tick when the player snaps out of the illusion (16.9s = 338 ticks) facing the real Mahoraga Wither,
     * which immediately turns its Divine Wheel and plays divine_wheel_adapting!
     */
    public static final int ILLUSION_END_TICK = 338;

    /** Number of Wither Skulls surrounding player & Megumi in the black void. */
    public static final int PERIMETER_SKULL_COUNT = 12;
}
