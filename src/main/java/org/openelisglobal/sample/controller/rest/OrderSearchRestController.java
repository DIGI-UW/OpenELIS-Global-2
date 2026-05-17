/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.sample.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.provider.bean.PatientInfoBean;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.qachecklist.service.SampleQaChecklistService;
import org.openelisglobal.qc.dao.SampleItemQcProfileDAO;
import org.openelisglobal.sample.service.SampleComplianceStandardService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.vector.service.VectorPoolService;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Order Search and Dashboard in the decoupled sample
 * collection workflow.
 *
 * <p>
 * Provides endpoints for: - Barcode scanner bar (NAV-6) to load orders by lab
 * number - Order dashboard (DSH-1 to DSH-9) to list in-progress orders
 *
 * @see Sample
 * @see SampleService
 */
@RestController
@RequestMapping("/rest/order")
public class OrderSearchRestController extends BaseRestController {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private AddressPartService addressPartService;

    @Autowired
    private PersonAddressService personAddressService;

    @Autowired
    private PatientPatientTypeService patientPatientTypeService;

    @Autowired
    private PatientContactService patientContactService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private ProgramSampleService programSampleService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private PersonService personService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SampleQaChecklistService sampleQaChecklistService;

    @Autowired
    private SampleItemQcProfileDAO sampleItemQcProfileDAO;

    @Autowired
    private SampleComplianceStandardService sampleComplianceStandardService;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private String ADDRESS_PART_VILLAGE_ID;
    private String ADDRESS_PART_COMMUNE_ID;
    private String ADDRESS_PART_DEPT_ID;

    /**
     * Get orders for the dashboard (DSH-1 to DSH-9).
     *
     * <p>
     * Returns paginated list of orders with filtering support.
     *
     * @param page            page number (1-based)
     * @param pageSize        items per page (25, 50, or 100)
     * @param search          search query for patient name, lab number, etc.
     * @param status          filter by order status
     * @param priority        filter by priority
     * @param includeExternal include external/EMR orders
     * @return Dashboard data with orders list and counts
     */
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize, @RequestParam(required = false) String search,
            @RequestParam(required = false) String status, @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "false") boolean includeExternal,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String workflowType) {

        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> ordersList = new ArrayList<>();

            // Get recent samples - getPageOfSamples expects 1-based startingRecNo
            int startingRecNo = ((page - 1) * pageSize) + 1;
            List<Sample> samples = sampleService.getPageOfSamples(startingRecNo);

            // Apply filters
            for (Sample sample : samples) {
                // Filter by search query (lab number or patient name)
                if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    boolean matchesLabNumber = sample.getAccessionNumber() != null
                            && sample.getAccessionNumber().toLowerCase().contains(searchLower);
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    boolean matchesPatient = false;
                    if (patient != null) {
                        String patientName = (patientService.getFirstName(patient) + " "
                                + patientService.getLastName(patient)).toLowerCase();
                        matchesPatient = patientName.contains(searchLower);
                    }
                    if (!matchesLabNumber && !matchesPatient) {
                        continue; // Skip this sample
                    }
                }

                // Filter by priority
                String samplePriority = sample.getPriority() != null ? sample.getPriority().name().toLowerCase()
                        : "routine";
                if (priority != null && !priority.isEmpty() && !"all".equals(priority)) {
                    if (!samplePriority.equals(priority.toLowerCase())) {
                        continue; // Skip this sample
                    }
                }

                // Filter by date range (using entered date)
                java.sql.Date sampleDate = sample.getEnteredDate();
                if (startDate != null && !startDate.isEmpty()) {
                    try {
                        java.sql.Date filterStartDate = java.sql.Date.valueOf(startDate);
                        if (sampleDate == null || sampleDate.before(filterStartDate)) {
                            continue; // Skip this sample
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid date format, skip filter
                    }
                }
                if (endDate != null && !endDate.isEmpty()) {
                    try {
                        java.sql.Date filterEndDate = java.sql.Date.valueOf(endDate);
                        if (sampleDate == null || sampleDate.after(filterEndDate)) {
                            continue; // Skip this sample
                        }
                    } catch (IllegalArgumentException e) {
                        // Invalid date format, skip filter
                    }
                }

                // Calculate step progress for status filtering
                List<SampleItem> sampleItemsForProgress = sampleItemService.getSampleItemsBySampleId(sample.getId());

                // Collect is complete if all sample items with tests have collection dates
                boolean collectComplete = false;
                if (!sampleItemsForProgress.isEmpty()) {
                    List<SampleItem> itemsWithTests = sampleItemsForProgress.stream()
                            .filter(si -> !analysisService.getAnalysesBySampleItem(si).isEmpty())
                            .collect(java.util.stream.Collectors.toList());
                    if (!itemsWithTests.isEmpty()) {
                        collectComplete = itemsWithTests.stream().allMatch(si -> si.getCollectionDate() != null);
                    }
                }

                // Label is complete if all sample items have storage assignments OR storage is
                // skipped
                boolean labelComplete = false;
                if (Boolean.TRUE.equals(sample.getStorageSkipped())) {
                    labelComplete = true;
                } else if (!sampleItemsForProgress.isEmpty()) {
                    labelComplete = sampleItemsForProgress.stream().allMatch(si -> {
                        SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findBySampleItemId(si.getId());
                        return assignment != null && assignment.getLocationId() != null;
                    });
                }

                // QA is complete if all checklist items are verified
                boolean qaComplete = sampleQaChecklistService.areAllItemsVerified(Integer.parseInt(sample.getId()));

                // Determine order status
                String orderStatus;
                if (qaComplete) {
                    orderStatus = "completed";
                } else if (labelComplete) {
                    orderStatus = "pending_qa";
                } else {
                    orderStatus = "in_progress";
                }

                // Filter by status
                if (status != null && !status.isEmpty() && !"all".equals(status)) {
                    if (!orderStatus.equals(status)) {
                        continue; // Skip this sample
                    }
                }

                Map<String, Object> orderData = new HashMap<>();
                orderData.put("id", sample.getId());
                orderData.put("labNumber", sample.getAccessionNumber());
                orderData.put("lastUpdated", sample.getLastupdated() != null ? sample.getLastupdated().toString() : "");
                orderData.put("priority", samplePriority);
                orderData.put("isExternal", false);
                orderData.put("returnedFromQA", false);

                // Determine workflow type early (needed for patient vs site column logic)
                String earlyWorkflowType = observationHistoryService
                        .getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE, sample.getId());
                boolean isEnvOrVector = "environmental".equalsIgnoreCase(earlyWorkflowType)
                        || "vector".equalsIgnoreCase(earlyWorkflowType);

                if (isEnvOrVector) {
                    // Environmental orders use VS_COLLECTION_SITE_NAME (VectorSection shared
                    // component stores site as vecCollectionSiteName). Vector orders do the same.
                    // Fall back to ENV_SAMPLING_SITE_NAME for older records.
                    String siteName = observationHistoryService
                            .getRawValueForSample(ObservationType.VS_COLLECTION_SITE_NAME, sample.getId());
                    if (siteName == null) {
                        siteName = observationHistoryService
                                .getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_NAME, sample.getId());
                    }
                    orderData.put("samplingSiteName", siteName != null ? siteName : "---");
                    orderData.put("patientName", null);
                } else {
                    // Clinical orders show patient name
                    Patient orderPatient = sampleHumanService.getPatientForSample(sample);
                    if (orderPatient != null) {
                        String patientName = (patientService.getFirstName(orderPatient) + " "
                                + patientService.getLastName(orderPatient)).trim();
                        orderData.put("patientName", patientName);
                    } else {
                        orderData.put("patientName", "---");
                    }
                }

                // Facility: referring organisation (all workflow types)
                String facilityName = "";
                RequesterService requesterService = new RequesterService(sample.getId());
                Organization referringOrg = requesterService.getOrganization();
                if (referringOrg == null)
                    referringOrg = requesterService.getOrganizationDepartment();
                if (referringOrg != null)
                    facilityName = referringOrg.getOrganizationName();
                orderData.put("facilityName", facilityName.isEmpty() ? "---" : facilityName);

                // Step progress - reuse values calculated for status filtering
                Map<String, Boolean> stepProgress = new HashMap<>();
                stepProgress.put("enter", isEnterComplete(sample));
                stepProgress.put("collect", collectComplete);
                stepProgress.put("label", labelComplete);
                stepProgress.put("qa", qaComplete);
                orderData.put("stepProgress", stepProgress);
                orderData.put("status", orderStatus);
                orderData.put("storageSkipped", Boolean.TRUE.equals(sample.getStorageSkipped()));

                // Filter by workflow context.
                // Clinical orders may store "clinical" explicitly (new) or null (legacy
                // pre-split).
                if (workflowType != null && !workflowType.isEmpty()) {
                    if ("clinical".equalsIgnoreCase(workflowType)) {
                        if (earlyWorkflowType != null && !"clinical".equalsIgnoreCase(earlyWorkflowType))
                            continue;
                    } else {
                        if (!workflowType.equalsIgnoreCase(earlyWorkflowType))
                            continue;
                    }
                }

                if (earlyWorkflowType != null) {
                    orderData.put("workflowType", earlyWorkflowType);
                }

                ordersList.add(orderData);
            }

            response.put("orders", ordersList);
            response.put("totalCount", ordersList.size()); // Simplified, should be total count
            response.put("externalCount", 0); // Placeholder for external orders count
            response.put("page", page);
            response.put("pageSize", pageSize);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getDashboard", "Error fetching dashboard: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search for an order by lab number (accession number).
     *
     * <p>
     * Returns order data including patient information and samples for display in
     * the order workflow steps. Used by the barcode scanner bar (NAV-6).
     *
     * <p>
     * Example: GET /rest/order/search?labNumber=20231201-001
     *
     * @param labNumber the lab/accession number to search for (required)
     * @return Order data with 200 OK, or 404 if not found
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchOrder(@RequestParam(required = false) String labNumber) {

        if (labNumber == null || labNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Find the sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNumber.trim());

            if (sample == null) {
                return ResponseEntity.notFound().build();
            }

            // Build response with order data
            Map<String, Object> response = new HashMap<>();
            response.put("id", sample.getId());
            response.put("labNumber", sample.getAccessionNumber());
            response.put("receivedDate", sample.getReceivedDateForDisplay());
            response.put("collectionDate", sample.getCollectionDateForDisplay());
            response.put("status", sample.getStatus());

            // Get patient information - using PatientInfoBean for consistency with
            // /rest/patient-details
            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient != null) {
                PatientInfoBean patientData = buildPatientProperties(patient);
                response.put("patientProperties", patientData);

                // Also include orderData structure for frontend compatibility
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("patientProperties", patientData);
                orderData.put("patientUpdateStatus", "UPDATE");
                response.put("orderData", orderData);
            }

            // Get sample items
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            List<Map<String, Object>> samplesData = new ArrayList<>();

            // Map sample_item.id -> index in samplesData so we can resolve
            // SampleItemQcProfile.parentSampleItemId back to the frontend's array index.
            Map<Integer, Integer> sampleItemIdToIndex = new HashMap<>();
            for (int i = 0; i < sampleItems.size(); i++) {
                String itemId = sampleItems.get(i).getId();
                if (itemId != null) {
                    sampleItemIdToIndex.put(Integer.valueOf(itemId), i);
                }
            }

            // Vector orders: analyses are anchored to vector_pool_id (not sampitem_id)
            // after fan-out. Build a per-organism index so each sample_item picks up
            // only its OWN pool's analyses; unioning across all pools on the sample
            // would bleed (e.g.) Mosquito tests onto Flea organism rows.
            // Also build a memberId→poolId map and a poolId→size map so the
            // frontend can group rows by stable pool identifier (not sampleTypeId,
            // which would merge two pools of the same animal into one).
            Map<String, List<Analysis>> analysesByMemberId = new HashMap<>();
            Map<String, String> poolIdByMemberId = new HashMap<>();
            Map<String, Integer> poolSizeById = new HashMap<>();
            if ("V".equals(sample.getDomain())) {
                for (VectorPool pool : vectorPoolService.getBySampleId(sample.getId())) {
                    String poolId = String.valueOf(pool.getId());
                    List<Analysis> poolAnalyses = analysisService.getAnalysesByVectorPoolId(poolId);
                    List<SampleItem> members = vectorPoolService.getMembersByPoolId(pool.getId());
                    poolSizeById.put(poolId, members.size());
                    // Always store an entry per member (even an empty list) so the
                    // per-item fallback `getAnalysesBySampleItem(sampleItem)` query
                    // is skipped for pools that legitimately have no analyses yet.
                    for (SampleItem member : members) {
                        poolIdByMemberId.put(member.getId(), poolId);
                        analysesByMemberId.put(member.getId(), poolAnalyses);
                    }
                }
            }

            for (SampleItem sampleItem : sampleItems) {
                Map<String, Object> sampleItemData = new HashMap<>();
                sampleItemData.put("id", sampleItem.getId());
                sampleItemData.put("sampleItemId", sampleItem.getId()); // For frontend to use in updates
                sampleItemData.put("sortOrder", sampleItem.getSortOrder());
                sampleItemData.put("sampleTypeId", sampleItem.getTypeOfSampleId());
                // Vector pool fan-out parents (sample_item rows with quantity=N) are
                // hard-deleted by VectorPoolFanOutServiceImpl and never reach this
                // response. The voided flag is still exposed for any non-vector
                // workflow that may soft-delete a sample_item; `sampleItemService
                // .getSampleItemsBySampleId(...)` already filters voided=false, so
                // this defaults to false in the current code path.
                sampleItemData.put("voided", sampleItem.isVoided());
                if (sampleItem.getVoidReason() != null) {
                    sampleItemData.put("voidReason", sampleItem.getVoidReason());
                }

                // Vector pool membership: expose the stable pool id + size so the
                // frontend can group organisms by pool. Two pools of the same
                // sampleTypeId must remain distinct rows.
                String memberPoolId = poolIdByMemberId.get(sampleItem.getId());
                if (memberPoolId != null) {
                    sampleItemData.put("vectorPoolId", memberPoolId);
                    sampleItemData.put("vectorPoolMemberCount", poolSizeById.getOrDefault(memberPoolId, 0));
                }

                // Get sample type name
                if (sampleItem.getTypeOfSampleId() != null) {
                    var typeOfSample = typeOfSampleService.get(sampleItem.getTypeOfSampleId());
                    if (typeOfSample != null) {
                        sampleItemData.put("name", typeOfSample.getLocalizedName());
                        sampleItemData.put("sampleTypeName", typeOfSample.getDescription());
                    }
                }

                String collectionDateDisplay = "";
                String collectionTimeDisplay = "";
                if (sampleItem.getCollectionDate() != null) {
                    collectionDateDisplay = DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate());
                    collectionTimeDisplay = DateUtil.convertTimestampToStringTime(sampleItem.getCollectionDate());
                }

                sampleItemData.put("collectionDate", collectionDateDisplay);
                sampleItemData.put("collectionTime", collectionTimeDisplay);
                sampleItemData.put("quantity", sampleItem.getQuantity() != null ? sampleItem.getQuantity() : "");
                sampleItemData.put("quantityUnit",
                        sampleItem.getUnitOfMeasure() != null ? sampleItem.getUnitOfMeasure().getId() : "");
                sampleItemData.put("collectorId", sampleItem.getCollector() != null ? sampleItem.getCollector() : "");
                sampleItemData.put("collectionConditions",
                        sampleItem.getCollectionConditions() != null ? sampleItem.getCollectionConditions() : "");
                sampleItemData.put("collectionMethod",
                        sampleItem.getCollectionMethod() != null ? sampleItem.getCollectionMethod() : "");
                sampleItemData.put("sampleTemperature",
                        sampleItem.getSampleTemperature() != null ? sampleItem.getSampleTemperature() : "");
                sampleItemData.put("specimenOrigin",
                        sampleItem.getSpecimenOrigin() != null ? sampleItem.getSpecimenOrigin() : "");

                String receivedDateDisplay = "";
                String receivedTimeDisplay = "";
                if (sampleItem.getReceivedDate() != null) {
                    receivedDateDisplay = DateUtil.convertTimestampToStringDate(sampleItem.getReceivedDate());
                    receivedTimeDisplay = DateUtil.convertTimestampToStringTime(sampleItem.getReceivedDate());
                }
                sampleItemData.put("receivedDate", receivedDateDisplay);
                sampleItemData.put("receivedTime", receivedTimeDisplay);

                Map<String, Object> sampleXML = new HashMap<>();
                sampleXML.put("collectionDate", collectionDateDisplay);
                sampleXML.put("collectionTime", collectionTimeDisplay);
                sampleXML.put("quantity", sampleItem.getQuantity());
                sampleXML.put("uom",
                        sampleItem.getUnitOfMeasure() != null ? sampleItem.getUnitOfMeasure().getId() : "");
                sampleXML.put("collector", sampleItem.getCollector());
                sampleXML.put("collectionMethod",
                        sampleItem.getCollectionMethod() != null ? sampleItem.getCollectionMethod() : "");
                sampleXML.put("sampleTemperature",
                        sampleItem.getSampleTemperature() != null ? sampleItem.getSampleTemperature() : "");
                sampleXML.put("specimenOrigin",
                        sampleItem.getSpecimenOrigin() != null ? sampleItem.getSpecimenOrigin() : "");
                sampleXML.put("container", sampleItem.getContainer() != null ? sampleItem.getContainer() : "");
                sampleXML.put("locationDetails",
                        sampleItem.getLocationDetails() != null ? sampleItem.getLocationDetails() : "");
                sampleXML.put("gpsLatitude", sampleItem.getGpsLatitude() != null ? sampleItem.getGpsLatitude() : "");
                sampleXML.put("gpsLongitude", sampleItem.getGpsLongitude() != null ? sampleItem.getGpsLongitude() : "");
                sampleItemData.put("sampleXML", sampleXML);

                // Get tests from analysis records for this sample item. For vector
                // organism children (which have NO direct analysis link), use the
                // pre-built per-member index FIRST so each member skips the
                // empty-result getAnalysesBySampleItem query (N+1 across a large
                // pool). Fall back to the item-anchored query only when no pool
                // membership is found, which covers non-vector samples and
                // post-deconvolution item-anchored analyses.
                List<Analysis> analyses;
                List<Analysis> memberAnalyses = analysesByMemberId.get(sampleItem.getId());
                if (memberAnalyses != null) {
                    analyses = memberAnalyses;
                } else {
                    analyses = analysisService.getAnalysesBySampleItem(sampleItem);
                }
                List<Map<String, Object>> testsData = new ArrayList<>();
                List<Map<String, Object>> panelsData = new ArrayList<>();

                for (Analysis analysis : analyses) {
                    if (analysis.getTest() != null) {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("id", analysis.getTest().getId());
                        testData.put("name", analysis.getTest().getLocalizedName());
                        testData.put("description", analysis.getTest().getDescription());
                        testsData.add(testData);
                    }
                    // Try to get panel - may be null if test wasn't added via panel
                    try {
                        if (analysis.getPanel() != null) {
                            // Check if panel already added
                            boolean panelExists = panelsData.stream()
                                    .anyMatch(p -> p.get("id").equals(analysis.getPanel().getId()));
                            if (!panelExists) {
                                Map<String, Object> panelData = new HashMap<>();
                                panelData.put("id", analysis.getPanel().getId());
                                panelData.put("name", analysis.getPanel().getLocalizedName());
                                panelsData.add(panelData);
                            }
                        }
                    } catch (Exception e) {
                        // Panel retrieval failed - log at debug level only
                    }
                }

                sampleItemData.put("tests", testsData);
                sampleItemData.put("panels", panelsData);
                sampleItemData.put("index", sampleItem.getSortOrder());

                // Attach QC metadata if this sample item has a QC profile
                try {
                    String currentItemId = sampleItem.getId();
                    if (currentItemId != null) {
                        sampleItemQcProfileDAO.findBySampleItemId(Integer.valueOf(currentItemId)).ifPresent(profile -> {
                            Map<String, Object> qcMetadata = new HashMap<>();
                            qcMetadata.put("qcType", profile.getQcType());
                            Integer parentItemId = profile.getParentSampleItemId();
                            Integer parentIndex = parentItemId != null ? sampleItemIdToIndex.get(parentItemId) : null;
                            qcMetadata.put("parentSampleIndex", parentIndex);
                            qcMetadata.put("expectedValue",
                                    profile.getExpectedValue() != null ? profile.getExpectedValue().toPlainString()
                                            : null);
                            sampleItemData.put("qcMetadata", qcMetadata);
                        });
                    }
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "searchOrder",
                            "Failed to load QC profile for sample item " + sampleItem.getId() + ": " + e.getMessage());
                }

                // Get storage assignment for this sample item
                SampleStorageAssignment storageAssignment = sampleStorageAssignmentDAO
                        .findBySampleItemId(sampleItem.getId());
                if (storageAssignment != null && storageAssignment.getLocationId() != null) {
                    sampleItemData.put("storageLocationId", storageAssignment.getLocationId());
                    sampleItemData.put("storageLocationType", storageAssignment.getLocationType());
                    sampleItemData.put("storagePositionCoordinate", storageAssignment.getPositionCoordinate());
                    sampleItemData.put("storageNotes", storageAssignment.getNotes());

                    // Get hierarchical path via the service
                    Map<String, Object> locationInfo = sampleStorageService.getSampleItemLocation(sampleItem.getId());
                    if (locationInfo != null && locationInfo.get("hierarchicalPath") != null) {
                        sampleItemData.put("storageHierarchicalPath", locationInfo.get("hierarchicalPath"));
                    }
                }

                samplesData.add(sampleItemData);
            }
            response.put("samples", samplesData);

            // Build comprehensive sampleOrderItems with provider, site, and clinical info
            Map<String, Object> sampleOrderItems = buildSampleOrderItems(sample);
            response.put("sampleOrderItems", sampleOrderItems);

            // Step progress - determine based on actual data
            boolean isVectorOrder = "V".equals(sample.getDomain());
            Map<String, Boolean> stepProgress = new HashMap<>();
            stepProgress.put("enter", true); // If sample exists, enter is complete

            // Vector workflow has no collect step — samples are created at order entry.
            // For clinical workflow, collect is complete when sample items with tests
            // all have collection dates set.
            if (isVectorOrder) {
                stepProgress.put("collect", true);
            } else {
                boolean collectComplete = false;
                if (!sampleItems.isEmpty()) {
                    List<SampleItem> sampleItemsWithTests = sampleItems.stream()
                            .filter(si -> !analysisService.getAnalysesBySampleItem(si).isEmpty())
                            .collect(java.util.stream.Collectors.toList());
                    if (!sampleItemsWithTests.isEmpty()) {
                        collectComplete = sampleItemsWithTests.stream().allMatch(si -> si.getCollectionDate() != null);
                    }
                }
                stepProgress.put("collect", collectComplete);
            }

            // Vector workflow has no storage requirement — label step is always complete.
            // For clinical workflow, label is complete when all sample items have storage
            // assignments or storage is explicitly skipped.
            boolean labelComplete;
            if (isVectorOrder) {
                labelComplete = true;
            } else if (Boolean.TRUE.equals(sample.getStorageSkipped())) {
                labelComplete = true;
            } else if (!sampleItems.isEmpty()) {
                labelComplete = sampleItems.stream().allMatch(si -> {
                    SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findBySampleItemId(si.getId());
                    return assignment != null && assignment.getLocationId() != null;
                });
            } else {
                labelComplete = false;
            }
            stepProgress.put("label", labelComplete);

            // QA is complete if all checklist items are verified
            boolean qaComplete = sampleQaChecklistService.areAllItemsVerified(Integer.parseInt(sample.getId()));
            stepProgress.put("qa", qaComplete);
            response.put("stepProgress", stepProgress);

            // Include storageSkipped flag (always false for vector — storage not
            // applicable)
            response.put("storageSkipped", !isVectorOrder && Boolean.TRUE.equals(sample.getStorageSkipped()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchOrder", "Error searching for order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update the storageSkipped flag on a sample.
     *
     * <p>
     * Used when the user checks "No storage required" checkbox on the Label step.
     *
     * @param labNumber      the lab/accession number
     * @param storageSkipped true if storage is intentionally skipped
     * @return success/failure response
     */
    @org.springframework.web.bind.annotation.PutMapping(value = "/storage-skipped", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateStorageSkipped(@RequestParam String labNumber,
            @RequestParam boolean storageSkipped, HttpServletRequest request) {

        try {
            Sample sample = sampleService.getSampleByAccessionNumber(labNumber);
            if (sample == null) {
                return ResponseEntity.notFound().build();
            }

            sample.setStorageSkipped(storageSkipped);
            sample.setSysUserId(getSysUserId(request));
            sampleService.update(sample);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("labNumber", labNumber);
            response.put("storageSkipped", storageSkipped);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "updateStorageSkipped",
                    "Error updating storageSkipped: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Build patient properties using PatientInfoBean - matches the format returned
     * by /rest/patient-details endpoint. This ensures consistency with the existing
     * patient search functionality.
     */
    private PatientInfoBean buildPatientProperties(Patient patient) {
        initAddressPartIds();

        Person person = patient.getPerson();
        PatientIdentityTypeMap identityMap = PatientIdentityTypeMap.getInstance();
        List<PatientIdentity> identityList = PatientUtil.getIdentityListForPatient(patient.getId());
        List<PatientContact> patientContacts = patientContactService.getForPatient(patient.getId());

        String city = getAddress(person, ADDRESS_PART_VILLAGE_ID);
        if (GenericValidator.isBlankOrNull(city)) {
            city = person.getCity();
        }
        String commune = getAddress(person, ADDRESS_PART_COMMUNE_ID);
        String dept = getAddress(person, ADDRESS_PART_DEPT_ID);

        PatientInfoBean patientInfo = new PatientInfoBean();
        patientInfo.setPatientPK(patient.getId());
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);
        patientInfo.setNationalId(patient.getNationalId());
        patientInfo.setSTnumber(identityMap.getIdentityValue(identityList, "ST"));
        patientInfo.setSubjectNumber(identityMap.getIdentityValue(identityList, "SUBJECT"));
        patientInfo.setLastName(getLastNameForResponse(person));
        patientInfo.setFirstName(person.getFirstName());
        patientInfo.setMothersName(identityMap.getIdentityValue(identityList, "MOTHER"));
        patientInfo.setAka(identityMap.getIdentityValue(identityList, "AKA"));
        patientInfo.setStreetAddress(person.getStreetAddress());
        patientInfo.setCity(city);
        patientInfo.setPrimaryPhone(person.getPrimaryPhone());
        patientInfo.setEmail(person.getEmail());
        patientInfo.setGender(patient.getGender());
        patientInfo.setPatientType(getPatientType(patient));
        patientInfo.setInsuranceNumber(identityMap.getIdentityValue(identityList, "INSURANCE"));
        patientInfo.setOccupation(identityMap.getIdentityValue(identityList, "OCCUPATION"));
        patientInfo.setCustomNotes(identityMap.getIdentityValue(identityList, "CUSTOM_NOTES"));
        patientInfo.setTargetDiseaseProgramme(identityMap.getIdentityValue(identityList, "DISEASE_PROGRAMME"));

        String format1 = "dd/MM/yyyy";
        String format2 = "MM/dd/yyyy";
        String rawBirthDate = patient.getBirthDateForDisplay();
        if (rawBirthDate != null && !rawBirthDate.isBlank()) {
            patientInfo.setBirthDateForDisplay(
                    ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE).equals("fr-FR")
                            ? DateUtil.formatStringDate(rawBirthDate, format1)
                            : DateUtil.formatStringDate(rawBirthDate, format2));
        }

        patientInfo.setCommune(commune);
        patientInfo.setAddressDepartment(dept);
        patientInfo.setMothersInitial(identityMap.getIdentityValue(identityList, "MOTHERS_INITIAL"));
        patientInfo.setEducation(identityMap.getIdentityValue(identityList, "EDUCATION"));
        patientInfo.setMaritialStatus(identityMap.getIdentityValue(identityList, "MARITIAL"));
        patientInfo.setNationality(identityMap.getIdentityValue(identityList, "NATIONALITY"));
        patientInfo.setOtherNationality(identityMap.getIdentityValue(identityList, "OTHER NATIONALITY"));
        patientInfo.setHealthDistrict(identityMap.getIdentityValue(identityList, "HEALTH DISTRICT"));
        patientInfo.setHealthRegion(identityMap.getIdentityValue(identityList, "HEALTH REGION"));
        patientInfo.setGuid(identityMap.getIdentityValue(identityList, "GUID"));

        if (patientContacts.size() >= 1) {
            PatientContact contact = patientContacts.get(0);
            patientInfo.setPatientContact(contact);
        }

        if (patient.getLastupdated() != null) {
            patientInfo.setPatientLastUpdated(patient.getLastupdated().toString());
        }
        if (person.getLastupdated() != null) {
            patientInfo.setPersonLastUpdated(person.getLastupdated().toString());
        }

        patientInfo.setReadOnly(false);

        return patientInfo;
    }

    private void initAddressPartIds() {
        if (ADDRESS_PART_DEPT_ID == null) {
            List<AddressPart> partList = addressPartService.getAll();
            for (AddressPart addressPart : partList) {
                if ("department".equals(addressPart.getPartName())) {
                    ADDRESS_PART_DEPT_ID = addressPart.getId();
                } else if ("commune".equals(addressPart.getPartName())) {
                    ADDRESS_PART_COMMUNE_ID = addressPart.getId();
                } else if ("village".equals(addressPart.getPartName())) {
                    ADDRESS_PART_VILLAGE_ID = addressPart.getId();
                }
            }
        }
    }

    private String getAddress(Person person, String addressPartId) {
        if (GenericValidator.isBlankOrNull(addressPartId) || person == null) {
            return "";
        }
        PersonAddress address = personAddressService.getByPersonIdAndPartId(person.getId(), addressPartId);
        return address != null ? address.getValue() : "";
    }

    private String getLastNameForResponse(Person person) {
        if (PatientUtil.getUnknownPerson().getId().equals(person.getId())) {
            return null;
        } else {
            return person.getLastName();
        }
    }

    private String getPatientType(Patient patient) {
        PatientType patientType = patientPatientTypeService.getPatientTypeForPatient(patient.getId());
        return patientType != null ? patientType.getType() : null;
    }

    /**
     * Build sampleOrderItems with all order-level data including: - Lab number and
     * dates - Provider/Requester info from SampleHuman (provider) and
     * SampleRequester (organization) - Referring site (organization) info - Program
     * from ObservationHistory - Clinical information from ObservationHistory
     */
    private Map<String, Object> buildSampleOrderItems(Sample sample) {
        Map<String, Object> sampleOrderItems = new HashMap<>();

        // Basic sample info
        sampleOrderItems.put("labNo", sample.getAccessionNumber());
        sampleOrderItems.put("collectionDate", sample.getCollectionDateForDisplay());
        sampleOrderItems.put("receivedDateForDisplay", sample.getReceivedDateForDisplay());
        sampleOrderItems.put("receivedTime", sample.getReceivedTimeForDisplay());

        String sampleId = sample.getId();

        // Provider - get from SampleHuman.providerId via
        // sampleHumanService.getProviderForSample()
        // This is the correct approach since SamplePatientUpdateData stores provider in
        // SampleHuman
        Provider provider = sampleHumanService.getProviderForSample(sample);
        if (provider != null && provider.getPerson() != null) {
            Person providerPerson = provider.getPerson();
            // Ensure person data is loaded
            personService.getData(providerPerson);
            sampleOrderItems.put("providerPersonId", providerPerson.getId());
            sampleOrderItems.put("providerFirstName", providerPerson.getFirstName());
            sampleOrderItems.put("providerLastName", providerPerson.getLastName());
            sampleOrderItems.put("providerWorkPhone", providerPerson.getWorkPhone());
            sampleOrderItems.put("providerEmail", providerPerson.getEmail());
            sampleOrderItems.put("providerFax", providerPerson.getFax());
        }

        // Referring site (organization) - use RequesterService for organization
        RequesterService requesterService = new RequesterService(sampleId);
        Organization referringSite = requesterService.getOrganization();

        // Referring site department
        Organization department = requesterService.getOrganizationDepartment();

        // If referringSite is null but department exists, use department as the site
        // This handles cases where organization was stored with department type instead
        // of site type
        if (referringSite == null && department != null) {
            referringSite = department;
            department = null; // Clear department since we're using it as the site
        }

        if (referringSite != null) {
            sampleOrderItems.put("referringSiteId", referringSite.getId());
            sampleOrderItems.put("referringSiteName", referringSite.getOrganizationName());
            sampleOrderItems.put("referringSiteCode", referringSite.getShortName());
        }

        if (department != null) {
            sampleOrderItems.put("referringSiteDepartmentId", department.getId());
            sampleOrderItems.put("referringSiteDepartmentName", department.getOrganizationName());
        }

        // Program - Try multiple approaches to find program info
        // 1. Check observation_history for program NAME
        // 2. If found, use ProgramSampleService to get the Program.id
        // 3. If not found in observation_history, check program_sample table directly
        String programName = observationHistoryService.getRawValueForSample(ObservationType.PROGRAM, sampleId);

        if (programName != null && !programName.isEmpty()) {
            sampleOrderItems.put("program", programName); // Keep the name for display
            // Try to resolve the numeric program ID from the name
            try {
                ProgramSample programSample = programSampleService.getProgrammeSampleBySample(Integer.valueOf(sampleId),
                        programName);
                if (programSample != null && programSample.getProgram() != null) {
                    String programId = programSample.getProgram().getId();
                    sampleOrderItems.put("programId", programId);

                    // Load questionnaire response if available
                    if (programSample.getQuestionnaireResponseUuid() != null) {
                        try {
                            QuestionnaireResponse qr = fhirUtil.getLocalFhirClient().read()
                                    .resource(QuestionnaireResponse.class)
                                    .withId(programSample.getQuestionnaireResponseUuid().toString()).execute();
                            if (qr != null) {
                                sampleOrderItems.put("additionalQuestions", qr);
                            }
                        } catch (Exception qrEx) {
                            LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                                    "Error loading QuestionnaireResponse: " + qrEx.getMessage());
                        }
                    }
                } else {
                    // Fall back: try to find program by name directly
                    List<Program> allPrograms = programService.getAll();
                    for (Program p : allPrograms) {
                        if (p.getProgramName() != null && p.getProgramName().equals(programName)) {
                            sampleOrderItems.put("programId", p.getId());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                        "Exception resolving program ID: " + e.getMessage());
            }
        } else {
            // No program in observation_history - check program_sample table directly
            try {
                // Get all program samples and filter by sample ID
                List<ProgramSample> programSamples = programSampleService
                        .getProgramSamplesByAccessionNumberOrProgramName(sample.getAccessionNumber());
                if (programSamples != null && !programSamples.isEmpty()) {
                    ProgramSample ps = programSamples.get(0);
                    if (ps.getProgram() != null) {
                        sampleOrderItems.put("programId", ps.getProgram().getId());
                        sampleOrderItems.put("program", ps.getProgram().getProgramName());

                        // Load questionnaire response if available
                        if (ps.getQuestionnaireResponseUuid() != null) {
                            try {
                                QuestionnaireResponse qr = fhirUtil.getLocalFhirClient().read()
                                        .resource(QuestionnaireResponse.class)
                                        .withId(ps.getQuestionnaireResponseUuid().toString()).execute();
                                if (qr != null) {
                                    sampleOrderItems.put("additionalQuestions", qr);
                                }
                            } catch (Exception qrEx) {
                                LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                                        "Error loading QuestionnaireResponse: " + qrEx.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                        "Exception checking program_sample: " + e.getMessage());
            }
        }

        // Payment status
        String paymentStatus = observationHistoryService.getRawValueForSample(ObservationType.PAYMENT_STATUS, sampleId);
        if (paymentStatus != null) {
            sampleOrderItems.put("paymentOptionSelection", paymentStatus);
        }
        // Also add paymentOptions list for the dropdown
        sampleOrderItems.put("paymentOptions",
                DisplayListService.getInstance().getList(ListType.SAMPLE_PATIENT_PAYMENT_OPTIONS));

        // Billing reference number
        String billingRef = observationHistoryService.getRawValueForSample(ObservationType.BILLING_REFERENCE_NUMBER,
                sampleId);
        if (billingRef != null) {
            sampleOrderItems.put("billingReferenceNumber", billingRef);
        }

        // Test location code
        String testLocationCode = observationHistoryService.getRawValueForSample(ObservationType.TEST_LOCATION_CODE,
                sampleId);
        if (testLocationCode != null) {
            sampleOrderItems.put("testLocationCode", testLocationCode);
        }

        // Other location code
        String otherLocationCode = observationHistoryService
                .getRawValueForSample(ObservationType.TEST_LOCATION_CODE_OTHER, sampleId);
        if (otherLocationCode != null) {
            sampleOrderItems.put("otherLocationCode", otherLocationCode);
        }

        // Request date
        String requestDate = observationHistoryService.getRawValueForSample(ObservationType.REQUEST_DATE, sampleId);
        if (requestDate != null) {
            sampleOrderItems.put("requestDate", requestDate);
        }

        // Next visit date
        String nextVisitDate = observationHistoryService.getRawValueForSample(ObservationType.NEXT_VISIT_DATE,
                sampleId);
        if (nextVisitDate != null) {
            sampleOrderItems.put("nextVisitDate", nextVisitDate);
        }

        // Provisional clinical diagnosis
        String provisionalDiagnosis = observationHistoryService
                .getRawValueForSample(ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS, sampleId);
        if (provisionalDiagnosis != null) {
            sampleOrderItems.put("provisionalClinicalDiagnosis", provisionalDiagnosis);
        }

        // Priority (from sample entity if available)
        if (sample.getPriority() != null) {
            sampleOrderItems.put("priority", sample.getPriority().name());
        }

        // Environmental workflow fields (OGC-356)
        Map<String, String> environmentalFields = buildEnvironmentalFields(sample, sampleId);
        if (!environmentalFields.isEmpty()) {
            sampleOrderItems.put("environmentalFields", environmentalFields);
        }

        return sampleOrderItems;
    }

    /**
     * Determine whether Step 1 (Enter Order) is genuinely complete.
     *
     * <p>
     * Generating a lab number alone is not sufficient — the order must have been
     * saved with its required data. We check:
     * <ul>
     * <li>receivedDate is set (always written by a proper Step 1 save)</li>
     * <li>a patient is linked (clinical workflow), OR</li>
     * <li>an environmental workflow type is recorded (environmental workflow)</li>
     * </ul>
     */
    private boolean isEnterComplete(Sample sample) {
        // receivedDate is set by a proper Step 1 save; absent for bare lab-number-only
        // records
        if (sample.getReceivedDate() == null) {
            return false;
        }
        // Clinical: patient must be linked
        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient != null) {
            return true;
        }
        // Environmental: workflow type observation must be recorded
        String workflowType = observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE,
                sample.getId());
        return workflowType != null;
    }

    /**
     * Build environmental fields map from ObservationHistory entries. Used for
     * environmental workflow orders (non-patient samples).
     */
    private Map<String, String> buildEnvironmentalFields(Sample sample, String sampleId) {
        Map<String, String> envFields = new HashMap<>();

        LogEvent.logDebug(this.getClass().getSimpleName(), "buildEnvironmentalFields",
                "Building environmental fields for sampleId: " + sampleId);

        // Workflow type
        String workflowType = observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE,
                sampleId);
        LogEvent.logDebug(this.getClass().getSimpleName(), "buildEnvironmentalFields",
                "workflowType from DB: " + workflowType);
        if (workflowType != null) {
            envFields.put("workflowType", workflowType);
        }

        // Collection site description
        String siteDescription = observationHistoryService
                .getRawValueForSample(ObservationType.ENV_COLLECTION_SITE_DESCRIPTION, sampleId);
        if (siteDescription != null) {
            envFields.put("collectionSiteDescription", siteDescription);
        }

        // Requester reference
        String requesterRef = observationHistoryService.getRawValueForSample(ObservationType.ENV_REQUESTER_REFERENCE,
                sampleId);
        if (requesterRef != null) {
            envFields.put("requesterReference", requesterRef);
        }

        // Environmental conditions
        String conditions = observationHistoryService.getRawValueForSample(ObservationType.ENV_ENVIRONMENTAL_CONDITIONS,
                sampleId);
        if (conditions != null) {
            envFields.put("environmentalConditions", conditions);
        }

        // Location hierarchy (Region, District, Village)
        String regionId = observationHistoryService.getRawValueForSample(ObservationType.ENV_LOCATION_REGION_ID,
                sampleId);
        if (regionId != null) {
            envFields.put("locationHierarchy.1", regionId);
        }

        String districtId = observationHistoryService.getRawValueForSample(ObservationType.ENV_LOCATION_DISTRICT_ID,
                sampleId);
        if (districtId != null) {
            envFields.put("locationHierarchy.2", districtId);
        }

        String villageId = observationHistoryService.getRawValueForSample(ObservationType.ENV_LOCATION_VILLAGE_ID,
                sampleId);
        if (villageId != null) {
            envFields.put("locationHierarchy.3", villageId);
        }

        // Sampling site fields
        String samplingSiteId = observationHistoryService.getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_ID,
                sampleId);
        if (samplingSiteId != null) {
            envFields.put("samplingSiteId", samplingSiteId);
        }
        String samplingSiteName = observationHistoryService.getRawValueForSample(ObservationType.ENV_SAMPLING_SITE_NAME,
                sampleId);
        if (samplingSiteName != null) {
            envFields.put("samplingSiteName", samplingSiteName);
        }
        String siteType = observationHistoryService.getRawValueForSample(ObservationType.ENV_SITE_TYPE, sampleId);
        if (siteType != null) {
            envFields.put("siteType", siteType);
        }
        String siteSubtype = observationHistoryService.getRawValueForSample(ObservationType.ENV_SITE_SUBTYPE, sampleId);
        if (siteSubtype != null) {
            envFields.put("siteSubtype", siteSubtype);
        }
        String envZone = observationHistoryService.getRawValueForSample(ObservationType.ENV_ENVIRONMENTAL_ZONE,
                sampleId);
        if (envZone != null) {
            envFields.put("environmentalZone", envZone);
        }
        String regulatoryRef = observationHistoryService.getRawValueForSample(ObservationType.ENV_REGULATORY_REFERENCE,
                sampleId);
        if (regulatoryRef != null) {
            envFields.put("regulatoryReference", regulatoryRef);
        }
        String collectionMethod = observationHistoryService.getRawValueForSample(ObservationType.ENV_COLLECTION_METHOD,
                sampleId);
        if (collectionMethod != null) {
            envFields.put("collectionMethod", collectionMethod);
        }
        String waterTemp = observationHistoryService.getRawValueForSample(ObservationType.ENV_WATER_TEMP, sampleId);
        if (waterTemp != null) {
            envFields.put("waterTemp", waterTemp);
        }
        String ambientTemp = observationHistoryService.getRawValueForSample(ObservationType.ENV_AMBIENT_TEMP, sampleId);
        if (ambientTemp != null) {
            envFields.put("ambientTemp", ambientTemp);
        }
        String weather = observationHistoryService.getRawValueForSample(ObservationType.ENV_WEATHER, sampleId);
        if (weather != null) {
            envFields.put("weather", weather);
        }
        String preservationMethod = observationHistoryService
                .getRawValueForSample(ObservationType.ENV_PRESERVATION_METHOD, sampleId);
        if (preservationMethod != null) {
            envFields.put("preservationMethod", preservationMethod);
        }
        String fieldNotes = observationHistoryService.getRawValueForSample(ObservationType.ENV_FIELD_NOTES, sampleId);
        if (fieldNotes != null) {
            envFields.put("fieldNotes", fieldNotes);
        }
        List<SampleComplianceStandard> scsLinks = sampleComplianceStandardService.getAllForSample(sampleId);
        if (!scsLinks.isEmpty()) {
            List<String> ids = scsLinks.stream().map(l -> l.getComplianceStandard().getId())
                    .collect(Collectors.toList());
            try {
                envFields.put("complianceStandards", JSON_MAPPER.writeValueAsString(ids));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                LogEvent.logWarn(this.getClass().getName(), "getEnvironmentalFields",
                        "Could not serialize compliance standard IDs for sample " + sampleId);
            }
        } else {
            String complianceStandards = observationHistoryService
                    .getRawValueForSample(ObservationType.ENV_COMPLIANCE_STANDARDS, sampleId);
            if (complianceStandards != null) {
                envFields.put("complianceStandards", complianceStandards);
            }
        }
        String contactPerson = observationHistoryService.getRawValueForSample(ObservationType.ENV_CONTACT_PERSON,
                sampleId);
        if (contactPerson != null) {
            envFields.put("contactPerson", contactPerson);
        }
        String contactPhone = observationHistoryService.getRawValueForSample(ObservationType.ENV_CONTACT_PHONE,
                sampleId);
        if (contactPhone != null) {
            envFields.put("contactPhone", contactPhone);
        }

        // GPS coordinates from Sample entity (already added by
        // 029-add-gps-coordinates-to-sample.xml)
        if (sample.getGpsLatitude() != null) {
            envFields.put("gpsLatitude", String.valueOf(sample.getGpsLatitude()));
        }
        if (sample.getGpsLongitude() != null) {
            envFields.put("gpsLongitude", String.valueOf(sample.getGpsLongitude()));
        }
        if (sample.getGpsAccuracyMeters() != null) {
            envFields.put("gpsAccuracy", String.valueOf(sample.getGpsAccuracyMeters()));
        }
        if (sample.getGpsCaptureMethod() != null) {
            envFields.put("gpsCaptureMethod", sample.getGpsCaptureMethod());
        }

        // Vector surveillance fields
        String vecCollectionSiteId = observationHistoryService
                .getRawValueForSample(ObservationType.VS_COLLECTION_SITE_ID, sampleId);
        if (vecCollectionSiteId != null)
            envFields.put("vecCollectionSiteId", vecCollectionSiteId);
        String vecCollectionSiteName = observationHistoryService
                .getRawValueForSample(ObservationType.VS_COLLECTION_SITE_NAME, sampleId);
        if (vecCollectionSiteName != null)
            envFields.put("vecCollectionSiteName", vecCollectionSiteName);
        String vecGpsLatitude = observationHistoryService.getRawValueForSample(ObservationType.VS_GPS_LATITUDE,
                sampleId);
        if (vecGpsLatitude != null)
            envFields.put("vecGpsLatitude", vecGpsLatitude);
        String vecGpsLongitude = observationHistoryService.getRawValueForSample(ObservationType.VS_GPS_LONGITUDE,
                sampleId);
        if (vecGpsLongitude != null)
            envFields.put("vecGpsLongitude", vecGpsLongitude);
        String vecLifecycleStage = observationHistoryService.getRawValueForSample(ObservationType.VS_LIFECYCLE_STAGE,
                sampleId);
        if (vecLifecycleStage != null)
            envFields.put("vecLifecycleStage", vecLifecycleStage);
        String vecTrapTypeId = observationHistoryService.getRawValueForSample(ObservationType.VS_TRAP_TYPE_ID,
                sampleId);
        if (vecTrapTypeId != null)
            envFields.put("vecTrapTypeId", vecTrapTypeId);
        String vecTimeOfDay = observationHistoryService.getRawValueForSample(ObservationType.VS_TIME_OF_DAY, sampleId);
        if (vecTimeOfDay != null)
            envFields.put("vecTimeOfDay", vecTimeOfDay);
        String vecRestingContext = observationHistoryService.getRawValueForSample(ObservationType.VS_RESTING_CONTEXT,
                sampleId);
        if (vecRestingContext != null)
            envFields.put("vecRestingContext", vecRestingContext);
        String vecHumanBitingCatch = observationHistoryService
                .getRawValueForSample(ObservationType.VS_HUMAN_BITING_CATCH, sampleId);
        if (vecHumanBitingCatch != null)
            envFields.put("vecHumanBitingCatch", vecHumanBitingCatch);
        String vecCollectionNotes = observationHistoryService.getRawValueForSample(ObservationType.VS_COLLECTION_NOTES,
                sampleId);
        if (vecCollectionNotes != null)
            envFields.put("vecCollectionNotes", vecCollectionNotes);

        return envFields;
    }
}
