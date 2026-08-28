package org.openelisglobal.eqa.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Configuration handler for EQA schemes/programmes (OGC-935). Lets a deployment
 * define its EQA programme registry as CSV under
 * {@code configuration/backend/eqa-programs/} instead of code — the registry is
 * implementation data (which schemes a lab runs or joins), so it belongs to the
 * instance, not the repository.
 *
 * Expected CSV format (header row required; only {@code name} is mandatory):
 *
 * <pre>
 * name,description,provider,schemeType,frequency,testSection,active
 * CPHL National HIV Serology EQA,PT for HIV serology sites,"CPHL, Port Moresby",REGIONAL_PT,Quarterly,Serology,Y
 * </pre>
 *
 * Rows upsert by programme name: a new name inserts, an existing name updates
 * the non-empty columns, so a corrected CSV re-applies without duplicating and
 * without erasing values the file does not mention. {@code testSection}
 * resolves against the active test-section catalog by name; an unknown section
 * logs and leaves the link empty rather than failing the file.
 */
@Component
public class EQAProgramConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private EQAProgramService eqaProgramService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    @Qualifier("daemonSysUserId")
    private String daemonSysUserId;

    @Override
    public String getDomainName() {
        return "eqa-programs";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 330; // after test sections (base entities), alongside the other QA handlers
    }

    // Deliberately not @Transactional at file grain: each row commits through the
    // service's own transaction, so a row the service rejects (BR-004: external
    // scheme without a provider) is logged and skipped without poisoning the
    // whole file with a rollback-only transaction.
    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("EQA programme configuration file " + fileName + " is empty");
        }
        String[] headers = parseCsvLine(headerLine);
        int nameIndex = findColumnIndex(headers, "name");
        if (nameIndex < 0) {
            throw new IllegalArgumentException(
                    "EQA programme configuration file " + fileName + " must have a 'name' column");
        }
        int descriptionIndex = findColumnIndex(headers, "description");
        int providerIndex = findColumnIndex(headers, "provider");
        int schemeTypeIndex = findColumnIndex(headers, "schemeType");
        int frequencyIndex = findColumnIndex(headers, "frequency");
        int testSectionIndex = findColumnIndex(headers, "testSection");
        int activeIndex = findColumnIndex(headers, "active");

        Map<String, EQAProgram> existingByName = new HashMap<>();
        for (EQAProgram program : eqaProgramService.getAll()) {
            if (program.getName() != null) {
                existingByName.put(program.getName(), program);
            }
        }
        Map<String, TestSection> sectionsByName = new HashMap<>();
        for (TestSection section : testSectionService.getAllActiveTestSections()) {
            if (section.getTestSectionName() != null) {
                sectionsByName.putIfAbsent(section.getTestSectionName(), section);
            }
        }

        int processedCount = 0;
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            try {
                String[] values = parseCsvLine(line);
                String name = getValueOrEmpty(values, nameIndex);
                if (name.isEmpty()) {
                    continue;
                }
                String description = getValueOrEmpty(values, descriptionIndex);
                String provider = getValueOrEmpty(values, providerIndex);
                String schemeType = getValueOrEmpty(values, schemeTypeIndex);
                String frequency = getValueOrEmpty(values, frequencyIndex);
                String testSection = getValueOrEmpty(values, testSectionIndex);
                String active = getValueOrEmpty(values, activeIndex);

                TestSection section = null;
                if (!testSection.isEmpty()) {
                    section = sectionsByName.get(testSection);
                    if (section == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                                "Test section '" + testSection + "' (line " + lineNumber + " of " + fileName
                                        + ") is not in the active catalog; leaving the programme's section empty");
                    }
                }

                EQAProgram existing = existingByName.get(name);
                if (existing == null) {
                    EQAProgram program = new EQAProgram();
                    program.setName(name);
                    program.setDescription(description.isEmpty() ? null : description);
                    program.setProvider(provider.isEmpty() ? null : provider);
                    program.setSchemeType(schemeType.isEmpty() ? EQASchemeType.INTERNATIONAL_PT
                            : EQASchemeType.valueOf(schemeType.toUpperCase()));
                    program.setFrequency(frequency.isEmpty() ? null : frequency);
                    program.setTestSection(section);
                    program.setIsActive(active.isEmpty() || "Y".equalsIgnoreCase(active));
                    program.setSysUserId(daemonSysUserId);
                    eqaProgramService.insert(program);
                    existingByName.put(name, program);
                } else {
                    if (!description.isEmpty()) {
                        existing.setDescription(description);
                    }
                    if (!provider.isEmpty()) {
                        existing.setProvider(provider);
                    }
                    if (!schemeType.isEmpty()) {
                        existing.setSchemeType(EQASchemeType.valueOf(schemeType.toUpperCase()));
                    }
                    if (!frequency.isEmpty()) {
                        existing.setFrequency(frequency);
                    }
                    if (section != null) {
                        existing.setTestSection(section);
                    }
                    if (!active.isEmpty()) {
                        existing.setIsActive("Y".equalsIgnoreCase(active));
                    }
                    existing.setSysUserId(daemonSysUserId);
                    eqaProgramService.update(existing);
                }
                processedCount++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Processed " + processedCount + " EQA programme rows from " + fileName);
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
            if (columnName.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value.trim() : "";
        }
        return "";
    }
}
