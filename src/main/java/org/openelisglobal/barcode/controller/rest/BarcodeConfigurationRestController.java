package org.openelisglobal.barcode.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.barcode.service.BarcodeConfigService;
import org.openelisglobal.barcode.util.BarcodeConfigUtil;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/rest")
public class BarcodeConfigurationRestController extends BaseController {

    private static final String FWD_SUCCESS = "success";
    private static final String FWD_FAIL = "fail";
    private static final String FWD_SUCCESS_INSERT = "success_insert";
    private static final String FWD_FAIL_INSERT = "fail_insert";
    private static final int MIN_LABEL_COUNT = 1;
    private static final int MAX_LABEL_COUNT = 1000;
    private static final int MAX_LABEL_FALLBACK = 10;
    private static final int DEFAULT_LABEL_FALLBACK = 1;

    private static final String[] ALLOWED_FIELDS = new String[] { //
            "heightOrderLabels", "heightSpecimenLabels", "heightBlockLabels", "heightSlideLabels",
            "heightFreezerLabels", "widthOrderLabels", "widthSpecimenLabels", "widthBlockLabels", "widthSlideLabels",
            "widthFreezerLabels", //
            "numMaxOrderLabels", "numMaxSpecimenLabels", "numMaxBlockLabels", "numMaxSlideLabels",
            "numMaxFreezerLabels", //
            "numDefaultOrderLabels", "numDefaultSpecimenLabels", "numDefaultSlideLabels", "numDefaultBlockLabels",
            "numDefaultFreezerLabels", //
            "orderPatientDobCheck", "orderPatientIdCheck", "orderPatientNameCheck", "orderSiteIdCheck",
            "specimenPatientDobCheck", "specimenPatientIdCheck", "specimenPatientNameCheck",
            "specimenCollectionDateCheck", "specimenCollectedByCheck", "specimenTestsCheck", "specimenPatientSexCheck",
            "slidePatientIdCheck", "slideSlideIdCheck", "slideStainTypeCheck", "slideBlockIdCheck",
            "slideCaseNumberCheck", "blockPatientIdCheck", "blockBlockIdCheck", "blockSpecimenTypeCheck",
            "blockCaseNumberCheck", "freezerPatientIdCheck", "freezerStorageLocationCheck", "freezerSpecimenTypeCheck",
            "freezerCollectionDateCheck", "freezerExpiryDateCheck", "prePrintDontUseAltAccession",
            "prePrintAltAccessionPrefix" };

    @Autowired
    private BarcodeConfigService barcodeConfigService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/BarcodeConfiguration")
    public BarcodeConfigurationForm showBarcodeConfiguration(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String forward = FWD_SUCCESS;
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();

        addFlashMsgsToRequest(request);
        form.setCancelAction("MasterListsPage");

        setFields(form);

        request.getSession().setAttribute(SAVE_DISABLED, "false");

        // return findForward(forward, form);
        return form;
    }

    /**
     * Set the form fields with those values stored in the database
     *
     * @param form The form to populate
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private void setFields(BarcodeConfigurationForm form) {

        // get the dimension values
        String heightOrderLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ORDER_LABEL_BARCODE_HEIGHT);
        String widthOrderLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ORDER_LABEL_BARCODE_WIDTH);
        String heightSpecimenLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_HEIGHT);
        String widthSpecimenLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_BARCODE_WIDTH);
        String heightSlideLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SLIDE_LABEL_BARCODE_HEIGHT);
        String widthSlideLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SLIDE_LABEL_BARCODE_WIDTH);
        String heightBlockLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BLOCK_LABEL_BARCODE_HEIGHT);
        String widthBlockLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BLOCK_LABEL_BARCODE_WIDTH);
        String heightFreezerLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_BARCODE_HEIGHT);
        String widthFreezerLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_BARCODE_WIDTH);
        // set the dimension values
        form.setHeightOrderLabels(BarcodeConfigUtil.parseFloatSafe(heightOrderLabels, 2.0f));
        form.setWidthOrderLabels(BarcodeConfigUtil.parseFloatSafe(widthOrderLabels, 2.0f));
        form.setHeightSpecimenLabels(BarcodeConfigUtil.parseFloatSafe(heightSpecimenLabels, 2.0f));
        form.setWidthSpecimenLabels(BarcodeConfigUtil.parseFloatSafe(widthSpecimenLabels, 2.0f));
        form.setHeightSlideLabels(BarcodeConfigUtil.parseFloatSafe(heightSlideLabels, 2.0f));
        form.setWidthSlideLabels(BarcodeConfigUtil.parseFloatSafe(widthSlideLabels, 2.0f));
        form.setHeightBlockLabels(BarcodeConfigUtil.parseFloatSafe(heightBlockLabels, 2.0f));
        form.setWidthBlockLabels(BarcodeConfigUtil.parseFloatSafe(widthBlockLabels, 2.0f));
        form.setHeightFreezerLabels(BarcodeConfigUtil.parseFloatSafe(heightFreezerLabels, 2.0f));
        form.setWidthFreezerLabels(BarcodeConfigUtil.parseFloatSafe(widthFreezerLabels, 2.0f));

        // get the maximum print values
        String numMaxOrderLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.MAX_ORDER_LABEL_PRINTED);
        String numMaxSpecimenLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.MAX_SPECIMEN_LABEL_PRINTED);
        String numMaxAliquotLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.MAX_ALIQUOT_LABEL_PRINTED);
        String numMaxSlideLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.MAX_SLIDE_LABEL_PRINTED);
        String numMaxBlockLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.MAX_BLOCK_LABEL_PRINTED);
        String numMaxFreezerLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.MAX_FREEZER_LABEL_PRINTED);
        // set the maximum print values
        form.setNumMaxOrderLabels(BarcodeConfigUtil.parseIntSafe(numMaxOrderLabels, 10));
        form.setNumMaxSpecimenLabels(BarcodeConfigUtil.parseIntSafe(numMaxSpecimenLabels, 10));
        form.setNumMaxAliquotLabels(BarcodeConfigUtil.parseIntSafe(numMaxAliquotLabels, 10));
        form.setNumMaxSlideLabels(BarcodeConfigUtil.parseIntSafe(numMaxSlideLabels, 10));
        form.setNumMaxBlockLabels(BarcodeConfigUtil.parseIntSafe(numMaxBlockLabels, 10));
        form.setNumMaxFreezerLabels(BarcodeConfigUtil.parseIntSafe(numMaxFreezerLabels, 10));

        // get the default print values
        String numDefaultOrderLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.DEFAULT_ORDER_LABEL_PRINTED);
        String numDefaultSpecimenLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.DEFAULT_SPECIMEN_LABEL_PRINTED);
        String numDefaultAliquotLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.DEFAULT_ALIQUOT_LABEL_PRINTED);
        String numDefaultSlideLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.DEFAULT_SLIDE_LABEL_PRINTED);
        String numDefaultBlockLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.DEFAULT_BLOCK_LABEL_PRINTED);
        String numDefaultFreezerLabels = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.DEFAULT_FREEZER_LABEL_PRINTED);
        // set the maximum print values
        form.setNumDefaultOrderLabels(BarcodeConfigUtil.parseIntSafe(numDefaultOrderLabels, 1));
        form.setNumDefaultSpecimenLabels(BarcodeConfigUtil.parseIntSafe(numDefaultSpecimenLabels, 1));
        form.setNumDefaultAliquotLabels(BarcodeConfigUtil.parseIntSafe(numDefaultAliquotLabels, 1));
        form.setNumDefaultSlideLabels(BarcodeConfigUtil.parseIntSafe(numDefaultSlideLabels, 1));
        form.setNumDefaultBlockLabels(BarcodeConfigUtil.parseIntSafe(numDefaultBlockLabels, 1));
        form.setNumDefaultFreezerLabels(BarcodeConfigUtil.parseIntSafe(numDefaultFreezerLabels, 1));

        // get the optional order values
        String orderPatientDobCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ORDER_LABEL_FIELD_PATIENT_DOB);
        String orderPatientIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ORDER_LABEL_FIELD_PATIENT_ID);
        String orderPatientNameCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ORDER_LABEL_FIELD_PATIENT_NAME);
        String orderSiteIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ORDER_LABEL_FIELD_SITE_ID);
        // set the optional order values
        form.setOrderPatientDobCheck(Boolean.valueOf(orderPatientDobCheck));
        form.setOrderPatientIdCheck(Boolean.valueOf(orderPatientIdCheck));
        form.setOrderPatientNameCheck(Boolean.valueOf(orderPatientNameCheck));
        form.setOrderSiteIdCheck(Boolean.valueOf(orderSiteIdCheck));

        // get the optional specimen values
        String specimenPatientDobCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_DOB);
        String specimenPatientIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_ID);
        String specimenPatientNameCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_NAME);
        String specimenCollectionDateCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTION_DATE);
        String specimenCollectedByCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTED_BY);
        String specimenTestsCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_TESTS);
        String specimenPatientSexCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_SEX);
        // set the optional specimen values
        form.setSpecimenPatientDobCheck(Boolean.valueOf(specimenPatientDobCheck));
        form.setSpecimenPatientIdCheck(Boolean.valueOf(specimenPatientIdCheck));
        form.setSpecimenPatientNameCheck(Boolean.valueOf(specimenPatientNameCheck));
        form.setSpecimenCollectionDateCheck(Boolean.valueOf(specimenCollectionDateCheck));
        form.setSpecimenCollectedByCheck(Boolean.valueOf(specimenCollectedByCheck));
        form.setSpecimenTestsCheck(Boolean.valueOf(specimenTestsCheck));
        form.setSpecimenPatientSexCheck(Boolean.valueOf(specimenPatientSexCheck));

        // get the optional slide values
        String slidePatientIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SLIDE_LABEL_FIELD_PATIENT_ID);
        String slideSlideIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SLIDE_LABEL_FIELD_SLIDE_ID);
        String slideStainTypeCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SLIDE_LABEL_FIELD_STAIN_TYPE);
        String slideBlockIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SLIDE_LABEL_FIELD_BLOCK_ID);
        String slideCaseNumberCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.SLIDE_LABEL_FIELD_CASE_NUMBER);
        // set the optional specimen values
        form.setSlidePatientIdCheck(Boolean.valueOf(slidePatientIdCheck));
        form.setSlideSlideIdCheck(Boolean.valueOf(slideSlideIdCheck));
        form.setSlideStainTypeCheck(Boolean.valueOf(slideStainTypeCheck));
        form.setSlideBlockIdCheck(Boolean.valueOf(slideBlockIdCheck));
        form.setSlideCaseNumberCheck(Boolean.valueOf(slideCaseNumberCheck));

        // get the block values
        String blockPatientIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BLOCK_LABEL_FIELD_PATIENT_ID);
        String blockBlockIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BLOCK_LABEL_FIELD_BLOCK_ID);
        String blockSpecimenTypeCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BLOCK_LABEL_FIELD_SPECIMEN_TYPE);
        String blockCaseNumberCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BLOCK_LABEL_FIELD_CASE_NUMBER);
        // set the optional specimen values
        form.setBlockPatientIdCheck(Boolean.valueOf(blockPatientIdCheck));
        form.setBlockBlockIdCheck(Boolean.valueOf(blockBlockIdCheck));
        form.setBlockSpecimenTypeCheck(Boolean.valueOf(blockSpecimenTypeCheck));
        form.setBlockCaseNumberCheck(Boolean.valueOf(blockCaseNumberCheck));

        // get the freezer values
        String freezerPatientIdCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_PATIENT_ID);
        String freezerStorageLocationCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_STORAGE_LOCATION);
        String freezerSpecimenTypeCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_SPECIMEN_TYPE);
        String freezerCollectionDateCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_COLLECTION_DATE);
        String freezerExpiryDateCheck = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.FREEZER_LABEL_FIELD_EXPIRY_DATE);
        // set the optional specimen values
        form.setFreezerPatientIdCheck(Boolean.valueOf(freezerPatientIdCheck));
        form.setFreezerStorageLocationCheck(Boolean.valueOf(freezerStorageLocationCheck));
        form.setFreezerSpecimenTypeCheck(Boolean.valueOf(freezerSpecimenTypeCheck));
        form.setFreezerCollectionDateCheck(Boolean.valueOf(freezerCollectionDateCheck));
        form.setFreezerExpiryDateCheck(Boolean.valueOf(freezerExpiryDateCheck));

        Boolean prePrintUseAltAccession = Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_ALT_ACCESSION_PREFIX));
        String prePrintAltAccessionPrefix = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.ALT_ACCESSION_PREFIX);
        form.setPrePrintDontUseAltAccession(!prePrintUseAltAccession);
        form.setPrePrintAltAccessionPrefix(prePrintAltAccessionPrefix);
        form.setSitePrefix(ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_PREFIX));
    }

    @PostMapping(value = "/BarcodeConfiguration")
    public Object barcodeConfigurationSave(HttpServletRequest request,
            @RequestBody @Valid BarcodeConfigurationForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        normalizeQuantityFields(form);
        if (!form.getPrePrintDontUseAltAccession()
                && GenericValidator.isBlankOrNull(form.getPrePrintAltAccessionPrefix())) {
            result.rejectValue("prePrintAltAccessionPrefix", "error.altaccession.required");
        }
        if (result.hasErrors()) {
            saveErrors(result);
            form.setCancelAction("MasterListsPage");
            return findForward(FWD_FAIL_INSERT, form);
        }

        // ensure transaction block
        try {
            barcodeConfigService.updateBarcodeInfoFromForm(form, getSysUserId(request));
        } catch (LIMSRuntimeException e) {
            result.reject("barcode.config.error.insert");
        } finally {
            ConfigurationProperties.loadDBValuesIntoConfiguration();
        }

        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_INSERT, form);
        }

        redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
        return findForward(FWD_SUCCESS_INSERT, form);
        // return "redirect:/rest/BarcodeConfiguration";
    }

    private void normalizeQuantityFields(BarcodeConfigurationForm form) {
        form.setNumMaxOrderLabels(normalizeLabelCount(form.getNumMaxOrderLabels(), MAX_LABEL_FALLBACK));
        form.setNumMaxSpecimenLabels(normalizeLabelCount(form.getNumMaxSpecimenLabels(), MAX_LABEL_FALLBACK));
        form.setNumMaxAliquotLabels(normalizeLabelCount(form.getNumMaxAliquotLabels(), MAX_LABEL_FALLBACK));
        form.setNumMaxSlideLabels(normalizeLabelCount(form.getNumMaxSlideLabels(), MAX_LABEL_FALLBACK));
        form.setNumMaxBlockLabels(normalizeLabelCount(form.getNumMaxBlockLabels(), MAX_LABEL_FALLBACK));
        form.setNumMaxFreezerLabels(normalizeLabelCount(form.getNumMaxFreezerLabels(), MAX_LABEL_FALLBACK));

        form.setNumDefaultOrderLabels(normalizeLabelCount(form.getNumDefaultOrderLabels(), DEFAULT_LABEL_FALLBACK));
        form.setNumDefaultSpecimenLabels(
                normalizeLabelCount(form.getNumDefaultSpecimenLabels(), DEFAULT_LABEL_FALLBACK));
        form.setNumDefaultAliquotLabels(normalizeLabelCount(form.getNumDefaultAliquotLabels(), DEFAULT_LABEL_FALLBACK));
        form.setNumDefaultSlideLabels(normalizeLabelCount(form.getNumDefaultSlideLabels(), DEFAULT_LABEL_FALLBACK));
        form.setNumDefaultBlockLabels(normalizeLabelCount(form.getNumDefaultBlockLabels(), DEFAULT_LABEL_FALLBACK));
        form.setNumDefaultFreezerLabels(normalizeLabelCount(form.getNumDefaultFreezerLabels(), DEFAULT_LABEL_FALLBACK));
    }

    private int normalizeLabelCount(int value, int fallback) {
        if (value < MIN_LABEL_COUNT || value > MAX_LABEL_COUNT) {
            return fallback;
        }
        return value;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "BarcodeConfigurationDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/rest/BarcodeConfiguration";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "BarcodeConfigurationDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "barcodeconfiguration.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "barcodeconfiguration.browse.title";
    }
}
