/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.localization.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing localization entries and their translations.
 * Provides endpoints for viewing and editing translations.
 */
@RestController
@RequestMapping("/rest/localizations")
public class LocalizationRestController extends BaseController {

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Autowired
    private SupportedLocaleService supportedLocaleService;

    /**
     * Get all localizations with their translations.
     *
     * @param description optional filter by description
     * @return list of localizations
     */
    @GetMapping
    public ResponseEntity<List<LocalizationDTO>> getAllLocalizations(
            @RequestParam(required = false) String description) {
        List<Localization> localizations;
        if (description != null && !description.isEmpty()) {
            localizations = localizationService.getAllMatching("description", description);
        } else {
            localizations = localizationService.getAll();
        }

        List<LocalizationDTO> dtos = localizations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific localization by ID.
     *
     * @param id the localization ID
     * @return the localization with all translations
     */
    @GetMapping("/{id}")
    public ResponseEntity<LocalizationDTO> getLocalization(@PathVariable String id) {
        try {
            Localization localization = localizationService.get(id);
            return ResponseEntity.ok(toDTO(localization));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get localizations with missing translations for a specific locale.
     *
     * @param locale the locale code to check
     * @return list of localizations missing translations for that locale
     */
    @GetMapping("/missing/{locale}")
    public ResponseEntity<List<LocalizationDTO>> getMissingTranslations(@PathVariable String locale) {
        List<Localization> allLocalizations = localizationService.getAll();
        List<LocalizationDTO> missing = new ArrayList<>();

        for (Localization loc : allLocalizations) {
            Map<String, LocalizationValue> values = loc.getValues();
            if (values == null || !values.containsKey(locale) ||
                    values.get(locale).getValue() == null ||
                    values.get(locale).getValue().isEmpty()) {
                missing.add(toDTO(loc));
            }
        }

        return ResponseEntity.ok(missing);
    }

    /**
     * Get translation statistics.
     *
     * @return statistics about translations per locale
     */
    @GetMapping("/stats")
    public ResponseEntity<TranslationStatsDTO> getTranslationStats() {
        List<SupportedLocale> activeLocales = supportedLocaleService.getAllActive();
        List<Localization> allLocalizations = localizationService.getAll();
        int totalEntries = allLocalizations.size();

        TranslationStatsDTO stats = new TranslationStatsDTO();
        stats.setTotalEntries(totalEntries);

        Map<String, LocaleStatsDTO> localeStats = new HashMap<>();
        for (SupportedLocale locale : activeLocales) {
            String localeCode = locale.getLocaleCode();
            int translated = 0;
            int missing = 0;

            for (Localization loc : allLocalizations) {
                Map<String, LocalizationValue> values = loc.getValues();
                if (values != null && values.containsKey(localeCode) &&
                        values.get(localeCode).getValue() != null &&
                        !values.get(localeCode).getValue().isEmpty()) {
                    translated++;
                } else {
                    missing++;
                }
            }

            LocaleStatsDTO localeStat = new LocaleStatsDTO();
            localeStat.setLocaleCode(localeCode);
            localeStat.setDisplayName(locale.getDisplayName());
            localeStat.setTranslated(translated);
            localeStat.setMissing(missing);
            localeStat.setPercentage(totalEntries > 0 ? (translated * 100.0 / totalEntries) : 0);
            localeStats.put(localeCode, localeStat);
        }

        stats.setLocaleStats(localeStats);
        return ResponseEntity.ok(stats);
    }

    /**
     * Update translations for a localization entry.
     *
     * @param id the localization ID
     * @param translations map of locale code to translation value
     * @return the updated localization
     */
    @PutMapping("/{id}/translations")
    public ResponseEntity<?> updateTranslations(
            @PathVariable String id,
            @RequestBody Map<String, String> translations) {
        try {
            Localization localization = localizationService.get(id);
            if (localization == null) {
                return ResponseEntity.notFound().build();
            }

            String sysUserId = getSysUserId(request);

            for (Map.Entry<String, String> entry : translations.entrySet()) {
                String locale = entry.getKey();
                String value = entry.getValue();

                LocalizationValue lv = localizationValueService.setTranslation(id, locale, value);
                lv.setSysUserId(sysUserId);
            }

            // Refresh the localization to get updated values
            localization = localizationService.get(id);

            LogEvent.logInfo(this.getClass().getSimpleName(), "updateTranslations",
                    "Updated translations for localization ID: " + id);

            return ResponseEntity.ok(toDTO(localization));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update translations: " + e.getMessage()));
        }
    }

    /**
     * Set a single translation for a localization entry.
     *
     * @param id the localization ID
     * @param locale the locale code
     * @param request the translation value
     * @return the updated localization
     */
    @PostMapping("/{id}/translations/{locale}")
    public ResponseEntity<?> setTranslation(
            @PathVariable String id,
            @PathVariable String locale,
            @RequestBody TranslationRequest request) {
        try {
            Localization localization = localizationService.get(id);
            if (localization == null) {
                return ResponseEntity.notFound().build();
            }

            LocalizationValue lv = localizationValueService.setTranslation(id, locale, request.getValue());
            lv.setSysUserId(getSysUserId(this.request));

            // Refresh the localization to get updated values
            localization = localizationService.get(id);

            LogEvent.logInfo(this.getClass().getSimpleName(), "setTranslation",
                    "Set translation for localization ID: " + id + ", locale: " + locale);

            return ResponseEntity.ok(toDTO(localization));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to set translation: " + e.getMessage()));
        }
    }

    /**
     * Bulk update translations from CSV/import data.
     *
     * @param importData list of localization import records
     * @return import result summary
     */
    @PostMapping("/import")
    public ResponseEntity<?> importTranslations(@RequestBody List<LocalizationImportDTO> importData) {
        try {
            String sysUserId = getSysUserId(request);
            int updated = 0;
            int failed = 0;
            List<String> errors = new ArrayList<>();

            for (LocalizationImportDTO item : importData) {
                try {
                    Localization localization = localizationService.get(item.getId());
                    if (localization != null) {
                        for (Map.Entry<String, String> entry : item.getTranslations().entrySet()) {
                            LocalizationValue lv = localizationValueService.setTranslation(
                                    item.getId(), entry.getKey(), entry.getValue());
                            lv.setSysUserId(sysUserId);
                        }
                        updated++;
                    } else {
                        failed++;
                        errors.add("Localization not found: " + item.getId());
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Error updating " + item.getId() + ": " + e.getMessage());
                }
            }

            ImportResultDTO result = new ImportResultDTO();
            result.setUpdated(updated);
            result.setFailed(failed);
            result.setErrors(errors);

            LogEvent.logInfo(this.getClass().getSimpleName(), "importTranslations",
                    "Imported translations: " + updated + " updated, " + failed + " failed");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to import translations: " + e.getMessage()));
        }
    }

    /**
     * Export all translations for a specific locale.
     *
     * @param locale the locale code
     * @return list of translations for export
     */
    @GetMapping("/export/{locale}")
    public ResponseEntity<List<LocalizationExportDTO>> exportTranslations(@PathVariable String locale) {
        List<Localization> allLocalizations = localizationService.getAll();
        String fallbackLocale = supportedLocaleService.getFallbackLocaleCode();

        List<LocalizationExportDTO> exports = new ArrayList<>();
        for (Localization loc : allLocalizations) {
            LocalizationExportDTO export = new LocalizationExportDTO();
            export.setId(loc.getId());
            export.setDescription(loc.getDescription());

            Map<String, LocalizationValue> values = loc.getValues();

            // Get fallback value (usually English)
            if (values != null && values.containsKey(fallbackLocale)) {
                export.setFallbackValue(values.get(fallbackLocale).getValue());
            } else {
                export.setFallbackValue(loc.getEnglish()); // Legacy fallback
            }

            // Get target locale value
            if (values != null && values.containsKey(locale)) {
                export.setTranslatedValue(values.get(locale).getValue());
            } else {
                export.setTranslatedValue("");
            }

            exports.add(export);
        }

        return ResponseEntity.ok(exports);
    }

    @Override
    protected String findLocalForward(String forward) {
        return null;
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }

    private LocalizationDTO toDTO(Localization localization) {
        LocalizationDTO dto = new LocalizationDTO();
        dto.setId(localization.getId());
        dto.setDescription(localization.getDescription());
        dto.setTranslations(localization.getValuesAsMap());

        // Include legacy values if new values are empty
        if (dto.getTranslations().isEmpty()) {
            Map<String, String> legacy = new HashMap<>();
            String english = localization.getEnglish();
            String french = localization.getFrench();
            if (english != null && !english.isEmpty()) {
                legacy.put("en", english);
            }
            if (french != null && !french.isEmpty()) {
                legacy.put("fr", french);
            }
            dto.setTranslations(legacy);
        }

        return dto;
    }

    // DTOs

    public static class LocalizationDTO {
        private String id;
        private String description;
        private Map<String, String> translations = new HashMap<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, String> getTranslations() {
            return translations;
        }

        public void setTranslations(Map<String, String> translations) {
            this.translations = translations;
        }
    }

    public static class TranslationRequest {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class TranslationStatsDTO {
        private int totalEntries;
        private Map<String, LocaleStatsDTO> localeStats = new HashMap<>();

        public int getTotalEntries() {
            return totalEntries;
        }

        public void setTotalEntries(int totalEntries) {
            this.totalEntries = totalEntries;
        }

        public Map<String, LocaleStatsDTO> getLocaleStats() {
            return localeStats;
        }

        public void setLocaleStats(Map<String, LocaleStatsDTO> localeStats) {
            this.localeStats = localeStats;
        }
    }

    public static class LocaleStatsDTO {
        private String localeCode;
        private String displayName;
        private int translated;
        private int missing;
        private double percentage;

        public String getLocaleCode() {
            return localeCode;
        }

        public void setLocaleCode(String localeCode) {
            this.localeCode = localeCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public int getTranslated() {
            return translated;
        }

        public void setTranslated(int translated) {
            this.translated = translated;
        }

        public int getMissing() {
            return missing;
        }

        public void setMissing(int missing) {
            this.missing = missing;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }

    public static class LocalizationImportDTO {
        private String id;
        private Map<String, String> translations = new HashMap<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, String> getTranslations() {
            return translations;
        }

        public void setTranslations(Map<String, String> translations) {
            this.translations = translations;
        }
    }

    public static class LocalizationExportDTO {
        private String id;
        private String description;
        private String fallbackValue;
        private String translatedValue;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getFallbackValue() {
            return fallbackValue;
        }

        public void setFallbackValue(String fallbackValue) {
            this.fallbackValue = fallbackValue;
        }

        public String getTranslatedValue() {
            return translatedValue;
        }

        public void setTranslatedValue(String translatedValue) {
            this.translatedValue = translatedValue;
        }
    }

    public static class ImportResultDTO {
        private int updated;
        private int failed;
        private List<String> errors = new ArrayList<>();

        public int getUpdated() {
            return updated;
        }

        public void setUpdated(int updated) {
            this.updated = updated;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
