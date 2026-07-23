package org.openelisglobal.qaevent.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceAttachmentService;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.service.NceHistoryService;
import org.openelisglobal.qaevent.service.NceNumberGeneratorService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.service.NceTypeService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceAttachment;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceHistory;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for NCE core features: categories, types, and NCE number
 * generation.
 */
@RestController
@RequestMapping("/rest/nce")
public class NceEnhancementRestController extends BaseRestController {

    @Autowired
    private NceNumberGeneratorService nceNumberGeneratorService;

    @Autowired
    private NceCategoryService nceCategoryService;

    @Autowired
    private NceTypeService nceTypeService;

    @Autowired
    private NCEventService ncEventService;

    @Autowired
    private NceSpecimenService nceSpecimenService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private NceAttachmentService nceAttachmentService;

    @Autowired
    private NceHistoryService nceHistoryService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private AnalysisService analysisService;

    @Value("${org.openelisglobal.nce.attachment.path:/var/lib/openelis-global/nce-attachments}")
    private String attachmentBaseDir;

    private static final int USER_AUTOCOMPLETE_RESULT_LIMIT = 25;

    @GetMapping(value = "/generate-number", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> generateNceNumber() {
        String nceNumber = nceNumberGeneratorService.generateNceNumber();
        return ResponseEntity.ok(Map.of("nceNumber", nceNumber));
    }

    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        List<NcEvent> allEvents = ncEventService.getAll();
        List<NceDashboardItemDTO> nceList = new ArrayList<>();

        for (NcEvent event : allEvents) {
            NceDashboardItemDTO item = new NceDashboardItemDTO();
            item.id = String.valueOf(event.getId());
            item.nceNumber = event.getNceNumber();
            item.title = event.getTitle();
            item.description = event.getDescription();
            item.status = event.getStatus() != null ? event.getStatus() : "Pending";
            item.severity = event.getSeverity();
            item.nceCategoryId = event.getNceCategoryId();
            item.nceTypeId = event.getNceTypeId();
            item.labOrderNumber = event.getLabOrderNumber();
            item.dateOfEvent = event.getDateOfEvent() != null ? event.getDateOfEvent().toLocalDate().toString() : null;
            item.reportDate = event.getReportDate() != null ? event.getReportDate().toLocalDate().toString() : null;
            item.nameOfReporter = event.getNameOfReporter();
            item.immediateAction = event.getImmediateAction();
            item.suspectedCauses = event.getSuspectedCauses();
            item.proposedAction = event.getProposedAction();

            // Fetch assigned user name
            if (event.getAssignedTo() != null) {
                item.assignedTo = String.valueOf(event.getAssignedTo());
                try {
                    SystemUser assignedUser = systemUserService.get(String.valueOf(event.getAssignedTo()));
                    if (assignedUser != null) {
                        item.assignedToName = assignedUser.getFirstName() + " " + assignedUser.getLastName();
                    }
                } catch (Exception e) {
                    item.assignedToName = "Unknown";
                }
            }

            // Fetch linked specimens
            List<NceSpecimen> specimens = nceSpecimenService.getSpecimenByNceId(event.getId());
            List<LinkedSpecimenDTO> linkedSpecimens = new ArrayList<>();
            for (NceSpecimen specimen : specimens) {
                LinkedSpecimenDTO linkedSpec = new LinkedSpecimenDTO();
                linkedSpec.sampleItemId = specimen.getSampleItemId();
                linkedSpec.analysisId = specimen.getAnalysisId();

                // Get sample item details
                SampleItem sampleItem = sampleItemService.get(String.valueOf(specimen.getSampleItemId()));
                if (sampleItem != null) {
                    linkedSpec.sampleType = sampleItem.getTypeOfSample() != null
                            ? sampleItem.getTypeOfSample().getDescription()
                            : null;

                    // Get sample/order details
                    Sample sample = sampleItem.getSample();
                    if (sample != null) {
                        linkedSpec.labOrderNumber = sample.getAccessionNumber();
                    }
                }

                // Get test name from analysis
                if (specimen.getAnalysisId() != null) {
                    try {
                        Analysis analysis = analysisService.get(String.valueOf(specimen.getAnalysisId()));
                        if (analysis != null && analysis.getTest() != null) {
                            linkedSpec.testName = analysis.getTest().getLocalizedTestName().getLocalizedValue();
                        }
                    } catch (Exception e) {
                        // Analysis may not exist
                    }
                }

                linkedSpecimens.add(linkedSpec);
            }
            item.linkedSpecimens = linkedSpecimens;

            // Fetch attachments
            List<NceAttachment> attachments = nceAttachmentService.findByNceId(event.getId());
            List<AttachmentDTO> attachmentDTOs = new ArrayList<>();
            for (NceAttachment attachment : attachments) {
                AttachmentDTO attachmentDTO = new AttachmentDTO();
                attachmentDTO.id = attachment.getId();
                attachmentDTO.fileName = attachment.getFileName();
                attachmentDTO.fileType = attachment.getFileType();
                attachmentDTO.fileSize = attachment.getFileSize();
                attachmentDTO.uploadedDate = attachment.getUploadedDate() != null
                        ? attachment.getUploadedDate().toInstant().toString()
                        : null;
                attachmentDTOs.add(attachmentDTO);
            }
            item.attachments = attachmentDTOs;

            // Fetch history and extract notes
            List<NceHistory> historyRecords = nceHistoryService.findByNceId(event.getId());
            List<HistoryDTO> historyDTOs = new ArrayList<>();
            List<NoteDTO> noteDTOs = new ArrayList<>();
            for (NceHistory history : historyRecords) {
                HistoryDTO historyDTO = new HistoryDTO();
                historyDTO.id = String.valueOf(history.getId());
                historyDTO.activity = history.getActivity();
                historyDTO.description = history.getDescription();
                historyDTO.timestamp = history.getTimestamp() != null ? history.getTimestamp().toInstant().toString()
                        : null;

                // Get user name
                String userName = null;
                if (history.getUserId() != null) {
                    try {
                        SystemUser user = systemUserService.get(String.valueOf(history.getUserId()));
                        if (user != null) {
                            userName = user.getFirstName() + " " + user.getLastName();
                        }
                    } catch (Exception e) {
                        userName = "Unknown";
                    }
                }
                historyDTO.userName = userName;
                historyDTOs.add(historyDTO);

                // Extract notes from history
                if ("NOTE_ADDED".equals(history.getActivity())) {
                    NoteDTO noteDTO = new NoteDTO();
                    noteDTO.id = String.valueOf(history.getId());
                    noteDTO.text = history.getDescription();
                    noteDTO.userName = userName;
                    noteDTO.timestamp = history.getTimestamp() != null ? history.getTimestamp().toInstant().toString()
                            : null;
                    noteDTOs.add(noteDTO);
                }
            }
            item.history = historyDTOs;
            item.notes = noteDTOs;
            item.notesCount = noteDTOs.size();

            nceList.add(item);
        }

        return ResponseEntity.ok(Map.of("nceList", nceList));
    }

    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NceCategoryDTO>> getCategories() {
        List<NceCategory> categories = nceCategoryService.getAllNceCategories();

        List<NceCategoryDTO> result = categories.stream()
                .filter(cat -> cat.getActive() == null || Boolean.TRUE.equals(cat.getActive())).map(cat -> {
                    NceCategoryDTO dto = new NceCategoryDTO();
                    dto.id = String.valueOf(cat.getId());
                    dto.name = cat.getLocalizedName();

                    List<NceType> types = nceTypeService.getNceTypesByCategoryId(cat.getId());
                    dto.types = types.stream()
                            .filter(type -> type.getActive() == null || Boolean.TRUE.equals(type.getActive()))
                            .map(type -> {
                                NceTypeDTO typeDto = new NceTypeDTO();
                                typeDto.id = String.valueOf(type.getId());
                                typeDto.name = type.getLocalizedName();
                                return typeDto;
                            }).collect(Collectors.toList());

                    return dto;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Download an NCE attachment file.
     *
     * @param attachmentId the attachment ID
     * @return the file as a downloadable resource
     */
    @GetMapping(value = "/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Integer attachmentId) {
        NceAttachment attachment = nceAttachmentService.get(attachmentId);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path basePath = Paths.get(attachmentBaseDir).toRealPath();
            Path filePath = Paths.get(attachment.getFilePath()).normalize();
            if (!filePath.startsWith(basePath)) {
                LogEvent.logError(this.getClass().getSimpleName(), "downloadAttachment",
                        "Attachment path is outside allowed directory: " + filePath);
                return ResponseEntity.status(403).build();
            }
            if (!Files.exists(filePath)) {
                LogEvent.logError(this.getClass().getSimpleName(), "downloadAttachment",
                        "Attachment file not found on disk: " + filePath);
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            ByteArrayResource resource = new ByteArrayResource(fileContent);

            String contentType = attachment.getFileType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentLength(fileContent.length).body(resource);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "downloadAttachment",
                    "Error reading attachment file: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add a history entry (note) to an NCE.
     *
     * @param historyRequest the history entry request
     * @param httpRequest    the HTTP request for user context
     * @return the created history entry
     */
    @PostMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addHistoryEntry(@RequestBody HistoryEntryRequest historyRequest,
            HttpServletRequest httpRequest) {
        if (historyRequest.nceId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "nceId is required"));
        }

        // Validate NCE exists
        NcEvent event = ncEventService.get(historyRequest.nceId);
        if (event == null) {
            return ResponseEntity.status(404).body(Map.of("error", "NCE not found"));
        }

        // Get current user ID from security context
        String sysUserId = getSysUserId(httpRequest);
        Integer userId = parseUserIdSafely(sysUserId);

        String activity = historyRequest.activity != null ? historyRequest.activity : "NOTE_ADDED";

        // If acknowledging, update NCE status from Pending to Under Investigation
        if ("ACKNOWLEDGED".equals(activity) && "Pending".equals(event.getStatus())) {
            event.setStatus("Under Investigation");
            event.setSysUserId(sysUserId);
            ncEventService.update(event);
        }

        NceHistory history = nceHistoryService.logActivity(historyRequest.nceId, activity, historyRequest.description,
                null, null, userId);

        return ResponseEntity.ok(Map.of("success", true, "id", history.getId()));
    }

    /**
     * Assign an NCE to a user.
     *
     * @param assignRequest the assignment request
     * @param httpRequest   the HTTP request for user context
     * @return success response
     */
    @PostMapping(value = "/assign", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> assignNce(@RequestBody AssignRequest assignRequest,
            HttpServletRequest httpRequest) {
        if (assignRequest.nceId == null || assignRequest.assignedTo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "nceId and assignedTo are required"));
        }

        // Safely parse assignedTo
        Integer assignedToId;
        try {
            assignedToId = Integer.valueOf(assignRequest.assignedTo);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "assignedTo must be a valid numeric ID"));
        }

        NcEvent event = ncEventService.get(assignRequest.nceId);
        if (event == null) {
            return ResponseEntity.status(404).body(Map.of("error", "NCE not found"));
        }

        // Validate assignee exists
        SystemUser assignee = systemUserService.get(assignRequest.assignedTo);
        if (assignee == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        String assigneeName = assignee.getFirstName() + " " + assignee.getLastName();

        // Update the event's assignedTo field
        event.setAssignedTo(assignedToId);
        String sysUserId = getSysUserId(httpRequest);
        event.setSysUserId(sysUserId);
        ncEventService.update(event);

        // Log assignment in history
        Integer userId = parseUserIdSafely(sysUserId);
        nceHistoryService.logActivity(assignRequest.nceId, "ASSIGNED", "Assigned to " + assigneeName, null,
                assignRequest.assignedTo, userId);

        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Get list of users for assignment autocomplete.
     *
     * @param search optional search term to filter users
     * @return list of users
     */
    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserDTO>> getUsers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search) {
        List<SystemUser> allUsers = systemUserService.getAllSystemUsers();

        List<UserDTO> result = allUsers.stream()
                .filter(user -> user.getIsActive() != null && "Y".equals(user.getIsActive())).filter(user -> {
                    if (search == null || search.isEmpty()) {
                        return true;
                    }
                    String searchLower = search.toLowerCase();
                    String firstName = user.getFirstName() != null ? user.getFirstName().toLowerCase() : "";
                    String lastName = user.getLastName() != null ? user.getLastName().toLowerCase() : "";
                    String loginName = user.getLoginName() != null ? user.getLoginName().toLowerCase() : "";
                    return firstName.contains(searchLower) || lastName.contains(searchLower)
                            || loginName.contains(searchLower);
                }).map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.id = String.valueOf(user.getId());
                    dto.firstName = user.getFirstName();
                    dto.lastName = user.getLastName();
                    dto.loginName = user.getLoginName();
                    dto.displayName = (user.getFirstName() != null ? user.getFirstName() : "")
                            + (user.getLastName() != null ? " " + user.getLastName() : "");
                    return dto;
                }).limit(USER_AUTOCOMPLETE_RESULT_LIMIT).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private Integer parseUserIdSafely(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(userId.trim());
        } catch (NumberFormatException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "parseUserIdSafely",
                    "Non-numeric userId from security context: " + userId);
            return null;
        }
    }

    public static class NceCategoryDTO {
        public String id;
        public String name;
        public List<NceTypeDTO> types;
    }

    public static class NceTypeDTO {
        public String id;
        public String name;
    }

    public static class NceDashboardItemDTO {
        public String id;
        public String nceNumber;
        public String title;
        public String description;
        public String status;
        public String severity;
        public Integer nceCategoryId;
        public Integer nceTypeId;
        public String labOrderNumber;
        public String dateOfEvent;
        public String reportDate;
        public String nameOfReporter;
        public String immediateAction;
        public String suspectedCauses;
        public String proposedAction;
        public String assignedTo;
        public String assignedToName;
        public int notesCount;
        public List<NoteDTO> notes;
        public List<LinkedSpecimenDTO> linkedSpecimens;
        public List<AttachmentDTO> attachments;
        public List<HistoryDTO> history;
    }

    public static class NoteDTO {
        public String id;
        public String text;
        public String userName;
        public String timestamp;
    }

    public static class HistoryDTO {
        public String id;
        public String activity;
        public String description;
        public String timestamp;
        public String userName;
    }

    public static class AttachmentDTO {
        public Integer id;
        public String fileName;
        public String fileType;
        public Long fileSize;
        public String uploadedDate;
    }

    public static class LinkedSpecimenDTO {
        public Integer sampleItemId;
        public Integer analysisId;
        public String labOrderNumber;
        public String sampleType;
        public String testName;
    }

    public static class HistoryEntryRequest {
        public Integer nceId;
        public String activity;
        public String description;
    }

    public static class AssignRequest {
        public Integer nceId;
        public String assignedTo;
    }

    public static class UserDTO {
        public String id;
        public String firstName;
        public String lastName;
        public String loginName;
        public String displayName;
    }
}
