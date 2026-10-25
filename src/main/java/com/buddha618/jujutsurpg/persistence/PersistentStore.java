package com.buddha618.jujutsurpg.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.function.Supplier;

/** Vanilla swallows loader exceptions. Never replace an existing unreadable file with empty data. */
public final class PersistentStore {
    public static <T extends SavedData> T get(MinecraftServer server, String id,
                                            Function<CompoundTag, T> loader, Supplier<T> create) {
        var storage = server.overworld().getDataStorage();
        T loaded = storage.get(loader, id);
        if (loaded != null) return loaded;
        var path = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(id + ".dat");
        // notExists, rather than !exists: an inaccessible path must also fail closed.
        if (!Files.notExists(path)) throw new IllegalStateException(
            "Refusing to replace unreadable JujutsuRPG data: " + path + ". Restore a backup or use a compatible mod version.");
        T fresh = create.get();
        storage.set(id, fresh);
        return fresh;
    }
}
