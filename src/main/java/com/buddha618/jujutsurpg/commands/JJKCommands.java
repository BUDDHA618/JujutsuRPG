
package com.buddha618.jujutsurpg.commands;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = JujutsuRPGMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class JJKCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("jjk")
                        .then(Commands.literal("test-awaken")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> {
                                var player = context.getSource().getPlayerOrException();
                                boolean completed = GeneticsSavedData.get(player.getServer()).completeAwakening(player.getUUID());
                                player.sendSystemMessage(Component.literal(completed
                                    ? "Пробуждение завершено. Откройте меню K или /jjk genetics."
                                    : "Профиль ещё не создан."));
                                return completed ? 1 : 0;
                            }))
                        .then(Commands.literal("genetics")
                                .executes(context -> {

                                    ServerPlayer player = context.getSource()
                                            .getPlayerOrException();

                                    var profile = GeneticsSavedData
                                            .get(context.getSource().getServer())
                                            .getOrCreate(player.getUUID());

                                    player.sendSystemMessage(
                                            Component.literal(
                                                    "Клан: " +
                                                            profile.clan().getDisplayName()
                                                            + " | Техника: " +
                                                            GeneticsSavedData.get(player.getServer()).visibleTechnique(player.getUUID())
                                            )
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
