import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  Link,
  Row,
  Stack,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Column,
  TextInput,
  Checkbox,
} from "@carbon/react";
import { Add } from "@carbon/react/icons";
import { getFromOpenElisServer } from "../utils/Utils";
import SampleType from "../addOrder/SampleType";
import { FormattedMessage, useIntl } from "react-intl";
import {
  OrderCurrentTestsHeaders,
  OrderPossibleTestsHeaders,
} from "../data/orderCurrentTestsHeaders";
const EditSample = (props) => {
  const { samples, setSamples, orderFormValues, setOrderFormValues, error } =
    props;

  const componentMounted = useRef(false);

  const intl = useIntl();

  const [elementsCounter, setElementsCounter] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [page2, setPage2] = useState(1);
  const [pageSize2, setPageSize2] = useState(5);

  const [rejectSampleReasons, setRejectSampleReasons] = useState([]);

  const handleAddNewSample = () => {
    let updateSamples = [...samples];
    let count = elementsCounter + 1;
    updateSamples.push({
      index: count,
      sampleRejected: false,
      rejectionReason: "",
      requestReferralEnabled: false,
      referralItems: [],
      sampleTypeId: "",
      sampleXML: null,
      panels: [],
      tests: [],
    });
    setSamples(updateSamples);
    setElementsCounter(count);
  };
  const formatTestsObject = (tests) => {
    return tests.map((test) => {
      test.id = test.testId;
      if (!test.accessionNumber) {
        test.accessionNumber = "";
      }
      if (!test.sampleType) {
        test.sampleType = "";
      }
      if (!test.collectionDate) {
        test.collectionDate = "";
      }
      if (!test.collectionTime) {
        test.collectionTime = "";
      }
      return test;
    });
  };
  const handleChecked = (e, testId) => {
    var tests = [];
    var updatedTests = [];
    if (e.currentTarget.name === "add") {
      tests = orderFormValues.possibleTests;
      updatedTests = tests.map((test) => {
        if (test.testId === testId) {
          return { ...test, add: e.currentTarget.checked };
        } else {
          return test;
        }
      });
      setOrderFormValues({
        ...orderFormValues,
        possibleTests: updatedTests,
      });
    } else if (e.currentTarget.name === "removeSample") {
      tests = orderFormValues.existingTests;
      updatedTests = tests.map((test) => {
        if (test.testId === testId) {
          return { ...test, removeSample: e.currentTarget.checked };
        }
        {
          return test;
        }
      });
      setOrderFormValues({
        ...orderFormValues,
        existingTests: updatedTests,
      });
    } else if (e.currentTarget.name === "canceled") {
      tests = orderFormValues.existingTests;
      updatedTests = tests.map((test) => {
        if (test.testId === testId) {
          return { ...test, canceled: e.currentTarget.checked };
        }
        {
          return test;
        }
      });
      setOrderFormValues({
        ...orderFormValues,
        existingTests: updatedTests,
      });
    }
  };

  const sampleTypeObject = (object) => {
    let newState = [...samples];
    switch (true) {
      case object.sampleTypeId !== undefined && object.sampleTypeId !== "":
        newState[object.sampleObjectIndex].sampleTypeId = object.sampleTypeId;
        break;
      case object.sampleRejected:
        newState[object.sampleObjectIndex].sampleRejected =
          object.sampleRejected;
        break;
      case object.rejectionReason !== undefined &&
        object.rejectionReason !== null:
        newState[object.sampleObjectIndex].rejectionReason =
          object.rejectionReason;
        break;
      case object.selectedTests !== undefined &&
        object.selectedTests.length > 0:
        newState[object.sampleObjectIndex].tests = object.selectedTests;
        break;
      case object.selectedPanels !== undefined &&
        object.selectedPanels.length > 0:
        newState[object.sampleObjectIndex].panels = object.selectedPanels;
        break;
      case object.sampleXML !== undefined && object.sampleXML !== null:
        newState[object.sampleObjectIndex].sampleXML = object.sampleXML;
        break;
      case object.requestReferralEnabled:
        newState[object.sampleObjectIndex].requestReferralEnabled =
          object.requestReferralEnabled;
        break;
      case object.referralItems !== undefined &&
        object.referralItems.length > 0:
        newState[object.sampleObjectIndex].referralItems = object.referralItems;
        break;
      default:
        props.setSamples(newState);
    }
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const handlePageChange2 = (pageInfo) => {
    if (page2 != pageInfo.page) {
      setPage2(pageInfo.page);
    }

    if (pageSize2 != pageInfo.pageSize) {
      setPageSize2(pageInfo.pageSize);
    }
  };

  const removeSample = (index) => {
    let updateSamples = samples.splice(index, 1);
    setSamples(updateSamples);
  };

  const fetchRejectSampleReasons = (res) => {
    if (componentMounted.current) {
      setRejectSampleReasons(res);
    }
  };

  const handleRemoveSample = (e, sample) => {
    e.preventDefault();
    let filtered = samples.filter(function (element) {
      return element !== sample;
    });
    setSamples(filtered);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/test-rejection-reasons",
      fetchRejectSampleReasons,
    );
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/test-rejection-reasons",
      fetchRejectSampleReasons,
    );
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const renderCell = (cell, row) => {
    var accession = row.cells.find(
      (e) => e.info.header === "accessionNumber",
    ).value;
    if (cell.info.header === "accessionNumber") {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    } else if (cell.info.header === "sampleType") {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    } else if (cell.info.header === "collectionDate") {
      return (
        <TableCell key={cell.id}>
          <TextInput
            id={cell.id + cell.info.header}
            labelText=""
            value={cell.value}
          ></TextInput>
        </TableCell>
      );
    } else if (cell.info.header === "collectionTime") {
      return (
        <TableCell key={cell.id}>
          <TextInput
            id={cell.id + cell.info.header}
            labelText=""
            value={cell.value}
          ></TextInput>
        </TableCell>
      );
    } else if (cell.info.header === "removeSample") {
      return (
        <>
          {accession !== "" ? (
            <TableCell key={cell.id}>
              <Checkbox
                id={cell.id + cell.info.header}
                labelText=""
                name="removeSample"
                checked={cell.value}
                onChange={(e) => handleChecked(e, row.id)}
              ></Checkbox>
            </TableCell>
          ) : (
            <TableCell key={cell.id}></TableCell>
          )}
        </>
      );
    } else if (cell.info.header === "testName") {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    } else if (cell.info.header === "hasResults") {
      return (
        <TableCell key={cell.id}>
          <Checkbox
            id={cell.id + cell.info.header}
            labelText=""
            checked={cell.value}
          ></Checkbox>
        </TableCell>
      );
    } else if (cell.info.header === "canceled") {
      return (
        <TableCell key={cell.id}>
          <Checkbox
            id={cell.id + cell.info.header}
            labelText=""
            name="canceled"
            checked={cell.value}
            onChange={(e) => handleChecked(e, row.id)}
          ></Checkbox>
        </TableCell>
      );
    } else if (cell.info.header === "add") {
      return (
        <TableCell key={cell.id}>
          <Checkbox
            id={cell.id + cell.info.header}
            labelText=""
            name="add"
            checked={cell.value}
            onChange={(e) => handleChecked(e, row.id)}
          ></Checkbox>
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}></TableCell>;
    }
  };

  return (
    <>
      <div className="orderLegendBody">
        <Column lg={16}>
          <DataTable
            rows={formatTestsObject(orderFormValues.existingTests)}
            headers={OrderCurrentTestsHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({ id: "currentests.title" })}
              >
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
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
                      {rows
                        .slice((page - 1) * pageSize)
                        .slice(0, pageSize)
                        .map((row) => (
                          <TableRow key={row.id}>
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
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
            pageSizes={[5, 10, 20, 30]}
            totalItems={orderFormValues.existingTests.length}
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
      </div>
      <div className="orderLegendBody">
        <Column lg={16}>
          <DataTable
            rows={formatTestsObject(orderFormValues.possibleTests)}
            headers={OrderPossibleTestsHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({ id: "availabletests.title" })}
              >
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
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
                      {rows
                        .slice((page2 - 1) * pageSize2)
                        .slice(0, pageSize2)
                        .map((row) => (
                          <TableRow key={row.id}>
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
                        ))}
                    </>
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <Pagination
            onChange={handlePageChange2}
            page={page2}
            pageSize={pageSize2}
            pageSizes={[5, 10, 20, 30]}
            totalItems={orderFormValues.possibleTests.length}
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
      </div>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="order.label.add" />
          </h3>
          {samples.map((sample, i) => {
            return (
              <div className="sampleType" key={i}>
                <h4>
                  <FormattedMessage id="label.button.sample" /> {i + 1}
                </h4>
                <Link href="#" onClick={(e) => handleRemoveSample(e, sample)}>
                  {<FormattedMessage id="sample.remove.action" />}
                </Link>
                <SampleType
                  index={i}
                  rejectSampleReasons={rejectSampleReasons}
                  removeSample={removeSample}
                  sample={sample}
                  setSample={(newSample) => {
                    let newSamples = [...samples];
                    newSamples[i] = newSample;
                    setSamples(newSamples);
                  }}
                  sampleTypeObject={sampleTypeObject}
                  error={error}
                />
              </div>
            );
          })}
          <Row>
            <div className="inlineDiv">
              <Button onClick={handleAddNewSample}>
                {<FormattedMessage id="sample.add.action" />}
                &nbsp; &nbsp;
                <Add size={16} />
              </Button>
            </div>
          </Row>
        </div>
      </Stack>
    </>
  );
};

export default EditSample;
