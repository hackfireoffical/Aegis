package com.hackfire.aegis.client;

import com.hackfire.aegis.Aegis;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = Aegis.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private static final Logger LOGGER = LoggerFactory.getLogger("Aegis");

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Aegis.MISSILE_ENTITY.get(), MissileRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(Aegis.MODID, "missile_flame"),
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            event.registerShader(shader, instance -> ModShaders.flame = instance);
        } catch (Exception ex) {
            // Never crash the game over a cosmetic shader: the missile just renders without its plume.
            LOGGER.error("Aegis: missile flame shader failed to load, using particles only", ex);
        }
    }
}
