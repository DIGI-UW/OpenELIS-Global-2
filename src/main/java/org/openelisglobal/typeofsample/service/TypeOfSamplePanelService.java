package org.openelisglobal.typeofsample.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
public interface TypeOfSamplePanelService extends BaseObjectService<TypeOfSamplePanel, String> {
    void getData(TypeOfSamplePanel typeOfSamplePanel);

    List<TypeOfSamplePanel> getAllTypeOfSamplePanels();

    List<TypeOfSamplePanel> getPageOfTypeOfSamplePanel(int startingRecNo);

    Integer getTotalTypeOfSamplePanelCount();

    List<TypeOfSamplePanel> getTypeOfSamplePanelsForPanel(String panelId);

    List<TypeOfSamplePanel> getTypeOfSamplePanelsForSampleType(String sampleType);
}
