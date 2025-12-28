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
    public static final ClientConfig CLIENT;
    
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CLIENT = new ClientConfig(builder);
        CLIENT_SPEC = builder.build();
    }
    
    public static class ClientConfig {
        // HDR 设置
        public final ModConfigSpec.BooleanValue hdrToSdrEnabled;
        public final ModConfigSpec.EnumValue<HdrTonemapMode> hdrTonemapMode;
        
        // 渲染设置
        public final ModConfigSpec.BooleanValue forceHdrInShaders;
        
        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("WaterVision Client Configuration")
                   .push("hdr");
            
            hdrToSdrEnabled = builder
                    .comment("Enable HDR to SDR tone mapping for HDR video content.",
                             "When enabled, HDR videos will be converted to SDR for proper display.",
                             "Disable if you experience visual issues or prefer raw HDR output.")
                    .define("enableHdrToSdr", true);
            
            hdrTonemapMode = builder
                    .comment("HDR tone mapping algorithm to use.",
                             "ACES: Film-like tone mapping with good color preservation (recommended)",
                             "REINHARD: Classic Reinhard tone mapping",
                             "AUTO: Automatically select based on content")
                    .defineEnum("tonemapMode", HdrTonemapMode.ACES);
            
            forceHdrInShaders = builder
                    .comment("Force HDR tone mapping even when shader mods (Iris/Oculus) are active.",
                             "Enable this if HDR videos look washed out with shaders.",
                             "Disable if you experience rendering issues with shaders.")
                    .define("forceHdrInShaders", true);
            
            builder.pop();
        }
    }
    
    /**
     * HDR 色调映射模式
     */
    public enum HdrTonemapMode {
        /** ACES 电影色调映射 */
        ACES,
        /** Reinhard 色调映射 */
        REINHARD,
        /** 自动选择 */
        AUTO
    }
    
    /**
     * 注册配置
     */
    public static void init(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
    
    // 便捷访问方法
    
    /**
     * 检查 HDR to SDR 是否启用
     */
    public static boolean isHdrToSdrEnabled() {
        return CLIENT.hdrToSdrEnabled.get();
    }
    
    /**
     * 获取色调映射模式
     */
    public static HdrTonemapMode getTonemapMode() {
        return CLIENT.hdrTonemapMode.get();
    }
    
    /**
     * 检查是否在光影环境下强制启用 HDR
     */
    public static boolean forceHdrInShaders() {
        return CLIENT.forceHdrInShaders.get();
    }
}
