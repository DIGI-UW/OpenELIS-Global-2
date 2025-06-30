package org.openelisglobal.renametestsection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.renametestsection.service.RenameTestSectionService;
import org.openelisglobal.renametestsection.valueholder.RenameTestSection;
import org.springframework.beans.factory.annotation.Autowired;

public class RenameTestSectionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    RenameTestSectionService renameTestSectionService;

    private List<RenameTestSection> testSections;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-section.xml");
    }

    @Test
    public void testGetData() {
        RenameTestSection testSection = renameTestSectionService.get("3");
        renameTestSectionService.getData(testSection);
        assertEquals("Toxicology", testSection.getTestSectionName());
        assertEquals("Toxic substances analysis", testSection.getDescription());
        assertEquals(Timestamp.valueOf("2025-06-10 10:30:00"), testSection.getLastupdated());
        assertEquals("Y", testSection.getIsActive());
    }

    @Test
    public void testInsert_ThrowsLIMSRuntimeException_WithDuplicateRow() {
        RenameTestSection testSection = new RenameTestSection();
        testSection.setTestSectionName("Hematology");
        testSection.setDescription("Blood-related tests");
        testSection.setIsExternal("N");
        testSection.setLastupdated(Timestamp.valueOf("2025-06-10 10:00:00"));
        testSection.setSortOrderInt(1);
        testSection.setIsActive("Y");
        assertThrows(LIMSRuntimeException.class, () -> renameTestSectionService.insert(testSection));
    }

    @Test
    public void testGetTestSections_UsingFilter() {
        testSections = renameTestSectionService.getTestSections("Micro");
        assertEquals(1, testSections.size());
        assertEquals("2", testSections.get(0).getId());
    }

    @Test
    public void testGetTestSectionByName() {
        RenameTestSection testSection = new RenameTestSection();
        testSection.setTestSectionName("Microbiology");
        RenameTestSection newTestSection = renameTestSectionService.getTestSectionByName(testSection);
        assertEquals("2", newTestSection.getId());
        assertEquals("Infectious diseases", newTestSection.getDescription());
        assertEquals("Y", newTestSection.getIsActive());
    }

    @Test
    public void testGetPageOfTestSections_ReturnsTestSections() {
        testSections = renameTestSectionService.getPageOfTestSections(1);
        int expectedNumberOfPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testSections.size() <= expectedNumberOfPages);
    }

    @Test
    public void testGetTotalTestSectionCount() {
        assertEquals(3, renameTestSectionService.getTotalTestSectionCount().intValue());
    }

    @Test
    public void testGetAllTestSections_ReturnsAllTestSections() {
        testSections = renameTestSectionService.getAllTestSections();
        assertEquals(3, testSections.size());
        assertEquals("1", testSections.get(0).getId());
        assertEquals("2", testSections.get(1).getId());
        assertEquals("3", testSections.get(2).getId());
    }

    @Test
    public void testGetTestSectionById_ReturnsRestSectionWithGivenId() {
        RenameTestSection testSection = renameTestSectionService.getTestSectionById("3");
        assertEquals("Toxicology", testSection.getTestSectionName());
        assertEquals("Toxic substances analysis", testSection.getDescription());
        assertEquals("Y", testSection.getIsActive());
    }

    @Test
    public void testGetLocalizationForRenameTestSection() {
        Localization localization = renameTestSectionService.getLocalizationForRenameTestSection("3");
        assertEquals("203", localization.getId());
        assertEquals("Test Description 3", localization.getDescription());
    }
}
