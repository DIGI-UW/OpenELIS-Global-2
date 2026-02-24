package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import org.junit.Test;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentLabUnit;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentTestMap;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;

/**
 * ORM validation test for EQA Enrollment entities (DM-007 through DM-010).
 * Validates all 4 new entities can be instantiated and have correct defaults.
 * Constitution V.4: Must execute in less than 5 seconds, NO database connection
 * required.
 */
public class EQAEnrollmentHibernateMappingTest {

    @Test
    public void testEQAProgramEnrollmentCanBeInstantiated() {
        EQAProgramEnrollment enrollment = new EQAProgramEnrollment();
        assertNotNull("EQAProgramEnrollment should be instantiable", enrollment);
        assertEquals("status should default to Active", "Active", enrollment.getStatus());
        assertNull("id should be null before persist", enrollment.getId());
    }

    @Test
    public void testEQAProgramEnrollmentFieldsCanBeSet() {
        EQAProgramEnrollment enrollment = new EQAProgramEnrollment();
        enrollment.setOrganizationId(1L);
        enrollment.setEnrollmentDate(new Date());
        enrollment.setStatus("Suspended");
        enrollment.setWithdrawalReason("Test reason");
        enrollment.setSysUserId("1");

        assertEquals(Long.valueOf(1L), enrollment.getOrganizationId());
        assertNotNull(enrollment.getEnrollmentDate());
        assertEquals("Suspended", enrollment.getStatus());
        assertEquals("Test reason", enrollment.getWithdrawalReason());
        assertEquals("1", enrollment.getSysUserId());
    }

    @Test
    public void testEQALabProgramEnrollmentCanBeInstantiated() {
        EQALabProgramEnrollment enrollment = new EQALabProgramEnrollment();
        assertNotNull("EQALabProgramEnrollment should be instantiable", enrollment);
        assertTrue("isActive should default to true", enrollment.getIsActive());
        assertNotNull("labUnits list should be initialized", enrollment.getLabUnits());
        assertNotNull("testMaps list should be initialized", enrollment.getTestMaps());
        assertTrue("labUnits list should be empty", enrollment.getLabUnits().isEmpty());
        assertTrue("testMaps list should be empty", enrollment.getTestMaps().isEmpty());
    }

    @Test
    public void testEQALabProgramEnrollmentFieldsCanBeSet() {
        EQALabProgramEnrollment enrollment = new EQALabProgramEnrollment();
        enrollment.setProgramName("WHO EQA Program");
        enrollment.setProvider("WHO");
        enrollment.setDescription("Test description");
        enrollment.setIsActive(true);
        enrollment.setCreatedDate(new Date());
        enrollment.setSysUserId("1");

        assertEquals("WHO EQA Program", enrollment.getProgramName());
        assertEquals("WHO", enrollment.getProvider());
        assertEquals("Test description", enrollment.getDescription());
        assertTrue(enrollment.getIsActive());
        assertNotNull(enrollment.getCreatedDate());
    }

    @Test
    public void testEQALabEnrollmentLabUnitCanBeInstantiated() {
        EQALabEnrollmentLabUnit labUnit = new EQALabEnrollmentLabUnit();
        assertNotNull("EQALabEnrollmentLabUnit should be instantiable", labUnit);
        assertNull("id should be null before persist", labUnit.getId());
        assertNull("enrollment should be null before set", labUnit.getEnrollment());
    }

    @Test
    public void testEQALabEnrollmentLabUnitFieldsCanBeSet() {
        EQALabEnrollmentLabUnit labUnit = new EQALabEnrollmentLabUnit();
        labUnit.setTestSectionId(42L);
        labUnit.setSysUserId("1");

        assertEquals(Long.valueOf(42L), labUnit.getTestSectionId());
        assertEquals("1", labUnit.getSysUserId());
    }

    @Test
    public void testEQALabEnrollmentTestMapCanBeInstantiated() {
        EQALabEnrollmentTestMap testMap = new EQALabEnrollmentTestMap();
        assertNotNull("EQALabEnrollmentTestMap should be instantiable", testMap);
        assertNull("testId should be null by default", testMap.getTestId());
        assertNull("panelId should be null by default", testMap.getPanelId());
    }

    @Test
    public void testEQALabEnrollmentTestMapWithTestId() {
        EQALabEnrollmentTestMap testMap = new EQALabEnrollmentTestMap();
        testMap.setTestId(10L);
        testMap.setSysUserId("1");

        assertEquals(Long.valueOf(10L), testMap.getTestId());
        assertNull("panelId should remain null for test mapping", testMap.getPanelId());
    }

    @Test
    public void testEQALabEnrollmentTestMapWithPanelId() {
        EQALabEnrollmentTestMap testMap = new EQALabEnrollmentTestMap();
        testMap.setPanelId(5L);
        testMap.setSysUserId("1");

        assertNull("testId should remain null for panel mapping", testMap.getTestId());
        assertEquals(Long.valueOf(5L), testMap.getPanelId());
    }

    @Test
    public void testEQALabProgramEnrollmentChildCollections() {
        EQALabProgramEnrollment enrollment = new EQALabProgramEnrollment();
        enrollment.setProgramName("Test");
        enrollment.setProvider("Provider");
        enrollment.setSysUserId("1");

        EQALabEnrollmentLabUnit labUnit = new EQALabEnrollmentLabUnit();
        labUnit.setEnrollment(enrollment);
        labUnit.setTestSectionId(1L);
        labUnit.setSysUserId("1");
        enrollment.getLabUnits().add(labUnit);

        EQALabEnrollmentTestMap testMap = new EQALabEnrollmentTestMap();
        testMap.setEnrollment(enrollment);
        testMap.setTestId(1L);
        testMap.setSysUserId("1");
        enrollment.getTestMaps().add(testMap);

        assertEquals(1, enrollment.getLabUnits().size());
        assertEquals(1, enrollment.getTestMaps().size());
        assertEquals(enrollment, enrollment.getLabUnits().get(0).getEnrollment());
        assertEquals(enrollment, enrollment.getTestMaps().get(0).getEnrollment());
    }
}
