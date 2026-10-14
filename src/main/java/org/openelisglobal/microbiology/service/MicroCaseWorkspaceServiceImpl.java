package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MicroCaseWorkspaceServiceImpl implements MicroCaseWorkspaceService {
    private final MicroCaseSearchDAO searchDAO;
    private final MicroCaseDAO cases;
    private final MicroCaseRequestDAO requests;
    private final MicroCaseMembershipService membership;
    private final MicrobiologyCaseAccessService access;
    private final TestSectionService units;
    private final TypeOfSampleService sampleTypes;
    private final MicroCaseActivityDAO activities;

    public MicroCaseWorkspaceServiceImpl(MicroCaseSearchDAO searchDAO, MicroCaseDAO cases, MicroCaseRequestDAO requests,
            MicroCaseMembershipService membership, MicrobiologyCaseAccessService access, TestSectionService units,
            TypeOfSampleService sampleTypes, MicroCaseActivityDAO activities) {
        this.searchDAO = searchDAO;
        this.cases = cases;
        this.requests = requests;
        this.membership = membership;
        this.access = access;
        this.units = units;
        this.sampleTypes = sampleTypes;
        this.activities = activities;
    }

    @Override
    public MicroCaseSearchPageForm search(MicroCaseSearchForm q, String userId) {
        normalize(q);
        Set<String> ids = readUnits(userId);
        if (q.labUnitId != null && ids != null && !ids.contains(q.labUnitId))
            throw new AccessDeniedException("Case lab unit access required");
        MicroCaseSearchPageForm page = new MicroCaseSearchPageForm();
        page.page = q.page;
        page.pageSize = q.pageSize;
        page.total = searchDAO.count(q, ids);
        page.rows = searchDAO.search(q, ids).stream().map(v -> summary(v, new MicroCaseSummaryForm())).toList();
        // Include units containing retained cases, even after their catalog tests are
        // deactivated.
        page.labUnits = units.getAllTestSections().stream().filter(u -> ids == null || ids.contains(u.getId()))
                .map(u -> new org.openelisglobal.common.util.IdValuePair(u.getId(), u.getTestSectionName())).toList();
        return page;
    }

    private Set<String> readUnits(String userId) {
        Set<String> results = access.permittedLabUnitIds(userId, Constants.ROLE_RESULTS);
        Set<String> validation = access.permittedLabUnitIds(userId, Constants.ROLE_VALIDATION);
        if (results == null || validation == null)
            return null;
        Set<String> ids = new LinkedHashSet<>(results);
        ids.addAll(validation);
        return ids;
    }

    @Override
    public MicroCaseShellForm get(String id, String userId) {
        if (userId == null || userId.isBlank())
            throw new AccessDeniedException("Authenticated system user required");
        Object[] v = searchDAO.getSummary(id);
        if (v == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        MicroCase c = (MicroCase) v[0];
        MicroCaseShellForm f = summary(v, new MicroCaseShellForm());
        f.readOnlyAccess = !access.canReadLabUnit(userId, c.getLabUnitId());
        boolean mutable = true;
        try {
            MicroCaseMutationGuard.requireMutable(c);
        } catch (MicroCaseLockedException locked) {
            mutable = false;
        }
        f.canWrite = mutable && access.hasLabUnitRole(userId, c.getLabUnitId(), Constants.ROLE_RESULTS);
        f.canValidate = access.hasLabUnitRole(userId, c.getLabUnitId(), Constants.ROLE_VALIDATION);
        for (var si : searchDAO.getSamples(id)) {
            MicroCaseSpecimenForm s = new MicroCaseSpecimenForm();
            s.id = si.getId();
            s.sampleItemId = si.getId();
            s.label = si.getExternalId();
            if (s.label == null || s.label.isBlank())
                s.label = si.getSample().getAccessionNumber() + "-" + si.getSortOrder();
            s.specimenType = si.getTypeOfSample() == null ? null : si.getTypeOfSample().getDescription();
            s.collectionDate = si.getCollectionDate();
            s.cultureSetNumber = si.getCultureSetNumber();
            f.samples.add(s);
        }
        Set<Integer> pending = new LinkedHashSet<>();
        for (var r : requests.getByCaseId(id)) {
            if (r.getCancelledAt() == null && r.getSampleItemId() == null && pending.add(r.getSampleTypeRequestId())) {
                MicroCaseSpecimenForm s = new MicroCaseSpecimenForm();
                s.id = String.valueOf(r.getSampleTypeRequestId());
                s.sampleTypeId = r.getSampleTypeId();
                var type = sampleTypes.get(r.getSampleTypeId());
                s.specimenType = type == null ? null : type.getDescription();
                f.pendingSamples.add(s);
            }
        }
        for (String relatedId : membership.getRelatedCaseIds(id)) {
            Object[] related = searchDAO.getSummary(relatedId);
            if (related != null)
                f.relatedCases.add(summary(related, new MicroCaseSummaryForm()));
        }
        if (f.canWrite) {
            var source = units.get(c.getLabUnitId());
            f.transferLabUnits = searchDAO.getEligibleLabUnits().stream()
                    .filter(u -> !u.getId().equals(c.getLabUnitId()))
                    .filter(u -> access.hasLabUnitRole(userId, u.getId(), Constants.ROLE_RESULTS))
                    .filter(u -> source != null && Objects.equals(source.getDomain(), units.get(u.getId()).getDomain()))
                    .toList();
        }
        return f;
    }

    @Override
    @Transactional
    public MicroCaseShellForm transfer(String id, String unitId, String userId) {
        MicroCase c = cases.getForUpdate(id);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!access.hasLabUnitRole(userId, c.getLabUnitId(), Constants.ROLE_RESULTS)
                || !access.hasLabUnitRole(userId, unitId, Constants.ROLE_RESULTS))
            throw new AccessDeniedException("Results rights in both lab units required");
        MicroCaseMutationGuard.requireMutable(c);
        if (Objects.equals(c.getLabUnitId(), unitId))
            return get(id, userId);
        var source = units.get(c.getLabUnitId());
        var destination = units.get(unitId);
        if (source == null || destination == null || !Objects.equals(source.getDomain(), destination.getDomain())
                || searchDAO.getEligibleLabUnits().stream().noneMatch(u -> u.getId().equals(unitId)))
            throw new IllegalArgumentException("Destination must be an eligible lab unit in the same domain");
        String from = c.getLabUnitId();
        c.setLabUnitId(unitId);
        c.setSysUserId(userId);
        cases.update(c);
        MicroCaseActivity a = new MicroCaseActivity();
        a.setCaseId(id);
        a.setActivityType("CASE_TRANSFERRED");
        a.setPerformedBy(userId);
        a.setOccurredAt(new Timestamp(System.currentTimeMillis()));
        a.setNote(source.getTestSectionName() + " → " + destination.getTestSectionName());
        try {
            a.setStructuredData(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(java.util.Map.of("fromLabUnitId", from, "toLabUnitId", unitId)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        activities.insert(a);
        return get(id, userId);
    }

    private <T extends MicroCaseSummaryForm> T summary(Object[] v, T f) {
        MicroCase c = (MicroCase) v[0];
        f.id = c.getId();
        f.accessionNumber = (String) v[1];
        f.labUnit = (String) v[2];
        f.patientId = (String) v[3];
        f.patientName = java.util.stream.Stream.of(v[4], v[5]).filter(Objects::nonNull).map(Object::toString)
                .collect(java.util.stream.Collectors.joining(" "));
        f.birthDate = (Timestamp) v[6];
        f.gender = (String) v[7];
        f.specimenType = (String) v[8];
        f.labUnitId = c.getLabUnitId();
        f.status = c.getStatus().name();
        f.stage = c.getStage();
        f.priority = c.getPriority();
        f.createdAt = c.getCreatedAt();
        return f;
    }

    private void normalize(MicroCaseSearchForm q) {
        q.status = text(q.status);
        q.labUnitId = text(q.labUnitId);
        q.q = text(q.q);
        q.patientId = text(q.patientId);
        q.accessionNumber = text(q.accessionNumber);
        q.from = text(q.from);
        q.to = text(q.to);
        if (q.status != null)
            MicroCaseStatus.valueOf(q.status);
        try {
            if (q.from != null)
                LocalDate.parse(q.from);
            if (q.to != null)
                LocalDate.parse(q.to);
        } catch (java.time.DateTimeException invalid) {
            throw new IllegalArgumentException("Invalid date filter", invalid);
        }
        if (q.from != null && q.to != null && q.from.compareTo(q.to) > 0)
            throw new IllegalArgumentException("Invalid date range");
        if (!java.util.List.of("newest", "accession").contains(q.sort))
            throw new IllegalArgumentException("Invalid sort");
        if (q.page < 1 || q.pageSize < 1 || q.pageSize > 100 || ((long) q.page - 1) * q.pageSize > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid pagination");
    }

    private String text(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
