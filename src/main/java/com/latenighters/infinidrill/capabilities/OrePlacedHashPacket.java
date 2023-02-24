package com.latenighters.infinidrill.capabilities;

import com.latenighters.infinidrill.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;

public class OrePlacedHashPacket {

    public final CompoundTag nbt;

    public OrePlacedHashPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public OrePlacedHashPacket(Set<BlockPos> blockPosSet, LevelChunk chunk) {
        this.nbt = new CompoundTag();
        this.nbt.putInt("hashcode", blockPosSet.hashCode());
        int chunkx = chunk.getPos().x;
        int chunkz = chunk.getPos().z;

        CompoundTag chunkTag = new CompoundTag();
        chunkTag.putInt("x", chunkx);
        chunkTag.putInt("z", chunkz);

        this.nbt.put("chunk", chunkTag);
    }

    public Integer getHashCode() {
        return this.nbt.getInt("hashcode");
    }

    public ChunkPos getChunkPos(){
        int chunkx = this.nbt.getCompound("chunk").getInt("x");
        int chunkz = this.nbt.getCompound("chunk").getInt("z");

        return new ChunkPos(chunkx, chunkz);
    }

    public static void encode(OrePlacedHashPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.nbt);
    }

    public static OrePlacedHashPacket decode(FriendlyByteBuf buf) {
        return new OrePlacedHashPacket(buf.readNbt());
    }

    public static void handle(final OrePlacedHashPacket msg, Supplier<NetworkEvent.Context> context) {

        context.get().enqueueWork(() -> {

            ServerLevel world = context.get().getSender().getLevel();
            LevelChunk chunk = world.getChunk(msg.getChunkPos().x, msg.getChunkPos().z);

            chunk.getCapability(CapabilityOreCounter.COUNTER).ifPresent(cap -> {
                if(msg.getHashCode() != cap.getPlacedSetHashcode()){

                    //send the update packet to the client
                    ServerPlayer player = context.get().getSender();
                    if(player!=null && !(player instanceof FakePlayer)) {
                        OrePlacedSetPacket response = new OrePlacedSetPacket(cap.getPlacedOres(), chunk);
                        PacketHandler.INSTANCE.sendTo(response, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
                    }
                }
            });

            context.get().setPacketHandled(true);

        });

    }



}
