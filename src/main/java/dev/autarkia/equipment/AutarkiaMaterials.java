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

    private AutarkiaMaterials() {
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
