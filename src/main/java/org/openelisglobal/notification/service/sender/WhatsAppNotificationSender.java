package org.openelisglobal.notification.service.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.externalconnections.service.BasicAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.notification.valueholder.WhatsAppNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppNotificationSender implements ClientNotificationSender<WhatsAppNotification> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${org.openelisglobal.notification.whatsapp.from:}")
    private String whatsappFromNumber;

    // Use the JVM default-truststore HttpClient — Twilio is a public API and the
    // app-wide @Autowired CloseableHttpClient is configured with a custom
    // truststore
    // (for internal mTLS endpoints) that does not include DigiCert (Twilio's CA),
    // causing PKIX path-building failures on send. Tests still override this field
    // via ReflectionTestUtils.setField(..., "httpClient", mockHttpClient).
    private CloseableHttpClient httpClient = HttpClients.createDefault();

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Autowired
    private BasicAuthenticationDataService basicAuthenticationDataService;

    @Override
    public Class<WhatsAppNotification> forClass() {
        return WhatsAppNotification.class;
    }

    @Override
    public void send(WhatsAppNotification notification) {
        // Throws IllegalStateException on every "didn't actually send" path so
        // callers (dispatcher retry, Resend) can distinguish sent-vs-skipped.
        // A silent return here was the source of false SUCCESS log rows.
        Optional<ExternalConnection> connection = externalConnectionService.getMatch("programmedConnection",
                ProgrammedConnection.WHATSAPP_SERVER.name());
        if (connection.isEmpty() || !Boolean.TRUE.equals(connection.get().getActive())) {
            throw new IllegalStateException("WHATSAPP_SERVER external connection is missing or inactive");
        }

        Optional<BasicAuthenticationData> basicAuth = basicAuthenticationDataService
                .getByExternalConnection(connection.get().getId());
        if (basicAuth.isEmpty() || basicAuth.get().getUsername() == null || basicAuth.get().getUsername().isBlank()
                || basicAuth.get().getPassword() == null || basicAuth.get().getPassword().isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp credentials missing on WHATSAPP_SERVER external connection (BASIC auth username/password)");
        }
        String accountSid = basicAuth.get().getUsername();
        String authToken = basicAuth.get().getPassword();

        String address = connection.get().getUri() == null ? null : connection.get().getUri().toString();
        if (address == null || address.isBlank()) {
            address = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
        }

        if (whatsappFromNumber == null || whatsappFromNumber.isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp 'from' number is not configured (org.openelisglobal.notification.whatsapp.from)");
        }

        String to = notification.getReceiverPhoneNumber();
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("WhatsApp notification missing recipient phone number");
        }
        if (!to.startsWith("+")) {
            to = "+" + to;
        }

        StringBuilder body = new StringBuilder();
        body.append("To=").append(URLEncoder.encode("whatsapp:" + to, StandardCharsets.UTF_8));
        body.append("&From=").append(URLEncoder.encode("whatsapp:" + whatsappFromNumber, StandardCharsets.UTF_8));

        if (notification.getTemplateContentSid() != null && !notification.getTemplateContentSid().isBlank()) {
            body.append("&ContentSid=")
                    .append(URLEncoder.encode(notification.getTemplateContentSid(), StandardCharsets.UTF_8));
            String variablesJson = serializeVariables(notification.getTemplateVariables());
            if (variablesJson != null) {
                body.append("&ContentVariables=").append(URLEncoder.encode(variablesJson, StandardCharsets.UTF_8));
            }
        } else if (notification.getMessage() != null) {
            body.append("&Body=").append(URLEncoder.encode(notification.getMessage(), StandardCharsets.UTF_8));
        } else {
            throw new IllegalArgumentException(
                    "WhatsApp notification has neither a template ContentSid nor a free-text body");
        }

        HttpPost postRequest = new HttpPost(address);
        postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        postRequest.setHeader("Authorization", "Basic " + credentials);

        postRequest.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException(
                        "Twilio WhatsApp send failed: status=" + statusCode + " body=" + responseBody);
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "send",
                    "Twilio WhatsApp response: status=" + statusCode + " body=" + responseBody);
        } catch (IOException e) {
            throw new IllegalStateException("failed to communicate with Twilio WhatsApp API at " + address, e);
        }
    }

    private String serializeVariables(Map<String, String> templateVariables) {
        if (templateVariables == null || templateVariables.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(templateVariables);
        } catch (JsonProcessingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "serializeVariables",
                    "failed to serialize WhatsApp template variables");
            LogEvent.logError(e);
            return null;
        }
    }
}
