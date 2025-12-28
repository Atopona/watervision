package me.srrapero720.watervision.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.srrapero720.watervision.VisionConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

/**
 * HDR 视频渲染器
 * 支持 HDR 到 SDR 的色调映射
 * 兼容 Iris/Shaders
 */
@OnlyIn(Dist.CLIENT)
public class HdrRenderer {
    
    /**
     * 渲染视频帧，自动处理 HDR 色调映射
     * 
     * @param graphics GuiGraphics 实例
     * @param texture 纹理资源位置
     * @param hdrMode HDR 模式
     * @param alpha 透明度
     * @param x X 坐标
     * @param y Y 坐标
     * @param width 宽度
     * @param height 高度
     */
    public static void blitVideo(GuiGraphics graphics, ResourceLocation texture, int hdrMode,
                                  float alpha, int x, int y, int width, int height) {
        blitWithHdr(graphics, texture, alpha, x, y, 0, 0, width, height, hdrMode);
    }
    
    /**
     * 检查是否应该使用 HDR 色调映射
     */
    private static boolean shouldUseHdrTonemap(int hdrMode) {
        // SDR 内容不需要色调映射
        if (hdrMode == HdrMode.SDR) {
            return false;
        }
        
        // 检查 shader 是否已注册
        if (!HdrShader.isRegistered()) {
            return false;
        }
        
        // 当光影启用时，不使用自定义 HDR shader（避免冲突）
        if (ShaderCompat.areShadersActive()) {
            return false;
        }
        
        // 检查配置
        try {
            // 检查配置是否启用 HDR to SDR
            if (!VisionConfig.isHdrToSdrEnabled()) {
                return false;
            }
        } catch (Exception e) {
            // 配置未加载时，默认启用 HDR
        }
        
        return true;
    }
    
    /**
     * 渲染带 HDR 色调映射的纹理
     * 兼容 Iris/Oculus 光影模组
     * 
     * @param graphics GuiGraphics 实例
     * @param texture 纹理资源位置
     * @param alpha 透明度
     * @param x X 坐标
     * @param y Y 坐标
     * @param offsetX 纹理偏移 X
     * @param offsetY 纹理偏移 Y
     * @param width 宽度
     * @param height 高度
     * @param hdrMode HDR 模式 (0=SDR, 1=PQ, 2=HLG)
     */
    public static void blitWithHdr(GuiGraphics graphics, ResourceLocation texture, float alpha,
                                    int x, int y, int offsetX, int offsetY, int width, int height, int hdrMode) {
        // 先刷新之前的渲染
        graphics.flush();
        
        final float pX1 = x;
        final float pX2 = x + width;
        final float pY1 = y;
        final float pY2 = y + height;
        final float pBlitOffset = 0.0f;
        final float pMinU = 0.0f;
        final float pMaxU = 1.0f;
        final float pMinV = 0.0f;
        final float pMaxV = 1.0f;

        // 保存当前 GL 状态
        final boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        final boolean wasDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        
        // 设置渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.setShaderTexture(0, texture);
        
        // 根据配置选择 shader
        boolean useHdrShader = shouldUseHdrTonemap(hdrMode);
        if (useHdrShader) {
            ShaderInstance hdrShader = HdrShader.getShader();
            if (hdrShader != null) {
                HdrShader.setMode(hdrMode);
                RenderSystem.setShader(() -> hdrShader);
            } else {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
            }
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }
        
        final Matrix4f matrix4f = graphics.pose().last().pose();
        final BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, pX1, pY1, pBlitOffset).setUv(pMinU, pMinV);
        bufferbuilder.addVertex(matrix4f, pX1, pY2, pBlitOffset).setUv(pMinU, pMaxV);
        bufferbuilder.addVertex(matrix4f, pX2, pY2, pBlitOffset).setUv(pMaxU, pMaxV);
        bufferbuilder.addVertex(matrix4f, pX2, pY1, pBlitOffset).setUv(pMaxU, pMinV);
        
        MeshData meshData = bufferbuilder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
        
        // 恢复 GL 状态
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (!wasBlendEnabled) {
            RenderSystem.disableBlend();
        }
        if (wasDepthTestEnabled) {
            RenderSystem.enableDepthTest();
        }
    }
    
    /**
     * 标准 SDR 渲染（无色调映射）
     */
    public static void blitSdr(GuiGraphics graphics, ResourceLocation texture, float alpha,
                                int x, int y, int offsetX, int offsetY, int width, int height) {
        blitWithHdr(graphics, texture, alpha, x, y, offsetX, offsetY, width, height, HdrMode.SDR);
    }
}
