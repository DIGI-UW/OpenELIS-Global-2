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
        assertEquals(1, policy.maxLevels());
    }

    @Test
    public void savingTheDefaultWritesValidateAllResultsToMatch() {
        ValidationPolicyView saved = validationPolicyService
                .saveLabWideDefault(new ValidationPolicyForm("NEEDS_REVIEW", 1, null), "1");

        assertEquals("NEEDS_REVIEW", saved.trigger());
        assertEquals(List.of(new ValidationPolicyView.Level(1, validationRoleId(), "Validation")), saved.levelRoles());
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
        assertRefused("levels", "levelsOutOfRange", new ValidationPolicyForm("ALL", 2, null));
        assertRefused("levels", "levelsOutOfRange", new ValidationPolicyForm("ALL", -1, null));
        assertRefused("levels", "levelsRequired", new ValidationPolicyForm("ALL", null, null));
        assertRefused("trigger", "triggerRequired", new ValidationPolicyForm("SOMETIMES", 1, null));
        assertRefused("trigger", "triggerRequired", new ValidationPolicyForm(null, 1, null));
        Integer reception = Integer.valueOf(roleService.getRoleByName("Reception").getId());
        assertRefused("roleIds", "roleNotAvailable", new ValidationPolicyForm("ALL", 1, List.of(reception)));
        assertEquals("a refused save changes nothing", "ALL", validationPolicyService.getLabWideDefault().trigger());
    }

    @Test
    public void thePolicyDecidesOnlyWhenTheFlagIsOn() {
        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        String previous = configuration.getPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION);
        try {
            configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "false");
            assertFalse(validationPolicyService.entryReviewTrigger().isPresent());
            assertFalse(validationPolicyService.getLabWideDefault().enabled());

            configuration.setPropertyValue(Property.USE_MULTI_LEVEL_VALIDATION, "true");
            validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("NONE", 0, null), "1");
            assertEquals(ReviewTrigger.NONE, validationPolicyService.entryReviewTrigger().get());
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
