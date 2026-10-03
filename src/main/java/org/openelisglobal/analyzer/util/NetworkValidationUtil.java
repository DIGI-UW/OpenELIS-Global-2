package org.openelisglobal.analyzer.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility for validating network addresses used in analyzer connections.
 *
 * <p>
 * Blocks addresses that should never be legitimate analyzer targets: loopback
 * (127.x), link-local/cloud metadata (169.254.x), multicast, unspecified
 * (0.0.0.0) and the IANA special-purpose ranges below. Private network ranges
 * (10.x, 172.16.x, 192.168.x) are intentionally allowed because laboratory
 * analyzers typically reside on private LANs.
 *
 * <p>
 * The special-purpose ranges matter for more than tidiness. Many resolvers
 * answer for names that do not exist - NXDOMAIN hijacking by an ISP or a
 * captive portal - and hand back an address in one of these reserved blocks
 * (198.18.0.0/15 is a common choice). Without them the fail-closed path is
 * never reached for such a name: resolution "succeeds", the address is in no
 * other blocked category, and the guard allows an outbound connection to
 * whatever the resolver chose.
 */
public final class NetworkValidationUtil {

    private NetworkValidationUtil() {
    }

    /**
     * Returns true if the given IP address should be blocked from outbound
     * connections. Fails closed: unknown or unresolvable addresses are blocked.
     */
    public static boolean isBlockedAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            return isBlockedAddress(addr);
        } catch (UnknownHostException e) {
            return true; // fail closed
        }
    }

    private static boolean isBlockedAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()) {
            return true; // 127.0.0.0/8, ::1
        }
        if (addr.isLinkLocalAddress()) {
            return true; // 169.254.0.0/16, fe80::/10 — includes cloud metadata
        }
        if (addr.isMulticastAddress()) {
            return true; // 224.0.0.0+, ff00::/8
        }
        if (addr.isAnyLocalAddress()) {
            return true; // 0.0.0.0, ::
        }
        if (isReservedIpv4(addr)) {
            return true;
        }

        // Check IPv6-mapped IPv4 (::ffff:x.x.x.x) — could embed a blocked IPv4
        if (addr instanceof Inet6Address) {
            byte[] bytes = addr.getAddress();
            boolean isV4Mapped = isAllZero(bytes, 0, 10) && bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
            if (isV4Mapped) {
                try {
                    byte[] v4 = new byte[4];
                    System.arraycopy(bytes, 12, v4, 0, 4);
                    InetAddress embedded = InetAddress.getByAddress(v4);
                    return isBlockedAddress(embedded);
                } catch (UnknownHostException e) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * IANA special-purpose IPv4 blocks that are never a real analyzer, and that a
     * hijacking resolver may return for a name that does not exist.
     *
     * <ul>
     * <li>100.64.0.0/10 - carrier-grade NAT (RFC 6598)</li>
     * <li>192.0.0.0/24 - IETF protocol assignments (RFC 6890)</li>
     * <li>192.0.2.0/24, 198.51.100.0/24, 203.0.113.0/24 - documentation (RFC
     * 5737)</li>
     * <li>198.18.0.0/15 - benchmarking (RFC 2544), a common NXDOMAIN-hijack
     * answer</li>
     * <li>240.0.0.0/4 - reserved for future use, which includes
     * 255.255.255.255</li>
     * </ul>
     */
    private static boolean isReservedIpv4(InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b.length != 4) {
            return false;
        }
        int o1 = b[0] & 0xFF;
        int o2 = b[1] & 0xFF;
        int o3 = b[2] & 0xFF;

        if (o1 == 100 && o2 >= 64 && o2 <= 127) {
            return true; // 100.64.0.0/10
        }
        if (o1 == 192 && o2 == 0 && (o3 == 0 || o3 == 2)) {
            return true; // 192.0.0.0/24, 192.0.2.0/24
        }
        if (o1 == 198 && (o2 == 18 || o2 == 19)) {
            return true; // 198.18.0.0/15
        }
        if (o1 == 198 && o2 == 51 && o3 == 100) {
            return true; // 198.51.100.0/24
        }
        if (o1 == 203 && o2 == 0 && o3 == 113) {
            return true; // 203.0.113.0/24
        }
        return o1 >= 240; // 240.0.0.0/4, incl. 255.255.255.255
    }

    private static boolean isAllZero(byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
