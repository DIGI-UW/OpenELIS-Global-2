import React, { useEffect, useState } from "react";
import {
  Stack,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tile,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * OGC-949 M11 / OGC-959..960 — Analyzers section (read-only).
 *
 * Lists the analyzers that can run this test, derived from analyzer test-code
 * mappings (GET /rest/test-catalog/tests/{id}/analyzers). The mappings' source
 * of truth is the analyzer record, edited on the Analyzer configuration screen —
 * so this section is read-only (no writes), with an info card making that clear.
 */
const AnalyzersSection = ({ testId }) => {
  const intl = useIntl();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [analyzers, setAnalyzers] = useState([]);

  useEffect(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/analyzers`,
      (res) => {
        setLoading(false);
        if (!res) {
          setError(true);
          return;
        }
        setAnalyzers(res.analyzers || []);
      },
    );
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
          id: "label.testCatalog.analyzers.loadError",
        })}
      />
    );
  }

  return (
    <Stack gap={6} data-testid="analyzers-section">
      <Tile>
        <FormattedMessage id="label.testCatalog.analyzers.infoCard" />
      </Tile>
      {analyzers.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.analyzers.empty",
          })}
        />
      ) : (
        <Table size="lg" useZebraStyles aria-label="analyzers">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.analyzers.col.analyzer" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.analyzers.col.testName" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {analyzers.map((a, idx) => (
              <TableRow
                key={`${a.analyzerId}-${idx}`}
                data-testid={`analyzer-row-${a.analyzerId}`}
              >
                <TableCell>{a.analyzerName}</TableCell>
                <TableCell>{a.analyzerTestName}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Stack>
  );
};

export default AnalyzersSection;
