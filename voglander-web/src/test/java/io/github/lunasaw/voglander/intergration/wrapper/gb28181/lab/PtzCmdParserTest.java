package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PtzCmdParserTest {

    /** A50F0101800080xx — TILT_UP(0x08), h-speed=0x80=128, v-speed=0x80=128 */
    @Test
    public void testTiltUpParsed() {
        // byte[3]=0x08(TILT_UP), data1=0x80, data2=0x80
        String hex = buildHex((byte)0x08, (byte)0x80, (byte)0x80);
        Map<String, Object> r = PtzCmdParser.parse(hex);
        assertEquals("UP", r.get("direction"));
        assertEquals(128, r.get("speed"));
    }

    @Test
    public void testPanLeftParsed() {
        String hex = buildHex((byte)0x02, (byte)0x40, (byte)0x00); // PAN_LEFT
        Map<String, Object> r = PtzCmdParser.parse(hex);
        assertEquals("LEFT", r.get("direction"));
        assertEquals(64, r.get("speed"));
    }

    @Test
    public void testStopParsed() {
        String hex = buildHex((byte)0x00, (byte)0x00, (byte)0x00);
        Map<String, Object> r = PtzCmdParser.parse(hex);
        assertEquals("STOP", r.get("direction"));
    }

    @Test
    public void testInvalidHexReturnsHexOnly() {
        Map<String, Object> r = PtzCmdParser.parse("ZZZZZZZZZZZZZZZZ");
        assertNotNull(r.get("hex"));
        assertNull(r.get("direction"));
    }

    @Test
    public void testNullHexReturnsEmpty() {
        Map<String, Object> r = PtzCmdParser.parse(null);
        assertEquals("", r.get("hex"));
    }

    /** 构造最简合法 8 字节 PTZ hex（不计算真实校验和，仅验证解析逻辑）。*/
    private String buildHex(byte instructionCode, byte data1, byte data2) {
        byte[] b = new byte[8];
        b[0] = (byte)0xA5;
        b[1] = 0x0F;      // combinationCode1（随意）
        b[2] = 0x01;      // addressLow
        b[3] = instructionCode;
        b[4] = data1;
        b[5] = data2;
        b[6] = 0x00;
        b[7] = 0x00;
        StringBuilder sb = new StringBuilder(16);
        for (byte x : b) sb.append(String.format("%02X", x));
        return sb.toString();
    }
}
