package org.openelisglobal.eqa.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;

public interface EQAPanelService extends BaseObjectService<EQAPanel, Long> {

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
