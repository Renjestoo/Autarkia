package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridMember;
import dev.autarkia.energy.Span;
import dev.autarkia.logistics.BeltBlockEntity;
import dev.autarkia.processing.MachineBlockEntity;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;

/**
 * Pattern loom (05 section 5.1): a blank {@code pattern_card} slot, the capture box (offset -24..+24 from the loom per
 * axis, size 1 to the tier max, default 8 x 8 x 8 in front of the loom), the Protect, Library and Strict air toggles,
 * and the capture scan of {@code pattern.scanCellsPerTick} cells per tick, bottom layer first, with a glow plane
 * sweeping up. The finished Pattern goes to its save file, the card gets {@code autarkia:pattern_ref}, a watched
 * instance and a library entry are added when their toggles are on.
 */
public class PatternLoomBlockEntity extends AutarkiaBlockEntity implements MenuProvider, JadeInfo, Ticking {
    public static final TagKey<Block> PATTERN_IGNORED = TagKey.create(Registries.BLOCK, Autarkia.id("pattern_ignored"));
    public static final int PROTECT = 1;
    public static final int LIBRARY = 2;
    public static final int STRICT_AIR = 4;
    private final ItemStacksResourceHandler card = new ItemStacksResourceHandler(1) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.is(AutarkiaItems.PATTERN_CARD.get());
        }

        @Override
        public int getCapacity(int index, ItemResource resource) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (level instanceof ServerLevel) {
                syncNow();
            }
        }
    };
    private final int[] offset = new int[3];
    private final int[] size = {8, 8, 8};
    private int flags = PROTECT | LIBRARY;
    private Scan scan;

    /** A capture in progress. */
    private static final class Scan {
        final UUID colony;
        final UUID author;
        final String name;
        final BlockPos min;
        final Vec3i size;
        final boolean strict;
        final int[] indices;
        final List<BlockState> palette = new ArrayList<>(List.of(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
        final Map<BlockState, Integer> lookup = new HashMap<>();
        final List<Integer> foreign = new ArrayList<>();
        final List<Integer> parts = new ArrayList<>();
        final Map<String, CompoundTag> data = new LinkedHashMap<>();
        final Map<Item, Integer> bom = new LinkedHashMap<>();
        final List<BlockEntity> found = new ArrayList<>();
        int cursor;

        Scan(UUID colony, UUID author, String name, BlockPos min, Vec3i size, boolean strict) {
            this.colony = colony;
            this.author = author;
            this.name = name;
            this.min = min;
            this.size = size;
            this.strict = strict;
            this.indices = new int[size.getX() * size.getY() * size.getZ()];
        }
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0, 1, 2 -> offset[i];
                case 3, 4, 5 -> size[i - 3];
                case 6 -> flags;
                case 7 -> scan == null ? -1 : (int) (1000L * scan.cursor / Math.max(1, scan.indices.length));
                default -> maxSize();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public PatternLoomBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.PATTERN_LOOM.get(), pos, state);
        Direction front = MachineBlock.front(state);
        BlockPos rel = BlockPos.ZERO.relative(front).offset(-4, 0, -4).offset(front.getStepX() * 4, 0, front.getStepZ() * 4);
        offset[0] = rel.getX();
        offset[1] = 0;
        offset[2] = rel.getZ();
    }

    /** Return the card slot handler. */
    public ItemStacksResourceHandler card() {
        return card;
    }

    /** Return the menu data: 0-2 offset, 3-5 size, 6 flags, 7 scan progress per mille (-1 idle), 8 max size. */
    public ContainerData data() {
        return data;
    }

    /** Return the capture box min corner. */
    public BlockPos boxMin() {
        return worldPosition.offset(offset[0], offset[1], offset[2]);
    }

    /** Return the capture box size. */
    public Vec3i boxSize() {
        return new Vec3i(size[0], size[1], size[2]);
    }

    /** Return the max capture size of the colony tier here (0 outside a T2+ domain or on the client). */
    public int maxSize() {
        if (!(level instanceof ServerLevel sl) || Colonies.server() == null) {
            return 48;
        }
        Colony c = Colonies.at(sl, worldPosition);
        return c == null ? 0 : ColonyTiers.patternMax(c.tier);
    }

    /** Apply menu button id: 0-5 decrease offset x, y, z and size x, y, z; 6-11 increase them; 12-14 toggle Protect, Library, Strict air. */
    public void button(int id) {
        if (scan != null) {
            return;
        }
        if (id >= 0 && id < 12) {
            int axis = id % 6;
            int d = id < 6 ? -1 : 1;
            if (axis < 3) {
                offset[axis] = Math.max(-24, Math.min(24, offset[axis] + d));
            } else {
                size[axis - 3] = Math.max(1, Math.min(Math.max(1, maxSize()), size[axis - 3] + d));
            }
        } else if (id >= 12 && id <= 14) {
            flags ^= 1 << (id - 12);
        }
        syncNow();
    }

    /** Return the card stack in the slot. */
    public ItemStack cardStack() {
        return card.getResource(0).toStack(card.getAmountAsInt(0));
    }

    /**
     * Start a capture for the player (05 section 5.1 step 3); returns the failure message key, or null when the scan
     * started. Needs a blank card, a colony domain where the player is owner or trusted, tier 2 or more, the box
     * within the tier max and every chunk of the box loaded.
     */
    public String record(ServerPlayer player, String name) {
        ServerLevel sl = serverLevel();
        if (scan != null) {
            return "message.autarkia.pattern.busy";
        }
        ItemStack st = cardStack();
        if (st.isEmpty() || st.has(AutarkiaComponents.PATTERN_REF.get())) {
            return "message.autarkia.pattern.no_blank";
        }
        Colony c = Colonies.at(sl, worldPosition);
        if (c == null || !c.canManage(player.getUUID())) {
            return "message.autarkia.pattern.no_rights";
        }
        int max = ColonyTiers.patternMax(c.tier);
        if (c.tier < 2 || max == 0) {
            return "message.autarkia.pattern.tier";
        }
        if (size[0] > max || size[1] > max || size[2] > max) {
            return "message.autarkia.pattern.too_big";
        }
        BlockPos min = boxMin();
        BlockPos max3 = min.offset(size[0] - 1, size[1] - 1, size[2] - 1);
        for (int cx = min.getX() >> 4; cx <= max3.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max3.getZ() >> 4; cz++) {
                if (!sl.hasChunk(cx, cz)) {
                    return "message.autarkia.pattern.unloaded";
                }
            }
        }
        String n = name.isBlank() ? "Pattern " + (c.library.size() + 1) : name.trim();
        scan = new Scan(c.id, player.getUUID(), n.substring(0, Math.min(32, n.length())), min, boxSize(), (flags & STRICT_AIR) != 0);
        sl.playSound(null, worldPosition, AutarkiaSounds.of("pattern_scan"), SoundSource.BLOCKS, 1f, 1f);
        syncNow();
        return null;
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        if (sl == null || scan == null) {
            return;
        }
        Scan s = scan;
        int sx = s.size.getX();
        int sz = s.size.getZ();
        int end = Math.min(s.indices.length, s.cursor + AutarkiaConfig.PATTERN_SCAN_CELLS_PER_TICK.get());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = s.cursor; i < end; i++) {
            int x = i % sx;
            int z = i / sx % sz;
            int y = i / (sx * sz);
            p.set(s.min.getX() + x, s.min.getY() + y, s.min.getZ() + z);
            s.indices[i] = classify(sl, s, i, p);
        }
        int layer = end / (sx * sz);
        if (s.cursor / (sx * sz) != layer || end == s.indices.length) {
            glow(sl, s, Math.min(layer, s.size.getY() - 1));
        }
        s.cursor = end;
        if (end >= s.indices.length) {
            finish(sl, s);
        }
    }

    /** Draw the capture box outline with glow particles along its 12 edges every 10 ticks while a blank card is in the slot. */
    @Override
    public void clientTick() {
        ItemStack st = cardStack();
        if (level == null || level.getGameTime() % 10 != 0 || st.isEmpty() || st.has(AutarkiaComponents.PATTERN_REF.get())) {
            return;
        }
        BlockPos min = boxMin();
        double x0 = min.getX();
        double y0 = min.getY();
        double z0 = min.getZ();
        double x1 = x0 + size[0];
        double y1 = y0 + size[1];
        double z1 = z0 + size[2];
        double[][] edges = {{x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
            {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
            {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
        var opt = com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions.of(0xFF60E0FF);
        for (double[] e : edges) {
            double len = Math.max(Math.abs(e[3] - e[0]), Math.max(Math.abs(e[4] - e[1]), Math.abs(e[5] - e[2])));
            int n = Math.max(2, (int) Math.ceil(len));
            for (int k = 0; k <= n; k++) {
                double t = k / (double) n;
                level.addParticle(opt, e[0] + (e[3] - e[0]) * t, e[1] + (e[4] - e[1]) * t, e[2] + (e[5] - e[2]) * t, 0, 0, 0);
            }
        }
    }

    private int classify(ServerLevel sl, Scan s, int i, BlockPos p) {
        BlockState st = sl.getBlockState(p);
        if (st.isAir() && !s.strict || st.is(PATTERN_IGNORED) || st.is(BlockTags.FIRE)
            || !st.getFluidState().isEmpty() && !st.getFluidState().isSource() && st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            return 0;
        }
        BlockEntity be = sl.getBlockEntity(p);
        if (be instanceof IMultiBlockEntity mb && !mb.isCenter()) {
            s.parts.add(i);
            return 0;
        }
        if (be != null) {
            s.found.add(be);
            Identifier type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
            if (type != null && type.getNamespace().equals(Autarkia.MOD_ID)) {
                if (be instanceof PatternDataHolder h) {
                    TagValueOutput out = TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, sl.registryAccess());
                    h.writePatternData(out);
                    s.data.put(Integer.toString(i), out.buildResult());
                }
            } else {
                s.foreign.add(i);
            }
        }
        if (!st.isAir()) {
            Item item = st.getBlock().asItem();
            if (item != net.minecraft.world.item.Items.AIR) {
                s.bom.merge(item, 1, Integer::sum);
            }
        }
        return s.lookup.computeIfAbsent(st, k -> {
            s.palette.add(k);
            return s.palette.size() - 1;
        });
    }

    private void glow(ServerLevel sl, Scan s, int y) {
        double py = s.min.getY() + y + 1.0;
        var opt = com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions.of(0xFF60E0FF);
        for (int k = 0; k < 24; k++) {
            double px = s.min.getX() + sl.getRandom().nextDouble() * s.size.getX();
            double pz = s.min.getZ() + sl.getRandom().nextDouble() * s.size.getZ();
            sl.sendParticles(opt, px, py, pz, 1, 0, 0, 0, 0);
        }
    }

    private void finish(ServerLevel sl, Scan s) {
        scan = null;
        Colony c = Colonies.registry().get(s.colony);
        int bits = Pattern.bitsFor(s.palette.size());
        long[] cells = Pattern.pack(s.indices, bits);
        List<Pattern.SpanRecord> spans = new ArrayList<>();
        List<Pattern.Port> ports = new ArrayList<>();
        BlockPos max = s.min.offset(s.size.getX() - 1, s.size.getY() - 1, s.size.getZ() - 1);
        net.minecraft.world.level.levelgen.structure.BoundingBox box = net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(s.min, max);
        for (Span sp : Grid.of(sl).data().spans.values()) {
            BlockPos a = BlockPos.of(sp.a);
            BlockPos b = BlockPos.of(sp.b);
            boolean ia = box.isInside(a);
            boolean ib = box.isInside(b);
            if (ia && ib) {
                spans.add(new Pattern.SpanRecord(a.subtract(s.min), b.subtract(s.min), sp.tier));
            } else if (ia || ib) {
                BlockPos in = ia ? a : b;
                BlockPos out = ia ? b : a;
                ports.add(new Pattern.Port(in.subtract(s.min), Pattern.PortKind.POWER, Direction.getApproximateNearest(out.getX() - in.getX(), 0,
                    out.getZ() - in.getZ())));
            }
        }
        boolean machine = false;
        boolean belt = false;
        boolean generator = false;
        boolean storage = false;
        boolean dock = false;
        boolean outpost = false;
        for (BlockEntity be : s.found) {
            String path = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()) == null ? ""
                : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getPath();
            machine |= be instanceof MachineBlockEntity;
            generator |= be instanceof GridMember gm && gm.gridRole() == GridMember.Role.GENERATOR;
            storage |= be instanceof ColonyStorage;
            dock |= path.equals("recharge_dock");
            outpost |= path.equals("outpost_core");
            if (be instanceof BeltBlockEntity bb) {
                belt = true;
                Direction f = MachineBlock.front(bb.getBlockState());
                BlockPos rel = bb.getBlockPos().subtract(s.min);
                if (!box.isInside(bb.getBlockPos().relative(f))) {
                    ports.add(new Pattern.Port(rel, Pattern.PortKind.ITEM_OUT, f));
                } else if (!box.isInside(bb.getBlockPos().relative(f.getOpposite()))) {
                    ports.add(new Pattern.Port(rel, Pattern.PortKind.ITEM_IN, f.getOpposite()));
                }
            }
        }
        String role = outpost ? "outpost" : dock ? "defense" : machine && belt ? "line" : generator ? "generator" : storage ? "storage" : "generic";
        CRC32 crc = new CRC32();
        for (int k : s.indices) {
            crc.update(k);
        }
        s.palette.forEach(st -> crc.update(st.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Pattern pattern = new Pattern(UUID.randomUUID(), s.name, s.author, sl.getGameTime(), s.size, List.copyOf(s.palette), cells, bits,
            List.copyOf(s.foreign), List.copyOf(s.parts), Map.copyOf(s.data), List.copyOf(spans), List.copyOf(ports), Map.copyOf(s.bom),
            (int) crc.getValue(), role);
        Pattern.store(sl.getServer(), pattern);
        int blocks = pattern.blocks();
        ItemStack out = cardStack().copyWithCount(1);
        out.set(AutarkiaComponents.PATTERN_REF.get(), Pattern.Ref.of(pattern, blocks));
        card.set(0, ItemResource.of(out), 1);
        ServerPlayer author = sl.getServer().getPlayerList().getPlayer(s.author);
        boolean added = false;
        if (c != null) {
            if ((flags & PROTECT) != 0) {
                c.instances.add(new Colony.Instance(UUID.randomUUID(), pattern.id(), s.min, net.minecraft.world.level.block.Rotation.NONE, s.size, true,
                    new long[(pattern.volume() + 63) / 64]));
            }
            if ((flags & LIBRARY) != 0 && c.library.size() < ColonyTiers.patternSlots(c.tier)) {
                c.library.add(new Colony.LibraryEntry(pattern.id(), role, true));
                added = true;
            }
            ColonyRuntime.of(c).log("Pattern " + pattern.name() + " recorded: " + blocks + " blocks");
            ColonyEvents.fire(c, "pattern_recorded");
            Colonies.changed();
        }
        Autarkia.LOG.info("[pattern] recorded {} ({}): {}x{}x{}, {} blocks, {} foreign, role {}", pattern.name(), pattern.id(), s.size.getX(),
            s.size.getY(), s.size.getZ(), blocks, s.foreign.size(), role);
        if (author != null) {
            author.sendSystemMessage(Component.translatable("message.autarkia.pattern.recorded", pattern.name(), blocks, s.foreign.size(),
                Component.translatable(added ? "message.autarkia.pattern.in_library" : "message.autarkia.pattern.not_in_library")));
        }
        sl.playSound(null, worldPosition, AutarkiaSounds.of("pattern_scan"), SoundSource.BLOCKS, 1f, 1.5f);
        syncNow();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cardStack());
        }
    }

    @Override
    public String jadeGroup() {
        return "machine";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Pattern.Ref ref = cardStack().get(AutarkiaComponents.PATTERN_REF.get());
        if (ref != null) {
            out.add(Component.translatable("jade.autarkia.loom.pattern", ref.name(), ref.sx(), ref.sy(), ref.sz(), ref.blocks()));
        } else {
            out.add(Component.translatable(cardStack().isEmpty() ? "jade.autarkia.loom.no_card" : "jade.autarkia.loom.blank"));
        }
        if (scan != null) {
            out.add(Component.translatable("jade.autarkia.loom.scanning", 100L * scan.cursor / Math.max(1, scan.indices.length)));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.autarkia.pattern_loom");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new PatternLoomMenu(id, playerInv, this);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        card.serialize(out.child("card"));
        out.putIntArray("boxOffset", offset.clone());
        out.putIntArray("size", size.clone());
        out.putInt("flags", flags);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("card").ifPresent(card::deserialize);
        in.getIntArray("boxOffset").filter(a -> a.length == 3).ifPresent(a -> System.arraycopy(a, 0, offset, 0, 3));
        in.getIntArray("size").filter(a -> a.length == 3).ifPresent(a -> System.arraycopy(a, 0, size, 0, 3));
        flags = in.getIntOr("flags", PROTECT | LIBRARY);
    }
}
