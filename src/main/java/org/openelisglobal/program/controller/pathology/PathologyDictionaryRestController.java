package org.openelisglobal.program.controller.pathology;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.service.PathologyConclusionSearchService;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntries;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The dictionary reads the pathology case view makes that no list it already
 * holds can answer: the diagnosis search over the whole dictionary.
 */
@RestController
@RequestMapping(value = "/rest/pathology", produces = MediaType.APPLICATION_JSON_VALUE)
public class PathologyDictionaryRestController extends BaseRestController {

    private final PathologyConclusionSearchService conclusionSearchService;

    public PathologyDictionaryRestController(PathologyConclusionSearchService conclusionSearchService) {
        this.conclusionSearchService = conclusionSearchService;
    }

    @GetMapping("/conclusionEntries")
    public ConclusionEntries conclusionEntries(@RequestParam(name = "q", required = false) String q) {
        return conclusionSearchService.search(q);
    }
}
