package org.openelisglobal.testreflex.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions.NumericRelationOptions;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions.OverallOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads reflex rules from CSV files in
 * {@code volume/configuration/backend/reflex-rules/}.
 *
 * <p>
 * One row per rule. Conditions and actions reference tests by name (looked up
 * via {@link TestService}). Pipe (|) separates multiple condition tests and
 * multiple action tests. Relation applies to all conditions; internalNote /
 * externalNote applied to all actions.
 *
 * <p>
 * CSV format:
 * 
 * <pre>
 * ruleName,overall,isActive,conditionTests,relation,value,actionTests,internalNote,externalNote
 * VR-01 Malaria Speciation,ANY,Y,Pan-Plasmodium CSP ELISA|Pan-Plasmodium 18S rRNA PCR,OUTSIDE_NORMAL_RANGE,,P. falciparum species-specific PCR|P. vivax species-specific PCR,System Seed Rule — VR-01,
 * </pre>
 *
 * <p>
 * Re-runs are idempotent: rules with a matching {@code rule_name} have their
 * conditions and actions replaced wholesale. Tests not found in the catalog are
 * skipped with a warning so a partial-catalog deployment doesn't strand the
 * rule entirely.
 */
@Component
public class ReflexRuleConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private TestReflexService testReflexService;

    @Autowired
    private TestService testService;

    @Override
    public String getDomainName() {
        return "reflex-rules";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        // Tests load at 200, panels at 300. Reflex rules reference tests by name,
        // so they load after both.
        return 400;
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Reflex rule file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        int ruleNameIdx = findColumnIndex(headers, "ruleName");
        int overallIdx = findColumnIndex(headers, "overall");
        int isActiveIdx = findColumnIndex(headers, "isActive");
        int conditionTestsIdx = findColumnIndex(headers, "conditionTests");
        int relationIdx = findColumnIndex(headers, "relation");
        int valueIdx = findColumnIndex(headers, "value");
        int actionTestsIdx = findColumnIndex(headers, "actionTests");
        int internalNoteIdx = findColumnIndex(headers, "internalNote");
        int externalNoteIdx = findColumnIndex(headers, "externalNote");

        if (ruleNameIdx < 0 || conditionTestsIdx < 0 || actionTestsIdx < 0) {
            throw new IllegalArgumentException(
                    "Reflex rule file " + fileName + " must have ruleName, conditionTests, actionTests columns");
        }

        int processed = 0;
        int skipped = 0;
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            try {
                String[] values = parseCsvLine(line);
                boolean ok = processRow(values, ruleNameIdx, overallIdx, isActiveIdx, conditionTestsIdx, relationIdx,
                        valueIdx, actionTestsIdx, internalNoteIdx, externalNoteIdx, lineNumber, fileName);
                if (ok) {
                    processed++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error on line " + lineNumber + " in " + fileName + ": " + e.getMessage());
                skipped++;
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Loaded " + processed + " reflex rules from " + fileName + " (" + skipped + " skipped)");
    }

    private boolean processRow(String[] values, int ruleNameIdx, int overallIdx, int isActiveIdx, int conditionTestsIdx,
            int relationIdx, int valueIdx, int actionTestsIdx, int internalNoteIdx, int externalNoteIdx, int lineNumber,
            String fileName) {
        String ruleName = getValueOrEmpty(values, ruleNameIdx);
        if (ruleName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "Skipping line " + lineNumber + " in " + fileName + ": missing ruleName");
            return false;
        }

        String conditionTestsValue = getValueOrEmpty(values, conditionTestsIdx);
        String actionTestsValue = getValueOrEmpty(values, actionTestsIdx);
        if (conditionTestsValue.isEmpty() || actionTestsValue.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "Skipping rule " + ruleName + ": missing conditionTests or actionTests");
            return false;
        }

        Set<ReflexRuleCondition> conditions = buildConditions(conditionTestsValue, getValueOrEmpty(values, relationIdx),
                getValueOrEmpty(values, valueIdx));
        Set<ReflexRuleAction> actions = buildActions(actionTestsValue, getValueOrEmpty(values, internalNoteIdx),
                getValueOrEmpty(values, externalNoteIdx));

        if (conditions.isEmpty() || actions.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "Skipping rule " + ruleName + ": no resolvable tests for conditions or actions");
            return false;
        }

        ReflexRule rule = findExistingRule(ruleName);
        if (rule == null) {
            rule = new ReflexRule();
            rule.setRuleName(ruleName);
        }
        rule.setOverall(parseOverall(getValueOrEmpty(values, overallIdx)));
        rule.setActive(parseBoolean(getValueOrEmpty(values, isActiveIdx), true));
        rule.setToggled(true);
        rule.setConditions(conditions);
        rule.setActions(actions);

        testReflexService.saveOrUpdateReflexRule(rule);
        return true;
    }

    private ReflexRule findExistingRule(String ruleName) {
        try {
            List<ReflexRule> all = testReflexService.getAllReflexRules();
            for (ReflexRule r : all) {
                if (ruleName.equals(r.getRuleName())) {
                    return r;
                }
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findExistingRule", e.getMessage());
        }
        return null;
    }

    private Set<ReflexRuleCondition> buildConditions(String conditionTestsValue, String relationValue,
            String valueValue) {
        NumericRelationOptions relation = parseRelation(relationValue);
        Set<ReflexRuleCondition> conditions = new HashSet<>();
        for (String testName : conditionTestsValue.split("\\|")) {
            testName = testName.trim();
            if (testName.isEmpty()) {
                continue;
            }
            Test test = findTestByName(testName);
            if (test == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "buildConditions",
                        "Skipping condition test (not found): " + testName);
                continue;
            }
            ReflexRuleCondition condition = new ReflexRuleCondition();
            condition.setTestName(test.getName());
            condition.setTestId(test.getId());
            condition.setRelation(relation);
            if (!valueValue.isEmpty()) {
                condition.setValue(valueValue);
            }
            conditions.add(condition);
        }
        return conditions;
    }

    private Set<ReflexRuleAction> buildActions(String actionTestsValue, String internalNote, String externalNote) {
        Set<ReflexRuleAction> actions = new HashSet<>();
        for (String testName : actionTestsValue.split("\\|")) {
            testName = testName.trim();
            if (testName.isEmpty()) {
                continue;
            }
            Test test = findTestByName(testName);
            if (test == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "buildActions",
                        "Skipping action test (not found): " + testName);
                continue;
            }
            ReflexRuleAction action = new ReflexRuleAction();
            action.setReflexTestName(test.getName());
            action.setReflexTestId(test.getId());
            if (!internalNote.isEmpty()) {
                action.setInternalNote(internalNote);
            }
            if (!externalNote.isEmpty()) {
                action.setExternalNote(externalNote);
            }
            actions.add(action);
        }
        return actions;
    }

    private Test findTestByName(String testName) {
        Test t = testService.getTestByName(testName);
        if (t != null) {
            return t;
        }
        List<Test> active = testService.getActiveTestByName(testName);
        return (active != null && !active.isEmpty()) ? active.get(0) : null;
    }

    private OverallOptions parseOverall(String value) {
        if (value == null || value.isBlank()) {
            return OverallOptions.ANY;
        }
        try {
            return OverallOptions.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OverallOptions.ANY;
        }
    }

    private NumericRelationOptions parseRelation(String value) {
        if (value == null || value.isBlank()) {
            return NumericRelationOptions.OUTSIDE_NORMAL_RANGE;
        }
        try {
            return NumericRelationOptions.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NumericRelationOptions.OUTSIDE_NORMAL_RANGE;
        }
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String v = value.trim().toLowerCase();
        return v.equals("y") || v.equals("yes") || v.equals("true") || v.equals("1");
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index < 0 || index >= values.length) {
            return "";
        }
        return values[index] != null ? values[index] : "";
    }

    private int findColumnIndex(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // RFC 4180: "" inside a quoted field → literal " (don't close the field)
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }
}
