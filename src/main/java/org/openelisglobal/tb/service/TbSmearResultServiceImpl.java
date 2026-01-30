package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.tb.dao.TbSmearResultDAO;
import org.openelisglobal.tb.valueholder.TbEnums.AfbResult;
import org.openelisglobal.tb.valueholder.TbSmearResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TB smear result operations.
 */
@Service
public class TbSmearResultServiceImpl extends AuditableBaseObjectServiceImpl<TbSmearResult, Integer>
        implements TbSmearResultService {

    @Autowired
    private TbSmearResultDAO tbSmearResultDAO;

    public TbSmearResultServiceImpl() {
        super(TbSmearResult.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<TbSmearResult, Integer> getBaseObjectDAO() {
        return tbSmearResultDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbSmearResult> findBySampleItemId(String sampleItemId) {
        return tbSmearResultDAO.findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSmearResult> findByAfbResult(AfbResult result) {
        return tbSmearResultDAO.findByAfbResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSmearResult> findPositiveResults() {
        return tbSmearResultDAO.findPositiveResults();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSmearResult> findByTestedBy(Integer userId) {
        return tbSmearResultDAO.findByTestedBy(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByAfbResult(AfbResult result) {
        return tbSmearResultDAO.countByAfbResult(result);
    }
}
