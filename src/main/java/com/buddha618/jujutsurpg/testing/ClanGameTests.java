package com.buddha618.jujutsurpg.testing;

import com.buddha618.jujutsurpg.clans.*;
import com.buddha618.jujutsurpg.genetics.*;
import com.buddha618.jujutsurpg.energy.CursedEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("jujutsurpg")
@PrefixGameTestTemplate(false)
public final class ClanGameTests {
    @GameTest(template="clan_training_arena",timeoutTicks=1000)
    public static void allEightOriginsTrainAndPassExam(GameTestHelper h) {
        var players=new ArrayList<ServerPlayer>();
        for(var clan:ClanRegistry.all()) {
            var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"clan-test"));
            p.moveTo(h.absolutePos(new BlockPos(16,1,16)),0,0);p.setOnGround(true);
            h.assertTrue(ClanSelectionService.pending(p),"New player skipped selection");
            var attack=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().generic(),2);
            ClanSelectionService.attack(attack);
            h.assertTrue(attack.isCanceled(),"Pending character was not protected");
            var interaction=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);
            ClanSelectionService.interact(interaction);
            h.assertTrue(interaction.isCanceled(),"Pending character could use items");
            h.assertFalse(CursedEnergy.get(p).tryMaintained(p,1,1,0),"Pending player spent CE");
            h.assertFalse(ClanTrainingService.start(p,false),"Pending player started training");
            h.assertFalse(GeneticsSavedData.get(p.getServer()).hasProfile(p.getUUID()),"Rejected action generated genetics");
            ClanSelectionService.select(p,clan.clan().id());
            h.assertFalse(ClanSelectionService.pending(p),"Confirmation did not create profile");
            var data=GeneticsSavedData.get(p.getServer());var original=data.getProfile(p.getUUID());
            ClanSelectionService.select(p,"gojo");
            h.assertTrue(data.getProfile(p.getUUID()).equals(original),"Repeated confirmation rerolled character");
            h.assertTrue(data.visibleTechnique(p.getUUID()).equals("hidden"),"Hidden technique leaked");
            h.assertTrue(ClanTrainingService.start(p,false),"Could not start "+clan.clan().id());
            var task=ClanTrainingService.describe(p);
            h.assertFalse(ClanTrainingService.step(p,task.sequence(),task.nonce()),"Immediate completion accepted");
            players.add(p);
        }
        stages(h,players,0,false);
    }
    private static void stages(GameTestHelper h,List<ServerPlayer> players,int step,boolean exam) {
        h.runAfterDelay(61,()->{
            for(var p:players) {
                var task=ClanTrainingService.describe(p);
                double before=CursedEnergy.get(p).current(p);
                h.assertFalse(ClanTrainingService.step(p,task.sequence(),task.nonce()+1),"Forged nonce accepted");
                h.assertTrue(CursedEnergy.get(p).current(p)==before,"Invalid nonce consumed CE");
                p.moveTo(task.target(),0,0);
                p.setShiftKeyDown(false);p.setPose(Pose.STANDING);p.setSprinting(false);p.setOnGround(true);
                p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                switch(task.action()) {
                    case SNEAK -> {p.setShiftKeyDown(true);p.setPose(Pose.CROUCHING);}
                    case WEAPON -> p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_SWORD));
                    case SPRINT -> p.setSprinting(true);
                    case LOOK_UP -> p.setXRot(-90);
                    case FOCUS -> {
                        var delta=Vec3.atBottomCenterOf(task.target()).add(0,1,0).subtract(p.getEyePosition());
                        p.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
                        p.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
                    }
                    default -> {}
                }
                h.assertTrue(ClanTrainingService.step(p,task.sequence(),task.nonce()),"Valid stage failed: "+task.action()+" "+p.getUUID());
                h.assertFalse(ClanTrainingService.step(p,task.sequence(),task.nonce()),"Same stage accepted twice");
            }
            int count=exam?8:4;
            if(step+1<count){stages(h,players,step+1,exam);return;}
            for(var p:players) {
                var data=GeneticsSavedData.get(p.getServer());var progress=data.progression(p.getUUID());
                if(!exam) {
                    h.assertTrue(progress.completions()==1&&progress.xp()==40&&progress.reputation()==1,"Wrong exercise reward");
                    // Seed earned XP through the same pure reward ledger; do not wait for days in a test.
                    while(progress.exam()==0)progress.reward(progress.begin(),false);
                    h.assertTrue(progress.level()==19,"XP crossed level20 without exam");
                    h.assertTrue(ClanTrainingService.start(p,true),"Exam failed to start");
                } else {
                    h.assertTrue(progress.level()==20&&progress.snapshot().getCompound("Exams").getBoolean("20"),"Exam not recorded");
                    var restored=GeneticsSavedData.load(data.save(new CompoundTag()));
                    h.assertTrue(restored.progression(p.getUUID()).snapshot().equals(progress.snapshot()),"Exam/reputation not persistent");
                    var energy=CursedEnergy.get(p).save(new CompoundTag()).getCompound("Players").getCompound(p.getUUID().toString());
                    h.assertTrue(energy.getDouble("Capacity")==100&&energy.getDouble("Output")==100,"Clan XP altered CE stats");
                    h.assertTrue(ClanTrainingService.start(p,false),"Post-exam practice failed");
                    var task=ClanTrainingService.describe(p);
                    ClanTrainingService.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
                    h.assertFalse(ClanTrainingService.step(p,task.sequence(),task.nonce()),"Disconnected session awarded XP");
                    h.assertFalse(data.selectOrigin(p.getUUID(),"abe",new Random()),"Logout reopened choice");
                    h.assertTrue(ClanTrainingService.start(p,false),"Restart after logout failed");
                    ClanTrainingService.dimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,p.level().dimension(),net.minecraft.world.level.Level.NETHER));
                    h.assertTrue(ClanTrainingService.describe(p).sequence()==0,"Dimension change kept training");
                    h.assertTrue(ClanTrainingService.start(p,false),"Restart after dimension change failed");
                    ClanTrainingService.respawn(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(p,false));
                    h.assertTrue(ClanTrainingService.describe(p).sequence()==0,"Respawn kept training");
                    h.assertTrue(ClanTrainingService.start(p,false),"Restart after respawn failed");
                    ClanTrainingService.damage(new net.minecraftforge.event.entity.living.LivingHurtEvent(p,p.damageSources().generic(),1));
                    h.assertTrue(ClanTrainingService.describe(p).sequence()==0,"Damage kept training");
                    h.assertTrue(progress.level()==20,"Canceled training changed level");
                }
            }
            if(!exam)stages(h,players,0,true);else h.succeed();
        });
    }
}
