package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.tb.dao.TbIdentificationResultDAO;
import org.openelisglobal.tb.valueholder.TbEnums.IdentificationResult;
import org.openelisglobal.tb.valueholder.TbIdentificationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TB identification result operations.
 */
@Service
public class TbIdentificationResultServiceImpl extends AuditableBaseObjectServiceImpl<TbIdentificationResult, Integer>
        implements TbIdentificationResultService {

    @Autowired
    private TbIdentificationResultDAO tbIdentificationResultDAO;

    public TbIdentificationResultServiceImpl() {
        super(TbIdentificationResult.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<TbIdentificationResult, Integer> getBaseObjectDAO() {
        return tbIdentificationResultDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbIdentificationResult> findBySampleItemId(String sampleItemId) {
        return tbIdentificationResultDAO.findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbIdentificationResult> findByResult(IdentificationResult result) {
        return tbIdentificationResultDAO.findByResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbIdentificationResult> findMtbPositive() {
        return tbIdentificationResultDAO.findMtbPositive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbIdentificationResult> findByTestedBy(Integer userId) {
        return tbIdentificationResultDAO.findByTestedBy(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByResult(IdentificationResult result) {
        return tbIdentificationResultDAO.countByResult(result);
    }
}
