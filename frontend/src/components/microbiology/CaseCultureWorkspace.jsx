import React, { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Button,
  Checkbox,
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
const localTime = (value) => {
  const date = value ? new Date(value) : new Date();
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 23);
};
const instant = (value) => (value ? new Date(value).toISOString() : null);

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
const applyMediumDefaults = (
  previous,
  selected,
  link,
  requireTrackedMedia,
) => ({
  ...previous,
  mediumItemId: selected?.id || "",
  lotId:
    String(previous.mediumItemId) === String(selected?.id)
      ? previous.lotId
      : "",
  notTracked: !(selected?.trackLots || requireTrackedMedia),
  atmosphereId:
    link?.atmosphereId || selected?.atmosphereId || previous.atmosphereId,
  temperature:
    link?.temperature ?? selected?.temperature ?? previous.temperature,
  duration: link?.duration ?? previous.duration,
  durationUnit: link?.durationUnit || previous.durationUnit,
  checkIntervalHours: link?.checkIntervalHours ?? previous.checkIntervalHours,
  loopVolume: link?.loopVolume ?? previous.loopVolume,
});

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
  const change = (key, value) =>
    setDraft((previous) => ({ ...previous, [key]: value }));
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
      disabled={key === "sourceSampleItemId" && !!draft.parentId}
      onChange={(e) =>
        key === "sourceSampleItemId"
          ? chooseSource(e.target.value)
          : change(key, e.target.value)
      }
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
  if (action.kind === "outcome") valid = !!draft.outcome;
  if (action.kind === "inoculated-time") valid = !!draft.inoculatedAt;
  if (action.kind === "positive-time") valid = !!draft.positiveAt;
  const submit = () => {
    const body = { ...draft };
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
    onSave(body);
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
            <Select
              id="culture-mediumItemId"
              labelText={t("mediumItemId")}
              value={draft.mediumItemId || ""}
              onChange={(e) => chooseMedium(e.target.value)}
            >
              <SelectItem value="" text={t("choose")} />
              {options.media.map((m) => (
                <SelectItem key={m.id} value={String(m.id)} text={m.name} />
              ))}
            </Select>
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
          </>
        )}
        {action.kind === "outcome" && (
          <>
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
  renderTests,
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
  const date = (value) =>
    value
      ? `${intl.formatDate(new Date(value))} ${intl.formatTime(new Date(value))}`
      : t("notAvailable");
  const numeric = (value) =>
    value == null ? t("notAvailable") : intl.formatNumber(Number(value));
  const open = (kind, row = null, draft = {}) => {
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
    setError("");
    setAction({ kind, row, draft });
  };
  const save = async (body) => {
    if (!detail.canWrite || saving) return;
    setSaving(true);
    setError("");
    try {
      const data =
        action.kind === "inoculate"
          ? await service.inoculateCulture(detail.id, body)
          : await service.cultureAction(
              detail.id,
              action.row.id,
              action.kind,
              body,
            );
      if (!live.current) return;
      setRows(data);
      setAction(null);
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
                <TableCell>{r.by}</TableCell>
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
                </TableCell>
                <TableCell>{e.reasonName}</TableCell>
                <TableCell>{e.note}</TableCell>
                <TableCell>{e.by}</TableCell>
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
            <Stack gap={4}>
              <h3>
                {row.containerIdentifier} — {row.mediumName}
              </h3>
              <Tag type={row.outcome === "GROWTH" ? "green" : "gray"}>
                {row.outcome ? t(`outcome.${row.outcome}`) : t("incubating")}
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
                    `${numeric(row.duration)} ${row.durationUnit ? t(`unit.${row.durationUnit}`) : ""}`,
                  ],
                  ["loopVolume", numeric(row.loopVolume)],
                  ["inoculatedAt", date(row.inoculatedAt)],
                  ["incubationEnds", date(row.incubationEnds)],
                  ["nextCheck", date(row.nextCheck)],
                  ["positiveAt", date(row.positiveAt)],
                  ["timeToPositivityHours", numeric(row.timeToPositivityHours)],
                  ...(row.outcome
                    ? [
                        [
                          "outcomeRecorded",
                          `${row.outcomeBy} — ${date(row.outcomeAt)}`,
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
                  {(!row.outcome || row.outcome === "GROWTH") && (
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
                  {row.canEditInoculatedAt && (
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
                  {row.positiveAt && (
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
                            (s) => s.sampleItemId === row.sourceSampleItemId,
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
                          (s) => s.sampleItemId === row.sourceSampleItemId,
                        )?.sampleTypeId,
                      )
                    }
                    onClick={() =>
                      onChooseTests(
                        {
                          ...row,
                          source: options.sources.find(
                            (s) => s.sampleItemId === row.sourceSampleItemId,
                          ),
                        },
                        options.gramStainTest,
                      )
                    }
                  >
                    {t("gramStain")}
                  </Button>
                </Stack>
              )}
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
                        by: p.confirmedBy,
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
