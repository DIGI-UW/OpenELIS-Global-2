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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.localization.ShippedLabels;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 *
 * <p>
 * A localization value is a test's name or another label everyone sees, so
 * every write needs the admin role the Test Catalog editor requires (OGC-1457);
 * reads stay open to signed-in users.
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

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    /**
     * A test's name and reporting name are cached for the pickers, results search
     * and reports; rebuild those caches when one of them is retranslated, the same
     * way a Basic Info save does, so the new name shows everywhere at once.
     */
    private void refreshTestNamesIfTestLocalization(String localizationId) {
        if (testService.isNameLocalization(localizationId)) {
            refreshTestNameCaches();
        }
    }

    private void refreshTestNameCaches() {
        testService.refreshTestNames();
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.ALL_TESTS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.ORDERABLE_TESTS);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
        typeOfSampleService.clearCache();
    }

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

        Map<String, String> shippedKeys = localizationService.shippedDisplayKeysByLocalizationId();
        List<SupportedLocale> active = supportedLocaleService.getAllActive();
        List<LocalizationDTO> dtos = localizations.stream().map(l -> toDTO(l, shippedKeys, active))
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
        List<Localization> missing = localizationService.findMissingTranslationsForLocale(locale);
        Map<String, String> shippedKeys = localizationService.shippedDisplayKeysByLocalizationId();
        List<SupportedLocale> active = supportedLocaleService.getAllActive();
        List<LocalizationDTO> dtos = missing.stream().map(l -> toDTO(l, shippedKeys, active))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get translation statistics.
     *
     * @return statistics about translations per locale
     */
    @GetMapping("/stats")
    public ResponseEntity<TranslationStatsDTO> getTranslationStats() {
        List<Object[]> rows = localizationService.getTranslationStatsForAllActiveLocales();
        int totalEntries = (int) localizationService.getCount();

        TranslationStatsDTO stats = new TranslationStatsDTO();
        stats.setTotalEntries(totalEntries);

        Map<String, LocaleStatsDTO> localeStats = new HashMap<>();
        for (Object[] row : rows) {
            String localeCode = (String) row[0];
            String displayName = (String) row[1];
            int translated = ((Number) row[2]).intValue();
            int missing = ((Number) row[3]).intValue();

            LocaleStatsDTO localeStat = new LocaleStatsDTO();
            localeStat.setLocaleCode(localeCode);
            localeStat.setDisplayName(displayName);
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
     * @param id           the localization ID
     * @param translations map of locale code to translation value
     * @return the updated localization
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/translations")
    public ResponseEntity<?> updateTranslations(@PathVariable String id,
            @RequestBody Map<String, String> translations) {
        try {
            Localization localization;
            try {
                localization = localizationService.get(id);
            } catch (RuntimeException notFound) {
                localization = null;
            }
            if (localization == null) {
                return ResponseEntity.notFound().build();
            }
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                if (!isKnownLocale(localization, entry.getKey())) {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Unknown locale: " + entry.getKey()));
                }
                if (entry.getValue() == null) {
                    return ResponseEntity.badRequest().body(
                            new ErrorResponse("The " + entry.getKey() + " value is missing; send \"\" to clear it"));
                }
            }
            String fallbackLocale = supportedLocaleService.getFallbackLocaleCode();
            if (fallbackLocale != null && translations.containsKey(fallbackLocale)
                    && (translations.get(fallbackLocale) == null || translations.get(fallbackLocale).isBlank())) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "The " + fallbackLocale + " value is what every screen falls back to and cannot be blank"));
            }

            String displayKey = localizationService.shippedDisplayKeysByLocalizationId().get(id);
            if (displayKey != null && translations.values().stream().anyMatch(String::isBlank)) {
                return ResponseEntity.badRequest().body(overrideBlank());
            }

            String sysUserId = getSysUserId(request);

            for (Map.Entry<String, String> entry : translations.entrySet()) {
                writeTranslation(localization, displayKey, entry.getKey(), entry.getValue(), sysUserId);
            }
            refreshTestNamesIfTestLocalization(id);

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
     * @param id      the localization ID
     * @param locale  the locale code
     * @param request the translation value
     * @return the updated localization
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/translations/{locale}")
    public ResponseEntity<?> setTranslation(@PathVariable String id, @PathVariable String locale,
            @RequestBody TranslationRequest request) {
        try {
            Localization localization = localizationService.get(id);
            if (localization == null) {
                return ResponseEntity.notFound().build();
            }
            String fallbackLocale = supportedLocaleService.getFallbackLocaleCode();
            if (!isKnownLocale(localization, locale)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Unknown locale: " + locale));
            }
            if (locale.equals(fallbackLocale) && (request.getValue() == null || request.getValue().isBlank())) {
                return ResponseEntity.badRequest().body(new ErrorResponse(
                        "The " + fallbackLocale + " value is what every screen falls back to and cannot be blank"));
            }

            String displayKey = localizationService.shippedDisplayKeysByLocalizationId().get(id);
            if (displayKey != null && (request.getValue() == null || request.getValue().isBlank())) {
                return ResponseEntity.badRequest().body(overrideBlank());
            }

            writeTranslation(localization, displayKey, locale, request.getValue(), getSysUserId(this.request));
            refreshTestNamesIfTestLocalization(id);

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
     * Return a shipped value's label in one language to the shipped translation,
     * dropping the lab's override (OGC-1507 FR-4). Only a value that ships with a
     * display key has a shipped translation to return to.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/translations/{locale}/reset")
    public ResponseEntity<?> resetToShipped(@PathVariable String id, @PathVariable String locale) {
        Map<String, String> shippedKeys = localizationService.shippedDisplayKeysByLocalizationId();
        String displayKey = shippedKeys.get(id);
        if (displayKey == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(UiMessages.current().get("localization.error.notShipped")));
        }
        List<SupportedLocale> active = supportedLocaleService.getAllActive();
        if (active.stream().noneMatch(supported -> supported.getLocaleCode().equals(locale))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Unknown locale: " + locale));
        }
        try {
            Localization localization = localizationService.get(id);
            String shipped = ShippedLabels.shippedLabel(displayKey, localization, Locale.forLanguageTag(locale));
            localizationValueService.setSystemTranslation(id, locale, shipped, getSysUserId(request), false);
            LogEvent.logInfo(this.getClass().getSimpleName(), "resetToShipped",
                    "Reset localization " + id + " (" + displayKey + "), locale " + locale + " to the shipped label");
            return ResponseEntity.ok(toDTO(localizationService.get(id), shippedKeys, active));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to reset the translation: " + e.getMessage()));
        }
    }

    /**
     * A shipped value's label set to its shipped translation is not an override, so
     * it keeps following Transifex; any other text is the lab's own label.
     */
    private void writeTranslation(Localization localization, String displayKey, String locale, String value,
            String sysUserId) {
        if (displayKey != null
                && value.equals(ShippedLabels.shippedLabel(displayKey, localization, Locale.forLanguageTag(locale)))) {
            localizationValueService.setSystemTranslation(localization.getId(), locale, value, sysUserId, false);
        } else {
            localizationValueService.setTranslation(localization.getId(), locale, value, sysUserId);
        }
    }

    /**
     * A label can be written for a supported language, or for a locale the entry
     * already holds (a configuration file may carry one that is not offered).
     */
    private boolean isKnownLocale(Localization localization, String locale) {
        if (locale == null || locale.isBlank() || locale.length() > 10) {
            return false;
        }
        if (localization.getValues() != null && localization.getValues().containsKey(locale)) {
            return true;
        }
        return supportedLocaleService.getAll().stream().anyMatch(supported -> supported.getLocaleCode().equals(locale));
    }

    /** Why an imported entry is refused, or null: the editor's rules apply. */
    private String refusedImport(Localization localization, String displayKey, Map<String, String> translations) {
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (!isKnownLocale(localization, entry.getKey())) {
                return "Unknown locale: " + entry.getKey();
            }
            if (entry.getValue() == null) {
                return "The " + entry.getKey() + " value is missing";
            }
            if (displayKey != null && entry.getValue().isBlank()) {
                return overrideBlank().getMessage();
            }
        }
        return null;
    }

    private ErrorResponse overrideBlank() {
        return new ErrorResponse(UiMessages.current().get("localization.error.overrideBlank"));
    }

    /**
     * Bulk update translations from CSV/import data.
     *
     * @param importData list of localization import records
     * @return import result summary
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public ResponseEntity<?> importTranslations(@RequestBody List<LocalizationImportDTO> importData) {
        try {
            String sysUserId = getSysUserId(request);
            Map<String, String> shippedKeys = localizationService.shippedDisplayKeysByLocalizationId();
            int updated = 0;
            int failed = 0;
            List<String> errors = new ArrayList<>();

            for (LocalizationImportDTO item : importData) {
                try {
                    Localization localization = localizationService.get(item.getId());
                    if (localization != null) {
                        String refused = refusedImport(localization, shippedKeys.get(item.getId()),
                                item.getTranslations());
                        if (refused != null) {
                            failed++;
                            errors.add(item.getId() + ": " + refused);
                            continue;
                        }
                        for (Map.Entry<String, String> entry : item.getTranslations().entrySet()) {
                            writeTranslation(localization, shippedKeys.get(item.getId()), entry.getKey(),
                                    entry.getValue(), sysUserId);
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

            if (updated > 0) {
                refreshTestNameCaches();
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
        return toDTO(localization, localizationService.shippedDisplayKeysByLocalizationId(),
                supportedLocaleService.getAllActive());
    }

    /**
     * A value OpenELIS ships with a display key also tells, per active language,
     * whether its label is the shipped translation or the lab's override, and what
     * the shipped translation reads (OGC-1507 FR-6); any other entry is the site's
     * own.
     */
    private LocalizationDTO toDTO(Localization localization, Map<String, String> shippedKeys,
            List<SupportedLocale> activeLocales) {
        LocalizationDTO dto = new LocalizationDTO();
        dto.setId(localization.getId());
        dto.setDescription(localization.getDescription());
        dto.setTranslations(localization.getValuesAsMap());
        String displayKey = shippedKeys.get(localization.getId());
        dto.setDisplayKey(displayKey);
        for (SupportedLocale supported : activeLocales) {
            String code = supported.getLocaleCode();
            dto.getOrigins().put(code, ShippedLabels.origin(displayKey, localization, code).name());
            if (displayKey != null) {
                dto.getShippedLabels().put(code,
                        ShippedLabels.shippedLabel(displayKey, localization, supported.toLocale()));
                dto.getLabels().put(code, ShippedLabels.label(displayKey, localization, "", supported.toLocale()));
            }
        }

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
        private String displayKey;
        private Map<String, String> origins = new HashMap<>();
        private Map<String, String> shippedLabels = new HashMap<>();
        private Map<String, String> labels = new HashMap<>();

        /** For a shipped value, the label each language shows. */
        public Map<String, String> getLabels() {
            return labels;
        }

        public String getDisplayKey() {
            return displayKey;
        }

        public void setDisplayKey(String displayKey) {
            this.displayKey = displayKey;
        }

        public Map<String, String> getOrigins() {
            return origins;
        }

        public Map<String, String> getShippedLabels() {
            return shippedLabels;
        }

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
