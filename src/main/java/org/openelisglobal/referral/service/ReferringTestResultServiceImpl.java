package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.referral.dao.ReferringTestResultDAO;
import org.openelisglobal.referral.valueholder.ReferringTestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferringTestResultServiceImpl extends AuditableBaseObjectServiceImpl<ReferringTestResult, String>
        implements ReferringTestResultService {
    @Autowired
    protected ReferringTestResultDAO baseObjectDAO;

    ReferringTestResultServiceImpl() {
        super(ReferringTestResult.class);
    }

    @Override
    protected ReferringTestResultDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferringTestResult> getReferringTestResultsForSampleItem(String id) {
        return baseObjectDAO.getReferringTestResultsForSampleItem(id);
    }
}
