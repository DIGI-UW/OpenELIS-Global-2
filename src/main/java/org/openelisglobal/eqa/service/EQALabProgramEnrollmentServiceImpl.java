package org.openelisglobal.eqa.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQALabProgramEnrollmentDAO;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentLabUnit;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentTestMap;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQALabProgramEnrollmentServiceImpl extends BaseObjectServiceImpl<EQALabProgramEnrollment, Long>
        implements EQALabProgramEnrollmentService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EQALabProgramEnrollmentDAO enrollmentDAO;

    @Autowired
    private AuditTrailService auditTrailService;

    public EQALabProgramEnrollmentServiceImpl() {
        super(EQALabProgramEnrollment.class);
    }

    @Override
    protected EQALabProgramEnrollmentDAO getBaseObjectDAO() {
        return enrollmentDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQALabProgramEnrollment> findAll() {
        return enrollmentDAO.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQALabProgramEnrollment> findActiveEnrollments() {
        return enrollmentDAO.findByIsActive(true);
    }

    @Override
    public EQALabProgramEnrollment createEnrollment(EQALabProgramEnrollment enrollment, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes) {

        enrollment.setCreatedDate(new Date());
        enrollment.setIsActive(enrollment.getIsActive() != null ? enrollment.getIsActive() : true);
        enrollment.setStatus(Boolean.FALSE.equals(enrollment.getIsActive()) ? STATUS_SUSPENDED : STATUS_ACTIVE);

        setMappings(enrollment, labUnitIds, testIds, panelIds, testAnalytes);

        Long id = enrollmentDAO.insert(enrollment);
        return enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve created enrollment"));
    }

    @Override
    public EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds, Map<Long, Long> testAnalytes) {

        EQALabProgramEnrollment existing = enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));

        // The test maps are cleared and rebuilt below, so a caller that omits the
        // reporting-analyte map must mean "leave it as it is" rather than "clear it".
        // Toggling an enrollment's status has no business rewriting the map, and the
        // participant's submission bridge resolves the analyte through it.
        Map<Long, Long> effectiveAnalytes = testAnalytes != null ? testAnalytes : storedTestAnalytes(existing);

        existing.setProgramName(updated.getProgramName());
        existing.setProvider(updated.getProvider());
        existing.setDescription(updated.getDescription());
        // Editing an enrolment's details leaves its lifecycle alone. Suspending,
        // resuming and withdrawing all need a reason and an effective date, so they
        // go through updateStatus rather than riding on a plain save.
        existing.setLastModified(new Date());
        existing.setSysUserId(updated.getSysUserId());

        existing.getLabUnits().clear();
        existing.getTestMaps().clear();
        existing = enrollmentDAO.update(existing);
        entityManager.flush();

        setMappings(existing, labUnitIds, testIds, panelIds, effectiveAnalytes);
        enrollmentDAO.update(existing);
        entityManager.flush();
        entityManager.clear();
        return enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalStateException("Failed to reload enrollment after update"));
    }

    @Override
    public EQALabProgramEnrollment updateStatus(Long id, String newStatus, String reason, Date effectiveDate,
            String sysUserId) {

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to change an enrollment's status");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("An effective date is required to change an enrollment's status");
        }

        EQALabProgramEnrollment enrollment = enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
        validateStatusTransition(enrollment.getStatus(), newStatus);

        // The audit records the difference between two objects, so the state before
        // the change needs an object of its own. Reading the row twice would not do
        // it: the session hands back the same instance both times, and the second
        // read would follow the first one's edits.
        EQALabProgramEnrollment prior = new EQALabProgramEnrollment();
        BeanUtils.copyProperties(enrollment, prior);

        enrollment.setStatus(newStatus);
        enrollment.setStatusReason(reason);
        enrollment.setStatusEffectiveDate(effectiveDate);
        enrollment.setStatusChangedDate(new Date());
        enrollment.setStatusChangedBy(Long.valueOf(sysUserId));
        enrollment.setIsActive(STATUS_ACTIVE.equals(newStatus));
        enrollment.setLastModified(new Date());
        enrollment.setSysUserId(sysUserId);

        auditTrailService.saveHistory(enrollment, prior, sysUserId, IActionConstants.AUDIT_TRAIL_UPDATE,
                "eqa_lab_program_enrollment");

        return enrollmentDAO.update(enrollment);
    }

    @Override
    public void softDelete(Long id) {
        EQALabProgramEnrollment enrollment = enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
        enrollment.setIsActive(false);
        enrollment.setStatus(STATUS_SUSPENDED);
        enrollment.setLastModified(new Date());
        enrollmentDAO.update(enrollment);
    }

    /**
     * Active and Suspended swap either way, both lead to Withdrawn, and Withdrawn
     * is the end of the row. A laboratory that comes back enrols again, which is
     * how the provider's own enrolment behaves.
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        boolean valid = false;
        if (STATUS_ACTIVE.equals(currentStatus)) {
            valid = STATUS_SUSPENDED.equals(newStatus) || STATUS_WITHDRAWN.equals(newStatus);
        } else if (STATUS_SUSPENDED.equals(currentStatus)) {
            valid = STATUS_ACTIVE.equals(newStatus) || STATUS_WITHDRAWN.equals(newStatus);
        }
        if (!valid) {
            throw new IllegalArgumentException("Cannot move an enrollment from " + currentStatus + " to " + newStatus);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctProviders() {
        return enrollmentDAO.findDistinctProviders();
    }

    /**
     * The analyte currently recorded against each test, keyed the way setMappings
     * reads it.
     */
    private Map<Long, Long> storedTestAnalytes(EQALabProgramEnrollment enrollment) {
        Map<Long, Long> analytes = new HashMap<>();
        for (EQALabEnrollmentTestMap map : enrollment.getTestMaps()) {
            if (map.getTestId() != null && map.getAnalyteId() != null) {
                analytes.put(map.getTestId(), map.getAnalyteId());
            }
        }
        return analytes;
    }

    private void setMappings(EQALabProgramEnrollment enrollment, List<Long> labUnitIds, List<Long> testIds,
            List<Long> panelIds, Map<Long, Long> testAnalytes) {

        if (labUnitIds != null) {
            for (Long unitId : labUnitIds) {
                EQALabEnrollmentLabUnit labUnit = new EQALabEnrollmentLabUnit();
                labUnit.setEnrollment(enrollment);
                labUnit.setTestSectionId(unitId);
                labUnit.setSysUserId(enrollment.getSysUserId());
                enrollment.getLabUnits().add(labUnit);
            }
        }

        List<EQALabEnrollmentTestMap> testMaps = new ArrayList<>();
        if (testIds != null) {
            for (Long testId : testIds) {
                EQALabEnrollmentTestMap map = new EQALabEnrollmentTestMap();
                map.setEnrollment(enrollment);
                map.setTestId(testId);
                if (testAnalytes != null) {
                    map.setAnalyteId(testAnalytes.get(testId));
                }
                map.setSysUserId(enrollment.getSysUserId());
                testMaps.add(map);
            }
        }
        if (panelIds != null) {
            for (Long panelId : panelIds) {
                EQALabEnrollmentTestMap map = new EQALabEnrollmentTestMap();
                map.setEnrollment(enrollment);
                map.setPanelId(panelId);
                map.setSysUserId(enrollment.getSysUserId());
                testMaps.add(map);
            }
        }
        enrollment.getTestMaps().addAll(testMaps);
    }
}
