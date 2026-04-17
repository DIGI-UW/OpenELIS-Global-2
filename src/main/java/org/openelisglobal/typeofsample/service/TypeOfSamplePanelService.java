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

    /**
     * OGC-224 — reconcile SAMPLETYPE_PANEL to the panel's DERIVED sample-type set
     * (distinct sample types of its member tests). The junction is live on the
     * order-entry hot path (per-sample-type panel lists, e-order panel→sample-type
     * resolution), so every membership write must keep it in sync; historically
     * only the legacy PanelCreate/Modify-Test flows wrote it, and only partially.
     */
    void syncPanelSampleTypes(String panelId, String sysUserId);
}
