package org.openelisglobal.sample.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SampleComplianceStandardServiceImpl extends AuditableBaseObjectServiceImpl<SampleComplianceStandard, Long>
        implements SampleComplianceStandardService {

    @Autowired
    protected SampleComplianceStandardDAO baseObjectDAO;

    public SampleComplianceStandardServiceImpl() {
        super(SampleComplianceStandard.class);
    }

    @Override
    protected SampleComplianceStandardDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleComplianceStandard> getAllForSample(String sampleId) {
        return baseObjectDAO.getAllForSample(sampleId);
    }

    @Override
    public void replaceAllForSample(String sampleId, List<SampleComplianceStandard> standards) {
        baseObjectDAO.deleteAllForSample(sampleId);
        for (SampleComplianceStandard standard : standards) {
            baseObjectDAO.insert(standard);
        }
    }
}
