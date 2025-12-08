package org.openelisglobal.sampleitem.service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleItemServiceImpl extends AuditableBaseObjectServiceImpl<SampleItem, String>
        implements SampleItemService {

    private static String SAMPLE_ITEM_TABLE_REFERENCE_ID;

    @Autowired
    protected SampleItemDAO baseObjectDAO;

    @Autowired
    private ReferenceTablesService refTableService;

    @PostConstruct
    public void initializeGlobalVariables() {
        SAMPLE_ITEM_TABLE_REFERENCE_ID = refTableService.getReferenceTableByName("SAMPLE_ITEM").getId();
    }

    SampleItemServiceImpl() {
        super(SampleItem.class);
        this.auditTrailLog = true;
    }

    public static String getSampleItemTableReferenceId() {
        return SAMPLE_ITEM_TABLE_REFERENCE_ID;
    }

    @Override
    protected SampleItemDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public String insert(SampleItem sampleItem) {
        if (sampleItem.getFhirUuid() == null) {
            sampleItem.setFhirUuid(UUID.randomUUID());
        }
        return super.insert(sampleItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getSampleItemsBySampleId(String id) {

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("sample.id", id);
        criteria.put("voided", false);

        return baseObjectDAO.getAllMatching(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getSampleItemsBySampleIdAndStatus(String id, Set<Integer> enteredStatusSampleList) {
        return baseObjectDAO.getSampleItemsBySampleIdAndStatus(id, enteredStatusSampleList);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SampleItem sampleItem) {
        getBaseObjectDAO().getData(sampleItem);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleItem getData(String sampleItemId) {
        return getBaseObjectDAO().getData(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getSampleItemsBySampleIdAndType(String sampleId, TypeOfSample typeOfSample) {
        return getBaseObjectDAO().getSampleItemsBySampleIdAndType(sampleId, typeOfSample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getPageOfSampleItems(int startingRecNo) {
        return getBaseObjectDAO().getPageOfSampleItems(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getAllSampleItems() {
        return getBaseObjectDAO().getAllSampleItems();
    }

    @Override
    @Transactional(readOnly = true)
    public void getDataBySample(SampleItem sampleItem) {
        getBaseObjectDAO().getDataBySample(sampleItem);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTypeOfSampleId(SampleItem sampleItem) {
        sampleItem = get(sampleItem.getId());
        return sampleItem.getTypeOfSampleId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getSampleItemsByExternalID(String externalId) {
        return getBaseObjectDAO().getSampleItemsByExternalID(externalId);
    }

    @Override
    public boolean insertAliquots(SampleItem lastSampleItem, List<SampleItem> sampleItemsToInsert,
            List<List<String>> analysisGroups) {
        getBaseObjectDAO().insertAliquots(lastSampleItem, sampleItemsToInsert, analysisGroups);
        return true;
    }
}
