package org.openelisglobal.testcalculated.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.testcalculated.service.ResultCalculationService;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class CalculatedValueRestController {

    @Autowired
    TypeOfSampleService typeOfSampleService;

    @Autowired
    TestCalculationService testCalculationService;

    @Autowired
    DictionaryService dictionaryService;

    @Autowired
    PatientService patientService;

    @Autowired
    ResultService resultService;

    @Autowired
    ResultCalculationService resultCalculationService;

    @PostMapping(value = "test-calculation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void saveReflexRule(HttpServletRequest request, @RequestBody Calculation calculation) {
        if (calculation.getId() != null) {
            if (testCalculationService.get(calculation.getId()) != null) {
                testCalculationService.update(calculation);
            }
        } else {
            testCalculationService.save(calculation);
        }
    }

    @PostMapping(value = "deactivate-test-calculation/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void deactivateReflexRule(@PathVariable Integer id) {
        try {
            Calculation calculation = testCalculationService.get(id);
            calculation.setActive(false);
            testCalculationService.update(calculation);

        } catch (Exception e) {

        }

    }

    @GetMapping(value = "test-calculations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Calculation> getReflexRules(HttpServletRequest request) {
        List<Calculation> calculations = testCalculationService.getAll().stream().collect(Collectors.toList());
        calculations.forEach(c -> c.setToggled(false));
        return !calculations.isEmpty() ? calculations : Collections.<Calculation>emptyList();
    }

    @GetMapping(value = "math-functions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getMathFunctions() {
        return Operation.mathFunctions();
    }
}
