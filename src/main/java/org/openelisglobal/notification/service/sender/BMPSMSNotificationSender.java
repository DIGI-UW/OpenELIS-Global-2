package org.openelisglobal.notification.service.sender;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BMPSMSNotificationSender {

    @Value("${org.openelisglobal.notification.sms.sender:Covid Lab}")
    private String senderId;

    // Supported values: TWILIO, AFRICA_TALKING, LEGACY
    @Value("${org.openelisglobal.notification.bmp.provider:TWILIO}")
    private String bmpProvider;

    // Twilio requires a "From" phone number (your Twilio number)
    @Value("${org.openelisglobal.notification.twilio.from:}")
    private String twilioFromNumber;

    @Autowired
    private CloseableHttpClient httpClient;

    public void send(SMSNotification notification) {
        ConfigurationProperties configurationProperties = ConfigurationProperties.getInstance();

        String address = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_ADDRESS);
        String username = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_USERNAME);
        String password = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_PASSWORD);

        if ("TWILIO".equalsIgnoreCase(bmpProvider)) {
            sendViaTwilio(notification, address, username, password);
        } else if ("AFRICA_TALKING".equalsIgnoreCase(bmpProvider)) {
            sendViaAfricasTalking(notification, address, username, password);
        } else {
            sendViaLegacyBMP(notification, address, username, password, "");
        }
    }

    private void sendViaTwilio(SMSNotification notification, String address, String accountSid, String authToken) {
        // Build Twilio API URL:
        // https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json
        if (address == null || address.isBlank()) {
            address = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
        }

        String phoneNumber = notification.getReceiverPhoneNumber();
        if (phoneNumber != null && !phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }

        String body = "To=" + URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8) + "&From="
                + URLEncoder.encode(twilioFromNumber, StandardCharsets.UTF_8) + "&Body="
                + URLEncoder.encode(notification.getMessage(), StandardCharsets.UTF_8);

        HttpPost postRequest = new HttpPost(address);
        postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        postRequest.setHeader("Authorization", "Basic " + credentials);

        try (CloseableHttpClient twilioClient = HttpClients.createDefault()) {
            postRequest.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = twilioClient.execute(postRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                LogEvent.logInfo(this.getClass().getSimpleName(), "sendViaTwilio",
                        "Twilio response: status=" + statusCode + " body=" + responseBody);
                if (statusCode < 200 || statusCode >= 300) {
                    LogEvent.logError(this.getClass().getSimpleName(), "sendViaTwilio",
                            "Twilio SMS failed: status=" + statusCode + " body=" + responseBody);
                }
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendViaTwilio",
                    "failed to communicate with Twilio API at " + address);
            LogEvent.logError(e);
        }
    }

    private void sendViaAfricasTalking(SMSNotification notification, String address, String username, String apiKey) {
        if (address == null || address.isBlank()) {
            address = "https://api.africastalking.com/version1/messaging";
        }

        String phoneNumber = notification.getReceiverPhoneNumber();
        if (phoneNumber != null && !phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }

        String body = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) + "&to="
                + URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8) + "&message="
                + URLEncoder.encode(notification.getMessage(), StandardCharsets.UTF_8);

        HttpPost postRequest = new HttpPost(address);
        postRequest.setHeader("Accept", "application/json");
        postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        postRequest.setHeader("apiKey", apiKey);

        try (CloseableHttpClient atClient = HttpClients.createDefault()) {
            postRequest.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = atClient.execute(postRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                LogEvent.logInfo(this.getClass().getSimpleName(), "sendViaAfricasTalking",
                        "Africa's Talking response: status=" + statusCode + " body=" + responseBody);
                if (statusCode < 200 || statusCode >= 300) {
                    LogEvent.logError(this.getClass().getSimpleName(), "sendViaAfricasTalking",
                            "Africa's Talking SMS failed: status=" + statusCode + " body=" + responseBody);
                }
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendViaAfricasTalking",
                    "failed to communicate with Africa's Talking API at " + address);
            LogEvent.logError(e);
        }
    }

    private void sendViaLegacyBMP(SMSNotification notification, String address, String username, String password,
            String phonePrefix) {

        String getString = address + "?UserName=" + URLEncoder.encode(username, StandardCharsets.UTF_8) + "&PassWord="
                + URLEncoder.encode(password, StandardCharsets.UTF_8) + "&UserData="
                + URLEncoder.encode(notification.getMessage(), StandardCharsets.UTF_8) + "&SenderId="
                + URLEncoder.encode(senderId, StandardCharsets.UTF_8) + "&Concatenated=0&Mode=0&Deferred=false&Number="
                + URLEncoder.encode(phonePrefix + notification.getReceiverPhoneNumber(), StandardCharsets.UTF_8)
                + "&Dsr=false";

        String statusReturned = null;
        HttpGet getRequest = new HttpGet(getString);
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "sendViaLegacyBMP",
                    "response status code from BMP SMS: " + response.getStatusLine().getStatusCode());
            statusReturned = EntityUtils.toString(response.getEntity(), "UTF-8");
            LogEvent.logDebug(this.getClass().getSimpleName(), "sendViaLegacyBMP",
                    "response status from BMP SMS: " + statusReturned);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendViaLegacyBMP",
                    "failed to communicate with " + address + " for sending SMS");
            LogEvent.logError(e);
        }

        if (!GenericValidator.isBlankOrNull(statusReturned) && statusReturned.contains("-")) {
            String returnedCode = statusReturned.substring(statusReturned.indexOf("-") + 1).strip();
            if (returnedCode.length() < 4) {
                LogEvent.logError(this.getClass().getSimpleName(), "sendViaLegacyBMP",
                        "response from BMP SMS: " + statusReturned);
                try {
                    int code = Integer.parseInt(returnedCode);
                    if (code == 91 && !"00".equals(phonePrefix)) {
                        this.sendViaLegacyBMP(notification, address, username, password, "00");
                    }
                } catch (NumberFormatException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "sendViaLegacyBMP",
                            "failed to parse error response from SMS server");
                }
            }
        }
    }
}
