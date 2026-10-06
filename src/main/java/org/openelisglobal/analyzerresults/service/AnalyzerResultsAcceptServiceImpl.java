package org.openelisglobal.analyzerresults.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.analyzerresults.valueholder.SampleGrouping;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultEntryAlert;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.service.ResultEntryAcknowledgementService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultEntryAcknowledgement;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.test.service.EffectiveTestStatusService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

@Service
public class AnalyzerResultsAcceptServiceImpl implements AnalyzerResultsAcceptService {

    private static final String REJECT_VALUE = "XXXX";
    private static final String RESULT_SUBJECT = "Analyzer Result Note";

    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private AnalyzerResultPlacementService placementService;
    @Autowired
    private ResultEntryAcknowledgementService acknowledgementService;
    @Autowired
    private EffectiveTestStatusService effectiveTestStatusService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private SampleQaEventService sampleQaEventService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ResultLimitService resultLimitService;

    // ---------------------------------------------------------------
    // Public entry point
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void acceptAndPersist(List<AnalyzerResultItem> allResults, String sysUserId) {
        acceptAndPersist(allResults, sysUserId, List.of());
    }

    @Override
    @Transactional
    public void acceptAndPersist(List<AnalyzerResultItem> allResults, String sysUserId, List<ResultEntryAlert> alerts) {
        List<AnalyzerResultItem> actionableResults = extractActionableResult(allResults);
        retainResolvableResults(actionableResults);
        applyRedirects(actionableResults, sysUserId);
        keepGroupingsOnOneOrder(actionableResults);

        if (actionableResults.isEmpty()) {
            return;
        }

        // A result whose specimen ID matches several analyses has no placement
        // until the reviewer chooses one; its row stays staged, the rest proceed.
        holdRowsAwaitingPlacement(actionableResults, sysUserId);
        if (actionableResults.isEmpty()) {
            return;
        }

        // OGC-1145 FR-8 — never first-match a specimen: any accepted grouping
        // whose test is specimen-ambiguous and carries no reviewer choice stays
        // staged, flagged awaiting_specimen, so the review page shows its
        // chooser; everything else in the batch proceeds normally.
        holdGroupsAwaitingSpecimen(actionableResults, sysUserId);
        if (actionableResults.isEmpty()) {
            return;
        }

        // Remove actionable items from the remaining list so we can detect
        // childless controls among the leftovers.
        List<AnalyzerResultItem> remaining = new ArrayList<>(allResults);
        remaining.removeAll(actionableResults);

        List<AnalyzerResultItem> childlessControls = extractChildlessControls(remaining);
        retainResolvableResults(childlessControls);
        List<AnalyzerResults> deletableAnalyzerResults = getRemovableAnalyzerResults(actionableResults,
                childlessControls);

        List<SampleGrouping> sampleGroupList = new ArrayList<>();
        buildSampleGroupings(actionableResults, sampleGroupList, sysUserId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "acceptAndPersist",
                "Accept: " + actionableResults.size() + " actionable, " + sampleGroupList.size() + " sample groupings, "
                        + deletableAnalyzerResults.size() + " to delete from staging");

        // A deliberately skipped observation keeps its staging row for later review.
        Set<String> skippedResultIds = sampleGroupList.stream().flatMap(group -> group.skippedResultIds.stream())
                .collect(Collectors.toSet());
        deletableAnalyzerResults.removeIf(staged -> skippedResultIds.contains(staged.getId()));
        // A grouping whose every row was skipped has nothing to save.
        sampleGroupList.removeIf(grouping -> grouping.resultList.isEmpty());
        long expectedResults = actionableResults.stream()
                .filter(item -> !item.getIsDeleted() && !skippedResultIds.contains(item.getId())).count();
        long builtResults = sampleGroupList.stream().mapToLong(group -> group.resultList.size()).sum();
        if (builtResults != expectedResults) {
            throw new IllegalStateException(
                    "Analyzer review would remove " + expectedResults + " observations but persist " + builtResults);
        }

        analyzerResultsService.persistAnalyzerResults(deletableAnalyzerResults, sampleGroupList, sysUserId);
        recordAcknowledgements(alerts, sampleGroupList, sysUserId);
    }

    /** Each acknowledged value names the analysis and result it was saved as. */
    private void recordAcknowledgements(List<ResultEntryAlert> alerts, List<SampleGrouping> sampleGroupList,
            String sysUserId) {
        if (alerts.isEmpty()) {
            return;
        }
        for (ResultEntryAlert alert : alerts) {
            sampleGroupList.stream()
                    .filter(group -> group.sample != null
                            && Objects.equals(group.sample.getAccessionNumber(), alert.getAccessionNumber()))
                    .flatMap(group -> group.resultList.stream()).filter(result -> savedFor(result, alert)).findFirst()
                    .ifPresent(result -> {
                        alert.setResult(result);
                        alert.setAnalysisId(result.getAnalysis().getId());
                    });
        }
        acknowledgementService.recordAcknowledgements(alerts, ResultEntryAcknowledgement.SOURCE_ANALYZER_REVIEW,
                sysUserId);
    }

    private static boolean savedFor(Result result, ResultEntryAlert alert) {
        if (result == null || result.getAnalysis() == null || result.getAnalysis().getTest() == null
                || !Objects.equals(result.getAnalysis().getTest().getId(), alert.getTestId())) {
            return false;
        }
        return alert.getComponentId() == null || (result.getTestResult() != null
                && Objects.equals(alert.getComponentId(), result.getTestResult().getComponentId()));
    }

    /**
     * Never use client-supplied review flags to remove a server-held observation.
     */
    private void retainResolvableResults(List<AnalyzerResultItem> actionableResults) {
        actionableResults.removeIf(item -> {
            AnalyzerResults staged = analyzerResultsService.get(item.getId());
            if (staged == null) {
                throw new IllegalStateException("Analyzer result is no longer staged: " + item.getId());
            }
            if (AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN.equals(staged.getImportIssueReason())
                    && item.getIsAccepted() && staged.getTestId() != null
                    && Objects.equals(staged.getTestId(), item.getTestId())) {
                restoreStagedIdentity(item, staged);
                if (needsSpecimenChoice(item)) {
                    return true;
                }
                item.setReadOnly(false);
                return false;
            }
            if (AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT.equals(staged.getImportIssueReason())
                    && (item.getIsAccepted() || item.getIsRejected()) && staged.getTestId() != null
                    && Objects.equals(staged.getTestId(), item.getTestId())) {
                restoreStagedIdentity(item, staged);
                item.setReadOnly(false);
                return false;
            }
            if (staged.isReadOnly() || !GenericValidator.isBlankOrNull(staged.getImportIssueReason())
                    || GenericValidator.isBlankOrNull(staged.getTestId())) {
                return true;
            }
            restoreStagedIdentity(item, staged);
            return false;
        });
    }

    /**
     * Everything the review page derives from the staged row is the staged row's:
     * order, test, component, control flag, analyzer, completion time, result type
     * and precision. It is an analyzer result, never a manual analysis. The
     * reviewer supplies only the action, specimen choice, note, result and reflex
     * selection.
     */
    private void restoreStagedIdentity(AnalyzerResultItem item, AnalyzerResults staged) {
        item.setAccessionNumber(staged.getAccessionNumber());
        item.setInstrumentSpecimenId(staged.getInstrumentSpecimenId());
        item.setInstrumentPatientId(staged.getInstrumentPatientId());
        item.setInstrumentPatientName(staged.getInstrumentPatientName());
        item.setTestId(staged.getTestId());
        item.setComponentId(staged.getComponentId());
        item.setIsControl(staged.getIsControl());
        item.setAnalyzerId(staged.getAnalyzerId());
        item.setCompleteDate(staged.getCompleteDateForDisplay());
        item.setTestResultType(staged.getResultType());
        item.setTestName(staged.getTestName());
        item.setSignificantDigits(significantDigitsFor(staged));
        item.setManual(false);
    }

    /** As the review page derives it: the test's first active result definition. */
    private String significantDigitsFor(AnalyzerResults staged) {
        if (staged.getTestId() == null || GenericValidator.isBlankOrNull(staged.getResult())) {
            return null;
        }
        List<TestResult> testResults = testResultService.getActiveTestResultsByTest(staged.getTestId());
        return testResults == null || testResults.isEmpty() ? null : testResults.get(0).getSignificantDigits();
    }

    /**
     * A sample grouping is one order's results; a row whose staged order differs
     * from the rest of its grouping stays staged rather than joining that order.
     */
    private void keepGroupingsOnOneOrder(List<AnalyzerResultItem> actionableResults) {
        Map<Integer, String> orderByGrouping = new HashMap<>();
        actionableResults.removeIf(item -> {
            String order = orderByGrouping.computeIfAbsent(item.getSampleGroupingNumber(),
                    grouping -> item.getAccessionNumber());
            if (Objects.equals(order, item.getAccessionNumber())) {
                return false;
            }
            LogEvent.logWarn(this.getClass().getSimpleName(), "keepGroupingsOnOneOrder",
                    "Analyzer result " + item.getId() + " belongs to order " + item.getAccessionNumber()
                            + ", not its grouping's order " + order + "; it stays staged.");
            return true;
        });
    }

    // ---------------------------------------------------------------
    // Placement hold
    // ---------------------------------------------------------------

    /**
     * Removes each patient result whose analysis cannot be told without a choice:
     * several analyses match its specimen ID and the reviewer chose none of them,
     * or the instrument's patient is not the order's patient and the reviewer gave
     * no note saying why. The staged row keeps its place in review, flagged
     * {@code awaiting_placement}.
     */
    private void holdRowsAwaitingPlacement(List<AnalyzerResultItem> actionableResults, String sysUserId) {
        List<AnalyzerResultItem> held = actionableResults.stream()
                .filter(item -> !item.getIsControl() && !GenericValidator.isBlankOrNull(item.getTestId()))
                .filter(item -> awaitsPlacement(item)).toList();
        holdForPlacement(actionableResults, held, sysUserId);
    }

    private void holdForPlacement(List<AnalyzerResultItem> actionableResults, List<AnalyzerResultItem> held,
            String sysUserId) {
        actionableResults.removeAll(held);
        for (AnalyzerResultItem item : held) {
            AnalyzerResults stagedRow = analyzerResultsService.get(item.getId());
            if (stagedRow != null) {
                stagedRow.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT);
                stagedRow.setSysUserId(sysUserId);
                analyzerResultsService.update(stagedRow);
            }
            LogEvent.logWarn(this.getClass().getSimpleName(), "holdForPlacement",
                    "holding specimen " + specimenIdOf(item) + " test " + item.getTestId()
                            + " awaiting placement: its analysis is not determined");
        }
    }

    /**
     * The reviewer placed a grouping's results on another existing order, the way
     * to correct a specimen ID the instrument misread. It takes a reason, which is
     * recorded on each analysis, and an order that exists; without either, the
     * grouping's rows stay staged instead of guessing.
     */
    private void applyRedirects(List<AnalyzerResultItem> actionableResults, String sysUserId) {
        Map<Integer, AnalyzerResultItem> requests = new HashMap<>();
        for (AnalyzerResultItem item : actionableResults) {
            if (!item.getIsControl() && !GenericValidator.isBlankOrNull(item.getRedirectAccession())) {
                requests.putIfAbsent(item.getSampleGroupingNumber(), item);
            }
        }
        if (requests.isEmpty()) {
            return;
        }
        List<AnalyzerResultItem> held = new ArrayList<>();
        for (AnalyzerResultItem item : actionableResults) {
            AnalyzerResultItem request = requests.get(item.getSampleGroupingNumber());
            if (request == null || item.getIsControl()) {
                continue;
            }
            String target = request.getRedirectAccession().trim();
            if (GenericValidator.isBlankOrNull(request.getRedirectReason())
                    || sampleService.getSampleByAccessionNumber(target) == null) {
                held.add(item);
                continue;
            }
            String reported = specimenIdOf(item);
            String note = "Instrument reported specimen ID " + reported + "; the reviewer placed it on order " + target
                    + ": " + request.getRedirectReason().trim();
            item.setNote(GenericValidator.isBlankOrNull(item.getNote()) ? note : item.getNote() + " " + note);
            item.setAccessionNumber(target);
            item.setInstrumentSpecimenId(target);
        }
        holdForPlacement(actionableResults, held, sysUserId);
    }

    private boolean awaitsPlacement(AnalyzerResultItem item) {
        AnalyzerResultPlacement placed = placementOf(item);
        boolean unchosen = placed.state() == AnalyzerResultPlacement.State.MULTI_TUBE
                && !isCandidate(placed, item.getChosenAnalysisId());
        boolean unexplainedPatient = placed.patient().status() == AnalyzerResultPlacement.PatientStatus.MISMATCH
                && GenericValidator.isBlankOrNull(item.getNote());
        return unchosen || unexplainedPatient;
    }

    private static boolean isCandidate(AnalyzerResultPlacement placed, String analysisId) {
        return !GenericValidator.isBlankOrNull(analysisId)
                && placed.analyses().stream().anyMatch(candidate -> candidate.analysisId().equals(analysisId));
    }

    private AnalyzerResultPlacement placementOf(AnalyzerResultItem item) {
        return placementService.place(specimenIdOf(item), item.getTestId(), item.getInstrumentPatientId(),
                item.getInstrumentPatientName());
    }

    private static String specimenIdOf(AnalyzerResultItem item) {
        return GenericValidator.isBlankOrNull(item.getInstrumentSpecimenId()) ? item.getAccessionNumber()
                : item.getInstrumentSpecimenId();
    }

    // ---------------------------------------------------------------
    // OGC-1145 FR-8 — awaiting-specimen hold
    // ---------------------------------------------------------------

    /**
     * Removes every grouping that contains an accepted, specimen-ambiguous row
     * without a reviewer-chosen sample type, and flags its staged rows
     * {@code awaiting_specimen} so they stay in review with the chooser.
     */
    private void holdGroupsAwaitingSpecimen(List<AnalyzerResultItem> actionableResults, String sysUserId) {
        Set<Integer> heldGroups = new HashSet<>();
        Map<Integer, List<AnalyzerResultItem>> groupings = new HashMap<>();
        for (AnalyzerResultItem item : actionableResults) {
            if (item.getIsAccepted() && needsSpecimenChoice(item)) {
                heldGroups.add(item.getSampleGroupingNumber());
            }
            groupings.computeIfAbsent(item.getSampleGroupingNumber(), grouping -> new ArrayList<>()).add(item);
        }
        groupings.forEach((grouping, items) -> {
            if (!heldGroups.contains(grouping) && hasUnusableSpecimenChoice(items)) {
                heldGroups.add(grouping);
            }
        });
        if (heldGroups.isEmpty()) {
            return;
        }
        List<AnalyzerResultItem> heldItems = new ArrayList<>();
        for (AnalyzerResultItem item : actionableResults) {
            if (heldGroups.contains(item.getSampleGroupingNumber())) {
                heldItems.add(item);
            }
        }
        actionableResults.removeAll(heldItems);
        for (AnalyzerResultItem item : heldItems) {
            AnalyzerResults stagedRow = analyzerResultsService.get(item.getId());
            if (stagedRow != null) {
                stagedRow.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN);
                stagedRow.setSysUserId(sysUserId);
                analyzerResultsService.update(stagedRow);
            }
            LogEvent.logWarn(this.getClass().getSimpleName(), "holdGroupsAwaitingSpecimen",
                    "holding accession " + item.getAccessionNumber() + " test " + item.getTestId()
                            + " awaiting specimen: no sample type usable by every result in its grouping was chosen");
        }
    }

    /**
     * A grouping's new analyses are all persisted on its one sample item, whose
     * type must suit every one of them. The grouping waits for a decision when
     * reviewers chose different types or when no type suits every new test, instead
     * of saving a result on a specimen its test cannot use.
     */
    private boolean hasUnusableSpecimenChoice(List<AnalyzerResultItem> grouping) {
        List<AnalyzerResultItem> newAnalyses = newAnalysisRows(grouping).stream()
                .filter(AnalyzerResultItem::getIsAccepted).toList();
        long choices = newAnalyses.stream().map(AnalyzerResultItem::getTypeOfSampleId)
                .filter(choice -> !GenericValidator.isBlankOrNull(choice)).distinct().count();
        List<String> sharedTypes = sharedSampleTypeIds(newAnalyses);
        return choices > 1 || (sharedTypes != null && sharedTypes.isEmpty());
    }

    /**
     * The rows that create an analysis: patient results for an active test the
     * order does not have yet. A result for an inactive test creates nothing and
     * stays staged.
     */
    private List<AnalyzerResultItem> newAnalysisRows(List<AnalyzerResultItem> grouping) {
        return grouping.stream()
                .filter(item -> !item.getIsControl() && !GenericValidator.isBlankOrNull(item.getTestId())
                        && getExistingAnalysis(item) == null
                        && effectiveTestStatusService.isEffectivelyActive(testService.get(item.getTestId())))
                .toList();
    }

    /**
     * The sample types every given new analysis can be collected on: the first
     * test's types, in link order, that each other test also has, narrowed to the
     * reviewer's choice. A test without sample-type links does not constrain them.
     * Null when none of the tests has links.
     */
    private List<String> sharedSampleTypeIds(List<AnalyzerResultItem> newAnalyses) {
        String chosen = groupingSpecimenChoice(newAnalyses);
        List<String> shared = null;
        for (AnalyzerResultItem item : newAnalyses) {
            List<String> types = typeOfSampleTestService.getTypeOfSampleTestsForTest(item.getTestId()).stream()
                    .map(TypeOfSampleTest::getTypeOfSampleId).toList();
            if (types.isEmpty()) {
                continue;
            }
            if (shared == null) {
                shared = types.stream().filter(type -> chosen == null || chosen.equals(type))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else {
                shared.retainAll(types);
            }
        }
        return shared;
    }

    /**
     * The sample types, in preference order, for a grouping's sample item: those
     * its new analyses share. Otherwise, as for a rejected grouping with no shared
     * type or new tests without sample-type links, the first new test's types (the
     * first result's test's when nothing is new), narrowed to the reviewer's
     * choice.
     */
    private List<String> groupingSampleTypeIds(List<AnalyzerResultItem> grouping) {
        List<AnalyzerResultItem> newAnalyses = newAnalysisRows(grouping);
        List<String> sharedTypes = sharedSampleTypeIds(newAnalyses);
        if (sharedTypes != null && !sharedTypes.isEmpty()) {
            return sharedTypes;
        }
        AnalyzerResultItem decidingRow = newAnalyses.isEmpty() ? grouping.get(0) : newAnalyses.get(0);
        List<String> types = typeOfSampleTestService.getTypeOfSampleTestsForTest(decidingRow.getTestId()).stream()
                .map(TypeOfSampleTest::getTypeOfSampleId).toList();
        String chosen = groupingSpecimenChoice(grouping);
        return !GenericValidator.isBlankOrNull(chosen) && types.contains(chosen) ? List.of(chosen) : types;
    }

    private String groupingSpecimenChoice(List<AnalyzerResultItem> groupedAnalyzerResultItems) {
        return groupedAnalyzerResultItems.stream().map(AnalyzerResultItem::getTypeOfSampleId)
                .filter(choice -> !GenericValidator.isBlankOrNull(choice)).findFirst().orElse(null);
    }

    /**
     * True when accepting this row would force a specimen guess: its test runs on
     * more than one sample type, the reviewer chose none, no existing sample item
     * on the accession pins one of the candidates, and no special-case rule
     * (RetroCI LDBS→DBS) applies.
     */
    private boolean needsSpecimenChoice(AnalyzerResultItem item) {
        if (item.getIsControl() || GenericValidator.isBlankOrNull(item.getTestId())) {
            return false;
        }
        List<TypeOfSampleTest> candidates = typeOfSampleTestService.getTypeOfSampleTestsForTest(item.getTestId());
        if (candidates.size() <= 1 || candidates.stream()
                .anyMatch(candidate -> candidate.getTypeOfSampleId().equals(item.getTypeOfSampleId()))) {
            return false;
        }
        if (IS_RETROCI && DBS_SAMPLE_TYPE_ID != null && item.getAccessionNumber() != null
                && item.getAccessionNumber().startsWith("LDBS")
                && candidates.stream().anyMatch(c -> DBS_SAMPLE_TYPE_ID.equals(c.getTypeOfSampleId()))) {
            return false;
        }
        Sample sample = sampleService.getSampleByAccessionNumber(item.getAccessionNumber());
        if (sample != null) {
            for (Analysis analysis : analysisService.getAnalysesBySampleId(sample.getId())) {
                for (TypeOfSampleTest candidate : candidates) {
                    if (candidate.getTypeOfSampleId().equals(analysis.getSampleItem().getTypeOfSampleId())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------
    // Extraction helpers
    // ---------------------------------------------------------------

    /**
     * Applies the reviewer's decisions. Accept, retest and ignore are decided per
     * test: a test's components follow its decision, and the other tests on the
     * same specimen keep their own. Every row of a grouping takes its specimen's
     * accession.
     */
    List<AnalyzerResultItem> extractActionableResult(List<AnalyzerResultItem> resultItemList) {
        List<AnalyzerResultItem> actionableResultList = new ArrayList<>();
        Map<DecisionKey, AnalyzerResultItem> selectedActions = new HashMap<>();
        for (AnalyzerResultItem item : resultItemList) {
            if (item.getIsAccepted() || item.getIsRejected() || item.getIsDeleted()) {
                selectedActions.putIfAbsent(DecisionKey.of(item), item);
            }
        }

        Map<Integer, String> accessionByGrouping = new HashMap<>();
        for (AnalyzerResultItem resultItem : resultItemList) {
            String accessionNumber = accessionByGrouping.computeIfAbsent(resultItem.getSampleGroupingNumber(),
                    grouping -> resultItem.getAccessionNumber());
            AnalyzerResultItem action = selectedActions.getOrDefault(DecisionKey.of(resultItem), resultItem);
            resultItem.setAccessionNumber(accessionNumber);
            resultItem.setIsAccepted(action.getIsAccepted());
            resultItem.setIsRejected(action.getIsRejected());
            resultItem.setIsDeleted(action.getIsDeleted());

            if (resultItem.getIsAccepted() || resultItem.getIsRejected() || resultItem.getIsDeleted()) {
                actionableResultList.add(resultItem);
            }
        }

        return actionableResultList;
    }

    private record DecisionKey(int grouping, String testId) {

        static DecisionKey of(AnalyzerResultItem item) {
            return new DecisionKey(item.getSampleGroupingNumber(), item.getTestId());
        }
    }

    List<AnalyzerResultItem> extractChildlessControls(List<AnalyzerResultItem> resultItemList) {
        /*
         * A childless control is a control which is adjacent to another control. It is
         * the first set of controls which will be removed. For that reason we're going
         * through the list backwards.
         */
        List<AnalyzerResultItem> childLessControlList = new ArrayList<>();
        int sampleGroupingNumber = 0;
        boolean lastGroupIsControl = false;
        boolean inControlGroup = true; // covers the bottom control has no children

        for (int i = resultItemList.size() - 1; i >= 0; i--) {
            AnalyzerResultItem resultItem = resultItemList.get(i);

            if (sampleGroupingNumber != resultItem.getSampleGroupingNumber()) {
                lastGroupIsControl = inControlGroup;
                inControlGroup = resultItem.getIsControl();
                sampleGroupingNumber = resultItem.getSampleGroupingNumber();
            }

            if (lastGroupIsControl && resultItem.getIsControl()) {
                childLessControlList.add(resultItem);
            }
        }

        return childLessControlList;
    }

    List<AnalyzerResults> getRemovableAnalyzerResults(List<AnalyzerResultItem> actionableResults,
            List<AnalyzerResultItem> childlessControls) {

        Set<AnalyzerResults> deletableAnalyzerResults = new HashSet<>();

        for (AnalyzerResultItem resultItem : actionableResults) {
            AnalyzerResults result = new AnalyzerResults();
            result.setId(resultItem.getId());
            deletableAnalyzerResults.add(result);
        }

        for (AnalyzerResultItem resultItem : childlessControls) {
            AnalyzerResults result = new AnalyzerResults();
            result.setId(resultItem.getId());
            deletableAnalyzerResults.add(result);
        }

        List<AnalyzerResults> resultList = new ArrayList<>();
        resultList.addAll(deletableAnalyzerResults);
        return resultList;
    }

    // ---------------------------------------------------------------
    // SampleGrouping construction (was createResultsFromItems)
    // ---------------------------------------------------------------

    void buildSampleGroupings(List<AnalyzerResultItem> actionableResults, List<SampleGrouping> sampleGroupList,
            String sysUserId) {
        int groupingNumber = -1;
        List<AnalyzerResultItem> groupedResultList = new ArrayList<>();

        for (AnalyzerResultItem analyzerResultItem : actionableResults) {
            if (analyzerResultItem.getIsDeleted()) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "buildSampleGroupings",
                        "Skipping deleted item: " + analyzerResultItem.getAccessionNumber());
                continue;
            }

            if (analyzerResultItem.getSampleGroupingNumber() != groupingNumber) {
                groupingNumber = analyzerResultItem.getSampleGroupingNumber();

                SampleGrouping sampleGrouping = createRecordsForNewResult(groupedResultList, sysUserId);

                if (sampleGrouping != null) {
                    sampleGrouping.triggersToSelectedReflexesMap = new HashMap<>();
                    sampleGroupList.add(sampleGrouping);
                }

                groupedResultList = new ArrayList<>();
            }

            if (!analyzerResultItem.isReadOnly()) {
                groupedResultList.add(analyzerResultItem);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "buildSampleGroupings",
                        "Skipping read-only item: accession=" + analyzerResultItem.getAccessionNumber() + ", test="
                                + analyzerResultItem.getTestName() + ", testId=" + analyzerResultItem.getTestId());
            }
        }

        // for the last set of results the grouping number will not change
        SampleGrouping sampleGrouping = createRecordsForNewResult(groupedResultList, sysUserId);
        if (sampleGrouping != null) {
            sampleGrouping.triggersToSelectedReflexesMap = new HashMap<>();
            sampleGroupList.add(sampleGrouping);
        }
    }

    private SampleGrouping createRecordsForNewResult(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            String sysUserId) {

        if (groupedAnalyzerResultItems != null && !groupedAnalyzerResultItems.isEmpty()) {
            String accessionNumber = groupedAnalyzerResultItems.get(0).getAccessionNumber();
            StatusSet statusSet = statusService.getStatusSetForAccessionNumber(accessionNumber);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                    "Accession: " + accessionNumber + ", sampleStatus="
                            + (statusSet != null ? statusSet.getSampleRecordStatus() : "null") + ", patientStatus="
                            + (statusSet != null ? statusSet.getPatientRecordStatus() : "null") + ", noEntryDone="
                            + noEntryDone(statusSet, accessionNumber));

            if (noEntryDone(statusSet, accessionNumber)) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForNoSampleEntryDone for " + accessionNumber);
                return createGroupForNoSampleEntryDone(groupedAnalyzerResultItems, statusSet, sysUserId);
            } else if (statusSet
                    .getSampleRecordStatus() == org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered
                    && statusSet
                            .getPatientRecordStatus() == org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForPreviousAnalyzerDone for " + accessionNumber);
                return createGroupForPreviousAnalyzerDone(groupedAnalyzerResultItems, statusSet, sysUserId);
            } else if (statusSet
                    .getSampleRecordStatus() == org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForDemographicsEntered for " + accessionNumber);
                return createGroupForDemographicsEntered(groupedAnalyzerResultItems, statusSet, sysUserId);
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createRecordsForNewResult",
                        "Path: createGroupForSampleAndDemographicsEntered for " + accessionNumber);
                return createGroupForSampleAndDemographicsEntered(groupedAnalyzerResultItems, statusSet, sysUserId);
            }
        }

        return null;
    }

    private boolean noEntryDone(StatusSet statusSet, String accessionNumber) {
        boolean sampleOrPatientEntryDone = statusSet.getPatientRecordStatus() != null
                || statusSet.getSampleRecordStatus() != null;

        if (sampleOrPatientEntryDone) {
            return false;
        }

        return sampleService.getSampleByAccessionNumber(accessionNumber) == null;
    }

    // ---------------------------------------------------------------
    // Group-creation methods (one per status scenario)
    // ---------------------------------------------------------------

    private SampleGrouping createGroupForPreviousAnalyzerDone(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = sampleService
                .getSampleByAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        Patient patient = sampleHumanService.getPatientForSample(sample);
        createAndAddItems_Analysis_Results(groupedAnalyzerResultItems, analysisList, resultList,
                resultToUserSelectionMap, noteList, sampleGrouping.skippedResultIds, patient, sysUserId);
        recordResultsOnOrder(sample, resultList, false, sysUserId);

        SampleItem sampleItem = getOrCreateSampleItem(groupedAnalyzerResultItems, sample, sysUserId);

        sampleGrouping.sample = sample;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = false;
        sampleGrouping.addSampleItem = sampleItem.getId() == null;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.patient = patient;
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    SampleItem getOrCreateSampleItem(List<AnalyzerResultItem> groupedAnalyzerResultItems, Sample sample,
            String sysUserId) {
        List<Analysis> dBAnalysisList = analysisService.getAnalysesBySampleId(sample.getId());
        List<String> typeOfSampleIds = groupingSampleTypeIds(groupedAnalyzerResultItems);

        SampleItem sampleItem = null;
        int maxSampleItemSortOrder = 0;

        for (Analysis dbAnalysis : dBAnalysisList) {
            if (!GenericValidator.isBlankOrNull(dbAnalysis.getSampleItem().getSortOrder())) {
                maxSampleItemSortOrder = Math.max(maxSampleItemSortOrder,
                        Integer.parseInt(dbAnalysis.getSampleItem().getSortOrder()));
            }
            if (typeOfSampleIds.contains(dbAnalysis.getSampleItem().getTypeOfSampleId())) {
                sampleItem = dbAnalysis.getSampleItem();
                break;
            }
        }

        boolean newSampleItem = sampleItem == null;

        if (newSampleItem) {
            sampleItem = new SampleItem();
            sampleItem.setSysUserId(sysUserId);
            sampleItem.setSortOrder(Integer.toString(maxSampleItemSortOrder + 1));
            sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
            if (typeOfSampleIds.size() > 1) {
                // Accepted ambiguous groups are intercepted by the FR-8
                // awaiting-specimen hold before reaching here; this fallback
                // (e.g. rejected results) stays deterministic with a warning.
                LogEvent.logWarn(this.getClass().getSimpleName(), "getOrCreateSampleItem",
                        "creating sample item for accession " + groupedAnalyzerResultItems.get(0).getAccessionNumber()
                                + " whose results can use " + typeOfSampleIds.size()
                                + " sample types and no specimen context; using the primary link");
            }
            TypeOfSample typeOfSample = typeOfSampleService.get(typeOfSampleIds.get(0));
            sampleItem.setTypeOfSample(typeOfSample);
        }
        return sampleItem;
    }

    private SampleGrouping createGroupForDemographicsEntered(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = sampleService
                .getSampleByAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());

        SampleItem sampleItem = getOrCreateSampleItem(groupedAnalyzerResultItems, sample, sysUserId);

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        Patient patient = sampleHumanService.getPatientForSample(sample);
        createAndAddItems_Analysis_Results(groupedAnalyzerResultItems, analysisList, resultList,
                resultToUserSelectionMap, noteList, sampleGrouping.skippedResultIds, patient, sysUserId);
        recordResultsOnOrder(sample, resultList, true, sysUserId);

        sampleGrouping.sample = sample;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = false;
        sampleGrouping.updateSample = true;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.addSampleItem = sampleItem.getId() == null;
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.patient = patient;
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    private SampleGrouping createGroupForSampleAndDemographicsEntered(
            List<AnalyzerResultItem> groupedAnalyzerResultItems, StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = sampleService
                .getSampleByAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        // New analyses are persisted on the grouping's sample item; an existing
        // analysis keeps its own, which serves only when nothing new is added.
        SampleItem newAnalysisSampleItem = null;
        SampleItem existingAnalysisSampleItem = null;
        Patient patient = sampleHumanService.getPatientForSample(sample);

        for (AnalyzerResultItem resultItem : groupedAnalyzerResultItems) {
            Analysis analysis = getExistingAnalysis(resultItem);

            if (analysis == null) {
                Test test = testService.get(resultItem.getTestId());
                // OGC-189 (M4): gate creation only — an analysis that already
                // exists (the loop above) still accepts its result per D3.
                if (!effectiveTestStatusService.isEffectivelyActive(test)) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "persistResults",
                            "Analyzer result skipped: no analysis created for test id " + resultItem.getTestId()
                                    + " because its lab unit is inactive (OGC-189).");
                    sampleGrouping.skippedResultIds.add(resultItem.getId());
                    continue;
                }
                analysis = new Analysis();
                analysis.setTest(test);
                if (newAnalysisSampleItem == null) {
                    newAnalysisSampleItem = sampleItemForNewAnalyses(groupedAnalyzerResultItems, sample, sysUserId);
                }
                analysis.setSampleItem(newAnalysisSampleItem);
            } else {
                if (existingAnalysisSampleItem == null) {
                    existingAnalysisSampleItem = analysis.getSampleItem();
                    existingAnalysisSampleItem.setSysUserId(sysUserId);
                }
            }

            populateAnalysis(resultItem, analysis, analysis.getTest());
            analysis.setSysUserId(sysUserId);
            analysisList.add(analysis);

            Result result = getResult(analysis, patient, resultItem, sysUserId);
            resultToUserSelectionMap.put(result, resultItem.getReflexSelectionId());

            resultList.add(result);

            if (GenericValidator.isBlankOrNull(resultItem.getNote())) {
                noteList.add(null);
            } else {
                Note note = noteService.createSavableNote(analysis, NoteServiceImpl.NoteType.INTERNAL,
                        resultItem.getNote(), RESULT_SUBJECT, sysUserId);
                noteList.add(note);
            }
        }
        recordResultsOnOrder(sample, resultList, true, sysUserId);

        SampleItem sampleItem = newAnalysisSampleItem != null ? newAnalysisSampleItem : existingAnalysisSampleItem;
        sampleGrouping.sample = sample;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = false;
        sampleGrouping.updateSample = true;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.addSampleItem = (sampleItem == null || sampleItem.getId() == null);
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.patient = patient;
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    /**
     * The one sample item that a grouping's new analyses on an entered order share:
     * the order's own item of a type they can all use, else a new item of the first
     * such type.
     */
    private SampleItem sampleItemForNewAnalyses(List<AnalyzerResultItem> grouping, Sample sample, String sysUserId) {
        List<String> typeIds = groupingSampleTypeIds(grouping);
        List<SampleItem> sampleItemsForSample = sampleItemService.getSampleItemsBySampleId(sample.getId());
        SampleItem sampleItem = null;
        for (SampleItem item : sampleItemsForSample) {
            if (!typeIds.isEmpty() && item.getTypeOfSample() != null
                    && typeIds.contains(item.getTypeOfSample().getId())) {
                sampleItem = item;
            }
        }
        if (sampleItem == null && typeIds.isEmpty() && !sampleItemsForSample.isEmpty()) {
            sampleItem = sampleItemsForSample.get(0);
        }
        if (sampleItem == null) {
            sampleItem = new SampleItem();
            sampleItem.setSortOrder("1");
            sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
            sampleItem.setCollectionDate(DateUtil.getNowAsTimestamp());
            if (!typeIds.isEmpty()) {
                sampleItem.setTypeOfSample(typeOfSampleService.get(typeIds.get(0)));
            }
        }
        sampleItem.setSysUserId(sysUserId);
        return sampleItem;
    }

    /**
     * Marks an existing order as updated by a grouping that saves results on it.
     * The order is a managed entity, so any change to it is written at commit even
     * when the grouping is then discarded because every result was skipped.
     */
    private void recordResultsOnOrder(Sample sample, List<Result> resultList, boolean startEnteredOrder,
            String sysUserId) {
        if (resultList.isEmpty()) {
            return;
        }
        if (startEnteredOrder && statusService.getStatusID(OrderStatus.Entered).equals(sample.getStatusId())) {
            sample.setStatusId(statusService.getStatusID(OrderStatus.Started));
        }
        sample.setEnteredDate(new Date(new java.util.Date().getTime()));
        sample.setSysUserId(sysUserId);
    }

    private SampleGrouping createGroupForNoSampleEntryDone(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            StatusSet statusSet, String sysUserId) {
        SampleGrouping sampleGrouping = new SampleGrouping();
        Sample sample = new Sample();
        SampleHuman sampleHuman = new SampleHuman();
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSysUserId(sysUserId);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));

        List<Analysis> analysisList = new ArrayList<>();
        List<Result> resultList = new ArrayList<>();
        Map<Result, String> resultToUserSelectionMap = new HashMap<>();
        List<Note> noteList = new ArrayList<>();

        sample.setAccessionNumber(groupedAnalyzerResultItems.get(0).getAccessionNumber());
        sample.setDomain("H");
        sample.setStatusId(statusService.getStatusID(OrderStatus.Started));
        sample.setEnteredDate(new Date(new java.util.Date().getTime()));
        sample.setReceivedDate(new Date(new java.util.Date().getTime()));
        sample.setSysUserId(sysUserId);

        sampleHuman.setPatientId(PatientUtil.getUnknownPatient().getId());
        sampleHuman.setSysUserId(sysUserId);

        Patient patient = PatientUtil.getUnknownPatient();
        createAndAddItems_Analysis_Results(groupedAnalyzerResultItems, analysisList, resultList,
                resultToUserSelectionMap, noteList, sampleGrouping.skippedResultIds, patient, sysUserId);

        addSampleTypeToSampleItem(sampleItem, analysisList, sample.getAccessionNumber(), groupedAnalyzerResultItems);

        sampleGrouping.sample = sample;
        sampleGrouping.sampleHuman = sampleHuman;
        sampleGrouping.sampleItem = sampleItem;
        sampleGrouping.patient = patient;
        sampleGrouping.analysisList = analysisList;
        sampleGrouping.resultList = resultList;
        sampleGrouping.noteList = noteList;
        sampleGrouping.addSample = true;
        sampleGrouping.addSampleItem = true;
        sampleGrouping.statusSet = statusSet;
        sampleGrouping.accepted = groupedAnalyzerResultItems.get(0).getIsAccepted();
        sampleGrouping.resultToUserserSelectionMap = resultToUserSelectionMap;

        return sampleGrouping;
    }

    // ---------------------------------------------------------------
    // Analysis / Result / Note construction
    // ---------------------------------------------------------------

    private void createAndAddItems_Analysis_Results(List<AnalyzerResultItem> groupedAnalyzerResultItems,
            List<Analysis> analysisList, List<Result> resultList, Map<Result, String> resultToUserSelectionMap,
            List<Note> noteList, List<String> skippedResultIds, Patient patient, String sysUserId) {

        for (AnalyzerResultItem resultItem : groupedAnalyzerResultItems) {
            Analysis analysis = getExistingAnalysis(resultItem);

            if (analysis == null) {
                Test test = testService.get(resultItem.getTestId());
                // OGC-189 (M4): no NEW analysis for a test whose lab unit is
                // switched off. Decision D3 draws the line here — this branch
                // creates work that did not exist, so it is gated; the else
                // branch below completes an analysis that already exists, which
                // must keep flowing so an in-flight specimen is never stranded.
                if (!effectiveTestStatusService.isEffectivelyActive(test)) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "persistAnalyzerResults",
                            "Analyzer result skipped: no analysis created for test id " + resultItem.getTestId()
                                    + " because its lab unit is inactive (OGC-189).");
                    skippedResultIds.add(resultItem.getId());
                    continue;
                }
                analysis = new Analysis();
                populateAnalysis(resultItem, analysis, test);
            } else {
                String statusId = statusService
                        .getStatusID(resultItem.getIsAccepted() ? AnalysisStatus.TechnicalAcceptance
                                : AnalysisStatus.TechnicalRejected);
                analysis.setStatusId(statusId);
                analysis.setAnalyzerId(resultItem.getAnalyzerId());
                analysis.setRevision(nextRevision(analysis));
            }

            analysis.setSysUserId(sysUserId);
            analysisList.add(analysis);

            Result result = getResult(analysis, patient, resultItem, sysUserId);
            resultList.add(result);
            resultToUserSelectionMap.put(result, resultItem.getReflexSelectionId());
            if (GenericValidator.isBlankOrNull(resultItem.getNote())) {
                noteList.add(null);
            } else {
                Note note = noteService.createSavableNote(analysis, NoteServiceImpl.NoteType.INTERNAL,
                        resultItem.getNote(), RESULT_SUBJECT, sysUserId);
                noteList.add(note);
            }
        }
    }

    /**
     * The existing Result bound to the given component (null = PRIMARY), preferring
     * the last match to preserve prior single-component behavior. Null when none of
     * the analysis's results belongs to that component.
     */
    private Result findResultForComponent(List<Result> resultList, String componentId) {
        Result match = null;
        for (Result candidate : resultList) {
            String candidateComponentId = candidate.getTestResult() == null ? null
                    : candidate.getTestResult().getComponentId();
            if (componentId == null ? candidateComponentId == null : componentId.equals(candidateComponentId)) {
                match = candidate;
            }
        }
        return match;
    }

    /**
     * The one analysis this result lands on: the single match, or the reviewer's
     * pick among several. Null when the test is not ordered. Never the first of
     * many.
     */
    private Analysis getExistingAnalysis(AnalyzerResultItem resultItem) {
        AnalyzerResultPlacement placed = placementOf(resultItem);
        String chosen = resultItem.getChosenAnalysisId();
        if (isCandidate(placed, chosen)) {
            return analysisService.get(chosen);
        }
        return placed.proposedAnalysisId() != null && placed.analyses().size() == 1
                ? analysisService.get(placed.proposedAnalysisId())
                : null;
    }

    private Result getResult(Analysis analysis, Patient patient, AnalyzerResultItem resultItem, String sysUserId) {
        Result result = null;

        if (analysis.getId() != null) {
            List<Result> resultList = resultService.getResultsByAnalysis(analysis);

            // OGC-1129: update the existing Result for THIS component, not just the last
            // one — otherwise multiplex components sharing an analysis clobber each other.
            result = findResultForComponent(resultList, resultItem.getComponentId());
            if (result != null) {
                String resultValue = resultItem.getIsRejected() ? REJECT_VALUE : resultItem.getResult();
                TestResult resolvedTestResult = getTestResultForResult(resultItem);
                result.setTestResult(resolvedTestResult);
                if (resolvedTestResult != null && "D".equals(resolvedTestResult.getTestResultType())
                        && !resultItem.getIsRejected()) {
                    result.setValue(resolvedTestResult.getValue());
                    result.setResultType("D");
                } else {
                    result.setValue(resultValue);
                }
                result.setSysUserId(sysUserId);

                setAnalyte(result);
            }
        }

        if (result == null) {
            result = createNewResult(resultItem, patient, sysUserId);
        }

        return result;
    }

    private Result createNewResult(AnalyzerResultItem resultItem, Patient patient, String sysUserId) {
        Result result = new Result();
        String rawValue = resultItem.getIsRejected() ? REJECT_VALUE : resultItem.getResult();
        TestResult resolvedTestResult = getTestResultForResult(resultItem);
        result.setTestResult(resolvedTestResult);
        if (resolvedTestResult != null && "D".equals(resolvedTestResult.getTestResultType())
                && !resultItem.getIsRejected()) {
            result.setValue(resolvedTestResult.getValue());
            result.setResultType("D");
        } else if (resolvedTestResult != null) {
            result.setValue(rawValue);
            result.setResultType(resolvedTestResult.getTestResultType());
        } else {
            result.setValue(rawValue);
            result.setResultType(resultItem.getTestResultType());
        }
        if (!GenericValidator.isBlankOrNull(resultItem.getSignificantDigits())) {
            if (StringUtil.isInteger(resultItem.getSignificantDigits())) {
                result.setSignificantDigits(Integer.parseInt(resultItem.getSignificantDigits()));
            } else {
                LogEvent.logWarn(AnalyzerResultsAcceptServiceImpl.class.getSimpleName(), "createNewResult",
                        "Invalid significantDigits value for testId '" + resultItem.getTestId() + "'");
            }
        }

        addMinMaxNormal(result, resultItem, patient);
        result.setSysUserId(sysUserId);

        return result;
    }

    private void populateAnalysis(AnalyzerResultItem resultItem, Analysis analysis, Test test) {
        if (!statusService.getStatusID(AnalysisStatus.Canceled).equals(analysis.getStatusId())) {
            String statusId = statusService.getStatusID(
                    resultItem.getIsAccepted() ? AnalysisStatus.TechnicalAcceptance : AnalysisStatus.TechnicalRejected);
            analysis.setStatusId(statusId);
            analysis.setAnalysisType(resultItem.getManual() ? IActionConstants.ANALYSIS_TYPE_MANUAL
                    : IActionConstants.ANALYSIS_TYPE_AUTO);
            analysis.setCompletedDateForDisplay(resultItem.getCompleteDate());
            analysis.setTest(test);
            analysis.setTestSection(test.getTestSection());
            analysis.setIsReportable(test.getIsReportable());
            analysis.setRevision(nextRevision(analysis));
            analysis.setAnalyzerId(resultItem.getAnalyzerId());
        }
    }

    /**
     * A first result leaves the revision alone ("0" for a new analysis). Saving a
     * result over one the analysis already holds is a correction, so the revision
     * rises above 1, which validation reads as modified and reports as corrected.
     */
    private String nextRevision(Analysis analysis) {
        boolean replacing = analysis.getId() != null && !resultService.getResultsByAnalysis(analysis).isEmpty();
        if (!replacing) {
            return GenericValidator.isBlankOrNull(analysis.getRevision()) ? "0" : analysis.getRevision();
        }
        int current = StringUtil.isInteger(analysis.getRevision()) ? Integer.parseInt(analysis.getRevision().trim())
                : 0;
        return String.valueOf(Math.max(current, 1) + 1);
    }

    private void setAnalyte(Result result) {
        TestAnalyte testAnalyte = ResultUtil.getTestAnalyteForResult(result);

        if (testAnalyte != null) {
            result.setAnalyte(testAnalyte.getAnalyte());
        }
    }

    TestResult getTestResultForResult(AnalyzerResultItem resultItem) {
        List<TestResult> all = testResultService.getActiveTestResultsByTest(resultItem.getTestId());
        if (all == null || all.isEmpty()) {
            return null;
        }
        // OGC-1129: bind to the test_result rows of the resolved component so the
        // created Result carries the component (null = PRIMARY, today's behavior).
        List<TestResult> candidates = filterTestResultsByComponent(all, resultItem.getComponentId());
        boolean hasDictCandidates = candidates.stream().anyMatch(c -> "D".equals(c.getTestResultType()));
        if (hasDictCandidates) {
            TestResult testResult = testResultService.getTestResultsByTestAndDictonaryResult(resultItem.getTestId(),
                    resultItem.getResult());
            // Only trust the test-scoped dictionary match when it belongs to the target
            // component; otherwise fall through to the component-filtered candidates.
            String resolvedTestResultId = testResult == null ? null : testResult.getId();
            boolean belongsToComponent = candidates.stream()
                    .anyMatch(candidate -> candidate.getId().equals(resolvedTestResultId));
            if (testResult != null && !belongsToComponent) {
                testResult = null;
            }
            if (testResult == null && !StringUtil.isInteger(resultItem.getResult())) {
                String desired = resultItem.getResult().trim();
                for (TestResult candidate : candidates) {
                    if (!"D".equals(candidate.getTestResultType())) {
                        continue;
                    }
                    Dictionary dict = dictionaryService.get(candidate.getValue());
                    if (dict != null && dict.getDictEntry() != null
                            && desired.equalsIgnoreCase(dict.getDictEntry().trim())) {
                        testResult = candidate;
                        break;
                    }
                }
            }
            if (testResult != null) {
                return testResult;
            }
        }
        return candidates.get(0);
    }

    /**
     * Keep only the test_result rows belonging to the resolved component (null =
     * PRIMARY / legacy component_id-null rows). Falls back to the full list when no
     * row matches, so a test whose test_result rows predate components still works.
     */
    private List<TestResult> filterTestResultsByComponent(List<TestResult> candidates, String componentId) {
        List<TestResult> filtered = new ArrayList<>();
        for (TestResult candidate : candidates) {
            String candidateComponentId = candidate.getComponentId();
            if (componentId == null ? candidateComponentId == null : componentId.equals(candidateComponentId)) {
                filtered.add(candidate);
            }
        }
        return filtered.isEmpty() ? candidates : filtered;
    }

    private void addMinMaxNormal(Result result, AnalyzerResultItem resultItem, Patient patient) {
        boolean limitsFound = false;

        if (resultItem != null) {
            // OGC-1145 Phase 2: the reviewer-resolved specimen (when present)
            // selects a scoped limit over the shared set.
            ResultLimit resultLimit = resultLimitService.getResultLimitForTestAndPatient(resultItem.getTestId(),
                    patient, resultItem.getTypeOfSampleId());
            if (resultLimit != null) {
                result.setMinNormal(resultLimit.getLowNormal());
                result.setMaxNormal(resultLimit.getHighNormal());
                limitsFound = true;
            }
        }

        if (!limitsFound) {
            result.setMinNormal(Double.NEGATIVE_INFINITY);
            result.setMaxNormal(Double.POSITIVE_INFINITY);
        }
    }

    // ---------------------------------------------------------------
    // Sample type helpers
    // ---------------------------------------------------------------

    private void addSampleTypeToSampleItem(SampleItem sampleItem, List<Analysis> analysisList, String accessionNumber,
            List<AnalyzerResultItem> grouping) {
        if (analysisList.size() > 0) {
            String typeOfSampleId = getTypeOfSampleId(groupingSampleTypeIds(grouping), accessionNumber);
            sampleItem.setTypeOfSample(typeOfSampleService.get(typeOfSampleId));
        }
    }

    private String getTypeOfSampleId(List<String> typeOfSampleIds, String accessionNumber) {
        if (IS_RETROCI && DBS_SAMPLE_TYPE_ID != null && accessionNumber.startsWith("LDBS")
                && typeOfSampleIds.contains(DBS_SAMPLE_TYPE_ID)) {
            return DBS_SAMPLE_TYPE_ID;
        }
        if (typeOfSampleIds.size() > 1) {
            // Accepted ambiguous groups are intercepted by the FR-8 hold; this
            // fallback stays deterministic with a warning.
            LogEvent.logWarn(this.getClass().getSimpleName(), "getTypeOfSampleId",
                    "accession " + accessionNumber + " can use " + typeOfSampleIds.size()
                            + " sample types and no specimen context; using the primary link");
        }
        return typeOfSampleIds.get(0);
    }

    // ---------------------------------------------------------------
    // QA / validation helpers
    // ---------------------------------------------------------------

    boolean getQaEventByTestSection(Analysis analysis) {
        if (analysis == null) {
            return false;
        }
        if (analysis.getTestSection() != null && analysis.getSampleItem().getSample() != null) {
            Sample sample = analysis.getSampleItem().getSample();
            List<SampleQaEvent> sampleQaEventsList = getSampleQaEvents(sample);
            for (SampleQaEvent event : sampleQaEventsList) {
                QAService qa = new QAService(event);
                if (!GenericValidator.isBlankOrNull(qa.getObservationValue(QAObservationType.SECTION))
                        && qa.getObservationValue(QAObservationType.SECTION)
                                .equals(analysis.getTestSection().getNameKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    List<SampleQaEvent> getSampleQaEvents(Sample sample) {
        return sampleQaEventService.getSampleQaEventsBySample(sample);
    }

    public Errors validateSavableItems(List<AnalyzerResultItem> savableResults, Errors errors) {
        for (AnalyzerResultItem item : savableResults) {
            if (item.getIsAccepted() && item.isUserChoicePending()) {
                StringBuilder augmentedAccession = new StringBuilder(item.getAccessionNumber());
                augmentedAccession.append(" : ");
                augmentedAccession.append(item.getTestName());
                augmentedAccession.append(" - ");
                augmentedAccession.append(MessageUtil.getMessage("error.reflexStep.notChosen"));
                String errorMsg = "errors.followingAccession";
                errors.reject(errorMsg, new String[] { augmentedAccession.toString() }, errorMsg);
            }
        }

        return errors;
    }

    // ---------------------------------------------------------------
    // Configuration constants (copied from controller)
    // ---------------------------------------------------------------

    private static final boolean IS_RETROCI = org.openelisglobal.common.util.ConfigurationProperties.getInstance()
            .isPropertyValueEqual(org.openelisglobal.common.util.ConfigurationProperties.Property.configurationName,
                    "CI_GENERAL");

    private final String DBS_SAMPLE_TYPE_ID;

    /**
     * Resolves the DBS sample type ID when running in RetroCI mode. The type is
     * matched on its local abbreviation first because a catalog import can rewrite
     * the description; a missing type must not stop the application from starting.
     */
    public AnalyzerResultsAcceptServiceImpl(TypeOfSampleService typeOfSampleService) {
        DBS_SAMPLE_TYPE_ID = IS_RETROCI ? resolveDbsSampleTypeId(typeOfSampleService) : null;
    }

    private static String resolveDbsSampleTypeId(TypeOfSampleService typeOfSampleService) {
        TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("DBS",
                Domain.CLINICAL.name());
        if (typeOfSample == null) {
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription("DBS");
            searchType.setDomain(Domain.CLINICAL.name());
            typeOfSample = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, false);
        }
        if (typeOfSample == null) {
            LogEvent.logWarn(AnalyzerResultsAcceptServiceImpl.class.getSimpleName(), "resolveDbsSampleTypeId",
                    "No clinical DBS sample type found; LDBS accessions will not default to DBS");
            return null;
        }
        return typeOfSample.getId();
    }
}
