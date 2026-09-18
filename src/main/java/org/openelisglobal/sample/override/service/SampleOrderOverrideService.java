package org.openelisglobal.sample.override.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sample.override.valueholder.OverrideReasonCode;
import org.openelisglobal.sample.override.valueholder.OverrideType;
import org.openelisglobal.sample.override.valueholder.SampleOrderOverride;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleOrderOverrideService extends BaseObjectService<SampleOrderOverride, Long> {

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleOrderOverride> findBySampleId(Long sampleId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    Optional<SampleOrderOverride> findBySampleIdAndType(Long sampleId, OverrideType overrideType);

    /**
     * Records the decision, replacing any earlier decision of the same kind on the
     * same order so that "was this order overridden?" has one answer.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_EDIT')")
    SampleOrderOverride record(Long sampleId, OverrideType overrideType, OverrideReasonCode reasonCode, String reason,
            Long userId);

    /** Withdraws a decision, used when a user unticks the control that set it. */
    @PreAuthorize("hasAuthority('PRIV_ORDER_EDIT')")
    void clear(Long sampleId, OverrideType overrideType);
}
