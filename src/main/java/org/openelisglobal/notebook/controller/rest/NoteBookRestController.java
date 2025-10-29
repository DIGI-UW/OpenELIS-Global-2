package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.bean.NoteBookDashboardMetrics;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping(value = "/dashboard/items", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getFilteredNoteBooks(
            @RequestParam(required = false) List<NoteBookStatus> statuses,
            @RequestParam(required = false) List<String> types, @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {
        List<NoteBookDisplayBean> results = noteBookService.filterNoteBooks(statuses, types, tags, fromDate, toDate)
                .stream().map(e -> noteBookService.convertToDisplayBean(e.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
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
    public ResponseEntity<NoteBookForm> updateNoteBookEntry(@PathVariable("noteBookId") Integer noteBookId,
            @RequestBody NoteBookForm form, HttpServletRequest request) {
        form.setSystemUserId(Integer.valueOf(this.getSysUserId(request)));
        noteBookService.updateWithFormValues(noteBookId, form);

        return ResponseEntity.ok(form);
    }

    @PostMapping(value = "/updatestatus/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void updateNoteBookStatus(@PathVariable("noteBookId") Integer noteBookId,
            @RequestParam(required = false) NoteBookStatus status, HttpServletRequest request) {
        noteBookService.updateWithStatus(noteBookId, status);
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<NoteBookForm> createNoteBookEntry(@RequestBody NoteBookForm form,
            HttpServletRequest request) {
        form.setSystemUserId(Integer.valueOf(this.getSysUserId(request)));
        noteBookService.createWithFormValues(form);
        return ResponseEntity.ok(form);
    }

    @GetMapping(value = "/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SampleDisplayBean>> searchSamples(@RequestParam(required = false) String patientId,
            @RequestParam(required = false) String accession) {
        List<SampleDisplayBean> results = noteBookService.searchSampleItems(patientId, accession);
        return ResponseEntity.ok(results);
    }

}
