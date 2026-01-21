package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.biorepository.dao.ChainOfCustodyLogDAO;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for ChainOfCustodyLog operations.
 *
 * All entries are append-only (immutable audit log).
 */
@Service
public class ChainOfCustodyServiceImpl extends AuditableBaseObjectServiceImpl<ChainOfCustodyLog, Integer>
        implements ChainOfCustodyService {

    @Autowired
    protected ChainOfCustodyLogDAO baseObjectDAO;

    ChainOfCustodyServiceImpl() {
        super(ChainOfCustodyLog.class);
    }

    @Override
    protected ChainOfCustodyLogDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public ChainOfCustodyLog logCustodyAction(SampleItem sampleItem, CustodyAction action,
            SampleTransferRequest transferInRequest, SampleRetrievalRequest retrievalRequest, String storageCoordinates,
            SystemUser custodian, String fromLocation, String toLocation, BigDecimal temperature, String notes,
            String sysUserId) {

        if (sampleItem == null) {
            throw new IllegalArgumentException("Sample item is required");
        }
        if (action == null) {
            throw new IllegalArgumentException("Custody action is required");
        }

        ChainOfCustodyLog log = new ChainOfCustodyLog();
        log.setSampleItem(sampleItem);
        log.setCustodyAction(action);
        log.setTransferInRequest(transferInRequest);
        log.setRetrievalRequest(retrievalRequest);
        log.setStorageCoordinates(storageCoordinates);
        log.setFromLocation(fromLocation);
        log.setToLocation(toLocation);
        log.setTemperature(temperature);
        log.setNotes(notes);
        log.setActionTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setSysUserId(sysUserId);

        if (custodian != null) {
            if (action.name().contains("RETRIEVED") || action.name().contains("RELEASED")
                    || action.name().contains("INITIATED")) {
                log.setFromCustodian(custodian);
            } else {
                log.setToCustodian(custodian);
            }
        }

        return save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyLog> getBySampleItemId(Integer sampleItemId) {
        return baseObjectDAO.getBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyLog> getByTransferInRequestId(Integer transferInRequestId) {
        return baseObjectDAO.getByTransferInRequestId(transferInRequestId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyLog> getByRetrievalRequestId(Integer retrievalRequestId) {
        return baseObjectDAO.getByRetrievalRequestId(retrievalRequestId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyLog> getByAction(CustodyAction action) {
        return baseObjectDAO.getByAction(action);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyLog> getRecentActions(int limit) {
        return baseObjectDAO.getRecentActions(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyLog> getFullLifecycle(Integer sampleItemId, Integer transferInRequestId,
            Integer retrievalRequestId) {
        return baseObjectDAO.getFullLifecycle(sampleItemId, transferInRequestId, retrievalRequestId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAction(CustodyAction action) {
        return baseObjectDAO.countByAction(action);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChainOfCustodyLog> searchCustodyLogs(String sampleExternalId, CustodyAction action, Integer custodianId,
            Timestamp startDate, Timestamp endDate, int page, int pageSize) {
        return baseObjectDAO.searchCustodyLogs(sampleExternalId, action, custodianId, startDate, endDate, page,
                pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCustodyLogs(String sampleExternalId, CustodyAction action, Integer custodianId,
            Timestamp startDate, Timestamp endDate) {
        return baseObjectDAO.countCustodyLogs(sampleExternalId, action, custodianId, startDate, endDate);
    }
}
