package com.latenighters.infinidrill;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber
public class InfiniDrillConfig{

    public static final ForgeConfigSpec GENERAL_SPEC;
    public static ForgeConfigSpec.IntValue infiniteOreThreshold;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> blacklisted_ores;

    public static final ArrayList<Runnable> updateCallbacks = new ArrayList<>();

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        GENERAL_SPEC = configBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        infiniteOreThreshold = builder
                .comment("When the number of ores in a 5x5 chunk goes above this number, the vein is infinite")
                .defineInRange("infinite_ore_threshold", 10000, 0, 1000000);
        blacklisted_ores = builder
                .comment("The ores to be ignored by the infinite drill function")
                .defineList("blacklisted_ores", List.of(), entry -> true);
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event){
        updateCallbacks.forEach(Runnable::run);
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event){
        updateCallbacks.forEach(Runnable::run);
    }
}
