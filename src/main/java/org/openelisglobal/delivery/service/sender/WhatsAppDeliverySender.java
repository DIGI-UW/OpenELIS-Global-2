package org.openelisglobal.delivery.service.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openelisglobal.delivery.service.sender.DeliveryException.Code;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * WhatsApp through the Twilio messages API, driven by the WhatsApp channel row:
 * the API address (host), the account sid (username), the auth token (secret)
 * and the sending number (sender address).
 */
@Component
public class WhatsAppDeliverySender implements DeliverySender {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${org.openelisglobal.notification.whatsapp.from:}")
    private String fallbackFromNumber;

    // The JVM default trust store: Twilio is a public API and the application
    // client carries only the internal mTLS trust store.
    private CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(10_000).setSocketTimeout(30_000).setConnectionRequestTimeout(10_000).build()).build();

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public boolean isSetUp(DeliveryChannel config) {
        return config != null && config.isActive() && !GenericValidator.isBlankOrNull(config.getUsername())
                && !GenericValidator.isBlankOrNull(config.getSecret())
                && !GenericValidator.isBlankOrNull(fromNumber(config));
    }

    @Override
    public String send(DeliveryChannel config, DeliveryMessage message) {
        if (!isSetUp(config)) {
            throw new DeliveryException(Code.NOT_SET_UP, MessageUtil.getMessage("delivery.error.notSetUp",
                    MessageUtil.getMessage("delivery.channel.whatsapp")));
        }
        String to = message.getAddress();
        if (GenericValidator.isBlankOrNull(to)) {
            throw new DeliveryException(Code.REJECTED, MessageUtil.getMessage("delivery.error.noAddress"));
        }
        if (!to.startsWith("+")) {
            to = "+" + to;
        }
        String address = GenericValidator.isBlankOrNull(config.getHost())
                ? "https://api.twilio.com/2010-04-01/Accounts/" + config.getUsername() + "/Messages.json"
                : config.getHost();

        StringBuilder body = new StringBuilder();
        body.append("To=").append(URLEncoder.encode("whatsapp:" + to, StandardCharsets.UTF_8));
        body.append("&From=").append(URLEncoder.encode("whatsapp:" + fromNumber(config), StandardCharsets.UTF_8));
        if (!GenericValidator.isBlankOrNull(message.getTemplateExternalId())) {
            body.append("&ContentSid=")
                    .append(URLEncoder.encode(message.getTemplateExternalId(), StandardCharsets.UTF_8));
            String variables = serialize(message);
            if (variables != null) {
                body.append("&ContentVariables=").append(URLEncoder.encode(variables, StandardCharsets.UTF_8));
            }
        } else {
            body.append("&Body=").append(
                    URLEncoder.encode(message.getBody() == null ? "" : message.getBody(), StandardCharsets.UTF_8));
        }

        HttpPost post = new HttpPost(address);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((config.getUsername() + ":" + config.getSecret()).getBytes(StandardCharsets.UTF_8)));
        post.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (status == 401 || status == 403) {
                throw new DeliveryException(Code.AUTHENTICATION, MessageUtil.getMessage("delivery.error.auth"));
            }
            if (status < 200 || status >= 300) {
                throw new DeliveryException(Code.REJECTED, "WhatsApp gateway answered " + status + ": "
                        + responseBody.substring(0, Math.min(responseBody.length(), 500)));
            }
            return messageId(responseBody);
        } catch (IOException e) {
            throw new DeliveryException(Code.UNREACHABLE,
                    MessageUtil.getMessage("delivery.error.unreachable", new Object[] { address, "443" }), e);
        }
    }

    private String fromNumber(DeliveryChannel config) {
        return GenericValidator.isBlankOrNull(config.getSenderAddress()) ? fallbackFromNumber
                : config.getSenderAddress();
    }

    private static String serialize(DeliveryMessage message) {
        if (message.getTemplateVariables() == null || message.getTemplateVariables().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(message.getTemplateVariables());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String messageId(String responseBody) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(responseBody);
            return node != null && node.hasNonNull("sid") ? node.get("sid").asText() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
