package me.srrapero720.watervision.client.render;

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
import org.watermedia.api.player.videolan.VideoPlayer;

import java.util.Objects;

/**
 * HDR 视频渲染器
 * 支持 HDR 到 SDR 的色调映射
 */
@OnlyIn(Dist.CLIENT)
public class HdrRenderer {
    
    /**
     * 渲染视频帧，自动处理 HDR 色调映射
     * 
     * @param graphics GuiGraphics 实例
     * @param texture 纹理资源位置
     * @param player VideoPlayer 实例（用于获取 HDR 模式）
     * @param alpha 透明度
     * @param x X 坐标
     * @param y Y 坐标
     * @param width 宽度
     * @param height 高度
     */
    public static void blitVideo(GuiGraphics graphics, ResourceLocation texture, VideoPlayer player, 
                                  float alpha, int x, int y, int width, int height) {
        int hdrMode = player != null ? player.getHdrMode() : VideoPlayer.HDR_MODE_SDR;
        blitWithHdr(graphics, texture, alpha, x, y, 0, 0, width, height, hdrMode);
    }
    
    /**
     * 渲染带 HDR 色调映射的纹理
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
        final float pX1 = x;
        final float pX2 = x + width;
        final float pY1 = y;
        final float pY2 = y + height;
        final float pBlitOffset = 0.0f;
        final var pMinU = (float) offsetX / width;
        final var pMaxU = (float) (offsetX + width) / width;
        final var pMinV = (float) offsetY / height;
        final var pMaxV = (float) (offsetY + height) / height;

        RenderSystem.enableBlend();
        final int tex = Minecraft.getInstance().getTextureManager().getTexture(texture).getId();
        RenderSystem.bindTexture(tex);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        
        // 选择 shader
        ShaderInstance shader = null;
        if (hdrMode != VideoPlayer.HDR_MODE_SDR && HdrShader.isAvailable()) {
            shader = HdrShader.getShader();
            HdrShader.setMode(hdrMode);
        }
        
        if (shader != null) {
            RenderSystem.setShader(() -> shader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }
        
        final Matrix4f matrix4f = graphics.pose().last().pose();
        final BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, pX1, pY1, pBlitOffset).setUv(pMinU, pMinV);
        bufferbuilder.addVertex(matrix4f, pX1, pY2, pBlitOffset).setUv(pMinU, pMaxV);
        bufferbuilder.addVertex(matrix4f, pX2, pY2, pBlitOffset).setUv(pMaxU, pMaxV);
        bufferbuilder.addVertex(matrix4f, pX2, pY1, pBlitOffset).setUv(pMaxU, pMinV);
        BufferUploader.drawWithShader(Objects.requireNonNull(bufferbuilder.build()));
        RenderSystem.disableBlend();
    }
    
    /**
     * 标准 SDR 渲染（无色调映射）
     */
    public static void blitSdr(GuiGraphics graphics, ResourceLocation texture, float alpha,
                                int x, int y, int offsetX, int offsetY, int width, int height) {
        blitWithHdr(graphics, texture, alpha, x, y, offsetX, offsetY, width, height, VideoPlayer.HDR_MODE_SDR);
    }
}
