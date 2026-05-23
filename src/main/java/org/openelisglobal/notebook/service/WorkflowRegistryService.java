package org.openelisglobal.notebook.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.openelisglobal.common.constants.rbac.AHRIRoleCatalog;
import org.openelisglobal.common.constants.rbac.AHRITestSectionCatalog;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.valueholder.NotebookStageAction;
import org.openelisglobal.notebook.valueholder.WorkflowStageDefinition;
import org.springframework.stereotype.Service;

/**
 * In-memory AHRI workflow stage registry (loaded from workflow-registry CSV).
 */
@Service
public class WorkflowRegistryService {

    private final Map<String, List<WorkflowStageDefinition>> stagesByWorkflowType = new ConcurrentHashMap<>();

    public void replaceRegistry(List<WorkflowStageDefinition> definitions) {
        Map<String, List<WorkflowStageDefinition>> grouped = new LinkedHashMap<>();
        for (WorkflowStageDefinition definition : definitions) {
            grouped.computeIfAbsent(normalizeWorkflowType(definition.getWorkflowType()), key -> new ArrayList<>())
                    .add(definition);
        }
        grouped.values().forEach(list -> list.sort(Comparator.comparingInt(WorkflowStageDefinition::getStageOrder)));
        stagesByWorkflowType.clear();
        stagesByWorkflowType.putAll(grouped);
    }

    public List<WorkflowStageDefinition> getStagesForWorkflowType(String workflowType) {
        if (workflowType == null) {
            return List.of();
        }
        return stagesByWorkflowType.getOrDefault(normalizeWorkflowType(workflowType), List.of());
    }

    public Optional<WorkflowStageDefinition> getStage(String workflowType, int stageOrder) {
        return getStagesForWorkflowType(workflowType).stream()
                .filter(stage -> stage.getStageOrder() == stageOrder).findFirst();
    }

    public Optional<WorkflowStageDefinition> getStageByPageKey(String workflowType, String pageKey) {
        if (pageKey == null || pageKey.isBlank()) {
            return Optional.empty();
        }
        String normalizedKey = pageKey.trim();
        return getStagesForWorkflowType(workflowType).stream()
                .filter(stage -> normalizedKey.equalsIgnoreCase(stage.getPageKey())
                        || normalizedKey.equalsIgnoreCase(stage.getStageId()))
                .findFirst();
    }

    public List<String> getAllowedPersonas(String workflowType, int stageOrder) {
        return getStage(workflowType, stageOrder).map(WorkflowStageDefinition::getAllowedPersonas).orElse(List.of());
    }

    public boolean isActionPermitted(String workflowType, String pageKey, int stageOrder, NotebookStageAction action) {
        Optional<WorkflowStageDefinition> stage = getStageByPageKey(workflowType, pageKey);
        if (stage.isEmpty() && stageOrder > 0) {
            stage = getStage(workflowType, stageOrder);
        }
        return stage.map(def -> def.permitsAction(action)).orElse(false);
    }

    public List<String> resolveAllowedPersonasForAction(String workflowType, String pageKey, Integer stageOrder,
            Integer pageOrder, NotebookStageAction action, List<String> pageAllowedRoles) {
        if (action != null) {
            Optional<WorkflowStageDefinition> stage = getStageByPageKey(workflowType, pageKey);
            int order = stageOrder != null ? stageOrder : (pageOrder != null ? pageOrder : 0);
            if (stage.isEmpty() && order > 0) {
                stage = getStage(workflowType, order);
            }
            if (stage.isPresent() && !stage.get().permitsAction(action)) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveAllowedPersonasForAction",
                        "Action " + action + " not permitted for workflowType=" + workflowType + " pageKey=" + pageKey);
                return List.of();
            }
        }
        return resolveAllowedPersonas(workflowType, stageOrder, pageOrder, pageKey, pageAllowedRoles);
    }

    public List<String> resolveAllowedPersonas(String workflowType, Integer stageOrder, Integer pageOrder,
            List<String> pageAllowedRoles) {
        return resolveAllowedPersonas(workflowType, stageOrder, pageOrder, null, pageAllowedRoles);
    }

    public List<String> resolveAllowedPersonas(String workflowType, Integer stageOrder, Integer pageOrder,
            String pageKey, List<String> pageAllowedRoles) {
        if (pageAllowedRoles != null && !pageAllowedRoles.isEmpty()) {
            return pageAllowedRoles.stream().filter(AHRIRoleCatalog::isDepartmentRoleName)
                    .collect(Collectors.toList());
        }
        if (pageKey != null && !pageKey.isBlank()) {
            Optional<WorkflowStageDefinition> byKey = getStageByPageKey(workflowType, pageKey);
            if (byKey.isPresent() && !byKey.get().getAllowedPersonas().isEmpty()) {
                return byKey.get().getAllowedPersonas();
            }
        }
        int order = stageOrder != null ? stageOrder : (pageOrder != null ? pageOrder : 0);
        if (order > 0) {
            List<String> fromRegistry = getAllowedPersonas(workflowType, order);
            if (!fromRegistry.isEmpty()) {
                return fromRegistry;
            }
        }
        LogEvent.logWarn(this.getClass().getSimpleName(), "resolveAllowedPersonas",
                "No allowed personas for workflowType=" + workflowType + " pageKey=" + pageKey + " stageOrder="
                        + order);
        return List.of();
    }

    public boolean isKnownWorkflowType(String workflowType) {
        return workflowType != null && stagesByWorkflowType.containsKey(normalizeWorkflowType(workflowType));
    }

    public static String normalizeWorkflowType(String workflowType) {
        if (workflowType == null) {
            return "";
        }
        return workflowType.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static List<String> parsePersonas(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] parts = raw.split("\\|");
        List<String> personas = new ArrayList<>();
        for (String part : parts) {
            String name = part.trim();
            if (!name.isEmpty() && AHRIRoleCatalog.isDepartmentRoleName(name)) {
                personas.add(name);
            }
        }
        return Collections.unmodifiableList(personas);
    }

    public static void validateDepartmentName(String departmentName, String fileName, int lineNumber) {
        if (!AHRITestSectionCatalog.contains(departmentName)) {
            LogEvent.logWarn("WorkflowRegistry", "validate",
                    fileName + " line " + lineNumber + ": department '" + departmentName
                            + "' is not in AHRI test section allowlist");
        }
    }
}
