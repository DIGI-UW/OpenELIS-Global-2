package org.openelisglobal.qaevent.controller.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceAttachmentService;
import org.openelisglobal.qaevent.service.NceCategoryService;
import org.openelisglobal.qaevent.service.NceNumberGeneratorService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.service.NceTypeService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceAttachment;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            item.dateOfEvent = event.getDateOfEvent() != null ? event.getDateOfEvent().toString() : null;
            item.reportDate = event.getReportDate() != null ? event.getReportDate().toString() : null;
            item.nameOfReporter = event.getNameOfReporter();
            item.immediateAction = event.getImmediateAction();
            item.suspectedCauses = event.getSuspectedCauses();
            item.proposedAction = event.getProposedAction();

            // Fetch linked specimens
            List<NceSpecimen> specimens = nceSpecimenService.getSpecimenByNceId(event.getId());
            List<LinkedSpecimenDTO> linkedSpecimens = new ArrayList<>();
            for (NceSpecimen specimen : specimens) {
                LinkedSpecimenDTO linkedSpec = new LinkedSpecimenDTO();
                linkedSpec.sampleItemId = specimen.getSampleItemId();

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
                linkedSpecimens.add(linkedSpec);
            }
            item.linkedSpecimens = linkedSpecimens;

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
            Path filePath = Paths.get(attachment.getFilePath());
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

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentLength(fileContent.length)
                    .body(resource);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "downloadAttachment",
                    "Error reading attachment file: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
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
        public List<LinkedSpecimenDTO> linkedSpecimens;
    }

    public static class LinkedSpecimenDTO {
        public Integer sampleItemId;
        public String labOrderNumber;
        public String sampleType;
    }
}
