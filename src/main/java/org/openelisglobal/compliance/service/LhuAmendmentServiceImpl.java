package org.openelisglobal.compliance.service;

import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LhuAmendmentServiceImpl implements LhuAmendmentService {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ElectronicSignatureService electronicSignatureService;

    @Override
    @Transactional(readOnly = true)
    public boolean hasBeenReleased(Long sampleId) {
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(String.valueOf(sampleId));
        for (SampleItem item : items) {
            List<Analysis> analyses = analysisService.getAnalysesBySampleItem(item);
            for (Analysis analysis : analyses) {
                List<ElectronicSignature> sigs = electronicSignatureService.getSignaturesForRecord("VALIDATION_BATCH",
                        Long.parseLong(analysis.getId()));
                for (ElectronicSignature sig : sigs) {
                    if (sig.getSignatureMeaning() == SignatureMeaning.VALIDATED_AND_RELEASED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void applyLhuAmendment(Long sampleId, String priorCertificateNumber, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Amendment reason must not be blank");
        }
        Sample sample = sampleService.get(String.valueOf(sampleId));
        if (sample == null) {
            throw new IllegalArgumentException("Sample not found: " + sampleId);
        }
        Integer current = sample.getAmendmentNumber();
        sample.setAmendmentNumber(current == null ? 1 : current + 1);
        sample.setAmendsLhuNumber(priorCertificateNumber);
        sample.setAmendmentReason(reason);
        sampleService.save(sample);
    }

    @Override
    public String certificateNumberWithAmendmentSuffix(String baseCertificateNumber, Integer amendmentNumber) {
        if (baseCertificateNumber == null) {
            return null;
        }
        if (amendmentNumber == null || amendmentNumber <= 0) {
            return baseCertificateNumber;
        }
        return baseCertificateNumber + "/Am." + amendmentNumber;
    }
}
