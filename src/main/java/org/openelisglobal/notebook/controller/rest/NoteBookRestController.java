package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.form.AuditTrailViewForm;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.historyservices.NoteBookHistoryService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.notebook.bean.NoteBookDashboardMetrics;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest/notebook")
public class NoteBookRestController extends BaseRestController {

    @Autowired
    private NoteBookService noteBookService;

    @GetMapping(value = "/dashboard/entries", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getFilteredNoteBooks(
            @RequestParam(required = false) List<NoteBookStatus> statuses,
            @RequestParam(required = false) List<String> types, @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer noteBookId) {

        List<NoteBookDisplayBean> results = noteBookService
                .filterNoteBookEntries(statuses, types, tags, getFormatedDate(fromDate), getFormatedDate(toDate),
                        noteBookId)
                .stream().map(e -> noteBookService.convertToDisplayBean(e.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/dashboard/notebooks", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getAllNoteBooks() {

        List<NoteBookDisplayBean> results = noteBookService.getAllTemplateNoteBooks().stream()
                .map(e -> noteBookService.convertToDisplayBean(e.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/dashboard/entries/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getNoteBookEntries(
            @PathVariable("noteBookId") Integer noteBookId) {

        List<NoteBookDisplayBean> results = noteBookService.getNoteBookEntries(noteBookId).stream()
                .map(e -> noteBookService.convertToDisplayBean(e.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    private Date getFormatedDate(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }

        try {
            String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE);

            String pattern;
            if ("fr-FR".equalsIgnoreCase(locale)) {
                pattern = "dd/MM/yyyy";
            } else {
                pattern = "MM/dd/yyyy";
            }

            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // normalize
            return sdf.parse(date);

        } catch (ParseException e) {
            // consider logging or rethrowing
            return null;
        }
    }

    @GetMapping(value = "/dashboard/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<NoteBookDashboardMetrics> getNoteBookDashboardMetrics() {
        NoteBookDashboardMetrics metrics = new NoteBookDashboardMetrics();
        metrics.setTotal(noteBookService.getTotalCount());
        metrics.setDrafts(noteBookService.getCountWithStatus(Arrays.asList(NoteBookStatus.DRAFT)));
        metrics.setPending(noteBookService.getCountWithStatus(Arrays.asList(NoteBookStatus.SUBMITTED)));

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant weekAgoInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Timestamp weekAgoTimestamp = Timestamp.from(weekAgoInstant);
        metrics.setFinalized(noteBookService.getCountWithStatusBetweenDates(
                Arrays.asList(NoteBookStatus.FINALIZED, NoteBookStatus.ARCHIVED, NoteBookStatus.LOCKED),
                weekAgoTimestamp, currentTimestamp));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping(value = "/view/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<NoteBookFullDisplayBean> getNoteBookEntry(@PathVariable("noteBookId") Integer noteBookId) {
        return ResponseEntity.ok(noteBookService.convertToFullDisplayBean(noteBookId));
    }

    @PostMapping(value = "/update/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> updateNoteBookEntry(@PathVariable("noteBookId") Integer noteBookId,
            @RequestBody NoteBookForm form, HttpServletRequest request) {
        form.setSystemUserId(Integer.valueOf(this.getSysUserId(request)));
        noteBookService.updateWithFormValues(noteBookId, form);

        return ResponseEntity.ok(Map.of("id", noteBookId));
    }

    @PostMapping(value = "/updatestatus/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void updateNoteBookStatus(@PathVariable("noteBookId") Integer noteBookId,
            @RequestParam(required = false) NoteBookStatus status, HttpServletRequest request) {
        noteBookService.updateWithStatus(noteBookId, status, this.getSysUserId(request));
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> createNoteBookEntry(@RequestBody NoteBookForm form,
            HttpServletRequest request) {
        form.setSystemUserId(Integer.valueOf(this.getSysUserId(request)));
        NoteBook noteBook = noteBookService.createWithFormValues(form);
        return ResponseEntity.ok(Map.of("id", noteBook.getId()));
    }

    @GetMapping(value = "/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SampleDisplayBean>> searchSamples(@RequestParam(required = false) String accession) {
        List<SampleDisplayBean> results = noteBookService.searchSampleItems(accession);
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/auditTrail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AuditTrailViewForm> getNoteBookAuditTrail(@RequestParam Integer notebookId) {
        AuditTrailViewForm response = new AuditTrailViewForm();

        if (notebookId == null) {
            return ResponseEntity.ok(response);
        }

        NoteBook noteBook = noteBookService.get(notebookId);
        if (noteBook == null) {
            return ResponseEntity.ok(response);
        }

        NoteBookHistoryService historyService = new NoteBookHistoryService(noteBook);
        List<AuditTrailItem> items = historyService.getAuditTrailItems();

        if (items.size() == 0) {
            return ResponseEntity.ok(response);
        }

        // Populate the response object with status-only audit trail
        response.setLog(items);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getAvailableNotebooks() {
        List<NoteBookDisplayBean> results = noteBookService.getAllActiveNotebooks().stream()
                .map(notebook -> noteBookService.convertToDisplayBean(notebook.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

}
