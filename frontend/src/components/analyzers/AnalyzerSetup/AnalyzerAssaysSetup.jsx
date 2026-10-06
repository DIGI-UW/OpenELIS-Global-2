import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Loading,
  TextInput,
} from "@carbon/react";
import { ArrowRight } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getAnalyzerMapping,
  saveAnalyzerMapping,
} from "../../../services/analyzerService";
import { analyzerErrorText } from "../analyzerErrors";

// How the catalog matched an assay, for an operator deciding whether this
// instrument runs it.
const matchMessage = (assay) => {
  if (assay.mappingState === "BOUND") {
    return "analyzer.setup.assays.match.bound";
  }
  if (assay.unresolvedReason === "AMBIGUOUS") {
    return "analyzer.setup.assays.match.ambiguous";
  }
  if (assay.unresolvedReason === "INCOMPATIBLE") {
    return "analyzer.setup.assays.match.incompatible";
  }
  if (assay.mappingState === "EXCLUDED") {
    return "analyzer.setup.assays.match.excluded";
  }
  return "analyzer.setup.assays.match.noMatch";
};

/**
 * The instrument's host test code table: which of the profile's assays this
 * instrument runs, and the code it sends for each. An assay that is off is not
 * mapped in Verify; its results are held if they ever arrive.
 */
const AnalyzerAssaysSetup = ({ analyzerId, onContinue }) => {
  const intl = useIntl();
  const [mapping, setMapping] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [choices, setChoices] = useState({});
  const [saving, setSaving] = useState(false);
  const [saveFailure, setSaveFailure] = useState(null);

  useEffect(() => {
    getAnalyzerMapping(analyzerId, (response) => {
      if (!response || response.error || !Array.isArray(response.tests)) {
        setLoadFailed(true);
        return;
      }
      setMapping(response);
      setChoices(
        Object.fromEntries(
          response.tests
            .filter((test) => !test.subIdentity)
            .map((test) => [
              test.sourceRowKey,
              {
                enabled: test.enabled !== false,
                instrumentCode: test.instrumentCode || "",
              },
            ]),
        ),
      );
    });
  }, [analyzerId]);

  const assays = useMemo(
    () => (mapping?.tests || []).filter((test) => !test.subIdentity),
    [mapping],
  );

  const changed = assays.some(
    (assay) =>
      choices[assay.sourceRowKey]?.enabled !== (assay.enabled !== false) ||
      (choices[assay.sourceRowKey]?.instrumentCode || "") !==
        (assay.instrumentCode || ""),
  );

  const choose = (code, change) =>
    setChoices((current) => ({
      ...current,
      [code]: { ...current[code], ...change },
    }));

  const save = () => {
    if (!changed) {
      onContinue();
      return;
    }
    setSaving(true);
    setSaveFailure(null);
    saveAnalyzerMapping(
      analyzerId,
      {
        baseMappingFingerprint: mapping.mappingFingerprint,
        tests: mapping.tests.map((test) => ({
          sourceRowKey: test.sourceRowKey,
          subIdentity: test.subIdentity || "",
          mappingState: test.mappingState,
          testId: test.mappingState === "BOUND" ? test.testId : null,
          componentId:
            test.mappingState === "BOUND" ? test.componentId || null : null,
          callComponentId:
            test.mappingState === "BOUND" ? test.callComponentId || null : null,
          ...(test.subIdentity
            ? {}
            : {
                enabled: choices[test.sourceRowKey].enabled,
                instrumentCode:
                  choices[test.sourceRowKey].instrumentCode.trim() || null,
              }),
        })),
        results: mapping.tests.flatMap((test) =>
          (test.results || []).map((result) => ({
            sourceRowKey: test.sourceRowKey,
            subIdentity: test.subIdentity || "",
            rawValue: result.rawValue,
            mappingState: result.mappingState,
            testResultId:
              result.mappingState === "BOUND" ? result.resultOptionId : null,
          })),
        ),
      },
      (response) => {
        setSaving(false);
        if (!response || response.error || !Array.isArray(response.tests)) {
          setSaveFailure(response || {});
          return;
        }
        onContinue();
      },
    );
  };

  if (loadFailed) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "analyzer.setup.assays.loadError" })}
      />
    );
  }
  if (!mapping) {
    return (
      <Loading
        small
        withOverlay={false}
        description={intl.formatMessage({
          id: "analyzer.setup.assays.loading",
        })}
      />
    );
  }

  const on = assays.filter(
    (assay) => choices[assay.sourceRowKey]?.enabled,
  ).length;

  return (
    <div className="analyzer-setup__assays">
      <p>{intl.formatMessage({ id: "analyzer.setup.assays.help" })}</p>
      {assays.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "analyzer.setup.assays.none" })}
        />
      ) : (
        <>
          <p className="analyzer-setup__assays-count">
            {intl.formatMessage(
              { id: "analyzer.setup.assays.count" },
              { on, total: assays.length },
            )}
          </p>
          <ul className="analyzer-setup__assay-list">
            {assays.map((assay) => {
              const code = assay.sourceRowKey;
              const choice = choices[code] || {};
              const assayLabel = assay.testNameHint
                ? `${assay.rawCode} · ${assay.testNameHint}`
                : assay.rawCode;
              return (
                <li key={code} data-testid={`analyzer-assay-${code}`}>
                  <Checkbox
                    id={`analyzer-assay-enabled-${code}`}
                    aria-label={assayLabel}
                    labelText={assayLabel}
                    checked={Boolean(choice.enabled)}
                    onChange={(_, { checked }) =>
                      choose(code, { enabled: checked })
                    }
                  />
                  <span className="analyzer-setup__assay-match">
                    {intl.formatMessage(
                      { id: matchMessage(assay) },
                      { name: assay.selectedTest?.name },
                    )}
                  </span>
                  <TextInput
                    id={`analyzer-assay-code-${code}`}
                    size="sm"
                    labelText={intl.formatMessage(
                      { id: "analyzer.setup.assays.code" },
                      { code: assay.rawCode },
                    )}
                    placeholder={assay.rawCode}
                    value={choice.instrumentCode || ""}
                    disabled={!choice.enabled}
                    onChange={(event) =>
                      choose(code, { instrumentCode: event.target.value })
                    }
                  />
                </li>
              );
            })}
          </ul>
        </>
      )}
      {saveFailure && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "analyzer.setup.assays.saveError" })}
          subtitle={analyzerErrorText(intl, saveFailure, null)}
        />
      )}
      <div className="analyzer-setup__verify-actions">
        <Button
          type="button"
          renderIcon={ArrowRight}
          disabled={saving}
          onClick={save}
        >
          {intl.formatMessage({ id: "analyzer.setup.assays.continue" })}
        </Button>
      </div>
    </div>
  );
};

export default AnalyzerAssaysSetup;
