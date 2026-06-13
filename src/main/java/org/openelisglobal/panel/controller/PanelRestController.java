package org.openelisglobal.panel.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.panel.form.PanelAddTestRequest;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.form.PanelExportRequest;
import org.openelisglobal.panel.form.PanelExportResponse;
import org.openelisglobal.panel.form.PanelForm;
import org.openelisglobal.panel.form.PanelImportPreviewResponse;
import org.openelisglobal.panel.form.PanelImportRequest;
import org.openelisglobal.panel.form.PanelImportResponse;
import org.openelisglobal.panel.form.PanelTestForm;
import org.openelisglobal.panel.service.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/panel")
public class PanelRestController extends BaseRestController {

    private final PanelService panelService;

    @Autowired
    public PanelRestController(PanelService panelService) {
        this.panelService = panelService;
    }

    /**
     * List panels with optional filters.
     *
     * @param active    filter by active status (null = all)
     * @param labUnitId filter by lab unit ID
     * @param search    search by name or code (case-insensitive substring)
     */
    @GetMapping
    public ResponseEntity<List<PanelForm>> list(@RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String labUnitId, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(panelService.listForms(active, labUnitId, search));
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
    public ResponseEntity<PanelForm> create(@Valid @RequestBody PanelCreateForm request) {
        PanelForm created = panelService.createForm(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PanelForm> update(@PathVariable String id, @Valid @RequestBody PanelCreateForm request) {
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

    /**
     * Duplicate a panel (copy with all tests, lab units, sample types). The copy is
     * created as inactive so the user can review before activating.
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<PanelForm> duplicate(@PathVariable String id) {
        PanelForm copy = panelService.duplicatePanel(id);
        if (copy == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(copy);
    }

    // -------------------------------------------------------------------------
    // Lab Units
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/lab-units")
    public ResponseEntity<List<String>> getLabUnits(@PathVariable String id) {
        return ResponseEntity.ok(panelService.getLabUnitIds(id));
    }

    @PutMapping("/{id}/lab-units")
    public ResponseEntity<Void> updateLabUnits(@PathVariable String id, @RequestBody List<String> labUnitIds) {
        panelService.updateLabUnits(id, labUnitIds);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Sample Types
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/sample-types")
    public ResponseEntity<List<String>> getSampleTypes(@PathVariable String id) {
        return ResponseEntity.ok(panelService.getSampleTypeIds(id));
    }

    @PutMapping("/{id}/sample-types")
    public ResponseEntity<Void> updateSampleTypes(@PathVariable String id, @RequestBody List<String> sampleTypeIds) {
        panelService.updateSampleTypes(id, sampleTypeIds);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Panel Tests
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/tests")
    public ResponseEntity<List<PanelTestForm>> getPanelTests(@PathVariable String id) {
        return ResponseEntity.ok(panelService.getPanelTests(id));
    }

    @PostMapping("/{id}/tests")
    public ResponseEntity<Void> addPanelTest(@PathVariable String id, @Valid @RequestBody PanelAddTestRequest request) {
        panelService.addPanelTest(id, request.getTestId(), request.getDisplayOrder(), request.getPanelLoincCode());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/tests")
    public ResponseEntity<Void> updateAllPanelTests(@PathVariable String id, @RequestBody List<PanelTestForm> tests) {
        panelService.updateAllPanelTests(id, tests);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/tests/{testId}")
    public ResponseEntity<Void> updatePanelTest(@PathVariable String id, @PathVariable String testId,
            @RequestBody PanelTestForm request) {
        panelService.updatePanelTest(id, testId, request.getDisplayOrder(), request.getPanelLoincCode());
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

    // -------------------------------------------------------------------------
    // Import / Export
    // -------------------------------------------------------------------------

    @PostMapping("/export")
    public ResponseEntity<PanelExportResponse> exportPanels(@Valid @RequestBody PanelExportRequest request) {
        return ResponseEntity.ok(panelService.exportPanels(request));
    }

    @PostMapping("/import/validate")
    public ResponseEntity<PanelImportPreviewResponse> validateImport(@Valid @RequestBody PanelImportRequest request) {
        return ResponseEntity.ok(panelService.validateImport(request));
    }

    @PostMapping("/import")
    public ResponseEntity<PanelImportResponse> executeImport(@Valid @RequestBody PanelImportRequest request) {
        return ResponseEntity.ok(panelService.executeImport(request));
    }
}
