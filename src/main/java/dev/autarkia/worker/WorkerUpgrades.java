package dev.autarkia.worker;

import dev.autarkia.Autarkia;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The two upgrade slots of a worker (06 section 5.4). Speed: +20% movement or flying speed and +10% move drain. Cargo:
 * +1 carry slot (Loader: +9 pallet slots) and +5% move drain. Armor: +6 armor and +25% max health. Range: +2 reach.
 * The same type may fill both slots. Modifier ids {@code autarkia:upgrade_<type>_<slot>}.
 */
public final class WorkerUpgrades {
    /** Upgrade kinds in item order. */
    public enum Kind { SPEED, CARGO, ARMOR, RANGE }

    public static final int SLOTS = 2;
    private final WorkerEntity worker;
    private final List<Kind> installed = new ArrayList<>();

    WorkerUpgrades(WorkerEntity worker) {
        this.worker = worker;
    }

    /** Return the installed upgrades in slot order. */
    public List<Kind> installed() {
        return List.copyOf(installed);
    }

    /** Return how many upgrades of the kind are installed. */
    public int count(Kind k) {
        return (int) installed.stream().filter(i -> i == k).count();
    }

    /** Return the bit mask of installed kinds for the renderer (bit = kind ordinal). */
    public int mask() {
        int m = 0;
        for (Kind k : installed) {
            m |= 1 << k.ordinal();
        }
        return m;
    }

    /** Return the extra cargo slots the cargo upgrades give. */
    public int extraCargo() {
        return count(Kind.CARGO) * (worker.workerType == WorkerType.LOADER ? 9 : 1);
    }

    /** Return the move drain factor (1 + 10% per speed and 5% per cargo upgrade). */
    public double moveDrainFactor() {
        return 1 + 0.10 * count(Kind.SPEED) + 0.05 * count(Kind.CARGO);
    }

    /** Return the extra reach in blocks. */
    public double extraReach() {
        return 2.0 * count(Kind.RANGE);
    }

    /** Install one upgrade; return false when both slots are full. */
    public boolean install(Kind k) {
        if (installed.size() >= SLOTS) {
            return false;
        }
        installed.add(k);
        apply();
        return true;
    }

    /** Remove all upgrades and return them as items. */
    public List<ItemStack> removeAll() {
        List<ItemStack> out = new ArrayList<>();
        for (Kind k : installed) {
            out.add(new ItemStack(WorkerUpgradeItem.item(k)));
        }
        installed.clear();
        apply();
        return out;
    }

    /** Set the attribute modifiers, cargo slots and synced mask from the installed upgrades. */
    void apply() {
        Holder<Attribute> speed = worker.workerType.flying() ? Attributes.FLYING_SPEED : Attributes.MOVEMENT_SPEED;
        for (int slot = 0; slot < SLOTS; slot++) {
            Kind k = slot < installed.size() ? installed.get(slot) : null;
            for (Kind each : Kind.values()) {
                String id = "upgrade_" + each.name().toLowerCase(java.util.Locale.ROOT) + "_" + slot;
                boolean on = each == k;
                switch (each) {
                    case SPEED -> modifier(speed, id, on ? 0.2 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                    case ARMOR -> {
                        modifier(Attributes.ARMOR, id, on ? 6 : 0, AttributeModifier.Operation.ADD_VALUE);
                        modifier(Attributes.MAX_HEALTH, id + "_health", on ? 0.25 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                    }
                    default -> {
                    }
                }
            }
        }
        if (worker.getHealth() > worker.getMaxHealth()) {
            worker.setHealth(worker.getMaxHealth());
        }
        worker.resizeCargo(worker.workerType.cargoStacks() + extraCargo());
        worker.syncUpgrades(mask());
    }

    private void modifier(Holder<Attribute> attr, String id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = worker.getAttribute(attr);
        if (inst == null) {
            return;
        }
        if (amount == 0) {
            inst.removeModifier(Autarkia.id(id));
        } else {
            inst.addOrUpdateTransientModifier(new AttributeModifier(Autarkia.id(id), amount, op));
        }
    }

    /** Save the installed kinds. */
    void save(ValueOutput out) {
        out.putIntArray("upgrades", installed.stream().mapToInt(Kind::ordinal).toArray());
    }

    /** Load the installed kinds and apply them. */
    void load(ValueInput in) {
        installed.clear();
        for (int k : in.getIntArray("upgrades").orElse(new int[0])) {
            if (k >= 0 && k < Kind.values().length && installed.size() < SLOTS) {
                installed.add(Kind.values()[k]);
            }
        }
        apply();
    }
}
