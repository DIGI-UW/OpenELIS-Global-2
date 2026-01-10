package org.openelisglobal.barcode.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.barcode.service.BarcodeInformationService;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BarcodeConfigurationController extends BaseController {

        private static final String[] ALLOWED_FIELDS = new String[] { //
                        "heightOrderLabels", "heightSpecimenLabels", "heightBlockLabels", "heightSlideLabels",
                        "heightFreezerLabels",
                        "widthOrderLabels", "widthSpecimenLabels", "widthBlockLabels", "widthSlideLabels",
                        "widthFreezerLabels", //
                        "numMaxOrderLabels", "numMaxSpecimenLabels", "numMaxBlockLabels", "numMaxSlideLabels",
                        "numMaxFreezerLabels", //
                        "numDefaultOrderLabels", " numDefaultSpecimenLabels", "numDefaultSlideLabels",
                        "numDefaultBlockLabels",
                        "numDefaultFreezerLabels", //
                        "orderPatientDobCheck", "orderPatientIdCheck", "orderPatientNameCheck", "orderSiteIdCheck",
                        "specimenPatientDobCheck", "specimenPatientIdCheck", "specimenPatientNameCheck",
                        "specimenCollectionDateCheck", "specimenCollectedByCheck", "specimenTestsCheck",
                        "specimenPatientSexCheck",
                        "slidePatientIdCheck", "slideSlideIdCheck", "slideStainTypeCheck", "slideBlockIdCheck",
                        "slideCaseNumberCheck", "blockPatientIdCheck", "blockBlockIdCheck", "blockSpecimenTypeCheck",
                        "blockCaseNumberCheck",
                        "freezerPatientIdCheck", "freezerStorageLocationCheck", "freezerSpecimenTypeCheck",
                        "freezerCollectionDateCheck", "freezerExpiryDateCheck",
                        "prePrintDontUseAltAccession", "prePrintAltAccessionPrefix" };

        @Autowired
        private BarcodeInformationService barcodeInformationService;

        @InitBinder
        public void initBinder(WebDataBinder binder) {
                binder.setAllowedFields(ALLOWED_FIELDS);
        }

        @RequestMapping(value = "/BarcodeConfiguration", method = RequestMethod.GET)
        public ModelAndView showBarcodeConfiguration(HttpServletRequest request)
                        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
                String forward = FWD_SUCCESS;
                BarcodeConfigurationForm form = new BarcodeConfigurationForm();

                addFlashMsgsToRequest(request);
                form.setCancelAction("MasterListsPage");

                setFields(form);

                request.getSession().setAttribute(SAVE_DISABLED, "false");

                return findForward(forward, form);
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
                form.setHeightOrderLabels(Float.parseFloat(heightOrderLabels));
                form.setWidthOrderLabels(Float.parseFloat(widthOrderLabels));
                form.setHeightSpecimenLabels(Float.parseFloat(heightSpecimenLabels));
                form.setWidthSpecimenLabels(Float.parseFloat(widthSpecimenLabels));
                form.setHeightSlideLabels(Float.parseFloat(heightSlideLabels));
                form.setWidthSlideLabels(Float.parseFloat(widthSlideLabels));
                form.setHeightBlockLabels(Float.parseFloat(heightBlockLabels));
                form.setWidthBlockLabels(Float.parseFloat(widthBlockLabels));
                form.setHeightFreezerLabels(Float.parseFloat(heightFreezerLabels));
                form.setWidthFreezerLabels(Float.parseFloat(widthFreezerLabels));

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
                form.setNumMaxOrderLabels(Integer.parseInt(numMaxOrderLabels));
                form.setNumMaxSpecimenLabels(Integer.parseInt(numMaxSpecimenLabels));
                form.setNumMaxAliquotLabels(Integer.parseInt(numMaxAliquotLabels));
                form.setNumMaxSlideLabels(Integer.parseInt(numMaxSlideLabels));
                form.setNumMaxBlockLabels(Integer.parseInt(numMaxBlockLabels));
                form.setNumMaxFreezerLabels(Integer.parseInt(numMaxFreezerLabels));

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
                form.setNumDefaultOrderLabels(Integer.parseInt(numDefaultOrderLabels));
                form.setNumDefaultSpecimenLabels(Integer.parseInt(numDefaultSpecimenLabels));
                form.setNumDefaultAliquotLabels(Integer.parseInt(numDefaultAliquotLabels));
                form.setNumDefaultSlideLabels(Integer.parseInt(numDefaultSlideLabels));
                form.setNumDefaultBlockLabels(Integer.parseInt(numDefaultBlockLabels));
                form.setNumDefaultFreezerLabels(Integer.parseInt(numDefaultFreezerLabels));

                // get the optional specimen values
                String collectionDateCheck = ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTION_DATE);
                String collectedByCheck = ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_COLLECTED_BY);
                String testsCheck = ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_TESTS);
                String patientSexCheck = ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.SPECIMEN_LABEL_FIELD_PATIENT_SEX);
                // set the optional specimen values
                form.setSpecimenCollectionDateCheck(Boolean.valueOf(collectionDateCheck));
                form.setSpecimenCollectedByCheck(Boolean.valueOf(collectedByCheck));
                form.setSpecimenTestsCheck(Boolean.valueOf(testsCheck));
                form.setSpecimenPatientSexCheck(Boolean.valueOf(patientSexCheck));

                Boolean prePrintUseAltAccession = Boolean
                                .valueOf(ConfigurationProperties.getInstance()
                                                .getPropertyValue(Property.USE_ALT_ACCESSION_PREFIX));
                String prePrintAltAccessionPrefix = ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.ALT_ACCESSION_PREFIX);
                form.setPrePrintDontUseAltAccession(!prePrintUseAltAccession);
                form.setPrePrintAltAccessionPrefix(prePrintAltAccessionPrefix);
                form.setSitePrefix(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.ACCESSION_NUMBER_PREFIX));
        }

        @RequestMapping(value = "/BarcodeConfiguration", method = RequestMethod.POST)
        public ModelAndView barcodeConfigurationSave(HttpServletRequest request,
                        @ModelAttribute("form") @Valid BarcodeConfigurationForm form, BindingResult result,
                        RedirectAttributes redirectAttributes) {
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
                        barcodeInformationService.updateBarcodeInfoFromForm(form, getSysUserId(request));
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
        }

        @Override
        protected String findLocalForward(String forward) {
                if (FWD_SUCCESS.equals(forward)) {
                        return "BarcodeConfigurationDefinition";
                } else if (FWD_SUCCESS_INSERT.equals(forward)) {
                        return "redirect:/BarcodeConfiguration";
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
