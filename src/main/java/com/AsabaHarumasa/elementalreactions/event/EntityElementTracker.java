package com.AsabaHarumasa.elementalreactions.event;

import com.AsabaHarumasa.elementalreactions.ElementType;
import com.AsabaHarumasa.elementalreactions.ElementalAttachHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EntityElementTracker {

    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent.SpecialSpawn event) {
        LivingEntity entity = event.getEntityLiving();
        World world = event.getWorld();
        Biome biome = world.getBiome(entity.getPosition());
        
        // 根据环境和怪物类型赋予元素
        if (world.isRainingAt(entity.getPosition())) {
            attachWaterAffectedEntities(entity);
        } else if (world.isDaytime()) {
            attachDaytimeEntities(entity, biome);
        } else {
            attachNighttimeEntities(entity, biome);
        }
        
        // 特定生物元素附着
        if (entity instanceof ZombifiedPiglinEntity) {
            ElementalAttachHandler.attachElement(entity, ElementType.FIRE, 300);
        } else if (entity instanceof CreeperEntity) {
            ElementalAttachHandler.attachElement(entity, ElementType.ELECTRO, 300);
        } else if (entity instanceof SnowGolemEntity) {
            ElementalAttachHandler.attachElement(entity, ElementType.ICE, 300);
        } else if (entity instanceof MooshroomEntity) {
            ElementalAttachHandler.attachElement(entity, ElementType.GRASS, 300);
        }
    }
    
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            World world = event.getWorld();
            
            // 水下生物赋予水元素
            if (entity.isInWater()) {
                ElementalAttachHandler.attachElement(entity, ElementType.WATER, 300);
            }
            
            // 火焰生物赋予火元素
            if (entity.isBurning()) {
                ElementalAttachHandler.attachElement(entity, ElementType.FIRE, 300);
            }
            
            // 高海拔生物赋予风元素
            if (entity.getPosition().getY() > 128) {
                ElementalAttachHandler.attachElement(entity, ElementType.WIND, 300);
            }
            
            // 山地生物赋予岩元素
            if (world.getBiome(entity.getPosition()).getCategory() == Biome.Category.EXTREME_HILLS) {
                ElementalAttachHandler.attachElement(entity, ElementType.ROCK, 300);
            }
        }
    }
    
    private static void attachWaterAffectedEntities(LivingEntity entity) {
        // 雨天附着水元素的实体
        if (entity instanceof EndermanEntity) {
            ElementalAttachHandler.attachElement(entity, ElementType.WATER, 300);
        } else if (entity instanceof BlazeEntity) {
            // 雨天削弱烈焰人
            entity.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 300, 1));
        }
    }
    
    private static void attachDaytimeEntities(LivingEntity entity, Biome biome) {
        // 沙漠生物
        if (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS) {
            if (entity instanceof SkeletonEntity || entity instanceof ZombieEntity) {
                ElementalAttachHandler.attachElement(entity, ElementType.FIRE, 300);
            }
        }
    }
    
    private static void attachNighttimeEntities(LivingEntity entity, Biome biome) {
        // 冰雪生物
        if (biome == Biomes.SNOWY_TUNDRA || biome == Biomes.ICE_SPIKES) {
            if (entity instanceof StrayEntity || entity instanceof PolarBearEntity) {
                ElementalAttachHandler.attachElement(entity, ElementType.ICE, 300);
            }
        }
    }
}



