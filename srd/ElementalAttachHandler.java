package com.AsabaHarumasa.elementalreactions;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CauldronBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ElementalAttachHandler {
    private static final Map<UUID, ElementAttachment> entityElements = new ConcurrentHashMap<>();
    private static final int BASE_DURATION = 300; // 15秒 (300 ticks)
    
    public static void init() {
        // 初始化代码（如有需要）
    }
    
    /**
     * 检测环境中的元素源并附着元素
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEntityTick(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide) return;
        
        // 处理元素衰减
        processElementDecay(entity);
        
        // 检测环境元素源（不包括草地/树叶）
        checkEnvironmentalElements(entity);
    }
    
    /**
     * 处理来自攻击的元素附着
     */
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntityLiving();
        DamageSource source = event.getSource();
        
        // 来自武器攻击的元素
        if (source.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) source.getEntity();
            ElementType weaponElement = getWeaponElement(attacker);
            if (weaponElement != ElementType.NONE && weaponElement != ElementType.GRASS) {
                attachElement(target, weaponElement, BASE_DURATION);
            }
        }
        
        // 来自投射物的元素
        if (source.getDirectEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) source.getDirectEntity();
            if (arrow.getOwner() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) arrow.getOwner();
                ElementType arrowElement = getArrowElement(shooter, arrow);
                if (arrowElement != ElementType.NONE && arrowElement != ElementType.GRASS) {
                    attachElement(target, arrowElement, BASE_DURATION);
                }
            }
        }
        
        // 来自烟花火箭的元素
        if (source.getDirectEntity() instanceof FireworkRocketEntity) {
            attachElement(target, ElementType.FIRE, BASE_DURATION);
        }
        
        // 来自特殊伤害源的元素
        if (source == DamageSource.DROWN) {
            attachElement(target, ElementType.WATER, BASE_DURATION);
        } else if (source == DamageSource.IN_FIRE || source == DamageSource.ON_FIRE ||
                   source == DamageSource.LAVA || source == DamageSource.HOT_FLOOR) {
            attachElement(target, ElementType.FIRE, BASE_DURATION);
        } else if (source == DamageSource.LIGHTNING_BOLT) {
            attachElement(target, ElementType.ELECTRO, BASE_DURATION);
        }
    }
    
    /**
     * 处理实体交互导致的元素附着
     */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        BlockPos pos = event.getPos();
        BlockState state = event.getWorld().getBlockState(pos);
        
        // 在盛水炼药锅中右键
        if (state.getBlock() instanceof CauldronBlock && 
            state.getValue(CauldronBlock.LEVEL) > 0 &&
            player.getItemInHand(event.getHand()).isEmpty()) {
            attachElement(player, ElementType.WATER, BASE_DURATION);
        }
    }
    
    /**
     * 处理方块被破坏时产生的元素
     */
    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        PlayerEntity player = event.getPlayer();
        
        // 冰被破坏时产生冰元素
        if (state.getBlock() == Blocks.ICE || state.getBlock() == Blocks.PACKED_ICE ||
            state.getBlock() == Blocks.BLUE_ICE || state.getBlock() == Blocks.FROSTED_ICE) {
            attachElement(player, ElementType.ICE, BASE_DURATION);
        }
    }
    
    /**
     * 处理投射物击中实体时的元素附着
     */
    @SubscribeEvent
    public void onProjectileHit(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof AbstractArrowEntity &&
            event.getRayTraceResult().getType() == net.minecraft.util.math.RayTraceResult.Type.ENTITY) {
            AbstractArrowEntity arrow = (AbstractArrowEntity) event.getProjectile();
            if (arrow.getOwner() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) arrow.getOwner();
                net.minecraft.entity.Entity target = ((net.minecraft.util.math.EntityRayTraceResult) event.getRayTraceResult()).getEntity();
                if (target instanceof LivingEntity) {
                    ElementType arrowElement = getArrowElement(shooter, arrow);
                    if (arrowElement != ElementType.NONE && arrowElement != ElementType.GRASS) {
                        attachElement((LivingEntity) target, arrowElement, BASE_DURATION);
                    }
                }
            }
        }
    }
    
    /**
     * 处理元素衰减和反应
     */
    private void processElementDecay(LivingEntity entity) {
        if (entityElements.containsKey(entity.getUUID())) {
            ElementAttachment attachment = entityElements.get(entity.getUUID());
            attachment.tick();
            
            // 检查元素反应
            ElementalReactionHandler.checkReactions(entity, attachment.getElement());
            
            // 移除过期元素
            if (attachment.isExpired()) {
                entityElements.remove(entity.getUUID());
            } else if (attachment.isExpiring()) {
                // 在元素即将结束时传播元素
                spreadElement(entity, attachment.getElement());
            }
        }
    }
    
    /**
     * 检测环境中的元素源（忽略草地/树叶相关的草元素）
     */
    private void checkEnvironmentalElements(LivingEntity entity) {
        World world = entity.level;
        BlockPos pos = entity.blockPosition();
        Biome biome = world.getBiome(pos);
        
        // 水中元素检测
        if (entity.isInWater() || entity.isInWaterOrBubble()) {
            attachElement(entity, ElementType.WATER, BASE_DURATION);
        }
        
        // 雨中的元素检测
        if (world.isRainingAt(pos) && 
            (!world.isRainingAt(pos) || world.getHeight() <= pos.getY())) {
            attachElement(entity, ElementType.WATER, BASE_DURATION);
        }
        
        // 火中/熔岩中的元素检测
        if (entity.isInLava() || 
            entity.isOnFire() || 
            world.getBlockState(pos).getBlock() == Blocks.FIRE || 
            world.getBlockState(pos).getBlock() == Blocks.LAVA) {
            attachElement(entity, ElementType.FIRE, BASE_DURATION);
        }
        
        // 低温环境检测（忽略雪方块产生的草元素）
        if ((biome.getTemperature(pos) < 0.15 && world.getHeight() < pos.getY()) || 
            world.getBlockState(pos).getBlock() == Blocks.SNOW_BLOCK || 
            world.getBlockState(pos).getBlock() == Blocks.POWDER_SNOW) {
            attachElement(entity, ElementType.ICE, BASE_DURATION);
        }
        
        // 注意：这里完全跳过了草地和树叶的草元素检测
    }
    
    /**
     * 附着元素到实体
     */
    public static void attachElement(LivingEntity entity, ElementType element, int duration) {
        if (entity.level.isClientSide) return;
        
        entityElements.compute(entity.getUUID(), (uuid, existing) -> {
            // 无现有元素或已有相同元素
            if (existing == null || existing.getElement() == element) {
                return new ElementAttachment(element, duration);
            }
            
            // 多种元素共存处理（反应前判断）
            return ElementalReactionHandler.handleElementReaction(entity, existing, element);
        });
    }
    
    /**
     * 传播元素效果
     */
    private void spreadElement(LivingEntity source, ElementType element) {
        if (element == ElementType.FIRE) {
            // 火元素传播：点燃火焰
            World world = source.level;
            if (ForgeHooks.onLivingDestroyBlock(source, source.blockPosition(), world.getBlockState(source.blockPosition()))) {
                world.setBlock(source.blockPosition(), Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        // 其他元素传播逻辑可以在这里实现
    }
    
    /**
     * 获取武器元素类型
     */
    private ElementType getWeaponElement(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            ItemStack heldItem = player.getMainHandItem();
            
            // 火焰附加附魔
            if (heldItem.getEnchantmentTags().toString().contains("fire_aspect")) {
                return ElementType.FIRE;
            }
            
            // 特定物品
            if (heldItem.getItem() == Items.BLAZE_ROD) {
                return ElementType.FIRE;
            } else if (heldItem.getItem() == Items.WATER_BUCKET) {
                return ElementType.WATER;
            } else if (heldItem.getItem() == Items.PRISMARINE_SHARD) {
                return ElementType.WATER;
            } else if (heldItem.getItem() == Items.ICE) {
                return ElementType.ICE;
            } else if (heldItem.getItem() == Items.PACKED_ICE) {
                return ElementType.ICE;
            } else if (heldItem.getItem() == Items.GUNPOWDER) {
                return ElementType.FIRE;
            }
        }
        return ElementType.NONE;
    }
    
    /**
     * 获取箭矢元素类型
     */
    private ElementType getArrowElement(LivingEntity shooter, AbstractArrowEntity arrow) {
        // 火焰箭
        if (arrow.isOnFire()) {
            return ElementType.FIRE;
        }
        
        // 特殊的箭
        if (shooter instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) shooter;
            ItemStack bow = player.getUseItem();
            
            // 弓附魔
            if (bow.getEnchantmentTags().toString().contains("flame")) {
                return ElementType.FIRE;
            } else if (bow.getEnchantmentTags().toString().contains("frost")) {
                return ElementType.ICE;
            } else if (bow.getEnchantmentTags().toString().contains("shock")) {
                return ElementType.ELECTRO;
            }
        }
        
        // 特殊药水箭
        if (arrow.getEffects().stream().anyMatch(e -> e.getEffect().getRegistryName().toString().contains("freezing"))) {
            return ElementType.ICE;
        } else if (arrow.getEffects().stream().anyMatch(e -> e.getEffect().getRegistryName().toString().contains("shocking"))) {
            return ElementType.ELECTRO;
        }
        
        return ElementType.NONE;
    }
    
    /**
     * 实体是否附着指定元素
     */
    public static boolean hasElement(LivingEntity entity, ElementType type) {
        ElementAttachment attachment = entityElements.get(entity.getUUID());
        return attachment != null && attachment.getElement() == type;
    }
    
    /**
     * 获取实体附着的元素
     */
    public static ElementType getAttachedElement(UUID uuid) {
        ElementAttachment attachment = entityElements.get(uuid);
        return attachment != null ? attachment.getElement() : ElementType.NONE;
    }
}

/**
 * 元素附着信息类
 */
class ElementAttachment {
    private final ElementType element;
    private int duration;
    
    public ElementAttachment(ElementType element, int duration) {
        this.element = element;
        this.duration = duration;
    }
    
    /**
     * 每tick减少持续时间
     */
    public void tick() {
        if (duration > 0) duration--;
    }
    
    /**
     * 是否已过期
     */
    public boolean isExpired() {
        return duration <= 0;
    }
    
    /**
     * 是否即将结束（最后3秒）
     */
    public boolean isExpiring() {
        return duration > 0 && duration < 60; // 最后3秒
    }
    
    public ElementType getElement() {
        return element;
    }
    
    public int getRemainingTicks() {
        return duration;
    }
}


