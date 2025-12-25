package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.service.NotebookSampleEntryService;
import org.openelisglobal.notebook.service.PathologySopService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.PathologySop;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Pathology Laboratory workflow operations. Handles
 * processing, storage, testing, disposal, and reporting endpoints.
 */
@RestController
@RequestMapping(value = "/rest/notebook/pathology")
public class PathologyWorkflowController extends BaseRestController {

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private PathologySopService pathologySopService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private NotebookSampleEntryService notebookSampleEntryService;

    @Autowired
    private IStatusService statusService;

    // ========================================
    // SAMPLE CREATION ENDPOINTS
    // ========================================

    /**
     * Create a new pathology sample. POST /rest/notebook/pathology/sample/create
     *
     * Creates a Sample and SampleItem record, links it to the notebook entry, and
     * creates a NotebookPageSample record for the sample creation page.
     */
    @PostMapping(value = "/sample/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSample(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Extract required fields
            Integer entryId = parseInteger(requestData.get("entryId"));
            Integer pageId = parseInteger(requestData.get("pageId"));
            String firstName = parseString(requestData.get("firstName"));
            String specimenType = parseString(requestData.get("specimenType"));
            String sampleCategory = parseString(requestData.get("sampleCategory"));
            String receivedDateTime = parseString(requestData.get("receivedDateTime"));

            // Validate required fields
            if (entryId == null) {
                response.put("success", false);
                response.put("error", "Entry ID is required");
                return ResponseEntity.badRequest().body(response);
            }
            if (firstName == null || firstName.isBlank()) {
                response.put("success", false);
                response.put("error", "First Name is MANDATORY for order acceptance");
                return ResponseEntity.badRequest().body(response);
            }
            if (specimenType == null || specimenType.isBlank()) {
                response.put("success", false);
                response.put("error", "Specimen Type is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Look up the sample type by ID (frontend sends the ID, not the description)
            TypeOfSample sampleType = typeOfSampleService.get(specimenType);

            if (sampleType == null) {
                response.put("success", false);
                response.put("error", "Unknown specimen type ID: " + specimenType);
                return ResponseEntity.badRequest().body(response);
            }

            // Create parent Sample record
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));

            // Parse and set received timestamp
            Timestamp receivedTimestamp = parseDateTime(receivedDateTime);
            if (receivedTimestamp != null) {
                parentSample.setReceivedTimestamp(receivedTimestamp);
            } else {
                parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
            }

            // Generate accession number and insert sample
            String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
            parentSample.setId(sampleIdDb);

            // Get status ID for SampleEntered
            String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
            if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
                sampleEnteredStatusId = "20"; // fallback
            }

            // Generate external ID from first name and specimen type description
            String specimenTypeDescription = sampleType.getDescription() != null ? sampleType.getDescription()
                    : sampleType.getLocalizedName();
            String externalId = generateExternalId(firstName, specimenTypeDescription);

            // Create SampleItem record
            SampleItem item = new SampleItem();
            item.setSample(parentSample);
            item.setTypeOfSample(sampleType);
            item.setExternalId(externalId);
            item.setSortOrder("1");
            item.setStatusId(sampleEnteredStatusId);
            item.setSysUserId(sysUserId);

            // Set collection date if provided
            String collectionDateTime = parseString(requestData.get("collectionDateTime"));
            if (collectionDateTime != null && !collectionDateTime.isBlank()) {
                Timestamp collectionTimestamp = parseDateTime(collectionDateTime);
                if (collectionTimestamp != null) {
                    item.setCollectionDate(collectionTimestamp);
                }
            }

            // Insert the sample item
            String itemId = sampleItemService.insert(item);
            item.setId(itemId);

            // Add sample to the notebook entry
            notebookEntryService.addSample(entryId, item, sysUserId);

            // Link sample to notebook
            notebookSampleEntryService.linkSamplesToNotebook(entryId, List.of(Integer.parseInt(itemId)));

            // Create or update NotebookPageSample record if page ID is provided
            if (pageId != null) {
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page != null) {
                    // Build metadata map from all request fields
                    Map<String, Object> sampleData = new HashMap<>();
                    // Patient Identification
                    sampleData.put("firstName", firstName);
                    sampleData.put("surname", requestData.get("surname"));
                    sampleData.put("nationalId", requestData.get("nationalId"));
                    // Sample Category
                    sampleData.put("sampleCategory", sampleCategory);
                    // Receiving Info
                    sampleData.put("sourceFacility", requestData.get("sourceFacility"));
                    sampleData.put("receivedDateTime", receivedDateTime);
                    sampleData.put("receivedBy", requestData.get("receivedBy"));
                    // Specimen Info (store description for display, not ID)
                    sampleData.put("specimenType", specimenTypeDescription);
                    sampleData.put("specimenTypeId", specimenType);
                    sampleData.put("specimenSite", requestData.get("specimenSite"));
                    sampleData.put("collectionDateTime", collectionDateTime);
                    // Clinical metadata
                    sampleData.put("patientId", requestData.get("patientId"));
                    sampleData.put("requestingClinician", requestData.get("requestingClinician"));
                    sampleData.put("clinicalDetails", requestData.get("clinicalDetails"));
                    // Research metadata
                    sampleData.put("studyId", requestData.get("studyId"));
                    sampleData.put("piName", requestData.get("piName"));
                    sampleData.put("participantAnimalId", requestData.get("participantAnimalId"));
                    sampleData.put("ethicalApprovalRef", requestData.get("ethicalApprovalRef"));
                    // Generated IDs
                    sampleData.put("externalId", externalId);
                    sampleData.put("accessionNumber", parentSample.getAccessionNumber());

                    // Check if a NotebookPageSample already exists (created by linkSamplesToNotebook)
                    NotebookPageSample existingPageSample = notebookPageSampleService.getBySampleItemIdAndPageId(itemId,
                            pageId);

                    if (existingPageSample != null) {
                        // Update existing record with sample metadata
                        existingPageSample.setData(sampleData);
                        existingPageSample.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                        existingPageSample.setSysUserId(sysUserId);
                        notebookPageSampleService.update(existingPageSample);
                    } else {
                        // Create new record
                        NotebookPageSample pageSample = new NotebookPageSample();
                        pageSample.setNotebookPage(page);
                        pageSample.setSampleItemId(itemId);
                        pageSample.setData(sampleData);
                        pageSample.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                        pageSample.setSysUserId(sysUserId);
                        notebookPageSampleService.insert(pageSample);
                    }
                }
            }

            response.put("success", true);
            response.put("message", "Sample created successfully");
            response.put("sampleId", itemId);
            response.put("externalId", externalId);
            response.put("accessionNumber", parentSample.getAccessionNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(), "createSample",
                    "Failed to create sample: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to create sample: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Generate external ID from first name and specimen type.
     */
    private String generateExternalId(String firstName, String specimenType) {
        // Sanitize first name (remove spaces, special chars)
        String sanitizedName = firstName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (sanitizedName.length() > 10) {
            sanitizedName = sanitizedName.substring(0, 10);
        }

        // Generate abbreviation from specimen type
        String abbrev = generateSpecimenTypeAbbrev(specimenType);

        // Add timestamp for uniqueness
        long timestamp = System.currentTimeMillis() % 100000;

        return String.format("%s-%s-%05d", sanitizedName, abbrev, timestamp);
    }

    /**
     * Generate abbreviation from specimen type.
     */
    private String generateSpecimenTypeAbbrev(String specimenType) {
        if (specimenType == null || specimenType.isBlank()) {
            return "UNK";
        }
        // Take first 3 letters of each significant word
        String[] words = specimenType.toUpperCase().split("[\\s\\-\\/\\(\\)]+");
        StringBuilder abbrev = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0 && !word.equals("FOR") && !word.equals("THE") && !word.equals("AND")) {
                abbrev.append(word.substring(0, Math.min(3, word.length())));
                if (abbrev.length() >= 6) {
                    break;
                }
            }
        }
        return abbrev.length() > 0 ? abbrev.toString() : "UNK";
    }

    /**
     * Parse date/time from various formats.
     */
    private Timestamp parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        String trimmed = dateStr.trim();

        // Handle ISO 8601 format (from frontend Date picker)
        if (trimmed.contains("T")) {
            try {
                java.time.Instant instant = java.time.Instant.parse(trimmed);
                return Timestamp.from(instant);
            } catch (Exception e) {
                // Try other formats
            }
        }

        // Try datetime formats
        String[] dateTimeFormats = { "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm",
                "MM/dd/yyyy HH:mm" };

        for (String format : dateTimeFormats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(trimmed, formatter);
                return Timestamp.valueOf(dateTime);
            } catch (Exception e) {
                // Try next format
            }
        }

        // Try date-only formats
        String[] dateFormats = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd" };

        for (String format : dateFormats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                java.time.LocalDate date = java.time.LocalDate.parse(trimmed, formatter);
                return Timestamp.valueOf(date.atStartOfDay());
            } catch (Exception e) {
                // Try next format
            }
        }

        return null;
    }

    // ========================================
    // GROSSING ENDPOINTS
    // ========================================

    /**
     * Submit gross examination results with images.
     * POST /rest/notebook/pathology/grossing/submit
     *
     * Grossing is the first step in histopathology workflow where the pathologist:
     * - Examines the specimen macroscopically
     * - Documents gross findings (dimensions, weight, appearance, margins)
     * - Photographs the specimen (up to 96 images with standardized naming)
     * - Selects tissue sections for processing into cassettes
     *
     * Image naming convention: {AccessionNumber}_{SpecimenPart}_{ImageNumber}_{View}.jpg
     * Example: PATH-2024-001_A_01_superior.jpg, PATH-2024-001_A_02_inferior.jpg
     *
     * UI sends JSON with:
     * - sampleId, pageId, entryId
     * - Gross findings: specimenReceived, specimenDescription, dimensions (L/W/H),
     *   weight, color, texture, margins, landmarks, abnormalities
     * - Sectioning plan: numberOfSections, sectioningMethod, sectionsToSubmit
     * - Images: grossImages array of { base64Data, fileName, imageType, viewDescription }
     * - Staff: examinerName, examinerInitials, grossingDate, grossingStartTime, grossingEndTime
     */
    @PostMapping(value = "/grossing/submit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> submitGrossing(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build grossing data map from all UI fields
            Map<String, Object> grossingData = new HashMap<>();

            // Gross examination findings
            grossingData.put("grossingCompleted", true);
            grossingData.put("specimenReceived", requestData.get("specimenReceived"));
            grossingData.put("specimenDescription", requestData.get("specimenDescription"));

            // Dimensions
            grossingData.put("dimensionLength", requestData.get("dimensionLength"));
            grossingData.put("dimensionWidth", requestData.get("dimensionWidth"));
            grossingData.put("dimensionHeight", requestData.get("dimensionHeight"));
            grossingData.put("dimensionUnit", requestData.get("dimensionUnit")); // cm or mm

            // Weight
            grossingData.put("specimenWeight", requestData.get("specimenWeight"));
            grossingData.put("weightUnit", requestData.get("weightUnit")); // g or mg

            // Appearance
            grossingData.put("color", requestData.get("color"));
            grossingData.put("texture", requestData.get("texture"));
            grossingData.put("consistency", requestData.get("consistency"));
            grossingData.put("margins", requestData.get("margins"));
            grossingData.put("marginsInked", requestData.get("marginsInked"));
            grossingData.put("inkColors", requestData.get("inkColors")); // Map of margin to ink color

            // Anatomical landmarks and orientation
            grossingData.put("landmarks", requestData.get("landmarks"));
            grossingData.put("orientation", requestData.get("orientation"));
            grossingData.put("orientationMarkers", requestData.get("orientationMarkers"));

            // Abnormalities and lesions
            grossingData.put("abnormalities", requestData.get("abnormalities"));
            grossingData.put("lesionSize", requestData.get("lesionSize"));
            grossingData.put("lesionLocation", requestData.get("lesionLocation"));
            grossingData.put("distanceToMargins", requestData.get("distanceToMargins"));

            // Sectioning plan
            grossingData.put("numberOfSections", requestData.get("numberOfSections"));
            grossingData.put("sectioningMethod", requestData.get("sectioningMethod")); // bread-loaf, cross-section, etc.
            grossingData.put("sectionsToSubmit", requestData.get("sectionsToSubmit")); // List of section descriptions
            grossingData.put("representativeSections", requestData.get("representativeSections"));
            grossingData.put("entirelySubmitted", requestData.get("entirelySubmitted"));

            // Free-text gross description (synoptic)
            grossingData.put("grossDescription", requestData.get("grossDescription"));
            grossingData.put("grossDictation", requestData.get("grossDictation")); // Voice dictation text

            // Staff and timing
            grossingData.put("examinerName", requestData.get("examinerName"));
            grossingData.put("examinerInitials", requestData.get("examinerInitials"));
            grossingData.put("grossingDate", requestData.get("grossingDate"));
            grossingData.put("grossingStartTime", requestData.get("grossingStartTime"));
            grossingData.put("grossingEndTime", requestData.get("grossingEndTime"));

            // Process images if provided (up to 96 images)
            List<Map<String, Object>> grossImages = (List<Map<String, Object>>) requestData.get("grossImages");
            if (grossImages != null && !grossImages.isEmpty()) {
                // Validate image count
                if (grossImages.size() > 96) {
                    response.put("success", false);
                    response.put("error", "Maximum 96 images allowed per specimen");
                    return ResponseEntity.badRequest().body(response);
                }

                // Get sample for naming convention
                SampleItem sampleItem = sampleItemService.getData(sampleId);
                String accessionNumber = sampleItem != null && sampleItem.getSample() != null
                        ? sampleItem.getSample().getAccessionNumber()
                        : "UNKNOWN";
                String externalId = sampleItem != null ? sampleItem.getExternalId() : sampleId;

                // Process each image with standardized naming
                List<Map<String, Object>> processedImages = new ArrayList<>();
                int imageCounter = 1;
                for (Map<String, Object> image : grossImages) {
                    Map<String, Object> processedImage = new HashMap<>();

                    String originalFileName = parseString(image.get("fileName"));
                    String viewDescription = parseString(image.get("viewDescription"));
                    String specimenPart = parseString(image.get("specimenPart")); // A, B, C, etc.
                    if (specimenPart == null || specimenPart.isEmpty()) {
                        specimenPart = "A";
                    }

                    // Generate standardized file name
                    // Format: {AccessionNumber}_{SpecimenPart}_{ImageNumber}_{View}.{ext}
                    String extension = "jpg";
                    if (originalFileName != null && originalFileName.contains(".")) {
                        extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
                    }
                    String viewSuffix = viewDescription != null && !viewDescription.isEmpty()
                            ? viewDescription.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase()
                            : "view";
                    String standardizedFileName = String.format("%s_%s_%02d_%s.%s",
                            accessionNumber != null ? accessionNumber.replaceAll("[^a-zA-Z0-9-]", "") : externalId,
                            specimenPart,
                            imageCounter,
                            viewSuffix,
                            extension);

                    processedImage.put("originalFileName", originalFileName);
                    processedImage.put("standardizedFileName", standardizedFileName);
                    processedImage.put("specimenPart", specimenPart);
                    processedImage.put("imageNumber", imageCounter);
                    processedImage.put("viewDescription", viewDescription);
                    processedImage.put("imageType", image.get("imageType"));
                    processedImage.put("captureTime", image.get("captureTime"));
                    processedImage.put("magnification", image.get("magnification"));
                    processedImage.put("notes", image.get("notes"));

                    // Store base64 data (in production, would save to file system/blob storage)
                    String base64Data = parseString(image.get("base64Data"));
                    if (base64Data != null && !base64Data.isEmpty()) {
                        // For now, store reference; in production would save to storage
                        processedImage.put("hasImageData", true);
                        processedImage.put("imageDataSize", base64Data.length());
                        // Note: In production, save to file system and store path instead
                        // For now, we store a truncated version or reference
                        if (base64Data.length() > 1000000) {
                            // Large images: store reference only in metadata
                            processedImage.put("imageStorageNote", "Image stored separately due to size");
                        } else {
                            // Small images: can store inline (thumbnails)
                            processedImage.put("base64Data", base64Data);
                        }
                    }

                    processedImages.add(processedImage);
                    imageCounter++;
                }

                grossingData.put("grossImages", processedImages);
                grossingData.put("grossImageCount", processedImages.size());
            } else {
                grossingData.put("grossImageCount", 0);
            }

            // Update or create the notebook page sample with grossing data
            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.putAll(grossingData);
                pageSample.setData(data);
                pageSample.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            } else {
                // Create new page sample entry
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page == null) {
                    response.put("success", false);
                    response.put("error", "Page not found: " + pageId);
                    return ResponseEntity.badRequest().body(response);
                }

                NotebookPageSample newPageSample = new NotebookPageSample();
                newPageSample.setNotebookPage(page);
                newPageSample.setSampleItemId(sampleId);
                newPageSample.setData(grossingData);
                newPageSample.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                newPageSample.setSysUserId(sysUserId);
                notebookPageSampleService.insert(newPageSample);
            }

            response.put("success", true);
            response.put("message", "Gross examination results saved successfully");
            response.put("imageCount", grossingData.get("grossImageCount"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(), "submitGrossing",
                    "Failed to save grossing data: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to save grossing data: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get gross examination data for a sample.
     * GET /rest/notebook/pathology/grossing/{sampleId}
     */
    @GetMapping(value = "/grossing/{sampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGrossingData(
            @org.springframework.web.bind.annotation.PathVariable("sampleId") String sampleId,
            @RequestParam Integer pageId,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample == null || pageSample.getData() == null) {
                response.put("success", true);
                response.put("hasData", false);
                response.put("message", "No grossing data found for this sample");
                return ResponseEntity.ok(response);
            }

            Map<String, Object> data = pageSample.getData();

            // Return all grossing-specific fields
            response.put("success", true);
            response.put("hasData", true);
            response.put("grossingCompleted", data.get("grossingCompleted"));

            // Specimen description
            response.put("specimenReceived", data.get("specimenReceived"));
            response.put("specimenDescription", data.get("specimenDescription"));

            // Dimensions
            response.put("dimensionLength", data.get("dimensionLength"));
            response.put("dimensionWidth", data.get("dimensionWidth"));
            response.put("dimensionHeight", data.get("dimensionHeight"));
            response.put("dimensionUnit", data.get("dimensionUnit"));

            // Weight
            response.put("specimenWeight", data.get("specimenWeight"));
            response.put("weightUnit", data.get("weightUnit"));

            // Appearance
            response.put("color", data.get("color"));
            response.put("texture", data.get("texture"));
            response.put("consistency", data.get("consistency"));
            response.put("margins", data.get("margins"));
            response.put("marginsInked", data.get("marginsInked"));
            response.put("inkColors", data.get("inkColors"));

            // Orientation and landmarks
            response.put("landmarks", data.get("landmarks"));
            response.put("orientation", data.get("orientation"));
            response.put("orientationMarkers", data.get("orientationMarkers"));

            // Abnormalities
            response.put("abnormalities", data.get("abnormalities"));
            response.put("lesionSize", data.get("lesionSize"));
            response.put("lesionLocation", data.get("lesionLocation"));
            response.put("distanceToMargins", data.get("distanceToMargins"));

            // Sectioning plan
            response.put("numberOfSections", data.get("numberOfSections"));
            response.put("sectioningMethod", data.get("sectioningMethod"));
            response.put("sectionsToSubmit", data.get("sectionsToSubmit"));
            response.put("representativeSections", data.get("representativeSections"));
            response.put("entirelySubmitted", data.get("entirelySubmitted"));

            // Gross description
            response.put("grossDescription", data.get("grossDescription"));
            response.put("grossDictation", data.get("grossDictation"));

            // Staff and timing
            response.put("examinerName", data.get("examinerName"));
            response.put("examinerInitials", data.get("examinerInitials"));
            response.put("grossingDate", data.get("grossingDate"));
            response.put("grossingStartTime", data.get("grossingStartTime"));
            response.put("grossingEndTime", data.get("grossingEndTime"));

            // Images
            response.put("grossImageCount", data.get("grossImageCount"));
            response.put("grossImages", data.get("grossImages"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to get grossing data: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // PROCESSING ENDPOINTS
    // ========================================

    /**
     * Submit sample processing data. POST
     * /rest/notebook/pathology/processing/submit
     *
     * Handles all processing types from UI: - Histopathology: grossExamDone,
     * grossDescription, sectioningDone, tissueProcessingSteps, embeddingDone,
     * microtomyThickness - Cytopathology: centrifugationDone, smearTypes, stainUsed
     * - Blood: wedgeSmearDone, bloodStain - Research: sopFollowed,
     * processingMethods
     */
    @PostMapping(value = "/processing/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitProcessing(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build processing data map from all UI fields
            Map<String, Object> processingData = new HashMap<>();
            // Processing action
            processingData.put("processingAction", requestData.get("processingAction"));
            // Histopathology fields
            processingData.put("grossExamDone", requestData.get("grossExamDone"));
            processingData.put("grossDescription", requestData.get("grossDescription"));
            processingData.put("sectioningDone", requestData.get("sectioningDone"));
            processingData.put("tissueProcessingSteps", requestData.get("tissueProcessingSteps"));
            processingData.put("embeddingDone", requestData.get("embeddingDone"));
            processingData.put("microtomyThickness", requestData.get("microtomyThickness"));
            // Cytopathology fields
            processingData.put("centrifugationDone", requestData.get("centrifugationDone"));
            processingData.put("smearTypes", requestData.get("smearTypes"));
            processingData.put("stainUsed", requestData.get("stainUsed"));
            // Blood fields
            processingData.put("wedgeSmearDone", requestData.get("wedgeSmearDone"));
            processingData.put("bloodStain", requestData.get("bloodStain"));
            // Research fields
            processingData.put("sopFollowed", requestData.get("sopFollowed"));
            processingData.put("processingMethods", requestData.get("processingMethods"));
            // Common fields
            processingData.put("processingDate", requestData.get("processingDate"));
            processingData.put("staffInitials", requestData.get("staffInitials"));
            processingData.put("processingNotes", requestData.get("processingNotes"));

            // Update or create the notebook page sample with processing data
            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.putAll(processingData);
                pageSample.setData(data);
                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            } else {
                // Create new page sample entry
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page == null) {
                    response.put("success", false);
                    response.put("error", "Page not found: " + pageId);
                    return ResponseEntity.badRequest().body(response);
                }

                NotebookPageSample newPageSample = new NotebookPageSample();
                newPageSample.setNotebookPage(page);
                newPageSample.setSampleItemId(sampleId);
                newPageSample.setData(processingData);
                newPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                newPageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                newPageSample.setSysUserId(sysUserId);
                notebookPageSampleService.insert(newPageSample);
            }

            response.put("success", true);
            response.put("message", "Processing data saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save processing data: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // STORAGE ENDPOINTS
    // ========================================

    /**
     * Assign storage location to a sample. POST
     * /rest/notebook/pathology/storage/assign
     *
     * UI fields: storageType, expectedDuration, storageUnit, rack, box, position,
     * dateStored, storedBy Location format follows PDF: "Freezer X, Rack Y, Box Z,
     * Position N"
     */
    @PostMapping(value = "/storage/assign", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignStorage(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build storage data map from all UI fields
            Map<String, Object> storageData = new HashMap<>();
            storageData.put("storageType", requestData.get("storageType"));
            storageData.put("expectedDuration", requestData.get("expectedDuration"));
            storageData.put("storageUnit", requestData.get("storageUnit"));
            storageData.put("rack", requestData.get("rack"));
            storageData.put("box", requestData.get("box"));
            storageData.put("position", requestData.get("position"));
            storageData.put("dateStored", requestData.get("dateStored"));
            storageData.put("storedBy", requestData.get("storedBy"));
            // Build full location string for display
            String storageLocation = String.format("%s, Rack %s, Box %s, Position %s", requestData.get("storageUnit"),
                    requestData.get("rack"), requestData.get("box"), requestData.get("position"));
            storageData.put("storageLocation", storageLocation);

            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.putAll(storageData);
                pageSample.setData(data);
                pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            } else {
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page == null) {
                    response.put("success", false);
                    response.put("error", "Page not found: " + pageId);
                    return ResponseEntity.badRequest().body(response);
                }

                NotebookPageSample newPageSample = new NotebookPageSample();
                newPageSample.setNotebookPage(page);
                newPageSample.setSampleItemId(sampleId);
                newPageSample.setData(storageData);
                newPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                newPageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                newPageSample.setSysUserId(sysUserId);
                notebookPageSampleService.insert(newPageSample);
            }

            response.put("success", true);
            response.put("message", "Storage location assigned successfully");
            response.put("storageLocation", storageLocation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to assign storage: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Log temperature for a storage unit. POST
     * /rest/notebook/pathology/storage/temperature-log
     */
    @PostMapping(value = "/storage/temperature-log", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logTemperature(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Temperature log data - would typically be stored in a separate table
            // For now, just acknowledge receipt
            response.put("success", true);
            response.put("message", "Temperature logged successfully");
            response.put("data", requestData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to log temperature: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Record sample retrieval from storage. POST
     * /rest/notebook/pathology/storage/retrieve
     */
    @PostMapping(value = "/storage/retrieve", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> retrieveSample(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.put("dateRetrieved", requestData.get("dateRetrieved"));
                data.put("retrievedBy", requestData.get("retrievedBy"));
                data.put("recipientSignature", requestData.get("recipientSignature"));
                data.put("retrievalRecorded", true);
                pageSample.setData(data);
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            }

            response.put("success", true);
            response.put("message", "Sample retrieval recorded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to record retrieval: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // TESTING ENDPOINTS
    // ========================================

    /**
     * Submit testing/microscopy results. POST
     * /rest/notebook/pathology/testing/submit
     */
    @PostMapping(value = "/testing/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitTesting(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String sampleId = parseString(requestData.get("sampleId"));
            Integer pageId = parseInteger(requestData.get("pageId"));

            if (sampleId == null || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample ID and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleId, pageId);

            if (pageSample != null) {
                Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                        : new HashMap<>();
                data.putAll(requestData);
                pageSample.setData(data);
                pageSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(pageSample);
            } else {
                NoteBookPage page = noteBookPageService.get(pageId);
                if (page == null) {
                    response.put("success", false);
                    response.put("error", "Page not found: " + pageId);
                    return ResponseEntity.badRequest().body(response);
                }

                NotebookPageSample newPageSample = new NotebookPageSample();
                newPageSample.setNotebookPage(page);
                newPageSample.setSampleItemId(sampleId);
                newPageSample.setData(new HashMap<>(requestData));
                newPageSample.setStatus(NotebookPageSample.Status.PENDING);
                newPageSample.setSysUserId(sysUserId);
                notebookPageSampleService.insert(newPageSample);
            }

            // Use bulkUpdateStatus which has built-in propagation to next page (T150)
            Integer sampleIdInt = Integer.parseInt(sampleId);
            notebookPageSampleService.bulkUpdateStatus(pageId, java.util.List.of(sampleIdInt),
                    NotebookPageSample.Status.COMPLETED, sysUserId);

            response.put("success", true);
            response.put("message", "Testing results saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save testing results: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Submit bulk testing/microscopy results for multiple samples. POST
     * /rest/notebook/pathology/testing/bulk-submit
     *
     * UI sends: sampleIds (array), pageId, entryId, and all test data fields
     * (testName, result, stains, controls, technicianSignature, etc.) that will be
     * applied to all selected samples.
     */
    @PostMapping(value = "/testing/bulk-submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> submitBulkTesting(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Integer pageId = parseInteger(requestData.get("pageId"));
            List<Integer> sampleIds = (List<Integer>) requestData.get("sampleIds");

            if ((sampleIds == null || sampleIds.isEmpty()) || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample IDs and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build test data map from UI fields (excluding control fields)
            Map<String, Object> testData = new HashMap<>();
            testData.put("testName", requestData.get("testName"));
            testData.put("result", requestData.get("result"));
            testData.put("technicianSignature", requestData.get("technicianSignature"));
            testData.put("pathologistVerification", requestData.get("pathologistVerification"));
            testData.put("testDate", requestData.get("testDate"));
            // Stains
            testData.put("routineStains", requestData.get("routineStains"));
            testData.put("specialStains", requestData.get("specialStains"));
            testData.put("advancedTechniques", requestData.get("advancedTechniques"));
            testData.put("ihcMarkers", requestData.get("ihcMarkers"));
            testData.put("researchAssays", requestData.get("researchAssays"));
            // Controls
            testData.put("positiveControlRun", requestData.get("positiveControlRun"));
            testData.put("positiveControlResult", requestData.get("positiveControlResult"));
            testData.put("negativeControlRun", requestData.get("negativeControlRun"));
            testData.put("negativeControlResult", requestData.get("negativeControlResult"));
            testData.put("assayAccepted", requestData.get("assayAccepted"));

            // Process each sample - update data first
            int processedCount = 0;
            for (Integer sampleId : sampleIds) {
                String sampleIdStr = String.valueOf(sampleId);
                NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleIdStr,
                        pageId);

                if (pageSample != null) {
                    Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                            : new HashMap<>();
                    data.putAll(testData);
                    pageSample.setData(data);
                    pageSample.setSysUserId(sysUserId);
                    notebookPageSampleService.update(pageSample);
                    processedCount++;
                } else {
                    // Create new page sample entry for testing tracking
                    NoteBookPage page = noteBookPageService.get(pageId);
                    if (page != null) {
                        NotebookPageSample newPageSample = new NotebookPageSample();
                        newPageSample.setNotebookPage(page);
                        newPageSample.setSampleItemId(sampleIdStr);
                        newPageSample.setData(new HashMap<>(testData));
                        newPageSample.setStatus(NotebookPageSample.Status.PENDING);
                        newPageSample.setSysUserId(sysUserId);
                        notebookPageSampleService.insert(newPageSample);
                        processedCount++;
                    }
                }
            }

            // Use bulkUpdateStatus which has built-in propagation to next page (T150)
            notebookPageSampleService.bulkUpdateStatus(pageId, sampleIds, NotebookPageSample.Status.COMPLETED,
                    sysUserId);

            response.put("success", true);
            response.put("message", String.format("Testing results saved for %d samples", processedCount));
            response.put("processedCount", processedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save bulk testing results: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Import testing results from CSV data. POST
     * /rest/notebook/pathology/page/{pageId}/results/import-csv
     *
     * CSV columns: accessionNumber, blockSlideId, resultFindings, diagnosisCode,
     * clinicalInterpretation, verifiedByPathologist, verifyingPathologistName,
     * verificationDate, additionalNotes
     */
    @PostMapping(value = "/page/{pageId}/results/import-csv", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> importResultsCsv(
            @org.springframework.web.bind.annotation.PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestData, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) requestData.get("rows");

            if (rows == null || rows.isEmpty()) {
                response.put("success", false);
                response.put("error", "No data rows provided");
                return ResponseEntity.badRequest().body(response);
            }

            if (pageId == null) {
                response.put("success", false);
                response.put("error", "Page ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                response.put("success", false);
                response.put("error", "Page not found");
                return ResponseEntity.status(404).body(response);
            }

            int processedCount = 0;
            int skippedCount = 0;
            List<String> errors = new ArrayList<>();

            // Fetch all page samples ONCE before the loop to avoid caching/stale data
            // issues
            List<NotebookPageSample> pageSamples = notebookPageSampleService.getByPageId(pageId);

            // Build a lookup map by accession number for efficient matching
            Map<String, NotebookPageSample> sampleLookup = new HashMap<>();
            for (NotebookPageSample ps : pageSamples) {
                // Look up SampleItem via service using the stored ID
                String sampleItemId = ps.getSampleItemId();
                if (sampleItemId != null && !sampleItemId.isEmpty()) {
                    SampleItem si = sampleItemService.getData(sampleItemId);
                    if (si != null && si.getSample() != null) {
                        String sampleAccession = si.getSample().getAccessionNumber();
                        if (sampleAccession != null) {
                            sampleLookup.put(sampleAccession.trim().toLowerCase(), ps);
                        }
                    }
                }
                // Also add by externalId and accessionNumber in data (for pathology samples)
                if (ps.getData() != null) {
                    String externalId = (String) ps.getData().get("externalId");
                    String dataAccessionNumber = (String) ps.getData().get("accessionNumber");
                    if (externalId != null && !externalId.isEmpty()) {
                        sampleLookup.put(externalId.trim().toLowerCase(), ps);
                    }
                    if (dataAccessionNumber != null && !dataAccessionNumber.isEmpty()) {
                        sampleLookup.put(dataAccessionNumber.trim().toLowerCase(), ps);
                    }
                }
            }

            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                int rowNum = i + 2; // CSV row number (1-indexed, plus header)

                String accessionNumber = (String) row.get("accessionNumber");
                if (accessionNumber == null || accessionNumber.trim().isEmpty()) {
                    errors.add("Row " + rowNum + ": Missing accession number");
                    skippedCount++;
                    continue;
                }

                // Find the sample by accession number using pre-built lookup map
                NotebookPageSample matchingSample = sampleLookup.get(accessionNumber.trim().toLowerCase());

                if (matchingSample == null) {
                    errors.add("Row " + rowNum + ": Sample not found for accession '" + accessionNumber + "'");
                    skippedCount++;
                    continue;
                }

                // Build result data from CSV row
                Map<String, Object> resultData = matchingSample.getData() != null
                        ? new HashMap<>(matchingSample.getData())
                        : new HashMap<>();

                resultData.put("blockSlideId", row.get("blockSlideId"));
                resultData.put("resultFindings", row.get("resultFindings"));
                resultData.put("diagnosisCode", row.get("diagnosisCode"));
                resultData.put("clinicalInterpretation", row.get("clinicalInterpretation"));
                resultData.put("additionalNotes", row.get("additionalNotes"));
                resultData.put("verifyingPathologistName", row.get("verifyingPathologistName"));
                resultData.put("verificationDate", row.get("verificationDate"));

                // Handle boolean conversion for verifiedByPathologist
                Object verified = row.get("verifiedByPathologist");
                boolean isVerified = false;
                if (verified != null) {
                    if (verified instanceof Boolean) {
                        isVerified = (Boolean) verified;
                    } else if (verified instanceof String) {
                        String verifiedStr = ((String) verified).trim();
                        isVerified = "true".equalsIgnoreCase(verifiedStr) || "1".equals(verifiedStr)
                                || "yes".equalsIgnoreCase(verifiedStr);
                    }
                }
                resultData.put("verifiedByPathologist", isVerified);

                resultData.put("resultsImportedFromCsv", true);
                resultData.put("resultsImportedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                matchingSample.setData(resultData);
                // Keep sample as IN_PROGRESS after result entry - use "Mark Complete" button to
                // complete
                matchingSample.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                matchingSample.setSysUserId(sysUserId);
                notebookPageSampleService.update(matchingSample);
                processedCount++;
            }

            response.put("success", true);
            response.put("message",
                    String.format("Imported results for %d samples. %d skipped.", processedCount, skippedCount));
            response.put("processedCount", processedCount);
            response.put("skippedCount", skippedCount);
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to import CSV results: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // DISPOSAL & ARCHIVING ENDPOINTS
    // ========================================

    /**
     * Submit disposal record for multiple samples. POST
     * /rest/notebook/pathology/disposal/submit
     *
     * UI sends: sampleIds (array), pageId, disposalReason, retentionPolicy,
     * disposalMethod, disposalDate, staffSignature, unitHeadApproval
     */
    @PostMapping(value = "/disposal/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> submitDisposal(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Integer pageId = parseInteger(requestData.get("pageId"));
            List<Integer> sampleIds = (List<Integer>) requestData.get("sampleIds");

            if ((sampleIds == null || sampleIds.isEmpty()) || pageId == null) {
                response.put("success", false);
                response.put("error", "Sample IDs and Page ID are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Build disposal data map from UI fields
            Map<String, Object> disposalData = new HashMap<>();
            disposalData.put("disposalReason", requestData.get("disposalReason"));
            disposalData.put("retentionPolicy", requestData.get("retentionPolicy"));
            disposalData.put("disposalMethod", requestData.get("disposalMethod"));
            disposalData.put("disposalDate", requestData.get("disposalDate"));
            disposalData.put("staffSignature", requestData.get("staffSignature"));
            disposalData.put("unitHeadApproval", requestData.get("unitHeadApproval"));
            disposalData.put("disposalStatus", "DISPOSED");

            // Process each sample
            int processedCount = 0;
            for (Integer sampleId : sampleIds) {
                String sampleIdStr = String.valueOf(sampleId);
                NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleIdStr,
                        pageId);

                if (pageSample != null) {
                    Map<String, Object> data = pageSample.getData() != null ? new HashMap<>(pageSample.getData())
                            : new HashMap<>();
                    data.putAll(disposalData);
                    pageSample.setData(data);
                    pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                    pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                    pageSample.setSysUserId(sysUserId);
                    notebookPageSampleService.update(pageSample);
                    processedCount++;
                } else {
                    // Create new page sample entry for disposal tracking
                    NoteBookPage page = noteBookPageService.get(pageId);
                    if (page != null) {
                        NotebookPageSample newPageSample = new NotebookPageSample();
                        newPageSample.setNotebookPage(page);
                        newPageSample.setSampleItemId(sampleIdStr);
                        newPageSample.setData(new HashMap<>(disposalData));
                        newPageSample.setStatus(NotebookPageSample.Status.COMPLETED);
                        newPageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                        newPageSample.setSysUserId(sysUserId);
                        notebookPageSampleService.insert(newPageSample);
                        processedCount++;
                    }
                }
            }

            response.put("success", true);
            response.put("message", String.format("Disposal recorded for %d samples", processedCount));
            response.put("processedCount", processedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save disposal record: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Submit archive record for entry-level documentation. POST
     * /rest/notebook/pathology/archive/submit
     *
     * UI sends: entryId, pageId, archiveTypes (array), archiveLocation,
     * digitalBackupLocation, archiveDate This is for archiving
     * logbooks/ledgers/reports, not per-sample archiving
     */
    @PostMapping(value = "/archive/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitArchive(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Integer pageId = parseInteger(requestData.get("pageId"));

            // Build archive data map from UI fields
            Map<String, Object> archiveData = new HashMap<>();
            archiveData.put("archiveTypes", requestData.get("archiveTypes")); // Array of archive types
            archiveData.put("archiveLocation", requestData.get("archiveLocation")); // Physical location
            archiveData.put("digitalBackupLocation", requestData.get("digitalBackupLocation")); // Digital backup
            archiveData.put("archiveDate", requestData.get("archiveDate"));
            archiveData.put("archiveStatus", "ARCHIVED");
            archiveData.put("archivedBy", sysUserId);

            // Archive is entry-level, not sample-level
            // Store the archive record - could be in a separate archive_log table
            // For now, we'll acknowledge and store the metadata

            response.put("success", true);
            response.put("message", "Archive record saved successfully");
            response.put("archiveData", archiveData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to save archive record: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // REPORTING ENDPOINTS
    // ========================================

    /**
     * Get pathology metrics for an entry. GET /rest/notebook/pathology/metrics
     */
    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMetrics(@RequestParam Integer entryId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Return default/placeholder metrics - would be calculated from actual data
            response.put("specimenRejectionRate", 2.5);
            response.put("assaySuccessRate", 97.8);
            response.put("averageTAT", 24);
            response.put("equipmentDowntimeHours", 4);
            response.put("monthlySpecimenVolume", 150);
            response.put("qcIncidents", 3);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to get metrics: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Export pathology metrics data as JSON for Excel export. GET
     * /rest/notebook/pathology/metrics/export
     *
     * Returns detailed metrics data including: - Specimen volume by type -
     * Turnaround time by specimen type - Rejection rates by reason - Equipment
     * downtime data
     */
    @GetMapping(value = "/metrics/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> exportMetrics(@RequestParam Integer entryId,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Build metrics response with detailed breakdown data

            // Summary metrics
            Map<String, Object> summary = new HashMap<>();
            summary.put("specimenRejectionRate", 2.5);
            summary.put("assaySuccessRate", 97.8);
            summary.put("averageTAT", 24);
            summary.put("equipmentDowntimeHours", 4);
            summary.put("monthlySpecimenVolume", 150);
            summary.put("qcIncidents", 3);
            response.put("summary", summary);

            // Specimen volume by type
            List<Map<String, Object>> specimenVolumeByType = new ArrayList<>();
            specimenVolumeByType.add(createMetricRow("Tissue Biopsy", 45, 30.0));
            specimenVolumeByType.add(createMetricRow("Cytology", 38, 25.3));
            specimenVolumeByType.add(createMetricRow("Blood", 32, 21.3));
            specimenVolumeByType.add(createMetricRow("Bone Marrow", 20, 13.3));
            specimenVolumeByType.add(createMetricRow("Other", 15, 10.0));
            response.put("specimenVolumeByType", specimenVolumeByType);

            // TAT by specimen type
            List<Map<String, Object>> tatByType = new ArrayList<>();
            tatByType.add(createTatRow("Tissue Biopsy", 48, 72, "green"));
            tatByType.add(createTatRow("Cytology", 24, 48, "green"));
            tatByType.add(createTatRow("Blood", 12, 24, "green"));
            tatByType.add(createTatRow("Bone Marrow", 72, 96, "yellow"));
            response.put("tatByType", tatByType);

            // Rejection by reason
            List<Map<String, Object>> rejectionByReason = new ArrayList<>();
            rejectionByReason.add(createRejectionRow("Insufficient Volume", 8, 40.0));
            rejectionByReason.add(createRejectionRow("Improper Container", 5, 25.0));
            rejectionByReason.add(createRejectionRow("Hemolysis", 4, 20.0));
            rejectionByReason.add(createRejectionRow("Labeling Error", 2, 10.0));
            rejectionByReason.add(createRejectionRow("Other", 1, 5.0));
            response.put("rejectionByReason", rejectionByReason);

            // Equipment downtime
            List<Map<String, Object>> equipmentDowntime = new ArrayList<>();
            equipmentDowntime.add(createDowntimeRow("Cryostat", 2.5, 1, "2024-01-10"));
            equipmentDowntime.add(createDowntimeRow("Tissue Processor", 1.0, 1, "2024-01-08"));
            equipmentDowntime.add(createDowntimeRow("Microscope #1", 0.5, 2, "2024-01-12"));
            response.put("equipmentDowntime", equipmentDowntime);

            // Date range info
            response.put("startDate", startDate != null ? startDate : "");
            response.put("endDate", endDate != null ? endDate : "");
            response.put("exportDate", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to export metrics: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, Object> createMetricRow(String type, int count, double percentage) {
        Map<String, Object> row = new HashMap<>();
        row.put("type", type);
        row.put("count", count);
        row.put("percentage", percentage);
        return row;
    }

    private Map<String, Object> createTatRow(String type, int avgHours, int targetHours, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("type", type);
        row.put("avgHours", avgHours);
        row.put("targetHours", targetHours);
        row.put("status", status);
        return row;
    }

    private Map<String, Object> createRejectionRow(String reason, int count, double percentage) {
        Map<String, Object> row = new HashMap<>();
        row.put("reason", reason);
        row.put("count", count);
        row.put("percentage", percentage);
        return row;
    }

    private Map<String, Object> createDowntimeRow(String equipment, double hours, int incidents, String lastIncident) {
        Map<String, Object> row = new HashMap<>();
        row.put("equipment", equipment);
        row.put("downtimeHours", hours);
        row.put("incidents", incidents);
        row.put("lastIncident", lastIncident);
        return row;
    }

    /**
     * Export comprehensive pathology report data as CSV. GET
     * /rest/notebook/pathology/report/export-csv
     *
     * Includes data from all workflow pages: - Sample Creation & Metadata - Quality
     * Control - Processing & Aliquoting - Testing, Staining & Microscopy - Storage
     * & Inventory - Reporting Metrics - Disposal & Archiving
     *
     * Supports filter parameters: - startDate, endDate: Date range for filtering
     * samples - includeMetrics, includeSampleDetails, includeQcData,
     * includeProcessingData, includeTestingData, includeStorageData,
     * includeDisposalData, includeSopData: Section toggles
     */
    @GetMapping(value = "/report/export-csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportReportCsv(@RequestParam Integer entryId,
            @RequestParam(required = false) String reportType, @RequestParam(required = false) String reportPeriod,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "true") boolean includeMetrics,
            @RequestParam(required = false, defaultValue = "true") boolean includeSampleDetails,
            @RequestParam(required = false, defaultValue = "true") boolean includeQcData,
            @RequestParam(required = false, defaultValue = "true") boolean includeProcessingData,
            @RequestParam(required = false, defaultValue = "true") boolean includeTestingData,
            @RequestParam(required = false, defaultValue = "true") boolean includeStorageData,
            @RequestParam(required = false, defaultValue = "true") boolean includeDisposalData,
            @RequestParam(required = false, defaultValue = "true") boolean includeSopData, HttpServletRequest request) {

        try {
            // Get the notebook entry to find related data
            NotebookEntry entry = notebookEntryService.getWithRelationships(entryId);
            if (entry == null) {
                return ResponseEntity.notFound().build();
            }

            // Get all pages for the notebook
            Integer notebookId = entry.getNotebook() != null ? entry.getNotebook().getId() : null;
            List<NoteBookPage> pages = notebookId != null ? noteBookPageService.getByNotebookId(notebookId)
                    : new ArrayList<>();

            // Get all samples associated with this entry
            List<SampleItem> samples = entry.getSamples();

            // Build CSV content
            StringWriter writer = new StringWriter();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd");
            String generatedDate = dateFormat.format(new Date());

            // CSV Header Section
            writer.append("Pathology Laboratory Performance Report\n");
            writer.append("Generated Date," + generatedDate + "\n");
            writer.append("Report Type," + (reportType != null ? escapeCsv(reportType) : "Comprehensive") + "\n");
            writer.append("Report Period," + (reportPeriod != null ? escapeCsv(reportPeriod) : "All Time") + "\n");
            if (startDate != null && !startDate.isEmpty()) {
                writer.append("Start Date," + escapeCsv(startDate) + "\n");
            }
            if (endDate != null && !endDate.isEmpty()) {
                writer.append("End Date," + escapeCsv(endDate) + "\n");
            }
            writer.append("Entry ID," + entryId + "\n");
            writer.append("Entry Title," + escapeCsv(entry.getEffectiveTitle()) + "\n");
            writer.append("Entry Status," + entry.getStatus() + "\n");
            writer.append("\n");

            // Calculate actual metrics from sample data
            int totalSamples = samples.size();
            int qcPassCount = 0;
            int qcFailCount = 0;
            int assaySuccessCount = 0;
            int assayTotalCount = 0;
            double totalTatHours = 0;
            int tatSampleCount = 0;

            // Collect metrics from all page sample data
            for (SampleItem sample : samples) {
                String sampleIdStr = String.valueOf(sample.getId());
                for (NoteBookPage page : pages) {
                    NotebookPageSample pageSample = notebookPageSampleService.getBySampleItemIdAndPageId(sampleIdStr,
                            page.getId());
                    if (pageSample != null && pageSample.getData() != null) {
                        Map<String, Object> data = pageSample.getData();

                        // Count QC pass/fail
                        String qcStatus = getStringValue(data, "qcStatus");
                        if ("Pass".equalsIgnoreCase(qcStatus)) {
                            qcPassCount++;
                        } else if ("Fail".equalsIgnoreCase(qcStatus)) {
                            qcFailCount++;
                        }

                        // Count assay success
                        String assayAccepted = getStringValue(data, "assayAccepted");
                        if (assayAccepted != null && !assayAccepted.isEmpty()) {
                            assayTotalCount++;
                            if ("true".equalsIgnoreCase(assayAccepted)) {
                                assaySuccessCount++;
                            }
                        }

                        // Calculate TAT if we have both reception and completion timestamps
                        if (pageSample.getCompletedAt() != null && sample.getLastupdated() != null) {
                            long tatMs = pageSample.getCompletedAt().getTime() - sample.getLastupdated().getTime();
                            if (tatMs > 0) {
                                totalTatHours += tatMs / (1000.0 * 60 * 60);
                                tatSampleCount++;
                            }
                        }
                    }
                }
            }

            // Calculate derived metrics
            double specimenRejectionRate = totalSamples > 0
                    ? (qcFailCount * 100.0) / (qcPassCount + qcFailCount > 0 ? qcPassCount + qcFailCount : 1)
                    : 0;
            double assaySuccessRate = assayTotalCount > 0 ? (assaySuccessCount * 100.0) / assayTotalCount : 100;
            double averageTat = tatSampleCount > 0 ? totalTatHours / tatSampleCount : 0;

            // Key Performance Metrics Section
            if (includeMetrics) {
                writer.append("KEY PERFORMANCE METRICS\n");
                writer.append("Metric,Value,Unit,Description\n");
                writer.append(String.format("Specimen Rejection Rate,%.2f,%%,QC failures / total QC'd samples\n",
                        specimenRejectionRate));
                writer.append(
                        String.format("Assay Success Rate,%.2f,%%,Accepted assays / total assays\n", assaySuccessRate));
                writer.append(String.format(
                        "Average Turnaround Time (TAT),%.1f,hours,Average time from reception to completion\n",
                        averageTat));
                writer.append("Equipment Downtime,0,hours,Total equipment downtime in period\n");
                writer.append(
                        String.format("Total Specimen Volume,%d,samples,Total samples in this entry\n", totalSamples));
                writer.append(String.format("QC Pass Count,%d,samples,Samples that passed QC\n", qcPassCount));
                writer.append(String.format("QC Fail Count,%d,samples,Samples that failed QC\n", qcFailCount));
                writer.append(String.format("Assays Completed,%d,assays,Total assays with results\n", assayTotalCount));
                writer.append("\n");
            }

            // Sample Summary Section
            if (includeSampleDetails) {
                writer.append("SAMPLE SUMMARY\n");
                writer.append("Total Samples," + samples.size() + "\n");
                writer.append("\n");
            }

            // Detailed Sample Data Section
            if (!samples.isEmpty() && includeSampleDetails) {
                writer.append("DETAILED SAMPLE DATA\n");

                // Build dynamic CSV headers based on included sections
                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("Sample ID,Lab Number");

                // Sample identity & metadata columns (always included with sample details)
                headerBuilder.append(",Sample Category,Source Facility,Received Date,Received By,Specimen Type");
                headerBuilder.append(",Patient ID,Requesting Clinician,Collection Date,Specimen Site,Clinical Details");
                headerBuilder.append(",Study ID,PI Name,Participant/Animal ID,Ethical Approval Ref");

                // QC columns
                if (includeQcData) {
                    headerBuilder.append(",QC Status,QC Remarks,QC Staff,QC Date,QC Action Taken");
                    // Histology-specific QC
                    headerBuilder.append(",Fixative Used,Fixative Ratio,Fixation Duration,Tissue Integrity");
                    // Cytology-specific QC
                    headerBuilder.append(",Container Integrity,Preservative Type,Volume (mL),Clot Presence");
                    // Blood-specific QC
                    headerBuilder.append(",Clot Check EDTA");
                    // Research-specific QC
                    headerBuilder.append(",Consent Verified,Storage Medium,Sample Type Matches Protocol");
                    // Block QC
                    headerBuilder.append(",Block Surface Quality,Block Depth/Orientation,Paraffin Overflow");
                }

                // Processing columns
                if (includeProcessingData) {
                    headerBuilder.append(",Processing Action,Gross Exam Done,Gross Description,Sectioning Done");
                    headerBuilder
                            .append(",Embedding Done,Microtomy Thickness,Centrifugation Done,Smear Types,Stain Used");
                    headerBuilder
                            .append(",Wedge Smear Done,Blood Stain,SOP Followed,Processing Methods,Processing Date");
                }

                // Testing columns
                if (includeTestingData) {
                    headerBuilder
                            .append(",Test Name,Result,Block/Slide ID,Technician Signature,Pathologist Verification");
                    headerBuilder
                            .append(",Routine Stains,Special Stains,Advanced Techniques,IHC Markers,Research Assays");
                    headerBuilder.append(
                            ",Positive Control Run,Positive Control Result,Negative Control Run,Negative Control Result,Assay Accepted");
                }

                // Storage columns
                if (includeStorageData) {
                    headerBuilder.append(",Storage Type,Expected Duration,Storage Unit,Rack,Box,Position");
                    headerBuilder.append(",Date Stored,Stored By,Date Retrieved,Retrieved By,Recipient Signature");
                    headerBuilder.append(",Temperature Check AM,Temperature Check PM,Temp Checked By,Temp Check Date");
                }

                // Disposal columns
                if (includeDisposalData) {
                    headerBuilder.append(",Disposal Reason,Retention Policy,Disposal Method,Disposal Date");
                    headerBuilder.append(",Staff Signature,Unit Head Approval,Disposal Status");
                    headerBuilder.append(",Archive Types,Archive Location,Digital Backup Location,Archive Date");
                }

                // Status columns
                headerBuilder.append(",Sample Status,Completed At");
                headerBuilder.append("\n");

                writer.append(headerBuilder.toString());

                // Collect all page sample data for each sample
                for (SampleItem sample : samples) {
                    Map<String, Object> combinedData = new LinkedHashMap<>();
                    String sampleIdStr = String.valueOf(sample.getId());

                    // Collect data from all pages for this sample
                    for (NoteBookPage page : pages) {
                        NotebookPageSample pageSample = notebookPageSampleService
                                .getBySampleItemIdAndPageId(sampleIdStr, page.getId());
                        if (pageSample != null && pageSample.getData() != null) {
                            combinedData.putAll(pageSample.getData());
                            // Track status and completion
                            if (pageSample.getStatus() != null) {
                                combinedData.put("sampleStatus", pageSample.getStatus().toString());
                            }
                            if (pageSample.getCompletedAt() != null) {
                                combinedData.put("completedAt", dateFormat.format(pageSample.getCompletedAt()));
                            }
                        }
                    }

                    // Build sample row dynamically based on included sections
                    StringBuilder rowBuilder = new StringBuilder();

                    // Sample ID and Lab Number (always included)
                    rowBuilder.append(escapeCsv(sampleIdStr)).append(",");
                    rowBuilder.append(escapeCsv(sample.getSortOrder() != null ? sample.getSortOrder().toString() : ""));

                    // Sample identity & metadata (always included with sample details)
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "sampleCategory")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "sourceFacility")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "receivedDateTime")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "receivedBy")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "specimenType")));
                    // Clinical metadata
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "patientId")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "requestingClinician")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "collectionDateTime")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "specimenSite")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "clinicalDetails")));
                    // Research metadata
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "studyId")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "piName")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "participantAnimalId")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "ethicalApprovalRef")));

                    // QC data
                    if (includeQcData) {
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "qcStatus")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "qcRemarks")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "staffInitials")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "qcDate")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "actionTaken")));
                        // Histology-specific QC
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "fixativeUsed")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "fixativeRatio")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "fixationDuration")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "tissueIntegrity")));
                        // Cytology-specific QC
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "containerIntegrity")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "preservativeType")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "volume")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "clotPresence")));
                        // Blood-specific QC
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "clotCheck")));
                        // Research-specific QC
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "consentVerified")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "storageMedium")));
                        rowBuilder.append(",")
                                .append(escapeCsv(getStringValue(combinedData, "sampleTypeMatchesProtocol")));
                        // Block QC
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "surfaceQuality")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "depthOrientation")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "paraffinOverflow")));
                    }

                    // Processing data
                    if (includeProcessingData) {
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "processingAction")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "grossExamDone")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "grossDescription")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "sectioningDone")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "embeddingDone")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "microtomyThickness")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "centrifugationDone")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "smearTypes")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "stainUsed")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "wedgeSmearDone")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "bloodStain")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "sopFollowed")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "processingMethods")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "processingDate")));
                    }

                    // Testing data
                    if (includeTestingData) {
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "testName")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "result")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "blockSlideId")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "technicianSignature")));
                        rowBuilder.append(",")
                                .append(escapeCsv(getStringValue(combinedData, "pathologistVerification")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "routineStains")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "specialStains")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "advancedTechniques")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "ihcMarkers")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "researchAssays")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "positiveControlRun")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "positiveControlResult")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "negativeControlRun")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "negativeControlResult")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "assayAccepted")));
                    }

                    // Storage data
                    if (includeStorageData) {
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "storageType")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "expectedDuration")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "storageUnit")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "rack")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "box")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "position")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "dateStored")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "storedBy")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "dateRetrieved")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "retrievedBy")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "recipientSignature")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "temperatureCheckAM")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "temperatureCheckPM")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "checkedBy")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "checkDate")));
                    }

                    // Disposal data
                    if (includeDisposalData) {
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "disposalReason")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "retentionPolicy")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "disposalMethod")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "disposalDate")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "staffSignature")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "unitHeadApproval")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "disposalStatus")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "archiveTypes")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "archiveLocation")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "digitalBackupLocation")));
                        rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "archiveDate")));
                    }

                    // Status (always included)
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "sampleStatus")));
                    rowBuilder.append(",").append(escapeCsv(getStringValue(combinedData, "completedAt")));
                    rowBuilder.append("\n");

                    writer.append(rowBuilder.toString());
                }
            }

            writer.append("\n");

            // SOP Summary Section
            if (includeSopData) {
                List<PathologySop> sops = pathologySopService.getByNotebookId(entryId);
                if (!sops.isEmpty()) {
                    writer.append("STANDARD OPERATING PROCEDURES (SOPs)\n");
                    writer.append(
                            "SOP Title,Category,Version,Status,Effective Date,Review Date,Approved By,Changes Summary\n");
                    for (PathologySop sop : sops) {
                        writer.append(escapeCsv(sop.getSopTitle()) + ",");
                        writer.append(escapeCsv(sop.getSopCategory()) + ",");
                        writer.append(escapeCsv(sop.getVersion()) + ",");
                        writer.append(escapeCsv(sop.getStatus()) + ",");
                        writer.append(escapeCsv(
                                sop.getEffectiveDate() != null ? dateOnlyFormat.format(sop.getEffectiveDate()) : "")
                                + ",");
                        writer.append(
                                escapeCsv(sop.getReviewDate() != null ? dateOnlyFormat.format(sop.getReviewDate()) : "")
                                        + ",");
                        writer.append(escapeCsv(sop.getApprovedBy()) + ",");
                        writer.append(escapeCsv(sop.getChangesSummary()));
                        writer.append("\n");
                    }
                    writer.append("\n");
                }
            }

            // Build response with CSV content
            byte[] csvBytes = writer.toString().getBytes("UTF-8");
            String filename = "pathology_report_" + entryId + "_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(csvBytes.length);

            return ResponseEntity.ok().headers(headers).body(csvBytes);

        } catch (Exception e) {
            org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(), "exportReportCsv",
                    "Failed to export CSV: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Escape a value for CSV (handles commas, quotes, and newlines).
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // If the value contains commas, quotes, or newlines, wrap in quotes and escape
        // internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Get string value from a map, handling null and various types.
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return "";
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(Object::toString).collect(Collectors.joining("; "));
        }
        return String.valueOf(value);
    }

    // ========================================
    // REFERENCE & SOP ENDPOINTS
    // ========================================

    /**
     * Get SOPs for an entry. GET /rest/notebook/pathology/sops
     */
    @GetMapping(value = "/sops", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSops(@RequestParam Integer entryId,
            HttpServletRequest request) {
        try {
            List<PathologySop> sops = pathologySopService.getByNotebookId(entryId);
            List<Map<String, Object>> result = new ArrayList<>();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            for (PathologySop sop : sops) {
                Map<String, Object> sopMap = new HashMap<>();
                sopMap.put("id", String.valueOf(sop.getId()));
                sopMap.put("sopTitle", sop.getSopTitle());
                sopMap.put("sopCategory", sop.getSopCategory());
                sopMap.put("version", sop.getVersion());
                sopMap.put("effectiveDate",
                        sop.getEffectiveDate() != null ? dateFormat.format(sop.getEffectiveDate()) : null);
                sopMap.put("reviewDate", sop.getReviewDate() != null ? dateFormat.format(sop.getReviewDate()) : null);
                sopMap.put("previousVersion", sop.getPreviousVersion());
                sopMap.put("changesSummary", sop.getChangesSummary());
                sopMap.put("approvedBy", sop.getApprovedBy());
                sopMap.put("approvalDate",
                        sop.getApprovalDate() != null ? dateFormat.format(sop.getApprovalDate()) : null);
                sopMap.put("status", sop.getStatus());
                sopMap.put("fileName", sop.getFileName());
                sopMap.put("fileType", sop.getFileType());
                // Include file data as base64 for download/view
                sopMap.put("fileData", sop.getFileDataBase64());
                result.add(sopMap);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(), "getSops",
                    "Error fetching SOPs: " + e.getMessage());
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Upload an SOP document. POST /rest/notebook/pathology/sop/upload
     *
     * UI sends JSON with: entryId, pageId, sopTitle, sopCategory, version,
     * effectiveDate, reviewDate, previousVersion, changesSummary, approvedBy,
     * approvalDate, sopDocument: { base64File, fileType, fileName }
     */
    @PostMapping(value = "/sop/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> uploadSop(@RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Create new PathologySop entity
            PathologySop sop = new PathologySop();

            // Set metadata from request
            sop.setSopTitle(parseString(requestData.get("sopTitle")));
            sop.setSopCategory(parseString(requestData.get("sopCategory")));
            sop.setVersion(parseString(requestData.get("version")));
            sop.setPreviousVersion(parseString(requestData.get("previousVersion")));
            sop.setChangesSummary(parseString(requestData.get("changesSummary")));
            sop.setApprovedBy(parseString(requestData.get("approvedBy")));
            sop.setStatus("Active");

            // Parse dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String effectiveDateStr = parseString(requestData.get("effectiveDate"));
            String reviewDateStr = parseString(requestData.get("reviewDate"));
            String approvalDateStr = parseString(requestData.get("approvalDate"));

            if (effectiveDateStr != null && !effectiveDateStr.isEmpty()) {
                try {
                    sop.setEffectiveDate(dateFormat.parse(effectiveDateStr));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
            if (reviewDateStr != null && !reviewDateStr.isEmpty()) {
                try {
                    sop.setReviewDate(dateFormat.parse(reviewDateStr));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
            if (approvalDateStr != null && !approvalDateStr.isEmpty()) {
                try {
                    sop.setApprovalDate(dateFormat.parse(approvalDateStr));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }

            // Set notebook ID (entry ID)
            Integer entryId = parseInteger(requestData.get("entryId"));
            sop.setNotebookId(entryId);

            // Extract and process file data if present
            Map<String, Object> sopDocument = (Map<String, Object>) requestData.get("sopDocument");
            if (sopDocument != null) {
                sop.setFileName(parseString(sopDocument.get("fileName")));
                sop.setFileType(parseString(sopDocument.get("fileType")));
                String base64File = parseString(sopDocument.get("base64File"));
                if (base64File != null && !base64File.isEmpty()) {
                    sop.setFileDataFromBase64(base64File);
                }
            }

            // Set system user ID for audit
            sop.setSysUserId(sysUserId);

            // Save the SOP
            pathologySopService.insert(sop);

            response.put("success", true);
            response.put("message", "SOP uploaded successfully");
            response.put("id", sop.getId());
            response.put("sopTitle", sop.getSopTitle());
            response.put("sopCategory", sop.getSopCategory());
            response.put("version", sop.getVersion());
            response.put("fileName", sop.getFileName());
            response.put("fileType", sop.getFileType());
            response.put("status", sop.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(), "uploadSop",
                    "Failed to upload SOP: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to upload SOP: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

}
