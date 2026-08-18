package org.openelisglobal.panelterminology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping;

public interface PanelTerminologyMappingService extends BaseObjectService<PanelTerminologyMapping, String> {

    /** Active terminology mappings for a panel. */
    List<PanelTerminologyMapping> getActiveByPanelId(String panelId);

    /**
     * Reconcile a panel's terminology mappings to exactly the desired set, in one
     * transaction. Identity is the natural key {@code (source, code)} (which the DB
     * also enforces unique per panel): a desired mapping whose
     * {@code (source, code)} already exists is updated/reactivated rather than
     * re-inserted — so re-adding a previously-removed code never collides with the
     * unique constraint. Existing active mappings absent from {@code desired} are
     * soft-deleted ({@code is_active = 'N'}).
     *
     * <p>
     * The primary LOINC stays denormalized on {@code panel.loinc} (FHIR intake
     * routes e-orders by it): after the reconcile, {@code panel.loinc} is set to
     * the first active SAME_AS LOINC mapping's code, or cleared when none remains.
     */
    void saveMappingsForPanel(String panelId, List<PanelTerminologyMapping> desired, String sysUserId);
}
