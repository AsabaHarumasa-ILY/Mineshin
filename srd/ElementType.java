package com.AsabaHarumasa.elementalreactions;

import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.particles.IParticleData;
import java.awt.Color;

public enum ElementType {
    NONE(0xFFFFFF, new RedstoneParticleData(1.0f, 1.0f, 1.0f, 1.0f), "none"),
    WATER(0x0000FF, ParticleTypes.SPLASH, "water"),
    FIRE(0xFF0000, ParticleTypes.FLAME, "fire"),
    ELECTRO(0x800080, new RedstoneParticleData(0.5f, 0.0f, 1.0f, 1.0f), "electro"),
    ICE(0x00FFFF, ParticleTypes.ITEM_SNOWBALL, "ice"),
    WIND(0x00FF00, ParticleTypes.CLOUD, "wind"),
    GRASS(0x32CD32, new RedstoneParticleData(0.2f, 1.0f, 0.0f, 1.0f), "grass"),
    ROCK(0xDAA520, new RedstoneParticleData(0.85f, 0.7f, 0.0f, 1.0f), "rock");

    private final int color;
    private final IParticleData particleData;
    private final String iconName;
    
    // 资源位置缓存
    private ResourceLocation iconResource;

    ElementType(int color, IParticleData particleData, String iconName) {
        this.color = color;
        this.particleData = particleData;
        this.iconName = iconName;
        this.iconResource = new ResourceLocation(ElementalReactionsMod.MODID, "textures/gui/elements/" + iconName + ".png");
    }

    public int getColor() {
        return color;
    }

    public static ElementType fromId(int id) {
        if (id < 0 || id >= values().length) return NONE;
        return values()[id];
    }

    public int getId() {
        return this.ordinal();
    }

    public IParticleData getParticleData() {
        return particleData;
    }
    
    public ResourceLocation getIconResource() {
        return iconResource;
    }

    public String getIconName() {
        return iconName;
    }
    
    // 获取对应的颜色值用于粒子效果
    public float getRed() {
        return ((color >> 16) & 0xFF) / 255.0f;
    }
    
    public float getGreen() {
        return ((color >> 8) & 0xFF) / 255.0f;
    }
    
    public float getBlue() {
        return (color & 0xFF) / 255.0f;
    }
    
    // 检查是否有自定义粒子数据
    public boolean hasCustomParticle() {
        return particleData != ParticleTypes.SPLASH && 
               particleData != ParticleTypes.FLAME &&
               particleData != ParticleTypes.ITEM_SNOWBALL &&
               particleData != ParticleTypes.CLOUD;
    }
}


