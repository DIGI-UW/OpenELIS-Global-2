import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Column,
  ContentSwitcher,
  Grid,
  InlineNotification,
  Loading,
  Switch,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Tag,
  TextArea,
  TextInput,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../../common/CustomNotification";
import { TRIGGER_DESCRIPTORS } from "../descriptors";

// Substitute `[token]` placeholders in `template` with values from `sampleValues`.
// Tokens not present in `sampleValues` are rendered blank — matches the runtime
// dispatcher's behaviour for missing context entries.
function renderPreview(template, sampleValues) {
  if (!template) return "";
  return template.replace(/\[(\w+)\]/g, (_match, token) => {
    const v = sampleValues?.[token];
    return v === undefined || v === null ? "" : v;
  });
}

const CHANNEL_EMAIL = "EMAIL";
const CHANNEL_WHATSAPP = "WHATSAPP";

export default function TemplatesTab() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [templates, setTemplates] = useState([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [selectedChannel, setSelectedChannel] = useState(CHANNEL_EMAIL);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [dirtyByType, setDirtyByType] = useState({});

  // Refs to the body textareas, keyed by template type. Used to insert merge
  // tokens at the user's caret position when they click a merge-field tile.
  const bodyRefs = useRef({});

  const fetchTemplates = (signal) => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/NotificationTriggerConfig/templates",
      (res) => {
        if (signal?.aborted) return;
        if (res && Array.isArray(res.templates)) {
          setTemplates(res.templates);
        }
        setLoading(false);
        setDirtyByType({});
      },
      signal,
    );
  };

  useEffect(() => {
    const controller = new AbortController();
    fetchTemplates(controller.signal);
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateTemplate = (type, mutator) => {
    setTemplates((prev) =>
      prev.map((t) => (t.type === type ? mutator({ ...t }) : t)),
    );
    setDirtyByType((prev) => ({ ...prev, [type]: true }));
  };

  const onSubjectChange = (type, value) =>
    updateTemplate(type, (t) => ({ ...t, subjectTemplate: value }));

  const onBodyChange = (type, value) =>
    updateTemplate(type, (t) => ({ ...t, messageTemplate: value }));

  const onResetToDefault = (type) => {
    updateTemplate(type, (t) => ({
      ...t,
      subjectTemplate: t.defaultSubject,
      messageTemplate: t.defaultMessage,
    }));
  };

  const onSave = (type) => {
    const tpl = templates.find((t) => t.type === type);
    if (!tpl) return;
    setSaving(true);
    putToOpenElisServerJsonResponse(
      `/rest/NotificationTriggerConfig/templates/${type}`,
      JSON.stringify({
        subjectTemplate: tpl.subjectTemplate,
        messageTemplate: tpl.messageTemplate,
      }),
      (res) => {
        setSaving(false);
        if (res && res.type === type) {
          setTemplates((prev) => prev.map((t) => (t.type === type ? res : t)));
          setDirtyByType((prev) => ({ ...prev, [type]: false }));
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
          });
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const insertToken = (type, token) => {
    const ref = bodyRefs.current[type];
    const insertText = `[${token}]`;
    if (ref && typeof ref.selectionStart === "number") {
      const start = ref.selectionStart;
      const end = ref.selectionEnd;
      const before = ref.value.slice(0, start);
      const after = ref.value.slice(end);
      const next = before + insertText + after;
      onBodyChange(type, next);
      // Restore caret position after React re-renders the textarea.
      requestAnimationFrame(() => {
        if (bodyRefs.current[type]) {
          const caret = start + insertText.length;
          bodyRefs.current[type].selectionStart = caret;
          bodyRefs.current[type].selectionEnd = caret;
          bodyRefs.current[type].focus();
        }
      });
    } else {
      // No caret available — append at end.
      const tpl = templates.find((t) => t.type === type);
      if (tpl) onBodyChange(type, (tpl.messageTemplate || "") + insertText);
    }
  };

  const triggerName = (type) => {
    const descriptor = TRIGGER_DESCRIPTORS[type];
    if (descriptor) {
      return intl.formatMessage({
        id: descriptor.nameKey,
        defaultMessage: type,
      });
    }
    return type;
  };

  const triggerStatusLabel = (type) => {
    const descriptor = TRIGGER_DESCRIPTORS[type];
    return descriptor
      ? intl.formatMessage({ id: descriptor.statusKey, defaultMessage: "" })
      : "";
  };

  const eventDescription = (type) => {
    const descriptor = TRIGGER_DESCRIPTORS[type];
    if (descriptor?.templateDescriptionKey) {
      return intl.formatMessage({
        id: descriptor.templateDescriptionKey,
        defaultMessage: "",
      });
    }
    return "";
  };

  const sortedTypes = useMemo(() => templates.map((t) => t.type), [templates]);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {(loading || saving) && <Loading />}
      <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <h4>
            <FormattedMessage id="notificationtrigger.templates.heading" />
          </h4>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage id="notificationtrigger.templates.subtitle" />
          </p>
        </Column>
      </Grid>

      {templates.length > 0 && (
        <Tabs
          selectedIndex={activeIndex}
          onChange={({ selectedIndex }) => {
            setActiveIndex(selectedIndex);
            setSelectedChannel(CHANNEL_EMAIL);
          }}
        >
          <TabList aria-label="Notification template tabs">
            {sortedTypes.map((type) => (
              <Tab key={type}>
                <span
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  {triggerName(type)}
                  {triggerStatusLabel(type) && (
                    <Tag size="sm" type="green">
                      {triggerStatusLabel(type)}
                    </Tag>
                  )}
                </span>
              </Tab>
            ))}
          </TabList>
          <TabPanels>
            {templates.map((tpl) => {
              const type = tpl.type;
              const isDirty = !!dirtyByType[type];
              const isEmail = selectedChannel === CHANNEL_EMAIL;
              const previewSource = isEmail
                ? `${tpl.subjectTemplate || ""}\n\n${tpl.messageTemplate || ""}`
                : tpl.messageTemplate || "";
              const rendered = renderPreview(previewSource, tpl.sampleValues);

              return (
                <TabPanel key={type}>
                  <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
                    <Column lg={11} md={8} sm={4}>
                      <p style={{ color: "#525252", marginBottom: "1rem" }}>
                        {eventDescription(type)}
                      </p>

                      <div style={{ marginBottom: "1rem" }}>
                        <div
                          style={{
                            fontSize: "0.875rem",
                            fontWeight: 600,
                            marginBottom: "0.25rem",
                          }}
                        >
                          <FormattedMessage id="notificationtrigger.templates.channel.label" />
                        </div>
                        <ContentSwitcher
                          selectedIndex={isEmail ? 0 : 1}
                          onChange={({ index }) =>
                            setSelectedChannel(
                              index === 0 ? CHANNEL_EMAIL : CHANNEL_WHATSAPP,
                            )
                          }
                        >
                          <Switch
                            name={CHANNEL_EMAIL}
                            text={intl.formatMessage({
                              id: "notificationtrigger.channel.email",
                            })}
                          />
                          <Switch
                            name={CHANNEL_WHATSAPP}
                            text={intl.formatMessage({
                              id: "notificationtrigger.channel.whatsapp",
                            })}
                          />
                        </ContentSwitcher>
                        <p
                          style={{
                            color: "#6f6f6f",
                            fontSize: "0.75rem",
                            marginTop: "0.25rem",
                          }}
                        >
                          <FormattedMessage id="notificationtrigger.templates.channel.help" />
                        </p>
                      </div>

                      {isEmail && (
                        <div style={{ marginBottom: "1rem" }}>
                          <TextInput
                            id={`subject-${type}`}
                            labelText={intl.formatMessage({
                              id: "notificationtrigger.templates.subject.label",
                            })}
                            value={tpl.subjectTemplate || ""}
                            onChange={(e) =>
                              onSubjectChange(type, e.target.value)
                            }
                          />
                        </div>
                      )}

                      <div style={{ marginBottom: "0.25rem" }}>
                        <TextArea
                          id={`body-${type}`}
                          ref={(el) => (bodyRefs.current[type] = el)}
                          labelText={intl.formatMessage({
                            id: "notificationtrigger.templates.body.label",
                          })}
                          rows={10}
                          value={tpl.messageTemplate || ""}
                          onChange={(e) => onBodyChange(type, e.target.value)}
                          style={{ fontFamily: "monospace" }}
                        />
                      </div>
                      <p
                        style={{
                          color: "#6f6f6f",
                          fontSize: "0.75rem",
                          marginBottom: "1rem",
                        }}
                      >
                        <FormattedMessage id="notificationtrigger.templates.body.help" />
                      </p>

                      <Tile
                        light
                        style={{
                          backgroundColor: "#f4f4f4",
                          marginBottom: "1rem",
                        }}
                      >
                        <div
                          style={{
                            fontSize: "0.875rem",
                            fontWeight: 600,
                            marginBottom: "0.5rem",
                          }}
                        >
                          <FormattedMessage id="notificationtrigger.templates.preview.label" />
                        </div>
                        <div style={{ whiteSpace: "pre-wrap" }}>{rendered}</div>
                      </Tile>

                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-end",
                          gap: "0.5rem",
                        }}
                      >
                        <Button
                          kind="secondary"
                          onClick={() => onResetToDefault(type)}
                          disabled={saving}
                        >
                          <FormattedMessage id="notificationtrigger.templates.button.reset" />
                        </Button>
                        <Button
                          onClick={() => onSave(type)}
                          disabled={!isDirty || saving}
                        >
                          <FormattedMessage id="notificationtrigger.templates.button.save" />
                        </Button>
                      </div>
                    </Column>

                    <Column lg={5} md={8} sm={4}>
                      <h5 style={{ marginBottom: "0.5rem" }}>
                        <FormattedMessage id="notificationtrigger.templates.mergefields.heading" />
                      </h5>
                      <InlineNotification
                        kind="info"
                        lowContrast
                        hideCloseButton
                        title=""
                        subtitle={intl.formatMessage(
                          {
                            id: "notificationtrigger.templates.mergefields.help",
                          },
                          { eventCode: type },
                        )}
                      />
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          gap: "0.5rem",
                          marginTop: "0.5rem",
                        }}
                      >
                        {(tpl.availableVariables || []).map((v) => (
                          <Tile
                            key={v.token}
                            light
                            onClick={() => insertToken(type, v.token)}
                            style={{ cursor: "pointer" }}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(e) => {
                              if (e.key === "Enter" || e.key === " ") {
                                e.preventDefault();
                                insertToken(type, v.token);
                              }
                            }}
                          >
                            <div
                              style={{
                                fontFamily: "monospace",
                                fontSize: "0.875rem",
                              }}
                            >
                              {`[${v.token}]`}
                            </div>
                            <div
                              style={{
                                color: "#525252",
                                fontSize: "0.75rem",
                                marginTop: "0.25rem",
                              }}
                            >
                              {v.description}
                            </div>
                          </Tile>
                        ))}
                      </div>
                      <p
                        style={{
                          color: "#6f6f6f",
                          fontSize: "0.75rem",
                          marginTop: "0.75rem",
                        }}
                      >
                        <FormattedMessage id="notificationtrigger.templates.mergefields.optionalnote" />
                      </p>
                    </Column>
                  </Grid>
                </TabPanel>
              );
            })}
          </TabPanels>
        </Tabs>
      )}
    </>
  );
}
