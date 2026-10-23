package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.curses.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class StudentRenderer extends HumanoidMobRenderer<StudentMage,HumanoidModel<StudentMage>> {
    public StudentRenderer(EntityRendererProvider.Context c) {super(c,new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER)),.45f);}
    @Override public ResourceLocation getTextureLocation(StudentMage entity) {return new ResourceLocation(JujutsuRPGMod.MODID,"textures/entity/student.png");}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e) {e.registerEntityRenderer(CurseEntities.STUDENT.get(),StudentRenderer::new);}
}
