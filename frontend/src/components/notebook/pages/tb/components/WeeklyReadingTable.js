import React from "react";
import {
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Tag,
  Button,
} from "@carbon/react";
import { Add, Checkmark, Warning, Close } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Growth observation display config.
 */
const OBSERVATION_CONFIG = {
  NO_GROWTH: {
    type: "gray",
    text: "No Growth",
    icon: null,
  },
  GROWTH_DETECTED: {
    type: "red",
    text: "Growth Detected",
    icon: Warning,
  },
  CONTAMINATED: {
    type: "magenta",
    text: "Contaminated",
    icon: Close,
  },
};

/**
 * WeeklyReadingTable - Displays weekly culture readings for a sample.
 * Used inside expanded rows in the Incubation Monitoring page.
 *
 * @param {Array} readings - Array of reading objects for this sample
 * @param {number} currentWeek - Current incubation week
 * @param {function} onAddReading - Handler for adding a new reading
 * @param {function} onMarkPositive - Handler for marking culture positive
 * @param {function} onMarkNegative - Handler for marking culture negative
 * @param {string} cultureResult - Current culture result (if finalized)
 */
function WeeklyReadingTable({
  readings = [],
  currentWeek = 1,
  onAddReading,
  onMarkPositive,
  onMarkNegative,
  cultureResult = null,
}) {
  const intl = useIntl();

  // Sort readings by week number
  const sortedReadings = [...readings].sort(
    (a, b) => (a.weekNumber || 0) - (b.weekNumber || 0),
  );

  // Check if we have a growth detected reading
  const hasGrowth = readings.some(
    (r) => r.growthObservation === "GROWTH_DETECTED",
  );

  // Check if we've reached week 8 with no growth
  const hasWeek8NoGrowth = readings.some(
    (r) => r.weekNumber === 8 && r.growthObservation === "NO_GROWTH",
  );

  // Determine which weeks are missing
  const recordedWeeks = readings.map((r) => r.weekNumber);
  const missingWeeks = [];
  for (let w = 1; w <= Math.min(currentWeek, 8); w++) {
    if (!recordedWeeks.includes(w)) {
      missingWeeks.push(w);
    }
  }

  const renderObservation = (observation) => {
    const config = OBSERVATION_CONFIG[observation] || {
      type: "gray",
      text: observation,
    };
    const IconComponent = config.icon;

    return (
      <Tag type={config.type} size="sm">
        {IconComponent && (
          <IconComponent size={12} style={{ marginRight: "4px" }} />
        )}
        {config.text}
      </Tag>
    );
  };

  return (
    <div style={{ padding: "1rem" }}>
      {/* Readings Table */}
      <StructuredListWrapper isCondensed>
        <StructuredListHead>
          <StructuredListRow head>
            <StructuredListCell head style={{ width: "60px" }}>
              <FormattedMessage
                id="notebook.tb.incubation.week"
                defaultMessage="Week"
              />
            </StructuredListCell>
            <StructuredListCell head style={{ width: "120px" }}>
              <FormattedMessage
                id="notebook.tb.incubation.date"
                defaultMessage="Date"
              />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage
                id="notebook.tb.incubation.observation"
                defaultMessage="Observation"
              />
            </StructuredListCell>
            <StructuredListCell head style={{ width: "100px" }}>
              <FormattedMessage
                id="notebook.tb.incubation.readBy"
                defaultMessage="Read By"
              />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage
                id="notebook.tb.incubation.notes"
                defaultMessage="Notes"
              />
            </StructuredListCell>
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          {sortedReadings.length === 0 ? (
            <StructuredListRow>
              <StructuredListCell colSpan={5}>
                <span style={{ color: "#8d8d8d", fontStyle: "italic" }}>
                  <FormattedMessage
                    id="notebook.tb.incubation.noReadings"
                    defaultMessage="No readings recorded yet"
                  />
                </span>
              </StructuredListCell>
            </StructuredListRow>
          ) : (
            sortedReadings.map((reading, index) => (
              <StructuredListRow key={reading.id || index}>
                <StructuredListCell>
                  <strong>{reading.weekNumber}</strong>
                </StructuredListCell>
                <StructuredListCell>
                  {reading.readingDate
                    ? new Date(reading.readingDate).toLocaleDateString()
                    : "-"}
                </StructuredListCell>
                <StructuredListCell>
                  {renderObservation(reading.growthObservation)}
                </StructuredListCell>
                <StructuredListCell>
                  {reading.readBy?.loginName || reading.readByInitials || "-"}
                </StructuredListCell>
                <StructuredListCell>{reading.notes || "-"}</StructuredListCell>
              </StructuredListRow>
            ))
          )}
        </StructuredListBody>
      </StructuredListWrapper>

      {/* Action Buttons */}
      {!cultureResult && (
        <div
          style={{
            marginTop: "1rem",
            display: "flex",
            gap: "0.5rem",
            flexWrap: "wrap",
          }}
        >
          {/* Add Reading Button */}
          <Button
            kind="primary"
            size="sm"
            renderIcon={Add}
            onClick={onAddReading}
          >
            <FormattedMessage
              id="notebook.tb.incubation.addReading"
              defaultMessage="Add Reading"
            />
          </Button>

          {/* Mark Positive Button (if growth detected) */}
          {hasGrowth && (
            <Button
              kind="danger"
              size="sm"
              renderIcon={Warning}
              onClick={onMarkPositive}
            >
              <FormattedMessage
                id="notebook.tb.incubation.markPositive"
                defaultMessage="Mark as Positive"
              />
            </Button>
          )}

          {/* Mark Negative Button (if week 8 with no growth) */}
          {hasWeek8NoGrowth && !hasGrowth && (
            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={onMarkNegative}
            >
              <FormattedMessage
                id="notebook.tb.incubation.markNegative"
                defaultMessage="Mark as Negative"
              />
            </Button>
          )}
        </div>
      )}

      {/* Culture Result Badge */}
      {cultureResult && (
        <div
          style={{
            marginTop: "1rem",
            padding: "0.75rem",
            backgroundColor:
              cultureResult === "POSITIVE"
                ? "#fff1f1"
                : cultureResult === "NEGATIVE"
                  ? "#defbe6"
                  : "#fff8e1",
            borderRadius: "4px",
            borderLeft: `4px solid ${
              cultureResult === "POSITIVE"
                ? "#da1e28"
                : cultureResult === "NEGATIVE"
                  ? "#24a148"
                  : "#f1c21b"
            }`,
          }}
        >
          <strong>
            <FormattedMessage
              id="notebook.tb.incubation.finalResult"
              defaultMessage="Final Result:"
            />
          </strong>{" "}
          <Tag
            type={
              cultureResult === "POSITIVE"
                ? "red"
                : cultureResult === "NEGATIVE"
                  ? "green"
                  : "orange"
            }
            size="sm"
          >
            {cultureResult}
          </Tag>
        </div>
      )}

      {/* Missing Weeks Warning */}
      {missingWeeks.length > 0 && !cultureResult && (
        <div
          style={{
            marginTop: "0.75rem",
            padding: "0.5rem",
            backgroundColor: "#fff8e1",
            borderRadius: "4px",
            fontSize: "0.875rem",
            color: "#6a5103",
          }}
        >
          <Warning size={16} style={{ marginRight: "0.5rem" }} />
          <FormattedMessage
            id="notebook.tb.incubation.missingWeeks"
            defaultMessage="Missing readings for week(s): {weeks}"
            values={{ weeks: missingWeeks.join(", ") }}
          />
        </div>
      )}
    </div>
  );
}

export default WeeklyReadingTable;
