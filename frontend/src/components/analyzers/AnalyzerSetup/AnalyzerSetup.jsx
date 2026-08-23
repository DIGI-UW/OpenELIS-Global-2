import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  ComboBox,
  FilterableMultiSelect,
  InlineNotification,
  Link as CarbonLink,
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
} from "../../../services/analyzerService";

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
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [saveError, setSaveError] = useState(false);

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

        if (analyzer.profileId && analyzer.profileRevision) {
          const params = new URLSearchParams(location.search);
          const revision = String(analyzer.profileRevision);
          if (
            params.get("profile") !== analyzer.profileId ||
            params.get("revision") !== revision
          ) {
            params.set("profile", analyzer.profileId);
            params.set("revision", revision);
            history.replace({
              pathname: location.pathname,
              search: params.toString(),
            });
          }
        }
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
    const profileId = params.get("profile") || candidate?.profileId;
    const revision = Number(
      params.get("revision") || candidate?.profileRevision,
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
    createAnalyzer(
      {
        name: analyzerName.trim(),
        profileId: selectedType.profileId,
        profileRevision: selectedType.revision,
        status: "SETUP",
        testUnitIds: selectedLabUnitIds,
      },
      (response) => {
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
        const params = new URLSearchParams(location.search);
        params.set("setup", "verify");
        params.set("analyzerId", String(response.id));
        params.set("profile", selectedType.profileId);
        params.set("revision", String(selectedType.revision));
        history.push({
          pathname: location.pathname,
          search: params.toString(),
        });
      },
    );
  };

  const returnParams = new URLSearchParams(location.search);
  returnParams.delete("profile");
  returnParams.delete("revision");
  const returnTo = `${location.pathname}?${returnParams.toString()}`;
  const createTypeTarget = `/analyzers/types?action=create&returnTo=${encodeURIComponent(
    returnTo,
  )}`;

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
            </li>
          );
        })}
      </ol>
    </section>
  );
};

export default AnalyzerSetup;
