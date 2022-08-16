package com.latenighters.infinidrill.capabilities;

import com.latenighters.infinidrill.InfiniDrillConfig;
import com.latenighters.infinidrill.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OreCountRequestPacket{

    public final CompoundTag nbt;

    public OreCountRequestPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public OreCountRequestPacket(Block block, LevelChunk chunk) {
        this.nbt = new CompoundTag();
        this.nbt.put("block", NbtUtils.writeBlockState(block.defaultBlockState()));

        int chunkx = chunk.getPos().x;
        int chunkz = chunk.getPos().z;

        CompoundTag chunkTag = new CompoundTag();
        chunkTag.putInt("x", chunkx);
        chunkTag.putInt("z", chunkz);

        this.nbt.put("chunk", chunkTag);
    }

    public Block getBlock(){
        return NbtUtils.readBlockState(this.nbt.getCompound("block")).getBlock();
    }

    public ChunkPos getChunkPos(){
        int chunkx = this.nbt.getCompound("chunk").getInt("x");
        int chunkz = this.nbt.getCompound("chunk").getInt("z");

        return new ChunkPos(chunkx, chunkz);
    }

    public static void encode(OreCountRequestPacket msg, FriendlyByteBuf buf){
        buf.writeNbt(msg.nbt);
    }

    public static OreCountRequestPacket decode(FriendlyByteBuf buf){
        return new OreCountRequestPacket(buf.readNbt());
    }

    public static void handle(final OreCountRequestPacket msg, Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(()->{

            ServerLevel world = context.get().getSender().getLevel();
            LevelChunk chunk = world.getChunk(msg.getChunkPos().x, msg.getChunkPos().z);

            chunk.getCapability(CapabilityOreCounter.COUNTER).ifPresent(cap -> {
                cap.lazyCountBlocksOfType(msg.getBlock(), InfiniDrillConfig.isNaturalOnly()).ifPresent(count->{
                    ServerPlayer player = context.get().getSender();
                    OreCountSyncPacket response = new OreCountSyncPacket(msg.getBlock(), count, chunk);
                    if(player!=null && !(player instanceof FakePlayer))
                        PacketHandler.INSTANCE.sendTo(response, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
                });
            });

            context.get().setPacketHandled(true);
        });
    }
}