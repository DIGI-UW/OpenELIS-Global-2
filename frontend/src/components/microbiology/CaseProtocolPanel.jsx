import React, { useEffect, useMemo, useRef, useState } from "react";
import { Edit } from "@carbon/icons-react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  Select,
  SelectItem,
  Stack,
  Tag,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import "./CaseProtocolPanel.scss";

const CaseProtocolPanel = ({
  caseId,
  currentMethodId,
  open = false,
  readOnly = false,
  service,
  onOpen,
  onClose,
  onChanged,
}) => {
  const intl = useIntl();
  const [options, setOptions] = useState([]);
  const [targetMethodId, setTargetMethodId] = useState("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const triggerRef = useRef(null);
  const selectRef = useRef(null);
  const previousOpenRef = useRef(open);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    Promise.resolve(service.getCaseProtocolOptions(caseId))
      .then((response) => {
        if (active) {
          setOptions(Array.isArray(response) ? response : []);
        }
      })
      .catch((loadError) => {
        if (active) {
          setOptions([]);
          setError(loadError?.message || String(loadError));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [caseId, currentMethodId, service]);

  useEffect(() => {
    if (open && !previousOpenRef.current) {
      setTargetMethodId("");
      setReason("");
      setError("");
      setSaved(false);
    } else if (!open && previousOpenRef.current) {
      triggerRef.current?.focus();
    }
    previousOpenRef.current = open;
  }, [open]);

  useEffect(() => {
    if (open && !loading) {
      selectRef.current?.focus();
    }
  }, [loading, open]);

  const current = useMemo(
    () =>
      options.find((option) => option.id === currentMethodId) ||
      options.find((option) => option.current) ||
      null,
    [currentMethodId, options],
  );
  const summary = current
    ? [
        current.mediaDefaults,
        current.incubationDefaults,
        current.atmosphereDefaults,
      ]
        .filter(Boolean)
        .join(" - ")
    : "";
  const selectableOptions = options.filter(
    (option) => option.active && option.id !== currentMethodId,
  );
  const valid = Boolean(targetMethodId && reason.trim());

  const submit = () => {
    setSaving(true);
    setError("");
    setSaved(false);
    Promise.resolve(
      service.changeCaseProtocol(caseId, {
        cultureMethodId: targetMethodId,
        reason: reason.trim(),
      }),
    )
      .then((detail) => {
        if (
          !detail ||
          detail.error ||
          detail.status === 0 ||
          detail.status >= 400 ||
          detail.statusCode >= 400
        ) {
          throw new Error(detail?.message || detail?.error || "UNKNOWN_ERROR");
        }
        setSaved(true);
        onChanged(detail);
        onClose();
      })
      .catch((saveError) => {
        setError(formatMicrobiologyEnum(saveError?.message, intl));
      })
      .finally(() => setSaving(false));
  };

  return (
    <section
      className="microbiology-case-protocol"
      aria-labelledby="microbiology-case-protocol-title"
    >
      <Stack gap={4}>
        <div className="microbiology-case-protocol__header">
          <div>
            <h3 id="microbiology-case-protocol-title">
              {intl.formatMessage({ id: "microbiology.protocol.title" })}
            </h3>
            <p className="microbiology-case-protocol__helper">
              {intl.formatMessage({ id: "microbiology.protocol.helper" })}
            </p>
          </div>
          <Button
            ref={triggerRef}
            kind="tertiary"
            size="sm"
            renderIcon={Edit}
            disabled={readOnly || loading}
            onClick={onOpen}
          >
            {intl.formatMessage({
              id: current
                ? "microbiology.protocol.change"
                : "microbiology.protocol.set",
            })}
          </Button>
        </div>

        {loading ? (
          <InlineLoading
            description={intl.formatMessage({
              id: "microbiology.protocol.loading",
            })}
          />
        ) : current ? (
          <div className="microbiology-case-protocol__summary">
            <div className="microbiology-case-protocol__name-row">
              <p className="microbiology-case-protocol__name">
                {current.label}
              </p>
              {!current.active && (
                <Tag type="gray">
                  {intl.formatMessage({ id: "microbiology.protocol.inactive" })}
                </Tag>
              )}
            </div>
            {summary && <p>{summary}</p>}
          </div>
        ) : (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.protocol.noneTitle",
            })}
            subtitle={intl.formatMessage({
              id: "microbiology.protocol.noneMessage",
            })}
          />
        )}

        {saved && (
          <InlineNotification
            kind="success"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.protocol.updated",
            })}
          />
        )}
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "microbiology.protocol.error" })}
            subtitle={error}
          />
        )}

        {open && (
          <div className="microbiology-case-protocol__form">
            <Stack gap={5}>
              <Select
                ref={selectRef}
                id="microbiology-case-protocol-method"
                aria-label={intl.formatMessage({
                  id: "microbiology.protocol.field",
                })}
                labelText={intl.formatMessage({
                  id: "microbiology.protocol.field",
                })}
                value={targetMethodId}
                disabled={saving}
                onChange={(event) => setTargetMethodId(event.target.value)}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "microbiology.protocol.placeholder",
                  })}
                />
                {selectableOptions.map((option) => (
                  <SelectItem
                    key={option.id}
                    value={option.id}
                    text={option.label}
                  />
                ))}
              </Select>
              {selectableOptions.length === 0 && (
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage({
                    id: "microbiology.protocol.noChoices",
                  })}
                />
              )}
              <TextInput
                id="microbiology-case-protocol-reason"
                labelText={intl.formatMessage({
                  id: "microbiology.protocol.reason",
                })}
                value={reason}
                maxLength={255}
                disabled={saving}
                onChange={(event) => setReason(event.target.value)}
              />
              <div className="microbiology-case-protocol__actions">
                <Button disabled={!valid || saving} onClick={submit}>
                  {intl.formatMessage({ id: "microbiology.protocol.save" })}
                </Button>
                <Button kind="ghost" disabled={saving} onClick={onClose}>
                  {intl.formatMessage({ id: "button.cancel" })}
                </Button>
              </div>
            </Stack>
          </div>
        )}
      </Stack>
    </section>
  );
};

export default CaseProtocolPanel;
