import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  ComboBox,
  FilterableMultiSelect,
  InlineNotification,
  Link as CarbonLink,
  Loading,
  Tag,
  TextInput,
} from "@carbon/react";
import { ArrowRight, Close } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { Link as RouterLink, useHistory, useLocation } from "react-router-dom";

import {
  createAnalyzer,
  getAnalyzer,
  getAnalyzerLabUnits,
  getAnalyzerTypeCatalog,
  getAnalyzerTypeMapping,
  selectAnalyzerSiteBinding,
  updateAnalyzer,
} from "../../../services/analyzerService";
import {
  formatRecognitionCondition,
  formatRecognitionMode,
} from "../AnalyzerTypeManagement/recognitionText";
import { includesComboBoxText } from "../comboBoxSearch";
import AnalyzerConnectionSetup from "./AnalyzerConnectionSetup";

import "./AnalyzerSetup.scss";

const SETUP_STEPS = ["instrument", "verify", "connect"];

const AnalyzerSetup = ({ currentStep = "instrument", onClose }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const currentIndex = Math.max(SETUP_STEPS.indexOf(currentStep), 0);
  const [analyzerName, setAnalyzerName] = useState("");
  const [profileCatalog, setProfileCatalog] = useState(null);
  const [labUnits, setLabUnits] = useState([]);
  const [selectedLabUnitIds, setSelectedLabUnitIds] = useState([]);
  const [candidate, setCandidate] = useState(null);
  const [mappingResult, setMappingResult] = useState({
    requestKey: null,
    mapping: null,
    error: false,
  });
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [saveError, setSaveError] = useState(false);
  const [bindingSelectionError, setBindingSelectionError] = useState(false);
  const [selectingBinding, setSelectingBinding] = useState(false);

  const analyzerId = new URLSearchParams(location.search).get("analyzerId");

  useEffect(() => {
    const controller = new AbortController();
    getAnalyzerTypeCatalog(setProfileCatalog, controller.signal);
    getAnalyzerLabUnits(
      (units) => setLabUnits(Array.isArray(units) ? units : []),
      controller.signal,
    );
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!analyzerId) {
      return undefined;
    }

    const controller = new AbortController();
    getAnalyzer(
      analyzerId,
      (response) => {
        const analyzer = response?.analyzers?.[0] || response;
        if (!analyzer?.id) {
          setSaveError(true);
          return;
        }

        setCandidate(analyzer);
        setAnalyzerName(analyzer.name || "");
        setSelectedLabUnitIds(
          (analyzer.testUnitIds || []).map((id) => String(id)),
        );
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [analyzerId, history, location.pathname]);

  const activeTypes = useMemo(
    () =>
      (profileCatalog?.types || [])
        .filter((type) => type.status === "ACTIVE")
        .sort((left, right) =>
          left.displayName.localeCompare(right.displayName),
        ),
    [profileCatalog],
  );

  const selectedType = useMemo(() => {
    const params = new URLSearchParams(location.search);
    const profileId = candidate?.profileId || params.get("profile");
    const revision = Number(
      candidate?.profileRevision || params.get("revision"),
    );
    return (
      (profileCatalog?.types || []).find(
        (type) => type.profileId === profileId && type.revision === revision,
      ) || null
    );
  }, [candidate, location.search, profileCatalog]);

  const selectedLabUnits = useMemo(
    () =>
      selectedLabUnitIds
        .map((id) => labUnits.find((unit) => String(unit.id) === id))
        .filter(Boolean),
    [labUnits, selectedLabUnitIds],
  );

  const mappingRoute = useMemo(() => {
    const params = new URLSearchParams(location.search);
    const profileId = params.get("profile");
    const revision = Number(params.get("revision"));
    const valid =
      Boolean(profileId) && Number.isInteger(revision) && revision >= 1;
    return {
      profileId,
      revision,
      valid,
      requestKey: valid ? `${profileId}@${revision}` : null,
    };
  }, [location.search]);

  const candidateProfileValid =
    Boolean(candidate?.profileId) &&
    Number.isInteger(Number(candidate?.profileRevision)) &&
    Number(candidate?.profileRevision) >= 1;
  const candidateMatchesMappingRoute =
    Boolean(analyzerId) &&
    String(candidate?.id) === String(analyzerId) &&
    candidate?.profileId === mappingRoute.profileId &&
    Number(candidate?.profileRevision) === mappingRoute.revision;

  useEffect(() => {
    if (!candidateProfileValid) {
      return;
    }

    const params = new URLSearchParams(location.search);
    const revision = String(candidate.profileRevision);
    if (
      params.get("profile") === candidate.profileId &&
      params.get("revision") === revision
    ) {
      return;
    }

    params.set("profile", candidate.profileId);
    params.set("revision", revision);
    history.replace({
      pathname: location.pathname,
      search: params.toString(),
    });
  }, [
    candidate?.profileId,
    candidate?.profileRevision,
    candidateProfileValid,
    history,
    location.pathname,
    location.search,
  ]);

  useEffect(() => {
    if (
      currentStep !== "verify" ||
      !mappingRoute.valid ||
      !candidateMatchesMappingRoute
    ) {
      return;
    }

    getAnalyzerTypeMapping(
      mappingRoute.profileId,
      mappingRoute.revision,
      (response) => {
        const error =
          !response ||
          response.error ||
          !Array.isArray(response.tests) ||
          response.profileId !== mappingRoute.profileId ||
          response.profileRevision !== mappingRoute.revision;
        setMappingResult({
          requestKey: mappingRoute.requestKey,
          mapping: error ? null : response,
          error,
        });
      },
    );
  }, [candidateMatchesMappingRoute, currentStep, mappingRoute]);

  const mappingMatchesRoute =
    mappingResult.requestKey === mappingRoute.requestKey;
  const mapping = mappingMatchesRoute ? mappingResult.mapping : null;
  const mappingLoading =
    currentStep === "verify" &&
    Boolean(analyzerId) &&
    !saveError &&
    (!candidate ||
      (candidateProfileValid && !candidateMatchesMappingRoute) ||
      (candidateMatchesMappingRoute && !mappingMatchesRoute));
  const mappingLoadError =
    currentStep === "verify" &&
    (!analyzerId ||
      (saveError && !candidate) ||
      (candidate && !candidateProfileValid) ||
      (candidateMatchesMappingRoute &&
        mappingMatchesRoute &&
        mappingResult.error));

  const verification = useMemo(() => {
    if (!mapping) {
      return {
        testsReady: 0,
        testsTotal: 0,
        resultsReady: 0,
        resultsTotal: 0,
        complete: false,
      };
    }

    const resultRows = mapping.tests.flatMap((test) => test.results || []);
    const testIsReady = (test) =>
      test.mappingState === "EXCLUDED" ||
      (test.mappingState === "BOUND" && Boolean(test.testId));
    const resultIsReady = (result) =>
      result.mappingState === "EXCLUDED" ||
      (result.mappingState === "BOUND" && Boolean(result.resultOptionId));
    const testsReady = mapping.tests.filter(testIsReady).length;
    const resultsReady = resultRows.filter(resultIsReady).length;

    return {
      testsReady,
      testsTotal: mapping.tests.length,
      resultsReady,
      resultsTotal: resultRows.length,
      complete:
        mapping.tests.length > 0 &&
        testsReady === mapping.tests.length &&
        resultsReady === resultRows.length &&
        mapping.confirmation?.state === "CURRENT",
    };
  }, [mapping]);

  const typeLabel = (type) =>
    type
      ? intl.formatMessage(
          { id: "analyzer.setup.instrument.typeOption" },
          {
            name: type.displayName,
            manufacturer: type.manufacturer || "-",
            protocol: type.protocol,
            revision: type.revision,
          },
        )
      : "";

  const selectType = (type) => {
    const params = new URLSearchParams(location.search);
    if (type) {
      params.set("profile", type.profileId);
      params.set("revision", String(type.revision));
    } else {
      params.delete("profile");
      params.delete("revision");
    }
    history.push({ pathname: location.pathname, search: params.toString() });
  };

  const submitInstrument = () => {
    setSubmitAttempted(true);
    setSaveError(false);
    if (
      !selectedType ||
      !analyzerName.trim() ||
      selectedLabUnitIds.length === 0
    ) {
      return;
    }

    setSubmitting(true);
    const payload = {
      name: analyzerName.trim(),
      profileId: selectedType.profileId,
      profileRevision: selectedType.revision,
      testUnitIds: selectedLabUnitIds,
    };
    const handleSaved = (response) => {
      setSubmitting(false);
      if (
        !response?.id ||
        response.error ||
        Number(response.statusCode) >= 400
      ) {
        setSaveError(true);
        return;
      }

      setCandidate(response);
      const instrumentParams = new URLSearchParams(location.search);
      instrumentParams.set("setup", "instrument");
      instrumentParams.set("analyzerId", String(response.id));
      instrumentParams.set("profile", selectedType.profileId);
      instrumentParams.set("revision", String(selectedType.revision));
      history.replace({
        pathname: location.pathname,
        search: instrumentParams.toString(),
      });

      const verifyParams = new URLSearchParams(instrumentParams);
      verifyParams.set("setup", "verify");
      history.push({
        pathname: location.pathname,
        search: verifyParams.toString(),
      });
    };

    if (candidate?.id) {
      updateAnalyzer(candidate.id, payload, handleSaved);
    } else {
      createAnalyzer(payload, handleSaved);
    }
  };

  const returnParams = new URLSearchParams(location.search);
  returnParams.delete("profile");
  returnParams.delete("revision");
  const returnTo = `${location.pathname}?${returnParams.toString()}`;
  const createTypeTarget = `/analyzers/types?action=create&returnTo=${encodeURIComponent(
    returnTo,
  )}`;
  const currentSetupUrl = `${location.pathname}${location.search}`;
  const mappingEditorTarget = mappingRoute.valid
    ? `/analyzers/types/${encodeURIComponent(
        mappingRoute.profileId,
      )}/mapping?revision=${mappingRoute.revision}&returnTo=${encodeURIComponent(
        currentSetupUrl,
      )}`
    : "/analyzers/types";

  const continueToConnect = () => {
    if (!verification.complete || selectingBinding) {
      return;
    }
    setSelectingBinding(true);
    setBindingSelectionError(false);
    selectAnalyzerSiteBinding(
      candidate.id,
      {
        siteBindingId: mapping.siteBindingId,
        revision: mapping.siteBindingRevision,
        bindingFingerprint: mapping.bindingFingerprint,
      },
      (response) => {
        setSelectingBinding(false);
        if (
          !response?.id ||
          response.error ||
          Number(response.statusCode) >= 400
        ) {
          setBindingSelectionError(true);
          return;
        }
        setCandidate(response);
        const params = new URLSearchParams(location.search);
        params.set("setup", "connect");
        history.push({
          pathname: location.pathname,
          search: params.toString(),
        });
      },
    );
  };

  const editStep = (step) => {
    const params = new URLSearchParams(location.search);
    params.set("setup", step);
    history.push({ pathname: location.pathname, search: params.toString() });
  };

  return (
    <section
      className="analyzer-setup"
      aria-labelledby="analyzer-setup-title"
      data-testid="analyzer-setup"
    >
      <header className="analyzer-setup__header">
        <h2 id="analyzer-setup-title">
          {intl.formatMessage({ id: "analyzer.setup.title" })}
        </h2>
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          renderIcon={Close}
          iconDescription={intl.formatMessage({
            id: "analyzer.setup.close",
          })}
          onClick={onClose}
          data-testid="analyzer-setup-close"
        />
      </header>

      {saveError && (
        <InlineNotification
          className="analyzer-setup__notification"
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzer.setup.instrument.saveError",
          })}
        />
      )}

      <ol
        className="analyzer-setup__steps"
        aria-label={intl.formatMessage({ id: "analyzer.setup.steps" })}
      >
        {SETUP_STEPS.map((step, index) => {
          const state =
            index < currentIndex
              ? "complete"
              : index === currentIndex
                ? "current"
                : "future";

          return (
            <li
              key={step}
              className={`analyzer-setup__step analyzer-setup__step--${state}`}
              aria-current={state === "current" ? "step" : undefined}
              aria-disabled={state === "future" ? true : undefined}
            >
              <div className="analyzer-setup__step-heading">
                <span className="analyzer-setup__step-number" aria-hidden>
                  {index + 1}
                </span>
                <h3>
                  {intl.formatMessage({ id: `analyzer.setup.${step}.title` })}
                </h3>
                {state === "complete" && (
                  <Button
                    type="button"
                    kind="ghost"
                    size="sm"
                    className="analyzer-setup__step-edit"
                    aria-label={intl.formatMessage(
                      { id: "analyzer.setup.step.edit" },
                      {
                        step: intl.formatMessage({
                          id: `analyzer.setup.${step}.title`,
                        }),
                      },
                    )}
                    onClick={() => editStep(step)}
                  >
                    {intl.formatMessage({ id: "button.edit" })}
                  </Button>
                )}
              </div>
              {state === "current" && step === "instrument" && (
                <div className="analyzer-setup__instrument">
                  <ComboBox
                    id="analyzer-setup-type"
                    titleText={intl.formatMessage({
                      id: "analyzer.setup.instrument.type",
                    })}
                    placeholder={intl.formatMessage({
                      id: "analyzer.setup.instrument.type.placeholder",
                    })}
                    items={activeTypes}
                    selectedItem={selectedType}
                    itemToString={typeLabel}
                    shouldFilterItem={includesComboBoxText}
                    onChange={({ selectedItem }) => selectType(selectedItem)}
                    invalid={submitAttempted && !selectedType}
                    invalidText={intl.formatMessage({
                      id: "analyzer.setup.instrument.type.required",
                    })}
                  />
                  <TextInput
                    id="analyzer-setup-name"
                    labelText={intl.formatMessage({
                      id: "analyzer.setup.instrument.name",
                    })}
                    value={analyzerName}
                    onChange={(event) => setAnalyzerName(event.target.value)}
                    invalid={submitAttempted && !analyzerName.trim()}
                    invalidText={intl.formatMessage({
                      id: "analyzer.setup.instrument.name.required",
                    })}
                  />
                  <FilterableMultiSelect
                    id="analyzer-setup-lab-units"
                    titleText={intl.formatMessage({
                      id: "analyzer.setup.instrument.labUnits",
                    })}
                    label={intl.formatMessage({
                      id: "analyzer.setup.instrument.labUnits.placeholder",
                    })}
                    items={labUnits}
                    selectedItems={selectedLabUnits}
                    itemToString={(unit) => unit?.name || ""}
                    onChange={({ selectedItems }) =>
                      setSelectedLabUnitIds(
                        (selectedItems || []).map((unit) => String(unit.id)),
                      )
                    }
                    invalid={submitAttempted && selectedLabUnitIds.length === 0}
                    invalidText={intl.formatMessage({
                      id: "analyzer.setup.instrument.labUnits.required",
                    })}
                  />
                  <div className="analyzer-setup__instrument-actions">
                    <CarbonLink as={RouterLink} to={createTypeTarget}>
                      {intl.formatMessage({
                        id: "analyzer.setup.instrument.notListed",
                      })}
                    </CarbonLink>
                    <Button
                      type="button"
                      renderIcon={ArrowRight}
                      disabled={submitting}
                      onClick={submitInstrument}
                    >
                      {intl.formatMessage({
                        id: submitting
                          ? "analyzer.setup.instrument.saving"
                          : "analyzer.setup.instrument.continue",
                      })}
                    </Button>
                  </div>
                </div>
              )}
              {state === "complete" && step === "instrument" && candidate && (
                <dl
                  className="analyzer-setup__instrument-summary"
                  aria-label={intl.formatMessage({
                    id: "analyzer.setup.instrument.summary",
                  })}
                >
                  <div>
                    <dt>
                      {intl.formatMessage({
                        id: "analyzer.setup.instrument.name",
                      })}
                    </dt>
                    <dd>{candidate.name}</dd>
                  </div>
                  <div>
                    <dt>
                      {intl.formatMessage({
                        id: "analyzer.setup.instrument.type",
                      })}
                    </dt>
                    <dd>{typeLabel(selectedType)}</dd>
                  </div>
                  <div>
                    <dt>
                      {intl.formatMessage({
                        id: "analyzer.setup.instrument.labUnits",
                      })}
                    </dt>
                    <dd>
                      {selectedLabUnits.map((unit) => unit.name).join(", ")}
                    </dd>
                  </div>
                </dl>
              )}
              {state === "current" && step === "verify" && (
                <div className="analyzer-setup__verify">
                  {mappingLoading ? (
                    <Loading
                      small
                      withOverlay={false}
                      description={intl.formatMessage({
                        id: "analyzer.setup.verify.loading",
                      })}
                    />
                  ) : mappingLoadError || !mapping ? (
                    <InlineNotification
                      kind="error"
                      lowContrast
                      hideCloseButton
                      title={intl.formatMessage({
                        id: "analyzer.setup.verify.loadError",
                      })}
                    />
                  ) : (
                    <>
                      <div className="analyzer-setup__verify-heading">
                        <div>
                          <h4>
                            {intl.formatMessage({
                              id: "analyzer.setup.verify.heading",
                            })}
                          </h4>
                          <p>
                            {intl.formatMessage(
                              { id: "analyzer.setup.verify.profile" },
                              {
                                name: mapping.displayName,
                                protocol: mapping.protocol,
                                revision: mapping.profileRevision,
                              },
                            )}
                          </p>
                        </div>
                        <Tag
                          type={
                            mapping.confirmation?.state === "CURRENT"
                              ? "green"
                              : "warm-gray"
                          }
                        >
                          {intl.formatMessage({
                            id: `analyzerType.mappingEditor.confirmation.summary.${String(
                              mapping.confirmation?.state || "UNCONFIRMED",
                            ).toLowerCase()}`,
                          })}
                        </Tag>
                      </div>

                      {!verification.complete && (
                        <InlineNotification
                          kind="warning"
                          lowContrast
                          hideCloseButton
                          title={intl.formatMessage({
                            id: "analyzer.setup.verify.attention",
                          })}
                        />
                      )}

                      <dl
                        className="analyzer-setup__verify-counts"
                        aria-label={intl.formatMessage({
                          id: "analyzer.setup.verify.counts",
                        })}
                      >
                        <div>
                          <dt>
                            {intl.formatMessage({
                              id: "analyzerType.mappingEditor.tests",
                            })}
                          </dt>
                          <dd>
                            {intl.formatMessage(
                              { id: "analyzer.setup.verify.testsReady" },
                              {
                                ready: verification.testsReady,
                                total: verification.testsTotal,
                              },
                            )}
                          </dd>
                        </div>
                        <div>
                          <dt>
                            {intl.formatMessage({
                              id: "analyzerType.mappingEditor.results",
                            })}
                          </dt>
                          <dd>
                            {intl.formatMessage(
                              { id: "analyzer.setup.verify.resultsReady" },
                              {
                                ready: verification.resultsReady,
                                total: verification.resultsTotal,
                              },
                            )}
                          </dd>
                        </div>
                      </dl>

                      <section
                        className="analyzer-setup__verify-recognition"
                        aria-labelledby="analyzer-setup-recognition"
                      >
                        <div className="analyzer-setup__verify-recognition-heading">
                          <h4 id="analyzer-setup-recognition">
                            {intl.formatMessage({
                              id: "analyzerType.recognition.heading",
                            })}
                          </h4>
                          <Tag type="blue">
                            {formatRecognitionMode(
                              intl,
                              mapping.controlRecognition.mode,
                            )}
                          </Tag>
                        </div>
                        {mapping.controlRecognition.mode === "NONE" ? (
                          <p>
                            {intl.formatMessage({
                              id: "analyzerType.recognition.mode.none",
                            })}
                          </p>
                        ) : (
                          <ul>
                            {mapping.controlRecognition.conditions.map(
                              (condition) => (
                                <li key={condition.key}>
                                  {formatRecognitionCondition(intl, condition)}
                                </li>
                              ),
                            )}
                          </ul>
                        )}
                      </section>

                      {mapping.confirmation?.state === "CURRENT" &&
                        mapping.confirmation.confirmedByDisplayName &&
                        mapping.confirmation.confirmedAt && (
                          <InlineNotification
                            kind="success"
                            lowContrast
                            hideCloseButton
                            title={intl.formatMessage({
                              id: "analyzerType.mappingEditor.confirmation.current",
                            })}
                            subtitle={intl.formatMessage(
                              {
                                id: "analyzerType.mappingEditor.confirmation.by",
                              },
                              {
                                actor:
                                  mapping.confirmation.confirmedByDisplayName,
                                date: intl.formatDate(
                                  mapping.confirmation.confirmedAt,
                                  {
                                    year: "numeric",
                                    month: "short",
                                    day: "numeric",
                                    hour: "numeric",
                                    minute: "2-digit",
                                  },
                                ),
                              },
                            )}
                          />
                        )}

                      {bindingSelectionError && (
                        <InlineNotification
                          kind="error"
                          lowContrast
                          hideCloseButton
                          title={intl.formatMessage({
                            id: "analyzer.setup.verify.selectionError",
                          })}
                        />
                      )}
                    </>
                  )}

                  <div className="analyzer-setup__verify-actions">
                    <CarbonLink as={RouterLink} to={mappingEditorTarget}>
                      {intl.formatMessage({
                        id: "analyzer.setup.verify.review",
                      })}
                    </CarbonLink>
                    <Button
                      type="button"
                      renderIcon={ArrowRight}
                      disabled={!verification.complete || selectingBinding}
                      onClick={continueToConnect}
                    >
                      {intl.formatMessage({
                        id: selectingBinding
                          ? "analyzer.setup.verify.selecting"
                          : "analyzer.setup.verify.continue",
                      })}
                    </Button>
                  </div>
                </div>
              )}
              {state === "complete" && step === "verify" && (
                <p className="analyzer-setup__verify-summary">
                  {intl.formatMessage({ id: "analyzer.setup.verify.summary" })}
                </p>
              )}
              {state === "current" &&
                step === "connect" &&
                (candidate ? (
                  <AnalyzerConnectionSetup
                    key={`${candidate.id}:${candidate.profileId}:${candidate.profileRevision}`}
                    candidate={candidate}
                    onCandidateChange={setCandidate}
                    onClose={onClose}
                  />
                ) : (
                  <Loading
                    small
                    withOverlay={false}
                    description={intl.formatMessage({
                      id: "analyzer.setup.connect.loading",
                    })}
                  />
                ))}
            </li>
          );
        })}
      </ol>
    </section>
  );
};

export default AnalyzerSetup;
