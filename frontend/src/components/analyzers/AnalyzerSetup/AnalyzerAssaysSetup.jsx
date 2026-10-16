import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Loading,
  Search,
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
 * Which of the profile's assays this instrument runs, as on its host test code
 * table. An assay that is off is not mapped in Verify; its results are held if
 * they ever arrive.
 */
const AnalyzerAssaysSetup = ({ analyzerId, onContinue }) => {
  const intl = useIntl();
  const [mapping, setMapping] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [choices, setChoices] = useState({});
  const [saving, setSaving] = useState(false);
  const [saveFailure, setSaveFailure] = useState(null);
  const [filter, setFilter] = useState("");

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
              { enabled: test.enabled !== false },
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
      choices[assay.sourceRowKey]?.enabled !== (assay.enabled !== false),
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
            : { enabled: choices[test.sourceRowKey].enabled }),
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
  const needle = filter.trim().toLowerCase();
  const shown = needle
    ? assays.filter((assay) =>
        [assay.rawCode, assay.testNameHint, assay.selectedTest?.name]
          .filter(Boolean)
          .some((text) => text.toLowerCase().includes(needle)),
      )
    : assays;
  // Assays the catalog already matched first; the rest need a test chosen in
  // Verify if they are on.
  const groups = [
    {
      key: "matched",
      assays: shown.filter((assay) => assay.mappingState === "BOUND"),
    },
    {
      key: "unmatched",
      assays: shown.filter((assay) => assay.mappingState !== "BOUND"),
    },
  ];
  const setAll = (enabled) =>
    setChoices((current) =>
      Object.fromEntries(
        Object.entries(current).map(([code, choice]) => [
          code,
          { ...choice, enabled },
        ]),
      ),
    );

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
          <div className="analyzer-setup__assays-toolbar">
            <Search
              id="analyzer-setup-assays-search"
              size="md"
              labelText={intl.formatMessage({
                id: "analyzer.setup.assays.search",
              })}
              placeholder={intl.formatMessage({
                id: "analyzer.setup.assays.search",
              })}
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
            <div className="analyzer-setup__assays-bulk">
              <Button kind="ghost" size="sm" onClick={() => setAll(true)}>
                {intl.formatMessage({ id: "analyzer.setup.assays.allOn" })}
              </Button>
              <Button kind="ghost" size="sm" onClick={() => setAll(false)}>
                {intl.formatMessage({ id: "analyzer.setup.assays.allOff" })}
              </Button>
            </div>
          </div>
          <p className="analyzer-setup__assays-count">
            {intl.formatMessage(
              { id: "analyzer.setup.assays.count" },
              { on, total: assays.length },
            )}
          </p>
          {groups.map(
            (group) =>
              group.assays.length > 0 && (
                <section
                  key={group.key}
                  className="analyzer-setup__assay-group"
                  aria-labelledby={`analyzer-assay-group-${group.key}`}
                  data-testid={`analyzer-assays-group-${group.key}`}
                >
                  <h5 id={`analyzer-assay-group-${group.key}`}>
                    {intl.formatMessage(
                      { id: `analyzer.setup.assays.group.${group.key}` },
                      { count: group.assays.length },
                    )}
                  </h5>
                  <ul className="analyzer-setup__assay-list">
                    {group.assays.map((assay) => {
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
                        </li>
                      );
                    })}
                  </ul>
                </section>
              ),
          )}
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
      <div className="analyzer-setup__assays-actions">
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
