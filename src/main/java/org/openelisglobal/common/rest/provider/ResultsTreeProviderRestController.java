package org.openelisglobal.common.rest.provider;

import java.util.List;
import org.openelisglobal.common.rest.provider.bean.patientHistory.PanelDisplay;
import org.openelisglobal.common.rest.provider.bean.patientHistory.ResultTree;
import org.openelisglobal.common.services.PatientResultTreeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class ResultsTreeProviderRestController {

    @Autowired
    private PatientResultTreeService patientResultTreeService;

    @GetMapping(value = "result-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ResultTree> getResultTreeArray(@RequestParam String patientId) {
        return patientResultTreeService.getResultTree(patientId);
    }

    @GetMapping(value = "test-result-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PanelDisplay getTestResultTree(@RequestParam String patientId, @RequestParam String testId,
            @RequestParam(required = false) String componentId, @RequestParam(required = false) String sampleTypeId) {
        return patientResultTreeService.getTestResultTree(patientId, testId, componentId, sampleTypeId);
    }
}
