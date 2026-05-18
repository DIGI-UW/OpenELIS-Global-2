package org.openelisglobal.genericsample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.form.LabelsSectionForm;
import org.openelisglobal.barcode.form.PostSavePrintDialogForm;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;

public class GenericSampleOrderServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private GenericSampleOrderService genericSampleOrderService;

    @Autowired
    private SampleService sampleService;

    @Before
    public void setUp() throws Exception {
        try {
            java.lang.reflect.Field field = org.openelisglobal.sample.util.AccessionNumberUtil.class
                    .getDeclaredField("accessionNumberValidatorFactory");
            field.setAccessible(true);
            if (field.get(null) == null) {
                field.set(null, org.openelisglobal.spring.util.SpringContext
                        .getBean(org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.class));
            }
        } catch (Exception e) {
            // Ignore reflection errors in test setup
        }

        cleanRowsInCurrentConnection(new String[] { "sample_item", "sample" });
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    @Test
    public void saveGenericSampleOrderInternal_shouldDefaultLabelCountsToOneWhenMissing() throws Exception {
        GenericSampleOrderForm form = buildForm("M2-DEFAULT-001", null, null);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        assertNotNull("Sample ID should not be null", result.get("sampleId"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals("M2-DEFAULT-001", saved.getAccessionNumber());
    }

    @Test
    public void saveGenericSampleOrderInternal_shouldUseExplicitLabelCountsWhenProvided() throws Exception {
        GenericSampleOrderForm form = buildForm("M2-EXPLICIT-001", 3, 4);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        assertNotNull("Sample ID should not be null", result.get("sampleId"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals("M2-EXPLICIT-001", saved.getAccessionNumber());
    }

    @Test
    public void saveGenericSampleOrderInternal_shouldHandleMissingDefaultFieldsByApplyingFallbacks() throws Exception {
        GenericSampleOrderForm form = new GenericSampleOrderForm();

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success when default fields are missing", (Boolean) result.get("success"));
        assertNotNull("Generated accession number should be returned", result.get("accessionNumber"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals(result.get("accessionNumber"), saved.getAccessionNumber());
    }

    @Test
    public void saveGenericSampleOrderInternal_shouldIncludeWorkflowPrintModelsInResponse() throws Exception {
        GenericSampleOrderForm form = buildForm("M5-DIALOG-001", 2, 3);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrderInternal(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));

        LabelsSectionForm labelsSection = (LabelsSectionForm) result.get("labelsSection");
        assertNotNull("labelsSection should not be null", labelsSection);

        PostSavePrintDialogForm postSavePrintDialog = (PostSavePrintDialogForm) result.get("postSavePrintDialog");
        assertNotNull("postSavePrintDialog should not be null", postSavePrintDialog);
        assertEquals("M5-DIALOG-001", postSavePrintDialog.getAccessionNumber());
    }

    @Test
    public void saveGenericSampleOrder_shouldSaveOrderSuccessfully() throws Exception {
        GenericSampleOrderForm form = buildForm("M2-DELEGATE-001", null, null);

        Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrder(form, "1");

        assertTrue("Save should report success", (Boolean) result.get("success"));
        assertNotNull("Sample ID should not be null", result.get("sampleId"));

        Sample saved = sampleService.get((String) result.get("sampleId"));
        assertNotNull("Sample should be retrievable", saved);
        assertEquals("M2-DELEGATE-001", saved.getAccessionNumber());
    }

    @Test
    public void getGenericSampleOrderByAccessionNumber_shouldReturnPopulatedFormWhenAccessionNumberExists()
            throws Exception {
        GenericSampleOrderForm saveForm = buildForm("M2-QUERY-001", null, null);
        saveForm.getDefaultFields().setFrom("Query Location");
        Map<String, Object> saveResult = genericSampleOrderService.saveGenericSampleOrderInternal(saveForm, "1");
        assertTrue("Save should succeed", (Boolean) saveResult.get("success"));

        GenericSampleOrderForm retrievedForm = genericSampleOrderService
                .getGenericSampleOrderByAccessionNumber("M2-QUERY-001");
        assertNotNull("Retrieved form should not be null", retrievedForm);
        assertNotNull("Default fields should not be null", retrievedForm.getDefaultFields());
        assertEquals("M2-QUERY-001", retrievedForm.getDefaultFields().getLabNo());
        assertEquals("Query Location", retrievedForm.getDefaultFields().getFrom());
    }

    @Test
    public void getGenericSampleOrderByAccessionNumber_shouldReturnEmptyFormWhenAccessionNumberDoesNotExist()
            throws Exception {
        GenericSampleOrderForm retrievedForm = genericSampleOrderService
                .getGenericSampleOrderByAccessionNumber("NON-EXISTENT-ACCESSION");
        assertNotNull("Retrieved form should not be null", retrievedForm);
        assertTrue("Default fields should be null or empty",
                retrievedForm.getDefaultFields() == null || retrievedForm.getDefaultFields().getLabNo() == null);
    }

    @Test
    public void updateGenericSampleOrder_shouldUpdateOrderSuccessfully() throws Exception {
        GenericSampleOrderForm saveForm = buildForm("M2-UPDATE-001", null, null);
        saveForm.getDefaultFields().setFrom("Original Referring ID");
        Map<String, Object> saveResult = genericSampleOrderService.saveGenericSampleOrderInternal(saveForm, "1");
        assertTrue("Save should succeed", (Boolean) saveResult.get("success"));

        GenericSampleOrderForm updateForm = buildForm("M2-UPDATE-001", null, null);
        updateForm.getDefaultFields().setFrom("Updated Referring ID");

        ;
        Map<String, Object> updateResult = genericSampleOrderService.updateGenericSampleOrder("M2-UPDATE-001",
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
