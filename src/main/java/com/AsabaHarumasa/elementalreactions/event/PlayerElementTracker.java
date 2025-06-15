package com.AsabaHarumasa.elementalreactions.event;

import com.AsabaHarumasa.elementalreactions.ElementType;
import com.AsabaHarumasa.elementalreactions.ElementalAttachHandler;
import com.AsabaHarumasa.elementalreactions.ElementalReactionHandler;
import com.AsabaHarumasa.elementalreactions.ElementalReactionsMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalReactionsMod.MODID)
public class PlayerElementTracker {

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            DamageSource source = event.getSource();
            
            // 环境伤害元素附着
            if (source.isFireDamage()) {
                ElementalAttachHandler.attachElement(player, ElementType.FIRE, 300);
            } else if (source == DamageSource.DROWN) {
                ElementalAttachHandler.attachElement(player, ElementType.WATER, 300);
            } else if (source == DamageSource.LIGHTNING_BOLT) {
                ElementalAttachHandler.attachElement(player, ElementType.ELECTRO, 300);
            } else if (source == DamageSource.FREEZE) {
                ElementalAttachHandler.attachElement(player, ElementType.ICE, 300);
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 清除死亡玩家的元素状态
        PlayerEntity player = event.getPlayer();
        ElementalAttachHandler.removeElement(player.getUniqueID());
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.player;
            World world = player.world;
            
            // 1. 检查风元素扩散效果
            if (ElementalAttachHandler.hasElement(player, ElementType.WIND)) {
                applyWindDiffusion(player, world);
            }
            
            // 2. 检查草地上的燃烧传播
            BlockPos pos = player.getPosition();
            if (world.getBlockState(pos.down()).getBlock() == Blocks.GRASS_BLOCK &&
                ElementalAttachHandler.hasElement(player, ElementType.FIRE)) {
                igniteNearbyGrass(world, pos.down());
            }
            
            // 3. 周期性减少元素时间
            ElementalAttachHandler.reduceElementDuration(player, 1);
        }
    }
    
    private static void applyWindDiffusion(ServerPlayerEntity player, World world) {
        // 扩散范围内的元素
        world.getEntitiesWithinAABB(LivingEntity.class, 
            player.getBoundingBox().grow(5.0)).forEach(entity -> {
            if (entity != player) {
                ElementType element = ElementalAttachHandler.getAttachedElement(entity.getUniqueID());
                if (element != ElementType.NONE) {
                    // 触发扩散反应
                    ElementalReactionHandler.applyDiffusionReaction(player, entity, element);
                    
                    // 清除原始元素
                    ElementalAttachHandler.removeElement(entity.getUniqueID());
                }
            }
        });
    }
    
    private static void igniteNearbyGrass(World world, BlockPos center) {
        // 每5秒传播一次
        if (world.getGameTime() % 100 != 0) return;
        
        // 引燃周围草地
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (world.getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK) {
                    world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                }
            }
        }
    }
}



