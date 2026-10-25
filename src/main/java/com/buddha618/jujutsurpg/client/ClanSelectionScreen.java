package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.clans.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class ClanSelectionScreen extends Screen {
    private String selected="gojo";
    private boolean confirm;
    private int scroll, maxScroll;
    public ClanSelectionScreen(){super(Component.literal("Создание персонажа: происхождение"));}
    @Override protected void init(){
        int y=30, spacing=Math.min(22,Math.max(14,(height-65)/8));
        for(var d:ClanRegistry.all()) {
            addRenderableWidget(Button.builder(Component.literal(d.clan().displayName()),b->{selected=d.clan().id();confirm=false;scroll=0;rebuildWidgets();})
                .bounds(8,y,122,spacing-2).build());y+=spacing;
        }
        addRenderableWidget(Button.builder(Component.literal(confirm?"Подтвердить навсегда":"Выбрать происхождение"),b->{
            if(!confirm){confirm=true;rebuildWidgets();}else ClanNetwork.CHANNEL.sendToServer(new ClanNetwork.Choose(selected));
        }).bounds(Math.max(136,width/2-10),height-28,Math.min(180,width-144),20).build());
        addRenderableWidget(Button.builder(Component.literal("Выйти"),b->{
            if(minecraft.level!=null)minecraft.level.disconnect();minecraft.clearLevel();minecraft.setScreen(new TitleScreen());
        }).bounds(8,height-28,122,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.drawCenteredString(font,title,width/2,10,0xFFFFFF);
        var d=ClanRegistry.find(selected).orElseThrow();int x=140,y=32-scroll;
        String[] rows={d.clan().displayName()+" — "+d.group(),d.description(),d.heritageDescription(),
            "Школы: "+String.join(", ",d.schools().stream().map(s->s.title()).toList()),
            "Сильная сторона: "+d.strengths(),"Ограничения: "+d.limits(),
            "Старт: клановое мастерство1, общее усиление CE и начальное упражнение.",
            "Врождённая техника скрыта до пробуждения. Выбор нельзя повторить после подтверждения."};
        g.enableScissor(x,28,width-6,height-34);
        for(String row:rows){for(var line:font.split(Component.literal(row),Math.max(100,width-x-12))){g.drawString(font,line,x,y,0xDDDDDD,false);y+=10;}y+=5;}
        maxScroll=Math.max(0,y+scroll-(height-42));
        g.disableScissor();
        if(maxScroll>0)g.drawString(font,"Колесо мыши: описание",140,height-40,0xAAAAAA,false);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*20)));return true;}
    @Override public boolean shouldCloseOnEsc(){return false;}
    @Override public void onClose(){}
    @Override public boolean isPauseScreen(){return false;}
}
