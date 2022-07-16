package com.latenighters.infinidrill.mixin;

import com.latenighters.infinidrill.InfiniDrillConfig;
import com.latenighters.infinidrill.capabilities.CapabilityOreCounter;
import com.simibubi.create.content.contraptions.components.actors.BlockBreakingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.actors.DrillTileEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Mixin(DrillTileEntity.class)
public abstract class MixinDrill extends BlockBreakingKineticTileEntity {

    private static TagKey<Block> forgeOreTag = null;

    private static TagKey<Block> getForgeOreTag(){

        if(forgeOreTag!=null)
            return forgeOreTag;

        forgeOreTag = ForgeRegistries.BLOCKS.tags().stream()
                .map(ITag::getKey)
                .filter(blockTagKey -> {
                    return blockTagKey.location().toString().equals("forge:ores");
                }).findAny().orElse(null);

        return forgeOreTag;
    }

    private static int infiniteThreshold = 10000;
    private static List<Block> blacklisted_ores;
    static {
        InfiniDrillConfig.updateCallbacks.add(MixinDrill::reloadOres);
    }

    public MixinDrill(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        this.isInfinite().ifPresent(isCurrentlyInfinite->{
           if(isCurrentlyInfinite)
               TooltipHelper.addHint(tooltip, "hint.infinite_drill");
        });
        return addToGoggleTooltip;
    }

    @Override
    public void onBlockBroken(BlockState stateToBreak) {

        isInfinite().ifPresent(infinite->{
            FluidState FluidState = level.getFluidState(breakingPos);
            level.levelEvent(2001, breakingPos, Block.getId(stateToBreak));
            BlockEntity tileentity = stateToBreak.hasBlockEntity() ? level.getBlockEntity(breakingPos) : null;
            Vec3 vec = VecHelper.offsetRandomly(VecHelper.getCenterOf(breakingPos), level.random, .125f);

            Block.getDrops(stateToBreak, (ServerLevel) level, breakingPos, tileentity).forEach((stack) -> {
                if (!stack.isEmpty() && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                        && !level.restoringBlockSnapshots) {
                    ItemEntity itementity = new ItemEntity(level, vec.x, vec.y, vec.z, stack);
                    itementity.setDefaultPickUpDelay();
                    itementity.setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(itementity);
                }
            });

            if(!infinite) {
                if (level instanceof ServerLevel)
                    stateToBreak.spawnAfterBreak((ServerLevel) level, breakingPos, ItemStack.EMPTY);
                level.setBlock(breakingPos, FluidState.createLegacyBlock(), 3);
            }
        });

    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            if (this.shouldRun()) {
                if (this.getSpeed() != 0.0F) {
                    this.breakingPos = this.getBreakingPos();
                    if (this.ticksUntilNextProgress >= 0) {
                        if (this.ticksUntilNextProgress-- <= 0) {
                            BlockState stateToBreak = this.level.getBlockState(this.breakingPos);
                            float blockHardness = stateToBreak.getDestroySpeed(this.level, this.breakingPos);
                            if (!this.canBreak(stateToBreak, blockHardness)) {
                                if (this.destroyProgress != 0) {
                                    this.destroyProgress = 0;
                                    this.level.destroyBlockProgress(this.breakerId, this.breakingPos, -1);
                                }

                            } else {
                                if(this.destroyProgress==0)
                                    this.isInfinite(); //go check if this block is infinite (start a search)
                                float breakSpeed = this.getBreakSpeed();
                                this.destroyProgress += Mth.clamp((int)(breakSpeed / blockHardness), 1, 10 - this.destroyProgress);
                                this.level.playSound((Player)null, this.worldPosition, stateToBreak.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.25F, 1.0F);
                                if (this.destroyProgress >= 10) {
                                    this.onBlockBroken(stateToBreak);
                                    this.destroyProgress = 0;
                                    this.ticksUntilNextProgress = -1;
                                    this.level.destroyBlockProgress(this.breakerId, this.breakingPos, -1);
                                } else {
                                    this.ticksUntilNextProgress = (int)(blockHardness / breakSpeed);
                                    this.level.destroyBlockProgress(this.breakerId, this.breakingPos, this.destroyProgress);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private LazyOptional<Boolean> isInfinite(){

        if(blacklisted_ores == null) reloadOres();

        if(blacklisted_ores.contains(this.level.getBlockState(this.breakingPos).getBlock()))
            return LazyOptional.of(()->false);

        if(!this.level.getBlockState(this.breakingPos).getTags().anyMatch(tag->{
            return tag.location().equals(getForgeOreTag().location());
        }))
            return LazyOptional.of(()->false);

        AtomicReference<LazyOptional<Boolean>> infinite = new AtomicReference<>(LazyOptional.empty());
        this.containedChunk().getCapability(CapabilityOreCounter.COUNTER).ifPresent(oreCap -> {
            Block target = level.getBlockState(this.breakingPos).getBlock();
            oreCap.lazyCountBlocksOfType(target).ifPresent(count -> {
                if(count> infiniteThreshold){
                    infinite.set(LazyOptional.of(()->true));
                }else{
                    infinite.set(LazyOptional.of(()->false));
                }
            });
        });
        return infinite.get();
    }

    private static void reloadOres(){
        blacklisted_ores = new ArrayList<>();
        InfiniDrillConfig.blacklisted_ores.get().forEach(string->{
            ResourceLocation rsl = new ResourceLocation(string);
            if (ForgeRegistries.BLOCKS.containsKey(rsl)){
                blacklisted_ores.add(ForgeRegistries.BLOCKS.getValue(rsl));
            }
        });

        infiniteThreshold = InfiniDrillConfig.infiniteOreThreshold.get();
    }
}
