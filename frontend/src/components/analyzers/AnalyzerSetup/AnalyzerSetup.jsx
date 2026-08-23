import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  ComboBox,
  FilterableMultiSelect,
  Link as CarbonLink,
  TextInput,
} from "@carbon/react";
import { Close } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { Link as RouterLink, useHistory, useLocation } from "react-router-dom";

import {
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
  const [selectedLabUnits, setSelectedLabUnits] = useState([]);

  useEffect(() => {
    const controller = new AbortController();
    getAnalyzerTypeCatalog(setProfileCatalog, controller.signal);
    getAnalyzerLabUnits(
      (units) => setLabUnits(Array.isArray(units) ? units : []),
      controller.signal,
    );
    return () => controller.abort();
  }, []);

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
    const profileId = params.get("profile");
    const revision = Number(params.get("revision"));
    return (
      activeTypes.find(
        (type) => type.profileId === profileId && type.revision === revision,
      ) || null
    );
  }, [activeTypes, location.search]);

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
    if (!type) {
      return;
    }
    const params = new URLSearchParams(location.search);
    params.set("profile", type.profileId);
    params.set("revision", String(type.revision));
    history.push({ pathname: location.pathname, search: params.toString() });
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
                  />
                  <TextInput
                    id="analyzer-setup-name"
                    labelText={intl.formatMessage({
                      id: "analyzer.setup.instrument.name",
                    })}
                    value={analyzerName}
                    onChange={(event) => setAnalyzerName(event.target.value)}
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
                      setSelectedLabUnits(selectedItems || [])
                    }
                  />
                  <CarbonLink as={RouterLink} to={createTypeTarget}>
                    {intl.formatMessage({
                      id: "analyzer.setup.instrument.notListed",
                    })}
                  </CarbonLink>
                </div>
              )}
            </li>
          );
        })}
      </ol>
    </section>
  );
};

export default AnalyzerSetup;
