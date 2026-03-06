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
 * defaults to "N" if not specified - Only one locale should have isFallback=Y
 * (the last one wins if multiple) - sortOrder determines display order in
 * dropdowns
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

        List<SupportedLocale> processedLocales = new ArrayList<>();
        String line;
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                SupportedLocale locale = processCsvLine(values, localeCodeIndex, displayNameIndex, isActiveIndex,
                        isFallbackIndex, sortOrderIndex);
                if (locale != null) {
                    processedLocales.add(locale);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
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

    private SupportedLocale processCsvLine(String[] values, int localeCodeIndex, int displayNameIndex,
            int isActiveIndex, int isFallbackIndex, int sortOrderIndex) {

        String localeCode = getValueOrEmpty(values, localeCodeIndex);
        String displayName = getValueOrEmpty(values, displayNameIndex);

        if (localeCode.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing localeCode");
            return null;
        }

        if (displayName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row with missing displayName");
            return null;
        }

        // Check if locale already exists
        Optional<SupportedLocale> existingLocale = supportedLocaleService.getByLocaleCode(localeCode);
        SupportedLocale locale;

        if (existingLocale.isPresent()) {
            locale = existingLocale.get();
            updateLocaleFromCsv(locale, displayName, values, isActiveIndex, isFallbackIndex, sortOrderIndex);
            supportedLocaleService.update(locale);
            LogEvent.logDebug(this.getClass().getSimpleName(), "processCsvLine",
                    "Updated existing locale: " + localeCode);
        } else {
            locale = new SupportedLocale();
            locale.setLocaleCode(localeCode);
            updateLocaleFromCsv(locale, displayName, values, isActiveIndex, isFallbackIndex, sortOrderIndex);
            String id = supportedLocaleService.insert(locale);
            locale.setId(id);
            LogEvent.logDebug(this.getClass().getSimpleName(), "processCsvLine", "Created new locale: " + localeCode);
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

        String isFallback = getValueOrEmpty(values, isFallbackIndex);
        if (!isFallback.isEmpty()) {
            boolean shouldBeFallback = "Y".equalsIgnoreCase(isFallback) || "true".equalsIgnoreCase(isFallback);
            if (shouldBeFallback) {
                // Clear any existing fallback first
                clearExistingFallback();
            }
            locale.setFallback(shouldBeFallback);
        } else {
            locale.setFallback(false); // Default to not fallback
        }

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

        locale.setSysUserId("1"); // System user for configuration loading
    }

    private void clearExistingFallback() {
        Optional<SupportedLocale> existingFallback = supportedLocaleService.getFallback();
        if (existingFallback.isPresent()) {
            SupportedLocale fallback = existingFallback.get();
            fallback.setFallback(false);
            fallback.setSysUserId("1");
            supportedLocaleService.update(fallback);
        }
    }
}
