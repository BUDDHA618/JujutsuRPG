package com.buddha618.jujutsurpg.commands;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JujutsuRPGMod.MODID)
public final class SeasonCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jjkseason").requires(s -> s.hasPermission(2))
            .then(Commands.literal("status").executes(c -> {
                var data = GeneticsSavedData.get(c.getSource().getServer());
                c.getSource().sendSuccess(() -> Component.literal("Сезон " + data.seasonNumber()
                    + "; участников: " + data.seasonCandidates() + "; Шесть глаз: "
                    + data.sixEyesOwner().map(Object::toString).orElse("отбор не проведён")), false);
                return 1;
            }))
            .then(Commands.literal("enroll").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                var data = GeneticsSavedData.get(c.getSource().getServer());
                var player = EntityArgument.getPlayer(c, "player");
                boolean ok = data.enrollSixEyes(player.getUUID());
                c.getSource().sendSuccess(() -> Component.literal(ok ? "Участник зарегистрирован."
                    : "Отказ: нужен клан Годжо, открытый отбор и отсутствие повторной регистрации."), true);
                return ok ? 1 : 0;
            })))
            .then(Commands.literal("draw").executes(c -> {
                var selected = GeneticsSavedData.get(c.getSource().getServer()).drawSixEyes();
                c.getSource().sendSuccess(() -> Component.literal(selected.map(id -> "Владелец Шести глаз: " + id)
                    .orElse("Отбор не выполнен: слот занят или нет совместимых участников.")), true);
                return selected.isPresent() ? 1 : 0;
            }))
            .then(Commands.literal("next").then(Commands.argument("currentSeason", LongArgumentType.longArg(1)).executes(c -> {
                boolean changed = GeneticsSavedData.get(c.getSource().getServer())
                    .advanceSeason(LongArgumentType.getLong(c, "currentSeason"));
                c.getSource().sendSuccess(() -> Component.literal(changed
                    ? "Новый сезон начат. Прошлые права архивированы; генетика сохранена."
                    : "Номер текущего сезона не совпадает."), true);
                return changed ? 1 : 0;
            }))));
    }
}
