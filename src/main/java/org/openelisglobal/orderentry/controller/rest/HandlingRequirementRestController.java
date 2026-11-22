package org.openelisglobal.orderentry.controller.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.orderentry.service.HandlingRequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Required line of a sample's Handling group (FRS clinical order entry v4,
 * FR-C9a): the test catalog's storage condition and holding time for the tests
 * on the sample.
 */
@RestController
@RequestMapping("/rest/sample-handling")
public class HandlingRequirementRestController extends BaseRestController {

    @Autowired
    private HandlingRequirementService handlingRequirementService;

    @GetMapping(value = "/requirements", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> requirements(@RequestParam(required = false, defaultValue = "") String testIds) {
        return handlingRequirementService.requirements(Arrays.asList(testIds.split(",")));
    }
}
