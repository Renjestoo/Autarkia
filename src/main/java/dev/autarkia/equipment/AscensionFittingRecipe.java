package dev.autarkia.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.processing.MachineInput;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaRecipes;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
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
 * Recipe type {@code autarkia:ascension_fitting} (07 section 5 rows 46 to 49, 08b section 4, decision G15): the armor
 * {@code piece} slot ({@code feet}, {@code legs}, {@code chest} or {@code head}), the item the player must wear there,
 * the inputs the Forge of Ascension takes from its 27 slots at the equip tick, the FE taken evenly over the step, the
 * fitting step (2 boots, 3 legs, 4 chest, 5 helmet) and the result put on. Inputs are matched by total count over all
 * slots (09 R4: special, no recipe book).
 */
public record AscensionFittingRecipe(EquipmentSlot piece, Item worn, List<SizedIngredient> inputs, long energy, int step, Item result)
    implements Recipe<MachineInput> {
    public static final MapCodec<AscensionFittingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        EquipmentSlot.CODEC.fieldOf("piece").forGetter(AscensionFittingRecipe::piece),
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("worn").forGetter(AscensionFittingRecipe::worn),
        SizedIngredient.NESTED_CODEC.listOf(1, 27).fieldOf("inputs").forGetter(AscensionFittingRecipe::inputs),
        Codec.LONG.fieldOf("energy").forGetter(AscensionFittingRecipe::energy),
        Codec.intRange(2, 5).fieldOf("step").forGetter(AscensionFittingRecipe::step),
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(AscensionFittingRecipe::result)
    ).apply(i, AscensionFittingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AscensionFittingRecipe> STREAM_CODEC = StreamCodec.composite(
        EquipmentSlot.STREAM_CODEC, AscensionFittingRecipe::piece,
        ByteBufCodecs.registry(Registries.ITEM), AscensionFittingRecipe::worn,
        SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AscensionFittingRecipe::inputs,
        ByteBufCodecs.VAR_LONG, AscensionFittingRecipe::energy,
        ByteBufCodecs.VAR_INT, AscensionFittingRecipe::step,
        ByteBufCodecs.registry(Registries.ITEM), AscensionFittingRecipe::result,
        AscensionFittingRecipe::new);

    /** Return true when the inventory holds every input in its count (summed over all slots). */
    public boolean available(MachineInventory inv) {
        return holds(inv.size(), inv::get);
    }

    private boolean holds(int size, java.util.function.IntFunction<ItemStack> slot) {
        for (SizedIngredient s : inputs) {
            int have = 0;
            for (int i = 0; i < size; i++) {
                ItemStack st = slot.apply(i);
                if (!st.isEmpty() && s.ingredient().test(st)) {
                    have += st.getCount();
                }
            }
            if (have < s.count()) {
                return false;
            }
        }
        return true;
    }

    /** Take every input in its count from the inventory; return false and take nothing when one is missing. */
    public boolean consume(MachineInventory inv) {
        if (!available(inv)) {
            return false;
        }
        for (SizedIngredient s : inputs) {
            int left = s.count();
            for (int i = 0; i < inv.size() && left > 0; i++) {
                ItemStack st = inv.get(i);
                if (!st.isEmpty() && s.ingredient().test(st)) {
                    int take = Math.min(left, st.getCount());
                    inv.shrink(i, take);
                    left -= take;
                }
            }
        }
        return true;
    }

    @Override
    public boolean matches(MachineInput input, Level level) {
        return holds(input.size(), input::getItem);
    }

    @Override
    public ItemStack assemble(MachineInput input) {
        return new ItemStack(result);
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
    public RecipeSerializer<AscensionFittingRecipe> getSerializer() {
        return AutarkiaRecipes.ASCENSION_FITTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<AscensionFittingRecipe> getType() {
        return AutarkiaRecipes.ASCENSION_FITTING.get();
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
