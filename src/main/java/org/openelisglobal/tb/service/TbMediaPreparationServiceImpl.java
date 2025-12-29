package org.openelisglobal.tb.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.tb.dao.TbMediaPreparationDAO;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.MediaType;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TB media preparation operations.
 */
@Service
public class TbMediaPreparationServiceImpl extends AuditableBaseObjectServiceImpl<TbMediaPreparation, Integer>
        implements TbMediaPreparationService {

    @Autowired
    private TbMediaPreparationDAO tbMediaPreparationDAO;

    public TbMediaPreparationServiceImpl() {
        super(TbMediaPreparation.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<TbMediaPreparation, Integer> getBaseObjectDAO() {
        return tbMediaPreparationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbMediaPreparation> findByBatchId(String batchId) {
        return tbMediaPreparationDAO.findByBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findByMediaType(MediaType mediaType) {
        return tbMediaPreparationDAO.findByMediaType(mediaType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findByQcStatus(MediaQcStatus qcStatus) {
        return tbMediaPreparationDAO.findByQcStatus(qcStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findAvailableForInoculation() {
        return tbMediaPreparationDAO.findAvailableForInoculation();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findAvailableByMediaType(MediaType mediaType) {
        return tbMediaPreparationDAO.findAvailableByMediaType(mediaType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBatchId(String batchId) {
        return tbMediaPreparationDAO.existsByBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByQcStatus(MediaQcStatus qcStatus) {
        return tbMediaPreparationDAO.countByQcStatus(qcStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateBatchId(MediaType mediaType) {
        String prefix = mediaType == MediaType.LJ ? "LJ" : "MGIT";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseBatchId = prefix + "-" + datePart;

        // Find the next sequence number for this day
        int sequence = 1;
        while (existsByBatchId(baseBatchId + "-" + String.format("%03d", sequence))) {
            sequence++;
        }

        return baseBatchId + "-" + String.format("%03d", sequence);
    }

    @Override
    @Transactional
    public TbMediaPreparation updateQcStatus(Integer id, MediaQcStatus status, String notes, String sysUserId) {
        TbMediaPreparation mediaPrep = get(id);
        if (mediaPrep != null) {
            mediaPrep.setQcStatus(status);
            if (notes != null && !notes.isEmpty()) {
                mediaPrep.setQcNotes(notes);
            }
            mediaPrep.setSysUserId(sysUserId);
            update(mediaPrep);
        }
        return mediaPrep;
    }
}
