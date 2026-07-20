import React, { useEffect, useState } from "react";
import {
  Stack,
  Tile,
  Button,
  Link,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Launch } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";

const REFLEX_RULES_URL = "/MasterListsPage/reflex";
const CALC_URL = "/MasterListsPage/calculatedValue";

/**
 * OGC-949 / OGC-764 (OGC-998 + OGC-999) — read-only Reflex &amp; Calc section.
 *
 * Surfaces reflex rules triggered by this test and calculations that produce or
 * consume it (GET /rest/test-catalog/{testId}/reflex-calc). Editing happens in
 * Master Lists — every sub-section deep-links there; nothing is editable here.
 */
const ReflexCalcSection = ({ testId }) => {
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [data, setData] = useState({
    reflexRules: [],
    calculatedBy: [],
    feedsInto: [],
  });

  useEffect(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(`/rest/test-catalog/${testId}/reflex-calc`, (res) => {
      setLoading(false);
      if (!res) {
        setError(true);
        return;
      }
      setData({
        reflexRules: res.reflexRules || [],
        calculatedBy: res.calculatedBy || [],
        feedsInto: res.feedsInto || [],
      });
    });
  }, [testId]);

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "label.loading" })}
        withOverlay={false}
      />
    );
  }

  if (error) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.reflexCalc.loadError",
        })}
      />
    );
  }

  const manageButton = (url, labelId) => (
    <Button
      kind="ghost"
      size="sm"
      renderIcon={Launch}
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      as="a"
    >
      {intl.formatMessage({ id: labelId })}
    </Button>
  );

  return (
    <Stack gap={7} data-testid="reflex-calc-section">
      {/* ── Reflex Tests (OGC-998) ── */}
      <Stack gap={4}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <h4>
            <FormattedMessage id="label.testCatalog.reflexCalc.reflex.heading" />
          </h4>
          {manageButton(
            REFLEX_RULES_URL,
            "label.testCatalog.reflexCalc.reflex.manage",
          )}
        </div>
        <p className="cds--label">
          <FormattedMessage id="label.testCatalog.reflexCalc.reflex.helper" />
        </p>
        {data.reflexRules.length === 0 ? (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "label.testCatalog.reflexCalc.reflex.empty",
            })}
          />
        ) : (
          <TableContainer>
            <Table size="lg" useZebraStyles aria-label="reflex-rules">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.reflex.col.rule" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.reflex.col.trigger" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.reflex.col.reflexTests" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.reflexRules.map((r) => (
                  <TableRow key={r.id} data-testid={`reflex-row-${r.id}`}>
                    <TableCell>
                      <Link
                        href={REFLEX_RULES_URL}
                        target="_blank"
                        renderIcon={Launch}
                      >
                        {r.ruleName || "—"}
                      </Link>
                    </TableCell>
                    <TableCell>{r.triggerCondition}</TableCell>
                    <TableCell>{r.reflexTests || "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Stack>

      {/* ── Calculated Results (OGC-999) ── */}
      <Stack gap={4}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <h4>
            <FormattedMessage id="label.testCatalog.reflexCalc.calc.heading" />
          </h4>
          {manageButton(CALC_URL, "label.testCatalog.reflexCalc.calc.manage")}
        </div>
        <p className="cds--label">
          <FormattedMessage id="label.testCatalog.reflexCalc.calc.helper" />
        </p>

        <Tile>
          <strong>
            <FormattedMessage id="label.testCatalog.reflexCalc.calc.feedsInto" />
          </strong>
          {data.feedsInto.length === 0 ? (
            <p>
              <FormattedMessage id="label.testCatalog.reflexCalc.calc.empty" />
            </p>
          ) : (
            <Table size="sm" aria-label="calc-feeds-into">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.calc.col.name" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.calc.col.formula" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.calc.col.output" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.feedsInto.map((c) => (
                  <TableRow key={c.id} data-testid={`feeds-row-${c.id}`}>
                    <TableCell>{c.name}</TableCell>
                    <TableCell>
                      <code>{c.formula}</code>
                    </TableCell>
                    <TableCell>{c.outputTest || "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Tile>

        <Tile>
          <strong>
            <FormattedMessage id="label.testCatalog.reflexCalc.calc.calculatedBy" />
          </strong>
          {data.calculatedBy.length === 0 ? (
            <p>
              <FormattedMessage id="label.testCatalog.reflexCalc.calc.empty" />
            </p>
          ) : (
            <Table size="sm" aria-label="calc-calculated-by">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.calc.col.name" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.reflexCalc.calc.col.formula" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.calculatedBy.map((c) => (
                  <TableRow key={c.id} data-testid={`calcby-row-${c.id}`}>
                    <TableCell>{c.name}</TableCell>
                    <TableCell>
                      <code>{c.formula}</code>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Tile>
      </Stack>
    </Stack>
  );
};

export default ReflexCalcSection;
