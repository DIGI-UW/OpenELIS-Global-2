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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing supported locales/languages. Allows
 * administrators to add, remove, and configure available languages.
 */
@RestController
@RequestMapping("/rest/supportedlocales")
public class SupportedLocaleRestController extends BaseController {

    @Autowired
    private SupportedLocaleService supportedLocaleService;

    /**
     * Get all supported locales.
     *
     * @return list of all supported locales
     */
    @GetMapping
    public ResponseEntity<List<SupportedLocaleDTO>> getAllLocales() {
        List<SupportedLocale> locales = supportedLocaleService.getAll();
        List<SupportedLocaleDTO> dtos = locales.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get only active locales.
     *
     * @return list of active supported locales
     */
    @GetMapping("/active")
    public ResponseEntity<List<SupportedLocaleDTO>> getActiveLocales() {
        List<SupportedLocale> locales = supportedLocaleService.getAllActive();
        List<SupportedLocaleDTO> dtos = locales.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific locale by ID.
     *
     * @param id the locale ID
     * @return the locale if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupportedLocaleDTO> getLocale(@PathVariable String id) {
        try {
            SupportedLocale locale = supportedLocaleService.get(id);
            return ResponseEntity.ok(toDTO(locale));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get the fallback locale.
     *
     * @return the fallback locale if configured
     */
    @GetMapping("/fallback")
    public ResponseEntity<SupportedLocaleDTO> getFallbackLocale() {
        Optional<SupportedLocale> fallback = supportedLocaleService.getFallback();
        return fallback.map(locale -> ResponseEntity.ok(toDTO(locale))).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new supported locale.
     *
     * @param dto the locale data
     * @return the created locale
     */
    @PostMapping
    public ResponseEntity<?> createLocale(@RequestBody SupportedLocaleDTO dto) {
        try {
            // Check if locale code already exists
            Optional<SupportedLocale> existing = supportedLocaleService.getByLocaleCode(dto.getLocaleCode());
            if (existing.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Locale code already exists: " + dto.getLocaleCode()));
            }

            SupportedLocale locale = fromDTO(dto);
            locale.setSysUserId(getSysUserId(request));
            String id = supportedLocaleService.insert(locale);
            locale.setId(id);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createLocale",
                    "Created new supported locale: " + dto.getLocaleCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(locale));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create locale: " + e.getMessage()));
        }
    }

    /**
     * Update an existing locale.
     *
     * @param id  the locale ID
     * @param dto the updated locale data
     * @return the updated locale
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLocale(@PathVariable String id, @RequestBody SupportedLocaleDTO dto) {
        try {
            SupportedLocale existing = supportedLocaleService.get(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            // Don't allow changing locale code if it would conflict
            if (!existing.getLocaleCode().equals(dto.getLocaleCode())) {
                Optional<SupportedLocale> conflict = supportedLocaleService.getByLocaleCode(dto.getLocaleCode());
                if (conflict.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse("Locale code already exists: " + dto.getLocaleCode()));
                }
            }

            existing.setLocaleCode(dto.getLocaleCode());
            existing.setDisplayName(dto.getDisplayName());
            existing.setActive(dto.isActive());
            existing.setFallback(dto.isFallback());
            existing.setSortOrder(dto.getSortOrder());
            existing.setSysUserId(getSysUserId(request));

            supportedLocaleService.update(existing);

            LogEvent.logInfo(this.getClass().getSimpleName(), "updateLocale",
                    "Updated supported locale: " + dto.getLocaleCode());

            return ResponseEntity.ok(toDTO(existing));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update locale: " + e.getMessage()));
        }
    }

    /**
     * Delete a locale.
     *
     * @param id the locale ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLocale(@PathVariable String id) {
        try {
            SupportedLocale existing = supportedLocaleService.get(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            // Don't allow deleting the fallback locale
            if (existing.isFallback()) {
                return ResponseEntity.badRequest().body(
                        new ErrorResponse("Cannot delete the fallback locale. Set another locale as fallback first."));
            }

            supportedLocaleService.delete(existing);

            LogEvent.logInfo(this.getClass().getSimpleName(), "deleteLocale",
                    "Deleted supported locale: " + existing.getLocaleCode());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete locale: " + e.getMessage()));
        }
    }

    /**
     * Set a locale as the fallback.
     *
     * @param id the locale ID to set as fallback
     * @return the updated locale
     */
    @PostMapping("/{id}/setFallback")
    public ResponseEntity<?> setFallback(@PathVariable String id) {
        try {
            SupportedLocale newFallback = supportedLocaleService.get(id);
            if (newFallback == null) {
                return ResponseEntity.notFound().build();
            }

            // Clear existing fallback
            Optional<SupportedLocale> currentFallback = supportedLocaleService.getFallback();
            if (currentFallback.isPresent() && !currentFallback.get().getId().equals(id)) {
                SupportedLocale old = currentFallback.get();
                old.setFallback(false);
                old.setSysUserId(getSysUserId(request));
                supportedLocaleService.update(old);
            }

            // Set new fallback
            newFallback.setFallback(true);
            newFallback.setSysUserId(getSysUserId(request));
            supportedLocaleService.update(newFallback);

            LogEvent.logInfo(this.getClass().getSimpleName(), "setFallback",
                    "Set fallback locale to: " + newFallback.getLocaleCode());

            return ResponseEntity.ok(toDTO(newFallback));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to set fallback: " + e.getMessage()));
        }
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

    private SupportedLocaleDTO toDTO(SupportedLocale locale) {
        SupportedLocaleDTO dto = new SupportedLocaleDTO();
        dto.setId(locale.getId());
        dto.setLocaleCode(locale.getLocaleCode());
        dto.setDisplayName(locale.getDisplayName());
        dto.setActive(locale.isActive());
        dto.setFallback(locale.isFallback());
        dto.setSortOrder(locale.getSortOrder());
        return dto;
    }

    private SupportedLocale fromDTO(SupportedLocaleDTO dto) {
        SupportedLocale locale = new SupportedLocale();
        locale.setId(dto.getId());
        locale.setLocaleCode(dto.getLocaleCode());
        locale.setDisplayName(dto.getDisplayName());
        locale.setActive(dto.isActive());
        locale.setFallback(dto.isFallback());
        locale.setSortOrder(dto.getSortOrder());
        return locale;
    }

    /**
     * DTO for SupportedLocale data transfer.
     */
    public static class SupportedLocaleDTO {
        private String id;
        private String localeCode;
        private String displayName;
        private boolean active;
        private boolean fallback;
        private int sortOrder;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isFallback() {
            return fallback;
        }

        public void setFallback(boolean fallback) {
            this.fallback = fallback;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * Simple error response DTO.
     */
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
