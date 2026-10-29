package org.openelisglobal.microbiology.service;

import java.util.List;
import java.util.Objects;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroAstPanelAssignmentService {
    private final MicroAstPanelAssignmentDAO assignments;
    private final MicroIsolateDAO isolates;
    private final MicroOrganismDAO organisms;
    private final MicroAstPanelDAO panels;
    private final MicroCaseAuthorizationService authorization;

    public MicroAstPanelAssignmentService(MicroAstPanelAssignmentDAO assignments, MicroIsolateDAO isolates,
            MicroOrganismDAO organisms, MicroAstPanelDAO panels, MicroCaseAuthorizationService authorization) {
        this.assignments = assignments;
        this.isolates = isolates;
        this.organisms = organisms;
        this.panels = panels;
        this.authorization = authorization;
    }

    public List<MicroAstPanelAssignment> getAssignments(String isolateId) {
        return assignments.getByIsolateId(isolateId).stream().filter(a -> a.getRetiredAt() == null).toList();
    }

    @Transactional
    public void assignDefault(MicroIsolate isolate, String actor) {
        if (!"CONFIRMED".equals(isolate.getIdentificationStatus()) || isolate.getOrganismId() == null)
            return;
        var organism = organisms.get(isolate.getOrganismId()).orElseThrow();
        if ("NTM".equals(organism.getSusceptibilityProtocol()) || organism.getDefaultAstPanelId() == null)
            return;
        var panel = panels.get(organism.getDefaultAstPanelId()).orElse(null);
        if (panel == null || panel.getPublishedAt() == null || !"Y".equals(panel.getIsActive()))
            return;
        requireCompatible(organism, panel);
        if (getAssignments(isolate.getId()).stream().anyMatch(a -> a.getPanelFamily().equals(panel.getLogicalKey())))
            return;
        insert(isolate.getId(), panel, "ORGANISM_DEFAULT", actor);
    }

    @Transactional
    public MicroAstPanelAssignment assign(String isolateId, String panelId, String actor) {
        var isolate = isolates.get(isolateId).orElseThrow();
        authorization.requireWrite(isolate.getCaseId(), actor, Constants.ROLE_RESULTS);
        if (!"CONFIRMED".equals(isolate.getIdentificationStatus()) || isolate.getCancelledAt() != null)
            throw new MicroAstConflictException("AST_ISOLATE_IDENTIFICATION_REQUIRED");
        var organism = organisms.get(isolate.getOrganismId()).orElseThrow();
        var panel = panels.get(panelId).orElseThrow();
        requireCompatible(organism, panel);
        if (getAssignments(isolateId).stream().anyMatch(a -> a.getPanelFamily().equals(panel.getLogicalKey())))
            throw new MicroAstConflictException("AST_PANEL_ALREADY_ASSIGNED");
        return insert(isolateId, panel, "MANUAL", actor);
    }

    public static void requireCompatible(MicroOrganism organism, MicroAstPanel panel) {
        if (panel.getPublishedAt() == null || !"Y".equals(panel.getIsActive()) || !"Y".equals(panel.getIsCurrent()))
            throw new IllegalArgumentException("AST_PANEL_NOT_PUBLISHED");
        if (!Objects.equals(organism.getSusceptibilityProtocol(), panel.getSusceptibilityProtocol())
                || (panel.getOrganismGroup() != null && !panel.getOrganismGroup().isBlank()
                        && !Objects.equals(organism.getOrganismGroup(), panel.getOrganismGroup())))
            throw new IllegalArgumentException("AST_PANEL_ORGANISM_MISMATCH");
    }

    private MicroAstPanelAssignment insert(String isolateId, MicroAstPanel panel, String provenance, String actor) {
        var a = new MicroAstPanelAssignment();
        a.setIsolateId(isolateId);
        a.setPanelId(panel.getId());
        a.setPanelFamily(panel.getLogicalKey());
        a.setProvenance(provenance);
        a.setAssignedBy(actor);
        a.setAssignedAt(MicroCaseServiceImpl.now());
        assignments.insert(a);
        return a;
    }
}
