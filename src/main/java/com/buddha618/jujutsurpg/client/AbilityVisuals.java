package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.network.CharacterNetwork;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

/** World-space translucent geometry; never draws a screen overlay. */
@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,value=Dist.CLIENT)
public final class AbilityVisuals {
    private static final Map<Integer,Visual> EFFECTS=new HashMap<>();
    private static final class Visual {
        Vec3 old,pos;int kind,life=0;
        Visual(Vec3 pos,int kind) {old=pos;this.pos=pos;this.kind=kind;}
    }
    @SubscribeEvent public static void update(CharacterNetwork.AbilityVisualEvent e) {
        var mc=Minecraft.getInstance();var m=e.visual;
        if(mc.level==null || m.kind()>2 || !Double.isFinite(m.x()) || !Double.isFinite(m.y()) || !Double.isFinite(m.z()))return;
        var pos=new Vec3(m.x(),m.y(),m.z());
        var v=EFFECTS.computeIfAbsent(m.owner(),id->new Visual(pos,m.kind()));
        v.old=v.pos;v.pos=pos;v.kind=m.kind();v.life=0;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        if(Minecraft.getInstance().level==null) {EFFECTS.clear();return;}
        EFFECTS.values().removeIf(v->++v.life>5);
    }
    private static void vertex(VertexConsumer b,Matrix4f m,Vec3 p,float r,float g,float blue,float alpha) {
        b.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(r,g,blue,alpha).endVertex();
    }
    private static Vec3 sphere(double longitude,double latitude,double radius) {
        return new Vec3(Math.cos(longitude)*Math.sin(latitude)*radius,Math.cos(latitude)*radius,Math.sin(longitude)*Math.sin(latitude)*radius);
    }
    private static void shell(VertexConsumer b,Matrix4f m,double radius,float r,float g,float blue,float alpha,double rotation) {
        for(int y=0;y<10;y++)for(int x=0;x<24;x++) {
            double u=x*Math.PI*2/24+rotation,w=(x+1)*Math.PI*2/24+rotation;
            double v=y*Math.PI/10,t=(y+1)*Math.PI/10;
            vertex(b,m,sphere(u,v,radius),r,g,blue,alpha);
            vertex(b,m,sphere(w,v,radius),r,g,blue,alpha);
            vertex(b,m,sphere(w,t,radius),r,g,blue,alpha);
            vertex(b,m,sphere(u,t,radius),r,g,blue,alpha);
        }
    }
    private static void arc(VertexConsumer b,Matrix4f m,Vec3 a,Vec3 end,float r,float g,float blue,float alpha) {
        Vec3 side=end.subtract(a).cross(new Vec3(.2,1,.3)).normalize().scale(.018);
        vertex(b,m,a.add(side),r,g,blue,alpha);vertex(b,m,end.add(side),r,g,blue,alpha);
        vertex(b,m,end.subtract(side),r,g,blue,alpha);vertex(b,m,a.subtract(side),r,g,blue,alpha);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var camera=e.getCamera().getPosition();var pose=e.getPoseStack();
        var buffers=mc.renderBuffers().bufferSource();var buffer=buffers.getBuffer(RenderType.lightning());
        double time=mc.level.getGameTime()+e.getPartialTick();
        for(var entry:EFFECTS.entrySet()) {
            var v=entry.getValue();
            Vec3 position=v.old.lerp(v.pos,Math.min(1,e.getPartialTick()+v.life));
            double distance=position.distanceTo(camera);
            // Fade near the first-person camera so shots never fill the viewport.
            float alpha=(float)Math.max(0,Math.min(1,(distance-2.3)/2));
            if(alpha<=0)continue;
            alpha*=Math.max(0,1-v.life/6f);
            float r=v.kind==0?.03f:v.kind==1?.7f:1;
            float g=v.kind==0?.22f:v.kind==1?.04f:.025f;
            float blue=v.kind==2?.06f:1;
            double size=v.kind==1?1.5: .85;
            pose.pushPose();pose.translate(position.x-camera.x,position.y-camera.y,position.z-camera.z);
            Matrix4f matrix=pose.last().pose();
            shell(buffer,matrix,size,r,g,blue,.12f*alpha,time*.05);
            shell(buffer,matrix,size*.65,r,g,blue,.28f*alpha,-time*.09);
            shell(buffer,matrix,size*.32,1,.8f,1,.5f*alpha,time*.1);
            // Three rotating jagged filaments with bright branches.
            for(int axis=0;axis<3;axis++) {
                Vec3 previous=null;
                for(int i=0;i<=36;i++) {
                    double angle=i*Math.PI*2/36+time*(axis%2==0?.13:-.17);
                    double radius=size*(1.07+.12*Math.sin(i*2.3+Math.floor(time/2)));
                    Vec3 point=new Vec3(Math.cos(angle)*radius,Math.sin(angle)*radius,Math.sin(angle*3+time*.1)*.12);
                    if(axis==1)point=new Vec3(point.z,point.x,point.y);
                    if(axis==2)point=new Vec3(point.y,point.z,point.x);
                    if(previous!=null)arc(buffer,matrix,previous,point,r,.35f,blue,.8f*alpha);
                    if(i%9==0)arc(buffer,matrix,point,point.scale(1.3),1,.7f,1,.65f*alpha);
                    previous=point;
                }
            }
            pose.popPose();
        }
        buffers.endBatch(RenderType.lightning());
    }
}
