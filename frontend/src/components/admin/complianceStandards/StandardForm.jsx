import React, { useContext, useEffect, useState } from "react";
import {
  Tile,
  Grid,
  Column,
  Stack,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  ComboBox,
  Tag,
  Button,
  Accordion,
  DatePicker,
  DatePickerInput,
  InlineNotification,
} from "@carbon/react";
import { Add, Save, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
  putToOpenElisServerFullResponse,
  deleteFromOpenElisServer,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import ParameterGroupAccordionItem from "./ParameterGroupAccordionItem";

const STATUSES = ["DRAFT", "ACTIVE", "SUPERSEDED", "ARCHIVED"];

function ExplicitSampleTypesPanel({ sampleTypes, onChange }) {
  const intl = useIntl();
  const [pending, setPending] = useState("");
  const [allCategories, setAllCategories] = useState([]);

  useEffect(() => {
    let mounted = true;
    getFromOpenElisServer("/rest/compliance/sample-type-categories", (data) => {
      if (mounted && Array.isArray(data)) {
        setAllCategories(data);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const availableToAdd = allCategories.filter(
    (st) => !sampleTypes.includes(st),
  );

  const handleAdd = () => {
    if (pending && !sampleTypes.includes(pending)) {
      onChange([...sampleTypes, pending]);
      setPending("");
    }
  };
  const handleRemove = (st) => {
    onChange(sampleTypes.filter((s) => s !== st));
  };

  return (
    <div
      style={{
        background: "var(--cds-layer-01)",
        border: "1px solid var(--cds-border-subtle)",
        borderRadius: "4px",
        padding: "1rem",
        marginBottom: "1rem",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          gap: "1rem",
          flexWrap: "wrap",
        }}
      >
        <div style={{ flex: 1, minWidth: "240px" }}>
          <h5 style={{ marginTop: 0, marginBottom: "0.25rem" }}>
            <FormattedMessage
              id="compliance.sampleTypes.heading"
              defaultMessage="Applicable Sample Types"
            />
          </h5>
          <p
            style={{
              fontSize: "0.75rem",
              color: "var(--cds-text-02)",
              marginBottom: 0,
              marginTop: 0,
              maxWidth: "560px",
            }}
          >
            <FormattedMessage
              id="compliance.sampleTypes.explicitHint"
              defaultMessage="Declare the sample types used with this standard before linking tests. These choices drive the filter chips in the test linking form."
            />
          </p>
        </div>
        <div
          style={{
            display: "flex",
            alignItems: "flex-end",
            gap: "0.5rem",
          }}
        >
          <ComboBox
            id="add-sample-type"
            titleText=""
            items={availableToAdd}
            selectedItem={pending || null}
            onChange={({ selectedItem }) => setPending(selectedItem || "")}
            placeholder={intl.formatMessage({
              id: "compliance.sampleTypes.selectPlaceholder",
              defaultMessage: "Add sample type...",
            })}
            disabled={availableToAdd.length === 0}
            style={{ minWidth: "220px" }}
          />
          <Button
            kind="ghost"
            size="md"
            renderIcon={Add}
            onClick={handleAdd}
            disabled={!pending}
          >
            <FormattedMessage id="label.button.add" defaultMessage="Add" />
          </Button>
        </div>
      </div>

      {sampleTypes.length === 0 ? (
        <div style={{ marginTop: "0.75rem" }}>
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "compliance.sampleTypes.noneTitle",
              defaultMessage: "No sample types configured yet.",
            })}
            subtitle={intl.formatMessage({
              id: "compliance.sampleTypes.noneSubtitle",
              defaultMessage:
                "Add at least one sample type before linking tests.",
            })}
          />
        </div>
      ) : (
        <>
          <Stack
            orientation="horizontal"
            gap={2}
            style={{ flexWrap: "wrap", marginTop: "0.75rem" }}
          >
            {sampleTypes.map((st) => (
              <Tag
                key={st}
                type="blue"
                size="md"
                filter
                onClose={() => handleRemove(st)}
                title={intl.formatMessage({
                  id: "compliance.sampleTypes.remove",
                  defaultMessage: "Remove",
                })}
              >
                {st}
              </Tag>
            ))}
          </Stack>
          <p
            style={{
              fontSize: "0.75rem",
              color: "var(--cds-text-02)",
              marginTop: "0.5rem",
              marginBottom: 0,
            }}
          >
            <FormattedMessage
              id="compliance.sampleTypes.counter"
              defaultMessage="{count, plural, one {# sample type configured} other {# sample types configured}}"
              values={{ count: sampleTypes.length }}
            />
          </p>
        </>
      )}
    </div>
  );
}

function ParameterGroupsEditor({
  standardId,
  groups,
  onAdd,
  onDelete,
  onEdit,
  onMove,
  sampleTypes,
  hasSampleTypes,
}) {
  const intl = useIntl();
  const [adding, setAdding] = useState(false);
  const [newName, setNewName] = useState("");
  const [newDesc, setNewDesc] = useState("");

  const handleAdd = () => {
    if (newName.trim()) {
      onAdd({ name: newName.trim(), description: newDesc.trim() });
      setNewName("");
      setNewDesc("");
      setAdding(false);
    }
  };

  return (
    <div style={{ marginTop: "1.5rem" }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "0.5rem",
        }}
      >
        <h5 style={{ margin: 0 }}>
          <FormattedMessage
            id="compliance.parameterGroups.heading"
            defaultMessage="Parameter Groups & Linked Tests"
          />
        </h5>
        {!adding && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Add}
            onClick={() => setAdding(true)}
            disabled={!hasSampleTypes}
          >
            <FormattedMessage
              id="compliance.parameterGroups.add"
              defaultMessage="Add Group"
            />
          </Button>
        )}
      </div>

      {!hasSampleTypes && (
        <p
          style={{
            fontSize: "0.875rem",
            color: "var(--cds-text-placeholder)",
            fontStyle: "italic",
            marginBottom: "0.5rem",
          }}
        >
          <FormattedMessage
            id="compliance.parameterGroups.requireSampleTypes"
            defaultMessage="Configure the standard's fields and sample types first, then link tests."
          />
        </p>
      )}

      {groups.length > 0 && (
        <Accordion>
          {groups.map((g, idx) => (
            <ParameterGroupAccordionItem
              key={g.id}
              standardId={standardId}
              group={g}
              standardSampleTypes={sampleTypes}
              onDelete={() => onDelete(g)}
              onEdit={onEdit ? (updated) => onEdit(updated) : undefined}
              onMoveUp={onMove ? () => onMove(g, "up") : undefined}
              onMoveDown={onMove ? () => onMove(g, "down") : undefined}
              canMoveUp={idx > 0}
              canMoveDown={idx < groups.length - 1}
            />
          ))}
        </Accordion>
      )}

      {adding && (
        <Tile style={{ marginTop: "0.75rem", padding: "0.75rem" }}>
          <Stack gap={3}>
            <TextInput
              id="new-pg-name"
              labelText={intl.formatMessage({
                id: "compliance.parameterGroup.name",
                defaultMessage: "Group name",
              })}
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
            />
            <TextArea
              id="new-pg-desc"
              labelText={intl.formatMessage({
                id: "compliance.parameterGroup.description",
                defaultMessage: "Description",
              })}
              value={newDesc}
              onChange={(e) => setNewDesc(e.target.value)}
              rows={2}
            />
            <Stack orientation="horizontal" gap={2}>
              <Button kind="primary" size="sm" onClick={handleAdd}>
                <FormattedMessage id="label.button.add" defaultMessage="Add" />
              </Button>
              <Button kind="ghost" size="sm" onClick={() => setAdding(false)}>
                <FormattedMessage
                  id="label.button.cancel"
                  defaultMessage="Cancel"
                />
              </Button>
            </Stack>
          </Stack>
        </Tile>
      )}
    </div>
  );
}

function StandardForm({ standard, isNew, hideHeading, onSaved, onCancel }) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  // Toast on every action so the admin gets visible confirmation for each
  // save / add / delete / edit / move; inline errors are kept where they
  // attribute a specific row (e.g. save validation), but are paired with a
  // toast so the user is never left guessing whether anything happened.
  const toast = (kind, titleKey, titleDefault, messageKey, messageDefault) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: titleKey, defaultMessage: titleDefault }),
      message: intl.formatMessage({
        id: messageKey,
        defaultMessage: messageDefault,
      }),
    });
    setNotificationVisible(true);
  };
  const [name, setName] = useState(standard?.name || "");
  const [issuingBody, setIssuingBody] = useState(standard?.issuingBody || "");
  const [regulationNumber, setRegulationNumber] = useState(
    standard?.regulationNumber || "",
  );
  const [version, setVersion] = useState(standard?.version || "");
  const [countryRegion, setCountryRegion] = useState(
    standard?.countryRegion || "",
  );
  const [status, setStatus] = useState(standard?.status || "DRAFT");
  const toDateString = (d) => {
    if (!d) return "";
    if (typeof d === "string") return d;
    if (Array.isArray(d) && d.length >= 3) {
      return `${d[0]}-${String(d[1]).padStart(2, "0")}-${String(d[2]).padStart(2, "0")}`;
    }
    return "";
  };
  const [effectiveDate, setEffectiveDate] = useState(
    toDateString(standard?.effectiveDate),
  );
  const [expiryDate, setExpiryDate] = useState(
    toDateString(standard?.expiryDate),
  );
  const [sampleTypes, setSampleTypes] = useState(standard?.sampleTypes || []);
  const [description, setDescription] = useState(standard?.description || "");
  const [groups, setGroups] = useState(standard?.parameterGroups || []);
  const [savedId, setSavedId] = useState(standard?.id || null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [countryRegionOptions, setCountryRegionOptions] = useState([]);

  useEffect(() => {
    let mounted = true;
    getFromOpenElisServer(
      "/rest/compliance/standards/country-regions",
      (data) => {
        if (mounted && Array.isArray(data)) {
          setCountryRegionOptions(data);
        }
      },
    );
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!standard?.id) return;
    let mounted = true;
    getFromOpenElisServer(
      `/rest/compliance/standards/${standard.id}/parameter-groups`,
      (data) => {
        if (mounted && Array.isArray(data)) {
          setGroups(data);
        }
      },
    );
    return () => {
      mounted = false;
    };
  }, [standard?.id]);

  const [submitted, setSubmitted] = useState(false);

  const trim = (s) => (s || "").trim();
  const requiredErrors = {
    name: !trim(name),
    issuingBody: !trim(issuingBody),
    regulationNumber: !trim(regulationNumber),
    version: !trim(version),
    countryRegion: !trim(countryRegion),
    effectiveDate: !trim(effectiveDate),
  };
  const dateOrderError =
    effectiveDate && expiryDate && expiryDate < effectiveDate;

  const handleSave = () => {
    setSubmitted(true);
    if (dateOrderError) {
      toast(
        NotificationKinds.error,
        "notification.title.error",
        "Error",
        "compliance.validation.expiryAfterEffective",
        "Expiry date must be on or after the effective date.",
      );
      return;
    }
    if (
      requiredErrors.name ||
      requiredErrors.issuingBody ||
      requiredErrors.regulationNumber ||
      requiredErrors.version ||
      requiredErrors.countryRegion ||
      requiredErrors.effectiveDate
    ) {
      toast(
        NotificationKinds.error,
        "notification.title.error",
        "Error",
        "compliance.validation.fillRequired",
        "Please fill in all required fields.",
      );
      return;
    }
    setError(null);
    setSaving(true);
    const {
      parameterGroupCount: _pgIgnored,
      linkedTestCount: _ltIgnored,
      ...standardBase
    } = standard || {};
    const payload = {
      ...standardBase,
      name,
      issuingBody,
      regulationNumber,
      version,
      countryRegion,
      description: description || null,
      status,
      effectiveDate: effectiveDate || null,
      expiryDate: expiryDate || null,
      sampleTypes,
    };
    if (isNew) {
      postToOpenElisServerJsonResponse(
        "/rest/compliance/standards",
        JSON.stringify(payload),
        (resp) => {
          setSaving(false);
          if (resp && resp.id && !resp.error) {
            setSavedId(resp.id);
            toast(
              NotificationKinds.success,
              "notification.title.success",
              "Saved",
              "compliance.standard.created",
              "Compliance standard created.",
            );
            onSaved && onSaved(resp);
          } else {
            setError(
              (resp && (resp.error || resp.message)) ||
                intl.formatMessage({
                  id: "compliance.standard.saveError",
                  defaultMessage: "Could not save standard.",
                }),
            );
            toast(
              NotificationKinds.error,
              "notification.title.error",
              "Error",
              "compliance.standard.saveError",
              "Could not save standard.",
            );
          }
        },
      );
    } else {
      payload.id = standard.id;
      putToOpenElisServerFullResponse(
        `/rest/compliance/standards/${standard.id}`,
        JSON.stringify(payload),
        async (response) => {
          setSaving(false);
          if (response && response.ok) {
            toast(
              NotificationKinds.success,
              "notification.title.success",
              "Saved",
              "compliance.standard.updated",
              "Compliance standard updated.",
            );
            try {
              const saved = await response.json();
              onSaved && onSaved(saved);
            } catch (_e) {
              onSaved && onSaved(payload);
            }
          } else {
            let serverMsg = null;
            if (response) {
              try {
                const body = await response.json();
                serverMsg = body?.error || body?.message;
              } catch (_e) {
                /* ignore */
              }
            }
            setError(
              serverMsg ||
                intl.formatMessage({
                  id: "compliance.standard.saveError",
                  defaultMessage: "Could not save standard.",
                }),
            );
            toast(
              NotificationKinds.error,
              "notification.title.error",
              "Error",
              "compliance.standard.saveError",
              "Could not save standard.",
            );
          }
        },
      );
    }
  };

  const handleAddGroup = (group) => {
    if (!savedId) {
      setError(
        intl.formatMessage({
          id: "compliance.standard.saveBeforeGroups",
          defaultMessage: "Save the standard before adding parameter groups.",
        }),
      );
      toast(
        NotificationKinds.error,
        "notification.title.error",
        "Error",
        "compliance.standard.saveBeforeGroups",
        "Save the standard before adding parameter groups.",
      );
      return;
    }
    postToOpenElisServerJsonResponse(
      `/rest/compliance/standards/${savedId}/parameter-groups`,
      JSON.stringify({ ...group, sortOrder: groups.length + 1 }),
      (resp) => {
        if (resp && resp.id && !resp.error) {
          setGroups([...groups, resp]);
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Saved",
            "compliance.parameterGroup.created",
            "Parameter group added.",
          );
        } else {
          setError(
            (resp && (resp.error || resp.message)) ||
              intl.formatMessage({
                id: "compliance.parameterGroup.createError",
                defaultMessage: "Could not add parameter group.",
              }),
          );
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.parameterGroup.createError",
            "Could not add parameter group.",
          );
        }
      },
    );
  };

  const handleDeleteGroup = (g) => {
    if (!savedId) return;
    deleteFromOpenElisServer(
      `/rest/compliance/standards/${savedId}/parameter-groups/${g.id}`,
      (resp) => {
        // deleteFromOpenElisServer fires the callback regardless of HTTP
        // status; treat anything non-2xx as a failure so the admin learns
        // the row is still present.
        const ok = !resp || resp === true || (resp.status && resp.status < 300);
        if (ok) {
          setGroups(groups.filter((x) => x.id !== g.id));
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Deleted",
            "compliance.parameterGroup.deleted",
            "Parameter group deleted.",
          );
        } else {
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.parameterGroup.deleteError",
            "Could not delete parameter group.",
          );
        }
      },
    );
  };

  const handleEditGroup = (updated) => {
    if (!savedId || !updated || !updated.id) return;
    putToOpenElisServer(
      `/rest/compliance/standards/${savedId}/parameter-groups/${updated.id}`,
      JSON.stringify(updated),
      (status) => {
        if (status >= 200 && status < 300) {
          setGroups(groups.map((g) => (g.id === updated.id ? updated : g)));
          toast(
            NotificationKinds.success,
            "notification.title.success",
            "Saved",
            "compliance.parameterGroup.updated",
            "Parameter group updated.",
          );
        } else {
          setError(
            intl.formatMessage({
              id: "compliance.parameterGroup.updateError",
              defaultMessage: "Could not update parameter group.",
            }),
          );
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.parameterGroup.updateError",
            "Could not update parameter group.",
          );
        }
      },
    );
  };

  const handleMoveGroup = (g, direction) => {
    if (!savedId) return;
    const idx = groups.findIndex((x) => x.id === g.id);
    if (idx < 0) return;
    const swapIdx = direction === "up" ? idx - 1 : idx + 1;
    if (swapIdx < 0 || swapIdx >= groups.length) return;
    const a = groups[idx];
    const b = groups[swapIdx];
    const aOrder = a.sortOrder ?? idx + 1;
    const bOrder = b.sortOrder ?? swapIdx + 1;
    const updatedA = { ...a, sortOrder: bOrder };
    const updatedB = { ...b, sortOrder: aOrder };
    putToOpenElisServer(
      `/rest/compliance/standards/${savedId}/parameter-groups/${a.id}`,
      JSON.stringify(updatedA),
      (statusA) => {
        if (statusA < 200 || statusA >= 300) {
          toast(
            NotificationKinds.error,
            "notification.title.error",
            "Error",
            "compliance.parameterGroup.moveError",
            "Could not reorder parameter group.",
          );
          return;
        }
        putToOpenElisServer(
          `/rest/compliance/standards/${savedId}/parameter-groups/${b.id}`,
          JSON.stringify(updatedB),
          (statusB) => {
            if (statusB < 200 || statusB >= 300) {
              toast(
                NotificationKinds.error,
                "notification.title.error",
                "Error",
                "compliance.parameterGroup.moveError",
                "Could not reorder parameter group.",
              );
              return;
            }
            const next = [...groups];
            next[idx] = updatedB;
            next[swapIdx] = updatedA;
            next.sort((x, y) => (x.sortOrder ?? 0) - (y.sortOrder ?? 0));
            setGroups(next);
            toast(
              NotificationKinds.success,
              "notification.title.success",
              "Reordered",
              "compliance.parameterGroup.moved",
              "Parameter group reordered.",
            );
          },
        );
      },
    );
  };

  return (
    <div>
      {!hideHeading && (
        <h4 style={{ marginBottom: "1rem", marginTop: 0 }}>
          {isNew ? (
            <FormattedMessage
              id="compliance.standard.addNew"
              defaultMessage="Add New Compliance Standard"
            />
          ) : (
            <FormattedMessage
              id="compliance.standard.edit"
              defaultMessage="Edit Compliance Standard"
            />
          )}
        </h4>
      )}

      {error && (
        <div
          style={{
            color: "var(--cds-support-error)",
            fontSize: "0.875rem",
            marginBottom: "0.75rem",
          }}
        >
          {error}
        </div>
      )}

      <Grid>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="std-name"
            labelText={intl.formatMessage({
              id: "compliance.standard.name",
              defaultMessage: "Standard Name",
            })}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={intl.formatMessage({
              id: "compliance.standard.placeholder.name",
              defaultMessage: "e.g., PP No. 22/2021 — Water Quality",
            })}
            required
            invalid={submitted && requiredErrors.name}
            invalidText={intl.formatMessage({
              id: "compliance.validation.required",
              defaultMessage: "This field is required.",
            })}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="std-issuing-body"
            labelText={intl.formatMessage({
              id: "compliance.standard.issuingBody",
              defaultMessage: "Issuing Body",
            })}
            value={issuingBody}
            onChange={(e) => setIssuingBody(e.target.value)}
            placeholder={intl.formatMessage({
              id: "compliance.standard.placeholder.issuingBody",
              defaultMessage: "e.g., Government of Indonesia",
            })}
            required
            invalid={submitted && requiredErrors.issuingBody}
            invalidText={intl.formatMessage({
              id: "compliance.validation.required",
              defaultMessage: "This field is required.",
            })}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="std-reg-number"
            labelText={intl.formatMessage({
              id: "compliance.standard.regulationNumber",
              defaultMessage: "Regulation No.",
            })}
            value={regulationNumber}
            onChange={(e) => setRegulationNumber(e.target.value)}
            placeholder={intl.formatMessage({
              id: "compliance.standard.placeholder.regulationNumber",
              defaultMessage: "e.g., PP 22/2021",
            })}
            required
            invalid={submitted && requiredErrors.regulationNumber}
            invalidText={intl.formatMessage({
              id: "compliance.validation.required",
              defaultMessage: "This field is required.",
            })}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="std-version"
            labelText={intl.formatMessage({
              id: "compliance.standard.version",
              defaultMessage: "Version",
            })}
            value={version}
            onChange={(e) => setVersion(e.target.value)}
            placeholder={intl.formatMessage({
              id: "compliance.standard.placeholder.version",
              defaultMessage: "e.g., 2021",
            })}
            required
            invalid={submitted && requiredErrors.version}
            invalidText={intl.formatMessage({
              id: "compliance.validation.required",
              defaultMessage: "This field is required.",
            })}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="std-status"
            labelText={intl.formatMessage({
              id: "compliance.standard.status",
              defaultMessage: "Status",
            })}
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            {STATUSES.map((s) => (
              <SelectItem
                key={s}
                value={s}
                text={intl.formatMessage({
                  id: `compliance.status.${s}`,
                  defaultMessage: s,
                })}
              />
            ))}
          </Select>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            value={effectiveDate ? [effectiveDate] : []}
            onChange={(dates) => {
              if (dates && dates[0]) {
                setEffectiveDate(dates[0].toISOString().slice(0, 10));
              } else {
                setEffectiveDate("");
              }
            }}
          >
            <DatePickerInput
              id="std-eff-date"
              labelText={intl.formatMessage({
                id: "compliance.standard.effectiveDate",
                defaultMessage: "Effective Date",
              })}
              placeholder={intl.formatMessage({
                id: "compliance.standard.placeholder.date",
                defaultMessage: "yyyy-mm-dd",
              })}
              required
              invalid={submitted && requiredErrors.effectiveDate}
              invalidText={intl.formatMessage({
                id: "compliance.validation.required",
                defaultMessage: "This field is required.",
              })}
            />
          </DatePicker>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            value={expiryDate ? [expiryDate] : []}
            onChange={(dates) => {
              if (dates && dates[0]) {
                setExpiryDate(dates[0].toISOString().slice(0, 10));
              } else {
                setExpiryDate("");
              }
            }}
          >
            <DatePickerInput
              id="std-expiry-date"
              labelText={intl.formatMessage({
                id: "compliance.standard.expiryDate",
                defaultMessage: "Expiry Date",
              })}
              placeholder={intl.formatMessage({
                id: "compliance.standard.placeholder.date",
                defaultMessage: "yyyy-mm-dd",
              })}
              invalid={dateOrderError}
              invalidText={intl.formatMessage({
                id: "compliance.validation.expiryAfterEffective",
                defaultMessage:
                  "Expiry date must be on or after the effective date.",
              })}
            />
          </DatePicker>
        </Column>
        <Column lg={4} md={4} sm={4}>
          {/* FR-1-007: ComboBox with type-ahead + free-text. Carbon's
              ComboBox doesn't accept arbitrary input by default, so we
              wire onInputChange to update the selected value as the
              admin types — picking an existing item or creating a new
              one work the same way. */}
          <ComboBox
            id="std-country"
            titleText={intl.formatMessage({
              id: "compliance.standard.countryRegion",
              defaultMessage: "Country / Region",
            })}
            items={countryRegionOptions}
            selectedItem={countryRegion || null}
            onChange={({ selectedItem }) =>
              setCountryRegion(selectedItem || "")
            }
            onInputChange={(value) => setCountryRegion(value || "")}
            placeholder={intl.formatMessage({
              id: "compliance.standard.placeholder.countryRegion",
              defaultMessage: "e.g., Indonesia",
            })}
            allowCustomValue
            invalid={submitted && requiredErrors.countryRegion}
            invalidText={intl.formatMessage({
              id: "compliance.validation.required",
              defaultMessage: "This field is required.",
            })}
          />
        </Column>
        {/* FR-1-007: Description is a full-width TextArea (optional). Placed
            after the field grid so the multi-line input doesn't disrupt the
            4-column header layout. */}
        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="std-description"
            labelText={intl.formatMessage({
              id: "compliance.standard.description",
              defaultMessage: "Description",
            })}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={intl.formatMessage({
              id: "compliance.standard.placeholder.description",
              defaultMessage:
                "Optional. Full description of the standard's scope.",
            })}
            rows={3}
            maxLength={2048}
          />
        </Column>
      </Grid>

      <div style={{ marginTop: "1rem" }}>
        <ExplicitSampleTypesPanel
          sampleTypes={sampleTypes}
          onChange={setSampleTypes}
        />
      </div>

      {savedId && (
        <ParameterGroupsEditor
          standardId={savedId}
          groups={groups}
          onAdd={handleAddGroup}
          onDelete={handleDeleteGroup}
          onEdit={handleEditGroup}
          onMove={handleMoveGroup}
          sampleTypes={sampleTypes}
          hasSampleTypes={sampleTypes.length > 0}
        />
      )}

      <Stack
        orientation="horizontal"
        gap={3}
        style={{ marginTop: "1.5rem", alignItems: "center" }}
      >
        <Button
          kind="primary"
          renderIcon={Save}
          onClick={handleSave}
          disabled={
            saving ||
            !name ||
            !issuingBody ||
            !regulationNumber ||
            !version ||
            !countryRegion ||
            !effectiveDate
          }
        >
          {saving ? (
            <FormattedMessage
              id="compliance.button.saving"
              defaultMessage="Saving…"
            />
          ) : isNew ? (
            <FormattedMessage
              id="compliance.button.save"
              defaultMessage="Save"
            />
          ) : (
            <FormattedMessage
              id="compliance.button.saveStandard"
              defaultMessage="Save Standard"
            />
          )}
        </Button>
        <Button kind="ghost" onClick={onCancel} disabled={saving}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
      </Stack>
    </div>
  );
}

export default StandardForm;
