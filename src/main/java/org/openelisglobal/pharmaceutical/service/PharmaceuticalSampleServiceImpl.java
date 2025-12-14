package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.AliquotDAO;
import org.openelisglobal.pharmaceutical.dao.ChainOfCustodyEventDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.dao.QCCheckDAO;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.openelisglobal.pharmaceutical.valueholder.QCCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PharmaceuticalSampleServiceImpl implements PharmaceuticalSampleService {

    @Autowired
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Autowired
    private AliquotDAO aliquotDAO;

    @Autowired
    private QCCheckDAO qcCheckDAO;

    @Autowired
    private ChainOfCustodyEventDAO chainOfCustodyEventDAO;

    @Override
    @Transactional(readOnly = true)
    public PharmaceuticalSample get(Integer id) {
        return pharmaceuticalSampleDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> getAll() {
        return pharmaceuticalSampleDAO.getAll();
    }

    @Override
    public PharmaceuticalSample save(PharmaceuticalSample sample) {
        Integer id = pharmaceuticalSampleDAO.insert(sample);
        sample.setId(id);
        return sample;
    }

    @Override
    public PharmaceuticalSample update(PharmaceuticalSample sample) {
        pharmaceuticalSampleDAO.update(sample);
        return sample;
    }

    @Override
    public void delete(Integer id) {
        PharmaceuticalSample sample = get(id);
        if (sample != null) {
            pharmaceuticalSampleDAO.delete(sample);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PharmaceuticalSample findByUniqueSampleId(String uniqueSampleId) {
        return pharmaceuticalSampleDAO.findByUniqueSampleId(uniqueSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public PharmaceuticalSample findByBarcode(String barcode) {
        return pharmaceuticalSampleDAO.findByBarcode(barcode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> findByStatus(PharmaceuticalSample.SampleStatus status) {
        return pharmaceuticalSampleDAO.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> findByLabType(PharmaceuticalSample.LabType labType) {
        return pharmaceuticalSampleDAO.findByLabType(labType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> findExpiringSoon(int daysAhead) {
        return pharmaceuticalSampleDAO.findExpiringSoon(daysAhead);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> searchByName(String searchTerm) {
        return pharmaceuticalSampleDAO.searchByName(searchTerm);
    }

    @Override
    public PharmaceuticalSample registerSample(PharmaceuticalSample sample, String userId) {
        if (sample.getUniqueSampleId() == null || sample.getUniqueSampleId().isEmpty()) {
            sample.setUniqueSampleId(generateUniqueSampleId());
        }

        if (pharmaceuticalSampleDAO.findByUniqueSampleId(sample.getUniqueSampleId()) != null) {
            throw new LIMSRuntimeException("Sample with ID " + sample.getUniqueSampleId() + " already exists");
        }

        sample.setStatus(PharmaceuticalSample.SampleStatus.REGISTERED);
        sample.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
        sample.setSysUserId(userId);

        String barcode = generateBarcode(sample);
        sample.setBarcode(barcode);
        sample.setQrCode(barcode);

        Integer id = pharmaceuticalSampleDAO.insert(sample);
        sample.setId(id);

        return sample;
    }

    @Override
    public PharmaceuticalSample updateStatus(Integer sampleId, PharmaceuticalSample.SampleStatus newStatus,
            String userId) {
        PharmaceuticalSample sample = get(sampleId);
        if (sample == null) {
            throw new LIMSRuntimeException("Sample not found: " + sampleId);
        }

        sample.setStatus(newStatus);
        sample.setSysUserId(userId);
        pharmaceuticalSampleDAO.update(sample);

        return sample;
    }

    @Override
    public String generateBarcode(PharmaceuticalSample sample) {
        String prefix = "PS";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + timestamp + "-" + random;
    }

    private String generateUniqueSampleId() {
        String prefix = "PHARMA";
        String year = String.valueOf(java.time.Year.now().getValue());
        String sequence = String.format("%05d", System.currentTimeMillis() % 100000);
        return prefix + "-" + year + "-" + sequence;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleWithDetails(Integer sampleId) {
        PharmaceuticalSample sample = get(sampleId);
        if (sample == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sample", sample);

        List<Aliquot> aliquots = aliquotDAO.findByParentSampleId(sampleId);
        result.put("aliquots", aliquots);
        result.put("aliquotCount", aliquots.size());

        List<QCCheck> qcChecks = qcCheckDAO.findBySampleId(sampleId);
        result.put("qcChecks", qcChecks);

        QCCheck latestQC = qcCheckDAO.findLatestBySampleId(sampleId);
        result.put("latestQCCheck", latestQC);

        List<ChainOfCustodyEvent> custodyEvents = chainOfCustodyEventDAO.findBySampleId(sampleId);
        result.put("custodyEvents", custodyEvents);

        return result;
    }
}
