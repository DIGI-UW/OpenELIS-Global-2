package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.service.*;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.typeofsample.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MicroIsolateWorkspaceServiceImpl implements MicroIsolateWorkspaceService {
    private final org.openelisglobal.method.service.MethodService methods;
    private final MicroCaseAmendmentDAO amendments;
    private final MicroCaseDAO cases;
    private final MicroIsolateDAO isolates;
    private final MicroCaseInoculationDAO cultures;
    private final MicroCaseActivityDAO activities;
    private final MicroOrganismDAO organisms;
    private final MicroCaseAnalysisDAO analyses;
    private final MicrobiologyCaseAccessService access;
    private final MicroIsolateService identification;
    private final SampleItemService sampleItems;
    private final TypeOfSampleService sampleTypes;
    private final TypeOfSampleTestService sampleTests;
    private final MicroCaseMembershipService membership;
    private final MicroCaseResultService results;
    private final ReferralDAO referrals;
    private final ReferralSetService referralSets;
    private final OrganizationService organizations;
    private final ReferralReasonService reasons;
    private final TestSectionService units;

    public MicroIsolateWorkspaceServiceImpl(org.openelisglobal.method.service.MethodService methods,
            MicroCaseAmendmentDAO amendments, MicroCaseDAO cases, MicroIsolateDAO isolates,
            MicroCaseInoculationDAO cultures, MicroCaseActivityDAO activities, MicroOrganismDAO organisms,
            MicroCaseAnalysisDAO analyses, MicrobiologyCaseAccessService access, MicroIsolateService identification,
            SampleItemService sampleItems, TypeOfSampleService sampleTypes, TypeOfSampleTestService sampleTests,
            MicroCaseMembershipService membership, MicroCaseResultService results, ReferralDAO referrals,
            ReferralSetService referralSets, OrganizationService organizations, ReferralReasonService reasons,
            TestSectionService units) {
        this.methods = methods;
        this.amendments = amendments;
        this.cases = cases;
        this.isolates = isolates;
        this.cultures = cultures;
        this.activities = activities;
        this.organisms = organisms;
        this.analyses = analyses;
        this.access = access;
        this.identification = identification;
        this.sampleItems = sampleItems;
        this.sampleTypes = sampleTypes;
        this.sampleTests = sampleTests;
        this.membership = membership;
        this.results = results;
        this.referrals = referrals;
        this.referralSets = referralSets;
        this.organizations = organizations;
        this.reasons = reasons;
        this.units = units;
    }

    @Override
    public MicroIsolateWorkspaceForm get(String caseId, String actor) {
        if (actor == null || actor.isBlank())
            throw new AccessDeniedException("Authenticated system user required");
        var c = requireCase(caseId);
        boolean mutable = mutable(c);
        boolean writer = mutable && access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS);
        boolean validator = mutable && access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_VALIDATION);
        var sources = cultures.getSources(caseId);
        var form = new MicroIsolateWorkspaceForm();
        form.isolates = isolates.getWorkspace(caseId).stream().map(i -> {
            var sample = sources.stream().filter(s -> s.getId().equals(i.getSampleItemId())).findFirst().orElse(null);
            var history = i.getIdentificationHistory().stream()
                    .map(e -> new MicroIsolateWorkspaceForm.Identification(e.getId(), e.getNewOrganismId(),
                            organismName(e.getNewOrganismId(), e.getNewOrganismText()),
                            status(e.getNewIdentificationStatus()), e.getNewIdentificationMethod(),
                            e.getNewIdentificationConfidence(), e.getIdentifiedAt(), e.getChangedBy(), e.getChangedAt(),
                            e.getReason(), e.getAmendmentId()))
                    .toList();
            return new MicroIsolateWorkspaceForm.Isolate(i.getId(), i.getIsolateLabel(), i.getSourceCultureId(),
                    i.getSourceCulture() == null ? null : i.getSourceCulture().getContainerIdentifier(),
                    i.getSourceSampleItemId(), i.getSampleItemId(), sample == null ? null : label(sample),
                    i.getGramStain(), i.getColonyMorphology(), significance(i.getSignificance()), i.getOrganismId(),
                    organismName(i.getOrganismId(), i.getPreliminaryOrganismText()),
                    status(i.getIdentificationStatus()), i.getIdentificationMethod(), i.getIdentificationConfidence(),
                    i.getIdentifiedAt(), i.getCreatedBy(), i.getCreatedAt(), received(i),
                    i.getReceivedFromOrganizationId(), organizationName(i.getReceivedFromOrganizationId()),
                    i.getReportedOrganismId(), i.getReportedOrganismText(), mismatch(i), i.getMismatchAcknowledgedBy(),
                    i.getMismatchAcknowledgedAt(), writer && !identified(i), validator && identified(i),
                    validator && mismatch(i), version(i), history);
        }).toList();
        var linked = referrals.getWorkspaceReferrals(sources.stream().map(SampleItem::getId).distinct().toList());
        form.referred = linked.stream().anyMatch(r -> r.getStatus() != null && !r.getStatus().isTerminal());
        form.referrals = linked.stream()
                .map(r -> new MicroIsolateWorkspaceForm.Referral(r.getId(), r.getAnalysis().getSampleItem().getId(),
                        r.getAnalysis().getTest().getName(),
                        r.getOrganization() == null ? null : r.getOrganization().getOrganizationName(),
                        r.getStatus() == null ? null : r.getStatus().name(), r.getSentDate(),
                        r.getSubcontract() == null ? null : r.getSubcontract().getSubcontractNotes()))
                .toList();
        return form;
    }

    @Override
    public MicroIsolateWorkspaceForm.Options options(String caseId) {
        var c = requireCase(caseId);
        var f = new MicroIsolateWorkspaceForm.Options();
        f.methods = methods.getAllActiveMethods().stream().map(m -> new IdValuePair(m.getId(), m.getMethodName()))
                .toList();
        f.organisms = organisms.getActiveOrganisms().stream().map(o -> new IdValuePair(o.getId(), o.getDisplayName()))
                .toList();
        f.sampleTypes = sampleTypes.getAll().stream().filter(t -> t.getIsActive() && t.isMicrobiologyIsolate())
                .map(t -> new IdValuePair(t.getId(), t.getDescription())).toList();
        f.organizations = organizations.getActiveOrganizations().stream()
                .map(o -> new IdValuePair(o.getId(), o.getOrganizationName())).toList();
        f.reasons = reasons.getAllReferralReasons().stream().map(r -> new IdValuePair(r.getId(), r.getName())).toList();
        f.sources = cultures.getSources(caseId).stream().filter(s -> !s.isRejected())
                .map(s -> new MicroIsolateWorkspaceForm.Source(s.getId(), label(s), s.getTypeOfSampleId())).toList();
        f.tests = units.getTestsInSection(c.getLabUnitId()).stream()
                .filter(t -> t.isActive() && !Boolean.FALSE.equals(t.getOrderable()))
                .map(t -> new MicroIsolateWorkspaceForm.Test(t.getId(), t.getName(),
                        sampleTests.getTypeOfSampleTestsForTest(t.getId()).stream()
                                .map(org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest::getTypeOfSampleId)
                                .toList()))
                .toList();
        return f;
    }

    @Override
    @Transactional
    public MicroIsolateWorkspaceForm pick(String caseId, MicroIsolateWorkspaceRequestForm r, String actor) {
        var c = writeCase(caseId, actor, Constants.ROLE_RESULTS);
        if (r == null)
            bad("REQUIRED");
        var row = cultures.getRows(caseId).stream().filter(i -> i.getId().equals(r.cultureId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_ISOLATE_CULTURE_REQUIRED"));
        if (!"GROWTH".equals(row.getOutcome()))
            bad("GROWTH_REQUIRED");
        if ("RECEIVED_ISOLATE".equals(row.getPositiveSource()))
            bad("RECEIVED_SOURCE");
        source(caseId, row.getSourceSampleItemId());
        text(r.gramStain, 255, false);
        text(r.colonyMorphology, 255, false);
        var existing = isolates.getAllMatching("caseId", caseId);
        int number = 1;
        while (true) {
            final String label = "ISO-" + number;
            if (existing.stream().noneMatch(i -> label.equals(i.getIsolateLabel())))
                break;
            number++;
        }
        var isolate = new MicroIsolate();
        isolate.setCaseId(caseId);
        isolate.setIsolateLabel("ISO-" + number);
        isolate.setSourceCultureId(row.getId());
        isolate.setSourceCulture(row);
        isolate.setSourceSampleItemId(row.getSourceSampleItemId());
        isolate.setGramStain(trim(r.gramStain));
        isolate.setColonyMorphology(trim(r.colonyMorphology));
        isolate.setSignificance(parseSignificance(r.significance).name());
        isolate.setCreatedBy(actor);
        isolate.setCreatedAt(MicroCaseServiceImpl.now());
        isolate.setSysUserId(actor);
        var amendment = amendments.getOpenByCaseId(caseId);
        if (amendment != null)
            isolate.setAmendmentId(amendment.getId());
        isolates.insert(isolate);
        audit(caseId, "ISOLATE_CREATED", isolate.getId(), actor);
        if (amendment == null)
            c.setStage(MicroCaseStage.IDENTIFICATION.name());
        c.setSysUserId(actor);
        cases.update(c);
        return get(caseId, actor);
    }

    @Override
    @Transactional
    public MicroIsolateWorkspaceForm act(String caseId, String isolateId, String action,
            MicroIsolateWorkspaceRequestForm r, String actor) {
        if (action == null)
            bad("ACTION");
        String role = List.of("reidentify", "acknowledge").contains(action) ? Constants.ROLE_VALIDATION
                : Constants.ROLE_RESULTS;
        var c = writeCase(caseId, actor, role);
        var i = requireIsolate(caseId, isolateId);
        if (r == null)
            bad("REQUIRED");
        switch (action) {
        case "identify", "reidentify" -> {
            if (identified(i) != "reidentify".equals(action))
                bad("IDENTIFICATION_ACTION");
            requireVersion(i, r.version);
            var method = methods.findById(r.identificationMethod);
            if (method == null || !"Y".equals(method.getIsActive()))
                bad("METHOD");
            text(method.getMethodName(), 80, true);
            text(r.reason, 2000, "reidentify".equals(action));
            if (r.identificationStatus == null || !List.of("PRELIMINARY", "FINAL").contains(r.identificationStatus))
                bad("STATUS");
            requireOrganism(r.organismId);
            if (r.identifiedAt == null || r.identifiedAt.after(MicroCaseServiceImpl.now()))
                bad("IDENTIFICATION_TIME");
            if (r.confidence != null
                    && (r.confidence.signum() < 0 || r.confidence.compareTo(new java.math.BigDecimal("100")) > 0
                            || r.confidence.stripTrailingZeros().scale() > 2))
                bad("CONFIDENCE");
            if (received(i) && (i.getReceivedFromOrganizationId() == null
                    || (i.getReportedOrganismId() == null && i.getReportedOrganismText() == null)))
                bad("SENDER_REQUIRED");
            identification.updateIdentification(isolateId, r.organismId, null, parseSignificance(r.significance),
                    "FINAL".equals(r.identificationStatus) ? MicroIsolateIdentificationStatus.CONFIRMED
                            : MicroIsolateIdentificationStatus.PRELIMINARY,
                    method.getMethodName(), r.confidence, r.reason, r.identifiedAt, actor);
        }
        case "sample-item" -> ensureSampleItem(c, i, r.sampleTypeId, r.purpose, actor);
        case "received-origin" -> {
            if (!received(i) || identified(i))
                bad("RECEIVED_ORIGIN_LOCKED");
            if (r.sendingOrganizationId == null)
                bad("ORGANIZATION");
            var org = organizations.get(r.sendingOrganizationId);
            if (org == null || !"Y".equals(org.getIsActive()))
                bad("ORGANIZATION");
            if (trim(r.reportedOrganismId) != null)
                requireOrganism(r.reportedOrganismId);
            text(r.reportedOrganismText, 255, trim(r.reportedOrganismId) == null);
            i.setReceivedFromOrganizationId(org.getId());
            i.setReportedOrganismId(trim(r.reportedOrganismId));
            i.setReportedOrganismText(trim(r.reportedOrganismText));
            i.setSysUserId(actor);
            isolates.update(i);
            audit(caseId, "RECEIVED_ISOLATE_ORIGIN", isolateId, actor);
        }
        case "acknowledge" -> {
            requireVersion(i, r.version);
            if (!mismatch(i))
                bad("NO_MISMATCH");
            text(r.reason, 2000, true);
            i.setMismatchAcknowledgedBy(actor);
            i.setMismatchAcknowledgedAt(MicroCaseServiceImpl.now());
            i.setSysUserId(actor);
            isolates.update(i);
            audit(caseId, "IDENTIFICATION_MISMATCH_ACKNOWLEDGED", r.reason.trim(), actor);
        }
        case "referral" -> {
            var sample = ensureSampleItem(c, i, r.sampleTypeId, "REFERRAL", actor);
            r.sampleItemId = sample.getId();
            createReferral(c, r, actor);
        }
        case "test" -> {
            var sample = ensureSampleItem(c, i, r.sampleTypeId, "TESTING", actor);
            addTest(caseId, sample.getId(), r.testId, actor);
        }
        default -> bad("ACTION");
        }
        return get(caseId, actor);
    }

    @Override
    @Transactional
    public MicroIsolateWorkspaceForm refer(String caseId, MicroIsolateWorkspaceRequestForm r, String actor) {
        var c = writeCase(caseId, actor, Constants.ROLE_RESULTS);
        if (r == null)
            bad("REQUIRED");
        source(caseId, r.sampleItemId);
        createReferral(c, r, actor);
        return get(caseId, actor);
    }

    private void createReferral(MicroCase c, MicroIsolateWorkspaceRequestForm r, String actor) {
        if (r.referral == null)
            bad("REFERRAL_REQUIRED");
        var item = r.referral;
        if (item.getReferredInstituteId() == null)
            bad("ORGANIZATION");
        var receivingLab = organizations.get(item.getReferredInstituteId());
        if (receivingLab == null || !"Y".equals(receivingLab.getIsActive()))
            bad("ORGANIZATION");
        item.setReferralReasonId(trim(item.getReferralReasonId()));
        if (item.getReferralReasonId() != null && reasons.getMatch("id", item.getReferralReasonId()).isEmpty())
            bad("REFERRAL_REASON");
        text(item.getSubcontractNotes(), 2000, false);
        item.setReferredSendDate(trim(item.getReferredSendDate()));
        if (item.getReferredSendDate() != null) {
            var format = new java.text.SimpleDateFormat(org.openelisglobal.common.util.DateUtil.getDateFormat());
            format.setLenient(false);
            var position = new java.text.ParsePosition(0);
            var sent = format.parse(item.getReferredSendDate(), position);
            if (sent == null || position.getIndex() != item.getReferredSendDate().length()
                    || sent.after(MicroCaseServiceImpl.now()))
                bad("REFERRAL_DATE");
        }
        text(item.getAgreementReference(), 100, false);
        text(item.getCocContactName(), 100, false);
        text(item.getCocContactPhone(), 50, false);
        text(item.getCocContactEmail(), 255, false);
        var analysis = addTest(c.getId(), r.sampleItemId, r.testId, actor);
        if (referrals.getWorkspaceReferrals(List.of(r.sampleItemId)).stream()
                .anyMatch(f -> analysis.getId().equals(f.getAnalysis().getId()) && f.getStatus() != null
                        && !f.getStatus().isTerminal()))
            bad("REFERRAL_EXISTS");
        item.setReferralId(null);
        item.setReferredTestId(r.testId);
        var referral = referralSets.createDraftForAnalysis(analysis, item, actor);
        audit(c.getId(), "ISOLATE_REFERRED", referral.getId(), actor);
    }

    private org.openelisglobal.analysis.valueholder.Analysis addTest(String caseId, String sampleItemId, String testId,
            String actor) {
        if (options(caseId).tests.stream().noneMatch(t -> t.id().equals(testId)))
            bad("TEST");
        var body = new MicroCaseAddTestsForm();
        body.sampleItemId = sampleItemId;
        body.placement = "ADDITIONAL";
        body.testIds = List.of(testId);
        results.addTests(caseId, body, actor);
        return analyses.getAnalyses(caseId).stream()
                .filter(a -> a.getSampleItem().getId().equals(sampleItemId) && a.getTest().getId().equals(testId))
                .findFirst().orElseThrow(() -> new IllegalStateException("Isolate analysis missing"));
    }

    private SampleItem ensureSampleItem(MicroCase c, MicroIsolate i, String typeId, String purpose, String actor) {
        if (purpose == null || !List.of("LABEL", "REFERRAL", "TESTING").contains(purpose))
            bad("SAMPLE_PURPOSE");
        if (i.getSampleItemId() != null)
            return source(c.getId(), i.getSampleItemId());
        var origin = source(c.getId(), i.getSourceSampleItemId());
        if (typeId == null)
            bad("SAMPLE_TYPE");
        var type = sampleTypes.get(typeId);
        if (type == null || !type.getIsActive() || !type.isMicrobiologyIsolate())
            bad("SAMPLE_TYPE");
        // Serialize order-wide item numbering as well as isolate idempotency.
        cases.lockOrder(origin.getSample().getId());
        var sample = new SampleItem();
        sample.setSample(origin.getSample());
        sample.setParentSampleItem(origin);
        sample.setTypeOfSample(type);
        sample.setStatusId(origin.getStatusId());
        sample.setReceivedDate(MicroCaseServiceImpl.now());
        int sort = sampleItems.getSampleItemsBySampleId(origin.getSample().getId()).stream()
                .mapToInt(s -> Integer.parseInt(s.getSortOrder())).max().orElse(0) + 1;
        sample.setSortOrder(String.valueOf(sort));
        sample.setQuantity(1.0);
        sample.setFhirUuid(UUID.randomUUID());
        sample.setExternalId(origin.getSample().getAccessionNumber() + "-" + i.getIsolateLabel());
        sample.setSysUserId(actor);
        sample.setId(sampleItems.insert(sample));
        membership.addSample(c.getId(), sample.getId(), actor);
        i.setSampleItemId(sample.getId());
        i.setSysUserId(actor);
        isolates.update(i);
        audit(c.getId(), "ISOLATE_SAMPLE_CREATED", i.getId() + ":" + purpose, actor);
        return sample;
    }

    private MicroCase writeCase(String id, String actor, String role) {
        var c = cases.getForUpdate(id);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (actor == null || !access.hasLabUnitRole(actor, c.getLabUnitId(), role))
            throw new AccessDeniedException("Case lab unit rights required");
        MicroCaseMutationGuard.requireMutable(c);
        return c;
    }

    private MicroCase requireCase(String id) {
        return cases.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private MicroIsolate requireIsolate(String caseId, String id) {
        return isolates.getWorkspace(caseId).stream().filter(i -> i.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private SampleItem source(String caseId, String id) {
        return cultures.getSources(caseId).stream().filter(s -> s.getId().equals(id) && !s.isRejected()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_ISOLATE_SOURCE"));
    }

    private void requireOrganism(String id) {
        if (id == null || organisms.get(id).filter(o -> "Y".equals(o.getIsActive())).isEmpty())
            bad("ORGANISM");
    }

    static boolean received(MicroIsolate i) {
        return i.getSourceCulture() != null && "RECEIVED_ISOLATE".equals(i.getSourceCulture().getPositiveSource());
    }

    static boolean identified(MicroIsolate i) {
        return i.getOrganismId() != null || trim(i.getPreliminaryOrganismText()) != null;
    }

    private boolean mutable(MicroCase c) {
        try {
            MicroCaseMutationGuard.requireMutable(c);
            return true;
        } catch (MicroCaseLockedException e) {
            return false;
        }
    }

    private String version(MicroIsolate i) {
        return i.getLastupdated() == null ? "" : i.getLastupdated().toString();
    }

    private void requireVersion(MicroIsolate i, String expected) {
        if (!Objects.equals(version(i), expected))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MICROBIOLOGY_ISOLATE_STALE");
    }

    public boolean mismatch(MicroIsolate i) {
        if (!identified(i))
            return false;
        if (i.getReportedOrganismId() != null)
            return !i.getReportedOrganismId().equals(i.getOrganismId());
        return trim(i.getReportedOrganismText()) != null && !i.getReportedOrganismText().trim()
                .equalsIgnoreCase(organismName(i.getOrganismId(), i.getPreliminaryOrganismText()));
    }

    private String organismName(String id, String fallback) {
        return id == null ? fallback : organisms.get(id).map(MicroOrganism::getDisplayName).orElse(fallback);
    }

    private String organizationName(String id) {
        var org = id == null ? null : organizations.get(id);
        return org == null ? null : org.getOrganizationName();
    }

    private static String label(SampleItem sample) {
        return sample.getExternalId() == null ? sample.getSample().getAccessionNumber() + "-" + sample.getSortOrder()
                : sample.getExternalId();
    }

    private static String status(String value) {
        return "CONFIRMED".equals(value) ? "FINAL" : value;
    }

    private static String significance(String value) {
        return "CLINICALLY_SIGNIFICANT".equals(value) ? "SIGNIFICANT" : value;
    }

    private MicroIsolateSignificance parseSignificance(String value) {
        try {
            return MicroIsolateSignificance.valueOf(
                    value == null ? "UNKNOWN" : "SIGNIFICANT".equals(value) ? "CLINICALLY_SIGNIFICANT" : value);
        } catch (IllegalArgumentException e) {
            bad("SIGNIFICANCE");
            return null;
        }
    }

    private void audit(String caseId, String type, String note, String actor) {
        var activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(type);
        activity.setNote(note);
        activity.setPerformedBy(actor);
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setSysUserId(actor);
        activities.insert(activity);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void text(String value, int max, boolean required) {
        if ((required && trim(value) == null) || (value != null && value.length() > max))
            bad("TEXT");
    }

    private static void bad(String code) {
        throw new IllegalArgumentException("MICROBIOLOGY_ISOLATE_" + code);
    }
}
