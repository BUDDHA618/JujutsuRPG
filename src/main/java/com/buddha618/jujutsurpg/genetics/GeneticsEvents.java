package com.buddha618.jujutsurpg.genetics;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.clans.ClanNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID)
public final class GeneticsEvents {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            var data=GeneticsSavedData.get(p.getServer());
            if(data.hasProfile(p.getUUID()))data.getProfile(p.getUUID()); // Only migrate existing profiles.
            ClanNetwork.send(p,"");
        }
    }
}
