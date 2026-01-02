import React, { useState, useEffect } from "react";
import {
  DataTable,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  ButtonSet,
  TextInput,
  DatePicker,
  DatePickerInput,
  Loading,
  InlineNotification,
  Pagination,
  OverflowMenu,
  OverflowMenuItem,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Document } from "@carbon/icons-react";
import CartridgeUsageAPI from "./EquipmentUsageService";

/**
 * EquipmentUsageHistory Component
 *
 * Displays historical records of equipment/cartridge usage.
 * Features:
 * - View all usage records
 * - Filter by cartridge item, date range, user
 * - Download/Export to CSV
 * - View detailed usage records
 * - Pagination for large datasets
 */
const EquipmentUsageHistory = () => {
  const intl = useIntl();

  // State Management
  const [usageRecords, setUsageRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filter State
  const [filters, setFilters] = useState({
    equipment: "",
    startDate: "",
    endDate: "",
  });

  // Pagination State
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Load Usage History
  useEffect(() => {
    const fetchUsageHistory = async () => {
      setLoading(true);
      setError(null);

      try {
        // Fetch all usage records from inventory
        CartridgeUsageAPI.getUsageHistory(null, (data) => {
          if (data && Array.isArray(data)) {
            // Filter by date range if specified
            let filtered = data;

            if (filters.startDate) {
              const startDate = new Date(filters.startDate);
              filtered = filtered.filter((record) => {
                const recordDate = new Date(record.usageDate);
                return recordDate >= startDate;
              });
            }

            if (filters.endDate) {
              const endDate = new Date(filters.endDate);
              endDate.setHours(23, 59, 59, 999);
              filtered = filtered.filter((record) => {
                const recordDate = new Date(record.usageDate);
                return recordDate <= endDate;
              });
            }

            setUsageRecords(filtered);
          } else {
            setError(
              intl.formatMessage({ id: "equipment.usage.error.loadFailed" })
            );
          }
          setLoading(false);
        });
      } catch (error) {
        console.error("Error fetching usage history:", error);
        setError(
          intl.formatMessage({ id: "equipment.usage.error.loadFailed" })
        );
        setLoading(false);
      }
    };

    fetchUsageHistory();
  }, [filters, intl]);

  // Handle Filter Changes
  const handleFilterChange = (field, value) => {
    setFilters((prev) => ({
      ...prev,
      [field]: value,
    }));
    setCurrentPage(1); // Reset to first page on filter change
  };

  // Export to CSV
  const handleExportCSV = () => {
    if (!usageRecords || usageRecords.length === 0) {
      setError(
        intl.formatMessage({ id: "equipment.usage.error.noRecords" })
      );
      return;
    }

    // Prepare CSV data
    const headers = [
      "Date",
      "Equipment ID",
      "Lot Number",
      "Quantity Used",
      "User",
      "Test Result ID",
    ];
    const rows = usageRecords.map((record) => [
      new Date(record.usageDate).toLocaleDateString(),
      record.inventoryItem?.id || "",
      record.lot?.lotNumber || "",
      record.quantityUsed || "",
      record.performedByUser || "",
      record.testResultId || "",
    ]);

    // Create CSV content
    const csvContent = [
      headers.join(","),
      ...rows.map((row) => row.map((cell) => `"${cell}"`).join(",")),
    ].join("\n");

    // Create and download file
    const blob = new Blob([csvContent], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `equipment-usage-${new Date().toISOString().split("T")[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  // Pagination Calculations
  const totalPages = Math.ceil(usageRecords.length / pageSize);
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedRecords = usageRecords.slice(startIndex, endIndex);

  return (
    <div className="equipmentUsageHistoryContainer">
      <h3>
        <FormattedMessage id="equipment.usage.history" />
      </h3>

      {error && (
        <InlineNotification
          kind="error"
          title="Error"
          subtitle={error}
          onClose={() => setError(null)}
        />
      )}

      {/* Filters */}
      <div className="filtersSection">
        <div className="filterRow">
          <DatePickerInput
            labelText={intl.formatMessage({
              id: "equipment.usage.filter.startDate",
              defaultMessage: "From Date",
            })}
            placeholder="mm/dd/yyyy"
            value={filters.startDate}
            onChange={(e) =>
              handleFilterChange("startDate", e.target.value)
            }
          />
          <DatePickerInput
            labelText={intl.formatMessage({
              id: "equipment.usage.filter.endDate",
              defaultMessage: "To Date",
            })}
            placeholder="mm/dd/yyyy"
            value={filters.endDate}
            onChange={(e) =>
              handleFilterChange("endDate", e.target.value)
            }
          />
          <Button
            kind="ghost"
            size="sm"
            onClick={() => {
              setFilters({ equipment: "", startDate: "", endDate: "" });
              setCurrentPage(1);
            }}
          >
            <FormattedMessage id="common.clearFilters" />
          </Button>
        </div>
      </div>

      {/* Export Button */}
      <div className="exportSection">
        <Button kind="secondary" size="sm" onClick={handleExportCSV}>
          <FormattedMessage id="common.export" />
        </Button>
      </div>

      {/* Usage History Table */}
      {loading ? (
        <Loading description="Loading usage history..." />
      ) : usageRecords.length === 0 ? (
        <div className="noRecordsMessage">
          <p>
            <FormattedMessage id="equipment.usage.noHistory" />
          </p>
        </div>
      ) : (
        <>
          <div className="tableWrapper">
            <table className="historyTable">
              <thead>
                <tr>
                  <th>
                    <FormattedMessage id="equipment.usage.history.date" />
                  </th>
                  <th>
                    <FormattedMessage id="equipment.usage.history.equipment" />
                  </th>
                  <th>
                    <FormattedMessage id="equipment.usage.history.lot" />
                  </th>
                  <th>
                    <FormattedMessage id="equipment.usage.history.quantity" />
                  </th>
                  <th>
                    <FormattedMessage id="equipment.usage.history.user" />
                  </th>
                  <th>
                    <FormattedMessage id="equipment.usage.history.testResult" />
                  </th>
                  <th>
                    <FormattedMessage id="common.actions" />
                  </th>
                </tr>
              </thead>
              <tbody>
                {paginatedRecords.map((record, index) => (
                  <tr key={record.id || index}>
                    <td>
                      {record.usageDate
                        ? new Date(record.usageDate).toLocaleDateString()
                        : "—"}
                    </td>
                    <td>
                      {record.inventoryItem?.name || "—"}
                    </td>
                    <td>
                      {record.lot?.lotNumber || "—"}
                    </td>
                    <td>
                      {record.quantityUsed || "—"}
                    </td>
                    <td>
                      {record.performedByUser || "—"}
                    </td>
                    <td>
                      {record.testResultId || "—"}
                    </td>
                    <td>
                      <OverflowMenu
                        flipped
                        ariaLabel="Usage record actions"
                      >
                        <OverflowMenuItem
                          itemText={intl.formatMessage({
                            id: "common.view",
                            defaultMessage: "View",
                          })}
                          onClick={() => {
                            // View record details
                            console.log("View record:", record);
                          }}
                        />
                        <OverflowMenuItem
                          itemText={intl.formatMessage({
                            id: "common.delete",
                            defaultMessage: "Delete",
                          })}
                          isDelete
                          onClick={() => {
                            // Delete record
                            console.log("Delete record:", record);
                          }}
                        />
                      </OverflowMenu>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="paginationSection">
              <Pagination
                backwardText={intl.formatMessage({
                  id: "common.previous",
                  defaultMessage: "Previous",
                })}
                forwardText={intl.formatMessage({
                  id: "common.next",
                  defaultMessage: "Next",
                })}
                itemsShownText={intl.formatMessage({
                  id: "common.itemsShown",
                  defaultMessage: "Items shown",
                })}
                pageNumberText={intl.formatMessage({
                  id: "common.pageNumber",
                  defaultMessage: "Page number",
                })}
                pageSize={pageSize}
                pageSizes={[10, 20, 50]}
                totalItems={usageRecords.length}
                onChange={({ pageSize: newPageSize, page }) => {
                  setPageSize(newPageSize);
                  setCurrentPage(page);
                }}
                page={currentPage}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default EquipmentUsageHistory;
