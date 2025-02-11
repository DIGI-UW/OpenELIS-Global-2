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
} from "@carbon/react";

import { FormattedMessage, useIntl } from "react-intl";
import { ChevronDown, Edit, TaskAdd } from "@carbon/icons-react";
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

  useEffect(() => {}, []);

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

  function markRowOutOfSync(index) {
    // jQuery("#enterButton_" + index).attr("disabled", "disabled");
    // jQuery("#editButton_" + index).attr("disabled", "disabled");
    // jQuery("#eOrderRow_" + index).addClass("unsynced-resource");
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
    getFromOpenElisServer("/rest/SampleEntryGenerateScanProvider", (res) => {
      handleGeneratedAccessionNo(res, index);
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
      <>
        <div className="formInlineDiv">
          <div className="formInlineDiv">
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
              className="inputText"
            />
            <span className="middleAlignVertical">
              <Button
                type="button"
                kind="tertiary"
                label={intl.formatMessage({ id: "eorder.button.editOrder" })}
                hasIconOnly={true}
                renderIcon={Edit}
                iconDescription={intl.formatMessage({
                  id: "eorder.button.editOrder",
                })}
                onClick={() => {
                  editOrder(electronicOrderUUID, eOrders[index].labNo);
                }}
              />
              <Button
                type="button"
                kind="primary"
                label={intl.formatMessage({ id: "eorder.button.enterOrder" })}
                hasIconOnly={true}
                renderIcon={TaskAdd}
                iconDescription={intl.formatMessage({
                  id: "eorder.button.enterOrder",
                })}
                onClick={() => {
                  saveEntry(electronicOrderUUID, eOrders[index].labNo);
                }}
              />
            </span>
          </div>
          <div className="formInlineInput">
            <div className="inputText"></div>
          </div>
        </div>
        <div></div>
      </>
    );
  };

  const renderCell = (cell, row) => {
    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const createDataTable = (eOrdersCurrent) => {
    return (
      <>
        <DataTable
          id="eOrderTable"
          rows={eOrdersCurrent.slice((page - 1) * pageSize, page * pageSize)}
          headers={[
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
          ]}
          isSortable
          expandableRows
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
        <div className="orderLegendBody">
          <FormattedMessage id="eorder.instructions.enter1" /> <ChevronDown />{" "}
          <FormattedMessage id="eorder.instructions.enter2" /> <TaskAdd />{" "}
          <FormattedMessage id="eorder.instructions.enter3" /> <Edit />{" "}
          <FormattedMessage id="eorder.instructions.enter4" />
          <br></br>
          {createDataTable(eOrders)}
        </div>
      )}
    </div>
  );
};

export default EOrder;
