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
  Checkbox,
} from "@carbon/react";
import { Add, ChevronDown, ChevronUp } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../utils/Utils";

const SPECIES_URL = "/rest/admin/vector/species";
const GROUPS_URL = "/rest/admin/vector/groups";
const PATHOGENS_URL = "/rest/vector/dictionary/pathogens";
const LIFECYCLE_URL = "/rest/vector/dictionary/lifecycle-stages";

const emptyForm = {
  genus: "",
  species: "",
  subspecies: "",
  groupId: "",
  pathogenIds: [],
  lifecycleIds: [],
  active: true,
};

function DictMultiSelect({ label, options, selectedIds, onChange }) {
  const toggle = (id) => {
    const sid = String(id);
    onChange(
      selectedIds.includes(sid)
        ? selectedIds.filter((x) => x !== sid)
        : [...selectedIds, sid],
    );
  };
  return (
    <div>
      <p
        style={{
          fontSize: "0.75rem",
          fontWeight: 600,
          marginBottom: "0.5rem",
          color: "#525252",
        }}
      >
        {label}
      </p>
      <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem 1.5rem" }}>
        {options.map((opt) => (
          <Checkbox
            key={opt.id}
            id={`dict-${opt.id}`}
            labelText={opt.dictEntry}
            checked={selectedIds.includes(String(opt.id))}
            onChange={() => toggle(opt.id)}
          />
        ))}
      </div>
    </div>
  );
}

function SpeciesForm({
  initial = emptyForm,
  groups,
  pathogens,
  lifecycleStages,
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
                id="vector.admin.group"
                defaultMessage="Organism group"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          value={form.groupId}
          onChange={set("groupId")}
        >
          <SelectItem value="" text="" />
          {groups.map((g) => (
            <SelectItem key={g.id} value={String(g.id)} text={g.label} />
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

      <div style={{ marginBottom: "0.75rem" }}>
        <DictMultiSelect
          label={intl.formatMessage({
            id: "vector.species.pathogensOfInterest",
            defaultMessage: "Pathogens of interest",
          })}
          options={pathogens}
          selectedIds={form.pathogenIds}
          onChange={(ids) => setForm((f) => ({ ...f, pathogenIds: ids }))}
        />
      </div>

      <div style={{ marginBottom: "0.75rem" }}>
        <DictMultiSelect
          label={intl.formatMessage({
            id: "vector.species.lifecycleStages",
            defaultMessage: "Lifecycle stages",
          })}
          options={lifecycleStages}
          selectedIds={form.lifecycleIds}
          onChange={(ids) => setForm((f) => ({ ...f, lifecycleIds: ids }))}
        />
      </div>

      <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.75rem" }}>
        <Button
          onClick={() => onSave(form)}
          disabled={!form.genus || !form.groupId}
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

function SpeciesRow({ sp, groups, pathogens, lifecycleStages, onSaved }) {
  const intl = useIntl();
  const [expanded, setExpanded] = useState(false);
  const groupLabel =
    groups.find((g) => String(g.id) === String(sp.group?.id))?.label || "";

  const spPathogenIds = (sp.pathogensOfInterest || []).map((d) => String(d.id));
  const spLifecycleIds = (sp.lifecycleStages || []).map((d) => String(d.id));

  const handleSave = (form) => {
    putToOpenElisServer(
      `${SPECIES_URL}/${sp.id}`,
      JSON.stringify({
        genus: form.genus,
        species: form.species,
        subspecies: form.subspecies,
        pathogensOfInterest: form.pathogenIds.map((id) => ({ id })),
        lifecycleStages: form.lifecycleIds.map((id) => ({ id })),
        active: form.active,
        id: sp.id,
        group: { id: form.groupId },
      }),
      () => {
        setExpanded(false);
        onSaved();
      },
    );
  };

  const initial = {
    genus: sp.genus || "",
    species: sp.species || "",
    subspecies: sp.subspecies || "",
    groupId: String(sp.group?.id || ""),
    pathogenIds: spPathogenIds,
    lifecycleIds: spLifecycleIds,
    active: sp.active !== false,
  };

  const pathogenSummary =
    spPathogenIds.length > 0
      ? pathogens
          .filter((p) => spPathogenIds.includes(String(p.id)))
          .map((p) => p.dictEntry)
          .slice(0, 2)
          .join(", ") + (spPathogenIds.length > 2 ? ` +${spPathogenIds.length - 2}` : "")
      : "—";

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
          {groupLabel && (
            <Tag type="green" size="sm">
              {groupLabel}
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
          {pathogenSummary}
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
              pathogens={pathogens}
              lifecycleStages={lifecycleStages}
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
  const [pathogens, setPathogens] = useState([]);
  const [lifecycleStages, setLifecycleStages] = useState([]);
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
    getFromOpenElisServer(PATHOGENS_URL, (data) => setPathogens(data || []));
    getFromOpenElisServer(LIFECYCLE_URL, (data) => setLifecycleStages(data || []));
  }, [load]);

  const handleCreate = (form) => {
    postToOpenElisServer(
      SPECIES_URL,
      JSON.stringify({
        genus: form.genus,
        species: form.species,
        subspecies: form.subspecies,
        pathogensOfInterest: form.pathogenIds.map((id) => ({ id })),
        lifecycleStages: form.lifecycleIds.map((id) => ({ id })),
        active: true,
        group: { id: form.groupId },
      }),
      () => {
        setShowNewForm(false);
        load();
      },
    );
  };

  const filtered = speciesList.filter((s) => {
    const matchSearch =
      !search ||
      s.genus?.toLowerCase().includes(search.toLowerCase()) ||
      s.species?.toLowerCase().includes(search.toLowerCase());
    const matchGroup = !filterGroup || String(s.group?.id) === filterGroup;
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
            {g.label}
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
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.species.genus" defaultMessage="Genus" />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.species.species" defaultMessage="Species" />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.species.subspecies" defaultMessage="Subspecies" />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.admin.group" defaultMessage="Group" />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.species.pathogensOfInterest" defaultMessage="Pathogens of interest" />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.admin.status" defaultMessage="Status" />
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
                  pathogens={pathogens}
                  lifecycleStages={lifecycleStages}
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
                pathogens={pathogens}
                lifecycleStages={lifecycleStages}
                onSaved={load}
              />
            ))
          )}
        </tbody>
      </table>
    </>
  );
}
