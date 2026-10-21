package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.processing.MachineRecipeKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe types and serializers: one own RecipeType instance and one serializer per machine recipe kind (09 R4). */
public final class AutarkiaRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Autarkia.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Autarkia.MOD_ID);
    private static final Map<MachineRecipeKind, DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>>> TYPE_MAP = new EnumMap<>(MachineRecipeKind.class);
    private static final Map<MachineRecipeKind, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>>> SER_MAP = new EnumMap<>(MachineRecipeKind.class);

    static {
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            String full = Autarkia.MOD_ID + ":" + kind.id;
            TYPE_MAP.put(kind, TYPES.register(kind.id, () -> new RecipeType<MachineRecipe>() {
                @Override
                public String toString() {
                    return full;
                }
            }));
            SER_MAP.put(kind, SERIALIZERS.register(kind.id, () -> new RecipeSerializer<>(MachineRecipe.codec(kind), MachineRecipe.streamCodec(kind))));
        }
    }

    private AutarkiaRecipes() {
    }

    /** Register both deferred registers and the recipe sync to clients (JEI reads the synced map). */
    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> e.sendRecipes(allTypes()));
    }

    /** Return the recipe type of a kind. */
    public static RecipeType<MachineRecipe> type(MachineRecipeKind kind) {
        return TYPE_MAP.get(kind).get();
    }

    /** Return the serializer of a kind. */
    public static RecipeSerializer<MachineRecipe> serializer(MachineRecipeKind kind) {
        return SER_MAP.get(kind).get();
    }

    /** Return all machine recipe types. */
    public static List<RecipeType<?>> allTypes() {
        List<RecipeType<?>> list = new ArrayList<>();
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            list.add(type(kind));
        }
        return list;
    }
}
