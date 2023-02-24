package com.latenighters.infinidrill;

import com.latenighters.infinidrill.capabilities.CapabilityOreCounter;
import com.latenighters.infinidrill.capabilities.IOreCountHandler;
import com.latenighters.infinidrill.capabilities.OreCountHandler;
import com.latenighters.infinidrill.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("infinidrill")
@Mod.EventBusSubscriber
public class InfiniDrill
{
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "infinidrill";

    public InfiniDrill() {
        MinecraftForge.EVENT_BUS.register(this);

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, InfiniDrillConfig.GENERAL_SPEC, "infinidrill-server.toml");

        PacketHandler.register();
    }

    private void setup(final FMLCommonSetupEvent event){


    }

    @SubscribeEvent
    public void registerCapability(RegisterCapabilitiesEvent event){
        CapabilityOreCounter.register(event);
    }

    @SubscribeEvent
    public void attachCapability(final AttachCapabilitiesEvent<LevelChunk> event){

        OreCountHandler backend = new OreCountHandler(event.getObject());
        LazyOptional<IOreCountHandler> optionalStorage = LazyOptional.of(()->backend);

        ICapabilityProvider provider = new ICapabilitySerializable<CompoundTag>() {
            @NotNull
            @Override
            public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if(cap==CapabilityOreCounter.COUNTER){
                    return optionalStorage.cast();
                }else{
                    return LazyOptional.empty();
                }
            }

            @Override
            public CompoundTag serializeNBT() {
                return backend.serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                backend.deserializeNBT(nbt);
            }
        };

        event.addCapability(new ResourceLocation("infinidrill", "ore_counter_capability"), provider);
    }


}
