import React, { useState, useEffect, useContext } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Pagination,
  Link,
  Grid,
  Column,
  OverflowMenu,
  OverflowMenuItem,
  InlineLoading,
  Stack
} from "@carbon/react";

import { FormattedMessage, useIntl } from "react-intl";
import { ChevronDown, Edit, TaskAdd, Menu } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../utils/Utils";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";

const EOrder = ({ eOrders, setEOrders, eOrderRef }) => {
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [entering, setEntering] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [windowWidth, setWindowWidth] = useState(window.innerWidth);

  useEffect(() => {
    const handleResize = () => {
      setWindowWidth(window.innerWidth);
    };

    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  // Determine which headers to show based on screen size
  const getResponsiveHeaders = () => {
    const allHeaders = [
      {
        key: "requestDateDisplay",
        header: intl.formatMessage({ id: "eorder.requestDate" }),
      },
      {
        key: "patientLastName",
        header: intl.formatMessage({ id: "eorder.name.last" }),
      },
      {
        key: "patientFirstName",
        header: intl.formatMessage({ id: "eorder.name.first" }),
      },
      {
        key: "patientNationalId",
        header: intl.formatMessage({
          id: "eorder.id.national",
        }),
      },
      {
        key: "requestingFacility",
        header: intl.formatMessage({
          id: "eorder.facility.requesting",
        }),
      },
      {
        key: "priority",
        header: intl.formatMessage({
          id: "eorder.priority",
        }),
      },
      {
        key: "status",
        header: intl.formatMessage({
          id: "eorder.status",
        }),
      },
      {
        key: "testName",
        header: intl.formatMessage({
          id: "eorder.test.name",
        }),
      },
      {
        key: "referringLabNumber",
        header: intl.formatMessage({
          id: "eorder.labnumber.referring",
        }),
      },
      {
        key: "passportNumber",
        header: intl.formatMessage({
          id: "eorder.passport.number",
        }),
      },
      {
        key: "subjectNumber",
        header: intl.formatMessage({
          id: "eorder.id.subjectNumber",
        }),
      },
      {
        key: "labNumber",
        header: intl.formatMessage({
          id: "eorder.labNumber",
        }),
      },
    ];

    if (windowWidth < 768) {
      return allHeaders.filter(header => 
        ['requestDateDisplay', 'patientLastName', 'patientFirstName', 'status'].includes(header.key)
      );
    }

    if (windowWidth < 1200) {
      return allHeaders.filter(header => 
        ['requestDateDisplay', 'patientLastName', 'patientFirstName', 'patientNationalId', 
        'requestingFacility', 'status', 'testName'].includes(header.key)
      );
    }

    return allHeaders;
  };

  function saveEntry(externalOrderId, labNumber) {
    window.open(
      "SamplePatientEntry?ID=" +
        (externalOrderId || "") +
        "&labNumber=" +
        (labNumber || "") +
        "&attemptAutoSave=true",
      "_blank",
    );
  }

  function editOrder(externalOrderId, labNumber) {
    window.open(
      "SamplePatientEntry?ID=" +
        externalOrderId +
        "&labNumber=" +
        (labNumber || ""),
    );
  }

  const handleLabNoGeneration = (e, index) => {
    if (e) {
      e.preventDefault();
    }
    setEntering(true);
    getFromOpenElisServer("/rest/SampleEntryGenerateScanProvider", (res) => {
      handleGeneratedAccessionNo(res, index);
      setEntering(false);
    });
  };

  function handleGeneratedAccessionNo(res, index) {
    if (res.status) {
      let newEOrders = [...eOrders];
      newEOrders[index].labNo = res.body;
      setEOrders(newEOrders);
    }
  }

  function handleLabNo(e, rawVal, index) {
    let newEOrders = [...eOrders];
    newEOrders[index].labNo = rawVal ? rawVal : e?.target?.value;
    setEOrders(newEOrders);
  }

  function accessionNumberValidationResults(res) {
    if (res.status === false) {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: res.body,
      });
    }
  }

  const handleLabNoValidation = (e, index) => {
    const labNo = eOrders[index].labNo;
    if (labNo !== "") {
      getFromOpenElisServer(
        "/rest/SampleEntryAccessionNumberValidation?ignoreYear=false&ignoreUsage=false&field=labNo&accessionNumber=" +
          labNo,
        accessionNumberValidationResults,
      );
    }
  };

  const handleKeyPress = (event, index) => {
    if (event.key === "Enter") {
      handleLabNoGeneration(event, index);
    }
  };

  const renderExpandedRow = (row) => {
    const eOrderId = row.id;
    const electronicOrderUUID = eOrders.find((item) => {
      return item.id === eOrderId;
    })?.externalOrderId;
    if (!electronicOrderUUID) {
      return <></>;
    }
    const index = eOrders.findIndex((item) => {
      return item.id === eOrderId;
    });

    return (
      <Grid narrow>
        <Column sm={4} md={6} lg={8}>
          <Stack gap={4}>
            <CustomLabNumberInput
              name="labNo"
              value={eOrders[index].labNo || ""}
              onBlur={(e) => {
                handleLabNoValidation(e, index);
              }}
              onChange={(e, rawInput) => {
                handleLabNo(e, rawInput, index);
              }}
              onKeyPress={(e) => {
                handleKeyPress(e, index);
              }}
              labelText={intl.formatMessage({ id: "sample.label.labnumber" })}
              id="labNo"
              helperText={
                <>
                  <FormattedMessage id="label.order.scan.text" />{" "}
                  <Link
                    href="#"
                    onClick={(e) => {
                      handleLabNoGeneration(e, index);
                    }}
                  >
                    <FormattedMessage id="sample.label.labnumber.generate" />
                  </Link>
                </>
              }
            />
            {entering ? (
              <InlineLoading description={intl.formatMessage({ id: "loading" })} />
            ) : (
              <Stack orientation="horizontal" gap={2}>
                <Button
                  type="button"
                  kind="tertiary"
                  renderIcon={Edit}
                  iconDescription={intl.formatMessage({
                    id: "eorder.button.editOrder",
                  })}
                  onClick={() => {
                    editOrder(electronicOrderUUID, eOrders[index].labNo);
                  }}
                >
                  {windowWidth > 768 ? intl.formatMessage({ id: "eorder.button.editOrder" }) : ""}
                </Button>
                <Button
                  type="button"
                  kind="primary"
                  renderIcon={TaskAdd}
                  iconDescription={intl.formatMessage({
                    id: "eorder.button.enterOrder",
                  })}
                  onClick={() => {
                    saveEntry(electronicOrderUUID, eOrders[index].labNo);
                  }}
                >
                  {windowWidth > 768 ? intl.formatMessage({ id: "eorder.button.enterOrder" }) : ""}
                </Button>
              </Stack>
            )}
          </Stack>
        </Column>
      </Grid>
    );
  };

  const renderCell = (cell, row) => {
    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  const handlePageChange = (pageInfo) => {
    if (page !== pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize !== pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  // For very small screens, create a card-based view instead of a table
  const renderCardView = (eOrdersCurrent) => {
    const currentOrders = eOrdersCurrent.slice((page - 1) * pageSize, page * pageSize);

    return (
      <Stack gap={4}>
        {currentOrders.map((order, index) => (
          <div key={order.id} style={{ border: '1px solid #e0e0e0', borderRadius: '4px', padding: '1rem' }}>
            <Stack gap={3}>
              <div>
                <strong>{intl.formatMessage({ id: "eorder.name.last" })}:</strong> {order.patientLastName}
              </div>
              <div>
                <strong>{intl.formatMessage({ id: "eorder.name.first" })}:</strong> {order.patientFirstName}
              </div>
              <div>
                <strong>{intl.formatMessage({ id: "eorder.requestDate" })}:</strong> {order.requestDateDisplay}
              </div>
              <div>
                <strong>{intl.formatMessage({ id: "eorder.status" })}:</strong> {order.status}
              </div>
              <div>
                <OverflowMenu flipped renderIcon={Menu}>
                  <OverflowMenuItem 
                    itemText={intl.formatMessage({ id: "eorder.button.editOrder" })}
                    onClick={() => {
                      const electronicOrderUUID = order.externalOrderId;
                      editOrder(electronicOrderUUID, order.labNo);
                    }}
                  />
                  <OverflowMenuItem 
                    itemText={intl.formatMessage({ id: "eorder.button.enterOrder" })}
                    onClick={() => {
                      const electronicOrderUUID = order.externalOrderId;
                      saveEntry(electronicOrderUUID, order.labNo);
                    }}
                  />
                  <OverflowMenuItem 
                    itemText={intl.formatMessage({ id: "sample.label.labnumber.generate" })}
                    onClick={(e) => {
                      handleLabNoGeneration(e, index);
                    }}
                  />
                </OverflowMenu>
              </div>
            </Stack>
          </div>
        ))}
      </Stack>
    );
  };

  const createDataTable = (eOrdersCurrent) => {
    if (windowWidth < 576) {
      return (
        <>
          {renderCardView(eOrdersCurrent)}
          <Pagination
            onChange={handlePageChange}
            page={page}
            pageSize={pageSize}
            pageSizes={[10, 20, 30]}
            totalItems={eOrdersCurrent.length}
            forwardText={intl.formatMessage({ id: "pagination.forward" })}
            backwardText={intl.formatMessage({ id: "pagination.backward" })}
            itemsPerPageText={intl.formatMessage({
              id: "pagination.items-per-page",
            })}
            pageNumberText={intl.formatMessage({
              id: "pagination.page-number",
            })}
          />
        </>
      );
    }

    return (
      <>
        <DataTable
          id="eOrderTable"
          rows={eOrdersCurrent.slice((page - 1) * pageSize, page * pageSize)}
          headers={getResponsiveHeaders()}
          isSortable
          expandableRows
          size={windowWidth < 992 ? "sm" : "md"}
        >
          {({
            rows,
            headers,
            getHeaderProps,
            getRowProps,
            getTableProps,
            getTableContainerProps,
          }) => (
            <TableContainer
              title=""
              description=""
              {...getTableContainerProps()}
            >
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    <TableExpandHeader aria-label="expand row" />
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
                  <>
                    {rows.map((row) => (
                      <React.Fragment key={row.id}>
                        <TableExpandRow
                          ariaLabel="row"
                          {...getRowProps({
                            row,
                          })}
                        >
                          {row.cells.map((cell) => renderCell(cell, row))}
                        </TableExpandRow>
                        <TableExpandedRow colSpan={headers.length + 1}>
                          {renderExpandedRow(row)}
                        </TableExpandedRow>
                      </React.Fragment>
                    ))}
                  </>
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
        <Pagination
          onChange={handlePageChange}
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 20, 30]}
          totalItems={eOrdersCurrent.length}
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
      </>
    );
  };

  return (
    <div ref={eOrderRef}>
      {eOrders.length > 0 && (
        <Grid fullWidth>
          <Column sm={4} md={8} lg={16}>
            <div style={{ marginBottom: '1rem' }}>
              <FormattedMessage id="eorder.instructions.enter1" /> <ChevronDown />{" "}
              <FormattedMessage id="eorder.instructions.enter2" /> <TaskAdd />{" "}
              <FormattedMessage id="eorder.instructions.enter3" /> <Edit />{" "}
              <FormattedMessage id="eorder.instructions.enter4" />
            </div>
            {createDataTable(eOrders)}
          </Column>
        </Grid>
      )}
    </div>
  );
};

export default EOrder;