import React, { useState } from "react";
import {
  Stack,
  Tile,
  Tag,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TextInput,
  Checkbox,
  Button,
} from "@carbon/react";
import { Save } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

const LIMIT_TYPES = [
  {
    type: "MAXIMUM",
    labelKey: "compliance.limitType.high",
    fallback: "High Limit ≤",
    tagType: "red",
    hasUpper: true,
    hasLower: false,
    hintKey: "compliance.limitType.high.hint",
    hintFallback: "Non-compliant if result exceeds upper limit",
  },
  {
    type: "MINIMUM",
    labelKey: "compliance.limitType.low",
    fallback: "Low Limit ≥",
    tagType: "blue",
    hasUpper: false,
    hasLower: true,
    hintKey: "compliance.limitType.low.hint",
    hintFallback: "Non-compliant if result is below lower limit",
  },
  {
    type: "RANGE",
    labelKey: "compliance.limitType.range",
    fallback: "Normal Range",
    tagType: "teal",
    hasUpper: true,
    hasLower: true,
    hintKey: "compliance.limitType.range.hint",
    hintFallback: "Compliant within lower–upper range",
  },
  {
    type: "BORDERLINE",
    labelKey: "compliance.limitType.borderline",
    fallback: "Borderline (Advisory)",
    tagType: "warm-gray",
    hasUpper: true,
    hasLower: true,
    hintKey: "compliance.limitType.borderline.hint",
    hintFallback: "Advisory zone — triggers review flag, not fail",
  },
  {
    type: "DESCRIPTIVE",
    labelKey: "compliance.limitType.descriptive",
    fallback: "Qualitative / Descriptive",
    tagType: "purple",
    hasUpper: false,
    hasLower: false,
    hasText: true,
    hintKey: "compliance.limitType.descriptive.hint",
    hintFallback: "Text value evaluated by analyst",
  },
];

function MultiLimitForm({
  testCode,
  testName,
  unit: defaultUnit,
  existingLimits,
  onSave,
  onCancel,
}) {
  const intl = useIntl();

  const initEnabled = () => {
    const en = {};
    LIMIT_TYPES.forEach((lt) => {
      en[lt.type] = !!existingLimits?.some((l) => l.type === lt.type);
    });
    return en;
  };
  const initValues = () => {
    const vals = {};
    LIMIT_TYPES.forEach((lt) => {
      const existing = existingLimits?.find((l) => l.type === lt.type);
      vals[lt.type] = {
        lower: existing?.lower ?? "",
        upper: existing?.upper ?? "",
        unit: existing?.unit ?? defaultUnit ?? "",
        note: existing?.note ?? "",
        descriptive: existing?.descriptive ?? "",
      };
    });
    return vals;
  };

  const [enabled, setEnabled] = useState(initEnabled);
  const [values, setValues] = useState(initValues);
  const [submitted, setSubmitted] = useState(false);
  // Per-row server-side errors keyed by limit type, set by callers via the
  // optional `serverErrors` prop on subsequent renders.
  const [serverErrors, setServerErrors] = useState({});

  const toggleType = (type) => {
    setEnabled((prev) => ({ ...prev, [type]: !prev[type] }));
  };
  const updateValue = (type, field, val) => {
    setValues((prev) => ({
      ...prev,
      [type]: { ...prev[type], [field]: val },
    }));
  };

  const isNumeric = (s) =>
    s !== "" && s !== null && s !== undefined && !Number.isNaN(Number(s));

  // Per-row client validation. Returns {[type]: errorMessage} for any rows that
  // have problems; empty object means OK.
  const computeRowErrors = () => {
    const errs = {};
    LIMIT_TYPES.forEach((lt) => {
      if (!enabled[lt.type]) return;
      const v = values[lt.type];
      if (lt.type === "MAXIMUM") {
        if (!isNumeric(v.upper)) errs[lt.type] = "missing-upper";
      } else if (lt.type === "MINIMUM") {
        if (!isNumeric(v.lower)) errs[lt.type] = "missing-lower";
      } else if (lt.type === "RANGE" || lt.type === "BORDERLINE") {
        if (!isNumeric(v.lower) || !isNumeric(v.upper)) {
          errs[lt.type] = "missing-bounds";
        } else if (Number(v.lower) > Number(v.upper)) {
          errs[lt.type] = "lower-gt-upper";
        }
      } else if (lt.type === "DESCRIPTIVE") {
        if (!v.descriptive || !v.descriptive.trim()) {
          errs[lt.type] = "missing-descriptive";
        }
      }
    });
    return errs;
  };

  const rowErrors = computeRowErrors();
  const hasAnyError = Object.keys(rowErrors).length > 0;

  const errorMessages = {
    "missing-upper": intl.formatMessage({
      id: "compliance.validation.missingUpper",
      defaultMessage: "Upper limit is required.",
    }),
    "missing-lower": intl.formatMessage({
      id: "compliance.validation.missingLower",
      defaultMessage: "Lower limit is required.",
    }),
    "missing-bounds": intl.formatMessage({
      id: "compliance.validation.missingBounds",
      defaultMessage: "Both lower and upper bounds are required.",
    }),
    "lower-gt-upper": intl.formatMessage({
      id: "compliance.validation.lowerGtUpper",
      defaultMessage: "Lower bound must be less than or equal to upper bound.",
    }),
    "missing-descriptive": intl.formatMessage({
      id: "compliance.validation.missingDescriptive",
      defaultMessage: "Qualitative value is required.",
    }),
  };

  const handleSave = () => {
    setSubmitted(true);
    if (hasAnyError) return;
    setServerErrors({});
    const limits = LIMIT_TYPES.filter((lt) => enabled[lt.type]).map((lt) => ({
      type: lt.type,
      lower: values[lt.type].lower,
      upper: values[lt.type].upper,
      unit: values[lt.type].unit,
      note: values[lt.type].note,
      descriptive: values[lt.type].descriptive,
    }));
    // onSave may return a Promise that resolves to {[type]: serverErrorMsg}
    // when one or more inserts fail. We surface those per-row.
    const result = onSave(limits);
    if (result && typeof result.then === "function") {
      result.then((errs) => {
        if (errs && typeof errs === "object" && Object.keys(errs).length > 0) {
          setServerErrors(errs);
        }
      });
    }
  };

  return (
    <Tile style={{ padding: "1rem", background: "var(--cds-layer-02)" }}>
      <h6 style={{ marginBottom: "0.5rem" }}>
        <FormattedMessage
          id="compliance.multiLimit.editHeading"
          defaultMessage="Configure Limits"
        />{" "}
        — {testName}
      </h6>
      <p
        style={{
          fontSize: "0.75rem",
          color: "var(--cds-text-02)",
          marginBottom: "1rem",
        }}
      >
        <FormattedMessage
          id="compliance.multiLimit.hint"
          defaultMessage="Enable one or more limit types for this test. At least one limit type must be enabled."
        />
      </p>

      <Table size="sm" style={{ marginBottom: "1rem" }}>
        <TableHead>
          <TableRow>
            <TableHeader style={{ width: "3rem" }}> </TableHeader>
            <TableHeader style={{ width: "10rem" }}>
              <FormattedMessage
                id="compliance.multiLimit.type"
                defaultMessage="Limit Type"
              />
            </TableHeader>
            <TableHeader>
              <FormattedMessage
                id="compliance.multiLimit.lower"
                defaultMessage="Lower / Min"
              />
            </TableHeader>
            <TableHeader>
              <FormattedMessage
                id="compliance.multiLimit.upper"
                defaultMessage="Upper / Max"
              />
            </TableHeader>
            <TableHeader style={{ width: "6rem" }}>
              <FormattedMessage
                id="compliance.multiLimit.unit"
                defaultMessage="Unit"
              />
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {LIMIT_TYPES.map((lt) => {
            const isEnabled = enabled[lt.type];
            const vals = values[lt.type];
            const label = intl.formatMessage({
              id: lt.labelKey,
              defaultMessage: lt.fallback,
            });
            const rowError = isEnabled && submitted ? rowErrors[lt.type] : null;
            const rowServerError = isEnabled ? serverErrors[lt.type] : null;
            const inputInvalid = !!rowError;
            const lowerInvalid =
              inputInvalid &&
              (rowError === "missing-lower" ||
                rowError === "missing-bounds" ||
                rowError === "lower-gt-upper");
            const upperInvalid =
              inputInvalid &&
              (rowError === "missing-upper" ||
                rowError === "missing-bounds" ||
                rowError === "lower-gt-upper");
            const descriptiveInvalid =
              inputInvalid && rowError === "missing-descriptive";
            // Mockup: DESCRIPTIVE renders a single wide text input that
            // spans Lower + Upper + Unit instead of three separate cells.
            const isDescriptive = lt.type === "DESCRIPTIVE";
            // Mockup: when BORDERLINE is enabled, the row gets a faint
            // yellow tint (.mlt-borderline-active) and an Advisory Note
            // sub-row appears below it.
            const borderlineHighlight =
              lt.type === "BORDERLINE" && isEnabled
                ? { background: "#fffbf0" }
                : undefined;
            return (
              <React.Fragment key={lt.type}>
                <TableRow
                  style={{
                    opacity: isEnabled ? 1 : 0.45,
                    ...borderlineHighlight,
                  }}
                >
                  <TableCell>
                    <Checkbox
                      id={`mlt-${testCode}-${lt.type}`}
                      labelText=""
                      checked={isEnabled}
                      onChange={() => toggleType(lt.type)}
                    />
                  </TableCell>
                  <TableCell>
                    <Tag size="sm" type={lt.tagType}>
                      {label}
                    </Tag>
                  </TableCell>
                  {isDescriptive ? (
                    <TableCell colSpan={3}>
                      <TextInput
                        id={`mlt-${testCode}-${lt.type}-text`}
                        labelText=""
                        hideLabel
                        value={vals.descriptive}
                        onChange={(e) =>
                          updateValue(lt.type, "descriptive", e.target.value)
                        }
                        disabled={!isEnabled}
                        size="sm"
                        placeholder={intl.formatMessage({
                          id: "compliance.multiLimit.descriptive.placeholder",
                          defaultMessage:
                            "Accepted value(s) e.g., No odor, Absent",
                        })}
                        invalid={descriptiveInvalid}
                      />
                    </TableCell>
                  ) : (
                    <>
                      <TableCell>
                        {lt.hasLower ? (
                          <TextInput
                            id={`mlt-${testCode}-${lt.type}-lower`}
                            labelText=""
                            hideLabel
                            value={vals.lower}
                            onChange={(e) =>
                              updateValue(lt.type, "lower", e.target.value)
                            }
                            disabled={!isEnabled}
                            size="sm"
                            placeholder={intl.formatMessage({
                              id: "compliance.threshold.placeholder.lower",
                              defaultMessage: "Lower / Min",
                            })}
                            invalid={lowerInvalid}
                          />
                        ) : (
                          <span
                            style={{
                              color: "var(--cds-text-placeholder)",
                              fontStyle: "italic",
                            }}
                          >
                            —
                          </span>
                        )}
                      </TableCell>
                      <TableCell>
                        {lt.hasUpper ? (
                          <TextInput
                            id={`mlt-${testCode}-${lt.type}-upper`}
                            labelText=""
                            hideLabel
                            value={vals.upper}
                            onChange={(e) =>
                              updateValue(lt.type, "upper", e.target.value)
                            }
                            disabled={!isEnabled}
                            size="sm"
                            placeholder={intl.formatMessage({
                              id: "compliance.threshold.placeholder.upper",
                              defaultMessage: "Upper / Max",
                            })}
                            invalid={upperInvalid}
                          />
                        ) : (
                          <span
                            style={{
                              color: "var(--cds-text-placeholder)",
                              fontStyle: "italic",
                            }}
                          >
                            —
                          </span>
                        )}
                      </TableCell>
                      <TableCell>
                        <TextInput
                          id={`mlt-${testCode}-${lt.type}-unit`}
                          labelText=""
                          hideLabel
                          value={vals.unit}
                          onChange={(e) =>
                            updateValue(lt.type, "unit", e.target.value)
                          }
                          disabled={!isEnabled}
                          size="sm"
                          placeholder={intl.formatMessage({
                            id: "compliance.threshold.placeholder.unit",
                            defaultMessage: "Unit",
                          })}
                        />
                      </TableCell>
                    </>
                  )}
                </TableRow>
                {/* Mockup's mlt-note-row: only shown when BORDERLINE is
                    active, full-width, yellow tint. */}
                {lt.type === "BORDERLINE" && isEnabled && (
                  <TableRow style={{ background: "#fffbf0" }}>
                    <TableCell />
                    <TableCell colSpan={4}>
                      <label
                        htmlFor={`mlt-${testCode}-${lt.type}-note`}
                        style={{
                          fontSize: "0.6875rem",
                          fontWeight: 600,
                          color: "var(--cds-text-02)",
                          display: "block",
                          marginBottom: "0.25rem",
                          textTransform: "uppercase",
                          letterSpacing: "0.04em",
                        }}
                      >
                        <FormattedMessage
                          id="compliance.multiLimit.advisoryNote"
                          defaultMessage="Advisory Note"
                        />
                      </label>
                      <TextInput
                        id={`mlt-${testCode}-${lt.type}-note`}
                        labelText=""
                        hideLabel
                        value={vals.note}
                        onChange={(e) =>
                          updateValue(lt.type, "note", e.target.value)
                        }
                        size="sm"
                        placeholder={intl.formatMessage({
                          id: "compliance.multiLimit.advisoryNote.placeholder",
                          defaultMessage:
                            "e.g., Requires re-sampling within 24h",
                        })}
                      />
                    </TableCell>
                  </TableRow>
                )}
                {(rowError || rowServerError) && (
                  <TableRow>
                    <TableCell colSpan={5} style={{ paddingTop: 0 }}>
                      <p
                        style={{
                          color: "var(--cds-support-error)",
                          fontSize: "0.75rem",
                          margin: 0,
                        }}
                      >
                        {rowServerError || errorMessages[rowError]}
                      </p>
                    </TableCell>
                  </TableRow>
                )}
              </React.Fragment>
            );
          })}
        </TableBody>
      </Table>

      <Stack orientation="horizontal" gap={3}>
        <Button
          kind="primary"
          size="sm"
          renderIcon={Save}
          onClick={handleSave}
          disabled={!Object.values(enabled).some(Boolean)}
        >
          <FormattedMessage
            id="compliance.button.saveLimits"
            defaultMessage="Save Limits"
          />
        </Button>
        <Button kind="ghost" size="sm" onClick={onCancel}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
      </Stack>
    </Tile>
  );
}

export default MultiLimitForm;
export { LIMIT_TYPES };
