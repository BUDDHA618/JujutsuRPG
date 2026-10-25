package com.buddha618.jujutsurpg;

import com.buddha618.jujutsurpg.clans.*;
import com.buddha618.jujutsurpg.genetics.*;
import net.minecraft.nbt.*;
import java.util.*;
import java.io.*;

public final class ClanCheck {
    private static int assertions;
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static GeneticsSavedData reload(GeneticsSavedData data)throws Exception {
        var out=new ByteArrayOutputStream();NbtIo.writeCompressed(data.save(new CompoundTag()),out);
        return GeneticsSavedData.load(NbtIo.readCompressed(new ByteArrayInputStream(out.toByteArray())));
    }
    public static void main(String[] args)throws Exception {
        check(ClanRegistry.all().size()==8,"Eight selectable origins");
        var noRandom=new Random(){@Override public int nextInt(int bound){throw new AssertionError("Unexpected reroll");}};
        var highRoll=new Random(){@Override public int nextInt(int bound){return bound-1;}};
        var data=new GeneticsSavedData();UUID untouched=UUID.randomUUID();
        check(!data.hasProfile(untouched),"Reading created a profile");
        try{data.getProfile(untouched);throw new AssertionError("Absent profile silently generated");}catch(IllegalStateException expected){assertions++;}
        check(!data.selectOrigin(untouched,"nonexistent",noRandom),"Invalid origin accepted");
        check(data.save(new CompoundTag()).getCompound("Players").isEmpty(),"Invalid input created permanent data");
        for(var definition:ClanRegistry.all()) {
            UUID id=UUID.randomUUID();
            check(data.selectOrigin(id,definition.clan().id(),new Random(11)),"Selection failed");
            var profile=data.getProfile(id);
            check(profile.clan().equals(definition.clan()),"Player choice ignored");
            check(data.visibleTechnique(id).equals("hidden"),"Secret leaked");
            check(!data.hasSixEyes(id),"Choice granted Six Eyes");
            check(!data.selectOrigin(id,"gojo",noRandom),"Duplicate choice rerolled");
            data=reload(data);
            check(data.getProfile(id).equals(profile),"Reload changed genetics");
            check(!data.selectOrigin(id,"zenin",noRandom),"Reload reopened choice");
            check(GeneticsSavedData.rollCompatible(definition,true,noRandom)==InnateTechnique.NONE,"Physical restriction got CE technique");
            check(definition.schools().size()>=3,"Missing specializations");
            check(definition.schools().stream().map(s->s.id()).distinct().count()==definition.schools().size(),"Duplicate school ID");
        }
        for(String clan:List.of("gojo","zenin","kamo","inumaki")) {
            UUID id=UUID.randomUUID();data.selectOrigin(id,clan,highRoll);
            check(data.getProfile(id).technique()==InnateTechnique.NONE,"Family technique guaranteed");
        }
        var legacyId=UUID.randomUUID();var legacy=new CompoundTag();legacy.putString("Clan","kamo");legacy.putString("Technique","blood_manipulation");legacy.putBoolean("Awakened",true);
        var profiles=new CompoundTag();profiles.put(legacyId.toString(),legacy);var root=new CompoundTag();root.put("Players",profiles);
        var old=GeneticsSavedData.load(root);check(old.getProfile(legacyId).clan().equals(Clan.KAMO),"Legacy origin changed");
        check(!old.selectOrigin(legacyId,"gojo",noRandom),"Legacy profile got free choice");
        check(old.migrateLegacyOrigin(legacyId,"fujiwara"),"Explicit migration failed");
        check(old.getProfile(legacyId).technique()==InnateTechnique.BLOOD_MANIPULATION,"Migration rerolled technique");
        check(old.visibleTechnique(legacyId).equals("blood_manipulation"),"Migration lost awakening");
        check(!old.migrateLegacyOrigin(legacyId,"abe"),"Second migration accepted");
        old=reload(old);check(!old.migrateLegacyOrigin(legacyId,"abe"),"Reload reopened migration");
        var snapshot=old.save(new CompoundTag()).getCompound("Players").getCompound(legacyId.toString()).getCompound("BeforeOriginMigration");
        check(snapshot.getString("Clan").equals("kamo"),"Missing migration snapshot");

        UUID progressionId=UUID.randomUUID();data.selectOrigin(progressionId,"gojo",new Random(1));
        var original=data.getProfile(progressionId);
        var progress=data.progression(progressionId);
        long first=progress.begin();check(progress.reward(first,false),"No training reward");
        int xp=progress.xp();check(!progress.reward(first,false)&&progress.xp()==xp,"Duplicate XP");
        long abandoned=progress.begin();long current=progress.begin();
        check(!progress.reward(abandoned,false),"Stale training rewarded");check(progress.reward(current,false),"Current training rejected");
        check(data.getProfile(progressionId).equals(original),"XP changed genetics");
        check(data.visibleTechnique(progressionId).equals("hidden"),"XP awakened technique");
        int gates=0;
        for(int iteration=0;progress.level()<100&&iteration<10000;iteration++) {
            int gate=progress.exam();long sequence=progress.begin();
            if(gate>0){int before=progress.level();check(progress.reward(sequence,true),"Exam reward failed");check(progress.level()==before+1,"Wrong exam level");gates++;}
            else check(progress.reward(sequence,false),"Training progress failed");
            check(!progress.reward(sequence,false),"Replay after exam/training");
        }
        check(progress.level()==100 && gates==6,"All six milestones must require exams");
        var clan=ClanRegistry.find("gojo").orElseThrow();
        check(progress.selectSchool(clan,"barrier","hidden"),"Earned school unavailable");
        check(!progress.selectSchool(clan,"space","limitless"),"Unimplemented innate school granted");
        check(!progress.selectSchool(clan,"weapon","hidden"),"Other clan school granted");
        var beforeSave=progress.snapshot();data=reload(data);
        check(data.progression(progressionId).snapshot().equals(beforeSave),"Progress/reputation/exams lost on reload");
        check(data.advanceSeason(1),"Season advance failed");
        check(data.progression(progressionId).snapshot().equals(beforeSave),"Season wiped clan progress");
        check(data.getProfile(progressionId).equals(original),"Season rerolled origin");

        var challenge=new TrainingChallenge(7,100,1000,2);
        check(!challenge.accepts(7,100,1059),"AFK minimum stage time bypassed");
        check(!challenge.accepts(8,100,1060),"Other session accepted");
        check(!challenge.accepts(7,101,1060),"Forged nonce accepted");
        check(challenge.advance(7,100,1060,200),"Valid stage failed");
        check(!challenge.advance(7,100,1120,300),"Old stage replayed");
        check(challenge.advance(7,200,1120,300)&&challenge.complete(),"Final stage failed");
        check(!challenge.advance(7,300,1180,400),"Completed challenge replayed");
        check(!new TrainingChallenge(1,1,0,4).accepts(1,1,3601),"Expired task accepted");
        System.out.println("Clan checks passed: "+assertions+" assertions (choice, legacy migration, eight origins, progression, exams, replay protection)");
    }
}
