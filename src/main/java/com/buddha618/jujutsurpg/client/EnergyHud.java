package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation ORBS=new ResourceLocation(JujutsuRPGMod.MODID,"textures/gui/ce_orbs.png");
    public static void render(ForgeGui gui,GuiGraphics g,float partial,int width,int height) {
        var mc=Minecraft.getInstance();
        if(energy==null||mc.player==null||mc.options.hideGui||mc.player.isSpectator())return;
        double ratio=energy.maximum()>0?energy.current()/energy.maximum():0;
        ratio=Double.isFinite(ratio)?Math.max(0,Math.min(1,ratio)):0;
        int y=height-gui.rightHeight;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        for(int i=0;i<10;i++) {
            int x=width/2+10+i*8;
            g.blit(ORBS,x,y,0,0,9,9,18,9);
            int filled=(int)Math.ceil(Math.max(0,Math.min(1,ratio*10-i))*9);
            if(filled>0)g.blit(ORBS,x,y+9-filled,9,9-filled,9,filled,18,9);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        gui.rightHeight+=10;
    }
}
