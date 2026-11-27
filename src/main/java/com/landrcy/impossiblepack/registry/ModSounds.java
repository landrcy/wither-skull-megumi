package com.landrcy.impossiblepack.registry;

import com.landrcy.impossiblepack.TheImpossiblePackMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, TheImpossiblePackMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MAHORAGA_SUMMON =
            SOUND_EVENTS.register("mahoraga_summon", () ->
                    SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(TheImpossiblePackMod.MODID, "mahoraga_summon")));

    public static final DeferredHolder<SoundEvent, SoundEvent> DIVINE_WHEEL_ADAPTING =
            SOUND_EVENTS.register("divine_wheel_adapting", () ->
                    SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(TheImpossiblePackMod.MODID, "divine_wheel_adapting")));
}
