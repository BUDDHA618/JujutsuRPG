package com.buddha618.jujutsurpg.builder;
import net.minecraft.core.BlockPos;
import java.util.List;
public final class BuilderPlacement {
    public static BlockPos shiftToAnchor(List<BlockPos> positions,BlockPos anchor) {
        int x=Integer.MAX_VALUE,y=Integer.MAX_VALUE,z=Integer.MAX_VALUE;
        for(var pos:positions){x=Math.min(x,pos.getX());y=Math.min(y,pos.getY());z=Math.min(z,pos.getZ());}
        if(positions.isEmpty())throw new IllegalArgumentException("Empty structure");
        return anchor.subtract(new BlockPos(x,y,z));
    }
}
