package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarcodeConfigServiceImpl implements BarcodeConfigService {

    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    @Override
    @Transactional
    public void updateBarcodeInfoFromForm(BarcodeConfigurationForm form, String sysUserId) {
        SiteInformationDomain labelsDomain = siteInformationDomainService.getByName("labels");
        updateSiteInfo("heightOrderLabels", Float.toString(form.getHeightOrderLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("widthOrderLabels", Float.toString(form.getWidthOrderLabels()), "text", sysUserId, labelsDomain);
        updateSiteInfo("heightSpecimenLabels", Float.toString(form.getHeightSpecimenLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("widthSpecimenLabels", Float.toString(form.getWidthSpecimenLabels()), "text", sysUserId,
                labelsDomain);
        // updateSiteInfo("heightAliquotLabels",
        // Float.toString(form.getHeightAliquotLabels()), "text", sysUserId);
        // updateSiteInfo("widthAliquotLabels",
        // Float.toString(form.getWidthAliquotLabels()), "text", sysUserId);
        updateSiteInfo("heightBlockLabels", Float.toString(form.getHeightBlockLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("widthBlockLabels", Float.toString(form.getWidthBlockLabels()), "text", sysUserId, labelsDomain);
        updateSiteInfo("heightSlideLabels", Float.toString(form.getHeightSlideLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("widthSlideLabels", Float.toString(form.getWidthSlideLabels()), "text", sysUserId, labelsDomain);
        updateSiteInfo("heightFreezerLabels", Float.toString(form.getHeightFreezerLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("widthFreezerLabels", Float.toString(form.getWidthFreezerLabels()), "text", sysUserId,
                labelsDomain);

        updateSiteInfo("numMaxOrderLabels", Integer.toString(form.getNumMaxOrderLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("numMaxSpecimenLabels", Integer.toString(form.getNumMaxSpecimenLabels()), "text", sysUserId,
                labelsDomain);
        // updateSiteInfo("numMaxAliquotLabels",
        // Integer.toString(form.getNumMaxAliquotLabels()), "text", sysUserId);
        updateSiteInfo("numMaxSlideLabels", Integer.toString(form.getNumMaxSlideLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("numMaxBlockLabels", Integer.toString(form.getNumMaxBlockLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("numMaxFreezerLabels", Integer.toString(form.getNumMaxFreezerLabels()), "text", sysUserId,
                labelsDomain);

        updateSiteInfo("numDefaultOrderLabels", Integer.toString(form.getNumDefaultOrderLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("numDefaultSpecimenLabels", Integer.toString(form.getNumDefaultSpecimenLabels()), "text",
                sysUserId, labelsDomain);
        // updateSiteInfo("numDefaultAliquotLabels",
        // Integer.toString(form.getNumDefaultAliquotLabels()), "text",
        // sysUserId);
        updateSiteInfo("numDefaultSlideLabels", Integer.toString(form.getNumDefaultSlideLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("numDefaultBlockLabels", Integer.toString(form.getNumDefaultBlockLabels()), "text", sysUserId,
                labelsDomain);
        updateSiteInfo("numDefaultFreezerLabels", Integer.toString(form.getNumDefaultFreezerLabels()), "text",
                sysUserId, labelsDomain);

        updateSiteInfo("orderLabelPatientDob", Boolean.toString(form.getOrderPatientDobCheck()), "boolean", sysUserId,
                labelsDomain);

        updateSiteInfo("orderLabelPatientId", Boolean.toString(form.getOrderPatientIdCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("orderLabelPatientName", Boolean.toString(form.getOrderPatientNameCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("orderLabelSiteId", Boolean.toString(form.getOrderSiteIdCheck()), "boolean", sysUserId,
                labelsDomain);

        updateSiteInfo("specimenLabelPatientDob", Boolean.toString(form.getSpecimenPatientDobCheck()), "boolean",
                sysUserId, labelsDomain);
        updateSiteInfo("specimenLabelPatientId", Boolean.toString(form.getSpecimenPatientIdCheck()), "boolean",
                sysUserId, labelsDomain);
        updateSiteInfo("specimenLabelPatientName", Boolean.toString(form.getSpecimenPatientNameCheck()), "boolean",
                sysUserId, labelsDomain);
        updateSiteInfo("specimenLabelCollectionDate", Boolean.toString(form.getSpecimenCollectionDateCheck()),
                "boolean", sysUserId, labelsDomain);
        updateSiteInfo("specimenLabelCollectedBy", Boolean.toString(form.getSpecimenCollectedByCheck()), "boolean",
                sysUserId, labelsDomain);
        updateSiteInfo("specimenLabelTests", Boolean.toString(form.getSpecimenTestsCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("specimenLabelPatientSex", Boolean.toString(form.getSpecimenPatientSexCheck()), "boolean",
                sysUserId, labelsDomain);

        updateSiteInfo("slideLabelPatientId", Boolean.toString(form.getSlidePatientIdCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("slideLabelSlideId", Boolean.toString(form.getSlideSlideIdCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("slideLabelStainType", Boolean.toString(form.getSlideStainTypeCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("slideLabelBlockId", Boolean.toString(form.getSlideBlockIdCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("slideLabelCaseNumber", Boolean.toString(form.getSlideCaseNumberCheck()), "boolean", sysUserId,
                labelsDomain);

        updateSiteInfo("blockLabelPatientId", Boolean.toString(form.getBlockPatientIdCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("blockLabelBlockId", Boolean.toString(form.getBlockBlockIdCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("blockLabelSpecimenType", Boolean.toString(form.getBlockSpecimenTypeCheck()), "boolean",
                sysUserId, labelsDomain);
        updateSiteInfo("blockLabelCaseNumber", Boolean.toString(form.getBlockCaseNumberCheck()), "boolean", sysUserId,
                labelsDomain);

        updateSiteInfo("freezerLabelPatientId", Boolean.toString(form.getFreezerPatientIdCheck()), "boolean", sysUserId,
                labelsDomain);
        updateSiteInfo("freezerLabelStorageLocation", Boolean.toString(form.getFreezerStorageLocationCheck()),
                "boolean", sysUserId, labelsDomain);
        updateSiteInfo("freezerLabelSpecimenType", Boolean.toString(form.getFreezerSpecimenTypeCheck()), "boolean",
                sysUserId, labelsDomain);
        updateSiteInfo("freezerLabelCollectionDate", Boolean.toString(form.getFreezerCollectionDateCheck()), "boolean",
                sysUserId, labelsDomain);
        updateSiteInfo("freezerLabelExpiryDate", Boolean.toString(form.getFreezerExpiryDateCheck()), "boolean",
                sysUserId, labelsDomain);

        updateSiteInfo("prePrintUseAltAccession", Boolean.toString(!form.getPrePrintDontUseAltAccession()), "boolean",
                sysUserId, labelsDomain);
        String altAccessionPrefix = form.getPrePrintAltAccessionPrefix() == null ? ""
                : form.getPrePrintAltAccessionPrefix().trim();
        updateSiteInfo("prePrintAltAccessionPrefix", altAccessionPrefix, "text", sysUserId, labelsDomain);
    }

    /**
     * Persist a bar code configuration value in the database under site_information
     *
     * @param name      The name in the database
     * @param value     The new value to save
     * @param valueType The type of the value to save
     */
    private void updateSiteInfo(String name, String value, String valueType, String sysUserId,
            SiteInformationDomain siteInformationDomain) {
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
            siteInformation.setDomain(siteInformationDomain);
            siteInformationService.insert(siteInformation);
        } else {
            siteInformation.setValue(value);
            siteInformation.setSysUserId(sysUserId);
            siteInformation.setDomain(siteInformationDomain);
            siteInformationService.update(siteInformation);
        }
    }
}
