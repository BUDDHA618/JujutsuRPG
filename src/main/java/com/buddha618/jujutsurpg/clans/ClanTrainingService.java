package com.buddha618.jujutsurpg.clans;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.energy.CursedEnergy;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID)
public final class ClanTrainingService {
    private static final Map<MinecraftServer,Map<UUID,Session>> SESSIONS=new WeakHashMap<>();
    public record Task(String text,long sequence,long nonce,BlockPos target,ClanDefinition.DrillAction action) {}
    private static final class Session {
        final String clan,dimension,school;
        final BlockPos origin;
        final TrainingChallenge challenge;
        final boolean exam;
        BlockPos target;
        long sprintAt=-1000;
        Session(ServerPlayer p,String clan,String school,long sequence,boolean exam,int count) {
            this.clan=clan;this.school=school;this.exam=exam;
            dimension=p.level().dimension().location().toString();origin=p.blockPosition();
            challenge=new TrainingChallenge(sequence,ThreadLocalRandom.current().nextLong(),now(p),count);
        }
    }
    private static Map<UUID,Session> sessions(ServerPlayer p){return SESSIONS.computeIfAbsent(p.getServer(),s->new HashMap<>());}
    private static long now(ServerPlayer p){return p.getServer().overworld().getGameTime();}
    public static void action(ServerPlayer p,ClanNetwork.Action request) {
        var data=GeneticsSavedData.get(p.getServer());
        if(!data.hasProfile(p.getUUID()))return;
        switch(request.action()) {
            case "train" -> start(p,false);
            case "exam" -> start(p,true);
            case "cancel" -> {sessions(p).remove(p.getUUID());ClanNetwork.send(p,"Упражнение отменено, награда не выдана.");}
            case "step" -> step(p,request.sequence(),request.nonce());
            case "school" -> {
                var clan=ClanRegistry.find(data.getProfile(p.getUUID()).clan().id()).orElseThrow();
                boolean ok=!sessions(p).containsKey(p.getUUID())&&data.progression(p.getUUID()).selectSchool(clan,request.argument(),data.visibleTechnique(p.getUUID()));
                ClanNetwork.send(p,ok?"Учебная специализация выбрана.":"Школа закрыта или уже идёт упражнение.");
            }
            default -> ClanNetwork.send(p,"Неизвестное действие.");
        }
    }
    public static boolean start(ServerPlayer p,boolean exam) {
        var data=GeneticsSavedData.get(p.getServer());
        if(!data.hasProfile(p.getUUID())||!p.isAlive()||p.isSpectator()||sessions(p).containsKey(p.getUUID()))return false;
        var progress=data.progression(p.getUUID());
        if(exam && progress.exam()==0){ClanNetwork.send(p,"Для экзамена сначала заполните XP перед порогом уровня.");return false;}
        if(!exam&&progress.level()==100){ClanNetwork.send(p,"Клановое мастерство уже достигло100.");return false;}
        var clan=ClanRegistry.find(data.getProfile(p.getUUID()).clan().id()).orElseThrow();
        var session=new Session(p,clan.clan().id(),progress.activeSchool(clan),progress.begin(),exam,(exam?8:4)+progress.level()/20);
        session.target=findTarget(p,session,null);
        if(session.target==null){ClanNetwork.send(p,"Нужно открытое ровное место примерно15×15 блоков. Награда не выдана.");return false;}
        sessions(p).put(p.getUUID(),session);ClanNetwork.send(p,"Упражнение начато. Вернитесь в мир и подтверждайте шаги клавишей V.");return true;
    }
    private static BlockPos findTarget(ServerPlayer p,Session s,BlockPos previous) {
        var random=ThreadLocalRandom.current();var level=p.serverLevel();
        for(int attempt=0;attempt<80;attempt++) {
            int x=s.origin.getX()+random.nextInt(-6,7),z=s.origin.getZ()+random.nextInt(-6,7);
            for(int dy=2;dy>=-3;dy--) {
                var pos=new BlockPos(x,s.origin.getY()+dy,z);
                if(pos.distSqr(previous==null?s.origin:previous)<9||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)
                    ||pos.getY()<level.getMinBuildHeight()+1||pos.getY()+2>=level.getMaxBuildHeight())continue;
                if(level.getBlockState(pos.below()).isCollisionShapeFullBlock(level,pos.below())
                    && level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()
                    && level.getFluidState(pos).isEmpty()&&level.getFluidState(pos.above()).isEmpty())return pos;
            }
        }
        return null;
    }
    private static ClanDefinition.DrillAction action(Session s) {
        var definition=ClanRegistry.find(s.clan).orElseThrow();
        int school=0;for(int i=0;i<definition.schools().size();i++)if(definition.schools().get(i).id().equals(s.school))school=i;
        // Secondary schools add observation/stance checks, not automatic stat bonuses.
        if(school>0 && s.challenge.step()%2==0)return school==1?ClanDefinition.DrillAction.FOCUS:ClanDefinition.DrillAction.GROUNDED;
        return definition.actions().get((s.challenge.step()+school)%definition.actions().size());
    }
    private static String instruction(ClanDefinition.DrillAction action) {
        return switch(action) {
            case FOCUS -> "посмотрите точно на светящуюся точку";
            case SNEAK -> "присядьте";
            case UNARMED -> "освободите основную руку";
            case WEAPON -> "возьмите меч или топор";
            case SPRINT -> "подбегите спринтом и подтвердите в течение3сек";
            case LOOK_UP -> "посмотрите вверх";
            case EMPTY_OFFHAND -> "освободите вторую руку";
            case GROUNDED -> "встаньте на землю без приседания";
        };
    }
    private static boolean performed(ServerPlayer p,Session s) {
        return switch(action(s)) {
            case FOCUS -> p.getLookAngle().dot(Vec3.atBottomCenterOf(s.target).add(0,1,0).subtract(p.getEyePosition()).normalize())>=.94;
            case SNEAK -> p.isCrouching();
            case UNARMED -> p.getMainHandItem().isEmpty();
            case WEAPON -> p.getMainHandItem().getItem() instanceof SwordItem || p.getMainHandItem().getItem() instanceof AxeItem;
            case SPRINT -> p.isSprinting() || now(p)-s.sprintAt<=60;
            case LOOK_UP -> p.getLookAngle().y>.8;
            case EMPTY_OFFHAND -> p.getOffhandItem().isEmpty();
            case GROUNDED -> p.onGround()&&!p.isCrouching();
        };
    }
    public static boolean step(ServerPlayer p,long sequence,long nonce) {
        var s=sessions(p).get(p.getUUID());
        if(s==null||!valid(p,s)||!s.challenge.accepts(sequence,nonce,now(p)))return false;
        if(p.position().distanceToSqr(Vec3.atBottomCenterOf(s.target))>2.25||!performed(p,s)) {
            ClanNetwork.send(p,"Подойдите к нужной точке и выполните условие. Между шагами минимум3сек.");return false;
        }
        var data=GeneticsSavedData.get(p.getServer());
        // A physical restriction uses a non-CE version of the same movement/stance exercise.
        if(!data.fullPhysical(p.getUUID())&&!CursedEnergy.get(p).tryMaintained(p,s.exam?2:1,s.exam?2:1,0)) {
            ClanNetwork.send(p,"Недостаточно CE или доступного выхода. Регенерация продолжает работать.");return false;
        }
        s.challenge.advance(sequence,nonce,now(p),ThreadLocalRandom.current().nextLong());
        if(s.challenge.complete()) {
            sessions(p).remove(p.getUUID());
            boolean awarded=data.progression(p.getUUID()).reward(sequence,s.exam);
            p.sendSystemMessage(Component.literal(awarded?(s.exam?"Клановый экзамен пройден.":"Упражнение завершено: клановый XP и репутация получены."):"Награда уже учтена."));
            ClanNetwork.send(p,"Завершено. Прогресс сохранён.");return awarded;
        }
        s.target=findTarget(p,s,s.target);
        if(s.target==null){sessions(p).remove(p.getUUID());ClanNetwork.send(p,"Маршрут недоступен. Попробуйте на более ровной площадке; награда не выдана.");return false;}
        ClanNetwork.send(p,"");return true;
    }
    private static boolean valid(ServerPlayer p,Session s) {
        var data=GeneticsSavedData.get(p.getServer());
        return p.isAlive()&&!p.isSpectator()&&!s.challenge.expired(now(p))
            &&p.level().dimension().location().toString().equals(s.dimension)
            &&data.hasProfile(p.getUUID())&&data.getProfile(p.getUUID()).clan().id().equals(s.clan)
            &&p.blockPosition().distSqr(s.origin)<1600;
    }
    public static Task describe(ServerPlayer p) {
        var s=sessions(p).get(p.getUUID());if(s==null)return new Task("Нет активного упражнения.",0,0,BlockPos.ZERO,ClanDefinition.DrillAction.GROUNDED);
        return new Task((s.exam?"Экзамен":"Упражнение")+" "+(s.challenge.step()+1)+"/"+s.challenge.count()+": "
            +s.target.getX()+", "+s.target.getY()+", "+s.target.getZ()+" — "+instruction(action(s))+". Подтверждение: V (переназначается).",
            s.challenge.sequence(),s.challenge.nonce(),s.target,action(s));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        var s=sessions(p).get(p.getUUID());if(s==null)return;
        if(!valid(p,s)){sessions(p).remove(p.getUUID());ClanNetwork.send(p,"Упражнение прервано без награды.");return;}
        if(p.isSprinting())s.sprintAt=now(p);
        if(p.tickCount%10==0)p.serverLevel().sendParticles(p,ParticleTypes.END_ROD,false,s.target.getX()+.5,s.target.getY()+1,s.target.getZ()+.5,3,.05,.05,.05,0);
        if(p.tickCount%40==0)p.displayClientMessage(Component.literal(describe(p).text()),true);
    }
    private static void cancel(ServerPlayer p){sessions(p).remove(p.getUUID());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void damage(LivingHurtEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){SESSIONS.remove(e.getServer());}
}
