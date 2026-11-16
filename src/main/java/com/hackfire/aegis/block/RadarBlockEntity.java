package com.hackfire.aegis.block;

import com.hackfire.aegis.Aegis;
import com.hackfire.aegis.entity.MissileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RadarBlockEntity extends BlockEntity {
    public static final int RANGE = 64;
    public static final int KIND_MOB = 0;
    public static final int KIND_PLAYER = 1;
    public static final int KIND_MISSILE = 2;

    public record Contact(float dx, float dy, float dz, int kind, String name) {}

    private List<Contact> contacts = new ArrayList<>();
    private int signal;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(Aegis.RADAR_BE.get(), pos, state);
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    /** Redstone output: 0 when no missile is tracked, 1-15 rising as the nearest missile gets closer. */
    public int getSignal() {
        return signal;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RadarBlockEntity be) {
        if (level.getGameTime() % 5 != 0) return;
        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(RANGE);
        List<Contact> found = new ArrayList<>();
        double nearestMissile = Double.MAX_VALUE;
        for (Entity e : level.getEntities((Entity) null, box,
                x -> x.isAlive() && (x instanceof LivingEntity || x instanceof MissileEntity))) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(center);
            if (d.lengthSqr() > (double) RANGE * RANGE) continue;
            int kind = e instanceof MissileEntity ? KIND_MISSILE : e instanceof Player ? KIND_PLAYER : KIND_MOB;
            if (kind == KIND_MISSILE) nearestMissile = Math.min(nearestMissile, Math.sqrt(d.lengthSqr()));
            found.add(new Contact((float) d.x, (float) d.y, (float) d.z, kind, e.getType().getDescription().getString()));
            if (found.size() >= 64) break;
        }
        int newSignal = nearestMissile == Double.MAX_VALUE ? 0 : Math.max(1, 15 - (int) (nearestMissile / RANGE * 15));
        if (newSignal != be.signal) {
            be.signal = newSignal;
            level.updateNeighborsAt(pos, state.getBlock());
        }
        if (!found.equals(be.contacts)) {
            be.contacts = found;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Contact c : contacts) {
            CompoundTag t = new CompoundTag();
            t.putFloat("x", c.dx());
            t.putFloat("y", c.dy());
            t.putFloat("z", c.dz());
            t.putInt("k", c.kind());
            t.putString("n", c.name());
            list.add(t);
        }
        tag.put("Contacts", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        List<Contact> loaded = new ArrayList<>();
        ListTag list = tag.getList("Contacts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            loaded.add(new Contact(t.getFloat("x"), t.getFloat("y"), t.getFloat("z"), t.getInt("k"), t.getString("n")));
        }
        contacts = loaded;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
