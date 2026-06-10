package org.openelisglobal.compliance.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.openelisglobal.compliance.dao.ComplianceReportGenerationDAO;
import org.openelisglobal.compliance.valueholder.ComplianceReportGeneration;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ComplianceReportGenerationServiceImpl implements ComplianceReportGenerationService {

    @Autowired
    private ComplianceReportGenerationDAO dao;

    @Override
    public void recordGeneration(Long sampleId, String userId) {
        ComplianceReportGeneration record = new ComplianceReportGeneration();
        Sample sample = new Sample();
        sample.setId(String.valueOf(sampleId));
        record.setSample(sample);
        record.setGeneratedAt(LocalDateTime.now());
        record.setGeneratedByUserId(userId);
        dao.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> getLastGenerated(Long sampleId) {
        return dao.findLatestBySampleId(sampleId).map(ComplianceReportGeneration::getGeneratedAt);
    }
}
