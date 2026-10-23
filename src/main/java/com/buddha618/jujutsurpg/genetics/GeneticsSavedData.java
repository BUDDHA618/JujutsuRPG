
package com.buddha618.jujutsurpg.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GeneticsSavedData extends SavedData {

    private CompoundTag players = new CompoundTag();

    public static GeneticsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(
                        GeneticsSavedData::load,
                        GeneticsSavedData::new,
                        "jujutsurpg_genetics"
                );
    }

    public static GeneticsSavedData load(CompoundTag tag) {
        GeneticsSavedData data = new GeneticsSavedData();
        data.players = tag.getCompound("Players").copy();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("Players", players.copy());
        return tag;
    }

    public boolean hasProfile(UUID uuid) {
        return players.contains(uuid.toString(), Tag.TAG_COMPOUND);
    }

    public GeneticProfile getOrCreate(UUID uuid) {
        String key = uuid.toString();

        if (!hasProfile(uuid)) {
            Clan clan = rollClan();
            InnateTechnique technique = rollTechnique(clan);

            CompoundTag profile = new CompoundTag();
            profile.putString("Clan", clan.getId());
            profile.putString("Technique", technique.getId());

            players.put(key, profile);
            setDirty();
        }

        CompoundTag profile = players.getCompound(key);

        return new GeneticProfile(
                readClan(profile.getString("Clan")),
                readTechnique(profile.getString("Technique"))
        );
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
