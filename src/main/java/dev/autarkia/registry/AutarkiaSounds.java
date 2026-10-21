package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All 107 Autarkia sound events (names generated from tools/sounds_spec.py). */
public final class AutarkiaSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Autarkia.MOD_ID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_NAME = new LinkedHashMap<>();

    static {
        for (String name : SoundIds.NAMES) {
            BY_NAME.put(name, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Autarkia.id(name))));
        }
    }

    /**
     * Return the sound event registered under the given name.
     *
     * @throws IllegalArgumentException when the name is not one of the generated sound ids
     */
    public static Holder<SoundEvent> get(String name) {
        DeferredHolder<SoundEvent, SoundEvent> h = BY_NAME.get(name);
        if (h == null) {
            throw new IllegalArgumentException("Unknown Autarkia sound event: " + name);
        }
        return h;
    }

    /** Return the sound event value registered under the given name. */
    public static SoundEvent of(String name) {
        return get(name).value();
    }

    private AutarkiaSounds() {
    }
}
