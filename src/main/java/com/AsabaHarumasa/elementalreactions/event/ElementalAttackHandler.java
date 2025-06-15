package com.AsabaHarumasa.elementalreactions.event;

import com.AsabaHarumasa.elementalreactions.ElementType;
import com.AsabaHarumasa.elementalreactions.ElementalAttachHandler;
import com.AsabaHarumasa.elementalreactions.ElementalReactionHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber
public class ElementalAttackHandler {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (source.getImmediateSource() != null && source.getImmediateSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) source.getImmediateSource();
            LivingEntity target = event.getEntityLiving();
            
            // 获取攻击携带的元素类型
            ElementType weaponElement = getWeaponElement(attacker);
            
            // 附加武器元素到目标
            if (weaponElement != ElementType.NONE) {
                ElementalAttachHandler.attachElement(target, weaponElement, 300);
            }
            
            // 特殊反应：冰攻击水附着目标
            if (weaponElement == ElementType.ICE && ElementalAttachHandler.hasElement(target, ElementType.WATER)) {
                handleFreezeReaction(attacker, target); // 冻结反应
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getTrueSource() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) event.getSource().getTrueSource();
            LivingEntity target = event.getEntityLiving();
            
            // 获取所有相关元素类型
            ElementType weaponElement = getWeaponElement(attacker);
            ElementType targetElement = ElementalAttachHandler.getAttachedElement(target.getUniqueID());
            
            // 计算元素反应伤害加成
            float damageMultiplier = calculateReactionDamage(attacker, target, weaponElement, targetElement);
            
            // 应用伤害加成
            if (damageMultiplier != 1.0f) {
                event.setAmount(event.getAmount() * damageMultiplier);
            }
        }
    }
    
    private static ElementType getWeaponElement(LivingEntity attacker) {
        if (attacker instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) attacker;
            ItemStack mainHandItem = player.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
            
            // 1. 检查附魔元素
            if (mainHandItem.getEnchantmentTags().toString().contains("fire_aspect")) {
                return ElementType.FIRE;
            } else if (mainHandItem.getEnchantmentTags().toString().contains("frost_walker")) {
                return ElementType.ICE;
            } else if (mainHandItem.getEnchantmentTags().toString().contains("channeling")) {
                return ElementType.ELECTRO;
            }
            
            // 2. 检查物品本身材质类型
            String itemName = mainHandItem.getItem().getRegistryName().getPath().toLowerCase();
            if (itemName.contains("fire") || itemName.contains("flame")) {
                return ElementType.FIRE;
            } else if (itemName.contains("ice") || itemName.contains("frost")) {
                return ElementType.ICE;
            } else if (itemName.contains("thunder") || itemName.contains("lightning")) {
                return ElementType.ELECTRO;
            } else if (itemName.contains("water") || itemName.contains("aqua")) {
                return ElementType.WATER;
            }
        }
        return ElementType.NONE;
    }
    
    private static float calculateReactionDamage(LivingEntity attacker, LivingEntity target,
                                               ElementType weaponElement, ElementType targetElement) {
        // 1. 蒸发/融化反应 (2倍伤害)
        if ((weaponElement == ElementType.WATER && targetElement == ElementType.FIRE) ||
            (weaponElement == ElementType.ICE && targetElement == ElementType.FIRE)) {
            triggerParticleEffect(target, ElementType.FIRE);
            return 2.0f;
        }
        
        // 2. 融化反应 (1.5倍伤害)
        if ((weaponElement == ElementType.FIRE && targetElement == ElementType.ICE)) {
            triggerParticleEffect(target, ElementType.ICE);
            return 1.5f;
        }
        
        // 3. 超导反应 (1.5倍伤害)
        if ((weaponElement == ElementType.ICE && targetElement == ElementType.ELECTRO) ||
            (weaponElement == ElementType.ELECTRO && targetElement == ElementType.ICE)) {
            applySuperconductReaction(target);
            return 1.5f;
        }
        
        // 4. 感电反应 (2倍伤害+链式闪电)
        if ((weaponElement == ElementType.WATER && targetElement == ElementType.ELECTRO) ||
            (weaponElement == ElementType.ELECTRO && targetElement == ElementType.WATER)) {
            applyElectroChargedReaction(target);
            return 2.0f;
        }
        
        // 5. 超载反应 (2.75倍伤害+击飞)
        if ((weaponElement == ElementType.FIRE && targetElement == ElementType.ELECTRO) ||
            (weaponElement == ElementType.ELECTRO && targetElement == ElementType.FIRE)) {
            applyOverloadReaction(attacker, target);
            return 2.75f;
        }
        
        // 6. 燃烧反应 (1.25倍持续伤害)
        if ((weaponElement == ElementType.FIRE && targetElement == ElementType.GRASS) ||
            (weaponElement == ElementType.GRASS && targetElement == ElementType.FIRE)) {
            applyBurningReaction(target);
            return 1.25f;
        }
        
        return 1.0f;
    }
    
    private static void handleFreezeReaction(LivingEntity attacker, LivingEntity target) {
        // 应用冻结效果
        target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 200, 100)); // 10秒最大减速
        target.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 200, 100)); // 降低攻击速度
        
        // 添加冰冻粒子效果
        triggerParticleEffect(target, ElementType.ICE);
        
        // 播放音效
        target.world.playSound(null, target.getPosition(), SoundEvents.BLOCK_GLASS_BREAK, 
                             SoundCategory.PLAYERS, 0.8f, 0.5f);
    }
    
    private static void applySuperconductReaction(LivingEntity target) {
        // 应用区域物理伤害减免
        AxisAlignedBB area = target.getBoundingBox().grow(3.0);
        List<LivingEntity> entities = target.world.getEntitiesWithinAABB(LivingEntity.class, area);
        
        for (LivingEntity entity : entities) {
            if (entity != target) {
                entity.addPotionEffect(new EffectInstance(Effects.RESISTANCE, 100, 1)); // 5秒伤害减免
            }
        }
        
        // 添加特效
        triggerParticleEffect(target, ElementType.ELECTRO);
    }
    
    private static void applyElectroChargedReaction(LivingEntity target) {
        // 触发连锁闪电效果
        AxisAlignedBB area = target.getBoundingBox().grow(5.0);
        List<LivingEntity> entities = target.world.getEntitiesWithinAABB(LivingEntity.class, area);
        
        for (LivingEntity entity : entities) {
            if (entity != target && (ElementalAttachHandler.hasElement(entity, ElementType.WATER) || 
                                   ElementalAttachHandler.hasElement(entity, ElementType.ELECTRO))) {
                entity.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 3.0f);
                triggerParticleEffect(entity, ElementType.ELECTRO);
            }
        }
        
        // 5秒后清除所有水/雷元素附着
        ElementalAttachHandler.scheduleElementRemoval(target, 100, ElementType.WATER);
        ElementalAttachHandler.scheduleElementRemoval(target, 100, ElementType.ELECTRO);
    }
    
    private static void applyOverloadReaction(LivingEntity attacker, LivingEntity target) {
        // 击飞效果
        Vector3d knockbackDirection = attacker.getPositionVec().subtract(target.getPositionVec()).normalize();
        target.addVelocity(knockbackDirection.x * 0.8, 0.5, knockbackDirection.z * 0.8);
        
        // 爆炸效果
        target.world.createExplosion(attacker, target.getPosX(), target.getPosY(), target.getPosZ(), 
                                   1.5f, World.ExplosionInteraction.NONE);
        
        // 区域击退
        AxisAlignedBB area = target.getBoundingBox().grow(5.0);
        List<LivingEntity> entities = target.world.getEntitiesWithinAABB(LivingEntity.class, area);
        
        for (LivingEntity entity : entities) {
            if (entity != target && entity != attacker) {
                Vector3d dirToEntity = target.getPositionVec().subtract(entity.getPositionVec()).normalize();
                entity.addVelocity(dirToEntity.x * 0.6, 0.3, dirToEntity.z * 0.6);
            }
        }
        
        // 特效和音效
        triggerParticleEffect(target, ElementType.FIRE);
        target.world.playSound(null, target.getPosition(), SoundEvents.ENTITY_GENERIC_EXPLODE, 
                             SoundCategory.PLAYERS, 1.0f, 0.8f);
    }
    
    private static void applyBurningReaction(LivingEntity target) {
        // 应用燃烧效果 (10秒)
        target.setFire(10);
        
        // 引燃5格半径内的草地
        World world = target.world;
        BlockPos pos = target.getPosition();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos blockPos = pos.add(x, 0, z);
                if (world.getBlockState(blockPos).getBlock() == Blocks.GRASS_BLOCK) {
                    world.setBlockState(blockPos, Blocks.DIRT.getDefaultState());
                    
                    // 对范围内所有实体造成伤害
                    world.getEntitiesWithinAABB(LivingEntity.class, 
                         new AxisAlignedBB(blockPos).grow(5.0)).forEach(entity -> {
                         entity.setFire(5);
                         entity.attackEntityFrom(DamageSource.IN_FIRE, 2.0f);
                    });
                }
            }
        }
        
        // 特效和音效
        triggerParticleEffect(target, ElementType.FIRE);
        world.playSound(null, target.getPosition(), SoundEvents.BLOCK_FIRE_AMBIENT, 
                      SoundCategory.PLAYERS, 0.8f, 1.0f);
    }
    
    private static void triggerParticleEffect(LivingEntity entity, ElementType element) {
        // 基于元素类型触发不同粒子效果
        World world = entity.world;
        Vector3d pos = entity.getPositionVec();
        
        double offset = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2;
        
        switch (element) {
            case FIRE:
                for (int i = 0; i < 10; i++) {
                    world.addParticle(ParticleTypes.FLAME, 
                        pos.x + offset, pos.y + 1, pos.z + offset, 
                        offset * 0.1, 0.1, offset * 0.1);
                }
                break;
            case WATER:
                for (int i = 0; i < 15; i++) {
                    world.addParticle(ParticleTypes.RAIN, 
                        pos.x + offset, pos.y + 1, pos.z + offset, 
                        0, 0.1, 0);
                }
                break;
            case ELECTRO:
                for (int i = 0; i < 8; i++) {
                    world.addParticle(ParticleTypes.ELECTRIC_SPARK, 
                        pos.x + offset, pos.y + 1, pos.z + offset, 
                        offset * 0.2, 0.1, offset * 0.2);
                }
                break;
            case ICE:
                for (int i = 0; i < 12; i++) {
                    world.addParticle(ParticleTypes.ITEM_SNOWBALL, 
                        pos.x + offset, pos.y + 1, pos.z + offset, 
                        0, 0.05, 0);
                }
                break;
            case GRASS:
                for (int i = 0; i < 6; i++) {
                    world.addParticle(ParticleTypes.HAPPY_VILLAGER, 
                        pos.x + offset, pos.y + 1, pos.z + offset, 
                        offset * 0.1, 0.1, offset * 0.1);
                }
                break;
        }
    }
}



