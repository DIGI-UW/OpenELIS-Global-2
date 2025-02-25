package org.openelisglobal.qaevent.controller.rest;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest")
public class NonConformingEventsCorrectionActionRestController extends BaseRestController {

    private NCEventService ncEventService = SpringContext.getBean(NCEventService.class);

    @Autowired
    private NonConformingEventWorker nonConformingEventWorker;

    @GetMapping(value = "/nonconformingcorrectiveaction")
    public ResponseEntity<?> getNCECorrectionActions(@RequestParam(required = false) String labNumber,
            @RequestParam(required = false) String nceNumber, @RequestParam(required = false) String status) {
        NonConformingEventForm nceForm = new NonConformingEventForm();

        Map<String, Object> searchParameters = new HashMap<>();
        List<NcEvent> searchResults = new ArrayList<>();

        searchParameters.put("status", status);

        if (!"".equalsIgnoreCase(labNumber)) {
            searchParameters.put("labOrderNumber", labNumber);
        } else if (!"".equalsIgnoreCase(nceNumber)) {
            searchParameters.put("nceNumber", nceNumber);
        }

        searchResults = ncEventService.getAllMatching(searchParameters);

        System.out.println("search Results Size" + searchResults.size());

        nceForm.setnceEventsSearchResults(searchResults);
        nceForm.setReportingUnits(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE));

        return ResponseEntity.ok().body(nceForm);
    }

    @GetMapping(value = "/NCECorrectiveAction")
    public ResponseEntity<?> getNCECorrectiveActionForm(@RequestParam(required = true) String nceNumber,
            HttpServletRequest request)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NonConformingEventForm form = new NonConformingEventForm();

        form.setCurrentUserId(getSysUserId(request));

        form.setPatientSearch(new PatientSearch());

        if (!GenericValidator.isBlankOrNull(nceNumber)) {
            nonConformingEventWorker.initFormForCorrectiveAction(nceNumber, form);
        }

        return ResponseEntity.ok().body(form);
    }

    @PostMapping(value = "/NCECorrectiveAction")
    public ResponseEntity<?> updateNCECorretiveActionForm(@RequestBody NonConformingEventForm form) {

        boolean updated = nonConformingEventWorker.updateCorrectiveAction(form);

        if (updated) {
            return ResponseEntity.ok().body(Map.of("success", true));
        } else {
            return ResponseEntity.ok().body(Map.of("success", false));
        }
    }
}
