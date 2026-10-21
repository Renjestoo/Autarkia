package dev.autarkia.equipment;

import dev.autarkia.Autarkia;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/** Tool and armor materials of 07 section 1.1. */
public final class AutarkiaMaterials {
    public static final TagKey<Item> BRONZE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Autarkia.id("bronze_tool_materials"));
    public static final TagKey<Item> REPAIRS_BRONZE_ARMOR = TagKey.create(Registries.ITEM, Autarkia.id("repairs_bronze_armor"));
    public static final ToolMaterial BRONZE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 300, 6.5F, 2.0F, 16, BRONZE_TOOL_MATERIALS);
    public static final ResourceKey<EquipmentAsset> BRONZE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Autarkia.id("bronze"));
    public static final ArmorMaterial BRONZE_ARMOR = new ArmorMaterial(14, defense(2, 5, 4, 2, 5), 12, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
        REPAIRS_BRONZE_ARMOR, BRONZE_ASSET);

    public static final TagKey<Item> STEEL_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Autarkia.id("steel_tool_materials"));
    public static final TagKey<Item> REPAIRS_STEEL_ARMOR = TagKey.create(Registries.ITEM, Autarkia.id("repairs_steel_armor"));
    public static final ToolMaterial STEEL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1200, 7.5F, 2.5F, 12, STEEL_TOOL_MATERIALS);
    public static final ResourceKey<EquipmentAsset> STEEL_RIG_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Autarkia.id("steel_rig"));
    public static final ArmorMaterial STEEL_RIG = new ArmorMaterial(25, defense(3, 7, 5, 2, 7), 10, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F,
        REPAIRS_STEEL_ARMOR, STEEL_RIG_ASSET);

    private AutarkiaMaterials() {
    }

    /**
     * Return the steel rig attribute modifiers of a piece: the material defense and toughness plus +4% block break
     * speed (07 section 1.3), id {@code autarkia:steel_rig_break_speed_<type>}.
     */
    public static net.minecraft.world.item.component.ItemAttributeModifiers steelRig(ArmorType type) {
        return STEEL_RIG.createAttributes(type).withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED,
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(Autarkia.id("steel_rig_break_speed_" + type.getSerializedName()), 0.04,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            net.minecraft.world.entity.EquipmentSlotGroup.bySlot(type.getSlot()));
    }

    /** Return the defense map from helmet, chest, legs, boots and body values. */
    static Map<ArmorType, Integer> defense(int helmet, int chest, int legs, int boots, int body) {
        Map<ArmorType, Integer> m = new EnumMap<>(ArmorType.class);
        m.put(ArmorType.HELMET, helmet);
        m.put(ArmorType.CHESTPLATE, chest);
        m.put(ArmorType.LEGGINGS, legs);
        m.put(ArmorType.BOOTS, boots);
        m.put(ArmorType.BODY, body);
        return m;
    }
}
