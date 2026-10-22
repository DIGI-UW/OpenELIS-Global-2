package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Test;
import org.openelisglobal.addon.ai.AiContextService;
import org.openelisglobal.addon.controller.rest.AddonApiRestController;
import org.openelisglobal.addon.dao.AddonAccessLogDAO;
import org.openelisglobal.addon.security.AddonAccessLogInterceptor;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.service.AddonAccessLogService;
import org.openelisglobal.addon.service.AddonDataService;
import org.openelisglobal.addon.service.AddonQueryService;
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
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
    private static final String MCP_KEY = "oeak_mcp";
    private static final String MCP_KEY_NO_IDENTITY = "oeak_mcp_noid";
    private static final String SETTINGS_TOKEN = "oeat.settings";

    static final List<AddonAccessLogDAO.Entry> LOGGED = new CopyOnWriteArrayList<>();

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

    @Test
    public void whoamiNamesTheUserAndTheSlotOfAUiToken() throws Exception {
        mockMvc.perform(get(API + "/whoami").with(bearer(SETTINGS_TOKEN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("user")).andExpect(jsonPath("$.userId").value("1"))
                .andExpect(jsonPath("$.slot").value("addon-settings-page-slot"));
        mockMvc.perform(get(API + "/whoami").with(bearer(MCP_KEY))).andExpect(status().isOk())
                .andExpect(jsonPath("$.slot").doesNotExist()).andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    public void listingOrdersNeedsOrdersSearch() throws Exception {
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-10-02").with(bearer(SERVICE_KEY)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.scope").value("orders.search"));
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-10-02").with(bearer(MCP_KEY)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders[0].accessionNumber").value("DEV0009"));
    }

    @Test
    public void searchesAreBounded() throws Exception {
        // 31 days at most, both ends, in order
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-11-01").with(bearer(MCP_KEY)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("range"));
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-10-31").with(bearer(MCP_KEY)))
                .andExpect(status().isOk());
        mockMvc.perform(get(API + "/orders?from=2026-10-05&to=2026-10-01").with(bearer(MCP_KEY)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(API + "/orders?to=2026-10-01").with(bearer(MCP_KEY))).andExpect(status().isBadRequest());
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-10-02&limit=101").with(bearer(MCP_KEY)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.param").value("limit"));
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-10-02&status=all").with(bearer(MCP_KEY)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.param").value("status"));
        mockMvc.perform(get(API + "/worklist?stage=entry").with(bearer(MCP_KEY))).andExpect(status().isBadRequest());
        mockMvc.perform(get(API + "/stats/tat?from=2026-10-01&to=2026-10-02&groupBy=patient").with(bearer(MCP_KEY)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.param").value("groupBy"));
        mockMvc.perform(get(API + "/patients?q=a").with(bearer(MCP_KEY))).andExpect(status().isBadRequest());
    }

    @Test
    public void patientsNeedTheIdentityScope() throws Exception {
        mockMvc.perform(get(API + "/patients?q=Nguyen").with(bearer(MCP_KEY_NO_IDENTITY)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.scope").value("patient.identity"));
        mockMvc.perform(get(API + "/patients/12").with(bearer(MCP_KEY_NO_IDENTITY))).andExpect(status().isForbidden());
        mockMvc.perform(get(API + "/patients/12/orders").with(bearer(MCP_KEY_NO_IDENTITY)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(API + "/patients?q=Nguyen").with(bearer(MCP_KEY))).andExpect(status().isOk())
                .andExpect(jsonPath("$.patients[0].fullName").value("Nguyen An"));
    }

    @Test
    public void anOrderNamesItsPatientOnlyWithTheIdentityScope() throws Exception {
        mockMvc.perform(get(API + "/orders/DEV0009").with(bearer(MCP_KEY))).andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.fullName").value("Nguyen An"));
        mockMvc.perform(get(API + "/orders/DEV0009").with(bearer(MCP_KEY_NO_IDENTITY))).andExpect(status().isOk())
                .andExpect(jsonPath("$.patient").doesNotExist());
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-10-02").with(bearer(MCP_KEY_NO_IDENTITY)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders[0].patient").doesNotExist());
    }

    @Test
    public void anAddonMayNarrowButNeverWidenItsScopesPerRequest() throws Exception {
        // an agent without the identifiers switch: the add-on drops patient.identity
        String narrow = "orders.read,results.read,orders.search";
        mockMvc.perform(get(API + "/orders/DEV0009").with(bearer(MCP_KEY)).header("X-Addon-Scopes", narrow))
                .andExpect(status().isOk()).andExpect(jsonPath("$.patient").doesNotExist());
        mockMvc.perform(get(API + "/patients?q=Nguyen").with(bearer(MCP_KEY)).header("X-Addon-Scopes", narrow))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.scope").value("patient.identity"));
        mockMvc.perform(get(API + "/orders?from=2026-10-01&to=2026-10-02").with(bearer(MCP_KEY))
                .header("X-Addon-Scopes", narrow)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].patient").doesNotExist());
        // a scope the add-on was not granted stays out of reach
        mockMvc.perform(get(API + "/stats/tat?from=2026-10-01&to=2026-10-02").with(bearer(MCP_KEY_NO_IDENTITY))
                .header("X-Addon-Scopes", "orders.read,stats.read")).andExpect(status().isForbidden());
        mockMvc.perform(get(API + "/whoami").with(bearer(MCP_KEY)).header("X-Addon-Scopes", "catalog.read"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scopes.length()").value(1));
    }

    @Test
    public void statisticsAndTheCatalogNeedTheirScopes() throws Exception {
        mockMvc.perform(get(API + "/stats/orders?from=2026-10-01&to=2026-10-02").with(bearer(SERVICE_KEY)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.scope").value("stats.read"));
        mockMvc.perform(get(API + "/stats/tat?from=2026-10-01&to=2026-10-02").with(bearer(MCP_KEY)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.groupBy").value("section"));
        mockMvc.perform(get(API + "/catalog/tests").with(bearer(SERVICE_KEY))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.scope").value("catalog.read"));
        mockMvc.perform(get(API + "/catalog/tests?active=any").with(bearer(MCP_KEY))).andExpect(status().isOk());
        mockMvc.perform(get(API + "/catalog/tests?active=maybe").with(bearer(MCP_KEY)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void everyAuthenticatedRequestIsLogged() throws Exception {
        LOGGED.clear();
        mockMvc.perform(get(API + "/orders/DEV0009/results").with(bearer(MCP_KEY))
                .header("X-Addon-Actor", "agent:a7f3 Claude Code \u2014 Dr Lan\n")
                .header("X-Addon-Purpose", "get_order")).andExpect(status().isOk());
        mockMvc.perform(get(API + "/patients?q=Nguyen").with(bearer(MCP_KEY_NO_IDENTITY)).header("X-Addon-Purpose",
                "find patients; drop table")).andExpect(status().isForbidden());
        mockMvc.perform(get(API + "/whoami")).andExpect(status().isUnauthorized());
        assertEquals(LOGGED.toString(), 2, LOGGED.size());
        AddonAccessLogDAO.Entry read = LOGGED.get(0);
        assertEquals("mcp", read.addonId());
        assertEquals("service", read.credential());
        assertEquals("/orders/{accession}/results", read.endpoint());
        assertEquals("order", read.subjectType());
        assertEquals("DEV0009", read.subject());
        assertEquals(200, read.status());
        assertEquals("get_order", read.purpose());
        // the firewall refuses a header with a line break or non-ASCII text; the read
        // is logged anyway
        assertEquals("(header refused)", read.actor());
        assertEquals(Integer.valueOf(1), read.rows());
        AddonAccessLogDAO.Entry refused = LOGGED.get(1);
        assertEquals(403, refused.status());
        assertEquals("/patients", refused.endpoint());
        assertNull("a purpose that is not a plain name is dropped", refused.purpose());
        assertEquals("query", refused.subjectType());
        assertEquals("patients Nguyen", refused.subject());
        mockMvc.perform(
                get(API + "/orders/DEV0009").with(bearer(MCP_KEY)).header("X-Addon-Actor", "agent:a7f3 Claude Code"))
                .andExpect(status().isOk());
        assertEquals("agent:a7f3 Claude Code", LOGGED.get(2).actor());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig implements WebMvcConfigurer {

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(addonAccessLogInterceptor()).addPathPatterns(SecurityConfig.ADDON_API_PATHS);
        }

        @Bean
        AddonAccessLogInterceptor addonAccessLogInterceptor() {
            return new AddonAccessLogInterceptor();
        }

        @Bean
        AddonAccessLogService addonAccessLogService() {
            AddonAccessLogService log = mock(AddonAccessLogService.class);
            doAnswer(i -> LOGGED.add(i.getArgument(0))).when(log).record(any());
            return log;
        }

        @Bean
        AddonQueryService addonQueryService() {
            Map<String, Object> patient = Map.of("patientId", "12", "fullName", "Nguyen An");
            AddonQueryService q = mock(AddonQueryService.class);
            when(q.orders(any(), any(), anyString(), any(), anyInt(), anyInt(), anyBoolean())).thenAnswer(i -> {
                Map<String, Object> order = new java.util.LinkedHashMap<>();
                order.put("accessionNumber", "DEV0009");
                if (Boolean.TRUE.equals(i.getArgument(6))) {
                    order.put("patient", patient);
                }
                return Map.of("orders", List.of(order));
            });
            when(q.findPatients(anyString())).thenReturn(Map.of("patients", List.of(patient)));
            when(q.patientOfOrder(anyString())).thenReturn(Optional.of(patient));
            when(q.turnaround(any(), any(), anyString()))
                    .thenAnswer(i -> Map.of("groupBy", (Object) i.getArgument(2), "rows", List.of()));
            when(q.orderCounts(any(), any(), anyString())).thenReturn(Map.of("rows", List.of()));
            when(q.tests(any(), any())).thenReturn(Map.of("tests", List.of()));
            return q;
        }

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
                    if (MCP_KEY.equals(bearer)) {
                        return new AddonToken.Claims("mcp", null, List.of("orders.read", "results.read",
                                "orders.search", "stats.read", "catalog.read", "patient.identity"), null,
                                Long.MAX_VALUE);
                    }
                    if (MCP_KEY_NO_IDENTITY.equals(bearer)) {
                        return new AddonToken.Claims("mcp", null,
                                List.of("orders.read", "results.read", "orders.search"), null, Long.MAX_VALUE);
                    }
                    if (SETTINGS_TOKEN.equals(bearer)) {
                        return new AddonToken.Claims("mcp", "1", List.of("orders.read"), List.of(), Long.MAX_VALUE,
                                "addon-settings-page-slot");
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
            when(data.results(anyString())).thenAnswer(i -> Optional.of(Map.of("accessionNumber",
                    (Object) i.getArgument(0), "results", List.of(Map.of("testCode", "GLU")))));
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
