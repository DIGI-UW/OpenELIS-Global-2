package org.openelisglobal.labunit.serviceimpl;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.labunit.dto.LabUnitResponse;
import org.openelisglobal.labunit.form.LabUnitForm;
import org.openelisglobal.labunit.form.LabUnitOrderForm;
import org.openelisglobal.labunit.service.LabUnitService;
import org.openelisglobal.labunit.valueholder.LabUnit;
import org.openelisglobal.labunit.valueholder.LabUnitAssignment;
import org.openelisglobal.labunit.valueholder.LabUnitWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Lab Unit management operations. Clean
 * implementation using independent LabUnit entity.
 */
@Service
@DependsOn({ "springContext" })
public class LabUnitServiceImpl extends AuditableBaseObjectServiceImpl<LabUnit, String> implements LabUnitService {

    private static final Logger logger = LoggerFactory.getLogger(LabUnitServiceImpl.class);

    @Autowired
    private org.openelisglobal.labunit.dao.LabUnitDAO labUnitDAO;

    @Autowired
    private org.openelisglobal.labunit.dao.LabUnitWorkflowDAO labUnitWorkflowDAO;

    public LabUnitServiceImpl() {
        super(LabUnit.class);
    }

    @Override
    protected org.openelisglobal.labunit.dao.LabUnitDAO getBaseObjectDAO() {
        return labUnitDAO;
    }

    @PostConstruct
    private void initialize() {
        // Initialize any required mappings
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabUnitResponse> getAllLabUnits() {
        try {
            List<LabUnit> labUnits = labUnitDAO.getAllLabUnits();
            return labUnits.stream().map(this::toLabUnitResponse).collect(Collectors.toList());
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving all lab units", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabUnitResponse> getLabUnitsByStatus(String status) {
        try {
            List<LabUnit> labUnits;
            if ("active".equalsIgnoreCase(status)) {
                labUnits = labUnitDAO.getActiveLabUnits();
            } else if ("inactive".equalsIgnoreCase(status)) {
                labUnits = labUnitDAO.getInactiveLabUnits();
            } else {
                labUnits = labUnitDAO.getAllLabUnits();
            }
            return labUnits.stream().map(this::toLabUnitResponse).collect(Collectors.toList());
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving lab units by status: " + status, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LabUnitResponse getLabUnitById(String id) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(id);
            if (labUnit == null) {
                return null;
            }
            return toLabUnitResponse(labUnit);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error retrieving lab unit by id: " + id, e);
        }
    }

    @Override
    @Transactional
    public LabUnitResponse createLabUnit(LabUnitForm form) {
        try {

            if (!isNameUnique(form.getName(), null)) {
                throw new LIMSRuntimeException("Lab unit name must be unique");
            }
            if (form.getCode() != null && !isCodeUnique(form.getCode(), null)) {
                throw new LIMSRuntimeException("Lab unit code must be unique");
            }

            LabUnit labUnit = fromLabUnitForm(form);
            labUnitDAO.insert(labUnit);

            return toLabUnitResponse(labUnit);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error creating lab unit", e);
        }
    }

    @Override
    @Transactional
    public LabUnitResponse updateLabUnit(String id, LabUnitForm form) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(id);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + id);
            }

            if (!isNameUnique(form.getName(), id)) {
                throw new LIMSRuntimeException("Lab unit name must be unique");
            }
            if (form.getCode() != null && !isCodeUnique(form.getCode(), id)) {
                throw new LIMSRuntimeException("Lab unit code must be unique");
            }

            updateLabUnitFromForm(labUnit, form);
            labUnitDAO.update(labUnit);

            return toLabUnitResponse(labUnit);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error updating lab unit: " + id, e);
        }
    }

    @Override
    @Transactional
    public void deleteLabUnit(String id) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(id);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + id);
            }

            // Check if lab unit has assignments
            if (hasAssignments(id)) {
                throw new LIMSRuntimeException("Cannot delete lab unit with existing assignments");
            }

            labUnit.setActive("N");
            labUnit.setSysUserId("1"); // System user for deletion
            labUnitDAO.update(labUnit);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error deleting lab unit: " + id, e);
        }
    }

    // Count operations for list view
    @Override
    @Transactional(readOnly = true)
    public long getTotalLabUnitsCount() {
        try {
            return labUnitDAO.getTotalLabUnitCount();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting total lab units count", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveLabUnitsCount() {
        try {
            return labUnitDAO.getActiveLabUnitCount();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting active lab units count", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getInactiveLabUnitsCount() {
        try {
            return labUnitDAO.getInactiveLabUnitCount();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inactive lab units count", e);
        }
    }

    // Ordering operations
    @Override
    @Transactional
    public void updateLabUnitOrder(List<LabUnitOrderForm.LabUnitOrderItem> orderItems) {
        try {
            for (LabUnitOrderForm.LabUnitOrderItem item : orderItems) {
                LabUnit labUnit = labUnitDAO.getLabUnitById(item.getId());
                if (labUnit != null) {
                    labUnit.setSortOrder(item.getSortOrder());
                    labUnitDAO.update(labUnit);
                }
            }
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error updating sort order for lab units", e);
        }
    }

    // Assignment operations
    @Override
    @Transactional(readOnly = true)
    public List<LabUnitResponse> getLabUnitAssignments(String labUnitId) {
        try {
            List<LabUnitAssignment> assignments = labUnitDAO.getAssignmentsForLabUnit(labUnitId);

            // For now, return empty assignments as placeholders
            LabUnitResponse response = new LabUnitResponse();
            response.setTests(new ArrayList<>());
            response.setPanels(new ArrayList<>());
            response.setPrograms(new ArrayList<>());
            response.setProjects(new ArrayList<>());
            response.setWorkflows(new ArrayList<>());

            return List.of(response);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lab units with assignments", e);
        }
    }

    @Override
    @Transactional
    public void assignTestsToLabUnit(String labUnitId, String[] testIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for test assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error assigning tests to lab unit", e);
        }
    }

    @Override
    @Transactional
    public void removeTestsFromLabUnit(String labUnitId, String[] testIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for test assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error removing tests from lab unit", e);
        }
    }

    @Override
    @Transactional
    public void reassignTestsToLabUnit(String labUnitId, String[] testIds, String targetLabUnitId) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for test reassignment
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error reassigning tests between lab units", e);
        }
    }

    @Override
    @Transactional
    public void assignPanelsToLabUnit(String labUnitId, String[] panelIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for panel assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error assigning panels to lab unit", e);
        }
    }

    @Override
    @Transactional
    public void removePanelsFromLabUnit(String labUnitId, String[] panelIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for panel assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error removing panels from lab unit", e);
        }
    }

    @Override
    @Transactional
    public void assignProgramsToLabUnit(String labUnitId, String[] programIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for program assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error assigning programs to lab unit", e);
        }
    }

    @Override
    @Transactional
    public void removeProgramsFromLabUnit(String labUnitId, String[] programIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for program assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error removing programs from lab unit", e);
        }
    }

    @Override
    @Transactional
    public void assignProjectsToLabUnit(String labUnitId, String[] projectIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for project assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error assigning projects to lab unit", e);
        }
    }

    @Override
    @Transactional
    public void removeProjectsFromLabUnit(String labUnitId, String[] projectIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for project assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error removing projects from lab unit", e);
        }
    }

    @Override
    @Transactional
    public void assignWorkflowsToLabUnit(String labUnitId, String[] workflowIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Implementation placeholder for workflow assignments
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error assigning workflows to lab unit", e);
        }
    }

    @Override
    @Transactional
    public void removeWorkflowsFromLabUnit(String labUnitId, String[] workflowIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Check if any of the workflows to be removed are default workflows
            for (String workflowId : workflowIds) {
                LabUnitWorkflow labUnitWorkflow = labUnitWorkflowDAO.getByLabUnitAndWorkflowId(labUnitId, workflowId);
                if (labUnitWorkflow != null && Boolean.TRUE.equals(labUnitWorkflow.getIsDefault())) {
                    throw new LIMSRuntimeException("Cannot remove default workflow: " + workflowId + 
                        ". Please set another workflow as default first.");
                }
            }

            // Get current workflows for this lab unit
            List<LabUnitWorkflow> currentWorkflows = labUnitWorkflowDAO.getWorkflowsByLabUnitId(labUnitId);
            
            // Remove specified workflows
            for (String workflowId : workflowIds) {
                LabUnitWorkflow toRemove = labUnitWorkflowDAO.getByLabUnitAndWorkflowId(labUnitId, workflowId);
                if (toRemove != null) {
                    labUnitWorkflowDAO.delete(toRemove);
                }
            }

            // Check if lab unit will have any workflows remaining
            List<LabUnitWorkflow> remainingWorkflows = labUnitWorkflowDAO.getWorkflowsByLabUnitId(labUnitId);
            if (remainingWorkflows.isEmpty()) {
                logger.warn("All workflows removed from lab unit {}. Consider assigning at least one workflow.", labUnitId);
            }

            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error removing workflows from lab unit", e);
        }
    }

    @Override
    @Transactional
    public void setDefaultWorkflow(String labUnitId, String workflowId) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Verify the workflow is assigned to this lab unit
            LabUnitWorkflow labUnitWorkflow = labUnitWorkflowDAO.getByLabUnitAndWorkflowId(labUnitId, workflowId);
            if (labUnitWorkflow == null) {
                throw new LIMSRuntimeException("Workflow is not assigned to this lab unit");
            }

            // Clear existing default workflows first
            labUnitWorkflowDAO.clearDefaultWorkflows(labUnitId);

            // Set new default workflow
            labUnitWorkflowDAO.updateDefaultWorkflow(labUnitId, workflowId);

            logger.info("Default workflow set to {} for lab unit {}", workflowId, labUnitId);
            
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error setting default workflow for lab unit", e);
        }
    }

    @Override
    @Transactional
    public void clearDefaultWorkflows(String labUnitId) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            // Clear all default workflows for this lab unit
            labUnitWorkflowDAO.clearDefaultWorkflows(labUnitId);

            logger.info("Default workflows cleared for lab unit {}", labUnitId);
            
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error clearing default workflows for lab unit", e);
        }
    }

    // Status operations
    @Override
    @Transactional
    public void activateLabUnit(String id, Boolean cascade, String reason, String sysUserId) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(id);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + id);
            }

            labUnit.setActive("Y");
            labUnit.setSysUserId(sysUserId);
            labUnitDAO.update(labUnit);

            // Log activation with reason for audit compliance
            logger.info("Lab unit {} activated by user {}. Reason: {}", id, sysUserId, reason);

            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error activating lab unit: " + id, e);
        }
    }

    @Override
    @Transactional
    public void deactivateLabUnit(String id, Boolean cascade, String reason, String sysUserId) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(id);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + id);
            }

            if (!cascade && hasAssignments(id)) {
                throw new LIMSRuntimeException("Cannot deactivate lab unit with assignments when cascade is false");
            }

            labUnit.setActive("N");
            labUnit.setSysUserId(sysUserId);
            labUnitDAO.update(labUnit);

            // Log deactivation with reason for audit compliance
            logger.info("Lab unit {} deactivated by user {}. Reason: {}", id, sysUserId, reason);

            // Refresh display lists
            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error deactivating lab unit: " + id, e);
        }
    }

    // Validation operations
    @Override
    @Transactional(readOnly = true)
    public boolean isNameUnique(String name, String excludeId) {
        try {
            List<LabUnit> allUnits = labUnitDAO.getAllLabUnits();
            return allUnits.stream().noneMatch(unit -> name.equals(unit.getName()) && !unit.getId().equals(excludeId));
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking lab unit name uniqueness", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeUnique(String code, String excludeId) {
        try {
            if (code == null || code.trim().isEmpty()) {
                return true; // Empty code is considered unique
            }

            List<LabUnit> allUnits = labUnitDAO.getAllLabUnits();
            return allUnits.stream()
                    .noneMatch(unit -> code.equals(unit.getNameKey()) && !unit.getId().equals(excludeId));
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking lab unit code uniqueness", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAssignments(String labUnitId) {
        return labUnitDAO.hasAssignments(labUnitId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabUnitResponse> searchLabUnits(String searchTerm, String status, int page, int size) {
        try {
            List<LabUnit> labUnits = labUnitDAO.getLabUnitsByNameFilter(searchTerm);
            return labUnits.stream().map(this::toLabUnitResponse).collect(Collectors.toList());
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error searching lab units", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportLabUnits(List<String> labUnitIds, String format) {
        try {
            List<LabUnitResponse> labUnits;
            if (labUnitIds != null && !labUnitIds.isEmpty()) {
                labUnits = labUnitIds.stream()
                    .map(this::getLabUnitById)
                    .filter(unit -> unit != null)
                    .collect(Collectors.toList());
            } else {
                labUnits = getAllLabUnits();
            }

            if ("csv".equalsIgnoreCase(format)) {
                return exportLabUnitsToCSV(labUnits);
            } else {
                // Default to JSON format
                return exportLabUnitsJSON(labUnitIds);
            }
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error exporting lab units", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabUnitResponse> importLabUnits(byte[] importData, String format) {
        // Implementation placeholder for import
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportLabUnitsJSON(List<String> labUnitIds) {
        // Implementation placeholder for JSON export
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> validateLabUnitImport(byte[] importData, String format) {
        // Implementation placeholder for import validation
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> validateLabUnitImportJSON(byte[] importData) {
        // Implementation placeholder for JSON import validation
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public List<LabUnitResponse> importLabUnitsJSON(byte[] importData) {
        // Implementation placeholder for JSON import
        return new ArrayList<>();
    }



    @Override
    @Transactional(readOnly = true)
    public List<LabUnitResponse> getLabUnitsWithAssignments(String labUnitId) {
        try {
            LabUnitResponse response = getLabUnitById(labUnitId);
            if (response == null) {
                return new ArrayList<>();
            }

            // Get assignments and populate counts
            List<LabUnitAssignment> assignments = labUnitDAO.getAssignmentsForLabUnit(labUnitId);
            
            // Populate assignments lists
            response.setTests(getAssignmentsByType(assignments, "TEST"));
            response.setPanels(getAssignmentsByType(assignments, "PANEL"));
            response.setPrograms(getAssignmentsByType(assignments, "PROGRAM"));
            response.setProjects(getAssignmentsByType(assignments, "PROJECT"));
            response.setWorkflows(getAssignmentsByType(assignments, "WORKFLOW"));

            // Set counts
            response.setTestCount((long) response.getTests().size());
            response.setPanelCount((long) response.getPanels().size());
            response.setProgramCount((long) response.getPrograms().size());
            response.setProjectCount((long) response.getProjects().size());
            response.setWorkflowCount((long) response.getWorkflows().size());

            return List.of(response);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting lab units with assignments", e);
        }
    }

    // Data transformation helpers
    @Override
    public LabUnitResponse toLabUnitResponse(LabUnit labUnit) {
        if (labUnit == null) {
            return null;
        }

        LabUnitResponse response = new LabUnitResponse();
        response.setId(labUnit.getId());
        response.setName(labUnit.getName());
        response.setCode(labUnit.getNameKey());
        response.setDescription(labUnit.getDescription());
        response.setActive(labUnit.isActive());
        response.setSortOrder(labUnit.getSortOrder());
        response.setFhirUuid(labUnit.getFhirUuid().toString());

        if (labUnit.getOrganization() != null) {
            response.setOrganizationId(labUnit.getOrganization().getId());
            response.setOrganizationName(labUnit.getOrganization().getOrganizationName());
        }

        if (labUnit.getParentLabUnit() != null) {
            response.setParentLabUnitId(labUnit.getParentLabUnit().getId());
            response.setParentLabUnitName(labUnit.getParentLabUnit().getName());
        }

        response.setLastUpdated(labUnit.getLastupdated().toLocalDateTime());

        return response;
    }

    @Override
    public LabUnit fromLabUnitForm(LabUnitForm form) {
        LabUnit labUnit = new LabUnit();
        labUnit.setName(form.getName());
        labUnit.setCode(form.getCode());
        labUnit.setDescription(form.getDescription());
        labUnit.setOrganizationId(form.getOrganizationId());
        labUnit.setParentLabUnitId(form.getParentLabUnitId());
        labUnit.setSortOrder(form.getSortOrder());
        labUnit.setActive(form.getActive() ? "Y" : "N");
        return labUnit;
    }

    // Private helper methods
    private void refreshDisplayLists() {
        // Placeholder for refresh functionality
    }

    // Helper method for updating lab unit from form
    private void updateLabUnitFromForm(LabUnit labUnit, LabUnitForm form) {
        labUnit.setName(form.getName());
        labUnit.setCode(form.getCode());
        labUnit.setDescription(form.getDescription());
        labUnit.setOrganizationId(form.getOrganizationId());
        labUnit.setParentLabUnitId(form.getParentLabUnitId());
        labUnit.setSortOrder(form.getSortOrder());
        labUnit.setActive(form.getActive() ? "Y" : "N");
    }

    // CSV Export helper method
    public byte[] exportLabUnitsToCSV(List<LabUnitResponse> labUnits) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                
                // Write CSV header
                writer.write("ID,Name,Code,Description,Organization,Parent Lab Unit,Status,Sort Order,Test Count,Panel Count,Program Count,Project Count,Workflow Count\n");
                
                // Write data rows
                for (LabUnitResponse unit : labUnits) {
                    StringBuilder row = new StringBuilder();
                    row.append(escapeCsvValue(unit.getId())).append(",");
                    row.append(escapeCsvValue(unit.getName())).append(",");
                    row.append(escapeCsvValue(unit.getCode())).append(",");
                    row.append(escapeCsvValue(unit.getDescription())).append(",");
                    row.append(escapeCsvValue(unit.getOrganizationName())).append(",");
                    row.append(escapeCsvValue(unit.getParentLabUnitName())).append(",");
                    row.append(unit.getActive() != null && unit.getActive() ? "Active" : "Inactive").append(",");
                    row.append(unit.getSortOrder() != null ? unit.getSortOrder().toString() : "").append(",");
                    row.append(unit.getTestCount() != null ? unit.getTestCount().toString() : "0").append(",");
                    row.append(unit.getPanelCount() != null ? unit.getPanelCount().toString() : "0").append(",");
                    row.append(unit.getProgramCount() != null ? unit.getProgramCount().toString() : "0").append(",");
                    row.append(unit.getProjectCount() != null ? unit.getProjectCount().toString() : "0").append(",");
                    row.append(unit.getWorkflowCount() != null ? unit.getWorkflowCount().toString() : "0").append("\n");
                    
                    writer.write(row.toString());
                }
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new LIMSRuntimeException("Error generating CSV export", e);
        }
    }

    // Helper method to escape CSV values
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // Helper method to filter assignments by type
    private List<org.openelisglobal.labunit.dto.LabUnitAssignmentResponse> getAssignmentsByType(
            List<LabUnitAssignment> assignments, String type) {
        return assignments.stream()
            .filter(assignment -> type.equals(assignment.getAssignmentType()))
            .map(this::toAssignmentResponse)
            .collect(Collectors.toList());
    }

    // Helper method to convert assignment to response
    private org.openelisglobal.labunit.dto.LabUnitAssignmentResponse toAssignmentResponse(LabUnitAssignment assignment) {
        org.openelisglobal.labunit.dto.LabUnitAssignmentResponse response = new org.openelisglobal.labunit.dto.LabUnitAssignmentResponse();
        response.setId(assignment.getAssignedItemId());
        response.setAssignmentType(assignment.getAssignmentType());
        response.setAssignedDate(assignment.getAssignedDate());
        // Note: Name would need to be fetched from respective entity based on assignment type
        // For now, using the ID as name placeholder
        response.setName(assignment.getAssignedItemId());
        return response;
    }
}