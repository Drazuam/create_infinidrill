package com.latenighters.infinidrill.capabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Set;

public interface IOreCountHandler {

    public Integer countBlocksOfType(Block block, Boolean naturalOnly);
    public LazyOptional<Integer> lazyCountBlocksOfType(Block block, Boolean naturalOnly);
    public void markOrePlaced(BlockPos pos);
    public Set<BlockPos> getPlacedOres();
    public void setScanResult(Block block, Integer count);
    public Boolean isNaturallyPlaced(BlockPos blockPos);
    public int getPlacedSetHashcode();
    public void setPlacedOres(Set<BlockPos> placedOres);
}
