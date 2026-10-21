package dev.autarkia.worker;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import dev.autarkia.processing.MachineInput;
import dev.autarkia.registry.AutarkiaRecipes;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

/**
 * Recipe type {@code autarkia:worker_assembly} (06 section 5.1): the worker entity, its frame, the parts with counts,
 * the time, the FE per tick and the minimum Core tier. Slot 0 of the input is the frame slot, slots 1 to 6 the parts.
 */
public record WorkerAssemblyRecipe(Identifier worker, SizedIngredient frame, List<SizedIngredient> parts, int time, int fePerTick, int minTier)
    implements Recipe<MachineInput> {
    public static final MapCodec<WorkerAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Identifier.CODEC.fieldOf("worker").forGetter(WorkerAssemblyRecipe::worker),
        SizedIngredient.NESTED_CODEC.fieldOf("frame").forGetter(WorkerAssemblyRecipe::frame),
        SizedIngredient.NESTED_CODEC.listOf(1, 6).fieldOf("parts").forGetter(WorkerAssemblyRecipe::parts),
        Codec.INT.fieldOf("time").forGetter(WorkerAssemblyRecipe::time),
        Codec.INT.fieldOf("fe_per_tick").forGetter(WorkerAssemblyRecipe::fePerTick),
        Codec.INT.optionalFieldOf("min_tier", 1).forGetter(WorkerAssemblyRecipe::minTier)
    ).apply(i, WorkerAssemblyRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkerAssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, WorkerAssemblyRecipe::worker,
        SizedIngredient.STREAM_CODEC, WorkerAssemblyRecipe::frame,
        SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), WorkerAssemblyRecipe::parts,
        ByteBufCodecs.VAR_INT, WorkerAssemblyRecipe::time,
        ByteBufCodecs.VAR_INT, WorkerAssemblyRecipe::fePerTick,
        ByteBufCodecs.VAR_INT, WorkerAssemblyRecipe::minTier,
        WorkerAssemblyRecipe::new);

    /** Return the entity type the recipe builds. */
    public EntityType<?> entityType() {
        return BuiltInRegistries.ENTITY_TYPE.getValue(worker);
    }

    /** Return whether the frame slot and the part slots hold every ingredient in its count (part order is free). */
    @Override
    public boolean matches(MachineInput input, Level level) {
        ItemStack f = input.getItem(0);
        if (f.isEmpty() || !frame.ingredient().test(f) || f.getCount() < frame.count()) {
            return false;
        }
        boolean[] taken = new boolean[input.size()];
        for (SizedIngredient part : parts) {
            boolean found = false;
            for (int s = 1; s < input.size() && !found; s++) {
                ItemStack st = input.getItem(s);
                if (!taken[s] && !st.isEmpty() && part.ingredient().test(st) && st.getCount() >= part.count()) {
                    taken[s] = true;
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(MachineInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<WorkerAssemblyRecipe> getSerializer() {
        return AutarkiaRecipes.WORKER_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<WorkerAssemblyRecipe> getType() {
        return AutarkiaRecipes.WORKER_ASSEMBLY.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @Nullable RecipeBookCategory recipeBookCategory() {
        return null;
    }
}
