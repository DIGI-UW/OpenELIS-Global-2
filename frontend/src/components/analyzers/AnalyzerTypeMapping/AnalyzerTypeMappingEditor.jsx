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
  Tag,
} from "@carbon/react";
import { ArrowLeft, Copy, Save } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { Link, useLocation, useParams } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AffectedAnalyzerList from "../AnalyzerTypeManagement/AffectedAnalyzerList";
import {
  formatRecognitionCondition,
  formatRecognitionMode,
} from "../AnalyzerTypeManagement/recognitionText";
import { safeInternalPath } from "../../utils/UrlUtils";
import {
  confirmAnalyzerTypeMapping,
  getAnalyzerMappingResultOptions,
  getAnalyzerMappingTests,
  getAnalyzerTypeMapping,
  getAnalyzerTypeRevision,
  saveAnalyzerTypeMapping,
} from "../../../services/analyzerService";
import "./AnalyzerTypeMappingEditor.scss";

const hasApiError = (response) =>
  !response || Boolean(response.error) || Number(response.status || 0) >= 400;

const errorText = (response, fallback) =>
  response?.error || response?.message || fallback;

const cloneTests = (tests = []) =>
  tests.map((test) => ({
    ...test,
    aliases: [...(test.aliases || [])],
    results: (test.results || []).map((result) => ({ ...result })),
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

const AnalyzerTypeMappingEditor = () => {
  const intl = useIntl();
  const location = useLocation();
  const { profileId } = useParams();
  const query = useMemo(
    () => new URLSearchParams(location.search),
    [location.search],
  );
  const revision = Number(query.get("revision"));
  const returnTo = safeInternalPath(query.get("returnTo"), "/analyzers/types");
  const [mapping, setMapping] = useState(null);
  const [typeSummary, setTypeSummary] = useState(null);
  const [draftTests, setDraftTests] = useState([]);
  const [catalogTests, setCatalogTests] = useState([]);
  const [resultOptionsByTest, setResultOptionsByTest] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [notification, setNotification] = useState(null);
  const loadedResultOptions = useRef(new Set());
  const routeIsValid =
    Boolean(profileId) && Number.isInteger(revision) && revision >= 1;
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
    getAnalyzerTypeMapping(profileId, revision, (response) => {
      setLoading(false);
      if (hasApiError(response) || !Array.isArray(response.tests)) {
        setLoadError(
          errorText(
            response,
            intl.formatMessage({
              id: "analyzerType.mappingEditor.error.load",
            }),
          ),
        );
        return;
      }
      loadedResultOptions.current = new Set();
      setResultOptionsByTest({});
      applyMapping(response);
    });
    getAnalyzerTypeRevision(profileId, revision, (response) => {
      if (!hasApiError(response)) {
        setTypeSummary(response);
      }
    });
    getAnalyzerMappingTests((response) => {
      if (Array.isArray(response)) {
        setCatalogTests(response);
      }
    });
  }, [applyMapping, intl, profileId, revision, routeIsValid]);

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
  }, [draftTests]);

  const updateTest = (sourceRowKey, transform) => {
    setDraftTests((current) =>
      current.map((test) =>
        test.sourceRowKey === sourceRowKey ? transform(test) : test,
      ),
    );
    setDirty(true);
    setNotification(null);
  };

  const selectTest = (sourceRowKey, selectedTest) => {
    updateTest(sourceRowKey, (test) => {
      const changed = test.testId !== selectedTest?.id;
      return {
        ...test,
        mappingState: selectedTest ? "BOUND" : "UNRESOLVED",
        testId: selectedTest?.id || null,
        selectedTest: selectedTest || null,
        results: changed
          ? test.results.map((result) => ({
              ...result,
              mappingState: "UNRESOLVED",
              resultOptionId: null,
              selectedOption: null,
            }))
          : test.results,
      };
    });
  };

  const excludeTest = (sourceRowKey, checked) => {
    updateTest(sourceRowKey, (test) => ({
      ...test,
      mappingState: checked ? "EXCLUDED" : "UNRESOLVED",
      testId: null,
      selectedTest: null,
      results: test.results.map((result) => ({
        ...result,
        mappingState: checked ? "EXCLUDED" : "UNRESOLVED",
        resultOptionId: null,
        selectedOption: null,
      })),
    }));
  };

  const selectResult = (sourceRowKey, rawValue, selectedOption) => {
    updateTest(sourceRowKey, (test) => ({
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

  const excludeResult = (sourceRowKey, rawValue, checked) => {
    updateTest(sourceRowKey, (test) => ({
      ...test,
      results: test.results.map((result) =>
        result.rawValue === rawValue
          ? {
              ...result,
              mappingState: checked ? "EXCLUDED" : "UNRESOLVED",
              resultOptionId: null,
              selectedOption: null,
            }
          : result,
      ),
    }));
  };

  const updatePayload = useMemo(
    () => ({
      baseBindingFingerprint: mapping?.bindingFingerprint || null,
      tests: draftTests.map((test) => ({
        sourceRowKey: test.sourceRowKey,
        mappingState: test.mappingState,
        testId: test.mappingState === "BOUND" ? test.testId : null,
      })),
      results: draftTests.flatMap((test) =>
        test.results.map((result) => ({
          sourceRowKey: test.sourceRowKey,
          rawValue: result.rawValue,
          mappingState: result.mappingState,
          testResultId:
            result.mappingState === "BOUND" ? result.resultOptionId : null,
        })),
      ),
    }),
    [draftTests, mapping?.bindingFingerprint],
  );

  const complete = useMemo(
    () =>
      draftTests.length > 0 &&
      draftTests.every(
        (test) =>
          test.mappingState !== "UNRESOLVED" &&
          (test.mappingState !== "BOUND" || Boolean(test.selectedTest)) &&
          test.results.every(
            (result) =>
              result.mappingState !== "UNRESOLVED" &&
              (result.mappingState !== "BOUND" ||
                Boolean(result.selectedOption)),
          ),
      ),
    [draftTests],
  );

  const counts = useMemo(() => {
    const results = draftTests.flatMap((test) => test.results);
    return {
      testsBound: draftTests.filter((test) => test.mappingState === "BOUND")
        .length,
      testsTotal: draftTests.length,
      resultsBound: results.filter((result) => result.mappingState === "BOUND")
        .length,
      resultsTotal: results.length,
    };
  }, [draftTests]);

  const save = () => {
    if (!dirty || saving) {
      return;
    }
    setSaving(true);
    saveAnalyzerTypeMapping(profileId, revision, updatePayload, (response) => {
      setSaving(false);
      if (hasApiError(response) || !Array.isArray(response.tests)) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "analyzerType.mappingEditor.error.save",
          }),
          subtitle: errorText(response, ""),
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

  const confirm = () => {
    if (!complete || dirty || confirming || !mapping?.bindingFingerprint) {
      return;
    }
    const confirmedRows = [];
    const excludedRows = [];
    draftTests.forEach((test) => {
      const destination =
        test.mappingState === "BOUND" ? confirmedRows : excludedRows;
      destination.push({ sourceRowKey: test.sourceRowKey, rawValue: null });
      test.results.forEach((result) => {
        const resultDestination =
          result.mappingState === "BOUND" ? confirmedRows : excludedRows;
        resultDestination.push({
          sourceRowKey: test.sourceRowKey,
          rawValue: result.rawValue,
        });
      });
    });
    setConfirming(true);
    confirmAnalyzerTypeMapping(
      profileId,
      revision,
      {
        baseBindingFingerprint: mapping.bindingFingerprint,
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
            subtitle: errorText(response, ""),
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

  const mappingMatchesRoute =
    mapping?.profileId === profileId && mapping?.profileRevision === revision;
  const currentTypeSummary =
    typeSummary?.profileId === profileId && typeSummary?.revision === revision
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
            subtitle={routeError || loadError || ""}
            actionButtonLabel={intl.formatMessage({
              id: "analyzerType.button.retry",
            })}
            onActionButtonClick={retry}
          />
        </Column>
      </Grid>
    );
  }

  const heading = intl.formatMessage(
    { id: "analyzerType.mappingEditor.title" },
    { name: mapping.displayName },
  );
  const currentUrl = `${location.pathname}${location.search}`;
  const duplicateParams = new URLSearchParams({
    action: "duplicate",
    profile: profileId,
    revision: String(revision),
    returnTo: currentUrl,
  });
  const confirmation = mapping.confirmation || { state: "UNCONFIRMED" };

  return (
    <>
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
          { label: "analyzerType.page.title", link: returnTo },
          { label: heading, isCurrentPage: true },
        ]}
      />
      <Grid fullWidth className="analyzer-type-mapping">
        <Column lg={16} md={8} sm={4}>
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
                to={returnTo}
              >
                <FormattedMessage id="analyzerType.mappingEditor.return" />
              </Button>
              <Button
                as={Link}
                kind="secondary"
                renderIcon={Copy}
                to={`/analyzers/types?${duplicateParams.toString()}`}
              >
                <FormattedMessage id="analyzerType.button.duplicate" />
              </Button>
            </div>
          </div>

          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            className="analyzer-type-mapping__notice"
            title={intl.formatMessage({
              id: "analyzerType.mappingEditor.shared.title",
            })}
            subtitle={intl.formatMessage(
              { id: "analyzerType.mappingEditor.shared.subtitle" },
              { count: currentTypeSummary?.usedBy || 0 },
            )}
          />
          <AffectedAnalyzerList
            analyzers={currentTypeSummary?.affectedAnalyzers || []}
          />

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
            className="analyzer-type-mapping__summary"
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
            <div>
              <span>
                <FormattedMessage id="analyzerType.mappingEditor.confirmation" />
              </span>
              <Tag
                type={confirmation.state === "CURRENT" ? "green" : "warm-gray"}
              >
                <FormattedMessage
                  id={`analyzerType.mappingEditor.confirmation.summary.${confirmation.state.toLowerCase()}`}
                />
              </Tag>
            </div>
          </section>

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
            <Accordion align="start">
              {draftTests.map((test) => {
                const selectedTest =
                  catalogTests.find(
                    (candidate) => candidate.id === test.testId,
                  ) ||
                  test.selectedTest ||
                  null;
                const resultOptions = test.testId
                  ? resultOptionsByTest[test.testId]
                  : undefined;
                return (
                  <AccordionItem
                    key={test.sourceRowKey}
                    open={test.mappingState === "UNRESOLVED"}
                    title={
                      <div className="analyzer-type-mapping__row-title">
                        <strong>{test.rawCode}</strong>
                        <span>{test.testNameHint}</span>
                        <Tag type={stateTagType(test.mappingState)} size="sm">
                          <FormattedMessage
                            id={stateMessageId(test.mappingState)}
                          />
                        </Tag>
                      </div>
                    }
                  >
                    <div
                      className="analyzer-type-mapping__row"
                      data-testid="analyzer-type-mapping-row"
                    >
                      <div className="analyzer-type-mapping__source">
                        <div>
                          <span className="analyzer-type-mapping__label">
                            <FormattedMessage id="analyzerType.mappingEditor.sourceCode" />
                          </span>
                          <strong>{test.rawCode}</strong>
                        </div>
                        {test.loinc && (
                          <div>
                            <span className="analyzer-type-mapping__label">
                              LOINC
                            </span>
                            <strong>{test.loinc}</strong>
                          </div>
                        )}
                        {test.normalizedCoding && (
                          <div>
                            <span className="analyzer-type-mapping__label">
                              <FormattedMessage id="analyzerType.mappingEditor.normalized" />
                            </span>
                            <strong>
                              {test.normalizedCoding.display ||
                                test.normalizedCoding.code}
                            </strong>
                          </div>
                        )}
                        {(test.aliases || []).map((alias) => (
                          <span
                            className="analyzer-type-mapping__alias"
                            key={alias}
                          >
                            <FormattedMessage
                              id="analyzerType.mappingEditor.alias"
                              values={{ alias }}
                            />
                          </span>
                        ))}
                      </div>

                      <div className="analyzer-type-mapping__decision">
                        <ComboBox
                          id={`analyzer-test-${test.sourceRowKey}`}
                          titleText={intl.formatMessage(
                            { id: "analyzerType.mappingEditor.testPicker" },
                            { code: test.rawCode },
                          )}
                          placeholder={intl.formatMessage({
                            id: "analyzerType.mappingEditor.testPicker.placeholder",
                          })}
                          items={catalogTests}
                          itemToString={testItemText}
                          selectedItem={
                            test.mappingState === "BOUND" ? selectedTest : null
                          }
                          disabled={test.mappingState === "EXCLUDED"}
                          onChange={({ selectedItem }) =>
                            selectTest(test.sourceRowKey, selectedItem)
                          }
                        />
                        {test.suggestedTest &&
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
                                onClick={() =>
                                  selectTest(
                                    test.sourceRowKey,
                                    test.suggestedTest,
                                  )
                                }
                              >
                                <FormattedMessage id="analyzerType.mappingEditor.useSuggestion" />
                              </Button>
                            </div>
                          )}
                        <Checkbox
                          id={`exclude-test-${test.sourceRowKey}`}
                          aria-label={intl.formatMessage(
                            { id: "analyzerType.mappingEditor.excludeTest" },
                            { code: test.rawCode },
                          )}
                          labelText={intl.formatMessage(
                            { id: "analyzerType.mappingEditor.excludeTest" },
                            { code: test.rawCode },
                          )}
                          checked={test.mappingState === "EXCLUDED"}
                          onChange={(_, state) =>
                            excludeTest(test.sourceRowKey, state.checked)
                          }
                        />
                      </div>

                      {test.results.length > 0 &&
                        test.mappingState !== "EXCLUDED" && (
                          <div className="analyzer-type-mapping__results">
                            <h3>
                              <FormattedMessage
                                id="analyzerType.mappingEditor.results.heading"
                                values={{
                                  name: selectedTest?.name || test.rawCode,
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
                                    (option) =>
                                      option.id === result.resultOptionId,
                                  ) ||
                                  result.selectedOption ||
                                  null;
                                return (
                                  <div
                                    className="analyzer-type-mapping__result-row"
                                    key={`${test.sourceRowKey}:${result.rawValue}`}
                                  >
                                    <code>{result.rawValue}</code>
                                    <Dropdown
                                      id={`result-${test.sourceRowKey}-${result.rawValue.replace(/[^a-z0-9]/gi, "-")}`}
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
                                        result.mappingState === "BOUND"
                                          ? selectedOption
                                          : null
                                      }
                                      disabled={
                                        result.mappingState === "EXCLUDED"
                                      }
                                      onChange={({ selectedItem }) =>
                                        selectResult(
                                          test.sourceRowKey,
                                          result.rawValue,
                                          selectedItem,
                                        )
                                      }
                                    />
                                    <Checkbox
                                      id={`exclude-result-${test.sourceRowKey}-${result.rawValue.replace(/[^a-z0-9]/gi, "-")}`}
                                      aria-label={intl.formatMessage(
                                        {
                                          id: "analyzerType.mappingEditor.excludeResult",
                                        },
                                        { value: result.rawValue },
                                      )}
                                      labelText={intl.formatMessage({
                                        id: "analyzerType.mappingEditor.excludeResult.short",
                                      })}
                                      checked={
                                        result.mappingState === "EXCLUDED"
                                      }
                                      onChange={(_, state) =>
                                        excludeResult(
                                          test.sourceRowKey,
                                          result.rawValue,
                                          state.checked,
                                        )
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
              })}
            </Accordion>
          </section>

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
                  {formatRecognitionMode(intl, mapping.controlRecognition.mode)}
                </p>
              </div>
              <Tag type="blue">
                {formatRecognitionMode(intl, mapping.controlRecognition.mode)}
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
                { actor: confirmation.confirmedByDisplayName },
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

          <div className="analyzer-type-mapping__actions">
            <div>
              {dirty && (
                <span>
                  <FormattedMessage id="analyzerType.mappingEditor.unsaved" />
                </span>
              )}
              {!complete && !dirty && (
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
                onClick={save}
              >
                <FormattedMessage id="analyzerType.mappingEditor.save" />
              </Button>
              <Button
                disabled={!complete || dirty || confirming}
                onClick={confirm}
              >
                <FormattedMessage id="analyzerType.mappingEditor.confirm" />
              </Button>
            </div>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default AnalyzerTypeMappingEditor;
