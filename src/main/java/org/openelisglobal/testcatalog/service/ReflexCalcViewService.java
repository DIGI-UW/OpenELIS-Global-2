package org.openelisglobal.testcatalog.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * OGC-949 / OGC-764 — read-only cross-link view for the Reflex &amp; Calc
 * editor section. Assembles reflex rules and calculations that touch a test
 * into a fully-materialized DTO inside a single transaction (lazy associations
 * are resolved here, never in the controller).
 */
@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
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
        /** The test_reflex row's own id. */
        public String id;
        /**
         * The Reflex Rules record that owns this row, when one does — a rule's action
         * records the test_reflex it created. Null for a legacy row configured
         * directly, which no rule can be opened for.
         */
        public Integer ruleId;
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
