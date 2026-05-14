package org.openelisglobal.sample.attachment.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sample.attachment.service.OrderAttachmentService;
import org.openelisglobal.sample.attachment.valueholder.OrderAttachment;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rest/order")
public class OrderAttachmentRestController extends BaseRestController {

    @Autowired
    private OrderAttachmentService orderAttachmentService;

    @Autowired
    private SampleService sampleService;

    @PostMapping(value = "/{accessionNumber}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadAttachments(@PathVariable String accessionNumber,
            @RequestPart("files") List<MultipartFile> files, HttpServletRequest request) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null || sample.getId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found"));
        }
        try {
            Long sampleId = Long.valueOf(sample.getId());
            Integer userId = parseUserId(getSysUserId(request));
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    orderAttachmentService.createAttachmentFromUpload(sampleId, file, userId);
                }
            }
            List<OrderAttachment> attachments = orderAttachmentService.findActiveBySampleId(sampleId);
            return ResponseEntity.ok(attachments.stream().map(this::toDto).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "uploadAttachments", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save attachment"));
        }
    }

    @GetMapping(value = "/{accessionNumber}/attachments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listAttachments(@PathVariable String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null || sample.getId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found"));
        }
        Long sampleId = Long.valueOf(sample.getId());
        List<OrderAttachment> attachments = orderAttachmentService.findActiveBySampleId(sampleId);
        return ResponseEntity.ok(attachments.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping(value = "/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Integer attachmentId) {
        return serveAttachment(attachmentId, "attachment");
    }

    @GetMapping(value = "/attachments/{attachmentId}/view")
    public ResponseEntity<Resource> viewAttachment(@PathVariable Integer attachmentId) {
        return serveAttachment(attachmentId, "inline");
    }

    private ResponseEntity<Resource> serveAttachment(Integer attachmentId, String disposition) {
        OrderAttachment attachment = orderAttachmentService.get(attachmentId);
        if (attachment == null || Boolean.TRUE.equals(attachment.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }
        byte[] fileContent = attachment.getFileContent();
        if (fileContent == null) {
            return ResponseEntity.notFound().build();
        }
        ByteArrayResource resource = new ByteArrayResource(fileContent);

        String contentType = attachment.getFileType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + attachment.getOriginalFileName() + "\"")
                .contentLength(fileContent.length).body(resource);
    }

    @DeleteMapping(value = "/attachments/{attachmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteAttachment(@PathVariable Integer attachmentId, HttpServletRequest request) {
        OrderAttachment attachment = orderAttachmentService.get(attachmentId);
        if (attachment == null || Boolean.TRUE.equals(attachment.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }
        Integer userId = parseUserId(getSysUserId(request));
        orderAttachmentService.softDelete(attachmentId, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Integer parseUserId(String sysUserId) {
        if (sysUserId == null || sysUserId.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(sysUserId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> toDto(OrderAttachment a) {
        Timestamp ts = a.getUploadedAt();
        return Map.of("id", a.getId(), "fileName", a.getOriginalFileName(), "fileType",
                a.getFileType() == null ? "" : a.getFileType(), "fileSizeBytes",
                a.getFileSizeBytes() == null ? 0L : a.getFileSizeBytes(), "uploadedAt",
                ts == null ? "" : ts.toString());
    }
}
