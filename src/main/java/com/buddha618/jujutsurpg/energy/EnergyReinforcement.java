package com.buddha618.jujutsurpg.energy;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Maintained server ability. Removes only this mod's attribute modifiers, never potion effects. */
@Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID)
public final class EnergyReinforcement {
    private static final String MODE = "jujutsurpg_reinforcement_mode";
    private static final String LAST_USE = "jujutsurpg_reinforcement_tick";
    private static final UUID ATTACK = UUID.fromString("2e7cd84f-c16b-4e1f-94ad-5cf81d454101");
    private static final UUID DEFENSE = UUID.fromString("2e7cd84f-c16b-4e1f-94ad-5cf81d454102");

    /** 0 off, 1 balanced, 2 attack, 3 defense; clicking the current mode stops it. */
    public static void activate(ServerPlayer player, int mode) {
        if (!player.isAlive() || player.isSpectator() || mode < 0 || mode > 3) return;
        long now = player.getServer().overworld().getGameTime();
        var tags = player.getPersistentData();
        if (tags.contains(LAST_USE) && now >= tags.getLong(LAST_USE) && now - tags.getLong(LAST_USE) < 10) return;
        tags.putLong(LAST_USE, now);
        if (tags.getInt(MODE) == mode || mode == 0) { stop(player); return; }
        if (CursedEnergy.get(player).current(player) <= 0) return;
        clearModifiers(player);
        tags.putInt(MODE, mode);
        player.displayClientMessage(Component.literal("Усиление включено. Повторное нажатие отключает. Расход задаётся настройками сервера."), true);
    }
    private static void clearModifiers(ServerPlayer player) {
        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ATTACK);
        player.getAttribute(Attributes.ARMOR).removeModifier(DEFENSE);
    }
    public static void onDepleted(ServerPlayer player) { stop(player); }
    private static void stop(ServerPlayer player) {
        player.getPersistentData().remove(MODE);
        clearModifiers(player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !(e.player instanceof ServerPlayer p)) return;
        int mode = p.getPersistentData().getInt(MODE);
        if (mode == 0) return;
        var energy = CursedEnergy.get(p);
        // A common rolling output budget is shared with all instant techniques.
        if (!p.isAlive() || p.isSpectator() || !energy.tryMaintained(p, com.buddha618.jujutsurpg.FoundationConfig.REINFORCEMENT_COST.get() / 20, com.buddha618.jujutsurpg.FoundationConfig.REINFORCEMENT_COST.get() / 20, 0)) {
            stop(p);
            p.displayClientMessage(Component.literal("Усиление остановлено: недостаточно ПЭ или доступного выхода."), true);
            return;
        }
        if (energy.current(p) <= 0) { stop(p); return; }
        double attack = mode == 2 ? 2 : mode == 1 ? 1 : 0;
        double armor = mode == 3 ? 4 : mode == 1 ? 2 : 0;
        if (attack > 0 && p.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(ATTACK) == null)
            p.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(ATTACK, "Jujutsu reinforcement", attack, AttributeModifier.Operation.ADDITION));
        if (armor > 0 && p.getAttribute(Attributes.ARMOR).getModifier(DEFENSE) == null)
            p.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(DEFENSE, "Jujutsu reinforcement", armor, AttributeModifier.Operation.ADDITION));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) { if(e.getEntity() instanceof ServerPlayer p) stop(p); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if(e.getEntity() instanceof ServerPlayer p) stop(p); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if(e.getEntity() instanceof ServerPlayer p) stop(p); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if(e.getEntity() instanceof ServerPlayer p) stop(p); }
}
