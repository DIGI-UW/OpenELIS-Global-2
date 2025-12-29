package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.tb.dao.TbSampleRegistrationDAO;
import org.openelisglobal.tb.valueholder.TbSampleRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TB sample registration operations.
 */
@Service
public class TbSampleRegistrationServiceImpl extends AuditableBaseObjectServiceImpl<TbSampleRegistration, Integer>
        implements TbSampleRegistrationService {

    @Autowired
    private TbSampleRegistrationDAO tbSampleRegistrationDAO;

    public TbSampleRegistrationServiceImpl() {
        super(TbSampleRegistration.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<TbSampleRegistration, Integer> getBaseObjectDAO() {
        return tbSampleRegistrationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbSampleRegistration> findBySampleItemId(String sampleItemId) {
        return tbSampleRegistrationDAO.findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleRegistration> findByDocumentNumber(String documentNumber) {
        return tbSampleRegistrationDAO.findByDocumentNumber(documentNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleRegistration> findByReferringFacility(String facility) {
        return tbSampleRegistrationDAO.findByReferringFacility(facility);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleRegistration> findByReceivedSite(String site) {
        return tbSampleRegistrationDAO.findByReceivedSite(site);
    }
}
