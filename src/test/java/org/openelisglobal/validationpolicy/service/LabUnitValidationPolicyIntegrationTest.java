package org.openelisglobal.validationpolicy.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.validationpolicy.dao.ValidationPolicyDAO;
import org.openelisglobal.validationpolicy.form.LabUnitPoliciesView;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyForm;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyView;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.valueholder.ReviewTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1364 v1 slice 2: a lab unit can override the lab-wide default. Its active
 * override decides which of its results wait for review; a deactivated override
 * hands the unit back to the default.
 */
@Transactional
public class LabUnitValidationPolicyIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ValidationPolicyService validationPolicyService;
    @Autowired
    private SiteInformationService siteInformationService;

    private String waterQuality;
    private String hematology;
    private String retiredUnit;

    @Before
    public void labUnits() {
        jdbcTemplate.update("INSERT INTO clinlims.site_information (id, name, lastupdated, description, value,"
                + " encrypted, domain_id, value_type) SELECT nextval('clinlims.site_information_seq'),"
                + " 'validate all results', now(), 'all results should be validated even if normal', 'true', false,"
                + " (SELECT id FROM clinlims.site_information_domain WHERE name = 'resultConfiguration'), 'boolean'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.site_information WHERE name = 'validate all results')");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 1, null), "1");
        waterQuality = labUnit("QA1364 Water Quality", "ENVIRONMENTAL", "Y");
        hematology = labUnit("QA1364 Hematology", "CLINICAL", "Y");
        retiredUnit = labUnit("QA1364 Retired", "CLINICAL", "N");
    }

    @Test
    public void anOverrideIsListedWithItsLabUnitAndAppliesOnlyToThatUnit() {
        LabUnitPolicyView added = validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "NEEDS_REVIEW", 1, null), "1");

        assertEquals(waterQuality, added.testSectionId());
        assertEquals("QA1364 Water Quality", added.labUnitName());
        assertEquals("ENVIRONMENTAL", added.domain());
        assertEquals("NEEDS_REVIEW", added.trigger());
        assertEquals(1, added.levels());
        assertEquals("Validation", added.levelRoles().get(0).roleName());
        assertTrue(added.active());

        LabUnitPoliciesView policies = validationPolicyService.getLabUnitPolicies();
        assertEquals(List.of(added), policies.overrides());
        assertTrue("active units are offered", policies.labUnits().stream()
                .anyMatch(unit -> unit.id().equals(hematology) && unit.name().equals("QA1364 Hematology")));
        assertFalse("a retired unit is not offered",
                policies.labUnits().stream().anyMatch(unit -> unit.id().equals(retiredUnit)));

        withFlag("true", () -> {
            assertEquals(ReviewTrigger.NEEDS_REVIEW, validationPolicyService.entryReviewTrigger(waterQuality).get());
            assertEquals("a unit without an override follows the default", ReviewTrigger.ALL,
                    validationPolicyService.entryReviewTrigger(hematology).get());
            assertEquals("a result without a lab unit follows the default", ReviewTrigger.ALL,
                    validationPolicyService.entryReviewTrigger(null).get());
            return null;
        });
        withFlag("false", () -> {
            assertFalse("with the flag off nothing decides",
                    validationPolicyService.entryReviewTrigger(waterQuality).isPresent());
            return null;
        });
    }

    @Test
    public void anOverrideIsEditedDeactivatedAndReactivated() {
        Long id = validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "NEEDS_REVIEW", 1, null), "1").id();

        LabUnitPolicyView edited = validationPolicyService.saveLabUnitOverride(id,
                new ValidationPolicyForm("NONE", 0, null), "1");
        assertEquals("NONE", edited.trigger());
        assertTrue(edited.levelRoles().isEmpty());

        LabUnitPolicyView deactivated = validationPolicyService.setLabUnitOverrideActive(id, false, "1");
        assertFalse(deactivated.active());
        assertEquals("a deactivated override is still listed", 1,
                validationPolicyService.getLabUnitPolicies().overrides().size());
        withFlag("true", () -> {
            assertEquals("a deactivated override hands the unit back to the default", ReviewTrigger.ALL,
                    validationPolicyService.entryReviewTrigger(waterQuality).get());
            return null;
        });
        assertRefused("id", "overrideDeactivated",
                () -> validationPolicyService.saveLabUnitOverride(id, new ValidationPolicyForm("ALL", 1, null), "1"));

        validationPolicyService.setLabUnitOverrideActive(id, true, "1");
        withFlag("true", () -> {
            assertEquals("reactivated, it decides again with the rule it had", ReviewTrigger.NONE,
                    validationPolicyService.entryReviewTrigger(waterQuality).get());
            return null;
        });
    }

    @Test
    public void savingTheDefaultLeavesOverridesAloneAndOverridesLeaveValidateAllResultsAlone() {
        validationPolicyService.addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "NONE", 0, null), "1");
        assertEquals("an override does not write the lab-wide setting", "true", validateAllResults());

        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("NEEDS_REVIEW", 1, null), "1");

        assertEquals("NONE", validationPolicyService.getLabUnitPolicies().overrides().get(0).trigger());
        assertEquals("NEEDS_REVIEW", validationPolicyService.getLabWideDefault().trigger());
    }

    @Test
    public void anOverrideThatCannotBeSavedIsRefusedNamingTheField() {
        assertRefused("testSectionId", "labUnitRequired",
                () -> validationPolicyService.addLabUnitOverride(new LabUnitPolicyForm(null, "ALL", 1, null), "1"));
        assertRefused("testSectionId", "labUnitUnknown",
                () -> validationPolicyService.addLabUnitOverride(new LabUnitPolicyForm("987654", "ALL", 1, null), "1"));
        assertRefused("testSectionId", "labUnitUnknown",
                () -> validationPolicyService.addLabUnitOverride(new LabUnitPolicyForm("unit", "ALL", 1, null), "1"));
        assertRefused("testSectionId", "labUnitUnknown", () -> validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(retiredUnit, "ALL", 1, null), "1"));
        assertRefused("levels", "noReviewWithReviews", () -> validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "NONE", 1, null), "1"));
        assertRefused("levels", "levelsOutOfRange", () -> validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "ALL", 2, null), "1"));
        assertTrue("a refused add saves nothing", validationPolicyService.getLabUnitPolicies().overrides().isEmpty());

        Long id = validationPolicyService.addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "ALL", 1, null), "1")
                .id();
        assertRefused("levels", "zeroReviewsNeedsNoReview",
                () -> validationPolicyService.saveLabUnitOverride(id, new ValidationPolicyForm("ALL", 0, null), "1"));
        assertRefused("testSectionId", "labUnitHasOverride", () -> validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "NONE", 0, null), "1"));
        validationPolicyService.setLabUnitOverrideActive(id, false, "1");
        assertRefused("testSectionId", "labUnitHasOverride", () -> validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "NONE", 0, null), "1"));
        assertRefused("id", "overrideUnknown", () -> validationPolicyService.saveLabUnitOverride(id + 1000,
                new ValidationPolicyForm("ALL", 1, null), "1"));
        assertRefused("id", "overrideUnknown",
                () -> validationPolicyService.setLabUnitOverrideActive(id + 1000, true, "1"));
    }

    @Test
    public void anAddThatLosesARaceForItsUnitIsRefusedLikeAnyDuplicate() {
        jdbcTemplate.update(
                "INSERT INTO clinlims.validation_policy (id, test_section_id, review_trigger, review_levels,"
                        + " is_active) VALUES (nextval('clinlims.validation_policy_seq'), ?, 'ALL', 1, true)",
                Integer.valueOf(waterQuality));
        Object service = AopTestUtils.getTargetObject(validationPolicyService);
        ValidationPolicyDAO dao = (ValidationPolicyDAO) ReflectionTestUtils.getField(service, "validationPolicyDAO");
        ValidationPolicyDAO target = AopTestUtils.getTargetObject(dao);
        ValidationPolicyDAO missesTheConcurrentAdd = Mockito.spy(target);
        Mockito.doReturn(Optional.empty()).when(missesTheConcurrentAdd).findByTestSectionId(Mockito.any());
        ReflectionTestUtils.setField(service, "validationPolicyDAO", missesTheConcurrentAdd);
        try {
            assertRefused("testSectionId", "labUnitHasOverride", () -> validationPolicyService
                    .addLabUnitOverride(new LabUnitPolicyForm(waterQuality, "NONE", 0, null), "1"));
        } finally {
            ReflectionTestUtils.setField(service, "validationPolicyDAO", dao);
        }
    }

    private String labUnit(String name, String domain, String active) {
        Long localizationId = jdbcTemplate.queryForObject("SELECT nextval('clinlims.localization_seq')", Long.class);
        jdbcTemplate.update("INSERT INTO clinlims.localization (id, description) VALUES (?, ?)", localizationId, name);
        Long id = jdbcTemplate.queryForObject("SELECT nextval('clinlims.test_section_seq')", Long.class);
        jdbcTemplate.update("INSERT INTO clinlims.test_section (id, name, description, is_external, is_active,"
                + " lastupdated, sort_order, name_localization_id, domain) VALUES (?, ?, ?, 'N', ?, now(), 1, ?, ?)",
                id, name, name, active, localizationId, domain);
        return String.valueOf(id);
    }

    private void withFlag(String value, Supplier<Void> check) {
        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        String previous = configuration.getPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION);
        try {
            configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, value);
            check.get();
        } finally {
            configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, previous);
        }
    }

    private void assertRefused(String field, String code, Runnable save) {
        ValidationPolicyRefusedException refusal = assertThrows(ValidationPolicyRefusedException.class, save::run);
        assertEquals(field, refusal.getField());
        assertEquals(code, refusal.getCode());
    }

    private String validateAllResults() {
        return siteInformationService.getSiteInformationByName("validate all results").getValue();
    }
}
