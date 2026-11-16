package com.hackfire.aegis.item;

import com.hackfire.aegis.Aegis;
import com.hackfire.aegis.entity.MissileEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MissileItem extends Item {
    private static final double LOCK_RANGE = 96.0;
    private static final double LOCK_CONE = 0.97;

    public MissileItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Vec3 look = player.getLookAngle();
        Entity target = findLock(level, player, look);

        MissileEntity missile = new MissileEntity(Aegis.MISSILE_ENTITY.get(), level);
        missile.setPos(player.getEyePosition().add(look.scale(1.2)));
        missile.launch(look, player, target);
        level.addFreshEntity(missile);

        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 2.0f, 0.5f);
        player.displayClientMessage(target != null
                ? Component.translatable("message.aegis.lock", target.getDisplayName())
                : Component.translatable("message.aegis.no_lock"), true);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static Entity findLock(Level level, Player player, Vec3 look) {
        Vec3 eye = player.getEyePosition();
        AABB box = player.getBoundingBox().expandTowards(look.scale(LOCK_RANGE)).inflate(8);
        Entity best = null;
        double bestDot = LOCK_CONE;
        for (Entity e : level.getEntities(player, box, x -> x instanceof LivingEntity && x.isAlive() && !x.isSpectator())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > LOCK_RANGE || dist < 2.0) continue;
            double dot = to.normalize().dot(look);
            if (dot > bestDot && player.hasLineOfSight(e)) {
                bestDot = dot;
                best = e;
            }
        }
        return best;
    }
}
