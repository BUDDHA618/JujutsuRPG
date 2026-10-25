package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.clans.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClanScreen extends Screen {
    private int ticks,scroll,maxScroll;
    public ClanScreen(){super(Component.literal("Клановое обучение"));}
    public void refresh(){rebuildWidgets();}
    @Override protected void init(){
        var v=ClanClient.view;int left=width/2-150;
        addRenderableWidget(Button.builder(Component.literal("Упражнение"),b->ClanClient.send("train","")).bounds(left,height-74,96,20).build());
        addRenderableWidget(Button.builder(Component.literal("Экзамен"),b->ClanClient.send("exam","")).bounds(left+102,height-74,96,20).build());
        addRenderableWidget(Button.builder(Component.literal("Отменить"),b->ClanClient.send("cancel","")).bounds(left+204,height-74,96,20).build());
        if(v!=null&&!v.required()) {
            var clan=ClanRegistry.find(v.clan()).orElseThrow();
            for(int i=0;i<clan.schools().size();i++) {
                var school=clan.schools().get(i);
                var button=Button.builder(Component.literal((v.school().equals(school.id())?"✓ ":"")+school.title()),b->ClanClient.send("school",school.id()))
                    .bounds(left+i*102,height-50,96,20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        school.implemented()?"Уровень "+school.level()+", репутация "+school.reputation()+". Выбор меняет учебные задания.":"Закрыто: требуется отдельная реализация техники и её мастерства."))).build();
                button.active=v.available().contains(school.id())&&v.sequence()==0;addRenderableWidget(button);
            }
        }
        addRenderableWidget(Button.builder(Component.literal("В мир — выполнять задание"),b->onClose()).bounds(width/2-110,height-26,220,20).build());
    }
    @Override public void tick(){if(++ticks%20==0)ClanNetwork.CHANNEL.sendToServer(new ClanNetwork.Request());}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.drawCenteredString(font,title,width/2,12,0xFFFFFF);
        var v=ClanClient.view;int x=Math.max(12,width/2-150),y=36-scroll;
        if(v!=null&&!v.required()) {
            var clan=ClanRegistry.find(v.clan()).orElseThrow();
            String[] rows={clan.clan().displayName()+" • Мастерство: "+v.level()+" • Репутация: "+v.reputation(),
                "Клановый XP: "+v.xp()+" / "+v.needed()+(v.exam()>0?" • Доступен экзамен "+v.exam():""),
                clan.exercise(),v.task().replace("V (переназначается)",ClanClient.STEP.getTranslatedKeyMessage().getString()),
                v.message(),"Точки появляются рядом на открытой площадке. Урон, выход и смена измерения прерывают упражнение."};
            g.enableScissor(x,32,width-x,height-80);
            for(String row:rows){for(var line:font.split(Component.literal(row),Math.min(300,width-24))){g.drawString(font,line,x,y,0xDDDDDD,false);y+=11;}y+=4;}
            maxScroll=Math.max(0,y+scroll-(height-84));
            g.disableScissor();
        }else g.drawCenteredString(font,"Ожидание профиля…",width/2,45,0xFFFFFF);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*20)));return true;}
    @Override public boolean isPauseScreen(){return false;}
}
