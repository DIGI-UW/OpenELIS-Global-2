package org.openelisglobal.vector.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.vector.valueholder.VectorTrapType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads vector trap types from vector-trap-types.csv at startup.
 *
 * CSV format (leading # lines skipped, first non-comment line is header):
 * name,description,isActive,sampleTypeAbbrevs
 *
 * sampleTypeAbbrevs — semicolon-separated localAbbreviation values of vector
 * sample types (domain=V). Each abbreviation is resolved to a type_of_sample
 * row; unrecognised abbreviations are skipped with a warning.
 *
 * Runs at load order 450: after sample types (100) and species (400). Existing
 * records matched by name are updated; new ones are inserted.
 */
@Component
public class VectorTrapTypeConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private VectorTrapTypeService vectorTrapTypeService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Override
    public String getDomainName() {
        return "vector-trap-types";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 450;
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = null;
        String readLine;
        while ((readLine = reader.readLine()) != null) {
            if (!readLine.trim().isEmpty() && !readLine.trim().startsWith("#")) {
                headerLine = readLine;
                break;
            }
        }
        if (headerLine == null) {
            throw new IllegalArgumentException("Vector trap type file " + fileName + " is empty or has no header");
        }

        String[] headers = parseCsvLine(headerLine);
        int nameIdx = findColumn(headers, "name");
        int descIdx = findColumn(headers, "description");
        int activeIdx = findColumn(headers, "isActive");
        int abbrevIdx = findColumn(headers, "sampleTypeAbbrevs");

        if (nameIdx < 0 || abbrevIdx < 0) {
            throw new IllegalArgumentException(
                    "Vector trap type CSV " + fileName + " must have name and sampleTypeAbbrevs columns");
        }

        List<VectorTrapType> existing = vectorTrapTypeService.getAll();

        int loaded = 0;
        String line;
        int lineNum = 1;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            try {
                if (processRow(parseCsvLine(line), nameIdx, descIdx, activeIdx, abbrevIdx, existing)) {
                    loaded++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error on line " + lineNum + " of " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Loaded " + loaded + " vector trap types from " + fileName);
    }

    private boolean processRow(String[] values, int nameIdx, int descIdx, int activeIdx, int abbrevIdx,
            List<VectorTrapType> existing) {

        String name = get(values, nameIdx);
        if (name.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow", "Skipping row with missing name");
            return false;
        }

        String abbrevField = get(values, abbrevIdx);
        Set<String> sampleTypeIds = resolveSampleTypeIds(abbrevField);
        if (sampleTypeIds.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "No valid sampleTypeAbbrevs for trap type '" + name + "' — skipping");
            return false;
        }

        boolean isActive = !"N".equalsIgnoreCase(get(values, activeIdx));
        String description = get(values, descIdx);

        VectorTrapType match = existing.stream().filter(t -> name.equalsIgnoreCase(t.getName())).findFirst()
                .orElse(null);

        if (match != null) {
            VectorTrapType patch = new VectorTrapType();
            patch.setName(name);
            patch.setDescription(description.isEmpty() ? match.getDescription() : description);
            patch.setActive(isActive);
            vectorTrapTypeService.patchUpdate(match.getId(), patch, sampleTypeIds, "1");
            LogEvent.logInfo(this.getClass().getSimpleName(), "processRow", "Updated vector trap type: " + name);
        } else {
            VectorTrapType trapType = new VectorTrapType();
            trapType.setName(name);
            trapType.setDescription(description.isEmpty() ? null : description);
            trapType.setActive(isActive);
            vectorTrapTypeService.create(trapType, sampleTypeIds, "1");
            LogEvent.logInfo(this.getClass().getSimpleName(), "processRow", "Created vector trap type: " + name);
        }
        return true;
    }

    private Set<String> resolveSampleTypeIds(String abbrevField) {
        Set<String> ids = new HashSet<>();
        if (abbrevField == null || abbrevField.isEmpty()) {
            return ids;
        }
        for (String abbrev : abbrevField.split(";")) {
            abbrev = abbrev.trim();
            if (abbrev.isEmpty()) {
                continue;
            }
            TypeOfSample tos = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(abbrev, "V");
            if (tos == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveSampleTypeIds",
                        "Unknown sampleTypeAbbrev '" + abbrev + "' — skipping");
            } else {
                ids.add(tos.getId());
            }
        }
        return ids;
    }

    private String get(String[] values, int idx) {
        if (idx < 0 || idx >= values.length) {
            return "";
        }
        return values[idx] != null ? values[idx] : "";
    }

    private int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }
}
