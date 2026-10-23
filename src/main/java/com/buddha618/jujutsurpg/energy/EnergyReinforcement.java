package com.buddha618.jujutsurpg.energy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Only called by the server packet handler; the client chooses a mode, never a cost or power. */
public final class EnergyReinforcement {
    private static final String LAST_USE = "jujutsurpg_reinforcement_tick";
    public static final int DURATION_TICKS = 200;
    public static void activate(ServerPlayer player, boolean defense) {
        if (!player.isAlive() || player.isSpectator()) return;
        long now = player.getServer().overworld().getGameTime();
        var tags = player.getPersistentData();
        if (tags.contains(LAST_USE) && now - tags.getLong(LAST_USE) < 10) return;
        tags.putLong(LAST_USE, now);
        var data = CursedEnergy.get(player);
        double available = data.current(player);
        // Prevent arbitrarily tiny reserves from buying the same full-strength buff.
        if (available < 2) {
            player.displayClientMessage(Component.literal("Нужно хотя бы 2 ПЭ для усиления"), true);
            return;
        }
        var effect = defense ? MobEffects.DAMAGE_RESISTANCE : MobEffects.DAMAGE_BOOST;
        var existing = player.getEffect(effect);
        if (existing != null && (existing.getAmplifier() > 0 || existing.isInfiniteDuration()
                || existing.getDuration() >= DURATION_TICKS - 10)) return;
        if (!data.trySpend(player, available / 2)) return;
        player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, 0, false, true, true));
        player.displayClientMessage(Component.literal((defense ? "Защита" : "Сила")
            + " усилена на 10 секунд. Потрачено "
            + String.format(java.util.Locale.ROOT, "%.1f", available / 2) + " ПЭ"), true);
    }
}
