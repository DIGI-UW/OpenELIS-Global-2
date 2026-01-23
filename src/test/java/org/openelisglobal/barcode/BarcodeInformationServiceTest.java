package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.barcode.service.BarcodeInformationService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

public class BarcodeInformationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BarcodeInformationService barcodeInformationService;
    @Autowired
    private SiteInformationService siteInformationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/barcode-information.xml");
    }

    @Test
    public void updateBarcodeInfoFromForm() {
        BarcodeConfigurationForm barcodeConfigurationForm = getBarcodeConfigurationForm();

        List<SiteInformation> siteInformationList = siteInformationService.getAll();
        assertNotNull(siteInformationList);
        assertFalse(siteInformationList.stream().anyMatch(si -> "heightOrderLabels".equals(si.getName())));
        SiteInformation heightSiteInformation = siteInformationService.getSiteInformationByName("heightOrderLabels");
        assertNull(heightSiteInformation);

        assertTrue(siteInformationList.stream().anyMatch(si -> "widthSlideLabels".equals(si.getName())));
        SiteInformation silideLabelSiteInformation = siteInformationService
                .getSiteInformationByName("widthSlideLabels");
        assertNotNull(silideLabelSiteInformation);
        assertEquals("56", silideLabelSiteInformation.getValue());

        assertFalse(siteInformationList.stream().anyMatch(si -> "widthOrderLabels".equals(si.getName())));
        SiteInformation widthSiteInformation = siteInformationService.getSiteInformationByName("widthOrderLabels");
        assertNull(widthSiteInformation);

        assertFalse(siteInformationList.stream().anyMatch(si -> "numMaxSpecimenLabels".equals(si.getName())));
        SiteInformation spacemenSiteInformation = siteInformationService
                .getSiteInformationByName("numMaxSpecimenLabels");
        assertNull(spacemenSiteInformation);

        barcodeInformationService.updateBarcodeInfoFromForm(barcodeConfigurationForm, "8602");

        List<SiteInformation> updatedSiteInformationList = siteInformationService.getAll();
        assertNotNull(updatedSiteInformationList);
        assertTrue(updatedSiteInformationList.stream().anyMatch(si -> "heightOrderLabels".equals(si.getName())));
        SiteInformation updatedHeightSiteInformation = siteInformationService
                .getSiteInformationByName("heightOrderLabels");
        assertNotNull(updatedHeightSiteInformation);
        assertEquals("32.0", updatedHeightSiteInformation.getValue());

        assertTrue(updatedSiteInformationList.stream().anyMatch(si -> "widthOrderLabels".equals(si.getName())));
        SiteInformation updatedWidthSiteInformation = siteInformationService
                .getSiteInformationByName("widthOrderLabels");
        assertNotNull(updatedWidthSiteInformation);
        assertEquals("67.0", updatedWidthSiteInformation.getValue());

        assertTrue(updatedSiteInformationList.stream().anyMatch(si -> "numMaxSpecimenLabels".equals(si.getName())));
        SiteInformation updatedSpacemenSiteInformation = siteInformationService
                .getSiteInformationByName("numMaxSpecimenLabels");
        assertNotNull(updatedSpacemenSiteInformation);
        assertEquals("90", updatedSpacemenSiteInformation.getValue());
    }

    private static BarcodeConfigurationForm getBarcodeConfigurationForm() {
        BarcodeConfigurationForm barcodeConfigurationForm = new BarcodeConfigurationForm();
        barcodeConfigurationForm.setHeightOrderLabels(32);
        barcodeConfigurationForm.setWidthOrderLabels(67);
        barcodeConfigurationForm.setWidthSpecimenLabels(49);
        barcodeConfigurationForm.setHeightBlockLabels(23);
        barcodeConfigurationForm.setWidthBlockLabels(78);
        barcodeConfigurationForm.setHeightSlideLabels(56);
        barcodeConfigurationForm.setWidthSlideLabels(29);

        barcodeConfigurationForm.setNumMaxOrderLabels(78);
        barcodeConfigurationForm.setNumMaxSpecimenLabels(90);

        barcodeConfigurationForm.setNumDefaultOrderLabels(56);
        barcodeConfigurationForm.setNumDefaultSpecimenLabels(87);

        barcodeConfigurationForm.setCollectionDateCheck(true);
        barcodeConfigurationForm.setCollectedByCheck(false);
        barcodeConfigurationForm.setPatientSexCheck(true);
        barcodeConfigurationForm.setTestsCheck(true);
        barcodeConfigurationForm.setPrePrintDontUseAltAccession(false);
        barcodeConfigurationForm.setPrePrintAltAccessionPrefix("Before Print Form");
        return barcodeConfigurationForm;
    }
}
