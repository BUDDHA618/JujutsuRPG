package com.buddha618.jujutsurpg.curses;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;

/** Persistent campus defenders; never target players. */
public final class StudentMage extends PathfinderMob {
    private int spellCooldown;
    public StudentMage(EntityType<? extends PathfinderMob> type,Level level) {super(type,level);setPersistenceRequired();}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,true));
        goalSelector.addGoal(4,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.65));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,net.minecraft.world.entity.player.Player.class,10));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,CurseSpirit.class,true));
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(hasRestriction()) {var home=getRestrictCenter();tag.putInt("CampusX",home.getX());tag.putInt("CampusY",home.getY());tag.putInt("CampusZ",home.getZ());}
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("CampusX"))restrictTo(new net.minecraft.core.BlockPos(tag.getInt("CampusX"),tag.getInt("CampusY"),tag.getInt("CampusZ")),36);
    }
    @Override public void aiStep() {
        super.aiStep();
        if(!(level() instanceof ServerLevel server))return;
        if(!hasRestriction())restrictTo(blockPosition(),36);
        if(spellCooldown>0)spellCooldown--;
        var target=getTarget();
        if(target==null || !target.isAlive())return;
        if(!isWithinRestriction(target.blockPosition())) {setTarget(null);return;}
        if(spellCooldown==0 && distanceToSqr(target)<100 && hasLineOfSight(target)) {
            spellCooldown=35;
            var start=getEyePosition();var delta=target.getEyePosition().subtract(start);
            for(int i=0;i<16;i++) {var p=start.add(delta.scale(i/16.0));server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.x,p.y,p.z,1,0,0,0,0);}
            target.hurt(damageSources().mobAttack(this),6);
        }
    }
    @Override public net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand hand) {
        if(!level().isClientSide)p.displayClientMessage(Component.literal("Студент техникума: Тренируй контроль ПЭ. Проклятиям здесь не место."),false);
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }
}
