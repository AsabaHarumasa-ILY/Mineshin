package com.AsabaHarumasa.elementalreactions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class ParticleRenderer {
    private static final Random random = new Random();
    private static final int PLAYER_PARTICLE_COUNT = 8;   // 玩家周围生成的粒子数量
    private static final int ENTITY_PARTICLE_COUNT = 4;   // 实体周围生成的粒子数量
    private static final double PLAYER_PARTICLE_RANGE = 1.2; // 玩家周围粒子生成范围
    private static final double ENTITY_PARTICLE_RANGE = 0.8; // 实体周围粒子生成范围
    
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        
        // 渲染玩家自身的粒子效果
        renderPlayerParticles(player, event.getPartialTicks());
        
        // 渲染视野内所有实体的粒子效果
        renderNearbyEntitiesParticles(player, event.getPartialTicks());
    }
    
    /**
     * 渲染玩家周围的元素粒子效果
     */
    private void renderPlayerParticles(ClientPlayerEntity player, float partialTicks) {
        ElementType element = ElementalAttachHandler.getAttachedElement(player.getUUID());
        if (element == ElementType.NONE) return;
        
        IParticleData particleData = element.getParticleData();
        Vector3d pos = player.getPosition(partialTicks);
        
        for (int i = 0; i < PLAYER_PARTICLE_COUNT; i++) {
            // 随机生成粒子位置（在玩家周围）
            double xOffset = (random.nextDouble() - 0.5) * PLAYER_PARTICLE_RANGE;
            double yOffset = random.nextDouble() * player.getBbHeight();
            double zOffset = (random.nextDouble() - 0.5) * PLAYER_PARTICLE_RANGE;
            
            // 添加轻微随机运动
            double motionX = (random.nextDouble() - 0.5) * 0.1;
            double motionY = random.nextDouble() * 0.05;
            double motionZ = (random.nextDouble() - 0.5) * 0.1;
            
            // 创建粒子
            player.level.addParticle(
                particleData, 
                pos.x + xOffset, 
                pos.y + yOffset, 
                pos.z + zOffset, 
                motionX, motionY, motionZ
            );
            
            // 对于风元素添加额外运动效果
            if (element == ElementType.WIND) {
                player.level.addParticle(
                    ParticleTypes.CLOUD, 
                    pos.x + xOffset, 
                    pos.y + yOffset, 
                    pos.z + zOffset, 
                    motionX * 2.0, motionY * 1.5, motionZ * 2.0
                );
            }
        }
    }
    
    /**
     * 渲染玩家附近实体的元素粒子效果
     */
    private void renderNearbyEntitiesParticles(ClientPlayerEntity player, float partialTicks) {
        player.level.getEntitiesOfClass(
            LivingEntity.class, 
            player.getBoundingBox().inflate(16), // 16格范围内的实体
            entity -> entity != player && ElementalAttachHandler.getAttachedElement(entity.getUUID()) != ElementType.NONE
        ).forEach(entity -> {
            renderEntityParticles(entity, partialTicks);
        });
    }
    
    /**
     * 渲染单个实体的粒子效果
     */
    private void renderEntityParticles(LivingEntity entity, float partialTicks) {
        ElementType element = ElementalAttachHandler.getAttachedElement(entity.getUUID());
        if (element == ElementType.NONE) return;
        
        IParticleData particleData = element.getParticleData();
        Vector3d pos = entity.getPosition(partialTicks);
        double entityHeight = entity.getBbHeight();
        
        for (int i = 0; i < ENTITY_PARTICLE_COUNT; i++) {
            // 在实体头顶上方生成粒子
            double xOffset = (random.nextDouble() - 0.5) * ENTITY_PARTICLE_RANGE;
            double yOffset = entityHeight + 0.2 + random.nextDouble() * 0.5;
            double zOffset = (random.nextDouble() - 0.5) * ENTITY_PARTICLE_RANGE;
            
            // 添加随机运动
            double motionX = (random.nextDouble() - 0.5) * 0.05;
            double motionY = random.nextDouble() * 0.03;
            double motionZ = (random.nextDouble() - 0.5) * 0.05;
            
            // 特殊元素的运动处理
            if (element == ElementType.FIRE) {
                motionY += 0.1; // 火焰向上飘
            } else if (element == ElementType.ELECTRO) {
                // 雷元素有更随机的运动
                motionX = (random.nextDouble() - 0.5) * 0.15;
                motionZ = (random.nextDouble() - 0.5) * 0.15;
            }
            
            // 创建粒子
            entity.level.addParticle(
                particleData, 
                pos.x + xOffset, 
                pos.y + yOffset, 
                pos.z + zOffset, 
                motionX, motionY, motionZ
            );
            
            // 对于某些元素添加额外视觉效果
            switch (element) {
                case FIRE:
                    // 火焰添加火花效果
                    entity.level.addParticle(
                        ParticleTypes.FLAME, 
                        pos.x + xOffset, 
                        pos.y + yOffset, 
                        pos.z + zOffset, 
                        motionX, motionY * 2, motionZ
                    );
                    break;
                case ICE:
                    // 冰元素添加雪花效果
                    entity.level.addParticle(
                        ParticleTypes.ITEM_SNOWBALL, 
                        pos.x + xOffset, 
                        pos.y + yOffset, 
                        pos.z + zOffset, 
                        0, 0, 0
                    );
                    break;
                case WATER:
                    // 水元素添加水滴效果
                    entity.level.addParticle(
                        ParticleTypes.RAIN, 
                        pos.x + xOffset, 
                        pos.y + yOffset, 
                        pos.z + zOffset, 
                        0, -0.1, 0
                    );
                    break;
            }
        }
        
        // 对于燃烧效果添加额外粒子
        if (element == ElementType.FIRE && entity.isInWater()) {
            renderSteamEffect(entity, partialTicks);
        }
    }
    
    /**
     * 当火元素实体进入水中时生成蒸汽效果
     */
    private void renderSteamEffect(LivingEntity entity, float partialTicks) {
        Vector3d pos = entity.getPosition(partialTicks);
        for (int i = 0; i < 3; i++) {
            double x = pos.x + (random.nextDouble() - 0.5) * entity.getBbWidth();
            double y = pos.y + entity.getBbHeight() * random.nextDouble();
            double z = pos.z + (random.nextDouble() - 0.5) * entity.getBbWidth();
            
            // 生成蒸汽粒子
            entity.level.addParticle(
                ParticleTypes.CLOUD, 
                x, y, z, 
                0, 0.15, 0
            );
        }
    }
    
    /**
     * 当实体发生元素反应时调用，显示反应粒子效果
     * 
     * @param entity 发生反应的实体
     * @param reactionType 反应类型（用于确定粒子效果）
     */
    public static void spawnReactionParticles(LivingEntity entity, ReactionType reactionType) {
        if (entity.level.isClientSide) {
            Vector3d pos = entity.position();
            switch (reactionType) {
                case EVAPORATE:
                case MELT:
                    spawnVerticalRing(entity, ParticleTypes.CLOUD, 1.0);
                    break;
                case ELECTRIC_SHOCK:
                    spawnElectricEffect(entity);
                    break;
                case OVERLOAD:
                    spawnExplosionRing(entity);
                    break;
                case BURNING:
                    spawnBurningEffect(entity);
                    break;
                case BLOOM:
                    spawnPlantEffect(entity);
                    break;
                default:
                    spawnDefaultReactionEffect(entity);
                    break;
            }
        }
    }
    
    private static void spawnVerticalRing(LivingEntity entity, IParticleData particle, double height) {
        Vector3d pos = entity.position().add(0, height, 0);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            double x = Math.cos(angle) * 1.0;
            double z = Math.sin(angle) * 1.0;
            
            entity.level.addParticle(
                particle, 
                pos.x + x, pos.y, pos.z + z, 
                x * 0.1, 0.05, z * 0.1
            );
        }
    }
    
    private static void spawnElectricEffect(LivingEntity entity) {
        Vector3d pos = entity.position().add(0, entity.getBbHeight() * 0.8, 0);
        for (int i = 0; i < 10; i++) {
            double dx = (random.nextDouble() - 0.5) * 2.0;
            double dy = random.nextDouble() * 0.5;
            double dz = (random.nextDouble() - 0.5) * 2.0;
            
            entity.level.addParticle(
                new RedstoneParticleData(0.5f, 0.0f, 1.0f, 1.0f), 
                pos.x, pos.y, pos.z, 
                dx * 0.1, dy * 0.2, dz * 0.1
            );
        }
    }
    
    private static void spawnExplosionRing(LivingEntity entity) {
        Vector3d center = entity.position().add(0, entity.getBbHeight() / 2, 0);
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12;
            double speed = random.nextDouble() * 0.1 + 0.1;
            double x = Math.cos(angle) * speed;
            double z = Math.sin(angle) * speed;
            
            entity.level.addParticle(
                ParticleTypes.FLAME, 
                center.x, center.y, center.z, 
                x, 0, z
            );
            
            entity.level.addParticle(
                ParticleTypes.SMOKE, 
                center.x, center.y, center.z, 
                x * 0.7, 0.05, z * 0.7
            );
        }
    }
    
    private static void spawnBurningEffect(LivingEntity entity) {
        Vector3d pos = entity.position();
        for (int i = 0; i < 20; i++) {
            double x = (random.nextDouble() - 0.5) * 2.0;
            double y = random.nextDouble() * entity.getBbHeight();
            double z = (random.nextDouble() - 0.5) * 2.0;
            
            entity.level.addParticle(
                ParticleTypes.FLAME, 
                pos.x + x, pos.y + y, pos.z + z, 
                0, 0.05, 0
            );
            
            // 添加烟雾效果
            entity.level.addParticle(
                ParticleTypes.LARGE_SMOKE, 
                pos.x + x * 0.5, pos.y + y, pos.z + z * 0.5, 
                0, 0.02, 0
            );
        }
    }
    
    private static void spawnPlantEffect(LivingEntity entity) {
        Vector3d pos = entity.position().add(0, 0.1, 0);
        for (int i = 0; i < 15; i++) {
            double x = (random.nextDouble() - 0.5) * 1.5;
            double z = (random.nextDouble() - 0.5) * 1.5;
            
            entity.level.addParticle(
                new RedstoneParticleData(0.2f, 1.0f, 0.0f, 1.0f), 
                pos.x + x, pos.y, pos.z + z, 
                0, 0.15, 0
            );
            
            if (random.nextDouble() < 0.3) {
                entity.level.addParticle(
                    ParticleTypes.DRIPPING_HONEY, 
                    pos.x + x * 0.8, pos.y + 0.2, pos.z + z * 0.8, 
                    0, 0.05, 0
                );
            }
        }
    }
    
    private static void spawnDefaultReactionEffect(LivingEntity entity) {
        Vector3d pos = entity.position();
        for (int i = 0; i < 10; i++) {
            entity.level.addParticle(
                ParticleTypes.END_ROD, 
                pos.x, pos.y + entity.getBbHeight() * random.nextDouble(), pos.z, 
                0, 0.1, 0
            );
        }
    }
    
    /**
     * 元素反应类型，用于确定需要显示的粒子效果
     */
    public enum ReactionType {
        EVAPORATE,      // 蒸发
        MELT,           // 融化
        ELECTRIC_SHOCK,  // 感电
        OVERLOAD,       // 超载
        BURNING,        // 燃烧
        BLOOM,          // 绽放
        // 可以添加更多反应类型...
        DEFAULT
    }
}


