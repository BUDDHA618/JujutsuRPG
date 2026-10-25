package com.buddha618.jujutsurpg.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

/** Pure server rules; no player/client dependencies, exercised by persistence tests. */
public final class EnergyRules {
    public static final int VERSION = 2;
    public static double bounded(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
    public static CompoundTag migrate(Tag old) {
        if (old != null && !(old instanceof CompoundTag) && !(old instanceof NumericTag))
            throw new IllegalStateException("Invalid existing energy profile");
        CompoundTag p = old instanceof CompoundTag compound ? compound.copy() : new CompoundTag();
        if (p.getInt("Version") > VERSION) throw new IllegalStateException("Unsupported future energy profile version");
        boolean legacy = old instanceof CompoundTag;
        putDefault(p, "Capacity", 100);
        putDefault(p, "Current", old instanceof NumericTag n ? n.getAsDouble() : p.getDouble("Capacity"));
        // Keep the tested Limitless moves usable; 20 output requires a separate move rebalance.
        putDefault(p, "Output", 100);
        putDefault(p, "Control", 20);
        putDefault(p, "Regeneration", 1);
        if (!p.contains("Loss")) p.putDouble("Loss", legacy && p.contains("Efficiency")
            ? 1 / bounded(p.getDouble("Efficiency"), .05, 1) - 1 : .3);
        for (String field : new String[]{"Capacity", "Output", "Regeneration"})
            p.putDouble(field, bounded(p.getDouble(field), 0, 1_000_000));
        p.putDouble("Current", bounded(p.getDouble("Current"), 0, p.getDouble("Capacity")));
        p.putDouble("Control", bounded(p.getDouble("Control"), 0, 100));
        p.putDouble("Loss", bounded(p.getDouble("Loss"), 0, 19));
        p.putDouble("Strain", bounded(p.getDouble("Strain"), 0, 100));
        p.putInt("OverloadRemaining", Math.max(0, Math.min(60, p.getInt("OverloadRemaining"))));
        if (p.getDouble("Strain") >= 80 && !p.getBoolean("Overloaded")) {
            p.putBoolean("Overloaded", true);
            p.putInt("OverloadRemaining", 60);
        }
        p.putInt("Version", VERSION);
        return p;
    }
    private static void putDefault(CompoundTag p, String name, double value) {
        if (!p.contains(name, Tag.TAG_ANY_NUMERIC)) p.putDouble(name, value);
    }
    public static double cost(double base, double loss) {
        if (!Double.isFinite(base) || base < 0 || !Double.isFinite(loss) || loss < 0)
            throw new IllegalArgumentException("Invalid energy cost");
        return base * (1 + loss);
    }
    public static double output(CompoundTag p) {
        return p.getDouble("Output") * (p.getBoolean("Overloaded") ? .7 : 1);
    }
    /** One ONLINE living server tick; deliberately has no wall-clock timestamp. */
    public static boolean tick(CompoundTag p) {
        double current = p.getDouble("Current"), strain = p.getDouble("Strain");
        int remaining = p.getInt("OverloadRemaining");
        boolean overloaded = p.getBoolean("Overloaded");
        double next = Math.min(p.getDouble("Capacity"), current + p.getDouble("Regeneration") / 20);
        double cooled = Math.max(0, strain - .25);
        int countdown = Math.max(0, remaining - 1);
        boolean active = overloaded && !(countdown == 0 && cooled <= 40);
        if (next == current && cooled == strain && countdown == remaining && active == overloaded) return false;
        p.putDouble("Current", next);
        p.putDouble("Strain", cooled);
        p.putInt("OverloadRemaining", countdown);
        p.putBoolean("Overloaded", active);
        return true;
    }
}
