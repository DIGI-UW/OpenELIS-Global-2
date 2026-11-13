package org.openelisglobal.notification.service.sender;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.delivery.service.sender.*;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.externalconnections.service.*;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.notification.valueholder.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Real SMTP conversations with ephemeral certificates and synthetic messages
 * only.
 */
public class EmailTransportSecurityTest {
    private SSLContext previousContext;
    private final Properties defaults = new Properties();
    private final EmailDeliverySender delivery = new EmailDeliverySender();
    private final EmailNotificationSender notifications = new EmailNotificationSender();
    private final ExternalConnection connection = new ExternalConnection();

    @Before
    public void setUp() throws Exception {
        previousContext = SSLContext.getDefault();
        try (InputStream input = Files.newInputStream(Path.of("volume/properties/common.properties"))) {
            defaults.load(input);
        }
        ReflectionTestUtils.setField(delivery, "bcc", defaults.getProperty("org.openelisglobal.mail.bcc"));
        ReflectionTestUtils.setField(delivery, "fallbackFrom", defaults.getProperty("org.openelisglobal.mail.from"));
        ReflectionTestUtils.setField(notifications, "bcc", defaults.getProperty("org.openelisglobal.mail.bcc"));
        ReflectionTestUtils.setField(notifications, "from", "lab@example.invalid");
        ExternalConnectionService connections = mock(ExternalConnectionService.class);
        BasicAuthenticationDataService auth = mock(BasicAuthenticationDataService.class);
        connection.setId(123);
        connection.setActive(true);
        when(connections.getMatch("programmedConnection", "SMTP_SERVER")).thenReturn(Optional.of(connection));
        when(auth.getByExternalConnection(123)).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(notifications, "externalConnectionService", connections);
        ReflectionTestUtils.setField(notifications, "basicAuthenticationDataService", auth);
    }

    @After
    public void restoreTls() {
        SSLContext.setDefault(previousContext);
    }

    @Test
    public void distributionHasNoSenderOrBlindCopyAndDisallowsHttp() {
        assertEquals("", defaults.getProperty("org.openelisglobal.mail.from"));
        assertEquals("", defaults.getProperty("org.openelisglobal.mail.bcc"));
        assertEquals("false", defaults.getProperty("org.openelisglobal.fhir.subscriber.allowHTTP"));
        DeliveryChannel config = channel(12345, DeliveryChannel.Security.STARTTLS);
        config.setSenderAddress(null);
        assertFalse(delivery.isSetUp(config));
    }

    @Test
    public void deliverySendsOnlyToIntendedRecipientOverStartTls() throws Exception {
        try (SmtpReceiver receiver = new SmtpReceiver("localhost", true, false)) {
            SSLContext.setDefault(receiver.trustedClient);
            delivery.send(channel(receiver.port(), null), message());
            receiver.assertDelivered("recipient@example.invalid");
        }
    }

    @Test
    public void deliverySupportsImplicitTlsAndExplicitAuthorizedBlindCopy() throws Exception {
        try (SmtpReceiver receiver = new SmtpReceiver("localhost", true, true)) {
            SSLContext.setDefault(receiver.trustedClient);
            ReflectionTestUtils.setField(delivery, "bcc", "authorized@example.invalid");
            delivery.send(channel(receiver.port(), DeliveryChannel.Security.SSL_TLS), message());
            receiver.assertDelivered("recipient@example.invalid", "authorized@example.invalid");
        }
    }

    @Test
    public void notificationSendsOnlyToIntendedRecipientOverStartTls() throws Exception {
        try (SmtpReceiver receiver = new SmtpReceiver("localhost", true, false)) {
            SSLContext.setDefault(receiver.trustedClient);
            connection.setUri(URI.create("smtp://localhost:" + receiver.port()));
            notifications.send(notification());
            receiver.assertDelivered("recipient@example.invalid");
        }
    }

    @Test
    public void notificationPreservesExplicitMessageBlindCopy() throws Exception {
        try (SmtpReceiver receiver = new SmtpReceiver("localhost", true, false)) {
            SSLContext.setDefault(receiver.trustedClient);
            connection.setUri(URI.create("smtp://localhost:" + receiver.port()));
            EmailNotification message = notification();
            message.setBccs(List.of("authorized@example.invalid"));
            notifications.send(message);
            receiver.assertDelivered("recipient@example.invalid", "authorized@example.invalid");
        }
    }

    @Test
    public void bothSendersRefuseServersWithoutStartTlsBeforeSendingAnEnvelope() throws Exception {
        assertRefused("localhost", false, true);
    }

    @Test
    public void bothSendersRefuseTrustedCertificatesForAnotherHost() throws Exception {
        assertRefused("other.example.invalid", true, true);
    }

    @Test
    public void bothSendersRefuseUntrustedCertificates() throws Exception {
        assertRefused("localhost", true, false);
    }

    private void assertRefused(String certificateHost, boolean tls, boolean trust) throws Exception {
        for (boolean notification : new boolean[] { false, true }) {
            try (SmtpReceiver receiver = new SmtpReceiver(certificateHost, tls, false)) {
                SSLContext.setDefault(trust ? receiver.trustedClient : previousContext);
                if (notification) {
                    connection.setUri(URI.create("smtp://localhost:" + receiver.port()));
                    assertThrows(org.springframework.mail.MailException.class,
                            () -> notifications.send(notification()));
                } else {
                    assertThrows(DeliveryException.class, () -> delivery
                            .send(channel(receiver.port(), DeliveryChannel.Security.STARTTLS), message()));
                }
                receiver.await();
                assertTrue("No SMTP recipient before verified TLS", receiver.recipients.isEmpty());
                assertFalse(receiver.dataReceived);
            }
        }
    }

    private DeliveryChannel channel(int port, DeliveryChannel.Security security) {
        DeliveryChannel config = new DeliveryChannel();
        config.setActive(true);
        config.setHost("localhost");
        config.setPort(port);
        config.setSecurity(security);
        config.setSenderAddress("lab@example.invalid");
        return config;
    }

    private DeliveryMessage message() {
        return new DeliveryMessage(NotificationChannel.EMAIL, "recipient@example.invalid", "Synthetic delivery",
                "Synthetic result ready");
    }

    private EmailNotification notification() {
        EmailNotification notification = new EmailNotification();
        notification.setRecipientEmailAddress("recipient@example.invalid");
        notification.setPayload(NotificationPayload.of("Synthetic delivery", "Synthetic result ready"));
        return notification;
    }

    private static class SmtpReceiver implements AutoCloseable {
        final SSLContext serverContext;
        final SSLContext trustedClient;
        final ServerSocket listener;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<?> session;
        final List<String> recipients = new ArrayList<>();
        final StringBuilder body = new StringBuilder();
        boolean dataReceived;

        SmtpReceiver(String host, boolean advertiseTls, boolean implicitTls) throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            X500Name name = new X500Name("CN=" + host);
            Instant now = Instant.now();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name,
                    new BigInteger(128, new SecureRandom()), Date.from(now.minusSeconds(60)),
                    Date.from(now.plusSeconds(3600)), name, pair.getPublic());
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.dNSName, host)));
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                    builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
            KeyStore keys = KeyStore.getInstance("PKCS12");
            keys.load(null, null);
            keys.setKeyEntry("smtp", pair.getPrivate(), new char[0], new X509Certificate[] { cert });
            KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            km.init(keys, new char[0]);
            KeyStore trust = KeyStore.getInstance("PKCS12");
            trust.load(null, null);
            trust.setCertificateEntry("smtp", cert);
            TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tm.init(trust);
            serverContext = SSLContext.getInstance("TLS");
            serverContext.init(km.getKeyManagers(), null, null);
            trustedClient = SSLContext.getInstance("TLS");
            trustedClient.init(null, tm.getTrustManagers(), null);
            listener = implicitTls ? serverContext.getServerSocketFactory().createServerSocket() : new ServerSocket();
            listener.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            listener.setSoTimeout(10000);
            session = executor.submit(() -> receive(advertiseTls));
        }

        int port() {
            return listener.getLocalPort();
        }

        void receive(boolean advertiseTls) {
            try (Socket plain = listener.accept()) {
                plain.setSoTimeout(10000);
                Socket socket = plain;
                BufferedReader input = reader(socket);
                BufferedWriter output = writer(socket);
                reply(output, "220 localhost test receiver");
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.startsWith("EHLO")) {
                        reply(output, advertiseTls ? "250-localhost\r\n250 STARTTLS" : "250 localhost");
                    } else if (line.equals("STARTTLS")) {
                        reply(output, "220 Ready for TLS");
                        SSLSocket secure = (SSLSocket) serverContext.getSocketFactory().createSocket(socket,
                                "localhost", socket.getPort(), false);
                        secure.setUseClientMode(false);
                        secure.setSoTimeout(10000);
                        secure.startHandshake();
                        socket = secure;
                        input = reader(socket);
                        output = writer(socket);
                    } else if (line.startsWith("RCPT TO:")) {
                        recipients.add(line.substring(line.indexOf('<') + 1, line.indexOf('>')));
                        reply(output, "250 OK");
                    } else if (line.equals("DATA")) {
                        reply(output, "354 Send message");
                        while ((line = input.readLine()) != null && !line.equals("."))
                            body.append(line).append('\n');
                        dataReceived = true;
                        reply(output, "250 Accepted");
                    } else if (line.equals("QUIT")) {
                        reply(output, "221 Bye");
                        break;
                    } else {
                        reply(output, "250 OK");
                    }
                }
            } catch (SSLException | SocketException expectedWhenClientRefusesTls) {
                // Refusal tests assert that no recipient or DATA was accepted.
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private BufferedReader reader(Socket socket) throws IOException {
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        }

        private BufferedWriter writer(Socket socket) throws IOException {
            return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private void reply(BufferedWriter output, String response) throws IOException {
            output.write(response + "\r\n");
            output.flush();
        }

        void await() throws Exception {
            session.get(12, TimeUnit.SECONDS);
        }

        void assertDelivered(String... expected) throws Exception {
            await();
            assertEquals(Set.of(expected), new HashSet<>(recipients));
            assertTrue(dataReceived);
            assertTrue(body.toString().contains("Synthetic result ready"));
            assertTrue(body.toString().contains("Subject: Synthetic delivery"));
        }

        public void close() throws Exception {
            listener.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(12, TimeUnit.SECONDS));
        }
    }
}
