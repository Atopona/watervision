package me.srrapero720.watervision.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

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
     * 渲染带 HDR 色调映射的纹理
     * 兼容 Iris/Shaders - 通过直接绑定到主 framebuffer 并保存/恢复 GL 状态
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
        // 先 flush 当前的 GuiGraphics 批处理，确保之前的渲染完成
        graphics.flush();
        
        final float pX1 = x;
        final float pX2 = x + width;
        final float pY1 = y;
        final float pY2 = y + height;
        final float pBlitOffset = 0.0f;
        final var pMinU = (float) offsetX / width;
        final var pMaxU = (float) (offsetX + width) / width;
        final var pMinV = (float) offsetY / height;
        final var pMaxV = (float) (offsetY + height) / height;

        // 检测是否在光影环境下
        final boolean shadersActive = ShaderCompat.areShadersActive();
        
        // 保存当前 GL 状态 (Iris/Shaders 兼容)
        final int previousProgram = GL11.glGetInteger(GL30.GL_CURRENT_PROGRAM);
        final int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        final boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        final int previousBlendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC_ALPHA);
        final int previousBlendDst = GL11.glGetInteger(GL11.GL_BLEND_DST_ALPHA);
        final int previousActiveTexture = GL11.glGetInteger(GL30.GL_ACTIVE_TEXTURE);
        final boolean wasDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        
        try {
            // 绑定到 Minecraft 主 framebuffer，绕过 Iris 的 framebuffer
            final RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
            if (mainTarget != null) {
                mainTarget.bindWrite(false);
            }
            
            // 禁用深度测试，确保 GUI 元素总是在最前面
            RenderSystem.disableDepthTest();
            
            // 设置混合模式
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            
            // 绑定纹理
            final int tex = Minecraft.getInstance().getTextureManager().getTexture(texture).getId();
            RenderSystem.activeTexture(GL30.GL_TEXTURE0);
            RenderSystem.bindTexture(tex);
            RenderSystem.setShaderTexture(0, tex);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            
            // 选择 shader - 光影环境下使用标准 shader
            ShaderInstance shaderToUse;
            if (!shadersActive && hdrMode != HdrMode.SDR && HdrShader.isAvailable()) {
                shaderToUse = HdrShader.getShader();
                HdrShader.setMode(hdrMode);
            } else {
                shaderToUse = GameRenderer.getPositionTexShader();
            }
            
            if (shaderToUse == null) {
                return;
            }
            
            RenderSystem.setShader(() -> shaderToUse);
            
            // 应用 shader 并设置 uniforms
            shaderToUse.apply();
            
            // 构建顶点数据
            final Matrix4f matrix4f = graphics.pose().last().pose();
            final BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.addVertex(matrix4f, pX1, pY1, pBlitOffset).setUv(pMinU, pMinV);
            bufferbuilder.addVertex(matrix4f, pX1, pY2, pBlitOffset).setUv(pMinU, pMaxV);
            bufferbuilder.addVertex(matrix4f, pX2, pY2, pBlitOffset).setUv(pMaxU, pMaxV);
            bufferbuilder.addVertex(matrix4f, pX2, pY1, pBlitOffset).setUv(pMaxU, pMinV);
            
            // 绘制
            BufferUploader.drawWithShader(Objects.requireNonNull(bufferbuilder.build()));
            
            // 清理 shader
            shaderToUse.clear();
            
        } finally {
            // 恢复 GL 状态 (Iris/Shaders 兼容)
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL30.glUseProgram(previousProgram);
            RenderSystem.activeTexture(previousActiveTexture);
            
            if (wasDepthTestEnabled) {
                RenderSystem.enableDepthTest();
            }
            
            if (wasBlendEnabled) {
                RenderSystem.enableBlend();
                GlStateManager._blendFuncSeparate(previousBlendSrc, previousBlendDst, previousBlendSrc, previousBlendDst);
            } else {
                RenderSystem.disableBlend();
            }
            
            // 重置 shader 颜色
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
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
