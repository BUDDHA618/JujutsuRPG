package com.buddha618.jujutsurpg.network;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CharacterNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(JujutsuRPGMod.MODID, "character"), () -> "2", "2"::equals, "2"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> {}).decoder(buffer -> new Request())
            .consumerMainThread((message, context) -> {
                var player = context.get().getSender();
                if (player != null) {
                    com.buddha618.jujutsurpg.energy.CursedEnergy.get(player).sync(player);
                    var data = GeneticsSavedData.get(player.getServer());
                    // Login creates the profile. Opening this screen never generates one.
                    if (data.hasProfile(player.getUUID())) {
                        var profile = data.getOrCreate(player.getUUID());
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new Profile(profile.clan().getDisplayName(), profile.technique().getId()));
                    }
                }
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Energy.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message, buffer) -> { buffer.writeDouble(message.current()); buffer.writeDouble(message.maximum()); buffer.writeDouble(message.regeneration()); })
            .decoder(buffer -> new Energy(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()))
            .consumerMainThread((message, context) -> {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EnergyEvent(message));
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Profile.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message, buffer) -> { buffer.writeUtf(message.clan(), 128); buffer.writeUtf(message.technique(), 128); })
            .decoder(buffer -> new Profile(buffer.readUtf(128), buffer.readUtf(128)))
            .consumerMainThread((message, context) -> {
                // Client subscribers consume this event only on the physical client.
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ProfileEvent(message));
                context.get().setPacketHandled(true);
            }).add();
    }
    public record Energy(double current, double maximum, double regeneration) {}
    public static final class EnergyEvent extends net.minecraftforge.eventbus.api.Event {
        public final Energy energy;
        public EnergyEvent(Energy energy) { this.energy = energy; }
    }
    public record Request() {}
    public record Profile(String clan, String technique) {}
    public static final class ProfileEvent extends net.minecraftforge.eventbus.api.Event {
        public final Profile profile;
        public ProfileEvent(Profile profile) { this.profile = profile; }
    }
}

