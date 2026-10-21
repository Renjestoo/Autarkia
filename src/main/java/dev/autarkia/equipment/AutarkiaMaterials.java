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

    public static final TagKey<Item> ELECTRIC_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Autarkia.id("electric_tool_materials"));
    public static final ToolMaterial ELECTRIC = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1, 12.0F, 3.0F, 14, ELECTRIC_TOOL_MATERIALS);
    public static final TagKey<Item> REPAIRS_EXO_RIG = TagKey.create(Registries.ITEM, Autarkia.id("repairs_exo_rig"));
    public static final ResourceKey<EquipmentAsset> EXO_RIG_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Autarkia.id("exo_rig"));
    public static final ArmorMaterial EXO_RIG = new ArmorMaterial(33, defense(3, 7, 6, 3, 7), 12, SoundEvents.ARMOR_EQUIP_IRON, 2.0F, 0.0F,
        REPAIRS_EXO_RIG, EXO_RIG_ASSET);

    public static final TagKey<Item> VANTITE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Autarkia.id("vantite_tool_materials"));
    public static final ToolMaterial VANTITE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 16.0F, 4.5F, 15, VANTITE_TOOL_MATERIALS);
    public static final TagKey<Item> REPAIRS_FRONTIER_SUIT = TagKey.create(Registries.ITEM, Autarkia.id("repairs_frontier_suit"));
    public static final ResourceKey<EquipmentAsset> FRONTIER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Autarkia.id("frontier_suit"));
    public static final ArmorMaterial FRONTIER = new ArmorMaterial(40, defense(3, 8, 6, 3, 8), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.05F,
        REPAIRS_FRONTIER_SUIT, FRONTIER_ASSET);

    public static final TagKey<Item> LATTICE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Autarkia.id("lattice_tool_materials"));
    public static final ToolMaterial LATTICE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1, 24.0F, 6.0F, 18, LATTICE_TOOL_MATERIALS);
    public static final TagKey<Item> REPAIRS_LATTICE = TagKey.create(Registries.ITEM, Autarkia.id("repairs_lattice"));
    public static final ResourceKey<EquipmentAsset> LATTICE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Autarkia.id("lattice_carapace"));
    public static final ArmorMaterial LATTICE_CARAPACE = new ArmorMaterial(50, defense(4, 9, 7, 4, 9), 18, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.15F,
        REPAIRS_LATTICE, LATTICE_ASSET);

    public static final TagKey<Item> AEGIS_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Autarkia.id("aegis_tool_materials"));
    public static final ToolMaterial AEGIS = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1, 2000.0F, 10.0F, 22, AEGIS_TOOL_MATERIALS);
    public static final TagKey<Item> REPAIRS_AEGIS = TagKey.create(Registries.ITEM, Autarkia.id("repairs_aegis"));
    public static final ResourceKey<EquipmentAsset> AEGIS_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Autarkia.id("aegis"));
    public static final ArmorMaterial AEGIS_ARMOR = new ArmorMaterial(1, defense(5, 10, 8, 5, 10), 22, SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 0.25F,
        REPAIRS_AEGIS, AEGIS_ASSET);

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
