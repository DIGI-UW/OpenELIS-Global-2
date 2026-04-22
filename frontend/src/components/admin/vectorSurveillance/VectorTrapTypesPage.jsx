import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  TextInput,
  Tag,
  Checkbox,
  InlineLoading,
  Search,
} from "@carbon/react";
import { Add, ChevronDown, ChevronUp } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../utils/Utils";

const TRAP_URL = "/rest/admin/vector/trap-types";
const GROUPS_URL = "/rest/admin/vector/groups";

const emptyForm = { name: "", groupIds: [], description: "" };

function GroupCheckboxes({ groups, selectedIds, onChange }) {
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
        <FormattedMessage
          id="vector.admin.trapType.groups"
          defaultMessage="Organism groups"
        />
        <span style={{ color: "#da1e28" }}> *</span>
      </p>
      <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem 1.5rem" }}>
        {groups.map((g) => (
          <Checkbox
            key={g.id}
            id={`trap-group-${g.id}`}
            labelText={g.label}
            checked={selectedIds.includes(String(g.id))}
            onChange={() => toggle(g.id)}
          />
        ))}
      </div>
    </div>
  );
}

function TrapForm({ initial = emptyForm, groups, onSave, onCancel, isNew }) {
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
            id="vector.admin.newTrapType"
            defaultMessage="New trap type"
          />
        </p>
      )}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1rem",
          marginBottom: "0.75rem",
        }}
      >
        <TextInput
          id={`trap-name-${isNew ? "new" : form.name}`}
          labelText={
            <>
              <FormattedMessage
                id="vector.admin.trapType.name"
                defaultMessage="Trap name"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          value={form.name}
          onChange={set("name")}
        />
        <TextInput
          id={`trap-desc-${isNew ? "new" : form.name}`}
          labelText={
            <FormattedMessage
              id="vector.admin.trapType.description"
              defaultMessage="Description"
            />
          }
          value={form.description}
          onChange={set("description")}
        />
      </div>
      <div style={{ marginBottom: "0.75rem" }}>
        <GroupCheckboxes
          groups={groups}
          selectedIds={form.groupIds}
          onChange={(ids) => setForm((f) => ({ ...f, groupIds: ids }))}
        />
      </div>
      <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.75rem" }}>
        <Button
          onClick={() => onSave(form)}
          disabled={!form.name || form.groupIds.length === 0}
        >
          {isNew ? (
            <FormattedMessage
              id="vector.admin.saveTrap"
              defaultMessage="Save trap"
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

function TrapRow({ trap, groups, onSaved }) {
  const intl = useIntl();
  const [expanded, setExpanded] = useState(false);

  const trapGroupIds = (trap.groups || []).map((g) => String(g.id));
  const trapGroupLabels = groups
    .filter((g) => trapGroupIds.includes(String(g.id)))
    .map((g) => g.label);

  const handleSave = (form) => {
    putToOpenElisServer(
      `${TRAP_URL}/${trap.id}`,
      JSON.stringify({
        name: form.name,
        description: form.description,
        active: form.active,
        id: trap.id,
        lastupdated: trap.lastupdated,
        groups: form.groupIds.map((id) => ({ id })),
      }),
      () => {
        setExpanded(false);
        onSaved();
      },
    );
  };

  const initial = {
    name: trap.name || "",
    groupIds: trapGroupIds,
    description: trap.description || "",
    active: trap.active !== false,
  };

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
        <td style={{ padding: "0.75rem 1rem", fontWeight: 500 }}>
          {trap.name}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "0.25rem" }}>
            {trapGroupLabels.map((label) => (
              <Tag key={label} type="green" size="sm">
                {label}
              </Tag>
            ))}
          </div>
        </td>
        <td
          style={{
            padding: "0.75rem 1rem",
            color: "#525252",
            fontSize: "0.875rem",
          }}
        >
          {trap.description}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Tag type={trap.active ? "green" : "gray"} size="sm">
            {trap.active
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
            colSpan={6}
            style={{ padding: "0 1rem 1rem 1rem", background: "#f9f9f9" }}
          >
            <TrapForm
              initial={initial}
              groups={groups}
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

export default function VectorTrapTypesPage() {
  const intl = useIntl();
  const [traps, setTraps] = useState([]);
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [showNewForm, setShowNewForm] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(TRAP_URL, (data) => {
      setTraps(data || []);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    load();
    getFromOpenElisServer(GROUPS_URL, (data) => setGroups(data || []));
  }, [load]);

  const handleCreate = (form) => {
    postToOpenElisServer(
      TRAP_URL,
      JSON.stringify({
        name: form.name,
        description: form.description,
        active: true,
        groups: form.groupIds.map((id) => ({ id })),
      }),
      () => {
        setShowNewForm(false);
        load();
      },
    );
  };

  const filtered = traps.filter(
    (t) =>
      !search ||
      t.name?.toLowerCase().includes(search.toLowerCase()) ||
      t.description?.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <>
      <div
        style={{
          display: "flex",
          gap: "1rem",
          marginBottom: "1.5rem",
          alignItems: "center",
        }}
      >
        <Search
          id="trap-search"
          labelText=""
          placeholder={intl.formatMessage({
            id: "vector.admin.search.traps",
            defaultMessage: "Search trap name or description...",
          })}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ flex: 1 }}
        />
        <Button renderIcon={Add} onClick={() => setShowNewForm(true)}>
          <FormattedMessage
            id="vector.admin.addTrapType"
            defaultMessage="+ Add trap type"
          />
        </Button>
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
              <FormattedMessage id="vector.admin.trapType.name" defaultMessage="Trap name" />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.admin.trapType.groups" defaultMessage="Organism groups" />
            </th>
            <th style={{ padding: "0.75rem 1rem", textAlign: "left", fontWeight: 600 }}>
              <FormattedMessage id="vector.admin.trapType.description" defaultMessage="Description" />
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
                colSpan={6}
                style={{ padding: "0 1rem 1rem 1rem", background: "#f4f4f4" }}
              >
                <TrapForm
                  isNew
                  groups={groups}
                  onSave={handleCreate}
                  onCancel={() => setShowNewForm(false)}
                />
              </td>
            </tr>
          )}
          {loading ? (
            <tr>
              <td colSpan={6} style={{ padding: "2rem", textAlign: "center" }}>
                <InlineLoading />
              </td>
            </tr>
          ) : (
            filtered.map((t) => (
              <TrapRow key={t.id} trap={t} groups={groups} onSaved={load} />
            ))
          )}
        </tbody>
      </table>
    </>
  );
}
