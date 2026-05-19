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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final Logger logger = LoggerFactory.getLogger(CalculatedValueRestController.class);

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
    public ResponseEntity<Void> deactivateReflexRule(@PathVariable Integer id) {
        return setReflexRuleActive(id, false);
    }

    /**
     * OGC-655: counterpart to deactivate. Flips the Active flag back to true so the
     * UI Toggle Rule control has a round-trip persistence path.
     */
    @PostMapping(value = "activate-test-calculation/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> activateReflexRule(@PathVariable Integer id) {
        return setReflexRuleActive(id, true);
    }

    private ResponseEntity<Void> setReflexRuleActive(Integer id, boolean active) {
        try {
            Calculation calculation = testCalculationService.get(id);
            if (calculation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            calculation.setActive(active);
            testCalculationService.update(calculation);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // Replaces a silent empty catch that swallowed every error (OGC-655) —
            // the FE used to fire-and-forget without status feedback.
            logger.error("Failed to set Active={} on calculation {}", active, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "test-calculations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Calculation> getReflexRules(HttpServletRequest request) {
        // OGC-655: previously forced toggled=false on every load, which made
        // an active rule's body collapse on reload even though active=true. The
        // Toggle Rule control is a UI-collapse affordance; seed it from the
        // persisted active state so reload reflects what was saved.
        List<Calculation> calculations = testCalculationService.getAll().stream().collect(Collectors.toList());
        calculations.forEach(c -> c.setToggled(Boolean.TRUE.equals(c.getActive())));
        return !calculations.isEmpty() ? calculations : Collections.<Calculation>emptyList();
    }

    @GetMapping(value = "math-functions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<IdValuePair> getMathFunctions() {
        return Operation.mathFunctions();
    }
}
