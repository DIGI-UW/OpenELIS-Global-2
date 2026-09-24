package org.openelisglobal.qaevent.service;

import java.util.Arrays;
import java.util.List;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceReportServiceImpl implements NceReportService {

    private final NonConformingEventWorker worker;
    private final NceNumberGeneratorService numberService;
    private final NCEventService eventService;
    private final SystemUserService systemUserService;

    public NceReportServiceImpl(NonConformingEventWorker worker, NceNumberGeneratorService numberService,
            NCEventService eventService, SystemUserService systemUserService) {
        this.worker = worker;
        this.numberService = numberService;
        this.eventService = eventService;
        this.systemUserService = systemUserService;
    }

    @Override
    @Transactional
    public NcEvent report(NonConformingEventForm form, String authenticatedUserId) {
        require(form, "form");
        requireText(authenticatedUserId, "authenticatedUserId");
        requireText(form.getLabOrderNumber(), "labOrderNumber");
        requireText(form.getSpecimenId(), "specimenId");
        requireText(form.getDateOfEvent(), "dateOfEvent");
        require(form.getReportingUnit(), "reportingUnit");
        requireText(form.getDescription(), "description");
        requireText(form.getSeverity(), "severity");
        requireText(form.getNceCategoryId(), "nceCategoryId");

        String nceNumber = numberService.generateNceNumber();
        List<String> specimenIds = Arrays.stream(form.getSpecimenId().split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).toList();
        if (specimenIds.isEmpty()) {
            throw new IllegalArgumentException("specimenId is required");
        }
        NcEvent created = worker.create(form.getLabOrderNumber(), specimenIds, authenticatedUserId, nceNumber,
                form.getAnalysisId());
        SystemUser reporter = systemUserService.getUserById(authenticatedUserId);
        if (reporter != null) {
            String reporterName = (safe(reporter.getFirstName()) + " " + safe(reporter.getLastName())).trim();
            form.setName(reporterName);
            form.setReporterName(reporterName);
        }
        form.setId(String.valueOf(created.getId()));
        form.setNceNumber(nceNumber);
        form.setCurrentUserId(authenticatedUserId);
        if (!worker.update(form)) {
            throw new IllegalStateException("NCE could not be completed");
        }
        return eventService.get(created.getId());
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void require(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
