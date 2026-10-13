import React, { useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Modal,
  Tile,
  Tooltip,
} from "@carbon/react";
import { Download, DocumentPdf } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { labNow } from "../../utils/labClock";
import { postToOpenElisServer, toLocalIsoDate } from "../../utils/Utils";
import { useServerData } from "../../utils/useServerData";
import { exportEventsUrl, summaryUrl } from "./managementApi";
import {
  buildPdf,
  loadTables,
  saveFile,
  sectionsOnPage,
  toCsv,
} from "./managementExport";
import "./ExportBar.css";

const ExportBar = ({ scope, scopeLabel }) => {
  const intl = useIntl();
  const summary = useServerData(summaryUrl(scope));
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);
  const [choices, setChoices] = useState(null);

  const audit = (kind, tables) =>
    new Promise((resolve, reject) =>
      postToOpenElisServer(
        exportEventsUrl(),
        JSON.stringify({
          kind,
          scope,
          sections: tables.map((table) => ({
            id: table.section.id,
            rowCount: table.body.length,
          })),
        }),
        (status) => (status === 204 ? resolve() : reject(status)),
      ),
    );

  const run = (work) => {
    setBusy(true);
    setFailed(false);
    work()
      .catch(() => setFailed(true))
      .finally(() => setBusy(false));
  };

  const fileStem = `management-overview-${scope}-${toLocalIsoDate(labNow())}`;

  const exportPdf = () =>
    run(() =>
      loadTables(sectionsOnPage(), scope, intl).then((tables) =>
        audit("PDF", tables)
          .then(() =>
            buildPdf(tables, {
              title: intl.formatMessage({ id: "label.mgmt.title" }),
              labName: summary.data?.labName || "",
              cover: [
                [
                  intl.formatMessage({ id: "label.mgmt.export.lab" }),
                  summary.data?.labName || "—",
                ],
                [
                  intl.formatMessage({ id: "label.dashboard.scope" }),
                  scopeLabel,
                ],
                [
                  intl.formatMessage({ id: "label.mgmt.export.generated" }),
                  labNow().toLocaleString(),
                ],
              ],
              chartUnavailable: intl.formatMessage({
                id: "label.mgmt.export.chartUnavailable",
              }),
              pageLabel: (page, pages) =>
                intl.formatMessage(
                  { id: "label.mgmt.export.page" },
                  { 0: page, 1: pages },
                ),
            }),
          )
          .then((doc) => doc.save(`${fileStem}.pdf`)),
      ),
    );

  const downloadCsv = () => {
    const chosen = sectionsOnPage().filter((s) => choices.includes(s.id));
    setChoices(null);
    run(() =>
      loadTables(chosen, scope, intl).then((tables) =>
        audit("CSV", tables).then(() =>
          tables.forEach((table) =>
            saveFile(
              `${fileStem}-${table.section.id}.csv`,
              toCsv(table),
              "text/csv;charset=utf-8",
            ),
          ),
        ),
      ),
    );
  };

  return (
    <Tile className="mgmt-export" data-testid="mgmt-export">
      <h3>
        <FormattedMessage id="label.mgmt.export.title" />
      </h3>
      {failed && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "label.mgmt.export.failed" })}
        />
      )}
      <div className="mgmt-export__actions">
        <Button
          size="sm"
          renderIcon={DocumentPdf}
          disabled={busy}
          onClick={exportPdf}
          data-testid="mgmt-export-pdf"
        >
          <FormattedMessage id="label.mgmt.export.pdf" />
        </Button>
        <Button
          size="sm"
          kind="secondary"
          renderIcon={Download}
          disabled={busy}
          onClick={() => setChoices(sectionsOnPage().map((s) => s.id))}
          data-testid="mgmt-export-csv"
        >
          <FormattedMessage id="label.mgmt.export.csv" />
        </Button>
        <Tooltip
          align="top"
          label={intl.formatMessage({
            id: "label.mgmt.export.scheduleTooltip",
          })}
        >
          <span className="mgmt-export__schedule" tabIndex={0}>
            <Button size="sm" kind="ghost" disabled>
              <FormattedMessage id="label.mgmt.export.schedule" />
            </Button>
          </span>
        </Tooltip>
      </div>
      {choices && (
        <Modal
          open
          modalHeading={intl.formatMessage({ id: "label.mgmt.export.csv" })}
          primaryButtonText={intl.formatMessage({
            id: "label.mgmt.export.download",
          })}
          primaryButtonDisabled={choices.length === 0}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestClose={() => setChoices(null)}
          onRequestSubmit={downloadCsv}
          data-testid="mgmt-export-picker"
        >
          <p className="mgmt-export__picker-help">
            <FormattedMessage id="label.mgmt.export.chooseSections" />
          </p>
          {sectionsOnPage().map((section) => (
            <Checkbox
              key={section.id}
              id={`mgmt-export-${section.id}`}
              labelText={intl.formatMessage(
                { id: section.titleKey },
                { 0: "" },
              )}
              checked={choices.includes(section.id)}
              onChange={(event, { checked }) =>
                setChoices((current) =>
                  checked
                    ? [...current, section.id]
                    : current.filter((id) => id !== section.id),
                )
              }
            />
          ))}
        </Modal>
      )}
    </Tile>
  );
};

export default ExportBar;
