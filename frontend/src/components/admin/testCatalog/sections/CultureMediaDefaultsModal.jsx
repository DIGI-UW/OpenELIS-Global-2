import React, { useEffect, useState } from "react";
import {
  Modal,
  Select,
  SelectItem,
  Stack,
  TextInput,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
export default function CultureMediaDefaultsModal({
  testId,
  link,
  onClose,
  onSaved,
}) {
  const intl = useIntl();
  const t = (key) => intl.formatMessage({ id: `microbiology.culture.${key}` });
  const [atmospheres, setAtmospheres] = useState([]);
  const [draft, setDraft] = useState({ ...link });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  useEffect(() => {
    let live = true;
    getFromOpenElisServer("/rest/microbiology/culture-catalog", (data) => {
      if (live && data?.atmospheres) setAtmospheres(data.atmospheres);
    });
    return () => {
      live = false;
    };
  }, []);
  const input = (key, label) => (
    <TextInput
      id={`media-default-${key}`}
      type="number"
      step="0.01"
      labelText={t(label)}
      value={draft[key] ?? ""}
      onChange={(e) =>
        setDraft((previous) => ({ ...previous, [key]: e.target.value }))
      }
    />
  );
  const save = () => {
    setSaving(true);
    setError(false);
    const body = { ...draft, cultureDefaultsChanged: true };
    [
      "cultureDuration",
      "cultureTemperature",
      "cultureCheckIntervalHours",
      "cultureLoopVolume",
    ].forEach((key) => {
      body[key] =
        body[key] === "" || body[key] == null ? null : Number(body[key]);
    });
    body.cultureDurationUnit =
      body.cultureDuration == null
        ? null
        : draft.cultureDurationUnit || "HOURS";
    body.cultureAtmosphereId = draft.cultureAtmosphereId || null;
    putToOpenElisServer(
      `/rest/test-catalog/${testId}/reagents/${link.reagentId}`,
      JSON.stringify(body),
      (status) => {
        setSaving(false);
        if (status >= 200 && status < 300) onSaved();
        else setError(true);
      },
    );
  };
  return (
    <Modal
      open
      closeButtonLabel={intl.formatMessage({ id: "label.button.close" })}
      modalHeading={t("mediaDefaults")}
      primaryButtonText={t("save")}
      secondaryButtonText={t("cancel")}
      primaryButtonDisabled={saving}
      onRequestSubmit={save}
      onRequestClose={onClose}
      preventCloseOnClickOutside
    >
      <Stack gap={5}>
        {error && (
          <InlineNotification
            kind="error"
            title={t("saveError")}
            hideCloseButton
          />
        )}
        <p>{t("mediaDefaultsHelp")}</p>
        {input("cultureDuration", "duration")}
        <Select
          id="media-default-unit"
          labelText={t("durationUnit")}
          value={draft.cultureDurationUnit || "HOURS"}
          onChange={(e) =>
            setDraft((previous) => ({
              ...previous,
              cultureDurationUnit: e.target.value,
            }))
          }
        >
          {["HOURS", "DAYS", "WEEKS"].map((unit) => (
            <SelectItem key={unit} value={unit} text={t(`unit.${unit}`)} />
          ))}
        </Select>
        {input("cultureCheckIntervalHours", "checkIntervalHours")}
        {input("cultureLoopVolume", "loopVolume")}
        {input("cultureTemperature", "temperature")}
        <Select
          id="media-default-atmosphere"
          labelText={t("atmosphereId")}
          value={draft.cultureAtmosphereId || ""}
          onChange={(e) =>
            setDraft((previous) => ({
              ...previous,
              cultureAtmosphereId: e.target.value,
            }))
          }
        >
          <SelectItem value="" text={t("choose")} />
          {atmospheres.map((option) => (
            <SelectItem key={option.id} value={option.id} text={option.value} />
          ))}
        </Select>
      </Stack>
    </Modal>
  );
}
