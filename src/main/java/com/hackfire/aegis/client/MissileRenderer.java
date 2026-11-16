package com.hackfire.aegis.client;

import com.hackfire.aegis.Aegis;
import com.hackfire.aegis.entity.MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Procedural 3D missile (lathe body, ogive nose, fins, canards) with a shader-driven exhaust.
 * The plume and halo use the missile_flame shader, which reads day/night from the vertex colour.
 */
public class MissileRenderer extends EntityRenderer<MissileEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Aegis.MODID, "textures/entity/missile.png");

    // Swatch indices in the 4x4 colour palette texture.
    private static final int BODY = 0, BODY_DARK = 1, NOSE = 2, BAND = 3, FIN = 4, NOZZLE = 5;

    private static final int SIDES = 10;
    /** z0, r0, z1, r1, swatch */
    private static final float[][] PROFILE = {
            {-0.80f, 0.070f, -0.72f, 0.105f, NOZZLE},
            {-0.72f, 0.105f, -0.62f, 0.100f, NOZZLE},
            {-0.62f, 0.100f, 0.00f, 0.100f, BODY},
            {0.00f, 0.100f, 0.06f, 0.100f, BAND},
            {0.06f, 0.100f, 0.28f, 0.100f, BODY},
            {0.28f, 0.100f, 0.34f, 0.100f, BAND},
            {0.34f, 0.100f, 0.52f, 0.100f, BODY_DARK},
            {0.52f, 0.100f, 0.66f, 0.075f, NOSE},
            {0.66f, 0.075f, 0.78f, 0.035f, NOSE},
            {0.78f, 0.035f, 0.86f, 0.000f, NOSE},
    };
    private static final float NOZZLE_Z = -0.80f;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(MissileEntity e, float entityYaw, float partial, PoseStack ps,
                       MultiBufferSource buffers, int light) {
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        float centerY = e.getBbHeight() / 2.0f;

        ps.pushPose();
        ps.translate(0.0, centerY, 0.0);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.mulPose(Axis.ZP.rotationDegrees((e.tickCount + partial) * 14.0f));

        drawBody(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light);

        boolean shaderOk = ModShaders.flame != null;
        float night = nightFactor(e, partial, light);
        float power = 0.0f;
        if (shaderOk && e.isThrusting()) {
            power = Mth.clamp((float) e.getDeltaMovement().length() / 2.2f, 0.35f, 1.0f);
            drawPlume(ps, buffers.getBuffer(ModRenderTypes.FLAME_PLUME), e, night, power, partial);
        }
        ps.popPose();

        if (shaderOk && power > 0.0f) {
            double yawRad = Math.toRadians(-yaw);
            double elev = Math.toRadians(-pitch);
            double dx = Math.sin(yawRad) * Math.cos(elev);
            double dy = Math.sin(elev);
            double dz = Math.cos(yawRad) * Math.cos(elev);
            double back = -NOZZLE_Z + 0.25;
            ps.pushPose();
            ps.translate(-dx * back, centerY - dy * back, -dz * back);
            ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
            drawGlow(ps, buffers.getBuffer(ModRenderTypes.FLAME_GLOW), e, night, power);
            ps.popPose();
        }
        super.render(e, entityYaw, partial, ps, buffers, light);
    }

    /** 0 = bright day, 1 = night or dark (also underground / in the Nether and End). */
    private static float nightFactor(MissileEntity e, float partial, int light) {
        var level = e.level();
        if (!level.dimensionType().hasSkyLight()) {
            return 1.0f;
        }
        float sun = Mth.cos(level.getSunAngle(partial));
        float day = Mth.clamp((sun + 0.15f) / 0.45f, 0.0f, 1.0f);
        float sky = LightTexture.sky(light) / 15.0f;
        return Mth.clamp(1.0f - day * sky, 0.0f, 1.0f);
    }

    // ---------------------------------------------------------------- body

    private void drawBody(PoseStack ps, VertexConsumer c, int light) {
        PoseStack.Pose pose = ps.last();
        for (float[] s : PROFILE) {
            int sw = (int) s[4];
            float slope = (s[1] - s[3]) / Math.max(s[2] - s[0], 0.001f);
            for (int i = 0; i < SIDES; i++) {
                double a0 = i * Math.PI * 2.0 / SIDES;
                double a1 = (i + 1) * Math.PI * 2.0 / SIDES;
                double am = (a0 + a1) * 0.5;
                float nx = (float) Math.cos(am);
                float ny = (float) Math.sin(am);
                float nz = slope;
                vert(c, pose, (float) Math.cos(a0) * s[1], (float) Math.sin(a0) * s[1], s[0], sw, light, nx, ny, nz);
                vert(c, pose, (float) Math.cos(a1) * s[1], (float) Math.sin(a1) * s[1], s[0], sw, light, nx, ny, nz);
                vert(c, pose, (float) Math.cos(a1) * s[3], (float) Math.sin(a1) * s[3], s[2], sw, light, nx, ny, nz);
                vert(c, pose, (float) Math.cos(a0) * s[3], (float) Math.sin(a0) * s[3], s[2], sw, light, nx, ny, nz);
            }
        }
        // Tail fins and nose canards, each as a flat trapezoid (rendered double-sided).
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4.0 + k * Math.PI / 2.0;
            fin(c, pose, a, 0.10f, -0.30f, 0.10f, -0.74f, 0.31f, -0.81f, 0.31f, -0.62f, FIN, light);
            fin(c, pose, a, 0.10f, 0.42f, 0.10f, 0.54f, 0.19f, 0.50f, 0.19f, 0.46f, FIN, light);
        }
    }

    private static void fin(VertexConsumer c, PoseStack.Pose pose, double angle,
                            float r0, float z0, float r1, float z1, float r2, float z2, float r3, float z3,
                            int swatch, int light) {
        float ca = (float) Math.cos(angle);
        float sa = (float) Math.sin(angle);
        // normal is perpendicular to the fin's radial plane
        float nx = -sa;
        float ny = ca;
        vert(c, pose, ca * r0, sa * r0, z0, swatch, light, nx, ny, 0);
        vert(c, pose, ca * r1, sa * r1, z1, swatch, light, nx, ny, 0);
        vert(c, pose, ca * r2, sa * r2, z2, swatch, light, nx, ny, 0);
        vert(c, pose, ca * r3, sa * r3, z3, swatch, light, nx, ny, 0);
    }

    private static void vert(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
                             int swatch, int light, float nx, float ny, float nz) {
        float u = ((swatch % 4) * 4 + 2) / 16.0f;
        float v = ((swatch / 4) * 4 + 2) / 16.0f;
        c.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    // ---------------------------------------------------------------- flame

    private void drawPlume(PoseStack ps, VertexConsumer c, MissileEntity e, float night, float power, float partial) {
        PoseStack.Pose pose = ps.last();
        float flick = 0.92f + 0.08f * Mth.sin((e.tickCount + partial) * 2.7f);
        float length = (0.9f + 1.5f * power) * flick;
        float width = 0.30f + 0.08f * power;
        int r = Math.round(night * 255.0f);
        int g = (e.getId() * 37) & 255;
        int a = Math.round(power * 255.0f);
        float z0 = NOZZLE_Z + 0.04f;
        float z1 = z0 - length;
        for (int k = 0; k < 3; k++) {
            double ang = k * Math.PI / 3.0;
            float ax = (float) Math.cos(ang) * width;
            float ay = (float) Math.sin(ang) * width;
            plumeVert(c, pose, -ax, -ay, z0, 0, 0, r, g, a);
            plumeVert(c, pose, ax, ay, z0, 1, 0, r, g, a);
            plumeVert(c, pose, ax, ay, z1, 1, 1, r, g, a);
            plumeVert(c, pose, -ax, -ay, z1, 0, 1, r, g, a);
        }
    }

    private static void plumeVert(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z,
                                  float u, float v, int r, int g, int a) {
        c.addVertex(pose, x, y, z).setColor(r, g, 0, a).setUv(u, v);
    }

    private void drawGlow(PoseStack ps, VertexConsumer c, MissileEntity e, float night, float power) {
        PoseStack.Pose pose = ps.last();
        float s = 1.1f + 0.5f * power;
        int r = Math.round(night * 255.0f);
        int g = (e.getId() * 37) & 255;
        int a = Math.round(power * 255.0f);
        c.addVertex(pose, -s, -s, 0).setColor(r, g, 255, a).setUv(0, 0);
        c.addVertex(pose, s, -s, 0).setColor(r, g, 255, a).setUv(1, 0);
        c.addVertex(pose, s, s, 0).setColor(r, g, 255, a).setUv(1, 1);
        c.addVertex(pose, -s, s, 0).setColor(r, g, 255, a).setUv(0, 1);
    }
}
