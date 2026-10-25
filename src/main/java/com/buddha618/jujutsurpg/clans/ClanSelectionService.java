package com.buddha618.jujutsurpg.clans;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Pending characters are held at a server position, protected, and unable to act. */
@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID)
public final class ClanSelectionService {
    private static final Map<MinecraftServer,Map<UUID,Vec3>> ANCHORS=new WeakHashMap<>();
    public static boolean pending(ServerPlayer p) { return !GeneticsSavedData.get(p.getServer()).hasProfile(p.getUUID()); }
    public static void select(ServerPlayer p,String id) {
        if(!p.isAlive() || p.isSpectator()) { ClanNetwork.send(p,"Выбор доступен живому персонажу."); return; }
        boolean created=GeneticsSavedData.get(p.getServer()).selectOrigin(p.getUUID(),id,java.util.concurrent.ThreadLocalRandom.current());
        if(created) {
            ANCHORS.computeIfAbsent(p.getServer(),s->new HashMap<>()).remove(p.getUUID());
            com.buddha618.jujutsurpg.energy.CursedEnergy.get(p).sync(p);
        }
        ClanNetwork.send(p,created?"Происхождение подтверждено. Начните клановое обучение через K.":"Выбор отклонён: профиль уже существует или происхождение неизвестно.");
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !pending(p))return;
        var anchor=ANCHORS.computeIfAbsent(p.getServer(),s->new HashMap<>()).computeIfAbsent(p.getUUID(),id->p.position());
        if(p.position().distanceToSqr(anchor)>.04)p.teleportTo(p.serverLevel(),anchor.x,anchor.y,anchor.z,p.getYRot(),p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.clearFire();p.setAirSupply(p.getMaxAirSupply());
        if(p.tickCount%40==0)ClanNetwork.send(p,"");
    }
    @SubscribeEvent public static void attack(LivingAttackEvent e) {
        if(e.getEntity() instanceof ServerPlayer p && pending(p))e.setCanceled(true);
        else if(e.getSource().getEntity() instanceof ServerPlayer p && pending(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent e) {
        if(e.isCancelable() && e.getEntity() instanceof ServerPlayer p && pending(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void attackEntity(AttackEntityEvent e) {
        if(e.getEntity() instanceof ServerPlayer p && pending(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent e) {
        if(e.getPlayer() instanceof ServerPlayer p && pending(p))e.setCanceled(true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { clear(e); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { clear(e); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { clear(e); }
    private static void clear(PlayerEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            var map=ANCHORS.get(p.getServer());if(map!=null)map.remove(p.getUUID());
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { ANCHORS.remove(e.getServer()); }
}
