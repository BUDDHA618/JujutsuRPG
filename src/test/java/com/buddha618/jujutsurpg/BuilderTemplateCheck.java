package com.buddha618.jujutsurpg;

import net.minecraft.nbt.*;
import java.nio.file.*;
import java.io.*;
import java.util.zip.GZIPInputStream;

/** Real bounded NBT read, using the same importer budget; no game client required. */
public final class BuilderTemplateCheck {
    public static void main(String[] args) throws Exception {
        CompoundTag tag;
        try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(Path.of(args[0]))))) {
            tag=NbtIo.read(in,new NbtAccounter(128_000_000));
        }
        var size=tag.getList("size",Tag.TAG_INT);
        if(size.getInt(0)!=112 || size.getInt(2)!=112)throw new AssertionError("Campus size");
        var palette=tag.getList("palette",Tag.TAG_COMPOUND);
        var blocks=tag.getList("blocks",Tag.TAG_COMPOUND);
        int solid=0;
        for(int i=0;i<blocks.size();i++) {
            var block=blocks.getCompound(i);int state=block.getInt("state");
            if(state<0||state>=palette.size())throw new AssertionError("Bad palette index");
            if(!palette.getCompound(state).getString("Name").equals("minecraft:air"))solid++;
        }
        if(solid>200000 || blocks.size()>500000)throw new AssertionError("Campus exceeds importer limits");
        for(int turns=0;turns<4;turns++) {
            int x=111,z=111;
            for(int t=0;t<turns;t++){int old=x;x=-z;z=old;}
            if(Math.abs(x)>256||Math.abs(z)>256)throw new AssertionError("Rotation range");
        }
        var source=java.util.List.of(new net.minecraft.core.BlockPos(78,2,124),new net.minecraft.core.BlockPos(140,45,160));
        for(var rotation:net.minecraft.world.level.block.Rotation.values()) {
            var rotated=source.stream().map(pos->pos.rotate(rotation)).toList();
            var anchor=new net.minecraft.core.BlockPos(-300,70,900);
            var shift=com.buddha618.jujutsurpg.builder.BuilderPlacement.shiftToAnchor(rotated,anchor);
            var placed=rotated.stream().map(pos->pos.offset(shift)).toList();
            if(placed.stream().mapToInt(net.minecraft.core.BlockPos::getX).min().orElseThrow()!=anchor.getX()
                || placed.stream().mapToInt(net.minecraft.core.BlockPos::getY).min().orElseThrow()!=anchor.getY()
                || placed.stream().mapToInt(net.minecraft.core.BlockPos::getZ).min().orElseThrow()!=anchor.getZ())
                throw new AssertionError("Offset/negative-world/rotation regression: "+rotation);
            if(!placed.get(1).subtract(placed.get(0)).equals(rotated.get(1).subtract(rotated.get(0))))
                throw new AssertionError("Geometry changed");
        }
        System.out.println("Placement anchor regression passed, all4 rotations, source offsets and negative world coordinates");
        System.out.println("Builder NBT check passed: "+solid+" non-air blocks, bounded read + all4 rotations");
    }
}
