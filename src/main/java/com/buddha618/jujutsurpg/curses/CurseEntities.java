package com.buddha618.jujutsurpg.curses;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class CurseEntities {
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,JujutsuRPGMod.MODID);
    public static final RegistryObject<EntityType<CurseSpirit>> JOGO=type("jogo_curse",.9f,1.8f);
    public static final RegistryObject<EntityType<CurseSpirit>> HANAMI=type("hanami_curse",1.2f,2.7f);
    public static final RegistryObject<EntityType<CurseSpirit>> MAHITO=type("mahito_curse",.8f,2.1f);
    public static final RegistryObject<EntityType<StudentMage>> STUDENT=TYPES.register("student_mage",()->
        EntityType.Builder.of(StudentMage::new,MobCategory.CREATURE).sized(.6f,1.8f).clientTrackingRange(10).build("jujutsurpg:student_mage"));
    public static final RegistryObject<Item> STUDENT_EGG=JujutsuRPGMod.ITEMS.register("student_spawn_egg",()->new ForgeSpawnEggItem(STUDENT,0x18233D,0xD9B782,new Item.Properties()));
    private static RegistryObject<EntityType<CurseSpirit>> type(String id,float width,float height) {
        return TYPES.register(id,()->EntityType.Builder.of(CurseSpirit::new,MobCategory.MONSTER).sized(width,height)
            .clientTrackingRange(10).build(JujutsuRPGMod.MODID+":"+id));
    }
    public static final RegistryObject<Item> JOGO_EGG=JujutsuRPGMod.ITEMS.register("jogo_spawn_egg",()->new ForgeSpawnEggItem(JOGO,0x342D3A,0xFF601A,new Item.Properties()));
    public static final RegistryObject<Item> HANAMI_EGG=JujutsuRPGMod.ITEMS.register("hanami_spawn_egg",()->new ForgeSpawnEggItem(HANAMI,0xD2C9A6,0x46653A,new Item.Properties()));
    public static final RegistryObject<Item> MAHITO_EGG=JujutsuRPGMod.ITEMS.register("mahito_spawn_egg",()->new ForgeSpawnEggItem(MAHITO,0x8BABC0,0x423A61,new Item.Properties()));
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(CurseEntities::attributes);
        bus.addListener(CurseEntities::creative);
    }
    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(STUDENT.get(),net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MAX_HEALTH,40)
            .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,24).build());
        e.put(JOGO.get(),CurseSpirit.createMonsterAttributes().add(Attributes.MAX_HEALTH,70).add(Attributes.MOVEMENT_SPEED,.29).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.FOLLOW_RANGE,28).build());
        e.put(HANAMI.get(),CurseSpirit.createMonsterAttributes().add(Attributes.MAX_HEALTH,120).add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.ARMOR,8).add(Attributes.KNOCKBACK_RESISTANCE,.7).add(Attributes.FOLLOW_RANGE,28).build());
        e.put(MAHITO.get(),CurseSpirit.createMonsterAttributes().add(Attributes.MAX_HEALTH,85).add(Attributes.MOVEMENT_SPEED,.33).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.FOLLOW_RANGE,28).build());
    }
    private static void creative(BuildCreativeModeTabContentsEvent e) {
        if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS) { e.accept(JOGO_EGG);e.accept(HANAMI_EGG);e.accept(MAHITO_EGG);e.accept(STUDENT_EGG); }
    }
}
