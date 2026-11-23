package org.openelisglobal.validationpolicy.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyForm;
import org.openelisglobal.validationpolicy.form.LabUnitPolicyView;
import org.openelisglobal.validationpolicy.form.ValidationPolicyForm;
import org.openelisglobal.validationpolicy.form.ValidationPolicyView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1017: a policy save from a page that loaded an older version of the
 * policy is refused and writes nothing, and every policy change leaves an audit
 * row naming who made it.
 */
@Transactional
public class ValidationPolicyStaleSaveIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ValidationPolicyService validationPolicyService;

    private String hematology;

    @Before
    public void policyAndLabUnit() {
        jdbcTemplate.update("INSERT INTO clinlims.site_information (id, name, lastupdated, description, value,"
                + " encrypted, domain_id, value_type) SELECT nextval('clinlims.site_information_seq'),"
                + " 'validate all results', now(), 'all results should be validated even if normal', 'true', false,"
                + " (SELECT id FROM clinlims.site_information_domain WHERE name = 'resultConfiguration'), 'boolean'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.site_information WHERE name = 'validate all results')");
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("ALL", 1, null), "1");
        Long localizationId = jdbcTemplate.queryForObject("SELECT nextval('clinlims.localization_seq')", Long.class);
        jdbcTemplate.update("INSERT INTO clinlims.localization (id, description) VALUES (?, ?)", localizationId,
                "QA1017 Hematology");
        Long id = jdbcTemplate.queryForObject("SELECT nextval('clinlims.test_section_seq')", Long.class);
        jdbcTemplate.update("INSERT INTO clinlims.test_section (id, name, description, is_external, is_active,"
                + " lastupdated, sort_order, name_localization_id, domain) VALUES (?, ?, ?, 'N', 'Y', now(), 1, ?,"
                + " 'CLINICAL')", id, "QA1017 Hematology", "QA1017 Hematology", localizationId);
        hematology = String.valueOf(id);
    }

    @Test
    public void aDefaultSavedFromAnOutdatedPageIsRefusedAndTheNewerPolicyStays() throws Exception {
        ValidationPolicyView opened = validationPolicyService.getLabWideDefault();
        assertNotNull("the page gets the version it loaded", opened.lastupdated());
        nextMillisecond();
        ValidationPolicyView theirs = validationPolicyService
                .saveLabWideDefault(new ValidationPolicyForm("NEEDS_REVIEW", 1, null, opened.lastupdated()), "1");
        assertNotEquals(opened.lastupdated(), theirs.lastupdated());

        ValidationPolicyStaleException refused = assertThrows(ValidationPolicyStaleException.class,
                () -> validationPolicyService
                        .saveLabWideDefault(new ValidationPolicyForm("NONE", 0, null, opened.lastupdated()), "1"));

        assertEquals(theirs.lastupdated(), String.valueOf(refused.getCurrent().getTime()));
        assertEquals("NEEDS_REVIEW", validationPolicyService.getLabWideDefault().trigger());
    }

    @Test
    public void aSaveWithoutAVersionIsNotChecked() throws Exception {
        nextMillisecond();
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("NEEDS_REVIEW", 1, null), "1");

        ValidationPolicyView saved = validationPolicyService
                .saveLabWideDefault(new ValidationPolicyForm("NONE", 0, null), "1");

        assertEquals("NONE", saved.trigger());
    }

    @Test
    public void anOverrideSavedOrToggledFromAnOutdatedPageIsRefused() throws Exception {
        LabUnitPolicyView added = validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(hematology, "NEEDS_REVIEW", 1, null), "1");
        assertNotNull(added.lastupdated());
        nextMillisecond();
        LabUnitPolicyView theirs = validationPolicyService.saveLabUnitOverride(added.id(),
                new ValidationPolicyForm("ALL", 1, null, added.lastupdated()), "1");

        assertThrows(ValidationPolicyStaleException.class, () -> validationPolicyService.saveLabUnitOverride(added.id(),
                new ValidationPolicyForm("NONE", 0, null, added.lastupdated()), "1"));
        assertThrows(ValidationPolicyStaleException.class,
                () -> validationPolicyService.setLabUnitOverrideActive(added.id(), false, added.lastupdated(), "1"));

        LabUnitPolicyView stored = validationPolicyService.getLabUnitPolicies().overrides().stream()
                .filter(override -> override.id().equals(added.id())).findFirst().orElseThrow();
        assertEquals("ALL", stored.trigger());
        assertTrue(stored.active());
        nextMillisecond();
        assertEquals(false, validationPolicyService
                .setLabUnitOverrideActive(added.id(), false, theirs.lastupdated(), "1").active());
    }

    @Test
    public void everyPolicyChangeLeavesAnAuditRowWithTheUser() throws Exception {
        String defaultId = jdbcTemplate.queryForObject(
                "SELECT CAST(id AS varchar) FROM clinlims.validation_policy WHERE test_section_id IS NULL",
                String.class);
        nextMillisecond();
        validationPolicyService.saveLabWideDefault(new ValidationPolicyForm("NEEDS_REVIEW", 1, null), "1");
        LabUnitPolicyView added = validationPolicyService
                .addLabUnitOverride(new LabUnitPolicyForm(hematology, "NONE", 0, null), "1");
        nextMillisecond();
        validationPolicyService.setLabUnitOverrideActive(added.id(), false, null, "1");

        List<String> defaultRows = history(defaultId);
        assertTrue(defaultRows.toString(), defaultRows.contains("U 1 reviewTrigger"));
        List<String> overrideRows = history(String.valueOf(added.id()));
        assertTrue(overrideRows.toString(), overrideRows.contains("I 1 "));
        assertTrue(overrideRows.toString(), overrideRows.contains("U 1 active"));
    }

    private List<String> history(String referenceId) {
        return jdbcTemplate.queryForList(
                "SELECT h.activity || ' ' || h.sys_user_id || ' ' || CASE"
                        + " WHEN convert_from(h.changes, 'UTF8') LIKE '%reviewTrigger%' THEN 'reviewTrigger'"
                        + " WHEN convert_from(h.changes, 'UTF8') LIKE '%active%' THEN 'active' ELSE '' END"
                        + " FROM clinlims.history h JOIN clinlims.reference_tables r ON r.id = h.reference_table"
                        + " WHERE lower(r.name) = 'validation_policy' AND CAST(h.reference_id AS varchar) = ?",
                String.class, referenceId);
    }

    private static void nextMillisecond() throws InterruptedException {
        Thread.sleep(5);
    }
}
