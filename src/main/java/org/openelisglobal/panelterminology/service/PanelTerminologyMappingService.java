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

    /**
     * Reconcile the single legacy {@code panel.loinc} value into the terminology
     * mappings, so a code entered on the legacy panel pages shows up in the new
     * Panel Editor as LOINC / SAME_AS.
     *
     * <p>
     * This is the return leg of {@code saveMappingsForPanel}, which already pushes
     * the editor's primary SAME_AS code out to {@code panel.loinc}. Without it the
     * two stores only agreed in one direction: a legacy edit was invisible to the
     * editor, and saving there would then write the editor's empty set back over
     * the legacy column.
     *
     * <p>
     * A non-blank code upserts its LOINC mapping — reactivating rather than
     * re-inserting, since {@code (panel_id, source, code)} is unique — and
     * soft-deletes any other active LOINC mapping carrying a different code. A
     * blank code soft-deletes every active LOINC mapping. Mappings from other
     * systems are never touched.
     */
    void syncLegacyLoinc(String panelId, String loinc, String sysUserId);
}
