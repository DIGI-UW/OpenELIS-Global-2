package org.openelisglobal.security;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.addon.ai.AiContextService;
import org.openelisglobal.addon.controller.rest.AddonApiRestController;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.service.AddonDataService;
import org.openelisglobal.addon.service.AddonRegistryService;
import org.openelisglobal.addon.service.AddonSettings;
import org.openelisglobal.addon.service.AddonTokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The add-on API chain as SecurityConfig builds it: bearer credentials only, no
 * session, no route through the reverse proxy, scope and order checks per read.
 */
@WebAppConfiguration
@ContextConfiguration(classes = AddonApiSecurityTest.TestConfig.class)
public class AddonApiSecurityTest extends SecuritySliceMockMvcTest {

    private static final String API = "/rest/addon-api/v1";
    private static final String USER_TOKEN = "oeat.user";
    private static final String SERVICE_KEY = "oeak_service";
    private static final String AI_TOKEN = "oeat.ai";
    private static final String AI_SERVICE_KEY = "oeak_ai";

    private static RequestPostProcessor bearer(String credential) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + credential);
            return request;
        };
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    @Test
    public void aUserTokenReadsAnOrderOnItsScreen() throws Exception {
        var result = mockMvc.perform(get(API + "/orders/DEV0001").with(bearer(USER_TOKEN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessionNumber").value("DEV0001")).andReturn();
        assertNull("the add-on API must stay stateless", result.getRequest().getSession(false));
    }

    @Test
    public void anOrderOutsideTheTokenIsRefused() throws Exception {
        mockMvc.perform(get(API + "/orders/DEV0002").with(bearer(USER_TOKEN))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("context"));
    }

    @Test
    public void aReadWithoutItsScopeIsRefused() throws Exception {
        mockMvc.perform(get(API + "/orders/DEV0001/patient").with(bearer(USER_TOKEN))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("scope"));
    }

    @Test
    public void whoamiTellsTheKindOfCredential() throws Exception {
        mockMvc.perform(get(API + "/whoami").with(bearer(USER_TOKEN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("user")).andExpect(jsonPath("$.addon").value("hello"));
        mockMvc.perform(get(API + "/whoami").with(bearer(SERVICE_KEY))).andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("service"));
    }

    @Test
    public void noOrAnUnknownCredentialIsUnauthorized() throws Exception {
        mockMvc.perform(get(API + "/whoami")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(API + "/whoami").with(bearer("oeat.forged"))).andExpect(status().isUnauthorized());
    }

    @Test
    public void anAdministratorSessionIsNotAnAddonIdentity() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_GLOBAL_ADMIN"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        mockMvc.perform(get(API + "/orders/DEV0001").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    public void theApiIsNotReachableThroughTheReverseProxyOrFromOutside() throws Exception {
        mockMvc.perform(get(API + "/whoami").with(bearer(USER_TOKEN)).header("X-Forwarded-For", "198.51.100.7"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("network"));
        mockMvc.perform(get(API + "/whoami").with(bearer(USER_TOKEN)).with(from("203.0.113.9")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(API + "/whoami").with(bearer(USER_TOKEN)).with(from("172.20.1.140")))
                .andExpect(status().isOk());
    }

    @Test
    public void theAiContextNeedsItsScopeAndAScreen() throws Exception {
        mockMvc.perform(get(API + "/ai/context").with(bearer(USER_TOKEN))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("scope"));
        mockMvc.perform(get(API + "/ai/context").with(bearer(AI_SERVICE_KEY))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("context"));
        mockMvc.perform(get(API + "/ai/context?view=patient").with(bearer(AI_TOKEN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.view").value("patient"));
    }

    @Test
    public void textIsDeidentifiedForTheAiScopeOnly() throws Exception {
        mockMvc.perform(post(API + "/ai/deidentify").with(bearer(AI_TOKEN)).contentType("application/json")
                .content("{\"text\":\"is Jane Doe G3a?\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("is [removed] G3a?")).andExpect(jsonPath("$.changed").value(true));
        mockMvc.perform(post(API + "/ai/deidentify").with(bearer(USER_TOKEN)).contentType("application/json")
                .content("{\"text\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(post(API + "/ai/deidentify").with(bearer(AI_TOKEN)).contentType("application/json")
                .content("{\"text\":3}")).andExpect(status().isBadRequest());
    }

    @Test
    public void anAiTokenDoesNotRevealItsLabNumbers() throws Exception {
        mockMvc.perform(get(API + "/whoami").with(bearer(AI_TOKEN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessionNumbers").doesNotExist());
        mockMvc.perform(get(API + "/orders/DEV0001").with(bearer(AI_TOKEN))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("scope"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        AddonTokenService addonTokenService() {
            return new AddonTokenService() {
                @Override
                public IssuedToken issue(String addonId, String slot, Map<String, ?> context, String userId,
                        Collection<String> userRoles) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public AddonToken.Claims authenticate(String bearer) {
                    if (USER_TOKEN.equals(bearer)) {
                        return new AddonToken.Claims("hello", "7", List.of("orders.read", "results.read"),
                                List.of("DEV0001"), Long.MAX_VALUE);
                    }
                    if (SERVICE_KEY.equals(bearer)) {
                        return new AddonToken.Claims("hello", null, List.of("orders.read"), null, Long.MAX_VALUE);
                    }
                    if (AI_TOKEN.equals(bearer)) {
                        return new AddonToken.Claims("assistant", "7", List.of("ai.context"), List.of("DEV0001"),
                                Long.MAX_VALUE);
                    }
                    if (AI_SERVICE_KEY.equals(bearer)) {
                        return new AddonToken.Claims("assistant", null, List.of("ai.context"), null, Long.MAX_VALUE);
                    }
                    throw new IllegalArgumentException("token");
                }

                @Override
                public String opaqueId(String addonId, String value) {
                    return "x";
                }
            };
        }

        @Bean
        AiContextService aiContextService() {
            AiContextService ai = mock(AiContextService.class);
            when(ai.context(any(), anyString())).thenAnswer(i -> Map.of("view", i.getArgument(1)));
            when(ai.deidentify(any(), anyString()))
                    .thenAnswer(i -> ((String) i.getArgument(1)).replace("Jane Doe", "[removed]"));
            return ai;
        }

        @Bean
        AddonSettings addonSettings() {
            // @Value would overwrite a field set here, so the method is overridden
            return new AddonSettings() {
                @Override
                public List<String> allowedNetworks() {
                    return List.of("172.16.0.0/12", "127.0.0.0/8");
                }
            };
        }

        @Bean
        AddonDataService addonDataService() {
            AddonDataService data = mock(AddonDataService.class);
            when(data.mayRead(any(), anyString()))
                    .thenAnswer(i -> ((AddonToken.Claims) i.getArgument(0)).accessions() == null
                            || ((AddonToken.Claims) i.getArgument(0)).accessions().contains(i.getArgument(1)));
            when(data.order(anyString())).thenAnswer(i -> Optional.of(Map.of("accessionNumber", i.getArgument(0))));
            return data;
        }

        @Bean
        AddonRegistryService addonRegistryService() {
            return mock(AddonRegistryService.class);
        }

        @Bean
        AddonApiRestController addonApiRestController() {
            return new AddonApiRestController();
        }

        @Bean
        @Order(1)
        SecurityFilterChain addonApiSecurityFilterChain(HttpSecurity http, ObjectProvider<AddonTokenService> tokens,
                ObjectProvider<AddonSettings> settings) throws Exception {
            SecurityConfig.configureAddonApi(http, tokens, settings);
            return http.build();
        }

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        SecurityFilterChain browserSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }
    }
}
