package org.openelisglobal.panel.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.dao.PanelDAO;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.form.PanelExportRequest;
import org.openelisglobal.panel.form.PanelForm;
import org.openelisglobal.panel.form.PanelImportRequest;
import org.openelisglobal.panel.form.PanelTestForm;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.panellabunit.service.PanelLabUnitService;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;
import org.openelisglobal.test.dao.TestDAO;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class PanelServiceImpl extends AuditableBaseObjectServiceImpl<Panel, String> implements PanelService {

    @Autowired
    protected PanelDAO baseObjectDAO;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private PanelLabUnitService panelLabUnitService;

    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private TestDAO testDAO;

    PanelServiceImpl() {
        super(Panel.class);
        this.auditTrailLog = true;
    }

    @Override
    protected PanelDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Panel panel) {
        getBaseObjectDAO().getData(panel);
    }

    @Override
    @Transactional(readOnly = true)
    public String getIdForPanelName(String name) {
        return getBaseObjectDAO().getIdForPanelName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDescriptionForPanelId(String id) {
        return getBaseObjectDAO().getDescriptionForPanelId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNameForPanelId(String panelId) {
        return getBaseObjectDAO().getNameForPanelId(panelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getAllActivePanels() {
        return getBaseObjectDAO().getAllActivePanels();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPanelCount() {
        return getBaseObjectDAO().getTotalPanelCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getActivePanels(String filter) {
        return getBaseObjectDAO().getActivePanels(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public Panel getPanelByName(String panelName) {
        return getBaseObjectDAO().getPanelByName(panelName);
    }

    @Override
    @Transactional(readOnly = true)
    public Panel getPanelByName(Panel panel) {
        return getBaseObjectDAO().getPanelByName(panel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getPageOfPanels(int startingRecNo) {
        return getBaseObjectDAO().getPageOfPanels(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getAllPanels() {
        return baseObjectDAO.getAllPanels();
    }

    @Override
    @Transactional(readOnly = true)
    public Panel getPanelById(String id) {
        return baseObjectDAO.getPanelById(id);
    }

    @Override
    public String insert(Panel panel) {
        // set version fields early to avoid optimistic locking issues when duplicate
        // checks trigger a flush
        if (panel.getLastupdated() == null) {
            panel.setLastupdatedFields();
        }

        if (getBaseObjectDAO().duplicatePanelExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + panel.getPanelName());
        }
        if (getBaseObjectDAO().duplicatePanelDescriptionExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel description");
        }
        if (getBaseObjectDAO().duplicatePanelCodeExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel code: " + panel.getCode());
        }
        baseObjectDAO.clearIDMaps();
        return super.insert(panel);
    }

    @Override
    public Panel save(Panel panel) {
        // set version fields early to avoid optimistic locking issues when duplicate
        // checks trigger a flush
        if (panel.getLastupdated() == null) {
            panel.setLastupdatedFields();
        }

        if (getBaseObjectDAO().duplicatePanelExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + panel.getPanelName());
        }
        if (getBaseObjectDAO().duplicatePanelDescriptionExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel description");
        }
        if (getBaseObjectDAO().duplicatePanelCodeExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel code: " + panel.getCode());
        }
        baseObjectDAO.clearIDMaps();
        return super.save(panel);
    }

    @Override
    public Panel update(Panel panel) {
        // set version fields early to avoid optimistic locking issues when duplicate
        // checks trigger a flush
        if (panel.getLastupdated() == null) {
            panel.setLastupdatedFields();
        }

        if (getBaseObjectDAO().duplicatePanelExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + panel.getPanelName());
        }
        if (getBaseObjectDAO().duplicatePanelDescriptionExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel description");
        }
        if (getBaseObjectDAO().duplicatePanelCodeExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel code: " + panel.getCode());
        }
        baseObjectDAO.clearIDMaps();
        // Use a DAO bulk update to avoid optimistic locking failures when legacy rows
        // have null version timestamps. This updates the key fields and sets
        // lastupdated
        // to the current time in a single statement.
        baseObjectDAO.updatePanelFields(panel);
        return getPanelById(panel.getId());
    }

    @Override
    public void delete(Panel panel) {
        super.delete(panel);
        baseObjectDAO.clearIDMaps();
    }

    @Override
    public Localization getLocalizationForPanel(String id) {
        Panel panel = getPanelById(id);
        Localization localization = panel != null ? panel.getLocalization() : null;
        Hibernate.initialize(localization);
        return localization;
    }

    @Override
    public Panel getPanelByLoincCode(String loincCode) {
        return getBaseObjectDAO().getPanelByLoincCode(loincCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelForm> listForms(Boolean active) {
        return listForms(active, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelForm> listForms(Boolean active, String labUnitId) {
        List<Panel> panels;
        if (active == null) {
            panels = getAllPanels();
        } else if (Boolean.TRUE.equals(active)) {
            panels = getAllActivePanels();
        } else {
            // No dedicated "inactive only" query exists, so filter in-memory.
            panels = new ArrayList<>();
            for (Panel panel : getAllPanels()) {
                if (!"Y".equals(panel.getIsActive())) {
                    panels.add(panel);
                }
            }
        }

        // Filter by lab unit if specified
        if (labUnitId != null && !labUnitId.isEmpty()) {
            List<Panel> filteredPanels = new ArrayList<>();
            for (Panel panel : panels) {
                List<PanelLabUnit> labUnits = panelLabUnitService.getPanelLabUnitsByPanelId(panel.getId());
                if (labUnits != null) {
                    for (PanelLabUnit plu : labUnits) {
                        if (labUnitId.equals(plu.getLabUnitId())) {
                            filteredPanels.add(panel);
                            break;
                        }
                    }
                }
            }
            panels = filteredPanels;
        }

        List<PanelForm> forms = new ArrayList<>();
        for (Panel p : panels) {
            forms.add(toForm(p, false));
        }
        return forms;
    }

    @Override
    @Transactional(readOnly = true)
    public PanelForm getForm(String id) {
        return getForm(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PanelForm getForm(String id, boolean includeTests) {
        Panel p = getPanelById(id);
        return p == null ? null : toForm(p, includeTests);
    }

    @Override
    public PanelForm createForm(PanelCreateForm req) {
        Panel p = new Panel();
        p.setPanelName(req.getName());
        p.setCode(req.getCode());
        p.setId(null);
        p.setDescription(req.getDescription());
        p.setLoinc(req.getLoincCode());

        p.setIsActive(req.isActive() ? "Y" : "N");

        Localization localization = new Localization();
        localization.setLocalizedValue(req.getName());
        localizationService.insert(localization);
        p.setLocalization(localization);

        getBaseObjectDAO().ensureSequence();

        String newId = insert(p);
        Panel created = newId == null ? null : getPanelById(newId);
        if (created == null) {
            return null;
        }

        syncLabUnits(created.getId(), req.getLabUnitIds());
        syncSampleTypes(created.getId(), req.getSampleTypeIds());

        return toForm(created, false);
    }

    @Override
    @Transactional
    public PanelForm updateForm(String id, PanelCreateForm req) {
        Panel existing = getPanelById(id);
        if (existing == null) {
            return null;
        }

        Panel toUpdate = new Panel();
        toUpdate.setId(id);
        toUpdate.setPanelName(req.getName());
        toUpdate.setCode(req.getCode());
        toUpdate.setDescription(req.getDescription());
        toUpdate.setLoinc(req.getLoincCode());
        toUpdate.setIsActive(req.isActive() ? "Y" : "N");

        toUpdate.setLastupdated(existing.getLastupdated());

        Panel saved = update(toUpdate);
        if (saved == null) {
            return null;
        }

        syncLabUnits(saved.getId(), req.getLabUnitIds());
        syncSampleTypes(saved.getId(), req.getSampleTypeIds());

        return toForm(saved, false);
    }

    @Override
    public boolean deleteById(String id) {
        Panel existing = getPanelById(id);
        if (existing == null) {
            return false;
        }
        delete(existing);
        return true;
    }

    private PanelForm toForm(Panel p, boolean includeTests) {
        PanelForm form = new PanelForm();
        form.setId(p.getId());
        form.setName(p.getPanelName());
        form.setCode(p.getCode() != null ? p.getCode() : p.getId()); // Fallback to ID if code is null (backward
                                                                     // compatibility)
        form.setLoincCode(p.getLoinc());
        form.setDescription(p.getDescription());
        form.setActive("Y".equals(p.getIsActive()));

        List<PanelLabUnit> labUnits = panelLabUnitService.getPanelLabUnitsByPanelId(p.getId());
        if (labUnits != null && !labUnits.isEmpty()) {
            form.setLabUnitIds(labUnits.stream().map(PanelLabUnit::getLabUnitId).collect(Collectors.toList()));
        } else {
            form.setLabUnitIds(new ArrayList<>());
        }

        List<TypeOfSamplePanel> sampleTypes = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(p.getId());
        if (sampleTypes != null && !sampleTypes.isEmpty()) {
            form.setSampleTypeIds(
                    sampleTypes.stream().map(TypeOfSamplePanel::getTypeOfSampleId).collect(Collectors.toList()));
        } else {
            form.setSampleTypeIds(new ArrayList<>());
        }

        if (includeTests) {
            form.setTests(getPanelTests(p.getId()));
        }

        return form;
    }

    /**
     * Sync lab unit associations for a panel. Deletes existing associations and
     * creates new ones.
     *
     * @param panelId    the panel ID
     * @param labUnitIds list of lab unit IDs to associate (may be null or empty)
     */
    private void syncLabUnits(String panelId, List<String> labUnitIds) {
        // Delete existing lab unit associations
        List<PanelLabUnit> existing = panelLabUnitService.getPanelLabUnitsByPanelId(panelId);
        if (existing != null && !existing.isEmpty()) {
            for (PanelLabUnit plu : existing) {
                panelLabUnitService.delete(plu);
            }
        }

        // Create new lab unit associations
        if (labUnitIds != null && !labUnitIds.isEmpty()) {
            for (String labUnitId : labUnitIds) {
                PanelLabUnit plu = new PanelLabUnit();
                plu.setPanelId(panelId);
                plu.setLabUnitId(labUnitId);
                panelLabUnitService.insert(plu);
            }
        }
    }

    /**
     * Sync sample type associations for a panel. Deletes existing associations and
     * creates new ones.
     *
     * @param panelId       the panel ID
     * @param sampleTypeIds list of sample type IDs to associate (may be null or
     *                      empty)
     */
    private void syncSampleTypes(String panelId, List<String> sampleTypeIds) {
        // Delete existing sample type associations
        List<TypeOfSamplePanel> existing = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(panelId);
        if (existing != null && !existing.isEmpty()) {
            for (TypeOfSamplePanel tosp : existing) {
                typeOfSamplePanelService.delete(tosp);
            }
        }

        if (sampleTypeIds != null && !sampleTypeIds.isEmpty()) {
            for (String sampleTypeId : sampleTypeIds) {
                TypeOfSamplePanel tosp = new TypeOfSamplePanel();
                tosp.setPanelId(panelId);
                tosp.setTypeOfSampleId(sampleTypeId);
                typeOfSamplePanelService.insert(tosp);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getLabUnitIds(String panelId) {
        List<PanelLabUnit> labUnits = panelLabUnitService.getPanelLabUnitsByPanelId(panelId);
        if (labUnits == null || labUnits.isEmpty()) {
            return new ArrayList<>();
        }
        return labUnits.stream().map(PanelLabUnit::getLabUnitId).collect(Collectors.toList());
    }

    @Override
    public void updateLabUnits(String panelId, List<String> labUnitIds) {
        syncLabUnits(panelId, labUnitIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSampleTypeIds(String panelId) {
        List<TypeOfSamplePanel> sampleTypes = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(panelId);
        if (sampleTypes == null || sampleTypes.isEmpty()) {
            return new ArrayList<>();
        }
        return sampleTypes.stream().map(TypeOfSamplePanel::getTypeOfSampleId).collect(Collectors.toList());
    }

    @Override
    public void updateSampleTypes(String panelId, List<String> sampleTypeIds) {
        syncSampleTypes(panelId, sampleTypeIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelTestForm> getPanelTests(String panelId) {
        List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panelId);
        List<PanelTestForm> testForms = new ArrayList<>();

        for (PanelItem item : panelItems) {
            PanelTestForm form = new PanelTestForm();
            Test test = item.getTest();
            if (test != null) {
                form.setTestId(test.getId());
                form.setTestName(TestServiceImpl.getUserLocalizedTestName(test));
                form.setTestLoincCode(test.getLoinc());
            }
            form.setPanelLoincCode(item.getPanelLoincCode());
            form.setDisplayOrder(item.getSortOrder() != null ? Integer.parseInt(item.getSortOrder()) : null);
            testForms.add(form);
        }

        testForms.sort((a, b) -> {
            Integer orderA = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
            Integer orderB = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
            return orderA.compareTo(orderB);
        });

        return testForms;
    }

    @Override
    public void addPanelTest(String panelId, String testId, Integer displayOrder, String panelLoincCode) {
        Panel panel = getPanelById(panelId);
        Test test = testDAO.getTestById(testId);
        if (panel == null || test == null) {
            throw new IllegalArgumentException("Panel or test not found");
        }

        // Check if test already in panel
        List<PanelItem> existing = panelItemService.getPanelItemsForPanel(panelId);
        for (PanelItem item : existing) {
            if (testId.equals(item.getTest().getId())) {
                throw new LIMSDuplicateRecordException("Test already exists in panel");
            }
        }

        PanelItem panelItem = new PanelItem();
        panelItem.setPanel(panel);
        panelItem.setTest(test);
        panelItem.setSortOrder(displayOrder != null ? displayOrder.toString() : null);
        panelItem.setPanelLoincCode(panelLoincCode);
        panelItemService.insert(panelItem);
    }

    @Override
    public void updatePanelTest(String panelId, String testId, Integer displayOrder, String panelLoincCode) {
        List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panelId);
        for (PanelItem item : panelItems) {
            if (testId.equals(item.getTest().getId())) {
                item.setSortOrder(displayOrder != null ? displayOrder.toString() : null);
                item.setPanelLoincCode(panelLoincCode);
                panelItemService.update(item);
                return;
            }
        }
        throw new IllegalArgumentException("Test not found in panel");
    }

    @Override
    public void updateAllPanelTests(String panelId, List<PanelTestForm> tests) {
        List<PanelItem> existingItems = panelItemService.getPanelItemsForPanel(panelId);

        // Create map of testId -> PanelTestForm for quick lookup
        java.util.Map<String, PanelTestForm> testFormMap = new java.util.HashMap<>();
        for (PanelTestForm form : tests) {
            testFormMap.put(form.getTestId(), form);
        }

        for (PanelItem item : existingItems) {
            String testId = item.getTest().getId();
            PanelTestForm form = testFormMap.get(testId);
            if (form != null) {
                item.setSortOrder(form.getDisplayOrder() != null ? form.getDisplayOrder().toString() : null);
                item.setPanelLoincCode(form.getPanelLoincCode());
                panelItemService.update(item);
                testFormMap.remove(testId); // Mark as processed
            }
        }

        Panel panel = getPanelById(panelId);
        for (PanelTestForm form : testFormMap.values()) {
            Test test = testDAO.getTestById(form.getTestId());
            if (test != null) {
                PanelItem newItem = new PanelItem();
                newItem.setPanel(panel);
                newItem.setTest(test);
                newItem.setSortOrder(form.getDisplayOrder() != null ? form.getDisplayOrder().toString() : null);
                newItem.setPanelLoincCode(form.getPanelLoincCode());
                panelItemService.insert(newItem);
            }
        }
    }

    @Override
    public void removePanelTest(String panelId, String testId) {
        List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panelId);
        for (PanelItem item : panelItems) {
            if (testId.equals(item.getTest().getId())) {
                panelItemService.delete(item);
                return;
            }
        }
        throw new IllegalArgumentException("Test not found in panel");
    }

    @Override
    public void reorderPanelTests(String panelId, List<String> testIdsInOrder) {
        List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panelId);

        java.util.Map<String, PanelItem> itemMap = new java.util.HashMap<>();
        for (PanelItem item : panelItems) {
            itemMap.put(item.getTest().getId(), item);
        }

        for (int i = 0; i < testIdsInOrder.size(); i++) {
            PanelItem item = itemMap.get(testIdsInOrder.get(i));
            if (item != null) {
                item.setSortOrder(String.valueOf(i + 1));
                panelItemService.update(item);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Object exportPanels(PanelExportRequest request) {
        // TODO: Implement export logic
        return new java.util.HashMap<String, Object>();
    }

    @Override
    public Object validateImport(PanelImportRequest request) {
        // TODO: Implement validation logic
        return new java.util.HashMap<String, Object>();
    }

    @Override
    public Object executeImport(PanelImportRequest request) {
        // TODO: Implement import logic
        return new java.util.HashMap<String, Object>();
    }
}
