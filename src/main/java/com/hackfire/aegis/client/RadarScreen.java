package com.hackfire.aegis.client;

import com.hackfire.aegis.block.RadarBlockEntity;
import com.hackfire.aegis.block.RadarBlockEntity.Contact;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** PPI-style radar scope: rotating sweep, blips that fade after the sweep passes, contact list. */
public class RadarScreen extends Screen {
    private static final double TAU = Math.PI * 2.0;
    private static final int SWEEP_TICKS = 60;
    private final BlockPos pos;

    public RadarScreen(BlockPos pos) {
        super(Component.translatable("screen.aegis.radar"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Contact> contacts() {
        var level = Minecraft.getInstance().level;
        BlockEntity be = level == null ? null : level.getBlockEntity(pos);
        return be instanceof RadarBlockEntity r ? r.getContacts() : List.of();
    }

    private static int argb(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    private void dot(GuiGraphics g, int x, int y, int size, int color) {
        int h = size / 2;
        g.fill(x - h, y - h, x - h + size, y - h + size, color);
    }

    private void ring(GuiGraphics g, int cx, int cy, double r, int color) {
        int n = Math.max(16, (int) (TAU * r / 2.0));
        for (int i = 0; i < n; i++) {
            double a = i * TAU / n;
            int x = (int) Math.round(cx + Math.cos(a) * r);
            int y = (int) Math.round(cy + Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        int radius = Math.max(40, Math.min(height / 2 - 24, 110));
        int cx = Math.max(radius + 12, width / 2 - 70);
        int cy = height / 2;
        int range = RadarBlockEntity.RANGE;

        g.fill(cx - radius - 6, cy - radius - 6, cx + radius + 6, cy + radius + 6, 0xE0020A04);
        for (int i = 1; i <= 4; i++) {
            ring(g, cx, cy, radius * i / 4.0, argb(i == 4 ? 160 : 70, 0x20FF60));
        }
        g.fill(cx - radius, cy, cx + radius + 1, cy + 1, argb(60, 0x20FF60));
        g.fill(cx, cy - radius, cx + 1, cy + radius + 1, argb(60, 0x20FF60));

        double sweep = ((level.getGameTime() % SWEEP_TICKS) + partial) / SWEEP_TICKS * TAU;
        for (int k = 0; k < 28; k++) {
            double a = sweep - k * 0.03;
            int alpha = (int) (150 * (1.0 - k / 28.0));
            for (int s = 2; s <= radius; s += 2) {
                int x = (int) Math.round(cx + Math.cos(a) * s);
                int y = (int) Math.round(cy + Math.sin(a) * s);
                g.fill(x, y, x + 1, y + 1, argb(alpha, 0x40FF80));
            }
        }

        List<Contact> list = new ArrayList<>(contacts());
        for (Contact c : list) {
            double bx = cx + c.dx() / range * radius;
            double by = cy + c.dz() / range * radius;
            double blipAng = Math.atan2(c.dz(), c.dx());
            double diff = ((sweep - blipAng) % TAU + TAU) % TAU;
            int alpha = (int) (255 * Math.max(0.2, 1.0 - diff / TAU));
            int color = switch (c.kind()) {
                case RadarBlockEntity.KIND_MISSILE -> 0xFF3030;
                case RadarBlockEntity.KIND_PLAYER -> 0x30D0FF;
                default -> 0xFFD030;
            };
            dot(g, (int) Math.round(bx), (int) Math.round(by), c.kind() == RadarBlockEntity.KIND_MISSILE ? 5 : 3, argb(alpha, color));
        }
        dot(g, cx, cy, 3, 0xFFFFFFFF);

        int tx = cx + radius + 18;
        int ty = cy - radius;
        g.drawString(font, Component.translatable("screen.aegis.radar"), tx, ty, 0xFF40FF80);
        g.drawString(font, "Range " + range + "m   Contacts " + list.size(), tx, ty + 11, 0xFF9AD8A8);
        list.sort(Comparator.comparingDouble(c -> c.dx() * c.dx() + c.dy() * c.dy() + c.dz() * c.dz()));
        int maxLines = Math.max(0, (radius * 2 - 30) / 10);
        for (int i = 0; i < Math.min(maxLines, list.size()); i++) {
            Contact c = list.get(i);
            int bearing = (int) Math.round((Math.toDegrees(Math.atan2(c.dx(), -c.dz())) + 360.0) % 360.0);
            int dist = (int) Math.round(Math.sqrt(c.dx() * c.dx() + c.dy() * c.dy() + c.dz() * c.dz()));
            int color = c.kind() == RadarBlockEntity.KIND_MISSILE ? 0xFFFF5050
                    : c.kind() == RadarBlockEntity.KIND_PLAYER ? 0xFF50D8FF : 0xFFFFD850;
            String prefix = c.kind() == RadarBlockEntity.KIND_MISSILE ? "! " : "";
            g.drawString(font, String.format("%s%s  %03d\u00b0  %dm", prefix, c.name(), bearing, dist),
                    tx, ty + 28 + i * 10, color);
        }
        super.render(g, mouseX, mouseY, partial);
    }
}
