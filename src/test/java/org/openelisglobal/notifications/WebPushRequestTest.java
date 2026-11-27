package org.openelisglobal.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Builds a signed, encrypted web push request the way
 * NotificationRestController does, without sending it. The synchronous
 * PushService runs on Apache HttpClient 4, jose4j and Bouncy Castle only.
 */
public class WebPushRequestTest {

    private static final Base64.Encoder URL_SAFE = Base64.getUrlEncoder().withoutPadding();

    @BeforeClass
    public static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    public void preparePost_signsWithVapidAndEncryptsThePayload() throws Exception {
        KeyPair vapid = newP256KeyPair();
        KeyPair browser = newP256KeyPair();
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);

        PushService pushService = new PushService(
                URL_SAFE.encodeToString(Utils.encode((ECPublicKey) vapid.getPublic())),
                URL_SAFE.encodeToString(Utils.encode((ECPrivateKey) vapid.getPrivate())), "mailto:test@example.org");
        String payload = "{\"title\":\"OpenELIS Global Notification\"}";
        Notification notification = new Notification("https://push.example.org/send/abc",
                URL_SAFE.encodeToString(Utils.encode((ECPublicKey) browser.getPublic())), URL_SAFE.encodeToString(auth),
                payload);

        HttpPost post = pushService.preparePost(notification, Encoding.AES128GCM);

        assertEquals("https://push.example.org/send/abc", post.getURI().toString());
        assertEquals("aes128gcm", post.getFirstHeader("Content-Encoding").getValue());
        assertTrue(post.getFirstHeader("Authorization").getValue().startsWith("vapid t="));
        byte[] body = post.getEntity().getContent().readAllBytes();
        assertTrue(body.length > 0);
        assertFalse(new String(body, StandardCharsets.ISO_8859_1).contains("OpenELIS Global Notification"));
    }

    private static KeyPair newP256KeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
        return generator.generateKeyPair();
    }
}
