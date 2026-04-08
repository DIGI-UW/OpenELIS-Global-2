package org.openelisglobal.patient.merge.dao;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.merge.valueholder.PatientMergeAudit;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientMergeAuditDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientMergeAuditDAO patientMergeAuditDAO;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        cleanRowsInCurrentConnection(new String[] { "patient_merge_audit" });
        executeDataSetWithStateManagement("testdata/person.xml");
        executeDataSetWithStateManagement("testdata/patient.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    @Test
    public void testInsertPatientMergeAudit() {
        PatientMergeAudit audit = new PatientMergeAudit();
        audit.setPrimaryPatientId(1L);
        audit.setMergedPatientId(2L);
        audit.setMergeDate(new Timestamp(System.currentTimeMillis()));
        audit.setPerformedByUserId(1L);
        audit.setReason("Test merge");

        Long id = patientMergeAuditDAO.insert(audit);
        assertNotNull("Inserted audit should have ID", id);

        PatientMergeAudit retrieved = patientMergeAuditDAO.get(id).orElse(null);
        assertNotNull("Should retrieve inserted audit", retrieved);
        assertEquals("Primary patient ID should match", Long.valueOf(1L), retrieved.getPrimaryPatientId());
        assertEquals("Merged patient ID should match", Long.valueOf(2L), retrieved.getMergedPatientId());
    }

    @Test
    public void testFindByIdReturnsEmptyWhenNotFound() {
        assertTrue("Should return empty for non-existent ID", patientMergeAuditDAO.get(99999L).isEmpty());
    }

   @Test
    public void testInsertAndRetrieveAudit() {
        PatientMergeAudit audit = new PatientMergeAudit();
        audit.setPrimaryPatientId(1L);
        audit.setMergedPatientId(2L);
        audit.setMergeDate(new Timestamp(System.currentTimeMillis()));
        audit.setPerformedByUserId(1L);
        audit.setReason("Test insert and retrieve audit");

        Long id = patientMergeAuditDAO.insert(audit);
        assertNotNull("Should insert audit", id);

        PatientMergeAudit retrieved = patientMergeAuditDAO.get(id).orElse(null);
        assertNotNull("Should retrieve audit", retrieved);
        assertEquals("Primary patient ID should match", Long.valueOf(1L), retrieved.getPrimaryPatientId());
    }
}
