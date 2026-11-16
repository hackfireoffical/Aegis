package com.hackfire.aegis.entity;

import com.hackfire.aegis.Aegis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Guided missile. Phases: boost (fast acceleration), cruise (proportional-style homing with target lead),
 * terminal (tighter turns when close), then burnout (ballistic fall) when fuel runs out.
 */
public class MissileEntity extends Entity implements ItemSupplier {
    private static final double MAX_SPEED = 2.2;
    private static final int FUEL_TICKS = 160;
    private static final int MAX_AGE = 600;
    private static final float POWER = 3.5f;

    private UUID ownerId;
    private UUID targetId;
    private int age;
    private double speed = 0.3;
    private Vec3 dir = new Vec3(0, 1, 0);

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void launch(Vec3 direction, Entity owner, Entity target) {
        this.dir = direction.normalize();
        this.ownerId = owner == null ? null : owner.getUUID();
        this.targetId = target == null ? null : target.getUUID();
        updateRotation();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        speed = tag.getDouble("Speed");
        dir = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
        if (dir.lengthSqr() < 1.0E-6) dir = new Vec3(0, 1, 0);
        dir = dir.normalize();
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putDouble("Speed", speed);
        tag.putDouble("DirX", dir.x);
        tag.putDouble("DirY", dir.y);
        tag.putDouble("DirZ", dir.z);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        if (targetId != null) tag.putUUID("Target", targetId);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Aegis.MISSILE.get());
    }

    private boolean canHit(Entity e) {
        if (!e.isAlive() || e.isSpectator() || !e.canBeHitByProjectile()) return false;
        if (e instanceof MissileEntity) return false;
        return !(age < 15 && e.getUUID().equals(ownerId));
    }

    private void updateRotation() {
        setYRot((float) (Mth.atan2(dir.x, dir.z) * 57.29577951308232));
        setXRot((float) (Mth.atan2(dir.y, dir.horizontalDistance()) * 57.29577951308232));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        age++;
        boolean fueled = age <= FUEL_TICKS;

        Entity target = targetId == null ? null : sl.getEntity(targetId);
        if (target != null && !target.isAlive()) target = null;

        if (fueled) {
            speed = Math.min(MAX_SPEED, speed + (age < 20 ? 0.06 : 0.04));
            if (target != null && age > 8) {
                Vec3 aim = target.getBoundingBox().getCenter();
                double dist = aim.distanceTo(position());
                double t = dist / Math.max(speed, 0.1);
                Vec3 predicted = aim.add(target.getDeltaMovement().scale(Math.min(t, 40)));
                Vec3 want = predicted.subtract(position());
                if (want.lengthSqr() > 1.0E-6) {
                    want = want.normalize();
                    double turn = dist < 24 ? 0.22 : 0.10;
                    dir = dir.add(want.subtract(dir).scale(turn)).normalize();
                }
            }
        } else {
            speed = Math.max(0.5, speed * 0.985);
            dir = dir.add(0, -0.05, 0).normalize();
        }

        Vec3 start = position();
        Vec3 end = start.add(dir.scale(speed));
        HitResult hit = sl.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(sl, this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0), this::canHit);
        if (eh != null) {
            end = eh.getLocation();
            hit = eh;
        }
        if (hit.getType() != HitResult.Type.MISS) {
            detonate(end);
            return;
        }

        setPos(end);
        setDeltaMovement(dir.scale(speed));
        updateRotation();

        Vec3 back = position().subtract(dir.scale(0.6));
        sl.sendParticles(ParticleTypes.SMOKE, back.x, back.y, back.z, 2, 0.04, 0.04, 0.04, 0.01);
        if (fueled) {
            sl.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 1, 0.02, 0.02, 0.02, 0.02);
            if (age % 20 == 0) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundSource.NEUTRAL, 1.2f, 0.6f);
            }
        }

        if (age > MAX_AGE || getY() < sl.getMinBuildHeight() - 16) {
            discard();
        }
    }

    private void detonate(Vec3 pos) {
        level().explode(this, pos.x, pos.y, pos.z, POWER, Level.ExplosionInteraction.TNT);
        discard();
    }
}
