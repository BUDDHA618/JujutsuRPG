package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.clans.ClanNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,value=Dist.CLIENT)
public final class ClanClient {
    public static ClanNetwork.View view;
    private static int ticks;
    public static final KeyMapping STEP=new KeyMapping("Подтвердить шаг клановой тренировки",GLFW.GLFW_KEY_V,"key.categories.jujutsurpg");
    @SubscribeEvent public static void receive(ClanNetwork.ViewEvent e) {
        view=e.view;var mc=Minecraft.getInstance();
        if(mc.player==null)return;
        if(view.required()&&!(mc.screen instanceof ClanSelectionScreen))mc.setScreen(new ClanSelectionScreen());
        else if(!view.required()&&mc.screen instanceof ClanSelectionScreen) {
            mc.setScreen(new CharacterScreen());
            com.buddha618.jujutsurpg.network.CharacterNetwork.CHANNEL.sendToServer(new com.buddha618.jujutsurpg.network.CharacterNetwork.Request());
        }
        if(mc.screen instanceof ClanScreen screen)screen.refresh();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        if(++ticks%20==0 && (view==null||view.required())) {
            ClanNetwork.CHANNEL.sendToServer(new ClanNetwork.Request());
            if(view!=null && view.required() && !(mc.screen instanceof ClanSelectionScreen))mc.setScreen(new ClanSelectionScreen());
        }
        while(STEP.consumeClick())if(mc.screen==null&&view!=null&&view.sequence()>0)send("step","");
    }
    public static void send(String action,String argument) {
        ClanNetwork.CHANNEL.sendToServer(new ClanNetwork.Action(action,argument,view==null?0:view.sequence(),view==null?0:view.nonce()));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){view=null;ticks=0;}
    @Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(STEP);}
    }
}
