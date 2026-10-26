import {
  labDateTimeInput as localTime,
  labInputInstant as instant,
  labTimestamp,
  labTimeZone,
  positivityDuration,
  extendedEnd,
} from "./MicrobiologyTime";
import ContainerLabelButton from "./ContainerLabelButton";
import React, { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Button,
  Checkbox,
  ComboBox,
  Grid,
  Column,
  InlineLoading,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextArea,
  TextInput,
  Tile,
} from "@carbon/react";

const units = ["HOURS", "DAYS", "WEEKS"];
const outcomes = ["GROWTH", "NO_GROWTH", "CONTAMINATED"];
const mediaLink = (options, sourceId, mediumId) => {
  const source = options.sources.find((s) => s.sampleItemId === sourceId);
  const links = options.mediaLinks.filter(
    (link) => !mediumId || String(link.mediumItemId) === String(mediumId),
  );
  return (
    links.find((link) => link.sampleTypeId === source?.sampleTypeId) ||
    links.find((link) => !link.sampleTypeId)
  );
};
const applyMediumDefaults = (previous, selected, link, requireTrackedMedia) => {
  const sameMedium = String(previous.mediumItemId) === String(selected?.id);
  const retained = sameMedium ? previous : {};
  const trackingRequired = selected?.trackLots || requireTrackedMedia;
  return {
    ...previous,
    mediumItemId: selected?.id || "",
    lotId: sameMedium ? previous.lotId : "",
    notTracked: trackingRequired
      ? false
      : sameMedium
        ? previous.notTracked
        : true,
    atmosphereId:
      link?.atmosphereId ||
      selected?.atmosphereId ||
      retained.atmosphereId ||
      "",
    temperature:
      link?.temperature ?? selected?.temperature ?? retained.temperature ?? "",
    duration: link?.duration ?? retained.duration ?? "",
    durationUnit: link?.durationUnit || retained.durationUnit || "",
    checkIntervalHours:
      link?.checkIntervalHours ?? retained.checkIntervalHours ?? "",
    loopVolume: link?.loopVolume ?? retained.loopVolume ?? "",
  };
};

function CultureActionForm({
  action,
  options,
  detail,
  rows,
  onSave,
  onClose,
  saving,
}) {
  const intl = useIntl();
  const t = (key, values) =>
    intl.formatMessage({ id: `microbiology.culture.${key}` }, values);
  const [draft, setDraft] = useState(action.draft);
  const eligibleRows = rows.filter(
    (row) =>
      !row.receivedIsolate &&
      (!row.outcome ||
        (row.outcome === "NO_GROWTH" && draft.outcome === "GROWTH")),
  );
  const eligibleParents = rows.filter(
    (row) =>
      (!row.outcome || row.outcome === "GROWTH") &&
      options.sources.some(
        (source) => source.sampleItemId === row.sourceSampleItemId,
      ),
  );
  const selectedRow = action.chooseRow
    ? rows.find((row) => row.id === draft.cultureRowId)
    : action.row;
  const change = (key, value) =>
    setDraft((previous) => ({
      ...previous,
      [key]: value,
      ...(key === "outcome" && value === "GROWTH" && !previous.positiveAt
        ? { positiveAt: localTime() }
        : {}),
    }));
  const medium = options.media.find(
    (m) => String(m.id) === String(draft.mediumItemId),
  );
  const tracked = medium?.trackLots || options.requireTrackedMedia;
  const chooseMedium = (value) => {
    const selected = options.media.find((m) => String(m.id) === value);
    setDraft((previous) => ({
      ...applyMediumDefaults(
        previous,
        selected,
        mediaLink(options, previous.sourceSampleItemId, value),
        options.requireTrackedMedia,
      ),
      lotId: "",
    }));
  };
  const chooseSource = (value) => {
    setDraft((previous) => {
      const link =
        mediaLink(options, value, previous.mediumItemId) ||
        mediaLink(options, value);
      const selected = options.media.find(
        (m) =>
          String(m.id) === String(link?.mediumItemId || previous.mediumItemId),
      );
      return {
        ...applyMediumDefaults(
          previous,
          selected,
          link,
          options.requireTrackedMedia,
        ),
        sourceSampleItemId: value,
      };
    });
  };
  const select = (key, values, label = key) => (
    <Select
      id={`culture-${key}`}
      labelText={t(label)}
      value={draft[key] || ""}
      disabled={
        key === "sourceSampleItemId" &&
        (!!draft.parentId || action.chooseParent)
      }
      onChange={(e) => {
        const value = e.target.value;
        if (key === "sourceSampleItemId") chooseSource(value);
        else if (key === "parentId") {
          chooseSource(
            eligibleParents.find((row) => row.id === value)
              ?.sourceSampleItemId || "",
          );
          change(key, value);
        } else change(key, value);
      }}
    >
      <SelectItem value="" text={t("choose")} />
      {values.map((v) => (
        <SelectItem
          key={v.id}
          value={String(v.id)}
          text={v.value || v.name || v.lotNumber}
        />
      ))}
    </Select>
  );
  const input = (key, type = "text", label = key) => (
    <TextInput
      id={`culture-${key}`}
      type={type}
      labelText={t(label)}
      value={draft[key] ?? ""}
      maxLength={key === "containerIdentifier" ? 80 : undefined}
      step={
        type === "number"
          ? "0.01"
          : type === "datetime-local"
            ? "0.001"
            : undefined
      }
      max={type === "datetime-local" ? localTime() : undefined}
      onChange={(e) => change(key, e.target.value)}
    />
  );
  const unitChoices = units.map((id) => ({ id, value: t(`unit.${id}`) }));
  let valid = true;
  if (action.kind === "inoculate")
    valid =
      (!action.chooseParent ||
        eligibleParents.some((row) => row.id === draft.parentId)) &&
      !!draft.sourceSampleItemId &&
      !!draft.mediumItemId &&
      !!draft.atmosphereId &&
      Number(draft.duration) > 0 &&
      !!draft.durationUnit &&
      !!draft.inoculatedAt &&
      !!draft.containerIdentifier?.trim() &&
      ((draft.notTracked && !tracked) || !!draft.lotId);
  if (action.kind === "reading") valid = !!draft.readingId;
  if (action.kind === "extension")
    valid = Number(draft.extendBy) > 0 && !!draft.unit && !!draft.reasonId;
  if (action.kind === "outcome")
    valid =
      !!draft.outcome &&
      (!action.chooseRow ||
        eligibleRows.some((row) => row.id === draft.cultureRowId));
  if (action.kind === "inoculated-time") valid = !!draft.inoculatedAt;
  if (action.kind === "positive-time") valid = !!draft.positiveAt;
  const submit = () => {
    const body = { ...draft };
    const cultureRowId = body.cultureRowId;
    delete body.cultureRowId;
    [
      "mediumItemId",
      "lotId",
      "duration",
      "temperature",
      "checkIntervalHours",
      "loopVolume",
      "extendBy",
    ].forEach((key) => {
      if (key in body)
        body[key] =
          body[key] === "" || body[key] == null ? null : Number(body[key]);
    });
    ["inoculatedAt", "positiveAt"].forEach((key) => {
      if (key in body) body[key] = instant(body[key]);
    });
    onSave(body, cultureRowId);
  };
  return (
    <Modal
      open
      closeButtonLabel={intl.formatMessage({ id: "label.button.close" })}
      modalHeading={t(`action.${action.kind}`)}
      primaryButtonText={t("save")}
      secondaryButtonText={t("cancel")}
      primaryButtonDisabled={!valid || saving || !detail.canWrite}
      onRequestSubmit={submit}
      onRequestClose={() => {
        if (!saving) onClose();
      }}
      preventCloseOnClickOutside
    >
      <Stack gap={5}>
        {action.kind === "inoculate" && (
          <>
            {action.chooseParent &&
              select(
                "parentId",
                eligibleParents.map((row) => ({
                  id: row.id,
                  value: row.containerIdentifier,
                })),
                "parentCulture",
              )}
            {select(
              "sourceSampleItemId",
              options.sources.map((s) => ({
                id: s.sampleItemId,
                value: s.label || s.specimenType,
              })),
            )}
            {draft.parentId && (
              <>
                <p>
                  {t("fromCulture", {
                    value: rows.find((r) => r.id === draft.parentId)
                      ?.containerIdentifier,
                  })}
                </p>
                {select(
                  "subculturePurpose",
                  ["CLINICAL_DIAGNOSTIC", "ACTIVE_SCREENING"].map((id) => ({
                    id,
                    value: t(`purpose.${id}`),
                  })),
                )}
              </>
            )}
            {input("containerIdentifier")}
            <ComboBox
              id="culture-mediumItemId"
              titleText={t("mediumItemId")}
              items={options.media}
              selectedItem={medium || null}
              itemToString={(m) =>
                m
                  ? `${m.name} · ${t(m.trackLots || options.requireTrackedMedia ? "tracked" : "notTracked")}`
                  : ""
              }
              shouldFilterItem={({ item, inputValue }) =>
                item.name
                  .toLocaleLowerCase()
                  .includes((inputValue || "").toLocaleLowerCase())
              }
              onChange={({ selectedItem }) =>
                chooseMedium(selectedItem ? String(selectedItem.id) : "")
              }
            />
            <Checkbox
              id="culture-notTracked"
              labelText={t("notTracked")}
              checked={!!draft.notTracked}
              disabled={tracked}
              onChange={(_, { checked }) => change("notTracked", checked)}
            />
            {!draft.notTracked && select("lotId", medium?.lots || [])}
            {select("atmosphereId", options.atmospheres)}
            {input("temperature", "number")}
            {input("duration", "number")}
            {select("durationUnit", unitChoices)}
            {input("checkIntervalHours", "number")}
            {input("loopVolume", "number")}
            {input("inoculatedAt", "datetime-local")}
          </>
        )}
        {action.kind === "reading" && (
          <>
            {select("readingId", options.readings)}
            {select("quantityId", options.quantities)}
          </>
        )}
        {action.kind === "extension" && (
          <>
            {input("extendBy", "number")}
            {select("unit", unitChoices)}
            {select("reasonId", options.extensionReasons)}
            <p>
              {t("newIncubationEnd")}:{" "}
              {labTimestamp(
                intl,
                extendedEnd(
                  action.row?.incubationEnds,
                  draft.extendBy,
                  draft.unit,
                ),
              )}
            </p>
          </>
        )}
        {action.kind === "outcome" && (
          <>
            {action.chooseRow &&
              select(
                "cultureRowId",
                eligibleRows.map((row) => ({
                  id: row.id,
                  value: row.containerIdentifier,
                })),
              )}
            {select(
              "outcome",
              outcomes.map((id) => ({ id, value: t(`outcome.${id}`) })),
            )}
            {draft.outcome === "GROWTH" &&
              input("positiveAt", "datetime-local")}
          </>
        )}
        {action.kind === "inoculated-time" &&
          input("inoculatedAt", "datetime-local")}
        {action.kind === "positive-time" &&
          input("positiveAt", "datetime-local")}
        {["reading", "extension"].includes(action.kind) && (
          <TextArea
            id="culture-note"
            labelText={t("note")}
            value={draft.note || ""}
            maxLength={2000}
            onChange={(e) => change("note", e.target.value)}
          />
        )}
        {(draft.outcome === "GROWTH" || action.kind === "positive-time") &&
          draft.positiveAt &&
          selectedRow && (
            <p>
              {t("timeToPositivityHours")}:{" "}
              {positivityDuration(
                intl,
                (new Date(instant(draft.positiveAt)) -
                  new Date(selectedRow.inoculatedAt)) /
                  3600000,
              )}
            </p>
          )}
        <p>
          {intl.formatMessage(
            { id: "microbiology.case.labClock" },
            { zone: labTimeZone() },
          )}
        </p>
        <p>{t("serverValidation")}</p>
      </Stack>
    </Modal>
  );
}

export default function CaseCultureWorkspace({
  detail,
  service,
  selectedId,
  onSelect,
  onChooseTests,
  onPickIsolate,
  renderTests,
  renderNotes,
  entryAction,
  onEntryActionConsumed,
  onChanged,
}) {
  const intl = useIntl();
  const t = (key, values) =>
    intl.formatMessage({ id: `microbiology.culture.${key}` }, values);
  const live = useRef(true);
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  const [rows, setRows] = useState([]);
  const [options, setOptions] = useState(null);
  const [error, setError] = useState("");
  const [action, setAction] = useState(null);
  const [saving, setSaving] = useState(false);
  useEffect(() => {
    let live = true;
    Promise.all([
      service.getCultures(detail.id),
      service.getCultureOptions(detail.id),
    ])
      .then(([data, config]) => {
        if (live) {
          setRows(data);
          setOptions(config);
        }
      })
      .catch(() => {
        if (live) setError("loadError");
      });
    return () => {
      live = false;
    };
  }, [detail.id, detail.labUnitId, service]);
  const entryHandled = useRef("");
  useEffect(() => {
    if (
      ![
        "mark-positive",
        "mark-no-growth",
        "start-inoculation",
        "add-subculture",
      ].includes(entryAction)
    ) {
      entryHandled.current = "";
      return;
    }
    const key = `${detail.id}:${entryAction}`;
    if (!options || !detail.canWrite || entryHandled.current === key) return;
    entryHandled.current = key;
    if (entryAction === "start-inoculation") {
      open("inoculate");
      onEntryActionConsumed?.();
      return;
    }
    if (entryAction === "add-subculture") {
      const parents = rows.filter(
        (row) =>
          (!row.outcome || row.outcome === "GROWTH") &&
          options.sources.some(
            (source) => source.sampleItemId === row.sourceSampleItemId,
          ),
      );
      const parent =
        parents.find((row) => row.id === selectedId) ||
        (parents.length === 1 ? parents[0] : null);
      if (parents.length) open("inoculate", parent, {}, true);
      else setError("noEligibleParentRows");
      onEntryActionConsumed?.();
      return;
    }
    const outcome = entryAction === "mark-positive" ? "GROWTH" : "NO_GROWTH";
    const eligible = rows.filter(
      (row) =>
        !row.receivedIsolate &&
        (!row.outcome || (row.outcome === "NO_GROWTH" && outcome === "GROWTH")),
    );
    const row =
      eligible.find((row) => row.id === selectedId) ||
      (eligible.length === 1 ? eligible[0] : null);
    if (eligible.length === 0) {
      setError("noEligibleOutcomeRows");
    } else {
      setError("");
      setAction({
        kind: "outcome",
        chooseRow: true,
        row,
        draft: {
          outcome,
          cultureRowId: row?.id || "",
          ...(outcome === "GROWTH" ? { positiveAt: localTime() } : {}),
        },
      });
    }
    onEntryActionConsumed?.();
  }, [
    entryAction,
    detail.id,
    detail.canWrite,
    options,
    rows,
    selectedId,
    onEntryActionConsumed,
  ]);
  const date = (value) => labTimestamp(intl, value);
  const numeric = (value) =>
    value == null ? t("notAvailable") : intl.formatNumber(Number(value));
  const open = (kind, row = null, draft = {}, chooseParent = false) => {
    if (kind === "inoculate") {
      const previous = row || rows[rows.length - 1];
      const source =
        options.sources.find(
          (s) => s.sampleItemId === previous?.sourceSampleItemId,
        ) || options.sources[0];
      const link = mediaLink(options, source?.sampleItemId);
      const medium = options.media.find(
        (m) => m.id === (link?.mediumItemId || previous?.mediumItemId),
      );
      draft = {
        sourceSampleItemId: source?.sampleItemId || "",
        parentId: row?.id || null,
        subculturePurpose: "CLINICAL_DIAGNOSTIC",
        containerIdentifier: "",
        mediumItemId: medium?.id || "",
        lotId: "",
        notTracked: !(medium?.trackLots || options.requireTrackedMedia),
        atmosphereId:
          link?.atmosphereId ||
          medium?.atmosphereId ||
          previous?.atmosphereId ||
          "",
        temperature:
          link?.temperature ??
          medium?.temperature ??
          previous?.temperature ??
          "",
        duration: link?.duration ?? previous?.duration ?? "",
        durationUnit: link?.durationUnit || previous?.durationUnit || "HOURS",
        checkIntervalHours:
          link?.checkIntervalHours ?? previous?.checkIntervalHours ?? "",
        loopVolume: link?.loopVolume ?? previous?.loopVolume ?? "",
        inoculatedAt: localTime(),
        ...draft,
      };
    }
    if (kind === "outcome" && draft.outcome === "GROWTH" && !draft.positiveAt)
      draft.positiveAt = localTime();
    setError("");
    setAction({ kind, row, draft, chooseParent });
  };
  const save = async (body, cultureRowId) => {
    if (!detail.canWrite || saving) return;
    setSaving(true);
    setError("");
    try {
      const data =
        action.kind === "inoculate"
          ? await service.inoculateCulture(detail.id, body)
          : await service.cultureAction(
              detail.id,
              cultureRowId || action.row?.id,
              action.kind,
              body,
            );
      if (!live.current) return;
      setRows(data);
      setAction(null);
      onChanged?.();
    } catch (failure) {
      if (!live.current) return;
      const message =
        failure.response?.message || failure.response?.error || "";
      setError(
        message.startsWith("MICROBIOLOGY_CULTURE_") ? message : "saveError",
      );
    } finally {
      if (live.current) setSaving(false);
    }
  };
  const log = (row) => (
    <>
      <TableContainer title={t("readings")}>
        <Table aria-label={t("readings")}>
          <TableHead>
            <TableRow>
              {[
                "readingId",
                "quantityId",
                "note",
                "incubationDay",
                "by",
                "at",
              ].map((key) => (
                <TableHeader key={key}>{t(key)}</TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {row.readings.map((r) => (
              <TableRow key={r.id}>
                <TableCell>{r.readingName}</TableCell>
                <TableCell>{r.quantityName || t("notAvailable")}</TableCell>
                <TableCell>{r.note}</TableCell>
                <TableCell>{numeric(r.incubationDay)}</TableCell>
                <TableCell>{r.byDisplay || r.by}</TableCell>
                <TableCell>{date(r.at)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <TableContainer title={t("extensions")}>
        <Table aria-label={t("extensions")}>
          <TableHead>
            <TableRow>
              {["extendBy", "reasonId", "note", "by", "at"].map((key) => (
                <TableHeader key={key}>{t(key)}</TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {row.extensions.map((e) => (
              <TableRow key={e.id}>
                <TableCell>
                  {numeric(e.extendBy)} {t(`unit.${e.unit}`)}
                  {e.undoneAt && <Tag>{t("extensionUndone")}</Tag>}
                </TableCell>
                <TableCell>{e.reasonName}</TableCell>
                <TableCell>{e.note}</TableCell>
                <TableCell>{e.byDisplay || e.by}</TableCell>
                <TableCell>{date(e.at)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
  const tree = (parentId = null) =>
    rows
      .filter((row) => (row.parentId || null) === parentId)
      .map((row) => (
        <AccordionItem key={row.id} title={row.containerIdentifier} open>
          <Tile data-testid={`culture-row-${row.id}`}>
            {!row.receivedIsolate && (
              <ContainerLabelButton
                detail={detail}
                kind="PLT"
                rowId={row.id}
                service={service}
              />
            )}
            <Stack gap={4}>
              <h3>
                {row.containerIdentifier} —{" "}
                {row.receivedIsolate
                  ? intl.formatMessage({
                      id: "microbiology.isolateWorkspace.receivedGrowth",
                    })
                  : row.mediumName}
              </h3>
              <Tag type={row.outcome === "GROWTH" ? "green" : "gray"}>
                {row.receivedIsolate
                  ? intl.formatMessage({
                      id: "microbiology.isolateWorkspace.receivedGrowth",
                    })
                  : row.outcome
                    ? t(`outcome.${row.outcome}`)
                    : t("incubating")}
              </Tag>
              {row.checkDue && <Tag type="warm-gray">{t("checkDue")}</Tag>}
              {row.finalReadDue && <Tag type="purple">{t("finalReadDue")}</Tag>}
              {row.overdue && <Tag type="red">{t("overdue")}</Tag>}
              <Grid condensed>
                {[
                  [
                    "lotId",
                    row.notTracked
                      ? t("notTracked")
                      : row.lotNumber || t("notAvailable"),
                  ],
                  ["atmosphereId", row.atmosphereName],
                  ["temperature", numeric(row.temperature)],
                  [
                    "duration",
                    <>
                      {numeric(row.duration)}{" "}
                      {row.durationUnit ? t(`unit.${row.durationUnit}`) : ""}{" "}
                      {row.extensions.filter((e) => !e.undoneAt).length > 0 && (
                        <Tag type="purple">
                          {t("extended", {
                            count: row.extensions.filter((e) => !e.undoneAt)
                              .length,
                          })}
                        </Tag>
                      )}
                    </>,
                  ],
                  ["loopVolume", numeric(row.loopVolume)],
                  ["inoculatedAt", date(row.inoculatedAt)],
                  ["incubationEnds", date(row.incubationEnds)],
                  ["nextCheck", date(row.nextCheck)],
                  ["positiveAt", date(row.positiveAt)],
                  [
                    "timeToPositivityHours",
                    positivityDuration(
                      intl,
                      row.positiveAt && row.inoculatedAt
                        ? (new Date(row.positiveAt) -
                            new Date(row.inoculatedAt)) /
                            3600000
                        : row.timeToPositivityHours,
                    ),
                  ],
                  ...(row.outcome
                    ? [
                        [
                          "outcomeRecorded",
                          `${row.outcomeByDisplay || row.outcomeBy} — ${date(row.outcomeAt)}`,
                        ],
                      ]
                    : []),
                ].map(([label, value]) => (
                  <Column key={label} lg={4} md={4} sm={4}>
                    <dl>
                      <dt>{t(label)}</dt>
                      <dd>{value}</dd>
                    </dl>
                  </Column>
                ))}
              </Grid>
              <Button
                kind="ghost"
                onClick={() =>
                  onSelect({
                    ...row,
                    source: options.sources.find(
                      (s) => s.sampleItemId === row.sourceSampleItemId,
                    ),
                  })
                }
              >
                {selectedId === row.id ? t("selected") : t("selectTests")}
              </Button>
              {detail.canWrite && options && (
                <Stack
                  orientation="horizontal"
                  gap={3}
                  style={{ display: "flex", flexWrap: "wrap" }}
                >
                  {!row.receivedIsolate &&
                    (!row.outcome || row.outcome === "GROWTH") && (
                      <>
                        <Button
                          kind="tertiary"
                          onClick={() => open("reading", row)}
                        >
                          {t("action.reading")}
                        </Button>
                        <Button
                          kind="tertiary"
                          onClick={() => open("extension", row)}
                        >
                          {t("action.extension")}
                        </Button>
                        {!row.outcome && (
                          <Button
                            kind="tertiary"
                            onClick={() => open("outcome", row)}
                          >
                            {t("action.outcome")}
                          </Button>
                        )}
                      </>
                    )}
                  {row.outcome === "NO_GROWTH" && (
                    <Button
                      kind="tertiary"
                      onClick={() =>
                        open("outcome", row, { outcome: "GROWTH" })
                      }
                    >
                      {t("lateGrowth")}
                    </Button>
                  )}
                  {!row.receivedIsolate && row.canEditInoculatedAt && (
                    <Button
                      kind="ghost"
                      onClick={() =>
                        open("inoculated-time", row, {
                          inoculatedAt: localTime(row.inoculatedAt),
                        })
                      }
                    >
                      {t("action.inoculated-time")}
                    </Button>
                  )}
                  {!row.receivedIsolate && row.positiveAt && (
                    <Button
                      kind="ghost"
                      onClick={() =>
                        open("positive-time", row, {
                          positiveAt: localTime(row.positiveAt),
                        })
                      }
                    >
                      {t("action.positive-time")}
                    </Button>
                  )}
                  {(!row.outcome || row.outcome === "GROWTH") &&
                    options.sources.some(
                      (source) =>
                        source.sampleItemId === row.sourceSampleItemId,
                    ) && (
                      <>
                        {!row.receivedIsolate &&
                          row.outcome === "GROWTH" &&
                          onPickIsolate && (
                            <Button
                              kind="tertiary"
                              onClick={() => onPickIsolate(row)}
                            >
                              {intl.formatMessage({
                                id: "microbiology.isolateWorkspace.pick",
                              })}
                            </Button>
                          )}
                        <Button
                          kind="tertiary"
                          onClick={() => open("inoculate", row)}
                        >
                          {t("subculture")}
                        </Button>
                        <Button
                          kind="tertiary"
                          onClick={() =>
                            onChooseTests(
                              {
                                ...row,
                                source: options.sources.find(
                                  (s) =>
                                    s.sampleItemId === row.sourceSampleItemId,
                                ),
                              },
                              null,
                            )
                          }
                        >
                          {t("testOnCulture")}
                        </Button>
                        <Button
                          kind="tertiary"
                          disabled={
                            !options.gramStainTest ||
                            !options.gramStainSampleTypeIds?.includes(
                              options.sources.find(
                                (s) =>
                                  s.sampleItemId === row.sourceSampleItemId,
                              )?.sampleTypeId,
                            )
                          }
                          onClick={() =>
                            onChooseTests(
                              {
                                ...row,
                                source: options.sources.find(
                                  (s) =>
                                    s.sampleItemId === row.sourceSampleItemId,
                                ),
                              },
                              options.gramStainTest,
                            )
                          }
                        >
                          {t("gramStain")}
                        </Button>
                      </>
                    )}
                </Stack>
              )}
              {renderNotes?.("CULTURE", row.id)}
              {log(row)}
              {row.proposals.map((p) => (
                <Tile key={p.id}>
                  <p>
                    {t("negativeProposal", {
                      signal: p.signal,
                      at: date(p.receivedAt),
                    })}
                  </p>
                  {p.confirmedAt ? (
                    <p>
                      {t("confirmed", {
                        by: p.confirmedByDisplay || p.confirmedBy,
                        at: date(p.confirmedAt),
                      })}
                    </p>
                  ) : (
                    detail.canWrite &&
                    !row.outcome && (
                      <Button
                        kind="tertiary"
                        onClick={() =>
                          open("outcome", row, {
                            outcome: "NO_GROWTH",
                            proposalId: p.id,
                          })
                        }
                      >
                        {t("confirmNegative")}
                      </Button>
                    )
                  )}
                </Tile>
              ))}
              {renderTests?.(row.id)}
              <Accordion>{tree(row.id)}</Accordion>
            </Stack>
          </Tile>
        </AccordionItem>
      ));
  return (
    <Stack gap={5}>
      <h2>{t("title")}</h2>
      {!options && !error && <InlineLoading description={t("loading")} />}
      {error && (
        <InlineNotification kind="error" title={t(error)} hideCloseButton />
      )}
      {options && detail.canWrite && options.sources.length > 0 && (
        <Button onClick={() => open("inoculate")}>
          {t("action.inoculate")}
        </Button>
      )}
      {options && options.media.length === 0 && (
        <InlineNotification kind="info" title={t("noMedia")} hideCloseButton />
      )}
      {options && rows.length === 0 && <p>{t("empty")}</p>}
      <Accordion>{tree()}</Accordion>
      {action && detail.canWrite && (
        <CultureActionForm
          key={`${action.kind}-${action.row?.id || "new"}`}
          action={action}
          options={options}
          detail={detail}
          rows={rows}
          saving={saving}
          onSave={save}
          onClose={() => setAction(null)}
        />
      )}
    </Stack>
  );
}
