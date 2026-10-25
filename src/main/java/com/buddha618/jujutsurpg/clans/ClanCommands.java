package com.buddha618.jujutsurpg.clans;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.genetics.GeneticsSavedData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID)
public final class ClanCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e) {
        var root=Commands.literal("jjkclan");
        for(String action:new String[]{"train","exam","cancel","step","status"})root.then(Commands.literal(action).executes(c->{
            var p=c.getSource().getPlayerOrException();var data=GeneticsSavedData.get(p.getServer());
            if(!data.hasProfile(p.getUUID())){ClanNetwork.send(p,"Сначала выберите происхождение.");return 0;}
            if(action.equals("status")) {
                var progress=data.progression(p.getUUID());p.sendSystemMessage(Component.literal("Клан: "+data.getProfile(p.getUUID()).clan().displayName()
                    +" | Мастерство: "+progress.level()+" | XP: "+progress.xp()+"/"+progress.requirement()+" | Репутация: "+progress.reputation()));
            } else {
                var task=ClanTrainingService.describe(p);
                ClanTrainingService.action(p,new ClanNetwork.Action(action,"",task.sequence(),task.nonce()));
            }
            return 1;
        }));
        root.then(Commands.literal("migrate").requires(s->s.hasPermission(2))
            .then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("origin",StringArgumentType.word())
                .suggests((c,b)->{for(var d:ClanRegistry.all())b.suggest(d.clan().id());return b.buildFuture();})
                .executes(c->{
                    var p=EntityArgument.getPlayer(c,"player");
                    boolean changed=GeneticsSavedData.get(p.getServer()).migrateLegacyOrigin(p.getUUID(),StringArgumentType.getString(c,"origin"));
                    c.getSource().sendSuccess(()->Component.literal(changed?"Происхождение изменено один раз. Техника сохранена; прежний профиль архивирован."
                        :"Отказ: допустим только прежний профиль без использованной миграции."),true);
                    ClanNetwork.send(p,changed?"Администратор перенёс старый профиль. Врождённая техника сохранена.":"");
                    return changed?1:0;
                }))));
        e.getDispatcher().register(root);
    }
}
