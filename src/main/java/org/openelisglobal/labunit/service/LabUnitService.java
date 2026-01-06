package org.openelisglobal.labunit.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.labunit.dto.LabUnitResponse;
import org.openelisglobal.labunit.form.LabUnitForm;
import org.openelisglobal.labunit.form.LabUnitOrderForm;
import org.openelisglobal.labunit.valueholder.LabUnit;

/**
 * Service interface for Lab Unit management operations. Extends OpenELIS
 * BaseObjectService pattern for consistency.
 */
public interface LabUnitService extends BaseObjectService<LabUnit, String> {

    List<LabUnitResponse> getAllLabUnits();

    List<LabUnitResponse> getLabUnitsByStatus(String status);

    LabUnitResponse getLabUnitById(String id);

    LabUnitResponse createLabUnit(LabUnitForm form);

    LabUnitResponse updateLabUnit(String id, LabUnitForm form);

    void deleteLabUnit(String id);

    // Count operations for list view
    long getTotalLabUnitsCount();

    long getActiveLabUnitsCount();

    long getInactiveLabUnitsCount();

    // Ordering operations
    void updateLabUnitOrder(List<LabUnitOrderForm.LabUnitOrderItem> orderItems);

    // Assignment operations
    List<LabUnitResponse> getLabUnitAssignments(String labUnitId);

    void assignTestsToLabUnit(String labUnitId, String[] testIds);

    void removeTestsFromLabUnit(String labUnitId, String[] testIds);

    void reassignTestsToLabUnit(String labUnitId, String[] testIds, String targetLabUnitId);

    void assignPanelsToLabUnit(String labUnitId, String[] panelIds);

    void removePanelsFromLabUnit(String labUnitId, String[] panelIds);

    void assignProgramsToLabUnit(String labUnitId, String[] programIds);

    void removeProgramsFromLabUnit(String labUnitId, String[] programIds);

    void assignProjectsToLabUnit(String labUnitId, String[] projectIds);

    void removeProjectsFromLabUnit(String labUnitId, String[] projectIds);

    void assignWorkflowsToLabUnit(String labUnitId, String[] workflowIds);

    void removeWorkflowsFromLabUnit(String labUnitId, String[] workflowIds);

    // Enhanced status operations with cascade options
    void activateLabUnit(String id, Boolean cascade, String reason, String sysUserId);

    void deactivateLabUnit(String id, Boolean cascade, String reason, String sysUserId);

    // Workflow assignment management
    void setDefaultWorkflow(String labUnitId, String workflowId);

    void clearDefaultWorkflows(String labUnitId);

    // Validation operations
    boolean isNameUnique(String name, String excludeId);

    boolean isCodeUnique(String code, String excludeId);

    boolean hasAssignments(String labUnitId);

    // Search and filtering
    List<LabUnitResponse> searchLabUnits(String searchTerm, String status, int page, int size);

    List<LabUnitResponse> getLabUnitsWithAssignments(String labUnitId);

    // Import/Export operations
    byte[] exportLabUnits(List<String> labUnitIds, String format);

    List<String> validateLabUnitImport(byte[] importData, String format);

    List<LabUnitResponse> importLabUnits(byte[] importData, String format);

    // Import/Export with JSON format
    byte[] exportLabUnitsJSON(List<String> labUnitIds);

    List<String> validateLabUnitImportJSON(byte[] importData);

    List<LabUnitResponse> importLabUnitsJSON(byte[] importData);

    // Data transformation helpers
    LabUnitResponse toLabUnitResponse(LabUnit labUnit);
 
    LabUnit fromLabUnitForm(LabUnitForm form);
}