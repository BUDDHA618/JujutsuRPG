package com.buddha618.jujutsurpg.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.*;

/** Embedded in genetics SavedData: ownership and profiles share one save boundary. */
public final class SeasonLedger {
    private final CompoundTag state;

    public SeasonLedger(CompoundTag saved) {
        state = saved.copy();
        if (state.contains("Number") && state.getLong("Number") < 1) throw new IllegalStateException("Invalid season number");
        if (state.contains("Owner") && !state.hasUUID("Owner")) throw new IllegalStateException("Invalid seasonal owner");
        for (String key : new String[]{"Candidates", "History"})
            if (state.contains(key) && !state.contains(key, Tag.TAG_COMPOUND)) throw new IllegalStateException("Invalid seasonal " + key);
        for (String id : state.getCompound("Candidates").getAllKeys()) UUID.fromString(id);
        if (!state.contains("Number")) state.putLong("Number", 1);
        if (!state.contains("Candidates", Tag.TAG_COMPOUND)) state.put("Candidates", new CompoundTag());
        if (!state.contains("History", Tag.TAG_COMPOUND)) state.put("History", new CompoundTag());
    }
    public CompoundTag save() { return state.copy(); }
    public long number() { return state.getLong("Number"); }
    public Optional<UUID> owner() { return state.hasUUID("Owner") ? Optional.of(state.getUUID("Owner")) : Optional.empty(); }
    public int candidates() { return state.getCompound("Candidates").size(); }
    public boolean enroll(UUID id) {
        if (owner().isPresent()) return false;
        var candidates = state.getCompound("Candidates");
        if (candidates.contains(id.toString())) return false;
        candidates.putBoolean(id.toString(), true);
        return true;
    }
    /** Caller supplies server randomness and current compatibility, never client data. */
    public Optional<UUID> draw(java.util.random.RandomGenerator random, java.util.function.Predicate<UUID> eligible) {
        if (owner().isPresent()) return Optional.empty();
        var pool = state.getCompound("Candidates").getAllKeys().stream()
            .map(UUID::fromString).filter(eligible).sorted().toList();
        if (pool.isEmpty()) return Optional.empty();
        UUID selected = pool.get(random.nextInt(pool.size()));
        state.putUUID("Owner", selected);
        return Optional.of(selected);
    }
    /** Monotonic IDs prevent reopening a past season. No profile is rerolled. */
    public boolean advance(long expectedCurrent) {
        if (expectedCurrent != number() || number() == Long.MAX_VALUE) return false;
        CompoundTag archived = new CompoundTag();
        owner().ifPresent(id -> archived.putUUID("Owner", id));
        archived.put("Candidates", state.getCompound("Candidates").copy());
        state.getCompound("History").put(Long.toString(number()), archived);
        state.putLong("Number", number() + 1);
        state.remove("Owner");
        state.put("Candidates", new CompoundTag());
        return true;
    }
}
