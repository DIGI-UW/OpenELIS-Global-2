package org.openelisglobal.compliance.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.openelisglobal.compliance.dao.ComplianceReportGenerationDAO;
import org.openelisglobal.compliance.valueholder.ComplianceReportGeneration;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ComplianceReportGenerationServiceImpl implements ComplianceReportGenerationService {

    @Autowired
    private ComplianceReportGenerationDAO dao;

    @Autowired
    private SampleService sampleService;

    @Override
    public void recordGeneration(Long sampleId, String userId) {
        ComplianceReportGeneration record = new ComplianceReportGeneration();
        record.setSample(sampleService.get(String.valueOf(sampleId)));
        record.setGeneratedAt(OffsetDateTime.now());
        record.setGeneratedByUserId(userId);
        dao.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> getLastGenerated(Long sampleId) {
        return dao.findLatestBySampleId(sampleId).map(ComplianceReportGeneration::getGeneratedAt);
    }
}
