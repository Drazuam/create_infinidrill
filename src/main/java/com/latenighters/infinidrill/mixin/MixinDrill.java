package com.latenighters.infinidrill.mixin;

import com.latenighters.infinidrill.InfiniDrillConfig;
import com.latenighters.infinidrill.capabilities.CapabilityOreCounter;
import com.simibubi.create.content.contraptions.components.actors.BlockBreakingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.actors.DrillTileEntity;
import com.simibubi.create.content.contraptions.relays.advanced.SpeedControllerTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.AdjustablePulleyTileEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(DrillTileEntity.class)
public abstract class MixinDrill extends BlockBreakingKineticTileEntity {

    private Boolean isInfiniteState;

    @Override
    public void lazyTick() {
        super.lazyTick();
        this.isInfinite();  //forces the infinite check on lazy ticks for stress calculations
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
                                if(this.destroyProgress>=4)
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

        AtomicReference<LazyOptional<Boolean>> infinite = new AtomicReference<>(LazyOptional.empty());

        if(this.breakingPos==null)
            infinite.set(LazyOptional.empty());

        else if(InfiniDrillConfig.getBlacklistedOres().contains(this.level.getBlockState(this.breakingPos).getBlock()))
            infinite.set(LazyOptional.of(()->false));

        else if(!this.level.getBlockState(this.breakingPos).is(Tags.Blocks.ORES))
            infinite.set(LazyOptional.of(()->false));

        else{

            this.containedChunk().getCapability(CapabilityOreCounter.COUNTER).ifPresent(oreCap -> {

                if(InfiniDrillConfig.isNaturalOnly() && !oreCap.isNaturallyPlaced(this.breakingPos)){
                    infinite.set(LazyOptional.of(()->false));
                    return;
                }

                Block target = level.getBlockState(this.breakingPos).getBlock();
                oreCap.lazyCountBlocksOfType(target, InfiniDrillConfig.isNaturalOnly()).ifPresent(count -> {
                    if(count > InfiniDrillConfig.getInfiniteOreThreshold()){
                        infinite.set(LazyOptional.of(()->true));
                    }else{
                        infinite.set(LazyOptional.of(()->false));
                    }
                });
            });
        }

        infinite.get().ifPresent(a -> {
            if(a!=isInfiniteState){
                isInfiniteState = a;
                updateRotation();
            }
        });

        return infinite.get();
    }

    private void updateRotation(){
        if(!this.level.isClientSide()) {
            //hacky?  Hard to say.  Create doesn't seem to like dynamic stresses
            if(this.getOrCreateNetwork()!=null) {
                this.detachKinetics();
                this.getOrCreateNetwork().remove(this);
                this.initialize();
                this.attachKinetics();
                this.setChanged();
            }
        }
    }

    @Override
    protected float getBreakSpeed() {
        AtomicReference<Float> speed = new AtomicReference<>(Math.abs(this.getSpeed() / 100.0F));

        isInfinite().ifPresent(inf -> {
            if(inf){
                speed.set((float) Math.abs(this.getSpeed() / 100.0F * InfiniDrillConfig.getSpeedMultiplier()));
            }
        });

        return speed.get();
    }

    @Override
    public float calculateStressApplied() {
        AtomicReference<Float> impact = new AtomicReference<>(super.calculateStressApplied());

        if(isInfiniteState!=null && isInfiniteState){
            impact.set((float) (impact.get() * InfiniDrillConfig.getStressMultiplier()));
        }

        return impact.get();
    }
}
