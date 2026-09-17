package org.openelisglobal.result.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;

/**
 * OGC-1022 (R3, FR-H1/H2) — this analysis's own event timeline for the unified
 * Results page: creation, status transitions, result value changes, bound
 * notes, retest revisions and reflex children. Explicitly NOT
 * patient-longitudinal history and NOT Westgard/QC statistics (D7).
 */
public interface AnalysisTimelineService {

    /** Every event for this analysis, newest first. */
    List<AnalysisTimelineEvent> getTimeline(Analysis analysis);

    class AnalysisTimelineEvent {
        /** CREATED | STATUS | RESULT | NOTE | RETEST | REFLEX | REFERRAL | NCE */
        private String type;
        private long timestamp;
        private String when;
        private String detail;
        private String by;
        /**
         * OGC-811 — the result component this event is attributable to (from the
         * result's test_result.component_id or the note's component scope). Null means
         * analysis-level: creation, status, referral, NCE, retests and any legacy
         * record with no component association.
         */
        private String componentId;

        public AnalysisTimelineEvent() {
        }

        public AnalysisTimelineEvent(String type, long timestamp, String when, String detail, String by) {
            this.type = type;
            this.timestamp = timestamp;
            this.when = when;
            this.detail = detail;
            this.by = by;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getWhen() {
            return when;
        }

        public void setWhen(String when) {
            this.when = when;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getBy() {
            return by;
        }

        public void setBy(String by) {
            this.by = by;
        }

        public String getComponentId() {
            return componentId;
        }

        public void setComponentId(String componentId) {
            this.componentId = componentId;
        }
    }
}
