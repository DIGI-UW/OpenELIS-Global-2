import React, { useState, useContext } from "react";
import {
  Button,
  Column,
  Grid,
  Section,
  Checkbox,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandRow,
  TableExpandedRow,
  TableExpandHeader,
  Tag,
  Pagination,
  TableContainer,
} from "@carbon/react";
import { Renew, CheckmarkOutline } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import ResultRow from "./ResultRow";
import RetestModal from "./RetestModal";

const ValidationPage = ({ results, setResults, onRefresh }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [expandedRows, setExpandedRows] = useState({});
  const [selectedRows, setSelectedRows] = useState([]);
  const [showRetestModal, setShowRetestModal] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const stats = {
    normal: results?.resultList?.filter((r) => r.isNormal).length || 0,
    abnormal:
      results?.resultList?.filter((r) => !r.isNormal && !r.isCritical).length ||
      0,
    flagged:
      results?.resultList?.filter((r) => r.flags && r.flags.length > 0)
        .length || 0,
  };

  const headers = [
    {
      key: "qcIndicator",
      header: intl.formatMessage({ id: "validation.header.qc" }),
    },
    {
      key: "labNumber",
      header: intl.formatMessage({ id: "validation.header.labnumber" }),
    },
    {
      key: "patientInfo",
      header: intl.formatMessage({ id: "validation.header.patient" }),
    },
    {
      key: "testName",
      header: intl.formatMessage({ id: "validation.header.test" }),
    },
    {
      key: "method",
      header: intl.formatMessage({ id: "validation.header.method" }),
    },
    {
      key: "range",
      header: intl.formatMessage({ id: "validation.header.range" }),
    },
    {
      key: "result",
      header: intl.formatMessage({ id: "validation.header.result" }),
    },
    {
      key: "flags",
      header: intl.formatMessage({ id: "validation.header.flags" }),
    },
    {
      key: "enteredBy",
      header: intl.formatMessage({ id: "validation.header.enteredby" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "validation.header.actions" }),
    },
  ];

  const rowsData =
    results?.resultList?.map((item) => ({
      id: item.analysisId,
      qcIndicator:
        item.qcStatus === "pass" ? "✓" : item.qcStatus === "fail" ? "✗" : "",
      labNumber: item.accessionNumber,
      patientInfo: item.patientInfoObject
        ? `${item.patientInfoObject.name} (${item.patientInfoObject.age}, ${item.patientInfoObject.sex})`
        : item.patientName || "",
      testName: item.testName,
      method: item.method || (item.isManual ? "Manual" : "Analyzer"),
      range: item.normalRange,
      result: item.result,
      flags: item.flags || [],
      enteredBy: item.enteredByObject
        ? `${item.enteredByObject.name} (${item.enteredByObject.date})`
        : "",
      rawData: item,
    })) || [];

  const handleSelectRow = (rowId) => {
    setSelectedRows((prev) => {
      if (prev.includes(rowId)) {
        return prev.filter((id) => id !== rowId);
      }
      return [...prev, rowId];
    });
  };

  const handleSelectAll = () => {
    if (selectedRows.length === rowsData.length) {
      setSelectedRows([]);
    } else {
      setSelectedRows(rowsData.map((row) => row.id));
    }
  };

  const handleSelectNormal = () => {
    const normalRows = rowsData
      .filter((row) => row.rawData.isNormal)
      .map((row) => row.id);
    setSelectedRows(normalRows);
  };

  const handleRetest = () => {
    if (selectedRows.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "validation.retest.noselection",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    setShowRetestModal(true);
  };

  const handleConfirmRetest = (reason) => {
    const retestRequest = {
      resultIds: selectedRows,
      reason: reason,
    };

    postToOpenElisServerJsonResponse(
      "/rest/AccessionValidation/retest",
      JSON.stringify(retestRequest),
      (data) => {
        if (data && (data.success || data.status === 200)) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "validation.retest.success" },
              { count: data.count || selectedRows.length },
            ),
          });
          setNotificationVisible(true);
          setSelectedRows([]);
          setShowRetestModal(false);

          if (onRefresh) {
            onRefresh();
          }
        } else if (data && data.error) {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              data.message ||
              intl.formatMessage({
                id: "validation.retest.error",
              }),
          });
          setNotificationVisible(true);
          setShowRetestModal(false);
        } else {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "validation.retest.success" },
              { count: selectedRows.length },
            ),
          });
          setNotificationVisible(true);
          setSelectedRows([]);
          setShowRetestModal(false);

          if (onRefresh) {
            onRefresh();
          }
        }
      },
    );
  };

  const handleAcceptRelease = () => {
    if (selectedRows.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "validation.accept.noselection",
        }),
      });
      setNotificationVisible(true);
      return;
    }

    const updatedResultList = results.resultList.map((item) => ({
      ...item,
      isAccepted: selectedRows.includes(item.analysisId),
    }));

    const validationForm = {
      ...results,
      resultList: updatedResultList,
    };

    postToOpenElisServer(
      "/rest/AccessionValidation",
      JSON.stringify(validationForm),
      (status) => {
        if (status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "validation.accept.success" },
              { count: selectedRows.length },
            ),
          });
          setNotificationVisible(true);
          setSelectedRows([]);

          if (onRefresh) {
            onRefresh();
          } else {
            window.location.reload();
          }
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "validation.accept.error" }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const toggleRowExpansion = (rowId) => {
    setExpandedRows((prev) => ({
      ...prev,
      [rowId]: !prev[rowId],
    }));
  };

  const handleSingleAcceptRelease = (analysisId, modifications = {}) => {
    const resultItem = results.resultList.find(
      (item) => item.analysisId === analysisId,
    );
    if (!resultItem) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "validation.accept.error" }),
      });
      setNotificationVisible(true);
      return;
    }

    const updatedResultList = results.resultList.map((item) => ({
      ...item,
      isAccepted: item.analysisId === analysisId,
      result:
        item.analysisId === analysisId && modifications.result
          ? modifications.result
          : item.result,
      note:
        item.analysisId === analysisId && modifications.note
          ? modifications.note
          : item.note,
    }));

    const validationForm = {
      ...results,
      resultList: updatedResultList,
    };

    postToOpenElisServer(
      "/rest/AccessionValidation",
      JSON.stringify(validationForm),
      (status) => {
        if (status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "validation.accept.success" },
              { count: 1 },
            ),
          });
          setNotificationVisible(true);

          if (onRefresh) {
            onRefresh();
          } else {
            window.location.reload();
          }
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "validation.accept.error" }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleSingleRetest = (analysisId, reason) => {
    const retestRequest = {
      resultIds: [analysisId],
      reason: reason,
    };

    postToOpenElisServerJsonResponse(
      "/rest/AccessionValidation/retest",
      JSON.stringify(retestRequest),
      (data) => {
        if (data && (data.success || data.status === 200)) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "validation.retest.success" },
              { count: 1 },
            ),
          });
          setNotificationVisible(true);

          if (onRefresh) {
            onRefresh();
          }
        } else if (data && data.error) {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              data.message ||
              intl.formatMessage({
                id: "validation.retest.error",
              }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleSaveModification = (analysisId, modifications) => {
    const updatedResultList = results.resultList.map((item) => {
      if (item.analysisId === analysisId) {
        return {
          ...item,
          result: modifications.result || item.result,
          note: modifications.note || item.note,
        };
      }
      return item;
    });

    setResults({
      ...results,
      resultList: updatedResultList,
    });

    addNotification({
      kind: NotificationKinds.info,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "validation.modification.saved.locally",
      }),
    });
    setNotificationVisible(true);
  };

  if (!results || !results.resultList || results.resultList.length === 0) {
    return (
      <Section className="validation-empty-state">
        <Grid>
          <Column lg={16}>
            <div style={{ textAlign: "center", padding: "4rem 0" }}>
              <h3>
                <FormattedMessage id="validation.search.empty.title" />
              </h3>
              <p>
                <FormattedMessage id="validation.search.empty.message" />
              </p>
            </div>
          </Column>
        </Grid>
      </Section>
    );
  }

  return (
    <Section className="validation-results-section">
      <Grid>
        <Column lg={16}>
          <div
            className="validation-stats"
            style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
          >
            <Tag type="green">
              <FormattedMessage id="validation.stats.normal" />: {stats.normal}
            </Tag>
            <Tag type="red">
              <FormattedMessage id="validation.stats.abnormal" />:{" "}
              {stats.abnormal}
            </Tag>
            <Tag type="purple">
              <FormattedMessage id="validation.stats.flagged" />:{" "}
              {stats.flagged}
            </Tag>
          </div>

          <div
            className="validation-batch-actions"
            style={{
              display: "flex",
              gap: "1rem",
              marginBottom: "1rem",
              alignItems: "center",
            }}
          >
            <Checkbox
              id="select-all"
              labelText={intl.formatMessage({
                id: "validation.batch.selectall",
              })}
              checked={
                selectedRows.length === rowsData.length && rowsData.length > 0
              }
              indeterminate={
                selectedRows.length > 0 && selectedRows.length < rowsData.length
              }
              onChange={handleSelectAll}
            />
            <Button kind="tertiary" size="sm" onClick={handleSelectNormal}>
              <FormattedMessage id="validation.batch.selectnormal" />
            </Button>
            <span>
              <FormattedMessage
                id="validation.batch.selected"
                values={{ count: selectedRows.length }}
              />
            </span>
            <div style={{ marginLeft: "auto", display: "flex", gap: "0.5rem" }}>
              <Button
                kind="secondary"
                renderIcon={Renew}
                onClick={handleRetest}
                disabled={selectedRows.length === 0}
              >
                <FormattedMessage id="validation.batch.retest" />
              </Button>
              <Button
                kind="primary"
                renderIcon={CheckmarkOutline}
                onClick={handleAcceptRelease}
                disabled={selectedRows.length === 0}
              >
                <FormattedMessage id="validation.batch.accept" />
              </Button>
            </div>
          </div>

          <DataTable rows={rowsData} headers={headers} isSortable>
            {({
              rows,
              headers,
              getHeaderProps,
              getRowProps,
              getTableProps,
              getTableContainerProps,
            }) => (
              <TableContainer
                {...getTableContainerProps()}
                title={intl.formatMessage({ id: "validation.results.title" })}
                description={intl.formatMessage(
                  { id: "validation.results.count" },
                  { count: rowsData.length },
                )}
              >
                <Table {...getTableProps()} aria-label="validation results">
                  <TableHead>
                    <TableRow>
                      <TableExpandHeader />
                      <TableHeader>
                        {intl.formatMessage({ id: "validation.header.select" })}
                      </TableHeader>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => {
                      const isExpanded = expandedRows[row.id];
                      const isSelected = selectedRows.includes(row.id);

                      return (
                        <React.Fragment key={row.id}>
                          <TableExpandRow
                            {...getRowProps({ row })}
                            isExpanded={isExpanded}
                            onExpand={() => toggleRowExpansion(row.id)}
                          >
                            <TableCell>
                              <Checkbox
                                id={`checkbox-${row.id}`}
                                labelText=""
                                hideLabel
                                checked={isSelected}
                                onChange={() => handleSelectRow(row.id)}
                              />
                            </TableCell>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "flags") {
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value.map((flag, idx) => (
                                      <Tag
                                        key={idx}
                                        type={
                                          flag === "critical"
                                            ? "red"
                                            : flag === "above-normal"
                                              ? "red"
                                              : flag === "below-normal"
                                                ? "blue"
                                                : "purple"
                                        }
                                        size="sm"
                                      >
                                        {flag}
                                      </Tag>
                                    ))}
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                          </TableExpandRow>
                          {isExpanded && (
                            <TableExpandedRow colSpan={headers.length + 3}>
                              <ResultRow
                                result={
                                  rowsData.find((r) => r.id === row.id)?.rawData
                                }
                                onAcceptRelease={handleSingleAcceptRelease}
                                onRetest={handleSingleRetest}
                                onSaveModification={handleSaveModification}
                              />
                            </TableExpandedRow>
                          )}
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>

          <Pagination
            page={currentPage}
            pageSize={pageSize}
            pageSizes={[10, 20, 50, 100]}
            totalItems={rowsData.length}
            onChange={({ page, pageSize }) => {
              setCurrentPage(page);
              setPageSize(pageSize);
            }}
            forwardText={intl.formatMessage({ id: "pagination.forward" })}
            backwardText={intl.formatMessage({ id: "pagination.backward" })}
            itemRangeText={(min, max, total) =>
              intl.formatMessage(
                { id: "pagination.item-range" },
                { min: min, max: max, total: total },
              )
            }
            itemsPerPageText={intl.formatMessage({
              id: "pagination.items-per-page",
            })}
            itemText={(min, max) =>
              intl.formatMessage(
                { id: "pagination.item" },
                { min: min, max: max },
              )
            }
            pageNumberText={intl.formatMessage({
              id: "pagination.page-number",
            })}
            pageRangeText={(_current, total) =>
              intl.formatMessage(
                { id: "pagination.page-range" },
                { total: total },
              )
            }
            pageText={(page, pagesUnknown) =>
              intl.formatMessage(
                { id: "pagination.page" },
                { page: pagesUnknown ? "" : page },
              )
            }
          />
        </Column>
      </Grid>

      {showRetestModal && (
        <RetestModal
          isOpen={showRetestModal}
          onClose={() => setShowRetestModal(false)}
          onConfirm={handleConfirmRetest}
          selectedCount={selectedRows.length}
        />
      )}
    </Section>
  );
};

export default ValidationPage;
