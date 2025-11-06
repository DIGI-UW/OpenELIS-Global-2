package org.openelisglobal.notification.service.sender;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
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

    @Autowired
    private CloseableHttpClient httpClient;

    public void send(SMSNotification notification) {
        ConfigurationProperties configurationProperties = ConfigurationProperties.getInstance();

        String address = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_ADDRESS);
        String username = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_USERNAME);
        String password = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_PASSWORD);

        sendSMS(notification, address, username, password, "");
    }

    private void sendSMS(SMSNotification notification, String address, String username, String password,
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
            System.out.println("response status code from BMP SMS: " + response.getStatusLine().getStatusCode());
            statusReturned = EntityUtils.toString(response.getEntity(), "UTF-8");
            System.out.println("response status from BMP SMS: " + statusReturned);
            LogEvent.logDebug(this.getClass().getSimpleName(), "sendSMS",
                    "response status code from BMP SMS: " + response.getStatusLine().getStatusCode());
            LogEvent.logDebug(this.getClass().getSimpleName(), "sendSMS",
                    "response status from BMP SMS: " + statusReturned);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendSMS",
                    "failed to communicate with " + address + " for sending SMS");
            LogEvent.logError(e);
        }

        if (!GenericValidator.isBlankOrNull(statusReturned) && statusReturned.contains("-")) {
            String returnedCode = statusReturned.substring(statusReturned.indexOf("-") + 1).strip();
            if (returnedCode.length() < 4) {
                // an error has occurred
                LogEvent.logError(this.getClass().getSimpleName(), "sendSMS",
                        "response from BMP SMS: " + statusReturned);
                try {
                    int code = Integer.parseInt(returnedCode);
                    if (code == 91 && !"00".equals(phonePrefix)) {
                        // phone format, try with 00 prefix
                        this.sendSMS(notification, address, username, password, "00");
                    }
                } catch (NumberFormatException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "sendSMS",
                            "failed to parse error response from SMS server");
                }
            }
        }
    }
}
