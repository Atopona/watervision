package me.srrapero720.watervision.compat;

import me.srrapero720.watervision.VisionConfig;
import me.srrapero720.watervision.WaterVision;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Sodium/Embeddium 兼容层
 * 检测是否安装了 Sodium 系列模组
 */
public class SodiumCompat {
    
    private static final boolean SODIUM_LOADED;
    private static final boolean EMBEDDIUM_LOADED;
    
    static {
        SODIUM_LOADED = isModLoaded("sodium");
        EMBEDDIUM_LOADED = isModLoaded("embeddium");
    }
    
    /**
     * 检查 Sodium 或 Embeddium 是否可用
     */
    public static boolean isAvailable() {
        return SODIUM_LOADED || EMBEDDIUM_LOADED;
    }
    
    /**
     * 检查是否是 Embeddium
     */
    public static boolean isEmbeddium() {
        return EMBEDDIUM_LOADED;
    }
    
    /**
     * 检查是否是 Sodium
     */
    public static boolean isSodium() {
        return SODIUM_LOADED;
    }
    
    private static boolean isModLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }
}
