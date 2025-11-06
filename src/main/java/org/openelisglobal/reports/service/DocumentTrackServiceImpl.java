package org.openelisglobal.reports.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.reports.dao.DocumentTrackDAO;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentTrackServiceImpl extends AuditableBaseObjectServiceImpl<DocumentTrack, String>
        implements DocumentTrackService {
    @Autowired
    protected DocumentTrackDAO baseObjectDAO;

    DocumentTrackServiceImpl() {
        super(DocumentTrack.class);
        this.auditTrailLog = true;
    }

    @Override
    protected DocumentTrackDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTrack> getByTypeRecordAndTable(String typeId, String tableId, String recordId) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("documentTypeId", typeId);
        propertyValues.put("tableId", tableId);
        propertyValues.put("recordId", recordId);
        return baseObjectDAO.getAllMatchingOrdered(propertyValues, "reportTime", false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTrack> getByTypeRecordAndTableAndName(String reportTypeId, String tableId, String recordId,
            String name) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("documentTypeId", reportTypeId);
        propertyValues.put("tableId", tableId);
        propertyValues.put("recordId", recordId);
        propertyValues.put("documentName", name);
        return baseObjectDAO.getAllMatchingOrdered(propertyValues, "reportTime", false);
    }
}
