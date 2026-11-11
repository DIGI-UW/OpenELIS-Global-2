package org.openelisglobal.testreflex.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the {@code reflex-rules} catalog domain from CSV: the rules the Reflex
 * Tests Configuration page builds, so a site's rules (CPHL keeps 29) travel
 * with its catalog instead of being re-keyed.
 * <p>
 * One flat file, one rule per {@code ruleName}, any number of rows per rule.
 * Columns:
 * {@code ruleName,overall,active,conditionTest,conditionSampleType,conditionComponent,relation,value,value2,reflexTest,reflexSampleType,internalNote,externalNote,addNotification}.
 * Each row contributes the condition its {@code conditionTest} columns describe
 * (if any) and the action its {@code reflexTest} columns describe (if any);
 * repeats within a rule collapse to one. {@code overall} is ANY (default) or
 * ALL; {@code relation} is a relation the rule builder offers (EQUALS by
 * default; BETWEEN uses {@code value2}); a specimen column may be left blank
 * when the test runs on one specimen. A rule is identified by its name: an
 * existing rule is rebuilt from the file's rows, a new one is created. Written
 * through {@link TestReflexService#saveOrUpdateReflexRule}, the same path the
 * page uses, so the checks that a condition names the test's own component and
 * a specimen the test is linked to apply here too.
 */
@Component
public class ReflexRuleConfigurationHandler extends AbstractCatalogCsvHandler {

    @Autowired
    private TestReflexService reflexService;

    @Autowired
    private TestService testService;

    @Autowired
    private CatalogReferenceResolver resolver;

    @Override
    public String getDomainName() {
        return "reflex-rules";
    }

    @Override
    public int getLoadOrder() {
        return 330;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "ruleName" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        Map<String, List<CsvRow>> byRule = new LinkedHashMap<>();
        for (CsvRow row : rows) {
            byRule.computeIfAbsent(row.get("ruleName"), k -> new ArrayList<>()).add(row);
        }
        for (List<CsvRow> group : byRule.values()) {
            LoadedRow<String> outcome;
            try {
                outcome = transaction.run(() -> loadRule(group, fileName));
            } catch (Exception e) {
                outcome = LoadedRow.skipped(CsvLoadSummary.reason(e));
            }
            for (CsvRow row : group) {
                summary.record(outcome, getClass().getSimpleName(), row.lineNumber());
            }
            flushUnresolvedReferences(fileName, group.get(0).lineNumber());
        }
    }

    private LoadedRow<String> loadRule(List<CsvRow> group, String fileName) {
        CsvRow first = group.get(0);
        String ruleName = first.get("ruleName");
        if (ruleName.isEmpty()) {
            return LoadedRow.skipped("missing ruleName");
        }
        ReflexRule rule = null;
        for (ReflexRule existing : reflexService.getAllReflexRules()) {
            if (ruleName.equalsIgnoreCase(existing.getRuleName())) {
                rule = existing;
                break;
            }
        }
        boolean created = rule == null;
        if (created) {
            rule = new ReflexRule();
            rule.setConditions(new HashSet<>());
            rule.setActions(new HashSet<>());
        }
        rule.setRuleName(ruleName);
        rule.setOverall(overall(first.get("overall"), first.lineNumber()));
        boolean active = first.flag("active", true);
        rule.setActive(active);
        rule.setToggled(active);

        Set<ReflexRuleCondition> conditions = new HashSet<>();
        Set<ReflexRuleAction> actions = new HashSet<>();
        Set<String> conditionKeys = new HashSet<>();
        Set<String> actionKeys = new HashSet<>();
        for (CsvRow row : group) {
            String context = fileName + " line " + row.lineNumber() + " (reflex rule " + ruleName + ")";
            if (!row.isBlank("conditionTest")) {
                ReflexRuleCondition condition = condition(row, context, rule);
                if (conditionKeys.add(
                        condition.getTestId() + "|" + condition.getSampleId() + "|" + condition.getComponentId() + "|"
                                + condition.getRelation() + "|" + condition.getValue() + "|" + condition.getValue2())) {
                    conditions.add(condition);
                }
            }
            if (!row.isBlank("reflexTest")) {
                ReflexRuleAction action = action(row, context, rule);
                if (actionKeys.add(action.getReflexTestId() + "|" + action.getSampleId())) {
                    actions.add(action);
                }
            }
        }
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("rule '" + ruleName + "' has no condition");
        }
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("rule '" + ruleName + "' has no reflex test");
        }
        rule.setConditions(conditions);
        rule.setActions(actions);
        reflexService.saveOrUpdateReflexRule(rule);
        String id = rule.getId() == null ? ruleName : String.valueOf(rule.getId());
        return created ? LoadedRow.created(id) : LoadedRow.updated(id);
    }

    private ReflexRuleCondition condition(CsvRow row, String context, ReflexRule rule) {
        Test test = resolver.resolveTest(row.get("conditionTest"), row.get("conditionSampleType"), context);
        if (test == null) {
            throw new IllegalArgumentException("test '" + row.get("conditionTest") + "' not found");
        }
        TypeOfSample specimen = specimenFor(test, row.get("conditionSampleType"), row, context);
        ReflexRuleCondition condition = existingCondition(rule, test.getId(), specimen.getId());
        if (condition == null) {
            condition = new ReflexRuleCondition();
        }
        condition.setTestId(test.getId());
        condition.setTestName(test.getLocalizedName());
        condition.setSampleId(specimen.getId());
        condition.setRelation(relation(row.get("relation"), row.lineNumber()));
        condition.setValue(row.get("value"));
        condition.setValue2(row.isBlank("value2") ? null : row.get("value2"));
        if (ReflexRuleOptions.NumericRelationOptions.BETWEEN.equals(condition.getRelation())
                && condition.getValue2() == null) {
            throw new IllegalArgumentException("a BETWEEN condition needs value2");
        }
        if (!row.isBlank("conditionComponent")) {
            TestResultComponent component = resolver.resolveComponent(test.getId(), row.get("conditionComponent"),
                    context);
            if (component == null) {
                throw new IllegalArgumentException(
                        "component '" + row.get("conditionComponent") + "' is not on '" + test.getDescription() + "'");
            }
            condition.setComponentId(component.getId());
        } else {
            condition.setComponentId(null);
        }
        return condition;
    }

    private ReflexRuleAction action(CsvRow row, String context, ReflexRule rule) {
        Test test = resolver.resolveTest(row.get("reflexTest"), row.get("reflexSampleType"), context);
        if (test == null) {
            throw new IllegalArgumentException("reflex test '" + row.get("reflexTest") + "' not found");
        }
        TypeOfSample specimen = specimenFor(test, row.get("reflexSampleType"), row, context);
        ReflexRuleAction action = existingAction(rule, test.getId(), specimen.getId());
        if (action == null) {
            action = new ReflexRuleAction();
        }
        action.setReflexTestId(test.getId());
        action.setReflexTestName(test.getLocalizedName());
        action.setSampleId(specimen.getId());
        action.setInternalNote(row.isBlank("internalNote") ? null : row.get("internalNote"));
        action.setExternalNote(row.isBlank("externalNote") ? null : row.get("externalNote"));
        action.setAddNotification(row.flag("addNotification", true) ? "Y" : "N");
        return action;
    }

    /**
     * The specimen a condition or action runs on: the named one, which must be
     * linked to the test, or the test's only specimen when the column is blank.
     */
    private TypeOfSample specimenFor(Test test, String sampleTypeName, CsvRow row, String context) {
        List<TypeOfSample> linked = testService.getTypeOfSamples(test);
        if (sampleTypeName.isEmpty()) {
            if (linked.size() == 1) {
                return linked.get(0);
            }
            throw new IllegalArgumentException(
                    "'" + test.getDescription() + "' runs on " + linked.size() + " specimens, name one");
        }
        TypeOfSample specimen = resolver.resolveSampleType(sampleTypeName, context);
        if (specimen == null) {
            throw new IllegalArgumentException("sample type '" + sampleTypeName + "' not found");
        }
        for (TypeOfSample type : linked) {
            if (type.getId().equals(specimen.getId())) {
                return specimen;
            }
        }
        throw new IllegalArgumentException(
                "'" + test.getDescription() + "' is not linked to sample type '" + sampleTypeName + "'");
    }

    private static ReflexRuleCondition existingCondition(ReflexRule rule, String testId, String sampleId) {
        if (rule.getConditions() == null) {
            return null;
        }
        for (ReflexRuleCondition condition : rule.getConditions()) {
            if (testId.equals(condition.getTestId()) && sampleId.equals(condition.getSampleId())) {
                return condition;
            }
        }
        return null;
    }

    private static ReflexRuleAction existingAction(ReflexRule rule, String testId, String sampleId) {
        if (rule.getActions() == null) {
            return null;
        }
        for (ReflexRuleAction action : rule.getActions()) {
            if (testId.equals(action.getReflexTestId()) && sampleId.equals(action.getSampleId())) {
                return action;
            }
        }
        return null;
    }

    private static ReflexRuleOptions.OverallOptions overall(String value, int lineNumber) {
        if (value.isEmpty()) {
            return ReflexRuleOptions.OverallOptions.ANY;
        }
        try {
            return ReflexRuleOptions.OverallOptions.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("overall must be ANY or ALL");
        }
    }

    private static ReflexRuleOptions.NumericRelationOptions relation(String value, int lineNumber) {
        if (value.isEmpty()) {
            return ReflexRuleOptions.NumericRelationOptions.EQUALS;
        }
        try {
            return ReflexRuleOptions.NumericRelationOptions.valueOf(value.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown relation '" + value + "'");
        }
    }
}
