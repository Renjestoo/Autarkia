package dev.autarkia.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.registry.AutarkiaRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

/**
 * One recipe class for all machine recipe types (03 section 5.2). Inputs match shapeless, one input per slot; chance
 * results are rolled by the machine. The kind decides type and serializer and is not part of the JSON.
 */
public record MachineRecipe(MachineRecipeKind kind, List<String> stations, List<SizedIngredient> inputs, List<Result> results,
                            Optional<Integer> time, Optional<Integer> energy, int water, long matter) implements Recipe<MachineInput> {

    /** One result with an optional chance (0..1). */
    public record Result(ItemStackTemplate item, float chance) {
        public static final Codec<Result> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStackTemplate.MAP_CODEC.forGetter(Result::item),
            Codec.floatRange(0, 1).optionalFieldOf("chance", 1f).forGetter(Result::chance)).apply(i, Result::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, Result::item, ByteBufCodecs.FLOAT, Result::chance, Result::new);

        /** Return true when the result always comes out. */
        public boolean certain() {
            return chance >= 1f;
        }
    }

    /** Return the codec for one kind; the kind is fixed by the serializer, not read from JSON. */
    public static MapCodec<MachineRecipe> codec(MachineRecipeKind kind) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("stations", List.of()).forGetter(MachineRecipe::stations),
            SizedIngredient.NESTED_CODEC.listOf(0, 9).optionalFieldOf("inputs", List.of()).forGetter(MachineRecipe::inputs),
            Result.CODEC.listOf(1, 4).fieldOf("results").forGetter(MachineRecipe::results),
            Codec.INT.optionalFieldOf("time").forGetter(MachineRecipe::time),
            Codec.INT.optionalFieldOf("energy").forGetter(MachineRecipe::energy),
            Codec.INT.optionalFieldOf("water", 0).forGetter(MachineRecipe::water),
            Codec.LONG.optionalFieldOf("matter", 0L).forGetter(MachineRecipe::matter)
        ).apply(i, (st, in, res, t, e, w, m) -> new MachineRecipe(kind, st, in, res, t, e, w, m)));
    }

    /** Return the network codec for one kind. */
    public static StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec(MachineRecipeKind kind) {
        return StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), MachineRecipe::stations,
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::inputs,
            Result.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::results,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), MachineRecipe::time,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), MachineRecipe::energy,
            ByteBufCodecs.VAR_INT, MachineRecipe::water,
            ByteBufCodecs.VAR_LONG, MachineRecipe::matter,
            (st, in, res, t, e, w, m) -> new MachineRecipe(kind, st, in, res, t, e, w, m));
    }

    /** Return true when the station (block id path) may run this recipe; an empty station list allows all. */
    public boolean allows(String station) {
        return stations.isEmpty() || stations.contains(station);
    }

    /** Return for each input the slot index of the input it uses, or null when the items do not match. */
    public int @Nullable [] slotMatch(MachineInput input) {
        int[] used = new int[inputs.size()];
        boolean[] taken = new boolean[input.size()];
        for (int k = 0; k < inputs.size(); k++) {
            SizedIngredient ing = inputs.get(k);
            int found = -1;
            for (int s = 0; s < input.size(); s++) {
                ItemStack st = input.getItem(s);
                if (!taken[s] && !st.isEmpty() && ing.ingredient().test(st) && st.getCount() >= ing.count()) {
                    found = s;
                    break;
                }
            }
            if (found < 0) {
                return null;
            }
            taken[found] = true;
            used[k] = found;
        }
        for (int s = 0; s < input.size(); s++) {
            if (!taken[s] && !input.getItem(s).isEmpty()) {
                return null;
            }
        }
        return used;
    }

    /** Return the stacks this run produces, rolling chance results with the given random. */
    public List<ItemStack> roll(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        for (Result r : results) {
            if (r.certain() || random.nextFloat() < r.chance()) {
                out.add(r.item().create());
            }
        }
        return out;
    }

    /** Return every possible result stack (for output room checks). */
    public List<ItemStack> allResults() {
        return results.stream().map(r -> r.item().create()).toList();
    }

    /** Return the operation time in ticks at speed 1, falling back to the machine default. */
    public int timeOr(int fallback) {
        return time.orElse(fallback);
    }

    /** Return the FE/t, falling back to the machine default. */
    public int energyOr(int fallback) {
        return energy.orElse(fallback);
    }

    @Override
    public boolean matches(MachineInput input, Level level) {
        return slotMatch(input) != null;
    }

    @Override
    public ItemStack assemble(MachineInput input) {
        return results.getFirst().item().create();
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
    public RecipeSerializer<MachineRecipe> getSerializer() {
        return AutarkiaRecipes.serializer(kind);
    }

    @Override
    public RecipeType<MachineRecipe> getType() {
        return AutarkiaRecipes.type(kind);
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
