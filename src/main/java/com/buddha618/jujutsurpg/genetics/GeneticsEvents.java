
package com.buddha618.jujutsurpg.genetics;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = JujutsuRPGMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class GeneticsEvents {

    @SubscribeEvent
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        GeneticsSavedData data = GeneticsSavedData.get(server);

        boolean firstLogin = !data.hasProfile(player.getUUID());

        GeneticsSavedData.GeneticProfile profile =
                data.getOrCreate(player.getUUID());


        String message = firstLogin
                ? "Генетика определена! "
                : "Твоя генетика: ";

        player.sendSystemMessage(Component.literal(
                message
                        + "Клан: " + profile.clan().getDisplayName()
                        + " | Техника: " + profile.technique().getId()
        ));
    }
}
