package org.openelisglobal.microbiology.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroCaseContextRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseShellForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCulturePurpose;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroCaseContextService {
    private final MicroCaseDAO cases;
    private final MicroCaseAuthorizationService authorization;
    private final MicroCaseActivityDAO activities;
    private final SampleService samples;
    private final VectorSamplingSiteService sites;
    private final ObservationHistoryService observations;
    private final TestSectionService units;
    private final org.openelisglobal.program.service.ProgramService programs;

    public MicroCaseContextService(MicroCaseDAO cases, MicroCaseAuthorizationService authorization,
            MicroCaseActivityDAO activities, SampleService samples, VectorSamplingSiteService sites,
            ObservationHistoryService observations, TestSectionService units,
            org.openelisglobal.program.service.ProgramService programs) {
        this.cases = cases;
        this.authorization = authorization;
        this.activities = activities;
        this.samples = samples;
        this.sites = sites;
        this.observations = observations;
        this.units = units;
        this.programs = programs;
    }

    public void compile(MicroCase c, MicroCaseShellForm form) {
        var order = samples.get(c.getSampleId());
        boolean environmental = Domain.fromRaw(order.getDomain()) == Domain.ENVIRONMENTAL;
        form.domain = environmental ? "ENVIRONMENTAL" : "CLINICAL";
        form.orderId = c.getSampleId();
        form.programId = c.getProgramId();
        form.programs = programs.getAll().stream().filter(p -> p.isShowOnMicroCase() && "Y".equals(p.getIsActive()))
                .filter(p -> Domain.normalize(p.getDomain()).equals(form.domain))
                .map(p -> new org.openelisglobal.common.util.IdValuePair(p.getId(), p.getProgramName())).toList();
        form.culturePurpose = c.getCulturePurpose();
        form.purposeOptions = MicroCulturePurpose.options(environmental);
        form.orderCaseCount = cases.getByOrder(c.getSampleId()).size();
        if (!environmental)
            return;
        requireEnvironmental(c);
        var site = sites.get(Integer.valueOf(c.getSiteId()));
        if (site == null)
            throw new IllegalStateException("MICROBIOLOGY_SITE_REQUIRED");
        form.patientId = null;
        form.patientName = null;
        form.birthDate = null;
        form.gender = null;
        form.siteId = c.getSiteId();
        form.siteName = site.getName();
        form.siteCode = site.getCode();
        form.siteLocation = site.getDescription();
        form.latitude = site.getGpsLatitude();
        form.longitude = site.getGpsLongitude();
        form.replicateCount = order.getMicrobiologyReplicateCount();
        form.requesterName = observations.getRawValueForSample(ObservationType.ENV_CONTACT_PERSON, order.getId());
        form.requesterPhone = observations.getRawValueForSample(ObservationType.ENV_CONTACT_PHONE, order.getId());
    }

    @Transactional
    public void update(String caseId, MicroCaseContextRequestForm request, String actor) {
        var beforeLock = cases.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        // Order lock precedes case locks, matching routing and order-wide changes.
        cases.lockOrder(beforeLock.getSampleId());
        var c = authorization.requireWrite(caseId, actor, Constants.ROLE_RESULTS);
        var order = samples.get(c.getSampleId());
        boolean environmental = Domain.fromRaw(order.getDomain()) == Domain.ENVIRONMENTAL;
        String purpose = MicroCulturePurpose.validate(request.culturePurpose, environmental);
        String programId = request.programId == null ? c.getProgramId()
                : request.programId.isBlank() ? null : request.programId;
        if (programId != null && !Objects.equals(programId, c.getProgramId())) {
            var program = programs.get(programId);
            if (program == null || !program.isShowOnMicroCase() || !"Y".equals(program.getIsActive())
                    || !Domain.normalize(program.getDomain()).equals(Domain.normalize(order.getDomain())))
                throw new IllegalArgumentException("MICROBIOLOGY_PROGRAM_INVALID");
        }
        if (environmental)
            requireEnvironmental(c);
        if (request.replicateCount != null
                && (!environmental || request.replicateCount < 1 || request.replicateCount > 999))
            throw new IllegalArgumentException("MICROBIOLOGY_REPLICATES_INVALID");
        boolean replicatesChanged = request.replicateCount != null
                && !Objects.equals(request.replicateCount, order.getMicrobiologyReplicateCount());
        List<MicroCase> siblings = replicatesChanged ? cases.getByOrder(c.getSampleId()) : List.of();
        // Validate every affected case before writing any shared order data.
        for (var sibling : siblings) {
            authorization.requireWrite(sibling.getId(), actor, Constants.ROLE_RESULTS);
            requireEnvironmental(sibling);
        }
        if (!Objects.equals(programId, c.getProgramId())) {
            record(c.getId(), "PROGRAM_CHANGED_REEXPORT_REQUIRED", c.getProgramId(), programId == null ? "" : programId,
                    actor);
            c.setProgramId(programId);
            c.setSysUserId(actor);
            cases.update(c);
        }
        if (!Objects.equals(c.getCulturePurpose(), purpose)) {
            record(c.getId(), "CULTURE_PURPOSE_CHANGED", c.getCulturePurpose(), purpose, actor);
            c.setCulturePurpose(purpose);
            c.setSysUserId(actor);
            cases.update(c);
        }
        if (replicatesChanged) {
            for (var sibling : siblings)
                record(sibling.getId(), "REPLICATES_CHANGED", order.getMicrobiologyReplicateCount(),
                        request.replicateCount, actor);
            order.setMicrobiologyReplicateCount(request.replicateCount);
            order.setSysUserId(actor);
            samples.update(order);
        }
    }

    private void requireEnvironmental(MicroCase c) {
        var unit = units.get(c.getLabUnitId());
        if (c.getSiteId() == null || unit == null || Domain.fromRaw(unit.getDomain()) != Domain.ENVIRONMENTAL)
            throw new IllegalArgumentException("MICROBIOLOGY_ENVIRONMENTAL_UNIT_REQUIRED");
    }

    private void record(String id, String type, Object before, Object after, String actor) {
        var activity = new MicroCaseActivity();
        activity.setCaseId(id);
        activity.setActivityType(type);
        activity.setPerformedBy(actor);
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        try {
            activity.setStructuredData(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(Map.of("before", before == null ? "" : before, "after", after)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        activities.insert(activity);
    }
}
