package org.openelisglobal.common.rest.bean;

import java.util.List;

public class NceSampleInfo {
    private String id;
    private String labOrderNumber;
    private List<NceSampleItemInfo> sampleItems;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabOrderNumber() {
        return labOrderNumber;
    }

    public void setLabOrderNumber(String labOrderNumber) {
        this.labOrderNumber = labOrderNumber;
    }

    public List<NceSampleItemInfo> getSampleItems() {
        return sampleItems;
    }

    public void setSampleItems(List<NceSampleItemInfo> sampleItems) {
        this.sampleItems = sampleItems;
    }
}
