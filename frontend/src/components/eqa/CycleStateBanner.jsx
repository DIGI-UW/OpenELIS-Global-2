import React, { useState } from "react";
import {
  Accordion,
  AccordionItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { CycleStatusTag, hintStyle } from "./eqaCommon";

/**
 * FR-V2.5-16: the cycle's state and how it got there, on every page that acts on
 * a cycle. The history is the cycle-transition audit table, so a manual override
 * shows who forced it and the reason they gave.
 *
 * The history loads when the accordion is first opened, not on render
 * — most visits to a workbench never ask for it.
 */
const CycleStateBanner = ({ cycleId, status, hint }) => {
  const intl = useIntl();
  const t = (id, defaultMessage) => intl.formatMessage({ id, defaultMessage });
  const [transitions, setTransitions] = useState(null);

  const loadHistory = () => {
    if (transitions !== null || !cycleId) {
      return;
    }
    getFromOpenElisServer(`/rest/eqa/cycles/${cycleId}/transitions`, (rows) =>
      setTransitions(rows || []),
    );
  };

  return (
    <Tile style={{ marginBottom: "1rem" }}>
      <CycleStatusTag status={status} />
      {hint && (
        <span style={{ ...hintStyle, marginLeft: "0.5rem" }}>{hint}</span>
      )}
      <Accordion size="sm">
        <AccordionItem
          title={t("eqa.cycle.history", "Cycle history")}
          onHeadingClick={loadHistory}
        >
          {transitions === null ? (
            <p style={hintStyle}>{t("label.loading", "Loading...")}</p>
          ) : transitions.length === 0 ? (
            <p style={hintStyle}>
              {t(
                "eqa.cycle.history.empty",
                "No state changes recorded for this cycle yet.",
              )}
            </p>
          ) : (
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {t("eqa.cycle.history.when", "When")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.cycle.history.state", "State")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.cycle.history.trigger", "Trigger")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.cycle.history.reason", "Reason")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {transitions.map((row) => (
                  <TableRow key={row.id}>
                    <TableCell>{(row.occurredAt || "").slice(0, 19)}</TableCell>
                    <TableCell>
                      <CycleStatusTag status={row.newState} />
                    </TableCell>
                    <TableCell>
                      {row.triggerType === "MANUAL"
                        ? t("eqa.cycle.history.manual", "Manual override")
                        : row.triggerEvent || row.triggerType}
                    </TableCell>
                    <TableCell>{row.reason || "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </AccordionItem>
      </Accordion>
    </Tile>
  );
};

export default CycleStateBanner;
