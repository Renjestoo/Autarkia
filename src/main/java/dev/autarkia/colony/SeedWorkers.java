package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.storage.DepotBlockEntity;
import dev.autarkia.worker.WorkerAssemblyRecipe;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * The workers of an Autarky Seed (05 section 8.4, 06 sections 5.1 and 6.1, M9 decisions G9 and G18): the default crew
 * (2 Porter, 2 Builder Drone) and their bills from the {@code worker_assembly} recipes of the {@code worker_assembler};
 * the bill items are taken from colony storage when the matrix packs; at the unpack finish each worker unfolds at the
 * seed outpost with a full battery and its home there, but only while the domain cap and its sub-cap have room, else its
 * bill goes into the outpost depots (the rest drops at the core).
 */
public final class SeedWorkers {
    public static final List<Identifier> DEFAULT = List.of(Autarkia.id("porter"), Autarkia.id("porter"), Autarkia.id("builder_drone"),
        Autarkia.id("builder_drone"));

    private SeedWorkers() {
    }

    /** Return the frame and parts of a worker type's assembler recipe; throws when the recipe is not loaded. */
    public static List<SizedIngredient> bill(ServerLevel level, Identifier worker) {
        for (var h : level.recipeAccess().recipeMap().byType(AutarkiaRecipes.WORKER_ASSEMBLY.get())) {
            WorkerAssemblyRecipe r = h.value();
            if (r.worker().equals(worker) && r.forMachine(WorkerAssemblyRecipe.ASSEMBLER)) {
                return r.bill();
            }
        }
        throw new IllegalStateException("No worker_assembly recipe at the worker_assembler builds " + worker + "; the seed crew needs it");
    }

    /**
     * Take every ingredient (with its count) from the colony storage in one transaction, all or nothing. The taken stacks
     * are added to {@code taken}. Returns the first ingredient that storage lacks, or null when all were taken.
     */
    public static @Nullable SizedIngredient take(ServerLevel level, Colony c, List<SizedIngredient> needs, List<ItemStack> taken) {
        List<ResourceHandler<ItemResource>> handlers = new ArrayList<>();
        for (BlockPos p : List.copyOf(ColonyRuntime.of(c).storages)) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof ColonyStorage s) {
                handlers.add(s.colonyItems());
            }
        }
        List<ItemStack> got = new ArrayList<>();
        try (Transaction tx = Transaction.openRoot()) {
            for (SizedIngredient need : needs) {
                int left = need.count();
                for (ResourceHandler<ItemResource> h : handlers) {
                    for (int i = 0; i < h.size() && left > 0; i++) {
                        ItemResource r = h.getResource(i);
                        if (h.getAmountAsInt(i) > 0 && need.ingredient().test(r.toStack(1))) {
                            int n = h.extract(r, left, tx);
                            if (n > 0) {
                                left -= n;
                                got.add(r.toStack(n));
                            }
                        }
                    }
                }
                if (left > 0) {
                    return need;
                }
            }
            tx.commit();
        }
        taken.addAll(got);
        return null;
    }

    /** Return the stacks a bill stands for (the first item of each ingredient, its count). */
    public static List<ItemStack> refund(List<SizedIngredient> bill) {
        List<ItemStack> out = new ArrayList<>();
        for (SizedIngredient si : bill) {
            si.ingredient().items().findFirst().map(Holder::value).ifPresent(item -> out.add(new ItemStack(item, si.count())));
        }
        return out;
    }

    /** Unfold the crew at the seed outpost; returns the number of workers that unfolded. */
    public static int unfold(ServerLevel level, Colony c, Colony.Outpost o, List<Identifier> crew) {
        int n = 0;
        for (int i = 0; i < crew.size(); i++) {
            Identifier id = crew.get(i);
            EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            WorkerType type = WorkerType.of(et);
            boolean room = c.workerCost() + type.capCost() <= ColonyTiers.workerCap(c.tier) && c.workerCount(type.id()) < type.subCap(c.tier);
            if (!room || !(et.create(level, EntitySpawnReason.MOB_SUMMONED) instanceof WorkerEntity w)) {
                store(level, o, refund(bill(level, id)));
                ColonyRuntime.of(c).log("Seed worker " + id.getPath() + " has no cap room; its parts went to the outpost depot");
                continue;
            }
            BlockPos at = o.corePos.offset(i - 1, 0, 3);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
            w.snapTo(at.getX() + 0.5, y, at.getZ() + 0.5, 0, 0);
            w.setEnergy(type.capacity());
            level.addFreshEntity(w);
            w.bind(c);
            w.recall(o.corePos);
            w.trigger("unfold");
            w.sound("entity.worker.servo", 0.6f, 1.2f);
            n++;
        }
        return n;
    }

    private static void store(ServerLevel level, Colony.Outpost o, List<ItemStack> items) {
        List<DepotBlockEntity> depots = Outposts.depots(level, o);
        for (ItemStack s : items) {
            int left = s.getCount();
            for (DepotBlockEntity d : depots) {
                if (left <= 0) {
                    break;
                }
                try (Transaction tx = Transaction.openRoot()) {
                    left -= d.items().insert(ItemResource.of(s), left, tx);
                    tx.commit();
                }
            }
            if (left > 0) {
                Containers.dropItemStack(level, o.corePos.getX() + 0.5, o.corePos.getY() + 1, o.corePos.getZ() + 0.5, s.copyWithCount(left));
            }
        }
    }
}
