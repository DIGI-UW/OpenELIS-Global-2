import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  Accordion,
  AccordionItem,
  ActionableNotification,
  Button,
  Checkbox,
  Column,
  ComboBox,
  Dropdown,
  Grid,
  InlineNotification,
  Link as CarbonLink,
  Loading,
  Modal,
  Tag,
} from "@carbon/react";
import { ArrowLeft, Copy, Save } from "@carbon/icons-react";
import { parsePath } from "history";
import { FormattedMessage, useIntl } from "react-intl";
import { Link, useHistory, useLocation, useParams } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AffectedAnalyzerList from "../AnalyzerTypeManagement/AffectedAnalyzerList";
import {
  formatRecognitionCondition,
  formatRecognitionMode,
} from "../AnalyzerTypeManagement/recognitionText";
import { safeInternalPath } from "../../utils/UrlUtils";
import {
  adoptAnalyzerRevision,
  applyAnalyzerMapping,
  confirmAnalyzerMapping,
  getAnalyzerAdoption,
  getAnalyzerMapping,
  getAnalyzerMappingComponents,
  getAnalyzerMappingResultOptions,
  getAnalyzerMappingTests,
  getAnalyzerTypeDefaults,
  getAnalyzerTypeRevision,
  saveAnalyzerMapping,
} from "../../../services/analyzerService";
import { includesComboBoxText } from "../comboBoxSearch";
import { analyzerErrorText } from "../analyzerErrors";
import "./AnalyzerTypeMappingEditor.scss";

const hasApiError = (response) =>
  !response || Boolean(response.error) || Number(response.status || 0) >= 400;

const cloneTests = (tests = []) =>
  tests.map((test) => ({
    ...test,
    aliases: [...(test.aliases || [])],
    results: (test.results || []).map((result) => ({ ...result })),
  }));

// A test can report several records under one code; each is its own row,
// named by its code and the vendor's sub-identity.
const recordKey = (test) =>
  test.subIdentity
    ? `${test.sourceRowKey} ${test.subIdentity}`
    : test.sourceRowKey;

const recordLabel = (test) =>
  test.subIdentity ? `${test.rawCode} ${test.subIdentity}` : test.rawCode;

// A record lands on a component of its test; the main record of a test that
// reports a call sends the call to one.
const takesComponent = (test) =>
  Boolean(test.subIdentity || test.componentCode);

const takesCallComponent = (test) =>
  !test.subIdentity && Boolean(test.callComponentCode || test.callComponentId);

const answerComponentId = (test) =>
  test.subIdentity ? test.componentId : test.callComponentId;

const componentWithCode = (components, code) => {
  const matches = code
    ? components.filter((component) => component.code === code)
    : [];
  return matches.length === 1 ? matches[0].id : null;
};

const unresolvedResults = (results) =>
  results.map((result) => ({
    ...result,
    mappingState: "UNRESOLVED",
    resultOptionId: null,
    selectedOption: null,
  }));

const testItemText = (test) => {
  if (!test) {
    return "";
  }
  const identity = [test.code, ...(test.loincCodes || [])]
    .filter(Boolean)
    .join(" · ");
  return identity ? `${test.name} · ${identity}` : test.name;
};

const resultItemText = (option) => option?.label || "";

const componentItemText = (component) =>
  component?.label || component?.code || "";

const stateTagType = (state) => {
  if (state === "BOUND") {
    return "green";
  }
  if (state === "EXCLUDED") {
    return "gray";
  }
  return "warm-gray";
};

const stateMessageId = (state) =>
  `analyzerType.mappingEditor.state.${String(state || "UNRESOLVED").toLowerCase()}`;

// Adoption shows rows by what adopting the newer revision does to them, the
// ones that need the operator first.
const ADOPTION_BUCKETS = ["BLOCKED", "NEEDS_MAPPING", "CHANGED", "UNCHANGED"];

const planRowKey = (planRow) =>
  planRow.key.subIdentity
    ? `${planRow.key.sourceRowKey} ${planRow.key.subIdentity}`
    : planRow.key.sourceRowKey;

const sameTestDecision = (test, decision) =>
  Boolean(decision) &&
  test.mappingState === decision.test.mappingState &&
  (test.testId || null) === (decision.test.testId || null);

// The draft row already holds this decision, answers included.
const holdsDecision = (test, decision) =>
  sameTestDecision(test, decision) &&
  test.results.every((result) => {
    const decided = decision.results.find(
      (candidate) => candidate.rawValue === result.rawValue,
    );
    return (
      result.mappingState === (decided?.mappingState || "UNRESOLVED") &&
      (result.resultOptionId || null) === (decided?.testResultId || null)
    );
  });

/**
 * What the operator changed since the last save, row by row, so Save can show
 * the effect before it is written.
 */
// A mapped side is named by its target, so pointing a row at another test
// reads as a change of test rather than "Mapped to Mapped".
const changeSide = (state, targetName) => ({
  state,
  name: state === "BOUND" ? targetName || null : null,
});

const listChanges = (savedTests = [], draftTests = []) => {
  const saved = new Map(savedTests.map((test) => [recordKey(test), test]));
  const changes = [];
  draftTests.forEach((test) => {
    const before = saved.get(recordKey(test));
    if (
      before &&
      (before.mappingState !== test.mappingState ||
        (before.testId || null) !== (test.testId || null) ||
        (before.componentId || null) !== (test.componentId || null) ||
        (before.callComponentId || null) !== (test.callComponentId || null))
    ) {
      changes.push({
        key: recordKey(test),
        label: recordLabel(test),
        from: changeSide(before.mappingState, before.selectedTest?.name),
        to: changeSide(test.mappingState, test.selectedTest?.name),
      });
    }
    const savedResults = new Map(
      (before?.results || []).map((result) => [result.rawValue, result]),
    );
    test.results.forEach((result) => {
      const beforeResult = savedResults.get(result.rawValue);
      if (
        beforeResult &&
        (beforeResult.mappingState !== result.mappingState ||
          (beforeResult.resultOptionId || null) !==
            (result.resultOptionId || null))
      ) {
        changes.push({
          key: `${recordKey(test)}:${result.rawValue}`,
          label: `${recordLabel(test)} / ${result.rawValue}`,
          from: changeSide(
            beforeResult.mappingState,
            beforeResult.selectedOption?.label,
          ),
          to: changeSide(result.mappingState, result.selectedOption?.label),
        });
      }
    });
  });
  return changes;
};

/**
 * The mapping editor page. Embedded (in setup's Verify step) it edits the named
 * analyzer's mapping without page chrome, shows only the assays the instrument
 * runs, and reports the mapping and whether it has unsaved edits.
 */
const AnalyzerTypeMappingEditor = ({
  analyzerId: embeddedAnalyzerId,
  embedded = false,
  onMappingChange,
} = {}) => {
  const intl = useIntl();
  const location = useLocation();
  const history = useHistory();
  const params = useParams();
  const profileId = params.profileId;
  const analyzerId = embeddedAnalyzerId || params.analyzerId;
  // On /analyzers/:id/adoption the editor reviews the analyzer's mapping on a
  // newer revision of its profile before saving it there.
  const adopting = /\/adoption$/.test(location.pathname);
  // With no analyzer the editor shows the defaults a new analyzer on the
  // profile revision would get. Each analyzer owns and edits its own mapping.
  const readOnly = !analyzerId;
  const query = useMemo(
    () => new URLSearchParams(location.search),
    [location.search],
  );
  // Embedded, the page's own query (setup's profile and revision) is not this
  // editor's: it always edits the analyzer's newest mapping.
  const revision = embedded ? null : Number(query.get("revision"));
  const returnTo = safeInternalPath(
    query.get("returnTo"),
    readOnly ? "/analyzers/types" : "/analyzers",
  );
  const returnDestination = { ...parsePath(returnTo), state: location.state };
  const focusTest = query.get("focusTest");
  const focusValue = query.get("focusValue");
  const [mapping, setMapping] = useState(null);
  const [typeSummary, setTypeSummary] = useState(null);
  const [draftTests, setDraftTests] = useState([]);
  const [catalogTests, setCatalogTests] = useState([]);
  const [resultOptionsByTest, setResultOptionsByTest] = useState({});
  const [componentsByTest, setComponentsByTest] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [applying, setApplying] = useState(false);
  const [reviewingSave, setReviewingSave] = useState(false);
  const [notification, setNotification] = useState(null);
  const [adoption, setAdoption] = useState(null);
  const loadedResultOptions = useRef(new Set());
  const loadedComponents = useRef(new Set());
  const focusedResultRow = useRef(null);
  const focusHandled = useRef(false);
  const routeIsValid =
    readOnly || adopting
      ? (readOnly ? Boolean(profileId) : true) &&
        Number.isInteger(revision) &&
        revision >= 1
      : true;
  const routeError = routeIsValid
    ? null
    : intl.formatMessage({ id: "analyzerType.mappingEditor.error.route" });

  const applyMapping = useCallback((nextMapping) => {
    setMapping(nextMapping);
    setDraftTests(cloneTests(nextMapping.tests));
    setDirty(false);
  }, []);

  const requestMapping = useCallback(() => {
    if (!routeIsValid) {
      return;
    }
    const load = readOnly
      ? (callback) => getAnalyzerTypeDefaults(profileId, revision, callback)
      : adopting
        ? (callback) =>
            getAnalyzerAdoption(analyzerId, revision, (response) => {
              if (!hasApiError(response) && response?.proposal) {
                setAdoption(response);
                callback(response.proposal);
              } else {
                callback(response);
              }
            })
        : (callback) => getAnalyzerMapping(analyzerId, callback);
    load((response) => {
      setLoading(false);
      if (hasApiError(response) || !Array.isArray(response.tests)) {
        // The heading already says the mapping could not load; the line under it
        // names the cause when the server gave one.
        setLoadError({ cause: analyzerErrorText(intl, response, null) });
        return;
      }
      loadedResultOptions.current = new Set();
      loadedComponents.current = new Set();
      focusHandled.current = false;
      setResultOptionsByTest({});
      setComponentsByTest({});
      applyMapping(response);
    });
    if (readOnly) {
      getAnalyzerTypeRevision(profileId, revision, (response) => {
        if (!hasApiError(response)) {
          setTypeSummary(response);
        }
      });
    }
    getAnalyzerMappingTests((response) => {
      if (Array.isArray(response)) {
        setCatalogTests(response);
      }
    });
  }, [
    adopting,
    applyMapping,
    intl,
    analyzerId,
    profileId,
    readOnly,
    revision,
    routeIsValid,
  ]);

  useEffect(() => {
    requestMapping();
  }, [requestMapping]);

  const retry = () => {
    if (!routeIsValid) {
      return;
    }
    setLoading(true);
    setLoadError(null);
    requestMapping();
  };

  useEffect(() => {
    const selectedTestIds = new Set(
      draftTests
        .filter((test) => test.mappingState === "BOUND" && test.testId)
        .map((test) => test.testId),
    );
    (adoption?.rows || [])
      .filter((planRow) => planRow.bucket === "CHANGED")
      .flatMap((planRow) => [planRow.current, planRow.newDefault])
      .filter((decision) => decision?.test.testId)
      .forEach((decision) => selectedTestIds.add(decision.test.testId));
    selectedTestIds.forEach((testId) => {
      if (loadedResultOptions.current.has(testId)) {
        return;
      }
      loadedResultOptions.current.add(testId);
      getAnalyzerMappingResultOptions(testId, (response) => {
        setResultOptionsByTest((current) => ({
          ...current,
          [testId]: Array.isArray(response) ? response : [],
        }));
      });
    });
  }, [draftTests, adoption]);

  useEffect(() => {
    draftTests
      .filter(
        (test) =>
          test.mappingState === "BOUND" &&
          test.testId &&
          (takesComponent(test) || takesCallComponent(test)),
      )
      .forEach(({ testId }) => {
        if (loadedComponents.current.has(testId)) {
          return;
        }
        loadedComponents.current.add(testId);
        getAnalyzerMappingComponents(testId, (response) => {
          setComponentsByTest((current) => ({
            ...current,
            [testId]: Array.isArray(response) ? response : [],
          }));
        });
      });
  }, [draftTests]);

  // A record moved to another test lands on that test's component with the
  // code the profile declares, once the test's components have loaded.
  useEffect(() => {
    if (
      !draftTests.some(
        (test) => test.placeByCode && componentsByTest[test.testId],
      )
    ) {
      return;
    }
    setDraftTests((current) =>
      current.map(({ placeByCode, ...test }) => {
        const components = componentsByTest[test.testId];
        if (!placeByCode || !components) {
          return placeByCode ? { ...test, placeByCode } : test;
        }
        return {
          ...test,
          componentId: takesComponent(test)
            ? componentWithCode(components, test.componentCode)
            : null,
          callComponentId: takesCallComponent(test)
            ? componentWithCode(components, test.callComponentCode)
            : null,
        };
      }),
    );
  }, [draftTests, componentsByTest]);

  useEffect(() => {
    if (
      focusHandled.current ||
      !focusTest ||
      !focusValue ||
      !focusedResultRow.current
    ) {
      return;
    }
    const control = focusedResultRow.current.querySelector('[role="combobox"]');
    if (!control) {
      return;
    }
    focusedResultRow.current.scrollIntoView?.({ block: "center" });
    control.focus();
    focusHandled.current = true;
  }, [draftTests, focusTest, focusValue, resultOptionsByTest]);

  const updateTest = (key, transform) => {
    setDraftTests((current) =>
      current.map((test) => (recordKey(test) === key ? transform(test) : test)),
    );
    setDirty(true);
    setNotification(null);
  };

  const selectTest = (key, selectedTest) => {
    updateTest(key, (test) => {
      const changed = test.testId !== selectedTest?.id;
      return {
        ...test,
        mappingState: selectedTest ? "BOUND" : "UNRESOLVED",
        testId: selectedTest?.id || null,
        componentId: changed ? null : test.componentId || null,
        callComponentId: changed ? null : test.callComponentId || null,
        placeByCode: changed ? Boolean(selectedTest) : test.placeByCode,
        selectedTest: selectedTest || null,
        results: changed ? unresolvedResults(test.results) : test.results,
      };
    });
  };

  // Answers belong to the component a record lands on, so moving the record
  // to another component clears them.
  const selectComponent = (key, field, component) => {
    updateTest(key, (test) => {
      const next = { ...test, [field]: component?.id || null };
      return answerComponentId(next) === answerComponentId(test)
        ? next
        : { ...next, results: unresolvedResults(test.results) };
    });
  };

  // Excluding keeps the row's selections and each result's own state, so that
  // un-excluding before Save restores them. A saved excluded row has no target.
  const excludeTest = (key, checked) => {
    updateTest(key, (test) => ({
      ...test,
      mappingState: checked ? "EXCLUDED" : test.testId ? "BOUND" : "UNRESOLVED",
      results: test.results.map(({ stateBeforeExclusion, ...result }) =>
        checked
          ? {
              ...result,
              stateBeforeExclusion: stateBeforeExclusion || result.mappingState,
              mappingState: "EXCLUDED",
            }
          : {
              ...result,
              mappingState:
                stateBeforeExclusion ||
                (result.resultOptionId ? "BOUND" : "UNRESOLVED"),
            },
      ),
    }));
  };

  const selectResult = (key, rawValue, selectedOption) => {
    updateTest(key, (test) => ({
      ...test,
      results: test.results.map((result) =>
        result.rawValue === rawValue
          ? {
              ...result,
              mappingState: selectedOption ? "BOUND" : "UNRESOLVED",
              resultOptionId: selectedOption?.id || null,
              selectedOption: selectedOption || null,
            }
          : result,
      ),
    }));
  };

  const excludeResult = (key, rawValue, checked) => {
    updateTest(key, (test) => ({
      ...test,
      results: test.results.map((result) =>
        result.rawValue === rawValue
          ? {
              ...result,
              mappingState: checked
                ? "EXCLUDED"
                : result.resultOptionId
                  ? "BOUND"
                  : "UNRESOLVED",
            }
          : result,
      ),
    }));
  };

  // Takes one side of a changed row: the operator's current decision or the
  // newer revision's default, answers included.
  const takeDecision = (key, decision) => {
    const options = resultOptionsByTest[decision.test.testId] || [];
    updateTest(key, (test) => ({
      ...test,
      mappingState: decision.test.mappingState,
      testId: decision.test.testId || null,
      componentId: decision.test.componentId || null,
      callComponentId: decision.test.callComponentId || null,
      placeByCode: false,
      selectedTest:
        catalogTests.find(
          (candidate) => candidate.id === decision.test.testId,
        ) || null,
      results: test.results.map((result) => {
        const taken = decision.results.find(
          (candidate) => candidate.rawValue === result.rawValue,
        );
        return {
          ...result,
          mappingState: taken?.mappingState || "UNRESOLVED",
          resultOptionId: taken?.testResultId || null,
          selectedOption:
            options.find((option) => option.id === taken?.testResultId) || null,
        };
      }),
    }));
  };

  const updatePayload = useMemo(
    () => ({
      baseMappingFingerprint: mapping?.mappingFingerprint || null,
      tests: draftTests.map((test) => ({
        sourceRowKey: test.sourceRowKey,
        subIdentity: test.subIdentity || "",
        mappingState: test.mappingState,
        testId: test.mappingState === "BOUND" ? test.testId : null,
        componentId:
          test.mappingState === "BOUND" ? test.componentId || null : null,
        callComponentId:
          test.mappingState === "BOUND" ? test.callComponentId || null : null,
      })),
      results: draftTests.flatMap((test) =>
        test.results.map((result) => ({
          sourceRowKey: test.sourceRowKey,
          subIdentity: test.subIdentity || "",
          rawValue: result.rawValue,
          mappingState: result.mappingState,
          testResultId:
            result.mappingState === "BOUND" ? result.resultOptionId : null,
        })),
      ),
    }),
    [draftTests, mapping?.mappingFingerprint],
  );

  const changes = useMemo(
    () => listChanges(mapping?.tests, draftTests),
    [mapping?.tests, draftTests],
  );

  const adoptionRows = useMemo(
    () =>
      new Map(
        (adoption?.rows || []).map((planRow) => [planRowKey(planRow), planRow]),
      ),
    [adoption],
  );

  // Adoption is refused while a dropped record has held results, or while a
  // record still points at a test that is no longer active.
  const adoptionBlocked = (adoption?.rows || []).some(
    (planRow) =>
      planRow.blockReason === "HELD_RESULTS" ||
      (planRow.blockReason === "INACTIVE_TEST" &&
        draftTests.some(
          (test) =>
            recordKey(test) === planRowKey(planRow) &&
            sameTestDecision(test, planRow.current),
        )),
  );

  const confirmable = useMemo(
    () =>
      draftTests.length > 0 &&
      draftTests.every(
        (test) =>
          (test.mappingState !== "BOUND" || Boolean(test.selectedTest)) &&
          test.results.every(
            (result) =>
              result.mappingState !== "BOUND" || Boolean(result.selectedOption),
          ),
      ),
    [draftTests],
  );

  // Embedded in Verify, only the assays this instrument runs are mapped here.
  const visibleTests = useMemo(() => {
    if (!embedded) {
      return draftTests;
    }
    const enabledCodes = new Set(
      draftTests
        .filter((test) => !test.subIdentity && test.enabled !== false)
        .map((test) => test.sourceRowKey),
    );
    return draftTests.filter((test) => enabledCodes.has(test.sourceRowKey));
  }, [draftTests, embedded]);

  useEffect(() => {
    if (onMappingChange && mapping) {
      onMappingChange(mapping, dirty);
    }
  }, [mapping, dirty, onMappingChange]);

  const counts = useMemo(() => {
    const results = visibleTests.flatMap((test) => test.results);
    return {
      testsBound: visibleTests.filter((test) => test.mappingState === "BOUND")
        .length,
      testsTotal: visibleTests.length,
      unresolved:
        visibleTests.filter((test) => test.mappingState === "UNRESOLVED")
          .length +
        results.filter((result) => result.mappingState === "UNRESOLVED").length,
      resultsBound: results.filter((result) => result.mappingState === "BOUND")
        .length,
      resultsTotal: results.length,
    };
  }, [visibleTests]);

  const save = () => {
    if (!dirty || saving) {
      return;
    }
    setSaving(true);
    saveAnalyzerMapping(analyzerId, updatePayload, (response) => {
      setSaving(false);
      setReviewingSave(false);
      if (hasApiError(response) || !Array.isArray(response.tests)) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "analyzerType.mappingEditor.error.save",
          }),
          subtitle: analyzerErrorText(intl, response, null),
        });
        return;
      }
      applyMapping(response);
      setNotification({
        kind: "success",
        title: intl.formatMessage({
          id: "analyzerType.mappingEditor.saved",
        }),
        subtitle: "",
      });
    });
  };

  const saveAdoption = () => {
    if (saving || adoptionBlocked) {
      return;
    }
    setSaving(true);
    adoptAnalyzerRevision(
      analyzerId,
      revision,
      { tests: updatePayload.tests, results: updatePayload.results },
      (response) => {
        setSaving(false);
        if (hasApiError(response) || !response?.mappingId) {
          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "analyzerType.adoption.error.save",
            }),
            subtitle: analyzerErrorText(intl, response, null),
          });
          return;
        }
        history.push({
          pathname: `/analyzers/${analyzerId}/mapping`,
          state: { adoptedRevision: revision },
        });
      },
    );
  };

  const confirm = () => {
    if (
      readOnly ||
      !confirmable ||
      dirty ||
      confirming ||
      !mapping?.mappingFingerprint
    ) {
      return;
    }
    const confirmedRows = [];
    const excludedRows = [];
    draftTests.forEach((test) => {
      if (test.mappingState !== "UNRESOLVED") {
        const destination =
          test.mappingState === "BOUND" ? confirmedRows : excludedRows;
        destination.push({
          sourceRowKey: test.sourceRowKey,
          subIdentity: test.subIdentity || "",
          rawValue: null,
        });
      }
      test.results.forEach((result) => {
        if (result.mappingState === "UNRESOLVED") return;
        const resultDestination =
          result.mappingState === "BOUND" ? confirmedRows : excludedRows;
        resultDestination.push({
          sourceRowKey: test.sourceRowKey,
          subIdentity: test.subIdentity || "",
          rawValue: result.rawValue,
        });
      });
    });
    setConfirming(true);
    confirmAnalyzerMapping(
      analyzerId,
      {
        baseMappingFingerprint: mapping.mappingFingerprint,
        recognitionFingerprint:
          mapping.controlRecognition.recognitionFingerprint,
        confirmedRows,
        excludedRows,
      },
      (response) => {
        setConfirming(false);
        if (hasApiError(response)) {
          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "analyzerType.mappingEditor.error.confirm",
            }),
            subtitle: analyzerErrorText(intl, response, null),
          });
          return;
        }
        setMapping((current) => ({ ...current, confirmation: response }));
        setNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "analyzerType.mappingEditor.confirmed",
          }),
          subtitle: "",
        });
      },
    );
  };

  const applyToAnalyzer = () => {
    if (
      readOnly ||
      dirty ||
      saving ||
      applying ||
      !mapping?.mappingId ||
      !mapping?.mappingFingerprint ||
      mapping?.confirmation?.state !== "CURRENT"
    ) {
      return;
    }
    setApplying(true);
    applyAnalyzerMapping(
      analyzerId,
      {
        mappingId: mapping.mappingId,
        revision: mapping.mappingRevision,
        mappingFingerprint: mapping.mappingFingerprint,
      },
      (response) => {
        setApplying(false);
        const applied =
          !hasApiError(response) && String(response?.id) === String(analyzerId);
        setNotification({
          kind: applied ? "success" : "error",
          title: intl.formatMessage({
            id: applied
              ? "analyzerType.mappingEditor.appliedToAnalyzer"
              : "analyzerType.mappingEditor.error.applyToAnalyzer",
          }),
          subtitle: applied ? "" : analyzerErrorText(intl, response, null),
        });
      },
    );
  };

  const mappingMatchesRoute = readOnly
    ? mapping?.profileId === profileId && mapping?.profileRevision === revision
    : String(mapping?.analyzerId) === String(analyzerId);
  const currentTypeSummary =
    readOnly &&
    typeSummary?.profileId === profileId &&
    typeSummary?.revision === revision
      ? typeSummary
      : null;

  if (loading || (!routeError && !loadError && !mappingMatchesRoute)) {
    return (
      <div className="analyzer-type-mapping__loading">
        <Loading
          withOverlay={false}
          description={intl.formatMessage({
            id: "analyzerType.mappingEditor.loading",
          })}
        />
      </div>
    );
  }

  if (routeError || loadError || !mapping) {
    return (
      <Grid fullWidth className="analyzer-type-mapping">
        <Column lg={16} md={8} sm={4}>
          <ActionableNotification
            inline
            kind="error"
            lowContrast
            title={intl.formatMessage({
              id: "analyzerType.mappingEditor.error.load",
            })}
            subtitle={routeError || loadError?.cause || ""}
            actionButtonLabel={intl.formatMessage({
              id: "common.retry",
            })}
            onActionButtonClick={retry}
          />
        </Column>
      </Grid>
    );
  }

  const heading = intl.formatMessage(
    {
      id: adopting
        ? "analyzerType.adoption.title"
        : "analyzerType.mappingEditor.title",
    },
    { name: mapping.displayName, revision },
  );
  const currentUrl = `${location.pathname}${location.search}`;
  const duplicateParams = new URLSearchParams({
    action: "duplicate",
    profile: mapping.profileId,
    revision: String(mapping.profileRevision),
    returnTo: currentUrl,
  });
  const confirmation = mapping.confirmation || { state: "UNCONFIRMED" };

  const decisionName = (decision) =>
    decision?.test.mappingState === "BOUND"
      ? catalogTests.find((candidate) => candidate.id === decision.test.testId)
          ?.name || decision.test.testId
      : intl.formatMessage({ id: stateMessageId(decision?.test.mappingState) });

  const decisionAnswers = (decision) =>
    decision.results
      .filter((result) => result.mappingState === "BOUND")
      .map(
        (result) =>
          `${result.rawValue} → ${
            (resultOptionsByTest[decision.test.testId] || []).find(
              (option) => option.id === result.testResultId,
            )?.label || result.testResultId
          }`,
      )
      .join(", ");

  const renderAdoptionDetail = (test, planRow) => {
    const key = recordKey(test);
    if (
      planRow.blockReason === "INACTIVE_TEST" &&
      sameTestDecision(test, planRow.current)
    ) {
      return (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzerType.adoption.blocked.inactiveTest",
          })}
        />
      );
    }
    if (planRow.bucket !== "CHANGED") {
      return null;
    }
    return (
      <div
        className="analyzer-type-mapping__adoption-comparison"
        data-testid={`adoption-comparison-${key}`}
      >
        {[
          ["current", planRow.current, "keepCurrent"],
          ["newDefault", planRow.newDefault, "useNewDefault"],
        ]
          .filter(([, decision]) => decision)
          .map(([side, decision, action]) => (
            <div key={side}>
              <strong>
                <FormattedMessage
                  id={`analyzerType.adoption.changed.${side}`}
                  values={{ name: decisionName(decision) }}
                />
              </strong>
              {decisionAnswers(decision) && (
                <span>{decisionAnswers(decision)}</span>
              )}
              <Button
                kind="ghost"
                size="sm"
                disabled={holdsDecision(test, decision)}
                onClick={() => takeDecision(key, decision)}
              >
                <FormattedMessage id={`analyzerType.adoption.${action}`} />
              </Button>
            </div>
          ))}
      </div>
    );
  };

  const renderAdoptionSections = () => {
    const bucketOf = (test) =>
      adoptionRows.get(recordKey(test))?.bucket || "UNCHANGED";
    const outsideProposal = adoption.rows.filter(
      (planRow) =>
        !draftTests.some((test) => recordKey(test) === planRowKey(planRow)),
    );
    const held = outsideProposal.filter(
      (planRow) => planRow.blockReason === "HELD_RESULTS",
    );
    const retired = outsideProposal.filter(
      (planRow) => planRow.bucket === "RETIRED",
    );
    const bucketHeading = (bucket, id, values) => (
      <div className="analyzer-type-mapping__section-heading">
        <div>
          <h2 id={id}>
            <FormattedMessage id={`analyzerType.adoption.bucket.${bucket}`} />
          </h2>
          <p>
            <FormattedMessage
              id={`analyzerType.adoption.bucket.${bucket}.help`}
              values={values}
            />
          </p>
        </div>
      </div>
    );
    return (
      <>
        {ADOPTION_BUCKETS.map((bucket) => {
          const rows = draftTests.filter((test) => bucketOf(test) === bucket);
          const heldHere = bucket === "BLOCKED" ? held : [];
          if (rows.length === 0 && heldHere.length === 0) {
            return null;
          }
          const id = `adoption-bucket-${bucket.toLowerCase()}`;
          return (
            <section
              key={bucket}
              aria-labelledby={id}
              className="analyzer-type-mapping__bucket"
            >
              {bucketHeading(bucket.toLowerCase(), id)}
              {heldHere.map((planRow) => (
                <InlineNotification
                  key={planRowKey(planRow)}
                  kind="error"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage(
                    { id: "analyzerType.adoption.blocked.heldResults" },
                    {
                      code: planRowKey(planRow),
                      revision: adoption.fromRevision,
                    },
                  )}
                />
              ))}
              {heldHere.length > 0 && (
                <CarbonLink
                  as={Link}
                  to={`/AnalyzerResults?id=${encodeURIComponent(analyzerId)}`}
                >
                  <FormattedMessage id="analyzerType.adoption.blocked.reviewHeld" />
                </CarbonLink>
              )}
              {rows.length > 0 && (
                <Accordion align="start">{rows.map(renderRow)}</Accordion>
              )}
            </section>
          );
        })}
        {retired.length > 0 && (
          <section
            aria-labelledby="adoption-bucket-retired"
            className="analyzer-type-mapping__bucket"
          >
            {bucketHeading("retired", "adoption-bucket-retired", {
              revision: adoption.toRevision,
            })}
            <ul className="analyzer-type-mapping__retired">
              {retired.map((planRow) => (
                <li key={planRowKey(planRow)}>
                  <strong>{planRowKey(planRow)}</strong>
                  <span>{decisionName(planRow.current)}</span>
                </li>
              ))}
            </ul>
          </section>
        )}
      </>
    );
  };

  const renderRow = (test) => {
    const selectedTest =
      catalogTests.find((candidate) => candidate.id === test.testId) ||
      test.selectedTest ||
      null;
    const answerComponent = answerComponentId(test);
    const resultOptions = test.testId
      ? resultOptionsByTest[test.testId]?.filter(
          (option) =>
            !answerComponent || option.componentId === answerComponent,
        )
      : undefined;
    const components = componentsByTest[test.testId] || [];
    const key = recordKey(test);
    const label = recordLabel(test);
    const planRow = adopting ? adoptionRows.get(key) : null;
    return (
      <AccordionItem
        key={key}
        open={
          planRow?.bucket === "CHANGED" ||
          planRow?.bucket === "BLOCKED" ||
          test.mappingState === "UNRESOLVED" ||
          test.results.some((result) => result.mappingState === "UNRESOLVED") ||
          test.rawCode === focusTest
        }
        title={
          <div className="analyzer-type-mapping__row-title">
            <strong>{label}</strong>
            <span>{test.testNameHint}</span>
            <Tag
              type={stateTagType(
                test.results.some(
                  (result) => result.mappingState === "UNRESOLVED",
                )
                  ? "UNRESOLVED"
                  : test.mappingState,
              )}
              size="sm"
            >
              <FormattedMessage
                id={stateMessageId(
                  test.results.some(
                    (result) => result.mappingState === "UNRESOLVED",
                  )
                    ? "UNRESOLVED"
                    : test.mappingState,
                )}
              />
            </Tag>
            {test.origin === "OVERRIDE" && (
              <Tag type="blue" size="sm">
                <FormattedMessage id="analyzerType.mappingEditor.origin.override" />
              </Tag>
            )}
          </div>
        }
      >
        <div
          className="analyzer-type-mapping__row"
          data-testid="analyzer-type-mapping-row"
        >
          {planRow && renderAdoptionDetail(test, planRow)}
          <div className="analyzer-type-mapping__source">
            <div>
              <span className="analyzer-type-mapping__label">
                <FormattedMessage id="analyzerType.mappingEditor.sourceCode" />
              </span>
              <strong>{label}</strong>
            </div>
            {test.loinc && (
              <div>
                <span className="analyzer-type-mapping__label">LOINC</span>
                <strong>{test.loinc}</strong>
              </div>
            )}
            {test.normalizedCoding && (
              <div>
                <span className="analyzer-type-mapping__label">
                  <FormattedMessage id="analyzerType.mappingEditor.normalized" />
                </span>
                <strong>
                  {test.normalizedCoding.display || test.normalizedCoding.code}
                </strong>
              </div>
            )}
            {(test.aliases || []).map((alias) => (
              <span className="analyzer-type-mapping__alias" key={alias}>
                <FormattedMessage
                  id="analyzerType.mappingEditor.alias"
                  values={{ alias }}
                />
              </span>
            ))}
          </div>

          <div className="analyzer-type-mapping__decision">
            {test.mappingState === "UNRESOLVED" && test.unresolvedReason && (
              <p className="analyzer-type-mapping__reason">
                <FormattedMessage
                  id={`analyzerType.mappingEditor.reason.test.${test.unresolvedReason.toLowerCase()}`}
                />
              </p>
            )}
            <ComboBox
              id={`analyzer-test-${key}`}
              titleText={intl.formatMessage(
                { id: "analyzerType.mappingEditor.testPicker" },
                { code: label },
              )}
              placeholder={intl.formatMessage({
                id: "analyzerType.mappingEditor.testPicker.placeholder",
              })}
              items={catalogTests}
              itemToString={testItemText}
              shouldFilterItem={includesComboBoxText}
              // An excluded row keeps showing its prior choice: clearing
              // a controlled ComboBox fires onChange(null).
              selectedItem={
                test.mappingState === "UNRESOLVED" ? null : selectedTest
              }
              disabled={readOnly || test.mappingState === "EXCLUDED"}
              onChange={({ selectedItem }) => selectTest(key, selectedItem)}
            />
            {test.mappingState === "BOUND" && takesComponent(test) && (
              <Dropdown
                id={`analyzer-component-${key}`}
                titleText={intl.formatMessage(
                  {
                    id: "analyzerType.mappingEditor.componentPicker",
                  },
                  { code: label },
                )}
                label={intl.formatMessage({
                  id: "analyzerType.mappingEditor.componentPicker.placeholder",
                })}
                items={components}
                itemToString={componentItemText}
                selectedItem={
                  components.find(
                    (component) => component.id === test.componentId,
                  ) || null
                }
                disabled={readOnly}
                onChange={({ selectedItem }) =>
                  selectComponent(key, "componentId", selectedItem)
                }
              />
            )}
            {test.mappingState === "BOUND" && takesCallComponent(test) && (
              <Dropdown
                id={`analyzer-call-component-${key}`}
                titleText={intl.formatMessage(
                  {
                    id: "analyzerType.mappingEditor.callComponentPicker",
                  },
                  { code: label },
                )}
                label={intl.formatMessage({
                  id: "analyzerType.mappingEditor.componentPicker.placeholder",
                })}
                items={components}
                itemToString={componentItemText}
                selectedItem={
                  components.find(
                    (component) => component.id === test.callComponentId,
                  ) || null
                }
                disabled={readOnly}
                onChange={({ selectedItem }) =>
                  selectComponent(key, "callComponentId", selectedItem)
                }
              />
            )}
            {!readOnly &&
              test.suggestedTest &&
              test.mappingState === "UNRESOLVED" && (
                <div className="analyzer-type-mapping__suggestion">
                  <span>
                    <FormattedMessage
                      id="analyzerType.mappingEditor.suggestion"
                      values={{ name: test.suggestedTest.name }}
                    />
                  </span>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => selectTest(key, test.suggestedTest)}
                  >
                    <FormattedMessage id="analyzerType.mappingEditor.useSuggestion" />
                  </Button>
                </div>
              )}
            <Checkbox
              id={`exclude-test-${key}`}
              aria-label={intl.formatMessage(
                { id: "analyzerType.mappingEditor.excludeTest" },
                { code: label },
              )}
              labelText={intl.formatMessage(
                { id: "analyzerType.mappingEditor.excludeTest" },
                { code: label },
              )}
              checked={test.mappingState === "EXCLUDED"}
              disabled={readOnly}
              onChange={(_, state) => excludeTest(key, state.checked)}
            />
          </div>

          {test.results.length > 0 && test.mappingState !== "EXCLUDED" && (
            <div className="analyzer-type-mapping__results">
              <h3>
                <FormattedMessage
                  id="analyzerType.mappingEditor.results.heading"
                  values={{
                    name: selectedTest?.name || label,
                  }}
                />
              </h3>
              {test.mappingState !== "BOUND" ? (
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage({
                    id: "analyzerType.mappingEditor.results.testFirst",
                  })}
                />
              ) : resultOptions === undefined ? (
                <Loading
                  small
                  withOverlay={false}
                  description={intl.formatMessage({
                    id: "analyzerType.mappingEditor.results.loading",
                  })}
                />
              ) : resultOptions.length === 0 ? (
                <div className="analyzer-type-mapping__catalog-action">
                  {test.results.map((result) => (
                    <div key={result.rawValue}>
                      <code>{result.rawValue}</code>
                    </div>
                  ))}
                  <InlineNotification
                    kind="warning"
                    lowContrast
                    hideCloseButton
                    title={intl.formatMessage({
                      id: "analyzerType.mappingEditor.results.empty",
                    })}
                  />
                  <CarbonLink
                    as={Link}
                    to={`/MasterListsPage/TestCatalogEditor/${test.testId}/sample-results?returnTo=${encodeURIComponent(
                      currentUrl,
                    )}`}
                  >
                    <FormattedMessage id="analyzerType.mappingEditor.results.openCatalog" />
                  </CarbonLink>
                </div>
              ) : (
                test.results.map((result) => {
                  const selectedOption =
                    resultOptions.find(
                      (option) => option.id === result.resultOptionId,
                    ) ||
                    result.selectedOption ||
                    null;
                  return (
                    <div
                      className={
                        result.translationOf
                          ? "analyzer-type-mapping__result-row analyzer-type-mapping__result-row--translation"
                          : "analyzer-type-mapping__result-row"
                      }
                      key={`${key}:${result.rawValue}`}
                      ref={
                        test.rawCode === focusTest &&
                        result.rawValue === focusValue
                          ? focusedResultRow
                          : null
                      }
                    >
                      <div className="analyzer-type-mapping__result-source">
                        <code>{result.rawValue}</code>
                        {result.mappingState === "UNRESOLVED" &&
                          result.unresolvedReason && (
                            <span className="analyzer-type-mapping__reason">
                              <FormattedMessage
                                id={`analyzerType.mappingEditor.reason.result.${result.unresolvedReason.toLowerCase()}`}
                              />
                            </span>
                          )}
                        {result.translationOf && (
                          <span className="analyzer-type-mapping__translation-of">
                            <FormattedMessage
                              id="analyzerType.mappingEditor.translationOf"
                              values={{
                                value: result.translationOf,
                              }}
                            />
                          </span>
                        )}
                        {result.origin === "OVERRIDE" && (
                          <Tag type="blue" size="sm">
                            <FormattedMessage id="analyzerType.mappingEditor.origin.override" />
                          </Tag>
                        )}
                        {result.observed && (
                          <Tag type="warm-gray" size="sm">
                            <FormattedMessage id="analyzerType.mappingEditor.observed" />
                          </Tag>
                        )}
                      </div>
                      <Dropdown
                        id={`result-${key}-${result.rawValue.replace(/[^a-z0-9]/gi, "-")}`}
                        titleText={intl.formatMessage(
                          {
                            id: "analyzerType.mappingEditor.resultPicker",
                          },
                          { value: result.rawValue },
                        )}
                        label={intl.formatMessage({
                          id: "analyzerType.mappingEditor.resultPicker.placeholder",
                        })}
                        items={resultOptions}
                        itemToString={resultItemText}
                        selectedItem={
                          result.mappingState === "UNRESOLVED"
                            ? null
                            : selectedOption
                        }
                        disabled={
                          readOnly || result.mappingState === "EXCLUDED"
                        }
                        onChange={({ selectedItem }) =>
                          selectResult(key, result.rawValue, selectedItem)
                        }
                      />
                      <Checkbox
                        id={`exclude-result-${key}-${result.rawValue.replace(/[^a-z0-9]/gi, "-")}`}
                        aria-label={intl.formatMessage(
                          {
                            id: "analyzerType.mappingEditor.excludeResult",
                          },
                          { value: result.rawValue },
                        )}
                        labelText={intl.formatMessage({
                          id: "analyzerType.mappingEditor.excludeResult.short",
                        })}
                        checked={result.mappingState === "EXCLUDED"}
                        disabled={readOnly}
                        onChange={(_, state) =>
                          excludeResult(key, result.rawValue, state.checked)
                        }
                      />
                    </div>
                  );
                })
              )}
            </div>
          )}
        </div>
      </AccordionItem>
    );
  };

  // Embedded in setup, the page chrome and the way out belong to the wizard.
  const frame = (content) =>
    embedded ? (
      <div className="analyzer-type-mapping analyzer-type-mapping--embedded">
        {content}
      </div>
    ) : (
      <Grid fullWidth className="analyzer-type-mapping">
        <Column lg={16} md={8} sm={4}>
          {content}
        </Column>
      </Grid>
    );

  return (
    <>
      {!embedded && (
        <PageBreadCrumb
          breadcrumbs={
            readOnly
              ? [
                  { label: "home.label", link: "/" },
                  { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
                  { label: "analyzerType.page.title", link: returnDestination },
                  { label: heading, isCurrentPage: true },
                ]
              : [
                  { label: "home.label", link: "/" },
                  { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
                  { label: heading, isCurrentPage: true },
                ]
          }
        />
      )}
      {frame(
        <>
          {!embedded && (
            <div className="analyzer-type-mapping__heading">
              <div>
                <h1>{heading}</h1>
                <p>
                  <FormattedMessage
                    id="analyzerType.mappingEditor.subtitle"
                    values={{
                      protocol: mapping.protocol,
                      revision: mapping.profileRevision,
                    }}
                  />
                </p>
              </div>
              <div className="analyzer-type-mapping__heading-actions">
                <Button
                  as={Link}
                  kind="ghost"
                  renderIcon={ArrowLeft}
                  to={returnDestination}
                >
                  <FormattedMessage id="analyzerType.mappingEditor.return" />
                </Button>
                {!readOnly && !adopting && (
                  <Button
                    kind="primary"
                    disabled={
                      dirty ||
                      saving ||
                      applying ||
                      !mapping.mappingId ||
                      !mapping.mappingFingerprint ||
                      confirmation.state !== "CURRENT"
                    }
                    onClick={applyToAnalyzer}
                  >
                    <FormattedMessage id="analyzerType.mappingEditor.applyToAnalyzer" />
                  </Button>
                )}
                {readOnly && (
                  <Button
                    as={Link}
                    kind="secondary"
                    renderIcon={Copy}
                    to={`/analyzers/types?${duplicateParams.toString()}`}
                  >
                    <FormattedMessage id="analyzerType.button.duplicate" />
                  </Button>
                )}
              </div>
            </div>
          )}

          {!embedded && (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              className="analyzer-type-mapping__notice"
              title={intl.formatMessage(
                {
                  id: readOnly
                    ? "analyzerType.mappingEditor.defaults.title"
                    : adopting
                      ? "analyzerType.adoption.notice.title"
                      : "analyzerType.mappingEditor.own.title",
                },
                { revision },
              )}
              subtitle={intl.formatMessage(
                {
                  id: readOnly
                    ? "analyzerType.mappingEditor.defaults.subtitle"
                    : adopting
                      ? "analyzerType.adoption.notice.subtitle"
                      : "analyzerType.mappingEditor.own.subtitle",
                },
                { from: adoption?.fromRevision, revision },
              )}
            />
          )}
          {!embedded && !adopting && location.state?.adoptedRevision && (
            <InlineNotification
              kind="success"
              lowContrast
              className="analyzer-type-mapping__notice"
              title={intl.formatMessage(
                { id: "analyzerType.adoption.adopted" },
                { revision: location.state.adoptedRevision },
              )}
            />
          )}
          {readOnly && (
            <AffectedAnalyzerList
              analyzers={currentTypeSummary?.affectedAnalyzers || []}
              revision={currentTypeSummary?.revision}
            />
          )}

          {notification && (
            <InlineNotification
              kind={notification.kind}
              lowContrast
              className="analyzer-type-mapping__notice"
              title={notification.title}
              subtitle={notification.subtitle}
              onCloseButtonClick={() => setNotification(null)}
            />
          )}

          <section
            className={
              adopting
                ? "analyzer-type-mapping__summary analyzer-type-mapping__summary--two"
                : "analyzer-type-mapping__summary"
            }
            aria-label={intl.formatMessage({
              id: "analyzerType.mappingEditor.summary",
            })}
          >
            <div>
              <span>
                <FormattedMessage id="analyzerType.mappingEditor.tests" />
              </span>
              <strong>{`${counts.testsBound} / ${counts.testsTotal}`}</strong>
            </div>
            <div>
              <span>
                <FormattedMessage id="analyzerType.mappingEditor.results" />
              </span>
              <strong>{`${counts.resultsBound} / ${counts.resultsTotal}`}</strong>
            </div>
            {!adopting && (
              <div>
                <span>
                  <FormattedMessage id="analyzerType.mappingEditor.confirmation" />
                </span>
                <Tag
                  type={
                    confirmation.state === "CURRENT" ? "green" : "warm-gray"
                  }
                >
                  <FormattedMessage
                    id={`analyzerType.mappingEditor.confirmation.summary.${confirmation.state.toLowerCase()}`}
                  />
                </Tag>
              </div>
            )}
          </section>

          {adopting ? (
            renderAdoptionSections()
          ) : (
            <section aria-labelledby="analyzer-type-test-mappings">
              <div className="analyzer-type-mapping__section-heading">
                <div>
                  <h2 id="analyzer-type-test-mappings">
                    <FormattedMessage id="analyzerType.mappingEditor.tests.heading" />
                  </h2>
                  <p>
                    <FormattedMessage id="analyzerType.mappingEditor.tests.help" />
                  </p>
                </div>
              </div>
              <Accordion align="start">{visibleTests.map(renderRow)}</Accordion>
            </section>
          )}

          <section
            className="analyzer-type-mapping__recognition"
            aria-labelledby="analyzer-type-recognition"
          >
            <div className="analyzer-type-mapping__section-heading">
              <div>
                <h2 id="analyzer-type-recognition">
                  <FormattedMessage id="analyzerType.mappingEditor.recognition.heading" />
                </h2>
                <p>
                  {formatRecognitionMode(
                    intl,
                    mapping.controlRecognition.mode,
                    mapping.controlRecognition.conditions,
                  )}
                </p>
              </div>
              <Tag type="blue">
                {formatRecognitionMode(
                  intl,
                  mapping.controlRecognition.mode,
                  mapping.controlRecognition.conditions,
                )}
              </Tag>
            </div>
            {mapping.controlRecognition.mode === "NONE" ? (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "analyzerType.mappingEditor.recognition.none",
                })}
              />
            ) : mapping.controlRecognition.conditions.length === 0 ? (
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "analyzerType.recognition.mode.unconfigured",
                })}
              />
            ) : (
              <ul>
                {mapping.controlRecognition.conditions.map((condition) => (
                  <li key={condition.key}>
                    <strong>
                      {formatRecognitionCondition(intl, condition)}
                    </strong>
                    {(condition.controlLevel || condition.controlType) && (
                      <span>
                        {[condition.controlLevel, condition.controlType]
                          .filter(Boolean)
                          .join(" · ")}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>

          {confirmation.state === "CURRENT" && (
            <InlineNotification
              kind="success"
              lowContrast
              hideCloseButton
              className="analyzer-type-mapping__notice"
              title={intl.formatMessage({
                id: "analyzerType.mappingEditor.confirmation.current",
              })}
              subtitle={intl.formatMessage(
                { id: "analyzerType.mappingEditor.confirmation.by" },
                {
                  actor: confirmation.confirmedByDisplayName,
                  date: intl.formatDate(confirmation.confirmedAt, {
                    year: "numeric",
                    month: "short",
                    day: "numeric",
                    hour: "numeric",
                    minute: "2-digit",
                  }),
                },
              )}
            />
          )}
          {confirmation.state === "STALE" && (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              className="analyzer-type-mapping__notice"
              title={intl.formatMessage({
                id: "analyzerType.mappingEditor.confirmation.stale",
              })}
            />
          )}

          {counts.unresolved > 0 && (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={intl.formatMessage(
                { id: "analyzerType.mappingEditor.partial" },
                { count: counts.unresolved },
              )}
            />
          )}
          {adopting && (
            <div className="analyzer-type-mapping__actions">
              <div />
              <div>
                <Button
                  renderIcon={Save}
                  disabled={saving || adoptionBlocked}
                  onClick={saveAdoption}
                >
                  <FormattedMessage
                    id="analyzerType.adoption.save"
                    values={{ revision }}
                  />
                </Button>
              </div>
            </div>
          )}
          {!readOnly && !adopting && (
            <div className="analyzer-type-mapping__actions">
              <div>
                {dirty && (
                  <span>
                    <FormattedMessage id="analyzerType.mappingEditor.unsaved" />
                  </span>
                )}
                {!confirmable && !dirty && (
                  <span>
                    <FormattedMessage id="analyzerType.mappingEditor.incomplete" />
                  </span>
                )}
              </div>
              <div>
                <Button
                  kind="secondary"
                  renderIcon={Save}
                  disabled={!dirty || saving}
                  onClick={() => setReviewingSave(true)}
                >
                  <FormattedMessage id="analyzerType.mappingEditor.save" />
                </Button>
                <Button
                  disabled={!confirmable || dirty || confirming}
                  onClick={confirm}
                >
                  <FormattedMessage id="analyzerType.mappingEditor.confirm" />
                </Button>
              </div>
            </div>
          )}
          <Modal
            open={reviewingSave}
            size="sm"
            modalHeading={intl.formatMessage({
              id: "analyzerType.mappingEditor.changes.heading",
            })}
            primaryButtonText={intl.formatMessage({
              id: "analyzerType.mappingEditor.changes.save",
            })}
            secondaryButtonText={intl.formatMessage({
              id: "label.button.cancel",
            })}
            primaryButtonDisabled={saving}
            onRequestSubmit={save}
            onRequestClose={() => setReviewingSave(false)}
          >
            {changes.length === 0 ? (
              <p>
                <FormattedMessage id="analyzerType.mappingEditor.changes.none" />
              </p>
            ) : (
              <ul data-testid="analyzer-mapping-changes">
                {changes.map((change) => (
                  <li key={change.key}>
                    <strong>{change.label}</strong>{" "}
                    <FormattedMessage
                      id="analyzerType.mappingEditor.changes.row"
                      values={{
                        from:
                          change.from.name ||
                          intl.formatMessage({
                            id: stateMessageId(change.from.state),
                          }),
                        to:
                          change.to.name ||
                          intl.formatMessage({
                            id: stateMessageId(change.to.state),
                          }),
                      }}
                    />
                  </li>
                ))}
              </ul>
            )}
          </Modal>
        </>,
      )}
    </>
  );
};

export default AnalyzerTypeMappingEditor;
