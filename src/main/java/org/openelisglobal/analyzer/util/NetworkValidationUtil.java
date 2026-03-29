package org.openelisglobal.analyzer.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility for validating network addresses used in analyzer connections.
 *
 * <p>
 * Blocks addresses that should never be legitimate analyzer targets: loopback
 * (127.x), link-local/cloud metadata (169.254.x), multicast, and unspecified
 * (0.0.0.0). Private network ranges (10.x, 172.16.x, 192.168.x) are
 * intentionally allowed because laboratory analyzers typically reside on
 * private LANs.
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

    private static boolean isAllZero(byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
