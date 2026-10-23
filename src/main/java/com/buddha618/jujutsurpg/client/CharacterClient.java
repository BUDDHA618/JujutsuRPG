package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID, value = Dist.CLIENT)
public final class CharacterClient {
    public static final KeyMapping OPEN = new KeyMapping("key.jujutsurpg.character", GLFW.GLFW_KEY_K, "key.categories.jujutsurpg");
    public static final KeyMapping BLUE = new KeyMapping("Синий (Безграничность)", GLFW.GLFW_KEY_Z, "key.categories.jujutsurpg");
    public static final KeyMapping PURPLE = new KeyMapping("Фиолетовый (Безграничность)", GLFW.GLFW_KEY_X, "key.categories.jujutsurpg");
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        while (BLUE.consumeClick()) if (mc.player != null && mc.screen == null)
            CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.CastAbility(false));
        while (PURPLE.consumeClick()) if (mc.player != null && mc.screen == null)
            CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.CastAbility(true));
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new CharacterScreen());
                CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Request());
            }
        }
    }
    @SubscribeEvent
    public static void profile(CharacterNetwork.ProfileEvent event) {
        if (Minecraft.getInstance().screen instanceof CharacterScreen screen) screen.profile = event.profile;
    }
    @Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); event.register(BLUE); event.register(PURPLE); }
    }
}
