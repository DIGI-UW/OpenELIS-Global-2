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
  InlineNotification,
} from "@carbon/react";
import { Add } from "@carbon/react/icons";
import { getFromOpenElisServer } from "../utils/Utils";
import SampleType from "../addOrder/SampleType";
import {
  applySampleTypeUpdate,
  newSampleKey,
} from "../addOrder/sampleTypeUpdate";
import { FormattedMessage, useIntl } from "react-intl";
import { OrderCurrentTestsHeaders } from "../data/orderCurrentTestsHeaders";
import ExistingSampleTests from "./ExistingSampleTests";
/**
 * The row id of a test on Modify Order: the analysis for a current test, the
 * sample item and test for one that can be added. A test id alone repeats
 * whenever two samples carry the same test, which made those rows share one
 * React key and one checkbox, so ticking one ticked the other.
 */
export const editTestRowId = (test) =>
  test.analysisId
    ? "analysis-" + test.analysisId
    : "item-" + test.sampleItemId + "-test-" + test.testId;

/**
 * The order's available tests grouped by the existing sample they can be added
 * to. The server names the sample (accession number, sample type) on the first
 * row of each group only.
 */
export const availableTestsBySample = (possibleTests) => {
  const groups = [];
  (possibleTests || []).forEach((test) => {
    let group = groups.find((g) => g.sampleItemId === test.sampleItemId);
    if (!group) {
      group = {
        sampleItemId: test.sampleItemId,
        accessionNumber: "",
        sampleType: "",
        sampleTypeId: "",
        tests: [],
      };
      groups.push(group);
    }
    group.accessionNumber = group.accessionNumber || test.accessionNumber || "";
    group.sampleType = group.sampleType || test.sampleType || "";
    group.sampleTypeId = group.sampleTypeId || test.sampleTypeId || "";
    group.tests.push(test);
  });
  return groups;
};

const EditSample = (props) => {
  const { samples, setSamples, orderFormValues, setOrderFormValues, error } =
    props;

  const componentMounted = useRef(false);

  const intl = useIntl();

  const [elementsCounter, setElementsCounter] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  const [rejectSampleReasons, setRejectSampleReasons] = useState([]);

  const handleAddNewSample = () => {
    let updateSamples = [...samples];
    let count = elementsCounter + 1;
    updateSamples.push({
      key: newSampleKey(),
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
      test.id = editTestRowId(test);
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
  const setTestsAdded = (sampleItemId, testIds, add) => {
    const ids = new Set(testIds);
    setOrderFormValues({
      ...orderFormValues,
      possibleTests: (orderFormValues.possibleTests || []).map((test) =>
        test.sampleItemId === sampleItemId && ids.has(test.testId)
          ? { ...test, add }
          : test,
      ),
    });
  };

  const handleChecked = (e, rowId) => {
    var tests = [];
    var updatedTests = [];
    if (e.currentTarget.name === "removeSample") {
      tests = orderFormValues.existingTests;
      updatedTests = tests.map((test) => {
        if (editTestRowId(test) === rowId) {
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
        if (editTestRowId(test) === rowId) {
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
    setSamples((currentSamples) =>
      applySampleTypeUpdate(currentSamples, object),
    );
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
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
      "/rest/displayList/REJECTION_REASONS",
      fetchRejectSampleReasons,
    );
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/displayList/REJECTION_REASONS",
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
      return accession !== "" ? (
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
    } else {
      return <TableCell key={cell.id}></TableCell>;
    }
  };

  const availableSamples = availableTestsBySample(
    orderFormValues.possibleTests,
  );

  return (
    <>
      <div className="orderLegendBody">
        <Column lg={16}>
          <DataTable
            rows={formatTestsObject(orderFormValues.existingTests)}
            headers={OrderCurrentTestsHeaders(intl)}
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
      <div className="orderLegendBody" data-testid="available-tests">
        <h3>
          <FormattedMessage id="availabletests.title" />
        </h3>
        {availableSamples.length === 0 && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "availabletests.none" })}
          />
        )}
        {availableSamples.map((sample, index) => (
          <ExistingSampleTests
            key={"available-" + (sample.sampleItemId || index)}
            sampleItemId={sample.sampleItemId}
            accessionNumber={sample.accessionNumber}
            sampleType={sample.sampleType}
            sampleTypeId={sample.sampleTypeId}
            tests={sample.tests}
            onAddChange={(testIds, add) =>
              setTestsAdded(sample.sampleItemId, testIds, add)
            }
          />
        ))}
      </div>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="order.label.add" />
          </h3>
          {samples.map((sample, i) => {
            return (
              <div className="sampleType" key={sample.key ?? i}>
                <h4>
                  <FormattedMessage id="label.button.sample" /> {i + 1}
                </h4>
                <Link href="#" onClick={(e) => handleRemoveSample(e, sample)}>
                  {<FormattedMessage id="sample.remove.action" />}
                </Link>
                <SampleType
                  index={i}
                  rejectSampleReasons={rejectSampleReasons}
                  sample={sample}
                  setSample={(newSample) => {
                    let newSamples = [...samples];
                    newSamples[i] = newSample;
                    setSamples(newSamples);
                  }}
                  sampleTypeObject={sampleTypeObject}
                  error={error}
                  allowReferral={false}
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
