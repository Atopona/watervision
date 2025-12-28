package me.srrapero720.watervision;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * WaterVision 配置类
 * 提供 HDR 渲染和其他视频设置的配置选项
 */
public class VisionConfig {
    
    public static final ModConfigSpec CLIENT_SPEC;
    static final ClientConfig CLIENT;
    
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CLIENT = new ClientConfig(builder);
        CLIENT_SPEC = builder.build();
    }
    
    static class ClientConfig {
        // HDR 设置
        final ModConfigSpec.BooleanValue hdrToSdrEnabled;
        
        ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("WaterVision Client Configuration")
                   .push("hdr");
            
            hdrToSdrEnabled = builder
                    .comment("Enable HDR to SDR tone mapping for HDR video content.",
                             "When enabled, HDR videos will be converted to SDR for proper display.",
                             "Disable if you experience visual issues or prefer raw HDR output.",
                             "Note: HDR tone mapping is automatically disabled when shader mods (Iris/Oculus) are active to avoid conflicts.")
                    .define("enableHdrToSdr", true);
            
            builder.pop();
        }
    }
    
    /**
     * 注册配置
     */
    public static void init(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
    
    // 便捷访问方法 - 带有安全检查
    
    /**
     * 检查 HDR to SDR 是否启用
     */
    public static boolean isHdrToSdrEnabled() {
        try {
            return !CLIENT_SPEC.isLoaded() || CLIENT.hdrToSdrEnabled.get();
        } catch (Exception e) {
            return true; // 默认启用
        }
    }
    
    /**
     * 设置 HDR to SDR 是否启用
     */
    public static void setHdrToSdrEnabled(boolean value) {
        try {
            if (CLIENT_SPEC.isLoaded()) {
                CLIENT.hdrToSdrEnabled.set(value);
            }
        } catch (Exception ignored) {}
    }
}
