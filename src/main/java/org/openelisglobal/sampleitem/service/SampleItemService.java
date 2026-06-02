package org.openelisglobal.sampleitem.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleItemService extends BaseObjectService<SampleItem, String> {
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    void getData(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    SampleItem getData(String sampleItemId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleItem> getSampleItemsBySampleIdAndType(String sampleId, TypeOfSample typeOfSample);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleItem> getPageOfSampleItems(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleItem> getAllSampleItems();

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleItem> getSampleItemsBySampleId(String id);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleItem> getSampleItemsBySampleIdAndStatus(String id, Set<String> includedStatusList);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    void getDataBySample(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    String getTypeOfSampleId(SampleItem sampleItem);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleItem> getSampleItemsByExternalID(String externalId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_EDIT')")
    boolean insertAliquots(SampleItem lastSampleItem, List<SampleItem> sampleItemsToInsert,
            List<List<String>> analysisGroups);
}
