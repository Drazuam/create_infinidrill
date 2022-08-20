package com.latenighters.infinidrill.capabilities;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.latenighters.infinidrill.InfiniDrillConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.http.concurrent.BasicFuture;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class OreFinderTask extends Thread {

    private final List<LevelChunk> chunks;
    private final Block target;
    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Set<BlockPos> ignoreList;
    private final List<TagKey<Block>> tags;

    public void cancel() {
        cancelled.set(true);
    }

    private static class CountedOres extends CompletableFuture<Integer>{
        public void setFinalCount(Integer finalCount){
            this.complete(finalCount);
        }
    };
    CountedOres countedOres = new CountedOres();

    private final List<Consumer<Integer>> callbacks = new ArrayList<>();


    public OreFinderTask(List<LevelChunk> chunks, Block target, Set<BlockPos> ignoreList) {
        this.chunks = chunks;
        this.target = target;
        if(ignoreList!=null)
            this.ignoreList = ignoreList;
        else
            this.ignoreList = new HashSet<>();

        tags = InfiniDrillConfig.getTagsFor(target.defaultBlockState());
    }

    public OreFinderTask(List<LevelChunk> chunks, Block target) {
        this(chunks, target, null);
    }

    @Override
    public void run() {


        List<Block> cachedNonMatch = new ArrayList<>();
        List<Block> cachedMatch = new ArrayList<>();

        for (LevelChunk chunk:chunks) {
            BlockPos chunkPos = chunk.getPos().getWorldPosition();
            for(int y=-63; y<128; y++){
                if(cancelled.get()) {
                    finish(count.get());
                    return;
                }
                for(int x=0; x<16; x++){
                    for(int z=0; z<16; z++){
                        BlockState blockState = chunk.getBlockState(new BlockPos(x, y, z));

                        //check if we've already matched this block
                        if(cachedMatch.contains(blockState.getBlock())){
                            if (!ignoreList.contains((new BlockPos(x, y, z)).offset(chunkPos)))
                                count.getAndIncrement();
                        }

                        //also check if we've already denied this block
                        else if(cachedNonMatch.contains(blockState.getBlock())){
                            //do  nothing
                        }

                        //if we haven't seen it, check if it's a match and update the cache
                        else if(blockState.getBlock().equals(target)) {
                            if (!ignoreList.contains((new BlockPos(x, y, z)).offset(chunkPos))) {
                                count.getAndIncrement();
                                cachedMatch.add(blockState.getBlock());
                            }
                            else{
                                cachedNonMatch.add(blockState.getBlock());
                            }
                        }

                        //if it's not a direct match, check the equivalent tags
                        else{
                            AtomicBoolean tagMatch = new AtomicBoolean(false);
                            tags.forEach(blockTagKey -> {
                                if (blockState.is(blockTagKey))
                                    tagMatch.set(true);
                            });
                            if (tagMatch.get() && !ignoreList.contains((new BlockPos(x, y, z)).offset(chunkPos))) {
                                count.getAndIncrement();
                                cachedMatch.add(blockState.getBlock());
                            }
                            else{
                                cachedNonMatch.add(blockState.getBlock());
                            }
                        }
                    }
                }
            }
        }
        finish(count.get());
    }

    public Integer get() throws ExecutionException, InterruptedException {
        return countedOres.get();
    }

    public OreFinderTask addCallback(Consumer<Integer> callback){
        if(!callbacks.contains(callback))
            callbacks.add(callback);
        return this;
    }

    private void finish(Integer finalCount){
        callbacks.forEach(callback->{
            callback.accept(finalCount);
        });
        countedOres.setFinalCount(finalCount);
    }
}
