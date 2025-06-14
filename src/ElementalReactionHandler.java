package com.AsabaHarumasa.elementalreactions;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ElementalReactionHandler {
    // 反应冷却时间（12秒，240 ticks）
    private static final int REACTION_COOLDOWN = 240;
    // 特殊反应持续时间
    private static final int BURNING_DURATION = 200; // 10秒
    private static final int ELECTROCUTED_DURATION = 100; // 5秒
    
    // 存储反应冷却
    private static final Map<UUID, CooldownEntry> reactionCooldowns = new ConcurrentHashMap<>();
    // 存储实体上的特殊反应（燃烧、感电等）
    private static final Map<UUID, SpecialReactionEntry> specialReactions = new ConcurrentHashMap<>();
    // 冻结状态存储
    private static final Map<UUID, FreezeEntry> frozenEntities = new ConcurrentHashMap<>();
    
    // 处理元素相遇时的反应
    public static void handleReaction(LivingEntity entity, ElementType existing, ElementType newElement) {
        if (reactionCooldowns.containsKey(entity.getUUID())) return;
        
        World world = entity.level;
        Vector3d pos = entity.position();
        
        // 蒸发反应 (水+火)
        if ((existing == ElementType.WATER && newElement == ElementType.FIRE) ||
            (existing == ElementType.FIRE && newElement == ElementType.WATER)) {
            boolean isWaterFirst = existing == ElementType.WATER;
            triggerEvaporation(entity, isWaterFirst);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 融化反应 (冰+火)
        else if ((existing == ElementType.ICE && newElement == ElementType.FIRE) ||
                 (existing == ElementType.FIRE && newElement == ElementType.ICE)) {
            boolean isFireFirst = existing == ElementType.FIRE;
            triggerMelt(entity, isFireFirst);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 超导反应 (冰+雷)
        else if ((existing == ElementType.ICE && newElement == ElementType.ELECTRO) ||
                 (existing == ElementType.ELECTRO && newElement == ElementType.ICE)) {
            triggerSuperconduct(entity);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 扩散反应 (风+水/火/雷/冰)
        else if (existing == ElementType.WIND && 
                (newElement == ElementType.WATER || newElement == ElementType.FIRE || 
                 newElement == ElementType.ELECTRO || newElement == ElementType.ICE)) {
            triggerDiffusion(entity, newElement);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        else if (newElement == ElementType.WIND && 
                (existing == ElementType.WATER || existing == ElementType.FIRE || 
                 existing == ElementType.ELECTRO || existing == ElementType.ICE)) {
            triggerDiffusion(entity, existing);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 感电反应 (水+雷)
        else if ((existing == ElementType.WATER && newElement == ElementType.ELECTRO) ||
                 (existing == ElementType.ELECTRO && newElement == ElementType.WATER)) {
            triggerElectrocution(entity);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 冻结反应 (水+冰)
        else if ((existing == ElementType.WATER && newElement == ElementType.ICE) ||
                 (existing == ElementType.ICE && newElement == ElementType.WATER)) {
            triggerFreeze(entity);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 超载反应 (雷+火)
        else if ((existing == ElementType.ELECTRO && newElement == ElementType.FIRE) ||
                 (existing == ElementType.FIRE && newElement == ElementType.ELECTRO)) {
            triggerOverload(entity);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 燃烧反应 (草+火)
        else if ((existing == ElementType.GRASS && newElement == ElementType.FIRE) ||
                 (existing == ElementType.FIRE && newElement == ElementType.GRASS)) {
            triggerBurning(entity);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 绽放反应 (草+水)
        else if ((existing == ElementType.GRASS && newElement == ElementType.WATER) ||
                 (existing == ElementType.WATER && newElement == ElementType.GRASS)) {
            triggerBloom(entity);
            reactionCooldowns.put(entity.getUUID(), new CooldownEntry());
        }
        // 激化反应 (草+雷) - 需要额外的元素
        else if ((existing == ElementType.GRASS && newElement == ElementType.ELECTRO) ||
                 (existing == ElementType.ELECTRO && newElement == ElementType.GRASS)) {
            // 延迟激化反应，可能需要第三个元素
        }
    }
    
    // ==== 具体反应实现 ====
    
    private static void triggerEvaporation(LivingEntity entity, boolean isWaterFirst) {
        float damage = isWaterFirst ? 10.0f : 7.5f; // 2x or 1.5x伤害
        entity.hurt(DamageSource.MAGIC, damage);
        playReactionEffects(entity.level, entity.position(), ElementType.FIRE);
    }
    
    private static void triggerMelt(LivingEntity entity, boolean isFireFirst) {
        float damage = isFireFirst ? 7.5f : 10.0f; // 1.5x or 2x伤害
        entity.hurt(DamageSource.MAGIC, damage);
        playReactionEffects(entity.level, entity.position(), ElementType.FIRE);
    }
    
    private static void triggerSuperconduct(LivingEntity entity) {
        entity.hurt(DamageSource.MAGIC, 7.5f); // 1.5x伤害
        
        // 查找附近的实体并造成伤害
        findNearbyEntities(entity, 5.0).forEach(e -> {
            if (e != entity) {
                e.hurt(DamageSource.MAGIC, 7.5f);
            }
        });
        playReactionEffects(entity.level, entity.position(), ElementType.ICE);
    }
    
    private static void triggerDiffusion(LivingEntity entity, ElementType element) {
        entity.hurt(DamageSource.MAGIC, 8.0f); // 1.6x伤害
        
        // 扩散伤害到附近的实体
        findNearbyEntities(entity, 5.0).forEach(e -> {
            if (e != entity) {
                e.hurt(DamageSource.MAGIC, 8.0f);
                ElementalAttachHandler.attachElement(e, element, 300);
            }
        });
        playReactionEffects(entity.level, entity.position(), element);
    }
    
    private static void triggerElectrocution(LivingEntity entity) {
        // 立即伤害
        entity.hurt(DamageSource.MAGIC, 10.0f); // 2x伤害
        playReactionEffects(entity.level, entity.position(), ElementType.ELECTRO);
        
        // 添加感电状态
        specialReactions.put(entity.getUUID(), 
            new SpecialReactionEntry(ElementType.ELECTRO, ELECTROCUTED_DURATION, 3.0f));
    }
    
    private static void triggerFreeze(LivingEntity entity) {
        // 冻结实体
        frozenEntities.put(entity.getUUID(), new FreezeEntry());
        playReactionEffects(entity.level, entity.position(), ElementType.ICE);
    }
    
    private static void triggerOverload(LivingEntity entity) {
        // 爆炸伤害
        float damage = 13.75f; // 2.75x伤害
        entity.hurt(DamageSource.MAGIC, damage);
        
        // 击飞附近的实体
        for (Entity e : findNearbyEntities(entity, 5.0)) {
            if (e != entity) {
                double dx = e.getX() - entity.getX();
                double dz = e.getZ() - entity.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < 0.01) distance = 1.0;
                double knockbackX = dx / distance * 2.0;
                double knockbackZ = dz / distance * 2.0;
                e.push(knockbackX, 0.5, knockbackZ);
            }
        }
        playReactionEffects(entity.level, entity.position(), ElementType.FIRE);
    }
    
    private static void triggerBurning(LivingEntity entity) {
        // 立即伤害
        entity.hurt(DamageSource.MAGIC, 6.25f); // 1.25x伤害
        
        // 添加燃烧状态
        specialReactions.put(entity.getUUID(), 
            new SpecialReactionEntry(ElementType.FIRE, BURNING_DURATION, 2.0f));
        
        // 引燃周围草方块
        if (!entity.level.isClientSide) {
            BlockPos center = entity.blockPosition();
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = center.offset(x, 0, z);
                    if (entity.level.getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK) {
                        entity.level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                    }
                }
            }
        }
        playReactionEffects(entity.level, entity.position(), ElementType.FIRE);
    }
    
    private static void triggerBloom(LivingEntity entity) {
        // 绽放伤害
        entity.hurt(DamageSource.MAGIC, 10.0f); // 2x伤害
        playReactionEffects(entity.level, entity.position(), ElementType.GRASS);
        
        // 添加绽放状态
        specialReactions.put(entity.getUUID(), 
            new SpecialReactionEntry(ElementType.GRASS, 300, 0.0f));
    }
    
    public static void triggerCatalyze(LivingEntity entity, ElementType triggerElement) {
        // 蔓激化或超激化
        ElementalAttachHandler.ElementAttachment grassAttachment = ElementalAttachHandler.getAttachment(entity.getUUID());
        ElementalAttachHandler.ElementAttachment electroAttachment = ElementalAttachHandler.getAttachment(entity.getUUID());
        
        if (grassAttachment != null && grassAttachment.getElement() == ElementType.GRASS &&
            electroAttachment != null && electroAttachment.getElement() == ElementType.ELECTRO) {
            
            if (triggerElement == ElementType.GRASS) {
                // 超激化 (草+雷+草)
                triggerHyperCatalyze(entity);
            } else if (triggerElement == ElementType.ELECTRO) {
                // 蔓激化 (草+雷+雷)
                triggerQuickCatalyze(entity);
            }
            
            // 清除元素
            ElementalAttachHandler.removeElement(entity, ElementType.GRASS);
            ElementalAttachHandler.removeElement(entity, ElementType.ELECTRO);
        }
    }
    
    private static void triggerQuickCatalyze(LivingEntity entity) {
        // 蔓激化 - 对最近目标造成伤害
        Entity closest = findClosestEntity(entity, 8.0);
        if (closest instanceof LivingEntity) {
            ((LivingEntity) closest).hurt(DamageSource.MAGIC, 6.25f); // 1.25x伤害
            ElementalAttachHandler.attachElement((LivingEntity) closest, ElementType.GRASS, 300);
        }
        playReactionEffects(entity.level, entity.position(), ElementType.GRASS);
    }
    
    private static void triggerHyperCatalyze(LivingEntity entity) {
        // 超激化 - 对范围内的目标造成伤害
        findNearbyEntities(entity, 5.0).forEach(e -> {
            if (e != entity && e instanceof LivingEntity) {
                ((LivingEntity) e).hurt(DamageSource.MAGIC, 5.75f); // 1.15x伤害
                ElementalAttachHandler.attachElement((LivingEntity) e, ElementType.ELECTRO, 300);
            }
        });
        playReactionEffects(entity.level, entity.position(), ElementType.ELECTRO);
    }
    
    // ===== 事件处理 =====
    
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        // 检查是否有元素附魔的武器
        if (event.getSource().getDirectEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getSource().getDirectEntity();
            ElementType weaponElement = getWeaponElement(player);
            
            if (weaponElement != ElementType.NONE) {
                // 附加元素
                ElementalAttachHandler.attachElement(event.getEntityLiving(), weaponElement, 300);
                
                // 检查绽放后的反应
                SpecialReactionEntry bloomEntry = specialReactions.get(event.getEntityLiving().getUUID());
                if (bloomEntry != null && bloomEntry.element == ElementType.GRASS) {
                    if (weaponElement == ElementType.FIRE) {
                        // 烈绽放
                        event.getEntityLiving().hurt(DamageSource.MAGIC, 15.0f);
                        playReactionEffects(event.getEntity().level, event.getEntity().position(), ElementType.FIRE);
                        specialReactions.remove(event.getEntityLiving().getUUID());
                    } else if (weaponElement == ElementType.ELECTRO) {
                        // 超绽放
                        event.getEntityLiving().hurt(DamageSource.MAGIC, 15.0f);
                        playReactionEffects(event.getEntity().level, event.getEntity().position(), ElementType.ELECTRO);
                        specialReactions.remove(event.getEntityLiving().getUUID());
                    }
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerAttack(AttackEntityEvent event) {
        // 处理冻结实体
        if (event.getTarget() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getTarget();
            FreezeEntry freezeEntry = frozenEntities.get(target.getUUID());
            
            if (freezeEntry != null) {
                freezeEntry.hits++;
                
                // 第三次攻击触发碎冰
                if (freezeEntry.hits >= 3) {
                    target.hurt(DamageSource.MAGIC, 15.0f); // 3x伤害
                    playReactionEffects(target.level, target.position(), ElementType.ICE);
                    frozenEntities.remove(target.getUUID());
                }
            }
        }
    }
    
    // 激化反应检查
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getTarget();
            triggerCatalyze(target, ElementType.GRASS); // 假设使用草元素触发
        }
    }
    
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isClientSide) {
            // 处理反应冷却
            reactionCooldowns.entrySet().removeIf(entry -> entry.getValue().tick());
            
            // 处理特殊效果
            specialReactions.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().tick();
                if (!expired) {
                    applySpecialEffect(entry.getKey(), entry.getValue());
                }
                return expired;
            });
            
            // 处理冻结状态
            frozenEntities.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().tick();
                Entity entity = event.world.getEntity(entry.getKey());
                if (entity != null) {
                    if (expired) {
                        // 移除冻结效果
                    } else {
                        // 应用冻结效果
                        entity.setDeltaMovement(0, 0, 0); // 冻结移动
                    }
                }
                return expired || entity == null;
            });
        }
    }
    
    // ==== 辅助方法 ====
    
    private static ElementType getWeaponElement(PlayerEntity player) {
        // 检查武器上的火焰附加附魔
        if (ForgeHooks.getEnchantmentLevel(net.minecraft.enchantment.Enchantments.FIRE_ASPECT, player) > 0) {
            return ElementType.FIRE;
        }
        // TODO: 添加其他元素的附魔支持
        return ElementType.NONE;
    }
    
    private static List<Entity> findNearbyEntities(Entity center, double radius) {
        AxisAlignedBB area = center.getBoundingBox().inflate(radius);
        return center.level.getEntities(center, area);
    }
    
    private static Entity findClosestEntity(Entity center, double radius) {
        List<Entity> entities = findNearbyEntities(center, radius);
        entities.remove(center);
        
        if (entities.isEmpty()) return null;
        
        // 找到最近的实体
        Entity closest = null;
        double minDistance = Double.MAX_VALUE;
        Vector3d centerPos = center.position();
        
        for (Entity entity : entities) {
            double distance = entity.distanceToSqr(centerPos.x, centerPos.y, centerPos.z);
            if (distance < minDistance) {
                minDistance = distance;
                closest = entity;
            }
        }
        
        return closest;
    }
    
    private static void applySpecialEffect(UUID entityId, SpecialReactionEntry entry) {
        Entity entity = null;
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            
            switch (entry.element) {
                case FIRE: // 燃烧
                    living.hurt(DamageSource.MAGIC, entry.damage);
                    playReactionEffects(living.level, living.position(), ElementType.FIRE);
                    break;
                    
                case ELECTRO: // 感电
                    for (Entity nearby : findNearbyEntities(living, 5.0)) {
                        if (nearby != living && 
                            (ElementalAttachHandler.hasElement(living, ElementType.WATER) || 
                             ElementalAttachHandler.hasElement(living, ElementType.ELECTRO))) {
                            nearby.hurt(DamageSource.MAGIC, entry.damage);
                        }
                    }
                    playReactionEffects(living.level, living.position(), ElementType.ELECTRO);
                    break;
                    
                case GRASS: // 绽放状态（无持续伤害）
                    // 仅等待触发
                    break;
            }
        }
    }
    
    private static void playReactionEffects(World world, Vector3d pos, ElementType element) {
        if (!world.isClientSide && world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            IParticleData particle = element.getParticleData();
            if (particle == null) {
                Color color = new Color(element.getColor());
                particle = new RedstoneParticleData(
                    color.getRed() / 255.0F, 
                    color.getGreen() / 255.0F, 
                    color.getBlue() / 255.0F, 
                    1.0F
                );
            }
            
            // 创建元素反应粒子效果
            for (int i = 0; i < 30; i++) {
                double x = pos.x + (Math.random() - 0.5) * 2.0;
                double y = pos.y + Math.random() * 2.0;
                double z = pos.z + (Math.random() - 0.5) * 2.0;
                serverWorld.sendParticles(particle, x, y, z, 1, 0, 0, 0, 0);
            }
            
            // 播放反应音效
            world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, 
                           SoundCategory.PLAYERS, 0.5f, 1.0f);
        }
    }
    
    // ==== 内部类 ====
    
    private static class CooldownEntry {
        private int cooldown = REACTION_COOLDOWN;
        
        public boolean tick() {
            return --cooldown <= 0;
        }
    }
    
    private static class SpecialReactionEntry {
        public ElementType element;
        public int duration;
        public float damage;
        
        public SpecialReactionEntry(ElementType element, int duration, float damage) {
            this.element = element;
            this.duration = duration;
            this.damage = damage;
        }
        
        public boolean tick() {
            return --duration <= 0;
        }
    }
    
    private static class FreezeEntry {
        public int hits = 0;
        private int duration = 300; // 15秒
        
        public boolean tick() {
            return --duration <= 0;
        }
    }
}


