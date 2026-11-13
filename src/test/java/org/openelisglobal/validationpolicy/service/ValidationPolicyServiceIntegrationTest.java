package org.openelisglobal.validationpolicy.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.openelisglobal.validationpolicy.valueholder.ReviewTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1364 v1: the lab-wide default validation policy that replaces "validate
 * all results", capped at one review signed by the Validation role.
 */
@Transactional
public class ValidationPolicyServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ValidationPolicyService validationPolicyService;
    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private org.openelisglobal.systemuser.service.UserService userService;
    @Autowired
    private org.openelisglobal.systemuser.service.SystemUserService systemUserService;

    /**
     * A fixture loaded by an earlier test class can replace site_information; the
     * setting this policy writes back must exist, as it does on every install.
     */
    @Before
    public void ensureValidateAllResults() {
        jdbcTemplate.update("INSERT INTO clinlims.site_information (id, name, lastupdated, description, value,"
                + " encrypted, domain_id, value_type) SELECT nextval('clinlims.site_information_seq'),"
                + " 'validate all results', now(), 'all results should be validated even if normal', 'true', false,"
                + " (SELECT id FROM clinlims.site_information_domain WHERE name = 'resultConfiguration'), 'boolean'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.site_information WHERE name = 'validate all results')");
    }

    @Test
    public void theDefaultCarriesOverValidateAllResults() {
        ValidationPolicyView policy = validationPolicyService.getLabWideDefault();

        assertEquals("validate all results is true in this database, so All results", "ALL", policy.trigger());
        assertEquals(1, policy.levels());
        assertEquals("Validation", policy.levelRoles().get(0).roleName());
        assertEquals(5, policy.maxLevels());
    }

    @Test
    public void savingTheDefaultWritesValidateAllResultsToMatch() {
        ValidationPolicyView saved = validationPolicyService
                .saveLabWideDefault(new ValidationPolicyForm("NEEDS_REVIEW", 1, null), "1");

        assertEquals("NEEDS_REVIEW", saved.trigger());
        assertEquals(1, saved.levelRoles().size());
        assertEquals(1, saved.levelRoles().get(0).level());
        assertEquals(validationRoleId(), saved.levelRoles().get(0).roleId());
        assertEquals("Validation", saved.levelRoles().get(0).roleName());
        assertEquals("false", validateAllResults());

        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 1, List.of(validationRoleId())),
                "1");
        assertEquals("true", validateAllResults());

        ValidationPolicyView none = validationPolicyService
                .saveLabWideDefault(new ValidationPolicyForm("NONE", 0, null), "1");
        assertEquals("NONE", none.trigger());
        assertTrue(none.levelRoles().isEmpty());
        assertEquals("false", validateAllResults());
        assertEquals("NONE", validationPolicyService.getLabWideDefault().trigger());
    }

    @Test
    public void aPolicyThatContradictsItselfIsRefusedNamingTheField() {
        assertRefused("levels", "noReviewWithReviews", new ValidationPolicyForm("NONE", 1, null));
        assertRefused("levels", "zeroReviewsNeedsNoReview", new ValidationPolicyForm("ALL", 0, null));
        assertRefused("levels", "levelsOutOfRange", new ValidationPolicyForm("ALL", 6, null));
        assertRefused("levels", "levelsOutOfRange", new ValidationPolicyForm("ALL", -1, null));
        assertRefused("levels", "levelsRequired", new ValidationPolicyForm("ALL", null, null));
        assertRefused("trigger", "triggerRequired", new ValidationPolicyForm("SOMETIMES", 1, null));
        assertRefused("trigger", "triggerRequired", new ValidationPolicyForm(null, 1, null));
        Integer labUnitRoles = Integer.valueOf(roleService.getRoleByName("Lab Unit Roles").getId());
        assertRefused("roleIds", "roleNotAvailable", new ValidationPolicyForm("ALL", 1, List.of(labUnitRoles)));
        assertRefused("roleIds", "roleNotAvailable", new ValidationPolicyForm("ALL", 1, List.of(987654)));
        assertEquals("a refused save changes nothing", "ALL", validationPolicyService.getLabWideDefault().trigger());
    }

    @Test
    public void upToFiveLevelsEachSignedByARoleWithItsSignerCount() {
        jdbcTemplate.update("INSERT INTO clinlims.system_role (id, name, description, is_grouping_role, active,"
                + " editable) VALUES (9410, 'QA1364 Roles', 'grouping', true, true, false)");
        jdbcTemplate.update("INSERT INTO clinlims.system_role (id, name, description, is_grouping_role,"
                + " grouping_parent, active, editable) VALUES (9411, 'QA1364 Pathologist', 'signs reports', false,"
                + " 9410, true, false)");
        Integer pathologist = 9411;
        ValidationPolicyView saved = validationPolicyService
                .saveLabWideDefault(new ValidationPolicyForm("ALL", 5, List.of(validationRoleId(), pathologist)), "1");

        assertEquals(5, saved.levels());
        assertEquals(List.of("Validation", "QA1364 Pathologist", "Validation", "Validation", "Validation"),
                saved.levelRoles().stream().map(ValidationPolicyView.Level::roleName).toList());
        userService.saveUserLabUnitRoles(systemUserService.get("1"),
                java.util.Map.of("AllLabUnits", java.util.Set.of(validationRoleId().toString())), "1");
        jdbcTemplate.update("DELETE FROM clinlims.system_user_role WHERE system_user_id = 1 AND role_id = ?",
                pathologist);
        int signersBefore = validationPolicyService.getLabWideDefault().levelRoles().get(1).signers();
        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id) VALUES (1, ?)",
                pathologist);
        int signersAfter = validationPolicyService.getLabWideDefault().levelRoles().get(1).signers();
        assertEquals("a validator granted Pathologist can sign the Pathologist level", signersBefore + 1, signersAfter);

        List<String> offered = validationPolicyService.levelRoleOptions(null).stream()
                .map(org.openelisglobal.validationpolicy.form.LevelRoleOption::name).toList();
        assertEquals("Validation comes first", "Validation", offered.get(0));
        assertTrue(offered.contains("QA1364 Pathologist"));
        assertFalse("a grouping is not a role", offered.contains("QA1364 Roles"));
    }

    @Test
    public void thePolicyDecidesOnlyWhenTheFlagIsOn() {
        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        String previous = configuration.getPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION);
        try {
            configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "false");
            assertFalse(validationPolicyService.entryReviewTrigger(null).isPresent());
            assertFalse(validationPolicyService.getLabWideDefault().enabled());

            configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "true");
            validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("NONE", 0, null), "1");
            assertEquals(ReviewTrigger.NONE, validationPolicyService.entryReviewTrigger(null).get());
            assertTrue(validationPolicyService.getLabWideDefault().enabled());
        } finally {
            configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, previous);
        }
    }

    private void assertRefused(String field, String code, ValidationPolicyForm form) {
        ValidationPolicyRefusedException refusal = assertThrows(ValidationPolicyRefusedException.class,
                () -> validationPolicyService.saveLabWideDefault(form, "1"));
        assertEquals(field, refusal.getField());
        assertEquals(code, refusal.getCode());
    }

    private Integer validationRoleId() {
        return Integer.valueOf(roleService.getRoleByName("Validation").getId());
    }

    private String validateAllResults() {
        return siteInformationService.getSiteInformationByName("validate all results").getValue();
    }
}
