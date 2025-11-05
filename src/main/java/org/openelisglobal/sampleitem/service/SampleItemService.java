package org.openelisglobal.sampleitem.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public interface SampleItemService extends BaseObjectService<SampleItem, String> {
    void getData(SampleItem sampleItem);

    SampleItem getData(String sampleItemId);

    List<SampleItem> getSampleItemsBySampleIdAndType(String sampleId, TypeOfSample typeOfSample);

    List<SampleItem> getPageOfSampleItems(int startingRecNo);

    List<SampleItem> getAllSampleItems();

    List<SampleItem> getSampleItemsBySampleId(String id);

    List<SampleItem> getSampleItemsBySampleIdAndStatus(String id, Set<Integer> includedStatusList);

    void getDataBySample(SampleItem sampleItem);

    String getTypeOfSampleId(SampleItem sampleItem);

    List<SampleItem> getSampleItemsByExternalID(String externalId);

    boolean insertAliquots(SampleItem lastSampleItem, List<SampleItem> sampleItemsToInsert,
            List<List<String>> analysisGroups);
}
