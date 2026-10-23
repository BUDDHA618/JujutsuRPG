package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.curses.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CurseRendering {
    private static final ModelLayerLocation[] LAYERS={layer("jogo"),layer("hanami"),layer("mahito")};
    private static ModelLayerLocation layer(String id) {return new ModelLayerLocation(new ResourceLocation(JujutsuRPGMod.MODID,id),"main");}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        for(int i=0;i<3;i++) {final int kind=i;e.registerLayerDefinition(LAYERS[i],()->Model.mesh(kind));}
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CurseEntities.JOGO.get(),c->new Renderer(c,0));
        e.registerEntityRenderer(CurseEntities.HANAMI.get(),c->new Renderer(c,1));
        e.registerEntityRenderer(CurseEntities.MAHITO.get(),c->new Renderer(c,2));
    }
    private static final class Renderer extends MobRenderer<CurseSpirit,Model> {
        private final int kind;
        Renderer(EntityRendererProvider.Context c,int kind) {super(c,new Model(c.bakeLayer(LAYERS[kind])),.6f);this.kind=kind;}
        @Override public ResourceLocation getTextureLocation(CurseSpirit e) {return new ResourceLocation(JujutsuRPGMod.MODID,"textures/entity/"+new String[]{"jogo","hanami","mahito"}[kind]+".png");}
        @Override protected void scale(CurseSpirit e,PoseStack p,float t) {float s=kind==1?1.2f:kind==0?.9f:1;p.scale(s,s,s);}
    }
    private static final class Model extends EntityModel<CurseSpirit> {
        final ModelPart root,head,leftArm,rightArm,leftLeg,rightLeg,body;
        Model(ModelPart root) {this.root=root;head=root.getChild("head");body=root.getChild("body");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");}
        private static PartDefinition box(PartDefinition parent,String name,int u,int v,float x,float y,float z,float w,float h,float d,PartPose pose) {
            return parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d),pose);
        }
        static LayerDefinition mesh(int kind) {
            MeshDefinition mesh=new MeshDefinition();var r=mesh.getRoot();
            var body=box(r,"body",0,32,-5,0,-3,10,12,6,PartPose.offset(0,0,0));
            var head=box(r,"head",0,0,-4,-8,-4,8,8,8,PartPose.ZERO);
            var la=box(r,"left_arm",0,0,-2,-1,-2,4,13,4,PartPose.offset(7,1,0));
            var ra=box(r,"right_arm",0,0,-2,-1,-2,4,13,4,PartPose.offset(-7,1,0));
            box(r,"left_leg",0,32,-2,0,-2,4,12,4,PartPose.offset(2.6f,12,0));
            box(r,"right_leg",0,32,-2,0,-2,4,12,4,PartPose.offset(-2.6f,12,0));
            if(kind==0) {
                box(head,"volcano_base",0,64,-5,-10,-4.5f,10,3,9,PartPose.ZERO);
                box(head,"volcano_neck",0,64,-3.5f,-13,-3,7,3,6,PartPose.ZERO);
                box(head,"crater",64,32,-2.5f,-13.2f,-2,5,.5f,4,PartPose.ZERO);
                box(head,"eye_socket",0,64,-3,-6,-4.5f,6,4,1,PartPose.ZERO);
                box(head,"single_eye",64,0,-2,-5,-5,4,2,1,PartPose.ZERO);
                box(head,"pupil",0,32,-.7f,-5,-5.6f,1.4f,2,.5f,PartPose.ZERO);
                box(head,"jaw",0,64,-3,-2,-4.5f,6,2,1,PartPose.ZERO);
                box(head,"ear_left",0,64,0,-2,-2,3,4,4,PartPose.offset(4,-3,0));
                box(head,"ear_right",0,64,-3,-2,-2,3,4,4,PartPose.offset(-4,-3,0));
                box(body,"mantle",64,64,-7,0,-4,14,4,8,PartPose.ZERO);
            } else if(kind==1) {
                box(body,"bark_armor",0,64,-6,-1,-4,12,8,8,PartPose.ZERO);
                box(body,"chest_bloom",64,32,-3,3,-4.8f,6,5,2,PartPose.ZERO);
                box(head,"mask",64,0,-4.2f,-8,-4.6f,8.4f,8,1,PartPose.ZERO);
                for(int side:new int[]{-1,1}) {
                    var branch=box(head,"eye_branch"+side,0,64,-.7f,-8,-.7f,1.4f,9,1.4f,PartPose.offsetAndRotation(side*2,-5,-4.5f,0,0,side*.6f));
                    box(branch,"fork",0,64,-.5f,-6,-.5f,1,6,1,PartPose.rotation(.5f,0,-side*.8f));
                    box(body,"shoulder"+side,0,64,-2,-8,-2,4,9,4,PartPose.offsetAndRotation(side*6,2,0,0,0,side*.45f));
                }
                box(la,"roots",0,64,-2,8,-2,4,8,4,PartPose.ZERO);
                box(ra,"roots",0,64,-2,8,-2,4,8,4,PartPose.ZERO);
            } else {
                box(head,"hair_cap",0,64,-4.5f,-8.5f,-4,9,2,9,PartPose.ZERO);
                for(int i=0;i<6;i++) box(head,"hair_lock"+i,0,64,-.7f,0,-1,1.4f,9+i%3,2,PartPose.offsetAndRotation(-4+i*1.6f,-7,3.5f,.12f,0,(i-3)*.07f));
                box(head,"eye_left",64,32,1,-5,-4.2f,2,1,1,PartPose.ZERO);
                box(head,"eye_right",64,0,-3,-5,-4.2f,2,1,1,PartPose.ZERO);
                box(head,"face_seam",64,64,-.25f,-8,-4.3f,.5f,8,.5f,PartPose.rotation(0,0,.12f));
                box(ra,"mutated_blade",64,0,-1.5f,8,-3,3,11,6,PartPose.rotation(.15f,0,-.15f));
                for(int i=0;i<4;i++) box(la,"finger"+i,0,0,-1.7f+i,11,-1, .7f,5+i%2,2,PartPose.ZERO);
                for(int i=0;i<5;i++) box(body,"stitch"+i,64,64,-4,1+i*2,-3.1f,8,.4f,.5f,PartPose.ZERO);
            }
            return LayerDefinition.create(mesh,128,128);
        }
        @Override public void setupAnim(CurseSpirit e,float walk,float amount,float age,float yaw,float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
            leftLeg.xRot=Mth.cos(walk*.6662f)*1.2f*amount;rightLeg.xRot=Mth.cos(walk*.6662f+Mth.PI)*1.2f*amount;
            leftArm.xRot=Mth.cos(walk*.6662f+Mth.PI)*amount;rightArm.xRot=Mth.cos(walk*.6662f)*amount;
            leftArm.zRot=.08f+Mth.sin(age*.06f)*.04f;rightArm.zRot=-leftArm.zRot;
            body.yScale=1+Mth.sin(age*.08f)*.015f;
            float attack=Mth.sin(attackTime*Mth.PI);rightArm.xRot-=attack*1.8f;leftArm.xRot-=attack*1.2f;
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,float red,float green,float blue,float alpha) {root.render(pose,consumer,light,overlay,red,green,blue,alpha);}
    }
}
