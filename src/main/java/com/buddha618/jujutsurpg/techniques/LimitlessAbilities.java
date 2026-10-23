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
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(.05f,.35f,1), 1.6f);
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(.7f,.05f,1), 2);
    private static final class Cast {
        Vec3 position;
        final Vec3 direction;
        final boolean purple;
        int age;
        final Set<UUID> hit = new HashSet<>();
        Cast(Vec3 position, Vec3 direction, boolean purple) { this.position=position; this.direction=direction; this.purple=purple; }
    }
    public static void activate(ServerPlayer p, boolean purple) {
        if (!p.isAlive() || p.isSpectator()) return;
        if (!GeneticsSavedData.get(p.getServer()).visibleTechnique(p.getUUID()).equals("limitless")) {
            tell(p, "Нужна пробуждённая Безграничность. K → DEV для тестирования."); return;
        }
        long now=p.serverLevel().getGameTime();
        var tags=p.getPersistentData();
        String key=purple ? "jjk_purple_ready" : "jjk_blue_ready";
        if (ACTIVE.containsKey(p.getUUID()) || tags.getLong(key)>now) { tell(p,"Техника перезаряжается."); return; }
        if (!CursedEnergy.get(p).tryTechnique(p, purple ? 60 : 20, purple ? 60 : 20, 1)) {
            tell(p,"Недостаточно ПЭ, контроля или доступного выхода."); return;
        }
        Vec3 direction=p.getLookAngle().normalize();
        Vec3 start=p.getEyePosition();
        Vec3 target=purple ? start.add(direction.scale(2)) : p.serverLevel().clip(new ClipContext(start,
            start.add(direction.scale(18)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
        ACTIVE.put(p.getUUID(),new Cast(target,direction,purple));
        tags.putLong(key,now+(purple ? 200 : 80));
        tell(p,purple ? "Фиолетовый!" : "Синий!");
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
        // Blue persists for 3 seconds; Purple sweeps 48 blocks in 24 ticks.
        int steps=c.purple ? 4 : 1;
        for(int step=0;step<steps;step++) {
            if(c.purple) c.position=c.position.add(c.direction.scale(.5));
            BlockPos center=BlockPos.containing(c.position);
            if(!level.hasChunkAt(center) || !level.getWorldBorder().isWithinBounds(center)) { ACTIVE.remove(p.getUUID()); return; }
            double radius=c.purple ? 1.6 : 5;
            level.sendParticles(c.purple ? PURPLE : BLUE,c.position.x,c.position.y,c.position.z,
                c.purple ? 8 : 16,c.purple ? .7 : .25,c.purple ? .7 : .25,c.purple ? .7 : .25,0);
            for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AABB(c.position,c.position).inflate(radius))) {
                if(target==p || !target.isAlive() || target.isSpectator() || target.isAlliedTo(p)) continue;
                if(target instanceof ServerPlayer other && (!p.getServer().isPvpAllowed() || !p.canHarmPlayer(other))) continue;
                if(target.position().distanceToSqr(c.position)>radius*radius) continue;
                if(c.purple) {
                    if(c.hit.add(target.getUUID())) target.hurt(level.damageSources().playerAttack(p),24);
                } else {
                    Vec3 pull=c.position.subtract(target.position()).normalize().scale(.18);
                    target.setDeltaMovement(target.getDeltaMovement().scale(.8).add(pull));
                    target.hurtMarked=true;
                    if(c.age%10==0 && target.distanceToSqr(c.position)<4)
                        target.hurt(level.damageSources().playerAttack(p),3);
                }
            }
            if(c.purple || c.age%10==0) {
                int reach=c.purple ? 2 : 1;
                for(BlockPos pos:BlockPos.betweenClosed(center.offset(-reach,-reach,-reach),center.offset(reach,reach,reach))) {
                    if(pos.getCenter().distanceToSqr(c.position)>(c.purple ? 2.56 : 2.25)) continue;
                    if (!level.hasChunkAt(pos)) continue;
                    var state=level.getBlockState(pos);
                    if(state.isAir() || state.getDestroySpeed(level,pos)<0 || state.hasBlockEntity()
                        || !level.hasChunkAt(pos) || !level.mayInteract(p,pos) || !p.mayBuild()) continue;
                    // Respect Forge break cancellation, spawn protection, border and unbreakable blocks.
                    if(!level.getWorldBorder().isWithinBounds(pos)) continue;
                    if(ForgeHooks.onBlockBreakEvent(level,p.gameMode.getGameModeForPlayer(),p,pos)<0) continue;
                    level.destroyBlock(pos,!c.purple,p);
                }
            }
        }
        c.age++;
        if(c.age>=(c.purple ? 24 : 60)) ACTIVE.remove(p.getUUID());
    }
}
