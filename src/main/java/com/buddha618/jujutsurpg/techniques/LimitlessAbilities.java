package com.buddha618.jujutsurpg.techniques;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.energy.CursedEnergy;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import java.util.*;

/** Playable prototype. Costs, damage, range and cooldowns are provisional test balance. */
@Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID)
public final class LimitlessAbilities {
    private static final Map<UUID, Cast> ACTIVE = new HashMap<>();
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(.02f,.3f,1), 1.5f);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1,.03f,.08f), 1.5f);
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(.7f,.08f,1), 1.8f);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1,.9f,1), 1);
    // 0 Blue, 1 Purple, 2 Red. Numbers are explicit provisional combat tuning.
    private static final class Cast {
        Vec3 position, direction;
        final int kind;
        int age;
        double travelled;
        final Set<UUID> hit = new HashSet<>();
        final Set<BlockPos> checked = new HashSet<>();
        Cast(int kind) { this.kind=kind; }
        int charge() { return kind==1 ? 36 : 16; }
        double range() { return kind==1 ? 30 : kind==2 ? 10 : 5; }
        double radius() { return kind==1 ? 1.5 : 1; }
        DustParticleOptions color() { return kind==1 ? PURPLE : kind==2 ? RED : BLUE; }
    }
    public static void activate(ServerPlayer p, int kind) {
        if (kind<0 || kind>2 || !p.isAlive() || p.isSpectator()) return;
        if (!GeneticsSavedData.get(p.getServer()).visibleTechnique(p.getUUID()).equals("limitless")) {
            tell(p, "Нужна пробуждённая Безграничность. K → DEV для тестирования."); return;
        }
        long now=p.serverLevel().getGameTime();
        String key=kind==1 ? "jjk_purple_ready" : kind==2 ? "jjk_red_ready" : "jjk_blue_ready";
        if (ACTIVE.containsKey(p.getUUID()) || p.getPersistentData().getLong(key)>now) { tell(p,"Техника перезаряжается."); return; }
        double cost=kind==1 ? 60 : kind==2 ? 30 : 20;
        if (!CursedEnergy.get(p).tryTechnique(p,cost,cost,1)) {
            tell(p,"Недостаточно ПЭ, контроля или доступного выхода."); return;
        }
        ACTIVE.put(p.getUUID(),new Cast(kind));
        p.getPersistentData().putLong(key,now+(kind==1 ? 200 : kind==2 ? 100 : 80));
        sound(p,net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,.7f,kind==1 ? .6f : 1.3f);
        tell(p,kind==1 ? "Мнимая техника: Фиолетовый" : kind==2 ? "Обратная техника: Красный" : "Усиление техники: Синий");
    }
    private static void sound(ServerPlayer p,net.minecraft.sounds.SoundEvent sound,float volume,float pitch) {
        p.serverLevel().playSound(null,p.getX(),p.getY(),p.getZ(),sound,net.minecraft.sounds.SoundSource.PLAYERS,volume,pitch);
    }
    private static void dot(ServerPlayer p,Vec3 v,DustParticleOptions color) {
        p.serverLevel().sendParticles(color,v.x,v.y,v.z,1,0,0,0,0);
    }
    /** Ring in the plane perpendicular to the shot; phase rotates the energy ribbons. */
    private static void ring(ServerPlayer p,Vec3 center,Vec3 direction,double radius,double phase,DustParticleOptions color,int count) {
        Vec3 u=direction.cross(Math.abs(direction.y)>.9 ? new Vec3(1,0,0) : new Vec3(0,1,0)).normalize();
        Vec3 v=direction.cross(u).normalize();
        for(int i=0;i<count;i++) {
            double angle=phase+i*Math.PI*2/count;
            dot(p,center.add(u.scale(Math.cos(angle)*radius)).add(v.scale(Math.sin(angle)*radius)),color);
        }
    }
    private static void tell(ServerPlayer p,String text) { p.displayClientMessage(Component.literal(text),true); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { ACTIVE.remove(e.getEntity().getUUID()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { ACTIVE.remove(e.getEntity().getUUID()); }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Cast c=ACTIVE.get(p.getUUID());
        if(c==null) return;
        if(!p.isAlive() || !GeneticsSavedData.get(p.getServer()).visibleTechnique(p.getUUID()).equals("limitless")) {
            ACTIVE.remove(p.getUUID()); return;
        }
        var level=p.serverLevel();
        if(c.age<c.charge()) {
            Vec3 dir=p.getLookAngle().normalize();
            Vec3 hand=p.getEyePosition().add(dir.scale(1.5)).add(0,-.2,0);
            double progress=(c.age+1.0)/c.charge();
            if(c.kind==1) {
                Vec3 side=dir.cross(new Vec3(0,1,0)).normalize().scale(1-progress);
                ring(p,hand.add(side),dir,.18+progress*.3,c.age*.45,BLUE,12);
                ring(p,hand.subtract(side),dir,.18+progress*.3,-c.age*.45,RED,12);
            }
            ring(p,hand,dir,1.3-progress,c.age*.5,c.color(),20);
            ring(p,hand,dir,.15+progress*.3,-c.age*.7,WHITE,10);
            c.age++;
            return;
        }
        if(c.position==null) {
            c.direction=p.getLookAngle().normalize();
            c.position=p.getEyePosition().add(c.direction.scale(.5));
            sound(p,net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,.65f,c.kind==1 ? .7f : 1.5f);
        }
        // Swept half-block samples prevent tunnelling. Range is the center's travel distance.
        for(int step=0;step<3 && c.travelled<c.range();step++) {
            double distance=Math.min(.5,c.range()-c.travelled);
            c.position=c.position.add(c.direction.scale(distance));
            c.travelled+=distance;
            BlockPos center=BlockPos.containing(c.position);
            if(!level.hasChunkAt(center) || !level.getWorldBorder().isWithinBounds(center)) { ACTIVE.remove(p.getUUID()); return; }
            ring(p,c.position,c.direction,c.radius(),c.age*.6,c.color(),20);
            ring(p,c.position,c.direction,c.radius()*.55,-c.age*.7,WHITE,10);
            dot(p,c.position,c.color());
            for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AABB(c.position,c.position).inflate(c.radius()))) {
                if(!valid(p,target) || !touches(target.getBoundingBox(),c.position,c.radius())) continue;
                if(c.hit.add(target.getUUID())) {
                    target.hurt(level.damageSources().playerAttack(p),c.kind==1 ? 24 : c.kind==2 ? 10 : 4);
                    if(c.kind==2) {
                        target.setDeltaMovement(c.direction.scale(1.6).add(0,.35,0));
                        target.hurtMarked=true;
                    }
                }
            }
            int reach=2;
            for(BlockPos pos:BlockPos.betweenClosed(center.offset(-reach,-reach,-reach),center.offset(reach,reach,reach))) {
                // Block-center sphere keeps the Purple cross-section within diameter 3.
                if(pos.getCenter().distanceToSqr(c.position)>c.radius()*c.radius() || !level.hasChunkAt(pos)) continue;
                if(!c.checked.add(pos.immutable())) continue;
                var state=level.getBlockState(pos);
                if(state.isAir() || state.getDestroySpeed(level,pos)<0 || state.hasBlockEntity()
                    || !level.mayInteract(p,pos) || !p.mayBuild() || !level.getWorldBorder().isWithinBounds(pos)) continue;
                if(ForgeHooks.onBlockBreakEvent(level,p.gameMode.getGameModeForPlayer(),p,pos)<0) continue;
                level.destroyBlock(pos,c.kind!=1,p);
            }
        }
        if(c.kind==0) {
            // Blue draws groups toward the caster rather than toward a remote point.
            for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AABB(c.position,c.position).inflate(4))) {
                if(!valid(p,target) || target.distanceToSqr(p)>144) continue;
                Vec3 destination=p.position().add(p.getLookAngle().scale(1.8)).add(0,.5,0);
                Vec3 delta=destination.subtract(target.position());
                if(delta.lengthSqr()>.5) {
                    target.setDeltaMovement(target.getDeltaMovement().scale(.65).add(delta.normalize().scale(.35)));
                    target.hurtMarked=true;
                }
            }
            ring(p,c.position,c.direction,1.2+.3*Math.sin(c.age*.4),c.age*.5,BLUE,20);
        }
        c.age++;
        if(c.travelled>=c.range() && (c.kind!=0 || c.age>=c.charge()+40)) {
            for(int i=0;i<3;i++) ring(p,c.position,c.direction,c.radius()+i*.35,c.age*.3,c.color(),24);
            sound(p,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,.5f,c.kind==1 ? .7f : 1.5f);
            ACTIVE.remove(p.getUUID());
        }
    }
    private static boolean valid(ServerPlayer p,LivingEntity target) {
        return target!=p && target.isAlive() && !target.isSpectator() && !target.isAlliedTo(p)
            && (!(target instanceof ServerPlayer other) || (p.getServer().isPvpAllowed() && p.canHarmPlayer(other)));
    }
    private static boolean touches(AABB box,Vec3 center,double radius) {
        double x=Math.max(box.minX,Math.min(box.maxX,center.x));
        double y=Math.max(box.minY,Math.min(box.maxY,center.y));
        double z=Math.max(box.minZ,Math.min(box.maxZ,center.z));
        return center.distanceToSqr(new Vec3(x,y,z))<=radius*radius;
    }
}
