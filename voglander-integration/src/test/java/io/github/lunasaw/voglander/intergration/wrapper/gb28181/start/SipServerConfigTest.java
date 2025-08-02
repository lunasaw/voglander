package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SIP服务器配置测试类
 * 
 * @author luna
 * @since 2025/8/2
 */
class SipServerConfigTest {

    private SipServerConfig sipServerConfig;

    @BeforeEach
    void setUp() {
        sipServerConfig = new SipServerConfig();
    }

    @Test
    void setAndGetIp_Success() {
        // Given
        String testIp = "192.168.1.100";

        // When
        sipServerConfig.setIp(testIp);

        // Then
        assertEquals(testIp, sipServerConfig.getIp());
    }

    @Test
    void setAndGetPort_Success() {
        // Given
        Integer testPort = 5060;

        // When
        sipServerConfig.setPort(testPort);

        // Then
        assertEquals(testPort, sipServerConfig.getPort());
    }

    @Test
    void setAndGetEnableLog_Success() {
        // When
        sipServerConfig.setEnableLog(true);

        // Then
        assertTrue(sipServerConfig.getEnableLog());

        // When
        sipServerConfig.setEnableLog(false);

        // Then
        assertFalse(sipServerConfig.getEnableLog());
    }

    @Test
    void setAndGetEnable_Success() {
        // When
        sipServerConfig.setEnable(true);

        // Then
        assertTrue(sipServerConfig.getEnable());

        // When
        sipServerConfig.setEnable(false);

        // Then
        assertFalse(sipServerConfig.getEnable());
    }

    @Test
    void defaultEnableLogValue() {
        // Given
        SipServerConfig config = new SipServerConfig();

        // Then
        assertFalse(config.getEnableLog());
    }

    @Test
    void nullValues_ShouldBeAccepted() {
        // When
        sipServerConfig.setIp(null);
        sipServerConfig.setPort(null);
        sipServerConfig.setEnableLog(null);
        sipServerConfig.setEnable(null);

        // Then
        assertNull(sipServerConfig.getIp());
        assertNull(sipServerConfig.getPort());
        assertNull(sipServerConfig.getEnableLog());
        assertNull(sipServerConfig.getEnable());
    }

    @Test
    void emptyStringIp_ShouldBeAccepted() {
        // When
        sipServerConfig.setIp("");

        // Then
        assertEquals("", sipServerConfig.getIp());
    }

    @Test
    void whitespaceIp_ShouldBeAccepted() {
        // Given
        String whitespaceIp = "   ";

        // When
        sipServerConfig.setIp(whitespaceIp);

        // Then
        assertEquals(whitespaceIp, sipServerConfig.getIp());
    }

    @Test
    void validIpFormats_ShouldBeAccepted() {
        // Test IPv4
        sipServerConfig.setIp("192.168.1.1");
        assertEquals("192.168.1.1", sipServerConfig.getIp());

        // Test localhost
        sipServerConfig.setIp("localhost");
        assertEquals("localhost", sipServerConfig.getIp());

        // Test hostname
        sipServerConfig.setIp("server.example.com");
        assertEquals("server.example.com", sipServerConfig.getIp());
    }

    @Test
    void portBoundaryValues_ShouldBeAccepted() {
        // Test minimum port (usually 1)
        sipServerConfig.setPort(1);
        assertEquals(1, sipServerConfig.getPort());

        // Test common SIP port
        sipServerConfig.setPort(5060);
        assertEquals(5060, sipServerConfig.getPort());

        // Test maximum port (65535)
        sipServerConfig.setPort(65535);
        assertEquals(65535, sipServerConfig.getPort());

        // Test zero port (for auto-assignment)
        sipServerConfig.setPort(0);
        assertEquals(0, sipServerConfig.getPort());
    }

    @Test
    void equalsAndHashCode_ShouldWork() {
        // Given
        SipServerConfig config1 = new SipServerConfig();
        config1.setIp("192.168.1.1");
        config1.setPort(5060);
        config1.setEnableLog(true);
        config1.setEnable(true);

        SipServerConfig config2 = new SipServerConfig();
        config2.setIp("192.168.1.1");
        config2.setPort(5060);
        config2.setEnableLog(true);
        config2.setEnable(true);

        SipServerConfig config3 = new SipServerConfig();
        config3.setIp("192.168.1.2");
        config3.setPort(5060);
        config3.setEnableLog(true);
        config3.setEnable(true);

        // Then
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1, config3);
    }

    @Test
    void toString_ShouldContainAllFields() {
        // Given
        sipServerConfig.setIp("192.168.1.1");
        sipServerConfig.setPort(5060);
        sipServerConfig.setEnableLog(true);
        sipServerConfig.setEnable(true);

        // When
        String toString = sipServerConfig.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("192.168.1.1"));
        assertTrue(toString.contains("5060"));
        assertTrue(toString.contains("true"));
    }
}