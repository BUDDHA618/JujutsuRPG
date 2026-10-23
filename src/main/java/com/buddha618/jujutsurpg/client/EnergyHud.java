package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID, value = Dist.CLIENT)
public final class EnergyHud {
    private static CharacterNetwork.Energy energy;
    @SubscribeEvent public static void receive(CharacterNetwork.EnergyEvent event) { energy = event.energy; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { energy = null; }
    public static String description() {
        return energy == null ? "ПЭ: ожидание сервера…" : String.format(java.util.Locale.ROOT,
            "ПЭ: %.0f / %.0f (+%.0f/с)", energy.current(), energy.maximum(), energy.regeneration());
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (energy == null || mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        var g = event.getGuiGraphics();
        int x = 12, y = 12, w = 140;
        g.fill(x - 3, y - 3, x + w + 3, y + 28, 0xB015192B);
        g.drawString(mc.font, description(), x, y, 0xD4C4FF, false);
        g.fill(x, y + 14, x + w, y + 23, 0xFF30334B);
        double ratio = energy.maximum() > 0 ? energy.current() / energy.maximum() : 0;
        g.fill(x, y + 14, x + (int)(w * Math.max(0, Math.min(1, ratio))), y + 23, 0xFF9270EB);
    }
}
