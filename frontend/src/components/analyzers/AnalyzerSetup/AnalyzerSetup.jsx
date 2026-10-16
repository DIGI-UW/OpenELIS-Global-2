import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  ComboBox,
  DismissibleTag,
  FilterableMultiSelect,
  InlineNotification,
  Link as CarbonLink,
  Loading,
  ProgressIndicator,
  ProgressStep,
  TextInput,
} from "@carbon/react";
import { ArrowRight, Close } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  Link as RouterLink,
  Redirect,
  useHistory,
  useLocation,
} from "react-router-dom";

import {
  createAnalyzer,
  getAnalyzer,
  getAnalyzerLabUnits,
  applyAnalyzerMapping,
  getAnalyzerTypeCatalog,
  updateAnalyzer,
} from "../../../services/analyzerService";
import { analyzerErrorText } from "../analyzerErrors";
import { includesComboBoxText } from "../comboBoxSearch";
import AnalyzerPageHeader from "../AnalyzerPageHeader";
import AnalyzerTypeMappingEditor from "../AnalyzerTypeMapping/AnalyzerTypeMappingEditor";
import AnalyzerAssaysSetup from "./AnalyzerAssaysSetup";
import AnalyzerConnectionSetup, {
  needsMappingVerification,
} from "./AnalyzerConnectionSetup";

import "./AnalyzerSetup.scss";

const SETUP_STEPS = ["instrument", "assays", "verify", "connect"];

/**
 * The analyzer setup as its own page: the four steps as a progress indicator
 * on the left, the current step's form on the right. The URL carries the
 * step and the analyzer (`?setup=<step>&analyzerId=…&profile=…&revision=…`),
 * so every step can be bookmarked and the browser's Back works.
 */
const AnalyzerSetup = ({ currentStep = "instrument", onClose }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const currentIndex = Math.max(SETUP_STEPS.indexOf(currentStep), 0);
  const [analyzerName, setAnalyzerName] = useState(
    () => new URLSearchParams(location.search).get("name") || "",
  );
  const [profileCatalog, setProfileCatalog] = useState(null);
  const [labUnits, setLabUnits] = useState([]);
  const [selectedLabUnitIds, setSelectedLabUnitIds] = useState(() =>
    (new URLSearchParams(location.search).get("labUnits") || "")
      .split(",")
      .filter(Boolean),
  );
  const [candidate, setCandidate] = useState(null);
  // What the mapping editor embedded in Verify last reported.
  const [verifyState, setVerifyState] = useState({
    mapping: null,
    dirty: false,
  });
  const reportVerifyMapping = useCallback(
    (mapping, dirty) => setVerifyState({ mapping, dirty }),
    [],
  );
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [saveError, setSaveError] = useState(false);
  const [bindingSelectionError, setBindingSelectionError] = useState(false);
  const [selectingBinding, setSelectingBinding] = useState(false);
  const [connectReadiness, setConnectReadiness] = useState(null);

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

  const candidateProfileValid =
    Boolean(candidate?.profileId) &&
    Number.isInteger(Number(candidate?.profileRevision)) &&
    Number(candidate?.profileRevision) >= 1;

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

  const mapping = verifyState.mapping;

  // Continue needs every record of the assays this instrument runs mapped, the
  // mapping saved and confirmed. Assays that are off are not mapped here.
  const verification = useMemo(() => {
    if (!mapping) {
      return { assaysOn: 0, unresolved: 0, confirmed: false, complete: false };
    }
    const enabledCodes = new Set(
      mapping.tests
        .filter((test) => !test.subIdentity && test.enabled !== false)
        .map((test) => test.sourceRowKey),
    );
    const rows = mapping.tests.filter((test) =>
      enabledCodes.has(test.sourceRowKey),
    );
    const unresolved =
      rows.filter((test) => test.mappingState === "UNRESOLVED").length +
      rows
        .flatMap((test) => test.results || [])
        .filter((result) => result.mappingState === "UNRESOLVED").length;
    const confirmed =
      !verifyState.dirty && mapping.confirmation?.state === "CURRENT";
    return {
      assaysOn: enabledCodes.size,
      unresolved,
      confirmed,
      complete: enabledCodes.size > 0 && unresolved === 0 && confirmed,
    };
  }, [mapping, verifyState.dirty]);

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

  // The name on its own line, what tells two types apart beneath it.
  const typeOption = (type) =>
    type ? (
      <span className="analyzer-setup__type-option">
        <span className="analyzer-setup__type-option-name">
          {type.displayName}
        </span>
        <span className="cds--visually-hidden"> · </span>
        <span className="analyzer-setup__type-option-detail">
          {intl.formatMessage(
            { id: "analyzer.setup.instrument.typeOption.detail" },
            {
              manufacturer: type.manufacturer || "-",
              protocol: type.protocol,
              revision: type.revision,
            },
          )}
        </span>
      </span>
    ) : null;

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
        setSaveError(response || true);
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

      const assaysParams = new URLSearchParams(instrumentParams);
      assaysParams.set("setup", "assays");
      history.push({
        pathname: location.pathname,
        search: assaysParams.toString(),
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
  if (!candidate?.id) {
    returnParams.delete("name");
    returnParams.delete("labUnits");
    if (analyzerName.trim()) {
      returnParams.set("name", analyzerName.trim());
    }
    if (selectedLabUnitIds.length > 0) {
      returnParams.set("labUnits", selectedLabUnitIds.join(","));
    }
  }
  const returnTo = `${location.pathname}?${returnParams.toString()}`;
  const createTypeTarget = `/analyzers/types?action=create&returnTo=${encodeURIComponent(
    returnTo,
  )}`;

  const continueToConnect = () => {
    if (!verification.complete || selectingBinding) {
      return;
    }
    setSelectingBinding(true);
    setBindingSelectionError(false);
    applyAnalyzerMapping(
      candidate.id,
      {
        mappingId: mapping.mappingId,
        revision: mapping.mappingRevision,
        mappingFingerprint: mapping.mappingFingerprint,
      },
      (response) => {
        setSelectingBinding(false);
        if (
          !response?.id ||
          response.error ||
          Number(response.statusCode) >= 400
        ) {
          setBindingSelectionError(response || true);
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

  // A reset analyzer has no analyzer type; setup starts again at the instrument step.
  const reloadAfterReset = () => {
    getAnalyzer(candidate.id, (response) => {
      const analyzer = response?.analyzers?.[0] || response;
      if (analyzer?.id) {
        setCandidate(analyzer);
      }
      editStep("instrument");
    });
  };

  const editStep = (step) => {
    const params = new URLSearchParams(location.search);
    params.set("setup", step);
    history.push({ pathname: location.pathname, search: params.toString() });
  };

  // A step after Instrument needs an analyzer; a link that skips ahead
  // without one lands on the first step instead of waiting for nothing.
  if (currentIndex > 0 && !analyzerId) {
    const params = new URLSearchParams(location.search);
    params.set("setup", "instrument");
    return (
      <Redirect
        to={{ pathname: location.pathname, search: params.toString() }}
      />
    );
  }

  const stepTitle = (step) =>
    intl.formatMessage({ id: `analyzer.setup.${step}.title` });
  const stepSecondary = (step, state) => {
    if (step === "instrument" && state === "complete" && candidate?.name) {
      return candidate.name;
    }
    if (step === "verify" && state === "complete") {
      return intl.formatMessage({
        id: needsMappingVerification(connectReadiness)
          ? "analyzer.setup.verify.summary.stale"
          : "analyzer.setup.verify.summary",
      });
    }
    return undefined;
  };
  const pageTitle = candidate?.name
    ? intl.formatMessage(
        { id: "analyzer.setup.title.named" },
        { name: candidate.name },
      )
    : intl.formatMessage({ id: "analyzer.setup.title" });

  return (
    <section
      className="analyzer-setup"
      aria-labelledby="analyzer-setup-title"
      data-testid="analyzer-setup"
    >
      <AnalyzerPageHeader
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
          { label: pageTitle, isCurrentPage: true },
        ]}
        title={pageTitle}
        titleId="analyzer-setup-title"
        subtitle={intl.formatMessage({ id: "analyzer.setup.subtitle" })}
        actions={
          <Button
            kind="ghost"
            renderIcon={Close}
            onClick={() => onClose()}
            data-testid="analyzer-setup-close"
          >
            {intl.formatMessage({ id: "analyzer.setup.close" })}
          </Button>
        }
      />

      {saveError && (
        <InlineNotification
          className="analyzer-setup__notification"
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzer.setup.instrument.saveError",
          })}
          subtitle={analyzerErrorText(intl, saveError, null)}
        />
      )}

      <div className="analyzer-setup__body">
        <ProgressIndicator
          vertical
          currentIndex={currentIndex}
          aria-label={intl.formatMessage({ id: "analyzer.setup.steps" })}
          className="analyzer-setup__steps"
          onChange={(index) => {
            if (index < currentIndex) editStep(SETUP_STEPS[index]);
          }}
        >
          {SETUP_STEPS.map((step, index) => {
            const state =
              index < currentIndex
                ? "complete"
                : index === currentIndex
                  ? "current"
                  : "future";
            return (
              <ProgressStep
                key={step}
                label={stepTitle(step)}
                secondaryLabel={stepSecondary(step, state)}
                complete={state === "complete"}
                current={state === "current"}
                disabled={state === "future"}
                aria-label={
                  state === "complete"
                    ? intl.formatMessage(
                        { id: "analyzer.setup.step.edit" },
                        { step: stepTitle(step) },
                      )
                    : undefined
                }
                data-testid={`analyzer-setup-progress-${step}`}
              />
            );
          })}
        </ProgressIndicator>

        <div
          className="analyzer-setup__content"
          data-testid={`analyzer-setup-step-${currentStep}`}
        >
          <h2 className="analyzer-setup__step-title">
            <span className="analyzer-setup__step-count">
              {intl.formatMessage(
                { id: "analyzer.setup.step.count" },
                { number: currentIndex + 1, total: SETUP_STEPS.length },
              )}
            </span>
            {stepTitle(currentStep)}
          </h2>

          {currentStep === "instrument" && (
            <div className="analyzer-setup__instrument">
              <ComboBox
                id="analyzer-setup-type"
                titleText={intl.formatMessage({
                  id: "analyzer.setup.instrument.type",
                })}
                placeholder={intl.formatMessage({
                  id: "analyzer.setup.instrument.type.placeholder",
                })}
                helperText={intl.formatMessage({
                  id: "analyzer.setup.instrument.type.helper",
                })}
                items={activeTypes}
                selectedItem={selectedType}
                itemToString={typeLabel}
                itemToElement={typeOption}
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
              {selectedLabUnits.length > 0 && (
                <div
                  className="analyzer-setup__lab-unit-tags"
                  data-testid="analyzer-setup-lab-unit-tags"
                >
                  {selectedLabUnits.map((unit) => {
                    const remove = intl.formatMessage(
                      { id: "analyzer.setup.instrument.labUnits.remove" },
                      { name: unit.name },
                    );
                    return (
                      <DismissibleTag
                        key={unit.id}
                        type="gray"
                        size="sm"
                        text={unit.name}
                        title={remove}
                        dismissTooltipLabel={remove}
                        onClose={() =>
                          setSelectedLabUnitIds((ids) =>
                            ids.filter((id) => id !== String(unit.id)),
                          )
                        }
                      />
                    );
                  })}
                </div>
              )}
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

          {currentStep === "assays" &&
            (candidate ? (
              <AnalyzerAssaysSetup
                analyzerId={String(candidate.id)}
                onContinue={() => editStep("verify")}
              />
            ) : (
              <Loading
                small
                withOverlay={false}
                description={intl.formatMessage({
                  id: "analyzer.setup.assays.loading",
                })}
              />
            ))}

          {currentStep === "verify" && (
            <div className="analyzer-setup__verify">
              {candidate ? (
                <>
                  <AnalyzerTypeMappingEditor
                    analyzerId={String(candidate.id)}
                    embedded
                    onMappingChange={reportVerifyMapping}
                    statusText={
                      mapping && !verification.complete
                        ? intl.formatMessage(
                            {
                              id:
                                verification.assaysOn === 0
                                  ? "analyzer.setup.verify.noAssays"
                                  : verification.unresolved > 0
                                    ? "analyzer.setup.verify.unresolved"
                                    : "analyzer.setup.verify.confirm",
                            },
                            { count: verification.unresolved },
                          )
                        : null
                    }
                    nextAction={{
                      label: intl.formatMessage({
                        id: selectingBinding
                          ? "analyzer.setup.verify.selecting"
                          : "analyzer.setup.verify.continue",
                      }),
                      icon: ArrowRight,
                      disabled: !verification.complete || selectingBinding,
                      onClick: continueToConnect,
                    }}
                  />
                  {bindingSelectionError && (
                    <InlineNotification
                      kind="error"
                      lowContrast
                      hideCloseButton
                      title={intl.formatMessage({
                        id: "analyzer.setup.verify.selectionError",
                      })}
                      subtitle={analyzerErrorText(
                        intl,
                        bindingSelectionError,
                        null,
                      )}
                    />
                  )}
                </>
              ) : (
                <Loading
                  small
                  withOverlay={false}
                  description={intl.formatMessage({
                    id: "analyzer.setup.verify.loading",
                  })}
                />
              )}
            </div>
          )}

          {currentStep === "connect" &&
            (candidate ? (
              <AnalyzerConnectionSetup
                key={`${candidate.id}:${candidate.profileId}:${candidate.profileRevision}`}
                candidate={candidate}
                onCandidateChange={setCandidate}
                onClose={onClose}
                onVerifyMappings={() => editStep("verify")}
                onProfileReset={reloadAfterReset}
                onReadinessChange={setConnectReadiness}
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
        </div>
      </div>
    </section>
  );
};

export default AnalyzerSetup;
