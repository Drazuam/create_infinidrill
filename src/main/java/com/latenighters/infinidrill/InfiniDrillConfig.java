package com.latenighters.infinidrill;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.Blocks.IRON_ORE;

@Mod.EventBusSubscriber
public class InfiniDrillConfig{

    public static final ForgeConfigSpec GENERAL_SPEC;
    private static ForgeConfigSpec.IntValue infiniteOreThreshold;
    private static ForgeConfigSpec.IntValue searchRadius;
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistedOreNames;
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> equivalentOres;

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
                .defineInRange("infinite_ore_threshold", 6000, 0, 1000000);
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
        equivalentOres = builder
                .comment("a list of tags for ores that should be considered equivalent to each other")
                .defineList("equivalent_ore_tags", List.of(
                        "forge:ores/iron",
                        "forge:ores/coal",
                        "forge:ores/copper",
                        "forge:ores/diamond",
                        "forge:ores/emerald",
                        "forge:ores/gold",
                        "forge:ores/lapis",
                        "forge:ores/netherite_scrap",
                        "forge:ores/quartz",
                        "forge:ores/redstone"), entry->true);
        searchRadius = builder
                .comment("the radius to scan for ores, in chunks.  Radius 2 results in a 5x5 chunk area")
                .defineInRange("scan_radius", 2, 0, 7);

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

    private static List<TagKey<Block>> getEquivalentOreTags(){
        List<TagKey<Block>> retVal = new ArrayList<>();
        equivalentOres.get().forEach(tagString -> {
            if(!tagString.contains(":"))return;
            retVal.add(BlockTags.create(new ResourceLocation(tagString)));
        });
        return retVal;
    }

    public static List<TagKey<Block>> getTagsFor(BlockState blockState){
        List<TagKey<Block>> retval = new ArrayList<>();
        List<TagKey<Block>> tagKeyList = getEquivalentOreTags();

        tagKeyList.forEach(tagKey -> {
            if(blockState.is(tagKey))
                retval.add(tagKey);
        });

        return retval;
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
        return searchRadius.get();
    }

}
