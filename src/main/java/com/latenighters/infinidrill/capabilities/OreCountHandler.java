package com.latenighters.infinidrill.capabilities;

import com.latenighters.infinidrill.InfiniDrill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber
public class OreCountHandler implements IOreCountHandler, INBTSerializable<CompoundTag> {

    private LevelChunk chunk;

    Map<Block, Integer> counts = new HashMap<>();
    Map<Block, LocalDateTime> ages   = new HashMap<>();
    Map<Block, OreFinderTask> runningScans = new HashMap<>();

    private final int age_limit = 10;//in seconds
    private final int grace_period = 5; //in seconds
    private final int scanRadius = 2;

    public OreCountHandler(LevelChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public CompoundTag serializeNBT() {

        CompoundTag retVal =  new CompoundTag();
        List<CompoundTag> oreCounts = new ArrayList<>();
        counts.forEach(new BiConsumer<Block, Integer>() {
            @Override
            public void accept(Block block, Integer integer) {
                CompoundTag toAdd = new CompoundTag();
                toAdd.putString("age", ages.get(block).toString());
                toAdd.putInt("count", counts.get(block));
                toAdd.put("block",NbtUtils.writeBlockState(block.defaultBlockState()));
                oreCounts.add(toAdd);
            }
        });
        ListTag oreCountList = new ListTag();
        oreCountList.addAll(oreCounts);
        retVal.put("ore_counts", oreCountList);

        return retVal;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        ((ListTag) Objects.requireNonNull(nbt.get("ore_counts"))).forEach(tag -> {
            CompoundTag ctag = (CompoundTag)tag;
            Block block = NbtUtils.readBlockState(ctag.getCompound("block")).getBlock();
            counts.put(block, ctag.getInt("count"));
            ages.put(block, LocalDateTime.parse(ctag.getString("age")));
            runningScans.put(block, null);
        });


    }

    @Override
    public Integer countBlocksOfType(Block block) {
        return lazyCountBlocksOfType(block).orElse(0);
    }

    @Override
    public LazyOptional<Integer> lazyCountBlocksOfType (Block block){

        //check that we have a recent scan for this block
        if(counts.containsKey(block)){
            if(ages.get(block).isAfter(LocalDateTime.now().minus(age_limit, ChronoUnit.SECONDS))){
                return LazyOptional.of(()->{return counts.get(block);});
            }
            else{
                //check if we're in the grace period, and if so give the current count
                if(ages.get(block).isAfter(LocalDateTime.now().minus(age_limit+grace_period, ChronoUnit.SECONDS))){
                    startScan(block);
                    return LazyOptional.of(()->{return counts.get(block);});
                }
                //if we're not in the grace period and there is no scan running, start a new scan
                if(runningScans.get(block)==null){
                    return startScan(block);  //this will return an empty optional
                }else{
                    //how to properly cancel the scan and start a new one?  for now just start an additional scan
                    return startScan(block);
                }
            }
        }
        else {
            return startScan(block);
        }
        //return LazyOptional.empty();
    }

    private LazyOptional<Integer> startScan(Block block) {

        if(!counts.containsKey(block)) {
            counts.put(block, 0);
            ages.put(block, LocalDateTime.now());
            runningScans.put(block, null);
        }

        new OreFinderTask(getChunksToScan(), block).addCallback(count -> {
            counts.put(block, count);
            ages.put(block, LocalDateTime.now());
            runningScans.put(block, null);
        }).start();

        return LazyOptional.empty();
    }

    private List<LevelChunk> getChunksToScan(){
        ChunkPos homePos = chunk.getPos();
        List<LevelChunk> chunkList = new ArrayList<>();
        for (int x=-scanRadius; x<=scanRadius; x++){
            for(int z=-scanRadius; z<=scanRadius; z++){
                if(chunk.getWorldForge() != null) {
                    LevelChunk toScan = chunk.getWorldForge().getChunk(homePos.x + x, homePos.z + z);
                    chunkList.add(toScan);
                }
            }
        }
        return chunkList;
    }
}
