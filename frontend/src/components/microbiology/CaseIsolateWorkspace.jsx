import ContainerLabelButton from "./ContainerLabelButton";
import React, { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Button,
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
import ReferralStatusTag from "../order/steps/referOut/referralStatusTag";
import OrderReferOutForm from "../order/steps/referOut/OrderReferOutForm";

const localTime = (value = Date.now()) => {
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
};
const labels = {
  referred: "label.results.history.event.REFERRAL",

  title: "microbiology.case.isolates",
  gramStain: "microbiology.isolate.gramStain",
  colonyMorphology: "microbiology.isolate.colonyMorphology",
  organismId: "microbiology.isolate.organism",
  identificationMethod: "microbiology.isolate.identificationMethod",
  confidence: "microbiology.isolate.identificationConfidence",
  reason: "microbiology.isolate.identificationReason",
  identify: "microbiology.isolate.identify",
  reidentify: "microbiology.isolate.reidentify",
  history: "microbiology.isolate.identificationHistory",
  save: "label.button.save",
  cancel: "label.button.cancel",
  loading: "common.loading",
  empty: "microbiology.case.isolates.empty",
  notAvailable: "not.available",
  SIGNIFICANT: "microbiology.isolate.significant",
  CONTAMINANT: "microbiology.isolate.contaminant",
  NORMAL_FLORA: "microbiology.isolate.normalFlora",
  UNKNOWN: "microbiology.isolate.unknown",
  PRELIMINARY: "microbiology.enum.PRELIMINARY",
  FINAL: "microbiology.enum.FINAL",
  PENDING: "microbiology.isolate.pending",
};

function IsolateAction({
  action,
  options,
  cultures,
  saving,
  error,
  onSave,
  onClose,
}) {
  const intl = useIntl();
  const t = (key, values) =>
    intl.formatMessage(
      { id: labels[key] || `microbiology.isolateWorkspace.${key}` },
      values,
    );
  const isolate = action.isolate;
  const [draft, setDraft] = useState(() => ({
    cultureId: action.cultureId || "",
    gramStain: "",
    colonyMorphology: "",
    significance: isolate?.significance || "UNKNOWN",
    organismId: isolate?.organismId || "",
    identificationStatus: isolate?.status === "FINAL" ? "FINAL" : "PRELIMINARY",
    identificationMethod:
      options.methods.find((m) => m.value === isolate?.method)?.id || "",
    confidence: isolate?.confidence ?? "",
    identifiedAt: localTime(),
    reason: "",
    version: isolate?.version || "",
    purpose: "LABEL",
    sampleTypeId:
      options.sampleTypes.length === 1 ? options.sampleTypes[0].id : "",
    sendingOrganizationId: isolate?.sendingOrganizationId || "",
    reportedOrganismId: isolate?.reportedOrganismId || "",
    reportedOrganismText: isolate?.reportedOrganismText || "",
    sampleItemId: "",
    testId: "",
  }));
  const set = (key, value) => setDraft((old) => ({ ...old, [key]: value }));
  const input = (key, type = "text") => (
    <TextInput
      id={`isolate-${key}`}
      key={key}
      labelText={t(key)}
      type={type}
      value={draft[key]}
      maxLength={255}
      onChange={(e) => set(key, e.target.value)}
    />
  );
  const select = (key, choices) => (
    <Select
      id={`isolate-${key}`}
      key={key}
      labelText={t(key)}
      value={draft[key]}
      onChange={(e) => {
        const value = e.target.value;
        setDraft((old) => ({
          ...old,
          [key]: value,
          ...(key === "sampleTypeId" || key === "sampleItemId"
            ? { testId: "" }
            : {}),
        }));
      }}
    >
      <SelectItem value="" text={t("choose")} />
      {choices.map((choice) => (
        <SelectItem key={choice.id} value={choice.id} text={choice.value} />
      ))}
    </Select>
  );
  const combo = (key, choices) => (
    <ComboBox
      id={`isolate-${key}`}
      key={key}
      titleText={t(key)}
      items={choices}
      itemToString={(item) => item?.value || ""}
      selectedItem={choices.find((choice) => choice.id === draft[key]) || null}
      shouldFilterItem={({ item, inputValue }) =>
        item.value.toLowerCase().includes((inputValue || "").toLowerCase())
      }
      onChange={({ selectedItem }) => set(key, selectedItem?.id || "")}
    />
  );
  const needsSample =
    isolate &&
    !isolate.sampleItemId &&
    ["sample-item", "test", "referral"].includes(action.kind);
  const specimenType = isolate?.sampleItemId
    ? options.sources.find((s) => s.id === isolate.sampleItemId)?.sampleTypeId
    : action.kind === "specimen-referral"
      ? options.sources.find((s) => s.id === draft.sampleItemId)?.sampleTypeId
      : draft.sampleTypeId;
  const tests = options.tests
    .filter((test) => test.sampleTypeIds.includes(specimenType))
    .map((test) => ({ id: test.id, value: test.name }));
  const isIdentification = ["identify", "reidentify"].includes(action.kind);
  const valid =
    action.kind === "pick"
      ? !!draft.cultureId
      : isIdentification
        ? !!draft.organismId &&
          !!draft.identificationMethod &&
          !!draft.identifiedAt &&
          (draft.confidence === "" ||
            (Number.isFinite(Number(draft.confidence)) &&
              Number(draft.confidence) >= 0 &&
              Number(draft.confidence) <= 100)) &&
          (action.kind !== "reidentify" || !!draft.reason.trim())
        : action.kind === "received-origin"
          ? !!draft.sendingOrganizationId &&
            (!!draft.reportedOrganismId || !!draft.reportedOrganismText.trim())
          : action.kind === "acknowledge"
            ? !!draft.reason.trim()
            : action.kind === "sample-item"
              ? !needsSample || !!draft.sampleTypeId
              : action.kind === "test"
                ? !!draft.testId && (!needsSample || !!draft.sampleTypeId)
                : !!draft.testId &&
                  (action.kind !== "specimen-referral" ||
                    !!draft.sampleItemId) &&
                  (!needsSample || !!draft.sampleTypeId);
  const submit = () =>
    onSave({
      ...draft,
      confidence: draft.confidence === "" ? null : Number(draft.confidence),
      identifiedAt: isIdentification
        ? new Date(draft.identifiedAt).toISOString()
        : null,
    });
  const referral = ["referral", "specimen-referral"].includes(action.kind);
  return (
    <Modal
      open
      modalHeading={t(action.kind)}
      closeButtonLabel={intl.formatMessage({ id: "label.button.close" })}
      passiveModal={referral}
      primaryButtonText={t("save")}
      secondaryButtonText={t("cancel")}
      primaryButtonDisabled={!valid || saving}
      onRequestSubmit={submit}
      onRequestClose={() => {
        if (!saving) onClose();
      }}
      preventCloseOnClickOutside
    >
      <Stack gap={5}>
        {error && (
          <InlineNotification kind="error" title={t(error)} hideCloseButton />
        )}
        {action.kind === "pick" && (
          <>
            {select(
              "cultureId",
              cultures
                .filter(
                  (c) =>
                    !c.receivedIsolate &&
                    c.outcome === "GROWTH" &&
                    c.sourceSampleItemId,
                )
                .map((c) => ({ id: c.id, value: c.containerIdentifier })),
            )}
            {input("gramStain")}
            {input("colonyMorphology")}
            {select(
              "significance",
              [
                "SIGNIFICANT",
                "CONTAMINANT",
                "COLONIZER",
                "NORMAL_FLORA",
                "UNKNOWN",
              ].map((id) => ({ id, value: t(id) })),
            )}
          </>
        )}
        {isIdentification && (
          <>
            {combo("organismId", options.organisms)}
            {select("identificationMethod", options.methods)}
            {select(
              "identificationStatus",
              ["PRELIMINARY", "FINAL"].map((id) => ({ id, value: t(id) })),
            )}
            {input("identifiedAt", "datetime-local")}
            {input("confidence", "number")}
            {select(
              "significance",
              [
                "SIGNIFICANT",
                "CONTAMINANT",
                "COLONIZER",
                "NORMAL_FLORA",
                "UNKNOWN",
              ].map((id) => ({ id, value: t(id) })),
            )}
          </>
        )}
        {["identify", "reidentify", "acknowledge"].includes(action.kind) && (
          <TextArea
            id="isolate-reason"
            labelText={t("reason")}
            maxLength={2000}
            value={draft.reason}
            onChange={(e) => set("reason", e.target.value)}
          />
        )}
        {action.kind === "received-origin" && (
          <>
            {combo("sendingOrganizationId", options.organizations)}
            {combo("reportedOrganismId", options.organisms)}
            {input("reportedOrganismText")}
          </>
        )}
        {needsSample && select("sampleTypeId", options.sampleTypes)}
        {action.kind === "sample-item" &&
          select(
            "purpose",
            ["LABEL", "REFERRAL", "TESTING"].map((id) => ({
              id,
              value: t(`purpose.${id}`),
            })),
          )}
        {action.kind === "specimen-referral" &&
          select("sampleItemId", options.sources)}
        {["test", "referral", "specimen-referral"].includes(action.kind) &&
          select("testId", tests)}
        {referral && (
          <OrderReferOutForm
            referralOrganizations={options.organizations}
            referralReasons={options.reasons}
            isSaving={saving || !valid}
            onCancel={onClose}
            onSave={(values) => onSave({ ...draft, referral: values })}
          />
        )}
      </Stack>
    </Modal>
  );
}

export default function CaseIsolateWorkspace({
  detail,
  service,
  initialCultureId = "",
  onChanged,
  onClosePick,
  onLogCritical,
}) {
  const intl = useIntl();
  const t = (key, values) =>
    intl.formatMessage(
      { id: labels[key] || `microbiology.isolateWorkspace.${key}` },
      values,
    );
  const [data, setData] = useState(null);
  const [options, setOptions] = useState(null);
  const [cultures, setCultures] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [action, setAction] = useState(
    initialCultureId ? { kind: "pick", cultureId: initialCultureId } : null,
  );
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    let alive = true;
    setData(null);
    setError("");
    Promise.all([
      service.getIsolates(detail.id),
      service.getIsolateOptions(detail.id),
      service.getCultures(detail.id),
    ])
      .then(([workspace, choices, rows]) => {
        if (alive) {
          setData(workspace);
          setOptions(choices);
          setCultures(rows);
        }
      })
      .catch(() => {
        if (alive) setError("loadError");
      });
    return () => {
      alive = false;
      mounted.current = false;
    };
  }, [detail.id, detail.labUnitId, service]);
  const date = (value) =>
    value
      ? `${intl.formatDate(new Date(value))} ${intl.formatTime(new Date(value))}`
      : t("notAvailable");
  const close = () => {
    setAction(null);
    onClosePick?.();
  };
  const open = (kind, isolate) => {
    setError("");
    setAction({ kind, isolate });
  };
  const save = async (body) => {
    if (saving) return;
    setSaving(true);
    setError("");
    try {
      const updated =
        action.kind === "pick"
          ? await service.pickIsolate(detail.id, body)
          : action.kind === "specimen-referral"
            ? await service.referSpecimen(detail.id, body)
            : await service.isolateAction(
                detail.id,
                action.isolate.id,
                action.kind,
                body,
              );
      if (!mounted.current) return;
      setData(updated);
      setOptions(await service.getIsolateOptions(detail.id));
      if (!mounted.current) return;
      close();
      onChanged?.();
    } catch (failure) {
      if (!mounted.current) return;
      setError(failure.response?.status === 409 ? "conflict" : "saveError");
    } finally {
      if (mounted.current) setSaving(false);
    }
  };
  return (
    <Stack
      gap={5}
      data-testid="microbiology-isolates-card"
      style={{ gridTemplateColumns: "minmax(0, 1fr)" }}
    >
      <h2>{t("title")}</h2>
      {error && !action && (
        <InlineNotification kind="error" title={t(error)} hideCloseButton />
      )}
      {!data && !error && <InlineLoading description={t("loading")} />}
      {data && options && (
        <>
          {data.referred && <Tag type="purple">{t("referred")}</Tag>}
          {detail.canWrite && (
            <Stack gap={3}>
              <Button
                style={{ maxInlineSize: "100%" }}
                disabled={
                  !cultures.some(
                    (row) =>
                      !row.receivedIsolate &&
                      row.outcome === "GROWTH" &&
                      row.sourceSampleItemId,
                  )
                }
                onClick={() => open("pick")}
              >
                {t("pick")}
              </Button>
              <Button
                style={{ maxInlineSize: "100%" }}
                kind="tertiary"
                disabled={!options.sources.length}
                onClick={() => open("specimen-referral")}
              >
                {t("specimen-referral")}
              </Button>
            </Stack>
          )}
          {!data.isolates.length && <p>{t("empty")}</p>}
          {data.isolates.map((isolate) => (
            <Tile key={isolate.id} data-testid={`isolate-${isolate.id}`}>
              <ContainerLabelButton
                detail={detail}
                kind="ISO"
                rowId={isolate.id}
                service={service}
              />
              <Stack gap={4} style={{ gridTemplateColumns: "minmax(0, 1fr)" }}>
                <h3>
                  {isolate.label} —{" "}
                  {isolate.organismName ||
                    intl.formatMessage({
                      id: "microbiology.isolate.unidentified",
                    })}
                </h3>
                <Stack gap={3}>
                  <Tag type={isolate.status === "FINAL" ? "green" : "blue"}>
                    {t(isolate.status)}
                  </Tag>
                  <Tag type="gray">{t(isolate.significance)}</Tag>
                </Stack>
                <dl>
                  <dt>{t("cultureId")}</dt>
                  <dd>{isolate.cultureLabel || t("notAvailable")}</dd>
                  <dt>{t("gramStain")}</dt>
                  <dd>{isolate.gramStain || t("notAvailable")}</dd>
                  <dt>{t("colonyMorphology")}</dt>
                  <dd>{isolate.colonyMorphology || t("notAvailable")}</dd>
                  <dt>{t("sampleItemId")}</dt>
                  <dd>{isolate.sampleItemLabel || t("notCreated")}</dd>
                  <dt>{t("identificationMethod")}</dt>
                  <dd>{isolate.method || t("notAvailable")}</dd>
                  <dt>{t("identifiedAt")}</dt>
                  <dd>{date(isolate.identifiedAt)}</dd>
                  <dt>{t("confidence")}</dt>
                  <dd>
                    {isolate.confidence == null
                      ? t("notAvailable")
                      : intl.formatNumber(isolate.confidence)}
                  </dd>
                </dl>
                {isolate.received && (
                  <>
                    <Tag type="teal">{t("receivedGrowth")}</Tag>
                    <dl>
                      <dt>{t("sendingOrganizationId")}</dt>
                      <dd>
                        {isolate.sendingOrganizationName || t("notAvailable")}
                      </dd>
                      <dt>{t("reportedOrganismText")}</dt>
                      <dd>
                        {options.organisms.find(
                          (o) => o.id === isolate.reportedOrganismId,
                        )?.value ||
                          isolate.reportedOrganismText ||
                          t("notAvailable")}
                      </dd>
                    </dl>
                  </>
                )}
                {isolate.mismatch && (
                  <InlineNotification
                    kind={isolate.mismatchAcknowledgedAt ? "info" : "warning"}
                    title={t("mismatch")}
                    subtitle={
                      isolate.mismatchAcknowledgedAt
                        ? t("acknowledged", {
                            by: isolate.mismatchAcknowledgedBy,
                            at: date(isolate.mismatchAcknowledgedAt),
                          })
                        : t("acknowledgmentRequired")
                    }
                    hideCloseButton
                  />
                )}
                <Grid condensed>
                  {isolate.canIdentify && (
                    <Column sm={4} md={8} lg={16}>
                      <Button
                        style={{ maxInlineSize: "100%" }}
                        kind="tertiary"
                        onClick={() => open("identify", isolate)}
                      >
                        {t("identify")}
                      </Button>
                    </Column>
                  )}
                  {isolate.canReidentify && (
                    <Column sm={4} md={8} lg={16}>
                      <Button
                        style={{ maxInlineSize: "100%" }}
                        kind="tertiary"
                        onClick={() => open("reidentify", isolate)}
                      >
                        {t("reidentify")}
                      </Button>
                    </Column>
                  )}
                  {isolate.canAcknowledge &&
                    !isolate.mismatchAcknowledgedAt && (
                      <Column sm={4} md={8} lg={16}>
                        <Button
                          style={{ maxInlineSize: "100%" }}
                          kind="tertiary"
                          onClick={() => open("acknowledge", isolate)}
                        >
                          {t("acknowledge")}
                        </Button>
                      </Column>
                    )}
                  {onLogCritical && (
                    <Column sm={4} md={8} lg={16}>
                      <Button
                        style={{ maxInlineSize: "100%" }}
                        kind="ghost"
                        onClick={() => onLogCritical(isolate)}
                      >
                        {intl.formatMessage(
                          { id: "microbiology.critical.logForIsolate" },
                          { isolate: isolate.label },
                        )}
                      </Button>
                    </Column>
                  )}
                  {detail.canWrite && (
                    <>
                      {isolate.received && !isolate.organismId && (
                        <Column sm={4} md={8} lg={16}>
                          <Button
                            style={{ maxInlineSize: "100%" }}
                            kind="tertiary"
                            onClick={() => open("received-origin", isolate)}
                          >
                            {t("received-origin")}
                          </Button>
                        </Column>
                      )}
                      {!isolate.sampleItemId && (
                        <Column sm={4} md={8} lg={16}>
                          <Button
                            style={{ maxInlineSize: "100%" }}
                            kind="tertiary"
                            onClick={() => open("sample-item", isolate)}
                          >
                            {t("sample-item")}
                          </Button>
                        </Column>
                      )}
                      <Column sm={4} md={8} lg={16}>
                        <Button
                          style={{ maxInlineSize: "100%" }}
                          kind="tertiary"
                          onClick={() => open("test", isolate)}
                        >
                          {t("test")}
                        </Button>
                      </Column>
                      <Column sm={4} md={8} lg={16}>
                        <Button
                          style={{ maxInlineSize: "100%" }}
                          kind="tertiary"
                          onClick={() => open("referral", isolate)}
                        >
                          {t("referral")}
                        </Button>
                      </Column>
                    </>
                  )}
                </Grid>
                <Accordion>
                  <AccordionItem title={t("history")}>
                    <TableContainer>
                      <Table aria-label={t("history")}>
                        <TableHead>
                          <TableRow>
                            {[
                              "organismId",
                              "identificationStatus",
                              "identificationMethod",
                              "confidence",
                              "identifiedAt",
                              "by",
                              "recordedAt",
                              "reason",
                            ].map((key) => (
                              <TableHeader key={key}>{t(key)}</TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {isolate.history.map((event) => (
                            <TableRow key={event.id}>
                              <TableCell>{event.organismName}</TableCell>
                              <TableCell>{t(event.status)}</TableCell>
                              <TableCell>{event.method}</TableCell>
                              <TableCell>
                                {event.confidence == null
                                  ? t("notAvailable")
                                  : intl.formatNumber(event.confidence)}
                              </TableCell>
                              <TableCell>{date(event.identifiedAt)}</TableCell>
                              <TableCell>{event.by}</TableCell>
                              <TableCell>{date(event.at)}</TableCell>
                              <TableCell>{event.reason}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </AccordionItem>
                </Accordion>
              </Stack>
            </Tile>
          ))}
          <TableContainer title={t("referrals")}>
            <Table aria-label={t("referrals")}>
              <TableHead>
                <TableRow>
                  {[
                    "sampleItemId",
                    "testId",
                    "receivingLab",
                    "referralStatus",
                    "sentAt",
                    "transportDetails",
                  ].map((key) => (
                    <TableHeader key={key}>{t(key)}</TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {data.referrals.map((referral) => (
                  <TableRow key={referral.id}>
                    <TableCell>
                      {options.sources.find(
                        (s) => s.id === referral.sampleItemId,
                      )?.value || referral.sampleItemId}
                    </TableCell>
                    <TableCell>{referral.testName}</TableCell>
                    <TableCell>{referral.organizationName}</TableCell>
                    <TableCell>
                      <ReferralStatusTag status={referral.status} />
                    </TableCell>
                    <TableCell>{date(referral.sentAt)}</TableCell>
                    <TableCell>{referral.transportDetails}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          {action && (
            <IsolateAction
              key={`${action.kind}-${action.isolate?.id || "new"}`}
              action={action}
              options={options}
              cultures={cultures}
              saving={saving}
              error={error}
              onSave={save}
              onClose={close}
            />
          )}
        </>
      )}
    </Stack>
  );
}
