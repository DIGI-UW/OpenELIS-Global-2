import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  TextInput,
  Tag,
  Select,
  SelectItem,
  InlineLoading,
  Search,
} from "@carbon/react";
import { Add, ChevronDown, ChevronUp } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../utils/Utils";

const SPECIES_URL = "/rest/admin/vector/species";
const GROUPS_URL = "/rest/admin/vector/sample-types";
const PATHOGEN_CATS_URL = "/rest/vector/dictionary/pathogen-categories";
const LIFECYCLE_CATS_URL = "/rest/vector/dictionary/lifecycle-categories";

const emptyForm = {
  genus: "",
  species: "",
  subspecies: "",
  sampleTypeId: "",
  pathogenCategoryId: "",
  lifecycleCategoryId: "",
  active: true,
};

function SpeciesForm({
  initial = emptyForm,
  groups,
  pathogenCategories,
  lifecycleCategories,
  onSave,
  onCancel,
  isNew,
}) {
  const intl = useIntl();
  const [form, setForm] = useState(initial);
  useEffect(() => setForm(initial), [initial]);
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  return (
    <div
      style={{
        background: "#f4f4f4",
        border: "1px solid #e0e0e0",
        borderRadius: "4px",
        padding: "1rem",
        marginBottom: "0.5rem",
      }}
    >
      {isNew && (
        <p style={{ fontWeight: 600, marginBottom: "0.75rem" }}>
          <FormattedMessage
            id="vector.admin.newSpecies"
            defaultMessage="New species"
          />
        </p>
      )}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr 1fr",
          gap: "1rem",
          marginBottom: "0.75rem",
        }}
      >
        <TextInput
          id={`sp-genus-${isNew ? "new" : form.genus}`}
          labelText={
            <>
              <FormattedMessage
                id="vector.species.genus"
                defaultMessage="Genus"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          placeholder={intl.formatMessage({
            id: "vector.species.genus.placeholder",
            defaultMessage: "e.g., Mansonia",
          })}
          value={form.genus}
          onChange={set("genus")}
        />
        <TextInput
          id={`sp-species-${isNew ? "new" : form.genus}`}
          labelText={
            <>
              <FormattedMessage
                id="vector.species.species"
                defaultMessage="Species"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          placeholder={intl.formatMessage({
            id: "vector.species.species.placeholder",
            defaultMessage: "e.g., uniformis",
          })}
          value={form.species}
          onChange={set("species")}
        />
        <TextInput
          id={`sp-sub-${isNew ? "new" : form.genus}`}
          labelText={
            <FormattedMessage
              id="vector.species.subspecies"
              defaultMessage="Subspecies (optional)"
            />
          }
          placeholder={intl.formatMessage({
            id: "vector.species.subspecies.placeholder",
            defaultMessage: "e.g., diardii",
          })}
          value={form.subspecies}
          onChange={set("subspecies")}
        />
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1rem",
          marginBottom: "0.75rem",
        }}
      >
        <Select
          id={`sp-group-${isNew ? "new" : form.genus}`}
          labelText={
            <>
              <FormattedMessage
                id="vector.admin.sampleType"
                defaultMessage="Sample type"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          value={form.sampleTypeId}
          onChange={set("sampleTypeId")}
        >
          <SelectItem value="" text="" />
          {groups.map((g) => (
            <SelectItem key={g.id} value={String(g.id)} text={g.description} />
          ))}
        </Select>
        {!isNew && (
          <Select
            id={`sp-status-${form.genus}`}
            labelText={
              <FormattedMessage
                id="vector.admin.status"
                defaultMessage="Status"
              />
            }
            value={form.active ? "active" : "inactive"}
            onChange={(e) =>
              setForm((f) => ({ ...f, active: e.target.value === "active" }))
            }
          >
            <SelectItem
              value="active"
              text={intl.formatMessage({
                id: "vector.admin.status.active",
                defaultMessage: "Active",
              })}
            />
            <SelectItem
              value="inactive"
              text={intl.formatMessage({
                id: "vector.admin.status.inactive",
                defaultMessage: "Inactive",
              })}
            />
          </Select>
        )}
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1rem",
          marginBottom: "0.75rem",
        }}
      >
        <Select
          id={`sp-pathogen-cat-${isNew ? "new" : form.genus}`}
          labelText={
            <FormattedMessage
              id="vector.species.pathogenCategory"
              defaultMessage="Pathogen category"
            />
          }
          value={form.pathogenCategoryId}
          onChange={(e) =>
            setForm((f) => ({ ...f, pathogenCategoryId: e.target.value }))
          }
        >
          <SelectItem value="" text="—" />
          {pathogenCategories.map((c) => (
            <SelectItem
              key={c.id}
              value={String(c.id)}
              text={c.categoryName}
            />
          ))}
        </Select>
        <Select
          id={`sp-lifecycle-cat-${isNew ? "new" : form.genus}`}
          labelText={
            <FormattedMessage
              id="vector.species.lifecycleCategory"
              defaultMessage="Lifecycle category"
            />
          }
          value={form.lifecycleCategoryId}
          onChange={(e) =>
            setForm((f) => ({ ...f, lifecycleCategoryId: e.target.value }))
          }
        >
          <SelectItem value="" text="—" />
          {lifecycleCategories.map((c) => (
            <SelectItem
              key={c.id}
              value={String(c.id)}
              text={c.categoryName}
            />
          ))}
        </Select>
      </div>

      <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.75rem" }}>
        <Button
          onClick={() => onSave(form)}
          disabled={!form.genus || !form.sampleTypeId}
        >
          {isNew ? (
            <FormattedMessage
              id="vector.admin.saveSpecies"
              defaultMessage="Save species"
            />
          ) : (
            <FormattedMessage id="label.button.save" defaultMessage="Save" />
          )}
        </Button>
        <Button kind="ghost" onClick={onCancel}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
      </div>
    </div>
  );
}

function SpeciesRow({ sp, groups, pathogenCategories, lifecycleCategories, onSaved }) {
  const intl = useIntl();
  const [expanded, setExpanded] = useState(false);
  const sampleTypeLabel =
    groups.find((g) => String(g.id) === String(sp.sampleTypeId))?.description ||
    "";

  const handleSave = (form) => {
    putToOpenElisServer(
      `${SPECIES_URL}/${sp.id}`,
      JSON.stringify({
        genus: form.genus,
        species: form.species,
        subspecies: form.subspecies,
        pathogenCategoryId: form.pathogenCategoryId ? Number(form.pathogenCategoryId) : null,
        lifecycleCategoryId: form.lifecycleCategoryId ? Number(form.lifecycleCategoryId) : null,
        active: form.active,
        id: sp.id,
        sampleTypeId: form.sampleTypeId ? Number(form.sampleTypeId) : null,
      }),
      (status) => {
        if (status === 200) {
          setExpanded(false);
          onSaved();
        }
      },
    );
  };

  const initial = {
    genus: sp.genus || "",
    species: sp.species || "",
    subspecies: sp.subspecies || "",
    sampleTypeId: sp.sampleTypeId != null ? String(sp.sampleTypeId) : "",
    pathogenCategoryId: sp.pathogenCategoryId != null ? String(sp.pathogenCategoryId) : "",
    lifecycleCategoryId: sp.lifecycleCategoryId != null ? String(sp.lifecycleCategoryId) : "",
    active: sp.active !== false,
  };

  const pathogenCatName =
    pathogenCategories.find((c) => String(c.id) === String(sp.pathogenCategoryId))?.categoryName || "—";

  return (
    <>
      <tr style={{ borderBottom: "1px solid #e0e0e0" }}>
        <td style={{ padding: "0.75rem 1rem", width: "32px" }}>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={expanded ? ChevronUp : ChevronDown}
            iconDescription="Toggle"
            hasIconOnly
            onClick={() => setExpanded((v) => !v)}
          />
        </td>
        <td
          style={{
            padding: "0.75rem 1rem",
            fontWeight: 500,
            fontStyle: "italic",
          }}
        >
          {sp.genus}
        </td>
        <td style={{ padding: "0.75rem 1rem", fontStyle: "italic" }}>
          {sp.species || "—"}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>{sp.subspecies || "—"}</td>
        <td style={{ padding: "0.75rem 1rem" }}>
          {sampleTypeLabel && (
            <Tag type="green" size="sm">
              {sampleTypeLabel}
            </Tag>
          )}
        </td>
        <td
          style={{
            padding: "0.75rem 1rem",
            color: "#525252",
            fontSize: "0.875rem",
          }}
        >
          {pathogenCatName}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Tag type={sp.active ? "green" : "gray"} size="sm">
            {sp.active
              ? intl.formatMessage({
                  id: "vector.admin.status.active",
                  defaultMessage: "Active",
                })
              : intl.formatMessage({
                  id: "vector.admin.status.inactive",
                  defaultMessage: "Inactive",
                })}
          </Tag>
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Button kind="ghost" size="sm" onClick={() => setExpanded((v) => !v)}>
            <FormattedMessage id="label.button.edit" defaultMessage="Edit" />
          </Button>
        </td>
      </tr>
      {expanded && (
        <tr>
          <td
            colSpan={8}
            style={{ padding: "0 1rem 1rem 1rem", background: "#f9f9f9" }}
          >
            <SpeciesForm
              initial={initial}
              groups={groups}
              pathogenCategories={pathogenCategories}
              lifecycleCategories={lifecycleCategories}
              onSave={handleSave}
              onCancel={() => setExpanded(false)}
              isNew={false}
            />
          </td>
        </tr>
      )}
    </>
  );
}

export default function VectorSpeciesPage() {
  const intl = useIntl();
  const [speciesList, setSpeciesList] = useState([]);
  const [groups, setGroups] = useState([]);
  const [pathogenCategories, setPathogenCategories] = useState([]);
  const [lifecycleCategories, setLifecycleCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filterGroup, setFilterGroup] = useState("");
  const [showNewForm, setShowNewForm] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(SPECIES_URL, (data) => {
      setSpeciesList(data || []);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    load();
    getFromOpenElisServer(GROUPS_URL, (data) => setGroups(data || []));
    getFromOpenElisServer(PATHOGEN_CATS_URL, (data) => setPathogenCategories(data || []));
    getFromOpenElisServer(LIFECYCLE_CATS_URL, (data) => setLifecycleCategories(data || []));
  }, [load]);

  const handleCreate = (form) => {
    postToOpenElisServer(
      SPECIES_URL,
      JSON.stringify({
        genus: form.genus,
        species: form.species,
        subspecies: form.subspecies,
        pathogenCategoryId: form.pathogenCategoryId ? Number(form.pathogenCategoryId) : null,
        lifecycleCategoryId: form.lifecycleCategoryId ? Number(form.lifecycleCategoryId) : null,
        active: true,
        sampleTypeId: form.sampleTypeId ? Number(form.sampleTypeId) : null,
      }),
      (status) => {
        if (status === 201) {
          setShowNewForm(false);
          load();
        }
      },
    );
  };

  const filtered = speciesList.filter((s) => {
    const matchSearch =
      !search ||
      s.genus?.toLowerCase().includes(search.toLowerCase()) ||
      s.species?.toLowerCase().includes(search.toLowerCase());
    const matchGroup = !filterGroup || String(s.sampleTypeId) === filterGroup;
    return matchSearch && matchGroup;
  });

  return (
    <>
      <div
        style={{
          display: "flex",
          gap: "1rem",
          marginBottom: "1rem",
          alignItems: "center",
        }}
      >
        <Search
          id="species-search"
          labelText=""
          placeholder={intl.formatMessage({
            id: "vector.admin.search.species",
            defaultMessage: "Search genus or species...",
          })}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ flex: 1 }}
        />
        <Button renderIcon={Add} onClick={() => setShowNewForm(true)}>
          <FormattedMessage
            id="vector.admin.addSpecies"
            defaultMessage="+ Add species"
          />
        </Button>
      </div>

      <div
        style={{
          display: "flex",
          gap: "0.5rem",
          marginBottom: "1rem",
          flexWrap: "wrap",
        }}
      >
        {groups.map((g) => (
          <Tag
            key={g.id}
            type={filterGroup === String(g.id) ? "blue" : "gray"}
            size="md"
            style={{ cursor: "pointer" }}
            onClick={() =>
              setFilterGroup(filterGroup === String(g.id) ? "" : String(g.id))
            }
          >
            {g.description}
          </Tag>
        ))}
      </div>

      <table
        style={{
          width: "100%",
          borderCollapse: "collapse",
          background: "#fff",
        }}
      >
        <thead>
          <tr
            style={{ background: "#f4f4f4", borderBottom: "2px solid #e0e0e0" }}
          >
            <th style={{ width: "32px" }} />
            <th
              style={{
                padding: "0.75rem 1rem",
                textAlign: "left",
                fontWeight: 600,
              }}
            >
              <FormattedMessage
                id="vector.species.genus"
                defaultMessage="Genus"
              />
            </th>
            <th
              style={{
                padding: "0.75rem 1rem",
                textAlign: "left",
                fontWeight: 600,
              }}
            >
              <FormattedMessage
                id="vector.species.species"
                defaultMessage="Species"
              />
            </th>
            <th
              style={{
                padding: "0.75rem 1rem",
                textAlign: "left",
                fontWeight: 600,
              }}
            >
              <FormattedMessage
                id="vector.species.subspecies"
                defaultMessage="Subspecies"
              />
            </th>
            <th
              style={{
                padding: "0.75rem 1rem",
                textAlign: "left",
                fontWeight: 600,
              }}
            >
              <FormattedMessage
                id="vector.admin.sampleType"
                defaultMessage="Sample type"
              />
            </th>
            <th
              style={{
                padding: "0.75rem 1rem",
                textAlign: "left",
                fontWeight: 600,
              }}
            >
              <FormattedMessage
                id="vector.species.pathogensOfInterest"
                defaultMessage="Pathogens of interest"
              />
            </th>
            <th
              style={{
                padding: "0.75rem 1rem",
                textAlign: "left",
                fontWeight: 600,
              }}
            >
              <FormattedMessage
                id="vector.admin.status"
                defaultMessage="Status"
              />
            </th>
            <th />
          </tr>
        </thead>
        <tbody>
          {showNewForm && (
            <tr>
              <td
                colSpan={8}
                style={{ padding: "0 1rem 1rem 1rem", background: "#f4f4f4" }}
              >
                <SpeciesForm
                  isNew
                  groups={groups}
                  pathogenCategories={pathogenCategories}
                  lifecycleCategories={lifecycleCategories}
                  onSave={handleCreate}
                  onCancel={() => setShowNewForm(false)}
                />
              </td>
            </tr>
          )}
          {loading ? (
            <tr>
              <td colSpan={8} style={{ padding: "2rem", textAlign: "center" }}>
                <InlineLoading />
              </td>
            </tr>
          ) : (
            filtered.map((s) => (
              <SpeciesRow
                key={s.id}
                sp={s}
                groups={groups}
                pathogenCategories={pathogenCategories}
                lifecycleCategories={lifecycleCategories}
                onSaved={load}
              />
            ))
          )}
        </tbody>
      </table>
    </>
  );
}
