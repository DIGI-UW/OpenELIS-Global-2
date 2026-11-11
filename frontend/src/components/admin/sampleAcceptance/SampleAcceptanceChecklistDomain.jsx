import React, { useContext, useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Select,
  SelectItem,
  TextInput,
  Tag,
  InlineLoading,
  Heading,
  Section,
} from "@carbon/react";
import { Add, ChevronUp, ChevronDown } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const API_BASE = "/rest/sample-acceptance-checklist/admin";

const ENFORCEMENT_OPTIONS = [
  { value: "MANDATORY", id: "sampleAcceptance.enforcement.mandatory" },
  { value: "OPTIONAL", id: "sampleAcceptance.enforcement.optional" },
  { value: "OFF", id: "sampleAcceptance.enforcement.off" },
];

const DOMAIN_META = {
  ALL: { id: "sampleAcceptance.domain.all", def: "All domains" },
  CLINICAL: { id: "sampleAcceptance.domain.clinical", def: "Clinical" },
  ENVIRONMENTAL: {
    id: "sampleAcceptance.domain.environmental",
    def: "Environmental",
  },
  VECTOR: { id: "sampleAcceptance.domain.vector", def: "Vector" },
};

const cellStyle = { padding: "0.75rem 1rem", textAlign: "left" };
const headerRowStyle = {
  background: "#f4f4f4",
  borderBottom: "2px solid #e0e0e0",
};
const tableStyle = {
  width: "100%",
  borderCollapse: "collapse",
  background: "#fff",
  marginBottom: "1rem",
};

function SampleAcceptanceChecklistDomain({ domain }) {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const isAll = domain === "ALL";
  const meta = DOMAIN_META[domain] || DOMAIN_META.ALL;
  const domainLabel = intl.formatMessage({
    id: meta.id,
    defaultMessage: meta.def,
  });

  const [view, setView] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showNewForm, setShowNewForm] = useState(false);
  const [newLabel, setNewLabel] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editLabel, setEditLabel] = useState("");
  const [editActive, setEditActive] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(`${API_BASE}?domain=${domain}`, (data) => {
      setView(data || null);
      setLoading(false);
    });
  }, [domain]);

  useEffect(() => {
    // Reset transient editing state when switching domains.
    setShowNewForm(false);
    setNewLabel("");
    setEditingId(null);
    load();
  }, [domain, load]);

  const notify = (kind, message) => {
    setNotificationVisible(true);
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message,
    });
  };

  const notifySuccess = (id, def) =>
    notify(
      NotificationKinds.success,
      intl.formatMessage({ id, defaultMessage: def }),
    );

  const isError = (json) => json && json.error;

  const handleAdd = () => {
    const label = newLabel.trim();
    if (!label) {
      return;
    }
    postToOpenElisServerJsonResponse(
      `${API_BASE}/items`,
      JSON.stringify({ domain, label }),
      (json) => {
        if (isError(json)) {
          notify(NotificationKinds.error, json.error);
          return;
        }
        setShowNewForm(false);
        setNewLabel("");
        notifySuccess("sampleAcceptance.notify.itemAdded", "Item added");
        load();
      },
    );
  };

  const beginEdit = (item) => {
    setEditingId(item.id);
    setEditLabel(item.label || "");
    setEditActive(item.active !== false);
  };

  const handleSaveEdit = () => {
    const label = editLabel.trim();
    if (!label) {
      return;
    }
    putToOpenElisServerJsonResponse(
      `${API_BASE}/items/${editingId}`,
      JSON.stringify({ label, active: editActive }),
      (json) => {
        if (isError(json)) {
          notify(NotificationKinds.error, json.error);
          return;
        }
        setEditingId(null);
        notifySuccess("sampleAcceptance.notify.itemSaved", "Item saved");
        load();
      },
    );
  };

  const handleMove = (index, direction) => {
    const ids = view.ownItems.map((i) => i.id);
    const target = direction === "up" ? index - 1 : index + 1;
    if (target < 0 || target >= ids.length) {
      return;
    }
    [ids[index], ids[target]] = [ids[target], ids[index]];
    postToOpenElisServerJsonResponse(
      `${API_BASE}/items/reorder`,
      JSON.stringify({ domain, orderedIds: ids }),
      (json) => {
        if (isError(json)) {
          notify(NotificationKinds.error, json.error);
          return;
        }
        // The reorder endpoint returns the refreshed view.
        setView(json);
      },
    );
  };

  const handleEnforcementChange = (e) => {
    const mode = e.target.value;
    putToOpenElisServerJsonResponse(
      `${API_BASE}/enforcement/${domain}`,
      JSON.stringify({ mode }),
      (json) => {
        if (isError(json)) {
          notify(NotificationKinds.error, json.error);
          return;
        }
        notifySuccess(
          "sampleAcceptance.notify.enforcementSaved",
          "Enforcement updated",
        );
        load();
      },
    );
  };

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "sidenav.label.admin.formEntry.sampleEntryconfig",
      link: "/MasterListsPage/SampleEntryConfigurationMenu",
    },
    {
      label: "sampleAcceptance.title",
      link: "/MasterListsPage/SampleAcceptanceChecklist/all",
    },
  ];

  const ownItems = view ? view.ownItems || [] : [];
  const labWideItems = view ? view.labWideItems || [] : [];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Section>
          <Heading>
            <FormattedMessage
              id="sampleAcceptance.title"
              defaultMessage="Sample Acceptance Checklist"
            />
            {" — "}
            {domainLabel}
          </Heading>
        </Section>
        <br />

        {loading || !view ? (
          <InlineLoading description={intl.formatMessage({ id: "loading" })} />
        ) : (
          <>
            {!isAll && (
              <div style={{ maxWidth: "28rem", marginBottom: "1.5rem" }}>
                <Select
                  id="enforcement-select"
                  labelText={intl.formatMessage(
                    {
                      id: "sampleAcceptance.enforcement.label",
                      defaultMessage: "Checklist enforcement — {domain}",
                    },
                    { domain: domainLabel },
                  )}
                  value={view.enforcement || "OPTIONAL"}
                  onChange={handleEnforcementChange}
                >
                  {ENFORCEMENT_OPTIONS.map((opt) => (
                    <SelectItem
                      key={opt.value}
                      value={opt.value}
                      text={intl.formatMessage({ id: opt.id })}
                    />
                  ))}
                </Select>
                <p
                  style={{
                    fontSize: "0.875rem",
                    color: "#525252",
                    marginTop: "0.5rem",
                  }}
                >
                  <FormattedMessage
                    id="sampleAcceptance.enforcement.help"
                    defaultMessage="Enforcement is set per domain, not per lab unit — a reception clerk spans many lab units."
                  />
                </p>
              </div>
            )}

            <p style={{ fontWeight: 600, marginBottom: "0.5rem" }}>
              {isAll ? (
                <FormattedMessage
                  id="sampleAcceptance.labWide.heading"
                  defaultMessage="Lab-wide items (apply unless a domain overrides)"
                />
              ) : (
                <FormattedMessage
                  id="sampleAcceptance.ownItems.heading"
                  defaultMessage="{domain} items"
                  values={{ domain: domainLabel }}
                />
              )}
            </p>

            <table style={tableStyle}>
              <thead>
                <tr style={headerRowStyle}>
                  <th style={{ ...cellStyle, width: "120px" }}>
                    <FormattedMessage
                      id="sampleAcceptance.col.order"
                      defaultMessage="Order"
                    />
                  </th>
                  <th style={cellStyle}>
                    <FormattedMessage
                      id="sampleAcceptance.col.label"
                      defaultMessage="Label"
                    />
                  </th>
                  <th style={{ ...cellStyle, width: "120px" }}>
                    <FormattedMessage
                      id="sampleAcceptance.col.active"
                      defaultMessage="Active"
                    />
                  </th>
                  <th style={{ ...cellStyle, width: "100px" }} />
                </tr>
              </thead>
              <tbody>
                {ownItems.map((item, index) =>
                  editingId === item.id ? (
                    <tr key={item.id}>
                      <td
                        colSpan={4}
                        style={{ padding: "1rem", background: "#f4f4f4" }}
                      >
                        <div
                          style={{
                            display: "flex",
                            gap: "1rem",
                            alignItems: "flex-end",
                            flexWrap: "wrap",
                          }}
                        >
                          <TextInput
                            id={`edit-label-${item.id}`}
                            labelText={intl.formatMessage({
                              id: "sampleAcceptance.col.label",
                              defaultMessage: "Label",
                            })}
                            value={editLabel}
                            onChange={(e) => setEditLabel(e.target.value)}
                            style={{ minWidth: "20rem" }}
                          />
                          <Select
                            id={`edit-active-${item.id}`}
                            labelText={intl.formatMessage({
                              id: "sampleAcceptance.col.active",
                              defaultMessage: "Active",
                            })}
                            value={editActive ? "active" : "inactive"}
                            onChange={(e) =>
                              setEditActive(e.target.value === "active")
                            }
                          >
                            <SelectItem
                              value="active"
                              text={intl.formatMessage({
                                id: "sampleAcceptance.status.active",
                                defaultMessage: "Active",
                              })}
                            />
                            <SelectItem
                              value="inactive"
                              text={intl.formatMessage({
                                id: "sampleAcceptance.status.inactive",
                                defaultMessage: "Inactive",
                              })}
                            />
                          </Select>
                          <Button
                            onClick={handleSaveEdit}
                            disabled={!editLabel.trim()}
                          >
                            <FormattedMessage
                              id="label.button.save"
                              defaultMessage="Save"
                            />
                          </Button>
                          <Button
                            kind="ghost"
                            onClick={() => setEditingId(null)}
                          >
                            <FormattedMessage
                              id="label.button.cancel"
                              defaultMessage="Cancel"
                            />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    <tr
                      key={item.id}
                      style={{ borderBottom: "1px solid #e0e0e0" }}
                    >
                      <td style={cellStyle}>
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={ChevronUp}
                          iconDescription={intl.formatMessage({
                            id: "sampleAcceptance.action.moveUp",
                            defaultMessage: "Move up",
                          })}
                          disabled={index === 0}
                          onClick={() => handleMove(index, "up")}
                        />
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={ChevronDown}
                          iconDescription={intl.formatMessage({
                            id: "sampleAcceptance.action.moveDown",
                            defaultMessage: "Move down",
                          })}
                          disabled={index === ownItems.length - 1}
                          onClick={() => handleMove(index, "down")}
                        />
                      </td>
                      <td style={cellStyle}>{item.label}</td>
                      <td style={cellStyle}>
                        <Tag type={item.active ? "green" : "gray"} size="sm">
                          {item.active ? (
                            <FormattedMessage
                              id="sampleAcceptance.status.active"
                              defaultMessage="Active"
                            />
                          ) : (
                            <FormattedMessage
                              id="sampleAcceptance.status.inactive"
                              defaultMessage="Inactive"
                            />
                          )}
                        </Tag>
                      </td>
                      <td style={cellStyle}>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => beginEdit(item)}
                        >
                          <FormattedMessage
                            id="label.button.edit"
                            defaultMessage="Edit"
                          />
                        </Button>
                      </td>
                    </tr>
                  ),
                )}
                {ownItems.length === 0 && !showNewForm && (
                  <tr>
                    <td colSpan={4} style={{ ...cellStyle, color: "#525252" }}>
                      <FormattedMessage
                        id="sampleAcceptance.ownItems.empty"
                        defaultMessage="No items yet — this domain falls back to the lab-wide list below."
                      />
                    </td>
                  </tr>
                )}
                {showNewForm && (
                  <tr>
                    <td
                      colSpan={4}
                      style={{ padding: "1rem", background: "#f4f4f4" }}
                    >
                      <div
                        style={{
                          display: "flex",
                          gap: "1rem",
                          alignItems: "flex-end",
                          flexWrap: "wrap",
                        }}
                      >
                        <TextInput
                          id="new-item-label"
                          labelText={intl.formatMessage({
                            id: "sampleAcceptance.col.label",
                            defaultMessage: "Label",
                          })}
                          value={newLabel}
                          onChange={(e) => setNewLabel(e.target.value)}
                          style={{ minWidth: "20rem" }}
                        />
                        <Button onClick={handleAdd} disabled={!newLabel.trim()}>
                          <FormattedMessage
                            id="label.button.save"
                            defaultMessage="Save"
                          />
                        </Button>
                        <Button
                          kind="ghost"
                          onClick={() => {
                            setShowNewForm(false);
                            setNewLabel("");
                          }}
                        >
                          <FormattedMessage
                            id="label.button.cancel"
                            defaultMessage="Cancel"
                          />
                        </Button>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>

            {!showNewForm && (
              <Button renderIcon={Add} onClick={() => setShowNewForm(true)}>
                <FormattedMessage
                  id="sampleAcceptance.button.addItem"
                  defaultMessage="Add item"
                />
              </Button>
            )}

            {!isAll && labWideItems.length > 0 && (
              <div style={{ marginTop: "2rem" }}>
                <p style={{ fontWeight: 600, marginBottom: "0.5rem" }}>
                  {view.domainOverrides ? (
                    <FormattedMessage
                      id="sampleAcceptance.labWide.superseded"
                      defaultMessage="Lab-wide items — superseded here (edit from “All domains”)"
                    />
                  ) : (
                    <FormattedMessage
                      id="sampleAcceptance.labWide.inherited"
                      defaultMessage="Lab-wide items — inherited (active fallback)"
                    />
                  )}
                </p>
                <table style={tableStyle}>
                  <thead>
                    <tr style={headerRowStyle}>
                      <th style={cellStyle}>
                        <FormattedMessage
                          id="sampleAcceptance.col.label"
                          defaultMessage="Label"
                        />
                      </th>
                      <th style={{ ...cellStyle, width: "200px" }}>
                        <FormattedMessage
                          id="sampleAcceptance.col.statusForDomain"
                          defaultMessage="Status for this domain"
                        />
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {labWideItems.map((item) => (
                      <tr
                        key={item.id}
                        style={{
                          borderBottom: "1px solid #e0e0e0",
                          color: view.domainOverrides ? "#a8a8a8" : "inherit",
                        }}
                      >
                        <td style={cellStyle}>{item.label}</td>
                        <td style={cellStyle}>
                          <Tag
                            type={view.domainOverrides ? "gray" : "green"}
                            size="sm"
                          >
                            {view.domainOverrides ? (
                              <FormattedMessage
                                id="sampleAcceptance.status.superseded"
                                defaultMessage="Superseded"
                              />
                            ) : (
                              <FormattedMessage
                                id="sampleAcceptance.status.inheritedActive"
                                defaultMessage="Active (inherited)"
                              />
                            )}
                          </Tag>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </>
  );
}

export default SampleAcceptanceChecklistDomain;
