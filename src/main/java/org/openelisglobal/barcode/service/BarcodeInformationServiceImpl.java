package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarcodeInformationServiceImpl implements BarcodeInformationService {

    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    @Transactional
    public void updateBarcodeInfoFromForm(BarcodeConfigurationForm form, String sysUserId) {
        updateSiteInfo("heightOrderLabels", Float.toString(form.getHeightOrderLabels()), "text", sysUserId);
        updateSiteInfo("widthOrderLabels", Float.toString(form.getWidthOrderLabels()), "text", sysUserId);
        updateSiteInfo("heightSpecimenLabels", Float.toString(form.getHeightSpecimenLabels()), "text", sysUserId);
        updateSiteInfo("widthSpecimenLabels", Float.toString(form.getWidthSpecimenLabels()), "text", sysUserId);
        // updateSiteInfo("heightAliquotLabels",
        // Float.toString(form.getHeightAliquotLabels()), "text", sysUserId);
        // updateSiteInfo("widthAliquotLabels",
        // Float.toString(form.getWidthAliquotLabels()), "text", sysUserId);
        updateSiteInfo("heightBlockLabels", Float.toString(form.getHeightBlockLabels()), "text", sysUserId);
        updateSiteInfo("widthBlockLabels", Float.toString(form.getWidthBlockLabels()), "text", sysUserId);
        updateSiteInfo("heightSlideLabels", Float.toString(form.getHeightSlideLabels()), "text", sysUserId);
        updateSiteInfo("widthSlideLabels", Float.toString(form.getWidthSlideLabels()), "text", sysUserId);
        updateSiteInfo("heightFreezerLabels", Float.toString(form.getHeightFreezerLabels()), "text", sysUserId);
        updateSiteInfo("widthFreezerLabels", Float.toString(form.getWidthFreezerLabels()), "text", sysUserId);

        updateSiteInfo("numMaxOrderLabels", Integer.toString(form.getNumMaxOrderLabels()), "text", sysUserId);
        updateSiteInfo("numMaxSpecimenLabels", Integer.toString(form.getNumMaxSpecimenLabels()), "text", sysUserId);
        // updateSiteInfo("numMaxAliquotLabels",
        // Integer.toString(form.getNumMaxAliquotLabels()), "text", sysUserId);
        updateSiteInfo("numMaxSlideLabels", Integer.toString(form.getNumMaxSlideLabels()), "text", sysUserId);
        updateSiteInfo("numMaxBlockLabels", Integer.toString(form.getNumMaxBlockLabels()), "text", sysUserId);
        updateSiteInfo("numMaxFreezerLabels", Integer.toString(form.getNumMaxFreezerLabels()), "text", sysUserId);

        updateSiteInfo("numDefaultOrderLabels", Integer.toString(form.getNumDefaultOrderLabels()), "text", sysUserId);
        updateSiteInfo("numDefaultSpecimenLabels", Integer.toString(form.getNumDefaultSpecimenLabels()), "text",
                sysUserId);
        // updateSiteInfo("numDefaultAliquotLabels",
        // Integer.toString(form.getNumDefaultAliquotLabels()), "text",
        // sysUserId);
        updateSiteInfo("numDefaultSlideLabels", Integer.toString(form.getNumDefaultSlideLabels()), "text",
                sysUserId);
        updateSiteInfo("numDefaultBlockLabels", Integer.toString(form.getNumDefaultBlockLabels()), "text",
                sysUserId);
        updateSiteInfo("numDefaultFreezerLabels", Integer.toString(form.getNumDefaultFreezerLabels()), "text",
                sysUserId);

        updateSiteInfo("collectionDateCheck", Boolean.toString(form.getCollectionDateCheck()), "boolean", sysUserId);
        updateSiteInfo("collectedByCheck", Boolean.toString(form.getCollectedByCheck()), "boolean", sysUserId);
        updateSiteInfo("patientSexCheck", Boolean.toString(form.getPatientSexCheck()), "boolean", sysUserId);
        updateSiteInfo("testsCheck", Boolean.toString(form.getTestsCheck()), "boolean", sysUserId);

        updateSiteInfo("prePrintUseAltAccession", Boolean.toString(!form.getPrePrintDontUseAltAccession()), "boolean",
                sysUserId);
        updateSiteInfo("prePrintAltAccessionPrefix", form.getPrePrintAltAccessionPrefix(), "text", sysUserId);
    }

    /**
     * Persist a bar code configuration value in the database under site_information
     *
     * @param errors    For error tracking on inserts
     * @param name      The name in the database
     * @param value     The new value to save
     * @param valueType The type of the value to save
     */
    private void updateSiteInfo(String name, String value, String valueType, String sysUserId) {
        if ("boolean".equals(valueType)) {
            value = "true".equalsIgnoreCase(value) ? "true" : "false";
        }
        SiteInformation siteInformation = siteInformationService.getSiteInformationByName(name);
        if (siteInformation == null) {
            siteInformation = new SiteInformation();
            siteInformation.setName(name);
            siteInformation.setValue(value);
            siteInformation.setValueType(valueType);
            siteInformation.setSysUserId(sysUserId);
            siteInformationService.insert(siteInformation);
        } else {
            siteInformation.setValue(value);
            siteInformation.setSysUserId(sysUserId);
            siteInformationService.update(siteInformation);
        }
    }
}
