package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;

public interface EQAPanelService extends BaseObjectService<EQAPanel, Long> {

    /**
     * FR-V2.4-02: a PREPARING panel and its samples in one write. Samples that
     * arrive without a blind code get one derived from the panel id, which is what
     * makes both wizard modes — split an existing pool, or define samples by hand —
     * the same call.
     *
     * <p>
     * Blind codes are filled in only for an IN_HOUSE scheme. A provider panel
     * (T-24) is shipped as physical material identified by its sample code and is
     * never blinded into local orders, so a blind code on one would assert
     * something untrue about it.
     *
     * @throws IllegalArgumentException when the panel has no scheme or no samples
     */
    EQAPanel create(EQAPanel panel, List<EQAPanelSample> samples, String sysUserId);

    /** Panels of one scheme, for the in-house landing list. */
    List<Map<String, Object>> getPanelDtosByScheme(Long schemeId);

    /**
     * Ids of the tests that carry an analyte. A panel target is stored against an
     * analyte, so a test without one cannot be blinded — the wizard filters its
     * test picker with this rather than failing at seal.
     */
    List<String> getTestableTestIds();

    /**
     * The analyte a panel target for this test is stored against. Both wizards pick
     * the orderable test rather than the analyte behind it — analyte is a catalog
     * detail no bench user thinks in — so both need this resolution, and it lives
     * here rather than once per caller. A test with several analytes takes the
     * first, which is the single-result shape every EQA analyte has today.
     *
     * @throws IllegalArgumentException when the test is unknown or carries no
     *                                  analyte
     */
    Long analyteIdForTest(String testId);

    /**
     * The same resolution as {@link #analyteIdForTest(String)}, returning null
     * instead of throwing when the test is unknown or carries no analyte.
     *
     * <p>
     * Callers that resolve an analyte only to label an export row or a FHIR
     * observation must use this one. A throw would join the caller's transaction
     * and mark it rollback-only, so catching it around the call is not enough — the
     * caller's own commit then fails with UnexpectedRollback even though it handled
     * the miss.
     */
    Long findAnalyteIdForTest(String testId);

    /**
     * PREPARING → SEALED (FR-V2.1-11). Refuses a panel with no samples, any sample
     * with a blank target (the encryption converter passes blanks through
     * unencrypted, so blanks must never reach the column), and an in-house panel
     * without an unblind date (FR-V2.1-16).
     *
     * @throws IllegalStateException    when the panel is not in PREPARING
     * @throws IllegalArgumentException when a seal precondition fails
     */
    EQAPanel seal(Long panelId, String sysUserId);

    /** SEALED → DISTRIBUTED. */
    EQAPanel distribute(Long panelId, String sysUserId);

    /** DISTRIBUTED → UNBLINDED (AC-V2.4-03's reveal point). */
    EQAPanel unblind(Long panelId, String sysUserId);

    /**
     * DISTRIBUTED → UNBLINDED taken under a row lock, recording how the panel was
     * unblinded (FR-V2.4-10). The lock is what makes the edge check a real
     * idempotency guard: without it a manual and a scheduled unblind can both read
     * DISTRIBUTED and both proceed to score.
     */
    EQAPanel unblindForUpdate(Long panelId, String sysUserId, EQAUnblindMethod method);

    /** Panels bound to a cycle, without samples. */
    List<Map<String, Object>> getPanelDtos(Long cycleId);

    Map<String, Object> toPanelDto(EQAPanel panel);

    /**
     * The panel's samples as DTOs. Sealed-target rule (FR-V2.1-16 / AC-V2.4-03):
     * target value, unit and acceptance range are null unless the panel has reached
     * UNBLINDED/SCORED/CLOSED or the caller holds the unblind permission — the
     * blinding guarantee is enforced here, in the mapping, not in the UI.
     */
    List<Map<String, Object>> getSampleDtos(Long panelId, boolean callerCanUnblind);
}
