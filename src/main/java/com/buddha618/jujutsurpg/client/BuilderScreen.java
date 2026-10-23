package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.builder.BuilderNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

public final class BuilderScreen extends Screen {
    private static String[] files={"technical_college.nbt"};
    private static int selected,rotation;
    private static boolean aligned;
    private static BlockPos origin;
    private EditBox x,y,z;
    public BuilderScreen(){
        super(Component.literal("Строитель — библиотека NBT"));
        if(origin==null||!BuilderGhost.ready)origin=nearPlayer();
    }
    private static BlockPos nearPlayer(){
        var p=net.minecraft.client.Minecraft.getInstance().player;
        if(p==null)return BlockPos.ZERO;
        var direction=p.getDirection();
        return p.blockPosition().offset(direction.getStepX()*8,-1,direction.getStepZ()*8);
    }
    private boolean capture(){
        try {if(x!=null)origin=new BlockPos(Integer.parseInt(x.getValue()),Integer.parseInt(y.getValue()),Integer.parseInt(z.getValue()));return true;}
        catch(NumberFormatException ex){return false;}
    }
    private String previewAction(){return aligned?"aligned":"preview";}
    public static void library(String list){files=list.isEmpty()?new String[0]:list.split("\n");selected=Math.min(selected,Math.max(0,files.length-1));if(net.minecraft.client.Minecraft.getInstance().screen instanceof BuilderScreen s)s.rebuildWidgets();}
    private void request(String action){
        try {if(x!=null)origin=new BlockPos(Integer.parseInt(x.getValue()),Integer.parseInt(y.getValue()),Integer.parseInt(z.getValue()));}
        catch(NumberFormatException ex){return;}
        BuilderNetwork.CHANNEL.sendToServer(new BuilderNetwork.Request(action,files.length==0?"":files[selected],origin,rotation));
    }
    private void button(String label,int top,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(width/2-145,top,290,20).build());}
    @Override protected void init(){
        int top=40;
        button("Обновить библиотеку",top,()->request("list"));
        button(files.length==0?"Нет файлов":files[selected],top+24,()->{if(!capture())return;selected=(selected+1)%Math.max(1,files.length);BuilderGhost.ready=false;rebuildWidgets();});
        x=field(width/2-145,top+50,origin.getX());y=field(width/2-45,top+50,origin.getY());z=field(width/2+55,top+50,origin.getZ());
        small("Поворот: "+rotation*90+"°",-145,top+76,()->{rotation=(rotation+1)%4;BuilderGhost.ready=false;request(previewAction());rebuildWidgets();});
        small("Позиция у игрока",5,top+76,()->{origin=nearPlayer();BuilderGhost.ready=false;rebuildWidgets();request(previewAction());});
        small("Предпросмотр",-145,top+100,()->{BuilderGhost.ready=false;request(previewAction());});
        small("Осмотреть",5,top+100,this::onClose);
        small("ПОСТРОИТЬ",-145,top+124,()->{if(BuilderGhost.ready)request("build");});
        small("Отменить постройку",5,top+124,()->request("undo"));
        small("Убрать просмотр",-145,top+148,()->request("clear"));
        small("Закрыть",5,top+148,this::onClose);
        button(aligned?"Режим: общий угол модулей":"Режим: рядом, по границам схемы",top+172,()->{
            if(!capture())return;aligned=!aligned;BuilderGhost.ready=false;rebuildWidgets();
        });
    }
    private void small(String text,int left,int top,Runnable action){addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(width/2+left,top,140,20).build());}
    private EditBox field(int left,int top,int value){var box=new EditBox(font,left,top,90,20,Component.literal("Координата"));box.setMaxLength(10);box.setValue(Integer.toString(value));box.setResponder(s->BuilderGhost.ready=false);addRenderableWidget(box);return box;}
    @Override public void render(GuiGraphics g,int mx,int my,float t){renderBackground(g);g.drawCenteredString(font,title,width/2,10,0xC6D8FF);g.drawCenteredString(font,"X / Y / Z — ближайший угол. "+(BuilderGhost.ready?"Предпросмотр готов":"Нажмите предпросмотр"),width/2,26,0xFFFFFF);super.render(g,mx,my,t);}
    @Override public boolean isPauseScreen(){return false;}
}
