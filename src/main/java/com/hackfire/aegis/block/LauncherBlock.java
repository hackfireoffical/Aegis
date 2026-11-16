package com.hackfire.aegis.block;

import com.hackfire.aegis.Aegis;
import com.hackfire.aegis.entity.MissileEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Vertical missile launcher: load missiles by hand, fire on a redstone pulse. Targets the nearest hostile mob. */
public class LauncherBlock extends Block {
    public static final MapCodec<LauncherBlock> CODEC = simpleCodec(LauncherBlock::new);
    public static final int MAX_AMMO = 4;
    public static final IntegerProperty AMMO = IntegerProperty.create("ammo", 0, MAX_AMMO);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final double TARGET_RANGE = 64.0;

    public LauncherBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AMMO, 0).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMMO, POWERED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Aegis.MISSILE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int ammo = state.getValue(AMMO);
        if (ammo >= MAX_AMMO) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(AMMO, ammo + 1), 3);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.8f);
            player.displayClientMessage(Component.translatable("message.aegis.ammo", ammo + 1, MAX_AMMO), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.aegis.ammo", state.getValue(AMMO), MAX_AMMO), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        BlockState next = state.setValue(POWERED, powered);
        if (powered) {
            int ammo = state.getValue(AMMO);
            if (ammo > 0) {
                fire((ServerLevel) level, pos);
                next = next.setValue(AMMO, ammo - 1);
            } else {
                level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        level.setBlock(pos, next, 3);
    }

    private static void fire(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(TARGET_RANGE);
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof Enemy && e.isAlive())
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(center)))
                .orElse(null);

        MissileEntity missile = new MissileEntity(Aegis.MISSILE_ENTITY.get(), level);
        missile.setPos(pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5);
        missile.launch(new Vec3(0, 1, 0), null, target);
        level.addFreshEntity(missile);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 2.0f, 0.5f);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            int ammo = state.getValue(AMMO);
            if (ammo > 0 && !level.isClientSide) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(Aegis.MISSILE.get(), ammo));
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
