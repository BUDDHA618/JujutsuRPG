package com.buddha618.jujutsurpg.energy;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Server-owned persistent energy. No regeneration while offline; death preserves the reserve. */
@Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID)
public final class CursedEnergy extends SavedData {
    public static final double CAPACITY = 100;
    public static final double REGEN_PER_SECOND = 1;
    private final java.util.Set<java.util.UUID> validated = new java.util.HashSet<>();
    private CompoundTag players = new CompoundTag();
    public static CursedEnergy get(ServerPlayer player) {
        return get(player.getServer());
    }
    private static CursedEnergy get(net.minecraft.server.MinecraftServer server) {
        return com.buddha618.jujutsurpg.persistence.PersistentStore.get(server, "jujutsurpg_energy", CursedEnergy::load, CursedEnergy::new);
    }
    public static CursedEnergy load(CompoundTag tag) {
        var data = new CursedEnergy();
        if (tag.getInt("Version") > EnergyRules.VERSION) throw new IllegalStateException("Unsupported future energy data version");
        if (tag.contains("Players") && !tag.contains("Players", 10)) throw new IllegalStateException("Invalid energy Players tag");
        data.players = tag.getCompound("Players").copy();
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) { tag.putInt("Version", EnergyRules.VERSION); tag.put("Players", players.copy()); return tag; }
    /** Independent MVP defaults, not approved genetic coefficients. */
    private CompoundTag profile(ServerPlayer player) {
        String id = player.getUUID().toString();
        if (!validated.contains(player.getUUID())) {
            var migrated = EnergyRules.migrate(players.get(id));
            if (!migrated.equals(players.get(id))) {
                players.put(id, migrated);
                setDirty();
            }
            validated.add(player.getUUID());
        }
        return players.getCompound(id);
    }
    private static double finite(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
    public double current(ServerPlayer player) {
        if (!com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).hasProfile(player.getUUID()) || com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).fullPhysical(player.getUUID())) return 0;
        var p = profile(player);
        return finite(p.getDouble("Current"), 0, finite(p.getDouble("Capacity"), 0, 1000000));
    }
    public double output(ServerPlayer player) {
        if (!com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).hasProfile(player.getUUID())
                || com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).fullPhysical(player.getUUID())) return 0;
        var p = profile(player);
        return finite(p.getDouble("Output"), 0, 1000000) * (p.getBoolean("Overloaded") ? .7 : 1);
    }
    public double loss(ServerPlayer player) {
        double base = profile(player).getDouble("Loss");
        boolean sixEyes = com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).hasSixEyes(player.getUUID());
        return base * (sixEyes ? com.buddha618.jujutsurpg.FoundationConfig.SIX_EYES_LOSS.get() : 1);
    }
    public boolean trySpend(ServerPlayer player, double amount) {
        return tryTechnique(player, amount, amount, 0);
    }
    /** Output is reserved over a rolling second, never supplied by a client packet. */
    public boolean tryTechnique(ServerPlayer player, double baseCost, double requiredOutput, double minimumControl) {
        return consume(player, baseCost, requiredOutput, minimumControl, true);
    }
    public boolean tryMaintained(ServerPlayer player, double baseCost, double requiredOutput, double minimumControl) {
        return consume(player, baseCost, requiredOutput, minimumControl, false);
    }
    private boolean consume(ServerPlayer player, double baseCost, double requiredOutput, double minimumControl, boolean immediateSync) {
        if (!com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).hasProfile(player.getUUID())
                || com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).fullPhysical(player.getUUID())
                || !player.isAlive() || player.isSpectator() || !Double.isFinite(baseCost)
                || !Double.isFinite(requiredOutput) || !Double.isFinite(minimumControl)
                || baseCost < 0 || requiredOutput < 0 || minimumControl < 0) return false;
        var p = profile(player);
        long now = player.getServer().overworld().getGameTime();
        var uses = recentUses.computeIfAbsent(player.getUUID(), id -> new java.util.ArrayDeque<>());
        while (!uses.isEmpty() && now - uses.peekFirst().tick() >= 20) uses.removeFirst();
        double used = uses.stream().mapToDouble(Use::output).sum();
        double cost = EnergyRules.cost(baseCost, loss(player));
        if ((!infinite(player) && (current(player) <= 0 || current(player) < cost)) || used + requiredOutput > output(player)
                || finite(p.getDouble("Control"), 0, 1000000) < minimumControl) return false;
        if (!infinite(player)) p.putDouble("Current", current(player) - cost);
        if (current(player) <= 0) EnergyReinforcement.onDepleted(player);
        uses.addLast(new Use(now, requiredOutput));
        // Test tuning: strain grows in proportion to output and decays at 5 units/sec.
        double strain = finite(p.getDouble("Strain") + requiredOutput / 2, 0, 100);
        p.putDouble("Strain", strain);
        if (strain >= 80 && !p.getBoolean("Overloaded")) {
            p.putBoolean("Overloaded", true);
            p.putInt("OverloadRemaining", 60);
        }
        setDirty();
        if (immediateSync) sync(player);
        return true;
    }
    private record Use(long tick, double output) {}
    private final java.util.Map<java.util.UUID, java.util.ArrayDeque<Use>> recentUses = new java.util.HashMap<>();
    public boolean infinite(ServerPlayer player) {
        return player.hasPermissions(2) && profile(player).getBoolean("DeveloperInfiniteCE");
    }
    public void toggleInfinite(ServerPlayer player) {
        if (!player.hasPermissions(2)) return;
        var p = profile(player);
        p.putBoolean("DeveloperInfiniteCE", !infinite(player));
        setDirty();
        if (infinite(player)) developerRestore(player);
        else sync(player);
    }
    public void developerRestore(ServerPlayer player) {
        var p = profile(player);
        p.putDouble("Current", finite(p.getDouble("Capacity"), 0, 1000000));
        p.putDouble("Strain", 0);
        p.putBoolean("Overloaded", false);
        p.putInt("OverloadRemaining", 0);
        recentUses.remove(player.getUUID());
        setDirty();
        sync(player);
    }
    public void sync(ServerPlayer player) {
        if (!com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).hasProfile(player.getUUID())
                || player instanceof net.minecraftforge.common.util.FakePlayer) return;
        var p = profile(player);
        boolean physical = com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).fullPhysical(player.getUUID());
        CharacterNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new CharacterNetwork.Energy(current(player), physical ? 0 : finite(p.getDouble("Capacity"), 0, 1000000),
                physical ? 0 : finite(p.getDouble("Regeneration"), 0, 1000000), output(player),
                p.getDouble("Control"), loss(player), p.getDouble("Strain"), p.getBoolean("Overloaded")));
    }
    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("jjkenergy")
            .requires(source -> source.hasPermission(2))
            .then(net.minecraft.commands.Commands.literal("spend")
                .then(net.minecraft.commands.Commands.argument("amount", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0))
                    .executes(context -> {
                        var player = context.getSource().getPlayerOrException();
                        double amount = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "amount");
                        boolean spent = get(player).trySpend(player, amount);
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            spent ? "ПЭ потрачена: " + amount : "Недостаточно ПЭ"), false);
                        return spent ? 1 : 0;
                    }))));
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        var data = get(event.getServer());
        long now = event.getServer().overworld().getGameTime();
        data.recentUses.entrySet().removeIf(e -> e.getValue().isEmpty() || now - e.getValue().peekLast().tick() >= 20);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) get(player).validated.remove(player.getUUID());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) get(player).sync(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) get(player).sync(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) get(player).sync(player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || !com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).hasProfile(player.getUUID())) return;
        var data = get(player);
        if (com.buddha618.jujutsurpg.genetics.GeneticsSavedData.get(player.getServer()).fullPhysical(player.getUUID())) return;
        var p = data.profile(player);
        if (EnergyRules.tick(p)) data.setDirty();
        var uses = data.recentUses.get(player.getUUID());
        if (uses != null) {
            long now = player.getServer().overworld().getGameTime();
            while (!uses.isEmpty() && now - uses.peekFirst().tick() >= 20) uses.removeFirst();
            if (uses.isEmpty()) data.recentUses.remove(player.getUUID());
        }
        if (player.tickCount % 20 == 0) data.sync(player);
    }
}
