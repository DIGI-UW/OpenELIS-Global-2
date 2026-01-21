import React, { useState, useEffect, useRef, useContext } from "react";
import {
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableCell,
  TableBody,
  TableContainer,
  TableSelectRow,
  TableSelectAll,
  Button,
  Search,
  Select,
  SelectItem,
  Pagination,
  Grid,
  Column,
  Loading,
  Toast,
  ToastNotification,
  InlineLoading,
} from "@carbon/react";
import {
  Add,
  Download,
  Upload,
  Edit,
  TrashCan,
  OverflowMenuVertical,
} from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

const LabUnitsManagement = () => {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [labUnits, setLabUnits] = useState([]);
  const [filteredLabUnits, setFilteredLabUnits] = useState([]);
  const [selectedRowIds, setSelectedRowIds] = useState([]);

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Filter state
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");

  // Sorting state
  const [sortField, setSortField] = useState("displayOrder");
  const [sortOrder, setSortOrder] = useState("asc");

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "labUnit.management.title",
      link: "/MasterListsPage/labUnitManagement",
    },
  ];

  useEffect(() => {
    componentMounted.current = true;
    loadLabUnits();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (componentMounted.current) {
      applyFilters();
    }
  }, [labUnits, searchQuery, statusFilter, sortField, sortOrder]);

  const loadLabUnits = () => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/api/lab-units?page=${page}&size=${pageSize}`,
      (response) => {
        if (componentMounted.current && response) {
          setLabUnits(response.items || []);
          setTotalItems(response.totalItems || 0);
          setTotalPages(response.totalPages || 0);
          setLoading(false);
        }
      },
    );
  };

  const applyFilters = () => {
    let filtered = [...labUnits];

    // Apply status filter
    if (statusFilter !== "all") {
      filtered = filtered.filter((unit) =>
        statusFilter === "active" ? unit.isActive : !unit.isActive,
      );
    }

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (unit) =>
          unit.name?.toLowerCase().includes(query) ||
          unit.code?.toLowerCase().includes(query),
      );
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];

      if (
        ["displayOrder", "tests", "panels", "programs", "workflows"].includes(
          sortField,
        )
      ) {
        aVal = Number(aVal) || 0;
        bVal = Number(bVal) || 0;
      } else {
        aVal = String(aVal || "").toLowerCase();
        bVal = String(bVal || "").toLowerCase();
      }

      if (aVal < bVal) return sortOrder === "asc" ? -1 : 1;
      if (aVal > bVal) return sortOrder === "asc" ? 1 : -1;
      return 0;
    });

    setFilteredLabUnits(filtered);
  };

  const handleSort = (field) => {
    if (sortField === field) {
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortOrder("asc");
    }
  };

  const getSortIcon = (field) => {
    if (sortField !== field) return null;
    return sortOrder === "asc" ? "↑" : "↓";
  };

  const handlePageChange = (newPage) => {
    setPage(newPage);
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setPage(1);
  };

  const handleRowSelection = (selectedRows) => {
    setSelectedRowIds(selectedRows);
  };

  const handleSearch = (event) => {
    const value = event.target.value;
    setSearchQuery(value);
    setPage(1); // Reset to first page on search
  };

  const handleStatusFilter = (event) => {
    setStatusFilter(event.target.value);
    setPage(1); // Reset to first page on filter
  };

  const handleEditUnit = (unit) => {
    // Navigate to editor - will implement when editor component is ready
    window.location.href = `/MasterListsPage/labUnitManagement/edit/${unit.id}`;
  };

  const handleAddUnit = () => {
    // Navigate to add new unit
    window.location.href = "/MasterListsPage/labUnitManagement/add";
  };

  const handleExport = () => {
    // Export functionality
    getFromOpenElisServer(`/rest/api/lab-units/export?format=csv`, (data) => {
      // Create download link
      const blob = new Blob([data], { type: "text/csv" });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "lab-units.csv";
      a.click();
      window.URL.revokeObjectURL(url);

      addNotification({
        title: intl.formatMessage({ id: "labUnit.export.success" }),
        kind: "success",
        timestamp: new Date().toISOString(),
      });
    });
  };

  const headers = [
    {
      key: "displayOrder",
      header: intl.formatMessage({ id: "labUnit.header.order" }),
      width: 80,
    },
    { key: "name", header: intl.formatMessage({ id: "labUnit.header.name" }) },
    {
      key: "code",
      header: intl.formatMessage({ id: "labUnit.header.code" }),
      width: 100,
    },
    {
      key: "tests",
      header: intl.formatMessage({ id: "labUnit.header.tests" }),
      width: 80,
    },
    {
      key: "panels",
      header: intl.formatMessage({ id: "labUnit.header.panels" }),
      width: 80,
    },
    {
      key: "programs",
      header: intl.formatMessage({ id: "labUnit.header.programs" }),
      width: 100,
    },
    {
      key: "workflows",
      header: intl.formatMessage({ id: "labUnit.header.workflows" }),
      width: 100,
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "labUnit.header.status" }),
      width: 100,
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "labUnit.header.actions" }),
      width: 80,
    },
  ];

  const getRowDescription = (row) => `${row.name} - ${row.code}`;

  const getCellValue = (row, header) => {
    if (header.key === "status") {
      return row.isActive ? (
        <span style={{ color: "#24a148", fontWeight: "500" }}>
          <FormattedMessage id="labUnit.status.active" />
        </span>
      ) : (
        <span style={{ color: "#525252", fontWeight: "500" }}>
          <FormattedMessage id="labUnit.status.inactive" />
        </span>
      );
    }

    if (header.key === "actions") {
      return (
        <div
          style={{ display: "flex", gap: "8px", justifyContent: "flex-end" }}
        >
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Edit}
            iconDescription={intl.formatMessage({ id: "button.edit" })}
            onClick={() => handleEditUnit(row)}
          />
          <Button
            kind="ghost"
            size="sm"
            renderIcon={OverflowMenuVertical}
            iconDescription={intl.formatMessage({ id: "button.more" })}
          />
        </div>
      );
    }

    return row[header.key] || "";
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth className="page-header">
        <Column lg={16}>
          <h1>
            <FormattedMessage id="labUnit.management.title" />
          </h1>
          <p className="page-description">
            <FormattedMessage id="labUnit.management.description" />
          </p>
        </Column>
      </Grid>

      <Grid fullWidth className="toolbar-section">
        <Column lg={12}>
          <div className="toolbar-left">
            <Search
              value={searchQuery}
              onChange={handleSearch}
              placeholder={intl.formatMessage({
                id: "labUnit.search.placeholder",
              })}
              className="search-input"
            />
            <Select
              value={statusFilter}
              onChange={handleStatusFilter}
              labelText=""
              className="status-filter"
            >
              <SelectItem value="all">
                <FormattedMessage id="labUnit.filter.all" />
              </SelectItem>
              <SelectItem value="active">
                <FormattedMessage id="labUnit.filter.active" />
              </SelectItem>
              <SelectItem value="inactive">
                <FormattedMessage id="labUnit.filter.inactive" />
              </SelectItem>
            </Select>
            <span className="results-count">
              {filteredLabUnits.length}{" "}
              <FormattedMessage id="labUnit.units.count" />
            </span>
          </div>
        </Column>
        <Column lg={4}>
          <div className="toolbar-right">
            <Button
              kind="secondary"
              renderIcon={Upload}
              onClick={() => {
                /* Import functionality */
              }}
            >
              <FormattedMessage id="button.import" />
            </Button>
            <Button
              kind="secondary"
              renderIcon={Download}
              onClick={handleExport}
            >
              <FormattedMessage id="button.export" />
            </Button>
            <Button kind="primary" renderIcon={Add} onClick={handleAddUnit}>
              <FormattedMessage id="labUnit.button.add" />
            </Button>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth>
        <Column lg={16}>
          {loading ? (
            <Loading />
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        width={header.width}
                        isSortable={
                          header.key !== "status" && header.key !== "actions"
                        }
                        onClick={() =>
                          header.key !== "status" &&
                          header.key !== "actions" &&
                          handleSort(header.key)
                        }
                      >
                        <span>
                          {header.header}
                          {getSortIcon(header.key) && (
                            <span style={{ marginLeft: "8px" }}>
                              {getSortIcon(header.key)}
                            </span>
                          )}
                        </span>
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredLabUnits
                    .slice((page - 1) * pageSize, page * pageSize)
                    .map((row) => (
                      <TableRow
                        key={row.id}
                        onClick={() => handleEditUnit(row)}
                        style={{ cursor: "pointer" }}
                      >
                        {headers.map((header) => (
                          <TableCell key={header.key}>
                            {getCellValue(row, header)}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Column>
      </Grid>

      <Grid fullWidth className="pagination-section">
        <Column lg={16}>
          <Pagination
            totalItems={filteredLabUnits.length}
            pageSize={pageSize}
            pageSizes={[25, 50, 100]}
            page={page}
            onChange={handlePageChange}
            onPageSizeChange={handlePageSizeChange}
          />
        </Column>
      </Grid>
    </>
  );
};

export default LabUnitsManagement;
