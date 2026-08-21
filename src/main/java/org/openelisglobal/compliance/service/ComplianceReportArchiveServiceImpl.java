package org.openelisglobal.compliance.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.openelisglobal.compliance.dao.ComplianceReportArchiveDAO;
import org.openelisglobal.compliance.valueholder.ComplianceReportArchive;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ComplianceReportArchiveServiceImpl implements ComplianceReportArchiveService {

    @Autowired
    private ComplianceReportArchiveDAO dao;

    @Autowired
    private SampleService sampleService;

    @Override
    public ComplianceReportArchive archiveIfAbsent(Long sampleId, Integer amendmentNumber, byte[] pdfBytes,
            String userId) {
        int effectiveAmendmentNumber = amendmentNumber == null ? 0 : amendmentNumber;

        Optional<ComplianceReportArchive> existing = dao.findBySampleIdAndAmendmentNumber(sampleId,
                effectiveAmendmentNumber);
        if (existing.isPresent()) {
            return existing.get();
        }

        ComplianceReportArchive archive = new ComplianceReportArchive();
        archive.setSample(sampleService.get(String.valueOf(sampleId)));
        archive.setAmendmentNumber(effectiveAmendmentNumber);
        archive.setPdfContent(pdfBytes);
        archive.setSha256Hash(sha256Hex(pdfBytes));
        archive.setGeneratedAt(OffsetDateTime.now());
        archive.setGeneratedByUserId(userId);
        return dao.save(archive);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceReportArchive> findBySampleIdAndAmendmentNumber(Long sampleId, Integer amendmentNumber) {
        return dao.findBySampleIdAndAmendmentNumber(sampleId, amendmentNumber == null ? 0 : amendmentNumber);
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
