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
    public double current(ServerPlayer player) {
        String id = player.getUUID().toString();
        if (!players.contains(id)) { players.putDouble(id, CAPACITY); setDirty(); }
        double value = players.getDouble(id);
        return Double.isFinite(value) ? Math.max(0, Math.min(CAPACITY, value)) : 0;
    }
    /** Future abilities must call this on the server thread before applying their effect. */
    public boolean trySpend(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        double value = current(player);
        if (value < amount) return false;
        players.putDouble(player.getUUID().toString(), value - amount);
        setDirty();
        sync(player);
        return true;
    }
    public void sync(ServerPlayer player) {
        CharacterNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new CharacterNetwork.Energy(current(player), CAPACITY, REGEN_PER_SECOND));
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
                || player.tickCount % 20 != 0 || !player.isAlive()) return;
        var data = get(player);
        double previous = data.current(player);
        double next = Math.min(CAPACITY, previous + REGEN_PER_SECOND);
        if (next != previous) {
            data.players.putDouble(player.getUUID().toString(), next);
            data.setDirty();
            data.sync(player);
        }
    }
}

