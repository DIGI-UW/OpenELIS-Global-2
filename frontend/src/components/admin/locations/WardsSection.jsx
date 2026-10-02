import React, { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  ComboBox,
  FormGroup,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
  Toggle,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { LocationsContext } from "./LocationsPage";
import {
  createWard,
  listOrganizations,
  listWards,
  moveWard,
  updateWard,
} from "./locationsApi";

const COLUMNS = [
  "label.locations.column.name",
  "label.locations.column.code",
  "label.locations.column.serviceType",
  "label.locations.column.contact",
  "label.locations.column.phone",
  "label.locations.column.email",
  "label.locations.column.gps",
  "label.locations.column.inUse",
  "label.locations.column.active",
  "label.locations.column.actions",
];

const emptyWard = () => ({
  name: "",
  code: "",
  serviceType: "",
  contactName: "",
  phone: "",
  email: "",
  gpsLatitude: "",
  gpsLongitude: "",
});

const toRequest = (ward) => ({
  name: ward.name,
  code: ward.code,
  serviceType: ward.serviceType,
  contactName: ward.contactName,
  phone: ward.phone,
  email: ward.email,
  gpsLatitude:
    ward.gpsLatitude === "" || ward.gpsLatitude === null
      ? null
      : Number(ward.gpsLatitude),
  gpsLongitude:
    ward.gpsLongitude === "" || ward.gpsLongitude === null
      ? null
      : Number(ward.gpsLongitude),
  lastupdated: ward.lastupdated || null,
});

/**
 * Section D: an organization's wards / depts as inline rows, added in place,
 * edited, moved to another organization, and each with its own GPS or its
 * parent's shown as inherited.
 */
const WardsSection = ({
  organization,
  lists,
  highlight,
  statusFilter,
  sectionId,
  onActiveChange,
  onChanged,
  onDraftsChange,
}) => {
  const intl = useIntl();
  const { notify } = useContext(LocationsContext);
  const [wards, setWards] = useState(organization.wards || []);
  const [newWards, setNewWards] = useState([]);
  const [editing, setEditing] = useState(null);
  const [draft, setDraft] = useState(null);
  const [moving, setMoving] = useState(null);
  const [targets, setTargets] = useState([]);
  const [errors, setErrors] = useState({});
  const parent = organization.row;
  const includeInactive = statusFilter && statusFilter !== "active";

  const reload = () =>
    listWards(parent.id, true)
      .then((loaded) => setWards(loaded || []))
      .catch((error) => notify(error.message, "error"));

  useEffect(() => {
    setWards(organization.wards || []);
  }, [organization]);

  useEffect(() => {
    if (onDraftsChange) {
      onDraftsChange(newWards.filter((ward) => ward.name.trim()).length);
    }
  }, [newWards]);

  const serviceTypes = lists ? lists.serviceTypes : [];
  const shown = wards.filter((ward) => includeInactive || ward.active);
  const hit = (ward) =>
    highlight &&
    [ward.name, ward.code].some(
      (value) => value && value.toLowerCase().includes(highlight.toLowerCase()),
    );
  const gpsText = (ward) => {
    if (ward.gpsLatitude === null || ward.gpsLatitude === undefined) {
      return "-";
    }
    const text = `${ward.gpsLatitude}, ${ward.gpsLongitude}`;
    return ward.gpsInherited ? (
      <span
        className="cds--label"
        title={`${intl.formatMessage({ id: "label.locations.inherited" })} ${parent.name}`}
      >
        {text} ({intl.formatMessage({ id: "label.locations.inherited" })})
      </span>
    ) : (
      text
    );
  };

  const searchTargets = (text) => {
    if (!text || text.trim().length < 2) return;
    listOrganizations({ view: "organizations", q: text, pageSize: 20 })
      .then((page) =>
        setTargets(
          (page.items || [])
            .filter((row) => row.id !== parent.id)
            .map((row) => ({ id: row.id, text: row.name })),
        ),
      )
      .catch(() => setTargets([]));
  };

  const saveEdit = (ward) => {
    setErrors({});
    updateWard(parent.id, ward.id, toRequest(draft))
      .then(() => {
        setEditing(null);
        setDraft(null);
        reload();
        onChanged && onChanged();
      })
      .catch((error) => {
        if (error.status === 422) {
          setErrors(error.fieldErrors || {});
        }
        notify(error.message, "error");
      });
  };

  /**
   * FR-D2: new wards are saved one at a time. Each one saved leaves the drafts,
   * so a failure says which ward was not saved, and Save again sends only the
   * wards still waiting.
   */
  const saveNew = async () => {
    setErrors({});
    const ready = newWards.filter(
      (ward) => ward.name.trim() && ward.serviceType,
    );
    let saved = 0;
    for (const ward of ready) {
      try {
        await createWard(parent.id, toRequest(ward));
        saved++;
        setNewWards((current) => current.filter((draft) => draft !== ward));
      } catch (error) {
        if (error.status === 422) {
          setErrors(error.fieldErrors || {});
        }
        notify(
          intl.formatMessage(
            { id: "error.locations.ward.notSaved" },
            {
              name: ward.name.trim(),
              reason:
                error.message ||
                intl.formatMessage({ id: "error.locations.request.failed" }),
            },
          ),
          "error",
        );
        break;
      }
    }
    if (saved > 0) {
      reload();
      onChanged && onChanged();
    }
  };

  const move = (ward, target) => {
    if (!target) return;
    moveWard(ward.id, target.id)
      .then(() => {
        setMoving(null);
        reload();
        onChanged && onChanged();
      })
      .catch((error) => notify(error.message, "error"));
  };

  const serviceLabel = (code) => {
    const found = serviceTypes.find((type) => type.id === code);
    return found ? found.label : code || "";
  };

  return (
    <FormGroup
      legendText={`${intl.formatMessage({ id: "label.locations.section.wards" })} (${wards.filter((w) => w.active).length})`}
      id={sectionId}
    >
      <p className="cds--form__helper-text">
        <FormattedMessage id="help.locations.ward" />.{" "}
        <FormattedMessage
          id="help.locations.ward.gps"
          values={{ parent: parent.name }}
        />
      </p>
      <Table
        size="sm"
        aria-label={intl.formatMessage({ id: "label.locations.section.wards" })}
        data-testid="locations-wards"
      >
        <TableHead>
          <TableRow>
            {COLUMNS.map((column) => (
              <TableHeader key={column}>
                <FormattedMessage id={column} />
              </TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {shown.map((ward) =>
            editing === ward.id && draft ? (
              <TableRow key={ward.id}>
                <TableCell>
                  <TextInput
                    id={`ward-name-${ward.id}`}
                    labelText=""
                    hideLabel
                    value={draft.name}
                    invalid={!!errors.name}
                    invalidText={errors.name}
                    onChange={(e) =>
                      setDraft({ ...draft, name: e.target.value })
                    }
                  />
                </TableCell>
                <TableCell>
                  <TextInput
                    id={`ward-code-${ward.id}`}
                    labelText=""
                    hideLabel
                    value={draft.code || ""}
                    onChange={(e) =>
                      setDraft({ ...draft, code: e.target.value })
                    }
                  />
                </TableCell>
                <TableCell>
                  <Select
                    id={`ward-service-${ward.id}`}
                    labelText=""
                    hideLabel
                    value={draft.serviceType || ""}
                    invalid={!!errors.serviceType}
                    invalidText={errors.serviceType}
                    onChange={(e) =>
                      setDraft({ ...draft, serviceType: e.target.value })
                    }
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({ id: "label.select" })}
                    />
                    {serviceTypes.map((type) => (
                      <SelectItem
                        key={type.id}
                        value={type.id}
                        text={type.label}
                      />
                    ))}
                  </Select>
                </TableCell>
                {["contactName", "phone", "email"].map((field) => (
                  <TableCell key={field}>
                    <TextInput
                      id={`ward-${field}-${ward.id}`}
                      labelText=""
                      hideLabel
                      value={draft[field] || ""}
                      invalid={!!errors[field]}
                      invalidText={errors[field]}
                      onChange={(e) =>
                        setDraft({ ...draft, [field]: e.target.value })
                      }
                    />
                  </TableCell>
                ))}
                <TableCell>
                  <TextInput
                    id={`ward-lat-${ward.id}`}
                    labelText=""
                    hideLabel
                    placeholder={
                      parent.location && organization.gpsLatitude !== null
                        ? `${organization.gpsLatitude} (${intl.formatMessage({ id: "label.locations.inherited" })})`
                        : intl.formatMessage({
                            id: "label.locations.field.gpsLatitude",
                          })
                    }
                    value={draft.gpsLatitude === null ? "" : draft.gpsLatitude}
                    invalid={!!errors.gpsLatitude}
                    invalidText={errors.gpsLatitude}
                    onChange={(e) =>
                      setDraft({ ...draft, gpsLatitude: e.target.value })
                    }
                  />
                  <TextInput
                    id={`ward-lng-${ward.id}`}
                    labelText=""
                    hideLabel
                    placeholder={intl.formatMessage({
                      id: "label.locations.field.gpsLongitude",
                    })}
                    value={
                      draft.gpsLongitude === null ? "" : draft.gpsLongitude
                    }
                    invalid={!!errors.gpsLongitude}
                    invalidText={errors.gpsLongitude}
                    onChange={(e) =>
                      setDraft({ ...draft, gpsLongitude: e.target.value })
                    }
                  />
                </TableCell>
                <TableCell>
                  {(ward.inUse ? ward.inUse.total : 0).toLocaleString()}
                </TableCell>
                <TableCell>
                  <Tag type={ward.active ? "green" : "gray"} size="sm">
                    <FormattedMessage
                      id={
                        ward.active
                          ? "label.locations.status.active"
                          : "label.locations.status.inactive"
                      }
                    />
                  </Tag>
                </TableCell>
                <TableCell>
                  <Button
                    kind="secondary"
                    size="sm"
                    disabled={!draft.name.trim()}
                    onClick={() => saveEdit(ward)}
                  >
                    <FormattedMessage id="button.save" />
                  </Button>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => {
                      setEditing(null);
                      setDraft(null);
                    }}
                  >
                    <FormattedMessage id="button.cancel" />
                  </Button>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => {
                      setEditing(null);
                      setDraft(null);
                      setMoving(ward.id);
                    }}
                  >
                    <FormattedMessage id="button.locations.moveWard" />
                  </Button>
                </TableCell>
              </TableRow>
            ) : (
              <TableRow
                key={ward.id}
                className={hit(ward) ? "lo-highlight" : ""}
                data-testid={`locations-ward-${ward.id}`}
              >
                <TableCell>{ward.name}</TableCell>
                <TableCell>
                  <code>
                    {ward.code ||
                      intl.formatMessage({ id: "label.locations.none" })}
                  </code>
                </TableCell>
                <TableCell>{serviceLabel(ward.serviceType)}</TableCell>
                <TableCell>{ward.contactName || "-"}</TableCell>
                <TableCell>{ward.phone || "-"}</TableCell>
                <TableCell>{ward.email || "-"}</TableCell>
                <TableCell>{gpsText(ward)}</TableCell>
                <TableCell>
                  {(ward.inUse ? ward.inUse.total : 0).toLocaleString()}
                </TableCell>
                <TableCell>
                  <Toggle
                    id={`ward-active-${ward.id}`}
                    size="sm"
                    labelA={intl.formatMessage({
                      id: "label.locations.status.inactive",
                    })}
                    labelB={intl.formatMessage({
                      id: "label.locations.status.active",
                    })}
                    aria-label={`${intl.formatMessage({ id: "label.locations.column.active" })}, ${ward.name}`}
                    toggled={ward.active}
                    onToggle={(on) =>
                      onActiveChange &&
                      Promise.resolve(
                        onActiveChange({ ...ward, kind: "ward" }, on),
                      ).then(reload)
                    }
                  />
                </TableCell>
                <TableCell>
                  {moving === ward.id ? (
                    <ComboBox
                      id={`ward-move-${ward.id}`}
                      titleText=""
                      aria-label={intl.formatMessage({
                        id: "button.locations.moveWard",
                      })}
                      items={targets}
                      itemToString={(item) => (item ? item.text : "")}
                      onInputChange={searchTargets}
                      onChange={({ selectedItem }) => move(ward, selectedItem)}
                    />
                  ) : (
                    <Button
                      kind="ghost"
                      size="sm"
                      aria-label={`${intl.formatMessage({ id: "button.edit" })} ${ward.name}`}
                      onClick={() => {
                        setEditing(ward.id);
                        setDraft({
                          ...ward,
                          gpsLatitude: ward.gpsInherited
                            ? ""
                            : ward.gpsLatitude,
                          gpsLongitude: ward.gpsInherited
                            ? ""
                            : ward.gpsLongitude,
                        });
                      }}
                    >
                      <FormattedMessage id="button.edit" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ),
          )}
          {newWards.map((ward, index) => (
            <TableRow key={`new-${index}`} data-testid="locations-ward-draft">
              <TableCell>
                <TextInput
                  id={`new-ward-name-${index}`}
                  labelText=""
                  hideLabel
                  placeholder={intl.formatMessage({
                    id: "label.locations.ward.namePlaceholder",
                  })}
                  value={ward.name}
                  onChange={(e) =>
                    setNewWards(
                      newWards.map((w, i) =>
                        i === index ? { ...w, name: e.target.value } : w,
                      ),
                    )
                  }
                />
              </TableCell>
              <TableCell>
                <TextInput
                  id={`new-ward-code-${index}`}
                  labelText=""
                  hideLabel
                  placeholder={intl.formatMessage({
                    id: "label.locations.optional",
                  })}
                  value={ward.code}
                  onChange={(e) =>
                    setNewWards(
                      newWards.map((w, i) =>
                        i === index ? { ...w, code: e.target.value } : w,
                      ),
                    )
                  }
                />
              </TableCell>
              <TableCell>
                <Select
                  id={`new-ward-service-${index}`}
                  labelText=""
                  hideLabel
                  value={ward.serviceType}
                  invalid={!ward.serviceType}
                  invalidText=""
                  onChange={(e) =>
                    setNewWards(
                      newWards.map((w, i) =>
                        i === index ? { ...w, serviceType: e.target.value } : w,
                      ),
                    )
                  }
                >
                  <SelectItem
                    value=""
                    text={`${intl.formatMessage({ id: "label.locations.field.serviceType" })} *`}
                  />
                  {serviceTypes.map((type) => (
                    <SelectItem
                      key={type.id}
                      value={type.id}
                      text={type.label}
                    />
                  ))}
                </Select>
              </TableCell>
              {["contactName", "phone", "email"].map((field) => (
                <TableCell key={field}>
                  <TextInput
                    id={`new-ward-${field}-${index}`}
                    labelText=""
                    hideLabel
                    placeholder={intl.formatMessage({
                      id: "label.locations.optional",
                    })}
                    value={ward[field]}
                    onChange={(e) =>
                      setNewWards(
                        newWards.map((w, i) =>
                          i === index ? { ...w, [field]: e.target.value } : w,
                        ),
                      )
                    }
                  />
                </TableCell>
              ))}
              <TableCell className="cds--label">
                {organization.gpsLatitude !== null &&
                organization.gpsLatitude !== undefined
                  ? intl.formatMessage({ id: "label.locations.inherits" })
                  : ""}
              </TableCell>
              <TableCell />
              <TableCell>
                <Tag type="blue" size="sm">
                  <FormattedMessage id="label.locations.draft" />
                </Tag>
              </TableCell>
              <TableCell>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() =>
                    setNewWards(newWards.filter((_, i) => i !== index))
                  }
                >
                  <FormattedMessage id="button.remove" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Stack orientation="horizontal" gap={3} className="locationsWardActions">
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Add}
          data-testid="locations-add-ward"
          onClick={() => setNewWards([...newWards, emptyWard()])}
        >
          <FormattedMessage id="button.locations.addWard" />
        </Button>
        {newWards.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            data-testid="locations-save-wards"
            disabled={newWards.some(
              (ward) => !ward.name.trim() || !ward.serviceType,
            )}
            onClick={saveNew}
          >
            <FormattedMessage
              id="button.locations.saveWards"
              values={{ count: newWards.length }}
            />
          </Button>
        )}
      </Stack>
    </FormGroup>
  );
};

export default WardsSection;
