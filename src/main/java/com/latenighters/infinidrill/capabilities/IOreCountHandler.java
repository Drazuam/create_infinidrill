package com.latenighters.infinidrill.capabilities;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.LazyOptional;

public interface IOreCountHandler {

    public Integer countBlocksOfType(Block block);
    public LazyOptional<Integer> lazyCountBlocksOfType(Block block);
}
