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
    public void testAllowsPublicIP_second() {
        // Was 203.0.113.1, which is NOT public: RFC 5737 reserves it for
        // documentation, and it is now blocked with the other special-purpose
        // ranges. 1.1.1.1 is genuinely routable, which is what this asserts.
        assertFalse("1.1.1.1 (public IP) should be allowed", NetworkValidationUtil.isBlockedAddress("1.1.1.1"));
    }

    /**
     * The special-purpose ranges are blocked. These are never a real analyzer, and
     * a resolver that hijacks NXDOMAIN commonly answers with one of them - which is
     * how an unresolvable hostname reached the "allowed" branch instead of failing
     * closed.
     */
    @Test
    public void testBlocksReservedRanges() {
        assertTrue("100.64.0.1 (CGNAT, RFC 6598) should be blocked",
                NetworkValidationUtil.isBlockedAddress("100.64.0.1"));
        assertTrue("192.0.0.1 (IETF protocol assignments) should be blocked",
                NetworkValidationUtil.isBlockedAddress("192.0.0.1"));
        assertTrue("192.0.2.1 (documentation, RFC 5737) should be blocked",
                NetworkValidationUtil.isBlockedAddress("192.0.2.1"));
        assertTrue("198.18.0.11 (benchmarking, RFC 2544 - a common NXDOMAIN-hijack answer) should be blocked",
                NetworkValidationUtil.isBlockedAddress("198.18.0.11"));
        assertTrue("198.51.100.1 (documentation, RFC 5737) should be blocked",
                NetworkValidationUtil.isBlockedAddress("198.51.100.1"));
        assertTrue("203.0.113.1 (documentation, RFC 5737) should be blocked",
                NetworkValidationUtil.isBlockedAddress("203.0.113.1"));
        assertTrue("240.0.0.1 (reserved for future use) should be blocked",
                NetworkValidationUtil.isBlockedAddress("240.0.0.1"));
        assertTrue("255.255.255.255 (broadcast) should be blocked",
                NetworkValidationUtil.isBlockedAddress("255.255.255.255"));
    }

    /** The ranges beside the reserved blocks stay allowed. */
    @Test
    public void testAllowsNeighboursOfTheReservedRanges() {
        assertFalse("100.63.255.255 is below 100.64.0.0/10 and should be allowed",
                NetworkValidationUtil.isBlockedAddress("100.63.255.255"));
        assertFalse("100.128.0.1 is above 100.64.0.0/10 and should be allowed",
                NetworkValidationUtil.isBlockedAddress("100.128.0.1"));
        assertFalse("198.17.255.255 is below 198.18.0.0/15 and should be allowed",
                NetworkValidationUtil.isBlockedAddress("198.17.255.255"));
        assertFalse("198.20.0.1 is above 198.18.0.0/15 and should be allowed",
                NetworkValidationUtil.isBlockedAddress("198.20.0.1"));
        assertFalse("239.255.255.255 is below 240.0.0.0/4 (and multicast ends here) ... use 223.x instead",
                NetworkValidationUtil.isBlockedAddress("223.255.255.255"));
    }
}
