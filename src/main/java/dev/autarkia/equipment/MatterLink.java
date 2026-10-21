package dev.autarkia.equipment;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.EventTrigger;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterValues;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * The matter link of {@code nanite_multitool} and {@code disassembler_beam} (07 section 1.6, decision C15): sneak + use
 * on a {@code matter_buffer} stores the buffer center in {@code autarkia:matter_link}. While linked, every drop with a
 * matter value becomes MU in {@code autarkia:matter_cache} (at most {@link #MAX_CACHE}) for {@link #FE_PER_ITEM} FE per
 * item; drops that do not fit, have no value or find no FE fall as items. Within {@code tools.matterStreamRange}
 * blocks of the linked buffer the cache flows into it as free MU at 64 MU per second (16 MU every 5 ticks) along a
 * visible nanite stream. The equipment event {@code matter_cache_full} fires when a cache reaches {@link #MAX_CACHE}.
 * Raw ore MU is read at the tier of the domain that holds the buffer (5 outside every domain).
 */
public final class MatterLink {
    public static final int MAX_CACHE = 4096;
    public static final int FE_PER_ITEM = 20;
    public static final int FLOW_PERIOD = 5;
    public static final int FLOW_PER_PERIOD = 16;
    public static final int STREAM_COLOR = 0xFF9FB4C8;

    private MatterLink() {
    }

    /** Register the drop listener that fills the cache of a linked multitool (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(MatterLink::onDrops);
    }

    /** Return the cached MU of a stack. */
    public static int cache(ItemStack stack) {
        return stack.getOrDefault(AutarkiaComponents.MATTER_CACHE.get(), 0);
    }

    /**
     * Link the tool to the clicked {@code matter_buffer} when the player sneaks; return PASS for any other click so the
     * item can run its own use. Both sides return SUCCESS for a link click; only the server writes the component.
     */
    public static InteractionResult tryLink(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !ctx.isSecondaryUseActive() || !ctx.getLevel().getBlockState(ctx.getClickedPos()).is(AutarkiaBlocks.MATTER_BUFFER.get())) {
            return InteractionResult.PASS;
        }
        if (!(ctx.getLevel() instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        MatterBufferBlockEntity buffer = MatterBank.nearest(sl, ctx.getClickedPos(), 2);
        if (buffer == null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.matter_link.unformed"));
            return InteractionResult.FAIL;
        }
        BlockPos center = buffer.getBlockPos();
        ctx.getItemInHand().set(AutarkiaComponents.MATTER_LINK.get(), GlobalPos.of(sl.dimension(), center));
        player.sendOverlayMessage(Component.translatable("message.autarkia.matter_link", center.getX(), center.getY(), center.getZ()));
        return InteractionResult.SUCCESS;
    }

    /**
     * Turn the drops of one broken block into cached MU; the stacks shrink by what the cache took. Needs a link, the
     * FE for each item and room in the cache.
     *
     * @param tool the real tool stack (its FE and cache change), not a copy
     * @param drops the drop stacks; stacks left non-empty fall as items
     */
    public static void absorb(ServerLevel level, Player player, ItemStack tool, List<ItemStack> drops) {
        GlobalPos link = tool.get(AutarkiaComponents.MATTER_LINK.get());
        if (link == null) {
            return;
        }
        int tier = domainTier(level, link);
        int cache = cache(tool);
        boolean wasFull = cache >= MAX_CACHE;
        for (ItemStack s : drops) {
            if (s.isEmpty() || !MatterValues.accepted(s, tier)) {
                continue;
            }
            long value = MatterValues.of(s, tier);
            if (value <= 0 || value > MAX_CACHE - cache) {
                continue;
            }
            int n = (int) Math.min(s.getCount(), Math.min((MAX_CACHE - cache) / value, PoweredItem.energy(tool) / FE_PER_ITEM));
            if (n <= 0) {
                continue;
            }
            cache += (int) (n * value);
            PoweredItem.setEnergy(tool, PoweredItem.energy(tool) - n * FE_PER_ITEM);
            s.shrink(n);
        }
        tool.set(AutarkiaComponents.MATTER_CACHE.get(), cache);
        if (!wasFull && cache >= MAX_CACHE && player instanceof ServerPlayer sp) {
            EventTrigger.EQUIPMENT.get().fire(sp, "matter_cache_full");
        }
    }

    /**
     * Move cached MU into the linked buffer while the holder is within {@code tools.matterStreamRange} blocks of it:
     * {@link #FLOW_PER_PERIOD} MU every {@link #FLOW_PERIOD} ticks, drawn as a nanite stream from the holder to the
     * buffer. A full or missing buffer keeps the cache.
     */
    public static void flow(ServerLevel level, Entity holder, ItemStack tool) {
        int cache = cache(tool);
        GlobalPos link = tool.get(AutarkiaComponents.MATTER_LINK.get());
        if (cache <= 0 || link == null || !link.dimension().equals(level.dimension()) || level.getGameTime() % FLOW_PERIOD != 0) {
            return;
        }
        BlockPos pos = link.pos();
        Vec3 target = Vec3.atCenterOf(pos);
        Vec3 from = holder.position().add(0, holder.getBbHeight() * 0.6, 0);
        if (from.distanceTo(target) > AutarkiaConfig.TOOLS_MATTER_STREAM_RANGE.get() || !level.isLoaded(pos)
            || !(level.getBlockEntity(pos) instanceof MatterBufferBlockEntity buffer)) {
            return;
        }
        long moved = buffer.addFree(Math.min(cache, FLOW_PER_PERIOD));
        if (moved <= 0) {
            return;
        }
        tool.set(AutarkiaComponents.MATTER_CACHE.get(), cache - (int) moved);
        stream(level, from, target);
    }

    /** Add the link and cache lines to a tool tooltip. */
    public static void tooltip(ItemStack stack, Consumer<Component> out) {
        GlobalPos link = stack.get(AutarkiaComponents.MATTER_LINK.get());
        if (link == null) {
            out.accept(Component.translatable("tooltip.autarkia.matter_unlinked").withColor(0x9FB4C8));
            return;
        }
        out.accept(Component.translatable("tooltip.autarkia.matter_link", link.pos().getX(), link.pos().getY(), link.pos().getZ(),
            link.dimension().identifier().toString()).withColor(0x9FB4C8));
        out.accept(Component.translatable("tooltip.autarkia.matter_cache", cache(stack), MAX_CACHE).withColor(0x9FB4C8));
    }

    private static void onDrops(BlockDropsEvent e) {
        if (!(e.getBreaker() instanceof Player p)) {
            return;
        }
        ItemStack tool = p.getMainHandItem();
        if (!(tool.getItem() instanceof NaniteMultitoolItem) || !e.getTool().is(tool.getItem())) {
            return;
        }
        absorb(e.getLevel(), p, tool, e.getDrops().stream().map(ItemEntity::getItem).toList());
        e.getDrops().removeIf(ie -> ie.getItem().isEmpty());
    }

    private static int domainTier(ServerLevel level, GlobalPos link) {
        ServerLevel at = level.getServer().getLevel(link.dimension());
        Colony c = at == null ? null : Colonies.at(at, link.pos());
        return c == null ? 5 : Math.max(5, c.tier);
    }

    private static void stream(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int n = Math.max(2, (int) Math.ceil(d.length() / 0.75));
        double phase = (level.getGameTime() / FLOW_PERIOD % 3) / 3.0;
        GlowParticleOptions opt = GlowParticleOptions.of(STREAM_COLOR).size(0.15f).age(20);
        for (int i = 0; i < n; i++) {
            Vec3 p = from.add(d.scale(Math.min(1.0, (i + phase) / n)));
            level.sendParticles(opt, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }
}
