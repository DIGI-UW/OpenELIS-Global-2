package org.openelisglobal.panel.controller;

import java.util.List;
import java.util.Map;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.form.PanelExportRequest;
import org.openelisglobal.panel.form.PanelForm;
import org.openelisglobal.panel.form.PanelImportRequest;
import org.openelisglobal.panel.form.PanelTestForm;
import org.openelisglobal.panel.service.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/panel")
public class PanelRestController {

    private final PanelService panelService;

    @Autowired
    public PanelRestController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping
    public ResponseEntity<List<PanelForm>> list(@RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String labUnitId) {
        return ResponseEntity.ok(panelService.listForms(active, labUnitId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PanelForm> get(@PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean includeTests) {
        PanelForm dto = panelService.getForm(id, includeTests);
        if (dto == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<PanelForm> create(@Validated @RequestBody PanelCreateForm request) {
        PanelForm created = panelService.createForm(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PanelForm> update(@PathVariable String id, @Validated @RequestBody PanelCreateForm request) {
        PanelForm updated = panelService.updateForm(id, request);
        if (updated == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = panelService.deleteById(id);
        if (!deleted)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }

    // Lab Units endpoints
    @GetMapping("/{id}/lab-units")
    public ResponseEntity<List<String>> getLabUnits(@PathVariable String id) {
        List<String> labUnitIds = panelService.getLabUnitIds(id);
        return ResponseEntity.ok(labUnitIds);
    }

    @PutMapping("/{id}/lab-units")
    public ResponseEntity<Void> updateLabUnits(@PathVariable String id, @RequestBody List<String> labUnitIds) {
        panelService.updateLabUnits(id, labUnitIds);
        return ResponseEntity.ok().build();
    }

    // Sample Types endpoints
    @GetMapping("/{id}/sample-types")
    public ResponseEntity<List<String>> getSampleTypes(@PathVariable String id) {
        List<String> sampleTypeIds = panelService.getSampleTypeIds(id);
        return ResponseEntity.ok(sampleTypeIds);
    }

    @PutMapping("/{id}/sample-types")
    public ResponseEntity<Void> updateSampleTypes(@PathVariable String id, @RequestBody List<String> sampleTypeIds) {
        panelService.updateSampleTypes(id, sampleTypeIds);
        return ResponseEntity.ok().build();
    }

    // Panel Tests endpoints
    @GetMapping("/{id}/tests")
    public ResponseEntity<List<PanelTestForm>> getPanelTests(@PathVariable String id) {
        List<PanelTestForm> tests = panelService.getPanelTests(id);
        return ResponseEntity.ok(tests);
    }

    @PostMapping("/{id}/tests")
    public ResponseEntity<Void> addPanelTest(@PathVariable String id, @RequestBody Map<String, Object> request) {
        String testId = (String) request.get("testId");
        Integer displayOrder = request.get("displayOrder") != null
                ? Integer.parseInt(request.get("displayOrder").toString())
                : null;
        String panelLoincCode = (String) request.get("panelLoincCode");
        panelService.addPanelTest(id, testId, displayOrder, panelLoincCode);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/tests")
    public ResponseEntity<Void> updateAllPanelTests(@PathVariable String id, @RequestBody List<PanelTestForm> tests) {
        panelService.updateAllPanelTests(id, tests);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/tests/{testId}")
    public ResponseEntity<Void> updatePanelTest(@PathVariable String id, @PathVariable String testId,
            @RequestBody Map<String, Object> request) {
        Integer displayOrder = request.get("displayOrder") != null
                ? Integer.parseInt(request.get("displayOrder").toString())
                : null;
        String panelLoincCode = (String) request.get("panelLoincCode");
        panelService.updatePanelTest(id, testId, displayOrder, panelLoincCode);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/tests/{testId}")
    public ResponseEntity<Void> removePanelTest(@PathVariable String id, @PathVariable String testId) {
        panelService.removePanelTest(id, testId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/tests/reorder")
    public ResponseEntity<Void> reorderPanelTests(@PathVariable String id, @RequestBody List<String> testIdsInOrder) {
        panelService.reorderPanelTests(id, testIdsInOrder);
        return ResponseEntity.ok().build();
    }

    // Import/Export endpoints
    @PostMapping("/export")
    public ResponseEntity<Object> exportPanels(@RequestBody PanelExportRequest request) {
        Object result = panelService.exportPanels(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import/validate")
    public ResponseEntity<Object> validateImport(@RequestBody PanelImportRequest request) {
        Object result = panelService.validateImport(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<Object> executeImport(@RequestBody PanelImportRequest request) {
        Object result = panelService.executeImport(request);
        return ResponseEntity.ok(result);
    }
}
