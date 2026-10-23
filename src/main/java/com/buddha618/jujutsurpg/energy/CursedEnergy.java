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
    private CompoundTag players = new CompoundTag();
    public static CursedEnergy get(ServerPlayer player) {
        return player.getServer().overworld().getDataStorage().computeIfAbsent(CursedEnergy::load, CursedEnergy::new, "jujutsurpg_energy");
    }
    public static CursedEnergy load(CompoundTag tag) {
        var data = new CursedEnergy();
        data.players = tag.getCompound("Players").copy();
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) { tag.put("Players", players.copy()); return tag; }
    /** Independent MVP defaults, not approved genetic coefficients. */
    private CompoundTag profile(ServerPlayer player) {
        String id = player.getUUID().toString();
        if (!players.contains(id, 10)) {
            double reserve = players.contains(id) ? players.getDouble(id) : CAPACITY;
            var profile = new CompoundTag();
            profile.putDouble("Current", finite(reserve, 0, CAPACITY));
            profile.putDouble("Capacity", CAPACITY);
            profile.putDouble("Output", 100);
            profile.putDouble("Control", 1);
            profile.putDouble("Efficiency", 1);
            profile.putDouble("Regeneration", REGEN_PER_SECOND);
            profile.putInt("Version", 1);
            players.put(id, profile);
            setDirty();
        }
        return players.getCompound(id);
    }
    private static double finite(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
    public double current(ServerPlayer player) {
        var p = profile(player);
        return finite(p.getDouble("Current"), 0, finite(p.getDouble("Capacity"), 1, 1000000));
    }
    public double output(ServerPlayer player) {
        var p = profile(player);
        return finite(p.getDouble("Output"), 0, 1000000) * (p.getBoolean("Overloaded") ? .7 : 1);
    }
    public boolean trySpend(ServerPlayer player, double amount) {
        return tryTechnique(player, amount, amount, 0);
    }
    /** Output is reserved over a rolling second, never supplied by a client packet. */
    public boolean tryTechnique(ServerPlayer player, double baseCost, double requiredOutput, double minimumControl) {
        if (!player.isAlive() || player.isSpectator() || !Double.isFinite(baseCost)
                || !Double.isFinite(requiredOutput) || !Double.isFinite(minimumControl)
                || baseCost < 0 || requiredOutput < 0 || minimumControl < 0) return false;
        var p = profile(player);
        long now = player.getServer().overworld().getGameTime();
        var uses = recentUses.computeIfAbsent(player.getUUID(), id -> new java.util.ArrayDeque<>());
        while (!uses.isEmpty() && now - uses.peekFirst().tick() >= 20) uses.removeFirst();
        double used = uses.stream().mapToDouble(Use::output).sum();
        double cost = baseCost / finite(p.getDouble("Efficiency"), .05, 1);
        if (current(player) <= 0 || current(player) < cost || used + requiredOutput > output(player)
                || finite(p.getDouble("Control"), 0, 1000000) < minimumControl) return false;
        p.putDouble("Current", current(player) - cost);
        uses.addLast(new Use(now, requiredOutput));
        // Test tuning: strain grows in proportion to output and decays at 5 units/sec.
        double strain = finite(p.getDouble("Strain") + requiredOutput / 2, 0, 100);
        p.putDouble("Strain", strain);
        if (strain >= 80 && !p.getBoolean("Overloaded")) {
            p.putBoolean("Overloaded", true);
            p.putInt("OverloadRemaining", 60);
        }
        setDirty();
        sync(player);
        return true;
    }
    private record Use(long tick, double output) {}
    private final java.util.Map<java.util.UUID, java.util.ArrayDeque<Use>> recentUses = new java.util.HashMap<>();
    public void developerRestore(ServerPlayer player) {
        var p = profile(player);
        p.putDouble("Current", finite(p.getDouble("Capacity"), 1, 1000000));
        p.putDouble("Strain", 0);
        p.putBoolean("Overloaded", false);
        p.putInt("OverloadRemaining", 0);
        recentUses.remove(player.getUUID());
        setDirty();
        sync(player);
    }
    public void sync(ServerPlayer player) {
        var p = profile(player);
        CharacterNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new CharacterNetwork.Energy(current(player), finite(p.getDouble("Capacity"), 1, 1000000),
                finite(p.getDouble("Regeneration"), 0, 1000000)));
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
                || !player.isAlive()) return;
        var data = get(player);
        var p = data.profile(player);
        p.putDouble("Current", Math.min(finite(p.getDouble("Capacity"), 1, 1000000),
            data.current(player) + finite(p.getDouble("Regeneration"), 0, 1000000) / 20));
        p.putDouble("Strain", Math.max(0, finite(p.getDouble("Strain"), 0, 100) - .25));
        int remaining = Math.max(0, p.getInt("OverloadRemaining") - 1);
        p.putInt("OverloadRemaining", remaining);
        if (p.getBoolean("Overloaded") && remaining == 0 && p.getDouble("Strain") <= 40)
            p.putBoolean("Overloaded", false);
        data.setDirty();
        if (player.tickCount % 20 == 0) data.sync(player);
    }
}
