import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import {
  Stack,
  Modal,
  Table,
  TableHead,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  Button,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  InlineNotification,
  InlineLoading,
} from "@carbon/react";

export default function CaseIncomingWorkspace({ detail, service, onChanged }) {
  const intl = useIntl();
  const t = (id) => intl.formatMessage({ id });
  const [rows, setRows] = useState([]),
    [error, setError] = useState(false),
    [loading, setLoading] = useState(true),
    [selected, setSelected] = useState(null),
    [sample, setSample] = useState(""),
    [target, setTarget] = useState(""),
    [reason, setReason] = useState(""),
    [saving, setSaving] = useState(false),
    [refresh, setRefresh] = useState(0),
    [destinationSearch, setDestinationSearch] = useState(""),
    [destinations, setDestinations] = useState([]),
    [destination, setDestination] = useState(null);
  useEffect(() => {
    if (!destinationSearch.trim()) {
      setDestinations([]);
      return undefined;
    }
    let live = true;
    service
      .searchCases(
        new URLSearchParams({ q: destinationSearch, pageSize: 20 }).toString(),
      )
      .then((data) => {
        if (live) setDestinations(data.rows);
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [destinationSearch, service]);
  useEffect(() => {
    if (!target || target === detail.id) {
      setDestination(null);
      return undefined;
    }
    let live = true;
    setDestination(null);
    setSample("");
    service
      .getCase(target)
      .then((data) => {
        if (live) {
          setDestination(data);
          setSample(data.samples[0]?.sampleItemId || "");
        }
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [target, detail.id, service]);
  useEffect(() => {
    let live = true;
    setLoading(true);
    setError(false);
    setSelected(null);
    service
      .getIncoming(detail.id)
      .then((data) => {
        if (live) setRows(data);
      })
      .catch(() => {
        if (live) setError(true);
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [detail.id, service, refresh]);
  const place = async (fail = false) => {
    setSaving(true);
    setError(false);
    try {
      if (fail) await service.failIncoming(detail.id, selected.id, reason);
      else
        await service.placeIncoming(detail.id, selected.id, {
          sampleItemId: sample,
          targetCaseId: target || detail.id,
          reason,
        });
      setSelected(null);
      setRefresh((n) => n + 1);
      onChanged?.();
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };
  // i18n-keys: microbiology.incoming.state.*
  return (
    <Stack gap={5}>
      <h3>{t("microbiology.incoming.title")}</h3>
      {loading && <InlineLoading description={t("common.loading")} />}
      {error && (
        <InlineNotification
          kind="error"
          title={t("microbiology.incoming.error")}
          hideCloseButton
        />
      )}
      <Table aria-label={t("microbiology.incoming.title")}>
        <TableHead>
          <TableRow>
            {[
              "test.name",
              "common.status",
              "microbiology.incoming.received",
              "microbiology.incoming.values",
              "common.actions",
            ].map((key) => (
              <TableHeader key={key}>{t(key)}</TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.id}>
              <TableCell>{row.testName}</TableCell>
              <TableCell>
                {t(`microbiology.incoming.state.${row.state}`)}
              </TableCell>
              <TableCell>
                {intl.formatDate(new Date(row.receivedAt))}{" "}
                {intl.formatTime(new Date(row.receivedAt))}
              </TableCell>
              <TableCell>
                {row.rawResultValue ||
                  row.result?.components?.map((c) => c.value).join("; ")}
              </TableCell>
              <TableCell>
                {detail.canWrite && row.state !== "PLACED" && (
                  <Button
                    size="sm"
                    disabled={navigator.onLine === false}
                    onClick={() => {
                      setSelected(row);
                      setSample(
                        row.sampleItemId ||
                          detail.samples[0]?.sampleItemId ||
                          "",
                      );
                      setTarget("");
                      setDestinationSearch("");
                      setReason("");
                    }}
                  >
                    {t("microbiology.incoming.review")}
                  </Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {!loading && !rows.length && <p>{t("microbiology.incoming.empty")}</p>}
      {selected && (
        <Modal
          open
          passiveModal
          modalHeading={selected.testName}
          onRequestClose={() => {
            if (!saving) setSelected(null);
          }}
          iconDescription={t("common.close")}
        >
          <Stack gap={4}>
            <h4>{selected.testName}</h4>
            <p>{selected.rawResultValue}</p>
            <TextInput
              id="incoming-destination-search"
              labelText={t("microbiology.incoming.findDestination")}
              value={destinationSearch}
              onChange={(e) => setDestinationSearch(e.target.value)}
            />
            <Select
              id="incoming-target"
              labelText={t("microbiology.incoming.targetCase")}
              value={target}
              onChange={(e) => setTarget(e.target.value)}
            >
              <SelectItem value="" text={t("microbiology.incoming.thisCase")} />
              {destinations
                .filter((c) => c.id !== detail.id)
                .map((c) => (
                  <SelectItem
                    key={c.id}
                    value={c.id}
                    text={`${c.accessionNumber} — ${c.patientName || ""}`}
                  />
                ))}
            </Select>
            <Select
              id="incoming-sample"
              labelText={t("microbiology.incoming.sample")}
              value={sample}
              onChange={(e) => setSample(e.target.value)}
            >
              {(target && target !== detail.id
                ? destination?.samples || []
                : detail.samples
              ).map((s) => (
                <SelectItem
                  key={s.sampleItemId}
                  value={s.sampleItemId}
                  text={s.label}
                />
              ))}
            </Select>
            <TextArea
              id="incoming-reason"
              labelText={t("microbiology.incoming.moveReason")}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <Button
              disabled={saving || !sample || navigator.onLine === false}
              onClick={() => place()}
            >
              {t("microbiology.incoming.place")}
            </Button>
            <Button
              kind="danger--tertiary"
              disabled={saving || !reason.trim() || navigator.onLine === false}
              onClick={() => place(true)}
            >
              {t("microbiology.incoming.fail")}
            </Button>
            <Button kind="secondary" onClick={() => setSelected(null)}>
              {t("common.cancel")}
            </Button>
          </Stack>
        </Modal>
      )}
    </Stack>
  );
}
