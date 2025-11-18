import React, { useState, useEffect } from "react";
import { Link, Accordion, AccordionItem } from "@carbon/react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * PrintHistoryDisplay - Display print history for a storage location
 *
 * Features:
 * - Calls GET /rest/storage/{type}/{id}/print-history
 * - Displays "Last printed: [date] [time] by [user]"
 * - Optional "View History" link that expands to show all print records
 *
 * Props:
 * - locationType: string - Location type ('device', 'shelf', 'rack')
 * - locationId: string - Location ID
 */
const PrintHistoryDisplay = ({ locationType, locationId }) => {
  const intl = useIntl();
  const [printHistory, setPrintHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showFullHistory, setShowFullHistory] = useState(false);

  /**
   * Load print history from API
   */
  useEffect(() => {
    if (!locationType || !locationId) {
      setIsLoading(false);
      return;
    }

    const url = `/rest/storage/${locationType}/${locationId}/print-history`;
    getFromOpenElisServer(
      url,
      (response) => {
        if (Array.isArray(response)) {
          setPrintHistory(response);
        } else {
          setPrintHistory([]);
        }
        setIsLoading(false);
      },
      (error) => {
        console.error("Error loading print history:", error);
        setPrintHistory([]);
        setIsLoading(false);
      },
    );
  }, [locationType, locationId]);

  /**
   * Format date and time for display
   */
  const formatDateTime = (dateString) => {
    if (!dateString) {
      return intl.formatMessage({
        id: "label.printHistory.never",
        defaultMessage: "Never",
      });
    }

    try {
      const date = new Date(dateString);
      const formattedDate = intl.formatDate(date, {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
      const formattedTime = intl.formatTime(date, {
        hour: "2-digit",
        minute: "2-digit",
      });
      return `${formattedDate} ${formattedTime}`;
    } catch (e) {
      return dateString;
    }
  };

  /**
   * Get last print record
   */
  const lastPrint =
    printHistory && printHistory.length > 0 ? printHistory[0] : null;

  if (isLoading) {
    return (
      <div data-testid="print-history-loading">
        {intl.formatMessage({
          id: "label.printHistory.loading",
          defaultMessage: "Loading print history...",
        })}
      </div>
    );
  }

  if (!lastPrint) {
    return (
      <div data-testid="print-history-empty">
        {intl.formatMessage({
          id: "label.printHistory.never",
          defaultMessage: "Never printed",
        })}
      </div>
    );
  }

  return (
    <div data-testid="print-history-display">
      <div style={{ marginBottom: "0.5rem" }}>
        {intl.formatMessage(
          {
            id: "label.lastPrinted",
            defaultMessage: "Last printed: {date} {time} by {user}",
          },
          {
            date: formatDateTime(lastPrint.printedDate).split(" ")[0],
            time: formatDateTime(lastPrint.printedDate).split(" ")[1],
            user:
              lastPrint.printedBy ||
              intl.formatMessage({
                id: "label.printHistory.unknownUser",
                defaultMessage: "Unknown",
              }),
          },
        )}
      </div>
      {printHistory.length > 1 && (
        <Accordion>
          <AccordionItem
            title={intl.formatMessage({
              id: "label.printHistory.viewAll",
              defaultMessage: "View History",
            })}
            open={showFullHistory}
            onHeadingClick={() => setShowFullHistory(!showFullHistory)}
            data-testid="print-history-accordion"
          >
            <div style={{ padding: "1rem 0" }}>
              {printHistory.map((record, index) => (
                <div
                  key={index}
                  style={{
                    padding: "0.5rem 0",
                    borderBottom:
                      index < printHistory.length - 1
                        ? "1px solid var(--cds-border-subtle-01)"
                        : "none",
                  }}
                >
                  <div style={{ fontWeight: "bold" }}>
                    {formatDateTime(record.printedDate)}
                  </div>
                  <div
                    style={{
                      fontSize: "0.875rem",
                      color: "var(--cds-text-secondary)",
                    }}
                  >
                    {intl.formatMessage(
                      {
                        id: "label.printHistory.byUser",
                        defaultMessage: "By {user}",
                      },
                      {
                        user:
                          record.printedBy ||
                          intl.formatMessage({
                            id: "label.printHistory.unknownUser",
                            defaultMessage: "Unknown",
                          }),
                      },
                    )}
                    {record.printCount > 1 && (
                      <span>
                        {" "}
                        (
                        {intl.formatMessage(
                          {
                            id: "label.printHistory.count",
                            defaultMessage: "{count} labels",
                          },
                          { count: record.printCount },
                        )}
                        )
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </AccordionItem>
        </Accordion>
      )}
    </div>
  );
};

PrintHistoryDisplay.propTypes = {
  locationType: PropTypes.oneOf(["device", "shelf", "rack"]).isRequired,
  locationId: PropTypes.string.isRequired,
};

export default PrintHistoryDisplay;
