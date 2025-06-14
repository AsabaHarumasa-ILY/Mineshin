package com.AsabaHarumasa.elementalreactions;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ElementHUD {
    private static final ResourceLocation ELEMENTS_TEXTURE = 
        new ResourceLocation(ElementalReactionsMod.MODID, "textures/gui/elements.png");
    private static final int ICON_SIZE = 16;
    private static final int ICON_GRID_SIZE = 16;
    private static final int ICON_ROWS = 1;
    private static final int ICON_COLS = 7;
    private static final int ICON_TEXTURE_SIZE = 112; // 16x7=112
    
    // 图标UV坐标 (u, v)
    private static final int[][] ICON_UV = {
        {0, 0},    // 水 (WATER)
        {16, 0},   // 火 (FIRE)
        {32, 0},   // 雷 (ELECTRO)
        {48, 0},   // 冰 (ICE)
        {64, 0},   // 风 (WIND)
        {80, 0},   // 草 (GRASS)
        {96, 0}    // 岩 (ROCK)
    };
    
    private static final int ICON_ANIMATION_DURATION = 10;
    private int animationTick = 0;
    private int currentAnimationFrame = 0;
    
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        
        ElementType element = ElementalAttachHandler.getAttachedElement(player.getUUID());
        if (element == ElementType.NONE) return;
        
        FontRenderer font = Minecraft.getInstance().font;
        MatrixStack matrixStack = event.getMatrixStack();
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        
        // 更新动画计时器
        animationTick++;
        if (animationTick >= ICON_ANIMATION_DURATION) {
            animationTick = 0;
            currentAnimationFrame = (currentAnimationFrame + 1) % 5; // 5帧动画
        }
        
        // 右上角显示元素名称和图标
        int elementId = element.ordinal();
        if (elementId >= 0 && elementId < ICON_UV.length) {
            int x = screenWidth - 70; // 图标和名称的起始X坐标
            int y = 10; // 顶部位置
            
            // 绘制元素图标背景
            drawElementBackground(matrixStack, x - 3, y - 3, 54, 22);
            
            // 绘制元素图标
            textureManager.bind(ELEMENTS_TEXTURE);
            int u = ICON_UV[elementId][0] + currentAnimationFrame * ICON_GRID_SIZE;
            int v = ICON_UV[elementId][1];
            
            Screen.blit(matrixStack, x, y, u, v, ICON_SIZE, ICON_SIZE, ICON_TEXTURE_SIZE, ICON_GRID_SIZE);
            
            // 绘制元素名称
            int nameX = x + ICON_SIZE + 5;
            int nameY = y + (ICON_SIZE - 8) / 2; // 垂直居中
            font.draw(matrixStack, element.name(), nameX, nameY, element.getColor());
        }
        
        // 背包界面显示状态
        if (Minecraft.getInstance().screen instanceof InventoryScreen) {
            Screen screen = Minecraft.getInstance().screen;
            int centerX = screen.width / 2;
            int centerY = screen.height / 2;
            
            // 状态栏背景
            drawStatusBarBackground(matrixStack, centerX + 80, centerY - 90, 80, 40);
            
            // 元素标题
            font.draw(matrixStack, "元素附着", centerX + 90, centerY - 85, 0xFFFFFF);
            
            // 分割线
            drawHorizontalLine(matrixStack, centerX + 80, centerX + 160, centerY - 75, 0x80FFFFFF);
            
            // 元素名称和图标
            int elementX = centerX + 90;
            int elementY = centerY - 70;
            
            textureManager.bind(ELEMENTS_TEXTURE);
            int u = ICON_UV[elementId][0] + currentAnimationFrame * ICON_GRID_SIZE;
            int v = ICON_UV[elementId][1];
            Screen.blit(matrixStack, elementX, elementY, u, v, ICON_SIZE, ICON_SIZE, ICON_TEXTURE_SIZE, ICON_GRID_SIZE);
            
            font.draw(matrixStack, element.name(), elementX + ICON_SIZE + 5, elementY + 4, element.getColor());
            
            // 剩余时间
            int timeLeft = ElementalAttachHandler.getRemainingTime(player.getUUID());
            String timeText = "剩余时间: " + (timeLeft / 20) + "秒";
            int timeY = centerY - 50;
            font.draw(matrixStack, timeText, centerX + 90, timeY, 0xD0D0D0);
        }
    }
    
    // 绘制元素背景（半透明圆角矩形）
    private void drawElementBackground(MatrixStack matrixStack, int x, int y, int width, int height) {
        int alpha = 180; // 透明度
        
        // 填充主矩形
        Screen.fill(matrixStack, x + 2, y, x + width - 2, y + height, 0xFFFFFF | (alpha << 24));
        
        // 四角圆角
        Screen.fill(matrixStack, x, y + 2, x + 2, y + height - 2, 0xFFFFFF | (alpha << 24));
        Screen.fill(matrixStack, x + width - 2, y + 2, x + width, y + height - 2, 0xFFFFFF | (alpha << 24));
        
        // 上边圆角
        Screen.fill(matrixStack, x + 2, y, x + width - 2, y + 2, 0xFFFFFF | (alpha << 24));
        
        // 下边圆角
        Screen.fill(matrixStack, x + 2, y + height - 2, x + width - 2, y + height, 0xFFFFFF | (alpha << 24));
        
        // 边框
        Screen.fill(matrixStack, x, y + 1, x + 1, y + height - 1, 0x888888 | (alpha << 24));
        Screen.fill(matrixStack, x + width - 1, y + 1, x + width, y + height - 1, 0x888888 | (alpha << 24));
        Screen.fill(matrixStack, x + 1, y, x + width - 1, y + 1, 0x888888 | (alpha << 24));
        Screen.fill(matrixStack, x + 1, y + height - 1, x + width - 1, y + height, 0x888888 | (alpha << 24));
    }
    
    // 绘制状态栏背景（深色半透明）
    private void drawStatusBarBackground(MatrixStack matrixStack, int x, int y, int width, int height) {
        // 半透明深灰色背景
        Screen.fill(matrixStack, x, y, x + width, y + height, 0x7F000000);
        
        // 白色边框
        Screen.fill(matrixStack, x, y, x + width, y + 1, 0xFFFFFFFF);
        Screen.fill(matrixStack, x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        Screen.fill(matrixStack, x, y, x + 1, y + height, 0xFFFFFFFF);
        Screen.fill(matrixStack, x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }
    
    // 绘制水平线
    private void drawHorizontalLine(MatrixStack matrixStack, int x1, int x2, int y, int color) {
        Screen.fill(matrixStack, x1, y, x2, y + 1, color);
    }
    
    // 绘制垂直线
    private void drawVerticalLine(MatrixStack matrixStack, int x, int y1, int y2, int color) {
        Screen.fill(matrixStack, x, y1, x + 1, y2, color);
    }
}


