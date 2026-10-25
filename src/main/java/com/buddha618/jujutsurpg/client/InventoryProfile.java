package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID,value=Dist.CLIENT)
public final class InventoryProfile {
    private static CharacterNetwork.Profile profile;
    private static boolean inventory(Screen s){return s instanceof InventoryScreen||s instanceof CreativeModeInventoryScreen;}
    @SubscribeEvent public static void receive(CharacterNetwork.ProfileEvent e){profile=e.profile;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){profile=null;}
    @SubscribeEvent public static void init(ScreenEvent.Init.Post e){
        if(!inventory(e.getScreen()))return;
        CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Request());
        var screen=e.getScreen();
        e.addListener(Button.builder(Component.literal("Профиль мага"),b->{
            Minecraft.getInstance().setScreen(new CharacterScreen());
            CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Request());
        }).bounds(4,screen.height-22,106,20).build());
    }
    private static String technique(String id){return com.buddha618.jujutsurpg.genetics.InnateTechnique.displayName(id);}
    @SubscribeEvent public static void render(ScreenEvent.Render.Post e){
        if(!inventory(e.getScreen()))return;
        var screen=(AbstractContainerScreen<?>)e.getScreen();var mc=Minecraft.getInstance();var g=e.getGuiGraphics();
        int right=screen.getGuiLeft()+screen.getXSize()+6;
        int left=screen.getGuiLeft()-134;
        List<String> rows=new ArrayList<>();
        rows.add(profile==null?"Ожидание сервера…":"Клан: "+profile.clan());
        if(profile!=null)rows.add("Техника: "+technique(profile.technique()));
        rows.add(EnergyHud.description());
        if(profile!=null&&profile.technique().equals("limitless")){
            rows.add(CharacterClient.BLUE.getTranslatedKeyMessage().getString()+" — Синий");
            rows.add(CharacterClient.RED.getTranslatedKeyMessage().getString()+" — Красный");
            rows.add(CharacterClient.PURPLE.getTranslatedKeyMessage().getString()+" — Фиолетовый");
        } else if(profile!=null&&!profile.technique().equals("hidden"))rows.add("Усиление силы и защиты: K");
        int x=left>=2?left:right+128<=screen.width?right:-1;
        if(x<0){
            // Compact layouts: keep slots clear; full information is available via the profile button.
            String line=rows.get(0)+(profile==null?"":" • "+technique(profile.technique()));
            g.fill(2,screen.height-36,screen.width-2,screen.height-25,0xD015192B);
            g.drawString(mc.font,mc.font.plainSubstrByWidth(line,screen.width-8),4,screen.height-35,0xB9DFFF,false);
            return;
        }
        int y=Math.max(6,screen.getGuiTop());
        List<net.minecraft.util.FormattedCharSequence> lines=new ArrayList<>();
        for(String row:rows)lines.addAll(mc.font.split(Component.literal(row),114));
        int h=28+lines.size()*11;
        g.fill(x,y,x+128,y+h,0xEE111A2C);g.fill(x,y,x+2,y+h,0xFF39BDFF);
        g.drawString(mc.font,"ПРОФИЛЬ МАГА",x+8,y+8,0x70D7FF,false);
        int yy=y+25;
        for(var line:lines){g.drawString(mc.font,line,x+8,yy,0xE4EEFF,false);yy+=11;}
    }
}
