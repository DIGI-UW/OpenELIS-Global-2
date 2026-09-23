package org.openelisglobal.vector.controller.rest;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.security.SystemContext;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.vector.service.VectorSpeciesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dictionary lookups backing the environmental and vector order-entry dropdowns
 * (sampling site types, weather, collection methods, containers, pathogens,
 * lifecycle stages).
 *
 * <p>
 * Every read here goes through {@link DictionaryService} or
 * {@link DictionaryCategoryService}, both gated on PRIV_DICTIONARY_VIEW — an
 * administrative privilege that no order-entry role holds. That made the whole
 * environmental entry form unusable for Reception: each lookup threw
 * AccessDenied, the broad {@code catch (Exception)} in each handler turned it
 * into a 500, and the form rendered with empty dropdowns and no indication why.
 *
 * <p>
 * These are reference-data lists for dropdowns, not privileged records, so the
 * reads run in system context — the same treatment as DisplayListService's
 * caches. The caller's own access is still gated at the endpoints that create
 * or modify orders; this only assembles the options the form displays.
 */
@RestController
@RequestMapping("/rest/vector/dictionary")
public class VectorDictionaryRestController {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private VectorSpeciesService vectorSpeciesService;

    @GetMapping(value = "/pathogens", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getPathogens() {
        try {
            List<Dictionary> entries = SystemContext.callAsSystem(() -> dictionaryService
                    .getDictionaryEntrysByCategoryAbbreviation("categoryName", "vecPathogens", true));
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lifecycle-stages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getLifecycleStages(@RequestParam(required = false) String sampleTypeId) {
        try {
            if (sampleTypeId != null && !sampleTypeId.isEmpty()) {
                List<Dictionary> stages = SystemContext
                        .callAsSystem(() -> vectorSpeciesService.getLifecycleStagesBySampleTypeId(sampleTypeId));
                stages.sort(Comparator.comparing(Dictionary::getDictEntry,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                return ResponseEntity.ok(stages);
            }
            List<Dictionary> entries = SystemContext.callAsSystem(() -> dictionaryService
                    .getDictionaryEntrysByCategoryAbbreviation("categoryName", "vecLifecycleStages", true));
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sampling-site-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getSamplingSiteTypes() {
        try {
            List<Dictionary> entries = SystemContext.callAsSystem(() -> dictionaryService
                    .getDictionaryEntrysByCategoryAbbreviation("categoryName", "Sampling Site Type", true));
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/environmental-zones", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getEnvironmentalZones() {
        try {
            List<Dictionary> entries = SystemContext.callAsSystem(() -> dictionaryService
                    .getDictionaryEntrysByCategoryAbbreviation("categoryName", "Environmental Zone", true));
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sample-containers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getSampleContainers() {
        try {
            List<Dictionary> entries = SystemContext.callAsSystem(() -> dictionaryService
                    .getDictionaryEntrysByCategoryAbbreviation("categoryName", "Sample Container", true));
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/env-collection-methods", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getEnvCollectionMethods() {
        try {
            List<Dictionary> entries = SystemContext.callAsSystem(() -> dictionaryService
                    .getDictionaryEntrysByCategoryAbbreviation("categoryName", "Env Collection Method", true));
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/env-weather", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getEnvWeather() {
        try {
            List<Dictionary> entries = SystemContext.callAsSystem(() -> dictionaryService
                    .getDictionaryEntrysByCategoryAbbreviation("categoryName", "Env Weather", true));
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/pathogen-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DictionaryCategory>> getPathogenCategories() {
        try {
            List<DictionaryCategory> all = SystemContext.callAsSystem(dictionaryCategoryService::getAll);
            List<DictionaryCategory> filtered = all.stream()
                    .filter(c -> c.getCategoryName() != null && c.getCategoryName().endsWith("Pathogens"))
                    .sorted(Comparator.comparing(DictionaryCategory::getCategoryName)).collect(Collectors.toList());
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lifecycle-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DictionaryCategory>> getLifecycleCategories() {
        try {
            List<DictionaryCategory> all = SystemContext.callAsSystem(dictionaryCategoryService::getAll);
            List<DictionaryCategory> filtered = all.stream().filter(c -> {
                String name = c.getCategoryName();
                return name != null && (name.endsWith("Cycle") || name.endsWith("Stages") || name.endsWith("FullCycle")
                        || name.endsWith("NoCycle"));
            }).sorted(Comparator.comparing(DictionaryCategory::getCategoryName)).collect(Collectors.toList());
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
