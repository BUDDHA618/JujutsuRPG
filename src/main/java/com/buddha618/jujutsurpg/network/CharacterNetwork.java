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
        new ResourceLocation(JujutsuRPGMod.MODID, "character"), () -> "9", "9"::equals, "9"::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        CHANNEL.messageBuilder(AbilityVisual.class, 7, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b) -> {b.writeInt(m.owner());b.writeByte(m.kind());b.writeDouble(m.x());b.writeDouble(m.y());b.writeDouble(m.z());})
            .decoder(b -> new AbilityVisual(b.readInt(),b.readUnsignedByte(),b.readDouble(),b.readDouble(),b.readDouble()))
            .consumerMainThread((m,c) -> {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new AbilityVisualEvent(m));
                c.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(DeveloperState.class, 6, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b) -> b.writeBoolean(m.infinite()))
            .decoder(b -> new DeveloperState(b.readBoolean()))
            .consumerMainThread((m,context) -> {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new DeveloperStateEvent(m.infinite()));
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(CastAbility.class, 5, NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b) -> b.writeByte(m.kind()))
            .decoder(b -> new CastAbility(b.readUnsignedByte()))
            .consumerMainThread((m,context) -> {
                var player=context.get().getSender();
                if(player!=null && allow(player,"cast",1)) com.buddha618.jujutsurpg.techniques.LimitlessAbilities.activate(player,m.kind());
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Developer.class, 4, NetworkDirection.PLAY_TO_SERVER)
            .encoder((m, b) -> { b.writeUtf(m.action(), 16); b.writeUtf(m.clan(), 64); b.writeUtf(m.technique(), 64); b.writeBoolean(m.awakened()); })
            .decoder(b -> new Developer(b.readUtf(16), b.readUtf(64), b.readUtf(64), b.readBoolean()))
            .consumerMainThread((m, context) -> {
                var player = context.get().getSender();
                if (player != null && player.hasPermissions(2) && allow(player,"developer",5)) {
                    if (m.action().equals("infinite") || m.action().equals("state")) {
                        var energy = com.buddha618.jujutsurpg.energy.CursedEnergy.get(player);
                        if (m.action().equals("infinite")) {
                            energy.toggleInfinite(player);
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "DEV: бесконечная ПЭ " + (energy.infinite(player) ? "включена" : "выключена")), true);
                        }
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DeveloperState(energy.infinite(player)));
                    } else if (m.action().equals("restore")) {
                        com.buddha618.jujutsurpg.energy.CursedEnergy.get(player).developerRestore(player);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("DEV: ПЭ восстановлена, перегрузка снята."));
                    } else if (m.action().equals("profile")) {
                        var clan = java.util.Arrays.stream(com.buddha618.jujutsurpg.genetics.Clan.values())
                            .filter(v -> v.getId().equals(m.clan())).findFirst();
                        var technique = java.util.Arrays.stream(com.buddha618.jujutsurpg.genetics.InnateTechnique.values())
                            .filter(v -> v.getId().equals(m.technique())).findFirst();
                        if (clan.isPresent() && technique.isPresent()) {
                            GeneticsSavedData.get(player.getServer()).developerSet(player.getUUID(), clan.get(), technique.get(), m.awakened());
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("DEV: профиль изменён: " + clan.get().getDisplayName()
                                + " / " + technique.get().getId() + " / пробуждение: " + m.awakened()));
                        }
                    }
                }
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Reinforce.class, 3, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> buffer.writeByte(message.mode()))
            .decoder(buffer -> new Reinforce(buffer.readUnsignedByte()))
            .consumerMainThread((message, context) -> {
                var player = context.get().getSender();
                if (player != null) com.buddha618.jujutsurpg.energy.EnergyReinforcement.activate(player, message.mode());
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> {}).decoder(buffer -> new Request())
            .consumerMainThread((message, context) -> {
                var player = context.get().getSender();
                if (player != null && allow(player,"profile",5)) {
                    com.buddha618.jujutsurpg.energy.CursedEnergy.get(player).sync(player);
                    var data = GeneticsSavedData.get(player.getServer());
                    // Login creates the profile. Opening this screen never generates one.
                    if (data.hasProfile(player.getUUID())) {
                        var profile = data.getOrCreate(player.getUUID());
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new Profile(profile.clan().getDisplayName(), GeneticsSavedData.get(player.getServer()).visibleTechnique(player.getUUID()), data.hasSixEyes(player.getUUID())));
                    }
                }
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Energy.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message, buffer) -> { buffer.writeDouble(message.current()); buffer.writeDouble(message.maximum()); buffer.writeDouble(message.regeneration()); buffer.writeDouble(message.output());
                buffer.writeDouble(message.control()); buffer.writeDouble(message.loss()); buffer.writeDouble(message.strain()); buffer.writeBoolean(message.overloaded()); })
            .decoder(buffer -> new Energy(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean()))
            .consumerMainThread((message, context) -> {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EnergyEvent(message));
                context.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Profile.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message, buffer) -> { buffer.writeUtf(message.clan(), 128); buffer.writeUtf(message.technique(), 128); buffer.writeBoolean(message.sixEyes()); })
            .decoder(buffer -> new Profile(buffer.readUtf(128), buffer.readUtf(128), buffer.readBoolean()))
            .consumerMainThread((message, context) -> {
                // Client subscribers consume this event only on the physical client.
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ProfileEvent(message));
                context.get().setPacketHandled(true);
            }).add();
    }
    private static boolean allow(net.minecraft.server.level.ServerPlayer player, String action, int interval) {
        long now = player.getServer().overworld().getGameTime();
        String key = "jujutsurpg_packet_" + action;
        var tag = player.getPersistentData();
        long last = tag.getLong(key);
        if (tag.contains(key) && now >= last && now - last < interval) return false;
        tag.putLong(key, now);
        return true;
    }
    public record Energy(double current, double maximum, double regeneration, double output,
                         double control, double loss, double strain, boolean overloaded) {}
    public static final class EnergyEvent extends net.minecraftforge.eventbus.api.Event {
        public final Energy energy;
        public EnergyEvent(Energy energy) { this.energy = energy; }
    }
    public record AbilityVisual(int owner,int kind,double x,double y,double z) {}
    public static final class AbilityVisualEvent extends net.minecraftforge.eventbus.api.Event {
        public final AbilityVisual visual;
        public AbilityVisualEvent(AbilityVisual visual) {this.visual=visual;}
    }
    public record DeveloperState(boolean infinite) {}
    public static final class DeveloperStateEvent extends net.minecraftforge.eventbus.api.Event {
        public final boolean infinite;
        public DeveloperStateEvent(boolean infinite) { this.infinite = infinite; }
    }
    public record CastAbility(int kind) {}
    public record Developer(String action, String clan, String technique, boolean awakened) {}
    public record Reinforce(int mode) {}
    public record Request() {}
    public record Profile(String clan, String technique, boolean sixEyes) {}
    public static final class ProfileEvent extends net.minecraftforge.eventbus.api.Event {
        public final Profile profile;
        public ProfileEvent(Profile profile) { this.profile = profile; }
    }
}
