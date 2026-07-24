package org.openelisglobal.siteinformation.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SiteInformationConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    @Override
    public String getDomainName() {
        return "site-information";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 10;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                    "Site information file " + fileName + " is empty - skipping");
            return;
        }

        String[] headers = parseCsvLine(headerLine.trim());
        validateHeaders(headers, fileName);

        int nameIndex = findColumnIndex(headers, "name");
        int valueIndex = findColumnIndex(headers, "value");
        int domainNameIndex = findColumnIndex(headers, "domainName");
        int valueTypeIndex = findColumnIndex(headers, "valueType");
        int tagIndex = findColumnIndex(headers, "tag");
        int descriptionIndex = findColumnIndex(headers, "description");
        int instructionKeyIndex = findColumnIndex(headers, "instructionKey");
        int descriptionKeyIndex = findColumnIndex(headers, "descriptionKey");
        int groupIndex = findColumnIndex(headers, "group");
        int encryptedIndex = findColumnIndex(headers, "encrypted");

        String line;
        int lineNumber = 1;
        int processedRows = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }
            try {
                processRow(parseCsvLine(line.trim()), nameIndex, valueIndex, domainNameIndex, valueTypeIndex, tagIndex,
                        descriptionIndex, instructionKeyIndex, descriptionKeyIndex, groupIndex, encryptedIndex);
                processedRows++;
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedRows + " site information rows from " + fileName);
    }

    private void processRow(String[] values, int nameIndex, int valueIndex, int domainNameIndex, int valueTypeIndex,
            int tagIndex, int descriptionIndex, int instructionKeyIndex, int descriptionKeyIndex, int groupIndex,
            int encryptedIndex) {
        String name = getValueOrEmpty(values, nameIndex);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("site_information row is missing name");
        }

        SiteInformation siteInformation = siteInformationService.getSiteInformationByName(name);
        boolean isNew = siteInformation == null;
        if (isNew) {
            siteInformation = new SiteInformation();
            siteInformation.setName(name);
        }

        siteInformation.setValue(getValueOrEmpty(values, valueIndex));
        siteInformation
                .setValueType(defaultText(getValueOrEmpty(values, valueTypeIndex), siteInformation.getValueType()));
        siteInformation.setTag(getValueOrEmpty(values, tagIndex));
        siteInformation.setDescription(getValueOrEmpty(values, descriptionIndex));
        siteInformation.setInstructionKey(getValueOrEmpty(values, instructionKeyIndex));
        siteInformation.setDescriptionKey(getValueOrEmpty(values, descriptionKeyIndex));
        siteInformation.setGroup(parseGroup(getValueOrEmpty(values, groupIndex), siteInformation.getGroup()));
        siteInformation.setEncrypted(Boolean.parseBoolean(getValueOrEmpty(values, encryptedIndex)));
        siteInformation.setSysUserId("1");

        String domainName = getValueOrEmpty(values, domainNameIndex);
        if (!domainName.isEmpty()) {
            SiteInformationDomain domain = siteInformationDomainService.getByName(domainName);
            if (domain != null) {
                siteInformation.setDomain(domain);
            }
        }

        if (isNew) {
            siteInformationService.insert(siteInformation);
        } else {
            siteInformationService.update(siteInformation);
        }
    }

    private void validateHeaders(String[] headers, String fileName) {
        if (findColumnIndex(headers, "name") < 0) {
            throw new IllegalArgumentException("Site information file " + fileName + " must have a 'name' column");
        }
        if (findColumnIndex(headers, "value") < 0) {
            throw new IllegalArgumentException("Site information file " + fileName + " must have a 'value' column");
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
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

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            return values[index] == null ? "" : values[index];
        }
        return "";
    }

    private String defaultText(String valueType, String currentValueType) {
        if (!valueType.isEmpty()) {
            return valueType;
        }
        return currentValueType == null || currentValueType.isBlank() ? "text" : currentValueType;
    }

    private int parseGroup(String rawGroup, int currentGroup) {
        if (rawGroup.isEmpty()) {
            return currentGroup;
        }
        try {
            return Integer.parseInt(rawGroup);
        } catch (NumberFormatException e) {
            return currentGroup;
        }
    }
}
