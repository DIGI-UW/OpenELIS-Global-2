package org.openelisglobal.eqa.daoimpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.eqa.dao.EQALabProgramEnrollmentDAO;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQALabProgramEnrollmentDAOImpl extends BaseDAOImpl<EQALabProgramEnrollment, Long>
        implements EQALabProgramEnrollmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(EQALabProgramEnrollmentDAOImpl.class);

    public EQALabProgramEnrollmentDAOImpl() {
        super(EQALabProgramEnrollment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQALabProgramEnrollment> findAll() {
        try {
            String hql = "FROM EQALabProgramEnrollment e ORDER BY e.programName";
            Query<EQALabProgramEnrollment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQALabProgramEnrollment.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving all lab program enrollments", e);
            throw new LIMSRuntimeException("Error retrieving all lab program enrollments", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQALabProgramEnrollment> findByIsActive(Boolean isActive) {
        try {
            String hql = "FROM EQALabProgramEnrollment e WHERE e.isActive = :isActive ORDER BY e.programName";
            Query<EQALabProgramEnrollment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    EQALabProgramEnrollment.class);
            query.setParameter("isActive", isActive);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving lab program enrollments by active status: {}", isActive, e);
            throw new LIMSRuntimeException("Error retrieving lab program enrollments by active status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDistinctProviders() {
        try {
            String hql = "SELECT DISTINCT e.provider FROM EQALabProgramEnrollment e"
                    + " WHERE e.provider IS NOT NULL ORDER BY e.provider";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(hql, String.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error retrieving distinct providers from lab enrollments", e);
            throw new LIMSRuntimeException("Error retrieving distinct providers", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findEqaCoveredTestIds() {
        // Native, because the panel leg crosses into panel_item, which is an HBM
        // mapping in another module. One statement either way.
        String sql = "SELECT DISTINCT covered.test_id FROM ("
                + "  SELECT m.test_id FROM clinlims.eqa_lab_enrollment_test_map m"
                + "    JOIN clinlims.eqa_lab_program_enrollment e ON e.id = m.enrollment_id"
                + "   WHERE e.is_active = TRUE AND m.test_id IS NOT NULL" + "  UNION"
                // A lab that enrolled a whole panel has cover for each of its tests;
                // ignoring that leg would report accredited tests as EQA gaps.
                + "  SELECT pi.test_id FROM clinlims.eqa_lab_enrollment_test_map m"
                + "    JOIN clinlims.eqa_lab_program_enrollment e ON e.id = m.enrollment_id"
                + "    JOIN clinlims.panel_item pi ON pi.panel_id = m.panel_id"
                + "   WHERE e.is_active = TRUE AND m.panel_id IS NOT NULL" + ") covered";
        try {
            List<?> rows = entityManager.unwrap(Session.class).createNativeQuery(sql).list();
            List<String> testIds = new ArrayList<>(rows.size());
            for (Object row : rows) {
                if (row != null) {
                    // numeric(10,0) arrives as BigDecimal; toPlainString keeps it "6",
                    // never "6.0", so it matches test_accreditation.test_id.
                    testIds.add(row instanceof BigDecimal ? ((BigDecimal) row).toPlainString() : row.toString());
                }
            }
            return testIds;
        } catch (Exception e) {
            logger.error("Error retrieving EQA-covered test ids", e);
            throw new LIMSRuntimeException("Error retrieving EQA-covered test ids", e);
        }
    }
}
