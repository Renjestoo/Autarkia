package dev.autarkia.equipment;

import dev.autarkia.registry.AutarkiaEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Sling and steam repeater projectile (07 section 1.2): renders its ammo item and deals autarkia:sling damage. */
public class SlingShotEntity extends ThrowableItemProjectile {
    private float damage = 3;

    public SlingShotEntity(EntityType<? extends SlingShotEntity> type, Level level) {
        super(type, level);
    }

    public SlingShotEntity(Level level, LivingEntity owner, ItemStack ammo, float damage) {
        super(AutarkiaEntities.SLING_SHOT.get(), owner, level, ammo);
        this.damage = damage;
    }

    /** Return the hit damage. */
    public float damage() {
        return damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3 && !getItem().isEmpty()) {
            for (int i = 0; i < 6; i++) {
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(getItem())), getX(), getY(), getZ(),
                    (random.nextDouble() - 0.5) * 0.1, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            hit.getEntity().hurtServer(sl, damageSources().source(AutarkiaDamage.SLING, this, getOwner()), damage);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putFloat("damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        damage = in.getFloatOr("damage", 3);
    }
}
