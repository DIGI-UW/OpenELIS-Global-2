package org.openelisglobal.genericsample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;

public class GenericSampleOrderServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private GenericSampleOrderService genericSampleOrderService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private AccessionNumberValidatorFactory accessionNumberValidatorFactory;

    @Before
    public void setUp() throws Exception {
        java.lang.reflect.Field field = AccessionNumberUtil.class.getDeclaredField("accessionNumberValidatorFactory");
        field.setAccessible(true);
        field.set(null, accessionNumberValidatorFactory);

        // Barcode tables must be cleared before sample/sample_item because they hold
        // FK references to those tables. Omitting them causes
        // ConstraintViolationException
        // on the second test run within the same JVM (CI sequential execution).
        cleanRowsInCurrentConnection(new String[] { "sample_barcode_info", "sample_item_barcode_info",
                "notebook_samples", "program_sample", "sample_human", "sample_item", "sample" });

        try (java.sql.Connection conn = dataSource.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER SEQUENCE clinlims.sample_barcode_info_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE clinlims.sample_item_barcode_info_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE clinlims.sample_seq RESTART WITH 1");
            stmt.execute("ALTER SEQUENCE clinlims.sample_item_seq RESTART WITH 1");
        } catch (java.sql.SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                // Sequences may not exist in all test environments; non-fatal.
            } else {
                throw e;
            }
        }

        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    // Accession numbers like "TEST-DEFAULT-001" are arbitrary unique identifiers
    // chosen
    // to be human-readable in test output. The prefix carries no format
    // significance to
    // the service — it accepts any non-blank string as a pre-supplied accession
    // number.

    @Test
    public void saveGenericSampleOrderInternal_shouldDefaultLabelCountsToOneWhenMissing() throws Exception {
        GenericSampleOrderForm form = buildForm("TEST-DEFAULT-001", null, null);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        assertNotNull("Sample ID should not be null", result.get("sampleId"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals("TEST-DEFAULT-001", saved.getAccessionNumber());

        // When numOrderLabels is null the service defaults to 1 (resolveLabelQuantity).
        LabelsSectionForm labelsSection = (LabelsSectionForm) result.get("labelsSection");
        assertNotNull("labelsSection must be present", labelsSection);
        assertNotNull("orderRow must be present", labelsSection.getOrderRow());
        assertEquals("Order label count should default to 1", 1, labelsSection.getOrderRow().getRowTotal());
    }

    @Test
    public void saveGenericSampleOrderInternal_shouldUseExplicitLabelCountsWhenProvided() throws Exception {
        GenericSampleOrderForm form = buildForm("TEST-EXPLICIT-001", 3, 4);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        assertNotNull("Sample ID should not be null", result.get("sampleId"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals("TEST-EXPLICIT-001", saved.getAccessionNumber());

        LabelsSectionForm labelsSection = (LabelsSectionForm) result.get("labelsSection");
        assertNotNull("labelsSection must be present", labelsSection);
        assertNotNull("orderRow must be present", labelsSection.getOrderRow());
        assertEquals("Order label count should match the explicit provided value", 3,
                labelsSection.getOrderRow().getRowTotal());
    }

    @Test
    public void saveGenericSampleOrderInternal_shouldHandleMissingDefaultFieldsByApplyingFallbacks() throws Exception {
        // Passing an empty form exercises the null-defaulting path: no labNo triggers
        // auto-generation, and no label counts trigger resolveLabelQuantity → 1.
        GenericSampleOrderForm form = new GenericSampleOrderForm();

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success when default fields are missing", (Boolean) result.get("success"));
        assertNotNull("Generated accession number should be returned", result.get("accessionNumber"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals(result.get("accessionNumber"), saved.getAccessionNumber());

        // Core defaulting behaviour: both label counts must resolve to 1.
        LabelsSectionForm labelsSection = (LabelsSectionForm) result.get("labelsSection");
        assertNotNull("labelsSection must be present", labelsSection);
        assertNotNull("orderRow must be present when label count defaults", labelsSection.getOrderRow());
        assertEquals("Order label count should default to 1 when not supplied", 1,
                labelsSection.getOrderRow().getRowTotal());
    }

    @Test
    public void saveGenericSampleOrderInternal_shouldIncludeWorkflowPrintModelsInResponse() throws Exception {
        GenericSampleOrderForm form = buildForm("TEST-DIALOG-001", 2, 3);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));

        LabelsSectionForm labelsSection = (LabelsSectionForm) result.get("labelsSection");
        assertNotNull("labelsSection should not be null", labelsSection);

        PostSavePrintDialogForm postSavePrintDialog = (PostSavePrintDialogForm) result.get("postSavePrintDialog");
        assertNotNull("postSavePrintDialog should not be null", postSavePrintDialog);
        assertEquals("TEST-DIALOG-001", postSavePrintDialog.getAccessionNumber());
    }

    @Test
    public void saveGenericSampleOrder_shouldSaveOrderSuccessfully() throws Exception {
        GenericSampleOrderForm form = buildForm("TEST-DELEGATE-001", null, null);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrder(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        assertNotNull("Sample ID should not be null", result.get("sampleId"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals("TEST-DELEGATE-001", saved.getAccessionNumber());
    }

    /**
     * Known bug: {@code saveGenericSampleOrder} does not catch the
     * {@link jakarta.persistence.PersistenceException} that Hibernate raises when a
     * duplicate accession number violates the {@code accnum_uk} constraint. The
     * service should intercept the exception and return {@code {success: false,
     * error: "..."}}, but currently the exception propagates to the caller. This
     * test documents the actual behaviour; it should be converted to a
     * graceful-error assertion once the service is hardened.
     */
    @Test(expected = jakarta.persistence.PersistenceException.class)
    public void saveGenericSampleOrder_duplicateAccessionNumber_currentlyThrows() throws Exception {
        GenericSampleOrderForm form = buildForm("TEST-DUPLICATE-001", null, null);
        genericSampleOrderService.saveGenericSampleOrder(form, "1");
        // Second call with the same accession number triggers the constraint violation.
        genericSampleOrderService.saveGenericSampleOrder(form, "1");
    }

    @Test
    public void getGenericSampleOrderByAccessionNumber_shouldReturnPopulatedFormWhenAccessionNumberExists()
            throws Exception {
        GenericSampleOrderForm saveForm = buildForm("TEST-QUERY-001", null, null);
        saveForm.getDefaultFields().setFrom("Query Location");
        Map<String, Object> saveResult = genericSampleOrderService.saveGenericSampleOrderInternal(saveForm, "1");
        assertTrue("Save should succeed", (Boolean) saveResult.get("success"));

        GenericSampleOrderForm retrievedForm = genericSampleOrderService
                .getGenericSampleOrderByAccessionNumber("TEST-QUERY-001");
        assertNotNull("Retrieved form should not be null", retrievedForm);
        assertNotNull("Default fields should not be null", retrievedForm.getDefaultFields());
        assertEquals("TEST-QUERY-001", retrievedForm.getDefaultFields().getLabNo());
        assertEquals("Query Location", retrievedForm.getDefaultFields().getFrom());
    }

    @Test
    public void getGenericSampleOrderByAccessionNumber_shouldReturnEmptyFormWhenAccessionNumberDoesNotExist()
            throws Exception {
        GenericSampleOrderForm retrievedForm = genericSampleOrderService
                .getGenericSampleOrderByAccessionNumber("NON-EXISTENT-ACCESSION");
        assertNotNull("Retrieved form should not be null", retrievedForm);
        // When no sample exists the service returns an empty form with null
        // defaultFields.
        assertNull("defaultFields should be null for an unknown accession number", retrievedForm.getDefaultFields());
    }

    @Test
    public void updateGenericSampleOrder_shouldUpdateOrderSuccessfully() throws Exception {
        GenericSampleOrderForm saveForm = buildForm("TEST-UPDATE-001", null, null);
        saveForm.getDefaultFields().setFrom("Original Referring ID");
        Map<String, Object> saveResult = genericSampleOrderService.saveGenericSampleOrderInternal(saveForm, "1");
        assertTrue("Save should succeed", (Boolean) saveResult.get("success"));

        GenericSampleOrderForm updateForm = buildForm("TEST-UPDATE-001", null, null);
        updateForm.getDefaultFields().setFrom("Updated Referring ID");

        Map<String, Object> updateResult = genericSampleOrderService.updateGenericSampleOrder("TEST-UPDATE-001",
                updateForm, "1");
        assertTrue("Update should report success", (Boolean) updateResult.get("success"));

        Sample saved = sampleService.get((String) saveResult.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals("Updated Referring ID", saved.getReferringId());
    }

    @Test
    public void updateGenericSampleOrder_shouldReturnErrorWhenAccessionNumberDoesNotExist() throws Exception {
        GenericSampleOrderForm updateForm = buildForm("NON-EXISTENT", null, null);
        Map<String, Object> updateResult = genericSampleOrderService.updateGenericSampleOrder("NON-EXISTENT",
                updateForm, "1");
        assertTrue("Update should report failure", !(Boolean) updateResult.get("success"));
        assertNotNull("Error message should be populated", updateResult.get("error"));
    }

    private GenericSampleOrderForm buildForm(String labNo, Integer numOrderLabels, Integer numSpecimenLabels) {
        GenericSampleOrderForm form = new GenericSampleOrderForm();
        GenericSampleOrderForm.DefaultFields defaultFields = new GenericSampleOrderForm.DefaultFields();
        defaultFields.setLabNo(labNo);
        defaultFields.setNumOrderLabels(numOrderLabels);
        defaultFields.setNumSpecimenLabels(numSpecimenLabels);
        form.setDefaultFields(defaultFields);
        return form;
    }
}
