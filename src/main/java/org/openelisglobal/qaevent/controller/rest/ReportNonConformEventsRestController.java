package org.openelisglobal.qaevent.controller.rest;

import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.bean.NceSampleInfo;
import org.openelisglobal.common.rest.bean.NceSampleItemInfo;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportNonConformEventsRestController extends BaseRestController {

    private final SampleService sampleService;
    private final SampleItemService sampleItemService;
    private final SearchResultsService searchResultsService;
    private final NonConformingEventWorker nonConformingEventWorker;
    private final NceCategoryService nceCategoryService;
    private final RequesterService requesterService;

    @Autowired
    private SystemUserService systemUserService;

    public ReportNonConformEventsRestController(SampleService sampleService, SampleItemService sampleItemService,
            SearchResultsService searchResultsService, NonConformingEventWorker nonConformingEventWorker,
            NceCategoryService nceCategoryService, RequesterService requesterService) {
        this.sampleService = sampleService;
        this.sampleItemService = sampleItemService;
        this.searchResultsService = searchResultsService;
        this.nonConformingEventWorker = nonConformingEventWorker;
        this.nceCategoryService = nceCategoryService;
        this.requesterService = requesterService;
    }

    @GetMapping(value = "/rest/nonconformevents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNCESampleSearch(@RequestParam(required = false) String lastName,
            @RequestParam(required = false) String firstName, @RequestParam(required = false) String STNumber,
            @RequestParam(required = false) String labNumber) {
        try {
            List<Sample> searchResults;
            if (labNumber != null) {
                Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
                searchResults = sample != null ? List.of(sample) : List.of();
            } else {
                List<PatientSearchResults> results = searchResultsService.getSearchResults(lastName, firstName,
                        STNumber, STNumber, STNumber, "", "", "", "", "");
                searchResults = results.stream()
                        .flatMap(patientSearchResults -> sampleService
                                .getSamplesForPatient(patientSearchResults.getPatientID()).stream())
                        .collect(Collectors.toList());
            }

            if (searchResults.isEmpty()) {
                return ResponseEntity.ok().body(new ArrayList<>());
            } else {
                List<NceSampleInfo> temp = new ArrayList<>();
                for (Sample sample : searchResults) {
                    temp.add(addSample(sample));
                }
                return ResponseEntity.ok().body(temp);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the request.");
        }
    }

    @GetMapping(value = "/rest/reportnonconformingevent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getReportNonConformingEvent(@RequestParam Map<String, String> params,
            HttpServletRequest request) {
        try {
            NonConformingEventForm eventData = new NonConformingEventForm();
            eventData.setLabOrderNumber(params.get("labOrderNumber"));
            eventData.setSpecimenId(params.get("specimenId"));
            eventData.setCurrentUserId(params.get("currentUserId"));
            eventData.setNceCategories(nceCategoryService.getAllNceCategories());

            SystemUser systemUser = systemUserService.getUserById(getSysUserId(request));
            eventData.setName(systemUser.getFirstName() + " " + systemUser.getLastName());

            String ncenumber = String.valueOf(System.currentTimeMillis());
            NcEvent event = nonConformingEventWorker.create(params.get("labOrderNumber"),
                    Arrays.asList(params.get("specimenId").split(",")), systemUser.getId(), ncenumber);

            eventData.setNceNumber(event.getNceNumber());
            eventData.setId(event.getId());

            Sample sample = getSampleForLabNumber(params.get("labOrderNumber"));
            if (sample != null) {
                List<SampleItem> sampleItems = new ArrayList<>();
                String[] sampleItemIdArray = params.get("specimenId").split(",");
                for (String s : sampleItemIdArray) {
                    SampleItem si = sampleItemService.getData(s);
                    sampleItems.add(si);
                }
                eventData.setSpecimens(sampleItems);
            }

            eventData.setCurrentUserId(getSysUserId(request));

            eventData.setReportingUnits(
                    DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE));

            requesterService.setSampleId(sample == null ? null : sample.getId());
            eventData.setSite(requesterService.getReferringSiteName());
            eventData.setPrescriberName(requesterService.getRequesterLastFirstName());
            eventData.setNceCategories(nceCategoryService.getAllNceCategories());
            eventData.setReportDate(DateUtil.formatDateAsText(Calendar.getInstance().getTime()));

            return ResponseEntity.ok(eventData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the request.");
        }
    }

    @PostMapping(value = "/rest/reportnonconformingevent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postReportNonConformingEvent(@RequestBody NonConformingEventForm form) {
        try {
            boolean updated = nonConformingEventWorker.update(form);
            if (updated) {
                return ResponseEntity.ok().body(Map.of("success", true));
            } else {
                return ResponseEntity.ok().body(Map.of("success", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the request.");
        }
    }

    private NceSampleInfo addSample(Sample sample) {
        NceSampleInfo sampleInfo = new NceSampleInfo();
        sampleInfo.setId(sample.getId());
        sampleInfo.setLabOrderNumber(sample.getAccessionNumber());

        List<NceSampleItemInfo> sampleItemsList = new ArrayList<>();
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());

        for (SampleItem sampleItem : sampleItems) {
            NceSampleItemInfo sampleItemInfo = new NceSampleItemInfo();
            sampleItemInfo.setId(sampleItem.getId());
            sampleItemInfo.setNumber(sampleItem.getSortOrder());
            sampleItemInfo.setType(sampleItem.getTypeOfSample().getDescription());
            sampleItemsList.add(sampleItemInfo);
        }

        sampleInfo.setSampleItems(sampleItemsList);
        return sampleInfo;
    }

    private Sample getSampleForLabNumber(String labNumber) throws LIMSInvalidConfigurationException {
        return sampleService.getSampleByAccessionNumber(labNumber);
    }
}
