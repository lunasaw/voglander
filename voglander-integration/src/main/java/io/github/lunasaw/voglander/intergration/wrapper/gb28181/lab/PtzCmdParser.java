package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

/**
 * PTZ 指令 hex 串解析器（Lab 可视化专用）。
 * <p>
 * GB28181 PTZCmd 为 8 字节串（16 字符 hex）：
 * byte[0]=0xA5, byte[1]=组合码1, byte[2]=地址低字节, byte[3]=指令码（方向/变倍）,
 * byte[4]=data1（水平速度）, byte[5]=data2（垂直速度）, byte[6]=组合码2, byte[7]=校验和。
 * </p>
 */
public final class PtzCmdParser {

    private PtzCmdParser() {}

    /**
     * 解析 PTZCmd hex 串，返回可视化字段 {direction, speed, hex}。
     * 解析失败时只返回 hex，不抛异常。
     */
    public static Map<String, Object> parse(String hex) {
        Map<String, Object> result = new HashMap<>();
        result.put("hex", hex != null ? hex : "");
        if (hex == null || hex.length() < 16) {
            return result;
        }
        try {
            byte[] b = hexToBytes(hex);
            if (b.length < 8) return result;
            byte instructionCode = b[3];
            int panBits  = instructionCode & 0x03;
            int tiltBits = instructionCode & 0x0C;
            int zoomBits = instructionCode & 0x30;
            String direction = resolveDirection(panBits, tiltBits, zoomBits);
            int speed = Math.max(b[4] & 0xFF, b[5] & 0xFF); // 取水平/垂直中较大值
            result.put("direction", direction);
            result.put("speed", speed);
        } catch (Exception ignored) {
            // hex 格式异常：仅保留原始 hex
        }
        return result;
    }

    private static String resolveDirection(int pan, int tilt, int zoom) {
        if (zoom == 0x10) return "ZOOM_IN";
        if (zoom == 0x20) return "ZOOM_OUT";
        if (tilt == 0x08 && pan == 0x00) return "UP";
        if (tilt == 0x04 && pan == 0x00) return "DOWN";
        if (tilt == 0x00 && pan == 0x02) return "LEFT";
        if (tilt == 0x00 && pan == 0x01) return "RIGHT";
        if (tilt == 0x08 && pan == 0x02) return "UP_LEFT";
        if (tilt == 0x08 && pan == 0x01) return "UP_RIGHT";
        if (tilt == 0x04 && pan == 0x02) return "DOWN_LEFT";
        if (tilt == 0x04 && pan == 0x01) return "DOWN_RIGHT";
        return "STOP";
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length() / 2;
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
            b[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return b;
    }
}
