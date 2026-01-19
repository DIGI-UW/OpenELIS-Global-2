package org.openelisglobal.medlab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for MedLab patient order operations. */
@Service
public class MedLabPatientOrderServiceImpl implements MedLabPatientOrderService {

    @Autowired
    private PatientService patientService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private TestService testService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private org.openelisglobal.storage.service.SampleStorageService sampleStorageService;

    @Autowired
    private org.openelisglobal.storage.service.StorageLocationService storageLocationService;

    @Autowired
    private org.openelisglobal.common.service.AccessionService accessionService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private OrderSampleLinkService orderSampleLinkService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Self-injection to enable proxy invocation for @Transactional methods called
    // within this class
    @Autowired
    @org.springframework.context.annotation.Lazy
    private MedLabPatientOrderService self;

    @Override
    @Transactional
    public Map<String, Object> createPatientOrder(String patientId, String labNo, String requestDate,
            String receivedDate, String priority, List<String> testIds, Integer notebookEntryId, Integer notebookPageId,
            String sysUserId) {

        Map<String, Object> result = new HashMap<>();
        LogEvent.logInfo(this.getClass().getSimpleName(), "createPatientOrder", "Starting order creation: patientId="
                + patientId + ", labNo=" + labNo + ", testIds=" + testIds + ", notebookPageId=" + notebookPageId);

        try {
            // Validate patient exists
            Patient patient = patientService.get(patientId);
            if (patient == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createPatientOrder",
                        "Patient not found: " + patientId);
                result.put("success", false);
                result.put("error", "Patient not found: " + patientId);
                return result;
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "createPatientOrder",
                    "Patient found: " + patient.getId());

            // Check if order with this lab number already exists
            List<ElectronicOrder> existingOrders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
            if (existingOrders != null && !existingOrders.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createPatientOrder",
                        "Order with lab number already exists: " + labNo);
                result.put("success", false);
                result.put("error", "Order with lab number already exists: " + labNo);
                return result;
            }

            // Also check if a sample with this accession number already exists
            Sample existingSample = sampleService.getSampleByAccessionNumber(labNo);
            if (existingSample != null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createPatientOrder",
                        "Sample with lab number already exists: " + labNo);
                result.put("success", false);
                result.put("error", "Sample with lab number already exists: " + labNo);
                return result;
            }

            // Validate test IDs and count valid tests
            int validTestCount = 0;
            for (String testId : testIds) {
                Test test = testService.get(testId);
                if (test != null) {
                    validTestCount++;
                }
            }
            if (validTestCount == 0) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createPatientOrder",
                        "No valid tests found in testIds: " + testIds);
                result.put("success", false);
                result.put("error", "No valid tests found");
                return result;
            }

            // Get status service
            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            // Create ElectronicOrder (the actual order entity)
            ElectronicOrder order = new ElectronicOrder();
            order.setExternalId(labNo);
            order.setPatient(patient);
            order.setStatusId(statusService.getStatusID(ExternalOrderStatus.Entered));
            order.setOrderTimestamp(DateUtil.getNowAsTimestamp());
            order.setSysUserId(sysUserId);
            order.setType(ElectronicOrderType.FHIR); // MedLab orders use FHIR type

            // Set priority
            if (StringUtils.isNotBlank(priority)) {
                try {
                    order.setPriority(OrderPriority.valueOf(priority));
                } catch (IllegalArgumentException e) {
                    order.setPriority(OrderPriority.ROUTINE);
                }
            } else {
                order.setPriority(OrderPriority.ROUTINE);
            }

            // Store order details (test IDs, dates, notebook info) in the data field as
            // JSON
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("testIds", testIds);
            orderData.put("requestDate", requestDate);
            orderData.put("receivedDate", receivedDate);
            orderData.put("notebookEntryId", notebookEntryId);
            orderData.put("notebookPageId", notebookPageId);
            orderData.put("patientId", patientId);

            try {
                order.setData(objectMapper.writeValueAsString(orderData));
            } catch (JsonProcessingException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "createPatientOrder",
                        "Failed to serialize order data: " + e.getMessage());
                // Use simple format as fallback
                order.setData("{\"testIds\":" + testIds + "}");
            }

            // Save the order
            electronicOrderService.insert(order);
            LogEvent.logInfo(this.getClass().getSimpleName(), "createPatientOrder",
                    "ElectronicOrder created: id=" + order.getId() + ", externalId=" + labNo);

            // Create NotebookPageSample entry for tracking (uses labNo as reference since
            // sample not yet created)
            if (notebookPageId != null) {
                try {
                    // Create a placeholder NotebookPageSample with the labNo as identifier
                    // This will be updated when the sample is collected
                    notebookPageSampleService.createPageSampleForPageString(notebookPageId, labNo,
                            NotebookPageSample.Status.PENDING);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "createPatientOrder",
                            "NotebookPageSample created for labNo=" + labNo + " on page=" + notebookPageId);
                } catch (Exception npsError) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createPatientOrder",
                            "Failed to create NotebookPageSample for labNo=" + labNo + " on page=" + notebookPageId
                                    + ": " + npsError.getMessage());
                    // Don't fail the order creation - notebook tracking is optional
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "createPatientOrder",
                    "Order creation completed: labNo=" + labNo + ", testCount=" + validTestCount);
            result.put("success", true);
            result.put("orderId", order.getId());
            result.put("labNo", labNo);
            result.put("patientId", patientId);
            result.put("testCount", validTestCount);
            result.put("status", "PENDING_COLLECTION");

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createPatientOrder",
                    "Exception during order creation: " + e.getMessage());
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error creating order: " + e.getMessage());
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "createPatientOrder",
                "Returning result: success=" + result.get("success"));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createBulkPatientOrders(List<Map<String, Object>> patients, String labNumberPrefix,
            List<String> testIds, String priority, Integer notebookEntryId, Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> createdOrders = new ArrayList<>();

        LogEvent.logInfo(this.getClass().getSimpleName(), "createBulkPatientOrders",
                "Starting atomic bulk order creation: patients=" + patients.size() + ", prefix=" + labNumberPrefix
                        + ", tests=" + testIds.size());

        // ==========================================================================
        // PHASE 1: VALIDATION - Validate ALL inputs before any database writes
        // ==========================================================================

        // Validate all patients exist
        List<String> validationErrors = new ArrayList<>();
        Map<String, Patient> validatedPatients = new LinkedHashMap<>(); // Preserve insertion order

        for (int i = 0; i < patients.size(); i++) {
            Map<String, Object> patientData = patients.get(i);
            String patientId = patientData.get("patientId") != null ? patientData.get("patientId").toString() : null;
            String firstName = (String) patientData.get("firstName");
            String lastName = (String) patientData.get("lastName");
            String patientName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");

            if (patientId == null || patientId.isEmpty()) {
                validationErrors.add("Row " + (i + 1) + ": Patient ID is required for " + patientName.trim());
                continue;
            }

            // Check for temporary/unsaved patient IDs (format: temp-XXXX)
            if (patientId.startsWith("temp-") || patientId.startsWith("temp_")) {
                validationErrors.add("Row " + (i + 1) + ": Patient '" + patientName.trim()
                        + "' has not been saved yet. Please save all patients before creating orders.");
                continue;
            }

            // Validate patientId is a valid numeric ID
            try {
                Long.parseLong(patientId);
            } catch (NumberFormatException e) {
                validationErrors.add("Row " + (i + 1) + ": Invalid patient ID format '" + patientId + "' for "
                        + patientName.trim() + ". Expected a numeric ID.");
                continue;
            }

            Patient patient = patientService.get(patientId);
            if (patient == null) {
                validationErrors.add("Row " + (i + 1) + ": Patient not found with ID '" + patientId + "' ("
                        + patientName.trim() + ")");
                continue;
            }

            // Store validated patient for use in creation phase
            validatedPatients.put(patientId, patient);
        }

        // Validate all tests exist
        Map<String, Test> validatedTests = new HashMap<>();
        for (String testId : testIds) {
            Test test = testService.get(testId);
            if (test == null) {
                validationErrors.add("Test not found with ID: " + testId);
            } else {
                validatedTests.put(testId, test);
            }
        }

        // If any validation errors, return immediately without creating anything
        if (!validationErrors.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "createBulkPatientOrders",
                    "Validation failed with " + validationErrors.size() + " errors");
            result.put("success", false);
            result.put("error", "Validation failed: " + String.join("; ", validationErrors));
            result.put("validationErrors", validationErrors);
            result.put("createdCount", 0);
            result.put("failedCount", patients.size());
            result.put("orders", createdOrders);
            return result;
        }

        // ==========================================================================
        // PHASE 2: CREATION - Create all orders atomically (all or nothing)
        // ==========================================================================

        try {
            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            for (Map<String, Object> patientData : patients) {
                String patientId = patientData.get("patientId").toString();
                String firstName = (String) patientData.get("firstName");
                String lastName = (String) patientData.get("lastName");
                Patient patient = validatedPatients.get(patientId);

                // Generate unique lab number
                long nextNumber = accessionService.getNextNumberIncrement(labNumberPrefix,
                        org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat.GENERAL);
                String labNo = labNumberPrefix + String.format("%03d", nextNumber);

                LogEvent.logInfo(this.getClass().getSimpleName(), "createBulkPatientOrders",
                        "Creating order for patient " + patientId + " with labNo " + labNo);

                // Check if order with this lab number already exists (race condition check)
                List<ElectronicOrder> existingOrders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
                if (existingOrders != null && !existingOrders.isEmpty()) {
                    throw new RuntimeException("Order with lab number '" + labNo + "' already exists for patient "
                            + patientId + " (" + firstName + " " + lastName + ")");
                }

                // Also check if a sample with this accession number already exists
                Sample existingSample = sampleService.getSampleByAccessionNumber(labNo);
                if (existingSample != null) {
                    throw new RuntimeException("Sample with lab number '" + labNo + "' already exists for patient "
                            + patientId + " (" + firstName + " " + lastName + ")");
                }

                // Create ElectronicOrder (the actual order entity)
                ElectronicOrder order = new ElectronicOrder();
                order.setExternalId(labNo);
                order.setPatient(patient);
                order.setStatusId(statusService.getStatusID(ExternalOrderStatus.Entered));
                order.setOrderTimestamp(DateUtil.getNowAsTimestamp());
                order.setSysUserId(sysUserId);
                order.setType(ElectronicOrderType.FHIR);

                // Set priority
                if (StringUtils.isNotBlank(priority)) {
                    try {
                        order.setPriority(OrderPriority.valueOf(priority));
                    } catch (IllegalArgumentException e) {
                        order.setPriority(OrderPriority.ROUTINE);
                    }
                } else {
                    order.setPriority(OrderPriority.ROUTINE);
                }

                // Store order details in the data field as JSON
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("testIds", testIds);
                orderData.put("notebookEntryId", notebookEntryId);
                orderData.put("notebookPageId", notebookPageId);
                orderData.put("patientId", patientId);

                try {
                    order.setData(objectMapper.writeValueAsString(orderData));
                } catch (JsonProcessingException e) {
                    order.setData("{\"testIds\":" + testIds + "}");
                }

                // Save the order
                electronicOrderService.insert(order);
                LogEvent.logInfo(this.getClass().getSimpleName(), "createBulkPatientOrders",
                        "ElectronicOrder created: id=" + order.getId() + ", externalId=" + labNo);

                // Create NotebookPageSample entry for tracking
                if (notebookPageId != null) {
                    try {
                        notebookPageSampleService.createPageSampleForPageString(notebookPageId, labNo,
                                NotebookPageSample.Status.PENDING);
                    } catch (Exception npsError) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "createBulkPatientOrders",
                                "Failed to create NotebookPageSample: " + npsError.getMessage());
                    }
                }

                // Build order info for response
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("orderId", order.getId());
                orderInfo.put("labNumber", labNo);
                orderInfo.put("patientId", patientId);
                orderInfo.put("patientName", lastName + ", " + firstName);
                orderInfo.put("testCount", validatedTests.size());
                orderInfo.put("status", "PENDING_COLLECTION");
                orderInfo.put("createdAt", new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                createdOrders.add(orderInfo);
            }

            // All orders created successfully
            result.put("success", true);
            result.put("createdCount", createdOrders.size());
            result.put("failedCount", 0);
            result.put("orders", createdOrders);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createBulkPatientOrders",
                    "Atomic bulk order creation completed successfully: created=" + createdOrders.size());

        } catch (Exception e) {
            // Any exception causes the entire transaction to roll back
            LogEvent.logError(this.getClass().getSimpleName(), "createBulkPatientOrders",
                    "Error during bulk order creation, rolling back all changes: " + e.getMessage());
            LogEvent.logError(e);

            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("createdCount", 0);
            result.put("failedCount", patients.size());
            result.put("orders", new ArrayList<>());

            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Bulk order creation failed: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createBulkIndependentOrders(List<Integer> sampleIds, String labNumberPrefix,
            List<String> testIds, Integer notebookEntryId, Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> orders = new ArrayList<>();

        LogEvent.logInfo(this.getClass().getSimpleName(), "createBulkIndependentOrders",
                "Starting bulk independent orders: samples=" + sampleIds.size() + ", prefix=" + labNumberPrefix
                        + ", tests=" + testIds.size());

        // PHASE 1: VALIDATION
        List<String> validationErrors = new ArrayList<>();

        // Validate all samples exist
        Map<Integer, Sample> validatedSamples = new LinkedHashMap<>();
        for (Integer sampleId : sampleIds) {
            Sample sample = sampleService.get(String.valueOf(sampleId));
            if (sample == null) {
                validationErrors.add("Sample not found: " + sampleId);
            } else {
                validatedSamples.put(sampleId, sample);
            }
        }

        // Validate all tests exist
        Map<String, Test> validatedTests = new HashMap<>();
        for (String testId : testIds) {
            Test test = testService.get(testId);
            if (test == null) {
                validationErrors.add("Test not found: " + testId);
            } else {
                validatedTests.put(testId, test);
            }
        }

        // If any validation errors, return immediately
        if (!validationErrors.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "createBulkIndependentOrders",
                    "Validation failed with " + validationErrors.size() + " errors");
            result.put("success", false);
            result.put("error", "Validation failed: " + String.join("; ", validationErrors));
            result.put("validationErrors", validationErrors);
            result.put("ordersCreated", 0);
            result.put("linksCreated", 0);
            result.put("orders", orders);
            return result;
        }

        // PHASE 2: CREATION
        try {
            IStatusService statusService = SpringContext.getBean(IStatusService.class);
            int ordersCreated = 0;
            int linksCreated = 0;

            for (Integer sampleId : sampleIds) {
                Sample sample = validatedSamples.get(sampleId);

                // Get sample item ID (first sample item)
                Integer sampleItemId = null;
                List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                if (sampleItems != null && !sampleItems.isEmpty()) {
                    sampleItemId = Integer.valueOf(sampleItems.get(0).getId());
                }

                // Get patient from sample (may be null for anonymous samples)
                Patient patient = sampleHumanService.getPatientForSample(sample);

                // Generate unique lab number
                long nextNumber = accessionService.getNextNumberIncrement(labNumberPrefix.trim(),
                        org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat.GENERAL);
                String labNo = labNumberPrefix.trim() + String.format("%03d", nextNumber);

                LogEvent.logInfo(this.getClass().getSimpleName(), "createBulkIndependentOrders",
                        "Creating order for sample " + sampleId + " with labNo " + labNo);

                // Check if order with this lab number already exists
                List<ElectronicOrder> existingOrders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
                if (existingOrders != null && !existingOrders.isEmpty()) {
                    throw new RuntimeException(
                            "Order with lab number '" + labNo + "' already exists for sample " + sampleId);
                }

                // Also check if a sample with this accession number already exists
                Sample existingSample = sampleService.getSampleByAccessionNumber(labNo);
                if (existingSample != null) {
                    throw new RuntimeException(
                            "Sample with lab number '" + labNo + "' already exists for sample " + sampleId);
                }

                // Create ElectronicOrder
                ElectronicOrder order = new ElectronicOrder();
                order.setExternalId(labNo);
                order.setPatient(patient); // May be null for anonymous samples
                order.setStatusId(statusService.getStatusID(ExternalOrderStatus.Entered));
                order.setOrderTimestamp(DateUtil.getNowAsTimestamp());
                order.setSysUserId(sysUserId);
                order.setType(ElectronicOrderType.FHIR);
                order.setPriority(OrderPriority.ROUTINE);

                // Store order details in the data field as JSON
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("testIds", testIds);
                orderData.put("notebookEntryId", notebookEntryId);
                orderData.put("notebookPageId", notebookPageId);
                if (patient != null) {
                    orderData.put("patientId", patient.getId());
                }
                orderData.put("sampleId", sampleId);

                try {
                    order.setData(objectMapper.writeValueAsString(orderData));
                } catch (JsonProcessingException e) {
                    order.setData("{\"testIds\":" + testIds + ",\"sampleId\":" + sampleId + "}");
                }

                // Save the order
                String orderId = electronicOrderService.insert(order);

                if (orderId != null) {
                    ordersCreated++;

                    // Create OrderSampleLink for each test
                    int linksForSample = 0;
                    for (String testId : testIds) {
                        orderSampleLinkService.linkSampleToOrder(Integer.valueOf(orderId), sampleId, sampleItemId,
                                Integer.valueOf(testId), Integer.valueOf(sysUserId));
                        linksCreated++;
                        linksForSample++;
                    }

                    // Add to result list
                    Map<String, Object> orderInfo = new HashMap<>();
                    orderInfo.put("orderId", orderId);
                    orderInfo.put("labNo", labNo);
                    orderInfo.put("sampleId", sampleId);
                    orderInfo.put("linksCreated", linksForSample);
                    orderInfo.put("patientId", patient != null ? patient.getId() : null);
                    orders.add(orderInfo);
                } else {
                    throw new RuntimeException("Failed to create order for sample " + sampleId);
                }
            }

            result.put("success", true);
            result.put("ordersCreated", ordersCreated);
            result.put("linksCreated", linksCreated);
            result.put("totalSamplesRequested", sampleIds.size());
            result.put("orders", orders);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createBulkIndependentOrders",
                    "Successfully created " + ordersCreated + " orders with " + linksCreated + " links");

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createBulkIndependentOrders",
                    "Error creating bulk independent orders: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Error creating orders: " + e.getMessage());
            result.put("ordersCreated", 0);
            result.put("linksCreated", 0);
            result.put("orders", new ArrayList<>());

            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Bulk independent order creation failed: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public List<String> getLabNumberPreview(String prefix, int count) {
        List<String> previewNumbers = new ArrayList<>();

        // Get the next number WITHOUT incrementing (peek at sequence)
        long nextNumber = accessionService.getNextNumberNoIncrement(prefix,
                org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat.GENERAL);

        // Generate preview numbers
        for (int i = 0; i < count; i++) {
            String labNo = prefix + String.format("%03d", nextNumber + i);
            previewNumbers.add(labNo);
        }

        return previewNumbers;
    }

    @Override
    public List<Map<String, Object>> getOrdersForPage(Integer pageId) {
        List<Map<String, Object>> orders = new ArrayList<>();

        if (pageId == null) {
            return orders;
        }

        // Get status service
        IStatusService statusService = SpringContext.getBean(IStatusService.class);

        // Get ElectronicOrders with "Entered" status (pending collection)
        List<Integer> pendingStatuses = new ArrayList<>();
        String statusId = statusService.getStatusID(ExternalOrderStatus.Entered);
        pendingStatuses.add(Integer.parseInt(statusId));

        List<ElectronicOrder> allPendingOrders = electronicOrderService
                .getAllElectronicOrdersByStatusList(pendingStatuses, ElectronicOrder.SortOrder.LAST_UPDATED_DESC);

        // Filter by pageId in the data field
        for (ElectronicOrder order : allPendingOrders) {
            try {
                // Check if this order belongs to this page
                if (StringUtils.isNotBlank(order.getData())) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderDataMap = objectMapper.readValue(order.getData(), Map.class);
                    Object pageIdObj = orderDataMap.get("notebookPageId");

                    // Only include orders for this specific page
                    if (pageIdObj != null && pageId.equals(Integer.parseInt(pageIdObj.toString()))) {
                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("id", order.getId());
                        orderData.put("labNo", order.getExternalId());
                        orderData.put("receivedDate", order.getOrderTimestamp());
                        orderData.put("priority", order.getPriority() != null ? order.getPriority().name() : "ROUTINE");
                        orderData.put("status", "PENDING_COLLECTION");

                        // Get patient info
                        Patient patient = order.getPatient();
                        if (patient != null) {
                            orderData.put("patientId", patient.getId());
                            String patientName = "";
                            if (patient.getPerson() != null) {
                                patientName = (patient.getPerson().getFirstName() != null
                                        ? patient.getPerson().getFirstName()
                                        : "") + " "
                                        + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                                : "");
                            }
                            orderData.put("patientName", patientName.trim());
                        }

                        // Get test count and test details from stored data
                        Object testIdsObj = orderDataMap.get("testIds");
                        int testCount = 0;
                        List<Map<String, Object>> tests = new ArrayList<>();
                        if (testIdsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> testIds = (List<String>) testIdsObj;
                            testCount = testIds.size();

                            // Get test details for each test ID
                            for (String testIdStr : testIds) {
                                try {
                                    Test test = testService.get(testIdStr);
                                    if (test != null) {
                                        Map<String, Object> testData = new HashMap<>();
                                        testData.put("id", test.getId());
                                        testData.put("testId", test.getId());
                                        testData.put("testName", test.getName());
                                        testData.put("name", test.getName());
                                        tests.add(testData);
                                    }
                                } catch (Exception e) {
                                    LogEvent.logWarn(this.getClass().getSimpleName(), "getOrdersForPage",
                                            "Error loading test " + testIdStr + ": " + e.getMessage());
                                }
                            }
                        }
                        orderData.put("testCount", testCount);
                        orderData.put("tests", tests);

                        orders.add(orderData);
                    }
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getOrdersForPage",
                        "Error processing electronic order: " + order.getId() + " - " + e.getMessage());
            }
        }

        return orders;
    }

    @Override
    public Map<String, Object> getOrderByLabNo(String labNo) {
        Map<String, Object> result = new HashMap<>();

        Sample sample = sampleService.getSampleByAccessionNumber(labNo);
        if (sample == null) {
            result.put("error", "Order not found: " + labNo);
            return result;
        }

        result.put("id", sample.getId());
        result.put("labNo", sample.getAccessionNumber());
        result.put("status", sample.getStatusId());
        result.put("receivedDate", sample.getReceivedTimestamp());
        result.put("priority", sample.getPriority() != null ? sample.getPriority().name() : "ROUTINE");

        // Get patient info
        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient != null) {
            result.put("patientId", patient.getId());
            result.put("patientName",
                    patient.getPerson() != null
                            ? patient.getPerson().getFirstName() + " " + patient.getPerson().getLastName()
                            : "");
        }

        // Get test count
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        result.put("testCount", analyses != null ? analyses.size() : 0);

        return result;
    }

    private TypeOfSample getDefaultSampleTypeForTest(Test test) {
        // Get the first sample type associated with this test
        List<TypeOfSample> sampleTypes = typeOfSampleService.getTypeOfSampleForTest(test.getId());
        if (sampleTypes != null && !sampleTypes.isEmpty()) {
            return sampleTypes.get(0);
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> recordSampleCollection(String labNo, String sampleTypeId, String containerType,
            String collectionTime, String collectionDate, String collectorId, String volume, String notes,
            Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                    "Starting sample collection for labNo=" + labNo);

            // Find the ElectronicOrder by externalId (labNo)
            List<ElectronicOrder> orders = electronicOrderService.getElectronicOrdersByExternalId(labNo);
            if (orders == null || orders.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "recordSampleCollection",
                        "No order found for labNo: " + labNo);
                result.put("success", false);
                result.put("error", "No order found for lab number: " + labNo);
                return result;
            }

            ElectronicOrder order = orders.get(0);
            LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                    "Found order: id=" + order.getId() + ", patient=" + order.getPatient());

            // Check if sample already exists (idempotency check)
            Sample existingSample = sampleService.getSampleByAccessionNumber(labNo);
            if (existingSample != null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                        "Sample already exists for labNo: " + labNo + ", updating collection info");
                // Update existing sample with collection details
                return updateExistingSampleCollection(existingSample, sampleTypeId, containerType, collectionTime,
                        collectionDate, collectorId, volume, sysUserId);
            }

            // Get patient from order
            Patient patient = order.getPatient();
            if (patient == null) {
                result.put("success", false);
                result.put("error", "Order has no associated patient");
                return result;
            }

            // Parse test IDs and notebookPageId from order data
            List<String> testIds = new ArrayList<>();
            Integer orderNotebookPageId = null;
            if (StringUtils.isNotBlank(order.getData())) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderData = objectMapper.readValue(order.getData(), Map.class);
                    Object testIdsObj = orderData.get("testIds");
                    if (testIdsObj instanceof List) {
                        for (Object tid : (List<?>) testIdsObj) {
                            testIds.add(tid.toString());
                        }
                    }
                    Object pageIdObj = orderData.get("notebookPageId");
                    if (pageIdObj != null) {
                        orderNotebookPageId = Integer.parseInt(pageIdObj.toString());
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordSampleCollection",
                            "Failed to parse order data: " + e.getMessage());
                }
            }

            if (testIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No tests specified in order");
                return result;
            }

            // Get status service
            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            // Create the Sample
            Sample sample = new Sample();
            sample.setAccessionNumber(labNo);
            sample.setFhirUuid(UUID.randomUUID());
            sample.setSysUserId(sysUserId);

            // Set collection date/time using explicit pattern for consistency
            Timestamp collectionTimestamp;
            if (StringUtils.isNotBlank(collectionDate)) {
                if (StringUtils.isNotBlank(collectionTime)) {
                    collectionTimestamp = DateUtil.convertStringDateToTimestampWithPatternNoLocale(
                            collectionDate + " " + collectionTime, "yyyy-MM-dd HH:mm");
                } else {
                    collectionTimestamp = DateUtil.convertStringDateToTruncatedTimestamp(collectionDate);
                }
            } else {
                collectionTimestamp = DateUtil.getNowAsTimestamp();
            }
            sample.setReceivedTimestamp(collectionTimestamp);
            sample.setCollectionDate(collectionTimestamp);

            // Set priority from order
            sample.setPriority(order.getPriority() != null ? order.getPriority() : OrderPriority.ROUTINE);

            // Set status
            sample.setStatusId(statusService.getStatusID(SampleStatus.Entered));
            sample.setEnteredDate(DateUtil.getNowAsSqlDate());

            // Save sample
            boolean inserted = sampleService.insertDataWithAccessionNumber(sample);
            if (!inserted) {
                result.put("success", false);
                result.put("error", "Failed to create sample with accession number: " + labNo);
                return result;
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                    "Sample created: id=" + sample.getId());

            // Create SampleHuman link
            SampleHuman sampleHuman = new SampleHuman();
            sampleHuman.setSampleId(sample.getId());
            sampleHuman.setPatientId(patient.getId());
            sampleHuman.setSysUserId(sysUserId);
            sampleHumanService.insert(sampleHuman);
            LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection", "SampleHuman link created");

            // Create OrderSampleLink to connect order to sample
            Integer orderId = Integer.parseInt(order.getId());
            Integer sampleId = Integer.parseInt(sample.getId());
            orderSampleLinkService.linkSampleToOrderWithRequirements(orderId, sampleId, null, null, containerType,
                    volume != null ? new java.math.BigDecimal(volume) : null, notes,
                    sysUserId != null ? Integer.parseInt(sysUserId) : null);
            LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                    "OrderSampleLink created: orderId=" + orderId + ", sampleId=" + sampleId);

            // Create SampleItems and Analyses for each test
            int testCount = 0;
            TypeOfSample typeOfSample = null;
            if (StringUtils.isNotBlank(sampleTypeId)) {
                typeOfSample = typeOfSampleService.get(sampleTypeId);
            }

            for (String testId : testIds) {
                Test test = testService.get(testId);
                if (test == null) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordSampleCollection",
                            "Test not found: " + testId);
                    continue;
                }

                // Get sample type - use provided or default for test
                TypeOfSample itemSampleType = typeOfSample != null ? typeOfSample : getDefaultSampleTypeForTest(test);
                if (itemSampleType == null) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordSampleCollection",
                            "No sample type for test: " + testId);
                    continue;
                }

                // Create SampleItem
                SampleItem sampleItem = new SampleItem();
                sampleItem.setSample(sample);
                sampleItem.setTypeOfSample(itemSampleType);
                sampleItem.setSortOrder(String.valueOf(testCount + 1));
                sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
                sampleItem.setSysUserId(sysUserId);

                // Store collection info
                StringBuilder collectionInfo = new StringBuilder();
                if (StringUtils.isNotBlank(collectorId)) {
                    collectionInfo.append("collector:").append(collectorId);
                }
                if (StringUtils.isNotBlank(containerType)) {
                    if (collectionInfo.length() > 0)
                        collectionInfo.append(";");
                    collectionInfo.append("container:").append(containerType);
                }
                if (StringUtils.isNotBlank(volume)) {
                    if (collectionInfo.length() > 0)
                        collectionInfo.append(";");
                    collectionInfo.append("volume:").append(volume);
                }
                if (collectionInfo.length() > 0) {
                    sampleItem.setCollector(collectionInfo.toString());
                }

                sampleItemService.insert(sampleItem);
                LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                        "SampleItem created: id=" + sampleItem.getId());

                // Create Analysis
                Analysis analysis = new Analysis();
                analysis.setSampleItem(sampleItem);
                analysis.setTest(test);
                analysis.setAnalysisType("MANUAL");
                analysis.setStatusId(statusService.getStatusID(SampleStatus.Entered));
                analysis.setSysUserId(sysUserId);
                analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
                analysisService.insert(analysis);
                LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                        "Analysis created: id=" + analysis.getId());

                testCount++;
            }

            // Update NotebookPageSample entries for this labNo to COMPLETED
            // Use orderNotebookPageId or the passed notebookPageId
            Integer effectivePageId = orderNotebookPageId != null ? orderNotebookPageId : notebookPageId;
            if (effectivePageId != null) {
                NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(labNo,
                        effectivePageId);
                if (pageSample != null) {
                    pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                    pageSample.setSampleItemId(sample.getId()); // Update with actual sample ID
                    pageSample.setSysUserId(sysUserId);
                    notebookPageSampleService.update(pageSample);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                            "NotebookPageSample updated: id=" + pageSample.getId());
                }
            }

            // Update order status (optional - could use a "Collected" status)
            // order.setStatusId(statusService.getStatusID(ExternalOrderStatus.Realized));
            // electronicOrderService.update(order);

            result.put("success", true);
            result.put("sampleId", sample.getId());
            result.put("orderId", order.getId());
            result.put("patientId", patient.getId());
            result.put("labNo", labNo);
            result.put("testCount", testCount);
            result.put("message", "Sample collection recorded successfully");

            LogEvent.logInfo(this.getClass().getSimpleName(), "recordSampleCollection",
                    "Sample collection completed: labNo=" + labNo + ", sampleId=" + sample.getId());

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "recordSampleCollection",
                    "Error recording sample collection: " + e.getMessage());
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording sample collection: " + e.getMessage());
        }

        return result;
    }

    /**
     * Helper method to update collection details on an existing sample.
     */
    private Map<String, Object> updateExistingSampleCollection(Sample sample, String sampleTypeId, String containerType,
            String collectionTime, String collectionDate, String collectorId, String volume, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        // Update collection date/time
        if (StringUtils.isNotBlank(collectionDate)) {
            Timestamp collectionTimestamp;
            if (StringUtils.isNotBlank(collectionTime)) {
                collectionTimestamp = DateUtil.convertStringDateToTimestamp(collectionDate + " " + collectionTime);
            } else {
                collectionTimestamp = DateUtil.convertStringDateToTruncatedTimestamp(collectionDate);
            }
            sample.setCollectionDate(collectionTimestamp);
        }
        sample.setSysUserId(sysUserId);
        sampleService.update(sample);

        // Update sample items
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (sampleItems != null && !sampleItems.isEmpty()) {
            TypeOfSample typeOfSample = null;
            if (StringUtils.isNotBlank(sampleTypeId)) {
                typeOfSample = typeOfSampleService.get(sampleTypeId);
            }

            for (SampleItem sampleItem : sampleItems) {
                if (typeOfSample != null) {
                    sampleItem.setTypeOfSample(typeOfSample);
                }

                StringBuilder collectionInfo = new StringBuilder();
                if (StringUtils.isNotBlank(collectorId)) {
                    collectionInfo.append("collector:").append(collectorId);
                }
                if (StringUtils.isNotBlank(containerType)) {
                    if (collectionInfo.length() > 0)
                        collectionInfo.append(";");
                    collectionInfo.append("container:").append(containerType);
                }
                if (StringUtils.isNotBlank(volume)) {
                    if (collectionInfo.length() > 0)
                        collectionInfo.append(";");
                    collectionInfo.append("volume:").append(volume);
                }
                if (collectionInfo.length() > 0) {
                    sampleItem.setCollector(collectionInfo.toString());
                }

                sampleItem.setSysUserId(sysUserId);
                sampleItemService.update(sampleItem);
            }
        }

        result.put("success", true);
        result.put("sampleId", sample.getId());
        result.put("labNo", sample.getAccessionNumber());
        result.put("message", "Sample collection updated successfully");
        return result;
    }

    @Override
    public List<Map<String, Object>> getOrdersForEntry(Integer entryId) {
        List<Map<String, Object>> orders = new ArrayList<>();

        if (entryId == null) {
            return orders;
        }

        try {
            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return orders;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return orders;
            }

            // Track samples we've already processed to avoid duplicates
            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();

            // Get orders from all pages
            for (NoteBookPage page : pages) {
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                for (NotebookPageSample pageSample : pageSamples) {
                    try {
                        SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                        if (sampleItem == null || sampleItem.getSample() == null) {
                            continue;
                        }

                        Sample sample = sampleItem.getSample();

                        // Skip if we've already processed this sample
                        if (processedSampleIds.contains(sample.getId())) {
                            continue;
                        }
                        processedSampleIds.add(sample.getId());

                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("id", sample.getId());
                        orderData.put("labNo", sample.getAccessionNumber());
                        orderData.put("receivedDate", sample.getReceivedTimestamp());
                        orderData.put("priority",
                                sample.getPriority() != null ? sample.getPriority().name() : "ROUTINE");

                        // Map NotebookPageSample status to order status
                        String status = pageSample.getStatus() != null ? pageSample.getStatus().name() : "PENDING";
                        orderData.put("status", status);

                        // Get patient info
                        Patient patient = sampleHumanService.getPatientForSample(sample);
                        if (patient != null) {
                            orderData.put("patientId", patient.getId());
                            String patientName = "";
                            if (patient.getPerson() != null) {
                                patientName = (patient.getPerson().getFirstName() != null
                                        ? patient.getPerson().getFirstName()
                                        : "") + " "
                                        + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                                : "");
                            }
                            orderData.put("patientName", patientName.trim());
                        }

                        // Get test count
                        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
                        orderData.put("testCount", analyses != null ? analyses.size() : 0);

                        orders.add(orderData);
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "getOrdersForEntry",
                                "Error processing sample item: " + pageSample.getSampleItemId() + " - "
                                        + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return orders;
    }

    @Override
    public List<Map<String, Object>> getSamplesForQC(Integer entryId) {
        List<Map<String, Object>> samples = new ArrayList<>();

        if (entryId == null) {
            return samples;
        }

        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                    "Getting samples for QC for entry: " + entryId);

            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                        "Entry or notebook is null for entryId: " + entryId);
                return samples;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                    "Found " + (pages != null ? pages.size() : 0) + " pages for notebook: " + notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return samples;
            }

            // Find the sample-collection page (previous step) and QC page
            Integer collectionPageId = null;
            Integer qcPageId = null;
            for (NoteBookPage page : pages) {
                if ("sample-collection".equals(page.getPageId())) {
                    collectionPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                            "Found sample-collection page with id: " + collectionPageId);
                } else if ("quality-check".equals(page.getPageId())
                        || "medlab-quality-check".equals(page.getPageId())) {
                    qcPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                            "Found QC page with id: " + qcPageId);
                }
            }

            if (collectionPageId == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                        "No sample-collection page found for notebook: " + notebook.getId());
                return samples;
            }

            // Get samples from the sample-collection page that have COMPLETED status
            List<NotebookPageSample> collectionPageSamples = notebookPageSampleService.getByPageId(collectionPageId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                    "Sample-collection page " + collectionPageId + " has " + collectionPageSamples.size() + " samples");

            // Track samples we've already processed to avoid duplicates
            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();

            for (NotebookPageSample collectionSample : collectionPageSamples) {
                try {
                    // Only include samples that have completed sample collection
                    if (collectionSample.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        continue;
                    }

                    SampleItem sampleItem = sampleItemService.get(collectionSample.getSampleItemId());
                    if (sampleItem == null || sampleItem.getSample() == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();

                    // Skip if we've already processed this sample
                    if (processedSampleIds.contains(sample.getId())) {
                        continue;
                    }
                    processedSampleIds.add(sample.getId());

                    // Get QC status - check if rejected on sample item AND if accepted (QC page
                    // COMPLETED)
                    boolean isRejected = sampleItem.isRejected();

                    // Check if sample has been QC'd (has COMPLETED status on QC page)
                    boolean isQCd = false;
                    Map<String, Object> qcData = null;
                    if (qcPageId != null) {
                        List<NotebookPageSample> qcPageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                        for (NotebookPageSample qcPs : qcPageSamples) {
                            if (qcPageId.equals(qcPs.getNotebookPageId())) {
                                if (qcPs.getStatus() == NotebookPageSample.Status.COMPLETED) {
                                    isQCd = true;
                                } else if (qcPs.getStatus() == NotebookPageSample.Status.SKIPPED) {
                                    isRejected = true;
                                }
                                qcData = qcPs.getData();
                                break;
                            }
                        }
                    }

                    // Determine QC status
                    String qcStatus;
                    if (isRejected) {
                        qcStatus = "REJECTED";
                    } else if (isQCd) {
                        qcStatus = "ACCEPTED";
                    } else {
                        qcStatus = "PENDING_QC";
                    }

                    LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForQC",
                            "Sample " + sample.getAccessionNumber() + " (sampleItemId=" + sampleItem.getId()
                                    + ") isRejected=" + isRejected + " isQCd=" + isQCd + " qcStatus=" + qcStatus);

                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("id", sample.getId());
                    sampleData.put("sampleItemId", sampleItem.getId());
                    sampleData.put("labNo", sample.getAccessionNumber());
                    sampleData.put("accessionNumber", sample.getAccessionNumber());
                    sampleData.put("receivedDate", sample.getReceivedTimestamp());
                    sampleData.put("collectionDate", sample.getCollectionDate());
                    sampleData.put("qcStatus", qcStatus);
                    sampleData.put("isRejected", isRejected);
                    sampleData.put("pageStatus", qcStatus.equals("ACCEPTED") ? "COMPLETED"
                            : qcStatus.equals("REJECTED") ? "SKIPPED" : "PENDING");

                    // Include QC data if available
                    if (qcData != null) {
                        sampleData.put("data", qcData);
                    }

                    // Get sample type
                    if (sampleItem.getTypeOfSample() != null) {
                        sampleData.put("sampleType", sampleItem.getTypeOfSample().getLocalizedName());
                        sampleData.put("sampleTypeId", sampleItem.getTypeOfSample().getId());
                    }

                    // Get patient info
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    if (patient != null) {
                        sampleData.put("patientId", patient.getId());
                        String patientName = "";
                        if (patient.getPerson() != null) {
                            patientName = (patient.getPerson().getFirstName() != null
                                    ? patient.getPerson().getFirstName()
                                    : "") + " "
                                    + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                            : "");
                        }
                        sampleData.put("patientName", patientName.trim());
                    }

                    // Get linked order information
                    List<org.openelisglobal.medlab.valueholder.OrderSampleLink> orderLinks = orderSampleLinkService
                            .getLinksBySampleId(Integer.parseInt(sample.getId()));
                    if (orderLinks != null && !orderLinks.isEmpty()) {
                        // Get the first order link (samples can have multiple orders, but show the
                        // first one)
                        org.openelisglobal.medlab.valueholder.OrderSampleLink firstLink = orderLinks.get(0);
                        if (firstLink.getElectronicOrderId() != null) {
                            ElectronicOrder order = electronicOrderService
                                    .get(String.valueOf(firstLink.getElectronicOrderId()));
                            if (order != null) {
                                sampleData.put("orderId", order.getId());
                                sampleData.put("orderLabNo", order.getExternalId());
                            }
                        }
                    }

                    samples.add(sampleData);
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForQC",
                            "Error processing sample item: " + collectionSample.getSampleItemId() + " - "
                                    + e.getMessage());
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return samples;
    }

    /**
     * @deprecated Transport & Packaging page has been removed from the Medical
     *             Laboratory workflow. This method is kept for backward
     *             compatibility but will always return an empty list or collected
     *             samples without transport packaging status.
     */
    @Override
    @Deprecated
    public List<Map<String, Object>> getSamplesForTransport(Integer entryId) {
        List<Map<String, Object>> samples = new ArrayList<>();

        if (entryId == null) {
            return samples;
        }

        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTransport",
                    "[DEPRECATED] Getting samples for transport packaging for entry: " + entryId);

            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTransport",
                        "Entry or notebook is null for entryId: " + entryId);
                return samples;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTransport",
                    "Found " + (pages != null ? pages.size() : 0) + " pages for notebook: " + notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return samples;
            }

            // Find the collection page (transport-packaging page no longer exists)
            Integer collectionPageId = null;
            for (NoteBookPage page : pages) {
                if ("sample-collection".equals(page.getPageId())) {
                    collectionPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTransport",
                            "Found collection page with id: " + collectionPageId);
                }
            }

            if (collectionPageId == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTransport",
                        "No sample-collection page found for notebook: " + notebook.getId());
                return samples;
            }

            // Get samples from the collection page that have COMPLETED status
            List<NotebookPageSample> collectionPageSamples = notebookPageSampleService.getByPageId(collectionPageId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTransport",
                    "Collection page " + collectionPageId + " has " + collectionPageSamples.size() + " samples");

            // Track samples we've already processed to avoid duplicates
            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();

            for (NotebookPageSample collectionSample : collectionPageSamples) {
                try {
                    // Only include samples that have been collected (COMPLETED on collection page)
                    if (collectionSample.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        continue;
                    }

                    SampleItem sampleItem = sampleItemService.get(collectionSample.getSampleItemId());
                    if (sampleItem == null || sampleItem.getSample() == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();

                    // Skip if we've already processed this sample
                    if (processedSampleIds.contains(sample.getId())) {
                        continue;
                    }
                    processedSampleIds.add(sample.getId());

                    // Transport packaging page has been removed - all collected samples are
                    // considered ready
                    String transportStatus = "PENDING";

                    LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTransport",
                            "[DEPRECATED] Sample " + sample.getAccessionNumber() + " (sampleItemId="
                                    + sampleItem.getId() + ") - Transport packaging page removed, status="
                                    + transportStatus);

                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("id", sampleItem.getId());
                    sampleData.put("sampleItemId", sampleItem.getId());
                    sampleData.put("labNo", sample.getAccessionNumber());
                    sampleData.put("accessionNumber", sample.getAccessionNumber());
                    sampleData.put("receivedDate", sample.getReceivedTimestamp());
                    sampleData.put("collectionDate", sample.getCollectionDate());
                    sampleData.put("transportStatus", transportStatus);
                    sampleData.put("pageStatus", "PENDING");

                    // Get sample type
                    if (sampleItem.getTypeOfSample() != null) {
                        sampleData.put("sampleType", sampleItem.getTypeOfSample().getLocalizedName());
                        sampleData.put("sampleTypeId", sampleItem.getTypeOfSample().getId());
                    }

                    // Get external ID if available
                    if (sampleItem.getExternalId() != null) {
                        sampleData.put("externalId", sampleItem.getExternalId());
                    }

                    // Get patient info
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    if (patient != null) {
                        sampleData.put("patientId", patient.getId());
                        String patientName = "";
                        if (patient.getPerson() != null) {
                            patientName = (patient.getPerson().getFirstName() != null
                                    ? patient.getPerson().getFirstName()
                                    : "") + " "
                                    + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                            : "");
                        }
                        sampleData.put("patientName", patientName.trim());
                    }

                    samples.add(sampleData);
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForTransport",
                            "Error processing sample item: " + collectionSample.getSampleItemId() + " - "
                                    + e.getMessage());
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return samples;
    }

    @Override
    @Transactional
    public Map<String, Object> recordQCDecision(String labNo, boolean accepted, String rejectionReason,
            Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Find the sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            if (sample == null) {
                result.put("success", false);
                result.put("error", "Sample not found: " + labNo);
                return result;
            }

            // Get sample items for this sample
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            if (sampleItems == null || sampleItems.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample items found for: " + labNo);
                return result;
            }

            for (SampleItem sampleItem : sampleItems) {
                if (!accepted) {
                    // Reject: Mark sample item as rejected
                    sampleItem.setRejected(true);
                    if (StringUtils.isNotBlank(rejectionReason)) {
                        sampleItem.setRejectReasonId(rejectionReason);
                    }
                } else {
                    // Accept: Ensure not marked as rejected
                    sampleItem.setRejected(false);
                    sampleItem.setRejectReasonId(null);
                }
                sampleItem.setSysUserId(sysUserId);
                sampleItemService.update(sampleItem);

                // Create or update NotebookPageSample for QC page if provided
                if (notebookPageId != null) {
                    // Check if there's already a page sample record for this page
                    List<NotebookPageSample> existingPageSamples = notebookPageSampleService
                            .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                    boolean foundPageSample = false;
                    for (NotebookPageSample pageSample : existingPageSamples) {
                        if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                            // Update existing record
                            pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                            pageSample.setSysUserId(sysUserId);
                            notebookPageSampleService.update(pageSample);
                            foundPageSample = true;
                            break;
                        }
                    }

                    // Create new record if not found
                    if (!foundPageSample) {
                        notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.COMPLETED);
                    }
                }
            }

            result.put("success", true);
            result.put("sampleId", sample.getId());
            result.put("labNo", labNo);
            result.put("accepted", accepted);
            result.put("message", accepted ? "Sample accepted" : "Sample rejected");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording QC decision: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> recordBulkQCDecision(List<String> labNumbers, boolean accepted, String rejectionReason,
            Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        List<String> failedLabNos = new ArrayList<>();

        try {
            for (String labNo : labNumbers) {
                Map<String, Object> singleResult = recordQCDecisionInternal(labNo, accepted, rejectionReason,
                        notebookPageId, sysUserId);
                if (Boolean.TRUE.equals(singleResult.get("success"))) {
                    successCount++;
                } else {
                    failedCount++;
                    failedLabNos.add(labNo);
                }
            }

            result.put("success", failedCount == 0);
            result.put("successCount", successCount);
            result.put("failedCount", failedCount);
            result.put("failedLabNos", failedLabNos);
            result.put("message", String.format("%d samples %s, %d failed", successCount,
                    accepted ? "accepted" : "rejected", failedCount));

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording bulk QC decision: " + e.getMessage());
        }

        return result;
    }

    // ==================== Sample Routing Methods ====================

    @Override
    public List<Map<String, Object>> getSamplesForRouting(Integer entryId) {
        List<Map<String, Object>> samples = new ArrayList<>();

        LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForRouting", "Called with entryId: " + entryId);

        if (entryId == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForRouting", "entryId is null");
            return samples;
        }

        try {
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForRouting",
                        "Entry or notebook is null for entryId: " + entryId);
                return samples;
            }

            // Get notebook pages for this entry
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return samples;
            }

            // Find the QC page and routing page
            Integer qcPageId = null;
            Integer routingPageId = null;

            for (NoteBookPage page : pages) {
                // Check for both standard and MedLab-specific page IDs
                String pageId = page.getPageId();
                if ("quality-check".equals(pageId) || "medlab-quality-check".equals(pageId)) {
                    qcPageId = page.getId();
                } else if ("sample-routing".equals(pageId) || "medlab-sample-routing".equals(pageId)) {
                    routingPageId = page.getId();
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForRouting",
                    "Found QC page ID: " + qcPageId + ", Routing page ID: " + routingPageId);

            if (qcPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForRouting",
                        "QC page not found for entry: " + entryId);
                return samples;
            }

            // Get samples from the QC page that have COMPLETED status (passed QC)
            List<NotebookPageSample> qcPageSamples = notebookPageSampleService.getByPageId(qcPageId);

            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForRouting",
                    "Found " + (qcPageSamples != null ? qcPageSamples.size() : 0) + " samples on QC page");

            for (NotebookPageSample qcSample : qcPageSamples) {
                try {
                    // Only include samples that have passed QC (COMPLETED on QC page)
                    if (qcSample.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        continue;
                    }

                    SampleItem sampleItem = sampleItemService.get(qcSample.getSampleItemId());
                    if (sampleItem == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();
                    if (sample == null) {
                        continue;
                    }

                    // Build sample data
                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("id", sampleItem.getId());
                    sampleData.put("sampleItemId", sampleItem.getId());
                    sampleData.put("labNo", sample.getAccessionNumber());
                    sampleData.put("accessionNumber", sample.getAccessionNumber());

                    // Include external ID to distinguish sample items with same accession
                    if (sampleItem.getExternalId() != null) {
                        sampleData.put("externalId", sampleItem.getExternalId());
                    }

                    // Get patient info
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    if (patient != null) {
                        String firstName = patient.getPerson() != null ? patient.getPerson().getFirstName() : "";
                        String lastName = patient.getPerson() != null ? patient.getPerson().getLastName() : "";
                        sampleData.put("patientName", (firstName + " " + lastName).trim());
                        sampleData.put("patientId", patient.getId());
                    }

                    // Get sample type
                    TypeOfSample typeOfSample = sampleItem.getTypeOfSample();
                    if (typeOfSample != null) {
                        sampleData.put("sampleType", typeOfSample.getDescription());
                    }

                    // Check routing status on routing page
                    String destinationType = null;
                    String qcStatus = "QC_ACCEPTED";

                    if (routingPageId != null) {
                        List<NotebookPageSample> routingPageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                        for (NotebookPageSample rps : routingPageSamples) {
                            if (rps.getNotebookPageId().equals(routingPageId)) {
                                Map<String, Object> routingData = rps.getData();
                                if (routingData != null) {
                                    destinationType = (String) routingData.get("destinationType");
                                }
                                break;
                            }
                        }
                    }

                    sampleData.put("destinationType", destinationType);
                    sampleData.put("qcStatus", qcStatus);
                    sampleData.put("data", qcSample.getData());

                    samples.add(sampleData);

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForRouting",
                            "Error processing sample: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return samples;
    }

    @Override
    public Map<String, Object> getRoutingSummary(Integer entryId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("internalAnalysis", 0);
        summary.put("externalLab", 0);
        summary.put("storage", 0);
        summary.put("unrouted", 0);
        summary.put("total", 0);

        if (entryId == null) {
            return summary;
        }

        try {
            List<Map<String, Object>> samples = getSamplesForRouting(entryId);
            int total = samples.size();
            int internalAnalysis = 0;
            int externalLab = 0;
            int storage = 0;
            int unrouted = 0;

            for (Map<String, Object> sample : samples) {
                String destinationType = (String) sample.get("destinationType");
                if (destinationType == null) {
                    unrouted++;
                } else if ("INTERNAL_ANALYSIS".equals(destinationType)) {
                    internalAnalysis++;
                } else if ("EXTERNAL_LAB".equals(destinationType)) {
                    externalLab++;
                } else if ("STORAGE".equals(destinationType)) {
                    storage++;
                } else {
                    unrouted++;
                }
            }

            summary.put("internalAnalysis", internalAnalysis);
            summary.put("externalLab", externalLab);
            summary.put("storage", storage);
            summary.put("unrouted", unrouted);
            summary.put("total", total);

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return summary;
    }

    @Override
    @Transactional
    public Map<String, Object> routeSamples(List<Integer> sampleIds, String destinationType, Integer notebookPageId,
            Map<String, Object> metadata, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleIds == null || sampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample IDs provided");
                return result;
            }

            if (StringUtils.isBlank(destinationType)) {
                result.put("success", false);
                result.put("error", "Destination type is required");
                return result;
            }

            int routedCount = 0;

            for (Integer sampleId : sampleIds) {
                try {
                    SampleItem sampleItem = sampleItemService.get(String.valueOf(sampleId));
                    if (sampleItem == null) {
                        continue;
                    }

                    // Build routing data
                    Map<String, Object> routingData = new HashMap<>();
                    routingData.put("destinationType", destinationType);
                    routingData.put("routedAt", java.time.LocalDateTime.now().toString());
                    routingData.put("routedBy", sysUserId);

                    // Add metadata if provided
                    if (metadata != null) {
                        routingData.putAll(metadata);
                    }

                    // Find or create routing page sample record
                    if (notebookPageId != null) {
                        List<NotebookPageSample> existingRecords = notebookPageSampleService
                                .getBySampleItemId(sampleId);
                        NotebookPageSample routingRecord = null;

                        for (NotebookPageSample nps : existingRecords) {
                            if (nps.getNotebookPageId().equals(notebookPageId)) {
                                routingRecord = nps;
                                break;
                            }
                        }

                        if (routingRecord != null) {
                            // Update existing record
                            routingRecord.setStatus(NotebookPageSample.Status.COMPLETED);
                            routingRecord.setData(routingData);
                            notebookPageSampleService.update(routingRecord);
                        } else {
                            // Create new record and then update with routing data
                            notebookPageSampleService.createPageSampleForPage(notebookPageId, sampleId,
                                    NotebookPageSample.Status.COMPLETED);
                            // Retrieve the newly created record to set data
                            List<NotebookPageSample> newRecords = notebookPageSampleService.getBySampleItemId(sampleId);
                            for (NotebookPageSample nps : newRecords) {
                                if (nps.getNotebookPageId().equals(notebookPageId)) {
                                    nps.setData(routingData);
                                    notebookPageSampleService.update(nps);
                                    break;
                                }
                            }
                        }
                    }

                    // If routing to STORAGE, sync with storage tables
                    if ("STORAGE".equals(destinationType) && metadata != null) {
                        try {
                            // Handle storageBoxId as either Integer or String
                            Object storageBoxIdObj = metadata.get("storageBoxId");
                            String locationId = storageBoxIdObj != null ? String.valueOf(storageBoxIdObj) : null;
                            String notes = (String) metadata.get("storageNotes");

                            // Default to 'box' level since storageBoxId refers to storage_box table
                            String locationType = "box";
                            if (metadata.get("locationType") != null) {
                                locationType = (String) metadata.get("locationType");
                            }

                            // Get well position from storageWellAssignments if available
                            String positionCoordinate = null;
                            @SuppressWarnings("unchecked")
                            Map<String, String> wellAssignments = (Map<String, String>) metadata
                                    .get("storageWellAssignments");
                            if (wellAssignments != null) {
                                // Well assignments can be keyed by sampleId as string
                                positionCoordinate = wellAssignments.get(String.valueOf(sampleId));
                            }
                            // Fallback to single positionCoordinate if not in map
                            if (positionCoordinate == null) {
                                positionCoordinate = (String) metadata.get("positionCoordinate");
                            }

                            if (locationId != null && !locationId.isEmpty()) {
                                // Validate storage location exists before calling service to avoid
                                // transaction rollback from nested @Transactional exception
                                Class<?> entityClass = getStorageEntityClass(locationType);
                                if (entityClass != null) {
                                    Object location = storageLocationService.get(Integer.parseInt(locationId),
                                            entityClass);
                                    if (location == null) {
                                        LogEvent.logWarn(this.getClass().getSimpleName(), "routeSamples",
                                                "Storage location not found: " + locationType + " " + locationId
                                                        + " - skipping storage assignment for sample " + sampleId);
                                    } else {
                                        sampleStorageService.assignSampleItemWithLocation(String.valueOf(sampleId),
                                                locationId, locationType, positionCoordinate, notes);
                                        LogEvent.logInfo(this.getClass().getSimpleName(), "routeSamples",
                                                "Assigned sample " + sampleId + " to storage location " + locationId
                                                        + " at position " + positionCoordinate);
                                    }
                                } else {
                                    LogEvent.logWarn(this.getClass().getSimpleName(), "routeSamples",
                                            "Unknown location type: " + locationType
                                                    + " - skipping storage assignment");
                                }
                            }
                        } catch (Exception storageEx) {
                            LogEvent.logWarn(this.getClass().getSimpleName(), "routeSamples",
                                    "Failed to assign sample " + sampleId + " to storage: " + storageEx.getMessage());
                            // Continue even if storage assignment fails - routing record is still saved
                        }
                    }

                    routedCount++;

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "routeSamples",
                            "Error routing sample " + sampleId + ": " + e.getMessage());
                }
            }

            result.put("success", routedCount > 0);
            result.put("routedCount", routedCount);

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error routing samples: " + e.getMessage());
        }

        return result;
    }

    /**
     * Internal method for QC decision logic, used by both single and bulk
     * operations.
     */
    private Map<String, Object> recordQCDecisionInternal(String labNo, boolean accepted, String rejectionReason,
            Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Find the sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            if (sample == null) {
                result.put("success", false);
                result.put("error", "Sample not found: " + labNo);
                return result;
            }

            // Get sample items for this sample
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            if (sampleItems == null || sampleItems.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample items found for: " + labNo);
                return result;
            }

            for (SampleItem sampleItem : sampleItems) {
                if (!accepted) {
                    // Reject: Mark sample item as rejected
                    sampleItem.setRejected(true);
                    if (StringUtils.isNotBlank(rejectionReason)) {
                        sampleItem.setRejectReasonId(rejectionReason);
                    }
                } else {
                    // Accept: Ensure not marked as rejected
                    sampleItem.setRejected(false);
                    sampleItem.setRejectReasonId(null);
                }
                sampleItem.setSysUserId(sysUserId);
                sampleItemService.update(sampleItem);

                // Create or update NotebookPageSample for QC page if provided
                if (notebookPageId != null) {
                    // Check if there's already a page sample record for this page
                    List<NotebookPageSample> existingPageSamples = notebookPageSampleService
                            .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                    boolean foundPageSample = false;
                    for (NotebookPageSample pageSample : existingPageSamples) {
                        if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                            // Update existing record
                            pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                            pageSample.setSysUserId(sysUserId);
                            notebookPageSampleService.update(pageSample);
                            foundPageSample = true;
                            break;
                        }
                    }

                    // Create new record if not found
                    if (!foundPageSample) {
                        notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.COMPLETED);
                    }
                }
            }

            result.put("success", true);
            result.put("labNo", labNo);
            result.put("accepted", accepted);

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording QC decision: " + e.getMessage());
        }

        return result;
    }

    // ==================== Result Entry Methods ====================

    @Override
    public List<Map<String, Object>> getSamplesForResultEntry(Integer entryId) {
        List<Map<String, Object>> samples = new ArrayList<>();

        if (entryId == null) {
            return samples;
        }

        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                    "Getting samples for result entry for entry: " + entryId);

            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return samples;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return samples;
            }

            // Find the QC page and Testing page
            Integer qcPageId = null;
            Integer testingPageId = null;
            for (NoteBookPage page : pages) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry", "Checking page: id="
                        + page.getId() + ", pageId=" + page.getPageId() + ", title=" + page.getTitle());
                if ("quality-check".equals(page.getPageId()) || "medlab-quality-check".equals(page.getPageId())) {
                    qcPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                            "Found QC page with id: " + qcPageId);
                } else if ("testing-analyzer".equals(page.getPageId())
                        || "medlab-testing-analyzer".equals(page.getPageId())) {
                    testingPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                            "Found Testing page with id: " + testingPageId);
                }
            }

            if (qcPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                        "No QC page found for notebook " + notebook.getId());
            }
            if (testingPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                        "No Testing page found for notebook " + notebook.getId());
            }

            // Store testingPageId in a final variable for use in lambda/inner class
            final Integer finalTestingPageId = testingPageId;

            // Track sample_items we've already processed (NOT samples!)
            // Each sample_item can have its own testing status, so we process them
            // individually
            // RULE: Any sample_item that has COMPLETED testing should appear in Result
            // Entry
            java.util.Set<String> processedSampleItemIds = new java.util.HashSet<>();

            // Look for sample_items that have COMPLETED testing
            for (NoteBookPage page : pages) {
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                        "Page " + page.getId() + " has " + pageSamples.size() + " page samples");

                for (NotebookPageSample pageSample : pageSamples) {
                    try {
                        LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                "Processing pageSample: sampleItemId=" + pageSample.getSampleItemId());

                        SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                        if (sampleItem == null || sampleItem.getSample() == null) {
                            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                    "SampleItem or Sample is null for sampleItemId=" + pageSample.getSampleItemId());
                            continue;
                        }

                        Sample sample = sampleItem.getSample();
                        LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                "SampleItem: id=" + sampleItem.getId() + ", Sample accession="
                                        + sample.getAccessionNumber() + ", rejected=" + sampleItem.isRejected());

                        // Skip if this sample_item already processed or rejected
                        if (processedSampleItemIds.contains(sampleItem.getId()) || sampleItem.isRejected()) {
                            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                    "Skipping sampleItem " + sampleItem.getId() + " (already processed or rejected)");
                            continue;
                        }
                        // NOTE: Don't add to processedSampleItemIds yet - only after all checks pass

                        // Check if sample passed QC (has COMPLETED status on QC page)
                        // For child samples, also check if their parent passed QC
                        boolean passedQC = false;
                        if (qcPageId != null) {
                            // First check this sample's QC status
                            List<NotebookPageSample> qcPageSamples = notebookPageSampleService
                                    .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry", "Found "
                                    + qcPageSamples.size() + " QC page samples for sampleItemId=" + sampleItem.getId());
                            for (NotebookPageSample qcPs : qcPageSamples) {
                                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                        "QC page sample: pageId=" + qcPs.getNotebookPageId() + ", status="
                                                + qcPs.getStatus() + ", qcPageId=" + qcPageId);
                                if (qcPageId.equals(qcPs.getNotebookPageId())
                                        && qcPs.getStatus() == NotebookPageSample.Status.COMPLETED) {
                                    passedQC = true;
                                    break;
                                }
                            }

                            // If not found, check parent sample's QC status (for child/aliquot samples)
                            if (!passedQC && sampleItem.getParentSampleItem() != null) {
                                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                        "Sample " + sampleItem.getId() + " has parent "
                                                + sampleItem.getParentSampleItem().getId()
                                                + ", checking parent QC status");
                                List<NotebookPageSample> parentQcSamples = notebookPageSampleService
                                        .getBySampleItemId(Integer.valueOf(sampleItem.getParentSampleItem().getId()));
                                for (NotebookPageSample parentQcPs : parentQcSamples) {
                                    if (qcPageId.equals(parentQcPs.getNotebookPageId())
                                            && parentQcPs.getStatus() == NotebookPageSample.Status.COMPLETED) {
                                        passedQC = true;
                                        LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                                "Parent sample " + sampleItem.getParentSampleItem().getId()
                                                        + " passed QC");
                                        break;
                                    }
                                }
                            }
                        }

                        LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                "Sample " + sample.getId() + " passedQC=" + passedQC);

                        if (!passedQC) {
                            continue;
                        }

                        // Check if sample completed testing phase (has COMPLETED status on
                        // testing-analyzer page)
                        // STRICT: Only allow samples that have explicitly been marked as COMPLETED
                        // on the Testing & Analyzer page. Samples without a testing record are NOT
                        // eligible.
                        boolean completedTesting = false;
                        if (finalTestingPageId != null) {
                            List<NotebookPageSample> allPageSamples = notebookPageSampleService
                                    .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                            for (NotebookPageSample testPs : allPageSamples) {
                                if (finalTestingPageId.equals(testPs.getNotebookPageId())
                                        && testPs.getStatus() == NotebookPageSample.Status.COMPLETED) {
                                    completedTesting = true;
                                    break;
                                }
                            }
                        }
                        // If no testing page exists in the workflow structure, samples cannot
                        // proceed to Result Entry (they must complete testing first)

                        LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                "Sample " + sample.getId() + " completedTesting=" + completedTesting);

                        if (!completedTesting) {
                            continue;
                        }

                        // Get analyses (tests) for this sample_item (NOT by sample ID!)
                        // Each sample_item should only show its OWN analyses
                        List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
                        if (analyses == null || analyses.isEmpty()) {
                            continue;
                        }

                        // Build test list with result status, units, reference ranges, and flags
                        List<Map<String, Object>> tests = new ArrayList<>();
                        int pendingCount = 0;
                        int enteredCount = 0;

                        for (Analysis analysis : analyses) {
                            Map<String, Object> testData = new HashMap<>();
                            Test test = analysis.getTest();
                            if (test == null) {
                                continue;
                            }

                            testData.put("analysisId", analysis.getId());
                            testData.put("testId", test.getId());
                            testData.put("testName", test.getLocalizedName());

                            // Get unit of measure
                            if (test.getUnitOfMeasure() != null) {
                                testData.put("unit", test.getUnitOfMeasure().getUnitOfMeasureName());
                            } else {
                                testData.put("unit", null);
                            }

                            // Get reference ranges from ResultLimit
                            ResultLimit resultLimit = resultLimitService.getResultLimitForAnalysis(analysis);
                            if (resultLimit != null) {
                                testData.put("lowNormal", resultLimit.getLowNormal());
                                testData.put("highNormal", resultLimit.getHighNormal());
                                testData.put("lowCritical", resultLimit.getLowCritical());
                                testData.put("highCritical", resultLimit.getHighCritical());
                                testData.put("lowValid", resultLimit.getLowValid());
                                testData.put("highValid", resultLimit.getHighValid());
                                // Build reference range string
                                String refRange = resultLimitService.getDisplayReferenceRange(resultLimit, "2", " - ");
                                testData.put("referenceRange", refRange);
                            }

                            // Check if result exists
                            List<Result> results = resultService.getResultsByAnalysis(analysis);
                            if (results != null && !results.isEmpty()) {
                                Result result = results.get(0);
                                testData.put("resultId", result.getId());
                                testData.put("resultValue", result.getValue());
                                testData.put("resultStatus", "ENTERED");
                                enteredCount++;

                                // Calculate flag based on result value and reference ranges
                                String flag = calculateResultFlag(result.getValue(), resultLimit);
                                testData.put("flag", flag);
                            } else {
                                testData.put("resultStatus", "PENDING");
                                testData.put("flag", null);
                                pendingCount++;
                            }

                            tests.add(testData);
                        }

                        // Determine overall sample status for result entry
                        String resultEntryStatus;
                        if (enteredCount == 0) {
                            resultEntryStatus = "PENDING";
                        } else if (pendingCount == 0) {
                            resultEntryStatus = "COMPLETED";
                        } else {
                            resultEntryStatus = "IN_PROGRESS";
                        }

                        Map<String, Object> sampleData = new HashMap<>();
                        sampleData.put("id", sample.getId());
                        sampleData.put("sampleItemId", sampleItem.getId());
                        sampleData.put("labNo", sample.getAccessionNumber());
                        // For aliquots, include externalId which may differ from parent's labNo
                        if (sampleItem.getExternalId() != null) {
                            sampleData.put("externalId", sampleItem.getExternalId());
                        }
                        // Flag if this is an aliquot (has a parent)
                        sampleData.put("isAliquot", sampleItem.getParentSampleItem() != null);
                        sampleData.put("receivedDate", sample.getReceivedTimestamp());
                        sampleData.put("resultEntryStatus", resultEntryStatus);
                        sampleData.put("tests", tests);
                        sampleData.put("pendingTests", pendingCount);
                        sampleData.put("enteredTests", enteredCount);
                        sampleData.put("totalTests", tests.size());

                        // Get sample type
                        if (sampleItem.getTypeOfSample() != null) {
                            sampleData.put("sampleType", sampleItem.getTypeOfSample().getLocalizedName());
                        }

                        // Get patient info
                        Patient patient = sampleHumanService.getPatientForSample(sample);
                        if (patient != null) {
                            sampleData.put("patientId", patient.getId());
                            String patientName = "";
                            if (patient.getPerson() != null) {
                                patientName = (patient.getPerson().getFirstName() != null
                                        ? patient.getPerson().getFirstName()
                                        : "") + " "
                                        + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                                : "");
                            }
                            sampleData.put("patientName", patientName.trim());
                        }

                        samples.add(sampleData);
                        // Mark this sample_item as processed AFTER successfully adding to results
                        processedSampleItemIds.add(sampleItem.getId());
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                                "Error processing sample item: " + pageSample.getSampleItemId() + " - "
                                        + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForResultEntry",
                "Returning " + samples.size() + " samples for result entry");

        return samples;
    }

    // ==================== Result Verification Methods ====================

    @Override
    public List<Map<String, Object>> getResultsForVerification(Integer entryId) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (entryId == null) {
            return results;
        }

        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForVerification",
                    "Getting results for verification for entry: " + entryId);

            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return results;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return results;
            }

            // Track samples we've already processed
            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();
            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            // Find the result-entry page ID - samples must be COMPLETED on this page
            Integer resultEntryPageId = null;
            for (NoteBookPage page : pages) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForVerification", "Checking page: id="
                        + page.getId() + ", pageId=" + page.getPageId() + ", title=" + page.getTitle());
                if ("result-entry".equals(page.getPageId()) || "medlab-result-entry".equals(page.getPageId())) {
                    resultEntryPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForVerification",
                            "Found Result Entry page with id: " + resultEntryPageId);
                    break;
                }
            }

            if (resultEntryPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getResultsForVerification",
                        "No Result Entry page found for notebook " + notebook.getId());
            }

            // Store resultEntryPageId in a final variable for use in lambda/inner class
            final Integer finalResultEntryPageId = resultEntryPageId;

            // Look for samples with results pending verification (TechnicalAcceptance
            // status)
            for (NoteBookPage page : pages) {
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                for (NotebookPageSample pageSample : pageSamples) {
                    try {
                        SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                        if (sampleItem == null || sampleItem.getSample() == null) {
                            continue;
                        }

                        Sample sample = sampleItem.getSample();

                        // Skip if already processed or rejected
                        if (processedSampleIds.contains(sample.getId()) || sampleItem.isRejected()) {
                            continue;
                        }
                        processedSampleIds.add(sample.getId());

                        // Check if sample has results ready for verification
                        // Primary check: Does sample have any results with TechnicalAcceptance,
                        // Finalized, or TechnicalRejected status?
                        // Secondary check: Is sample marked COMPLETED on result-entry page?
                        List<Analysis> analysesForCheck = analysisService.getAnalysesBySampleId(sample.getId());
                        boolean hasResultsReadyForVerification = false;
                        for (Analysis a : analysesForCheck) {
                            List<Result> checkResults = resultService.getResultsByAnalysis(a);
                            if (checkResults != null && !checkResults.isEmpty()) {
                                // Check if analysis has a verification-relevant status
                                if (statusService.matches(a.getStatusId(), AnalysisStatus.TechnicalAcceptance)
                                        || statusService.matches(a.getStatusId(), AnalysisStatus.Finalized)
                                        || statusService.matches(a.getStatusId(), AnalysisStatus.TechnicalRejected)) {
                                    hasResultsReadyForVerification = true;
                                    break;
                                }
                            }
                        }

                        // Also check page-based completion (fallback for workflows that use it)
                        boolean completedResultEntry = false;
                        if (finalResultEntryPageId != null) {
                            List<NotebookPageSample> allPageSamples = notebookPageSampleService
                                    .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                            for (NotebookPageSample rePs : allPageSamples) {
                                if (finalResultEntryPageId.equals(rePs.getNotebookPageId())
                                        && rePs.getStatus() == NotebookPageSample.Status.COMPLETED) {
                                    completedResultEntry = true;
                                    break;
                                }
                            }
                        }

                        // Allow samples if they have results ready for verification OR page is marked
                        // complete
                        boolean includeForVerification = hasResultsReadyForVerification || completedResultEntry;

                        LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForVerification",
                                "Sample " + sample.getId() + " hasResultsReadyForVerification="
                                        + hasResultsReadyForVerification + ", completedResultEntry="
                                        + completedResultEntry + ", includeForVerification=" + includeForVerification);

                        if (!includeForVerification) {
                            continue;
                        }

                        // Get analyses for this sample
                        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
                        if (analyses == null || analyses.isEmpty()) {
                            continue;
                        }

                        // Build test list with verification status
                        List<Map<String, Object>> tests = new ArrayList<>();
                        int pendingVerification = 0;
                        int verified = 0;

                        for (Analysis analysis : analyses) {
                            // Check if result exists
                            List<Result> analysisResults = resultService.getResultsByAnalysis(analysis);
                            if (analysisResults == null || analysisResults.isEmpty()) {
                                continue; // No result to verify
                            }

                            Result result = analysisResults.get(0);
                            Test test = analysis.getTest();
                            if (test == null) {
                                continue;
                            }

                            Map<String, Object> testData = new HashMap<>();
                            testData.put("analysisId", analysis.getId());
                            testData.put("testId", test.getId());
                            testData.put("testName", test.getLocalizedName());
                            testData.put("resultId", result.getId());
                            testData.put("resultValue", result.getValue());

                            // Check analysis status for verification state
                            String verificationStatus;
                            if (statusService.matches(analysis.getStatusId(), AnalysisStatus.TechnicalAcceptance)) {
                                verificationStatus = "PENDING_VERIFICATION";
                                pendingVerification++;
                            } else if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                                verificationStatus = "VERIFIED";
                                verified++;
                            } else if (statusService.matches(analysis.getStatusId(),
                                    AnalysisStatus.TechnicalRejected)) {
                                verificationStatus = "REJECTED";
                            } else {
                                continue; // Not ready for verification
                            }

                            testData.put("verificationStatus", verificationStatus);
                            tests.add(testData);
                        }

                        if (tests.isEmpty()) {
                            continue;
                        }

                        // Determine overall sample verification status
                        String sampleVerificationStatus;
                        if (pendingVerification > 0 && verified == 0) {
                            sampleVerificationStatus = "PENDING";
                        } else if (pendingVerification == 0 && verified > 0) {
                            sampleVerificationStatus = "VERIFIED";
                        } else if (pendingVerification > 0 && verified > 0) {
                            sampleVerificationStatus = "IN_PROGRESS";
                        } else {
                            sampleVerificationStatus = "PENDING";
                        }

                        Map<String, Object> sampleData = new HashMap<>();
                        sampleData.put("id", sample.getId());
                        sampleData.put("sampleItemId", sampleItem.getId());
                        sampleData.put("labNo", sample.getAccessionNumber());
                        sampleData.put("receivedDate", sample.getReceivedTimestamp());
                        sampleData.put("verificationStatus", sampleVerificationStatus);
                        sampleData.put("tests", tests);
                        sampleData.put("pendingVerification", pendingVerification);
                        sampleData.put("verified", verified);

                        // Get sample type
                        if (sampleItem.getTypeOfSample() != null) {
                            sampleData.put("sampleType", sampleItem.getTypeOfSample().getLocalizedName());
                        }

                        // Get patient info
                        Patient patient = sampleHumanService.getPatientForSample(sample);
                        if (patient != null) {
                            sampleData.put("patientId", patient.getId());
                            String patientName = "";
                            if (patient.getPerson() != null) {
                                patientName = (patient.getPerson().getFirstName() != null
                                        ? patient.getPerson().getFirstName()
                                        : "") + " "
                                        + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                                : "");
                            }
                            sampleData.put("patientName", patientName.trim());
                        }

                        results.add(sampleData);
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "getResultsForVerification",
                                "Error processing sample item: " + pageSample.getSampleItemId() + " - "
                                        + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return results;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyResult(String labNo, String testId, boolean approved, String comments,
            Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Find the sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            if (sample == null) {
                result.put("success", false);
                result.put("error", "Sample not found: " + labNo);
                return result;
            }

            // Find the analysis for this test
            List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
            Analysis targetAnalysis = null;
            for (Analysis analysis : analyses) {
                if (analysis.getTest() != null && testId.equals(analysis.getTest().getId())) {
                    targetAnalysis = analysis;
                    break;
                }
            }

            if (targetAnalysis == null) {
                result.put("success", false);
                result.put("error", "Test not found for sample: " + labNo);
                return result;
            }

            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            if (approved) {
                // Mark as Finalized (verified/approved)
                targetAnalysis.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
                targetAnalysis.setCompletedDate(DateUtil.getNowAsSqlDate());
            } else {
                // Mark as TechnicalRejected (needs re-entry)
                targetAnalysis.setStatusId(statusService.getStatusID(AnalysisStatus.TechnicalRejected));
            }

            targetAnalysis.setSysUserId(sysUserId);
            analysisService.update(targetAnalysis);

            // Update or create NotebookPageSample for verification page
            if (notebookPageId != null) {
                List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                for (SampleItem sampleItem : sampleItems) {
                    List<NotebookPageSample> existingPageSamples = notebookPageSampleService
                            .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                    boolean foundPageSample = false;
                    for (NotebookPageSample pageSample : existingPageSamples) {
                        if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                            pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                            pageSample.setSysUserId(sysUserId);
                            notebookPageSampleService.update(pageSample);
                            foundPageSample = true;
                            break;
                        }
                    }

                    if (!foundPageSample) {
                        notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.COMPLETED);
                    }
                }
            }

            result.put("success", true);
            result.put("labNo", labNo);
            result.put("testId", testId);
            result.put("approved", approved);
            result.put("message", approved ? "Result verified successfully" : "Result rejected");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error verifying result: " + e.getMessage());
        }

        return result;
    }

    // ==================== Reporting Methods ====================

    @Override
    public List<Map<String, Object>> getResultsForReporting(Integer entryId) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (entryId == null) {
            return results;
        }

        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForReporting",
                    "Getting results for reporting for entry: " + entryId);

            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return results;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return results;
            }

            // Track samples we've already processed
            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();
            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            // Find the result-verification or validation-reporting page ID
            // For MedLab workflow, we use validation-reporting (combined page)
            // For other workflows, we use separate result-verification page
            Integer verificationPageId = null;
            for (NoteBookPage page : pages) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForReporting", "Checking page: id="
                        + page.getId() + ", pageId=" + page.getPageId() + ", title=" + page.getTitle());
                if ("result-verification".equals(page.getPageId())) {
                    verificationPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForReporting",
                            "Found Result Verification page with id: " + verificationPageId);
                    break;
                } else if ("validation-reporting".equals(page.getPageId())
                        || "medlab-validation-reporting".equals(page.getPageId())) {
                    // For validation-reporting (combined page), verification happens within the
                    // same page
                    verificationPageId = page.getId();
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForReporting",
                            "Found Validation-Reporting combined page with id: " + verificationPageId);
                    break;
                }
            }

            if (verificationPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getResultsForReporting",
                        "No Result Verification or Validation-Reporting page found for notebook " + notebook.getId());
            }

            // Store verificationPageId in a final variable for use in lambda/inner class
            final Integer finalVerificationPageId = verificationPageId;

            // Look for samples with finalized results
            for (NoteBookPage page : pages) {
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                for (NotebookPageSample pageSample : pageSamples) {
                    try {
                        SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                        if (sampleItem == null || sampleItem.getSample() == null) {
                            continue;
                        }

                        Sample sample = sampleItem.getSample();

                        // Skip if already processed or rejected
                        if (processedSampleIds.contains(sample.getId()) || sampleItem.isRejected()) {
                            continue;
                        }
                        processedSampleIds.add(sample.getId());

                        // Check if sample has finalized results ready for reporting
                        // Primary check: Does sample have any results with Finalized status?
                        // Secondary check: Is sample marked COMPLETED on verification page?
                        List<Analysis> analysesForReportCheck = analysisService.getAnalysesBySampleId(sample.getId());
                        boolean hasFinalizedResults = false;
                        for (Analysis a : analysesForReportCheck) {
                            if (statusService.matches(a.getStatusId(), AnalysisStatus.Finalized)) {
                                List<Result> checkResults = resultService.getResultsByAnalysis(a);
                                if (checkResults != null && !checkResults.isEmpty()) {
                                    hasFinalizedResults = true;
                                    break;
                                }
                            }
                        }

                        // Also check page-based completion (fallback for workflows that use it)
                        boolean completedVerificationPage = false;
                        if (finalVerificationPageId != null) {
                            List<NotebookPageSample> allPageSamples = notebookPageSampleService
                                    .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                            for (NotebookPageSample rvPs : allPageSamples) {
                                if (finalVerificationPageId.equals(rvPs.getNotebookPageId())
                                        && rvPs.getStatus() == NotebookPageSample.Status.COMPLETED) {
                                    completedVerificationPage = true;
                                    break;
                                }
                            }
                        }

                        // Allow samples if they have finalized results OR verification page is marked
                        // complete
                        boolean includeForReporting = hasFinalizedResults || completedVerificationPage;

                        LogEvent.logInfo(this.getClass().getSimpleName(), "getResultsForReporting",
                                "Sample " + sample.getId() + " hasFinalizedResults=" + hasFinalizedResults
                                        + ", completedVerificationPage=" + completedVerificationPage
                                        + ", includeForReporting=" + includeForReporting);

                        if (!includeForReporting) {
                            continue;
                        }

                        // Get analyses for this sample
                        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
                        if (analyses == null || analyses.isEmpty()) {
                            continue;
                        }

                        // Check if all results are finalized (verified)
                        List<Map<String, Object>> tests = new ArrayList<>();
                        int finalizedCount = 0;
                        int totalWithResults = 0;
                        boolean isReported = false;

                        for (Analysis analysis : analyses) {
                            // Check if result exists
                            List<Result> analysisResults = resultService.getResultsByAnalysis(analysis);
                            if (analysisResults == null || analysisResults.isEmpty()) {
                                continue;
                            }

                            totalWithResults++;
                            Result resultObj = analysisResults.get(0);
                            Test test = analysis.getTest();
                            if (test == null) {
                                continue;
                            }

                            // Only include finalized results
                            if (!statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                                continue;
                            }

                            finalizedCount++;

                            Map<String, Object> testData = new HashMap<>();
                            testData.put("analysisId", analysis.getId());
                            testData.put("testId", test.getId());
                            testData.put("testName", test.getLocalizedName());
                            testData.put("resultId", resultObj.getId());
                            testData.put("resultValue", resultObj.getValue());

                            // Check if printed/released
                            if (analysis.getPrintedDate() != null || analysis.getReleasedDate() != null) {
                                testData.put("reportStatus", "REPORTED");
                                isReported = true;
                            } else {
                                testData.put("reportStatus", "PENDING");
                            }

                            tests.add(testData);
                        }

                        if (tests.isEmpty()) {
                            continue;
                        }

                        // Determine reporting status
                        String reportingStatus;
                        if (isReported) {
                            reportingStatus = "REPORTED";
                        } else if (finalizedCount == totalWithResults && totalWithResults > 0) {
                            reportingStatus = "READY";
                        } else {
                            reportingStatus = "PENDING_VERIFICATION";
                        }

                        Map<String, Object> sampleData = new HashMap<>();
                        sampleData.put("id", sample.getId());
                        sampleData.put("sampleItemId", sampleItem.getId());
                        sampleData.put("labNo", sample.getAccessionNumber());
                        sampleData.put("receivedDate", sample.getReceivedTimestamp());
                        sampleData.put("reportingStatus", reportingStatus);
                        sampleData.put("tests", tests);
                        sampleData.put("totalTests", tests.size());

                        // Get sample type
                        if (sampleItem.getTypeOfSample() != null) {
                            sampleData.put("sampleType", sampleItem.getTypeOfSample().getLocalizedName());
                        }

                        // Get patient info
                        Patient patient = sampleHumanService.getPatientForSample(sample);
                        if (patient != null) {
                            sampleData.put("patientId", patient.getId());
                            String patientName = "";
                            if (patient.getPerson() != null) {
                                patientName = (patient.getPerson().getFirstName() != null
                                        ? patient.getPerson().getFirstName()
                                        : "") + " "
                                        + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                                : "");
                            }
                            sampleData.put("patientName", patientName.trim());
                        }

                        results.add(sampleData);
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "getResultsForReporting",
                                "Error processing sample item: " + pageSample.getSampleItemId() + " - "
                                        + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return results;
    }

    @Override
    @Transactional
    public Map<String, Object> markResultReported(String labNo, String deliveryMethod, String recipient,
            Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Find the sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            if (sample == null) {
                result.put("success", false);
                result.put("error", "Sample not found: " + labNo);
                return result;
            }

            // Get all analyses for this sample
            List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            int reportedCount = 0;
            for (Analysis analysis : analyses) {
                // Only mark finalized results as reported
                if (statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                    analysis.setPrintedDate(DateUtil.getNowAsSqlDate());
                    analysis.setReleasedDate(DateUtil.getNowAsSqlDate());
                    analysis.setSysUserId(sysUserId);
                    analysisService.update(analysis);
                    reportedCount++;
                }
            }

            // Update or create NotebookPageSample for reporting page
            if (notebookPageId != null) {
                List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                for (SampleItem sampleItem : sampleItems) {
                    List<NotebookPageSample> existingPageSamples = notebookPageSampleService
                            .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                    boolean foundPageSample = false;
                    for (NotebookPageSample pageSample : existingPageSamples) {
                        if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                            pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                            pageSample.setSysUserId(sysUserId);
                            notebookPageSampleService.update(pageSample);
                            foundPageSample = true;
                            break;
                        }
                    }

                    if (!foundPageSample) {
                        notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.COMPLETED);
                    }
                }
            }

            result.put("success", true);
            result.put("labNo", labNo);
            result.put("deliveryMethod", deliveryMethod);
            result.put("recipient", recipient);
            result.put("reportedCount", reportedCount);
            result.put("message", "Results marked as reported");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error marking results as reported: " + e.getMessage());
        }

        return result;
    }

    // ==================== Storage Methods ====================

    @Override
    public List<Map<String, Object>> getSamplesForStorage(Integer entryId) {
        List<Map<String, Object>> samples = new ArrayList<>();

        if (entryId == null) {
            return samples;
        }

        try {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForStorage",
                    "Getting samples for storage for entry: " + entryId);

            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForStorage",
                        "Entry or notebook is null for entryId: " + entryId);
                return samples;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return samples;
            }

            // Find the QC page (samples must pass QC before storage) and storage page
            Integer qcPageId = null;
            Integer storagePageId = null;
            for (NoteBookPage page : pages) {
                if ("quality-check".equals(page.getPageId()) || "medlab-quality-check".equals(page.getPageId())) {
                    qcPageId = page.getId();
                } else if ("sample-storage".equals(page.getPageId())) {
                    storagePageId = page.getId();
                }
            }

            if (qcPageId == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForStorage",
                        "No quality-check page found for notebook: " + notebook.getId());
                return samples;
            }

            // Get samples from the QC page that have COMPLETED status (passed QC)
            List<NotebookPageSample> qcPageSamples = notebookPageSampleService.getByPageId(qcPageId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForStorage",
                    "QC page " + qcPageId + " has " + qcPageSamples.size() + " samples");

            // Track samples we've already processed
            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();

            for (NotebookPageSample qcSample : qcPageSamples) {
                try {
                    // Only include samples that have passed QC (COMPLETED on QC page)
                    if (qcSample.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        continue;
                    }

                    SampleItem sampleItem = sampleItemService.get(qcSample.getSampleItemId());
                    if (sampleItem == null || sampleItem.getSample() == null) {
                        continue;
                    }

                    // Skip rejected samples
                    if (sampleItem.isRejected()) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();

                    // Skip if we've already processed this sample
                    if (processedSampleIds.contains(sample.getId())) {
                        continue;
                    }
                    processedSampleIds.add(sample.getId());

                    // Determine storage status
                    String storageStatus = "PENDING";
                    String storageLocation = null;
                    String storageCondition = null;
                    String wellCoordinate = null;
                    Integer boxId = null;
                    String retentionExpiry = null;

                    if (storagePageId != null) {
                        List<NotebookPageSample> storagePageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                        for (NotebookPageSample sps : storagePageSamples) {
                            if (storagePageId.equals(sps.getNotebookPageId())) {
                                if (sps.getStatus() == NotebookPageSample.Status.COMPLETED) {
                                    storageStatus = "ASSIGNED";
                                } else if (sps.getStatus() == NotebookPageSample.Status.IN_PROGRESS) {
                                    storageStatus = "IN_PROGRESS";
                                }

                                Map<String, Object> data = sps.getData();
                                if (data != null) {
                                    storageLocation = (String) data.get("storageLocation");
                                    storageCondition = (String) data.get("storageCondition");
                                    wellCoordinate = (String) data.get("wellCoordinate");
                                    boxId = data.get("boxId") != null ? ((Number) data.get("boxId")).intValue() : null;
                                    retentionExpiry = (String) data.get("retentionExpiry");
                                }
                                break;
                            }
                        }
                    }

                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("id", sample.getId());
                    sampleData.put("sampleItemId", sampleItem.getId());
                    sampleData.put("labNo", sample.getAccessionNumber());
                    sampleData.put("accessionNumber", sample.getAccessionNumber());
                    sampleData.put("receivedDate", sample.getReceivedTimestamp());
                    sampleData.put("collectionDate", sample.getCollectionDate());
                    sampleData.put("pageStatus", storageStatus);
                    sampleData.put("storageLocation", storageLocation);
                    sampleData.put("storageCondition", storageCondition);
                    sampleData.put("wellCoordinate", wellCoordinate);
                    sampleData.put("boxId", boxId);
                    sampleData.put("retentionExpiry", retentionExpiry);

                    // Get sample type
                    if (sampleItem.getTypeOfSample() != null) {
                        sampleData.put("sampleType", sampleItem.getTypeOfSample().getLocalizedName());
                        sampleData.put("sampleTypeId", sampleItem.getTypeOfSample().getId());
                    }

                    // Get patient info
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    if (patient != null) {
                        sampleData.put("patientId", patient.getId());
                        String patientName = "";
                        if (patient.getPerson() != null) {
                            patientName = (patient.getPerson().getFirstName() != null
                                    ? patient.getPerson().getFirstName()
                                    : "") + " "
                                    + (patient.getPerson().getLastName() != null ? patient.getPerson().getLastName()
                                            : "");
                        }
                        sampleData.put("patientName", patientName.trim());
                    }

                    samples.add(sampleData);

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForStorage",
                            "Error processing sample item: " + qcSample.getSampleItemId() + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return samples;
    }

    @Override
    @Transactional
    public Map<String, Object> assignSamplesToStorage(List<Integer> sampleIds, Integer boxId, String boxLabel,
            Map<Integer, String> wellAssignments, String condition, Integer retentionYears, String cryovialId,
            Integer notebookPageId, String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleIds == null || sampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No samples provided");
                return result;
            }

            int assignedCount = 0;

            // Calculate retention expiry date
            java.time.LocalDate expiryDate = java.time.LocalDate.now().plusYears(retentionYears);
            String retentionExpiry = expiryDate.toString();

            for (Integer sampleId : sampleIds) {
                try {
                    SampleItem sampleItem = sampleItemService.get(String.valueOf(sampleId));
                    if (sampleItem == null) {
                        continue;
                    }

                    String wellCoord = wellAssignments.get(sampleId);
                    String storageLocation = boxLabel != null ? boxLabel + " - " + wellCoord : wellCoord;

                    // Save storage data to NotebookPageSample
                    Map<String, Object> storageData = new HashMap<>();
                    storageData.put("boxId", boxId);
                    storageData.put("boxLabel", boxLabel);
                    storageData.put("wellCoordinate", wellCoord);
                    storageData.put("storageLocation", storageLocation);
                    storageData.put("storageCondition", condition);
                    storageData.put("retentionYears", retentionYears);
                    storageData.put("retentionExpiry", retentionExpiry);
                    storageData.put("cryovialId", cryovialId);
                    storageData.put("assignedAt", java.time.LocalDateTime.now().toString());

                    // Update or create notebook page sample
                    if (notebookPageId != null) {
                        List<NotebookPageSample> existingPageSamples = notebookPageSampleService
                                .getBySampleItemId(sampleId);
                        boolean foundPageSample = false;

                        for (NotebookPageSample pageSample : existingPageSamples) {
                            if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                                pageSample.setData(storageData);
                                pageSample.setSysUserId(sysUserId);
                                notebookPageSampleService.update(pageSample);
                                foundPageSample = true;
                                break;
                            }
                        }

                        if (!foundPageSample) {
                            notebookPageSampleService.createPageSampleForPage(notebookPageId, sampleId,
                                    NotebookPageSample.Status.COMPLETED);
                            // Get the newly created page sample to update with data
                            List<NotebookPageSample> newPageSamples = notebookPageSampleService
                                    .getBySampleItemId(sampleId);
                            for (NotebookPageSample nps : newPageSamples) {
                                if (nps.getNotebookPageId().equals(notebookPageId)) {
                                    nps.setData(storageData);
                                    notebookPageSampleService.update(nps);
                                    break;
                                }
                            }
                        }
                    }

                    assignedCount++;

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "assignSamplesToStorage",
                            "Error assigning sample " + sampleId + " to storage: " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("assignedCount", assignedCount);
            result.put("message", "Successfully assigned " + assignedCount + " samples to storage");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error assigning samples to storage: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> recordEnvironmentalReading(Integer entryId, Integer deviceId, Double temperatureReading,
            String readingTime, String readingPeriod, String recordedBy, Boolean alarmTriggered, String notes,
            String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (entryId == null) {
                result.put("success", false);
                result.put("error", "Entry ID is required");
                return result;
            }

            // Store the reading in notebook entry notes as JSON
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null) {
                result.put("success", false);
                result.put("error", "Entry not found");
                return result;
            }

            // Parse existing notes as JSON or create new structure
            String existingNotes = entry.getNotes();
            List<Map<String, Object>> readings = new ArrayList<>();

            if (existingNotes != null && existingNotes.startsWith("{\"environmentalReadings\":")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> notesData = mapper.readValue(existingNotes, Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> existingReadings = (List<Map<String, Object>>) notesData
                            .get("environmentalReadings");
                    if (existingReadings != null) {
                        readings.addAll(existingReadings);
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordEnvironmentalReading",
                            "Could not parse existing notes as JSON: " + e.getMessage());
                }
            }

            // Add new reading
            Map<String, Object> reading = new HashMap<>();
            reading.put("id", java.util.UUID.randomUUID().toString());
            reading.put("deviceId", deviceId);
            reading.put("temperatureReading", temperatureReading);
            reading.put("readingTime", readingTime);
            reading.put("readingPeriod", readingPeriod);
            reading.put("recordedBy", recordedBy);
            reading.put("alarmTriggered", alarmTriggered);
            reading.put("notes", notes);
            reading.put("recordedAt", java.time.LocalDateTime.now().toString());

            readings.add(reading);

            // Save back as JSON in notes
            Map<String, Object> notesData = new HashMap<>();
            notesData.put("environmentalReadings", readings);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            entry.setNotes(mapper.writeValueAsString(notesData));
            notebookEntryService.update(entry);

            result.put("success", true);
            result.put("readingId", reading.get("id"));
            result.put("message", "Environmental reading recorded successfully");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording environmental reading: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getEnvironmentalReadings(Integer entryId) {
        List<Map<String, Object>> readings = new ArrayList<>();

        if (entryId == null) {
            return readings;
        }

        try {
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null) {
                return readings;
            }

            String notes = entry.getNotes();
            if (notes != null && notes.startsWith("{\"environmentalReadings\":")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> notesData = mapper.readValue(notes, Map.class);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> storedReadings = (List<Map<String, Object>>) notesData
                            .get("environmentalReadings");
                    if (storedReadings != null) {
                        readings.addAll(storedReadings);
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getEnvironmentalReadings",
                            "Could not parse notes as JSON: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return readings;
    }

    // ==================== Sample Processing Methods ====================

    @Override
    public List<Map<String, Object>> getSamplesForProcessing(Integer entryId) {
        List<Map<String, Object>> samples = new ArrayList<>();

        if (entryId == null) {
            return samples;
        }

        try {
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForProcessing",
                        "Entry or notebook not found for ID: " + entryId);
                return samples;
            }

            // Get notebook pages for this entry
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return samples;
            }

            // Find the QC page, routing page, and processing page
            Integer qcPageId = null;
            Integer routingPageId = null;
            Integer processingPageId = null;

            for (NoteBookPage page : pages) {
                if ("quality-check".equals(page.getPageId()) || "medlab-quality-check".equals(page.getPageId())) {
                    qcPageId = page.getId();
                } else if ("sample-routing".equals(page.getPageId())
                        || "medlab-sample-routing".equals(page.getPageId())) {
                    routingPageId = page.getId();
                } else if ("sample-processing".equals(page.getPageId())
                        || "medlab-sample-processing".equals(page.getPageId())) {
                    processingPageId = page.getId();
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForProcessing", "QC page ID: " + qcPageId
                    + ", Routing page ID: " + routingPageId + ", Processing page ID: " + processingPageId);

            if (qcPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForProcessing",
                        "QC page not found for entry: " + entryId);
                return samples;
            }

            // Get samples from the QC page that have COMPLETED status (passed QC)
            List<NotebookPageSample> qcPageSamples = notebookPageSampleService.getByPageId(qcPageId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForProcessing",
                    "Found " + qcPageSamples.size() + " samples on QC page");

            for (NotebookPageSample qcSample : qcPageSamples) {
                try {
                    // Only include samples that have passed QC (COMPLETED on QC page)
                    if (qcSample.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        continue;
                    }

                    // Check routing destination - only include INTERNAL_ANALYSIS samples
                    if (routingPageId != null) {
                        List<NotebookPageSample> routingRecords = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(qcSample.getSampleItemId()));
                        boolean isInternalAnalysis = false;
                        for (NotebookPageSample rps : routingRecords) {
                            if (rps.getNotebookPageId().equals(routingPageId)) {
                                Map<String, Object> routingData = rps.getData();
                                if (routingData != null
                                        && "INTERNAL_ANALYSIS".equals(routingData.get("destinationType"))) {
                                    isInternalAnalysis = true;
                                }
                                break;
                            }
                        }
                        // Skip samples not routed to INTERNAL_ANALYSIS
                        if (!isInternalAnalysis) {
                            continue;
                        }
                    }

                    SampleItem sampleItem = sampleItemService.get(qcSample.getSampleItemId());
                    if (sampleItem == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();
                    if (sample == null) {
                        continue;
                    }

                    // Build sample data
                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("sampleItemId", sampleItem.getId());
                    sampleData.put("labNo", sample.getAccessionNumber());
                    sampleData.put("accessionNumber", sample.getAccessionNumber());

                    // Get patient info
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    if (patient != null) {
                        String firstName = patient.getPerson() != null ? patient.getPerson().getFirstName() : "";
                        String lastName = patient.getPerson() != null ? patient.getPerson().getLastName() : "";
                        sampleData.put("patientName", (firstName + " " + lastName).trim());
                        sampleData.put("patientId", patient.getId());
                    }

                    // Get sample type
                    TypeOfSample typeOfSample = sampleItem.getTypeOfSample();
                    if (typeOfSample != null) {
                        sampleData.put("sampleType", typeOfSample.getDescription());
                        sampleData.put("sampleTypeId", typeOfSample.getId());
                    }

                    // Check if sample has been processed (has entry on processing page)
                    String processingStatus = "PENDING";
                    String processingType = null;
                    String derivedMaterial = null;
                    Boolean isBioequivalence = false;
                    Boolean transferToBioanalytical = false;

                    if (processingPageId != null) {
                        List<NotebookPageSample> procPageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                        for (NotebookPageSample pps : procPageSamples) {
                            if (pps.getNotebookPageId().equals(processingPageId)) {
                                processingStatus = pps.getStatus() == NotebookPageSample.Status.COMPLETED ? "COMPLETED"
                                        : pps.getStatus() == NotebookPageSample.Status.PENDING ? "PENDING"
                                                : "IN_PROGRESS";

                                // Get processing data from the page sample
                                Map<String, Object> procData = pps.getData();
                                if (procData != null) {
                                    processingType = (String) procData.get("processingType");
                                    derivedMaterial = (String) procData.get("derivedMaterial");
                                    isBioequivalence = Boolean.TRUE.equals(procData.get("isBioequivalence"));
                                    transferToBioanalytical = Boolean.TRUE
                                            .equals(procData.get("transferToBioanalytical"));
                                }
                                break;
                            }
                        }
                    }

                    sampleData.put("pageStatus", processingStatus);
                    sampleData.put("processingType", processingType);
                    sampleData.put("derivedMaterial", derivedMaterial);
                    sampleData.put("isBioequivalence", isBioequivalence);
                    sampleData.put("transferToBioanalytical", transferToBioanalytical);

                    // Check for child samples (aliquots)
                    List<SampleItem> childSamples = sampleItemService.getChildSamples(sampleItem);
                    sampleData.put("hasChildren", childSamples != null && !childSamples.isEmpty());
                    sampleData.put("childAliquotCount", childSamples != null ? childSamples.size() : 0);
                    sampleData.put("externalId", sampleItem.getExternalId());

                    samples.add(sampleData);

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForProcessing",
                            "Error processing sample: " + e.getMessage());
                }
            }

            // Second pass: Add child samples (aliquots) to the list with hierarchy
            // information
            // Make a copy of samples list to avoid concurrent modification
            List<Map<String, Object>> parentSamples = new ArrayList<>(samples);
            for (Map<String, Object> parentData : parentSamples) {
                String parentSampleItemId = (String) parentData.get("sampleItemId");
                if (parentSampleItemId == null) {
                    continue;
                }

                try {
                    SampleItem parentSampleItem = sampleItemService.get(parentSampleItemId);
                    if (parentSampleItem == null) {
                        continue;
                    }

                    // Get child samples for this parent
                    List<SampleItem> childSamples = sampleItemService.getChildSamples(parentSampleItem);
                    if (childSamples == null || childSamples.isEmpty()) {
                        continue;
                    }

                    // Add each child sample to the list
                    for (SampleItem childSampleItem : childSamples) {
                        try {
                            Sample childSample = childSampleItem.getSample();
                            if (childSample == null) {
                                continue;
                            }

                            // Build child sample data
                            Map<String, Object> childData = new HashMap<>();
                            childData.put("sampleItemId", childSampleItem.getId());
                            // For aliquots/children, use externalId as their unique labNo
                            // (children share the parent's Sample, so accessionNumber is not unique)
                            String childLabNo = childSampleItem.getExternalId() != null
                                    ? childSampleItem.getExternalId()
                                    : childSample.getAccessionNumber();
                            childData.put("labNo", childLabNo);
                            childData.put("accessionNumber", childSample.getAccessionNumber());
                            childData.put("externalId", childSampleItem.getExternalId());

                            // Copy patient info from parent
                            childData.put("patientName", parentData.get("patientName"));
                            childData.put("patientId", parentData.get("patientId"));

                            // Get sample type for child
                            TypeOfSample childTypeOfSample = childSampleItem.getTypeOfSample();
                            if (childTypeOfSample != null) {
                                childData.put("sampleType", childTypeOfSample.getDescription());
                                childData.put("sampleTypeId", childTypeOfSample.getId());
                            }

                            // Check if child has been processed on processing page
                            String childProcessingStatus = "PENDING";
                            String childProcessingType = null;
                            String childDerivedMaterial = null;
                            Boolean childIsBioequivalence = false;
                            Boolean childTransferToBioanalytical = false;
                            String childContainerType = null;

                            if (processingPageId != null) {
                                List<NotebookPageSample> childProcPageSamples = notebookPageSampleService
                                        .getBySampleItemId(Integer.valueOf(childSampleItem.getId()));
                                for (NotebookPageSample pps : childProcPageSamples) {
                                    if (pps.getNotebookPageId().equals(processingPageId)) {
                                        childProcessingStatus = pps.getStatus() == NotebookPageSample.Status.COMPLETED
                                                ? "COMPLETED"
                                                : pps.getStatus() == NotebookPageSample.Status.PENDING ? "PENDING"
                                                        : "IN_PROGRESS";

                                        // Get processing data for child
                                        Map<String, Object> procData = pps.getData();
                                        if (procData != null) {
                                            childProcessingType = (String) procData.get("processingType");
                                            childDerivedMaterial = (String) procData.get("derivedMaterial");
                                            childIsBioequivalence = Boolean.TRUE
                                                    .equals(procData.get("isBioequivalence"));
                                            childTransferToBioanalytical = Boolean.TRUE
                                                    .equals(procData.get("transferToBioanalytical"));
                                            childContainerType = (String) procData.get("containerType");
                                        }
                                        break;
                                    }
                                }
                            }

                            childData.put("pageStatus", childProcessingStatus);
                            childData.put("processingType", childProcessingType);
                            childData.put("derivedMaterial", childDerivedMaterial);
                            childData.put("isBioequivalence", childIsBioequivalence);
                            childData.put("transferToBioanalytical", childTransferToBioanalytical);
                            childData.put("containerType", childContainerType);

                            // Child samples don't have their own children (1-level hierarchy only)
                            childData.put("hasChildren", false);
                            childData.put("childAliquotCount", 0);

                            // Hierarchy information for child samples
                            childData.put("isAliquot", true);
                            childData.put("nestingLevel", 1);
                            childData.put("parentSampleItemId", parentSampleItemId);
                            childData.put("parentExternalId", parentData.get("externalId"));

                            samples.add(childData);

                        } catch (Exception e) {
                            LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForProcessing",
                                    "Error processing child sample: " + e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForProcessing",
                            "Error loading children for parent " + parentSampleItemId + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return samples;
    }

    @Override
    @Transactional
    public Map<String, Object> recordProcessing(List<Integer> sampleIds, String processingType, String derivedMaterial,
            String notes, Boolean isBioequivalence, Boolean transferToBioanalytical, Integer notebookPageId,
            String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleIds == null || sampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample IDs provided");
                return result;
            }

            int processedCount = 0;

            for (Integer sampleId : sampleIds) {
                try {
                    SampleItem sampleItem = sampleItemService.get(String.valueOf(sampleId));
                    if (sampleItem == null) {
                        continue;
                    }

                    // Build processing data
                    Map<String, Object> processingData = new HashMap<>();
                    processingData.put("processingType", processingType);
                    processingData.put("derivedMaterial", derivedMaterial);
                    processingData.put("notes", notes);
                    processingData.put("isBioequivalence", isBioequivalence);
                    processingData.put("transferToBioanalytical", transferToBioanalytical);
                    processingData.put("processedAt", java.time.LocalDateTime.now().toString());
                    processingData.put("processedBy", sysUserId);

                    // Find the processing page ID if not provided
                    Integer effectivePageId = notebookPageId;
                    if (effectivePageId == null) {
                        // Try to find the processing page from the sample's notebook entry
                        effectivePageId = findProcessingPageForSampleItem(sampleItem);
                    }

                    // Find or create processing page sample record
                    if (effectivePageId != null) {
                        List<NotebookPageSample> existingRecords = notebookPageSampleService
                                .getBySampleItemId(sampleId);
                        NotebookPageSample processingRecord = null;

                        for (NotebookPageSample nps : existingRecords) {
                            if (nps.getNotebookPageId().equals(effectivePageId)) {
                                processingRecord = nps;
                                break;
                            }
                        }

                        if (processingRecord != null) {
                            // Update existing record
                            processingRecord.setStatus(NotebookPageSample.Status.COMPLETED);
                            processingRecord.setData(processingData);
                            notebookPageSampleService.update(processingRecord);
                        } else {
                            // Create new record
                            notebookPageSampleService.createPageSampleForPage(effectivePageId, sampleId,
                                    NotebookPageSample.Status.COMPLETED);

                            // Get the newly created record and update with processing data
                            List<NotebookPageSample> newPageSamples = notebookPageSampleService
                                    .getBySampleItemId(sampleId);
                            for (NotebookPageSample nps : newPageSamples) {
                                if (nps.getNotebookPageId().equals(effectivePageId)) {
                                    nps.setData(processingData);
                                    notebookPageSampleService.update(nps);
                                    break;
                                }
                            }
                        }
                    }

                    processedCount++;

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "recordProcessing",
                            "Error processing sample " + sampleId + ": " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("processedCount", processedCount);
            result.put("message", "Successfully recorded processing for " + processedCount + " sample(s)");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording processing: " + e.getMessage());
        }

        return result;
    }

    /**
     * Helper method to find the processing page for a sample item. This looks up
     * the notebook entry associated with the sample (or its parent if it's an
     * aliquot) and finds the processing page within that notebook.
     */
    private Integer findProcessingPageForSampleItem(SampleItem sampleItem) {
        try {
            // For aliquots, get the parent sample item to find its notebook entry
            SampleItem rootSampleItem = sampleItem;
            if (sampleItem.getParentSampleItem() != null) {
                rootSampleItem = sampleItem.getParentSampleItem();
            }

            // Find notebook page samples for the root sample item
            List<NotebookPageSample> pageSamples = notebookPageSampleService
                    .getBySampleItemId(Integer.valueOf(rootSampleItem.getId()));

            if (pageSamples == null || pageSamples.isEmpty()) {
                return null;
            }

            // Get the notebook from any page sample
            for (NotebookPageSample nps : pageSamples) {
                NoteBookPage page = noteBookPageService.get(nps.getNotebookPageId());
                if (page != null && page.getNotebook() != null) {
                    // Get all pages in this notebook
                    List<NoteBookPage> allPages = noteBookPageService.getByNotebookId(page.getNotebook().getId());
                    for (NoteBookPage p : allPages) {
                        if ("sample-processing".equals(p.getPageId())
                                || "medlab-sample-processing".equals(p.getPageId())) {
                            return p.getId();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "findProcessingPageForSampleItem",
                    "Error finding processing page: " + e.getMessage());
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> createAliquots(List<Integer> parentSampleIds, int childCountPerParent,
            String externalIdPrefix, String containerType, Integer notebookPageId, String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (parentSampleIds == null || parentSampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No parent sample IDs provided");
                return result;
            }

            if (childCountPerParent <= 0) {
                childCountPerParent = 1;
            }

            String prefix = externalIdPrefix != null ? externalIdPrefix : "ALQ";
            int createdCount = 0;
            List<Map<String, Object>> createdChildren = new ArrayList<>();

            // Get year for external ID generation
            String year = String.valueOf(java.time.Year.now().getValue());

            for (Integer parentId : parentSampleIds) {
                try {
                    SampleItem parentSampleItem = sampleItemService.get(String.valueOf(parentId));
                    if (parentSampleItem == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "createAliquots",
                                "Parent sample not found: " + parentId);
                        continue;
                    }

                    Sample parentSample = parentSampleItem.getSample();
                    if (parentSample == null) {
                        continue;
                    }

                    // Get existing child count for sequence number
                    List<SampleItem> existingChildren = sampleItemService.getChildSamples(parentSampleItem);
                    int existingChildCount = existingChildren != null ? existingChildren.size() : 0;

                    // Create child samples
                    for (int i = 0; i < childCountPerParent; i++) {
                        // Create child SampleItem
                        SampleItem childSampleItem = new SampleItem();
                        childSampleItem.setSample(parentSample);
                        childSampleItem.setParentSampleItem(parentSampleItem);
                        childSampleItem.setTypeOfSample(parentSampleItem.getTypeOfSample());
                        childSampleItem.setSortOrder(String.valueOf(existingChildCount + i + 1));

                        // Generate external ID: PREFIX-YEAR-PARENTSEQ-CHILDSEQ
                        String parentAccession = parentSample.getAccessionNumber();
                        String childExternalId = String.format("%s-%s-%s-%03d", prefix, year,
                                parentAccession != null
                                        ? parentAccession.substring(Math.max(0, parentAccession.length() - 4))
                                        : parentId,
                                existingChildCount + i + 1);
                        childSampleItem.setExternalId(childExternalId);

                        // Set collection date from parent
                        if (parentSampleItem.getCollectionDate() != null) {
                            childSampleItem.setCollectionDate(parentSampleItem.getCollectionDate());
                        } else {
                            childSampleItem.setCollectionDate(DateUtil.getNowAsTimestamp());
                        }

                        // Set status ID (required field) - use SampleEntered status
                        IStatusService statusService = SpringContext.getBean(IStatusService.class);
                        childSampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));

                        // Save child sample item
                        childSampleItem.setSysUserId(sysUserId);
                        sampleItemService.insert(childSampleItem);

                        // If processing page is specified, create page sample record
                        if (notebookPageId != null) {
                            notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                    Integer.valueOf(childSampleItem.getId()), NotebookPageSample.Status.PENDING);

                            // Store container type info
                            List<NotebookPageSample> newPageSamples = notebookPageSampleService
                                    .getBySampleItemId(Integer.valueOf(childSampleItem.getId()));
                            for (NotebookPageSample nps : newPageSamples) {
                                if (nps.getNotebookPageId().equals(notebookPageId)) {
                                    Map<String, Object> aliquotData = new HashMap<>();
                                    aliquotData.put("containerType", containerType);
                                    aliquotData.put("parentSampleId", parentId);
                                    aliquotData.put("createdAt", java.time.LocalDateTime.now().toString());
                                    nps.setData(aliquotData);
                                    notebookPageSampleService.update(nps);
                                    break;
                                }
                            }
                        }

                        // Add to created list
                        Map<String, Object> childInfo = new HashMap<>();
                        childInfo.put("id", childSampleItem.getId());
                        childInfo.put("externalId", childExternalId);
                        childInfo.put("containerType", containerType);
                        childInfo.put("parentId", parentId);
                        createdChildren.add(childInfo);

                        createdCount++;
                    }

                    LogEvent.logInfo(this.getClass().getSimpleName(), "createAliquots",
                            "Created " + childCountPerParent + " aliquots for parent " + parentId);

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createAliquots",
                            "Error creating aliquots for parent " + parentId + ": " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("createdCount", createdCount);
            result.put("children", createdChildren);
            result.put("message", "Successfully created " + createdCount + " aliquots");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error creating aliquots: " + e.getMessage());
        }

        return result;
    }

    // ==================== Testing & Analyzer Methods ====================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSamplesForTesting(Integer entryId) {
        List<Map<String, Object>> samples = new ArrayList<>();

        if (entryId == null) {
            return samples;
        }

        try {
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTesting",
                        "Entry or notebook not found for ID: " + entryId);
                return samples;
            }

            // Get notebook pages for this entry
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return samples;
            }

            // Find the processing page (samples must be processed before testing) and
            // testing page
            Integer processingPageId = null;
            Integer testingPageId = null;

            for (NoteBookPage page : pages) {
                String pageId = page.getPageId();
                if ("sample-processing".equals(pageId) || "medlab-sample-processing".equals(pageId)) {
                    processingPageId = page.getId();
                } else if ("testing-analyzer".equals(pageId) || "medlab-testing-analyzer".equals(pageId)) {
                    testingPageId = page.getId();
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTesting",
                    "Processing page ID: " + processingPageId + ", Testing page ID: " + testingPageId);

            if (processingPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForTesting",
                        "Processing page not found for entry: " + entryId);
                return samples;
            }

            // Get samples from the Processing page that have COMPLETED status (processed)
            List<NotebookPageSample> processingPageSamples = notebookPageSampleService.getByPageId(processingPageId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForTesting",
                    "Found " + processingPageSamples.size() + " samples on Processing page");

            for (NotebookPageSample processedSample : processingPageSamples) {
                try {
                    // Only include samples that have been processed (COMPLETED on processing page)
                    if (processedSample.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        continue;
                    }

                    SampleItem sampleItem = sampleItemService.get(processedSample.getSampleItemId());
                    if (sampleItem == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();
                    if (sample == null) {
                        continue;
                    }

                    // Build sample data
                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("sampleItemId", sampleItem.getId());
                    sampleData.put("labNo", sample.getAccessionNumber());
                    sampleData.put("accessionNumber", sample.getAccessionNumber());

                    // Get patient info
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    if (patient != null) {
                        String firstName = patient.getPerson() != null ? patient.getPerson().getFirstName() : "";
                        String lastName = patient.getPerson() != null ? patient.getPerson().getLastName() : "";
                        sampleData.put("patientName", (firstName + " " + lastName).trim());
                        sampleData.put("patientId", patient.getId());
                    }

                    // Get sample type
                    TypeOfSample typeOfSample = sampleItem.getTypeOfSample();
                    if (typeOfSample != null) {
                        sampleData.put("sampleType", typeOfSample.getDescription());
                        sampleData.put("sampleTypeId", typeOfSample.getId());
                    }

                    // Get tests/analyses for this sample
                    List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
                    List<Map<String, Object>> testList = new ArrayList<>();
                    int pendingTests = 0;
                    int completedTests = 0;

                    if (analyses != null) {
                        for (Analysis analysis : analyses) {
                            Map<String, Object> testInfo = new HashMap<>();
                            testInfo.put("analysisId", analysis.getId());
                            if (analysis.getTest() != null) {
                                testInfo.put("testId", analysis.getTest().getId());
                                testInfo.put("testName", analysis.getTest().getLocalizedName());
                            }
                            testInfo.put("status", analysis.getStatusId());
                            testList.add(testInfo);

                            // Count test status
                            if (analysis.getStatusId() != null) {
                                // Check if status indicates result entered
                                if ("6".equals(analysis.getStatusId()) || "7".equals(analysis.getStatusId())) {
                                    completedTests++;
                                } else {
                                    pendingTests++;
                                }
                            }
                        }
                    }
                    sampleData.put("tests", testList);
                    sampleData.put("testCount", testList.size());
                    sampleData.put("pendingTests", pendingTests);
                    sampleData.put("completedTests", completedTests);

                    // Check testing status on testing page
                    String testingStatus = "PENDING";
                    String analyzerName = null;
                    String technologyUsed = null;
                    Boolean qcLocked = false;

                    if (testingPageId != null) {
                        List<NotebookPageSample> testingPageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                        for (NotebookPageSample tps : testingPageSamples) {
                            if (tps.getNotebookPageId().equals(testingPageId)) {
                                testingStatus = tps.getStatus() == NotebookPageSample.Status.COMPLETED ? "COMPLETED"
                                        : "IN_PROGRESS";

                                // Get testing data
                                Map<String, Object> testingData = tps.getData();
                                if (testingData != null) {
                                    analyzerName = (String) testingData.get("analyzerName");
                                    technologyUsed = (String) testingData.get("technologyUsed");
                                    qcLocked = Boolean.TRUE.equals(testingData.get("qcLocked"));
                                }
                                break;
                            }
                        }
                    }

                    sampleData.put("testingStatus", testingStatus);
                    sampleData.put("pageStatus", testingStatus);
                    sampleData.put("analyzerName", analyzerName);
                    sampleData.put("technologyUsed", technologyUsed);
                    sampleData.put("qcLocked", qcLocked);

                    // Check for abnormal results
                    boolean hasAbnormalResults = false;
                    // Logic for abnormal detection would go here (comparing to reference ranges)
                    sampleData.put("hasAbnormalResults", hasAbnormalResults);

                    samples.add(sampleData);

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForTesting",
                            "Error processing sample: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return samples;
    }

    @Override
    @Transactional
    public Map<String, Object> executeTests(List<Integer> sampleIds, String analyzerId, String analyzerName,
            Boolean worklistGenerated, Boolean isManualTest, String technologyUsed, Integer notebookPageId,
            String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleIds == null || sampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample IDs provided");
                return result;
            }

            int processedCount = 0;

            for (Integer sampleId : sampleIds) {
                try {
                    SampleItem sampleItem = sampleItemService.get(String.valueOf(sampleId));
                    if (sampleItem == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "executeTests",
                                "Sample item not found: " + sampleId);
                        continue;
                    }

                    // Create/update NotebookPageSample record
                    if (notebookPageId != null) {
                        Map<String, Object> testingData = new HashMap<>();
                        testingData.put("analyzerId", analyzerId);
                        testingData.put("analyzerName", analyzerName != null ? analyzerName : "Manual");
                        testingData.put("worklistGenerated", worklistGenerated);
                        testingData.put("isManualTest", isManualTest);
                        testingData.put("technologyUsed", technologyUsed);
                        testingData.put("executedAt", java.time.LocalDateTime.now().toString());
                        testingData.put("executedBy", sysUserId);

                        // Check if page sample exists
                        List<NotebookPageSample> existingPageSamples = notebookPageSampleService
                                .getBySampleItemId(sampleId);
                        NotebookPageSample testingRecord = null;
                        for (NotebookPageSample nps : existingPageSamples) {
                            if (nps.getNotebookPageId().equals(notebookPageId)) {
                                testingRecord = nps;
                                break;
                            }
                        }

                        if (testingRecord != null) {
                            testingRecord.setStatus(NotebookPageSample.Status.COMPLETED);
                            testingRecord.setData(testingData);
                            notebookPageSampleService.update(testingRecord);
                        } else {
                            notebookPageSampleService.createPageSampleForPage(notebookPageId, sampleId,
                                    NotebookPageSample.Status.COMPLETED);

                            List<NotebookPageSample> newPageSamples = notebookPageSampleService
                                    .getBySampleItemId(sampleId);
                            for (NotebookPageSample nps : newPageSamples) {
                                if (nps.getNotebookPageId().equals(notebookPageId)) {
                                    nps.setData(testingData);
                                    notebookPageSampleService.update(nps);
                                    break;
                                }
                            }
                        }
                    }

                    processedCount++;

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "executeTests",
                            "Error executing tests for sample " + sampleId + ": " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("processedCount", processedCount);
            result.put("message", "Successfully executed tests for " + processedCount + " sample(s)");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error executing tests: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> assignTestsToSamples(List<Integer> sampleItemIds, List<String> testIds,
            Integer notebookPageId, String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleItemIds == null || sampleItemIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample IDs provided");
                return result;
            }

            if (testIds == null || testIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No test IDs provided");
                return result;
            }

            // Load selected tests
            List<Test> selectedTests = new ArrayList<>();
            for (String testId : testIds) {
                Test test = testService.get(testId);
                if (test != null) {
                    selectedTests.add(test);
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "assignTestsToSamples",
                            "Test not found: " + testId);
                }
            }

            if (selectedTests.isEmpty()) {
                result.put("success", false);
                result.put("error", "No valid tests found");
                return result;
            }

            // Get Analysis status ID
            IStatusService iStatusService = SpringContext.getBean(IStatusService.class);
            String analysisNotStartedStatusId = iStatusService
                    .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.NotStarted);
            if (analysisNotStartedStatusId == null || "-1".equals(analysisNotStartedStatusId)) {
                analysisNotStartedStatusId = "1";
            }

            int analysesCreated = 0;
            int samplesProcessed = 0;

            for (Integer sampleItemId : sampleItemIds) {
                try {
                    SampleItem sampleItem = sampleItemService.get(String.valueOf(sampleItemId));
                    if (sampleItem == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "assignTestsToSamples",
                                "Sample item not found: " + sampleItemId);
                        continue;
                    }

                    // Check if sample already has analyses
                    Sample sample = sampleItem.getSample();
                    if (sample == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "assignTestsToSamples",
                                "Sample not found for item: " + sampleItemId);
                        continue;
                    }

                    // Get existing analyses to avoid duplicates
                    List<Analysis> existingAnalyses = analysisService.getAnalysesBySampleItem(sampleItem);
                    java.util.Set<String> existingTestIds = new java.util.HashSet<>();
                    if (existingAnalyses != null) {
                        for (Analysis a : existingAnalyses) {
                            if (a.getTest() != null) {
                                existingTestIds.add(a.getTest().getId());
                            }
                        }
                    }

                    // Create Analysis records for each selected test
                    for (Test test : selectedTests) {
                        // Skip if this test already exists for this sample
                        if (existingTestIds.contains(test.getId())) {
                            LogEvent.logInfo(this.getClass().getSimpleName(), "assignTestsToSamples",
                                    "Test " + test.getId() + " already exists for sample item " + sampleItemId);
                            continue;
                        }

                        Analysis analysis = new Analysis();
                        analysis.setSampleItem(sampleItem);
                        analysis.setTest(test);
                        analysis.setAnalysisType("MANUAL");
                        analysis.setStatusId(analysisNotStartedStatusId);
                        analysis.setSysUserId(sysUserId);
                        analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
                        analysisService.insert(analysis);
                        analysesCreated++;
                    }

                    samplesProcessed++;

                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "assignTestsToSamples",
                            "Error assigning tests to sample " + sampleItemId + ": " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("samplesProcessed", samplesProcessed);
            result.put("analysesCreated", analysesCreated);
            result.put("message", "Successfully created " + analysesCreated + " analysis records for "
                    + samplesProcessed + " samples");

            LogEvent.logInfo(this.getClass().getSimpleName(), "assignTestsToSamples",
                    "Created " + analysesCreated + " analysis records for " + samplesProcessed + " samples");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error assigning tests: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> recordQc(String analyzerId, String analyzerName, String qcType, String qcLevel,
            String qcResult, String calibrationStatus, String notes, Integer notebookPageId, String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Create a QC record as a special NotebookPageSample entry with sampleItemId =
            // -1 (system record)
            // This stores QC data in the data field of NotebookPageSample
            Map<String, Object> qcRecord = new HashMap<>();
            qcRecord.put("id", java.util.UUID.randomUUID().toString());
            qcRecord.put("recordType", "QC_RECORD");
            qcRecord.put("analyzerId", analyzerId);
            qcRecord.put("analyzerName", analyzerName);
            qcRecord.put("qcType", qcType);
            qcRecord.put("qcLevel", qcLevel);
            qcRecord.put("result", qcResult);
            qcRecord.put("qcResult", qcResult);
            qcRecord.put("calibrationStatus", calibrationStatus);
            qcRecord.put("notes", notes);
            qcRecord.put("recordedDate", java.time.LocalDateTime.now().toString());
            qcRecord.put("recordedBy", sysUserId);
            qcRecord.put("notebookPageId", notebookPageId);

            // Store QC record using NotebookPageSample with a special marker sampleItemId
            if (notebookPageId != null) {
                NoteBookPage notebookPage = noteBookPageService.get(notebookPageId);
                if (notebookPage != null) {
                    // Create a special NotebookPageSample with sampleItemId = "-1" for QC records
                    NotebookPageSample qcPageSample = new NotebookPageSample();
                    qcPageSample.setNotebookPage(notebookPage);
                    qcPageSample.setSampleItemId("-1"); // Special marker for QC records (String)
                    qcPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                    qcPageSample.setData(qcRecord);
                    qcPageSample.setSysUserId(sysUserId);

                    notebookPageSampleService.insert(qcPageSample);
                }
            }

            result.put("success", true);
            result.put("qcRecord", qcRecord);
            result.put("message", "QC record saved successfully");

            // If QC failed, flag for locking results
            if ("fail".equals(qcResult)) {
                result.put("qcFailed", true);
                result.put("warning", "QC failed - results will be locked until QC passes");
            }

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording QC: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQcRecords(Integer entryId) {
        List<Map<String, Object>> qcRecords = new ArrayList<>();

        try {
            if (entryId == null) {
                return qcRecords;
            }

            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return qcRecords;
            }

            // Get notebook pages and look for QC records (NotebookPageSample with
            // sampleItemId = -1)
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            for (NoteBookPage page : pages) {
                if ("medlab-testing-analyzer".equals(page.getPageId())) {
                    // Get all page samples for this page
                    List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());
                    for (NotebookPageSample nps : pageSamples) {
                        // QC records have sampleItemId = "-1"
                        if ("-1".equals(nps.getSampleItemId())) {
                            Map<String, Object> data = nps.getData();
                            if (data != null && "QC_RECORD".equals(data.get("recordType"))) {
                                qcRecords.add(data);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return qcRecords;
    }

    @Override
    @Transactional
    public Map<String, Object> recordDeviation(List<Integer> sampleIds, String deviationType, String actionTaken,
            String rootCauseAnalysis, String notes, Integer notebookPageId, String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> deviationRecord = new HashMap<>();
            deviationRecord.put("id", java.util.UUID.randomUUID().toString());
            deviationRecord.put("recordType", "DEVIATION");
            deviationRecord.put("sampleIds", sampleIds);
            deviationRecord.put("deviationType", deviationType);
            deviationRecord.put("actionTaken", actionTaken);
            deviationRecord.put("rootCauseAnalysis", rootCauseAnalysis);
            deviationRecord.put("notes", notes);
            deviationRecord.put("recordedDate", java.time.LocalDateTime.now().toString());
            deviationRecord.put("recordedBy", sysUserId);
            deviationRecord.put("resolved", false);

            // Store deviation using NotebookPageSample with sampleItemId = "-2" for
            // deviations
            if (notebookPageId != null) {
                NoteBookPage notebookPage = noteBookPageService.get(notebookPageId);
                if (notebookPage != null) {
                    NotebookPageSample deviationPageSample = new NotebookPageSample();
                    deviationPageSample.setNotebookPage(notebookPage);
                    deviationPageSample.setSampleItemId("-2"); // Special marker for deviation records (String)
                    deviationPageSample.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                    deviationPageSample.setData(deviationRecord);
                    deviationPageSample.setSysUserId(sysUserId);

                    notebookPageSampleService.insert(deviationPageSample);
                }
            }

            result.put("success", true);
            result.put("deviation", deviationRecord);
            result.put("message", "Deviation recorded successfully");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording deviation: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDeviations(Integer entryId) {
        List<Map<String, Object>> deviations = new ArrayList<>();

        try {
            if (entryId == null) {
                return deviations;
            }

            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return deviations;
            }

            // Get notebook pages and look for deviation records (NotebookPageSample with
            // sampleItemId = -2)
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            for (NoteBookPage page : pages) {
                if ("medlab-testing-analyzer".equals(page.getPageId())) {
                    List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());
                    for (NotebookPageSample nps : pageSamples) {
                        // Deviation records have sampleItemId = "-2"
                        if ("-2".equals(nps.getSampleItemId())) {
                            Map<String, Object> data = nps.getData();
                            if (data != null && "DEVIATION".equals(data.get("recordType"))) {
                                deviations.add(data);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return deviations;
    }

    // ==================== Helper Methods ====================

    /**
     * Calculates the result flag (CRITICAL_LOW, LOW, NORMAL, HIGH, CRITICAL_HIGH)
     * based on result value and reference ranges.
     */
    private String calculateResultFlag(String resultValue, ResultLimit resultLimit) {
        if (resultValue == null || resultValue.isEmpty() || resultLimit == null) {
            return null;
        }

        try {
            double value = Double.parseDouble(resultValue);

            // Check critical ranges first
            Double lowCritical = resultLimit.getLowCritical();
            Double highCritical = resultLimit.getHighCritical();
            Double lowNormal = resultLimit.getLowNormal();
            Double highNormal = resultLimit.getHighNormal();

            if (lowCritical != null && value < lowCritical) {
                return "CRITICAL_LOW";
            }
            if (highCritical != null && value > highCritical) {
                return "CRITICAL_HIGH";
            }
            if (lowNormal != null && value < lowNormal) {
                return "LOW";
            }
            if (highNormal != null && value > highNormal) {
                return "HIGH";
            }

            return "NORMAL";
        } catch (NumberFormatException e) {
            // Non-numeric result (e.g., "Positive", "Negative")
            return null;
        }
    }

    // ==================== Enhanced Result Entry Methods ====================

    @Override
    @Transactional
    public Map<String, Object> saveTestResult(String labNo, String testId, String resultValue, String unit,
            String entryType, String notes, Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Find the sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNo);
            if (sample == null) {
                result.put("success", false);
                result.put("error", "Sample not found: " + labNo);
                return result;
            }

            // Find the analysis for this test
            List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
            Analysis targetAnalysis = null;
            for (Analysis analysis : analyses) {
                if (analysis.getTest() != null && testId.equals(analysis.getTest().getId())) {
                    targetAnalysis = analysis;
                    break;
                }
            }

            if (targetAnalysis == null) {
                result.put("success", false);
                result.put("error", "Test not found for sample: " + labNo);
                return result;
            }

            IStatusService statusService = SpringContext.getBean(IStatusService.class);

            // Check if result already exists
            List<Result> existingResults = resultService.getResultsByAnalysis(targetAnalysis);
            Result testResult;

            if (existingResults != null && !existingResults.isEmpty()) {
                // Update existing result
                testResult = existingResults.get(0);
                testResult.setValue(resultValue);
                testResult.setSysUserId(sysUserId);
                resultService.update(testResult);
            } else {
                // Create new result
                testResult = new Result();
                testResult.setAnalysis(targetAnalysis);
                testResult.setValue(resultValue);
                testResult.setResultType("N"); // Numeric result type (can be enhanced)
                testResult.setIsReportable("Y");
                testResult.setSortOrder("1");
                testResult.setSysUserId(sysUserId);
                resultService.insert(testResult);
            }

            // Update analysis status to TechnicalAcceptance (result entered, pending
            // verification)
            targetAnalysis.setStatusId(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance));
            targetAnalysis.setSysUserId(sysUserId);
            analysisService.update(targetAnalysis);

            // Calculate flag for response
            ResultLimit resultLimit = resultLimitService.getResultLimitForAnalysis(targetAnalysis);
            String flag = calculateResultFlag(resultValue, resultLimit);

            // Update or create NotebookPageSample for result entry page
            if (notebookPageId != null) {
                List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                for (SampleItem sampleItem : sampleItems) {
                    List<NotebookPageSample> existingPageSamples = notebookPageSampleService
                            .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                    boolean foundPageSample = false;
                    for (NotebookPageSample pageSample : existingPageSamples) {
                        if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                            pageSample.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                            // Store entry type and notes in data
                            Map<String, Object> data = pageSample.getData();
                            if (data == null) {
                                data = new HashMap<>();
                            }
                            data.put("entryType", entryType);
                            data.put("notes", notes);
                            pageSample.setData(data);
                            pageSample.setSysUserId(sysUserId);
                            notebookPageSampleService.update(pageSample);
                            foundPageSample = true;
                            break;
                        }
                    }

                    if (!foundPageSample) {
                        notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.IN_PROGRESS);
                        // Fetch the created record to update data
                        NotebookPageSample newPageSample = notebookPageSampleService
                                .getByPageIdAndSampleItemId(notebookPageId, Integer.valueOf(sampleItem.getId()));
                        if (newPageSample != null) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("entryType", entryType);
                            data.put("notes", notes);
                            newPageSample.setData(data);
                            notebookPageSampleService.update(newPageSample);
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("resultId", testResult.getId());
            result.put("labNo", labNo);
            result.put("testId", testId);
            result.put("flag", flag);
            result.put("message", "Result saved successfully");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error saving result: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> saveBulkResults(List<Map<String, Object>> results, String entryType, String analyzerName,
            String runId, Integer notebookPageId, String sysUserId) {

        Map<String, Object> response = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            for (Map<String, Object> resultData : results) {
                String labNo = (String) resultData.get("labNo");
                String testId = (String) resultData.get("testId");
                String resultValue = (String) resultData.get("resultValue");
                String unit = (String) resultData.get("unit");

                // Build notes with analyzer info
                String notes = "Analyzer: " + analyzerName + ", Run ID: " + runId;

                Map<String, Object> saveResult = saveTestResult(labNo, testId, resultValue, unit, entryType, notes,
                        notebookPageId, sysUserId);

                if (Boolean.TRUE.equals(saveResult.get("success"))) {
                    successCount++;
                } else {
                    failCount++;
                    errors.add(labNo + ": " + saveResult.get("error"));
                }
            }

            response.put("success", failCount == 0);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("totalCount", results.size());
            response.put("errors", errors);
            response.put("message", "Imported " + successCount + " of " + results.size() + " results");

        } catch (Exception e) {
            LogEvent.logError(e);
            response.put("success", false);
            response.put("error", "Error importing bulk results: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> markResultEntryComplete(List<Integer> sampleIds, Integer notebookPageId,
            String sysUserId) {

        Map<String, Object> result = new HashMap<>();
        int updatedCount = 0;

        try {
            for (Integer sampleId : sampleIds) {
                Sample sample = sampleService.get(String.valueOf(sampleId));
                if (sample == null) {
                    continue;
                }

                // Check if all tests have results
                List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
                boolean allHaveResults = true;

                for (Analysis analysis : analyses) {
                    List<Result> results = resultService.getResultsByAnalysis(analysis);
                    if (results == null || results.isEmpty()) {
                        allHaveResults = false;
                        break;
                    }
                }

                if (!allHaveResults) {
                    continue; // Skip samples that don't have all results
                }

                // Update the NotebookPageSample status to COMPLETED
                if (notebookPageId != null) {
                    List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                    for (SampleItem sampleItem : sampleItems) {
                        List<NotebookPageSample> pageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                        for (NotebookPageSample pageSample : pageSamples) {
                            if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                                pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                                pageSample.setSysUserId(sysUserId);
                                notebookPageSampleService.update(pageSample);
                                updatedCount++;
                                break;
                            }
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("updatedCount", updatedCount);
            result.put("message", "Marked " + updatedCount + " samples as complete");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error marking samples complete: " + e.getMessage());
        }

        return result;
    }

    // ==================== Performance Monitoring Methods ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPerformanceDashboard(Integer entryId) {
        Map<String, Object> dashboard = new HashMap<>();

        try {
            // Get all sub-metrics
            Map<String, Object> tatStats = getTurnaroundTimeStats(entryId);
            Map<String, Object> acceptanceStats = getSampleAcceptanceStats(entryId);
            List<Map<String, Object>> qcTrends = getQcPerformanceTrends(entryId);
            List<Map<String, Object>> equipmentStats = getEquipmentUsageStats(entryId);
            Map<String, Object> utilizationReport = getSampleUtilizationReport(entryId);
            List<Map<String, Object>> correctiveActions = getCorrectiveActionsLog(entryId);

            dashboard.put("turnaroundTime", tatStats);
            dashboard.put("sampleAcceptance", acceptanceStats);
            dashboard.put("qcTrends", qcTrends);
            dashboard.put("equipmentUsage", equipmentStats);
            dashboard.put("sampleUtilization", utilizationReport);
            dashboard.put("correctiveActions", correctiveActions);
            dashboard.put("success", true);

        } catch (Exception e) {
            LogEvent.logError(e);
            dashboard.put("success", false);
            dashboard.put("error", "Error loading dashboard: " + e.getMessage());
        }

        return dashboard;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTurnaroundTimeStats(Integer entryId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            if (entryId == null) {
                return stats;
            }

            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return stats;
            }

            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return stats;
            }

            // Calculate TAT phases
            long totalReceiptToQc = 0;
            long totalQcToTesting = 0;
            long totalTestingToResult = 0;
            long totalResultToVerification = 0;
            long totalVerificationToReport = 0;
            int sampleCount = 0;

            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();

            for (NoteBookPage page : pages) {
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                for (NotebookPageSample pageSample : pageSamples) {
                    SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                    if (sampleItem == null || sampleItem.getSample() == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();
                    if (processedSampleIds.contains(sample.getId())) {
                        continue;
                    }
                    processedSampleIds.add(sample.getId());

                    // Get timestamps from sample and analyses
                    Timestamp received = sample.getReceivedTimestamp();
                    if (received != null) {
                        sampleCount++;

                        // Get all page samples for this sample to track phase times
                        List<NotebookPageSample> allPageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                        for (NotebookPageSample ps : allPageSamples) {
                            if (ps.getCompletedAt() != null) {
                                // Calculate phase duration based on page type
                                long duration = ps.getCompletedAt().getTime() - received.getTime();
                                // This is a simplified calculation - in production you'd track each phase
                                // separately
                                if (duration > 0) {
                                    totalReceiptToQc += duration / (1000 * 60); // Convert to minutes
                                }
                            }
                        }
                    }
                }
            }

            // Calculate averages
            if (sampleCount > 0) {
                stats.put("avgReceiptToQcMinutes", totalReceiptToQc / sampleCount);
                stats.put("avgQcToTestingMinutes", totalQcToTesting / sampleCount);
                stats.put("avgTestingToResultMinutes", totalTestingToResult / sampleCount);
                stats.put("avgResultToVerificationMinutes", totalResultToVerification / sampleCount);
                stats.put("avgVerificationToReportMinutes", totalVerificationToReport / sampleCount);
                stats.put("avgTotalTatMinutes", (totalReceiptToQc + totalQcToTesting + totalTestingToResult
                        + totalResultToVerification + totalVerificationToReport) / sampleCount);
            }
            stats.put("sampleCount", sampleCount);

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleAcceptanceStats(Integer entryId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            if (entryId == null) {
                return stats;
            }

            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return stats;
            }

            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return stats;
            }

            // Find QC page
            Integer qcPageId = null;
            for (NoteBookPage page : pages) {
                if ("quality-check".equals(page.getPageId()) || "medlab-quality-check".equals(page.getPageId())) {
                    qcPageId = page.getId();
                    break;
                }
            }

            int totalSamples = 0;
            int acceptedSamples = 0;
            int rejectedSamples = 0;

            if (qcPageId != null) {
                List<NotebookPageSample> qcPageSamples = notebookPageSampleService.getByPageId(qcPageId);
                for (NotebookPageSample ps : qcPageSamples) {
                    totalSamples++;
                    if (ps.getStatus() == NotebookPageSample.Status.COMPLETED) {
                        SampleItem sampleItem = sampleItemService.get(ps.getSampleItemId());
                        if (sampleItem != null && !sampleItem.isRejected()) {
                            acceptedSamples++;
                        } else {
                            rejectedSamples++;
                        }
                    } else if (ps.getStatus() == NotebookPageSample.Status.SKIPPED) {
                        // SKIPPED status indicates rejected samples in QC context
                        rejectedSamples++;
                    }
                }
            }

            stats.put("totalSamples", totalSamples);
            stats.put("acceptedSamples", acceptedSamples);
            stats.put("rejectedSamples", rejectedSamples);
            if (totalSamples > 0) {
                stats.put("acceptanceRate", (acceptedSamples * 100.0) / totalSamples);
                stats.put("rejectionRate", (rejectedSamples * 100.0) / totalSamples);
            } else {
                stats.put("acceptanceRate", 0.0);
                stats.put("rejectionRate", 0.0);
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQcPerformanceTrends(Integer entryId) {
        List<Map<String, Object>> trends = new ArrayList<>();

        try {
            // Get QC records and aggregate by date/analyzer
            List<Map<String, Object>> qcRecords = getQcRecords(entryId);

            // Group by analyzer and calculate pass/fail rates
            Map<String, List<Map<String, Object>>> byAnalyzer = new HashMap<>();
            for (Map<String, Object> record : qcRecords) {
                String analyzer = (String) record.get("analyzerName");
                if (analyzer == null)
                    analyzer = "Unknown";
                byAnalyzer.computeIfAbsent(analyzer, k -> new ArrayList<>()).add(record);
            }

            for (Map.Entry<String, List<Map<String, Object>>> entry : byAnalyzer.entrySet()) {
                Map<String, Object> trend = new HashMap<>();
                trend.put("analyzer", entry.getKey());

                int total = entry.getValue().size();
                int passed = 0;
                for (Map<String, Object> record : entry.getValue()) {
                    if ("pass".equals(record.get("qcResult"))) {
                        passed++;
                    }
                }

                trend.put("totalQcRuns", total);
                trend.put("passedRuns", passed);
                trend.put("failedRuns", total - passed);
                trend.put("passRate", total > 0 ? (passed * 100.0) / total : 0.0);
                trends.add(trend);
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return trends;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEquipmentUsageStats(Integer entryId) {
        List<Map<String, Object>> equipmentStats = new ArrayList<>();

        try {
            if (entryId == null) {
                return equipmentStats;
            }

            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return equipmentStats;
            }

            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            // Count tests by analyzer
            Map<String, Integer> testCountByAnalyzer = new HashMap<>();
            Map<String, String> technologyByAnalyzer = new HashMap<>();

            for (NoteBookPage page : pages) {
                if (!"testing-analyzer".equals(page.getPageId())) {
                    continue;
                }

                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());
                for (NotebookPageSample ps : pageSamples) {
                    // Get analyzer info from the data map if available
                    String analyzer = "Manual";
                    Map<String, Object> data = ps.getData();
                    if (data != null && data.get("analyzerName") != null) {
                        analyzer = (String) data.get("analyzerName");
                    }
                    testCountByAnalyzer.merge(analyzer, 1, Integer::sum);
                }
            }

            // Build response
            for (Map.Entry<String, Integer> entry2 : testCountByAnalyzer.entrySet()) {
                Map<String, Object> equipment = new HashMap<>();
                equipment.put("analyzerName", entry2.getKey());
                equipment.put("testCount", entry2.getValue());
                equipment.put("technology", technologyByAnalyzer.getOrDefault(entry2.getKey(), "Unknown"));
                equipmentStats.add(equipment);
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return equipmentStats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleUtilizationReport(Integer entryId) {
        Map<String, Object> report = new HashMap<>();

        try {
            if (entryId == null) {
                return report;
            }

            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return report;
            }

            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            int totalSamples = 0;
            int samplesWithResults = 0;
            int samplesReported = 0;
            int samplesStored = 0;
            int samplesArchived = 0;

            java.util.Set<String> processedSampleIds = new java.util.HashSet<>();

            for (NoteBookPage page : pages) {
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                for (NotebookPageSample ps : pageSamples) {
                    SampleItem sampleItem = sampleItemService.get(ps.getSampleItemId());
                    if (sampleItem == null || sampleItem.getSample() == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();
                    if (processedSampleIds.contains(sample.getId())) {
                        continue;
                    }
                    processedSampleIds.add(sample.getId());
                    totalSamples++;

                    // Check if sample has results
                    List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
                    if (analyses != null) {
                        for (Analysis analysis : analyses) {
                            List<Result> results = resultService.getResultsByAnalysis(analysis);
                            if (results != null && !results.isEmpty()) {
                                samplesWithResults++;
                                break;
                            }
                        }
                    }
                }
            }

            report.put("totalSamples", totalSamples);
            report.put("samplesWithResults", samplesWithResults);
            report.put("samplesReported", samplesReported);
            report.put("samplesStored", samplesStored);
            report.put("samplesArchived", samplesArchived);
            if (totalSamples > 0) {
                report.put("utilizationRate", (samplesWithResults * 100.0) / totalSamples);
            } else {
                report.put("utilizationRate", 0.0);
            }

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return report;
    }

    @Override
    @Transactional
    public Map<String, Object> modifyReferenceRange(String testId, Double lowNormal, Double highNormal,
            Double lowCritical, Double highCritical, Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Get existing result limits for the test
            List<ResultLimit> resultLimits = resultLimitService.getResultLimits(testId);

            if (resultLimits == null || resultLimits.isEmpty()) {
                result.put("success", false);
                result.put("error", "No existing reference range found for test: " + testId);
                return result;
            }

            // Update the first result limit (simplification - in practice you might want to
            // handle multiple)
            ResultLimit resultLimit = resultLimits.get(0);
            if (lowNormal != null) {
                resultLimit.setLowNormal(lowNormal);
            }
            if (highNormal != null) {
                resultLimit.setHighNormal(highNormal);
            }
            if (lowCritical != null) {
                resultLimit.setLowCritical(lowCritical);
            }
            if (highCritical != null) {
                resultLimit.setHighCritical(highCritical);
            }
            resultLimit.setSysUserId(sysUserId);

            resultLimitService.update(resultLimit);

            result.put("success", true);
            result.put("message", "Reference range updated successfully");
            result.put("testId", testId);
            result.put("lowNormal", lowNormal);
            result.put("highNormal", highNormal);
            result.put("lowCritical", lowCritical);
            result.put("highCritical", highCritical);

            LogEvent.logInfo(this.getClass().getSimpleName(), "modifyReferenceRange",
                    "Reference range modified for test " + testId + " by user " + sysUserId);

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error modifying reference range: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCorrectiveActionsLog(Integer entryId) {
        // Get deviations which include corrective actions
        List<Map<String, Object>> deviations = getDeviations(entryId);

        // Filter to only those with corrective actions taken
        List<Map<String, Object>> correctiveActions = new ArrayList<>();
        for (Map<String, Object> deviation : deviations) {
            if (deviation.get("actionTaken") != null && !((String) deviation.get("actionTaken")).isEmpty()) {
                Map<String, Object> action = new HashMap<>();
                action.put("deviationType", deviation.get("deviationType"));
                action.put("actionTaken", deviation.get("actionTaken"));
                action.put("rootCauseAnalysis", deviation.get("rootCauseAnalysis"));
                action.put("notes", deviation.get("notes"));
                action.put("recordedAt", deviation.get("recordedAt"));
                correctiveActions.add(action);
            }
        }

        return correctiveActions;
    }

    @Override
    @Transactional
    public Map<String, Object> markVerificationComplete(List<Integer> sampleIds, Integer notebookPageId,
            String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleIds == null || sampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample IDs provided");
                return result;
            }

            int updatedCount = 0;

            for (Integer sampleId : sampleIds) {
                Sample sample = sampleService.get(String.valueOf(sampleId));
                if (sample != null) {
                    // Get sample items
                    List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                    for (SampleItem sampleItem : sampleItems) {
                        // Update or create NotebookPageSample status
                        List<NotebookPageSample> pageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                        boolean foundPageSample = false;
                        for (NotebookPageSample pageSample : pageSamples) {
                            if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                                pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                                pageSample.setSysUserId(sysUserId);
                                notebookPageSampleService.update(pageSample);
                                updatedCount++;
                                foundPageSample = true;
                                break;
                            }
                        }

                        // If no page sample exists for this page, create one
                        if (!foundPageSample && notebookPageId != null) {
                            notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                    Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.COMPLETED);
                            updatedCount++;
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("updatedCount", updatedCount);
            result.put("message", "Marked " + updatedCount + " samples as verification complete");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error marking verification complete: " + e.getMessage());
        }

        return result;
    }

    // ==================== Disposal & Archiving Methods ====================

    @Override
    public List<Map<String, Object>> getSamplesForDisposal(Integer entryId) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (entryId == null) {
            return results;
        }

        try {
            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return results;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            if (pages == null || pages.isEmpty()) {
                return results;
            }

            // Find the validation-reporting page (samples must be validated before
            // disposal)
            Integer validationPageId = null;
            Integer disposalPageId = null;

            for (NoteBookPage page : pages) {
                if ("validation-reporting".equals(page.getPageId())
                        || "medlab-validation-reporting".equals(page.getPageId())) {
                    validationPageId = page.getId();
                } else if ("disposal-archiving".equals(page.getPageId())
                        || "medlab-disposal-archiving".equals(page.getPageId())) {
                    disposalPageId = page.getId();
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForDisposal",
                    "Validation page ID: " + validationPageId + ", Disposal page ID: " + disposalPageId);

            if (validationPageId == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForDisposal",
                        "Validation page not found for entry: " + entryId);
                return results;
            }

            // Get samples from the Validation page that have COMPLETED status
            List<NotebookPageSample> validationPageSamples = notebookPageSampleService.getByPageId(validationPageId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "getSamplesForDisposal",
                    "Found " + validationPageSamples.size() + " samples on Validation page");

            for (NotebookPageSample validatedSample : validationPageSamples) {
                try {
                    // Only include samples that have been validated (COMPLETED on validation page)
                    if (validatedSample.getStatus() != NotebookPageSample.Status.COMPLETED) {
                        continue;
                    }

                    SampleItem sampleItem = sampleItemService.get(validatedSample.getSampleItemId());
                    if (sampleItem == null || sampleItem.getSample() == null) {
                        continue;
                    }

                    Sample sample = sampleItem.getSample();

                    // Determine disposal status based on sample type
                    String recommendedMethod = determineDisposalMethod(sampleItem.getTypeOfSample());
                    String disposalStatus = "PENDING";

                    // Check if already disposed (stored in page sample data on disposal page)
                    if (disposalPageId != null) {
                        List<NotebookPageSample> disposalPageSamples = notebookPageSampleService
                                .getBySampleItemId(Integer.valueOf(sampleItem.getId()));
                        for (NotebookPageSample dps : disposalPageSamples) {
                            if (disposalPageId.equals(dps.getNotebookPageId())) {
                                Map<String, Object> pageData = dps.getData();
                                if (pageData != null && pageData.get("disposed") != null
                                        && Boolean.TRUE.equals(pageData.get("disposed"))) {
                                    disposalStatus = "DISPOSED";
                                }
                                break;
                            }
                        }
                    }

                    Map<String, Object> sampleData = new HashMap<>();
                    sampleData.put("id", sample.getId());
                    sampleData.put("sampleItemId", sampleItem.getId());
                    sampleData.put("labNo", sample.getAccessionNumber());
                    sampleData.put("sampleType",
                            sampleItem.getTypeOfSample() != null ? sampleItem.getTypeOfSample().getLocalizedName()
                                    : "Unknown");
                    sampleData.put("recommendedMethod", recommendedMethod);
                    sampleData.put("disposalStatus", disposalStatus);
                    sampleData.put("collectionDate", sample.getCollectionDate());
                    sampleData.put("receivedDate", sample.getReceivedTimestamp());

                    // Get patient info
                    Patient patient = sampleHumanService.getPatientForSample(sample);
                    if (patient != null && patient.getPerson() != null) {
                        org.openelisglobal.person.valueholder.Person person = patient.getPerson();
                        sampleData.put("patientName", (person.getFirstName() != null ? person.getFirstName() : "") + " "
                                + (person.getLastName() != null ? person.getLastName() : ""));
                    }

                    results.add(sampleData);
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "getSamplesForDisposal",
                            "Error processing sample item: " + validatedSample.getSampleItemId() + " - "
                                    + e.getMessage());
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return results;
    }

    /**
     * Determines the recommended disposal method based on sample type. Blood &
     * stool → incineration Urine & analyzer waste → chemical treatment
     */
    private String determineDisposalMethod(TypeOfSample sampleType) {
        if (sampleType == null) {
            return "INCINERATION"; // Default
        }

        String typeName = sampleType.getLocalizedName().toLowerCase();
        if (typeName.contains("blood") || typeName.contains("stool") || typeName.contains("serum")
                || typeName.contains("plasma")) {
            return "INCINERATION";
        } else if (typeName.contains("urine") || typeName.contains("analyzer")) {
            return "CHEMICAL_TREATMENT";
        } else {
            return "AUTOCLAVING";
        }
    }

    @Override
    @Transactional
    public Map<String, Object> recordDisposal(List<Integer> sampleIds, String disposalReason, String disposalMethod,
            String disposalDate, String responsiblePerson, String facilityDetails, String notes, Integer notebookPageId,
            String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleIds == null || sampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample IDs provided");
                return result;
            }

            int disposedCount = 0;

            for (Integer sampleId : sampleIds) {
                Sample sample = sampleService.get(String.valueOf(sampleId));
                if (sample != null) {
                    // Get sample items and update their status
                    List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                    for (SampleItem sampleItem : sampleItems) {
                        // Update or create NotebookPageSample for disposal page
                        if (notebookPageId != null) {
                            List<NotebookPageSample> pageSamples = notebookPageSampleService
                                    .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                            boolean foundPageSample = false;
                            for (NotebookPageSample pageSample : pageSamples) {
                                if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                                    // Update with disposal data
                                    Map<String, Object> data = pageSample.getData();
                                    if (data == null) {
                                        data = new HashMap<>();
                                    }
                                    data.put("disposed", true);
                                    data.put("disposalReason", disposalReason);
                                    data.put("disposalMethod", disposalMethod);
                                    data.put("disposalDate", disposalDate);
                                    data.put("responsiblePerson", responsiblePerson);
                                    data.put("facilityDetails", facilityDetails);
                                    data.put("disposalNotes", notes);
                                    data.put("disposedAt", new Timestamp(System.currentTimeMillis()).toString());

                                    pageSample.setData(data);
                                    pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                                    pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                                    pageSample.setSysUserId(sysUserId);
                                    notebookPageSampleService.update(pageSample);
                                    foundPageSample = true;
                                    disposedCount++;
                                    break;
                                }
                            }

                            if (!foundPageSample) {
                                // Create new page sample with disposal data
                                Map<String, Object> data = new HashMap<>();
                                data.put("disposed", true);
                                data.put("disposalReason", disposalReason);
                                data.put("disposalMethod", disposalMethod);
                                data.put("disposalDate", disposalDate);
                                data.put("responsiblePerson", responsiblePerson);
                                data.put("facilityDetails", facilityDetails);
                                data.put("disposalNotes", notes);
                                data.put("disposedAt", new Timestamp(System.currentTimeMillis()).toString());

                                // Create page sample (returns void)
                                notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                        Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.COMPLETED);
                                // Fetch the created page sample
                                NotebookPageSample newPageSample = notebookPageSampleService.getByPageIdAndSampleItemId(
                                        notebookPageId, Integer.valueOf(sampleItem.getId()));
                                if (newPageSample != null) {
                                    newPageSample.setData(data);
                                    notebookPageSampleService.update(newPageSample);
                                    disposedCount++;
                                }
                            }
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("disposedCount", disposedCount);
            result.put("message", "Disposed " + disposedCount + " samples");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording disposal: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getDisposalRecords(Integer entryId) {
        List<Map<String, Object>> records = new ArrayList<>();

        if (entryId == null) {
            return records;
        }

        try {
            // Get the notebook entry
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return records;
            }

            // Get all pages for this notebook
            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            // Find disposal page and get records
            for (NoteBookPage page : pages) {
                if ("end-of-project-archiving".equals(page.getPageId())) {
                    List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                    for (NotebookPageSample pageSample : pageSamples) {
                        Map<String, Object> data = pageSample.getData();
                        if (data != null && Boolean.TRUE.equals(data.get("disposed"))) {
                            SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                            if (sampleItem != null && sampleItem.getSample() != null) {
                                Map<String, Object> record = new HashMap<>(data);
                                record.put("sampleItemId", sampleItem.getId());
                                record.put("labNo", sampleItem.getSample().getAccessionNumber());
                                record.put("sampleType",
                                        sampleItem.getTypeOfSample() != null
                                                ? sampleItem.getTypeOfSample().getLocalizedName()
                                                : "Unknown");
                                records.add(record);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return records;
    }

    @Override
    @Transactional
    public Map<String, Object> recordArchiving(List<Integer> sampleIds, Integer retentionYears, String storageCondition,
            Boolean transferToBiobank, String biobankDetails, String notes, Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            if (sampleIds == null || sampleIds.isEmpty()) {
                result.put("success", false);
                result.put("error", "No sample IDs provided");
                return result;
            }

            int archivedCount = 0;

            for (Integer sampleId : sampleIds) {
                Sample sample = sampleService.get(String.valueOf(sampleId));
                if (sample != null) {
                    List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
                    for (SampleItem sampleItem : sampleItems) {
                        if (notebookPageId != null) {
                            List<NotebookPageSample> pageSamples = notebookPageSampleService
                                    .getBySampleItemId(Integer.valueOf(sampleItem.getId()));

                            boolean foundPageSample = false;
                            for (NotebookPageSample pageSample : pageSamples) {
                                if (pageSample.getNotebookPageId().equals(notebookPageId)) {
                                    Map<String, Object> data = pageSample.getData();
                                    if (data == null) {
                                        data = new HashMap<>();
                                    }
                                    data.put("archived", true);
                                    data.put("retentionYears", retentionYears);
                                    data.put("storageCondition", storageCondition);
                                    data.put("transferToBiobank", transferToBiobank);
                                    data.put("biobankDetails", biobankDetails);
                                    data.put("archiveNotes", notes);
                                    data.put("archivedAt", new Timestamp(System.currentTimeMillis()).toString());

                                    pageSample.setData(data);
                                    pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                                    pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                                    pageSample.setSysUserId(sysUserId);
                                    notebookPageSampleService.update(pageSample);
                                    foundPageSample = true;
                                    archivedCount++;
                                    break;
                                }
                            }

                            if (!foundPageSample) {
                                Map<String, Object> data = new HashMap<>();
                                data.put("archived", true);
                                data.put("retentionYears", retentionYears);
                                data.put("storageCondition", storageCondition);
                                data.put("transferToBiobank", transferToBiobank);
                                data.put("biobankDetails", biobankDetails);
                                data.put("archiveNotes", notes);
                                data.put("archivedAt", new Timestamp(System.currentTimeMillis()).toString());

                                // Create page sample (returns void)
                                notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                        Integer.valueOf(sampleItem.getId()), NotebookPageSample.Status.COMPLETED);
                                // Fetch the created page sample
                                NotebookPageSample newPageSample = notebookPageSampleService.getByPageIdAndSampleItemId(
                                        notebookPageId, Integer.valueOf(sampleItem.getId()));
                                if (newPageSample != null) {
                                    newPageSample.setData(data);
                                    notebookPageSampleService.update(newPageSample);
                                    archivedCount++;
                                }
                            }
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("archivedCount", archivedCount);
            result.put("message", "Archived " + archivedCount + " samples");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording archiving: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getArchivingRecords(Integer entryId) {
        List<Map<String, Object>> records = new ArrayList<>();

        if (entryId == null) {
            return records;
        }

        try {
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return records;
            }

            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            for (NoteBookPage page : pages) {
                if ("end-of-project-archiving".equals(page.getPageId())) {
                    List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                    for (NotebookPageSample pageSample : pageSamples) {
                        Map<String, Object> data = pageSample.getData();
                        if (data != null && Boolean.TRUE.equals(data.get("archived"))) {
                            SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                            if (sampleItem != null && sampleItem.getSample() != null) {
                                Map<String, Object> record = new HashMap<>(data);
                                record.put("sampleItemId", sampleItem.getId());
                                record.put("labNo", sampleItem.getSample().getAccessionNumber());
                                record.put("sampleType",
                                        sampleItem.getTypeOfSample() != null
                                                ? sampleItem.getTypeOfSample().getLocalizedName()
                                                : "Unknown");
                                records.add(record);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return records;
    }

    @Override
    public Map<String, Object> getDisposalArchivingSummary(Integer entryId) {
        Map<String, Object> summary = new HashMap<>();

        if (entryId == null) {
            return summary;
        }

        try {
            List<Map<String, Object>> samplesForDisposal = getSamplesForDisposal(entryId);
            List<Map<String, Object>> disposalRecords = getDisposalRecords(entryId);
            List<Map<String, Object>> archivingRecords = getArchivingRecords(entryId);

            int totalSamples = samplesForDisposal.size();
            int disposedCount = disposalRecords.size();
            int archivedCount = archivingRecords.size();
            int pendingCount = totalSamples - disposedCount - archivedCount;

            summary.put("totalSamples", totalSamples);
            summary.put("disposedCount", disposedCount);
            summary.put("archivedCount", archivedCount);
            summary.put("pendingCount", Math.max(0, pendingCount));
            summary.put("percentComplete",
                    totalSamples > 0 ? ((disposedCount + archivedCount) * 100.0 / totalSamples) : 0);

            // Breakdown by disposal method
            Map<String, Integer> byMethod = new HashMap<>();
            for (Map<String, Object> record : disposalRecords) {
                String method = (String) record.get("disposalMethod");
                byMethod.put(method, byMethod.getOrDefault(method, 0) + 1);
            }
            summary.put("disposalByMethod", byMethod);

            // Breakdown by reason
            Map<String, Integer> byReason = new HashMap<>();
            for (Map<String, Object> record : disposalRecords) {
                String reason = (String) record.get("disposalReason");
                byReason.put(reason, byReason.getOrDefault(reason, 0) + 1);
            }
            summary.put("disposalByReason", byReason);

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return summary;
    }

    // ==================== Accreditation Support Methods ====================

    @Override
    public List<Map<String, Object>> getAuditTrail(Integer entryId) {
        List<Map<String, Object>> auditTrail = new ArrayList<>();

        if (entryId == null) {
            return auditTrail;
        }

        try {
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return auditTrail;
            }

            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            // Build audit trail from page samples
            for (NoteBookPage page : pages) {
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());

                for (NotebookPageSample pageSample : pageSamples) {
                    if (pageSample.getStatus() == NotebookPageSample.Status.COMPLETED) {
                        Map<String, Object> auditEvent = new HashMap<>();
                        auditEvent.put("pageId", page.getPageId());
                        auditEvent.put("pageTitle", page.getTitle());
                        auditEvent.put("sampleItemId", pageSample.getSampleItemId());
                        auditEvent.put("action", "COMPLETED");
                        auditEvent.put("timestamp", pageSample.getCompletedAt());
                        auditEvent.put("userId", pageSample.getSysUserId());

                        // Get sample details
                        SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                        if (sampleItem != null && sampleItem.getSample() != null) {
                            auditEvent.put("labNo", sampleItem.getSample().getAccessionNumber());
                        }

                        auditTrail.add(auditEvent);
                    }
                }
            }

            // Sort by timestamp
            auditTrail.sort((a, b) -> {
                Timestamp ta = (Timestamp) a.get("timestamp");
                Timestamp tb = (Timestamp) b.get("timestamp");
                if (ta == null && tb == null)
                    return 0;
                if (ta == null)
                    return 1;
                if (tb == null)
                    return -1;
                return ta.compareTo(tb);
            });

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return auditTrail;
    }

    @Override
    public Map<String, Object> getSopComplianceStatus(Integer entryId) {
        Map<String, Object> compliance = new HashMap<>();

        if (entryId == null) {
            return compliance;
        }

        try {
            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null || entry.getNotebook() == null) {
                return compliance;
            }

            NoteBook notebook = entry.getNotebook();
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebook.getId());

            // Define SOPs for each workflow page
            List<Map<String, Object>> sopList = new ArrayList<>();

            for (NoteBookPage page : pages) {
                Map<String, Object> sopEntry = new HashMap<>();
                sopEntry.put("pageId", page.getPageId());
                sopEntry.put("pageTitle", page.getTitle());
                sopEntry.put("sopId", "SOP-" + page.getPageId().toUpperCase().replace("-", "_"));
                sopEntry.put("sopName", "Standard Operating Procedure - " + page.getTitle());

                // Check if page has any completed samples (SOP was followed)
                List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(page.getId());
                boolean hasCompletedSamples = pageSamples.stream()
                        .anyMatch(ps -> ps.getStatus() == NotebookPageSample.Status.COMPLETED);

                sopEntry.put("status", hasCompletedSamples ? "COMPLIANT" : "PENDING");
                sopEntry.put("completedSamples", pageSamples.stream()
                        .filter(ps -> ps.getStatus() == NotebookPageSample.Status.COMPLETED).count());
                sopEntry.put("totalSamples", pageSamples.size());

                sopList.add(sopEntry);
            }

            compliance.put("sops", sopList);
            compliance.put("totalSops", sopList.size());
            compliance.put("compliantSops", sopList.stream().filter(s -> "COMPLIANT".equals(s.get("status"))).count());
            compliance.put("overallCompliance",
                    sopList.isEmpty() ? 0
                            : (sopList.stream().filter(s -> "COMPLIANT".equals(s.get("status"))).count() * 100.0
                                    / sopList.size()));

        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return compliance;
    }

    @Override
    @Transactional
    public Map<String, Object> recordSopCompletion(String sopId, String completedBy, String completionDate,
            String notes, Integer notebookPageId, String sysUserId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Store SOP completion in the page data
            if (notebookPageId != null) {
                NoteBookPage page = noteBookPageService.get(notebookPageId);
                if (page != null) {
                    // For now, we log the SOP completion
                    // In a full implementation, this would be stored in a separate SOP tracking
                    // table
                    LogEvent.logInfo(this.getClass().getSimpleName(), "recordSopCompletion", "SOP " + sopId
                            + " completed by " + completedBy + " on " + completionDate + " for page " + notebookPageId);

                    result.put("success", true);
                    result.put("sopId", sopId);
                    result.put("completedBy", completedBy);
                    result.put("completionDate", completionDate);
                    result.put("message", "SOP completion recorded");
                } else {
                    result.put("success", false);
                    result.put("error", "Page not found");
                }
            } else {
                result.put("success", false);
                result.put("error", "Page ID required");
            }

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error recording SOP completion: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> finalizeNotebookEntry(Integer entryId, String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (entryId == null) {
                result.put("success", false);
                result.put("error", "Entry ID required");
                return result;
            }

            NotebookEntry entry = notebookEntryService.get(entryId);
            if (entry == null) {
                result.put("success", false);
                result.put("error", "Entry not found");
                return result;
            }

            // Check if all samples are disposed or archived
            Map<String, Object> summary = getDisposalArchivingSummary(entryId);
            int pendingCount = (Integer) summary.getOrDefault("pendingCount", 0);

            if (pendingCount > 0) {
                result.put("success", false);
                result.put("error", "Cannot finalize: " + pendingCount + " samples still pending disposal/archiving");
                return result;
            }

            // Mark entry as finalized
            entry.setStatus(NotebookEntry.EntryStatus.FINALIZED);
            entry.setSysUserId(sysUserId);
            notebookEntryService.update(entry);

            // Log finalization
            LogEvent.logInfo(this.getClass().getSimpleName(), "finalizeNotebookEntry",
                    "Notebook entry " + entryId + " finalized by user " + sysUserId);

            result.put("success", true);
            result.put("entryId", entryId);
            result.put("message", "Notebook entry finalized successfully. No further modifications allowed.");

        } catch (Exception e) {
            LogEvent.logError(e);
            result.put("success", false);
            result.put("error", "Error finalizing notebook entry: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get the entity class for a storage location type.
     *
     * @param locationType the location type string (box, rack, shelf, device)
     * @return the corresponding entity class, or null if unknown
     */
    private Class<?> getStorageEntityClass(String locationType) {
        if (locationType == null) {
            return null;
        }
        switch (locationType) {
        case "box":
            return org.openelisglobal.storage.valueholder.StorageBox.class;
        case "rack":
            return org.openelisglobal.storage.valueholder.StorageRack.class;
        case "shelf":
            return org.openelisglobal.storage.valueholder.StorageShelf.class;
        case "device":
            return org.openelisglobal.storage.valueholder.StorageDevice.class;
        default:
            return null;
        }
    }
}
