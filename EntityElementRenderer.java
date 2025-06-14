package com.AsabaHarumasa.elementalreactions;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class EntityElementRenderer {
    private static final float ICON_SIZE = 0.8F; // 图标尺寸（方块单位）
    private static final float ICON_PADDING = 0.2F; // 图标与实体头顶的间距
    private static final float MAX_RENDER_DISTANCE = 64.0F; // 最大渲染距离（格）

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        UUID uuid = entity.getUUID();
        ElementType element = ElementalAttachHandler.getAttachedElement(uuid);
        if (element == ElementType.NONE) return;

        Vector3d entityPos = entity.getPosition(event.getPartialRenderTick());
        Vector3d viewerPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float distance = (float) entityPos.distanceTo(viewerPos);
        if (distance > MAX_RENDER_DISTANCE) return;

        float entityHeight = entity.getBbHeight();
        Vector3d iconPos = entityPos.add(0, entityHeight + ICON_PADDING, 0);
        
        MatrixStack matrixStack = event.getMatrixStack();
        IRenderTypeBuffer buffer = event.getBuffers();
        int light = event.getLight();
        
        renderElementIcon(matrixStack, buffer, light, iconPos, element, distance);
    }

    private void renderElementIcon(MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                  int light, Vector3d position, 
                                  ElementType element, float distance) {
        // 设置图标大小和位置
        float scale = MathHelper.clamp(ICON_SIZE * (1.0F - distance / MAX_RENDER_DISTANCE) * 2.0F, 0.1F, ICON_SIZE);
        matrixStack.pushPose();
        matrixStack.translate(position.x, position.y, position.z);
        matrixStack.scale(scale, scale, scale);
        
        // 始终面向玩家
        Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3d cameraDirection = position.subtract(cameraPos).normalize();
        Vector3f viewVector = new Vector3f((float) cameraDirection.x, 0, (float) cameraDirection.z);
        viewVector.normalize();
        matrixStack.mulPose(new Quaternion(viewVector, 0, true));
        
        // 轻微浮动效果
        float floatingOffset = (float) Math.sin(System.currentTimeMillis() / 500.0) * 0.1F;
        matrixStack.translate(0, floatingOffset, 0);
        
        // 创建旋转矩阵
        Matrix4f matrix = matrixStack.last().pose();
        
        // 获取纹理资源
        ResourceLocation iconTexture = element.getIconResource();
        TextureManager textureManager = Minecraft.getInstance().textureManager;
        textureManager.bind(iconTexture);
        
        // 渲染图标
        float alpha = MathHelper.clamp(1.0F - (distance - 32.0F) / 32.0F, 0.2F, 1.0F);
        renderBillboard(matrix, alpha, light);
        
        matrixStack.popPose();
    }

    private void renderBillboard(Matrix4f matrix, float alpha, int light) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP);
        
        // 半透明白色，带透明度变化
        int color = 0xFFFFFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int)(alpha * 255);
        
        int packedLight = (light >> 16) & 0xFFFF;
        
        // 创建四边形顶点 (Z轴为0，面向玩家)
        bufferBuilder.vertex(matrix, -0.5F, 0.5F, 0)
                .color(r, g, b, a)
                .uv(0, 0)
                .uv2(packedLight)
                .endVertex();
        
        bufferBuilder.vertex(matrix, 0.5F, 0.5F, 0)
                .color(r, g, b, a)
                .uv(1, 0)
                .uv2(packedLight)
                .endVertex();
        
        bufferBuilder.vertex(matrix, 0.5F, -0.5F, 0)
                .color(r, g, b, a)
                .uv(1, 1)
                .uv2(packedLight)
                .endVertex();
        
        bufferBuilder.vertex(matrix, -0.5F, -0.5F, 0)
                .color(r, g, b, a)
                .uv(0, 1)
                .uv2(packedLight)
                .endVertex();
        
        tessellator.end();
    }
}


