package org.openelisglobal.delivery.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.DeliveryRequest;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The delivery REST surface: channels without their secrets, lab-wide settings,
 * and the Sent Messages list with its filters, detail and resend. Who may call
 * what is pinned by {@link DeliveryRestControllerSecurityTest}; method security
 * is inert in this context.
 */
public class DeliveryRestControllerTest extends BaseWebContextSensitiveTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int ADMIN_USER = 9301;
    private static final int REPORTS_USER = 9302;

    @Autowired
    private DeliveryOutboxService outbox;

    private UserSessionData admin;
    private UserSessionData reports;

    /**
     * The patient report fixture carries the system users 9301 and 9302; they stand
     * in for an administrator and a report user.
     */
    @Before
    public void seedUsers() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        ensureDeliverySettingsRows();
        cleanRows();
        admin = session(ADMIN_USER);
        reports = session(REPORTS_USER);
    }

    @After
    public void removeRows() {
        cleanRows();
    }

    /**
     * Every channel back to not set up, so no test inherits the Email set-up
     * another one saved.
     */
    private void cleanRows() {
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_attempt");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_channel");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log");
        jdbcTemplate.update("UPDATE clinlims.delivery_channel SET active = true, host = NULL, port = NULL,"
                + " username = NULL, secret = NULL, sender_address = NULL, sender_name = NULL, reply_to = NULL,"
                + " last_success_at = NULL, last_failure_at = NULL, last_error = NULL");
    }

    private void setUpEmail() throws Exception {
        mockMvc.perform(put("/rest/delivery/channels/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true,\"host\":\"smtp.example.org\",\"port\":587,\"security\":\"STARTTLS\","
                        + "\"username\":\"lab\",\"secret\":\"s3cret\",\"senderAddress\":\"lab@example.org\"}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk());
    }

    /**
     * Liquibase seeds the delivery.* rows, but a sibling fixture can replace
     * site_information for the rest of the run; the settings endpoints need them.
     */
    private void ensureDeliverySettingsRows() {
        ensureSiteInformation("delivery.retryDelays", "1,5,15");
        ensureSiteInformation("delivery.defaultFormat", "MASKED");
        ensureSiteInformation("delivery.holdMinutes", "10");
        ensureSiteInformation("delivery.sendPartials", "true");
        ensureSiteInformation("delivery.labContact", null);
        ensureSiteInformation("delivery.location", "LOCAL");
        ensureSiteInformation("delivery.centralAddress", null);
        ensureSiteInformation("delivery.secureLinks", "false");
        ensureSiteInformation("delivery.linkDays", "14");
        ensureSiteInformation("delivery.countryCode", null);
    }

    private static UserSessionData session(int userId) {
        UserSessionData data = new UserSessionData();
        data.setSytemUserId(userId);
        return data;
    }

    @Test
    public void channelsNeverCarryTheSecretAndABlankPutKeepsIt() throws Exception {
        mockMvc.perform(put("/rest/delivery/channels/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true,\"host\":\"smtp.example.org\",\"port\":587,\"security\":\"STARTTLS\","
                        + "\"username\":\"lab\",\"secret\":\"s3cret\",\"senderAddress\":\"lab@example.org\"}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.secretSet").value(true)).andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.status").value("READY"));

        MvcResult listed = mockMvc
                .perform(get("/rest/delivery/channels").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andReturn();
        JsonNode channels = JSON.readTree(listed.getResponse().getContentAsString());
        assertEquals(3, channels.size());
        assertFalse(listed.getResponse().getContentAsString().contains("s3cret"));

        mockMvc.perform(put("/rest/delivery/channels/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true,\"host\":\"smtp.example.org\",\"port\":587,\"security\":\"STARTTLS\","
                        + "\"username\":\"lab\",\"secret\":\"\",\"senderAddress\":\"lab@example.org\","
                        + "\"senderName\":\"CPHL\"}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.secretSet").value(true)).andExpect(jsonPath("$.senderName").value("CPHL"));

        mockMvc.perform(put("/rest/delivery/channels/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true,\"host\":\"smtp.example.org\",\"port\":70000}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("port"));
    }

    /**
     * The status describes the configuration on file: a failure recorded against
     * the old host is retired once the host changes, and kept when only the switch
     * is toggled.
     */
    @Test
    public void aReconfiguredChannelStartsReadyAgain() throws Exception {
        setUpEmail();
        jdbcTemplate.update("UPDATE clinlims.delivery_channel SET last_failure_at = now(),"
                + " last_error = 'Could not reach smtp.example.org on port 587' WHERE channel = 'EMAIL'");
        mockMvc.perform(get("/rest/delivery/channels/email").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILING"));

        mockMvc.perform(put("/rest/delivery/channels/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false,\"host\":\"smtp.example.org\",\"port\":587,\"security\":\"STARTTLS\","
                        + "\"username\":\"lab\",\"secret\":\"\",\"senderAddress\":\"lab@example.org\"}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.lastError").value("Could not reach smtp.example.org on port 587"));

        mockMvc.perform(put("/rest/delivery/channels/email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true,\"host\":\"mail.example.org\",\"port\":587,\"security\":\"STARTTLS\","
                        + "\"username\":\"lab\",\"secret\":\"\",\"senderAddress\":\"lab@example.org\"}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY")).andExpect(jsonPath("$.lastError").doesNotExist());
    }

    @Test
    public void settingsRoundTripAndRefuseBadValues() throws Exception {
        mockMvc.perform(put("/rest/delivery/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"retryDelays\":[2,10],\"defaultFormat\":\"MASKED\",\"holdMinutes\":5,"
                        + "\"sendPartials\":false,\"labContact\":\"CPHL 7000 0000\",\"location\":\"LOCAL\","
                        + "\"secureLinks\":false,\"linkDays\":14,\"countryCode\":\"+675\"}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.retryDelays[0]").value(2)).andExpect(jsonPath("$.holdMinutes").value(5))
                .andExpect(jsonPath("$.labContact").value("CPHL 7000 0000"));
        mockMvc.perform(get("/rest/delivery/settings").sessionAttr(IActionConstants.USER_SESSION_DATA, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.retryDelays[1]").value(10))
                .andExpect(jsonPath("$.sendPartials").value(false));
        mockMvc.perform(put("/rest/delivery/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"retryDelays\":[],\"defaultFormat\":\"MASKED\",\"holdMinutes\":5,\"location\":\"LOCAL\","
                        + "\"linkDays\":14}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("retryDelays"));
        mockMvc.perform(put("/rest/delivery/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"retryDelays\":[1,5,15],\"defaultFormat\":\"MASKED\",\"holdMinutes\":10,"
                        + "\"sendPartials\":true,\"location\":\"LOCAL\",\"linkDays\":14}")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, admin)).andExpect(status().isOk());
    }

    /**
     * Email is set up first so the alert queues; a queued message cannot be resent,
     * a failed one can.
     */
    @Test
    public void sentMessagesAreListedFilteredAndResent() throws Exception {
        setUpEmail();
        NotificationLog alert = outbox.enqueue(new DeliveryRequest().setProduct(Product.ALERT).setEventCode("ALERT")
                .setRuleName("Cholera").setRecipientType(NotificationRecipientType.TYPED_ADDRESS)
                .setRecipientDisplayName("pho@example.org").setRecipientEmail("pho@example.org").setSubject("Alert")
                .setMessage("Cholera RDT positive").setReferenceAccession("ACC-300")
                .addChannel(NotificationChannel.EMAIL));
        outbox.enqueue(new DeliveryRequest().setProduct(Product.TRIGGER).setEventCode("REFERRAL_OUT")
                .setRecipientType(NotificationRecipientType.ORDERING_PROVIDER).setRecipientDisplayName("Dr Kila")
                .setRecipientPhone("+67570000000").setSubject("Referred").setMessage("Sample referred")
                .setReferenceAccession("ACC-301").addChannel(NotificationChannel.WHATSAPP));

        mockMvc.perform(get("/rest/delivery/messages").param("product", "alert")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, reports)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.rows[0].ruleName").value("Cholera"))
                .andExpect(jsonPath("$.rows[0].automatic").value(true));
        mockMvc.perform(get("/rest/delivery/messages").param("labNo", "ACC-301")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, reports)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.rows[0].product").value("TRIGGER"));
        mockMvc.perform(get("/rest/delivery/messages").param("channel", "whatsapp").param("pageSize", "1")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, reports)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.pageSize").value(1));
        mockMvc.perform(get("/rest/delivery/messages/" + alert.getId()).sessionAttr(IActionConstants.USER_SESSION_DATA,
                reports)).andExpect(status().isOk())
                .andExpect(jsonPath("$.renderedMessage").value("Cholera RDT positive"));
        mockMvc.perform(
                get("/rest/delivery/messages/999999999").sessionAttr(IActionConstants.USER_SESSION_DATA, reports))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/rest/delivery/messages/" + alert.getId() + "/resend")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, reports)).andExpect(status().isBadRequest());
        jdbcTemplate.update(
                "UPDATE clinlims.notification_log_channel SET status = 'FAILED' WHERE notification_log_id = ?",
                alert.getId());
        jdbcTemplate.update("UPDATE clinlims.notification_log SET overall_status = 'FAILED' WHERE id = ?",
                alert.getId());
        MvcResult resent = mockMvc
                .perform(post("/rest/delivery/messages/" + alert.getId() + "/resend")
                        .sessionAttr(IActionConstants.USER_SESSION_DATA, reports))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("QUEUED")).andReturn();
        assertTrue(resent.getResponse().getContentAsString().contains("\"resentFromId\":" + alert.getId()));
    }
}
