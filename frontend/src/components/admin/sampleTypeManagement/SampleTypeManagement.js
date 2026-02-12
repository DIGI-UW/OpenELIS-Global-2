import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  Pagination,
  Search,
  TableContainer,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, injectIntl } from "react-intl";
import "../../../Style.css";
import { getFromOpenElisServer, postToOpenElisServerJsonResponse } from "../../utils/Utils";
import UserSessionDetailsContext from "../../../contexts/UserSessionDetailsContext";
import { NotificationContext } from "../../layout/Layout";
import CustomCheckBox from "../../common/CustomCheckBox";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType";

let sampleTypeManagementController = new AbortController();

function SampleTypeManagement(props) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const intl = props.intl;

  // Data state
  const [sampleTypeList, setSampleTypeList] = useState();
  const [sampleTypeListShow, setSampleTypeListShow] = useState([]);
  const [loading, setLoading] = useState(true);

  // Search state
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [isSearching, setIsSearching] = useState(false);

  // Filter state
  const [filters, setFilters] = useState([]);

  // Selection state
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [modifyButton, setModifyButton] = useState(true);
  const [deactivateButton, setDeactivateButton] = useState(true);

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const componentMounted = useRef(false);

  const handlePageChange = (pageInfo) => {
    setPage(pageInfo.page);
    setPageSize(pageInfo.pageSize);
  };

  const handlePanelSearchChange = (e) => {
    setPanelSearchTerm(e.target.value);
  };

  const handleFilterChange = (isChecked) => {
    setFilters(isChecked ? ["isActive"] : []);
  };

  const handleRowSelect = (rowId, isSelected) => {
    if (isSelected) {
      setSelectedRowIds([...selectedRowIds, rowId]);
    } else {
      setSelectedRowIds(selectedRowIds.filter((id) => id !== rowId));
    }
  };

  const handleSelectAll = (isSelected) => {
    if (isSelected) {
      setSelectedRowIds(sampleTypeListShow.map((item) => item.id));
    } else {
      setSelectedRowIds([]);
    }
  };

  const handleDeactivateSampleTypes = () => {
    if (selectedRowIds.length > 0) {
      const idsParam = selectedRowIds.join(",");

      postToOpenElisServerJsonResponse(
        `/rest/SampleTypeManagement/deactivate?IDS=${idsParam}`,
        {},
        (res) => {
          if (res && !res.error) {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "notification.sample.type.deactivate.success",
              }),
              kind: "success",
            });
            setNotificationVisible(true);
            setSelectedRowIds([]);
            loadSampleTypes(); // Reload data
          } else {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({ id: "error.save.msg" }),
              kind: "error",
            });
            setNotificationVisible(true);
          }
        }
      );
    }
  };

  const handleExportCSV = () => {
    const searchParam = panelSearchTerm ? `searchString=${encodeURIComponent(panelSearchTerm)}` : "";
    const filterParam = filters.includes("isActive") ? "filter=isActive" : "";
    const params = [searchParam, filterParam].filter(Boolean).join("&");

    window.location.href = `/rest/SampleTypeManagement/export?format=csv&${params}`;

    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "notification.sample.type.export.success",
      }),
      kind: "success",
    });
    setNotificationVisible(true);
  };

  const loadSampleTypes = () => {
    sampleTypeManagementController.abort();
    sampleTypeManagementController = new AbortController();
    setIsSearching(true);

    const searchParam = panelSearchTerm ? `searchString=${encodeURIComponent(panelSearchTerm)}` : "";
    const filterParam = filters.includes("isActive") ? "filter=isActive" : "";
    const params = [searchParam, filterParam].filter(Boolean).join("&");

    getFromOpenElisServer(
      `/rest/SampleTypeManagement${params ? `?${params}` : ""}`,
      (res) => {
        if (componentMounted.current) {
          setSampleTypeList(res);
          setIsSearching(false);
          setLoading(false);
        }
      },
      sampleTypeManagementController.signal
    );
  };

  useEffect(() => {
    componentMounted.current = true;
    loadSampleTypes();
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
      sampleTypeManagementController.abort();
    };
  }, []);

  useEffect(() => {
    if (sampleTypeList) {
      setSampleTypeListShow(sampleTypeList.menuList || []);
    }
  }, [sampleTypeList]);

  useEffect(() => {
    const debounceTimer = setTimeout(() => {
      if (componentMounted.current) {
        loadSampleTypes();
      }
    }, 500);

    return () => clearTimeout(debounceTimer);
  }, [panelSearchTerm, filters]);

  useEffect(() => {
    // Update button states based on selection
    if (selectedRowIds.length === 1) {
      setModifyButton(false);
      setDeactivateButton(false);
    } else if (selectedRowIds.length > 1) {
      setModifyButton(true);
      setDeactivateButton(false);
    } else {
      setModifyButton(true);
      setDeactivateButton(true);
    }
  }, [selectedRowIds]);

  // Pagination logic
  const startIndex = (page - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedData = sampleTypeListShow.slice(startIndex, endIndex);

  const headers = [
    { key: "select", header: intl.formatMessage({ id: "label.select" }) },
    { key: "description", header: intl.formatMessage({ id: "sample.type.description" }) },
    { key: "localAbbreviation", header: intl.formatMessage({ id: "sample.type.abbreviation" }) },
    { key: "sortOrder", header: intl.formatMessage({ id: "sample.type.sort.order" }) },
    { key: "isActive", header: intl.formatMessage({ id: "label.status" }) },
  ];

  const rows = paginatedData.map((sampleType) => ({
    id: sampleType.id,
    select: sampleType.id,
    description: sampleType.description || "",
    localAbbreviation: sampleType.localAbbreviation || "",
    sortOrder: sampleType.sortOrder || "",
    isActive: sampleType.isActive
      ? intl.formatMessage({ id: "label.status.active" })
      : intl.formatMessage({ id: "label.status.inactive" }),
  }));

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      <div className="adminPageContent">
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <h2>
                  <FormattedMessage id="sample.type.browser.title" />
                </h2>
              </Section>
            </Section>
          </Column>
        </Grid>

        <br />

        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Search
              size="lg"
              id="sample-type-search-bar"
              labelText={<FormattedMessage id="sample.type.browser.search" />}
              placeholder={intl.formatMessage({ id: "sample.type.search.placeholder" })}
              onChange={handlePanelSearchChange}
              value={panelSearchTerm}
            />
          </Column>
        </Grid>

        <br />

        <Grid fullWidth={true}>
          <Column lg={4} md={2} sm={1}>
            <CustomCheckBox
              id="active-filter"
              label={<FormattedMessage id="sample.type.filter.active.only" />}
              onChange={handleFilterChange}
              checked={filters.includes("isActive")}
            />
          </Column>
        </Grid>

        <br />

        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            {isSearching && (
              <InlineLoading
                status="active"
                iconDescription="Searching"
                description={<FormattedMessage id="label.searching" />}
              />
            )}

            <DataTable
              rows={rows}
              headers={headers}
              render={({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getSelectionProps,
                getTableProps,
                selectAll,
                selectedRows,
                onInputChange,
                getBatchActionProps,
              }) => (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll
                          {...getSelectionProps()}
                          onSelect={(isSelected) => handleSelectAll(isSelected)}
                          checked={selectedRowIds.length === sampleTypeListShow.length && sampleTypeListShow.length > 0}
                          indeterminate={selectedRowIds.length > 0 && selectedRowIds.length < sampleTypeListShow.length}
                        />
                        {headers
                          .filter((header) => header.key !== "select")
                          .map((header) => (
                            <TableHeader key={header.key} {...getHeaderProps({ header })}>
                              {header.header}
                            </TableHeader>
                          ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          <TableSelectRow
                            {...getSelectionProps({ row })}
                            onSelect={(isSelected) => handleRowSelect(row.id, isSelected)}
                            checked={selectedRowIds.includes(row.id)}
                          />
                          {row.cells
                            .filter((cell) => cell.info.header !== "select")
                            .map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            />
          </Column>
        </Grid>

        <br />

        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Pagination
              totalItems={sampleTypeListShow.length}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 50]}
              onChange={handlePageChange}
              size="md"
            />
          </Column>
        </Grid>

        <br />

        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <ActionPaginationButtonType
                selectedRowIds={selectedRowIds}
                modifyButton={modifyButton}
                deactivateButton={deactivateButton}
                deleteDeactivate={handleDeactivateSampleTypes}
                handleAdd={() => {
                  window.location.assign("/MasterListsPage?type=SampleTypeCreate#SampleTypeCreate");
                }}
                handleModify={() => {
                  if (selectedRowIds.length === 1) {
                    window.location.assign(`/MasterListsPage?type=SampleTypeRenameEntry&ID=${selectedRowIds[0]}#SampleTypeRenameEntry`);
                  }
                }}
                type="SampleType"
                addButtonText={intl.formatMessage({ id: "sample.type.add.button" })}
                modifyButtonText={intl.formatMessage({ id: "label.button.modify" })}
                deactivateButtonText={intl.formatMessage({ id: "label.button.deactivate" })}
              />
              <br />
              <Button
                onClick={handleExportCSV}
                kind="secondary"
                disabled={sampleTypeListShow.length === 0}
              >
                <FormattedMessage id="button.export.csv" />
              </Button>
            </Section>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeManagement);