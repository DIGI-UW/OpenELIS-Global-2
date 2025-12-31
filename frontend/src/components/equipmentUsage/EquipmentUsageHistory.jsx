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
} from "@carbon/react";
import { Search } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import EquipmentUsageService from "./EquipmentUsageService";

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

  useEffect(() => {
    loadEquipment();
    loadEntries();
  }, [refreshTrigger]);

  const loadEquipment = async () => {
    try {
      const data = await EquipmentUsageService.getEquipmentForDropdown();
      setEquipment(data || []);
    } catch (err) {
      console.error("Failed to load equipment:", err);
      setError(`Failed to load equipment: ${err.message}`);
    }
  };

  const loadEntries = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await EquipmentUsageService.getAllUsageEntries();
      setEntries(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
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
      setEntries(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
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
    <>
      {error && (
        <InlineNotification
          title="Error"
          subtitle={error}
          kind="error"
          onClose={() => setError(null)}
        />
      )}

      <Grid fullWidth={true} style={{ marginBottom: "1.5rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="equipment-filter"
            labelText="Equipment"
            value={filters.equipment}
            onChange={(e) => handleFilterChange("equipment", e.target.value)}
          >
            <SelectItem value="" text="All Equipment" />
            {equipment.map((eq) => (
              <SelectItem key={eq.id} value={eq.id.toString()} text={eq.name} />
            ))}
          </Select>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <Select
            id="status-filter"
            labelText="Entry Status"
            value={filters.status}
            onChange={(e) => handleFilterChange("status", e.target.value)}
          >
            <SelectItem value="" text="All Statuses" />
            <SelectItem value="DRAFT" text="Draft" />
            <SelectItem value="SUBMITTED" text="Submitted" />
            <SelectItem value="APPROVED" text="Approved" />
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
              placeholder="Start Date"
              labelText="Date Range"
            />
            <DatePickerInput id="end-date" placeholder="End Date" labelText="" />
          </DatePicker>
        </Column>

        <Column lg={2} md={4} sm={4} style={{ display: "flex", alignItems: "flex-end" }}>
          <Stack orientation="horizontal" gap={3}>
            <Button kind="primary" renderIcon={Search} onClick={handleSearch}>
              <FormattedMessage id="common.button.search" defaultMessage="Search" />
            </Button>
            <Button kind="secondary" onClick={handleClearFilters}>
              <FormattedMessage id="common.button.clear" defaultMessage="Clear" />
            </Button>
          </Stack>
        </Column>
      </Grid>

      <DataTable
        rows={entries}
        headers={[
          { key: "id", header: "ID" },
          { key: "equipment", header: "Equipment" },
          { key: "operatorName", header: "Operator" },
          { key: "loginTime", header: "Date/Time" },
          { key: "equipmentStatus", header: "Equipment Status" },
          { key: "entryStatus", header: "Entry Status" },
          { key: "approvedBy", header: "Approved By" },
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
                      {cell.info.header === "Equipment"
                        ? row.original.equipment?.name
                        : cell.info.header === "Approved By"
                        ? row.original.approvedBy?.username || "N/A"
                        : cell.value}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </table>
        )}
      />
    </>
  );
};

export default EquipmentUsageHistory;
