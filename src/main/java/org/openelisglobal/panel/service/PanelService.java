package org.openelisglobal.panel.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.form.PanelExportRequest;
import org.openelisglobal.panel.form.PanelExportResponse;
import org.openelisglobal.panel.form.PanelForm;
import org.openelisglobal.panel.form.PanelImportPreviewResponse;
import org.openelisglobal.panel.form.PanelImportRequest;
import org.openelisglobal.panel.form.PanelImportResponse;
import org.openelisglobal.panel.form.PanelTestForm;
import org.openelisglobal.panel.valueholder.Panel;

public interface PanelService extends BaseObjectService<Panel, String> {

    void getData(Panel panel);

    String getIdForPanelName(String name);

    String getDescriptionForPanelId(String id);

    String getNameForPanelId(String panelId);

    List<Panel> getAllActivePanels();

    Integer getTotalPanelCount();

    List<Panel> getActivePanels(String filter);

    Panel getPanelByName(String panelName);

    Panel getPanelByName(Panel panel);

    Panel getPanelById(String id);

    List<Panel> getPageOfPanels(int startingRecNo);

    List<Panel> getAllPanels();

    Localization getLocalizationForPanel(String id);

    Panel getPanelByLoincCode(String loincCode);

    List<PanelForm> listForms(Boolean active, String labUnitId);

    List<PanelForm> listForms(Boolean active, String labUnitId, String search);

    List<PanelForm> listForms(Boolean active);

    PanelForm duplicatePanel(String id);

    PanelForm getForm(String id, boolean includeTests);

    PanelForm getForm(String id);

    PanelForm createForm(PanelCreateForm req);

    PanelForm updateForm(String id, PanelCreateForm req);

    boolean deleteById(String id);

    // Lab Units management
    List<String> getLabUnitIds(String panelId);

    void updateLabUnits(String panelId, List<String> labUnitIds);

    // Sample Types management
    List<String> getSampleTypeIds(String panelId);

    void updateSampleTypes(String panelId, List<String> sampleTypeIds);

    // Panel Tests management
    List<PanelTestForm> getPanelTests(String panelId);

    void addPanelTest(String panelId, String testId, Integer displayOrder, String panelLoincCode);

    void updatePanelTest(String panelId, String testId, Integer displayOrder, String panelLoincCode);

    void updateAllPanelTests(String panelId, List<PanelTestForm> tests);

    void removePanelTest(String panelId, String testId);

    void reorderPanelTests(String panelId, List<String> testIdsInOrder);

    PanelExportResponse exportPanels(PanelExportRequest request);

    PanelImportPreviewResponse validateImport(PanelImportRequest request);

    PanelImportResponse executeImport(PanelImportRequest request);

}
