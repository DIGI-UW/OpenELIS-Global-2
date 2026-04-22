import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  TextInput,
  Tag,
  InlineLoading,
  Grid,
  Column,
  Stack,
  Search,
} from "@carbon/react";
import { Add, ChevronDown, ChevronUp, Edit } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../utils/Utils";

const GROUPS_URL = "/rest/admin/vector/groups";

const emptyForm = {
  code: "",
  label: "",
  colorTag: "",
  description: "",
};

function GroupForm({ initial = emptyForm, isSystem, onSave, onCancel, isNew }) {
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
            id="vector.admin.newGroup"
            defaultMessage="New group (unsaved)"
          />
        </p>
      )}
      {isSystem && (
        <div
          style={{
            borderLeft: "4px solid #0f62fe",
            background: "#edf5ff",
            padding: "0.5rem 0.75rem",
            marginBottom: "0.75rem",
            borderRadius: "0 4px 4px 0",
          }}
        >
          <p style={{ fontWeight: 600, marginBottom: "0.25rem" }}>
            <FormattedMessage
              id="vector.admin.systemGroup"
              defaultMessage="System-protected group"
            />
          </p>
          <p style={{ fontSize: "0.8125rem", color: "#525252" }}>
            <FormattedMessage
              id="vector.admin.systemGroup.desc"
              defaultMessage="The code is locked because this group is referenced by core reports and seed data. You may still edit the label, color, and description."
            />
          </p>
        </div>
      )}
      <Grid narrow>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id={`group-code-${isNew ? "new" : form.code}`}
            labelText={
              <>
                <FormattedMessage
                  id="vector.admin.group.code"
                  defaultMessage="Code"
                />
                {isSystem && (
                  <span style={{ color: "#6f6f6f", fontWeight: 400 }}>
                    {" "}
                    (
                    <FormattedMessage
                      id="vector.admin.locked"
                      defaultMessage="locked"
                    />
                    )
                  </span>
                )}
                {!isSystem && <span style={{ color: "#da1e28" }}> *</span>}
              </>
            }
            placeholder={intl.formatMessage({
              id: "vector.admin.group.code.placeholder",
              defaultMessage: "e.g., SANDFLY",
            })}
            value={form.code}
            onChange={set("code")}
            readOnly={isSystem}
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id={`group-label-${isNew ? "new" : form.code}`}
            labelText={
              <>
                <FormattedMessage
                  id="vector.admin.group.label"
                  defaultMessage="Label"
                />
                <span style={{ color: "#da1e28" }}> *</span>
              </>
            }
            placeholder={intl.formatMessage({
              id: "vector.admin.group.label.placeholder",
              defaultMessage: "e.g., Sandfly",
            })}
            value={form.label}
            onChange={set("label")}
          />
        </Column>
        <Column lg={6} md={4} sm={4}>
          <TextInput
            id={`group-color-${isNew ? "new" : form.code}`}
            labelText={
              <FormattedMessage
                id="vector.admin.group.colorTag"
                defaultMessage="Color tag* (type to search or create)"
              />
            }
            placeholder="Teal"
            value={form.colorTag}
            onChange={set("colorTag")}
          />
        </Column>
        <Column lg={16} md={8} sm={4}>
          <TextInput
            id={`group-desc-${isNew ? "new" : form.code}`}
            labelText={
              <FormattedMessage
                id="vector.admin.group.description"
                defaultMessage="Description"
              />
            }
            value={form.description}
            onChange={set("description")}
          />
        </Column>
        <Column
          lg={16}
          md={8}
          sm={4}
          style={{ marginTop: "0.75rem", display: "flex", gap: "0.5rem" }}
        >
          <Button
            onClick={() => onSave(form)}
            disabled={!form.code || !form.label}
          >
            {isNew ? (
              <FormattedMessage
                id="vector.admin.saveGroup"
                defaultMessage="Save group"
              />
            ) : (
              <FormattedMessage id="label.button.save" defaultMessage="Save" />
            )}
          </Button>
          <Button kind="ghost" onClick={onCancel}>
            <FormattedMessage
              id="label.button.cancel"
              defaultMessage="Cancel"
            />
          </Button>
        </Column>
      </Grid>
    </div>
  );
}

function GroupRow({ group, onSaved }) {
  const intl = useIntl();
  const [expanded, setExpanded] = useState(false);

  const handleSave = (form) => {
    putToOpenElisServer(
      `${GROUPS_URL}/${group.id}`,
      JSON.stringify({
        ...form,
        id: group.id,
        active: group.active,
        isSystem: group.isSystem,
        lastupdated: group.lastupdated,
      }),
      () => {
        setExpanded(false);
        onSaved();
      },
    );
  };

  const initial = {
    code: group.code || "",
    label: group.label || "",
    colorTag: group.colorTag || "",
    description: group.description || "",
  };

  return (
    <>
      <tr style={{ borderBottom: "1px solid #e0e0e0" }}>
        <td style={{ padding: "0.75rem 1rem", width: "32px" }}>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={expanded ? ChevronUp : ChevronDown}
            iconDescription={expanded ? "Collapse" : "Expand"}
            hasIconOnly
            onClick={() => setExpanded((v) => !v)}
          />
        </td>
        <td style={{ padding: "0.75rem 1rem", fontWeight: 500 }}>
          {group.code}
          {group.isSystem && (
            <Tag type="blue" size="sm" style={{ marginLeft: "0.5rem" }}>
              <FormattedMessage
                id="vector.admin.system"
                defaultMessage="SYSTEM"
              />
            </Tag>
          )}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>{group.label}</td>
        <td style={{ padding: "0.75rem 1rem" }}>
          {group.colorTag && (
            <Tag type="green" size="sm">
              {group.colorTag}
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
          {group.description}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          {/* Used by stats could go here */}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Tag type={group.active ? "green" : "gray"} size="sm">
            {group.active
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
            <GroupForm
              initial={initial}
              isSystem={group.isSystem}
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

export default function VectorGroupsPage() {
  const intl = useIntl();
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [showNewForm, setShowNewForm] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(GROUPS_URL, (data) => {
      setGroups(data || []);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleCreate = (form) => {
    postToOpenElisServer(
      GROUPS_URL,
      JSON.stringify({ ...form, active: true }),
      () => {
        setShowNewForm(false);
        load();
      },
    );
  };

  const filtered = groups.filter(
    (g) =>
      !search ||
      g.code?.toLowerCase().includes(search.toLowerCase()) ||
      g.label?.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <>
      {/* Header bar */}
      <div
        style={{
          display: "flex",
          gap: "1rem",
          marginBottom: "1.5rem",
          alignItems: "center",
        }}
      >
        <Search
          id="group-search"
          labelText=""
          placeholder={intl.formatMessage({
            id: "vector.admin.search.groups",
            defaultMessage: "Search by code or label...",
          })}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ flex: 1 }}
        />
        <Button renderIcon={Add} onClick={() => setShowNewForm(true)}>
          <FormattedMessage
            id="vector.admin.addGroup"
            defaultMessage="+ Add group"
          />
        </Button>
      </div>

      {/* Table */}
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
                id="vector.admin.group.code"
                defaultMessage="Code"
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
                id="vector.admin.group.label"
                defaultMessage="Label"
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
                id="vector.admin.group.colorTag"
                defaultMessage="Color tag"
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
                id="vector.admin.group.description"
                defaultMessage="Description"
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
                id="vector.admin.usedBy"
                defaultMessage="Used by"
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
          {/* New group form row */}
          {showNewForm && (
            <tr>
              <td
                colSpan={8}
                style={{ padding: "0 1rem 1rem 1rem", background: "#f4f4f4" }}
              >
                <GroupForm
                  isNew
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
            filtered.map((g) => (
              <GroupRow key={g.id} group={g} onSaved={load} />
            ))
          )}
        </tbody>
      </table>
    </>
  );
}
