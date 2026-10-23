package com.buddha618.jujutsurpg;
import net.minecraft.nbt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
public final class ReferenceBundleCheck {
 public static void main(String[] args)throws Exception {
  int count=0;
  try(var files=Files.list(Path.of(args[0]))) {
   for(var path:files.filter(p->p.toString().endsWith(".nbt")).sorted().toList()) {
    CompoundTag tag;
    try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))){tag=NbtIo.read(in,new NbtAccounter(128_000_000));}
    var size=tag.getList("size",Tag.TAG_INT);var blocks=tag.getList("blocks",Tag.TAG_COMPOUND);var palette=tag.getList("palette",Tag.TAG_COMPOUND);
    if(size.getInt(0)!=240||size.getInt(1)!=64||size.getInt(2)!=240||blocks.size()>200000)throw new AssertionError(path);
    Set<String> positions=new HashSet<>();
    for(int i=0;i<blocks.size();i++) {
     var b=blocks.getCompound(i);var pos=b.getList("pos",Tag.TAG_INT);
     if(!positions.add(pos.toString()))throw new AssertionError("Duplicate position");
     for(int n=0;n<3;n++)if(pos.getInt(n)<0||pos.getInt(n)>=size.getInt(n))throw new AssertionError("Out of bounds");
     if(b.getInt("state")<0||b.getInt("state")>=palette.size())throw new AssertionError("Palette");
    }
    System.out.println(path.getFileName()+": "+blocks.size()+" blocks OK");count++;
   }
  }
  if(count!=7)throw new AssertionError("Expected7 modules");
 }
}
