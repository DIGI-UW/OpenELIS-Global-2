package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.AliquotDAO;
import org.openelisglobal.pharmaceutical.dao.ChainOfCustodyEventDAO;
import org.openelisglobal.pharmaceutical.dao.PharmaceuticalSampleDAO;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AliquotServiceImpl implements AliquotService {

    @Autowired
    private AliquotDAO aliquotDAO;

    @Autowired
    private PharmaceuticalSampleDAO pharmaceuticalSampleDAO;

    @Autowired
    private ChainOfCustodyEventDAO chainOfCustodyEventDAO;

    @Override
    @Transactional(readOnly = true)
    public Aliquot get(Integer id) {
        return aliquotDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> getAll() {
        return aliquotDAO.getAll();
    }

    @Override
    public Aliquot save(Aliquot aliquot) {
        Integer id = aliquotDAO.insert(aliquot);
        aliquot.setId(id);
        return aliquot;
    }

    @Override
    public Aliquot update(Aliquot aliquot) {
        aliquotDAO.update(aliquot);
        return aliquot;
    }

    @Override
    public void delete(Integer id) {
        Aliquot aliquot = get(id);
        if (aliquot != null) {
            aliquotDAO.delete(aliquot);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Aliquot findByBarcode(String barcode) {
        return aliquotDAO.findByBarcode(barcode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findByParentSampleId(Integer parentSampleId) {
        return aliquotDAO.findByParentSampleId(parentSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findByStatus(Aliquot.AliquotStatus status) {
        return aliquotDAO.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findByStorageLocation(Integer storageLocationId) {
        return aliquotDAO.findByStorageLocation(storageLocationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findAvailableByParentSample(Integer parentSampleId) {
        return aliquotDAO.findAvailableByParentSample(parentSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findExceedingFreezeThawLimit() {
        return aliquotDAO.findExceedingFreezeThawLimit();
    }

    @Override
    public Aliquot createAliquot(Integer parentSampleId, Aliquot aliquot, String userId) {
        PharmaceuticalSample parentSample = pharmaceuticalSampleDAO.get(parentSampleId).orElse(null);
        if (parentSample == null) {
            throw new LIMSRuntimeException("Parent sample not found: " + parentSampleId);
        }

        List<Aliquot> existingAliquots = aliquotDAO.findByParentSampleId(parentSampleId);
        int sequenceNumber = existingAliquots.size() + 1;

        aliquot.setParentSampleId(parentSampleId);
        aliquot.setAliquotCode(generateAliquotBarcode(parentSampleId, sequenceNumber));
        aliquot.setStatus(Aliquot.AliquotStatus.AVAILABLE);
        aliquot.setFreezeThawCount(0);
        aliquot.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        aliquot.setSysUserId(userId);

        String barcode = generateAliquotBarcode(parentSampleId, sequenceNumber);
        aliquot.setBarcode(barcode);

        Integer id = aliquotDAO.insert(aliquot);
        aliquot.setId(id);

        recordCustodyEvent(null, id, ChainOfCustodyEvent.CustodyAction.ALIQUOTED,
                "Created aliquot from parent sample " + parentSampleId, userId);

        return aliquot;
    }

    @Override
    public Aliquot recordFreezeThaw(Integer aliquotId, String userId) {
        Aliquot aliquot = get(aliquotId);
        if (aliquot == null) {
            throw new LIMSRuntimeException("Aliquot not found: " + aliquotId);
        }

        aliquot.incrementFreezeThaw();
        aliquot.setSysUserId(userId);
        aliquotDAO.update(aliquot);

        String comment = String.format("Freeze-thaw cycle recorded. Count: %d/%d",
                aliquot.getFreezeThawCount(), aliquot.getFreezeThawLimit());

        if (aliquot.isFreezeThawLimitExceeded()) {
            aliquot.setStatus(Aliquot.AliquotStatus.EXHAUSTED);
            aliquotDAO.update(aliquot);
            comment += " - LIMIT EXCEEDED, aliquot marked as exhausted";
        }

        recordCustodyEvent(null, aliquotId, ChainOfCustodyEvent.CustodyAction.RETRIEVED,
                comment, userId);

        return aliquot;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFreezeThawLimitExceeded(Integer aliquotId) {
        Aliquot aliquot = get(aliquotId);
        if (aliquot == null) {
            throw new LIMSRuntimeException("Aliquot not found: " + aliquotId);
        }
        return aliquot.isFreezeThawLimitExceeded();
    }

    @Override
    public Aliquot updateStatus(Integer aliquotId, Aliquot.AliquotStatus newStatus, String userId) {
        Aliquot aliquot = get(aliquotId);
        if (aliquot == null) {
            throw new LIMSRuntimeException("Aliquot not found: " + aliquotId);
        }

        Aliquot.AliquotStatus oldStatus = aliquot.getStatus();
        aliquot.setStatus(newStatus);
        aliquot.setSysUserId(userId);
        aliquotDAO.update(aliquot);

        recordCustodyEvent(null, aliquotId, ChainOfCustodyEvent.CustodyAction.TRANSFERRED,
                "Status changed from " + oldStatus + " to " + newStatus, userId);

        return aliquot;
    }

    @Override
    public String generateAliquotBarcode(Integer parentSampleId, int sequenceNumber) {
        String prefix = "ALQ";
        String parentRef = String.format("%05d", parentSampleId);
        String seq = String.format("%03d", sequenceNumber);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + parentRef + "-" + seq + "-" + random;
    }

    private void recordCustodyEvent(Integer sampleId, Integer aliquotId,
            ChainOfCustodyEvent.CustodyAction action, String comments, String userId) {
        ChainOfCustodyEvent event = new ChainOfCustodyEvent();
        event.setSampleId(sampleId);
        event.setAliquotId(aliquotId);
        event.setAction(action);
        event.setComments(comments);
        event.setEventTimestamp(new Timestamp(System.currentTimeMillis()));
        event.setPerformedBy(userId);
        event.setApprovalStatus(ChainOfCustodyEvent.ApprovalStatus.NOT_REQUIRED);
        event.setSysUserId(userId);
        chainOfCustodyEventDAO.insert(event);
    }
}
