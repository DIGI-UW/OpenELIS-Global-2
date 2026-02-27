package org.openelisglobal.patient.merge.dao;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.merge.valueholder.PatientMergeAudit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class PatientMergeAuditDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PatientMergeAuditDAO patientMergeAuditDAO;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @After
    public void tearDown() throws Exception {
        jdbcTemplate.update("DELETE FROM patient_merge_audit WHERE id > 0");
    }

    @Test
    public void testInsertPatientMergeAudit() {
        Long primaryPatientId = createTestPatient();
        Long mergedPatientId = createTestPatient();
        Long userId = createTestSystemUser();

        PatientMergeAudit audit = new PatientMergeAudit();
        audit.setPrimaryPatientId(primaryPatientId);
        audit.setMergedPatientId(mergedPatientId);
        audit.setMergeDate(new Timestamp(System.currentTimeMillis()));
        audit.setPerformedByUserId(userId);
        audit.setReason("Test merge");

        Long id = patientMergeAuditDAO.insert(audit);
        assertNotNull("Inserted audit should have ID", id);

        PatientMergeAudit retrieved = patientMergeAuditDAO.get(id).orElse(null);
        assertNotNull("Should retrieve inserted audit", retrieved);
        assertEquals("Primary patient ID should match", primaryPatientId, retrieved.getPrimaryPatientId());
        assertEquals("Merged patient ID should match", mergedPatientId, retrieved.getMergedPatientId());
    }

    @Test
    public void testFindByIdReturnsEmptyWhenNotFound() {
        assertTrue("Should return empty for non-existent ID", patientMergeAuditDAO.get(99999L).isEmpty());
    }

    @Test
    public void testFindByPrimaryPatientId() {
        Long primaryPatientId = createTestPatient();
        Long mergedPatientId = createTestPatient();
        Long userId = createTestSystemUser();

        PatientMergeAudit audit = new PatientMergeAudit();
        audit.setPrimaryPatientId(primaryPatientId);
        audit.setMergedPatientId(mergedPatientId);
        audit.setMergeDate(new Timestamp(System.currentTimeMillis()));
        audit.setPerformedByUserId(userId);
        audit.setReason("Test find by primary patient");

        Long id = patientMergeAuditDAO.insert(audit);
        assertNotNull("Should insert audit", id);

        // Verify the audit can be found by primary patient ID
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.patient_merge_audit WHERE primary_patient_id = ?", Integer.class,
                primaryPatientId);
        assertEquals("Should find one audit for primary patient", Integer.valueOf(1), count);
    }

    private Long createTestPatient() {
        Long personId = jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.person (id) VALUES (nextval('clinlims.person_seq')) RETURNING id", Long.class);
        return jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.patient (id, person_id) VALUES (nextval('clinlims.patient_seq'), ?) RETURNING id",
                Long.class, personId);
    }

    private Long createTestSystemUser() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.system_user (id, login_name, first_name, last_name, is_active, is_employee) VALUES (nextval('clinlims.system_user_seq'), ?, 'Test', 'User', ?, ?) RETURNING id",
                Long.class, "test" + System.currentTimeMillis() % 100000, "Y", "Y");
    }
}
