package com.buddha618.jujutsurpg.builder;

import com.buddha618.jujutsurpg.JujutsuRPGMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Mod.EventBusSubscriber(modid=JujutsuRPGMod.MODID)
public final class BuilderService {
    private record Entry(BlockPos pos,BlockState state) {}
    private record Change(BlockPos pos,BlockState before,BlockState after) {}
    private record Plan(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,List<Entry> blocks) {}
    private static final Map<UUID,Plan> PLANS=new HashMap<>();
    private record Undo(Plan plan,List<Change> changes) {}
    private static final Map<UUID,Undo> UNDO=new HashMap<>();
    private static final Map<UUID,Long> LAST=new HashMap<>();
    private static Job job;
    private static final class Job {
        final ServerPlayer owner;final Plan plan;final boolean undo;
        final List<Change> changes;int index,skipped;
        Job(ServerPlayer p,Plan plan,boolean undo,List<Change> changes){owner=p;this.plan=plan;this.undo=undo;this.changes=changes;}
    }
    private static Path folder() throws IOException {
        Path p=FMLPaths.GAMEDIR.get().resolve("config/jujutsurpg/buildings");Files.createDirectories(p);
        Path sample=p.resolve("technical_college.nbt");
        if(!Files.exists(sample))try(var in=BuilderService.class.getResourceAsStream("/data/jujutsurpg/structures/technical_college/campus.nbt")){if(in!=null)Files.copy(in,sample);}
        return p;
    }
    private static void tell(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("Строитель: "+s));}
    private static void send(ServerPlayer p,Object packet){BuilderNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet);}
    public static void handle(ServerPlayer p,BuilderNetwork.Request m) {
        if(!p.hasPermissions(2))return;
        try {
            long now=p.serverLevel().getGameTime();
            if(now-LAST.getOrDefault(p.getUUID(),-100L)<5)return;
            LAST.put(p.getUUID(),now);
            if(m.action().equals("list")) {
                try(var stream=Files.list(folder())) {send(p,new BuilderNetwork.Library(String.join("\n",stream.filter(f->Files.isRegularFile(f,LinkOption.NOFOLLOW_LINKS)&&f.getFileName().toString().matches("[a-zA-Z0-9_-]+\\.nbt")).map(f->f.getFileName().toString()).sorted().limit(100).toList())));}
            } else if(m.action().equals("clear")) {
                PLANS.remove(p.getUUID());send(p,new BuilderNetwork.Preview(true,true,new long[0],new int[0]));
            } else if(m.action().equals("preview")) {
                if(job!=null) {tell(p,"Дождитесь завершения текущей операции.");return;}
                PLANS.remove(p.getUUID());send(p,new BuilderNetwork.Preview(true,false,new long[0],new int[0]));
                if(!m.file().matches("[a-zA-Z0-9_-]+\\.nbt")||m.rotation()>3)throw new IOException("Неверное имя/поворот");
                Path path=folder().resolve(m.file());if(Files.isSymbolicLink(path)||Files.size(path)>8_000_000)throw new IOException("Файл слишком большой или ссылка");
                CompoundTag tag;
                try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))){tag=NbtIo.read(in,new NbtAccounter(128_000_000));}
                var palette=tag.getList("palette",Tag.TAG_COMPOUND);var blocks=tag.getList("blocks",Tag.TAG_COMPOUND);
                var size=tag.getList("size",Tag.TAG_INT);
                if(size.size()!=3 || blocks.size()>500000 || palette.isEmpty())throw new IOException("Неподдерживаемый NBT");
                for(int i=0;i<3;i++)if(size.getInt(i)<1||size.getInt(i)>256)throw new IOException("Предел стороны: 256 блоков");
                var rotation=Rotation.values()[m.rotation()];List<Entry> entries=new ArrayList<>();
                var registry=p.serverLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK);
                for(int i=0;i<blocks.size();i++) {
                    var block=blocks.getCompound(i);int state=block.getInt("state");
                    if(state<0||state>=palette.size())throw new IOException("Индекс палитры");
                    var xyz=block.getList("pos",Tag.TAG_INT);if(xyz.size()!=3)throw new IOException("Координаты");
                    for(int n=0;n<3;n++)if(xyz.getInt(n)<0||xyz.getInt(n)>=size.getInt(n))throw new IOException("Блок вне размеров");
                    var bs=NbtUtils.readBlockState(registry,palette.getCompound(state)).rotate(rotation);
                    if(bs.isAir())continue; // Additive import: never carve a giant air box.
                    if(block.contains("nbt") || bs.hasBlockEntity() || bs.getBlock() instanceof net.minecraft.world.level.block.CommandBlock)continue;
                    var relative=new BlockPos(xyz.getInt(0),xyz.getInt(1),xyz.getInt(2)).rotate(rotation);
                    var absolute=m.origin().offset(relative);
                    if(!p.serverLevel().getWorldBorder().isWithinBounds(absolute)||p.serverLevel().isOutsideBuildHeight(absolute)||absolute.distSqr(p.blockPosition())>256*256)throw new IOException("Постройка вне высоты/границы/256 блоков от игрока");
                    entries.add(new Entry(absolute,bs));
                    if(entries.size()>200000)throw new IOException("Лимит:200000 непустых блоков");
                }
                if(entries.isEmpty())throw new IOException("Нет подходящих блоков");
                PLANS.put(p.getUUID(),new Plan(p.level().dimension(),List.copyOf(entries)));
                for(int start=0;start<entries.size();start+=2048) {
                    int n=Math.min(2048,entries.size()-start);long[] positions=new long[n];int[] states=new int[n];
                    for(int i=0;i<n;i++){var b=entries.get(start+i);positions[i]=b.pos().asLong();states[i]=Block.getId(b.state());}
                    send(p,new BuilderNetwork.Preview(false,start+n==entries.size(),positions,states));
                }
                tell(p,"Предпросмотр: "+entries.size()+" блоков. Мир не изменён. Воздух, NPC и блоки с данными не импортируются.");
            } else if(m.action().equals("build")) {
                if(job!=null){tell(p,"Операция уже выполняется.");return;}
                var plan=PLANS.get(p.getUUID());if(plan==null||plan.dimension()!=p.level().dimension()){tell(p,"Сначала обновите предпросмотр.");return;}
                job=new Job(p,plan,false,new ArrayList<>());UNDO.remove(p.getUUID());tell(p,"Начато размещение.");
            } else if(m.action().equals("undo")) {
                if(job!=null){tell(p,"Дождитесь завершения операции.");return;}
                var undo=UNDO.get(p.getUUID());var plan=undo==null?null:undo.plan();var changes=undo==null?null:undo.changes();
                if(changes==null||plan==null||plan.dimension()!=p.level().dimension()){tell(p,"Нет доступной отмены в этом измерении.");return;}
                UNDO.remove(p.getUUID());job=new Job(p,plan,true,changes);tell(p,"Начата отмена.");
            }
        } catch(Exception ex){tell(p,"Ошибка: "+ex.getMessage());send(p,new BuilderNetwork.Preview(true,true,new long[0],new int[0]));}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||job==null)return;
        var j=job;var p=j.owner;
        if(!p.hasPermissions(2)||p.hasDisconnected()||p.level().dimension()!=j.plan.dimension()) {finish(j);return;}
        var level=p.serverLevel();int total=j.undo?j.changes.size():j.plan.blocks().size();
        for(int count=0;count<512&&j.index<total;count++,j.index++) {
            var entry=j.undo?null:j.plan.blocks().get(j.index);
            var change=j.undo?j.changes.get(j.changes.size()-1-j.index):null;
            var pos=j.undo?change.pos():entry.pos();
            if(!level.hasChunkAt(pos)||!level.mayInteract(p,pos)||!p.mayBuild()||!level.getWorldBorder().isWithinBounds(pos)){j.skipped++;continue;}
            var before=level.getBlockState(pos);var after=j.undo?change.before():entry.state();
            if(before.hasBlockEntity()||before.getDestroySpeed(level,pos)<0||(j.undo&&!before.equals(change.after()))) {j.skipped++;continue;}
            if(before.equals(after))continue;
            if(ForgeHooks.onBlockBreakEvent(level,p.gameMode.getGameModeForPlayer(),p,pos)<0){j.skipped++;continue;}
            var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
            if(level.setBlock(pos,after,2)) {
                if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(p,snapshot,net.minecraft.core.Direction.UP)) {
                    snapshot.restore(true,false);j.skipped++;continue;
                }
                if(!j.undo)j.changes.add(new Change(pos,before,after));
            }
        }
        if(j.index>=total)finish(j);
    }
    private static void finish(Job j){
        if(!j.undo)UNDO.put(j.owner.getUUID(),new Undo(j.plan,List.copyOf(j.changes)));
        send(j.owner,new BuilderNetwork.Preview(true,true,new long[0],new int[0]));
        tell(j.owner,(j.undo?"Отмена":"Размещение")+" завершено; пропущено: "+j.skipped+". Отмена хранится до выхода/перезапуска.");job=null;
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        UUID id=e.getEntity().getUUID();PLANS.remove(id);
        if(e.getEntity() instanceof ServerPlayer p)send(p,new BuilderNetwork.Preview(true,true,new long[0],new int[0]));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id=e.getEntity().getUUID();if(job!=null&&job.owner.getUUID().equals(id))job=null;
        PLANS.remove(id);UNDO.remove(id);LAST.remove(id);
    }
}
