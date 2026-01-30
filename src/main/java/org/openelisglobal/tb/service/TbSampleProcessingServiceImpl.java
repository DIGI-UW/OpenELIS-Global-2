package org.openelisglobal.tb.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.dao.TbSampleProcessingDAO;
import org.openelisglobal.tb.valueholder.TbEnums.DecontaminationMethod;
import org.openelisglobal.tb.valueholder.TbEnums.ProcessingStatus;
import org.openelisglobal.tb.valueholder.TbSampleProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TB sample processing operations.
 */
@Service
public class TbSampleProcessingServiceImpl extends AuditableBaseObjectServiceImpl<TbSampleProcessing, Integer>
        implements TbSampleProcessingService {

    @Autowired
    private TbSampleProcessingDAO tbSampleProcessingDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SystemUserService systemUserService;

    public TbSampleProcessingServiceImpl() {
        super(TbSampleProcessing.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<TbSampleProcessing, Integer> getBaseObjectDAO() {
        return tbSampleProcessingDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbSampleProcessing> findBySampleItemId(String sampleItemId) {
        return tbSampleProcessingDAO.findBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleProcessing> findByProcessingStatus(ProcessingStatus status) {
        return tbSampleProcessingDAO.findByProcessingStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleProcessing> findReadyForInoculation() {
        return tbSampleProcessingDAO.findReadyForInoculation();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleProcessing> findByDecontaminationMethod(DecontaminationMethod method) {
        return tbSampleProcessingDAO.findByDecontaminationMethod(method);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySampleItemId(String sampleItemId) {
        return tbSampleProcessingDAO.existsBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByProcessingStatus(ProcessingStatus status) {
        return tbSampleProcessingDAO.countByProcessingStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findSampleItemIdsPendingProcessing() {
        return tbSampleProcessingDAO.findSampleItemIdsPendingProcessing();
    }

    @Override
    @Transactional
    public TbSampleProcessing markAsProcessed(Integer id) {
        TbSampleProcessing processing = get(id);
        if (processing != null) {
            processing.setProcessingStatus(ProcessingStatus.PROCESSED);
            update(processing);
        }
        return processing;
    }

    @Override
    @Transactional
    public TbSampleProcessing markReadyForInoculation(Integer id) {
        TbSampleProcessing processing = get(id);
        if (processing != null) {
            processing.setProcessingStatus(ProcessingStatus.READY_FOR_INOCULATION);
            update(processing);
        }
        return processing;
    }

    @Override
    @Transactional
    public List<TbSampleProcessing> batchProcess(List<String> sampleItemIds, DecontaminationMethod method,
            String processedById) {
        List<TbSampleProcessing> processedSamples = new ArrayList<>();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        SystemUser processedBy = null;

        if (processedById != null) {
            processedBy = systemUserService.get(processedById);
        }

        for (String sampleItemId : sampleItemIds) {
            // Skip if already processed
            if (existsBySampleItemId(sampleItemId)) {
                continue;
            }

            SampleItem sampleItem = sampleItemService.get(sampleItemId);
            if (sampleItem == null) {
                continue;
            }

            TbSampleProcessing processing = new TbSampleProcessing();
            processing.setSampleItem(sampleItem);
            processing.setDecontaminationMethod(method);
            processing.setProcessingDate(now);
            processing.setProcessedBy(processedBy);
            processing.setProcessingStatus(ProcessingStatus.READY_FOR_INOCULATION);

            insert(processing);
            processedSamples.add(processing);
        }

        return processedSamples;
    }
}
