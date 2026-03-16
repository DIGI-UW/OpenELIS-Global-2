package org.openelisglobal.barcode.controller.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.barcode.service.BarcodeConfigService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

@RunWith(MockitoJUnitRunner.class)
public class BarcodeConfigurationRestControllerValidationTest {

    @Mock
    private BarcodeConfigService barcodeConfigService;

    private BarcodeConfigurationRestController controller;

    @Before
    public void setUp() {
        controller = new BarcodeConfigurationRestController();
        ReflectionTestUtils.setField(controller, "barcodeConfigService", barcodeConfigService);
    }

    @Test
    public void barcodeConfigurationSave_rejectsNegativeLabelCountsBeforePersist() {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setNumMaxOrderLabels(-1);

        BindingResult result = new BeanPropertyBindingResult(form, "barcodeConfigurationForm");
        ReflectionTestUtils.invokeMethod(controller, "validateLabelCountRanges", form, result);

        assertTrue("Expected field error for negative max order labels", result.hasFieldErrors("numMaxOrderLabels"));
    }

    @Test
    public void barcodeConfigurationSave_rejectsOversizedLabelCountsBeforePersist() {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setNumMaxSpecimenLabels(5001);

        BindingResult result = new BeanPropertyBindingResult(form, "barcodeConfigurationForm");
        ReflectionTestUtils.invokeMethod(controller, "validateLabelCountRanges", form, result);

        assertTrue("Expected field error for oversized max specimen labels",
                result.hasFieldErrors("numMaxSpecimenLabels"));
    }

    @Test
    public void barcodeConfigurationSave_allowsUnspecifiedLabelCountsForNormalization() {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setNumMaxOrderLabels(0);

        BindingResult result = new BeanPropertyBindingResult(form, "barcodeConfigurationForm");
        ReflectionTestUtils.invokeMethod(controller, "validateLabelCountRanges", form, result);

        assertFalse("Expected no field error for unspecified max order labels",
                result.hasFieldErrors("numMaxOrderLabels"));
    }

    /** FR-004a: default must not exceed max for each label type */
    @Test
    public void barcodeConfigurationSave_rejectsDefaultGreaterThanMax() {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setNumMaxOrderLabels(5);
        form.setNumDefaultOrderLabels(10);

        BindingResult result = new BeanPropertyBindingResult(form, "barcodeConfigurationForm");
        ReflectionTestUtils.invokeMethod(controller, "validateLabelCountRanges", form, result);

        assertTrue("Expected field error when default order labels > max",
                result.hasFieldErrors("numDefaultOrderLabels"));
    }

    /** FR-002b: dimension values must be positive */
    @Test
    public void barcodeConfigurationSave_rejectsNonPositiveDimensions() {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setHeightOrderLabels(0);
        form.setWidthOrderLabels(-0.5f);

        BindingResult result = new BeanPropertyBindingResult(form, "barcodeConfigurationForm");
        ReflectionTestUtils.invokeMethod(controller, "validateDimensionFields", form, result);

        assertTrue("Expected field error for non-positive height", result.hasFieldErrors("heightOrderLabels"));
        assertTrue("Expected field error for non-positive width", result.hasFieldErrors("widthOrderLabels"));
    }
}
