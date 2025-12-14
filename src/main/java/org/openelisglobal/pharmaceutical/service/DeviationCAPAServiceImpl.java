package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.dao.AssayRunDAO;
import org.openelisglobal.pharmaceutical.dao.DeviationCAPADAO;
import org.openelisglobal.pharmaceutical.valueholder.AssayRun;
import org.openelisglobal.pharmaceutical.valueholder.DeviationCAPA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeviationCAPAServiceImpl implements DeviationCAPAService {

    @Autowired
    private DeviationCAPADAO deviationCAPADAO;

    @Autowired
    private AssayRunDAO assayRunDAO;

    @Override
    @Transactional(readOnly = true)
    public DeviationCAPA get(Integer id) {
        return deviationCAPADAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationCAPA> getAll() {
        return deviationCAPADAO.getAll();
    }

    @Override
    public DeviationCAPA save(DeviationCAPA deviationCAPA) {
        Integer id = deviationCAPADAO.insert(deviationCAPA);
        deviationCAPA.setId(id);
        return deviationCAPA;
    }

    @Override
    public DeviationCAPA update(DeviationCAPA deviationCAPA) {
        deviationCAPADAO.update(deviationCAPA);
        return deviationCAPA;
    }

    @Override
    public void delete(Integer id) {
        DeviationCAPA deviationCAPA = get(id);
        if (deviationCAPA != null) {
            deviationCAPADAO.delete(deviationCAPA);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationCAPA> findByAssayRunId(Integer assayRunId) {
        return deviationCAPADAO.findByAssayRunId(assayRunId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationCAPA> findByStatus(DeviationCAPA.CAPAStatus status) {
        return deviationCAPADAO.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationCAPA> findOpenCAPAs() {
        return deviationCAPADAO.findOpenCAPAs();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviationCAPA findByDeviationNumber(String deviationNumber) {
        return deviationCAPADAO.findByDeviationNumber(deviationNumber);
    }

    @Override
    public DeviationCAPA createDeviation(Integer assayRunId, DeviationCAPA deviation, String userId) {
        AssayRun assayRun = assayRunDAO.get(assayRunId).orElse(null);
        if (assayRun == null) {
            throw new LIMSRuntimeException("Assay run not found: " + assayRunId);
        }

        deviation.setAssayRunId(assayRunId);
        deviation.setDeviationNumber(generateDeviationNumber());
        deviation.setStatus(DeviationCAPA.CAPAStatus.OPEN);
        deviation.setRaisedAt(new Timestamp(System.currentTimeMillis()));
        deviation.setRaisedBy(userId);
        deviation.setSysUserId(userId);

        Integer id = deviationCAPADAO.insert(deviation);
        deviation.setId(id);

        assayRun.setLinkedCAPAId(id);
        assayRun.setSysUserId(userId);
        assayRunDAO.update(assayRun);

        return deviation;
    }

    @Override
    public DeviationCAPA initiateCAPA(Integer deviationId, String correctiveAction, String preventiveAction,
            String userId) {
        DeviationCAPA deviation = get(deviationId);
        if (deviation == null) {
            throw new LIMSRuntimeException("Deviation not found: " + deviationId);
        }

        deviation.setCorrectiveAction(correctiveAction);
        deviation.setPreventiveAction(preventiveAction);
        deviation.setStatus(DeviationCAPA.CAPAStatus.INVESTIGATION);
        deviation.setSysUserId(userId);
        deviationCAPADAO.update(deviation);

        return deviation;
    }

    @Override
    public DeviationCAPA updateCAPAStatus(Integer deviationId, DeviationCAPA.CAPAStatus newStatus, String userId) {
        DeviationCAPA deviation = get(deviationId);
        if (deviation == null) {
            throw new LIMSRuntimeException("Deviation not found: " + deviationId);
        }

        deviation.setStatus(newStatus);
        deviation.setSysUserId(userId);
        deviationCAPADAO.update(deviation);

        return deviation;
    }

    @Override
    public DeviationCAPA closeCAPA(Integer deviationId, String closureNotes, String userId) {
        DeviationCAPA deviation = get(deviationId);
        if (deviation == null) {
            throw new LIMSRuntimeException("Deviation not found: " + deviationId);
        }

        deviation.setStatus(DeviationCAPA.CAPAStatus.CLOSED);
        deviation.setClosedAt(new Timestamp(System.currentTimeMillis()));
        deviation.setClosedBy(userId);
        if (closureNotes != null && !closureNotes.isEmpty()) {
            String existingDesc = deviation.getDescription();
            if (existingDesc != null && !existingDesc.isEmpty()) {
                deviation.setDescription(existingDesc + "\n\nClosure Notes: " + closureNotes);
            } else {
                deviation.setDescription("Closure Notes: " + closureNotes);
            }
        }
        deviation.setSysUserId(userId);
        deviationCAPADAO.update(deviation);

        return deviation;
    }

    @Override
    public String generateDeviationNumber() {
        String prefix = "DEV";
        String year = String.valueOf(java.time.Year.now().getValue());
        String sequence = String.format("%05d", System.currentTimeMillis() % 100000);
        return prefix + "-" + year + "-" + sequence;
    }
}
