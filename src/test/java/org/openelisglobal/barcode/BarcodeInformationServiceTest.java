package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.barcode.service.BarcodeConfigService;
import org.openelisglobal.barcode.util.BarcodeConfigUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

public class BarcodeInformationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BarcodeConfigService barcodeInformationService;
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

        barcodeConfigurationForm.setSpecimenCollectionDateCheck(true);
        barcodeConfigurationForm.setSpecimenCollectedByCheck(false);
        barcodeConfigurationForm.setSpecimenPatientSexCheck(true);
        barcodeConfigurationForm.setSpecimenTestsCheck(true);
        barcodeConfigurationForm.setPrePrintDontUseAltAccession(false);
        barcodeConfigurationForm.setPrePrintAltAccessionPrefix("Before Print Form");
        return barcodeConfigurationForm;
    }

    @Test
    public void barcodeLabelInfoMessageKeys_ShouldExistInEnAndFrBundles() {
        String[] keys = new String[] { "barcode.label.info.blockNumber", "barcode.label.info.slideNumber",
                "barcode.label.info.specimenType", "barcode.label.info.blockId", "barcode.label.info.caseNumber",
                "barcode.label.info.storageLocation", "barcode.label.info.collectionDate",
                "barcode.label.info.expiryDate" };

        for (String key : keys) {
            String enMessage = MessageUtil.getMessage(key, Locale.ENGLISH);
            String frMessage = MessageUtil.getMessage(key, Locale.FRENCH);
            assertFalse("English bundle missing key: " + key, MessageUtil.messageNotFound(enMessage, key));
            assertFalse("French bundle missing key: " + key, MessageUtil.messageNotFound(frMessage, key));
        }
    }

    @Test
    public void barcodeConfigUtil_ShouldFallbackForMalformedNumericValues() {
        assertEquals("Malformed integer should fallback", 10, BarcodeConfigUtil.parseIntSafe("abc", 10));
        assertEquals("Negative integer string is still parsable", -1, BarcodeConfigUtil.parseIntSafe("-1", 10));
        assertEquals("Malformed float should fallback", 2.0f, BarcodeConfigUtil.parseFloatSafe("xyz", 2.0f), 0.0f);
    }
}
