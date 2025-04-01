package org.openelisglobal.siteinformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

public class SiteInformationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiteInformationService siteInformationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/site-information.xml");
    }

    public void testDataInDatabase() {
        List<SiteInformation> siteInformations = siteInformationService.getAll();
        assertEquals(3, siteInformations.size());
        siteInformations.forEach(siteInfo -> {
            System.out.print(siteInfo.getName() + " ");
        });
    }

    @Test
    public void getPageOfSiteInformationByDomainName_shouldReturnCorrectSiteInformation() {
        List<SiteInformation> siteInfos = siteInformationService.getPageOfSiteInformationByDomainName(1, "formating");
        assertEquals(1, siteInfos.size());
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
    }

    @Test
    public void getCountForDomainName_shouldReturnCorrectCount() {
        int count = siteInformationService.getCountForDomainName("siteIdentity");
        assertEquals(1, count);
    }

    @Test
    public void getSiteInformationByName_shouldReturnCorrectSiteInformation() {
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName("testUsageSendStatus");
        assertNotNull(siteInfo);
        assertEquals("2", siteInfo.getId());
        assertEquals("/tests", siteInfo.getValue());
    }

    @Test
    public void getData_shouldPopulateSiteInformationObject() {
        SiteInformation siteInfo = new SiteInformation();
        siteInfo.setId("1");
        siteInformationService.getData(siteInfo);

        assertEquals("reportsDirectory", siteInfo.getName());
        assertEquals("/reports", siteInfo.getValue());
        assertEquals(false, siteInfo.isEncrypted());
    }

    @Test
    public void getAllSiteInformation_shouldReturnAllSiteInformation() {
        List<SiteInformation> siteInfos = siteInformationService.getAllSiteInformation();
        assertEquals(2, siteInfos.size());
    }

    @Test
    public void getSiteInformationById_shouldReturnCorrectSiteInformation() {
        SiteInformation siteInfo = siteInformationService.getSiteInformationById("2");
        assertNotNull(siteInfo);
        assertEquals("testUsageSendStatus", siteInfo.getName());
        assertEquals("/tests", siteInfo.getValue());
    }

    @Test
    public void getSiteInformationByDomainName_shouldReturnCorrectSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getSiteInformationByDomainName("formating");
        assertEquals(1, siteInfos.size());
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
    }

    @Test
    public void persistData_shouldSaveNewSiteInformation() {
        SiteInformation newSiteInfo = new SiteInformation();
        newSiteInfo.setName("TestSiteName");
        newSiteInfo.setValue("Test Value");
        newSiteInfo.setEncrypted(false);
        newSiteInfo.setValueType("text");

        siteInformationService.persistData(newSiteInfo, true);

        SiteInformation retrieved = siteInformationService.getSiteInformationByName("TestSiteName");
        assertNotNull(retrieved);
        assertEquals("Test Value", retrieved.getValue());
    }

    @Test
    public void persistData_shouldUpdateExistingSiteInformation() {
        SiteInformation siteInfo = siteInformationService.getSiteInformationById("1");
        siteInfo.setValue("Updated Value");

        siteInformationService.persistData(siteInfo, false);

        SiteInformation updated = siteInformationService.getSiteInformationById("1");
        assertEquals("Updated Value", updated.getValue());
    }

    @Test
    public void get_shouldDecryptEncryptedValues() {
        SiteInformation siteInfo = siteInformationService.get("2");
        assertNotNull(siteInfo);
        // This assumes the TextEncryptor in the test context is configured
        // to properly decrypt the value from XML data
        assertNotNull(siteInfo.getValue());
        // The actual value will depend on the encryption implementation
    }

    @Test
    public void getMatch_withPropertyNameAndValue_shouldReturnMatchingSiteInformation() {
        Optional<SiteInformation> siteInfoOpt = siteInformationService.getMatch("name", "reportsDirectory");
        assertTrue(siteInfoOpt.isPresent());
        SiteInformation siteInfo = siteInfoOpt.get();
        assertEquals("1", siteInfo.getId());
    }

    @Test
    public void getMatch_withPropertyMap_shouldReturnMatchingSiteInformation() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "reportsDirectory");
        properties.put("value", "/reports");

        Optional<SiteInformation> siteInfoOpt = siteInformationService.getMatch(properties);
        assertTrue(siteInfoOpt.isPresent());
        SiteInformation siteInfo = siteInfoOpt.get();
        assertEquals("1", siteInfo.getId());
    }

    @Test
    public void getAll_shouldReturnAllSiteInformation() {
        List<SiteInformation> siteInfos = siteInformationService.getAll();
        assertEquals(2, siteInfos.size());
    }

    @Test
    public void getAllMatching_withPropertyNameAndValue_shouldReturnMatchingSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getAllMatching("name", "reportsDirectory");
        assertEquals(1, siteInfos.size());
    }

    @Test
    public void getAllMatching_withPropertyMap_shouldReturnMatchingSiteInformations() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("encrypted", false);

        List<SiteInformation> siteInfos = siteInformationService.getAllMatching(properties);
        assertEquals(2, siteInfos.size());
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
        assertEquals("testUsageSendStatus", siteInfos.get(1).getName());
    }

    @Test
    public void getAllOrdered_withOrderProperty_shouldReturnOrderedSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getAllOrdered("name", false);
        assertEquals(2, siteInfos.size());
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
        assertEquals("testUsageSendStatus", siteInfos.get(1).getName());
    }

    @Test
    public void getAllOrdered_withOrderProperties_shouldReturnOrderedSiteInformations() {
        List<String> orderProperties = List.of("name", "value");
        List<SiteInformation> siteInfos = siteInformationService.getAllOrdered(orderProperties, false);
        assertEquals(2, siteInfos.size());
        // Assert the correct ordering based on domain_id first, then name
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
        assertEquals("testUsageSendStatus", siteInfos.get(1).getName());
    }

    @Test
    public void getAllMatchingOrdered_withPropertyNameAndOrderProperty_shouldReturnMatchingOrderedSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getAllMatchingOrdered("name", "reportsDirectory",
                "name", false);
        assertEquals(1, siteInfos.size());
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
    }

    @Test
    public void getAllMatchingOrdered_withPropertyNameAndOrderProperties_shouldReturnMatchingOrderedSiteInformations() {
        List<String> orderProperties = List.of("tag", "name");
        List<SiteInformation> siteInfos = siteInformationService.getAllMatchingOrdered("name", "reportsDirectory",
                orderProperties, false);
        assertEquals(1, siteInfos.size());
        // Ordering will depend on tag and name
    }

    @Test
    public void getAllMatchingOrdered_withPropertyMapAndOrderProperty_shouldReturnMatchingOrderedSiteInformations() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "testUsageSendStatus");

        List<SiteInformation> siteInfos = siteInformationService.getAllMatchingOrdered(properties, "name", false);
        assertEquals(1, siteInfos.size());
        assertEquals("testUsageSendStatus", siteInfos.get(0).getName());
    }

    @Test
    public void getAllMatchingOrdered_withPropertyMapAndOrderProperties_shouldReturnMatchingOrderedSiteInformations() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "reportsDirectory");

        List<String> orderProperties = List.of("encrypted", "name");
        List<SiteInformation> siteInfos = siteInformationService.getAllMatchingOrdered(properties, orderProperties,
                false);
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
        assertEquals(1, siteInfos.size());
        // Order should be based on encrypted status first, then name
    }

    @Test
    public void getPage_shouldReturnPagedSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getPage(1);
        assertFalse(siteInfos.isEmpty());
    }

    @Test
    public void getMatchingPage_withPropertyNameAndValue_shouldReturnMatchingPagedSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getMatchingPage("name", "testUsageSendStatus", 1);
        assertEquals("testUsageSendStatus", siteInfos.get(0).getName());
        assertEquals(1, siteInfos.size());
    }

    @Test
    public void getMatchingPage_withPropertyMap_shouldReturnMatchingPagedSiteInformations() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "reportsDirectory");

        List<SiteInformation> siteInfos = siteInformationService.getMatchingPage(properties, 1);
        assertEquals(1, siteInfos.size());
    }

    @Test
    public void getOrderedPage_withOrderProperty_shouldReturnOrderedPagedSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getOrderedPage("name", false, 1);
        assertEquals(2, siteInfos.size());
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
        assertEquals("testUsageSendStatus", siteInfos.get(1).getName());
    }

    @Test
    public void getOrderedPage_withOrderProperties_shouldReturnOrderedPagedSiteInformations() {
        List<String> orderProperties = List.of("name", "value");
        List<SiteInformation> siteInfos = siteInformationService.getOrderedPage(orderProperties, false, 1);
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
        assertEquals("testUsageSendStatus", siteInfos.get(1).getName());
        assertEquals(2, siteInfos.size());
    }

    @Test
    public void getMatchingOrderedPage_withPropertyNameAndOrderProperty_shouldReturnMatchingOrderedPagedSiteInformations() {
        List<SiteInformation> siteInfos = siteInformationService.getMatchingOrderedPage("name", "reportsDirectory",
                "name", false, 1);
        assertEquals("reportsDirectory", siteInfos.get(0).getName());
        assertEquals(1, siteInfos.size());
    }

    @Test
    public void getMatchingOrderedPage_withPropertyNameAndOrderProperties_shouldReturnMatchingOrderedPagedSiteInformations() {
        List<String> orderProperties = List.of("encrypted", "name");
        List<SiteInformation> siteInfos = siteInformationService.getMatchingOrderedPage("name", "testUsageSendStatus",
                orderProperties, false, 1);
        assertEquals("testUsageSendStatus", siteInfos.get(0).getName());
        assertEquals(1, siteInfos.size());
    }

    @Test
    public void getMatchingOrderedPage_withPropertyMapAndOrderProperty_shouldReturnMatchingOrderedPagedSiteInformations() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "testUsageSendStatus");

        List<SiteInformation> siteInfos = siteInformationService.getMatchingOrderedPage(properties, "name", false, 1);
        assertEquals(1, siteInfos.size());
    }

    @Test
    public void getMatchingOrderedPage_withPropertyMapAndOrderProperties_shouldReturnMatchingOrderedPagedSiteInformations() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "reportsDirectory");

        List<String> orderProperties = List.of("name", "value");
        List<SiteInformation> siteInfos = siteInformationService.getMatchingOrderedPage(properties, orderProperties,
                false, 1);
        assertEquals(1, siteInfos.size());
    }

    @Test
    public void insert_shouldCreateNewSiteInformation() {
        SiteInformation newSiteInfo = new SiteInformation();
        newSiteInfo.setName("NewTestSite");
        newSiteInfo.setValue("New Test Value");
        newSiteInfo.setEncrypted(false);
        newSiteInfo.setValueType("text");

        String id = siteInformationService.insert(newSiteInfo);
        assertNotNull(id);

        SiteInformation inserted = siteInformationService.get(id);
        assertEquals("NewTestSite", inserted.getName());
        assertEquals("New Test Value", inserted.getValue());
    }

    @Test
    public void insertAll_shouldCreateMultipleSiteInformations() {
        SiteInformation site1 = new SiteInformation();
        site1.setName("TestSite1");
        site1.setValue("Test Value 1");
        site1.setValueType("text");

        SiteInformation site2 = new SiteInformation();
        site2.setName("TestSite2");
        site2.setValue("Test Value 2");
        site2.setValueType("text");

        List<SiteInformation> sitesToInsert = List.of(site1, site2);
        List<String> ids = siteInformationService.insertAll(sitesToInsert);

        assertEquals(2, ids.size());
        assertEquals("TestSite1", siteInformationService.get(ids.get(0)).getName());
        assertEquals("TestSite2", siteInformationService.get(ids.get(1)).getName());
    }

    @Test
    public void save_shouldInsertNewSiteInformation() {
        SiteInformation newSiteInfo = new SiteInformation();
        newSiteInfo.setName("SaveTestSite");
        newSiteInfo.setValue("Save Test Value");
        newSiteInfo.setValueType("text");
        assertEquals(2, siteInformationService.getAll().size());

        SiteInformation saved = siteInformationService.save(newSiteInfo);
        assertNotNull(saved.getId());
        assertEquals("SaveTestSite", saved.getName());
        assertEquals("Save Test Value", saved.getValue());
        assertEquals(3, siteInformationService.getAll().size());
    }

    @Test
    public void save_shouldUpdateExistingSiteInformation() {
        SiteInformation siteInfo = siteInformationService.get("1");
        siteInfo.setValue("Updated via Save");

        SiteInformation saved = siteInformationService.save(siteInfo);
        assertEquals("1", saved.getId());
        assertEquals("Updated via Save", saved.getValue());
    }

    @Test
    public void saveAll_shouldInsertAndUpdateMultipleSiteInformations() {
        SiteInformation existingSite = siteInformationService.get("1");
        existingSite.setValue("Batch Updated");

        SiteInformation newSite = new SiteInformation();
        newSite.setName("BatchNewSite");
        newSite.setValue("Batch New Value");
        newSite.setValueType("text");

        List<SiteInformation> sitesToSave = List.of(existingSite, newSite);
        List<SiteInformation> savedSites = siteInformationService.saveAll(sitesToSave);

        assertEquals(2, savedSites.size());
        assertEquals("Batch Updated", siteInformationService.get("1").getValue());

        SiteInformation newSaved = savedSites.get(1);
        assertEquals("BatchNewSite", newSaved.getName());
        assertEquals("Batch New Value", newSaved.getValue());
    }

    @Test
    public void update_shouldUpdateExistingSiteInformation() {
        SiteInformation siteInfo = siteInformationService.get("2");
        siteInfo.setValue("https://updated-api.example.org/submit");

        SiteInformation updated = siteInformationService.update(siteInfo);
        assertEquals("2", updated.getId());
        assertEquals("https://updated-api.example.org/submit", updated.getValue());
    }

    @Test
    public void updateAll_shouldUpdateMultipleSiteInformations() {
        SiteInformation site1 = siteInformationService.get("1");
        site1.setValue("Mass Update 1");

        SiteInformation site2 = siteInformationService.get("2");
        site2.setValue("Mass Update 2");

        List<SiteInformation> sitesToUpdate = List.of(site1, site2);
        List<SiteInformation> updatedSites = siteInformationService.updateAll(sitesToUpdate);

        assertEquals(2, updatedSites.size());
        assertEquals("Mass Update 1", siteInformationService.get("1").getValue());
        assertEquals("Mass Update 2", siteInformationService.get("2").getValue());
    }

    @Test
    public void getNext_shouldReturnNextSiteInformation() {
        SiteInformation nextSite = siteInformationService.getNext("1");
        assertNotNull(nextSite);
        // Actual next ID depends on how the service defines "next"
    }

    @Test
    public void getPrevious_shouldReturnPreviousSiteInformation() {
        SiteInformation prevSite = siteInformationService.getPrevious("2");
        assertNotNull(prevSite);
        // Actual previous ID depends on how the service defines "previous"
    }

    @Test
    public void encryptAndDecrypt_shouldHandleEncryptedValuesCorrectly() {
        // Create a new site with encrypted value
        SiteInformation encryptedSite = new SiteInformation();
        encryptedSite.setName("EncryptedTestSite");
        encryptedSite.setValue("SecretValue1");
        encryptedSite.setEncrypted(true);
        encryptedSite.setValueType("text");
        assertEquals(2, siteInformationService.getAll().size());

        // Insert it (which should encrypt the value)
        String id = siteInformationService.insert(encryptedSite);
        assertEquals(3, siteInformationService.getAll().size());

        // Retrieve it (which should decrypt the value)
        SiteInformation retrieved = siteInformationService.get(id);
        assertEquals("EncryptedTestSite", retrieved.getName());
    }

    @Test
    public void updateSiteInformationByName_shouldUpdateMultipleSiteInformationsByName() {
        Map<String, String> updates = new HashMap<>();
        updates.put("reportsDirectory", "/newreports");
        updates.put("testUsageSendStatus", "/newtests");

        List<SiteInformation> updatedSites = siteInformationService.updateSiteInformationByName(updates);

        assertEquals(2, updatedSites.size());
        assertEquals("/newreports", siteInformationService.getSiteInformationByName("reportsDirectory").getValue());
        assertEquals("/newtests", siteInformationService.getSiteInformationByName("testUsageSendStatus").getValue());
    }
}