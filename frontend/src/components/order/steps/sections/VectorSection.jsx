import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  TextInput,
  Button,
  Tag,
  Link,
  Select,
  SelectItem,
  Checkbox,
  TextArea,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { ChevronDown, ChevronUp } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { useOrderContext } from "../../OrderContext";

const todayIso = () => {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
};

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

function VectorSection({ orderData, setOrderData, isReadOnly, workflowType }) {
  const intl = useIntl();
  const { samples, setSamples } = useOrderContext();

  const isEnv = workflowType === "environmental";

  // Key names must match what SamplePatientUpdateData.addEnvironmentalObservations()
  // reads from the environmentalFields map on the backend.
  const SITE_ID_KEY = isEnv ? "samplingSiteId" : "vecCollectionSiteId";
  const SITE_NAME_KEY = isEnv ? "samplingSiteName" : "vecCollectionSiteName";
  const SITE_CODE_KEY = isEnv ? "samplingSiteCode" : "vecCollectionSiteCode";
  const SITE_TYPE_KEY = isEnv ? "siteType" : "vecCollectionSiteType";
  const SITE_SUBTYPE_KEY = isEnv ? "siteSubtype" : "vecCollectionSiteSubtype";
  const SITE_ZONE_KEY = isEnv ? "environmentalZone" : "vecCollectionSiteZone";
  const SITE_CONTACT_KEY = isEnv
    ? "samplingSiteContact"
    : "vecCollectionSiteContact";
  const SITE_PHONE_KEY = isEnv ? "samplingSitePhone" : "vecCollectionSitePhone";
  const SITE_DESC_KEY = isEnv
    ? "samplingSiteDesc"
    : "vecCollectionSiteDescription";
  const GPS_LAT_KEY = isEnv ? "samplingSiteGpsLat" : "vecGpsLatitude";
  const GPS_LON_KEY = isEnv ? "samplingSiteGpsLon" : "vecGpsLongitude";
  const COLLECTION_DATE_KEY = isEnv ? "envCollectionDate" : "vecCollectionDate";

  const [sites, setSites] = useState([]);
  const [siteSearch, setSiteSearch] = useState("");
  const [siteResults, setSiteResults] = useState([]);
  const [selectedSite, setSelectedSite] = useState(null);
  const [collectionContextOpen, setCollectionContextOpen] = useState(false);

  const initialCollectionDate =
    orderData?.sampleOrderItems?.environmentalFields?.[COLLECTION_DATE_KEY] ||
    samples?.[0]?.collectionDate ||
    todayIso();
  const [collectionDate, setCollectionDate] = useState(initialCollectionDate);

  const handleCollectionDateChange = useCallback(
    (isoDate) => {
      if (!isoDate) return;
      setCollectionDate(isoDate);
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          environmentalFields: {
            ...prev.sampleOrderItems?.environmentalFields,
            [COLLECTION_DATE_KEY]: isoDate,
          },
        },
      }));
      setSamples(
        (samples || []).map((s) => ({ ...s, collectionDate: isoDate })),
      );
    },
    [samples, setSamples, setOrderData, COLLECTION_DATE_KEY],
  );

  useEffect(() => {
    if (!samples || samples.length === 0) return;
    if (samples.every((s) => s.collectionDate)) return;
    setSamples(
      samples.map((s) =>
        s.collectionDate ? s : { ...s, collectionDate: collectionDate },
      ),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [samples.length]);

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

  // Restore selected site when editing an existing order
  useEffect(() => {
    const envFields = orderData?.sampleOrderItems?.environmentalFields;
    if (!envFields?.[SITE_ID_KEY] || selectedSite) return;
    if (sites.length > 0) {
      const match = sites.find(
        (s) => String(s.id) === String(envFields[SITE_ID_KEY]),
      );
      if (match) {
        setSelectedSite(match);
        setOrderData((prev) => ({
          ...prev,
          sampleOrderItems: {
            ...prev.sampleOrderItems,
            environmentalFields: {
              ...prev.sampleOrderItems?.environmentalFields,
              [SITE_CODE_KEY]: match.code || "",
              [SITE_TYPE_KEY]: match.type || "",
              [SITE_SUBTYPE_KEY]: match.subtype || "",
              [SITE_ZONE_KEY]: match.environmentalZone || "",
              [SITE_CONTACT_KEY]: match.contactName || "",
              [SITE_PHONE_KEY]: match.contactPhone || "",
              [SITE_DESC_KEY]: match.description || "",
              [GPS_LAT_KEY]: match.gpsLatitude || "",
              [GPS_LON_KEY]: match.gpsLongitude || "",
            },
          },
        }));
        return;
      }
    }
    if (envFields[SITE_NAME_KEY]) {
      setSelectedSite({
        id: envFields[SITE_ID_KEY],
        name: envFields[SITE_NAME_KEY],
        code: envFields[SITE_CODE_KEY] || "",
        gpsLatitude: envFields[GPS_LAT_KEY] || "",
        gpsLongitude: envFields[GPS_LON_KEY] || "",
      });
    }
  }, [orderData?.sampleOrderItems?.environmentalFields, sites]);

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
          [SITE_ID_KEY]: String(site.id),
          [SITE_NAME_KEY]: site.name,
          [SITE_CODE_KEY]: site.code || "",
          [SITE_TYPE_KEY]: site.type || "",
          [SITE_SUBTYPE_KEY]: site.subtype || "",
          [SITE_ZONE_KEY]: site.environmentalZone || "",
          [SITE_CONTACT_KEY]: site.contactName || "",
          [SITE_PHONE_KEY]: site.contactPhone || "",
          [SITE_DESC_KEY]: site.description || "",
          [GPS_LAT_KEY]: site.gpsLatitude || "",
          [GPS_LON_KEY]: site.gpsLongitude || "",
        },
      },
    }));
  };

  const handleClearSite = () => {
    setSelectedSite(null);
    setSiteSearch("");
    setSiteResults([]);
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          [SITE_ID_KEY]: "",
          [SITE_NAME_KEY]: "",
          [SITE_CODE_KEY]: "",
          [SITE_TYPE_KEY]: "",
          [SITE_SUBTYPE_KEY]: "",
          [SITE_ZONE_KEY]: "",
          [SITE_CONTACT_KEY]: "",
          [SITE_PHONE_KEY]: "",
          [SITE_DESC_KEY]: "",
          [GPS_LAT_KEY]: "",
          [GPS_LON_KEY]: "",
        },
      },
    }));
  };

  return (
    <div style={{ maxWidth: "720px" }}>
      {workflowType !== "environmental" && (
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
      )}

      {/* Collection Date — vector skips the Collect step, so we capture it
          here next to the rest of the collection metadata. Defaults to today;
          adjust if the lot is being entered retroactively. Propagates onto
          every sample via handleCollectionDateChange. */}
      <div style={{ marginBottom: "1.75rem" }}>
        <SectionHeader
          title={intl.formatMessage({
            id: "vector.order.collectionDate.heading",
            defaultMessage: "Collection Date",
          })}
        />
        <div style={{ maxWidth: "240px" }}>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            value={collectionDate}
            maxDate={todayIso()}
            onChange={(dates) => {
              if (!dates || dates.length === 0) return;
              const d = dates[0];
              const yyyy = d.getFullYear();
              const mm = String(d.getMonth() + 1).padStart(2, "0");
              const dd = String(d.getDate()).padStart(2, "0");
              handleCollectionDateChange(`${yyyy}-${mm}-${dd}`);
            }}
          >
            <DatePickerInput
              id="vec-collection-date"
              labelText=""
              placeholder="YYYY-MM-DD"
              disabled={isReadOnly}
            />
          </DatePicker>
        </div>
        <p
          style={{
            fontSize: "0.75rem",
            color: "#525252",
            marginTop: "0.25rem",
          }}
        >
          <FormattedMessage
            id="vector.order.collectionDate.helper"
            defaultMessage="Date the lot was physically collected from the field. Applies to every sample on this order."
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

      {/* Collection Context — bionomics capture (vector only) */}
      {workflowType !== "environmental" && (
        <div style={{ marginBottom: "1.75rem" }}>
          <button
            type="button"
            onClick={() => setCollectionContextOpen((v) => !v)}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              width: "100%",
              background: collectionContextOpen ? "#e8f5e9" : "#f4f4f4",
              border: "1px solid #c6e6c8",
              borderLeft: "4px solid #24a148",
              borderRadius: "4px",
              padding: "0.625rem 0.875rem",
              cursor: "pointer",
              fontWeight: 600,
              fontSize: "0.875rem",
              color: "#161616",
              marginBottom: collectionContextOpen ? "0.75rem" : 0,
              transition: "background 0.15s",
            }}
            disabled={isReadOnly}
          >
            {collectionContextOpen ? (
              <ChevronUp size={16} />
            ) : (
              <ChevronDown size={16} />
            )}
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
                  value={
                    orderData?.sampleOrderItems?.environmentalFields
                      ?.vecTimeOfDay || ""
                  }
                  onChange={(e) =>
                    updateEnvField("vecTimeOfDay", e.target.value)
                  }
                  disabled={isReadOnly}
                >
                  <SelectItem value="" text="" />
                  <SelectItem
                    value="dawn"
                    text={intl.formatMessage({
                      id: "vector.order.timeOfDay.dawn",
                      defaultMessage: "Dawn",
                    })}
                  />
                  <SelectItem
                    value="day"
                    text={intl.formatMessage({
                      id: "vector.order.timeOfDay.day",
                      defaultMessage: "Day",
                    })}
                  />
                  <SelectItem
                    value="dusk"
                    text={intl.formatMessage({
                      id: "vector.order.timeOfDay.dusk",
                      defaultMessage: "Dusk",
                    })}
                  />
                  <SelectItem
                    value="night"
                    text={intl.formatMessage({
                      id: "vector.order.timeOfDay.night",
                      defaultMessage: "Night",
                    })}
                  />
                </Select>

                <Select
                  id="vec-resting-context"
                  labelText={intl.formatMessage({
                    id: "vector.order.restingContext",
                    defaultMessage: "Resting Context",
                  })}
                  value={
                    orderData?.sampleOrderItems?.environmentalFields
                      ?.vecRestingContext || ""
                  }
                  onChange={(e) =>
                    updateEnvField("vecRestingContext", e.target.value)
                  }
                  disabled={isReadOnly}
                >
                  <SelectItem value="" text="" />
                  <SelectItem
                    value="outdoor"
                    text={intl.formatMessage({
                      id: "vector.order.restingContext.outdoor",
                      defaultMessage: "Outdoor (exophilic)",
                    })}
                  />
                  <SelectItem
                    value="indoor"
                    text={intl.formatMessage({
                      id: "vector.order.restingContext.indoor",
                      defaultMessage: "Indoor (endophilic)",
                    })}
                  />
                  <SelectItem
                    value="peridomestic"
                    text={intl.formatMessage({
                      id: "vector.order.restingContext.peridomestic",
                      defaultMessage: "Peridomestic",
                    })}
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
                  checked={
                    orderData?.sampleOrderItems?.environmentalFields
                      ?.vecHumanBitingCatch === "true"
                  }
                  onChange={(_, { checked }) =>
                    updateEnvField("vecHumanBitingCatch", String(checked))
                  }
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
                value={
                  orderData?.sampleOrderItems?.environmentalFields
                    ?.vecCollectionNotes || ""
                }
                onChange={(e) =>
                  updateEnvField("vecCollectionNotes", e.target.value)
                }
                disabled={isReadOnly}
                rows={3}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default VectorSection;
