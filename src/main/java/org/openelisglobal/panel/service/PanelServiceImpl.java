package org.openelisglobal.panel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.dao.PanelDAO;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.form.PanelExportRequest;
import org.openelisglobal.panel.form.PanelExportResponse;
import org.openelisglobal.panel.form.PanelForm;
import org.openelisglobal.panel.form.PanelImportPreviewItem;
import org.openelisglobal.panel.form.PanelImportPreviewResponse;
import org.openelisglobal.panel.form.PanelImportRequest;
import org.openelisglobal.panel.form.PanelImportResponse;
import org.openelisglobal.panel.form.PanelTestForm;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelimport.service.PanelImportLogService;
import org.openelisglobal.panelimport.valueholder.PanelImportLog;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.panellabunit.service.PanelLabUnitService;
import org.openelisglobal.panellabunit.valueholder.PanelLabUnit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
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
    @Lazy
    private TestService testService;

    @Autowired
    private PanelImportLogService panelImportLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        validateNoDuplicates(panel);
        baseObjectDAO.clearIDMaps();
        return super.insert(panel);
    }

    @Override
    public Panel save(Panel panel) {
        validateNoDuplicates(panel);
        baseObjectDAO.clearIDMaps();
        return super.save(panel);
    }

    @Override
    public Panel update(Panel panel) {
        validateNoDuplicates(panel);
        baseObjectDAO.clearIDMaps();
        baseObjectDAO.update(panel);
        return getPanelById(panel.getId());
    }

    private void validateNoDuplicates(Panel panel) {
        if (getBaseObjectDAO().duplicatePanelExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + panel.getPanelName());
        }
        if (getBaseObjectDAO().duplicatePanelDescriptionExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel description");
        }
        if (getBaseObjectDAO().duplicatePanelCodeExists(panel)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for panel code: " + panel.getCode());
        }
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
        return listForms(active, labUnitId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelForm> listForms(Boolean active, String labUnitId, String search) {
        List<Panel> panels;

        // If lab unit filter is specified, filter at database level
        if (labUnitId != null && !labUnitId.isEmpty()) {
            panels = baseObjectDAO.getPanelsByLabUnitId(labUnitId, active);
        } else {
            // No lab unit filter - use standard queries
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
        }

        // Apply name/code search filter (case-insensitive substring)
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            panels = panels.stream().filter(p -> {
                String name = p.getPanelName() != null ? p.getPanelName().toLowerCase() : "";
                String code = p.getCode() != null ? p.getCode().toLowerCase() : "";
                return name.contains(lower) || code.contains(lower);
            }).collect(Collectors.toList());
        }

        // Batch load all related data to avoid N+1 queries
        Map<String, List<PanelLabUnit>> labUnitsMap = batchLoadPanelLabUnits(panels);
        Map<String, List<TypeOfSamplePanel>> sampleTypesMap = batchLoadTypeOfSamplePanels(panels);

        List<PanelForm> forms = new ArrayList<>();
        for (Panel p : panels) {
            forms.add(toForm(p, false, labUnitsMap, sampleTypesMap));
        }
        return forms;
    }

    @Override
    @Transactional
    public PanelForm duplicatePanel(String id) {
        Panel source = getPanelById(id);
        if (source == null) {
            return null;
        }

        // Build a unique name, code, and description for the copy
        String baseName = source.getPanelName() + " (Copy)";
        String baseCode = source.getCode() + "_COPY";
        String baseDesc = (source.getDescription() != null && !source.getDescription().isEmpty())
                ? source.getDescription() + " (Copy)"
                : baseName;

        // Resolve name/code/description collisions by appending incrementing suffix
        String copyName = buildUniqueName(baseName);
        String copyCode = buildUniqueCode(baseCode);
        String copyDesc = buildUniqueDescription(baseDesc);

        Panel copy = new Panel();
        copy.setPanelName(copyName);
        copy.setCode(copyCode);
        copy.setDescription(copyDesc);
        copy.setLoinc(source.getLoinc());
        // Copies are created inactive so the user can review before activating
        copy.setIsActive(IActionConstants.NO);

        Localization localization = new Localization();
        localization.setLocalizedValue(copyName);
        localizationService.insert(localization);
        copy.setLocalization(localization);

        getBaseObjectDAO().ensureSequence();
        String newId = insert(copy);
        Panel created = newId == null ? null : getPanelById(newId);
        if (created == null) {
            return null;
        }

        // Copy lab units
        List<PanelLabUnit> sourceLabUnits = panelLabUnitService.getPanelLabUnitsByPanelId(id);
        if (sourceLabUnits != null) {
            for (PanelLabUnit src : sourceLabUnits) {
                PanelLabUnit newPlu = new PanelLabUnit();
                newPlu.setPanelId(created.getId());
                newPlu.setLabUnitId(src.getLabUnitId());
                panelLabUnitService.insert(newPlu);
            }
        }

        // Copy sample types
        List<TypeOfSamplePanel> sourceSampleTypes = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(id);
        if (sourceSampleTypes != null) {
            for (TypeOfSamplePanel src : sourceSampleTypes) {
                TypeOfSamplePanel newTosp = new TypeOfSamplePanel();
                newTosp.setPanelId(created.getId());
                newTosp.setTypeOfSampleId(src.getTypeOfSampleId());
                typeOfSamplePanelService.insert(newTosp);
            }
        }

        // Copy panel tests (items)
        List<PanelItem> sourcePanelItems = panelItemService.getPanelItemsForPanel(id);
        if (sourcePanelItems != null) {
            for (PanelItem src : sourcePanelItems) {
                PanelItem newItem = new PanelItem();
                newItem.setPanel(created);
                newItem.setTest(src.getTest());
                newItem.setSortOrder(src.getSortOrder());
                newItem.setPanelLoincCode(src.getPanelLoincCode());
                panelItemService.insert(newItem);
            }
        }

        return toForm(created, true);
    }

    /** Returns a panel name that does not collide with existing panels. */
    private String buildUniqueName(String base) {
        if (getPanelByName(base) == null) {
            return base;
        }
        for (int i = 2; i < 100; i++) {
            String candidate = base + " " + i;
            if (getPanelByName(candidate) == null) {
                return candidate;
            }
        }
        return base + "_" + System.currentTimeMillis();
    }

    /** Returns a panel code that does not collide with existing panels. */
    private String buildUniqueCode(String base) {
        // truncate to keep within DB column limits
        if (base.length() > 20)
            base = base.substring(0, 20);
        Panel probe = new Panel();
        probe.setCode(base);
        if (!getBaseObjectDAO().duplicatePanelCodeExists(probe)) {
            return base;
        }
        for (int i = 2; i < 100; i++) {
            String candidate = (base + i).substring(0, Math.min((base + i).length(), 20));
            probe.setCode(candidate);
            if (!getBaseObjectDAO().duplicatePanelCodeExists(probe)) {
                return candidate;
            }
        }
        return (base + System.currentTimeMillis()).substring(0, 20);
    }

    /** Returns a panel description that does not collide with existing panels. */
    private String buildUniqueDescription(String base) {
        if (base == null)
            base = "";
        if (base.length() > 200)
            base = base.substring(0, 200);
        Panel probe = new Panel();
        probe.setDescription(base);
        if (!getBaseObjectDAO().duplicatePanelDescriptionExists(probe)) {
            return base;
        }
        for (int i = 2; i < 100; i++) {
            String candidate = base + " " + i;
            probe.setDescription(candidate);
            if (!getBaseObjectDAO().duplicatePanelDescriptionExists(probe)) {
                return candidate;
            }
        }
        return base + " " + System.currentTimeMillis();
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
    @Transactional
    public PanelForm createForm(PanelCreateForm req) {
        Panel p = new Panel();
        p.setPanelName(req.getName());
        p.setCode(req.getCode());
        p.setId(null);
        p.setDescription(req.getDescription() != null ? req.getDescription() : "");
        p.setLoinc(req.getLoincCode());

        p.setIsActive(req.isActive() ? IActionConstants.YES : IActionConstants.NO);

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

        existing.setPanelName(req.getName());
        existing.setCode(req.getCode());
        existing.setDescription(req.getDescription() != null ? req.getDescription() : "");
        existing.setLoinc(req.getLoincCode());
        existing.setIsActive(req.isActive() ? IActionConstants.YES : IActionConstants.NO);

        Panel saved = update(existing);
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
        return toForm(p, includeTests, null, null);
    }

    private PanelForm toForm(Panel p, boolean includeTests, Map<String, List<PanelLabUnit>> labUnitsMap,
            Map<String, List<TypeOfSamplePanel>> sampleTypesMap) {
        PanelForm form = new PanelForm();
        form.setId(p.getId());
        form.setName(p.getPanelName());
        form.setCode(p.getCode() != null ? p.getCode() : p.getId()); // Fallback to ID if code is null (backward
                                                                     // compatibility)
        form.setLoincCode(p.getLoinc());
        form.setDescription(p.getDescription());
        form.setActive("Y".equals(p.getIsActive()));

        // Use batch-loaded data if available, otherwise load individually
        List<PanelLabUnit> labUnits;
        if (labUnitsMap != null) {
            labUnits = labUnitsMap.getOrDefault(p.getId(), new ArrayList<>());
        } else {
            labUnits = panelLabUnitService.getPanelLabUnitsByPanelId(p.getId());
        }
        if (labUnits != null && !labUnits.isEmpty()) {
            form.setLabUnitIds(labUnits.stream().map(PanelLabUnit::getLabUnitId).collect(Collectors.toList()));
        } else {
            form.setLabUnitIds(new ArrayList<>());
        }

        List<TypeOfSamplePanel> sampleTypes;
        if (sampleTypesMap != null) {
            sampleTypes = sampleTypesMap.getOrDefault(p.getId(), new ArrayList<>());
        } else {
            sampleTypes = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(p.getId());
        }
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
     * Batch load PanelLabUnits for multiple panels in a single query to avoid N+1
     * query problem.
     *
     * @param panels list of panels to load lab units for
     * @return map of panelId -> List of PanelLabUnit
     */
    private Map<String, List<PanelLabUnit>> batchLoadPanelLabUnits(List<Panel> panels) {
        Map<String, List<PanelLabUnit>> resultMap = new HashMap<>();
        if (panels == null || panels.isEmpty()) {
            return resultMap;
        }

        try {
            // Convert panel IDs to integers for query
            List<Integer> panelIds = panels.stream().map(p -> {
                try {
                    return Integer.parseInt(p.getId());
                } catch (NumberFormatException e) {
                    return null;
                }
            }).filter(id -> id != null).collect(Collectors.toList());

            if (panelIds.isEmpty()) {
                return resultMap;
            }

            // Batch load all PanelLabUnits in a single query using service method
            List<PanelLabUnit> allLabUnits = panelLabUnitService.getPanelLabUnitsByPanelIds(panelIds);

            // Group by panelId
            for (PanelLabUnit plu : allLabUnits) {
                String panelId = String.valueOf(plu.getPanelId());
                resultMap.computeIfAbsent(panelId, k -> new ArrayList<>()).add(plu);
            }
        } catch (Exception e) {
            // If batch loading fails, return empty map - individual queries will be used
            // as fallback
            LogEvent.logError(e);
        }

        return resultMap;
    }

    /**
     * Batch load TypeOfSamplePanels for multiple panels in a single query to avoid
     * N+1 query problem.
     *
     * @param panels list of panels to load sample types for
     * @return map of panelId -> List of TypeOfSamplePanel
     */
    private Map<String, List<TypeOfSamplePanel>> batchLoadTypeOfSamplePanels(List<Panel> panels) {
        Map<String, List<TypeOfSamplePanel>> resultMap = new HashMap<>();
        if (panels == null || panels.isEmpty()) {
            return resultMap;
        }

        try {
            // Convert panel IDs to integers for query
            List<Integer> panelIds = panels.stream().map(p -> {
                try {
                    return Integer.parseInt(p.getId());
                } catch (NumberFormatException e) {
                    return null;
                }
            }).filter(id -> id != null).collect(Collectors.toList());

            if (panelIds.isEmpty()) {
                return resultMap;
            }

            // Batch load all TypeOfSamplePanels in a single query using service method
            List<TypeOfSamplePanel> allSampleTypes = typeOfSamplePanelService
                    .getTypeOfSamplePanelsForPanelIds(panelIds);

            // Group by panelId
            for (TypeOfSamplePanel tosp : allSampleTypes) {
                String panelId = String.valueOf(tosp.getPanelId());
                resultMap.computeIfAbsent(panelId, k -> new ArrayList<>()).add(tosp);
            }
        } catch (Exception e) {
            // If batch loading fails, return empty map - individual queries will be used
            // as fallback
            LogEvent.logError(e);
        }

        return resultMap;
    }

    /**
     * Sync lab unit associations for a panel. Keeps existing matching records,
     * deletes only removed items, and inserts only new items.
     *
     * @param panelId    the panel ID
     * @param labUnitIds list of lab unit IDs to associate (may be null or empty)
     */
    private void syncLabUnits(String panelId, List<String> labUnitIds) {
        List<PanelLabUnit> existing = panelLabUnitService.getPanelLabUnitsByPanelId(panelId);
        Set<String> newLabUnitIds = labUnitIds != null ? new HashSet<>(labUnitIds) : new HashSet<>();

        // Build map of existing lab unit IDs to PanelLabUnit entities for efficient
        // lookup
        Map<String, PanelLabUnit> existingMap = new HashMap<>();
        if (existing != null) {
            for (PanelLabUnit plu : existing) {
                existingMap.put(plu.getLabUnitId(), plu);
            }
        }

        // Delete only removed items (in existing but not in new list)
        for (Map.Entry<String, PanelLabUnit> entry : existingMap.entrySet()) {
            if (!newLabUnitIds.contains(entry.getKey())) {
                panelLabUnitService.delete(entry.getValue());
            }
        }

        // Insert only new items (in new list but not in existing)
        for (String labUnitId : newLabUnitIds) {
            if (!existingMap.containsKey(labUnitId)) {
                PanelLabUnit plu = new PanelLabUnit();
                plu.setPanelId(panelId);
                plu.setLabUnitId(labUnitId);
                panelLabUnitService.insert(plu);
            }
        }
    }

    /**
     * Sync sample type associations for a panel. Keeps existing matching records,
     * deletes only removed items, and inserts only new items.
     *
     * @param panelId       the panel ID
     * @param sampleTypeIds list of sample type IDs to associate (may be null or
     *                      empty)
     */
    private void syncSampleTypes(String panelId, List<String> sampleTypeIds) {
        List<TypeOfSamplePanel> existing = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(panelId);
        Set<String> newSampleTypeIds = sampleTypeIds != null ? new HashSet<>(sampleTypeIds) : new HashSet<>();

        // Build map of existing sample type IDs to TypeOfSamplePanel entities for
        // efficient lookup
        Map<String, TypeOfSamplePanel> existingMap = new HashMap<>();
        if (existing != null) {
            for (TypeOfSamplePanel tosp : existing) {
                existingMap.put(tosp.getTypeOfSampleId(), tosp);
            }
        }

        // Delete only removed items (in existing but not in new list)
        for (Map.Entry<String, TypeOfSamplePanel> entry : existingMap.entrySet()) {
            if (!newSampleTypeIds.contains(entry.getKey())) {
                typeOfSamplePanelService.delete(entry.getValue());
            }
        }

        // Insert only new items (in new list but not in existing)
        for (String sampleTypeId : newSampleTypeIds) {
            if (!existingMap.containsKey(sampleTypeId)) {
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
    @Transactional
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
    @Transactional
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
    @Transactional
    public void addPanelTest(String panelId, String testId, Integer displayOrder, String panelLoincCode) {
        Panel panel = getPanelById(panelId);
        Test test = testService.getTestById(testId);
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
    @Transactional
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
    @Transactional
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
            Test test = testService.getTestById(form.getTestId());
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
    @Transactional
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
    @Transactional
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
    public PanelExportResponse exportPanels(PanelExportRequest request) {
        List<String> panelIds = request.getPanelIds();
        boolean includeTests = request.isIncludeTests();
        String format = request.getFormat() != null ? request.getFormat().toLowerCase() : "json";

        // Resolve panels to export
        List<Panel> panels;
        if (panelIds != null && !panelIds.isEmpty()) {
            panels = panelIds.stream().map(this::getPanelById).filter(p -> p != null).collect(Collectors.toList());
        } else {
            panels = getAllPanels();
        }

        PanelExportResponse response = new PanelExportResponse();
        if ("csv".equals(format)) {
            response.setCsvData(buildCsvExport(panels, includeTests));
        } else {
            Map<String, Object> json = buildJsonExport(panels, includeTests);
            response.setPanels((List<PanelForm>) json.get("panels"));
            response.setExportedAt((String) json.get("exportedAt"));
            response.setCount((Integer) json.get("count"));
        }
        return response;
    }

    /**
     * Build a JSON-serialisable export structure: { panels: [ { id, name, code,
     * loincCode, description, active, labUnitIds, sampleTypeIds, tests: [...] } ] }
     */
    private Map<String, Object> buildJsonExport(List<Panel> panels, boolean includeTests) {
        List<Map<String, Object>> panelList = new ArrayList<>();
        for (Panel p : panels) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", p.getId());
            entry.put("name", p.getPanelName());
            entry.put("code", p.getCode());
            entry.put("loincCode", p.getLoinc());
            entry.put("description", p.getDescription());
            entry.put("active", "Y".equals(p.getIsActive()));
            entry.put("labUnitIds", getLabUnitIds(p.getId()));
            entry.put("sampleTypeIds", getSampleTypeIds(p.getId()));
            if (includeTests) {
                List<Map<String, Object>> testList = new ArrayList<>();
                for (PanelTestForm tf : getPanelTests(p.getId())) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("testId", tf.getTestId());
                    t.put("testName", tf.getTestName());
                    t.put("testLoincCode", tf.getTestLoincCode());
                    t.put("panelLoincCode", tf.getPanelLoincCode());
                    t.put("displayOrder", tf.getDisplayOrder());
                    testList.add(t);
                }
                entry.put("tests", testList);
            }
            panelList.add(entry);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("panels", panelList);
        result.put("exportedAt", new Timestamp(System.currentTimeMillis()).toString());
        result.put("count", panelList.size());
        return result;
    }

    /**
     * Build a flat CSV string for review purposes. Columns: panelId, panelName,
     * panelCode, panelLoinc, active, testId, testName, testLoinc, panelTestLoinc,
     * displayOrder
     */
    private String buildCsvExport(List<Panel> panels, boolean includeTests) {
        StringBuilder sb = new StringBuilder();
        sb.append("panelId,panelName,panelCode,panelLoinc,active,"
                + "testId,testName,testLoinc,panelTestLoinc,displayOrder\n");
        for (Panel p : panels) {
            String active = "Y".equals(p.getIsActive()) ? "true" : "false";
            if (includeTests) {
                List<PanelTestForm> tests = getPanelTests(p.getId());
                if (tests.isEmpty()) {
                    sb.append(toCsvRow(p.getId(), p.getPanelName(), p.getCode(), p.getLoinc(), active, "", "", "", "",
                            ""));
                } else {
                    for (PanelTestForm t : tests) {
                        sb.append(toCsvRow(p.getId(), p.getPanelName(), p.getCode(), p.getLoinc(), active,
                                t.getTestId(), t.getTestName(), t.getTestLoincCode(), t.getPanelLoincCode(),
                                t.getDisplayOrder() != null ? t.getDisplayOrder().toString() : ""));
                    }
                }
            } else {
                sb.append(toCsvRow(p.getId(), p.getPanelName(), p.getCode(), p.getLoinc(), active, "", "", "", "", ""));
            }
        }
        return sb.toString();
    }

    private String toCsvRow(String... values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String v = values[i] != null ? values[i] : "";
            // RFC 4180: wrap in quotes if the value contains comma, newline, or quote
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                v = "\"" + v.replace("\"", "\"\"") + "\"";
            }
            if (i > 0)
                row.append(",");
            row.append(v);
        }
        row.append("\n");
        return row.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public PanelImportPreviewResponse validateImport(PanelImportRequest request) {
        List<PanelImportPreviewItem> items = new ArrayList<>();
        List<Map<String, Object>> panelDataList = extractPanelList(request.getData());
        String mode = request.getMode() != null ? request.getMode() : "both";

        for (Map<String, Object> panelData : panelDataList) {
            String code = getString(panelData, "code");
            String name = getString(panelData, "name");
            Panel existing = code != null ? getPanelByCode(code) : null;

            String action;
            String reason = null;
            if (existing == null) {
                action = "create".equals(mode) || "both".equals(mode) ? "create" : "skip";
                if ("skip".equals(action))
                    reason = "mode=" + mode + " does not allow create";
            } else {
                action = "update".equals(mode) || "both".equals(mode) ? "update" : "skip";
                if ("skip".equals(action))
                    reason = "mode=" + mode + " does not allow update";
            }

            PanelImportPreviewItem item = new PanelImportPreviewItem();
            item.setCode(code);
            item.setName(name);
            item.setAction(action);
            item.setReason(reason);
            items.add(item);
        }

        PanelImportPreviewResponse response = new PanelImportPreviewResponse();
        response.setPreview(items);
        response.setCounts(Map.of("create", items.stream().filter(r -> "create".equals(r.getAction())).count(),
                "update", items.stream().filter(r -> "update".equals(r.getAction())).count(), "skip",
                items.stream().filter(r -> "skip".equals(r.getAction())).count()));
        return response;
    }

    @Override
    @Transactional
    public PanelImportResponse executeImport(PanelImportRequest request) {
        List<Map<String, Object>> panelDataList = extractPanelList(request.getData());
        String mode = request.getMode() != null ? request.getMode() : "both";

        int createdCount = 0, updatedCount = 0, skippedCount = 0;
        List<String> warnings = new ArrayList<>();

        for (Map<String, Object> panelData : panelDataList) {
            String code = getString(panelData, "code");
            String name = getString(panelData, "name");

            if (code == null || code.isBlank() || name == null || name.isBlank()) {
                warnings.add("Skipped panel with missing code or name: " + panelData);
                skippedCount++;
                continue;
            }

            Panel existing = getPanelByCode(code);
            try {
                if (existing == null) {
                    if ("create".equals(mode) || "both".equals(mode)) {
                        PanelCreateForm req = buildCreateFormFromMap(panelData);
                        createForm(req);
                        createdCount++;
                    } else {
                        skippedCount++;
                    }
                } else {
                    if ("update".equals(mode) || "both".equals(mode)) {
                        PanelCreateForm req = buildCreateFormFromMap(panelData);
                        updateForm(existing.getId(), req);
                        // Re-import tests if present
                        importPanelTests(existing.getId(), panelData);
                        updatedCount++;
                    } else {
                        skippedCount++;
                    }
                }
            } catch (Exception e) {
                warnings.add("Error importing panel '" + code + "': " + e.getMessage());
                skippedCount++;
                LogEvent.logError(e);
            }
        }

        // Write import log
        PanelImportLog log = new PanelImportLog();
        log.setImportDate(new Timestamp(System.currentTimeMillis()));
        log.setPanelsCreated(createdCount);
        log.setPanelsUpdated(updatedCount);
        log.setPanelsSkipped(skippedCount);
        log.setWarnings(warnings.isEmpty() ? null : String.join("; ", warnings));
        try {
            log.setImportData(objectMapper.writeValueAsString(request.getData()));
        } catch (Exception e) {
            log.setImportData("[serialization error]");
        }
        panelImportLogService.insert(log);

        PanelImportResponse response = new PanelImportResponse();
        response.setPanelsCreated(createdCount);
        response.setPanelsUpdated(updatedCount);
        response.setPanelsSkipped(skippedCount);
        if (!warnings.isEmpty())
            response.setWarnings(warnings);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractPanelList(Object data) {
        if (data == null)
            return new ArrayList<>();
        try {
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
            if (data instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) data;
                if (map.containsKey("panels")) {
                    return (List<Map<String, Object>>) map.get("panels");
                }
            }
        } catch (ClassCastException e) {
            LogEvent.logError(e);
        }
        return new ArrayList<>();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }

    /** Find a panel by its unique short code (optimized using DAO). */
    private Panel getPanelByCode(String code) {
        return getBaseObjectDAO().getPanelByCode(code);
    }

    @SuppressWarnings("unchecked")
    private PanelCreateForm buildCreateFormFromMap(Map<String, Object> data) {
        PanelCreateForm req = new PanelCreateForm();
        req.setName(getString(data, "name"));
        req.setCode(getString(data, "code"));
        req.setLoincCode(getString(data, "loincCode"));
        String desc = getString(data, "description");
        req.setDescription(desc != null ? desc : "");
        Object activeVal = data.get("active");
        req.setActive(activeVal == null || Boolean.parseBoolean(activeVal.toString()));
        if (data.get("labUnitIds") instanceof List) {
            req.setLabUnitIds((List<String>) data.get("labUnitIds"));
        }
        if (data.get("sampleTypeIds") instanceof List) {
            req.setSampleTypeIds((List<String>) data.get("sampleTypeIds"));
        }
        return req;
    }

    @SuppressWarnings("unchecked")
    private void importPanelTests(String panelId, Map<String, Object> panelData) {
        Object testsObj = panelData.get("tests");
        if (!(testsObj instanceof List))
            return;
        List<Map<String, Object>> tests = (List<Map<String, Object>>) testsObj;
        for (Map<String, Object> t : tests) {
            String testId = getString(t, "testId");
            if (testId == null)
                continue;
            String loincCode = getString(t, "panelLoincCode");
            Object orderObj = t.get("displayOrder");
            Integer order = orderObj != null ? Integer.parseInt(orderObj.toString()) : null;
            try {
                // Skip if already in panel; addPanelTest throws on duplicate
                addPanelTest(panelId, testId, order, loincCode);
            } catch (LIMSDuplicateRecordException e) {
                // Test already in panel — update instead
                updatePanelTest(panelId, testId, order, loincCode);
            } catch (Exception e) {
                LogEvent.logError(e);
            }
        }
    }
}
