package org.openelisglobal.labunit.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.labunit.dto.LabUnitListResponse;
import org.openelisglobal.labunit.dto.LabUnitResponse;
import org.openelisglobal.labunit.form.LabUnitAssignmentForm;
import org.openelisglobal.labunit.form.LabUnitForm;
import org.openelisglobal.labunit.form.LabUnitOrderForm;
import org.openelisglobal.labunit.form.LabUnitStatusForm;
import org.openelisglobal.labunit.service.LabUnitService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Lab Unit management.
 * 
 * Provides comprehensive CRUD operations for Lab Units including: - Basic CRUD
 * operations (Create, Read, Update, Delete) - Assignment management (Tests,
 * Panels, Programs, Projects) - Status operations (Activate/Deactivate) -
 * Ordering and pagination - Import/Export functionality
 * 
 * Base URL: /rest/api/lab-units
 * 
 * All endpoints require admin privileges (ROLE_GLOBAL_ADMIN)
 */
@RestController
@RequestMapping("/rest/api/lab-units")
public class LabUnitRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(LabUnitRestController.class);

    @Autowired
    private LabUnitService labUnitService;

    @Autowired
    private UserRoleService userRoleService;

    /**
     * GET /api/lab-units - List all lab units with counts and optional status
     * filter
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLabUnits(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "25") int size,
            HttpServletRequest request) {

        try {
            List<LabUnitResponse> labUnits;
            long totalItems;

            if (status != null && !status.trim().isEmpty()) {
                labUnits = labUnitService.getLabUnitsByStatus(status);
                totalItems = "active".equalsIgnoreCase(status) ? labUnitService.getActiveLabUnitsCount()
                        : labUnitService.getInactiveLabUnitsCount();
            } else {
                labUnits = labUnitService.getAllLabUnits();
                totalItems = labUnitService.getTotalLabUnitsCount();
            }

            // Apply pagination
            int start = (page - 1) * size;
            int end = Math.min(start + size, labUnits.size());

            if (start >= labUnits.size()) {
                LabUnitListResponse response = new LabUnitListResponse(new ArrayList<>(), page, 0, totalItems, size);
                return ResponseEntity.ok(response);
            }

            List<LabUnitResponse> pagedUnits = labUnits.subList(start, end);
            int totalPages = (int) Math.ceil((double) totalItems / size);

            LabUnitListResponse response = new LabUnitListResponse(pagedUnits, page, totalPages, totalItems, size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving lab units", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve lab units");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/lab-units/{id} - Get lab unit with all assignments
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLabUnit(@PathVariable String id, HttpServletRequest request) {
        try {
            LabUnitResponse labUnit = labUnitService.getLabUnitById(id);
            if (labUnit == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Lab unit not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Load assignments for detailed view
            List<LabUnitResponse> labUnitsWithAssignments = labUnitService.getLabUnitsWithAssignments(id);
            if (!labUnitsWithAssignments.isEmpty()) {
                return ResponseEntity.ok(labUnit);
            }

            return ResponseEntity.ok(labUnitsWithAssignments.get(0));

        } catch (Exception e) {
            logger.error("Error retrieving lab unit by id: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve lab unit");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units - Create new lab unit
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createLabUnit(@Valid @RequestBody LabUnitForm form, HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            LabUnitResponse created = labUnitService.createLabUnit(form);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (Exception e) {
            logger.error("Error creating lab unit", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * PUT /api/lab-units/{id} - Update lab unit
     */
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateLabUnit(@PathVariable String id, @Valid @RequestBody LabUnitForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            LabUnitResponse updated = labUnitService.updateLabUnit(id, form);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            logger.error("Error updating lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/lab-units/{id} - Soft delete lab unit
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteLabUnit(@PathVariable String id, HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.deleteLabUnit(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lab unit deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error deleting lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * PUT /api/lab-units/reorder - Update display order for multiple units
     */
    @PutMapping(value = "/reorder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reorderLabUnits(@Valid @RequestBody LabUnitOrderForm form, HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.updateLabUnitOrder(List.of(form.getItems()));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lab units reordered successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error reordering lab units", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /api/lab-units/{id}/tests - List assigned tests
     */
    @GetMapping(value = "/{id}/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLabUnitTests(@PathVariable String id, HttpServletRequest request) {

        try {
            List<LabUnitResponse> labUnitsWithAssignments = labUnitService.getLabUnitsWithAssignments(id);

            if (labUnitsWithAssignments.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            LabUnitResponse labUnit = labUnitsWithAssignments.get(0);
            return ResponseEntity.ok(labUnit.getTests());

        } catch (Exception e) {
            logger.error("Error retrieving lab unit tests: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve lab unit tests");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/tests - Assign tests
     */
    @PostMapping(value = "/{id}/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignTests(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.assignTestsToLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tests assigned successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error assigning tests to lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/lab-units/{id}/tests - Remove test assignments
     */
    @DeleteMapping(value = "/{id}/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeTests(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.removeTestsFromLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Test assignments removed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error removing tests from lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/tests/reassign - Bulk reassign tests
     */
    @PostMapping(value = "/{id}/tests/reassign", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reassignTests(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            if (form.getTargetLabUnitId() == null || form.getTargetLabUnitId().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Target lab unit ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            labUnitService.reassignTestsToLabUnit(id, form.getItemIds(), form.getTargetLabUnitId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tests reassigned successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error reassigning tests from lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /api/lab-units/{id}/panels - List assigned panels
     */
    @GetMapping(value = "/{id}/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLabUnitPanels(@PathVariable String id, HttpServletRequest request) {

        try {
            List<LabUnitResponse> labUnitsWithAssignments = labUnitService.getLabUnitsWithAssignments(id);

            if (labUnitsWithAssignments.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            LabUnitResponse labUnit = labUnitsWithAssignments.get(0);
            return ResponseEntity.ok(labUnit.getPanels());

        } catch (Exception e) {
            logger.error("Error retrieving lab unit panels: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve lab unit panels");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/panels - Assign panels
     */
    @PostMapping(value = "/{id}/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignPanels(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.assignPanelsToLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Panels assigned successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error assigning panels to lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/lab-units/{id}/panels - Remove panel assignments
     */
    @DeleteMapping(value = "/{id}/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removePanels(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.removePanelsFromLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Panel assignments removed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error removing panels from lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/activate - Activate lab unit
     */
    @PostMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> activateLabUnit(@PathVariable String id, @Valid @RequestBody LabUnitStatusForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.activateLabUnit(id, form.getCascade(), form.getReason(), getCurrentUserId(request));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lab unit activated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error activating lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/deactivate - Deactivate lab unit
     */
    @PostMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deactivateLabUnit(@PathVariable String id, @Valid @RequestBody LabUnitStatusForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.deactivateLabUnit(id, form.getCascade(), form.getReason(), getCurrentUserId(request));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lab unit deactivated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error deactivating lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /api/lab-units/{id}/programs - List assigned programs
     */
    @GetMapping(value = "/{id}/programs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLabUnitPrograms(@PathVariable String id, HttpServletRequest request) {

        try {
            List<LabUnitResponse> labUnitsWithAssignments = labUnitService.getLabUnitsWithAssignments(id);

            if (labUnitsWithAssignments.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            LabUnitResponse labUnit = labUnitsWithAssignments.get(0);
            return ResponseEntity.ok(labUnit.getPrograms());

        } catch (Exception e) {
            logger.error("Error retrieving lab unit programs: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve lab unit programs");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/programs - Assign programs
     */
    @PostMapping(value = "/{id}/programs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignPrograms(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.assignProgramsToLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Programs assigned successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error assigning programs to lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/lab-units/{id}/programs - Remove program assignments
     */
    @DeleteMapping(value = "/{id}/programs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removePrograms(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.removeProgramsFromLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Program assignments removed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error removing programs from lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /api/lab-units/{id}/projects - List assigned projects
     */
    @GetMapping(value = "/{id}/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLabUnitProjects(@PathVariable String id, HttpServletRequest request) {

        try {
            List<LabUnitResponse> labUnitsWithAssignments = labUnitService.getLabUnitsWithAssignments(id);

            if (labUnitsWithAssignments.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            LabUnitResponse labUnit = labUnitsWithAssignments.get(0);
            return ResponseEntity.ok(labUnit.getProjects());

        } catch (Exception e) {
            logger.error("Error retrieving lab unit projects: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve lab unit projects");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/projects - Assign projects
     */
    @PostMapping(value = "/{id}/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignProjects(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.assignProjectsToLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Projects assigned successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error assigning projects to lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/lab-units/{id}/projects - Remove project assignments
     */
    @DeleteMapping(value = "/{id}/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeProjects(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.removeProjectsFromLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Project assignments removed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error removing projects from lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /api/lab-units/{id}/workflows - List assigned workflows
     */
    @GetMapping(value = "/{id}/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getLabUnitWorkflows(@PathVariable String id, HttpServletRequest request) {

        try {
            List<LabUnitResponse> labUnitsWithAssignments = labUnitService.getLabUnitsWithAssignments(id);

            if (labUnitsWithAssignments.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            LabUnitResponse labUnit = labUnitsWithAssignments.get(0);
            return ResponseEntity.ok(labUnit.getWorkflows());

        } catch (Exception e) {
            logger.error("Error retrieving lab unit workflows: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve lab unit workflows");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units/{id}/workflows - Assign workflows
     */
    @PostMapping(value = "/{id}/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> assignWorkflows(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.assignWorkflowsToLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Workflows assigned successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error assigning workflows to lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE /api/lab-units/{id}/workflows - Remove workflow assignments
     */
    @DeleteMapping(value = "/{id}/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeWorkflows(@PathVariable String id, @Valid @RequestBody LabUnitAssignmentForm form,
            HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            labUnitService.removeWorkflowsFromLabUnit(id, form.getItemIds());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Workflow assignments removed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error removing workflows from lab unit: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /api/lab-units/export - Export selected lab units
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> exportLabUnits(@RequestParam(required = false) String ids,
            @RequestParam(defaultValue = "csv") String format, HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            List<String> labUnitIds = null;
            if (ids != null && !ids.trim().isEmpty()) {
                labUnitIds = List.of(ids.split(","));
            }

            byte[] exportData = labUnitService.exportLabUnits(labUnitIds, format);

            String filename = "lab-units." + format;
            String contentType = "csv".equalsIgnoreCase(format) ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", contentType).body(exportData);

        } catch (Exception e) {
            logger.error("Error exporting lab units", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units/import/validate - Validate import file
     */
    @PostMapping(value = "/import/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateImport(@RequestParam("file") byte[] fileData,
            @RequestParam(defaultValue = "csv") String format, HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            List<String> validationErrors = labUnitService.validateLabUnitImport(fileData, format);

            Map<String, Object> response = new HashMap<>();
            if (validationErrors.isEmpty()) {
                response.put("valid", true);
                response.put("message", "File is valid for import");
            } else {
                response.put("valid", false);
                response.put("errors", validationErrors);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating lab unit import", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to validate import file");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/lab-units/import - Execute import
     */
    @PostMapping(value = "/import", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> executeImport(@RequestParam("file") byte[] fileData,
            @RequestParam(defaultValue = "csv") String format, HttpServletRequest request) {

        try {

            if (!checkAdminStatus(request)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            List<LabUnitResponse> imported = labUnitService.importLabUnits(fileData, format);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lab units imported successfully");
            response.put("count", imported.size());
            response.put("items", imported);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error importing lab units", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Helper method to check admin status
     */
    private boolean checkAdminStatus(HttpServletRequest request) {
        try {
            String sysUserId = getCurrentUserId(request);
            if (sysUserId == null) {
                return false;
            }
            return userRoleService.userInRole(sysUserId, Constants.ROLE_GLOBAL_ADMIN);
        } catch (Exception e) {
            logger.debug("Could not determine admin status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get current system user ID from security context
     */
    private String getCurrentUserId(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "1"; // Fallback to system user
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            } else {
                return "1"; // Fallback to system user
            }
        } catch (Exception e) {
            logger.debug("Could not get user ID: {}", e.getMessage());
            return "1"; // Fallback to system user
        }
    }
}