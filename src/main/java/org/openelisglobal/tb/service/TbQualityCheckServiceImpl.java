package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.tb.dao.TbQualityCheckDAO;
import org.openelisglobal.tb.valueholder.TbEnums.QcResult;
import org.openelisglobal.tb.valueholder.TbQualityCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TB quality check operations.
 */
@Service
public class TbQualityCheckServiceImpl extends AuditableBaseObjectServiceImpl<TbQualityCheck, Integer>
        implements TbQualityCheckService {

    @Autowired
    private TbQualityCheckDAO tbQualityCheckDAO;

    public TbQualityCheckServiceImpl() {
        super(TbQualityCheck.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<TbQualityCheck, Integer> getBaseObjectDAO() {
        return tbQualityCheckDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbQualityCheck> findBySampleItemId(String sampleItemId) {
        return tbQualityCheckDAO.findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbQualityCheck> findByOverallResult(QcResult result) {
        return tbQualityCheckDAO.findByOverallResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbQualityCheck> findByCheckedBy(Integer userId) {
        return tbQualityCheckDAO.findByCheckedBy(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByOverallResult(QcResult result) {
        return tbQualityCheckDAO.countByOverallResult(result);
    }
}
