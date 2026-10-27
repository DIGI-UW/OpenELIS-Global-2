import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import {
  Button,
  Checkbox,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { labTimestamp } from "./MicrobiologyTime";

const qcStatusType = (status) =>
  status === "PASSED" ? "green" : status === "FAILED" ? "red" : "gray";

export default function CaseAstRunQc({ caseId, runId, service, onRecorded }) {
  const intl = useIntl();
  const [view, setView] = useState(null);
  const [open, setOpen] = useState(false);
  const [controlLotId, setControlLotId] = useState("");
  const [checkedLots, setCheckedLots] = useState({});
  const [values, setValues] = useState({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(false);

  useEffect(() => {
    if (!service.getAstQc) return undefined;
    let live = true;
    service
      .getAstQc(caseId, runId)
      .then((value) => {
        if (live) setView(value);
      })
      .catch(() => {
        if (live) setView(null);
      });
    return () => {
      live = false;
    };
  }, [caseId, runId, service]);

  if (!view) return null;

  const t = (id, values) => intl.formatMessage({ id }, values);
  const statusLabel = (status) => t(`microbiology.reagentLots.qc.${status}`);
  const antibioticName = (id) =>
    view.antibiotics.find((antibiotic) => antibiotic.id === id)?.name || id;
  const allLots = [...view.lots, ...view.candidateLots];
  const selectedLot = view.controlLots.find((lot) => lot.id === controlLotId);
  const rangeFor = (antibioticId) => selectedLot?.ranges?.[antibioticId];
  const missingRange =
    !!selectedLot &&
    view.antibiotics.some((antibiotic) => !rangeFor(antibiotic.id));
  const invalidValue = view.antibiotics.some((antibiotic) => {
    const value = values[antibiotic.id];
    return (
      value == null ||
      String(value).trim() === "" ||
      !Number.isFinite(Number(value))
    );
  });
  const openDialog = () => {
    setControlLotId("");
    setCheckedLots(Object.fromEntries(view.lots.map((lot) => [lot.id, true])));
    setValues({});
    setSaveError(false);
    setOpen(true);
  };
  const record = async () => {
    setSaving(true);
    setSaveError(false);
    try {
      const updated = await service.recordAstQc(caseId, runId, {
        controlLotId,
        lotIds: allLots
          .filter((lot) => checkedLots[lot.id])
          .map((lot) => lot.id),
        results: view.antibiotics.map((antibiotic) => ({
          antibioticId: antibiotic.id,
          measuredValue: Number(values[antibiotic.id]),
        })),
      });
      setView(updated);
      setOpen(false);
      onRecorded?.();
    } catch (error) {
      setSaveError(true);
    } finally {
      setSaving(false);
    }
  };

  // i18n-keys: microbiology.reagentLots.qc.*
  return (
    <section aria-label={t("microbiology.astQc.title")}>
      <h4>{t("microbiology.astQc.title")}</h4>
      {view.lots.length === 0 ? (
        <p>{t("microbiology.astQc.noLots")}</p>
      ) : (
        view.lots.map((lot) => (
          <div key={lot.id}>
            <span>{lot.lotNumber}</span> · <span>{lot.itemName}</span>{" "}
            <Tag type={qcStatusType(lot.qcStatus)}>
              {statusLabel(lot.qcStatus)}
            </Tag>
          </div>
        ))
      )}
      {view.controls.length === 0 ? (
        <p>{t("microbiology.astQc.noControls")}</p>
      ) : (
        view.controls.map((control) => (
          <div key={control.controlId}>
            <div>
              <span>{control.controlLotLabel}</span> ·{" "}
              {labTimestamp(intl, control.recordedAt)}{" "}
              <Tag type={control.passed ? "green" : "red"}>
                {t(
                  control.passed
                    ? "microbiology.astQc.passed"
                    : "microbiology.astQc.failed",
                )}
              </Tag>
            </div>
            <TableContainer>
              <Table size="sm" aria-label={t("microbiology.astQc.title")}>
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      {t("microbiology.astQc.antibiotic")}
                    </TableHeader>
                    <TableHeader>
                      {t("microbiology.astQc.expected")}
                    </TableHeader>
                    <TableHeader>
                      {t("microbiology.astQc.measured")}
                    </TableHeader>
                    <TableHeader>{t("microbiology.astQc.result")}</TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {control.rows.map((row) => (
                    <TableRow key={row.qcResultId || row.antibioticId}>
                      <TableCell>{antibioticName(row.antibioticId)}</TableCell>
                      <TableCell>
                        {row.low}–{row.high}
                      </TableCell>
                      <TableCell>{row.measuredValue}</TableCell>
                      <TableCell>
                        <Tag type={row.outcome === "PASS" ? "green" : "red"}>
                          {t(
                            row.outcome === "PASS"
                              ? "microbiology.astQc.pass"
                              : "microbiology.astQc.fail",
                          )}
                        </Tag>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </div>
        ))
      )}
      {!view.configured ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={t("microbiology.astQc.notConfigured")}
        />
      ) : (
        view.canRecord && (
          <Button kind="tertiary" size="sm" onClick={openDialog}>
            {t("microbiology.astQc.record")}
          </Button>
        )
      )}
      {open && (
        <Modal
          open
          modalHeading={t("microbiology.astQc.record")}
          primaryButtonText={t("microbiology.astQc.save")}
          secondaryButtonText={t("common.cancel")}
          primaryButtonDisabled={
            saving || !controlLotId || missingRange || invalidValue
          }
          onRequestClose={() => setOpen(false)}
          onSecondarySubmit={() => setOpen(false)}
          onRequestSubmit={record}
        >
          <Select
            id="ast-qc-control-lot"
            labelText={t("microbiology.astQc.controlLot")}
            value={controlLotId}
            onChange={(event) => setControlLotId(event.target.value)}
          >
            <SelectItem
              value=""
              text={t("microbiology.astQc.chooseControlLot")}
            />
            {view.controlLots.map((lot) => (
              <SelectItem key={lot.id} value={lot.id} text={lot.label} />
            ))}
          </Select>
          <h5>{t("microbiology.astQc.lotsUnderTest")}</h5>
          {allLots.map((lot) => (
            <Checkbox
              key={lot.id}
              id={`ast-qc-lot-${lot.id}`}
              labelText={`${lot.lotNumber} · ${lot.itemName} · ${statusLabel(lot.qcStatus)}`}
              checked={!!checkedLots[lot.id]}
              onChange={(_, { checked }) =>
                setCheckedLots((current) => ({ ...current, [lot.id]: checked }))
              }
            />
          ))}
          <TableContainer>
            <Table size="sm" aria-label={t("microbiology.astQc.record")}>
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {t("microbiology.astQc.antibiotic")}
                  </TableHeader>
                  <TableHeader>{t("microbiology.astQc.expected")}</TableHeader>
                  <TableHeader>{t("microbiology.astQc.measured")}</TableHeader>
                  <TableHeader>{t("microbiology.astQc.result")}</TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {view.antibiotics.map((antibiotic) => {
                  const range = rangeFor(antibiotic.id);
                  const value = values[antibiotic.id];
                  const hasNumber =
                    value != null &&
                    String(value).trim() !== "" &&
                    Number.isFinite(Number(value));
                  const passed =
                    !!range &&
                    hasNumber &&
                    Number(value) >= range.low &&
                    Number(value) <= range.high;
                  return (
                    <TableRow key={antibiotic.id}>
                      <TableCell>{antibiotic.name}</TableCell>
                      <TableCell>
                        {range
                          ? `${range.low}–${range.high}`
                          : t("microbiology.astQc.noRange")}
                      </TableCell>
                      <TableCell>
                        <TextInput
                          id={`ast-qc-value-${antibiotic.id}`}
                          labelText={t("microbiology.astQc.measuredFor", {
                            antibiotic: antibiotic.name,
                          })}
                          hideLabel
                          value={value || ""}
                          onChange={(event) =>
                            setValues((current) => ({
                              ...current,
                              [antibiotic.id]: event.target.value,
                            }))
                          }
                        />
                      </TableCell>
                      <TableCell>
                        {range && hasNumber && (
                          <Tag type={passed ? "green" : "red"}>
                            {t(
                              passed
                                ? "microbiology.astQc.pass"
                                : "microbiology.astQc.fail",
                            )}
                          </Tag>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
          {missingRange && (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={t("microbiology.astQc.rangeMissing")}
            />
          )}
          {saveError && (
            <InlineNotification
              kind="error"
              hideCloseButton
              title={t("microbiology.astQc.saveError")}
            />
          )}
        </Modal>
      )}
    </section>
  );
}
