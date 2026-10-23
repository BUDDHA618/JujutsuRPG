package com.buddha618.jujutsurpg.curses;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.*;
import net.minecraft.sounds.*;

/** Three hostile prototypes with telegraphed server-owned special attacks. */
public final class CurseSpirit extends Monster {
    private int specialCooldown=60, charge;
    public CurseSpirit(EntityType<? extends Monster> type,Level level) { super(type,level); xpReward=20; }
    public int variant() { return getType()==CurseEntities.JOGO.get()?0:getType()==CurseEntities.HANAMI.get()?1:2; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.0,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,12));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean fireImmune() { return variant()==0 || super.fireImmune(); }
    @Override public void aiStep() {
        super.aiStep();
        if(!(level() instanceof ServerLevel server) || !isAlive()) return;
        var target=getTarget();
        if(target==null || !target.isAlive() || (target instanceof Player p && (p.isCreative()||p.isSpectator()))) { charge=0;return; }
        if(specialCooldown>0) {specialCooldown--;return;}
        if(distanceToSqr(target)>144 || !hasLineOfSight(target)) {charge=0;return;}
        charge++;
        getLookControl().setLookAt(target,30,30);
        server.sendParticles(variant()==0?ParticleTypes.FLAME:variant()==1?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.SOUL,
            getX(),getY()+getBbHeight()*.7,getZ(),5,.4,.3,.4,.01);
        if(charge<20) return;
        charge=0;specialCooldown=variant()==0?100:variant()==1?140:80;
        playSound(SoundEvents.EVOKER_CAST_SPELL,.8f,variant()==0?.7f:1.2f);
        if(variant()==0) {
            // A visible line of flame before the hit; no unbounded terrain grief or lava placement.
            var start=getEyePosition();var delta=target.getEyePosition().subtract(start);
            for(int i=0;i<20;i++) {var point=start.add(delta.scale(i/20.0));server.sendParticles(ParticleTypes.FLAME,point.x,point.y,point.z,2,.08,.08,.08,.01);}
            if(target.hurt(damageSources().mobAttack(this),7)) target.setSecondsOnFire(3);
        } else if(variant()==1) {
            server.sendParticles(ParticleTypes.COMPOSTER,target.getX(),target.getY()+.2,target.getZ(),35,.7,.2,.7,.1);
            target.hurt(damageSources().mobAttack(this),5);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1));
        } else {
            var direction=target.position().subtract(position()).normalize();
            setDeltaMovement(direction.scale(.9).add(0,.25,0));hurtMarked=true;
            if(distanceToSqr(target)<16) {target.hurt(damageSources().mobAttack(this),8);target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,0));}
        }
    }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) {return SoundEvents.HUSK_HURT;}
    @Override protected SoundEvent getDeathSound() {return SoundEvents.HUSK_DEATH;}
}
