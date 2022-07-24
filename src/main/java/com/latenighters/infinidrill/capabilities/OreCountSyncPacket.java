package com.latenighters.infinidrill.capabilities;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OreCountSyncPacket {

    public final CompoundTag nbt;

    public OreCountSyncPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public OreCountSyncPacket(Block block, Integer count, LevelChunk chunk) {
        this.nbt = new CompoundTag();
        this.nbt.putInt("count", count);
        this.nbt.put("block", NbtUtils.writeBlockState(block.defaultBlockState()));

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

    public Integer getCount() {
        return this.nbt.getInt("count");
    }

    public Block getBlock() {
        return NbtUtils.readBlockState(this.nbt.getCompound("block")).getBlock();
    }

    public static void encode(OreCountSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.nbt);
    }

    public static OreCountSyncPacket decode(FriendlyByteBuf buf) {
        return new OreCountSyncPacket(buf.readNbt());
    }

    public static void handle(final OreCountSyncPacket msg, Supplier<NetworkEvent.Context> context) {

        Minecraft mc = Minecraft.getInstance();

        context.get().enqueueWork(() -> {

            LevelChunk chunk = mc.level.getChunk(msg.getChunkPos().x, msg.getChunkPos().z);
            chunk.getCapability(CapabilityOreCounter.COUNTER).ifPresent(cap -> {
                cap.setScanResult(msg.getBlock(), msg.getCount());
            });

            context.get().setPacketHandled(true);

        });

    }
}