import React, { useContext, useEffect, useState } from "react";
import { Button, Stack, TextInput, Tile, Toggle } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { settingDescription, settingName } from "./settingLabels";
import { SETTING_DEPENDS_ON, groupResultSettings } from "./resultSettingGroups";

const SETTING_API = "/rest/ResultConfiguration";

const saveSetting = (setting, value) =>
  new Promise((resolve, reject) => {
    getFromOpenElisServer(`${SETTING_API}?ID=${setting.id}`, (stored) => {
      if (!stored) {
        reject(new Error(setting.name));
        return;
      }
      postToOpenElisServer(
        `${SETTING_API}?ID=${setting.id}`,
        JSON.stringify({
          ...stored,
          value,
          lastupdated: setting.lastupdated ?? stored.lastupdated,
        }),
        (status) => {
          if (status === 200) {
            resolve();
            return;
          }
          const error = new Error(setting.name);
          error.stale = status === 409;
          reject(error);
        },
      );
    });
  });

/**
 * OGC-1364 v3: the result settings of Result & Validation Configuration as
 * plain-language switches grouped by purpose, each group with its own Save,
 * in place of the select-a-row-then-Modify table. A switch that only applies
 * while another is on (the technician-name autofill) is shown under it and
 * disabled while that one is off.
 */
export default function ResultValidationSettings({
  settings,
  onSaved,
  policyEnabled = true,
}) {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [draft, setDraft] = useState({});
  const [saving, setSaving] = useState(null);

  useEffect(() => {
    const stored = Object.fromEntries(
      (settings || []).map((setting) => [setting.id, setting.value]),
    );
    setDraft((prev) =>
      Object.fromEntries(
        Object.entries(prev).filter(([id, value]) => stored[id] !== value),
      ),
    );
  }, [settings]);

  const valueOf = (setting) =>
    Object.prototype.hasOwnProperty.call(draft, setting.id)
      ? draft[setting.id]
      : setting.value;
  const byName = Object.fromEntries(
    (settings || []).map((setting) => [setting.name, setting]),
  );
  const enabled = (setting) => {
    const parent = byName[SETTING_DEPENDS_ON[setting.name]];
    return !parent || valueOf(parent) === "true";
  };
  const changedIn = (group) =>
    group.settings.filter(
      (setting) =>
        Object.prototype.hasOwnProperty.call(draft, setting.id) &&
        draft[setting.id] !== setting.value,
    );

  const notify = (kind, id, values) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id }, values),
    });
    setNotificationVisible(true);
  };

  const saveGroup = async (group) => {
    const changed = changedIn(group);
    const saved = [];
    setSaving(group.id);
    try {
      for (const setting of changed) {
        try {
          await saveSetting(setting, draft[setting.id]);
        } catch (error) {
          const values = {
            saved: saved.join(", "),
            setting: settingName(intl, setting.name),
          };
          notify(
            NotificationKinds.error,
            error.stale
              ? saved.length > 0
                ? "resultSettings.stalePartial"
                : "resultSettings.stale"
              : saved.length > 0
                ? "resultSettings.savePartial"
                : "resultSettings.saveFailed",
            values,
          );
          if (error.stale) {
            setDraft((prev) => {
              const rest = { ...prev };
              delete rest[setting.id];
              return rest;
            });
          }
          if (error.stale || saved.length > 0) onSaved?.();
          return;
        }
        saved.push(settingName(intl, setting.name));
      }
      notify(NotificationKinds.success, "resultSettings.saved", {
        group: intl.formatMessage({ id: group.titleKey }),
      });
      onSaved?.();
    } finally {
      setSaving(null);
    }
  };

  const renderSetting = (setting) => {
    const label = settingName(intl, setting.name);
    const help = settingDescription(intl, setting.name, setting.description);
    const dependent = Boolean(SETTING_DEPENDS_ON[setting.name]);
    const style = dependent ? { marginLeft: "2rem" } : undefined;
    if (setting.valueType === "boolean") {
      return (
        <div
          key={setting.id}
          style={style}
          data-testid={`setting-${setting.id}`}
        >
          <Toggle
            id={`setting-${setting.id}`}
            size="sm"
            labelText={label}
            labelA={intl.formatMessage({ id: "resultSettings.off" })}
            labelB={intl.formatMessage({ id: "resultSettings.on" })}
            toggled={valueOf(setting) === "true"}
            disabled={!enabled(setting)}
            onToggle={(on) =>
              setDraft((prev) => ({ ...prev, [setting.id]: String(on) }))
            }
          />
          {help && <p className="cds--form__helper-text">{help}</p>}
        </div>
      );
    }
    return (
      <div key={setting.id} style={style} data-testid={`setting-${setting.id}`}>
        <TextInput
          id={`setting-${setting.id}`}
          labelText={label}
          helperText={help}
          value={valueOf(setting) ?? ""}
          disabled={!enabled(setting)}
          onChange={(event) => {
            const typed = event.target.value;
            setDraft((prev) => ({ ...prev, [setting.id]: typed }));
          }}
        />
      </div>
    );
  };

  return (
    <Stack gap={6} data-testid="result-validation-settings">
      {groupResultSettings(settings, { policyEnabled }).map((group) => (
        <Tile key={group.id} data-testid={`settings-group-${group.id}`}>
          <h4>
            <FormattedMessage id={group.titleKey} />
          </h4>
          <Stack gap={5}>
            {group.settings.map(renderSetting)}
            <div>
              <Button
                size="sm"
                kind="primary"
                disabled={changedIn(group).length === 0 || saving !== null}
                onClick={() => saveGroup(group)}
                data-testid={`save-group-${group.id}`}
              >
                <FormattedMessage id="resultSettings.save" />
              </Button>
            </div>
          </Stack>
        </Tile>
      ))}
    </Stack>
  );
}
