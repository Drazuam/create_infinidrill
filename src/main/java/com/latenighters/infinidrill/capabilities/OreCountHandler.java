package com.latenighters.infinidrill.capabilities;

import com.latenighters.infinidrill.InfiniDrill;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.function.BiConsumer;

import static com.latenighters.infinidrill.InfiniDrillConfig.getScanRadius;
import static net.minecraft.tags.BlockTags.GOLD_ORES;

@Mod.EventBusSubscriber
public class OreCountHandler implements IOreCountHandler, INBTSerializable<CompoundTag> {

    private final LevelChunk chunk;

    private final Map<Block, Integer> counts = new HashMap<>();
    private final Map<Block, LocalDateTime> ages   = new HashMap<>();
    private final Map<Block, OreFinderTask> runningScans = new HashMap<>();

    private final Set<BlockPos> placedOres = new HashSet<>();

    private final int age_limit = 10;//in seconds
    private final int grace_period = 5; //in seconds
    private final int scanRadius = 2;

    public OreCountHandler(LevelChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public void markOrePlaced(BlockPos pos) {
        placedOres.add(pos);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event){
        //if(event.getWorld().isClientSide()) return;
        if(!event.getPlacedBlock().is(Tags.Blocks.ORES)) return;

        ServerLevel level = (ServerLevel) event.getWorld();
        LevelChunk chunk = level.getChunkAt(event.getPos());

        chunk.getCapability(CapabilityOreCounter.COUNTER).ifPresent(cap -> {
            cap.markOrePlaced(event.getPos());
        });

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

        ListTag placedOresTag = new ListTag();
        placedOres.forEach(blockPos -> {
            placedOresTag.add(NbtUtils.writeBlockPos(blockPos));
        });

        retVal.put("ore_counts", oreCountList);
        retVal.put("placed_ores", placedOresTag);

        return retVal;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        if(nbt.contains("ore_counts")) {
            ((ListTag) nbt.get("ore_counts")).forEach(tag -> {
                CompoundTag ctag = (CompoundTag) tag;
                Block block = NbtUtils.readBlockState(ctag.getCompound("block")).getBlock();
                counts.put(block, ctag.getInt("count"));
                ages.put(block, LocalDateTime.parse(ctag.getString("age")));
                runningScans.put(block, null);
            });
        }

        if(nbt.contains("placed_ores")) {
            ((ListTag)nbt.get("placed_ores")).forEach(tag -> {
                placedOres.add(NbtUtils.readBlockPos((CompoundTag)tag));
            });
        }

    }

    @Override
    public Integer countBlocksOfType(Block block, Boolean naturalOnly) {
        return lazyCountBlocksOfType(block, naturalOnly).orElse(0);
    }

    @Override
    public LazyOptional<Integer> lazyCountBlocksOfType (Block block, Boolean naturalOnly){

        //check that we have a recent scan for this block
        if(counts.containsKey(block)){
            if(ages.get(block).isAfter(LocalDateTime.now().minus(age_limit, ChronoUnit.SECONDS))){
                return LazyOptional.of(()->{return counts.get(block);});
            }
            else{
                //check if we're in the grace period, and if so give the current count
                if(ages.get(block).isAfter(LocalDateTime.now().minus(age_limit+grace_period, ChronoUnit.SECONDS))){
                    startScan(block, naturalOnly);
                    return LazyOptional.of(()->{return counts.get(block);});
                }
                //if we're not in the grace period and there is no scan running, start a new scan
                if(runningScans.get(block)==null){
                    return startScan(block, naturalOnly);  //this will return an empty optional
                }else{
                    //how to properly cancel the scan and start a new one?  for now just start an additional scan
                    return startScan(block, naturalOnly);
                }
            }
        }
        else {
            return startScan(block, naturalOnly);
        }
        //return LazyOptional.empty();
    }

    private LazyOptional<Integer> startScan(Block block, Boolean naturalOnly) {

        if(!counts.containsKey(block)) {
            counts.put(block, 0);
            ages.put(block, LocalDateTime.now());
            runningScans.put(block, null);
        }

        Set<BlockPos> toIgnore = new HashSet<>();
        if(naturalOnly){
            ChunkPos homePos = chunk.getPos();
            for (int x=-getScanRadius(); x<=getScanRadius(); x++){
                for(int z=-getScanRadius(); z<=getScanRadius(); z++){
                    LevelChunk toAdd = chunk.getWorldForge().getChunk(homePos.x + x, homePos.z + z);
                    if(toAdd.getWorldForge() != null) {
                        toAdd.getCapability(CapabilityOreCounter.COUNTER).ifPresent(cap -> {
                            toIgnore.addAll(cap.getPlacedOres());
                        });
                    }
                }
            }
        }

        new OreFinderTask(getChunksToScan(), block, toIgnore).addCallback(count -> {
            counts.put(block, count);
            ages.put(block, LocalDateTime.now());
            runningScans.put(block, null);
        }).start();

        return LazyOptional.empty();
    }

    private List<LevelChunk> getChunksToScan(){
        ChunkPos homePos = chunk.getPos();
        List<LevelChunk> chunkList = new ArrayList<>();
        for (int x=-getScanRadius(); x<=getScanRadius(); x++){
            for(int z=-getScanRadius(); z<=getScanRadius(); z++){
                if(chunk.getWorldForge() != null) {
                    LevelChunk toScan = chunk.getWorldForge().getChunk(homePos.x + x, homePos.z + z);
                    chunkList.add(toScan);
                }
            }
        }
        return chunkList;
    }

    @Override
    public Set<BlockPos> getPlacedOres() {
        return placedOres;
    }
}
