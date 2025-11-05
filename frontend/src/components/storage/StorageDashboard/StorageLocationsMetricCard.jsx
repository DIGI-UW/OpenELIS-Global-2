import React, { useState, useEffect } from "react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import "./StorageLocationsMetricCard.css";

/**
 * StorageLocationsMetricCard component
 * Displays formatted breakdown of location counts by type with color-coding.
 * Per FR-057 and FR-057a:
 * - Shows "X rooms, Y devices, Z shelves, W racks" format
 * - Color-coded using Carbon Design System tokens:
 *   - Rooms: blue-70
 *   - Devices: teal-70
 *   - Shelves: purple-70
 *   - Racks: orange-70
 * - Only displays active locations (inactive excluded)
 */
const StorageLocationsMetricCard = () => {
  const [counts, setCounts] = useState({
    rooms: 0,
    devices: 0,
    shelves: 0,
    racks: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadLocationCounts();
  }, []);

  const loadLocationCounts = () => {
    getFromOpenElisServer(
      "/rest/storage/dashboard/location-counts",
      (response) => {
        if (response) {
          const newCounts = {
            rooms: response.rooms || 0,
            devices: response.devices || 0,
            shelves: response.shelves || 0,
            racks: response.racks || 0,
          };
          setCounts(newCounts);
          // Debug logging to catch 0 counts issue
          if (
            newCounts.rooms === 0 &&
            newCounts.devices === 0 &&
            newCounts.shelves === 0 &&
            newCounts.racks === 0
          ) {
            console.warn(
              "StorageLocationsMetricCard: All counts are 0. API response:",
              response,
            );
          }
        } else {
          console.error("StorageLocationsMetricCard: No response from API");
        }
        setLoading(false);
      },
    );
  };

  if (loading) {
    return (
      <div className="storage-locations-metric-card">
        <h3>
          <FormattedMessage id="storage.metrics.storage.locations" />
        </h3>
        <p className="metric-value">Loading...</p>
      </div>
    );
  }

  // Format breakdown text with color-coding and pills
  const formatBreakdown = () => {
    const parts = [];

    // Always include all location types, even if count is 0
    parts.push(
      <span key="rooms" className="location-count-pill location-count-rooms">
        <span className="location-count-number">{counts.rooms}</span>
        <span className="location-count-label">
          <FormattedMessage
            id="storage.location.type.rooms"
            defaultMessage="rooms"
          />
        </span>
      </span>,
    );

    parts.push(
      <span
        key="devices"
        className="location-count-pill location-count-devices"
      >
        <span className="location-count-number">{counts.devices}</span>
        <span className="location-count-label">
          <FormattedMessage
            id="storage.location.type.devices"
            defaultMessage="devices"
          />
        </span>
      </span>,
    );

    parts.push(
      <span
        key="shelves"
        className="location-count-pill location-count-shelves"
      >
        <span className="location-count-number">{counts.shelves}</span>
        <span className="location-count-label">
          <FormattedMessage
            id="storage.location.type.shelves"
            defaultMessage="shelves"
          />
        </span>
      </span>,
    );

    parts.push(
      <span key="racks" className="location-count-pill location-count-racks">
        <span className="location-count-number">{counts.racks}</span>
        <span className="location-count-label">
          <FormattedMessage
            id="storage.location.type.racks"
            defaultMessage="racks"
          />
        </span>
      </span>,
    );

    return parts;
  };

  return (
    <div className="storage-locations-metric-card">
      <h3>
        <FormattedMessage id="storage.metrics.storage.locations" />
      </h3>
      <div className="location-counts-breakdown">{formatBreakdown()}</div>
    </div>
  );
};

export default StorageLocationsMetricCard;
