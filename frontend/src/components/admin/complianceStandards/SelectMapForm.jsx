import React, { useState } from "react";
import {
  Stack,
  Tile,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Select,
  SelectItem,
  Button,
} from "@carbon/react";
import { Save } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

const STATUS_OPTIONS = [
  {
    value: "COMPLIANT",
    labelKey: "compliance.status.compliantWithIcon",
    fallback: "Compliant ✓",
    tagType: "green",
  },
  {
    value: "BORDERLINE",
    labelKey: "compliance.status.borderlineWithIcon",
    fallback: "Borderline ⚑",
    tagType: "warm-gray",
  },
  {
    value: "NON_COMPLIANT",
    labelKey: "compliance.status.nonCompliantWithIcon",
    fallback: "Non-Compliant ✗",
    tagType: "red",
  },
];

function SelectMapForm({
  testCode,
  testName,
  options,
  existingMap,
  onSave,
  onCancel,
}) {
  const intl = useIntl();
  // Default unset options to COMPLIANT (mockup convention — green tint
  // is the safe default; admin opts in to BORDERLINE / NON_COMPLIANT).
  const [valueMap, setValueMap] = useState(() => {
    const init = {};
    (options || []).forEach((opt) => {
      init[opt] = existingMap?.[opt] || "COMPLIANT";
    });
    return init;
  });

  const handleStatusChange = (option, status) => {
    setValueMap((prev) => ({ ...prev, [option]: status }));
  };

  return (
    <Tile style={{ padding: "1rem", background: "var(--cds-layer-02)" }}>
      <h6 style={{ marginBottom: "0.5rem" }}>
        <FormattedMessage
          id="compliance.selectMap.editHeading"
          defaultMessage="Compliance Mapping"
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
          id="compliance.selectMap.hint"
          defaultMessage="Map each result option to its compliance status. Options come from the test catalog."
        />
      </p>

      <Table size="sm" style={{ marginBottom: "1rem" }}>
        <TableHead>
          <TableRow>
            <TableHeader>
              <FormattedMessage
                id="compliance.selectMap.option"
                defaultMessage="Result Option (from catalog)"
              />
            </TableHeader>
            <TableHeader style={{ width: "12rem" }}>
              <FormattedMessage
                id="compliance.selectMap.status"
                defaultMessage="Compliance Status"
              />
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {(options || []).map((opt) => {
            const currentStatus = valueMap[opt];
            // Carbon background tokens — adapt to theme + high-contrast modes
            // automatically. Earlier rgba() literals locked the row tint to
            // the white theme palette.
            const tint =
              currentStatus === "COMPLIANT"
                ? "var(--cds-background-success, var(--cds-support-02-inverse, #defbe6))"
                : currentStatus === "BORDERLINE"
                  ? "var(--cds-background-warning, #fcf4d6)"
                  : currentStatus === "NON_COMPLIANT"
                    ? "var(--cds-background-error, var(--cds-support-01-inverse, #ffd7d9))"
                    : "transparent";
            return (
              <TableRow key={opt} style={{ background: tint }}>
                <TableCell style={{ fontWeight: 500 }}>{opt}</TableCell>
                <TableCell>
                  <Select
                    id={`smc-${testCode}-${opt.replace(/\s/g, "-")}`}
                    labelText={intl.formatMessage(
                      {
                        id: "compliance.selectMap.statusForOption",
                        defaultMessage: "Compliance status for {option}",
                      },
                      { option: opt },
                    )}
                    hideLabel
                    value={currentStatus}
                    onChange={(e) => handleStatusChange(opt, e.target.value)}
                    size="sm"
                  >
                    {STATUS_OPTIONS.map((so) => (
                      <SelectItem
                        key={so.value}
                        value={so.value}
                        text={intl.formatMessage({
                          id: so.labelKey,
                          defaultMessage: so.fallback,
                        })}
                      />
                    ))}
                  </Select>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>

      <Stack orientation="horizontal" gap={3}>
        <Button
          kind="primary"
          size="sm"
          renderIcon={Save}
          onClick={() => onSave(valueMap)}
        >
          <FormattedMessage
            id="compliance.button.saveMapping"
            defaultMessage="Save Mapping"
          />
        </Button>
        <Button kind="ghost" size="sm" onClick={onCancel}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
      </Stack>
    </Tile>
  );
}

export default SelectMapForm;
export { STATUS_OPTIONS };
