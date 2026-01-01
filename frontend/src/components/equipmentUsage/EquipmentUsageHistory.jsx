import React, { useState, useEffect } from "react";
import {
  Button,
  DataTable,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Grid,
  Column,
  Loading,
  InlineNotification,
  Stack,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Section,
  Heading,
} from "@carbon/react";
import { Search, Reset, Filter } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import EquipmentUsageService from "./EquipmentUsageService";
import "./EquipmentUsage.css";

// Utility functions for formatting status values
const formatEquipmentStatus = (status) => {
  const statusMap = {
    FUNCTIONAL: "Functional",
    UNDER_MAINTENANCE: "Under Maintenance",
    FAULTY: "Faulty",
    CALIBRATION_REQUIRED: "Calibration Required"
  };
  return statusMap[status] || status;
};

const formatEntryStatus = (status) => {
  const statusMap = {
    DRAFT: "Draft",
    SUBMITTED: "Submitted for Review",
    APPROVED: "Approved"
  };
  return statusMap[status] || status;
};

const EquipmentUsageHistory = ({ refreshTrigger }) => {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [equipment, setEquipment] = useState([]);
  const [filters, setFilters] = useState({
    equipment: "",
    status: "",
    startDate: "",
    endDate: "",
  });
  const [isMounted, setIsMounted] = useState(true);

  useEffect(() => {
    setIsMounted(true);
    loadEquipment();
    loadEntries();

    // Cleanup function to prevent memory leaks
    return () => {
      setIsMounted(false);
    };
  }, [refreshTrigger]);

  const loadEquipment = async () => {
    if (!isMounted) return;

    try {
      const data = await EquipmentUsageService.getEquipmentForDropdown();

      if (isMounted) {
        setEquipment(data || []);
      }
    } catch (err) {
      console.error("Failed to load equipment:", err);
      if (isMounted) {
        setError(`Failed to load equipment: ${err.message}`);
      }
    }
  };

  const loadEntries = async () => {
    if (!isMounted) return;

    setLoading(true);
    setError(null);
    try {
      const data = await EquipmentUsageService.getAllUsageEntries();
      const transformedData = Array.isArray(data)
        ? data.map((item) => ({
            ...item,
            id: String(item.id), // Convert ID to string for DataTable
          }))
        : [];

      if (isMounted) {
        setEntries(transformedData);
      }
    } catch (err) {
      if (isMounted) {
        setError(err.message);
      }
    } finally {
      if (isMounted) {
        setLoading(false);
      }
    }
  };

  const handleSearch = async () => {
    if (!isMounted) return;

    setLoading(true);
    setError(null);
    try {
      const searchFilters = {
        equipmentId: filters.equipment ? parseInt(filters.equipment) : null,
        status: filters.status || null,
        startDate: filters.startDate || null,
        endDate: filters.endDate || null,
      };
      const data = await EquipmentUsageService.searchUsageEntries(searchFilters);
      const transformedData = Array.isArray(data)
        ? data.map((item) => ({
            ...item,
            id: String(item.id), // Convert ID to string for DataTable
          }))
        : [];

      if (isMounted) {
        setEntries(transformedData);
      }
    } catch (err) {
      if (isMounted) {
        setError(err.message);
      }
    } finally {
      if (isMounted) {
        setLoading(false);
      }
    }
  };

  const handleFilterChange = (filterName, value) => {
    setFilters((prev) => ({
      ...prev,
      [filterName]: value,
    }));
  };

  const handleClearFilters = () => {
    setFilters({
      equipment: "",
      status: "",
      startDate: "",
      endDate: "",
    });
    loadEntries();
  };

  if (loading && entries.length === 0) {
    return <Loading />;
  }

  return (
    <div className="equipment-usage-container">
      {/* Header Section */}
      <Section>
        <Heading className="cds--margin-bottom-06">
          <FormattedMessage
            id="equipment.usage.history.title"
            defaultMessage="Equipment Usage History"
          />
        </Heading>

        {error && (
          <InlineNotification
            title={<FormattedMessage id="common.error" defaultMessage="Error" />}
            subtitle={error}
            kind="error"
            onClose={() => setError(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Filter Section */}
        <div className="filter-section cds--subgrid" style={{ marginBottom: "2rem", padding: "1.5rem", backgroundColor: "#f9f9f9", borderRadius: "8px" }}>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Heading size="sm" className="cds--margin-bottom-05">
                <Filter size={20} style={{ marginRight: "0.5rem", verticalAlign: "text-bottom" }} />
                <FormattedMessage id="common.filters" defaultMessage="Filters" />
              </Heading>
            </Column>
          </Grid>

          <Grid fullWidth={true} style={{ marginBottom: "1rem" }}>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="equipment-filter"
                labelText={<FormattedMessage id="equipment.label" defaultMessage="Equipment" />}
                placeholder="Select equipment..."
                value={filters.equipment}
                onChange={(e) => handleFilterChange("equipment", e.target.value)}
              >
                <SelectItem
                  value=""
                  text="All Equipment"
                />
                {equipment.map((eq) => (
                  <SelectItem key={eq.id} value={eq.id.toString()} text={eq.name} />
                ))}
              </Select>
            </Column>

            <Column lg={4} md={4} sm={4}>
              <Select
                id="status-filter"
                labelText={<FormattedMessage id="equipment.usage.status" defaultMessage="Entry Status" />}
                placeholder="Select status..."
                value={filters.status}
                onChange={(e) => handleFilterChange("status", e.target.value)}
              >
                <SelectItem
                  value=""
                  text="All Statuses"
                />
                <SelectItem
                  value="DRAFT"
                  text="Draft"
                />
                <SelectItem
                  value="SUBMITTED"
                  text="Submitted for Review"
                />
                <SelectItem
                  value="APPROVED"
                  text="Approved"
                />
              </Select>
            </Column>

            <Column lg={4} md={4} sm={4}>
              <DatePicker
                datePickerType="range"
                value={filters.startDate && filters.endDate ? `${filters.startDate}/${filters.endDate}` : ""}
                onChange={(dates) => {
                  if (dates && dates.length === 2) {
                    handleFilterChange("startDate", dates[0]);
                    handleFilterChange("endDate", dates[1]);
                  } else if (dates && dates.length === 0) {
                    handleFilterChange("startDate", "");
                    handleFilterChange("endDate", "");
                  }
                }}
              >
                <DatePickerInput
                  id="start-date"
                  placeholder="mm/dd/yyyy"
                  labelText={<FormattedMessage id="equipment.usage.date.range" defaultMessage="Date Range" />}
                />
                <DatePickerInput
                  id="end-date"
                  placeholder="mm/dd/yyyy"
                  labelText=""
                />
              </DatePicker>
            </Column>

            <Column lg={4} md={4} sm={4} style={{ display: "flex", alignItems: "flex-end" }}>
              <Stack orientation="horizontal" gap={3}>
                <Button
                  kind="primary"
                  renderIcon={Search}
                  onClick={handleSearch}
                  disabled={loading}
                >
                  <FormattedMessage id="common.button.search" defaultMessage="Search" />
                </Button>
                <Button
                  kind="secondary"
                  renderIcon={Reset}
                  onClick={handleClearFilters}
                  disabled={loading}
                >
                  <FormattedMessage id="common.button.clear" defaultMessage="Clear Filters" />
                </Button>
              </Stack>
            </Column>
          </Grid>
        </div>

        {/* Results Section */}
        <div style={{ marginTop: "2rem" }}>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Heading size="sm" className="cds--margin-bottom-05">
                <FormattedMessage
                  id="equipment.usage.history.results"
                  defaultMessage="Usage History ({count} entries)"
                  values={{ count: entries.length }}
                />
              </Heading>
            </Column>
          </Grid>

          {loading ? (
            <div style={{ display: "flex", justifyContent: "center", padding: "3rem" }}>
              <Loading description="Loading..." />
            </div>
          ) : entries.length === 0 ? (
            <div
              style={{
                textAlign: "center",
                padding: "3rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "8px",
                border: "1px dashed #c6c6c6"
              }}
            >
              <p style={{ fontSize: "1.125rem", color: "#525252", marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="equipment.usage.history.no.results"
                  defaultMessage="No equipment usage records found"
                />
              </p>
              <p style={{ fontSize: "0.875rem", color: "#8d8d8d" }}>
                <FormattedMessage
                  id="equipment.usage.history.no.results.hint"
                  defaultMessage="Try adjusting your filters or create a new usage entry"
                />
              </p>
            </div>
          ) : (
            <DataTable
              rows={entries}
              headers={[
                {
                  key: "id",
                  header: <FormattedMessage id="equipment.usage.table.id" defaultMessage="Entry ID" />
                },
                {
                  key: "equipment",
                  header: <FormattedMessage id="equipment.usage.table.equipment" defaultMessage="Equipment" />
                },
                {
                  key: "operatorName",
                  header: <FormattedMessage id="equipment.usage.table.operator" defaultMessage="Operator" />
                },
                {
                  key: "loginTime",
                  header: <FormattedMessage id="equipment.usage.table.datetime" defaultMessage="Login Date/Time" />
                },
                {
                  key: "equipmentStatus",
                  header: <FormattedMessage id="equipment.usage.table.equipment.status" defaultMessage="Equipment Status" />
                },
                {
                  key: "entryStatus",
                  header: <FormattedMessage id="equipment.usage.table.entry.status" defaultMessage="Entry Status" />
                },
                {
                  key: "approvedBy",
                  header: <FormattedMessage id="equipment.usage.table.approved.by" defaultMessage="Approved By" />
                },
              ]}
              render={({ rows, headers, getHeaderProps, getRowProps }) => (
                <table className="cds--data-table cds--data-table--zebra">
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader key={header.key} {...getHeaderProps({ header })}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header.props?.id === "equipment.usage.table.equipment"
                              ? row.original.equipment?.name || "—"
                              : cell.info.header.props?.id === "equipment.usage.table.approved.by"
                              ? row.original.approvedBy?.username || "—"
                              : cell.info.header.props?.id === "equipment.usage.table.datetime"
                              ? new Date(cell.value).toLocaleString()
                              : cell.info.header.props?.id === "equipment.usage.table.equipment.status"
                              ? formatEquipmentStatus(cell.value)
                              : cell.info.header.props?.id === "equipment.usage.table.entry.status"
                              ? formatEntryStatus(cell.value)
                              : cell.value || "—"}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </table>
              )}
            />
          )}
        </div>
      </Section>
    </div>
  );
};

export default EquipmentUsageHistory;
