package com.latenighters.infinidrill.capabilities;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.http.concurrent.BasicFuture;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class OreFinderTask extends Thread {

    private final List<LevelChunk> chunks;
    private final Block target;
    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

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


    public OreFinderTask(List<LevelChunk> chunks, Block target) {
        this.chunks = chunks;
        this.target = target;
    }

    @Override
    public void run() {
        for (LevelChunk chunk:chunks) {
            for(int y=-63; y<128; y++){
                if(cancelled.get()) {
                    finish(count.get());
                    return;
                }
                for(int x=0; x<16; x++){
                    for(int z=0; z<16; z++){
                        if(chunk.getBlockState(new BlockPos(x, y, z)).getBlock().equals(target))
                            count.getAndIncrement();
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
