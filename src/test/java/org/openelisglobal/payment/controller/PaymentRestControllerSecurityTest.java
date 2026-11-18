package org.openelisglobal.payment.controller;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.payment.PaymentTaxSettings;
import org.openelisglobal.payment.controller.rest.PaymentRestController;
import org.openelisglobal.payment.service.OrderPricingService;
import org.openelisglobal.payment.service.PaymentLinkService;
import org.openelisglobal.payment.service.PaymentRefundService;
import org.openelisglobal.payment.service.PaymentService;
import org.openelisglobal.payment.service.PaymentWebhookService;
import org.openelisglobal.payment.service.PriceQuote;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Who may call the payment endpoints. Viewing an order's payment and creating
 * its link: Reception (who enter and edit orders) or Admin. Refund: Admin only.
 * The Razorpay webhook has no role check (it is signature-checked instead).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { PaymentRestControllerSecurityTest.TestConfig.class })
public class PaymentRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String SUMMARY = "/rest/payments/sample/1/summary";
    private static final String PAYMENTS = "/rest/payments/sample/1";
    private static final String LINK = "/rest/payments/sample/1/link";
    private static final String REFUND = "/rest/payments/p1/refund";
    private static final String WEBHOOK = "/rest/payments/razorpay/webhook";

    /**
     * A logged-in user with one role, plus the OpenELIS session user the controller
     * reads its user id from.
     */
    private static RequestPostProcessor as(String role) {
        return request -> {
            UserSessionData userSessionData = new UserSessionData();
            userSessionData.setSytemUserId(1);
            request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
            return user(role.toLowerCase()).roles(role).postProcessRequest(request);
        };
    }

    /** Past the role check: whatever the endpoint answers, it is not 401 or 403. */
    private static ResultMatcher passedTheRoleCheck() {
        return result -> {
            assertNotEquals("unexpected 401", 401, result.getResponse().getStatus());
            assertNotEquals("unexpected 403", 403, result.getResponse().getStatus());
        };
    }

    // --- read: summary and payment list ---

    @Test
    public void summary_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get(SUMMARY)).andExpect(status().isUnauthorized());
    }

    @Test
    public void summary_ResultsRole_Returns403() throws Exception {
        mockMvc.perform(get(SUMMARY).with(as("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void summary_ReceptionRole_Returns200() throws Exception {
        mockMvc.perform(get(SUMMARY).with(as("RECEPTION"))).andExpect(status().isOk());
    }

    @Test
    public void summary_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get(SUMMARY).with(as("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void paymentList_ValidationRole_Returns403() throws Exception {
        mockMvc.perform(get(PAYMENTS).with(as("VALIDATION"))).andExpect(status().isForbidden());
    }

    @Test
    public void paymentList_ReceptionRole_Returns200() throws Exception {
        mockMvc.perform(get(PAYMENTS).with(as("RECEPTION"))).andExpect(status().isOk());
    }

    // --- create link ---

    @Test
    public void createLink_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(post(LINK)).andExpect(status().isUnauthorized());
    }

    @Test
    public void createLink_ResultsRole_Returns403() throws Exception {
        mockMvc.perform(post(LINK).with(as("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void createLink_ReceptionRole_PassesTheRoleCheck() throws Exception {
        mockMvc.perform(post(LINK).with(as("RECEPTION"))).andExpect(passedTheRoleCheck());
    }

    @Test
    public void createLink_AdminRole_PassesTheRoleCheck() throws Exception {
        mockMvc.perform(post(LINK).with(as("ADMIN"))).andExpect(passedTheRoleCheck());
    }

    // --- refund ---

    @Test
    public void refund_ReceptionRole_Returns403() throws Exception {
        mockMvc.perform(post(REFUND).with(as("RECEPTION"))).andExpect(status().isForbidden());
    }

    @Test
    public void refund_AdminRole_PassesTheRoleCheck() throws Exception {
        mockMvc.perform(post(REFUND).with(as("ADMIN"))).andExpect(passedTheRoleCheck());
    }

    // --- webhook ---

    @Test
    public void webhook_HasNoRoleCheck() throws Exception {
        // Razorpay calls it without any OpenELIS role; SecurityConfig leaves the URL
        // open and the controller checks the signature.
        mockMvc.perform(post(WEBHOOK).with(as("NONE")).content("{}")).andExpect(passedTheRoleCheck());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        PaymentService paymentService() {
            PaymentService service = mock(PaymentService.class);
            when(service.getPaymentsForSample(anyString())).thenReturn(List.of());
            return service;
        }

        @Bean
        OrderPricingService orderPricingService() {
            OrderPricingService service = mock(OrderPricingService.class);
            when(service.quoteSample(anyString())).thenReturn(PriceQuote.of(List.of()));
            return service;
        }

        @Bean
        PaymentLinkService paymentLinkService() {
            Payment pending = new Payment();
            pending.setId("p1");
            pending.setAmountPaise(11800L);
            pending.setStatus(PaymentStatus.PENDING);
            PaymentLinkService service = mock(PaymentLinkService.class);
            when(service.requestLink(anyString(), anyString())).thenReturn(pending);
            return service;
        }

        @Bean
        PaymentWebhookService paymentWebhookService() {
            return mock(PaymentWebhookService.class);
        }

        @Bean
        PaymentRefundService paymentRefundService() {
            return mock(PaymentRefundService.class);
        }

        /** Spring autowires this into the DaemonContextExecutor mock's fields. */
        @Bean(name = "daemonSysUserId")
        String daemonSysUserId() {
            return "1";
        }

        @Bean
        DaemonContextExecutor daemonContextExecutor() {
            return mock(DaemonContextExecutor.class);
        }

        @Bean
        PaymentTaxSettings paymentTaxSettings() {
            return new PaymentTaxSettings(18);
        }

        @Bean
        PaymentRestController paymentRestController() {
            return new PaymentRestController();
        }
    }
}
