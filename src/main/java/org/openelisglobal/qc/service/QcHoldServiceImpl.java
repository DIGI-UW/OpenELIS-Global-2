package org.openelisglobal.qc.service;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1147 FR-C1/C3 — which results are held because a control covering them
 * failed.
 *
 * <p>
 * Its own service because two call sites need the same answer and must not
 * disagree: Validation annotates its rows with it on load, and the save path
 * re-checks it before releasing anything. Keeping the decision in one place is
 * what stops the display and the enforcement from drifting apart.
 */
@Service
public class QcHoldServiceImpl implements QcHoldService {

    @Autowired
    private NceSpecimenService nceSpecimenService;

    @Override
    @Transactional(readOnly = true)
    public Set<String> heldAnalysisIds(Collection<String> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return Set.of();
        }
        // The DAO already short-circuits an empty collection, so no second guard here.
        Set<Integer> numeric = analysisIds.stream().map(QcHoldServiceImpl::toInteger).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return nceSpecimenService.findAnalysisIdsWithOpenQcHold(numeric).stream().map(String::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * The blocking decision, fail-closed. If the hold cannot be resolved while
     * blocking is switched on, every candidate is treated as held: withholding
     * release is recoverable, releasing a result whose QC state is unknown is not.
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> analysisIdsBlockedFromRelease(Collection<String> analysisIds) {
        if (!blocksRelease() || analysisIds == null || analysisIds.isEmpty()) {
            return Set.of();
        }
        try {
            return heldAnalysisIds(analysisIds);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getName(), "analysisIdsBlockedFromRelease",
                    "Could not resolve QC holds; withholding release for this batch: " + e.getMessage());
            return analysisIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        }
    }

    @Override
    public boolean blocksRelease() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.QC_FAIL_BLOCKS_VALIDATION, "true");
    }

    private static Integer toInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
