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
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration handler for linking notebook templates to test sections (departments).
 * <p>
 * This handler processes CSV files to create associations between notebooks and departments,
 * allowing test sections to see and access specific notebook templates.
 * <p>
 * CSV Format: notebookTitle,departmentName
 */
@Component
@Transactional
public class NotebookDepartmentConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private NoteBookDAO noteBookDAO;

    @Autowired
    private TestSectionService testSectionService;

    @Override
    public String getDomainName() {
        return "notebook-departments";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 220;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (GenericValidator.isBlankOrNull(headerLine)) {
            throw new IllegalArgumentException("Notebook-department configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);

        int notebookTitleIndex = findColumnIndex(headers, "notebookTitle");
        int departmentNameIndex = findColumnIndex(headers, "departmentName");

        if (notebookTitleIndex == -1) {
            throw new IllegalArgumentException("Notebook-department configuration file " + fileName +
                    " must have a 'notebookTitle' column");
        }
        if (departmentNameIndex == -1) {
            throw new IllegalArgumentException("Notebook-department configuration file " + fileName +
                    " must have a 'departmentName' column");
        }

        String line;
        int lineNumber = 1;
        int processedCount = 0;
        int skippedCount = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();

            if (GenericValidator.isBlankOrNull(line) || line.startsWith("#")) {
                continue;
            }

            try {
                boolean processed = processNotebookDepartmentLine(line, notebookTitleIndex, departmentNameIndex, fileName, lineNumber);
                if (processed) {
                    processedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Notebook-department configuration processing completed for " + fileName +
                ". Processed: " + processedCount + ", Skipped: " + skippedCount);
    }

    private boolean processNotebookDepartmentLine(String line, int notebookTitleIndex, int departmentNameIndex,
            String fileName, int lineNumber) {

        String[] values = parseCsvLine(line);

        String notebookTitle = getValueOrEmpty(values, notebookTitleIndex);
        String departmentName = getValueOrEmpty(values, departmentNameIndex);

        // Validate required fields
        if (GenericValidator.isBlankOrNull(notebookTitle)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processNotebookDepartmentLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing notebook title");
            return false;
        }

        if (GenericValidator.isBlankOrNull(departmentName)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processNotebookDepartmentLine",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing department name");
            return false;
        }

        List<NoteBook> notebooks = noteBookDAO.getAllMatching("title", notebookTitle);
        NoteBook notebook = null;
        for (NoteBook nb : notebooks) {
            if (nb.getIsTemplate()) {
                notebook = nb;
                break;
            }
        }

        if (notebook == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processNotebookDepartmentLine",
                    "Skipping line " + lineNumber + " in " + fileName +
                    ": notebook template with title '" + notebookTitle + "' not found");
            return false;
        }

        TestSection department = testSectionService.getTestSectionByName(departmentName);
        if (department == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processNotebookDepartmentLine",
                    "Skipping line " + lineNumber + " in " + fileName +
                    ": test section with name '" + departmentName + "' not found");
            return false;
        }

        if (notebook.getDepartments().contains(department)) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processNotebookDepartmentLine",
                    "Association between notebook '" + notebookTitle + "' and department '" +
                    departmentName + "' already exists. Skipping line " + lineNumber);
            return false;
        }

        try {
            notebook.getDepartments().add(department);
            noteBookDAO.update(notebook);

            LogEvent.logInfo(this.getClass().getSimpleName(), "processNotebookDepartmentLine",
                    "Successfully linked notebook '" + notebookTitle + "' to department '" + departmentName + "'");
            return true;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processNotebookDepartmentLine",
                    "Failed to create association between notebook '" + notebookTitle +
                    "' and department '" + departmentName + "': " + e.getMessage());
            return false;
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());
        return values.toArray(new String[0]);
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }
}