package org.openelisglobal.testcatalog.service;

import java.util.ArrayList;
import java.util.List;

/**
 * OGC-949 / OGC-764 — read-only cross-link view for the Reflex &amp; Calc
 * editor section. Assembles reflex rules and calculations that touch a test
 * into a fully-materialized DTO inside a single transaction (lazy associations
 * are resolved here, never in the controller).
 */
public interface ReflexCalcViewService {

    ReflexCalcView getForTest(String testId);

    /** Reflex rules + the two calculation directions for a test. */
    class ReflexCalcView {
        public List<ReflexRow> reflexRules = new ArrayList<>();
        public List<CalcRow> calculatedBy = new ArrayList<>();
        public List<CalcRow> feedsInto = new ArrayList<>();
    }

    /** A reflex rule whose trigger (source) test is the viewed test. */
    class ReflexRow {
        public String id;
        public String ruleName;
        public String triggerCondition;
        public String reflexTests;
    }

    /** A calculation that produces or consumes the viewed test. */
    class CalcRow {
        public Integer id;
        public String name;
        public String formula;
        public String outputTest;
    }
}
