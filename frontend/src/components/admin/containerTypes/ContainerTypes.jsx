import React, { useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  FilterableMultiSelect,
  Grid,
  Heading,
  InlineLoading,
  InlineNotification,
  Modal,
  Search,
  Section,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import "./ContainerTypes.css";

const API = "/rest/container-types";
const DOMAINS = ["CLINICAL", "ENVIRONMENTAL", "VECTOR"];
const COLUMNS = 11;

const EMPTY_FORM = {
  code: "",
  name: "",
  category: "",
  additive: "",
  material: "",
  nominalVolume: "",
  volumeUnit: "mL",
  capColourHex: "",
  capColourName: "",
  standardCode: "",
  domains: ["CLINICAL"],
  sampleTypeIds: [],
};

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
];

const toForm = (type) => ({
  code: type.code || "",
  name: type.name || "",
  category: type.category || "",
  additive: type.additive || "",
  material: type.material || "",
  nominalVolume:
    type.nominalVolume === null || type.nominalVolume === undefined
      ? ""
      : String(type.nominalVolume),
  volumeUnit: type.volumeUnit || "mL",
  capColourHex: type.capColourHex || "",
  capColourName: type.capColourName || "",
  standardCode: type.standardCode || "",
  domains: type.domains || [],
  sampleTypeIds: (type.sampleTypes || []).map((sampleType) => sampleType.id),
});

const toPayload = (form) => ({
  ...form,
  nominalVolume: form.nominalVolume === "" ? null : Number(form.nominalVolume),
  capColourHex: form.capColourHex || null,
});

/**
 * OGC-1611 FR-G1 to G3: the containers samples are taken in, beside Sample
 * Types. A container type is added and edited in its row, and deactivated,
 * never deleted, after a warning that names what still uses it.
 */
export default function ContainerTypes() {
  const intl = useIntl();
  const [types, setTypes] = useState(null);
  const [search, setSearch] = useState("");
  const [domain, setDomain] = useState("");
  const [category, setCategory] = useState("");
  const [showInactive, setShowInactive] = useState(false);
  const [categories, setCategories] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [refusal, setRefusal] = useState(null);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);
  const [deactivating, setDeactivating] = useState(null);

  const [reloads, setReloads] = useState(0);
  const [loadFailed, setLoadFailed] = useState(false);
  const load = () => setReloads((count) => count + 1);

  useEffect(() => {
    const controller = new AbortController();
    const params = new URLSearchParams({
      includeInactive: String(showInactive),
    });
    if (search.trim()) params.set("search", search.trim());
    if (domain) params.set("domain", domain);
    if (category) params.set("category", category);
    getFromOpenElisServer(
      `${API}?${params}`,
      (response) => {
        if (controller.signal.aborted) {
          return;
        }
        setLoadFailed(!Array.isArray(response));
        setTypes(Array.isArray(response) ? response : []);
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [search, domain, category, showInactive, reloads]);

  useEffect(() => {
    getFromOpenElisServer(`${API}?includeInactive=true`, (response) =>
      setCategories(
        [
          ...new Set(
            (Array.isArray(response) ? response : [])
              .map((type) => type.category)
              .filter(Boolean),
          ),
        ].sort((a, b) => a.localeCompare(b)),
      ),
    );
    getFromOpenElisServer("/rest/test-catalog/sample-types", (response) =>
      setSampleTypes(Array.isArray(response) ? response : []),
    );
  }, []);

  const startEdit = (type) => {
    setEditing(type ? type.id : "new");
    setForm(type ? toForm(type) : EMPTY_FORM);
    setRefusal(null);
  };

  const closeEdit = () => {
    setEditing(null);
    setRefusal(null);
  };

  const domainName = (key) =>
    intl.formatMessage({ id: `admin.containerTypes.domain.${key}` });

  const save = () => {
    setSaving(true);
    setRefusal(null);
    const send =
      editing === "new"
        ? postToOpenElisServerFullResponse
        : putToOpenElisServerFullResponse;
    const endPoint = editing === "new" ? API : `${API}/${editing}`;
    send(endPoint, JSON.stringify(toPayload(form)), async (response) => {
      setSaving(false);
      if (response && response.ok) {
        const saved = await response.json();
        setNotice({
          kind: "success",
          text: intl.formatMessage(
            { id: "admin.containerTypes.saved" },
            { name: saved.name },
          ),
        });
        closeEdit();
        load();
        return;
      }
      let body;
      try {
        body = (await response?.json()) || {};
      } catch {
        body = {};
      }
      setRefusal({ field: body.field, code: body.code || "saveFailed" });
    });
  };

  const askDeactivate = (type) => {
    getFromOpenElisServer(`${API}/${type.id}/usage`, (usage) =>
      setDeactivating({
        type,
        usage: usage && Array.isArray(usage.tests) ? usage : null,
      }),
    );
  };

  const setActive = (type, active) => {
    postToOpenElisServerFullResponse(
      `${API}/${type.id}/${active ? "activate" : "deactivate"}`,
      "{}",
      (response) => {
        setDeactivating(null);
        setNotice(
          response && response.ok
            ? {
                kind: "success",
                text: intl.formatMessage(
                  {
                    id: active
                      ? "admin.containerTypes.activated"
                      : "admin.containerTypes.deactivated",
                  },
                  { name: type.name },
                ),
              }
            : {
                kind: "error",
                text: intl.formatMessage({
                  id: "admin.containerTypes.error.saveFailed",
                }),
              },
        );
        load();
      },
    );
  };

  const fieldError = (field) =>
    refusal && refusal.field === field
      ? intl.formatMessage({
          id: `admin.containerTypes.error.${refusal.code}`,
        })
      : undefined;

  const editRow = (key) => (
    <TableRow key={`edit-${key}`} className="containerTypeEditRow">
      <TableCell colSpan={COLUMNS}>
        <div
          className="containerTypeForm"
          data-testid={`container-type-form-${key}`}
        >
          <TextInput
            id="container-type-name"
            maxLength={120}
            labelText={`${intl.formatMessage({
              id: "admin.containerTypes.field.name",
            })} *`}
            value={form.name}
            invalid={!!fieldError("name")}
            invalidText={fieldError("name")}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
          <TextInput
            id="container-type-code"
            maxLength={40}
            labelText={`${intl.formatMessage({
              id: "admin.containerTypes.field.code",
            })} *`}
            helperText={intl.formatMessage({
              id: "admin.containerTypes.field.codeHelp",
            })}
            value={form.code}
            invalid={!!fieldError("code")}
            invalidText={fieldError("code")}
            onChange={(e) =>
              setForm({ ...form, code: e.target.value.toUpperCase() })
            }
          />
          <fieldset className="cds--fieldset containerTypeDomains">
            <legend className="cds--label">
              <FormattedMessage id="admin.containerTypes.field.domains" /> *
            </legend>
            {DOMAINS.map((key) => (
              <Checkbox
                key={key}
                id={`container-type-domain-${key}`}
                labelText={domainName(key)}
                checked={form.domains.includes(key)}
                onChange={(_, { checked }) =>
                  setForm({
                    ...form,
                    domains: checked
                      ? [...form.domains, key]
                      : form.domains.filter((value) => value !== key),
                  })
                }
              />
            ))}
            {fieldError("domains") && (
              <div className="cds--form-requirement containerTypeFieldError">
                {fieldError("domains")}
              </div>
            )}
          </fieldset>
          <TextInput
            id="container-type-category"
            maxLength={60}
            invalid={!!fieldError("category")}
            invalidText={fieldError("category")}
            labelText={intl.formatMessage({
              id: "admin.containerTypes.field.category",
            })}
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })}
          />
          <TextInput
            id="container-type-additive"
            maxLength={120}
            invalid={!!fieldError("additive")}
            invalidText={fieldError("additive")}
            labelText={intl.formatMessage({
              id: "admin.containerTypes.field.additive",
            })}
            value={form.additive}
            onChange={(e) => setForm({ ...form, additive: e.target.value })}
          />
          <TextInput
            id="container-type-material"
            maxLength={60}
            invalid={!!fieldError("material")}
            invalidText={fieldError("material")}
            labelText={intl.formatMessage({
              id: "admin.containerTypes.field.material",
            })}
            value={form.material}
            onChange={(e) => setForm({ ...form, material: e.target.value })}
          />
          <div className="containerTypeVolume">
            <TextInput
              id="container-type-volume"
              type="number"
              labelText={intl.formatMessage({
                id: "admin.containerTypes.field.nominalVolume",
              })}
              value={form.nominalVolume}
              invalid={!!fieldError("nominalVolume")}
              invalidText={fieldError("nominalVolume")}
              onChange={(e) =>
                setForm({ ...form, nominalVolume: e.target.value })
              }
            />
            <Select
              id="container-type-volume-unit"
              maxLength={10}
              invalid={!!fieldError("volumeUnit")}
              invalidText={fieldError("volumeUnit")}
              labelText={intl.formatMessage({
                id: "admin.containerTypes.field.volumeUnit",
              })}
              value={form.volumeUnit}
              onChange={(e) => setForm({ ...form, volumeUnit: e.target.value })}
            >
              {["mL", "µL", "L"].map((unit) => (
                <SelectItem key={unit} value={unit} text={unit} />
              ))}
            </Select>
          </div>
          <div className="containerTypeColour">
            <label className="cds--label" htmlFor="container-type-colour">
              <FormattedMessage id="admin.containerTypes.field.capColour" />
            </label>
            <input
              id="container-type-colour"
              type="color"
              value={form.capColourHex || "#FFFFFF"}
              onChange={(e) =>
                setForm({ ...form, capColourHex: e.target.value.toUpperCase() })
              }
            />
            <TextInput
              id="container-type-colour-name"
              maxLength={40}
              invalid={!!fieldError("capColourName")}
              invalidText={fieldError("capColourName")}
              labelText={intl.formatMessage({
                id: "admin.containerTypes.field.capColourName",
              })}
              value={form.capColourName}
              onChange={(e) =>
                setForm({ ...form, capColourName: e.target.value })
              }
            />
            {form.capColourHex && (
              <Button
                kind="ghost"
                size="sm"
                onClick={() =>
                  setForm({ ...form, capColourHex: "", capColourName: "" })
                }
              >
                <FormattedMessage id="admin.containerTypes.field.noCap" />
              </Button>
            )}
            {fieldError("capColourHex") && (
              <div className="cds--form-requirement containerTypeFieldError">
                {fieldError("capColourHex")}
              </div>
            )}
          </div>
          <FilterableMultiSelect
            id="container-type-yields"
            titleText={intl.formatMessage({
              id: "admin.containerTypes.field.yields",
            })}
            items={sampleTypes}
            itemToString={(item) => (item ? item.name : "")}
            initialSelectedItems={sampleTypes.filter((sampleType) =>
              form.sampleTypeIds.includes(sampleType.id),
            )}
            invalid={!!fieldError("sampleTypeIds")}
            invalidText={fieldError("sampleTypeIds")}
            onChange={({ selectedItems }) =>
              setForm({
                ...form,
                sampleTypeIds: selectedItems.map((item) => item.id),
              })
            }
          />
          <TextInput
            id="container-type-standard-code"
            maxLength={60}
            invalid={!!fieldError("standardCode")}
            invalidText={fieldError("standardCode")}
            labelText={intl.formatMessage({
              id: "admin.containerTypes.field.standardCode",
            })}
            value={form.standardCode}
            onChange={(e) => setForm({ ...form, standardCode: e.target.value })}
          />
          {refusal && !refusal.field && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: `admin.containerTypes.error.${refusal.code}`,
              })}
            />
          )}
          <div className="containerTypeFormActions">
            <Button
              size="sm"
              disabled={saving}
              onClick={save}
              data-testid="container-type-save"
            >
              <FormattedMessage id="label.button.save" />
            </Button>
            <Button kind="secondary" size="sm" onClick={closeEdit}>
              <FormattedMessage id="label.button.cancel" />
            </Button>
          </div>
        </div>
      </TableCell>
    </TableRow>
  );

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth className="adminPageContent containerTypesPage">
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="admin.containerTypes.title" />
            </Heading>
            <p className="containerTypesIntro">
              <FormattedMessage id="admin.containerTypes.intro" />
            </p>
          </Section>
        </Column>
        {notice && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind={notice.kind}
              lowContrast
              title={notice.text}
              onClose={() => {
                setNotice(null);
                return true;
              }}
            />
          </Column>
        )}
        <Column lg={16} md={8} sm={4}>
          <div className="containerTypesFilters">
            <Search
              id="container-types-search"
              size="md"
              labelText={intl.formatMessage({
                id: "admin.containerTypes.search",
              })}
              placeholder={intl.formatMessage({
                id: "admin.containerTypes.search",
              })}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Select
              id="container-types-domain"
              labelText={intl.formatMessage({
                id: "admin.containerTypes.field.domains",
              })}
              value={domain}
              onChange={(e) => setDomain(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "admin.containerTypes.all" })}
              />
              {DOMAINS.map((key) => (
                <SelectItem key={key} value={key} text={domainName(key)} />
              ))}
            </Select>
            <Select
              id="container-types-category"
              labelText={intl.formatMessage({
                id: "admin.containerTypes.field.category",
              })}
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "admin.containerTypes.all" })}
              />
              {categories.map((name) => (
                <SelectItem key={name} value={name} text={name} />
              ))}
            </Select>
            <Toggle
              id="container-types-show-inactive"
              size="sm"
              labelText={intl.formatMessage({
                id: "admin.containerTypes.showDeactivated",
              })}
              hideLabel
              labelA={intl.formatMessage({
                id: "admin.containerTypes.showDeactivated",
              })}
              labelB={intl.formatMessage({
                id: "admin.containerTypes.showDeactivated",
              })}
              toggled={showInactive}
              onToggle={setShowInactive}
            />
            <Button
              size="md"
              onClick={() => startEdit(null)}
              disabled={editing !== null}
              data-testid="container-type-add"
            >
              <FormattedMessage id="admin.containerTypes.add" />
            </Button>
          </div>
        </Column>
        <Column lg={16} md={8} sm={4}>
          {types === null ? (
            <InlineLoading />
          ) : (
            <TableContainer>
              <Table size="md" data-testid="container-types">
                <TableHead>
                  <TableRow>
                    {[
                      "name",
                      "domains",
                      "additive",
                      "material",
                      "nominalVolume",
                      "capColour",
                      "yields",
                      "category",
                      "usedBy",
                      "status",
                    ].map((key) => (
                      <TableHeader key={key}>
                        <FormattedMessage
                          id={`admin.containerTypes.column.${key}`}
                        />
                      </TableHeader>
                    ))}
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {editing === "new" && editRow("new")}
                  {types.map((type) => (
                    <React.Fragment key={type.id}>
                      <TableRow data-testid={`container-type-${type.code}`}>
                        <TableCell>{type.name}</TableCell>
                        <TableCell>
                          {type.domains.map((key) => (
                            <Tag key={key} size="sm" type="cool-gray">
                              {domainName(key)}
                            </Tag>
                          ))}
                        </TableCell>
                        <TableCell>{type.additive || "—"}</TableCell>
                        <TableCell>{type.material || "—"}</TableCell>
                        <TableCell>
                          {type.nominalVolume
                            ? `${type.nominalVolume} ${type.volumeUnit || ""}`
                            : "—"}
                        </TableCell>
                        <TableCell>
                          {type.capColourHex ? (
                            <span className="containerTypeCap">
                              <span
                                className="containerTypeSwatch"
                                style={{ backgroundColor: type.capColourHex }}
                                aria-hidden="true"
                              />
                              {type.capColourName || type.capColourHex}
                            </span>
                          ) : (
                            <FormattedMessage id="admin.containerTypes.noCap" />
                          )}
                        </TableCell>
                        <TableCell>
                          {type.sampleTypes.map((sampleType) => (
                            <Tag key={sampleType.id} size="sm" type="blue">
                              {sampleType.value}
                            </Tag>
                          ))}
                        </TableCell>
                        <TableCell>{type.category || "—"}</TableCell>
                        <TableCell>
                          <FormattedMessage
                            id="admin.containerTypes.samplesUsing"
                            values={{ count: type.samplesUsing }}
                          />
                        </TableCell>
                        <TableCell>
                          <Tag size="sm" type={type.active ? "green" : "gray"}>
                            <FormattedMessage
                              id={
                                type.active
                                  ? "admin.containerTypes.active"
                                  : "admin.containerTypes.deactivatedStatus"
                              }
                            />
                          </Tag>
                        </TableCell>
                        <TableCell className="containerTypeActions">
                          <Button
                            kind="ghost"
                            size="sm"
                            disabled={editing !== null}
                            onClick={() => startEdit(type)}
                          >
                            <FormattedMessage id="label.button.edit" />
                          </Button>
                          {type.active ? (
                            <Button
                              kind="ghost"
                              size="sm"
                              onClick={() => askDeactivate(type)}
                            >
                              <FormattedMessage id="admin.containerTypes.deactivate" />
                            </Button>
                          ) : (
                            <Button
                              kind="ghost"
                              size="sm"
                              onClick={() => setActive(type, true)}
                            >
                              <FormattedMessage id="admin.containerTypes.activate" />
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                      {editing === type.id && editRow(type.id)}
                    </React.Fragment>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {loadFailed ? (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              data-testid="container-types-load-failed"
              title={intl.formatMessage({
                id: "admin.containerTypes.loadFailed",
              })}
            />
          ) : (
            types &&
            types.length === 0 && (
              <p className="containerTypesEmpty">
                <FormattedMessage id="admin.containerTypes.empty" />
              </p>
            )
          )}
        </Column>
      </Grid>
      {deactivating && (
        <Modal
          open
          danger
          modalHeading={intl.formatMessage(
            { id: "admin.containerTypes.deactivateHeading" },
            { name: deactivating.type.name },
          )}
          primaryButtonText={intl.formatMessage({
            id: "admin.containerTypes.deactivate",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestClose={() => setDeactivating(null)}
          onRequestSubmit={() => setActive(deactivating.type, false)}
        >
          <p data-testid="container-type-usage">
            {deactivating.usage ? (
              <FormattedMessage
                id="admin.containerTypes.deactivateWarning"
                values={{
                  tests: deactivating.usage.tests.length,
                  testNames: deactivating.usage.tests.length
                    ? ` (${deactivating.usage.tests.slice(0, 5).join(", ")})`
                    : "",
                  samples: deactivating.usage.samples,
                }}
              />
            ) : (
              <FormattedMessage id="admin.containerTypes.deactivateUsageUnknown" />
            )}
          </p>
        </Modal>
      )}
    </>
  );
}
