package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import java.util.Set;
import org.openelisglobal.notebook.valueholder.NotebookStageAction;
import org.openelisglobal.notebook.valueholder.WorkflowStageDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads {@code volume/configuration/backend/workflow-registry/*.csv}.
 */
@Component
@Transactional
public class WorkflowRegistryConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private WorkflowRegistryService workflowRegistryService;

    @Override
    public String getDomainName() {
        return "workflow-registry";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 215;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Workflow registry file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        int deptIdx = findColumn(headers, "departmentName");
        int typeIdx = findColumn(headers, "workflowType");
        int orderIdx = findColumn(headers, "stageOrder");
        int idIdx = findColumn(headers, "stageId");
        int titleIdx = findColumn(headers, "stageTitle");
        int personasIdx = findColumn(headers, "allowedPersonas");
        int pageKeyIdx = findColumn(headers, "pageKey");
        int actionsIdx = findColumn(headers, "requiredActions");

        if (typeIdx < 0 || orderIdx < 0 || personasIdx < 0) {
            throw new IllegalArgumentException(
                    "Workflow registry " + fileName + " requires workflowType, stageOrder, allowedPersonas columns");
        }

        List<WorkflowStageDefinition> loaded = new ArrayList<>();
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (GenericValidator.isBlankOrNull(line)) {
                continue;
            }
            String[] values = parseCsvLine(line);
            String departmentName = deptIdx >= 0 && deptIdx < values.length ? values[deptIdx].trim() : "";
            String workflowType = values[typeIdx].trim();
            int stageOrder = Integer.parseInt(values[orderIdx].trim());
            String stageId = idIdx >= 0 && idIdx < values.length ? values[idIdx].trim() : "stage-" + stageOrder;
            String stageTitle = titleIdx >= 0 && titleIdx < values.length ? values[titleIdx].trim() : stageId;
            String personasRaw = values[personasIdx].trim();
            String pageKey = pageKeyIdx >= 0 && pageKeyIdx < values.length && !values[pageKeyIdx].isBlank()
                    ? values[pageKeyIdx].trim()
                    : stageId;
            String actionsRaw = actionsIdx >= 0 && actionsIdx < values.length ? values[actionsIdx].trim() : "";
            Set<NotebookStageAction> allowedActions = NotebookStageAction.parseActions(actionsRaw);

            WorkflowRegistryService.validateDepartmentName(departmentName, fileName, lineNumber);
            List<String> personas = WorkflowRegistryService.parsePersonas(personasRaw);
            if (personas.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                        fileName + " line " + lineNumber + ": no valid SRS personas for stage " + stageId);
            }

            loaded.add(new WorkflowStageDefinition(departmentName, workflowType, stageOrder, stageId, pageKey,
                    stageTitle, personas, allowedActions));
        }

        if (!loaded.isEmpty()) {
            workflowRegistryService.replaceRegistry(loaded);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                    "Loaded " + loaded.size() + " workflow stages from " + fileName);
        }
    }

    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    private static int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }
}
