
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
                                                            profile.technique().getId()
                                            )
                                    );

                                    return 1;
                                })
                        )
        );
    }
}
