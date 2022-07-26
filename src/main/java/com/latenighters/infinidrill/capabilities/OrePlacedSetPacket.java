package com.latenighters.infinidrill.capabilities;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class OrePlacedSetPacket {


    public final CompoundTag nbt;

    public OrePlacedSetPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public OrePlacedSetPacket(Set<BlockPos> blockPosSet, LevelChunk chunk) {
        this.nbt = new CompoundTag();

        ListTag placedOres = new ListTag();
        blockPosSet.forEach(blockPos -> {
            placedOres.add(NbtUtils.writeBlockPos(blockPos));
        });
        this.nbt.put("placed_ores", placedOres);

        int chunkx = chunk.getPos().x;
        int chunkz = chunk.getPos().z;

        CompoundTag chunkTag = new CompoundTag();
        chunkTag.putInt("x", chunkx);
        chunkTag.putInt("z", chunkz);

        this.nbt.put("chunk", chunkTag);
    }

    public ChunkPos getChunkPos() {
        int chunkx = this.nbt.getCompound("chunk").getInt("x");
        int chunkz = this.nbt.getCompound("chunk").getInt("z");

        return new ChunkPos(chunkx, chunkz);
    }

    private Set<BlockPos> getPlacedOres() {
        Set<BlockPos> blockPosSet = new HashSet<>();

        this.nbt.getList("placed_ores", 10).forEach(placedOre -> {
            blockPosSet.add(NbtUtils.readBlockPos((CompoundTag) placedOre));
        });

        return blockPosSet;
    }

    public static void encode(OrePlacedSetPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.nbt);
    }

    public static OrePlacedSetPacket decode(FriendlyByteBuf buf) {
        return new OrePlacedSetPacket(buf.readNbt());
    }

    public static void handle(final OrePlacedSetPacket msg, Supplier<NetworkEvent.Context> context) {

        Minecraft mc = Minecraft.getInstance();

        context.get().enqueueWork(() -> {

            LevelChunk chunk = mc.level.getChunk(msg.getChunkPos().x, msg.getChunkPos().z);
            chunk.getCapability(CapabilityOreCounter.COUNTER).ifPresent(cap -> {
                cap.setPlacedOres(msg.getPlacedOres());
            });

            context.get().setPacketHandled(true);

        });

    }


}
