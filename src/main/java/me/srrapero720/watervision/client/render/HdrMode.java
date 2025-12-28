package me.srrapero720.watervision.client.render;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * HDR 模式常量和检测工具
 */
public final class HdrMode {
    /** SDR 模式 - 不做任何转换 */
    public static final int SDR = 0;
    /** PQ 模式 - HDR10 标准 (SMPTE ST 2084) */
    public static final int PQ = 1;
    /** HLG 模式 - 混合对数伽马 */
    public static final int HLG = 2;
    
    /** HDR 文件名检测模式 */
    private static final Pattern HDR10_PATTERN = Pattern.compile(
        "(hdr10|hdr\\.10|2160p.*hdr|4k.*hdr|uhd.*hdr|dv|dolby.?vision|pq)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HLG_PATTERN = Pattern.compile(
        "(hlg|hybrid.?log)",
        Pattern.CASE_INSENSITIVE
    );
    
    private HdrMode() {}
    
    /**
     * 根据 URI 检测 HDR 模式
     */
    public static int detect(URI uri) {
        if (uri == null) return SDR;
        return detect(uri.toString());
    }
    
    /**
     * 根据路径/URL 字符串检测 HDR 模式
     */
    public static int detect(String path) {
        if (path == null || path.isEmpty()) return SDR;
        
        if (HLG_PATTERN.matcher(path).find()) {
            return HLG;
        } else if (HDR10_PATTERN.matcher(path).find()) {
            return PQ;
        }
        return SDR;
    }
    
    /**
     * 获取模式名称
     */
    public static String getName(int mode) {
        return switch (mode) {
            case PQ -> "HDR10 (PQ)";
            case HLG -> "HLG";
            default -> "SDR";
        };
    }
}
