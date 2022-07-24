package com.latenighters.infinidrill.network;

import com.latenighters.infinidrill.capabilities.OreCountHandler;
import com.latenighters.infinidrill.capabilities.OreCountRequestPacket;
import com.latenighters.infinidrill.capabilities.OreCountSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.checkerframework.checker.units.qual.A;

import java.util.concurrent.atomic.AtomicInteger;

public class PacketHandler {

    private static final AtomicInteger msgIndex = new AtomicInteger(1);
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("infinidrill", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static Integer getIndex(){
        return msgIndex.incrementAndGet();
    }

    public static void register(){

        INSTANCE.registerMessage(getIndex(),
                OreCountRequestPacket.class,
                OreCountRequestPacket::encode,
                OreCountRequestPacket::decode,
                OreCountRequestPacket::handle);

        INSTANCE.registerMessage(getIndex(),
                OreCountSyncPacket.class,
                OreCountSyncPacket::encode,
                OreCountSyncPacket::decode,
                OreCountSyncPacket::handle);
    }



}
