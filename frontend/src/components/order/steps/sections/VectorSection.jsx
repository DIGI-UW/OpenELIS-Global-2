import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { TextInput, Button, Tag, Link, Select, SelectItem, Checkbox, TextArea } from "@carbon/react";
import { ChevronDown, ChevronUp } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../../utils/Utils";

const SITES_URL = "/rest/admin/vector/sampling-sites/active";

function SectionHeader({ title }) {
  return (
    <p
      style={{
        fontWeight: 600,
        fontSize: "1rem",
        marginBottom: "1rem",
        paddingBottom: "0.4rem",
        borderBottom: "1px solid #e0e0e0",
      }}
    >
      {title}
    </p>
  );
}

function FieldLabel({ label, required }) {
  return (
    <p
      style={{
        fontSize: "0.75rem",
        fontWeight: 600,
        color: "#525252",
        marginBottom: "0.25rem",
      }}
    >
      {label}
      {required && <span style={{ color: "#da1e28" }}> *</span>}
    </p>
  );
}

function SearchResults({ results, onSelect, renderRow }) {
  if (results.length === 0) return null;
  return (
    <div className="search-results" style={{ marginTop: "0.5rem" }}>
      <table
        style={{
          width: "100%",
          borderCollapse: "collapse",
          fontSize: "0.875rem",
        }}
      >
        <tbody>
          {results.map((r, i) => (
            <tr key={i} style={{ borderBottom: "1px solid #e0e0e0" }}>
              <td style={{ padding: "0.6rem 0.75rem" }}>{renderRow(r)}</td>
              <td
                style={{
                  padding: "0.6rem 0.75rem",
                  textAlign: "right",
                  whiteSpace: "nowrap",
                }}
              >
                <Button kind="primary" size="sm" onClick={() => onSelect(r)}>
                  <FormattedMessage
                    id="label.button.select"
                    defaultMessage="Select"
                  />
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SelectedCard({ onClear, children }) {
  return (
    <div className="selected-entity-card">
      <div className="selected-card-header">
        <Tag type="green" size="sm">
          <FormattedMessage id="selected" defaultMessage="Selected" />
        </Tag>
        <Link onClick={onClear}>
          <FormattedMessage id="label.button.clear" defaultMessage="Clear" />
        </Link>
      </div>
      <div className="selected-card-content">{children}</div>
    </div>
  );
}

function VectorSection({ orderData, setOrderData, isReadOnly }) {
  const intl = useIntl();

  const [sites, setSites] = useState([]);
  const [siteSearch, setSiteSearch] = useState("");
  const [siteResults, setSiteResults] = useState([]);
  const [selectedSite, setSelectedSite] = useState(null);
  const [collectionContextOpen, setCollectionContextOpen] = useState(false);

  const updateEnvField = useCallback(
    (key, value) => {
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          environmentalFields: {
            ...prev.sampleOrderItems?.environmentalFields,
            [key]: value,
          },
        },
      }));
    },
    [setOrderData],
  );

  useEffect(() => {
    getFromOpenElisServer(SITES_URL, (data) => setSites(data || []));
  }, []);

  useEffect(() => {
    if (!siteSearch.trim()) {
      setSiteResults([]);
      return;
    }
    const q = siteSearch.toLowerCase();
    setSiteResults(
      sites
        .filter(
          (s) =>
            s.name?.toLowerCase().includes(q) ||
            s.code?.toLowerCase().includes(q),
        )
        .slice(0, 10),
    );
  }, [siteSearch, sites]);

  const handleSelectSite = (site) => {
    setSelectedSite(site);
    setSiteSearch("");
    setSiteResults([]);
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          vecCollectionSiteId: String(site.id),
          vecCollectionSiteName: site.name,
          vecGpsLatitude: site.gpsLatitude || "",
          vecGpsLongitude: site.gpsLongitude || "",
        },
      },
    }));
  };

  const handleClearSite = () => {
    setSelectedSite(null);
    setSiteSearch("");
    setSiteResults([]);
    updateEnvField("vecCollectionSiteId", "");
    updateEnvField("vecCollectionSiteName", "");
    updateEnvField("vecGpsLatitude", "");
    updateEnvField("vecGpsLongitude", "");
  };

  return (
    <div style={{ maxWidth: "720px" }}>
      <div style={{ marginBottom: "1.5rem" }}>
        <h3
          style={{
            fontWeight: 700,
            fontSize: "1.125rem",
            margin: "0 0 0.25rem",
          }}
        >
          <FormattedMessage
            id="vector.order.title"
            defaultMessage="New Vector Order"
          />
        </h3>
        <p style={{ fontSize: "0.875rem", color: "#525252", margin: 0 }}>
          <FormattedMessage
            id="vector.order.subtitle"
            defaultMessage="Lab unit: Vector Surveillance Lab — Vector domain active."
          />
        </p>
      </div>

      <div style={{ marginBottom: "1.75rem" }}>
        <SectionHeader
          title={intl.formatMessage({
            id: "vector.order.samplingSite",
            defaultMessage: "Sampling Site",
          })}
        />
        <FieldLabel
          label={intl.formatMessage({
            id: "vector.admin.samplingSite.name",
            defaultMessage: "Site name or code",
          })}
          required
        />
        {selectedSite ? (
          <SelectedCard onClear={handleClearSite}>
            <h5>
              {selectedSite.code} — {selectedSite.name}
            </h5>
            <p>
              {selectedSite.type && `Type: ${selectedSite.type}`}
              {selectedSite.gpsLatitude &&
                ` · GPS: ${selectedSite.gpsLatitude}, ${selectedSite.gpsLongitude}`}
            </p>
          </SelectedCard>
        ) : (
          <>
            <TextInput
              id="vec-site-search"
              labelText=""
              placeholder={intl.formatMessage({
                id: "vector.order.site.placeholder",
                defaultMessage: "Search by site name or code...",
              })}
              value={siteSearch}
              onChange={(e) => setSiteSearch(e.target.value)}
              disabled={isReadOnly}
            />
            <SearchResults
              results={siteResults}
              onSelect={handleSelectSite}
              renderRow={(s) => (
                <>
                  <strong>{s.code}</strong> — {s.name}
                  {s.type ? (
                    <span
                      style={{
                        color: "#525252",
                        marginLeft: "0.5rem",
                        fontSize: "0.8rem",
                      }}
                    >
                      {s.type}
                    </span>
                  ) : null}
                </>
              )}
            />
          </>
        )}
      </div>

      {/* Collection Context — bionomics capture */}
      <div style={{ marginBottom: "1.75rem" }}>
        <button
          type="button"
          onClick={() => setCollectionContextOpen((v) => !v)}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "0.4rem",
            background: "none",
            border: "none",
            padding: 0,
            cursor: "pointer",
            fontWeight: 600,
            fontSize: "0.875rem",
            color: "#161616",
            marginBottom: collectionContextOpen ? "1rem" : 0,
          }}
          disabled={isReadOnly}
        >
          {collectionContextOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          <FormattedMessage
            id="vector.order.collectionContext"
            defaultMessage="Collection Context (optional — bionomics capture)"
          />
        </button>

        {collectionContextOpen && (
          <div
            style={{
              background: "#f4f4f4",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "1rem",
            }}
          >
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "1rem",
                marginBottom: "1rem",
              }}
            >
              <Select
                id="vec-time-of-day"
                labelText={intl.formatMessage({
                  id: "vector.order.timeOfDay",
                  defaultMessage: "Time of Day",
                })}
                value={orderData?.sampleOrderItems?.environmentalFields?.vecTimeOfDay || ""}
                onChange={(e) => updateEnvField("vecTimeOfDay", e.target.value)}
                disabled={isReadOnly}
              >
                <SelectItem value="" text="" />
                <SelectItem
                  value="dawn"
                  text={intl.formatMessage({ id: "vector.order.timeOfDay.dawn", defaultMessage: "Dawn" })}
                />
                <SelectItem
                  value="day"
                  text={intl.formatMessage({ id: "vector.order.timeOfDay.day", defaultMessage: "Day" })}
                />
                <SelectItem
                  value="dusk"
                  text={intl.formatMessage({ id: "vector.order.timeOfDay.dusk", defaultMessage: "Dusk" })}
                />
                <SelectItem
                  value="night"
                  text={intl.formatMessage({ id: "vector.order.timeOfDay.night", defaultMessage: "Night" })}
                />
              </Select>

              <Select
                id="vec-resting-context"
                labelText={intl.formatMessage({
                  id: "vector.order.restingContext",
                  defaultMessage: "Resting Context",
                })}
                value={orderData?.sampleOrderItems?.environmentalFields?.vecRestingContext || ""}
                onChange={(e) => updateEnvField("vecRestingContext", e.target.value)}
                disabled={isReadOnly}
              >
                <SelectItem value="" text="" />
                <SelectItem
                  value="outdoor"
                  text={intl.formatMessage({ id: "vector.order.restingContext.outdoor", defaultMessage: "Outdoor (exophilic)" })}
                />
                <SelectItem
                  value="indoor"
                  text={intl.formatMessage({ id: "vector.order.restingContext.indoor", defaultMessage: "Indoor (endophilic)" })}
                />
                <SelectItem
                  value="peridomestic"
                  text={intl.formatMessage({ id: "vector.order.restingContext.peridomestic", defaultMessage: "Peridomestic" })}
                />
              </Select>
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <Checkbox
                id="vec-human-biting-catch"
                labelText={
                  <>
                    <strong>
                      <FormattedMessage
                        id="vector.order.humanBitingCatch"
                        defaultMessage="Human-Biting Catch"
                      />
                    </strong>
                    {" — "}
                    <FormattedMessage
                      id="vector.order.humanBitingCatch.hint"
                      defaultMessage="specimen came from a human-landing collection"
                    />
                  </>
                }
                checked={orderData?.sampleOrderItems?.environmentalFields?.vecHumanBitingCatch === "true"}
                onChange={(_, { checked }) => updateEnvField("vecHumanBitingCatch", String(checked))}
                disabled={isReadOnly}
              />
            </div>

            <TextArea
              id="vec-collection-notes"
              labelText={intl.formatMessage({
                id: "vector.order.collectionNotes",
                defaultMessage: "Collection Notes",
              })}
              placeholder={intl.formatMessage({
                id: "vector.order.collectionNotes.placeholder",
                defaultMessage: "Weather, trap conditions, anomalies...",
              })}
              value={orderData?.sampleOrderItems?.environmentalFields?.vecCollectionNotes || ""}
              onChange={(e) => updateEnvField("vecCollectionNotes", e.target.value)}
              disabled={isReadOnly}
              rows={3}
            />
          </div>
        )}
      </div>
    </div>
  );
}

export default VectorSection;
