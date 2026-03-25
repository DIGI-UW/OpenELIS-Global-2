package org.openelisglobal.analyzer.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for NetworkValidationUtil SSRF blocklist.
 *
 * Verifies that loopback, link-local, multicast, and any-local addresses are
 * blocked, while private LAN ranges (used by lab analyzers) are allowed.
 */
public class NetworkValidationUtilTest {

    // ── Blocked addresses ───────────────────────────────────────────────

    @Test
    public void testBlocksNull() {
        assertTrue("null should be blocked", NetworkValidationUtil.isBlockedAddress(null));
    }

    @Test
    public void testBlocksEmpty() {
        assertTrue("empty string should be blocked", NetworkValidationUtil.isBlockedAddress(""));
    }

    @Test
    public void testBlocksBlank() {
        assertTrue("blank string should be blocked", NetworkValidationUtil.isBlockedAddress("   "));
    }

    @Test
    public void testBlocksLoopback_127_0_0_1() {
        assertTrue("127.0.0.1 (loopback) should be blocked", NetworkValidationUtil.isBlockedAddress("127.0.0.1"));
    }

    @Test
    public void testBlocksLoopback_127_0_0_2() {
        assertTrue("127.0.0.2 (loopback range) should be blocked", NetworkValidationUtil.isBlockedAddress("127.0.0.2"));
    }

    @Test
    public void testBlocksLoopback_127_255_255_255() {
        assertTrue("127.255.255.255 (loopback range end) should be blocked",
                NetworkValidationUtil.isBlockedAddress("127.255.255.255"));
    }

    @Test
    public void testBlocksLinkLocal_169_254_169_254() {
        assertTrue("169.254.169.254 (cloud metadata / link-local) should be blocked",
                NetworkValidationUtil.isBlockedAddress("169.254.169.254"));
    }

    @Test
    public void testBlocksLinkLocal_169_254_0_1() {
        assertTrue("169.254.0.1 (link-local) should be blocked", NetworkValidationUtil.isBlockedAddress("169.254.0.1"));
    }

    @Test
    public void testBlocksMulticast_224_0_0_1() {
        assertTrue("224.0.0.1 (multicast) should be blocked", NetworkValidationUtil.isBlockedAddress("224.0.0.1"));
    }

    @Test
    public void testBlocksMulticast_239_255_255_255() {
        assertTrue("239.255.255.255 (multicast range end) should be blocked",
                NetworkValidationUtil.isBlockedAddress("239.255.255.255"));
    }

    @Test
    public void testBlocksAnyLocal_0_0_0_0() {
        assertTrue("0.0.0.0 (any-local) should be blocked", NetworkValidationUtil.isBlockedAddress("0.0.0.0"));
    }

    @Test
    public void testBlocksUnresolvableHostname() {
        assertTrue("unresolvable hostname should be blocked (fail closed)",
                NetworkValidationUtil.isBlockedAddress("not-a-valid-host-xyz.invalid"));
    }

    @Test
    public void testBlocksIPv6Loopback() {
        assertTrue("::1 (IPv6 loopback) should be blocked", NetworkValidationUtil.isBlockedAddress("::1"));
    }

    @Test
    public void testBlocksIPv6MappedLoopback() {
        assertTrue("::ffff:127.0.0.1 (IPv6-mapped loopback) should be blocked",
                NetworkValidationUtil.isBlockedAddress("::ffff:127.0.0.1"));
    }

    @Test
    public void testBlocksIPv6MappedLinkLocal() {
        assertTrue("::ffff:169.254.169.254 (IPv6-mapped cloud metadata) should be blocked",
                NetworkValidationUtil.isBlockedAddress("::ffff:169.254.169.254"));
    }

    // ── Allowed addresses (private LAN — lab analyzers live here) ───────

    @Test
    public void testAllows_192_168_1_100() {
        assertFalse("192.168.1.100 (private LAN) should be allowed",
                NetworkValidationUtil.isBlockedAddress("192.168.1.100"));
    }

    @Test
    public void testAllows_10_0_1_50() {
        assertFalse("10.0.1.50 (private LAN) should be allowed", NetworkValidationUtil.isBlockedAddress("10.0.1.50"));
    }

    @Test
    public void testAllows_172_16_0_1() {
        assertFalse("172.16.0.1 (private LAN) should be allowed", NetworkValidationUtil.isBlockedAddress("172.16.0.1"));
    }

    @Test
    public void testAllows_172_31_255_254() {
        assertFalse("172.31.255.254 (private LAN upper bound) should be allowed",
                NetworkValidationUtil.isBlockedAddress("172.31.255.254"));
    }

    @Test
    public void testAllowsPublicIP() {
        assertFalse("8.8.8.8 (public IP) should be allowed", NetworkValidationUtil.isBlockedAddress("8.8.8.8"));
    }

    @Test
    public void testAllowsPublicIP_203() {
        assertFalse("203.0.113.1 (public IP) should be allowed", NetworkValidationUtil.isBlockedAddress("203.0.113.1"));
    }
}
