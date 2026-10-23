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
        System.out.println("Builder NBT check passed: "+solid+" non-air blocks, bounded read + all4 rotations");
    }
}
