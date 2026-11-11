package org.openelisglobal.notification.service;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Maps a {@link Sample} to its workflow type ({@code environmental},
 * {@code vector}, or {@code clinical}) for the Sent Messages tab's Reference
 * link routing. The workflow drives the frontend route — e.g.,
 * {@code /order/environmental/enter?labNumber=X} vs
 * {@code /order/clinical/enter?labNumber=X}.
 *
 * <p>
 * The value is stored on each sample as an {@code ENV_WORKFLOW_TYPE}
 * observation history entry. Legacy pre-split samples store no value; those
 * default to {@code clinical} (matches {@code OrderSearchRestController}'s
 * convention).
 */
@Component
public class SampleWorkflowResolver {

    @Autowired
    private ObservationHistoryService observationHistoryService;

    public String resolveWorkflow(Sample sample) {
        if (sample == null || sample.getId() == null) {
            return null;
        }
        try {
            String raw = observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE,
                    sample.getId());
            if (GenericValidator.isBlankOrNull(raw)) {
                return "clinical";
            }
            String normalized = raw.toLowerCase();
            if ("environmental".equals(normalized) || "vector".equals(normalized) || "clinical".equals(normalized)) {
                return normalized;
            }
            return "clinical";
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveWorkflow",
                    "could not resolve workflow for sample " + sample.getId() + "; defaulting to clinical");
            return "clinical";
        }
    }
}
