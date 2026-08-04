package org.openelisglobal.microbiology.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointActivationEventDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroBreakpointRuleAdminForm;
import org.openelisglobal.microbiology.form.MicroBreakpointStandardAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointActivationEvent;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroBreakpointAdminServiceImpl implements MicroBreakpointAdminService {

    private final MicroBreakpointStandardDAO standardDAO;
    @SuppressWarnings("unused")
    private final MicroBreakpointRuleDAO ruleDAO;
    private final MicroBreakpointActivationEventDAO activationEventDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;

    public MicroBreakpointAdminServiceImpl(MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO,
            MicroBreakpointActivationEventDAO activationEventDAO, MicroAstRunDAO astRunDAO,
            MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO) {
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
        this.activationEventDAO = activationEventDAO;
        this.astRunDAO = astRunDAO;
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReferenceAdminPageForm<MicroBreakpointStandardAdminForm> getStandards(
            MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminQueryForm normalized = normalizeQuery(query);
        int offset = (normalized.page - 1) * normalized.pageSize;
        List<MicroBreakpointStandardAdminForm> rows = standardDAO.search(normalized.q, normalized.status,
                normalized.authority, normalized.sort, offset, normalized.pageSize).stream().map(this::toStandardForm)
                .toList();
        return page(rows, standardDAO.countSearch(normalized.q, normalized.status, normalized.authority), normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReferenceAdminPageForm<MicroBreakpointRuleAdminForm> getRules(String standardId,
            MicroReferenceAdminQueryForm query) {
        requireStandard(standardId);
        MicroReferenceAdminQueryForm normalized = normalizeQuery(query);
        int offset = (normalized.page - 1) * normalized.pageSize;
        List<MicroBreakpointRuleAdminForm> rows = ruleDAO.search(standardId, normalized.q, normalized.organism,
                normalized.antibiotic, normalized.method, normalized.specimenTypeId, offset, normalized.pageSize)
                .stream().map(this::toRuleForm).toList();
        MicroReferenceAdminPageForm<MicroBreakpointRuleAdminForm> page = new MicroReferenceAdminPageForm<>();
        page.rows = new ArrayList<>(rows);
        page.total = ruleDAO.countSearch(standardId, normalized.q, normalized.organism, normalized.antibiotic,
                normalized.method, normalized.specimenTypeId);
        page.page = normalized.page;
        page.pageSize = normalized.pageSize;
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandardAdminForm getStandard(String standardId) {
        return toStandardForm(requireStandard(standardId));
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRuleAdminForm getRule(String standardId, String ruleId) {
        requireStandard(standardId);
        org.openelisglobal.microbiology.valueholder.MicroBreakpointRule rule = ruleDAO.get(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Breakpoint rule not found: " + ruleId));
        if (!standardId.equals(rule.getStandardId())) {
            throw new IllegalArgumentException("Breakpoint rule does not belong to this standard");
        }
        return toRuleForm(rule);
    }

    @Override
    @Transactional
    public MicroBreakpointRuleAdminForm saveRule(String standardId, String ruleId, MicroBreakpointRuleAdminForm request,
            String actorId) {
        requireActor(actorId);
        MicroBreakpointStandard standard = requireStandard(standardId);
        if ("ARCHIVED".equals(standard.getLifecycleStatus())) {
            throw new MicroReferenceConflictException("Archived breakpoint standards are read-only");
        }
        if (request == null) {
            throw new IllegalArgumentException("Breakpoint rule is required");
        }
        String organismId = trimToNull(request.organismId);
        String organismGroup = trimToNull(request.organismGroup);
        if ((organismId == null) == (organismGroup == null)) {
            throw new IllegalArgumentException("Specify exactly one organism or organism group");
        }
        if (organismId != null && organismDAO.get(organismId).isEmpty()) {
            throw new IllegalArgumentException("Organism not found: " + organismId);
        }
        String antibioticId = required(request.antibioticId, "antibioticId");
        if (antibioticDAO.get(antibioticId).isEmpty()) {
            throw new IllegalArgumentException("Antibiotic not found: " + antibioticId);
        }
        String method = required(request.method, "method").toUpperCase(Locale.ROOT);
        if (!Set.of("MIC", "ZONE").contains(method)) {
            throw new IllegalArgumentException("method must be MIC or ZONE");
        }
        String breakpointType = required(request.breakpointType, "breakpointType").toUpperCase(Locale.ROOT);
        if (!Set.of("MIC", "ZONE").contains(breakpointType)) {
            throw new IllegalArgumentException("breakpointType must be MIC or ZONE");
        }
        if (request.susceptibleValue == null && request.intermediateLowerValue == null
                && request.intermediateUpperValue == null && request.resistantValue == null) {
            throw new IllegalArgumentException("At least one breakpoint threshold is required");
        }
        String specimenTypeId = trimToNull(request.specimenTypeId);
        Optional<org.openelisglobal.microbiology.valueholder.MicroBreakpointRule> duplicate = ruleDAO.findByNaturalKey(
                standardId, organismId, organismGroup, antibioticId, method, specimenTypeId, breakpointType);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(ruleId)) {
            throw new MicroReferenceConflictException("A breakpoint rule already exists for this context");
        }
        org.openelisglobal.microbiology.valueholder.MicroBreakpointRule rule = ruleId == null
                ? new org.openelisglobal.microbiology.valueholder.MicroBreakpointRule()
                : ruleDAO.get(ruleId)
                        .orElseThrow(() -> new IllegalArgumentException("Breakpoint rule not found: " + ruleId));
        if (ruleId != null && !standardId.equals(rule.getStandardId())) {
            throw new IllegalArgumentException("Breakpoint rule does not belong to this standard");
        }
        rule.setStandardId(standardId);
        rule.setOrganismId(organismId);
        rule.setOrganismGroup(organismGroup);
        rule.setAntibioticId(antibioticId);
        rule.setMethod(method);
        rule.setSpecimenTypeId(specimenTypeId);
        rule.setBreakpointType(breakpointType);
        rule.setSusceptibleValue(request.susceptibleValue);
        rule.setIntermediateLowerValue(request.intermediateLowerValue);
        rule.setIntermediateUpperValue(request.intermediateUpperValue);
        rule.setResistantValue(request.resistantValue);
        rule.setUnits(trimToNull(request.units));
        rule.setNotes(trimToNull(request.notes));
        rule.setIsActive(request.active ? "Y" : "N");
        rule.setSeeded(false);
        rule.setLocallyCustomized(true);
        rule.setSourceRowHash(null);
        rule.setLastUpdatedBy(actorId);
        if (ruleId == null) {
            ruleDAO.insert(rule);
        } else {
            ruleDAO.update(rule);
        }
        return toRuleForm(rule);
    }

    @Override
    @Transactional
    public void activate(String standardId, Date effectiveDate, String actorId) {
        requireActor(actorId);
        if (effectiveDate == null) {
            throw new IllegalArgumentException("effectiveDate is required");
        }
        MicroBreakpointStandard requested = requireStandard(standardId);
        if ("ARCHIVED".equals(requested.getLifecycleStatus())) {
            throw new MicroReferenceConflictException("Archived breakpoint standards cannot be activated");
        }

        List<MicroBreakpointStandard> activeStandards = standardDAO.getActiveForAuthority(requested.getAuthority());
        for (MicroBreakpointStandard active : activeStandards) {
            if (active.getId().equals(requested.getId())) {
                continue;
            }
            active.setLifecycleStatus("LOADED");
            active.setIsActive("N");
            active.setLastUpdatedBy(actorId);
            standardDAO.update(active);
            activationEventDAO.insert(event(active.getId(), "DEACTIVATED", effectiveDate, actorId));
        }

        requested.setLifecycleStatus("ACTIVE");
        requested.setIsActive("Y");
        requested.setEffectiveDate(effectiveDate);
        requested.setLastUpdatedBy(actorId);
        standardDAO.update(requested);
        activationEventDAO.insert(event(requested.getId(), "ACTIVATED", effectiveDate, actorId));
    }

    @Override
    @Transactional
    public void archive(String standardId, String actorId) {
        requireActor(actorId);
        MicroBreakpointStandard standard = requireStandard(standardId);
        if ("ACTIVE".equals(standard.getLifecycleStatus())) {
            throw new MicroReferenceConflictException("Activate a replacement before archiving this standard");
        }
        long unresolvedRuns = astRunDAO.countUnresolvedByBreakpointStandardId(standardId);
        if (unresolvedRuns > 0) {
            throw new MicroReferenceConflictException(
                    "Breakpoint standard is referenced by " + unresolvedRuns + " unresolved AST run(s)");
        }
        if ("ARCHIVED".equals(standard.getLifecycleStatus())) {
            return;
        }
        standard.setLifecycleStatus("ARCHIVED");
        standard.setIsActive("N");
        standard.setLastUpdatedBy(actorId);
        standardDAO.update(standard);
        activationEventDAO.insert(event(standardId, "ARCHIVED", standard.getEffectiveDate(), actorId));
    }

    private MicroBreakpointStandard requireStandard(String standardId) {
        if (standardId == null || standardId.isBlank()) {
            throw new IllegalArgumentException("standardId is required");
        }
        return standardDAO.get(standardId)
                .orElseThrow(() -> new IllegalArgumentException("Breakpoint standard not found: " + standardId));
    }

    private MicroBreakpointActivationEvent event(String standardId, String action, Date effectiveDate, String actorId) {
        MicroBreakpointActivationEvent event = new MicroBreakpointActivationEvent();
        event.setStandardId(standardId);
        event.setAction(action);
        event.setEffectiveDate(effectiveDate);
        event.setActorId(actorId);
        return event;
    }

    private void requireActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("authenticated actor is required");
        }
    }

    private MicroBreakpointStandardAdminForm toStandardForm(MicroBreakpointStandard standard) {
        MicroBreakpointStandardAdminForm form = new MicroBreakpointStandardAdminForm();
        form.id = standard.getId();
        form.authority = standard.getAuthority();
        form.version = standard.getVersion();
        form.lifecycleStatus = standard.getLifecycleStatus();
        form.effectiveDate = standard.getEffectiveDate() == null ? null : standard.getEffectiveDate().toString();
        form.ruleCount = ruleDAO.countByStandardId(standard.getId());
        form.unresolvedRunCount = astRunDAO.countUnresolvedByBreakpointStandardId(standard.getId());
        return form;
    }

    private MicroBreakpointRuleAdminForm toRuleForm(
            org.openelisglobal.microbiology.valueholder.MicroBreakpointRule rule) {
        MicroBreakpointRuleAdminForm form = new MicroBreakpointRuleAdminForm();
        form.id = rule.getId();
        form.standardId = rule.getStandardId();
        form.organismId = rule.getOrganismId();
        form.organismGroup = rule.getOrganismGroup();
        if (rule.getOrganismId() != null) {
            organismDAO.get(rule.getOrganismId()).ifPresent(organism -> form.organismName = organism.getDisplayName());
        }
        form.antibioticId = rule.getAntibioticId();
        antibioticDAO.get(rule.getAntibioticId()).ifPresent(antibiotic -> {
            form.antibioticName = antibiotic.getDisplayName();
            form.antibioticCode = antibiotic.getWhonetCode();
        });
        form.method = rule.getMethod();
        form.specimenTypeId = rule.getSpecimenTypeId();
        form.breakpointType = rule.getBreakpointType();
        form.susceptibleValue = rule.getSusceptibleValue();
        form.intermediateLowerValue = rule.getIntermediateLowerValue();
        form.intermediateUpperValue = rule.getIntermediateUpperValue();
        form.resistantValue = rule.getResistantValue();
        form.units = rule.getUnits();
        form.notes = rule.getNotes();
        form.active = "Y".equals(rule.getIsActive());
        form.seeded = rule.isSeeded();
        form.locallyCustomized = rule.isLocallyCustomized();
        return form;
    }

    private MicroReferenceAdminQueryForm normalizeQuery(MicroReferenceAdminQueryForm input) {
        MicroReferenceAdminQueryForm query = input == null ? new MicroReferenceAdminQueryForm() : input;
        query.q = query.q == null ? "" : query.q.trim();
        query.status = query.status == null || query.status.isBlank() ? "ALL" : query.status.toUpperCase(Locale.ROOT);
        query.status = Set.of("ALL", "ACTIVE", "LOADED", "ARCHIVED").contains(query.status) ? query.status : "ALL";
        query.page = Math.max(query.page, 1);
        query.pageSize = Set.of(20, 50, 100).contains(query.pageSize) ? query.pageSize : 20;
        query.sort = Set.of("name", "name-desc").contains(query.sort) ? query.sort : "name";
        return query;
    }

    private <T> MicroReferenceAdminPageForm<T> page(List<T> rows, long total, MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminPageForm<T> result = new MicroReferenceAdminPageForm<>();
        result.rows = new ArrayList<>(rows);
        result.total = total;
        result.page = query.page;
        result.pageSize = query.pageSize;
        return result;
    }

    private String required(String value, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
