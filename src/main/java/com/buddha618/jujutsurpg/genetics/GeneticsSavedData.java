
package com.buddha618.jujutsurpg.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticsSavedData extends SavedData {

    public static final int VERSION = 2;
    private SeasonLedger season = new SeasonLedger(new CompoundTag());
    private CompoundTag players = new CompoundTag();

    public static GeneticsSavedData get(MinecraftServer server) {
        return com.buddha618.jujutsurpg.persistence.PersistentStore.get(server, "jujutsurpg_genetics",
            GeneticsSavedData::load, GeneticsSavedData::new);
    }

    public static GeneticsSavedData load(CompoundTag tag) {
        GeneticsSavedData data = new GeneticsSavedData();
        if (tag.getInt("Version") > VERSION) throw new IllegalStateException("Unsupported future genetics version; do not downgrade this world");
        if (tag.contains("Players") && !tag.contains("Players", Tag.TAG_COMPOUND))
            throw new IllegalStateException("Invalid genetics Players tag");
        if (tag.contains("Season") && !tag.contains("Season", Tag.TAG_COMPOUND))
            throw new IllegalStateException("Invalid genetics Season tag");
        data.players = tag.getCompound("Players").copy();
        data.season = new SeasonLedger(tag.getCompound("Season"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version", VERSION);
        tag.put("Season", season.save());
        tag.put("Players", players.copy());
        return tag;
    }

    public String visibleTechnique(UUID uuid) {
        if (!hasProfile(uuid)) return "hidden";
        CompoundTag profile = players.getCompound(uuid.toString());
        return profile.getBoolean("Awakened") ? profile.getString("Technique") : "hidden";
    }

    /** Test-only server completion hook. Never rerolls a profile. */
    public boolean completeAwakening(UUID uuid) {
        if (!hasProfile(uuid)) return false;
        var profile = players.getCompound(uuid.toString());
        profile.putBoolean("Awakened", true);
        setDirty();
        return true;
    }

    /** Explicit operator override; preserves unrelated profile data. */
    public void developerSet(UUID uuid, Clan clan, InnateTechnique technique, boolean awakened) {
        getOrCreate(uuid);
        var p = players.getCompound(uuid.toString());
        p.putString("Clan", clan.getId());
        p.putString("Technique", technique.getId());
        p.putBoolean("Awakened", awakened);
        setDirty();
    }

    public boolean hasProfile(UUID uuid) {
        return players.contains(uuid.toString(), Tag.TAG_COMPOUND);
    }

    public GeneticProfile getOrCreate(UUID uuid) {
        String key = uuid.toString();

        if (players.contains(key) && !hasProfile(uuid)) throw new IllegalStateException("Invalid existing genetic profile: " + key);
        if (!hasProfile(uuid)) {
            Clan clan = rollClan();
            InnateTechnique technique = rollTechnique(clan);
            technique = independentFallback(technique, ThreadLocalRandom.current());

            CompoundTag profile = new CompoundTag();
            profile.putString("Clan", clan.getId());
            profile.putString("Technique", technique.getId());

            players.put(key, profile);
            setDirty();
        }

        CompoundTag profile = players.getCompound(key);
        if (profile.getInt("Version") > VERSION) throw new IllegalStateException("Unsupported future genetic profile version");
        if (profile.getInt("Version") < VERSION) {
            // Deterministic additive migration: NONE, unknown IDs and unrelated fields survive unchanged.
            if (!profile.contains("Potentials", Tag.TAG_COMPOUND)) {
                var potentials = new CompoundTag();
                for (String skill : new String[]{"Capacity", "Output", "Control", "Efficiency", "Regeneration"})
                    potentials.putString(skill, "normal");
                profile.put("Potentials", potentials);
            }
            if (!profile.contains("Restriction")) profile.putString("Restriction", "none");
            if (!profile.contains("Awakened")) profile.putBoolean("Awakened", false);
            profile.putInt("Version", VERSION);
            setDirty();
        }

        return new GeneticProfile(
                readClan(profile.getString("Clan")),
                readTechnique(profile.getString("Technique"))
        );
    }

    public static InnateTechnique independentFallback(InnateTechnique inherited, java.util.random.RandomGenerator random) {
        if (inherited != InnateTechnique.NONE || random.nextInt(100) >= 75) return inherited;
        // Equal weights are a provisional choice; no rarity probabilities have been approved.
        return InnateTechnique.INDEPENDENT.get(random.nextInt(InnateTechnique.INDEPENDENT.size()));
    }
    public long seasonNumber() { return season.number(); }
    public int seasonCandidates() { return season.candidates(); }
    public java.util.Optional<UUID> sixEyesOwner() { return season.owner(); }
    public boolean hasSixEyes(UUID id) {
        return season.owner().filter(id::equals).isPresent() && eligible(id);
    }
    private boolean eligible(UUID id) {
        return hasProfile(id) && players.getCompound(id.toString()).getString("Clan").equals(Clan.GOJO.getId());
    }
    public boolean enrollSixEyes(UUID id) {
        boolean changed = eligible(id) && season.enroll(id);
        if (changed) setDirty();
        return changed;
    }
    public java.util.Optional<UUID> drawSixEyes() {
        var selected = season.draw(ThreadLocalRandom.current(), this::eligible);
        if (selected.isPresent()) setDirty();
        return selected;
    }
    public boolean advanceSeason(long expected) {
        boolean changed = season.advance(expected);
        if (changed) setDirty();
        return changed;
    }

    private static Clan rollClan() {
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 5) return Clan.GOJO;
        if (roll < 20) return Clan.ZENIN;
        if (roll < 35) return Clan.KAMO;

        return Clan.NONE;
    }

    private static InnateTechnique rollTechnique(Clan clan) {
        int roll = ThreadLocalRandom.current().nextInt(100);

        return switch (clan) {
            case GOJO -> roll < 20
                    ? InnateTechnique.LIMITLESS
                    : InnateTechnique.NONE;

            case ZENIN -> roll < 10
                    ? InnateTechnique.TEN_SHADOWS
                    : roll < 30
                    ? InnateTechnique.PROJECTION_SORCERY
                    : InnateTechnique.NONE;

            case KAMO -> roll < 40
                    ? InnateTechnique.BLOOD_MANIPULATION
                    : InnateTechnique.NONE;

            case NONE -> InnateTechnique.NONE;
        };
    }

    private static Clan readClan(String id) {
        for (Clan clan : Clan.values()) {
            if (clan.getId().equals(id)) return clan;
        }
        return Clan.NONE;
    }

    private static InnateTechnique readTechnique(String id) {
        for (InnateTechnique technique : InnateTechnique.values()) {
            if (technique.getId().equals(id)) return technique;
        }
        return InnateTechnique.NONE;
    }

    public record GeneticProfile(
            Clan clan,
            InnateTechnique technique
    ) {}
}
