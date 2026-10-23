package com.buddha618.jujutsurpg.builder;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BuilderNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(JujutsuRPGMod.MODID,"builder"),()->"1","1"::equals,"1"::equals);
    public record Request(String action,String file,BlockPos origin,int rotation) {}
    public record Library(String files) {}
    public record Preview(boolean reset,boolean done,long[] positions,int[] states) {}
    public static final class LibraryEvent extends net.minecraftforge.eventbus.api.Event {public final String files;public LibraryEvent(String files){this.files=files;}}
    public static final class PreviewEvent extends net.minecraftforge.eventbus.api.Event {public final Preview data;public PreviewEvent(Preview data){this.data=data;}}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent e) {
        CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeUtf(m.action(),16);b.writeUtf(m.file(),80);b.writeBlockPos(m.origin());b.writeByte(m.rotation());})
            .decoder(b->new Request(b.readUtf(16),b.readUtf(80),b.readBlockPos(),b.readUnsignedByte()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null && p.hasPermissions(2))BuilderService.handle(p,m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Library.class,1,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->b.writeUtf(m.files(),16000)).decoder(b->new Library(b.readUtf(16000)))
            .consumerMainThread((m,c)->{net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new LibraryEvent(m.files()));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Preview.class,2,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeBoolean(m.reset());b.writeBoolean(m.done());b.writeVarInt(m.positions().length);for(int i=0;i<m.positions().length;i++){b.writeLong(m.positions()[i]);b.writeVarInt(m.states()[i]);}})
            .decoder(b->{boolean reset=b.readBoolean(),done=b.readBoolean();int n=b.readVarInt();if(n<0||n>2048)throw new IllegalArgumentException("Preview size");long[] pos=new long[n];int[] states=new int[n];for(int i=0;i<n;i++){pos[i]=b.readLong();states[i]=b.readVarInt();}return new Preview(reset,done,pos,states);})
            .consumerMainThread((m,c)->{net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PreviewEvent(m));c.get().setPacketHandled(true);}).add();
    }
}
