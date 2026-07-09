package org.openelisglobal.qa.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated payload for the QA Overview page (OGC-694 WS-F). One fetch feeds
 * the This-Week counters, QC/EQA pillar chips, inspector answers, and the
 * server half of the Recent Activity feed. NCE-derived numbers are computed
 * client-side from the existing NCE dashboard payload (see nceOverview.js) and
 * are deliberately absent here.
 */
public class QaOverviewSummary {

    public QcStatus qc = new QcStatus();
    public EqaStatus eqa = new EqaStatus();
    public WeekCounters week = new WeekCounters();
    public List<ActivityItem> activity = new ArrayList<>();

    /** Instrument-compliance rollup over the QC dashboard's 30-day window. */
    public static class QcStatus {
        public int compliantInstruments;
        public int warningInstruments;
        public int nonCompliantInstruments;
        public int totalInstruments;
        public long violations24h;
        public long violationsThisWeek;
        /** Rule code -> count for this week's violations, largest first. */
        public Map<String, Long> weekRuleBreakdown = new LinkedHashMap<>();
    }

    /** Open EQA orders; "completed" does not exist in the current EQA model. */
    public static class EqaStatus {
        public long open;
        public long overdue;
        public long dueSoon14d;
    }

    /**
     * Counters for the current week (Monday 00:00 in the server zone to now). The
     * boundary is exported so the client computes its NCE counters against the same
     * window instead of a browser-local Monday.
     */
    public static class WeekCounters {
        /** ISO date of the server-zone Monday, for report_date comparisons. */
        public String weekStart;
        /** Exact boundary as an ISO-8601 instant, for timestamp comparisons. */
        public String weekStartInstant;
        public long auditEntries;
        public long signatureEvents;
    }

    /** One Recent-Activity row (last 24h): an e-signature or a QC alert. */
    public static class ActivityItem {
        public static final String TYPE_ESIG = "ESIG";
        public static final String TYPE_QC_VIOLATION = "QC_VIOLATION";

        public String type;
        /** ISO-8601 instant. */
        public String timestamp;
        /** Signer printed name (ESIG only). */
        public String actor;
        /** SignatureMeaning name (ESIG only). */
        public String meaning;
        public String recordType;
        public Long recordId;
        /** Westgard rule code (QC_VIOLATION only). */
        public String ruleCode;
        public String severity;
        public String instrumentName;
    }
}
