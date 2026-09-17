package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.RECEIPT_PROFILE_ID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingConfirmationDAO;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.service.AnalyzerInstanceLocalStateService;
import org.openelisglobal.analyzer.service.AnalyzerInstanceState;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingConfirmationRequest;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingSourceRow;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingTestDraft;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingService;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingUpdate;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingView;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real controllers, local services, actor lookup, history, and PostgreSQL
 * writes. The narrow test filter chain supplies authentication and CSRF; these
 * checks prove controller role enforcement, not production login or filter
 * routing. The shared Bridge catalog fixture is the only substituted workflow
 * dependency.
 */
@Transactional
@ContextConfiguration(classes = AnalyzerMappingMutationSecurityIntegrationTest.TestConfig.class)
public class AnalyzerMappingMutationSecurityIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private AnalyzerTypeMappingService mappings;
    @Autowired
    private AnalyzerInstanceLocalStateService instances;
    @Autowired
    private AnalyzerService analyzers;
    @Autowired
    private TestService tests;
    @Autowired
    private TestSectionService labUnits;
    @Autowired
    private LocalizationService localizations;
    @Autowired
    private SystemUserService users;
    @Autowired
    private AnalyzerSiteBindingConfirmationDAO confirmations;
    @Autowired
    private HistoryService history;
    @PersistenceContext
    private EntityManager entityManager;

    private AnalyzerInstanceState analyzer;
    private String testId;
    private String actorLogin;

    @Before
    public void createOwnedMappingFixture() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        actorLogin = users.get(TEST_SYS_USER_ID).getLoginName();
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setName("Authorized mapping test");
        test.setDescription("Authorized mapping test");
        test.setGuid(UUID.randomUUID().toString());
        test.setIsActive("Y");
        test.setIsReportable("Y");
        test.setSysUserId(TEST_SYS_USER_ID);
        testId = tests.insert(test);
        Localization localization = new Localization();
        localization.setDescription("test section name");
        localization.setLocalizedValue("en", "Mapping permissions");
        localization.setSysUserId(TEST_SYS_USER_ID);
        localizations.insert(localization);
        TestSection labUnit = new TestSection();
        labUnit.setLocalization(localization);
        labUnit.setTestSectionName("Mapping permissions");
        labUnit.setDescription("Owned by mapping permission test");
        labUnit.setIsActive("Y");
        labUnit.setSysUserId(TEST_SYS_USER_ID);
        String labUnitId = labUnits.insert(labUnit);
        AnalyzerInstanceRequest request = new AnalyzerInstanceRequest();
        request.setName("Mapping permissions " + UUID.randomUUID());
        request.setProfileId(RECEIPT_PROFILE_ID);
        request.setProfileRevision(1);
        request.setTestUnitIds(List.of(labUnitId));
        analyzer = instances.create(request, TEST_SYS_USER_ID);
        reload();
    }

    @Test
    public void saveRejectsUnauthorizedRequestsWithoutWrites() throws Exception {
        var initial = mappings.getMapping(RECEIPT_PROFILE_ID, 1);
        String body = mapToJson(update(initial));
        assertRejectedWithoutWrites(
                () -> put(mappingPath()).param("revision", "1").contentType("application/json").content(body));
        assertEquals(initial.bindingFingerprint(), mappings.getMapping(RECEIPT_PROFILE_ID, 1).bindingFingerprint());
    }

    @Test
    public void confirmationRejectsUnauthorizedRequestsWithoutWrites() throws Exception {
        var saved = saveMapping();
        String body = mapToJson(confirmation(saved));
        assertRejectedWithoutWrites(() -> post(mappingPath() + "/confirm").param("revision", "1")
                .contentType("application/json").content(body));
        var revision = analyzers.get(analyzer.analyzerId()).getPinnedProfileBinding();
        assertEquals(saved.bindingFingerprint(), mappings.getMapping(revision.getProfileId(), 1).bindingFingerprint());
    }

    @Test
    public void adoptionRejectsUnauthorizedRequestsWithoutWrites() throws Exception {
        var saved = saveMapping();
        String previousRevision = analyzers.get(analyzer.analyzerId()).getSiteBindingRevision().getId();
        String body = selection(saved);
        assertRejectedWithoutWrites(() -> put(selectionPath()).contentType("application/json").content(body));
        assertEquals(previousRevision, analyzers.get(analyzer.analyzerId()).getSiteBindingRevision().getId());
    }

    @Test
    public void analyzerRoleCanSaveConfirmAndAdoptWithPersistedHistory() throws Exception {
        assertAuthorizedWorkflow("ANALYSER_IMPORT");
    }

    @Test
    public void administratorCanSaveConfirmAndAdoptWithPersistedHistory() throws Exception {
        assertAuthorizedWorkflow("ADMIN");
    }

    private void assertAuthorizedWorkflow(String role) throws Exception {
        var initial = mappings.getMapping(RECEIPT_PROFILE_ID, 1);
        mockMvc.perform(put(mappingPath()).param("revision", "1").with(user(actorLogin).roles(role)).with(csrf())
                .contentType("application/json").content(mapToJson(update(initial)))).andExpect(status().isOk());
        reload();
        var saved = mappings.getMapping(RECEIPT_PROFILE_ID, 1);
        assertTrue(saved.siteBindingRevision() > initial.siteBindingRevision());
        assertEquals(testId, saved.tests().get(0).selectedTest().id());
        mockMvc.perform(post(mappingPath() + "/confirm").param("revision", "1").with(user(actorLogin).roles(role))
                .with(csrf()).contentType("application/json").content(mapToJson(confirmation(saved))))
                .andExpect(status().isOk());
        mockMvc.perform(put(selectionPath()).with(user(actorLogin).roles(role)).with(csrf())
                .contentType("application/json").content(selection(saved))).andExpect(status().isOk());
        reload();
        var adopted = analyzers.get(analyzer.analyzerId()).getSiteBindingRevision();
        assertEquals(saved.siteBindingRevision(), adopted.getRevisionNumber());
        var confirmed = confirmations.findByRevisionId(adopted.getId()).orElseThrow();
        assertEquals(TEST_SYS_USER_ID, confirmed.getConfirmedBy());
        assertEquals(TEST_SYS_USER_ID, history.get(confirmed.getAuditEventId()).getSysUserId());
    }

    private void assertRejectedWithoutWrites(Supplier<MockHttpServletRequestBuilder> request) throws Exception {
        Map<String, Long> before = persistedCounts();
        mockMvc.perform(request.get().with(anonymous()).with(csrf())).andExpect(status().isUnauthorized());
        assertEquals(before, persistedCounts());
        mockMvc.perform(request.get().with(user(actorLogin).roles("RESULTS")).with(csrf()))
                .andExpect(status().isForbidden());
        assertEquals(before, persistedCounts());
        mockMvc.perform(request.get().with(user(actorLogin).roles("ANALYSER_IMPORT")))
                .andExpect(status().isForbidden());
        assertEquals("Even an allowed role needs a valid CSRF token", before, persistedCounts());
    }

    private Map<String, Long> persistedCounts() {
        reload();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String entity : List.of("Analyzer", "AnalyzerSiteBindingRevision", "AnalyzerSiteBindingTest",
                "AnalyzerSiteBindingResult", "AnalyzerSiteBindingConfirmation", "History")) {
            counts.put(entity, entityManager.createQuery("select count(row) from " + entity + " row", Long.class)
                    .getSingleResult());
        }
        return counts;
    }

    private AnalyzerTypeMappingView saveMapping() {
        return mappings.saveMapping(RECEIPT_PROFILE_ID, 1, update(mappings.getMapping(RECEIPT_PROFILE_ID, 1)),
                TEST_SYS_USER_ID);
    }

    private AnalyzerTypeMappingUpdate update(AnalyzerTypeMappingView initial) {
        return new AnalyzerTypeMappingUpdate(initial.bindingFingerprint(),
                List.of(new AnalyzerSiteBindingTestDraft("WBC", AnalyzerSiteBindingMappingState.BOUND, testId)),
                List.of());
    }

    private AnalyzerSiteBindingConfirmationRequest confirmation(AnalyzerTypeMappingView saved) {
        return new AnalyzerSiteBindingConfirmationRequest(saved.bindingFingerprint(),
                saved.controlRecognition().recognitionFingerprint(),
                List.of(new AnalyzerSiteBindingSourceRow("WBC", null)), List.of());
    }

    private String selection(AnalyzerTypeMappingView saved) throws Exception {
        return mapToJson(Map.of("siteBindingId", saved.siteBindingId(), "revision", saved.siteBindingRevision(),
                "bindingFingerprint", saved.bindingFingerprint()));
    }

    private String mappingPath() {
        return "/rest/analyzer-types/" + RECEIPT_PROFILE_ID + "/mapping";
    }

    private String selectionPath() {
        return "/rest/analyzer/analyzers/" + analyzer.analyzerId() + "/site-binding";
    }

    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }

    // Explicitly imported lite configuration; never component-scanned into other
    // suites.
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    public static class TestConfig {
        @Bean
        SecurityFilterChain analyzerMutationTestFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }
}
