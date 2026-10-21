package dev.autarkia.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Tally board (04 section 2.2): counts the items of all storage blocks in a cube of radius 5 every 40 ticks and shows
 * the 6 largest counts on its front face. The Colony Core reads {@link #counts()}.
 */
public class TallyBoardBlockEntity extends AutarkiaBlockEntity implements Ticking {
    public static final int RADIUS = 5;

    /** One counted item. */
    public record Tally(Item item, long count) {
        public static final Codec<Tally> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Tally::item),
            Codec.LONG.fieldOf("count").forGetter(Tally::count)).apply(i, Tally::new));
    }

    private List<Tally> top = List.of();
    private Map<Item, Long> counts = Map.of();
    private int storages;

    public TallyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.TALLY_BOARD.get(), pos, state);
    }

    /** Return the 6 largest counts, largest first. */
    public List<Tally> top() {
        return top;
    }

    /** Return all counts of the last refresh. */
    public Map<Item, Long> counts() {
        return counts;
    }

    /** Return the number of storage blocks seen at the last refresh. */
    public int storages() {
        return storages;
    }

    @Override
    public void serverTick() {
        if (level.getGameTime() % 40 != Math.floorMod(worldPosition.asLong(), 40)) {
            return;
        }
        refresh();
    }

    /** Recount now. */
    public void refresh() {
        Map<Item, Long> map = new HashMap<>();
        int seen = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RADIUS, -RADIUS, -RADIUS), worldPosition.offset(RADIUS, RADIUS, RADIUS))) {
            if (level.getBlockEntity(p) instanceof CrateBlockEntity crate) {
                seen++;
                for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
                    ItemStack s = crate.slot(i);
                    if (!s.isEmpty()) {
                        map.merge(s.getItem(), (long) s.getCount(), Long::sum);
                    }
                }
            }
        }
        List<Tally> list = new ArrayList<>();
        map.forEach((k, v) -> list.add(new Tally(k, v)));
        list.sort((a, b) -> Long.compare(b.count, a.count));
        List<Tally> newTop = List.copyOf(list.subList(0, Math.min(6, list.size())));
        counts = map;
        boolean changed = !newTop.equals(top) || seen != storages;
        top = newTop;
        storages = seen;
        if (changed) {
            syncNow();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("top", Tally.CODEC.listOf(), top);
        out.putInt("storages", storages);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        top = in.read("top", Tally.CODEC.listOf()).orElse(List.of());
        storages = in.getIntOr("storages", 0);
    }
}
