package org.openelisglobal.sample.controller.rest;

import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Coded options for the clinical collect step.
 *
 * Clinical collection previously captured only free text, while the
 * environmental lane had dictionary-backed methods; these endpoints give the
 * clinical lane the same coded vocabulary. Entries are seeded from
 * configuration/backend/dictionaries/clinical-collection-dictionaries.csv, so a
 * deployment can replace them without a code change.
 */
@RestController
@RequestMapping("/rest/clinical/dictionary")
public class ClinicalCollectionDictionaryRestController {

    public static final String COLLECTION_METHOD_CATEGORY = "Clinical Collection Method";
    public static final String SPECIMEN_ORIGIN_CATEGORY = "Specimen Origin";

    @Autowired
    private DictionaryService dictionaryService;

    @GetMapping(value = "/collection-methods", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getCollectionMethods() {
        return entriesFor(COLLECTION_METHOD_CATEGORY);
    }

    @GetMapping(value = "/specimen-origins", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getSpecimenOrigins() {
        return entriesFor(SPECIMEN_ORIGIN_CATEGORY);
    }

    private ResponseEntity<List<Dictionary>> entriesFor(String categoryName) {
        try {
            return ResponseEntity.ok(
                    dictionaryService.getDictionaryEntrysByCategoryAbbreviation("categoryName", categoryName, true));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
