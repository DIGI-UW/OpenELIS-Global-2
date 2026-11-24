import React, { useEffect, useMemo, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Button,
  Checkbox,
  CopyButton,
  FilterableMultiSelect,
  InlineLoading,
  InlineNotification,
  NumberInput,
  Tag,
  Tile,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { newSampleRow } from "./SampleTestSection";

export const SUGGESTED_TESTS_API = "/rest/compliance/standards/suggested-tests";

/** The standard ids the order carries, as strings, in selection order. */
export const selectedStandardIds = (orderData) => {
  const raw =
    orderData?.sampleOrderItems?.environmentalFields?.complianceStandards;
  try {
    const ids = raw ? JSON.parse(raw) : [];
    return Array.isArray(ids) ? ids.map(String) : [];
  } catch {
    return [];
  }
};

/**
 * A date the server sends as an ISO string or as [year, month, day] (a Java
 * LocalDate without a date module), as YYYY-MM-DD.
 */
export const isoDay = (value) => {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }
  return value ? String(value).slice(0, 10) : "";
};

const sameText = (a, b) =>
  String(a || "")
    .trim()
    .toLowerCase() ===
  String(b || "")
    .trim()
    .toLowerCase();

const limitText = (intl, limit) => {
  const values = {
    min: limit.min ?? "",
    max: limit.max ?? "",
    target: limit.target ?? "",
    units: limit.units || "",
  };
  const id = ["RANGE", "MINIMUM", "MAXIMUM", "EXACT"].includes(limit.type)
    ? `env.testPlan.limit.${limit.type}`
    : "env.testPlan.limit.other";
  return intl
    .formatMessage({ id }, { ...values, type: limit.type || "" })
    .trim();
};

/**
 * OGC-768: the regulation-driven part of environmental order entry. The order
 * names one or more compliance standards; their tests make the test plan that
 * each sample row starts from, and their sample types can be added at once.
 */
const ComplianceStandardsSection = ({
  orderData,
  setOrderData,
  isReadOnly,
  samples = [],
  setSamples,
  onPlanChange,
}) => {
  const intl = useIntl();
  const mounted = useRef(true);
  const [standards, setStandards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [suggested, setSuggested] = useState([]);
  const [loadedKey, setLoadedKey] = useState(null);
  const [planLoading, setPlanLoading] = useState(false);
  const [planFailed, setPlanFailed] = useState(false);
  const [planAttempt, setPlanAttempt] = useState(0);
  const [excluded, setExcluded] = useState(() => new Set());
  const [quickCounts, setQuickCounts] = useState({});

  const selectedIds = selectedStandardIds(orderData);
  const selectionKey = selectedIds.join(",");

  useEffect(() => {
    mounted.current = true;
    getFromOpenElisServer("/rest/compliance/standards/active", (data) => {
      if (mounted.current) {
        setStandards(Array.isArray(data) ? data : []);
        setLoading(false);
      }
    });
    getFromOpenElisServer("/rest/environmental-sample-types", (data) => {
      if (mounted.current) {
        setSampleTypes(Array.isArray(data) ? data : []);
      }
    });
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!selectionKey) {
      setSuggested([]);
      setPlanFailed(false);
      setLoadedKey("");
      return;
    }
    let current = true;
    setPlanLoading(true);
    setPlanFailed(false);
    getFromOpenElisServer(
      `${SUGGESTED_TESTS_API}?standardIds=${encodeURIComponent(selectionKey)}`,
      (data) => {
        if (!current || !mounted.current) {
          return;
        }
        setPlanLoading(false);
        if (Array.isArray(data)) {
          setSuggested(data);
          setLoadedKey(selectionKey);
        } else {
          setSuggested([]);
          setPlanFailed(true);
        }
      },
    );
    return () => {
      current = false;
    };
  }, [selectionKey, planAttempt]);

  const planTests = useMemo(
    () => suggested.filter((test) => !excluded.has(String(test.testId))),
    [suggested, excluded],
  );
  const planKey = planTests.map((test) => test.testId).join(",");
  const planKnown = loadedKey === selectionKey;
  useEffect(() => {
    if (onPlanChange) {
      onPlanChange(planKnown ? (planKey ? planKey.split(",") : []) : null);
    }
  }, [planKey, planKnown]);
  useEffect(
    () => () => {
      if (onPlanChange) {
        onPlanChange(null);
      }
    },
    [],
  );

  const setSelected = (ids) => {
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          complianceStandards: JSON.stringify(ids),
        },
      },
    }));
  };

  const standardById = (id) =>
    standards.find((standard) => String(standard.id) === String(id));
  const selectedStandards = selectedIds.map(standardById).filter(Boolean);
  const items = standards.map((standard) => ({
    id: String(standard.id),
    label: `${standard.regulationNumber} — ${standard.name} · ${standard.issuingBody}`,
  }));

  const sampleTypeName = (id) => {
    const type = sampleTypes.find(
      (candidate) => String(candidate.id) === String(id),
    );
    return type ? type.value || type.name || type.description : String(id);
  };
  const activeTypeIds = samples
    .map((sample) => String(sample.sampleTypeId || ""))
    .filter(Boolean);
  const blocked = planTests.filter(
    (test) =>
      !(test.sampleTypeIds || []).some((id) =>
        activeTypeIds.includes(String(id)),
      ),
  );

  const chipTypes = [];
  selectedStandards.forEach((standard) =>
    (standard.sampleTypes || []).forEach((name) => {
      const type = sampleTypes.find(
        (candidate) =>
          sameText(candidate.value, name) ||
          sameText(candidate.name, name) ||
          sameText(candidate.description, name),
      );
      if (
        type &&
        !chipTypes.some((chip) => String(chip.id) === String(type.id))
      ) {
        chipTypes.push(type);
      }
    }),
  );
  planTests.forEach((test) =>
    (test.sampleTypeIds || []).forEach((id) => {
      const type = sampleTypes.find(
        (candidate) => String(candidate.id) === String(id),
      );
      if (
        type &&
        !chipTypes.some((chip) => String(chip.id) === String(type.id))
      ) {
        chipTypes.push(type);
      }
    }),
  );

  const addSamples = (sampleTypeId, count) => {
    if (!setSamples) {
      return;
    }
    let remaining = count;
    const filled = samples.map((sample) => {
      if (
        remaining > 0 &&
        !sample.sampleTypeId &&
        !(sample.tests || []).length
      ) {
        remaining -= 1;
        return { ...sample, sampleTypeId: String(sampleTypeId) };
      }
      return sample;
    });
    const rows = Array.from({ length: remaining }, (_, offset) =>
      newSampleRow(filled.length + offset, String(sampleTypeId)),
    );
    setSamples([...filled, ...rows]);
  };

  const skipTest = (testId) =>
    setExcluded((prev) => new Set(prev).add(String(testId)));
  const toggleTest = (testId, checked) =>
    setExcluded((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.delete(String(testId));
      } else {
        next.add(String(testId));
      }
      return next;
    });

  const standardsShared = suggested.filter(
    (test) => test.coveredBy.length > 1,
  ).length;

  return (
    <Tile className="order-section" data-testid="compliance-standards">
      <h4 className="section-title">
        <FormattedMessage id="env.complianceStandards.title" />
      </h4>
      <p className="helper-text" style={{ marginBottom: "1rem" }}>
        <FormattedMessage id="env.complianceStandards.subtitle" />
      </p>

      {loading ? (
        <InlineLoading description={intl.formatMessage({ id: "loading" })} />
      ) : (
        <FilterableMultiSelect
          key={`cs-${selectionKey}`}
          id="complianceStandards"
          titleText={
            <>
              <FormattedMessage id="env.complianceStandards.label" />
              <span className="required-marker"> *</span>
            </>
          }
          placeholder={intl.formatMessage({
            id: "env.complianceStandards.placeholder",
          })}
          items={items}
          itemToString={(item) => item?.label || ""}
          initialSelectedItems={items.filter((item) =>
            selectedIds.includes(item.id),
          )}
          onChange={({ selectedItems }) =>
            setSelected(selectedItems.map((item) => item.id))
          }
          disabled={isReadOnly || items.length === 0}
        />
      )}

      {!loading && items.length === 0 && (
        <p className="helper-text" style={{ marginTop: "0.5rem" }}>
          <FormattedMessage id="env.complianceStandards.empty" />
        </p>
      )}

      {selectedStandards.length > 0 && (
        <div className="compliance-standard-stack">
          {selectedStandards.map((standard) => {
            const id = String(standard.id);
            const linked = suggested.filter((test) =>
              test.coveredBy.some((cover) => String(cover.standardId) === id),
            );
            return (
              <Tile
                key={id}
                className="compliance-standard-card"
                data-testid={`compliance-standard-card-${id}`}
              >
                <div className="compliance-standard-card-head">
                  <strong>{standard.name}</strong>
                  <Tag type="green" size="sm">
                    <FormattedMessage id="env.complianceStandards.active" />
                  </Tag>
                </div>
                <div className="compliance-standard-regulation">
                  <code data-testid={`compliance-standard-regulation-${id}`}>
                    {standard.regulationNumber}
                  </code>
                  <CopyButton
                    iconDescription={intl.formatMessage({
                      id: "env.complianceStandards.copy",
                    })}
                    feedback={intl.formatMessage({
                      id: "env.complianceStandards.copied",
                    })}
                    onClick={() =>
                      navigator.clipboard?.writeText(standard.regulationNumber)
                    }
                  />
                </div>
                <p className="helper-text">
                  <FormattedMessage
                    id="env.complianceStandards.card.details"
                    values={{
                      issuingBody: standard.issuingBody,
                      version: standard.version,
                      effectiveDate: isoDay(standard.effectiveDate),
                    }}
                  />
                </p>
                <p className="helper-text">
                  <FormattedMessage
                    id="env.complianceStandards.card.linkedTests"
                    values={{ count: linked.length }}
                  />
                </p>
                <Accordion size="sm">
                  <AccordionItem
                    title={intl.formatMessage({
                      id: "env.complianceStandards.viewThresholds",
                    })}
                  >
                    <ul data-testid={`compliance-standard-thresholds-${id}`}>
                      {linked.map((test) =>
                        test.thresholds
                          .filter((limit) => String(limit.standardId) === id)
                          .map((limit, index) => (
                            <li key={`${test.testId}-${index}`}>
                              {test.testName}: {limitText(intl, limit)}
                            </li>
                          )),
                      )}
                    </ul>
                  </AccordionItem>
                </Accordion>
                {!isReadOnly && (
                  <Button
                    kind="ghost"
                    size="sm"
                    data-testid={`compliance-standard-remove-${id}`}
                    onClick={() =>
                      setSelected(selectedIds.filter((other) => other !== id))
                    }
                  >
                    <FormattedMessage
                      id="env.complianceStandards.remove"
                      values={{ name: standard.regulationNumber }}
                    />
                  </Button>
                )}
              </Tile>
            );
          })}
        </div>
      )}

      {selectedStandards.length > 0 && (
        <div
          className="compliance-test-plan"
          data-testid="compliance-test-plan"
        >
          <h5>
            <FormattedMessage id="env.testPlan.title" />
          </h5>
          {planLoading && (
            <InlineLoading
              description={intl.formatMessage({ id: "loading" })}
            />
          )}
          {planFailed && (
            <div data-testid="compliance-test-plan-error">
              <InlineNotification
                kind="error"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({ id: "env.testPlan.loadError" })}
              />
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => setPlanAttempt((attempt) => attempt + 1)}
              >
                <FormattedMessage id="common.retry" />
              </Button>
            </div>
          )}
          {!planLoading && !planFailed && suggested.length === 0 && (
            <p className="helper-text">
              <FormattedMessage id="env.testPlan.empty" />
            </p>
          )}
          {suggested.length > 0 && (
            <>
              <p
                className="helper-text"
                data-testid="compliance-test-plan-summary"
              >
                <FormattedMessage
                  id="env.testPlan.summary"
                  values={{
                    count: planTests.length,
                    standards: selectedStandards
                      .map((standard) => standard.regulationNumber)
                      .join(" + "),
                    shared: standardsShared,
                  }}
                />
              </p>
              <ul className="compliance-test-plan-list">
                {suggested.map((test) => {
                  const testId = String(test.testId);
                  return (
                    <li
                      key={testId}
                      data-testid={`compliance-plan-test-${testId}`}
                    >
                      <Checkbox
                        id={`compliance-plan-${testId}`}
                        labelText={test.testName}
                        checked={!excluded.has(testId)}
                        disabled={isReadOnly}
                        onChange={(_, { checked }) =>
                          toggleTest(testId, checked)
                        }
                      />
                      {test.loinc && (
                        <span className="helper-text">{test.loinc}</span>
                      )}
                      {test.coveredBy.map((cover) => (
                        <Tag key={cover.standardId} type="blue" size="sm">
                          {cover.regulationNumber}
                        </Tag>
                      ))}
                      {test.conflicting && (
                        <>
                          <Tag type="magenta" size="sm">
                            <FormattedMessage id="env.testPlan.conflict" />
                          </Tag>
                          <table
                            className="compliance-limit-compare"
                            data-testid={`compliance-plan-limits-${testId}`}
                          >
                            <tbody>
                              {test.thresholds.map((limit, index) => (
                                <tr key={index}>
                                  <th scope="row">{limit.regulationNumber}</th>
                                  <td>{limitText(intl, limit)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </>
                      )}
                    </li>
                  );
                })}
              </ul>
            </>
          )}

          {blocked.length > 0 && (
            <div data-testid="compliance-plan-blocked">
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title={intl.formatMessage(
                  { id: "env.testPlan.blocked.title" },
                  { count: blocked.length },
                )}
              />
              <ul>
                {blocked.map((test) => {
                  const typeId = test.sampleTypeIds[0];
                  return (
                    <li
                      key={test.testId}
                      data-testid={`compliance-plan-blocked-${test.testId}`}
                    >
                      {typeId ? (
                        <FormattedMessage
                          id="env.testPlan.blocked.item"
                          values={{
                            test: test.testName,
                            sampleTypes: test.sampleTypeIds
                              .map(sampleTypeName)
                              .join(", "),
                          }}
                        />
                      ) : (
                        <FormattedMessage
                          id="env.testPlan.blocked.noSampleType"
                          values={{ test: test.testName }}
                        />
                      )}
                      {!isReadOnly && (
                        <>
                          {typeId && (
                            <Button
                              kind="ghost"
                              size="sm"
                              onClick={() => addSamples(typeId, 1)}
                            >
                              <FormattedMessage
                                id="env.testPlan.blocked.add"
                                values={{ sampleType: sampleTypeName(typeId) }}
                              />
                            </Button>
                          )}
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => skipTest(test.testId)}
                          >
                            <FormattedMessage id="env.testPlan.blocked.skip" />
                          </Button>
                        </>
                      )}
                    </li>
                  );
                })}
              </ul>
            </div>
          )}

          {chipTypes.length > 0 && !isReadOnly && (
            <div
              className="compliance-quick-add"
              data-testid="compliance-quick-add"
            >
              <h6>
                <FormattedMessage id="env.quickAdd.title" />
              </h6>
              {chipTypes.map((type) => {
                const typeId = String(type.id);
                const count = quickCounts[typeId] || 1;
                const name = sampleTypeName(typeId);
                return (
                  <div key={typeId} className="compliance-quick-add-chip">
                    <NumberInput
                      id={`compliance-quick-add-count-${typeId}`}
                      label={intl.formatMessage(
                        { id: "env.quickAdd.count" },
                        { sampleType: name },
                      )}
                      min={1}
                      max={50}
                      value={count}
                      size="sm"
                      onChange={(event, { value }) =>
                        setQuickCounts((prev) => ({
                          ...prev,
                          [typeId]: Math.max(
                            1,
                            Math.min(50, Number(value) || 1),
                          ),
                        }))
                      }
                    />
                    <Button
                      kind="tertiary"
                      size="sm"
                      data-testid={`compliance-quick-add-${typeId}`}
                      onClick={() => addSamples(typeId, count)}
                    >
                      <FormattedMessage
                        id="env.quickAdd.add"
                        values={{ count, sampleType: name }}
                      />
                    </Button>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </Tile>
  );
};

export default ComplianceStandardsSection;
