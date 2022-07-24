package com.latenighters.infinidrill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class InfiniDrillConfig{

    public static final ForgeConfigSpec GENERAL_SPEC;
    private static ForgeConfigSpec.IntValue infiniteOreThreshold;
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedOreNames;
    private static ForgeConfigSpec.BooleanValue naturalOnly;

    private static ForgeConfigSpec.DoubleValue speedMultiplier;
    private static ForgeConfigSpec.DoubleValue stressMultiplier;

    private static List<Block> blacklistedOres = new ArrayList<>();


    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        GENERAL_SPEC = configBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        infiniteOreThreshold = builder
                .comment("When the number of ores in a 5x5 chunk goes above this number, the vein is infinite")
                .defineInRange("infinite_ore_threshold", 10000, 0, 1000000);
        blacklistedOreNames = builder
                .comment("The ores to be ignored by the infinite drill function")
                .defineList("blacklisted_ores", List.of(), entry -> true);
        naturalOnly = builder
                .comment("If set to true, only naturally generated ores will count towards the threshold")
                .define("natural_ore_only", true);
        speedMultiplier = builder
                .comment("This multiplier is added to the breaking speed of a block when infini-drilling")
                .defineInRange("speed_multiplier", 0.5, 0.01, 10);
        stressMultiplier = builder
                .comment("This multiplier is added to the stress requirement of the drill when infini-drilling")
                .defineInRange("stress_multiplier", 4, 0.01, 1024);
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event){
        reloadOres();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event){
        reloadOres();
    }


    private static void reloadOres(){
        blacklistedOres = new ArrayList<>();
        InfiniDrillConfig.blacklistedOreNames.get().forEach(string->{
            ResourceLocation rsl = new ResourceLocation(string);
            if (ForgeRegistries.BLOCKS.containsKey(rsl)){
                blacklistedOres.add(ForgeRegistries.BLOCKS.getValue(rsl));
            }
        });
    }

    public static Integer getInfiniteOreThreshold() {
        return infiniteOreThreshold.get();
    }

    public static List<Block> getBlacklistedOres() {
        return blacklistedOres;
    }

    public static Boolean isNaturalOnly(){
        return naturalOnly.get();
    }

    public static Double getStressMultiplier(){
        return stressMultiplier.get();
    }

    public static Double getSpeedMultiplier(){
        return speedMultiplier.get();
    }

    public static Integer getScanRadius(){
        return 2;
    }
}
