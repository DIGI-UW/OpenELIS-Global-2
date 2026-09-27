import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  TextInput,
  TextArea,
  Tag,
  Select,
  SelectItem,
  InlineLoading,
  Search,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../utils/Utils";
import AddressSearch from "../../patient/AddressSearch";

const SITES_URL = "/rest/admin/vector/sampling-sites";
const SITE_TYPES_URL = "/rest/vector/dictionary/sampling-site-types";
const ENV_ZONES_URL = "/rest/vector/dictionary/environmental-zones";
const HIERARCHY_LEVELS_URL = "/rest/address-hierarchy/levels";
const HIERARCHY_LEVEL_1_URL = "/rest/address-hierarchy/level/1";
const HIERARCHY_CHILDREN_URL = "/rest/address-hierarchy/children";

const TYPE_COLORS = {
  "Water Source": "blue",
  "Air Monitoring Station": "teal",
  "Vector Trap": "green",
  "Soil/Sediment": "warm-gray",
  "Soil Sampling Site": "warm-gray",
};

const emptyForm = {
  code: "",
  name: "",
  type: "",
  subtype: "",
  contactName: "",
  contactPhone: "",
  gpsLatitude: "",
  gpsLongitude: "",
  environmentalZone: "",
  locationOrgId: "",
  description: "",
  active: true,
};

function typeColor(type) {
  return TYPE_COLORS[type] || "gray";
}

function SiteForm({
  initial = emptyForm,
  siteTypes,
  envZones,
  onSave,
  onCancel,
  isNew,
}) {
  const intl = useIntl();
  const [form, setForm] = useState(initial);

  // Address hierarchy state
  const [hierarchyLevels, setHierarchyLevels] = useState([]);
  const [hierarchyValues, setHierarchyValues] = useState({});
  const [selectedHierarchyValues, setSelectedHierarchyValues] = useState({});

  useEffect(() => setForm(initial), [initial]);

  useEffect(() => {
    getFromOpenElisServer(HIERARCHY_LEVELS_URL, (levels) => {
      if (levels && levels.length > 0) {
        setHierarchyLevels(levels);
        getFromOpenElisServer(HIERARCHY_LEVEL_1_URL, (data) => {
          if (data) setHierarchyValues({ 0: data });
        });
      }
    });
  }, []);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
  const isValid = form.code.trim() && form.name.trim();
  const prefix = isNew ? "new" : form.code;

  const setDeepestLocation = (newSelected) => {
    // Find the deepest selected level and store its org ID as locationOrgId
    let deepestId = "";
    for (let i = hierarchyLevels.length - 1; i >= 0; i--) {
      if (newSelected[i]) {
        deepestId = newSelected[i];
        break;
      }
    }
    setForm((f) => ({ ...f, locationOrgId: deepestId }));
  };

  const handleHierarchySelection = (levelIndex, selectedId) => {
    const newSelected = {
      ...selectedHierarchyValues,
      [levelIndex]: selectedId,
    };
    // Clear child levels
    for (let i = levelIndex + 1; i < hierarchyLevels.length; i++) {
      delete newSelected[i];
      setHierarchyValues((prev) => {
        const updated = { ...prev };
        delete updated[i];
        return updated;
      });
    }
    setSelectedHierarchyValues(newSelected);
    setDeepestLocation(newSelected);

    if (selectedId && levelIndex < hierarchyLevels.length - 1) {
      getFromOpenElisServer(
        `${HIERARCHY_CHILDREN_URL}?parentId=${selectedId}`,
        (children) => {
          if (children) {
            setHierarchyValues((prev) => ({
              ...prev,
              [levelIndex + 1]: children,
            }));
          }
        },
      );
    }
  };

  const handleAddressSearchSelect = (hierarchyLevelsData) => {
    if (!hierarchyLevelsData || hierarchyLevelsData.length === 0) return;
    const newSelected = {};
    hierarchyLevelsData.forEach((levelData) => {
      newSelected[levelData.level - 1] = levelData.id;
    });
    setSelectedHierarchyValues(newSelected);
    setDeepestLocation(newSelected);

    // Fetch children for each level to populate dropdowns
    const fetchChildren = (idx) => {
      if (idx >= hierarchyLevelsData.length) return;
      const levelData = hierarchyLevelsData[idx];
      const nextIdx = idx + 1;
      if (nextIdx < hierarchyLevels.length) {
        getFromOpenElisServer(
          `${HIERARCHY_CHILDREN_URL}?parentId=${levelData.id}`,
          (children) => {
            if (children) {
              setHierarchyValues((prev) => ({ ...prev, [nextIdx]: children }));
              fetchChildren(nextIdx);
            }
          },
        );
      }
    };
    fetchChildren(0);
  };

  return (
    <div style={{ padding: "1rem 0" }}>
      {/* Code, Name, Type */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "160px 1fr 1fr",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        <TextInput
          id={`site-code-${prefix}`}
          labelText={
            <>
              <FormattedMessage
                id="vector.admin.samplingSite.code"
                defaultMessage="Code"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          value={form.code}
          onChange={set("code")}
          placeholder="e.g. WS-001"
        />
        <TextInput
          id={`site-name-${prefix}`}
          labelText={
            <>
              <FormattedMessage
                id="vector.admin.samplingSite.name"
                defaultMessage="Site Name"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          value={form.name}
          onChange={set("name")}
        />
        <Select
          id={`site-type-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.type"
              defaultMessage="Type"
            />
          }
          value={form.type}
          onChange={set("type")}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "label.select",
              defaultMessage: "Select...",
            })}
          />
          {siteTypes.map((t) => (
            <SelectItem key={t.id} value={t.dictEntry} text={t.dictEntry} />
          ))}
        </Select>
      </div>

      {/* Subtype */}
      <div style={{ marginBottom: "1rem" }}>
        <TextInput
          id={`site-subtype-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.subtype"
              defaultMessage="Subtype"
            />
          }
          value={form.subtype}
          onChange={set("subtype")}
          placeholder={intl.formatMessage({
            id: "vector.admin.samplingSite.subtype.placeholder",
            defaultMessage: "e.g. River, Well, BG-Sentinel, Fixed Station",
          })}
        />
      </div>

      {/* Environmental zone + GPS */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr 1fr",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        <Select
          id={`site-envzone-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.environmentalZone"
              defaultMessage="Environmental Zone"
            />
          }
          value={form.environmentalZone}
          onChange={set("environmentalZone")}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "label.select",
              defaultMessage: "Select...",
            })}
          />
          {envZones.map((z) => (
            <SelectItem key={z.id} value={z.dictEntry} text={z.dictEntry} />
          ))}
        </Select>
        <TextInput
          id={`site-lat-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.gpsLatitude"
              defaultMessage="GPS Latitude"
            />
          }
          value={form.gpsLatitude}
          onChange={set("gpsLatitude")}
          placeholder="-6.2088"
        />
        <TextInput
          id={`site-lon-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.gpsLongitude"
              defaultMessage="GPS Longitude"
            />
          }
          value={form.gpsLongitude}
          onChange={set("gpsLongitude")}
          placeholder="106.8456"
        />
      </div>

      {/* Address search + hierarchy */}
      {hierarchyLevels.length > 0 && (
        <div style={{ marginBottom: "1rem" }}>
          <p
            style={{
              fontSize: "0.75rem",
              fontWeight: 600,
              color: "#525252",
              marginBottom: "0.5rem",
            }}
          >
            <FormattedMessage
              id="vector.admin.samplingSite.addressLocation"
              defaultMessage="Address Location"
            />
          </p>
          <AddressSearch
            onAddressSelect={handleAddressSearchSelect}
            addressHierarchyLevels={hierarchyLevels}
            placeholder={intl.formatMessage({
              id: "vector.admin.samplingSite.addressSearch.placeholder",
              defaultMessage: "Search location to auto-fill dropdowns...",
            })}
          />
          <div
            style={{
              display: "grid",
              gridTemplateColumns: `repeat(${hierarchyLevels.length}, 1fr)`,
              gap: "1rem",
              marginTop: "0.5rem",
            }}
          >
            {hierarchyLevels.map((level, idx) => {
              const values = hierarchyValues[idx] || [];
              const selectedValue = selectedHierarchyValues[idx] || "";
              const isDisabled = idx > 0 && !selectedHierarchyValues[idx - 1];
              return (
                <Select
                  key={level.level}
                  id={`site-hier-${prefix}-${idx}`}
                  labelText={level.typeName}
                  value={selectedValue}
                  onChange={(e) =>
                    handleHierarchySelection(idx, e.target.value)
                  }
                  disabled={isDisabled || values.length === 0}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage(
                      {
                        id: "location.select.placeholder",
                        defaultMessage: "Select {levelName}...",
                      },
                      { levelName: level.typeName },
                    )}
                  />
                  {values.map((item) => (
                    <SelectItem
                      key={item.id}
                      value={item.id}
                      text={item.value}
                    />
                  ))}
                </Select>
              );
            })}
          </div>
        </div>
      )}

      {/* Contact */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        <TextInput
          id={`site-contact-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.contactName"
              defaultMessage="Contact Person"
            />
          }
          value={form.contactName}
          onChange={set("contactName")}
        />
        <TextInput
          id={`site-phone-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.contactPhone"
              defaultMessage="Contact Phone"
            />
          }
          value={form.contactPhone}
          onChange={set("contactPhone")}
        />
      </div>

      {/* Status (edit only) */}
      {!isNew && (
        <div style={{ marginBottom: "1rem" }}>
          <Select
            id={`site-status-${prefix}`}
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
        </div>
      )}

      {/* Description */}
      <div style={{ marginBottom: "1rem" }}>
        <TextArea
          id={`site-desc-${prefix}`}
          labelText={
            <FormattedMessage
              id="vector.admin.samplingSite.description"
              defaultMessage="Description"
            />
          }
          value={form.description}
          onChange={set("description")}
          rows={3}
        />
      </div>

      <div style={{ display: "flex", gap: "0.5rem" }}>
        <Button onClick={() => onSave(form)} disabled={!isValid}>
          {isNew ? (
            <FormattedMessage
              id="vector.admin.samplingSite.saveSite"
              defaultMessage="Save site"
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

function DetailPanel({ site, siteTypes, envZones, onSaved, onClose }) {
  const intl = useIntl();
  const [editing, setEditing] = useState(false);
  const [locationPath, setLocationPath] = useState(null);

  useEffect(() => {
    if (site.locationOrgId) {
      getFromOpenElisServer(
        `/rest/address-hierarchy/path/${site.locationOrgId}`,
        (path) => {
          if (path && path.length > 0) {
            setLocationPath(path.map((p) => p.value).join(" > "));
          }
        },
      );
    }
  }, [site.locationOrgId]);

  const initial = {
    code: site.code || "",
    name: site.name || "",
    type: site.type || "",
    subtype: site.subtype || "",
    contactName: site.contactName || "",
    contactPhone: site.contactPhone || "",
    gpsLatitude: site.gpsLatitude || "",
    gpsLongitude: site.gpsLongitude || "",
    environmentalZone: site.environmentalZone || "",
    locationOrgId: site.locationOrgId || "",
    description: site.description || "",
    active: site.active !== false,
  };

  const handleSave = (form) => {
    putToOpenElisServer(
      `${SITES_URL}/${site.id}`,
      JSON.stringify({ ...form, id: site.id, lastupdated: site.lastupdated }),
      (status) => {
        if (status === 200) {
          setEditing(false);
          onSaved();
        }
      },
    );
  };

  const handleToggleActive = () => {
    putToOpenElisServer(
      `${SITES_URL}/${site.id}`,
      JSON.stringify({
        ...initial,
        id: site.id,
        lastupdated: site.lastupdated,
        active: !site.active,
      }),
      (status) => {
        if (status === 200) onSaved();
      },
    );
  };

  if (editing) {
    return (
      <td
        colSpan={6}
        style={{
          padding: "0 1.5rem 1.5rem",
          background: "#f4f4f4",
          borderBottom: "2px solid #e0e0e0",
        }}
      >
        <SiteForm
          initial={initial}
          siteTypes={siteTypes}
          envZones={envZones}
          onSave={handleSave}
          onCancel={() => setEditing(false)}
          isNew={false}
        />
      </td>
    );
  }

  const hasGps = site.gpsLatitude || site.gpsLongitude;

  return (
    <td
      colSpan={6}
      style={{
        padding: "0",
        background: "#f4f4f4",
        borderBottom: "2px solid #e0e0e0",
      }}
    >
      <div style={{ padding: "1rem 1.5rem" }}>
        {/* Site name + type + subtype tags */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
            marginBottom: "0.75rem",
          }}
        >
          <span style={{ fontWeight: 600, fontSize: "1rem" }}>
            {site.code} — {site.name}
          </span>
          {site.type && (
            <Tag type={typeColor(site.type)} size="sm">
              {site.type}
            </Tag>
          )}
          {site.subtype && (
            <Tag type="cyan" size="sm">
              {site.subtype}
            </Tag>
          )}
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr 1fr",
            gap: "1.5rem",
          }}
        >
          {/* Left: location info */}
          <div>
            {locationPath && (
              <div style={{ marginBottom: "0.5rem" }}>
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: 600,
                    textTransform: "uppercase",
                  }}
                >
                  <FormattedMessage
                    id="vector.admin.samplingSite.addressLocation"
                    defaultMessage="Address Location"
                  />
                </span>
                <p style={{ margin: "0.15rem 0 0", fontSize: "0.875rem" }}>
                  {locationPath}
                </p>
              </div>
            )}
            {site.description && (
              <div style={{ marginBottom: "0.5rem" }}>
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: 600,
                    textTransform: "uppercase",
                  }}
                >
                  <FormattedMessage
                    id="vector.admin.samplingSite.description"
                    defaultMessage="Description"
                  />
                </span>
                <p
                  style={{
                    margin: "0.15rem 0 0",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  {site.description}
                </p>
              </div>
            )}
            {site.environmentalZone && (
              <div style={{ marginBottom: "0.5rem" }}>
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: 600,
                    textTransform: "uppercase",
                  }}
                >
                  <FormattedMessage
                    id="vector.admin.samplingSite.environmentalZone"
                    defaultMessage="Environmental Zone"
                  />
                </span>
                <p style={{ margin: "0.15rem 0 0", fontSize: "0.875rem" }}>
                  {site.environmentalZone}
                </p>
              </div>
            )}
          </div>

          {/* Middle: GPS + contact */}
          <div>
            {hasGps && (
              <div style={{ marginBottom: "0.5rem" }}>
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: 600,
                    textTransform: "uppercase",
                  }}
                >
                  <FormattedMessage
                    id="vector.admin.samplingSite.gpsCoordinates"
                    defaultMessage="GPS Coordinates"
                  />
                </span>
                <p
                  style={{
                    margin: "0.15rem 0 0",
                    fontSize: "0.875rem",
                    fontFamily: "monospace",
                  }}
                >
                  {[site.gpsLatitude, site.gpsLongitude]
                    .filter(Boolean)
                    .join(", ")}
                </p>
              </div>
            )}
            {(site.contactName || site.contactPhone) && (
              <div style={{ marginBottom: "0.5rem" }}>
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: 600,
                    textTransform: "uppercase",
                  }}
                >
                  <FormattedMessage
                    id="vector.admin.samplingSite.contactName"
                    defaultMessage="Contact Person"
                  />
                </span>
                <p style={{ margin: "0.15rem 0 0", fontSize: "0.875rem" }}>
                  {site.contactName}
                </p>
                {site.contactPhone && (
                  <p
                    style={{
                      margin: "0.1rem 0 0",
                      fontSize: "0.875rem",
                      color: "#525252",
                    }}
                  >
                    {site.contactPhone}
                  </p>
                )}
              </div>
            )}
          </div>

          {/* Right: timestamps */}
          <div>
            {site.lastupdated && (
              <div style={{ marginBottom: "0.5rem" }}>
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: 600,
                    textTransform: "uppercase",
                  }}
                >
                  <FormattedMessage
                    id="vector.admin.samplingSite.lastModified"
                    defaultMessage="Last Modified"
                  />
                </span>
                <p
                  style={{
                    margin: "0.15rem 0 0",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  {new Date(site.lastupdated).toLocaleDateString()}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Action buttons */}
        <div
          style={{
            display: "flex",
            gap: "0.5rem",
            marginTop: "1rem",
            borderTop: "1px solid #e0e0e0",
            paddingTop: "0.75rem",
          }}
        >
          <Button size="sm" onClick={() => setEditing(true)}>
            <FormattedMessage id="label.button.edit" defaultMessage="Edit" />
          </Button>
          <Button
            size="sm"
            kind={site.active !== false ? "danger--ghost" : "secondary"}
            onClick={handleToggleActive}
          >
            {site.active !== false
              ? intl.formatMessage({
                  id: "vector.admin.samplingSite.deactivate",
                  defaultMessage: "Deactivate",
                })
              : intl.formatMessage({
                  id: "vector.admin.samplingSite.activate",
                  defaultMessage: "Activate",
                })}
          </Button>
          <Button size="sm" kind="ghost" onClick={onClose}>
            <FormattedMessage id="label.button.close" defaultMessage="Close" />
          </Button>
        </div>
      </div>
    </td>
  );
}

function SiteRow({ site, siteTypes, envZones, onSaved }) {
  const intl = useIntl();
  const [expanded, setExpanded] = useState(false);

  return (
    <>
      <tr
        style={{
          borderBottom: expanded ? "none" : "1px solid #e0e0e0",
          background: expanded ? "#f4f4f4" : "#fff",
          cursor: "pointer",
        }}
        onClick={() => setExpanded((v) => !v)}
      >
        <td
          style={{
            padding: "0.75rem 1rem",
            fontWeight: 600,
            fontSize: "0.875rem",
            color: "#0f62fe",
            whiteSpace: "nowrap",
          }}
        >
          {site.code}
        </td>
        <td style={{ padding: "0.75rem 1rem", fontWeight: 500 }}>
          {site.name}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          {site.type && (
            <Tag type={typeColor(site.type)} size="sm">
              {site.type}
            </Tag>
          )}
          {site.subtype && (
            <Tag type="cyan" size="sm" style={{ marginLeft: "0.25rem" }}>
              {site.subtype}
            </Tag>
          )}
        </td>
        <td
          style={{
            padding: "0.75rem 1rem",
            fontSize: "0.875rem",
            color: "#525252",
          }}
        >
          {site.contactName || "—"}
        </td>
        <td
          style={{
            padding: "0.75rem 1rem",
            fontSize: "0.875rem",
            color: "#525252",
          }}
        >
          {"—"}
        </td>
        <td style={{ padding: "0.75rem 1rem" }}>
          <Tag type={site.active !== false ? "green" : "gray"} size="sm">
            {site.active !== false
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
      </tr>
      {expanded && (
        <tr style={{ background: "#f4f4f4" }}>
          <DetailPanel
            site={site}
            siteTypes={siteTypes}
            envZones={envZones}
            onSaved={() => {
              onSaved();
            }}
            onClose={() => setExpanded(false)}
          />
        </tr>
      )}
    </>
  );
}

export default function VectorSamplingSitesPage() {
  const intl = useIntl();
  const [sites, setSites] = useState([]);
  const [siteTypes, setSiteTypes] = useState([]);
  const [envZones, setEnvZones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [showNewForm, setShowNewForm] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(SITES_URL, (data) => {
      setSites(data || []);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    load();
    getFromOpenElisServer(SITE_TYPES_URL, (data) => setSiteTypes(data || []));
    getFromOpenElisServer(ENV_ZONES_URL, (data) => setEnvZones(data || []));
  }, [load]);

  const handleCreate = (form) => {
    postToOpenElisServer(SITES_URL, JSON.stringify({ ...form }), (status) => {
      if (status === 201) {
        setShowNewForm(false);
        load();
      }
    });
  };

  const filtered = sites.filter((s) => {
    const matchType = typeFilter === "" || s.type === typeFilter;
    const q = search.toLowerCase();
    const matchSearch =
      !q ||
      s.code?.toLowerCase().includes(q) ||
      s.name?.toLowerCase().includes(q);
    return matchType && matchSearch;
  });

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1.25rem",
          paddingTop: "0.25rem",
        }}
      >
        <h2 style={{ fontSize: "1.25rem", fontWeight: 600, margin: 0 }}>
          <FormattedMessage
            id="vector.admin.samplingSites.registry"
            defaultMessage="Sampling Site Registry"
          />
        </h2>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button renderIcon={Add} onClick={() => setShowNewForm((v) => !v)}>
            <FormattedMessage
              id="vector.admin.addSite"
              defaultMessage="+ New Site"
            />
          </Button>
        </div>
      </div>

      {/* New site form */}
      {showNewForm && (
        <div
          style={{
            background: "#f4f4f4",
            border: "1px solid #e0e0e0",
            borderRadius: "4px",
            padding: "1rem 1.5rem",
            marginBottom: "1.25rem",
          }}
        >
          <p style={{ fontWeight: 600, marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="vector.admin.samplingSite.newSite"
              defaultMessage="New site"
            />
          </p>
          <SiteForm
            isNew
            siteTypes={siteTypes}
            envZones={envZones}
            onSave={handleCreate}
            onCancel={() => setShowNewForm(false)}
          />
        </div>
      )}

      {/* Search + type filter */}
      <div
        style={{
          display: "flex",
          gap: "1rem",
          marginBottom: "1rem",
          alignItems: "flex-end",
        }}
      >
        <div style={{ flex: 1 }}>
          <Search
            id="site-search"
            labelText=""
            placeholder={intl.formatMessage({
              id: "vector.admin.samplingSite.search",
              defaultMessage: "Search by code or name...",
            })}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div style={{ width: "220px" }}>
          <Select
            id="site-type-filter"
            labelText={intl.formatMessage({
              id: "vector.admin.samplingSite.type",
              defaultMessage: "Type",
            })}
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "vector.admin.samplingSite.allTypes",
                defaultMessage: "All types",
              })}
            />
            {siteTypes.map((t) => (
              <SelectItem key={t.id} value={t.dictEntry} text={t.dictEntry} />
            ))}
          </Select>
        </div>
      </div>

      {/* Table */}
      <div
        style={{
          overflowX: "auto",
          background: "#fff",
          border: "1px solid #e0e0e0",
          borderRadius: "4px",
        }}
      >
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr
              style={{
                background: "#f4f4f4",
                borderBottom: "2px solid #e0e0e0",
              }}
            >
              <th
                style={{
                  padding: "0.75rem 1rem",
                  textAlign: "left",
                  fontWeight: 600,
                  fontSize: "0.875rem",
                  whiteSpace: "nowrap",
                }}
              >
                <FormattedMessage
                  id="vector.admin.samplingSite.code"
                  defaultMessage="Code"
                />
              </th>
              <th
                style={{
                  padding: "0.75rem 1rem",
                  textAlign: "left",
                  fontWeight: 600,
                  fontSize: "0.875rem",
                }}
              >
                <FormattedMessage
                  id="vector.admin.samplingSite.name"
                  defaultMessage="Name"
                />
              </th>
              <th
                style={{
                  padding: "0.75rem 1rem",
                  textAlign: "left",
                  fontWeight: 600,
                  fontSize: "0.875rem",
                }}
              >
                <FormattedMessage
                  id="vector.admin.samplingSite.type"
                  defaultMessage="Type"
                />
              </th>
              <th
                style={{
                  padding: "0.75rem 1rem",
                  textAlign: "left",
                  fontWeight: 600,
                  fontSize: "0.875rem",
                }}
              >
                <FormattedMessage
                  id="vector.admin.samplingSite.contactName"
                  defaultMessage="Contact"
                />
              </th>
              <th
                style={{
                  padding: "0.75rem 1rem",
                  textAlign: "left",
                  fontWeight: 600,
                  fontSize: "0.875rem",
                  whiteSpace: "nowrap",
                }}
              >
                <FormattedMessage
                  id="vector.admin.samplingSite.lastCollection"
                  defaultMessage="Last Collection"
                />
              </th>
              <th
                style={{
                  padding: "0.75rem 1rem",
                  textAlign: "left",
                  fontWeight: 600,
                  fontSize: "0.875rem",
                }}
              >
                <FormattedMessage
                  id="vector.admin.status"
                  defaultMessage="Status"
                />
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td
                  colSpan={6}
                  style={{ padding: "3rem", textAlign: "center" }}
                >
                  <InlineLoading
                    description={intl.formatMessage({
                      id: "label.loading",
                      defaultMessage: "Loading...",
                    })}
                  />
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td
                  colSpan={6}
                  style={{
                    padding: "3rem",
                    textAlign: "center",
                    color: "#525252",
                    fontSize: "0.875rem",
                  }}
                >
                  <FormattedMessage
                    id="label.noResults"
                    defaultMessage="No results found"
                  />
                </td>
              </tr>
            ) : (
              filtered.map((s) => (
                <SiteRow
                  key={s.id}
                  site={s}
                  siteTypes={siteTypes}
                  envZones={envZones}
                  onSaved={load}
                />
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Count */}
      {!loading && (
        <p
          style={{ marginTop: "0.5rem", fontSize: "0.75rem", color: "#6f6f6f" }}
        >
          {filtered.length} {filtered.length === 1 ? "site" : "sites"}
        </p>
      )}
    </div>
  );
}
