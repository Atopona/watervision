package me.srrapero720.watervision.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * HDR 色调映射 Shader 管理器
 * 支持 PQ (HDR10) 和 HLG 两种 HDR 传递函数到 SDR 的转换
 * 兼容 Iris/Oculus 光影模组
 */
@OnlyIn(Dist.CLIENT)
public class HdrShader {
    public static final ResourceLocation SHADER_ID = ResourceLocation.tryBuild("watervision", "hdr_tonemap");
    
    /** SDR 模式 - 不做任何转换 */
    public static final float MODE_SDR = 0.0f;
    /** PQ 模式 - HDR10 标准 (SMPTE ST 2084) */
    public static final float MODE_PQ = 1.0f;
    /** HLG 模式 - 混合对数伽马 */
    public static final float MODE_HLG = 2.0f;
    
    @Nullable
    private static ShaderInstance hdrTonemapShader;
    private static float currentMode = MODE_SDR;
    
    /**
     * 注册 shader，在 RegisterShadersEvent 中调用
     */
    public static void register(ResourceProvider provider) throws IOException {
        hdrTonemapShader = new ShaderInstance(provider, SHADER_ID.toString(), DefaultVertexFormat.POSITION_TEX);
    }
    
    /**
     * 获取 HDR 色调映射 shader
     */
    @Nullable
    public static ShaderInstance getShader() {
        return hdrTonemapShader;
    }
    
    /**
     * 设置 HDR 模式
     * @param mode MODE_SDR, MODE_PQ, 或 MODE_HLG
     */
    public static void setMode(float mode) {
        currentMode = mode;
        if (hdrTonemapShader != null) {
            var uniform = hdrTonemapShader.getUniform("HdrMode");
            if (uniform != null) {
                uniform.set(mode);
            }
        }
    }
    
    /**
     * 获取当前 HDR 模式
     */
    public static float getMode() {
        return currentMode;
    }
    
    /**
     * 检查 shader 是否可用
     * 当 Iris/Oculus 光影启用时，返回 false 以使用兼容的渲染路径
     */
    public static boolean isAvailable() {
        if (hdrTonemapShader == null) {
            return false;
        }
        // 当光影启用时，禁用自定义 HDR shader 以避免冲突
        if (ShaderCompat.areShadersActive()) {
            return false;
        }
        return true;
    }
    
    /**
     * 检查 shader 是否已注册（不考虑光影状态）
     */
    public static boolean isRegistered() {
        return hdrTonemapShader != null;
    }
    
    /**
     * 释放 shader 资源
     */
    public static void close() {
        if (hdrTonemapShader != null) {
            hdrTonemapShader.close();
            hdrTonemapShader = null;
        }
    }
}
