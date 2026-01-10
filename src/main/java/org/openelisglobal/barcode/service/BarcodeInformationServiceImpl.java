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
public class BarcodeInformationServiceImpl implements BarcodeInformationService {

        @Autowired
        private SiteInformationService siteInformationService;
        @Autowired
        private SiteInformationDomainService siteInformationDomainService;

        @Override
        @Transactional
        public void updateBarcodeInfoFromForm(BarcodeConfigurationForm form, String sysUserId) {
                updateSiteInfo("heightOrderLabels", Float.toString(form.getHeightOrderLabels()), "text", sysUserId);
                updateSiteInfo("widthOrderLabels", Float.toString(form.getWidthOrderLabels()), "text", sysUserId);
                updateSiteInfo("heightSpecimenLabels", Float.toString(form.getHeightSpecimenLabels()), "text",
                                sysUserId);
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
                updateSiteInfo("numMaxSpecimenLabels", Integer.toString(form.getNumMaxSpecimenLabels()), "text",
                                sysUserId);
                // updateSiteInfo("numMaxAliquotLabels",
                // Integer.toString(form.getNumMaxAliquotLabels()), "text", sysUserId);
                updateSiteInfo("numMaxSlideLabels", Integer.toString(form.getNumMaxSlideLabels()), "text", sysUserId);
                updateSiteInfo("numMaxBlockLabels", Integer.toString(form.getNumMaxBlockLabels()), "text", sysUserId);
                updateSiteInfo("numMaxFreezerLabels", Integer.toString(form.getNumMaxFreezerLabels()), "text",
                                sysUserId);

                updateSiteInfo("numDefaultOrderLabels", Integer.toString(form.getNumDefaultOrderLabels()), "text",
                                sysUserId);
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

                updateSiteInfo("orderLabelPatientDob", Boolean.toString(form.getOrderPatientDobCheck()), "boolean",
                                sysUserId);

                updateSiteInfo("orderLabelPatientId", Boolean.toString(form.getOrderPatientIdCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("orderLabelPatientName", Boolean.toString(form.getOrderPatientNameCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("orderLabelSiteId", Boolean.toString(form.getOrderSiteIdCheck()), "boolean",
                                sysUserId);

                updateSiteInfo("specimenLabelPatientDob", Boolean.toString(form.getSpecimenPatientDobCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("specimenLabelPatientId", Boolean.toString(form.getSpecimenPatientIdCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("specimenLabelPatientName", Boolean.toString(form.getSpecimenPatientNameCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("specimenLabelCollectionDate",
                                Boolean.toString(form.getSpecimenCollectionDateCheck()),
                                "boolean", sysUserId);
                updateSiteInfo("specimenLabelCollectedBy", Boolean.toString(form.getSpecimenCollectedByCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("specimenLabelTests", Boolean.toString(form.getSpecimenTestsCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("specimenLabelPatientSex", Boolean.toString(form.getSpecimenPatientSexCheck()),
                                "boolean",
                                sysUserId);

                updateSiteInfo("slideLabelPatientId", Boolean.toString(form.getSlidePatientIdCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("slideLabelSlideId", Boolean.toString(form.getSlideSlideIdCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("slideLabelStainType", Boolean.toString(form.getSlideStainTypeCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("slideLabelBlockId", Boolean.toString(form.getSlideBlockIdCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("slideLabelCaseNumber", Boolean.toString(form.getSlideCaseNumberCheck()), "boolean",
                                sysUserId);

                updateSiteInfo("blockLabelPatientId", Boolean.toString(form.getBlockPatientIdCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("blockLabelBlockId", Boolean.toString(form.getBlockBlockIdCheck()), "boolean",
                                sysUserId);
                updateSiteInfo("blockLabelSpecimenType", Boolean.toString(form.getBlockSpecimenTypeCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("blockLabelCaseNumber", Boolean.toString(form.getBlockCaseNumberCheck()),
                                "boolean",
                                sysUserId);

                updateSiteInfo("freezerLabelPatientId", Boolean.toString(form.getFreezerPatientIdCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("freezerLabelStorageLocation",
                                Boolean.toString(form.getFreezerStorageLocationCheck()),
                                "boolean", sysUserId);
                updateSiteInfo("freezerLabelSpecimenType", Boolean.toString(form.getFreezerSpecimenTypeCheck()),
                                "boolean",
                                sysUserId);
                updateSiteInfo("freezerLabelCollectionDate",
                                Boolean.toString(form.getFreezerCollectionDateCheck()),
                                "boolean", sysUserId);
                updateSiteInfo("freezerLabelExpiryDate", Boolean.toString(form.getFreezerExpiryDateCheck()),
                                "boolean",
                                sysUserId);

                updateSiteInfo("prePrintUseAltAccession", Boolean.toString(!form.getPrePrintDontUseAltAccession()),
                                "boolean",
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
                SiteInformationDomain siteInformationDomain = siteInformationDomainService.getByName("labels");
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
