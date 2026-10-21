package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * The {@code thrown_autarky_seed} projectile (05 section 8.4, 08b section 1): flies like an ender pearl (gravity 0.03,
 * thrown at 1.5, G10) and draws with the vanilla thrown item renderer. It keeps the thrower and the thrower's facing at
 * the throw (saved). On impact it lands through {@link SeedLanding}: a block hit lands on the hit column, an entity hit
 * at the impact point. A failed landing drops the seed as an item at the impact point and tells the thrower why.
 */
public class ThrownAutarkySeed extends ThrowableItemProjectile {
    public static final float VELOCITY = 1.5F;
    private @Nullable UUID thrower;
    private Direction facing = Direction.NORTH;

    public ThrownAutarkySeed(EntityType<? extends ThrownAutarkySeed> type, Level level) {
        super(type, level);
    }

    public ThrownAutarkySeed(Level level, LivingEntity owner, ItemStack seed) {
        super(AutarkiaEntities.THROWN_AUTARKY_SEED.get(), owner, level, seed);
        this.thrower = owner.getUUID();
        this.facing = owner.getDirection();
    }

    /** Return the UUID of the thrower, or null. */
    public @Nullable UUID thrower() {
        return thrower;
    }

    /** Return the thrower's horizontal facing at the throw (the footprint rotation). */
    public Direction facing() {
        return facing;
    }

    @Override
    protected Item getDefaultItem() {
        return AutarkiaItems.AUTARKY_SEED.get();
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel sl) || isRemoved()) {
            return;
        }
        BlockPos at = hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK ? b.getBlockPos() : BlockPos.containing(hit.getLocation());
        ItemStack stack = getItem().copy();
        String why = thrower == null ? "message.autarkia.outpost.no_colony" : SeedLanding.land(sl, stack, thrower, facing, at);
        if (why != null) {
            ItemEntity drop = new ItemEntity(sl, hit.getLocation().x, hit.getLocation().y + 0.25, hit.getLocation().z, stack);
            drop.setDefaultPickUpDelay();
            sl.addFreshEntity(drop);
            ServerPlayer p = thrower == null ? null : sl.getServer().getPlayerList().getPlayer(thrower);
            if (p != null) {
                p.sendSystemMessage(Component.translatable("message.autarkia.seed.failed", Component.translatable(why)).withStyle(ChatFormatting.RED));
            }
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (thrower != null) {
            out.store("thrower", UUIDUtil.CODEC, thrower);
        }
        out.putString("facing", facing.getSerializedName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        thrower = in.read("thrower", UUIDUtil.CODEC).orElse(null);
        Direction d = Direction.byName(in.getStringOr("facing", "north"));
        facing = d == null || d.getAxis().isVertical() ? Direction.NORTH : d;
    }
}
