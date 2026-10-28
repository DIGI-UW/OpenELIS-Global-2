package org.openelisglobal.qaevent.service;

import java.util.Arrays;
import java.util.List;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.worker.NonConformingEventWorker;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceReportServiceImpl implements NceReportService {

    private final NonConformingEventWorker worker;
    private final NceNumberGeneratorService numberService;
    private final NCEventService eventService;
    private final SystemUserService systemUserService;
    private final SampleService sampleService;
    private final SampleItemService sampleItemService;

    public NceReportServiceImpl(NonConformingEventWorker worker, NceNumberGeneratorService numberService,
            NCEventService eventService, SystemUserService systemUserService, @Lazy SampleService sampleService,
            @Lazy SampleItemService sampleItemService) {
        this.worker = worker;
        this.numberService = numberService;
        this.eventService = eventService;
        this.systemUserService = systemUserService;
        this.sampleService = sampleService;
        this.sampleItemService = sampleItemService;
    }

    @Override
    @Transactional
    public NcEvent report(NonConformingEventForm form, String authenticatedUserId) {
        require(form, "form");
        requireText(authenticatedUserId, "authenticatedUserId");
        requireText(form.getDateOfEvent(), "dateOfEvent");
        require(form.getReportingUnit(), "reportingUnit");
        requireText(form.getDescription(), "description");
        requireText(form.getSeverity(), "severity");
        requireText(form.getNceCategoryId(), "nceCategoryId");

        List<String> specimenIds = form.getSpecimenId() == null ? List.of()
                : Arrays.stream(form.getSpecimenId().split(",")).map(String::trim).filter(value -> !value.isEmpty())
                        .toList();
        boolean linkedToOrder = form.getLabOrderNumber() != null && !form.getLabOrderNumber().trim().isEmpty();
        if (linkedToOrder && specimenIds.isEmpty()) {
            specimenIds = orderSpecimens(form.getLabOrderNumber().trim());
        }
        if (!linkedToOrder && !specimenIds.isEmpty()) {
            throw new IllegalArgumentException("labOrderNumber is required");
        }
        String nceNumber = numberService.generateNceNumber();
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

    /**
     * An NCE reported from an order step concerns the order, so it is linked to the
     * order's specimens that were not rejected; an unknown lab number is refused
     * before anything is written.
     */
    private List<String> orderSpecimens(String labOrderNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(labOrderNumber);
        if (sample == null) {
            throw new IllegalArgumentException(UiMessages.current().format("nce.error.unknownOrder", labOrderNumber));
        }
        return sampleItemService.getSampleItemsBySampleId(sample.getId()).stream().filter(item -> !item.isRejected())
                .map(SampleItem::getId).toList();
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
