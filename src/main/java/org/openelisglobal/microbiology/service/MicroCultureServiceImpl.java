package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MicroCultureServiceImpl implements MicroCultureService {
    private final MicroCaseDAO cases;
    private final MicroCultureDAO cultures;
    private final MicroCaseInoculationDAO rows;
    private final MicroCaseAnalysisDAO analyses;
    private final MicroCaseActivityDAO activities;
    private final MicrobiologyCaseAccessService access;
    private final InventoryItemService items;
    private final InventoryLotService lots;
    private final DictionaryService dictionaries;
    private final TestSectionService units;
    private final TestReagentLinkService mediaLinks;
    private final org.openelisglobal.typeofsample.service.TypeOfSampleTestService sampleTests;

    public MicroCultureServiceImpl(MicroCaseDAO cases, MicroCultureDAO cultures, MicroCaseInoculationDAO rows,
            MicroCaseAnalysisDAO analyses, MicroCaseActivityDAO activities, MicrobiologyCaseAccessService access,
            InventoryItemService items, InventoryLotService lots, DictionaryService dictionaries,
            TestSectionService units, TestReagentLinkService mediaLinks,
            org.openelisglobal.typeofsample.service.TypeOfSampleTestService sampleTests) {
        this.cases = cases;
        this.cultures = cultures;
        this.rows = rows;
        this.analyses = analyses;
        this.activities = activities;
        this.access = access;
        this.items = items;
        this.lots = lots;
        this.dictionaries = dictionaries;
        this.units = units;
        this.mediaLinks = mediaLinks;
        this.sampleTests = sampleTests;
    }

    private MicroCase writeCase(String caseId, String actor) {
        var c = cases.getForUpdate(caseId);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (actor == null || actor.isBlank() || !access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS))
            throw new AccessDeniedException("Case lab unit Results rights required");
        MicroCaseMutationGuard.requireMutable(c);
        return c;
    }

    @Override
    public List<MicroCultureForm> getRows(String caseId) {
        return cultures.getRows(caseId).stream().map(this::form).toList();
    }

    @Override
    public MicroCultureOptionsForm getCatalog() {
        var f = new MicroCultureOptionsForm();
        f.atmospheres = choices("Culture atmosphere");
        f.readings = choices("Culture reading");
        f.quantities = choices("Culture quantity");
        f.extensionReasons = choices("Extend incubation reason");
        f.media = items.getAllActive().stream().filter(InventoryItem::isMicrobiologyMedium)
                .map(item -> new MicroCultureOptionsForm.Medium(item.getId(), item.getName(), item.isTrackLots(),
                        item.getUsualAtmosphereId(), item.getUsualTemperature(),
                        lots.getByInventoryItemId(item.getId()).stream().filter(this::usable)
                                .map(lot -> new MicroCultureOptionsForm.Lot(lot.getId(), lot.getLotNumber())).toList()))
                .toList();
        return f;
    }

    @Override
    public MicroCultureOptionsForm getOptions(String caseId) {
        var c = cases.get(caseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var f = getCatalog();
        f.requireTrackedMedia = units.get(c.getLabUnitId()).isRequireTrackedMedia();
        f.sources = cultures.getSources(caseId).stream().filter(s -> !s.isRejected())
                .map(s -> new MicroCultureOptionsForm.Source(s.getId(),
                        s.getExternalId() == null ? s.getSample().getAccessionNumber() + "-" + s.getSortOrder()
                                : s.getExternalId(),
                        s.getTypeOfSample() == null ? null : s.getTypeOfSample().getId(),
                        s.getTypeOfSample() == null ? null : s.getTypeOfSample().getDescription()))
                .toList();
        String gramTest = units.get(c.getLabUnitId()).getGramStainTestId();
        f.gramStainTest = units.getTestsInSection(c.getLabUnitId()).stream()
                .filter(t -> t.isActive() && !Boolean.FALSE.equals(t.getOrderable()) && t.getId().equals(gramTest))
                .map(t -> new IdValuePair(t.getId(), t.getName())).findFirst().orElse(null);
        f.gramStainSampleTypeIds = f.gramStainTest == null ? List.of()
                : sampleTests.getTypeOfSampleTestsForTest(f.gramStainTest.getId()).stream()
                        .map(org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest::getTypeOfSampleId).toList();
        f.mediaLinks = analyses.getAnalyses(caseId).stream()
                .filter(a -> "CULTURE".equals(a.getTest().getMicrobiologyCaseRole())
                        || analyses.getByCaseAndAnalysis(caseId, a.getId()).getCaseRole() == MicroCaseRole.CULTURE)
                .flatMap(a -> mediaLinks.getByTestId(a.getTest().getId()).stream())
                .filter(link -> f.media.stream().anyMatch(m -> m.id().equals(link.getReagentId())))
                .map(link -> new MicroCultureOptionsForm.MediaLink(link.getReagentId(), link.getSampleTypeId(),
                        link.getCultureDuration(), link.getCultureDurationUnit(), link.getCultureCheckIntervalHours(),
                        link.getCultureLoopVolume(), link.getCultureAtmosphereId(), link.getCultureTemperature()))
                .toList();
        return f;
    }

    @Override
    @Transactional
    public List<MicroCultureForm> inoculate(String caseId, MicroCultureRequestForm r, String actor) {
        var c = writeCase(caseId, actor);
        if (r == null)
            bad("REQUIRED");
        var source = requireSource(caseId, r.sourceSampleItemId);
        var received = source.getReceivedDate() == null ? source.getSample().getReceivedTimestamp()
                : source.getReceivedDate();
        validateInoculated(r.inoculatedAt, received);
        MicroCultureTiming.milliseconds(r.duration, r.durationUnit);
        if (r.checkIntervalHours != null)
            MicroCultureTiming.milliseconds(r.checkIntervalHours, "HOURS");
        MicroCultureTiming.validateTemperature(r.temperature);
        MicroCultureTiming.validateLoopVolume(r.loopVolume);
        coded(r.atmosphereId, "Culture atmosphere", true);
        var medium = r.mediumItemId == null ? null
                : items.getAllMatching("id", r.mediumItemId).stream().findFirst().orElse(null);
        if (medium == null || !medium.isActive() || !medium.isMicrobiologyMedium())
            bad("MEDIUM");
        if (r.notTracked && (units.get(c.getLabUnitId()).isRequireTrackedMedia() || medium.isTrackLots()))
            bad("TRACKED_MEDIA_REQUIRED");
        if (!r.notTracked) {
            var lot = r.lotId == null ? null : lots.getAllMatching("id", r.lotId).stream().findFirst().orElse(null);
            if (lot == null || !Objects.equals(lot.getInventoryItem().getId(), medium.getId()) || !usable(lot))
                bad("LOT");
        } else if (r.lotId != null)
            bad("LOT");
        var row = new MicroCaseInoculation();
        row.setCaseId(caseId);
        row.setSourceSampleItemId(r.sourceSampleItemId);
        if (r.parentId != null && !r.parentId.isBlank()) {
            var parent = requireRow(caseId, r.parentId);
            if (!Objects.equals(parent.getSourceSampleItemId(), r.sourceSampleItemId))
                bad("PARENT_SOURCE");
            if (r.inoculatedAt.before(parent.getOccurredAt()))
                bad("PARENT_TIME");
            try {
                row.setSubculturePurpose(MicroCulturePurpose.valueOf(r.subculturePurpose).name());
            } catch (RuntimeException error) {
                bad("PURPOSE");
            }
            row.setSourceInoculationId(parent.getId());
        }
        text(r.containerIdentifier, 80);
        row.setContainerIdentifier(r.containerIdentifier.trim());
        row.setMedia(medium.getName());
        row.setMediumItemId(medium.getId());
        row.setLotId(r.lotId);
        row.setNotTracked(r.notTracked);
        row.setAtmosphere(r.atmosphereId);
        row.setTemperature(r.temperature);
        row.setDuration(r.duration);
        row.setDurationUnit(r.durationUnit);
        row.setCheckIntervalHours(r.checkIntervalHours);
        row.setLoopVolume(r.loopVolume);
        row.setOccurredAt(r.inoculatedAt);
        row.setPerformedBy(actor);
        row.setSysUserId(actor);
        row.setActivityId(audit(caseId, "CULTURE_INOCULATED", row.getId(), r, actor).getId());
        rows.insert(row);
        if (List.of("RECEIVED", "INITIAL_TESTING", "SETUP_RECORDED").contains(c.getStage())) {
            c.setStage(MicroCaseStage.INCUBATING.name());
            c.setSysUserId(actor);
            cases.update(c);
        }
        return getRows(caseId);
    }

    @Override
    @Transactional
    public List<MicroCultureForm> act(String caseId, String rowId, String action, MicroCultureRequestForm r,
            String actor) {
        var c = writeCase(caseId, actor);
        var row = requireRow(caseId, rowId);
        if (r == null)
            bad("REQUIRED");
        Timestamp now = Timestamp.from(Instant.now());
        if (!"outcome".equals(action) && !"positive-time".equals(action)
                && ("NO_GROWTH".equals(row.getOutcome()) || "CONTAMINATED".equals(row.getOutcome())))
            bad("CONCLUDED");
        switch (action) {
        case "reading" -> {
            coded(r.readingId, "Culture reading", true);
            coded(r.quantityId, "Culture quantity", false);
            note(r.note);
            var reading = new MicroCultureReading();
            reading.setInoculation(row);
            reading.setReadingId(r.readingId);
            reading.setQuantityId(r.quantityId);
            reading.setNote(r.note);
            reading.setIncubationDay(MicroCultureTiming.hours(row.getOccurredAt(), now).divide(new BigDecimal("24"), 2,
                    java.math.RoundingMode.HALF_UP));
            reading.setRecordedBy(actor);
            reading.setRecordedAt(now);
            reading.setSysUserId(actor);
            cultures.insert(reading);
            row.getReadings().add(reading);
        }
        case "extension" -> {
            MicroCultureTiming.milliseconds(r.extendBy, r.unit);
            coded(r.reasonId, "Extend incubation reason", true);
            note(r.note);
            var extension = new MicroCultureExtension();
            extension.setInoculation(row);
            extension.setExtendBy(r.extendBy);
            extension.setUnit(r.unit);
            extension.setReasonId(r.reasonId);
            extension.setNote(r.note);
            extension.setRecordedAt(now);
            extension.setRecordedBy(actor);
            extension.setSysUserId(actor);
            cultures.insert(extension);
            row.getExtensions().add(extension);
        }
        case "inoculated-time" -> {
            if (!row.getReadings().isEmpty())
                bad("FIRST_READING_LOCK");
            var source = requireSource(caseId, row.getSourceSampleItemId());
            validateInoculated(r.inoculatedAt,
                    source.getReceivedDate() == null ? source.getSample().getReceivedTimestamp()
                            : source.getReceivedDate());
            if (row.getPositiveAt() != null && row.getPositiveAt().before(r.inoculatedAt))
                bad("POSITIVE_TIME");
            if (row.getSourceInoculationId() != null
                    && r.inoculatedAt.before(requireRow(caseId, row.getSourceInoculationId()).getOccurredAt()))
                bad("PARENT_TIME");
            if (cultures.getRows(caseId).stream().anyMatch(child -> rowId.equals(child.getSourceInoculationId())
                    && r.inoculatedAt.after(child.getOccurredAt())))
                bad("PARENT_TIME");
            audit(caseId, "CULTURE_INOCULATED_TIME", rowId, Map.of("old", row.getOccurredAt(), "new", r.inoculatedAt),
                    actor);
            row.setOccurredAt(r.inoculatedAt);
        }
        case "positive-time" -> {
            validatePositive(r.positiveAt, row.getOccurredAt());
            audit(caseId, "CULTURE_POSITIVE_TIME", rowId,
                    Map.of("old", row.getPositiveAt() == null ? "" : row.getPositiveAt(), "new", r.positiveAt), actor);
            row.setPositiveAt(r.positiveAt);
            row.setPositiveSource("USER");
        }
        case "negative-proposal" -> {
            text(r.signal, 1000);
            var proposal = new MicroCultureProposal();
            proposal.setInoculation(row);
            proposal.setSignal(r.signal);
            proposal.setRecordedAt(now);
            proposal.setRecordedBy(actor);
            proposal.setSysUserId(actor);
            cultures.insert(proposal);
            row.getProposals().add(proposal);
        }
        case "outcome" -> {
            if (!List.of("GROWTH", "NO_GROWTH", "CONTAMINATED").contains(r.outcome == null ? "" : r.outcome))
                bad("OUTCOME");
            if (row.getOutcome() != null && !("NO_GROWTH".equals(row.getOutcome()) && "GROWTH".equals(r.outcome)))
                bad("CONCLUDED");
            if (r.proposalId != null) {
                if (!"NO_GROWTH".equals(r.outcome))
                    bad("PROPOSAL");
                var proposal = row.getProposals().stream()
                        .filter(p -> p.getId().equals(r.proposalId) && p.getConfirmedAt() == null).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_CULTURE_PROPOSAL"));
                proposal.setConfirmedAt(now);
                proposal.setConfirmedBy(actor);
                proposal.setSysUserId(actor);
            }
            if ("GROWTH".equals(r.outcome)) {
                if (r.positiveAt != null) {
                    validatePositive(r.positiveAt, row.getOccurredAt());
                    row.setPositiveAt(r.positiveAt);
                    row.setPositiveSource("USER");
                } else if (row.getPositiveAt() == null) {
                    row.setPositiveAt(now);
                    row.setPositiveSource("USER");
                }
                if (!List.of("IDENTIFICATION", "AST_READY", "AST_IN_PROGRESS", "REVIEW_READY", "PRELIM_RELEASED",
                        "AMENDED").contains(c.getStage())) {
                    c.setStage(MicroCaseStage.GROWTH_DETECTED.name());
                    c.setSysUserId(actor);
                    cases.update(c);
                }
            }
            row.setOutcome(r.outcome);
            row.setOutcomeBy(actor);
            row.setOutcomeAt(now);
        }
        default -> bad("ACTION");
        }
        row.setSysUserId(actor);
        rows.update(row);
        audit(caseId, "CULTURE_" + action.toUpperCase(Locale.ROOT).replace('-', '_'), rowId, r, actor);
        return getRows(caseId);
    }

    private MicroCaseInoculation requireRow(String caseId, String rowId) {
        return cultures.getRows(caseId).stream().filter(r -> r.getId().equals(rowId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private SampleItem requireSource(String caseId, String id) {
        return cultures.getSources(caseId).stream().filter(s -> s.getId().equals(id) && !s.isRejected()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_CULTURE_SOURCE"));
    }

    private void validateInoculated(Timestamp at, Timestamp received) {
        if (received == null || at == null || at.toInstant().isAfter(Instant.now()) || at.before(received))
            bad("INOCULATED_TIME");
    }

    private void validatePositive(Timestamp at, Timestamp start) {
        if (at == null || at.before(start) || at.toInstant().isAfter(Instant.now()))
            bad("POSITIVE_TIME");
    }

    private boolean usable(InventoryLot lot) {
        return lot.getCurrentQuantity() != null && lot.isAvailableForUse();
    }

    private List<IdValuePair> choices(String category) {
        return dictionaries.getActiveSortedEntriesByCategoryName(category).stream()
                .map(d -> new IdValuePair(d.getId(), d.getLocalizedName())).toList();
    }

    private void coded(String id, String category, boolean required) {
        if (id == null || id.isBlank()) {
            if (required)
                bad("CODE");
            return;
        }
        if (choices(category).stream().noneMatch(d -> d.getId().equals(id)))
            bad("CODE");
    }

    private String label(String id) {
        var dictionary = id == null ? null : dictionaries.getDictionaryById(id);
        return dictionary == null ? id : dictionary.getLocalizedName();
    }

    private MicroCaseActivity audit(String caseId, String type, String rowId, Object data, String actor) {
        var event = new MicroCaseActivity();
        event.setCaseId(caseId);
        event.setActivityType(type);
        event.setPerformedBy(actor);
        event.setOccurredAt(Timestamp.from(Instant.now()));
        event.setSysUserId(actor);
        try {
            event.setStructuredData(new ObjectMapper().writeValueAsString(Map.of("cultureId", rowId, "data", data)));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException(error);
        }
        activities.insert(event);
        return event;
    }

    private void text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max)
            bad("TEXT");
    }

    private void note(String value) {
        if (value != null && value.length() > 2000)
            bad("NOTE");
    }

    private void bad(String suffix) {
        throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_" + suffix);
    }

    private MicroCultureForm form(MicroCaseInoculation row) {
        var f = new MicroCultureForm();
        f.id = row.getId();
        f.parentId = row.getSourceInoculationId();
        f.sourceSampleItemId = row.getSourceSampleItemId();
        f.subculturePurpose = row.getSubculturePurpose();
        f.containerIdentifier = row.getContainerIdentifier();
        f.mediumItemId = row.getMediumItemId();
        f.mediumName = row.getMedia();
        f.lotId = row.getLotId();
        var lot = f.lotId == null ? null : lots.get(f.lotId);
        f.lotNumber = lot == null ? null : lot.getLotNumber();
        f.notTracked = row.isNotTracked();
        f.atmosphereId = row.getAtmosphere();
        f.atmosphereName = label(f.atmosphereId);
        f.temperature = row.getTemperature();
        f.duration = row.getDuration();
        f.durationUnit = row.getDurationUnit();
        f.checkIntervalHours = row.getCheckIntervalHours();
        f.loopVolume = row.getLoopVolume();
        f.inoculatedAt = row.getOccurredAt();
        f.positiveAt = row.getPositiveAt();
        f.positiveSource = row.getPositiveSource();
        f.outcome = row.getOutcome();
        f.outcomeAt = row.getOutcomeAt();
        f.outcomeBy = row.getOutcomeBy();
        f.readings = row.getReadings().stream()
                .map(r -> new MicroCultureForm.Reading(r.getId(), r.getReadingId(), label(r.getReadingId()),
                        r.getQuantityId(), label(r.getQuantityId()), r.getNote(), r.getIncubationDay(),
                        r.getRecordedBy(), r.getRecordedAt()))
                .toList();
        f.extensions = row.getExtensions().stream()
                .map(e -> new MicroCultureForm.Extension(e.getId(), e.getExtendBy(), e.getUnit(), e.getReasonId(),
                        label(e.getReasonId()), e.getNote(), e.getRecordedBy(), e.getRecordedAt()))
                .toList();
        f.proposals = row.getProposals().stream().map(p -> new MicroCultureForm.Proposal(p.getId(), p.getSignal(),
                p.getRecordedAt(), p.getConfirmedBy(), p.getConfirmedAt())).toList();
        boolean incubating = f.outcome == null || "GROWTH".equals(f.outcome);
        f.canEditInoculatedAt = f.readings.isEmpty() && incubating;
        if (f.duration != null && f.durationUnit != null) {
            long extensionMillis = row.getExtensions().stream()
                    .mapToLong(e -> MicroCultureTiming.milliseconds(e.getExtendBy(), e.getUnit())).sum();
            f.incubationEnds = MicroCultureTiming.ends(f.inoculatedAt, f.duration, f.durationUnit, extensionMillis);
            Timestamp last = row.getReadings().stream().map(MicroCultureReading::getRecordedAt)
                    .max(Comparator.naturalOrder()).orElse(null);
            f.nextCheck = MicroCultureTiming.nextCheck(f.inoculatedAt, last, f.checkIntervalHours);
            Instant now = Instant.now();
            f.finalReadDue = incubating && MicroCultureTiming.due(f.incubationEnds, now);
            f.checkDue = incubating && MicroCultureTiming.due(f.nextCheck, now);
            f.overdue = incubating && (f.incubationEnds.toInstant().isBefore(now)
                    || f.nextCheck != null && f.nextCheck.toInstant().isBefore(now));
        }
        if (f.positiveAt != null)
            f.timeToPositivityHours = MicroCultureTiming.hours(f.inoculatedAt, f.positiveAt);
        return f;
    }
}
