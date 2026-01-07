import React, { useState, useEffect } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Button,
  TextInput,
  Select,
  SelectItem,
  Grid,
  Column,
  Pagination,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Loading,
} from "@carbon/react";

import {
  Search,
  Plus,
  Download,
  Upload,
  Edit,
  MoreVertical,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function LabUnitList({ onSelectUnit, onEditUnit }) {
  const intl = useIntl();
  const [labUnits, setLabUnits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [totalItems, setTotalItems] = useState(0);
  const [statusFilter, setStatusFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [sortField, setSortField] = useState("displayOrder");
  const [sortOrder, setSortOrder] = useState("asc");

  // Fetch lab units from API
  const fetchLabUnits = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        page: currentPage,
        size: pageSize,
        status: statusFilter,
        search: searchQuery,
        sortField,
        sortOrder,
      });

      const response = await fetch(`/rest/api/lab-units?${params}`);
      if (response.ok) {
        const data = await response.json();
        setLabUnits(data.labUnits || []);
        setTotalItems(data.total || 0);
      }
    } catch (error) {
      console.error("Error fetching lab units:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLabUnits();
  }, [currentPage, pageSize, statusFilter, searchQuery, sortField, sortOrder]);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortOrder("asc");
    }
    setCurrentPage(1);
  };

  const getSortIcon = (field) => {
    if (sortField !== field) return null;
    return sortOrder === "asc" ? "ascending" : "descending";
  };

  const getStatusBadge = (isActive) => {
    return isActive ? (
      <Tag type="green">
        {intl.formatMessage({ id: "labunit.status.active" })}
      </Tag>
    ) : (
      <Tag type="gray">
        {intl.formatMessage({ id: "labunit.status.inactive" })}
      </Tag>
    );
  };

  const totalPages = Math.ceil(totalItems / pageSize);

  return (
    <Grid fullWidth>
      <Column lg={16}>
        {/* Header */}
        <div style={{ marginBottom: "2rem" }}>
          <h1 style={{ marginBottom: "0.5rem" }}>
            {intl.formatMessage({ id: "labunit.setup.title" })}
          </h1>
          <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
            {intl.formatMessage({ id: "labunit.setup.subtitle" })}
          </p>

          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <Button kind="secondary" renderIcon={Upload}>
              {intl.formatMessage({ id: "button.import" })}
            </Button>
            <Button kind="secondary" renderIcon={Download}>
              {intl.formatMessage({ id: "button.export" })}
            </Button>
            <Button renderIcon={Plus} onClick={() => onSelectUnit(null)}>
              {intl.formatMessage({ id: "button.add.lab.unit" })}
            </Button>
          </div>
        </div>

        {/* Filters Bar */}
        <div
          style={{
            marginBottom: "1.5rem",
            display: "flex",
            gap: "1rem",
            alignItems: "center",
            flexWrap: "wrap",
          }}
        >
          <div style={{ minWidth: "300px", flex: "1" }}>
            <TextInput
              id="lab-unit-search"
              labelText={intl.formatMessage({
                id: "labunit.search.placeholder",
              })}
              placeholder={intl.formatMessage({
                id: "labunit.search.placeholder",
              })}
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setCurrentPage(1);
              }}
              renderIcon={Search}
            />
          </div>

          <Select
            id="status-filter"
            labelText={intl.formatMessage({ id: "labunit.filter.status" })}
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(1);
            }}
          >
            <SelectItem value="all">
              {intl.formatMessage({ id: "labunit.filter.all" })}
            </SelectItem>
            <SelectItem value="active">
              {intl.formatMessage({ id: "labunit.filter.active" })}
            </SelectItem>
            <SelectItem value="inactive">
              {intl.formatMessage({ id: "labunit.filter.inactive" })}
            </SelectItem>
          </Select>

          <div
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              fontWeight: "500",
            }}
          >
            {totalItems} {intl.formatMessage({ id: "labunit.count.label" })}
          </div>
        </div>

        {/* Table */}
        <div style={{ marginBottom: "1.5rem" }}>
          {loading ? (
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                padding: "3rem",
                gap: "1rem",
              }}
            >
              <Loading />
              <p>{intl.formatMessage({ id: "loading.lab.units" })}</p>
            </div>
          ) : (
            <DataTable rows={labUnits}>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableHeader
                        sortDirection={
                          sortField === "displayOrder"
                            ? getSortIcon("displayOrder")
                            : "none"
                        }
                        onClick={() => handleSort("displayOrder")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.order" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "name" ? getSortIcon("name") : "none"
                        }
                        onClick={() => handleSort("name")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.name" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "code" ? getSortIcon("code") : "none"
                        }
                        onClick={() => handleSort("code")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.code" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "tests" ? getSortIcon("tests") : "none"
                        }
                        onClick={() => handleSort("tests")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.tests" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "panels"
                            ? getSortIcon("panels")
                            : "none"
                        }
                        onClick={() => handleSort("panels")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.panels" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "programs"
                            ? getSortIcon("programs")
                            : "none"
                        }
                        onClick={() => handleSort("programs")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.programs" })}
                      </TableHeader>
                      <TableHeader
                        sortDirection={
                          sortField === "workflows"
                            ? getSortIcon("workflows")
                            : "none"
                        }
                        onClick={() => handleSort("workflows")}
                        isSortable
                      >
                        {intl.formatMessage({ id: "labunit.column.workflows" })}
                      </TableHeader>
                      <TableHeader>
                        {intl.formatMessage({ id: "labunit.column.status" })}
                      </TableHeader>
                      <TableHeader>
                        {intl.formatMessage({ id: "labunit.column.actions" })}
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {labUnits.map((unit, index) => (
                      <TableRow
                        key={unit.id}
                        style={{ cursor: "pointer" }}
                        onClick={() => onEditUnit && onEditUnit(unit)}
                      >
                        <TableCell>{unit.displayOrder}</TableCell>
                        <TableCell>
                          <span style={{ fontWeight: "500" }}>{unit.name}</span>
                        </TableCell>
                        <TableCell>
                          <span
                            style={{
                              fontFamily: "monospace",
                              fontSize: "0.875rem",
                            }}
                          >
                            {unit.code}
                          </span>
                        </TableCell>
                        <TableCell>{unit.tests}</TableCell>
                        <TableCell>{unit.panels}</TableCell>
                        <TableCell>{unit.programs}</TableCell>
                        <TableCell>{unit.workflows}</TableCell>
                        <TableCell>{getStatusBadge(unit.isActive)}</TableCell>
                        <TableCell>
                          <div
                            style={{
                              display: "flex",
                              gap: "0.25rem",
                              alignItems: "center",
                            }}
                          >
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={Edit}
                              onClick={(e) => {
                                e.stopPropagation();
                                onEditUnit && onEditUnit(unit);
                              }}
                              title={intl.formatMessage({ id: "button.edit" })}
                            />
                            <OverflowMenu size="sm">
                              <OverflowMenuItem
                                onClick={(e) => {
                                  e.stopPropagation();
                                  // Handle more actions
                                }}
                              >
                                {intl.formatMessage({
                                  id: "button.more.actions",
                                })}
                              </OverflowMenuItem>
                            </OverflowMenu>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </DataTable>
          )}
        </div>

        {/* Pagination */}
        {!loading && totalItems > 0 && (
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              flexWrap: "wrap",
              gap: "1rem",
            }}
          >
            <div style={{ fontSize: "0.875rem", color: "#525252" }}>
              {intl.formatMessage(
                { id: "pagination.showing.items" },
                {
                  start: (currentPage - 1) * pageSize + 1,
                  end: Math.min(currentPage * pageSize, totalItems),
                  total: totalItems,
                },
              )}
            </div>

            <div style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
              <Select
                id="page-size"
                hideLabel
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(1);
                }}
                size="sm"
              >
                <SelectItem value={25}>25</SelectItem>
                <SelectItem value={50}>50</SelectItem>
                <SelectItem value={100}>100</SelectItem>
              </Select>

              <Pagination
                backwardText={intl.formatMessage({ id: "pagination.previous" })}
                forwardText={intl.formatMessage({ id: "pagination.next" })}
                page={currentPage}
                pageSize={pageSize}
                totalItems={totalItems}
                pagesHidden={false}
                onChange={({ page }) => setCurrentPage(page)}
              />
            </div>
          </div>
        )}
      </Column>
    </Grid>
  );
}
