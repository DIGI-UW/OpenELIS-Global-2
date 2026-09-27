package org.openelisglobal.localization.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading supported locale configuration files. Supports CSV format
 * with locale definitions.
 *
 * Expected CSV format: localeCode,displayName,isActive,isFallback,sortOrder
 * en,English,Y,Y,1 fr,Français,Y,N,2 es,Español,Y,N,3
 *
 * Notes: - First line is the header (required) - localeCode and displayName are
 * required fields - isActive defaults to "Y" if not specified - isFallback
 * defaults to "N" if not specified - Only one locale may have isFallback=Y
 * (multiple fallbacks will cause a configuration error) - sortOrder determines
 * display order in dropdowns
 */
@Component
public class SupportedLocaleConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private SupportedLocaleService supportedLocaleService;

    @Override
    public String getDomainName() {
        return "locales";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 50; // Load very early since other configurations may reference locales
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Locale configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        int localeCodeIndex = findColumnIndex(headers, "localeCode");
        int displayNameIndex = findColumnIndex(headers, "displayName");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int isFallbackIndex = findColumnIndex(headers, "isFallback");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");

        // First pass: parse all lines and collect locale data
        List<LocaleRowData> parsedRows = new ArrayList<>();
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            String[] values = parseCsvLine(line);
            String localeCode = getValueOrEmpty(values, localeCodeIndex);
            String displayName = getValueOrEmpty(values, displayNameIndex);

            if (localeCode.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                        "Skipping line " + lineNumber + " with missing localeCode");
                continue;
            }
            if (displayName.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                        "Skipping line " + lineNumber + " with missing displayName");
                continue;
            }

            String isFallbackStr = getValueOrEmpty(values, isFallbackIndex);
            boolean isFallback = "Y".equalsIgnoreCase(isFallbackStr) || "true".equalsIgnoreCase(isFallbackStr);

            parsedRows.add(new LocaleRowData(lineNumber, values, localeCode, displayName, isFallback));
        }

        // Validate: at most one fallback locale
        List<LocaleRowData> fallbackRows = parsedRows.stream().filter(row -> row.isFallback).toList();
        if (fallbackRows.size() > 1) {
            String fallbackLocales = fallbackRows.stream().map(row -> row.localeCode + " (line " + row.lineNumber + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new IllegalArgumentException("Locale configuration file " + fileName
                    + " has multiple fallback locales. Only one locale can be marked as fallback. Found: "
                    + fallbackLocales);
        }

        // Second pass: persist locales now that validation passed
        List<SupportedLocale> processedLocales = new ArrayList<>();
        boolean needToClearExistingFallback = !fallbackRows.isEmpty();

        if (needToClearExistingFallback) {
            clearExistingFallback();
        }

        for (LocaleRowData row : parsedRows) {
            try {
                SupportedLocale locale = persistLocale(row, isActiveIndex, isFallbackIndex, sortOrderIndex);
                if (locale != null) {
                    processedLocales.add(locale);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + row.lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedLocales.size() + " supported locales from " + fileName);
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

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasLocaleCodeColumn = false;
        boolean hasDisplayNameColumn = false;

        for (String header : headers) {
            if ("localeCode".equalsIgnoreCase(header)) {
                hasLocaleCodeColumn = true;
            }
            if ("displayName".equalsIgnoreCase(header)) {
                hasDisplayNameColumn = true;
            }
        }

        if (!hasLocaleCodeColumn) {
            throw new IllegalArgumentException(
                    "Locale configuration file " + fileName + " must have a 'localeCode' column");
        }
        if (!hasDisplayNameColumn) {
            throw new IllegalArgumentException(
                    "Locale configuration file " + fileName + " must have a 'displayName' column");
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

    /**
     * Internal record to hold parsed row data before validation and persistence.
     */
    private record LocaleRowData(int lineNumber, String[] values, String localeCode, String displayName,
            boolean isFallback) {
    }

    private SupportedLocale persistLocale(LocaleRowData row, int isActiveIndex, int isFallbackIndex,
            int sortOrderIndex) {

        // Check if locale already exists
        Optional<SupportedLocale> existingLocale = supportedLocaleService.getByLocaleCode(row.localeCode);
        SupportedLocale locale;

        if (existingLocale.isPresent()) {
            locale = existingLocale.get();
            updateLocaleFromCsv(locale, row.displayName, row.values, isActiveIndex, isFallbackIndex, sortOrderIndex);
            supportedLocaleService.update(locale);
            LogEvent.logDebug(this.getClass().getSimpleName(), "persistLocale",
                    "Updated existing locale: " + row.localeCode);
        } else {
            locale = new SupportedLocale();
            locale.setLocaleCode(row.localeCode);
            updateLocaleFromCsv(locale, row.displayName, row.values, isActiveIndex, isFallbackIndex, sortOrderIndex);
            String id = supportedLocaleService.insert(locale);
            locale.setId(id);
            LogEvent.logDebug(this.getClass().getSimpleName(), "persistLocale",
                    "Created new locale: " + row.localeCode);
        }

        return locale;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private void updateLocaleFromCsv(SupportedLocale locale, String displayName, String[] values, int isActiveIndex,
            int isFallbackIndex, int sortOrderIndex) {

        locale.setDisplayName(displayName);

        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            locale.setActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive));
        } else {
            locale.setActive(true); // Default to active
        }

        // Fallback is already validated (at most one) and existing fallback cleared
        // before this method
        String isFallback = getValueOrEmpty(values, isFallbackIndex);
        boolean shouldBeFallback = "Y".equalsIgnoreCase(isFallback) || "true".equalsIgnoreCase(isFallback);
        locale.setFallback(shouldBeFallback);

        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                locale.setSortOrder(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateLocaleFromCsv",
                        "Invalid sortOrder value: " + sortOrderStr);
                locale.setSortOrder(0);
            }
        }

    }

    private void clearExistingFallback() {
        Optional<SupportedLocale> existingFallback = supportedLocaleService.getFallback();
        if (existingFallback.isPresent()) {
            SupportedLocale fallback = existingFallback.get();
            fallback.setFallback(false);
            supportedLocaleService.update(fallback);
        }
    }
}
