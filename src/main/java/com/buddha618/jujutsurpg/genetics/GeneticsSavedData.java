
package com.buddha618.jujutsurpg.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticsSavedData extends SavedData {

    public static final int VERSION = 3;
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
        if (!hasProfile(uuid)) {
            var initial = new CompoundTag(); initial.putString("Clan",clan.getId()); initial.putString("Technique",technique.getId());
            initial.putString("OriginSource","developer"); players.put(uuid.toString(),initial);
        }
        getProfile(uuid);
        var p = players.getCompound(uuid.toString());
        p.putString("Clan", clan.getId());
        p.putString("Technique", technique.getId());
        p.putBoolean("Awakened", awakened);
        setDirty();
    }

    public boolean hasProfile(UUID uuid) {
        return players.contains(uuid.toString(), Tag.TAG_COMPOUND);
    }

    public GeneticProfile getProfile(UUID uuid) {
        String key = uuid.toString();

        if (players.contains(key) && !hasProfile(uuid)) throw new IllegalStateException("Invalid existing genetic profile: " + key);
        if (!hasProfile(uuid)) throw new IllegalStateException("Character origin has not been selected");

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
            if (!profile.contains("OriginSource")) profile.putString("OriginSource","legacy");
            if (!profile.hasUUID("CharacterId")) profile.putUUID("CharacterId",UUID.nameUUIDFromBytes(("jujutsurpg:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (!profile.contains("CreatedSeason")) profile.putLong("CreatedSeason",season.number());
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
        return hasProfile(id) && !fullPhysical(id) && players.getCompound(id.toString()).getString("Clan").equals(Clan.GOJO.getId());
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

    /** The sole ordinary creation entry point: validate first, build locally, insert once. */
    public boolean selectOrigin(UUID uuid, String clanId, java.util.random.RandomGenerator random) {
        if (players.contains(uuid.toString())) return false;
        var definition = com.buddha618.jujutsurpg.clans.ClanRegistry.find(clanId);
        if (definition.isEmpty()) return false;
        var profile = new CompoundTag();
        profile.putString("Clan",clanId);
        profile.putString("Restriction","none"); // Random restrictions remain disabled until their physical path is implemented.
        profile.putString("Technique",rollCompatible(definition.get(),false,random).getId());
        var potentials = new CompoundTag();
        for (String skill : new String[]{"Capacity","Output","Control","Efficiency","Regeneration"}) {
            int roll = random.nextInt(100);
            potentials.putString(skill,roll<15?"low":roll<80?"normal":roll<98?"high":"exceptional");
        }
        profile.put("Potentials",potentials); // Proposed distribution; stat effects await energy progression.
        profile.putString("OriginSource","chosen");
        profile.putUUID("CharacterId",UUID.randomUUID()); profile.putLong("CreatedSeason",season.number());
        players.put(uuid.toString(),profile);
        getProfile(uuid); setDirty(); return true;
    }
    public static InnateTechnique rollCompatible(com.buddha618.jujutsurpg.clans.ClanDefinition definition,
                                                 boolean fullPhysicalRestriction, java.util.random.RandomGenerator random) {
        return fullPhysicalRestriction ? InnateTechnique.NONE : definition.roll(random);
    }
    public boolean fullPhysical(UUID id) {
        return hasProfile(id) && players.getCompound(id.toString()).getString("Restriction").equals("full_physical");
    }
    public com.buddha618.jujutsurpg.clans.ClanProgression progression(UUID id) {
        getProfile(id);
        var p = players.getCompound(id.toString());
        if (!p.contains("ClanProgress")) { p.put("ClanProgress",new CompoundTag()); setDirty(); }
        if (!p.contains("ClanProgress",Tag.TAG_COMPOUND)) throw new IllegalStateException("Invalid clan progression");
        return new com.buddha618.jujutsurpg.clans.ClanProgression(p.getCompound("ClanProgress"),this::setDirty);
    }
    /** Explicit one-time legacy migration, with a full profile snapshot and no technique reroll. */
    public boolean migrateLegacyOrigin(UUID id, String clanId) {
        if (!hasProfile(id) || com.buddha618.jujutsurpg.clans.ClanRegistry.find(clanId).isEmpty()) return false;
        getProfile(id);
        var p = players.getCompound(id.toString());
        if (!p.getString("OriginSource").equals("legacy") || p.getBoolean("LegacyMigrationUsed")) return false;
        p.put("BeforeOriginMigration",p.copy());
        p.putString("Clan",clanId); p.putString("OriginSource","migrated"); p.putBoolean("LegacyMigrationUsed",true);
        p.remove("ClanProgress"); setDirty(); return true;
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
