package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class TestAccreditationServiceImpl extends AuditableBaseObjectServiceImpl<TestAccreditation, Long>
        implements TestAccreditationService {

    @Autowired
    private TestAccreditationDAO baseObjectDAO;

    public TestAccreditationServiceImpl() {
        super(TestAccreditation.class);
        this.auditTrailLog = true;
    }

    @Override
    protected TestAccreditationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getByTestId(Long testId) {
        return baseObjectDAO.findByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getByAccreditingBodyId(Long accreditingBodyId) {
        return baseObjectDAO.findByAccreditingBodyId(accreditingBodyId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestAccreditation getByTestAndBody(Long testId, Long accreditingBodyId) {
        return baseObjectDAO.findByTestAndBody(testId, accreditingBodyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getExpiringOnOrBefore(LocalDate date) {
        return baseObjectDAO.findExpiringOnOrBefore(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getAllActive() {
        return baseObjectDAO.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTestAndBody(Long testId, Long accreditingBodyId) {
        return baseObjectDAO.existsByTestAndBody(testId, accreditingBodyId);
    }

    @Override
    @Transactional
    public Long insert(TestAccreditation testAccreditation) {
        if (baseObjectDAO.existsByTestAndBody(Long.parseLong(testAccreditation.getTest().getId()),
                testAccreditation.getAccreditingBody().getId())) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for test id=" + testAccreditation.getTest().getId()
                            + " and accrediting body id=" + testAccreditation.getAccreditingBody().getId());
        }
        return super.insert(testAccreditation);
    }

    @Override
    @Transactional
    public TestAccreditation update(TestAccreditation testAccreditation) {
        Optional<TestAccreditation> existingOpt = baseObjectDAO.get(testAccreditation.getId());

        if (!existingOpt.isPresent()) {
            throw new LIMSRuntimeException("TestAccreditation not found with id: " + testAccreditation.getId());
        }
        TestAccreditation existing = existingOpt.get();
        testAccreditation.setTest(existing.getTest());
        testAccreditation.setAccreditingBody(existing.getAccreditingBody());
        return super.update(testAccreditation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getByFilters(Long testId, Long accreditingBodyId, Long sectionId, String q) {
        return baseObjectDAO.findByFilters(testId, accreditingBodyId, sectionId, q);
    }

    @Override
    @Transactional
    public void bulkExtend(List<Long> ids, LocalDate newExpiresOn, String sysUserId) {
        for (Long id : ids) {
            Optional<TestAccreditation> opt = baseObjectDAO.get(id);

            if (!opt.isPresent()) {
                throw new LIMSRuntimeException("TestAccreditation not found with id: " + id);
            }
            TestAccreditation ta = opt.get();
            ta.setExpiresOn(newExpiresOn);
            ta.setSysUserId(sysUserId);
            super.update(ta);
        }
    }
}
