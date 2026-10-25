package com.buddha618.jujutsurpg;

import com.buddha618.jujutsurpg.energy.EnergyRules;
import com.buddha618.jujutsurpg.genetics.*;
import net.minecraft.nbt.*;
import java.io.*;
import java.util.*;

public final class FoundationCheck {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-8, message + ": " + actual);
    }
    private static CompoundTag roundTrip(CompoundTag tag) throws IOException {
        var out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, out);
        return NbtIo.readCompressed(new ByteArrayInputStream(out.toByteArray()));
    }
    public static void main(String[] args) throws Exception {
        UUID oldId = UUID.randomUUID();
        var oldProfile = new CompoundTag();
        oldProfile.putString("Clan", "gojo");
        oldProfile.putString("Technique", "none");
        oldProfile.putString("ExtensionField", "preserve");
        var players = new CompoundTag(); players.put(oldId.toString(), oldProfile);
        var root = new CompoundTag(); root.put("Players", players);
        var genetics = GeneticsSavedData.load(roundTrip(root));
        for (int i = 0; i < 100; i++) {
            check(genetics.getProfile(oldId).technique() == InnateTechnique.NONE, "Legacy NONE rerolled");
            check(genetics.getProfile(oldId).clan() == Clan.GOJO, "Legacy clan changed");
            check(genetics.visibleTechnique(oldId).equals("hidden"), "Secret leaked before awakening");
        }
        var saved = roundTrip(genetics.save(new CompoundTag()));
        check(saved.getCompound("Players").getCompound(oldId.toString()).getString("ExtensionField").equals("preserve"), "Extension lost");
        check(saved.getCompound("Players").getCompound(oldId.toString()).getCompound("Potentials").size() == 5, "Five independent potentials");
        genetics = GeneticsSavedData.load(saved);
        genetics.completeAwakening(oldId);
        check(genetics.visibleTechnique(oldId).equals("none"), "Awakening must reveal NONE honestly");
        UUID other = UUID.randomUUID();
        genetics.developerSet(other, Clan.GOJO, InnateTechnique.LIMITLESS, false);
        check(genetics.enrollSixEyes(oldId), "No-Limitless Gojo must be eligible");
        check(!genetics.enrollSixEyes(oldId), "Duplicate enrollment");
        check(genetics.drawSixEyes().orElseThrow().equals(oldId), "Selection mismatch");
        check(genetics.hasSixEyes(oldId), "Missing active rights");
        check(!genetics.hasSixEyes(other), "Duplicate active rights");
        check(genetics.drawSixEyes().isEmpty(), "Slot must never be rerolled");
        genetics = GeneticsSavedData.load(roundTrip(genetics.save(new CompoundTag())));
        check(genetics.hasSixEyes(oldId), "Lost owner after reload");
        check(!genetics.enrollSixEyes(other), "Occupied slot accepted participant");
        check(!genetics.advanceSeason(99), "Stale season transition accepted");
        check(genetics.advanceSeason(1), "Season transition failed");
        check(!genetics.advanceSeason(1), "Repeated transition accepted");
        check(!genetics.hasSixEyes(oldId), "Old rights remain active");
        check(genetics.getProfile(oldId).technique() == InnateTechnique.NONE, "Season reset changed genetics");
        check(genetics.save(new CompoundTag()).getCompound("Season").getCompound("History")
            .getCompound("1").getUUID("Owner").equals(oldId), "Missing historical owner");
        check(genetics.enrollSixEyes(other), "New season registration failed");
        check(genetics.drawSixEyes().orElseThrow().equals(other), "New selection failed");

        for (int roll = 0; roll < 100; roll++) {
            final int value = roll;
            var random = new Random(1) { @Override public int nextInt(int bound) { return bound == 100 ? value : 0; } };
            var result = GeneticsSavedData.independentFallback(InnateTechnique.NONE, random);
            check((result != InnateTechnique.NONE) == (roll < 75), "75% boundary " + roll);
            check(GeneticsSavedData.independentFallback(InnateTechnique.LIMITLESS, random) == InnateTechnique.LIMITLESS, "Inherited replaced");
        }
        var numeric = EnergyRules.migrate(DoubleTag.valueOf(37));
        near(numeric.getDouble("Current"), 37, "Legacy numeric CE lost");
        var oldEnergy = new CompoundTag();
        oldEnergy.putDouble("Efficiency", .5); oldEnergy.putDouble("Current", 42);
        var p = EnergyRules.migrate(oldEnergy);
        near(EnergyRules.cost(20, p.getDouble("Loss")), 40, "Legacy effective cost changed");
        near(EnergyRules.cost(20, .3), 26, "Approved loss formula");
        check(EnergyRules.migrate(p).equals(p), "Migration not idempotent");
        p.putDouble("Current", 0);
        for (int i = 0; i < 20; i++) EnergyRules.tick(p);
        near(p.getDouble("Current"), 1, "Constant online regeneration");
        var loaded = EnergyRules.migrate(roundTrip(p));
        near(loaded.getDouble("Current"), 1, "Load applied offline regen");
        p.putDouble("Capacity", 0); p.putDouble("Current", 0); p.putDouble("Regeneration", 0);
        EnergyRules.tick(p);
        near(p.getDouble("Current"), 0, "Zero capacity became positive");
        p.putDouble("Strain", 80); p.putBoolean("Overloaded", true); p.putInt("OverloadRemaining", 60);
        near(EnergyRules.output(p), 70, "Output not 70 percent");
        p.putDouble("Strain", 39);
        for (int i = 0; i < 59; i++) EnergyRules.tick(p);
        check(p.getBoolean("Overloaded"), "Overload ended before 3s");
        EnergyRules.tick(p);
        check(!p.getBoolean("Overloaded"), "Overload failed to exit");
        p.putDouble("Strain", 80); p.putBoolean("Overloaded", true); p.putInt("OverloadRemaining", 60);
        for (int i = 0; i < 60; i++) EnergyRules.tick(p);
        check(p.getBoolean("Overloaded"), "Overload ended above40");
        for (int i = 0; i < 100; i++) EnergyRules.tick(p);
        check(!p.getBoolean("Overloaded"), "Overload40 boundary");
        p.putDouble("Current", Double.NaN); p.putDouble("Strain", Double.POSITIVE_INFINITY);
        p = EnergyRules.migrate(p);
        check(Double.isFinite(p.getDouble("Current")) && Double.isFinite(p.getDouble("Strain")), "Nonfinite persisted state");
        var future = new CompoundTag(); future.putInt("Version", 999);
        try { EnergyRules.migrate(future); throw new AssertionError("Future energy accepted"); } catch (IllegalStateException expected) { assertions++; }
        try { GeneticsSavedData.load(future); throw new AssertionError("Future genetics accepted"); } catch (IllegalStateException expected) { assertions++; }
        try { EnergyRules.migrate(StringTag.valueOf("broken")); throw new AssertionError("Malformed energy reset"); } catch (IllegalStateException expected) { assertions++; }
        var brokenPlayers = new CompoundTag(); brokenPlayers.putString(oldId.toString(), "broken");
        var brokenRoot = new CompoundTag(); brokenRoot.put("Players", brokenPlayers);
        try { GeneticsSavedData.load(brokenRoot).getProfile(oldId); throw new AssertionError("Malformed genetics rerolled"); } catch (IllegalStateException expected) { assertions++; }
        System.out.println("Foundation checks passed: " + assertions + " assertions (NBT migration, privacy, 75%, seasons, CE, overload)");
    }
}
