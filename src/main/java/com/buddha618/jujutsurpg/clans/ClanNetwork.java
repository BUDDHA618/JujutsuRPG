package com.buddha618.jujutsurpg.clans;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.List;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClanNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(JujutsuRPGMod.MODID,"clans"),()->"1","1"::equals,"1"::equals);
    public record Request() {}
    public record Choose(String clan) {}
    public record Action(String action,String argument,long sequence,long nonce) {}
    public record View(boolean required,String clan,int level,int xp,int needed,int reputation,int exam,
                       String school,List<String> available,String message,String task,long sequence,long nonce) {}
    public static final class ViewEvent extends net.minecraftforge.eventbus.api.Event {
        public final View view; public ViewEvent(View view) { this.view=view; }
    }
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{}).decoder(b->new Request()).consumerMainThread((m,c)->{
                var p=c.get().getSender(); if(p!=null && allow(p,"request",5)) send(p,""); c.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Choose.class,1,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeUtf(m.clan(),64)).decoder(b->new Choose(b.readUtf(64)))
            .consumerMainThread((m,c)->{
                var p=c.get().getSender();
                if(p!=null && allow(p,"choose",10)) ClanSelectionService.select(p,m.clan());
                c.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(Action.class,2,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeUtf(m.action(),16);b.writeUtf(m.argument(),64);b.writeLong(m.sequence());b.writeLong(m.nonce());})
            .decoder(b->new Action(b.readUtf(16),b.readUtf(64),b.readLong(),b.readLong()))
            .consumerMainThread((m,c)->{
                var p=c.get().getSender();
                if(p!=null && allow(p,"action",5) && GeneticsSavedData.get(p.getServer()).hasProfile(p.getUUID()))
                    ClanTrainingService.action(p,m);
                c.get().setPacketHandled(true);
            }).add();
        CHANNEL.messageBuilder(View.class,3,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{
                b.writeBoolean(m.required());b.writeUtf(m.clan(),64);b.writeInt(m.level());b.writeInt(m.xp());
                b.writeInt(m.needed());b.writeInt(m.reputation());b.writeInt(m.exam());b.writeUtf(m.school(),64);
                b.writeVarInt(m.available().size());for(var id:m.available())b.writeUtf(id,64);
                b.writeUtf(m.message(),512);b.writeUtf(m.task(),1024);b.writeLong(m.sequence());b.writeLong(m.nonce());
            }).decoder(b->{
                boolean required=b.readBoolean();String clan=b.readUtf(64);int level=b.readInt(),xp=b.readInt(),needed=b.readInt(),rep=b.readInt(),exam=b.readInt();
                String school=b.readUtf(64);int count=b.readVarInt();
                if(count<0||count>32)throw new IllegalArgumentException("Too many schools");
                var available=new java.util.ArrayList<String>();for(int i=0;i<count;i++)available.add(b.readUtf(64));
                return new View(required,clan,level,xp,needed,rep,exam,school,List.copyOf(available),b.readUtf(512),b.readUtf(1024),b.readLong(),b.readLong());
            }).consumerMainThread((m,c)->{
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new ViewEvent(m));c.get().setPacketHandled(true);
            }).add();
    }
    private static boolean allow(ServerPlayer p,String action,int interval) {
        long now=p.getServer().overworld().getGameTime();String key="jjk_clan_packet_"+action;var tag=p.getPersistentData();
        if(tag.contains(key)&&now>=tag.getLong(key)&&now-tag.getLong(key)<interval)return false;
        tag.putLong(key,now);return true;
    }
    public static void send(ServerPlayer p,String message) {
        var data=GeneticsSavedData.get(p.getServer());View view;
        if(!data.hasProfile(p.getUUID()))view=new View(true,"",0,0,0,0,0,"",List.of(),message,"",0,0);
        else {
            var profile=data.getProfile(p.getUUID());var clan=ClanRegistry.find(profile.clan().id()).orElseThrow();
            var progress=data.progression(p.getUUID());var task=ClanTrainingService.describe(p);
            var available=clan.schools().stream().filter(s->progress.available(s,data.visibleTechnique(p.getUUID()))).map(s->s.id()).toList();
            view=new View(false,clan.clan().id(),progress.level(),progress.xp(),progress.requirement(),progress.reputation(),progress.exam(),
                progress.activeSchool(clan),available,message,task.text(),task.sequence(),task.nonce());
        }
        // FakePlayers have no Forge channel; their server logic is still testable.
        if(!(p instanceof net.minecraftforge.common.util.FakePlayer))
            CHANNEL.send(PacketDistributor.PLAYER.with(()->p),view);
    }
}
