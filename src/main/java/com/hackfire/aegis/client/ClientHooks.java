package com.hackfire.aegis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientHooks {
    private ClientHooks() {}

    public static void openRadar(BlockPos pos) {
        Minecraft.getInstance().setScreen(new RadarScreen(pos));
    }
}
