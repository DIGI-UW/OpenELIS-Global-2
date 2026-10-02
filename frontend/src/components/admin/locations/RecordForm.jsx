import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Column,
  ComboBox,
  FilterableMultiSelect,
  FormGroup,
  Grid,
  InlineNotification,
  Link,
  RadioButton,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { Add, RecentlyViewed } from "@carbon/icons-react";
import { LocationsContext } from "./LocationsPage";
import LocationComboBox from "./LocationComboBox";
import HistoryPanel from "./HistoryPanel";
import WardsSection from "./WardsSection";
import { toLocalIsoDate } from "../../utils/Utils";
import {
  createOrganization,
  getOrganization,
  pathText,
  updateOrganization,
} from "./locationsApi";

const EMPTY_REFERRAL = {
  approvalStatus: "",
  accreditationBody: "",
  accreditationNumber: "",
  accreditationExpiry: "",
  lastReviewDate: "",
  nextReviewDue: "",
  reviewNotes: "",
};

const emptyForm = (kind) => ({
  name: "",
  shortName: "",
  typeIds: [],
  categoryId: "",
  ownershipId: "",
  description: "",
  location: null,
  streetAddress: "",
  city: "",
  state: "",
  zipCode: "",
  gpsLatitude: "",
  gpsLongitude: "",
  contactName: "",
  phone: "",
  fax: "",
  email: "",
  internetAddress: "",
  identifiers: [{ label: "Code", value: "", reporting: true }],
  referral: { ...EMPTY_REFERRAL },
  site: { siteType: "", subtype: "", environmentalZone: "" },
  kind,
  lastupdated: null,
});

const fromDetail = (detail) => ({
  name: detail.row.name || "",
  shortName: detail.row.shortName || "",
  typeIds: (detail.row.types || []).map((type) => type.id),
  categoryId: detail.row.category ? detail.row.category.id : "",
  ownershipId: detail.row.ownership ? detail.row.ownership.id : "",
  description: detail.description || "",
  location: detail.row.location
    ? {
        ...detail.row.location,
        text: detail.row.location.name,
        sub: pathText(detail.row.location),
      }
    : null,
  streetAddress: detail.streetAddress || "",
  city: detail.city || "",
  state: detail.state || "",
  zipCode: detail.zipCode || "",
  gpsLatitude:
    detail.gpsLatitude === null || detail.gpsLatitude === undefined
      ? ""
      : String(detail.gpsLatitude),
  gpsLongitude:
    detail.gpsLongitude === null || detail.gpsLongitude === undefined
      ? ""
      : String(detail.gpsLongitude),
  contactName: detail.contactName || "",
  phone: detail.phone || "",
  fax: detail.fax || "",
  email: detail.email || "",
  internetAddress: detail.internetAddress || "",
  identifiers:
    detail.identifiers && detail.identifiers.length
      ? detail.identifiers.map((i) => ({ ...i }))
      : [{ label: "Code", value: "", reporting: true }],
  referral: { ...EMPTY_REFERRAL, ...(detail.referral || {}) },
  site: {
    siteType: (detail.site && detail.site.siteType) || "",
    subtype: (detail.site && detail.site.subtype) || "",
    environmentalZone: (detail.site && detail.site.environmentalZone) || "",
  },
  kind: detail.row.kind,
  lastupdated: detail.lastupdated,
  registry: detail.row.registry,
});

const toRequest = (form, kind) => ({
  kind,
  name: form.name,
  shortName: form.shortName,
  typeIds: form.typeIds,
  categoryId: form.categoryId,
  ownershipId: form.ownershipId,
  description: form.description,
  parentId: form.location ? form.location.id : null,
  streetAddress: form.streetAddress,
  city: form.city,
  state: form.state,
  zipCode: form.zipCode,
  gpsLatitude: form.gpsLatitude === "" ? null : Number(form.gpsLatitude),
  gpsLongitude: form.gpsLongitude === "" ? null : Number(form.gpsLongitude),
  contactName: form.contactName,
  phone: form.phone,
  fax: form.fax,
  email: form.email,
  internetAddress: form.internetAddress,
  identifiers: form.identifiers.filter(
    (i) =>
      (i.value || "").trim() ||
      ((i.label || "").trim() && i.label.trim() !== "Code"),
  ),
  referral: form.referral,
  site: form.site,
  lastupdated: form.lastupdated,
});

const badLatitude = (value) =>
  value !== "" &&
  (Number.isNaN(Number(value)) || Number(value) < -90 || Number(value) > 90);
const badLongitude = (value) =>
  value !== "" &&
  (Number.isNaN(Number(value)) || Number(value) < -180 || Number(value) > 180);

const isReferralType = (type) =>
  type && /^referral\s*lab$/i.test((type.name || "").trim());

const sameValue = (a, b) => JSON.stringify(a) === JSON.stringify(b);

/** The form field, by its error key, that a message about it points at. */
const FIELD_ELEMENT = {
  name: "name",
  shortName: "shortName",
  types: "types",
  description: "description",
  identifiers: "identifier-value",
  parent: "location",
  streetAddress: "street",
  city: "city",
  state: "state",
  zipCode: "zip",
  gpsLatitude: "lat",
  gpsLongitude: "lng",
  contactName: "contact",
  phone: "phone",
  email: "email",
  internetAddress: "web",
  referral: "ref-status",
  "referral.approvalStatus": "ref-status",
  "referral.accreditationBody": "ref-body",
  "referral.accreditationNumber": "ref-number",
  "referral.accreditationExpiry": "ref-expiry",
  "referral.lastReviewDate": "ref-last",
  "referral.nextReviewDue": "ref-next",
  "referral.reviewNotes": "ref-notes",
};

/** The label a message uses for a field, by its form or error key. */
const FIELD_LABEL = {
  name: "label.locations.column.name",
  shortName: "label.locations.field.shortName",
  types: "label.locations.field.types",
  typeIds: "label.locations.field.types",
  categoryId: "label.locations.field.category",
  ownershipId: "label.locations.field.ownership",
  description: "label.locations.field.description",
  identifiers: "label.locations.section.identifiers",
  location: "label.locations.field.location",
  parent: "label.locations.field.location",
  streetAddress: "label.locations.field.streetAddress",
  city: "label.locations.field.city",
  state: "label.locations.field.state",
  zipCode: "label.locations.field.zipCode",
  gpsLatitude: "label.locations.field.gpsLatitude",
  gpsLongitude: "label.locations.field.gpsLongitude",
  contactName: "label.locations.field.contactName",
  phone: "label.locations.field.phone",
  fax: "label.locations.field.phone",
  email: "label.locations.field.email",
  internetAddress: "label.locations.field.website",
  referral: "label.locations.referral.approval",
  site: "label.locations.section.site",
  registry: "label.locations.section.identity",
};

/**
 * FR-C5: after another admin saved first, the fields this user changed keep
 * their values and the rest take the other admin's; returns the merged form
 * and the fields the other admin changed.
 */
const mergeAfterConflict = (form, original, theirs) => {
  const merged = { ...form, lastupdated: theirs.lastupdated };
  const changedByThem = [];
  Object.keys(theirs).forEach((key) => {
    if (key === "lastupdated" || key === "kind") return;
    if (!sameValue(original[key], theirs[key])) {
      changedByThem.push(key);
      if (sameValue(form[key], original[key])) {
        merged[key] = theirs[key];
      }
    }
  });
  return { merged, changedByThem };
};

/**
 * FR-C1 to FR-C5: the inline record form, built from the sampling-site form,
 * with the sections the kind needs: Identity, Identifiers, Location, Contact,
 * Referral laboratory, Site details and Wards / Depts, plus the record's
 * history. Validation is inline; a stale save shows the other admin's values.
 */
const RecordForm = ({
  id,
  isNew,
  kind: kindProp,
  lists,
  highlight,
  statusFilter,
  onCancel,
  onSaved,
  onActiveChange,
  reload,
  onDirtyChange,
}) => {
  const intl = useIntl();
  const { notify } = useContext(LocationsContext);
  const [detail, setDetail] = useState(null);
  const [form, setForm] = useState(isNew ? emptyForm(kindProp) : null);
  const [tried, setTried] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [conflict, setConflict] = useState(null);
  const [saving, setSaving] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [wardDrafts, setWardDrafts] = useState(0);
  const container = useRef(null);
  const kind = kindProp || (form && form.kind) || "facility";

  useEffect(() => {
    if (onDirtyChange) onDirtyChange(dirty);
  }, [dirty]);

  useEffect(() => {
    if (isNew) {
      return undefined;
    }
    let current = true;
    getOrganization(id)
      .then((loaded) => {
        if (!current) return;
        setDetail(loaded);
        setForm(fromDetail(loaded));
      })
      .catch((error) => current && notify(error.message, "error"));
    return () => {
      current = false;
    };
  }, [id, isNew]);

  useEffect(() => {
    if (!dirty) return undefined;
    const warn = (event) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  const set = (field) => (eventOrValue) => {
    const value =
      eventOrValue && eventOrValue.target
        ? eventOrValue.target.value
        : eventOrValue;
    setForm((current) => ({ ...current, [field]: value }));
    setDirty(true);
  };
  const setReferral = (field) => (event) => {
    const value = event.target.value;
    setForm((current) => ({
      ...current,
      referral: { ...current.referral, [field]: value },
    }));
    setDirty(true);
  };
  const setSite = (field) => (event) => {
    const value = event.target.value;
    setForm((current) => ({
      ...current,
      site: { ...current.site, [field]: value },
    }));
    setDirty(true);
  };
  const setIdentifier = (index, field, value) => {
    setForm((current) => ({
      ...current,
      identifiers: current.identifiers.map((identifier, i) =>
        i === index
          ? { ...identifier, [field]: value }
          : field === "reporting" && value
            ? { ...identifier, reporting: false }
            : identifier,
      ),
    }));
    setDirty(true);
  };

  const facilityTypes = lists ? lists.facilityTypes : [];
  const typeItems = facilityTypes.map((type) => ({
    id: type.id,
    text: type.name,
  }));
  const isReferral =
    kind === "facility" &&
    (form ? form.typeIds : []).some((typeId) =>
      isReferralType(facilityTypes.find((type) => type.id === typeId)),
    );
  const labelItems = useMemo(() => {
    const labels = new Set([
      "Code",
      "CLIA",
      ...((lists && lists.identifierLabels) || []),
    ]);
    return [...labels];
  }, [lists]);

  if (!form) {
    return (
      <div className="locationsForm">
        <FormattedMessage id="label.loading" />
      </div>
    );
  }

  const localErrors = {
    name: !form.name.trim()
      ? intl.formatMessage({ id: "error.locations.name.required" })
      : null,
    types:
      kind === "facility" && form.typeIds.length === 0
        ? intl.formatMessage({ id: "error.locations.types.required" })
        : null,
    gpsLatitude: badLatitude(form.gpsLatitude)
      ? intl.formatMessage({ id: "error.locations.gps.range" })
      : null,
    gpsLongitude: badLongitude(form.gpsLongitude)
      ? intl.formatMessage({ id: "error.locations.gps.range" })
      : null,
    referral:
      isReferral && !form.referral.approvalStatus
        ? intl.formatMessage({ id: "error.locations.referral.status" })
        : null,
  };
  const errorFor = (field) =>
    (tried && localErrors[field]) || fieldErrors[field] || null;
  const overdue =
    form.referral.nextReviewDue &&
    form.referral.nextReviewDue < toLocalIsoDate(new Date());

  const focusField = (key) => {
    const prefix = FIELD_ELEMENT[key] || key;
    const suffix = prefix === "identifier-value" ? "-0" : "";
    const element =
      document.getElementById(`${prefix}-${id || "new"}${suffix}`) ||
      document.getElementById(`${prefix}-${id || "new"}-input`);
    if (element) {
      element.scrollIntoView({ block: "center" });
      element.focus();
    }
  };

  const source = `record-${id || "new"}`;

  const fieldName = (key) =>
    FIELD_LABEL[key]
      ? intl.formatMessage({ id: FIELD_LABEL[key] })
      : intl.formatMessage({ id: "label.locations.section.identity" });

  const save = () => {
    setTried(true);
    const invalid = Object.keys(localErrors).filter((key) => localErrors[key]);
    if (invalid.length > 0) {
      notify(
        intl.formatMessage(
          { id: "error.locations.save.check" },
          { fields: invalid.map(fieldName).join(", ") },
        ),
        "error",
        null,
        source,
      );
      focusField(invalid[0]);
      return;
    }
    if (wardDrafts > 0) {
      notify(
        intl.formatMessage(
          { id: "warning.locations.wards.unsaved" },
          { count: wardDrafts },
        ),
        "warning",
        null,
        source,
      );
      const drafts = container.current
        ? [
            ...container.current.querySelectorAll(
              '[data-testid="locations-ward-draft"] input, [data-testid="locations-ward-draft"] select',
            ),
          ]
        : [];
      const wardsSave =
        container.current &&
        container.current.querySelector('[data-testid="locations-save-wards"]');
      const target =
        wardsSave && !wardsSave.disabled
          ? wardsSave
          : drafts.find(
              (field) =>
                /^new-ward-(name|service)-/.test(field.id) && !field.value,
            ) || drafts[0];
      if (target) {
        target.scrollIntoView({ block: "center" });
        target.focus();
      }
      return;
    }
    setSaving(true);
    setFieldErrors({});
    setConflict(null);
    const request = toRequest(form, kind);
    const call = isNew
      ? createOrganization(request)
      : updateOrganization(id, request);
    call
      .then((result) => {
        setSaving(false);
        setDirty(false);
        onSaved(result.detail);
        (result.warnings || []).forEach((warning) =>
          notify(warning, "warning", null, source),
        );
      })
      .catch((error) => {
        setSaving(false);
        if (error.status === 409 && error.current) {
          const { merged, changedByThem } = mergeAfterConflict(
            form,
            fromDetail(detail),
            fromDetail(error.current),
          );
          setDetail(error.current);
          setForm(merged);
          setConflict(changedByThem);
          return;
        }
        if (error.status === 422) {
          const errors = error.fieldErrors || {};
          setFieldErrors(errors);
          const first = Object.keys(errors)[0];
          notify(
            first
              ? intl.formatMessage(
                  { id: "error.locations.save.failedBecause" },
                  { reason: errors[first] },
                )
              : intl.formatMessage({ id: "error.locations.save.failed" }),
            "error",
            null,
            source,
          );
          if (first) focusField(first);
          return;
        }
        notify(error.message, "error", null, source);
      });
  };

  const cancel = () => {
    if (
      dirty &&
      !window.confirm(intl.formatMessage({ id: "message.locations.unsaved" }))
    ) {
      return;
    }
    onCancel();
  };

  const sections = [
    ["identity", "label.locations.section.identity"],
    ["identifiers", "label.locations.section.identifiers"],
    ["location", "label.locations.section.location"],
    ["contact", "label.locations.section.contact"],
    ...(isReferral ? [["referral", "label.locations.section.referral"]] : []),
    ...(kind === "site" ? [["site", "label.locations.section.site"]] : []),
    ...(kind === "facility" && !isNew
      ? [["wards", "label.locations.section.wards"]]
      : []),
  ];
  const sectionId = (section) => `locations-section-${id || "new"}-${section}`;
  const fromRegistry = form.registry
    ? ` · ${intl.formatMessage({ id: "label.locations.registry.field" })}`
    : "";

  return (
    <div
      className="locationsForm"
      data-testid={`locations-form-${id || "new"}`}
      ref={container}
    >
      {form.registry && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title=""
          subtitle={intl.formatMessage({
            id: "warning.locations.registry.edit",
          })}
        />
      )}
      {conflict && (
        <InlineNotification
          kind="warning"
          lowContrast
          title=""
          subtitle={intl.formatMessage(
            { id: "error.locations.save.conflictKept" },
            {
              fields:
                conflict.length > 0
                  ? conflict.map(fieldName).join(", ")
                  : intl.formatMessage({ id: "label.locations.none" }),
            },
          )}
          onClose={() => setConflict(null)}
        />
      )}
      {sections.length > 3 && (
        <nav
          aria-label={intl.formatMessage({ id: "label.locations.form.jump" })}
          className="cds--label"
        >
          <FormattedMessage id="label.locations.form.jump" />:{" "}
          {sections.map(([key, labelId], index) => (
            <span key={key}>
              {index > 0 && " · "}
              <Link href={`#${sectionId(key)}`} size="sm">
                <FormattedMessage id={labelId} />
              </Link>
            </span>
          ))}
        </nav>
      )}

      <FormGroup
        legendText={intl.formatMessage({
          id: "label.locations.section.identity",
        })}
        id={sectionId("identity")}
      >
        <Grid condensed>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id={`name-${id || "new"}`}
              labelText={`${intl.formatMessage({ id: "label.locations.column.name" })} *${fromRegistry}`}
              value={form.name}
              maxLength={200}
              onChange={set("name")}
              invalid={!!errorFor("name")}
              invalidText={errorFor("name")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`shortName-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.shortName",
              })}
              value={form.shortName}
              maxLength={15}
              invalid={!!errorFor("shortName")}
              invalidText={errorFor("shortName")}
              onChange={set("shortName")}
            />
          </Column>
          {kind === "facility" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <FilterableMultiSelect
                  id={`types-${id || "new"}`}
                  titleText={`${intl.formatMessage({ id: "label.locations.field.types" })} *`}
                  items={typeItems}
                  itemToString={(item) => (item ? item.text : "")}
                  initialSelectedItems={typeItems.filter((item) =>
                    form.typeIds.includes(item.id),
                  )}
                  invalid={!!errorFor("types")}
                  invalidText={errorFor("types")}
                  helperText={
                    form.typeIds.length > 0 ? (
                      <span
                        data-testid={`locations-types-chosen-${id || "new"}`}
                      >
                        {typeItems
                          .filter((item) => form.typeIds.includes(item.id))
                          .map((item) => (
                            <Tag key={item.id} type="blue" size="sm">
                              {item.text}
                            </Tag>
                          ))}
                      </span>
                    ) : undefined
                  }
                  onChange={({ selectedItems }) => {
                    setForm((current) => ({
                      ...current,
                      typeIds: selectedItems.map((item) => item.id),
                    }));
                    setDirty(true);
                  }}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id={`category-${id || "new"}`}
                  labelText={intl.formatMessage({
                    id: "label.locations.field.category",
                  })}
                  value={form.categoryId}
                  onChange={set("categoryId")}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({ id: "label.select" })}
                  />
                  {(lists ? lists.categories : []).map((entry) => (
                    <SelectItem
                      key={entry.id}
                      value={entry.id}
                      text={entry.label}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id={`ownership-${id || "new"}`}
                  labelText={intl.formatMessage({
                    id: "label.locations.field.ownership",
                  })}
                  value={form.ownershipId}
                  onChange={set("ownershipId")}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({ id: "label.select" })}
                  />
                  {(lists ? lists.ownerships : []).map((entry) => (
                    <SelectItem
                      key={entry.id}
                      value={entry.id}
                      text={entry.label}
                    />
                  ))}
                </Select>
              </Column>
            </>
          )}
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id={`description-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.description",
              })}
              value={form.description}
              maxLength={1000}
              invalid={!!errorFor("description")}
              invalidText={errorFor("description")}
              onChange={set("description")}
              placeholder={
                kind === "site"
                  ? intl.formatMessage({
                      id: "help.locations.field.siteDescription",
                    })
                  : ""
              }
            />
          </Column>
        </Grid>
      </FormGroup>

      <FormGroup
        legendText={intl.formatMessage({
          id: "label.locations.section.identifiers",
        })}
        id={sectionId("identifiers")}
      >
        <Table
          size="sm"
          aria-label={intl.formatMessage({
            id: "label.locations.section.identifiers",
          })}
          className="locationsIdentifiers"
        >
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="label.locations.identifier.label" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.locations.identifier.value" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.locations.identifier.reporting" />
              </TableHeader>
              <TableHeader />
            </TableRow>
          </TableHead>
          <TableBody>
            {form.identifiers.map((identifier, index) => (
              <TableRow key={index}>
                <TableCell>
                  <ComboBox
                    id={`identifier-label-${id || "new"}-${index}`}
                    titleText=""
                    aria-label={intl.formatMessage({
                      id: "label.locations.identifier.label",
                    })}
                    allowCustomValue
                    items={labelItems}
                    selectedItem={identifier.label || null}
                    placeholder={intl.formatMessage({
                      id: "label.locations.identifier.labelPlaceholder",
                    })}
                    onChange={({ selectedItem, inputValue }) =>
                      setIdentifier(
                        index,
                        "label",
                        selectedItem || inputValue || "",
                      )
                    }
                    onInputChange={(text) =>
                      setIdentifier(index, "label", text || "")
                    }
                  />
                </TableCell>
                <TableCell>
                  <TextInput
                    id={`identifier-value-${id || "new"}-${index}`}
                    labelText=""
                    hideLabel
                    aria-label={`${identifier.label} ${intl.formatMessage({ id: "label.locations.identifier.value" })}`}
                    value={identifier.value}
                    maxLength={100}
                    onChange={(e) =>
                      setIdentifier(index, "value", e.target.value)
                    }
                  />
                </TableCell>
                <TableCell>
                  <RadioButton
                    id={`identifier-reporting-${id || "new"}-${index}`}
                    name={`identifier-reporting-${id || "new"}`}
                    labelText=""
                    hideLabel
                    checked={!!identifier.reporting}
                    onChange={() => setIdentifier(index, "reporting", true)}
                  />
                </TableCell>
                <TableCell>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => {
                      setForm((current) => ({
                        ...current,
                        identifiers: current.identifiers.filter(
                          (_, i) => i !== index,
                        ),
                      }));
                      setDirty(true);
                    }}
                  >
                    <FormattedMessage id="button.remove" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {errorFor("identifiers") && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title=""
            subtitle={errorFor("identifiers")}
          />
        )}
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Add}
          onClick={() => {
            setForm((current) => ({
              ...current,
              identifiers: [
                ...current.identifiers,
                { label: "", value: "", reporting: false },
              ],
            }));
            setDirty(true);
          }}
        >
          <FormattedMessage id="button.locations.identifier.add" />
        </Button>
        <p className="cds--form__helper-text">
          <FormattedMessage id="help.locations.identifiers" />
        </p>
      </FormGroup>

      <FormGroup
        legendText={intl.formatMessage({
          id: "label.locations.section.location",
        })}
        id={sectionId("location")}
      >
        <Grid condensed>
          <Column lg={8} md={4} sm={4}>
            <LocationComboBox
              id={`location-${id || "new"}`}
              titleText={intl.formatMessage({
                id: "label.locations.field.location",
              })}
              placeholder={intl.formatMessage({
                id: "label.locations.filter.location.any",
              })}
              helperText={`${form.location ? `${pathText(form.location)}. ` : ""}${intl.formatMessage({ id: "help.locations.field.location" })}`}
              invalid={!!errorFor("parent")}
              invalidText={errorFor("parent")}
              selected={form.location}
              onChange={(item) => {
                setForm((current) => ({ ...current, location: item }));
                setDirty(true);
              }}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id={`street-${id || "new"}`}
              labelText={`${intl.formatMessage({ id: "label.locations.field.streetAddress" })}${fromRegistry}`}
              value={form.streetAddress}
              maxLength={30}
              invalid={!!errorFor("streetAddress")}
              invalidText={errorFor("streetAddress")}
              onChange={set("streetAddress")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`city-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.city",
              })}
              value={form.city}
              maxLength={30}
              invalid={!!errorFor("city")}
              invalidText={errorFor("city")}
              onChange={set("city")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`state-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.state",
              })}
              value={form.state}
              maxLength={100}
              invalid={!!errorFor("state")}
              invalidText={errorFor("state")}
              onChange={set("state")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`zip-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.zipCode",
              })}
              value={form.zipCode}
              maxLength={10}
              invalid={!!errorFor("zipCode")}
              invalidText={errorFor("zipCode")}
              onChange={set("zipCode")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`lat-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.gpsLatitude",
              })}
              value={form.gpsLatitude}
              placeholder="-9.4705"
              helperText={intl.formatMessage({
                id: "help.locations.field.gps",
              })}
              invalid={!!errorFor("gpsLatitude")}
              invalidText={errorFor("gpsLatitude")}
              onChange={set("gpsLatitude")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`lng-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.gpsLongitude",
              })}
              value={form.gpsLongitude}
              placeholder="147.1597"
              invalid={!!errorFor("gpsLongitude")}
              invalidText={errorFor("gpsLongitude")}
              onChange={set("gpsLongitude")}
            />
          </Column>
        </Grid>
      </FormGroup>

      <FormGroup
        legendText={intl.formatMessage({
          id: "label.locations.section.contact",
        })}
        id={sectionId("contact")}
      >
        <Grid condensed>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`contact-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.contactName",
              })}
              value={form.contactName}
              maxLength={100}
              invalid={!!errorFor("contactName")}
              invalidText={errorFor("contactName")}
              onChange={set("contactName")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`phone-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.phone",
              })}
              value={form.phone}
              maxLength={20}
              invalid={!!errorFor("phone")}
              invalidText={errorFor("phone")}
              onChange={set("phone")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`email-${id || "new"}`}
              type="email"
              labelText={intl.formatMessage({
                id: "label.locations.field.email",
              })}
              value={form.email}
              maxLength={255}
              invalid={!!errorFor("email")}
              invalidText={errorFor("email")}
              onChange={set("email")}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id={`web-${id || "new"}`}
              labelText={intl.formatMessage({
                id: "label.locations.field.website",
              })}
              value={form.internetAddress}
              maxLength={40}
              invalid={!!errorFor("internetAddress")}
              invalidText={errorFor("internetAddress")}
              onChange={set("internetAddress")}
            />
          </Column>
        </Grid>
      </FormGroup>

      {isReferral && (
        <FormGroup
          legendText={`${intl.formatMessage({ id: "label.locations.section.referral" })} · ISO 15189 6.8`}
          id={sectionId("referral")}
        >
          {overdue && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title=""
              subtitle={intl.formatMessage(
                { id: "message.locations.referral.overdueSince" },
                { date: form.referral.nextReviewDue },
              )}
            />
          )}
          <Grid condensed>
            <Column lg={4} md={4} sm={4}>
              <Select
                id={`ref-status-${id || "new"}`}
                labelText={`${intl.formatMessage({ id: "label.locations.referral.approval" })} *`}
                value={form.referral.approvalStatus || ""}
                onChange={setReferral("approvalStatus")}
                invalid={
                  !!errorFor("referral") ||
                  !!errorFor("referral.approvalStatus")
                }
                invalidText={
                  errorFor("referral") || errorFor("referral.approvalStatus")
                }
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "label.select" })}
                />
                {(lists ? lists.referralStatuses : []).map((status) => (
                  <SelectItem
                    key={status.id}
                    value={status.id}
                    text={status.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`ref-body-${id || "new"}`}
                labelText={intl.formatMessage({
                  id: "label.locations.referral.accreditationBody",
                })}
                value={form.referral.accreditationBody || ""}
                maxLength={100}
                invalid={!!errorFor("referral.accreditationBody")}
                invalidText={errorFor("referral.accreditationBody")}
                onChange={setReferral("accreditationBody")}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`ref-number-${id || "new"}`}
                labelText={intl.formatMessage({
                  id: "label.locations.referral.accreditationNumber",
                })}
                value={form.referral.accreditationNumber || ""}
                maxLength={50}
                invalid={!!errorFor("referral.accreditationNumber")}
                invalidText={errorFor("referral.accreditationNumber")}
                onChange={setReferral("accreditationNumber")}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`ref-expiry-${id || "new"}`}
                type="date"
                labelText={intl.formatMessage({
                  id: "label.locations.referral.accreditationExpiry",
                })}
                value={form.referral.accreditationExpiry || ""}
                invalid={!!errorFor("referral.accreditationExpiry")}
                invalidText={errorFor("referral.accreditationExpiry")}
                onChange={setReferral("accreditationExpiry")}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`ref-last-${id || "new"}`}
                type="date"
                labelText={intl.formatMessage({
                  id: "label.locations.referral.lastReview",
                })}
                value={form.referral.lastReviewDate || ""}
                invalid={!!errorFor("referral.lastReviewDate")}
                invalidText={errorFor("referral.lastReviewDate")}
                onChange={setReferral("lastReviewDate")}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`ref-next-${id || "new"}`}
                type="date"
                labelText={intl.formatMessage({
                  id: "label.locations.referral.nextReview",
                })}
                value={form.referral.nextReviewDue || ""}
                warn={!!overdue}
                warnText={intl.formatMessage({
                  id: "label.locations.referral.overdue",
                })}
                invalid={!!errorFor("referral.nextReviewDue")}
                invalidText={errorFor("referral.nextReviewDue")}
                onChange={setReferral("nextReviewDue")}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id={`ref-notes-${id || "new"}`}
                labelText={intl.formatMessage({
                  id: "label.locations.referral.notes",
                })}
                value={form.referral.reviewNotes || ""}
                maxLength={1000}
                invalid={!!errorFor("referral.reviewNotes")}
                invalidText={errorFor("referral.reviewNotes")}
                onChange={setReferral("reviewNotes")}
              />
            </Column>
          </Grid>
        </FormGroup>
      )}

      {kind === "site" && (
        <FormGroup
          legendText={intl.formatMessage({
            id: "label.locations.section.site",
          })}
          id={sectionId("site")}
        >
          <Grid condensed>
            <Column lg={4} md={4} sm={4}>
              <Select
                id={`site-type-${id || "new"}`}
                labelText={intl.formatMessage({
                  id: "label.locations.field.siteType",
                })}
                value={form.site.siteType}
                onChange={setSite("siteType")}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "label.select" })}
                />
                {(lists ? lists.siteTypes : []).map((entry) => (
                  <SelectItem
                    key={entry.id}
                    value={entry.id}
                    text={entry.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`site-subtype-${id || "new"}`}
                labelText={intl.formatMessage({
                  id: "label.locations.field.subtype",
                })}
                value={form.site.subtype}
                onChange={setSite("subtype")}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id={`site-zone-${id || "new"}`}
                labelText={intl.formatMessage({
                  id: "label.locations.field.zone",
                })}
                value={form.site.environmentalZone}
                onChange={setSite("environmentalZone")}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "label.select" })}
                />
                {(lists ? lists.environmentalZones : []).map((entry) => (
                  <SelectItem
                    key={entry.id}
                    value={entry.id}
                    text={entry.label}
                  />
                ))}
              </Select>
            </Column>
          </Grid>
        </FormGroup>
      )}

      {kind === "facility" && !isNew && detail && (
        <WardsSection
          organization={detail}
          lists={lists}
          highlight={highlight}
          statusFilter={statusFilter}
          sectionId={sectionId("wards")}
          onActiveChange={onActiveChange}
          onChanged={reload}
          onDraftsChange={setWardDrafts}
        />
      )}

      {showHistory && !isNew && detail && (
        <HistoryPanel id={id} name={detail.row.name} />
      )}

      <div className="locationsStickyBar">
        <Button
          size="sm"
          onClick={save}
          disabled={saving}
          data-testid="locations-save"
        >
          <FormattedMessage id="button.save" />
        </Button>
        <Button kind="ghost" size="sm" onClick={cancel}>
          <FormattedMessage id="button.cancel" />
        </Button>
        {!isNew && detail && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={RecentlyViewed}
            aria-expanded={showHistory}
            onClick={() => setShowHistory(!showHistory)}
          >
            {showHistory ? (
              <FormattedMessage id="button.locations.history.hide" />
            ) : (
              <>
                <FormattedMessage id="button.locations.history" /> (
                {detail.historyCount})
              </>
            )}
          </Button>
        )}
      </div>
    </div>
  );
};

export default RecordForm;
