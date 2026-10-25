package com.buddha618.jujutsurpg;

import net.minecraftforge.common.ForgeConfigSpec;

/** Proposed server balance. Approved uniqueness, 75% and overload thresholds are not knobs. */
public final class FoundationConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.DoubleValue SIX_EYES_LOSS = B
        .comment("PROPOSED: multiply energy losses, not base cost, for the active Six Eyes owner.")
        .defineInRange("sixEyesLossMultiplier", .1, 0, 1);
    public static final ForgeConfigSpec.DoubleValue REINFORCEMENT_COST = B
        .comment("PROPOSED: base CE per second for maintained body reinforcement.")
        .defineInRange("reinforcementCostPerSecond", 3, .1, 1000);
    public static final ForgeConfigSpec SPEC = B.build();
}
