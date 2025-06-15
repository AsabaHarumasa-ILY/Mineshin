package com.AsabaHarumasa.elementalreactions.event;

import com.AsabaHarumasa.elementalreactions.ElementType;
import com.AsabaHarumasa.elementalreactions.ElementalAttachHandler;
import com.AsabaHarumasa.elementalreactions.ElementalReactionHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ElementalInteractionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        
        // 1. 检查冻结反应：水+冰
        if (world.getBlockState(pos).getBlock() == Blocks.ICE) {
            if (ElementalAttachHandler.hasElement(player, ElementType.WATER)) {
                handleShatterIceReaction(player, pos);
            }
        }
        
        // 2. 检查绽放反应：草+水
        if (world.getBlockState(pos).getBlock() == Blocks.GRASS || 
            world.getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK) {
            if (ElementalAttachHandler.hasElement(player, ElementType.WATER)) {
                applyBloomReaction(player, pos);
            }
        }
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        PlayerEntity player = event.getPlayer();
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        
        // 3. 草地燃烧反应
        if (world.getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK) {
            if (ElementalAttachHandler.hasElement(player, ElementType.FIRE)) {
                igniteGrassBlocks(world, pos);
            }
        }
        
        // 4. 碎冰攻击
        if (world.getBlockState(pos).getBlock() == Blocks.ICE) {
            handleIceShatterAttack(player, pos);
        }
    }
    
    private static void handleShatterIceReaction(PlayerEntity player, BlockPos icePos) {
        // 3次攻击触发碎冰
        if (player.getCooledAttackStrength(0.0f) > 0.9f) {
            player.resetCooldown();
            
            // 模拟3次攻击
            for (int i = 0; i < 3; i++) {
                // 触发碎冰反应
                World world = player.world;
                if (!world.isRemote) {
                    world.setBlockState(icePos, Blocks.WATER.getDefaultState());
                    ElementalReactionHandler.applyShatterIceReaction(player, icePos);
                }
            }
        }
    }
    
    private static void applyBloomReaction(PlayerEntity player, BlockPos pos) {
        // 绽放反应创建种籽
        World world = player.world;
        if (!world.isRemote) {
            world.setBlockState(pos.up(), Blocks.GRASS.getDefaultState());
            ElementalReactionHandler.applyBloomReaction(player, pos);
        }
    }
    
    private static void igniteGrassBlocks(World world, BlockPos center) {
        // 引燃5格半径内的草地
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (world.getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK) {
                    world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                    
                    // 对范围内所有实体造成燃烧伤害
                    world.getEntitiesWithinAABB(LivingEntity.class, 
                         new AxisAlignedBB(pos).grow(5.0)).forEach(entity -> {
                         entity.setFire(10);
                         entity.attackEntityFrom(DamageSource.IN_FIRE, 2.0f);
                    });
                }
            }
        }
    }
    
    private static void handleIceShatterAttack(PlayerEntity player, BlockPos icePos) {
        // 在玩家有冰附着时增加碎冰伤害
        if (ElementalAttachHandler.hasElement(player, ElementType.ICE)) {
            World world = player.world;
            if (!world.isRemote) {
                // 范围伤害
                world.getEntitiesWithinAABB(LivingEntity.class, 
                    new AxisAlignedBB(icePos).grow(3.0)).forEach(entity -> {
                    entity.attackEntityFrom(DamageSource.MAGIC, 5.0f);
                });
            }
        }
    }
}



