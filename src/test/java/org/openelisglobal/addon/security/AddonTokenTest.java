package org.openelisglobal.addon.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Base64;
import java.util.List;
import org.junit.Test;

public class AddonTokenTest {

    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes();
    private static final long NOW = 1_800_000_000L;

    private static String token(long expiresAt) {
        return AddonToken.sign(KEY,
                new AddonToken.Claims("hello", "7", List.of("orders.read"), List.of("DEV0001"), expiresAt));
    }

    @Test
    public void aSignedTokenVerifiesWithItsClaims() {
        AddonToken.Claims claims = AddonToken.verify(KEY, token(NOW + 600), NOW);
        assertEquals("hello", claims.addonId());
        assertEquals("7", claims.userId());
        assertEquals(List.of("orders.read"), claims.scopes());
        assertEquals(List.of("DEV0001"), claims.accessions());
        assertFalse(claims.isService());
    }

    @Test
    public void theSlotTravelsSignedInTheToken() {
        String token = AddonToken.sign(KEY,
                new AddonToken.Claims("mcp", "1", List.of(), List.of(), NOW + 600, "addon-settings-page-slot"));
        assertEquals("addon-settings-page-slot", AddonToken.verify(KEY, token, NOW).slot());
        assertEquals(null, AddonToken.verify(KEY, token(NOW + 600), NOW).slot());
    }

    @Test
    public void anExpiredTokenIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> AddonToken.verify(KEY, token(NOW), NOW));
    }

    @Test
    public void aTokenSignedWithAnotherKeyIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> AddonToken.verify("another key, another key, 32 by".getBytes(), token(NOW + 600), NOW));
    }

    @Test
    public void aTokenWhosePayloadWasChangedIsRefused() {
        String t = token(NOW + 600);
        String payload = t.substring(AddonToken.PREFIX.length(), t.lastIndexOf('.'));
        String json = new String(Base64.getUrlDecoder().decode(payload)).replace("DEV0001", "DEV9999");
        String forged = AddonToken.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes())
                + t.substring(t.lastIndexOf('.'));
        assertThrows(IllegalArgumentException.class, () -> AddonToken.verify(KEY, forged, NOW));
    }

    @Test
    public void garbageIsRefused() {
        for (String bad : List.of("", "oeat.", "oeat.a.b.c", "Bearer x", "oeat.!!.??")) {
            assertThrows(bad, IllegalArgumentException.class, () -> AddonToken.verify(KEY, bad, NOW));
        }
        assertTrue(token(NOW + 1).startsWith(AddonToken.PREFIX));
    }

    @Test
    public void sealedLabNumbersAreUnreadableInThePayloadButVerify() {
        String t = AddonToken.sign(KEY, new AddonToken.Claims("assistant", "7", List.of("ai.context"),
                List.of("DEV01260000000000063"), NOW + 600), true);
        String payload = new String(
                Base64.getUrlDecoder().decode(t.substring(AddonToken.PREFIX.length(), t.lastIndexOf('.'))));
        assertFalse(payload, payload.contains("DEV0126"));
        assertFalse(payload, payload.contains("\"c\""));
        AddonToken.Claims claims = AddonToken.verify(KEY, t, NOW);
        assertEquals(List.of("DEV01260000000000063"), claims.accessions());
        assertFalse(claims.isService());
    }

    @Test
    public void aSealedTokenFromAnotherKeyIsRefused() {
        String t = AddonToken.sign("another key, another key, 32 by".getBytes(),
                new AddonToken.Claims("assistant", "7", List.of("ai.context"), List.of("A1"), NOW + 600), true);
        assertThrows(IllegalArgumentException.class, () -> AddonToken.verify(KEY, t, NOW));
    }

    @Test
    public void sealingTheSameOrdersTwiceGivesDifferentCiphertext() {
        AddonToken.Claims c = new AddonToken.Claims("assistant", "7", List.of("ai.context"), List.of("A1"), NOW + 600);
        assertFalse(AddonToken.sign(KEY, c, true).equals(AddonToken.sign(KEY, c, true)));
    }
}
