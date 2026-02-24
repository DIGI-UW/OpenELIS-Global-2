package org.openelisglobal.eqa.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.EQALabProgramEnrollmentDAO;
import org.openelisglobal.eqa.dao.EQAProgramDAO;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentLabUnit;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentTestMap;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQALabProgramEnrollmentServiceImpl extends BaseObjectServiceImpl<EQALabProgramEnrollment, Long>
        implements EQALabProgramEnrollmentService {

    @Autowired
    private EQALabProgramEnrollmentDAO enrollmentDAO;

    @Autowired
    private EQAProgramDAO eqaProgramDAO;

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
            List<Long> testIds, List<Long> panelIds) {

        enrollment.setCreatedDate(new Date());
        enrollment.setIsActive(enrollment.getIsActive() != null ? enrollment.getIsActive() : true);

        setMappings(enrollment, labUnitIds, testIds, panelIds);

        Long id = enrollmentDAO.insert(enrollment);
        return enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve created enrollment"));
    }

    @Override
    public EQALabProgramEnrollment updateEnrollment(Long id, EQALabProgramEnrollment updated, List<Long> labUnitIds,
            List<Long> testIds, List<Long> panelIds) {

        EQALabProgramEnrollment existing = enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));

        existing.setProgramName(updated.getProgramName());
        existing.setProvider(updated.getProvider());
        existing.setDescription(updated.getDescription());
        existing.setIsActive(updated.getIsActive() != null ? updated.getIsActive() : existing.getIsActive());
        existing.setLastModified(new Date());
        existing.setSysUserId(updated.getSysUserId());

        existing.getLabUnits().clear();
        existing.getTestMaps().clear();
        setMappings(existing, labUnitIds, testIds, panelIds);

        return enrollmentDAO.update(existing);
    }

    @Override
    public void softDelete(Long id) {
        EQALabProgramEnrollment enrollment = enrollmentDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + id));
        enrollment.setIsActive(false);
        enrollment.setLastModified(new Date());
        enrollmentDAO.update(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctProviders() {
        List<String> labProviders = enrollmentDAO.findDistinctProviders();
        List<String> programProviders = eqaProgramDAO.findByIsActive(true).stream().map(p -> p.getProviderName())
                .filter(name -> name != null && !name.isBlank()).distinct().collect(Collectors.toList());

        return Stream.concat(labProviders.stream(), programProviders.stream()).distinct().sorted()
                .collect(Collectors.toList());
    }

    private void setMappings(EQALabProgramEnrollment enrollment, List<Long> labUnitIds, List<Long> testIds,
            List<Long> panelIds) {

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
