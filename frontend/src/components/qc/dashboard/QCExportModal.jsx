/**
 * QCExportModal Component (OGC-706)
 *
 * Filter dialog for the QC inspector export: instrument (required), control
 * level, and date range. Triggers a CSV or PDF download from the backend export
 * endpoints. The scope is always instrument-bounded, so no "wide export" warning
 * is needed. The endpoints are gated on qa.view.qc server-side; the dashboard
 * hides the entry button when the user lacks it.
 */

import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Button,
  InlineNotification,
} from "@carbon/react";
import { Download, DocumentPdf } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import config from "../../../config.json";
import { toLocalIsoDate } from "../../utils/Utils";

const LEVEL_OPTIONS = [
  { id: "ALL", labelKey: "qc.chart.filter.allLevels" },
  { id: "LOW", labelKey: "qc.chart.filter.levelLow" },
  { id: "NORMAL", labelKey: "qc.chart.filter.levelNormal" },
  { id: "HIGH", labelKey: "qc.chart.filter.levelHigh" },
];

const QCExportModal = ({ open, onClose, instruments = [] }) => {
  const intl = useIntl();

  const instrumentItems = instruments.map((inst) => ({
    id: String(inst.instrumentId || inst.id),
    label: inst.instrumentName || String(inst.instrumentId || inst.id),
  }));

  const [instrumentId, setInstrumentId] = useState(null);
  const [level, setLevel] = useState("ALL");
  const [dateRange, setDateRange] = useState([null, null]);

  // Derive the effective instrument (default to the first) rather than syncing
  // it in an effect — avoids cascading renders.
  const selectedInstrumentId = instrumentId ?? instrumentItems[0]?.id ?? null;

  const [start, end] = dateRange;
  const datesReversed = start && end && start > end;
  const canExport =
    !!selectedInstrumentId && !!start && !!end && !datesReversed;

  const doExport = (format) => {
    if (!canExport) return;
    const params = new URLSearchParams({
      instrumentId: selectedInstrumentId,
      startDate: toLocalIsoDate(start),
      endDate: toLocalIsoDate(end),
    });
    if (level && level !== "ALL") {
      params.append("controlLevel", level);
    }
    window.open(
      `${config.serverBaseUrl}/rest/qc/export/${format}?${params.toString()}`,
      "_blank",
    );
    onClose();
  };

  const levelItems = LEVEL_OPTIONS.map((o) => ({
    id: o.id,
    label: intl.formatMessage({ id: o.labelKey }),
  }));

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="sm"
      data-testid="qc-export-modal"
    >
      <ModalHeader
        label={intl.formatMessage({ id: "qc.dashboard.title" })}
        title={intl.formatMessage({ id: "qc.dashboard.export.title" })}
      />
      <ModalBody hasForm>
        <Dropdown
          id="qc-export-instrument"
          titleText={intl.formatMessage({
            id: "qc.dashboard.export.instrument",
          })}
          label={intl.formatMessage({
            id: "qc.dashboard.export.selectInstrument",
          })}
          items={instrumentItems}
          itemToString={(item) => item?.label || ""}
          selectedItem={
            instrumentItems.find((i) => i.id === selectedInstrumentId) || null
          }
          onChange={({ selectedItem }) =>
            setInstrumentId(selectedItem?.id || null)
          }
          data-testid="qc-export-instrument-dropdown"
        />
        <Dropdown
          id="qc-export-level"
          titleText={intl.formatMessage({ id: "qc.dashboard.export.level" })}
          label={intl.formatMessage({ id: "qc.chart.filter.allLevels" })}
          items={levelItems}
          itemToString={(item) => item?.label || ""}
          selectedItem={levelItems.find((i) => i.id === level) || levelItems[0]}
          onChange={({ selectedItem }) => setLevel(selectedItem?.id || "ALL")}
          data-testid="qc-export-level-dropdown"
        />
        <DatePicker
          datePickerType="range"
          dateFormat="Y-m-d"
          value={dateRange}
          onChange={(dates) => setDateRange(dates)}
        >
          <DatePickerInput
            id="qc-export-start"
            placeholder="yyyy-mm-dd"
            labelText={intl.formatMessage({
              id: "qc.dashboard.export.startDate",
            })}
            data-testid="qc-export-start-date"
          />
          <DatePickerInput
            id="qc-export-end"
            placeholder="yyyy-mm-dd"
            labelText={intl.formatMessage({
              id: "qc.dashboard.export.endDate",
            })}
            data-testid="qc-export-end-date"
          />
        </DatePicker>
        {datesReversed && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "qc.dashboard.export.dateError" })}
            data-testid="qc-export-date-error"
          />
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={onClose}
          data-testid="qc-export-cancel"
        >
          {intl.formatMessage({ id: "qc.dashboard.export.cancel" })}
        </Button>
        <Button
          kind="tertiary"
          renderIcon={Download}
          disabled={!canExport}
          onClick={() => doExport("csv")}
          data-testid="qc-export-csv"
        >
          {intl.formatMessage({ id: "qc.dashboard.export.csv" })}
        </Button>
        <Button
          kind="primary"
          renderIcon={DocumentPdf}
          disabled={!canExport}
          onClick={() => doExport("pdf")}
          data-testid="qc-export-pdf"
        >
          {intl.formatMessage({ id: "qc.dashboard.export.pdf" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default QCExportModal;
