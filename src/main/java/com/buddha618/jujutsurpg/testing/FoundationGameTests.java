package com.buddha618.jujutsurpg.testing;

import com.buddha618.jujutsurpg.energy.CursedEnergy;
import com.buddha618.jujutsurpg.genetics.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Only run via the Forge GameTest harness in a disposable test world. */
@GameTestHolder("jujutsurpg")
@PrefixGameTestTemplate(false)
public final class FoundationGameTests {
    @GameTest(template = "foundation_empty", timeoutTicks = 100)
    public static void serverEnergyAndPersistence(GameTestHelper h) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "foundation-test"));
        var data = CursedEnergy.get(player);
        double before = data.current(player);
        h.assertTrue(data.tryMaintained(player, 20, 20, 0), "First server spend rejected");
        h.assertTrue(Math.abs(data.current(player) - (before - 26)) < 1e-8, "Wrong effective cost");
        double after = data.current(player);
        h.assertFalse(data.tryMaintained(player, 1, 81, 0), "Shared output limit bypassed");
        h.assertFalse(data.tryMaintained(player, -1, 1, 0), "Negative cost accepted");
        h.assertFalse(data.tryMaintained(player, Double.NaN, 1, 0), "NaN accepted");
        h.assertTrue(data.current(player) == after, "Rejected actions consumed CE");
        var loaded = CursedEnergy.load(data.save(new CompoundTag()));
        h.assertTrue(loaded.current(player) == after, "CE roundtrip changed reserve");
        var genetics = GeneticsSavedData.get(player.getServer());
        genetics.developerSet(player.getUUID(), Clan.GOJO, InnateTechnique.NONE, false);
        h.assertTrue(genetics.visibleTechnique(player.getUUID()).equals("hidden"), "Hidden technique leaked");
        genetics.completeAwakening(player.getUUID());
        var reloaded = GeneticsSavedData.load(genetics.save(new CompoundTag()));
        h.assertTrue(reloaded.visibleTechnique(player.getUUID()).equals("none"), "Awakening lost");
        h.runAfterDelay(21, () -> {
            h.assertTrue(data.tryMaintained(player, 1, 81, 0), "Rolling output did not expire");

            h.succeed();
        });
    }
    @GameTest(template = "foundation_empty", timeoutTicks = 100)
    public static void reinforcementStopsWithoutRemovingOtherBonuses(GameTestHelper h) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "reinforce-test"));
        var attack = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        var other = java.util.UUID.randomUUID();
        attack.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(other, "Other mod", 7,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        double before = attack.getValue();
        com.buddha618.jujutsurpg.energy.EnergyReinforcement.activate(player, 2);
        var tick = new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.START, player);
        com.buddha618.jujutsurpg.energy.EnergyReinforcement.tick(tick);
        h.assertTrue(attack.getValue() == before + 2, "Attack modifier missing");
        for (int i = 0; i < 1000; i++) com.buddha618.jujutsurpg.energy.EnergyReinforcement.tick(tick);
        h.assertTrue(player.getPersistentData().getInt("jujutsurpg_reinforcement_mode") == 0, "Insufficient CE did not stop mode");
        h.assertTrue(attack.getValue() == before && attack.getModifier(other) != null, "Foreign modifier removed or own modifier leaked");
        h.assertTrue(CursedEnergy.get(player).current(player) >= 0, "Negative reserve");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", timeoutTicks = 100)
    public static void unreadableSaveIsNotReplaced(GameTestHelper h) throws Exception {
        String id = "jujutsurpg_test_invalid_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var server = h.getLevel().getServer();
        var directory = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").toAbsolutePath().normalize();
        java.nio.file.Files.createDirectories(directory);
        var path = directory.resolve(id + ".dat").normalize();
        h.assertTrue(path.startsWith(directory), "Invalid test path");
        var root = new CompoundTag(); var future = new CompoundTag(); future.putInt("Version", 999); root.put("data", future);
        net.minecraft.nbt.NbtIo.writeCompressed(root, path.toFile());
        byte[] before = java.nio.file.Files.readAllBytes(path);
        boolean rejected = false;
        try {
            com.buddha618.jujutsurpg.persistence.PersistentStore.get(server, id, GeneticsSavedData::load, GeneticsSavedData::new);
        } catch (IllegalStateException expected) { rejected = true; }
        h.assertTrue(rejected, "Vanilla loader failure silently created empty data");
        h.assertTrue(java.util.Arrays.equals(before, java.nio.file.Files.readAllBytes(path)), "Original file changed");
        java.nio.file.Files.delete(path);
        h.succeed();
    }
}
