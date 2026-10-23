package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.builder.BuilderNetwork;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,value=Dist.CLIENT)
public final class BuilderGhost {
    private record Ghost(BlockPos pos,int state) {}
    private static final List<Ghost> BLOCKS=new ArrayList<>();
    private static final Set<Long> SOLID=new HashSet<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    public static boolean ready;
    @SubscribeEvent public static void library(BuilderNetwork.LibraryEvent e){BuilderScreen.library(e.files);}
    @SubscribeEvent public static void preview(BuilderNetwork.PreviewEvent e){var m=e.data;if(m.reset()){BLOCKS.clear();SOLID.clear();ready=false;}for(int i=0;i<m.positions().length && BLOCKS.size()<200000;i++){BLOCKS.add(new Ghost(BlockPos.of(m.positions()[i]),m.states()[i]));if(Block.stateById(m.states()[i]).canOcclude())SOLID.add(m.positions()[i]);}if(m.done())ready=!BLOCKS.isEmpty();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(Minecraft.getInstance().level!=world){world=Minecraft.getInstance().level;BLOCKS.clear();SOLID.clear();ready=false;}}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||BLOCKS.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var camera=e.getCamera().getPosition();var pose=e.getPoseStack();var buffers=mc.renderBuffers().bufferSource();int drawn=0;
        net.minecraft.client.renderer.MultiBufferSource ghostBuffers=type->new Fade(buffers.getBuffer(RenderType.translucent()));
        for(var b:BLOCKS){
            if(b.pos().distToCenterSqr(camera.x,camera.y,camera.z)>32*32)continue;
            boolean enclosed=true;
            for(var direction:net.minecraft.core.Direction.values())if(!SOLID.contains(b.pos().relative(direction).asLong())){enclosed=false;break;}
            if(enclosed)continue;
            if(drawn++>=4096)break;
            pose.pushPose();pose.translate(b.pos().getX()-camera.x,b.pos().getY()-camera.y,b.pos().getZ()-camera.z);
            mc.getBlockRenderer().renderSingleBlock(Block.stateById(b.state()),pose,ghostBuffers,15728880,OverlayTexture.NO_OVERLAY,
                net.minecraftforge.client.model.data.ModelData.EMPTY,RenderType.translucent());pose.popPose();
        }
        buffers.endBatch(RenderType.translucent());
    }
    private static final class Fade implements VertexConsumer {
        private final VertexConsumer delegate;Fade(VertexConsumer delegate){this.delegate=delegate;}
        public VertexConsumer vertex(double x,double y,double z){delegate.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){delegate.color((int)(r*.65),(int)(g*.85),b,(int)(a*.28));return this;}
        public VertexConsumer uv(float u,float v){delegate.uv(u,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){delegate.overlayCoords(u,v);return this;}
        public VertexConsumer uv2(int u,int v){delegate.uv2(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){delegate.normal(x,y,z);return this;}
        public void endVertex(){delegate.endVertex();}
        public void defaultColor(int r,int g,int b,int a){delegate.defaultColor(r,g,b,(int)(a*.28));}
        public void unsetDefaultColor(){delegate.unsetDefaultColor();}
    }
}
