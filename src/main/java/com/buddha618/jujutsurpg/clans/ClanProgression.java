package com.buddha618.jujutsurpg.clans;

import net.minecraft.nbt.CompoundTag;
import java.util.List;

/** Independent clan ledger. No writes to CE, innate technique or sorcerer rank. */
public final class ClanProgression {
    public static final List<Integer> GATES = List.of(20,40,60,80,90,100);
    private final CompoundTag tag;
    private final Runnable dirty;
    public ClanProgression(CompoundTag tag, Runnable dirty) {
        this.tag = tag; this.dirty = dirty;
        if (tag.getInt("Version") > 1) throw new IllegalStateException("Future clan progression version");
        if (!tag.contains("Version")) {
            tag.putInt("Version",1); tag.putInt("Level",1); tag.put("Exams",new CompoundTag());
            tag.put("Schools",new CompoundTag()); dirty.run();
        }
    }
    public int level() { return Math.max(1,Math.min(100,tag.getInt("Level"))); }
    public int requirement() { return 100 + 10 * (level()-1); }
    public int xp() { return Math.max(0,tag.getInt("XP")); }
    public int reputation() { return Math.max(0,tag.getInt("Reputation")); }
    public int completions() { return Math.max(0,tag.getInt("Completions")); }
    public int exam() { return level() < 100 && GATES.contains(level()+1) && xp() >= requirement() ? level()+1 : 0; }
    public long begin() {
        long current = tag.getLong("Sequence");
        if (current < 0 || current == Long.MAX_VALUE) throw new IllegalStateException("Invalid training sequence");
        tag.putLong("Sequence",current+1); dirty.run(); return current+1;
    }
    public boolean reward(long sequence, boolean examination) {
        if (sequence <= 0 || sequence != tag.getLong("Sequence") || sequence <= tag.getLong("RewardedSequence")) return false;
        if (examination && exam() == 0) return false;
        tag.putLong("RewardedSequence",sequence);
        if (examination) {
            int next = exam();
            tag.getCompound("Exams").putBoolean(Integer.toString(next),true);
            tag.putInt("XP",Math.max(0,xp()-requirement())); tag.putInt("Level",next);
            tag.putInt("Reputation",(int)Math.min(1_000_000L,(long)reputation()+5));
        } else {
            tag.putInt("XP",(int)Math.min(1_000_000L,(long)xp()+40+level()/5));
            tag.putInt("Completions",(int)Math.min(Integer.MAX_VALUE,(long)completions()+1));
            tag.putInt("Reputation",(int)Math.min(1_000_000L,(long)reputation()+1));
            while (level() < 100 && xp() >= requirement() && !GATES.contains(level()+1)) {
                tag.putInt("XP",xp()-requirement()); tag.putInt("Level",level()+1);
            }
            if (level() == 100) tag.putInt("XP",0);
            else if (GATES.contains(level()+1)) tag.putInt("XP",Math.min(xp(),requirement()));
        }
        dirty.run(); return true;
    }
    public boolean available(ClanDefinition.School school, String visibleTechnique) {
        return school.implemented() && level() >= school.level() && reputation() >= school.reputation()
            && (school.technique().isEmpty() || school.technique().equals(visibleTechnique));
    }
    public boolean selectSchool(ClanDefinition clan, String id, String visibleTechnique) {
        var school = clan.schools().stream().filter(s -> s.id().equals(id)).findFirst();
        if (school.isEmpty() || !available(school.get(),visibleTechnique)) return false;
        tag.putString("ActiveSchool",id); tag.getCompound("Schools").putBoolean(id,true); dirty.run(); return true;
    }
    public String activeSchool(ClanDefinition clan) {
        String id = tag.getString("ActiveSchool");
        return clan.schools().stream().anyMatch(s -> s.id().equals(id)) ? id : clan.schools().get(0).id();
    }
    public CompoundTag snapshot() { return tag.copy(); }
}
