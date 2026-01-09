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
import java.util.Map;
import java.util.HashMap;
import org.openelisglobal.labunit.dto.LabUnitImportData;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.labunit.dto.LabUnitResponse;
import org.openelisglobal.labunit.form.LabUnitForm;
import org.openelisglobal.labunit.form.LabUnitOrderForm;
import org.openelisglobal.labunit.service.LabUnitService;
import org.openelisglobal.labunit.valueholder.LabUnit;
import org.openelisglobal.labunit.valueholder.LabUnitAssignment;
import org.openelisglobal.labunit.valueholder.LabUnitWorkflow;
import org.openelisglobal.labunit.valueholder.LabUnitProject;
import org.openelisglobal.labunit.valueholder.LabUnitProgram;
import org.openelisglobal.labunit.valueholder.LabUnitImportLog;
import org.openelisglobal.labunit.valueholder.LabUnitProgram;
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

    @Autowired
    private org.openelisglobal.labunit.dao.LabUnitProjectDAO labUnitProjectDAO;

    @Autowired
    private org.openelisglobal.labunit.dao.LabUnitProgramDAO labUnitProgramDAO;

    @Autowired
    private org.openelisglobal.labunit.dao.LabUnitImportLogDAO labUnitImportLogDAO;

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

            // Build response with actual assignment data
            LabUnitResponse response = new LabUnitResponse();
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

    @Override
    @Transactional
    public void assignTestsToLabUnit(String labUnitId, String[] testIds) {
        try {
            LabUnit labUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (labUnit == null) {
                throw new LIMSRuntimeException("Lab unit not found: " + labUnitId);
            }

            if (testIds == null || testIds.length == 0) {
                logger.warn("No test IDs provided for assignment");
                return;
            }

            logger.info("Assigning {} tests to lab unit {}", testIds.length, labUnitId);

            // Assign tests using LabUnitAssignment system
            for (String testId : testIds) {
                // Check if test is already assigned to this lab unit
                LabUnitAssignment existingAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                        labUnitId, "TEST", testId);

                if (existingAssignment == null) {
                    // Create new assignment
                    LabUnitAssignment assignment = new LabUnitAssignment();
                    assignment.setLabUnitId(labUnitId);
                    assignment.setAssignmentType("TEST");
                    assignment.setAssignedItemId(testId);
                    assignment.setAssignedDate(java.time.LocalDateTime.now());
                    assignment.setSysUserId("1"); // System user - should be parameterized

                    labUnitDAO.createAssignment(assignment);
                    logger.debug("Assigned test {} to lab unit {}", testId, labUnitId);
                } else {
                    logger.debug("Test {} already assigned to lab unit {}", testId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully assigned {} tests to lab unit {}", testIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error assigning tests to lab unit {}", labUnitId, e);
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

            if (testIds == null || testIds.length == 0) {
                logger.warn("No test IDs provided for removal");
                return;
            }

            logger.info("Removing {} tests from lab unit {}", testIds.length, labUnitId);

            // Remove test assignments
            for (String testId : testIds) {
                LabUnitAssignment existingAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                        labUnitId, "TEST", testId);

                if (existingAssignment != null) {
                    labUnitDAO.deleteAssignment(existingAssignment);
                    logger.debug("Removed test {} from lab unit {}", testId, labUnitId);
                } else {
                    logger.debug("Test {} not assigned to lab unit {}", testId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully removed {} tests from lab unit {}", testIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error removing tests from lab unit {}", labUnitId, e);
            throw new LIMSRuntimeException("Error removing tests from lab unit", e);
        }
    }

    @Override
    @Transactional
    public void reassignTestsToLabUnit(String labUnitId, String[] testIds, String targetLabUnitId) {
        try {
            if (testIds == null || testIds.length == 0) {
                logger.warn("No test IDs provided for reassignment");
                return;
            }

            // Validate source and target lab units exist
            LabUnit sourceLabUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (sourceLabUnit == null) {
                throw new LIMSRuntimeException("Source lab unit not found: " + labUnitId);
            }

            LabUnit targetLabUnit = labUnitDAO.getLabUnitById(targetLabUnitId);
            if (targetLabUnit == null) {
                throw new LIMSRuntimeException("Target lab unit not found: " + targetLabUnitId);
            }

            if (labUnitId.equals(targetLabUnitId)) {
                throw new LIMSRuntimeException("Source and target lab units cannot be the same");
            }

            logger.info("Reassigning {} tests from lab unit {} to lab unit {}",
                    testIds.length, labUnitId, targetLabUnitId);

            // Remove from source lab unit and add to target lab unit
            for (String testId : testIds) {
                // Remove from source lab unit
                LabUnitAssignment sourceAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                        labUnitId, "TEST", testId);

                if (sourceAssignment != null) {
                    labUnitDAO.deleteAssignment(sourceAssignment);
                    logger.debug("Removed test {} from lab unit {}", testId, labUnitId);
                }

                // Check if test is already assigned to target lab unit
                LabUnitAssignment targetAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                        targetLabUnitId, "TEST", testId);

                if (targetAssignment == null) {
                    // Add to target lab unit
                    LabUnitAssignment newAssignment = new LabUnitAssignment();
                    newAssignment.setLabUnitId(targetLabUnitId);
                    newAssignment.setAssignmentType("TEST");
                    newAssignment.setAssignedItemId(testId);
                    newAssignment.setAssignedDate(java.time.LocalDateTime.now());
                    newAssignment.setSysUserId("1"); // System user - should be parameterized

                    labUnitDAO.createAssignment(newAssignment);
                    logger.debug("Added test {} to lab unit {}", testId, targetLabUnitId);
                } else {
                    logger.debug("Test {} already assigned to lab unit {}", testId, targetLabUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully reassigned {} tests from {} to {}",
                    testIds.length, labUnitId, targetLabUnitId);

        } catch (Exception e) {
            logger.error("Error reassigning tests from lab unit {} to {}", labUnitId, targetLabUnitId, e);
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

            if (panelIds == null || panelIds.length == 0) {
                logger.warn("No panel IDs provided for assignment");
                return;
            }

            logger.info("Assigning {} panels to lab unit {}", panelIds.length, labUnitId);

            // Assign panels using LabUnitAssignment system
            for (String panelId : panelIds) {
                // Check if panel is already assigned to this lab unit
                LabUnitAssignment existingAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                        labUnitId, "PANEL", panelId);

                if (existingAssignment == null) {
                    // Create new assignment
                    LabUnitAssignment assignment = new LabUnitAssignment();
                    assignment.setLabUnitId(labUnitId);
                    assignment.setAssignmentType("PANEL");
                    assignment.setAssignedItemId(panelId);
                    assignment.setAssignedDate(java.time.LocalDateTime.now());
                    assignment.setSysUserId("1"); // System user - should be parameterized

                    labUnitDAO.createAssignment(assignment);
                    logger.debug("Assigned panel {} to lab unit {}", panelId, labUnitId);
                } else {
                    logger.debug("Panel {} already assigned to lab unit {}", panelId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully assigned {} panels to lab unit {}", panelIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error assigning panels to lab unit {}", labUnitId, e);
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

            if (panelIds == null || panelIds.length == 0) {
                logger.warn("No panel IDs provided for removal");
                return;
            }

            logger.info("Removing {} panels from lab unit {}", panelIds.length, labUnitId);

            // Remove panel assignments
            for (String panelId : panelIds) {
                LabUnitAssignment existingAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                        labUnitId, "PANEL", panelId);

                if (existingAssignment != null) {
                    labUnitDAO.deleteAssignment(existingAssignment);
                    logger.debug("Removed panel {} from lab unit {}", panelId, labUnitId);
                } else {
                    logger.debug("Panel {} not assigned to lab unit {}", panelId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully removed {} panels from lab unit {}", panelIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error removing panels from lab unit {}", labUnitId, e);
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

            if (programIds == null || programIds.length == 0) {
                logger.warn("No program IDs provided for assignment");
                return;
            }

            logger.info("Assigning {} programs to lab unit {}", programIds.length, labUnitId);

            // Assign programs using LabUnitProgram entities
            for (String programId : programIds) {
                // Check if program is already assigned to this lab unit
                LabUnitProgram existingAssignment = labUnitProgramDAO.getByLabUnitAndProgramId(labUnitId, programId);

                if (existingAssignment == null) {
                    // Create new assignment
                    LabUnitProgram assignment = new LabUnitProgram();
                    assignment.setId(java.util.UUID.randomUUID().toString());
                    assignment.setLabUnitId(labUnitId);
                    assignment.setProgramId(programId);
                    assignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

                    labUnitProgramDAO.insert(assignment);
                    logger.debug("Assigned program {} to lab unit {}", programId, labUnitId);
                } else {
                    logger.debug("Program {} already assigned to lab unit {}", programId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully assigned {} programs to lab unit {}", programIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error assigning programs to lab unit {}", labUnitId, e);
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

            if (programIds == null || programIds.length == 0) {
                logger.warn("No program IDs provided for removal");
                return;
            }

            logger.info("Removing {} programs from lab unit {}", programIds.length, labUnitId);

            // Remove program assignments
            for (String programId : programIds) {
                LabUnitProgram existingAssignment = labUnitProgramDAO.getByLabUnitAndProgramId(labUnitId, programId);

                if (existingAssignment != null) {
                    labUnitProgramDAO.delete(existingAssignment);
                    logger.debug("Removed program {} from lab unit {}", programId, labUnitId);
                } else {
                    logger.debug("Program {} not assigned to lab unit {}", programId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully removed {} programs from lab unit {}", programIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error removing programs from lab unit {}", labUnitId, e);
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

            if (projectIds == null || projectIds.length == 0) {
                logger.warn("No project IDs provided for assignment");
                return;
            }

            logger.info("Assigning {} projects to lab unit {}", projectIds.length, labUnitId);

            // Assign projects using LabUnitProject entities
            for (String projectId : projectIds) {
                // Check if project is already assigned to this lab unit
                org.openelisglobal.labunit.valueholder.LabUnitProject existingAssignment = labUnitProjectDAO
                        .getByLabUnitAndProjectId(labUnitId, projectId);

                if (existingAssignment == null) {
                    // Create new assignment
                    org.openelisglobal.labunit.valueholder.LabUnitProject assignment = new org.openelisglobal.labunit.valueholder.LabUnitProject();
                    assignment.setId(java.util.UUID.randomUUID().toString());
                    assignment.setLabUnitId(labUnitId);
                    assignment.setProjectId(projectId);
                    assignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

                    labUnitProjectDAO.insert(assignment);
                    logger.debug("Assigned project {} to lab unit {}", projectId, labUnitId);
                } else {
                    logger.debug("Project {} already assigned to lab unit {}", projectId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully assigned {} projects to lab unit {}", projectIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error assigning projects to lab unit {}", labUnitId, e);
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

            if (projectIds == null || projectIds.length == 0) {
                logger.warn("No project IDs provided for removal");
                return;
            }

            logger.info("Removing {} projects from lab unit {}", projectIds.length, labUnitId);

            // Remove project assignments
            for (String projectId : projectIds) {
                org.openelisglobal.labunit.valueholder.LabUnitProject existingAssignment = labUnitProjectDAO
                        .getByLabUnitAndProjectId(labUnitId, projectId);

                if (existingAssignment != null) {
                    labUnitProjectDAO.delete(existingAssignment);
                    logger.debug("Removed project {} from lab unit {}", projectId, labUnitId);
                } else {
                    logger.debug("Project {} not assigned to lab unit {}", projectId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully removed {} projects from lab unit {}", projectIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error removing projects from lab unit {}", labUnitId, e);
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

            if (workflowIds == null || workflowIds.length == 0) {
                logger.warn("No workflow IDs provided for assignment");
                return;
            }

            logger.info("Assigning {} workflows to lab unit {}", workflowIds.length, labUnitId);

            // Assign workflows using LabUnitWorkflow entities
            for (String workflowId : workflowIds) {
                // Check if workflow is already assigned to this lab unit
                LabUnitWorkflow existingAssignment = labUnitWorkflowDAO.getByLabUnitAndWorkflowId(labUnitId, workflowId);

                if (existingAssignment == null) {
                    // Create new assignment
                    LabUnitWorkflow assignment = new LabUnitWorkflow();
                    assignment.setId(java.util.UUID.randomUUID().toString());
                    assignment.setLabUnitId(labUnitId);
                    assignment.setWorkflowId(workflowId);
                    assignment.setIsDefault(false); // Default will be set separately if needed
                    assignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                    assignment.setSysUserId("1"); // System user - should be parameterized

                    labUnitWorkflowDAO.insert(assignment);
                    logger.debug("Assigned workflow {} to lab unit {}", workflowId, labUnitId);
                } else {
                    logger.debug("Workflow {} already assigned to lab unit {}", workflowId, labUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully assigned {} workflows to lab unit {}", workflowIds.length, labUnitId);

        } catch (Exception e) {
            logger.error("Error assigning workflows to lab unit {}", labUnitId, e);
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
                logger.warn("All workflows removed from lab unit {}. Consider assigning at least one workflow.",
                        labUnitId);
            }

            refreshDisplayLists();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error removing projects from lab unit " + labUnitId, e);
        }
    }

    @Override
    @Transactional
    public void reassignProgramsToLabUnit(String labUnitId, String[] programIds, String targetLabUnitId) {
        try {
            if (programIds == null || programIds.length == 0) {
                logger.warn("No program IDs provided for reassignment");
                return;
            }

            // Validate source and target lab units exist
            LabUnit sourceLabUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (sourceLabUnit == null) {
                throw new LIMSRuntimeException("Source lab unit not found: " + labUnitId);
            }

            LabUnit targetLabUnit = labUnitDAO.getLabUnitById(targetLabUnitId);
            if (targetLabUnit == null) {
                throw new LIMSRuntimeException("Target lab unit not found: " + targetLabUnitId);
            }

            if (labUnitId.equals(targetLabUnitId)) {
                throw new LIMSRuntimeException("Source and target lab units cannot be the same");
            }

            logger.info("Reassigning {} programs from lab unit {} to lab unit {}",
                    programIds.length, labUnitId, targetLabUnitId);

            // Remove from source lab unit and add to target lab unit
            for (String programId : programIds) {
                // Remove from source lab unit
                LabUnitProgram sourceAssignment = labUnitProgramDAO.getByLabUnitAndProgramId(labUnitId, programId);

                if (sourceAssignment != null) {
                    labUnitProgramDAO.delete(sourceAssignment);
                    logger.debug("Removed program {} from lab unit {}", programId, labUnitId);
                }

                // Check if program is already assigned to target lab unit
                LabUnitProgram targetAssignment = labUnitProgramDAO.getByLabUnitAndProgramId(targetLabUnitId,
                        programId);

                if (targetAssignment == null) {
                    // Add to target lab unit
                    LabUnitProgram newAssignment = new LabUnitProgram();
                    newAssignment.setId(java.util.UUID.randomUUID().toString());
                    newAssignment.setLabUnitId(targetLabUnitId);
                    newAssignment.setProgramId(programId);
                    newAssignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

                    labUnitProgramDAO.insert(newAssignment);
                    logger.debug("Added program {} to lab unit {}", programId, targetLabUnitId);
                } else {
                    logger.debug("Program {} already assigned to lab unit {}", programId, targetLabUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully reassigned {} programs from {} to {}",
                    programIds.length, labUnitId, targetLabUnitId);

        } catch (Exception e) {
            logger.error("Error reassigning programs from lab unit {} to {}", labUnitId, targetLabUnitId, e);
            throw new LIMSRuntimeException("Error reassigning programs between lab units", e);
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

    @Override
    @Transactional
    public void reassignWorkflowsToLabUnit(String labUnitId, String[] workflowIds, String targetLabUnitId) {
        try {
            if (workflowIds == null || workflowIds.length == 0) {
                logger.warn("No workflow IDs provided for reassignment");
                return;
            }

            // Validate source and target lab units exist
            LabUnit sourceLabUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (sourceLabUnit == null) {
                throw new LIMSRuntimeException("Source lab unit not found: " + labUnitId);
            }

            LabUnit targetLabUnit = labUnitDAO.getLabUnitById(targetLabUnitId);
            if (targetLabUnit == null) {
                throw new LIMSRuntimeException("Target lab unit not found: " + targetLabUnitId);
            }

            if (labUnitId.equals(targetLabUnitId)) {
                throw new LIMSRuntimeException("Source and target lab units cannot be same");
            }

            logger.info("Reassigning {} workflows from lab unit {} to lab unit {}",
                    workflowIds.length, labUnitId, targetLabUnitId);

            // Remove from source lab unit and add to target lab unit using LabUnitWorkflow
            for (String workflowId : workflowIds) {
                // Remove from source lab unit
                LabUnitWorkflow sourceAssignment = labUnitWorkflowDAO.getByLabUnitAndWorkflowId(labUnitId, workflowId);

                if (sourceAssignment != null) {
                    labUnitWorkflowDAO.delete(sourceAssignment);
                    logger.debug("Removed workflow {} from lab unit {}", workflowId, labUnitId);
                }

                // Check if workflow is already assigned to target lab unit
                LabUnitWorkflow targetAssignment = labUnitWorkflowDAO.getByLabUnitAndWorkflowId(targetLabUnitId, workflowId);

                if (targetAssignment == null) {
                    // Add to target lab unit
                    LabUnitWorkflow newAssignment = new LabUnitWorkflow();
                    newAssignment.setId(java.util.UUID.randomUUID().toString());
                    newAssignment.setLabUnitId(targetLabUnitId);
                    newAssignment.setWorkflowId(workflowId);
                    newAssignment.setIsDefault(sourceAssignment != null ? sourceAssignment.getIsDefault() : false);
                    newAssignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                    newAssignment.setSysUserId("1"); // System user - should be parameterized

                    labUnitWorkflowDAO.insert(newAssignment);
                    logger.debug("Added workflow {} to lab unit {}", workflowId, targetLabUnitId);
                } else {
                    logger.debug("Workflow {} already assigned to lab unit {}", workflowId, targetLabUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully reassigned {} workflows from {} to {}",
                    workflowIds.length, labUnitId, targetLabUnitId);

        } catch (Exception e) {
            logger.error("Error reassigning workflows from lab unit {} to {}", labUnitId, targetLabUnitId, e);
            throw new LIMSRuntimeException("Error reassigning workflows between lab units", e);
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
            List<LabUnit> labUnits;

            // Apply status filter if provided
            if (status != null && !status.trim().isEmpty()) {
                if ("active".equalsIgnoreCase(status)) {
                    labUnits = labUnitDAO.getActiveLabUnits();
                } else if ("inactive".equalsIgnoreCase(status)) {
                    labUnits = labUnitDAO.getInactiveLabUnits();
                } else {
                    labUnits = labUnitDAO.getAllLabUnits();
                }
            } else {
                labUnits = labUnitDAO.getAllLabUnits();
            }

            // Apply search filter if provided
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String filter = "%" + searchTerm.trim().toLowerCase() + "%";
                labUnits = labUnits.stream()
                        .filter(unit -> unit.getName().toLowerCase().contains(searchTerm.trim().toLowerCase()) ||
                                unit.getNameKey().toLowerCase().contains(searchTerm.trim().toLowerCase()))
                        .collect(Collectors.toList());
            }

            // Apply pagination
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, labUnits.size());

            List<LabUnit> paginatedUnits = labUnits.subList(startIndex, endIndex);

            return paginatedUnits.stream()
                    .map(this::toLabUnitResponse)
                    .collect(Collectors.toList());

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

            // Build JSON export structure according to requirements
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\n");
            jsonBuilder.append("  \"exportVersion\": \"1.0\",\n");
            jsonBuilder.append("  \"exportDate\": \"").append(java.time.LocalDateTime.now().toString()).append("\",\n");
            jsonBuilder.append("  \"labUnits\": [\n");

            for (int i = 0; i < labUnits.size(); i++) {
                LabUnitResponse unit = labUnits.get(i);
                jsonBuilder.append("    {\n");
                jsonBuilder.append("      \"code\": \"").append(escapeJson(unit.getCode())).append("\",\n");
                jsonBuilder.append("      \"name\": \"").append(escapeJson(unit.getName())).append("\",\n");
                jsonBuilder.append("      \"description\": \"").append(escapeJson(unit.getDescription()))
                        .append("\",\n");
                jsonBuilder.append("      \"displayOrder\": ").append(unit.getSortOrder()).append(",\n");
                jsonBuilder.append("      \"isActive\": ").append(unit.getActive()).append(",\n");

                // Get assignments for this lab unit
                List<LabUnitResponse> assignments = getLabUnitAssignments(unit.getId());
                List<org.openelisglobal.labunit.dto.LabUnitAssignmentResponse> tests = assignments.isEmpty()
                        ? new ArrayList<>()
                        : assignments.get(0).getTests();
                List<org.openelisglobal.labunit.dto.LabUnitAssignmentResponse> panels = assignments.isEmpty()
                        ? new ArrayList<>()
                        : assignments.get(0).getPanels();
                List<org.openelisglobal.labunit.dto.LabUnitAssignmentResponse> programs = assignments.isEmpty()
                        ? new ArrayList<>()
                        : assignments.get(0).getPrograms();
                List<org.openelisglobal.labunit.dto.LabUnitAssignmentResponse> projects = assignments.isEmpty()
                        ? new ArrayList<>()
                        : assignments.get(0).getProjects();
                List<org.openelisglobal.labunit.dto.LabUnitAssignmentResponse> workflows = assignments.isEmpty()
                        ? new ArrayList<>()
                        : assignments.get(0).getWorkflows();

                // Add tests to JSON
                jsonBuilder.append("      \"tests\": [\n");
                for (int j = 0; j < tests.size(); j++) {
                    org.openelisglobal.labunit.dto.LabUnitAssignmentResponse test = tests.get(j);
                    jsonBuilder.append("        {\n");
                    jsonBuilder.append("          \"code\": \"").append(escapeJson(test.getId())).append("\",\n");
                    jsonBuilder.append("          \"loincCode\": \"").append(escapeJson("")).append("\",\n"); // Would
                                                                                                              // need to
                                                                                                              // fetch
                                                                                                              // from
                                                                                                              // test
                                                                                                              // entity
                    jsonBuilder.append("          \"isPrimary\": ").append("true").append("\n"); // Would need to track
                                                                                                 // primary
                    jsonBuilder.append("        }").append(j < tests.size() - 1 ? ",\n" : "\n");
                }
                jsonBuilder.append("      ]").append(i < labUnits.size() - 1 ? ",\n" : "\n");

                // Add panels to JSON
                jsonBuilder.append("      \"panels\": [\n");
                for (int j = 0; j < panels.size(); j++) {
                    org.openelisglobal.labunit.dto.LabUnitAssignmentResponse panel = panels.get(j);
                    jsonBuilder.append("        {\n");
                    jsonBuilder.append("          \"code\": \"").append(escapeJson(panel.getId())).append("\",\n");
                    jsonBuilder.append("          \"name\": \"").append(escapeJson(panel.getName())).append("\",\n");
                    jsonBuilder.append("        }").append(j < panels.size() - 1 ? ",\n" : "\n");
                }
                jsonBuilder.append("      ]").append(i < labUnits.size() - 1 ? ",\n" : "\n");

                // Add programs to JSON
                jsonBuilder.append("      \"programs\": [\n");
                for (int j = 0; j < programs.size(); j++) {
                    org.openelisglobal.labunit.dto.LabUnitAssignmentResponse program = programs.get(j);
                    jsonBuilder.append("        {\n");
                    jsonBuilder.append("          \"code\": \"").append(escapeJson(program.getId())).append("\",\n");
                    jsonBuilder.append("          \"name\": \"").append(escapeJson(program.getName())).append("\",\n");
                    jsonBuilder.append("        }").append(j < programs.size() - 1 ? ",\n" : "\n");
                }
                jsonBuilder.append("      ]").append(i < labUnits.size() - 1 ? ",\n" : "\n");

                // Add workflows to JSON
                jsonBuilder.append("      \"workflows\": [\n");
                for (int j = 0; j < workflows.size(); j++) {
                    org.openelisglobal.labunit.dto.LabUnitAssignmentResponse workflow = workflows.get(j);
                    jsonBuilder.append("        {\n");
                    jsonBuilder.append("          \"code\": \"").append(escapeJson(workflow.getId())).append("\",\n");
                    jsonBuilder.append("          \"isDefault\": ").append("true").append("\n"); // Would need to track
                                                                                                 // default
                    jsonBuilder.append("        }").append(j < workflows.size() - 1 ? ",\n" : "\n");
                }
                jsonBuilder.append("      ]\n");
                jsonBuilder.append("    }").append(i < labUnits.size() - 1 ? ",\n" : "\n");
            }

            jsonBuilder.append("  ]\n");
            jsonBuilder.append("}");

            return jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new LIMSRuntimeException("Error generating JSON export", e);
        }
    }

    // Helper method to escape JSON strings
    private String escapeJson(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> validateLabUnitImport(byte[] importData, String format) {
        try {
            if (importData == null || importData.length == 0) {
                return List.of("Import file is empty");
            }

            List<String> errors = new ArrayList<>();

            // Basic validation for JSON format
            String jsonString = new String(importData, StandardCharsets.UTF_8);
            if (!"json".equalsIgnoreCase(format)) {
                errors.add("Unsupported format: " + format + ". Only JSON format is supported.");
                return errors;
            }

            // Try to parse JSON
            // Note: This is a simplified validation - in production, you'd want proper JSON
            // parsing
            if (!jsonString.trim().startsWith("{") || !jsonString.trim().endsWith("}")) {
                errors.add("Invalid JSON format: Expected JSON object format");
                return errors;
            }

            // Extract lab units and validate
            try {
                // Simple JSON validation - in production, use proper JSON library
                String labUnitsJson = jsonString.trim();
                if (labUnitsJson.contains("\"labUnits\"")) {
                    // JSON seems to have labUnits array - basic validation passed
                    return new ArrayList<>(); // No errors for basic structure
                }
            } catch (Exception e) {
                errors.add("Invalid JSON format: " + e.getMessage());
            }

            return errors;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error validating import data", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> validateLabUnitImportJSON(byte[] importData) {
        try {
            if (importData == null || importData.length == 0) {
                return List.of("Import file is empty");
            }

            List<String> errors = new ArrayList<>();
            String jsonString = new String(importData, StandardCharsets.UTF_8);

            // Basic JSON structure validation
            if (!jsonString.trim().startsWith("{") || !jsonString.trim().endsWith("}")) {
                errors.add("Invalid JSON format: Expected JSON object");
                return errors;
            }

            // Check for required top-level fields
            if (!jsonString.contains("\"labUnits\"")) {
                errors.add("Missing required field: labUnits");
            }
            
            if (!jsonString.contains("\"exportVersion\"")) {
                errors.add("Missing required field: exportVersion");
            }

            // Try to parse the structure
            try {
                LabUnitImportData parsedData = parseJSONImport(jsonString);
                if (parsedData == null) {
                    errors.add("Failed to parse JSON structure");
                    return errors;
                }

                // Validate each lab unit item
                if (parsedData.getLabUnits() != null) {
                    for (LabUnitImportData.LabUnitImportItem item : parsedData.getLabUnits()) {
                        if (item.getName() == null || item.getName().trim().isEmpty()) {
                            errors.add("Lab unit name is required for all items");
                        }
                        if (item.getCode() == null || item.getCode().trim().isEmpty()) {
                            errors.add("Lab unit code is required for all items");
                        }
                    }
                }

            } catch (Exception e) {
                errors.add("JSON parsing error: " + e.getMessage());
            }

            return errors;

        } catch (Exception e) {
            throw new LIMSRuntimeException("Error validating import data", e);
        }
    }

    @Override
    @Transactional
    public List<LabUnitResponse> importLabUnitsJSON(byte[] importData) {
        try {
            if (importData == null || importData.length == 0) {
                logger.warn("No import data provided");
                return new ArrayList<>();
            }

            logger.info("Importing lab units from JSON data");

            String jsonString = new String(importData, StandardCharsets.UTF_8);
            List<LabUnitResponse> importedLabUnits = new ArrayList<>();
            List<String> validationErrors = validateLabUnitImportJSON(importData);

            if (!validationErrors.isEmpty()) {
                logger.warn("Import validation failed: {} errors", validationErrors.size());
                throw new LIMSRuntimeException("Import validation failed: " + String.join(", ", validationErrors));
            }

            // Parse JSON manually (simplified approach without external JSON library)
            LabUnitImportData parsedImportData = parseJSONImport(jsonString);
            
            if (parsedImportData == null || parsedImportData.getLabUnits().isEmpty()) {
                logger.warn("No lab units found in import data");
                return new ArrayList<>();
            }

            int totalRecords = parsedImportData.getLabUnits().size();
            int successCount = 0;
            int errorCount = 0;
            List<String> importErrors = new ArrayList<>();

            // Process each lab unit
            for (LabUnitImportData.LabUnitImportItem labUnitItem : parsedImportData.getLabUnits()) {
                try {
                    LabUnitResponse importedUnit = processLabUnitImport(labUnitItem);
                    if (importedUnit != null) {
                        importedLabUnits.add(importedUnit);
                        successCount++;
                    } else {
                        errorCount++;
                        importErrors.add("Failed to import lab unit: " + labUnitItem.getName());
                    }
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error importing lab unit '" + labUnitItem.getName() + "': " + e.getMessage();
                    importErrors.add(errorMsg);
                    logger.error(errorMsg, e);
                }
            }

            // Create import log entry
            LabUnitImportLog importLog = new LabUnitImportLog();
            importLog.setId(java.util.UUID.randomUUID().toString());
            importLog.setImportDate(new java.sql.Timestamp(System.currentTimeMillis()));
            importLog.setUserId("1"); // System user - should be parameterized
            importLog.setTotalRecords(totalRecords);
            importLog.setSuccessCount(successCount);
            importLog.setErrorCount(errorCount);
            
            // Store import errors as JSON string
            if (!importErrors.isEmpty()) {
                importLog.setErrorDetails(String.join("; ", importErrors));
            }

            labUnitImportLogDAO.createImportLog(importLog);
            logger.info("Import completed: {} total, {} successful, {} errors", totalRecords, successCount, errorCount);

            refreshDisplayLists();
            return importedLabUnits;

        } catch (Exception e) {
            logger.error("Error importing lab units from JSON", e);
            throw new LIMSRuntimeException("Error importing lab units from JSON: " + e.getMessage(), e);
        }
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
                writer.write(
                        "ID,Name,Code,Description,Organization,Parent Lab Unit,Status,Sort Order,Test Count,Panel Count,Program Count,Project Count,Workflow Count\n");

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
    private org.openelisglobal.labunit.dto.LabUnitAssignmentResponse toAssignmentResponse(
            LabUnitAssignment assignment) {
        org.openelisglobal.labunit.dto.LabUnitAssignmentResponse response = new org.openelisglobal.labunit.dto.LabUnitAssignmentResponse();
        response.setId(assignment.getAssignedItemId());
        response.setAssignmentType(assignment.getAssignmentType());
        response.setAssignedDate(assignment.getAssignedDate());
        // Note: Name would need to be fetched from respective entity based on
        // assignment type
        // For now, using ID as name placeholder
        response.setName(assignment.getAssignedItemId());
        return response;
    }

    // JSON parsing helper method (simplified implementation)
    private LabUnitImportData parseJSONImport(String jsonString) {
        try {
            LabUnitImportData importData = new LabUnitImportData();
            
            // Simple JSON parsing - in production, use proper JSON library like Jackson/Gson
            // This is a basic implementation to get the structure working
            
            // Look for labUnits array
            if (jsonString.contains("\"labUnits\"")) {
                // Extract basic structure - simplified parsing
                List<LabUnitImportData.LabUnitImportItem> labUnits = new ArrayList<>();
                
                // For now, create a placeholder structure
                // In production, use proper JSON parsing library
                logger.info("JSON structure detected - parsing {} lab units", 1); // Placeholder
                
                // Create placeholder item to show structure
                LabUnitImportData.LabUnitImportItem item = new LabUnitImportData.LabUnitImportItem();
                labUnits.add(item);
                
                importData.setLabUnits(labUnits);
                importData.setExportVersion("1.0");
                importData.setExportDate(java.time.LocalDateTime.now().toString());
                
                return importData;
            } else {
                logger.error("Invalid JSON structure - missing 'labUnits' field");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error parsing JSON import data", e);
            return null;
        }
    }

    // Process individual lab unit import
    private LabUnitResponse processLabUnitImport(LabUnitImportData.LabUnitImportItem labUnitItem) {
        try {
            // Check if lab unit exists by code
            List<LabUnit> existingUnits = labUnitDAO.getAllLabUnits();
            LabUnit existingUnit = existingUnits.stream()
                    .filter(unit -> labUnitItem.getCode() != null && 
                                   labUnitItem.getCode().equals(unit.getNameKey()))
                    .findFirst()
                    .orElse(null);

            LabUnit labUnit;
            boolean isNew = false;

            if (existingUnit != null) {
                // Update existing lab unit
                labUnit = existingUnit;
                logger.info("Updating existing lab unit: {}", labUnitItem.getName());
            } else {
                // Create new lab unit
                labUnit = new LabUnit();
                labUnit.setId(java.util.UUID.randomUUID().toString());
                labUnit.setFhirUuid(java.util.UUID.randomUUID());
                isNew = true;
                logger.info("Creating new lab unit: {}", labUnitItem.getName());
            }

            // Set basic properties
            labUnit.setName(labUnitItem.getName());
            labUnit.setNameKey(labUnitItem.getCode());
            labUnit.setDescription(labUnitItem.getDescription());
            labUnit.setSortOrder(labUnitItem.getDisplayOrder() != null ? labUnitItem.getDisplayOrder() : 0);
            labUnit.setActive(labUnitItem.getIsActive() != null ? 
                    (labUnitItem.getIsActive() ? "Y" : "N") : "Y");
            labUnit.setSysUserId("1"); // System user - should be parameterized

            if (isNew) {
                labUnitDAO.insert(labUnit);
            } else {
                labUnitDAO.update(labUnit);
            }

            // Process assignments
            processAssignmentsForImport(labUnit, labUnitItem);

            return toLabUnitResponse(labUnit);

        } catch (Exception e) {
            logger.error("Error processing lab unit import: {}", labUnitItem.getName(), e);
            return null;
        }
    }

    // Process assignments for imported lab unit
    private void processAssignmentsForImport(LabUnit labUnit, LabUnitImportData.LabUnitImportItem labUnitItem) {
        try {
            // Process test assignments
            if (labUnitItem.getTests() != null) {
                for (LabUnitImportData.AssignmentItem testItem : labUnitItem.getTests()) {
                    // Here you would typically validate that the test exists in the system
                    // For now, create assignment if not already present
                    LabUnitAssignment existingAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                            labUnit.getId(), "TEST", testItem.getCode());
                    
                    if (existingAssignment == null) {
                        LabUnitAssignment assignment = new LabUnitAssignment();
                        assignment.setLabUnitId(labUnit.getId());
                        assignment.setAssignmentType("TEST");
                        assignment.setAssignedItemId(testItem.getCode());
                        assignment.setAssignedDate(java.time.LocalDateTime.now());
                        assignment.setSysUserId("1");
                        labUnitDAO.createAssignment(assignment);
                    }
                }
            }

            // Process panel assignments
            if (labUnitItem.getPanels() != null) {
                for (LabUnitImportData.AssignmentItem panelItem : labUnitItem.getPanels()) {
                    LabUnitAssignment existingAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                            labUnit.getId(), "PANEL", panelItem.getCode());
                    
                    if (existingAssignment == null) {
                        LabUnitAssignment assignment = new LabUnitAssignment();
                        assignment.setLabUnitId(labUnit.getId());
                        assignment.setAssignmentType("PANEL");
                        assignment.setAssignedItemId(panelItem.getCode());
                        assignment.setAssignedDate(java.time.LocalDateTime.now());
                        assignment.setSysUserId("1");
                        labUnitDAO.createAssignment(assignment);
                    }
                }
            }

            // Process program assignments
            if (labUnitItem.getPrograms() != null) {
                for (LabUnitImportData.AssignmentItem programItem : labUnitItem.getPrograms()) {
                    LabUnitProgram existingAssignment = labUnitProgramDAO.getByLabUnitAndProgramId(
                            labUnit.getId(), programItem.getCode());
                    
                    if (existingAssignment == null) {
                        LabUnitProgram assignment = new LabUnitProgram();
                        assignment.setId(java.util.UUID.randomUUID().toString());
                        assignment.setLabUnitId(labUnit.getId());
                        assignment.setProgramId(programItem.getCode());
                        assignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                        labUnitProgramDAO.insert(assignment);
                    }
                }
            }

            // Process project assignments
            if (labUnitItem.getProjects() != null) {
                for (LabUnitImportData.AssignmentItem projectItem : labUnitItem.getProjects()) {
                    org.openelisglobal.labunit.valueholder.LabUnitProject existingAssignment = labUnitProjectDAO
                            .getByLabUnitAndProjectId(labUnit.getId(), projectItem.getCode());
                    
                    if (existingAssignment == null) {
                        org.openelisglobal.labunit.valueholder.LabUnitProject assignment = 
                                new org.openelisglobal.labunit.valueholder.LabUnitProject();
                        assignment.setId(java.util.UUID.randomUUID().toString());
                        assignment.setLabUnitId(labUnit.getId());
                        assignment.setProjectId(projectItem.getCode());
                        assignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                        labUnitProjectDAO.insert(assignment);
                    }
                }
            }

            // Process workflow assignments
            if (labUnitItem.getWorkflows() != null) {
                for (LabUnitImportData.WorkflowAssignmentItem workflowItem : labUnitItem.getWorkflows()) {
                    LabUnitWorkflow existingAssignment = labUnitWorkflowDAO.getByLabUnitAndWorkflowId(
                            labUnit.getId(), workflowItem.getCode());
                    
                    if (existingAssignment == null) {
                        LabUnitWorkflow assignment = new LabUnitWorkflow();
                        assignment.setId(java.util.UUID.randomUUID().toString());
                        assignment.setLabUnitId(labUnit.getId());
                        assignment.setWorkflowId(workflowItem.getCode());
                        assignment.setIsDefault(workflowItem.getIsDefault() != null ? workflowItem.getIsDefault() : false);
                        labUnitWorkflowDAO.insert(assignment);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error processing assignments for lab unit: {}", labUnit.getName(), e);
            throw new LIMSRuntimeException("Error processing assignments for lab unit: " + labUnit.getName(), e);
        }
    }

    @Override
    @Transactional
    public void reassignPanelsToLabUnit(String labUnitId, String[] panelIds, String targetLabUnitId) {
        try {
            if (panelIds == null || panelIds.length == 0) {
                logger.warn("No panel IDs provided for reassignment");
                return;
            }

            // Validate source and target lab units exist
            LabUnit sourceLabUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (sourceLabUnit == null) {
                throw new LIMSRuntimeException("Source lab unit not found: " + labUnitId);
            }

            LabUnit targetLabUnit = labUnitDAO.getLabUnitById(targetLabUnitId);
            if (targetLabUnit == null) {
                throw new LIMSRuntimeException("Target lab unit not found: " + targetLabUnitId);
            }

            if (labUnitId.equals(targetLabUnitId)) {
                throw new LIMSRuntimeException("Source and target lab units cannot be the same");
            }

            logger.info("Reassigning {} panels from lab unit {} to lab unit {}",
                    panelIds.length, labUnitId, targetLabUnitId);

            // For each panel, remove from source and add to target using LabUnitAssignment
            for (String panelId : panelIds) {
                // Remove from source lab unit
                LabUnitAssignment existingAssignment = labUnitDAO.getAssignmentByLabUnitAndItem(
                        labUnitId, "PANEL", panelId);

                if (existingAssignment != null) {
                    labUnitDAO.deleteAssignment(existingAssignment);
                    logger.debug("Removed panel {} from lab unit {}", panelId, labUnitId);
                }

                // Add to target lab unit
                LabUnitAssignment newAssignment = new LabUnitAssignment();
                newAssignment.setLabUnitId(targetLabUnitId);
                newAssignment.setAssignmentType("PANEL");
                newAssignment.setAssignedItemId(panelId);
                newAssignment.setAssignedDate(java.time.LocalDateTime.now());
                newAssignment.setSysUserId("1"); // System user - should be parameterized

                labUnitDAO.createAssignment(newAssignment);
                logger.debug("Added panel {} to lab unit {}", panelId, targetLabUnitId);
            }

            refreshDisplayLists();
            logger.info("Successfully reassigned {} panels from {} to {}",
                    panelIds.length, labUnitId, targetLabUnitId);

        } catch (Exception e) {
            logger.error("Error reassigning panels from lab unit {} to {}", labUnitId, targetLabUnitId, e);
            throw new LIMSRuntimeException("Error reassigning panels between lab units", e);
        }
    }

    @Override
    @Transactional
    public void reassignProjectsToLabUnit(String labUnitId, String[] projectIds, String targetLabUnitId) {
        try {
            if (projectIds == null || projectIds.length == 0) {
                logger.warn("No project IDs provided for reassignment");
                return;
            }

            // Validate source and target lab units exist
            LabUnit sourceLabUnit = labUnitDAO.getLabUnitById(labUnitId);
            if (sourceLabUnit == null) {
                throw new LIMSRuntimeException("Source lab unit not found: " + labUnitId);
            }

            LabUnit targetLabUnit = labUnitDAO.getLabUnitById(targetLabUnitId);
            if (targetLabUnit == null) {
                throw new LIMSRuntimeException("Target lab unit not found: " + targetLabUnitId);
            }

            if (labUnitId.equals(targetLabUnitId)) {
                throw new LIMSRuntimeException("Source and target lab units cannot be the same");
            }

            logger.info("Reassigning {} projects from lab unit {} to lab unit {}",
                    projectIds.length, labUnitId, targetLabUnitId);

            // For each project, remove from source and add to target using LabUnitProject
            for (String projectId : projectIds) {
                // Remove from source lab unit
                org.openelisglobal.labunit.valueholder.LabUnitProject existingAssignment = labUnitProjectDAO
                        .getByLabUnitAndProjectId(labUnitId, projectId);
                if (existingAssignment != null) {
                    labUnitProjectDAO.delete(existingAssignment);
                    logger.debug("Removed project {} from lab unit {}", projectId, labUnitId);
                }

                // Check if project is already assigned to target lab unit
                org.openelisglobal.labunit.valueholder.LabUnitProject targetAssignment = labUnitProjectDAO
                        .getByLabUnitAndProjectId(targetLabUnitId, projectId);
                if (targetAssignment == null) {
                    // Add to target lab unit
                    org.openelisglobal.labunit.valueholder.LabUnitProject newAssignment = new org.openelisglobal.labunit.valueholder.LabUnitProject();
                    newAssignment.setId(java.util.UUID.randomUUID().toString());
                    newAssignment.setLabUnitId(targetLabUnitId);
                    newAssignment.setProjectId(projectId);
                    newAssignment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

                    labUnitProjectDAO.insert(newAssignment);
                    logger.debug("Added project {} to lab unit {}", projectId, targetLabUnitId);
                } else {
                    logger.debug("Project {} already assigned to lab unit {}", projectId, targetLabUnitId);
                }
            }

            refreshDisplayLists();
            logger.info("Successfully reassigned {} projects from {} to {}",
                    projectIds.length, labUnitId, targetLabUnitId);

        } catch (Exception e) {
            logger.error("Error reassigning projects from lab unit {} to {}", labUnitId, targetLabUnitId, e);
            throw new LIMSRuntimeException("Error reassigning projects between lab units", e);
        }
    }
}