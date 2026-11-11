package org.openelisglobal.notification.service.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.impl.client.HttpClients;
import org.jsmpp.PDUStringException;
import org.jsmpp.SMPPConstant;
import org.jsmpp.bean.CancelSm;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.QuerySm;
import org.jsmpp.bean.ReplaceSm;
import org.jsmpp.bean.SubmitMulti;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.BindRequest;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.QuerySmResult;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPServerSessionListener;
import org.jsmpp.session.ServerMessageReceiverListener;
import org.jsmpp.session.Session;
import org.jsmpp.util.MessageId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.delivery.service.sender.DeliveryException;
import org.openelisglobal.delivery.service.sender.DeliveryException.Code;
import org.openelisglobal.delivery.service.sender.DeliveryMessage;
import org.openelisglobal.delivery.service.sender.SmsDeliverySender;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class SmsGatewaySendersTest extends BaseWebContextSensitiveTest {

    private static final Property[] GATEWAY_PROPERTIES = { Property.PATIENT_RESULTS_SMPP_SMS_ENABLED,
            Property.PATIENT_RESULTS_SMPP_SMS_ADDRESS, Property.PATIENT_RESULTS_SMPP_SMS_USERNAME,
            Property.PATIENT_RESULTS_SMPP_SMS_PASSWORD, Property.PATIENT_RESULTS_BMP_SMS_ENABLED,
            Property.PATIENT_RESULTS_BMP_SMS_ADDRESS, Property.PATIENT_RESULTS_BMP_SMS_USERNAME,
            Property.PATIENT_RESULTS_BMP_SMS_PASSWORD };

    @Autowired
    private SMPPNotificationSender smppSender;

    @Autowired
    private BMPSMSNotificationSender bmpSender;

    @Autowired
    private SmsDeliverySender smsDeliverySender;

    private final Map<Property, String> originalProperties = new EnumMap<>(Property.class);
    private Object originalBmpProvider;
    private Object originalOzekiActive;
    private FakeSmsc smsc;
    private HttpServer gateway;

    @Before
    public void setUp() {
        ConfigurationProperties properties = ConfigurationProperties.getInstance();
        for (Property property : GATEWAY_PROPERTIES) {
            originalProperties.put(property, properties.getPropertyValue(property));
        }
        originalBmpProvider = ReflectionTestUtils.getField(bmpSender, "bmpProvider");
        originalOzekiActive = ReflectionTestUtils.getField(smsDeliverySender, "ozekiActive");
        ReflectionTestUtils.setField(smsDeliverySender, "ozekiActive", false);
        set(Property.PATIENT_RESULTS_SMPP_SMS_ENABLED, "false");
        set(Property.PATIENT_RESULTS_BMP_SMS_ENABLED, "false");
        set(Property.PATIENT_RESULTS_BMP_SMS_ADDRESS, "http://127.0.0.1:" + freePort() + "/bmp");
        set(Property.PATIENT_RESULTS_BMP_SMS_USERNAME, "bmp-user");
        set(Property.PATIENT_RESULTS_BMP_SMS_PASSWORD, "bmp-password");
    }

    @After
    public void tearDown() throws IOException {
        if (smsc != null) {
            smsc.close();
        }
        if (gateway != null) {
            gateway.stop(0);
        }
        originalProperties.forEach((property, value) -> set(property, value == null ? "" : value));
        ReflectionTestUtils.setField(bmpSender, "bmpProvider", originalBmpProvider);
        ReflectionTestUtils.setField(smsDeliverySender, "ozekiActive", originalOzekiActive);
    }

    @Test
    public void smppSendsThroughTheSmppConnectionNotTheBmpOne() throws Exception {
        smsc = new FakeSmsc("smpp-user", "smpp-pw");
        configureSmpp("smpp://127.0.0.1:" + smsc.port, "smpp-user", "smpp-pw");

        smppSender.send(sms("+256700000001", "Result ready"));

        assertEquals("smpp-user", smsc.boundAs.get());
        assertEquals(1, smsc.received.size());
        assertEquals("+256700000001", smsc.received.get(0).getDestAddress());
        assertEquals("Result ready", new String(smsc.received.get(0).getShortMessage(), StandardCharsets.UTF_8));
    }

    @Test
    public void smppAddressWithoutSchemeIsAccepted() throws Exception {
        smsc = new FakeSmsc("smpp-user", "smpp-pw");
        configureSmpp("127.0.0.1:" + smsc.port, "smpp-user", "smpp-pw");

        smppSender.send(sms("+256700000002", "Hello"));

        assertEquals(1, smsc.received.size());
    }

    @Test
    public void smppRefusedLoginSaysAuthenticationFailed() throws Exception {
        smsc = new FakeSmsc("smpp-user", "smpp-pw");
        configureSmpp("smpp://127.0.0.1:" + smsc.port, "smpp-user", "wrongpw");

        DeliveryException e = expectFailure(() -> smppSender.send(sms("+256700000003", "Hello")));

        assertEquals(Code.AUTHENTICATION, e.getCode());
        assertTrue(smsc.received.isEmpty());
    }

    @Test
    public void smppPasswordTooLongForSmppNeverShowsThePassword() throws Exception {
        smsc = new FakeSmsc("smpp-user", "smpp-pw");
        configureSmpp("smpp://127.0.0.1:" + smsc.port, "smpp-user", "secret-longer-than-8");

        DeliveryException e = expectFailure(() -> smppSender.send(sms("+256700000012", "Hello")));

        assertEquals(Code.AUTHENTICATION, e.getCode());
        assertFalse(e.getMessage(), e.getMessage().contains("secret-longer-than-8"));
    }

    @Test
    public void smppServerDownSaysUnreachable() {
        int port = freePort();
        configureSmpp("smpp://127.0.0.1:" + port, "smpp-user", "smpp-pw");

        DeliveryException e = expectFailure(() -> smppSender.send(sms("+256700000004", "Hello")));

        assertEquals(Code.UNREACHABLE, e.getCode());
        assertTrue(e.getMessage(),
                e.getMessage().contains("127.0.0.1") && e.getMessage().contains(String.valueOf(port)));
    }

    @Test
    public void smppWithoutAddressIsNotSetUp() {
        configureSmpp("", "smpp-user", "smpp-pw");

        DeliveryException e = expectFailure(() -> smppSender.send(sms("+256700000005", "Hello")));

        assertEquals(Code.NOT_SET_UP, e.getCode());
        assertTrue(!e.isRetryable());
    }

    @Test
    public void smsChannelKeepsTheGatewayReason() {
        set(Property.PATIENT_RESULTS_SMPP_SMS_ENABLED, "true");
        set(Property.PATIENT_RESULTS_SMPP_SMS_ADDRESS, "");
        DeliveryChannel channel = new DeliveryChannel();
        channel.setActive(true);

        try {
            smsDeliverySender.send(channel, new DeliveryMessage(NotificationChannel.SMS, "+256700000006", "s", "b"));
            fail("expected the send to fail");
        } catch (DeliveryException e) {
            assertEquals(Code.NOT_SET_UP, e.getCode());
        }
    }

    @Test
    public void twilioRefusalIsReported() throws Exception {
        startGateway(400, "{\"message\":\"A 'From' phone number is required.\"}");
        ReflectionTestUtils.setField(bmpSender, "bmpProvider", "TWILIO");

        DeliveryException e = expectFailure(() -> bmpSender.send(sms("+256700000007", "Hello")));

        assertEquals(Code.REJECTED, e.getCode());
        assertTrue(e.getMessage(), e.getMessage().contains("From' phone number is required"));
    }

    @Test
    public void africasTalkingBadKeySaysAuthenticationFailed() throws Exception {
        startGateway(401, "The supplied authentication is invalid");
        ReflectionTestUtils.setField(bmpSender, "bmpProvider", "AFRICA_TALKING");

        DeliveryException e = expectFailure(() -> bmpSender.send(sms("+256700000008", "Hello")));

        assertEquals(Code.AUTHENTICATION, e.getCode());
    }

    @Test
    public void legacyGatewayErrorCodeIsReported() throws Exception {
        startGateway(200, "ERR - 12");
        ReflectionTestUtils.setField(bmpSender, "bmpProvider", "LEGACY");
        Object contextClient = ReflectionTestUtils.getField(bmpSender, "httpClient");
        ReflectionTestUtils.setField(bmpSender, "httpClient", HttpClients.createDefault());
        try {
            DeliveryException e = expectFailure(() -> bmpSender.send(sms("256700000009", "Hello")));

            assertEquals(Code.REJECTED, e.getCode());
            assertTrue(e.getMessage(), e.getMessage().contains("ERR - 12"));
        } finally {
            ReflectionTestUtils.setField(bmpSender, "httpClient", contextClient);
        }
    }

    @Test
    public void bmpGatewayAcceptingTheMessageSucceeds() throws Exception {
        startGateway(201, "{\"sid\":\"SM1\"}");
        ReflectionTestUtils.setField(bmpSender, "bmpProvider", "TWILIO");

        bmpSender.send(sms("+256700000010", "Hello"));
    }

    @Test
    public void bmpGatewayDownSaysUnreachable() {
        ReflectionTestUtils.setField(bmpSender, "bmpProvider", "AFRICA_TALKING");

        DeliveryException e = expectFailure(() -> bmpSender.send(sms("+256700000011", "Hello")));

        assertEquals(Code.UNREACHABLE, e.getCode());
    }

    private void configureSmpp(String address, String username, String password) {
        set(Property.PATIENT_RESULTS_SMPP_SMS_ADDRESS, address);
        set(Property.PATIENT_RESULTS_SMPP_SMS_USERNAME, username);
        set(Property.PATIENT_RESULTS_SMPP_SMS_PASSWORD, password);
    }

    private void startGateway(int status, String body) throws IOException {
        gateway = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        gateway.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        gateway.start();
        set(Property.PATIENT_RESULTS_BMP_SMS_ADDRESS,
                "http://127.0.0.1:" + gateway.getAddress().getPort() + "/messages");
    }

    private static void set(Property property, String value) {
        ConfigurationProperties.getInstance().setPropertyValue(property, value);
    }

    private static SMSNotification sms(String number, String text) {
        SMSNotification notification = new SMSNotification();
        notification.setReceiverPhoneNumber(number);
        notification.setPayload(NotificationPayload.of("subject", text));
        return notification;
    }

    private static DeliveryException expectFailure(Runnable send) {
        try {
            send.run();
        } catch (DeliveryException e) {
            return e;
        }
        fail("expected a DeliveryException");
        return null;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A one-connection SMSC that accepts one system id and password. */
    private static final class FakeSmsc implements ServerMessageReceiverListener {

        final int port;
        final AtomicReference<String> boundAs = new AtomicReference<>();
        final List<SubmitSm> received = new CopyOnWriteArrayList<>();
        private final SMPPServerSessionListener listener;
        private final Thread acceptor;

        FakeSmsc(String systemId, String password) throws IOException {
            port = freePort();
            listener = new SMPPServerSessionListener(port);
            listener.setMessageReceiverListener(this);
            acceptor = new Thread(() -> {
                try {
                    SMPPServerSession session = listener.accept();
                    BindRequest bind = session.waitForBind(5000);
                    if (systemId.equals(bind.getSystemId()) && password.equals(bind.getPassword())) {
                        boundAs.set(bind.getSystemId());
                        bind.accept("fake-smsc");
                    } else {
                        bind.reject(SMPPConstant.STAT_ESME_RINVPASWD);
                    }
                } catch (Exception e) {
                    boundAs.compareAndSet(null, "error: " + e.getMessage());
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
        }

        void close() throws IOException {
            listener.close();
        }

        @Override
        public MessageId onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source) throws ProcessRequestException {
            received.add(submitSm);
            try {
                return new MessageId("1");
            } catch (PDUStringException e) {
                throw new ProcessRequestException(e.getMessage(), SMPPConstant.STAT_ESME_RSYSERR);
            }
        }

        @Override
        public SubmitMultiResult onAcceptSubmitMulti(SubmitMulti submitMulti, SMPPServerSession source)
                throws ProcessRequestException {
            throw new ProcessRequestException("not supported", SMPPConstant.STAT_ESME_RSYSERR);
        }

        @Override
        public QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession source) throws ProcessRequestException {
            throw new ProcessRequestException("not supported", SMPPConstant.STAT_ESME_RSYSERR);
        }

        @Override
        public void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession source) throws ProcessRequestException {
            throw new ProcessRequestException("not supported", SMPPConstant.STAT_ESME_RSYSERR);
        }

        @Override
        public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession source) throws ProcessRequestException {
            throw new ProcessRequestException("not supported", SMPPConstant.STAT_ESME_RSYSERR);
        }

        @Override
        public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
            throw new ProcessRequestException("not supported", SMPPConstant.STAT_ESME_RSYSERR);
        }
    }
}
