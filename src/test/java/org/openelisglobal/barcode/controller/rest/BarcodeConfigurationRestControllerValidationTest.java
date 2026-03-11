package org.openelisglobal.barcode.controller.rest;

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
}
