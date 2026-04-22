package org.openelisglobal.vector.controller.rest;

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

@RestController
@RequestMapping("/rest/vector/dictionary")
public class VectorDictionaryRestController {

    @Autowired
    private DictionaryService dictionaryService;

    @GetMapping(value = "/pathogens", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getPathogens() {
        try {
            List<Dictionary> entries = dictionaryService.getDictionaryEntrysByCategoryAbbreviation(
                    "categoryName", "vecPathogens", true);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lifecycle-stages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Dictionary>> getLifecycleStages() {
        try {
            List<Dictionary> entries = dictionaryService.getDictionaryEntrysByCategoryAbbreviation(
                    "categoryName", "vecLifecycleStages", true);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
