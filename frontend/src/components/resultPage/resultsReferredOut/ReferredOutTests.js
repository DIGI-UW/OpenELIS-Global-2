import React, { useContext, useState, useRef, useEffect } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../../Style.css";
import { encodeDate, getFromOpenElisServer, Roles } from "../../utils/Utils";
import {
  Form,
  Dropdown,
  Heading,
  Grid,
  FilterableMultiSelect,
  Column,
  Section,
  Button,
  Loading,
  Tag,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableBody,
  TableHeader,
  TableRow,
  TableSelectAll,
  TableSelectRow,
  TableCell,
  Pagination,
} from "@carbon/react";
import CustomLabNumberInput from "../../common/CustomLabNumberInput";
import config from "../../../config.json";
import CustomDatePicker from "../../common/CustomDatePicker";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { ConfigurationContext } from "../../layout/Layout";
import { Formik, Field } from "formik";
import ReferredOutTestsFormValues from "../../formModel/innitialValues/ReferredOutTestsFormValues";
import SearchPatientForm from "../../patient/SearchPatientForm";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "referral.label.referredOutTests", link: "/ReferredOutTests" },
];

function ReferredOutTests(props) {
  const [referredOutTestsFormValues, setReferredOutTestsFormValues] = useState(
    ReferredOutTestsFormValues,
  );
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();
  const dateTypeList = [
    {
      id: "option-0",
      text: "Sent Date",
      value: "SENT",
    },
    {
      id: "option-1",
      text: "Result Date",
      value: "RESULT",
    },
  ];

  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [searchByPatient, setSearchByPatient] = useState(true);
  const [searchByUnit, setSearchByUnit] = useState(true);
  const [searchByLabNumber, setSearchByLabNumber] = useState(true);
  const [pageSize, setPageSize] = useState(10);
  const [testUnits, setTestUnits] = useState([]);
  const [testUnitsIdList, setTestUnitsIdList] = useState([]);
  const [testUnitsValuesList, setTestUnitsValuesList] = useState([]);
  const [testUnitsPair, setTestUnitsPair] = useState([]);
  const [testNames, setTestNames] = useState([]);
  const [testNamesIdList, setTestNamesIdList] = useState([]);
  const [testNamesValuesList, setTestNamesValuesList] = useState([]);
  const [testNamesPair, setTestNamesPair] = useState([]);
  const [dateType, setDateType] = useState(dateTypeList[0].value);
  const [loading, setLoading] = useState(false);
  const [searchType, setSearchType] = useState("");
  const [tests, setTests] = useState([]);
  const [testSections, setTestSections] = useState([]);
  const [responseData, setResponseData] = useState({});
  const [responseDataShow, setResponseDataShow] = useState([]);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [selectedRowIdsPost, setSelectedRowIdsPost] = useState([]);

  const handleReferredOutPatient = () => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/ReferredOutTests?searchType=${searchType}&dateType=${dateType}&startDate=${referredOutTestsFormValues.startDate}&endDate=${referredOutTestsFormValues.endDate}&testUnitIds=${testUnitsIdList}&_testUnitIds=1&testIds=${testNamesIdList}&_testIds=1&labNumber=${referredOutTestsFormValues.labNumberInput}&dateOfBirthSearchValue=&selPatient=${referredOutTestsFormValues.selectedPatientId}&_analysisIds=on`,
      handleResponseData,
    );
    setLoading(false);
  };

  const handleResponseData = (res) => {
    if (!res) {
      setLoading(true);
    } else {
      setResponseData(res);
    }
  };

  useEffect(() => {
    if (responseData) {
      var objectsWithId = responseData.referralDisplayItems?.map(
        (obj, index) => ({ ...obj, id: index }),
      );
      setResponseDataShow(objectsWithId);
    }
  }, [responseData]);

  const handleSubmit = () => {
    setLoading(true);
    handleReferredOutPatient();
    setLoading(false);
  };

  function handleLabNumberSearch(e) {
    setReferredOutTestsFormValues({
      ...referredOutTestsFormValues,
      labNumberInput: e.target.value,
    });
    setSearchType(referredOutTestsFormValues.searchTypeValues[1]);
    setSearchByLabNumber(false);
  }

  const getSelectedPatient = (patient) => {
    setSearchType(referredOutTestsFormValues.searchTypeValues[2]);
    setReferredOutTestsFormValues({
      ...referredOutTestsFormValues,
      selectedPatientId: patient.patientPK,
    });
    setSearchByPatient(false);
  };

  const getDataOfBirth = (patient) => {
    searchType(referredOutTestsFormValues.searchTypeValues[2]);
    setReferredOutTestsFormValues({
      ...referredOutTestsFormValues,
      dateOfBirth: patient.birthDateForDisplay,
    });
    setSearchByPatient(false);
  };

  const handleDatePickerChangeDate = (datePicker, date) => {
    let updatedDate = date;
    let obj = null;
    switch (datePicker) {
      case "startDate":
        obj = {
          ...referredOutTestsFormValues,
          startDate: encodeDate(updatedDate),
        };
        break;
      case "endDate":
        obj = {
          ...referredOutTestsFormValues,
          endDate: encodeDate(updatedDate),
        };
        break;
      default:
        obj = {
          // startDate: configurationProperties.currentDateAsText,
          // endDate: configurationProperties.currentDateAsText,
          startDate: "",
          endDate: "",
        };
    }
    setReferredOutTestsFormValues(obj);
    setSearchType(referredOutTestsFormValues.searchTypeValues[0]);
  };

  const fetchTestSections = (response) => {
    setTestSections(response);
  };

  const getTests = (tests) => {
    if (componentMounted.current) {
      setTests(tests);
    }
  };

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
    setSelectedRowIds([]);
  };

  const handleReferredOutPatientPrint = () => {
    let patientReport =
      config.serverBaseUrl +
      `/ReportPrint?report=patientCILNSP_vreduit&type=patient&analysisIds=${selectedRowIdsPost.join(
        ",",
      )}`;
    window.open(patientReport);
  };

  useEffect(() => {
    if (testNames.testNames) {
      var testNamesIdList = testNames.testNames.map((test) => test.id);
      setTestNamesIdList(testNamesIdList);
      var testNamesValueList = testNames.testNames.map((test) => test.value);
      setTestNamesValuesList(testNamesValueList);
      var testNamesPair = testNames.testNames.map((test) => ({
        id: test.id,
        value: test.value,
      }));
      setTestNamesPair(testNamesPair);
      setSearchByUnit(false);
    }
    if (testUnits.testUnits) {
      var testUnitsIdList = testUnits.testUnits.map((test) => test.id);
      setTestUnitsIdList(testUnitsIdList);
      var testUnitsValueList = testUnits.testUnits.map((test) => test.value);
      setTestUnitsValuesList(testUnitsValueList);
      var testUnitsPair = testUnits.testUnits.map((test) => ({
        id: test.id,
        value: test.value,
      }));
      setTestUnitsPair(testUnitsPair);
      setSearchByUnit(false);
    }
  }, [testNames, testUnits]);

  useEffect(() => {
    componentMounted.current = true;
    let testId = new URLSearchParams(window.location.search).get(
      "selectedTest",
    );
    testId = testId ? testId : "";
    getFromOpenElisServer("/rest/test-list", (fetchedTests) => {
      let test = fetchedTests.find((test) => test.id === testId);
      let testLabel = test ? test.value : "";
      setTestUnits(testLabel);
      getTests(fetchedTests);
    });

    let testSectionId = new URLSearchParams(window.location.search).get(
      "testSectionId",
    );
    testSectionId = testSectionId ? testSectionId : "";
    getFromOpenElisServer(
      "/rest/user-test-sections/" + Roles.RESULTS,
      (fetchedTestSections) => {
        let testSection = fetchedTestSections.find(
          (testSection) => testSection.id === testSectionId,
        );
        let testSectionLabel = testSection ? testSection.value : "";
        setTestNames(testSectionLabel);
        fetchTestSections(fetchedTestSections);
      },
    );
  }, []);

  useEffect(() => {
    let patientId = new URLSearchParams(window.location.search).get(
      "patientId",
    );
    if (patientId) {
      let searchValues = {
        ...referredOutTestsFormValues,
        patientId: patientId,
      };
      setReferredOutTestsFormValues(searchValues);
      handleSubmit(searchValues);
      setSearchByPatient(false);
    }
  }, []);

  useEffect(() => {
    if (selectedRowIds.length > 0) {
      const selectedAnalysisIds = selectedRowIds.map((index) => {
        return responseDataShow[index]?.analysisId;
      });
      setSelectedRowIdsPost(selectedAnalysisIds);
    } else {
      setSelectedRowIdsPost([]);
    }
  }, [selectedRowIds, responseDataShow]);

  const renderCell = (cell, row, rowIndex) => {
    if (cell.info.header === "select") {
      return (
        <TableSelectRow
          key={cell.id}
          id={cell.id}
          checked={selectedRowIds.includes(rowIndex)}
          name="selectRowCheckbox"
          ariaLabel="selectRows"
          onSelect={() => {
            if (selectedRowIds.includes(rowIndex)) {
              setSelectedRowIds(
                selectedRowIds.filter((selectedId) => selectedId !== rowIndex),
              );
            } else {
              setSelectedRowIds([...selectedRowIds, rowIndex]);
            }
          }}
        />
      );
    } else if (cell.info.header === "active") {
      return <TableCell key={cell.id}>{cell.value.toString()}</TableCell>;
    } else if (cell.info.header === "notes") {
      return (
        <TableCell key={cell.id}>
          <div dangerouslySetInnerHTML={{ __html: cell.value }} />
        </TableCell>
      );
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="referral.out.head" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      {loading && <Loading />}
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <h5>
                <FormattedMessage id="referral.main.button" />
              </h5>
            </Section>
          </Column>
          <br />
          <Column lg={16} md={8} sm={4}>
            <SearchPatientForm
              getSelectedPatient={getSelectedPatient}
              // getDataOfBirth={getDataOfBirth}
              onChange={() => {
                setSearchByPatient(false);
              }}
            ></SearchPatientForm>
          </Column>
          <br></br>
          <Column lg={16} md={8} sm={4}>
            <Button
              data-cy="referralsByPatient"
              type="button"
              disabled={searchByPatient}
              onClick={handleReferredOutPatient}
            >
              <FormattedMessage
                id="referral.main.button"
                defaultMessage="Search Referrals By Patient"
              />
            </Button>
          </Column>
        </Grid>
        <hr />
        <br></br>
        <Formik
          initialValues={referredOutTestsFormValues}
          enableReinitialize={true}
          // validationSchema={}
          onSubmit={handleSubmit}
          onChange={() => {
            setSearchByUnit(false);
          }}
        >
          {({
            values,
            //errors,
            //touched,
            setFieldValue,
            handleChange,
            handleBlur,
            handleSubmit,
          }) => (
            <Form
              onSubmit={handleSubmit}
              onChange={handleChange}
              onBlur={handleBlur}
            >
              <Field name="guid">
                {({ field }) => (
                  <input type="hidden" name={field.name} id={field.name} />
                )}
              </Field>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <h5>
                    <FormattedMessage id="referral.out.request" />
                  </h5>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>
                <Column lg={4} md={8} sm={4}>
                  <Dropdown
                    id={"dateType"}
                    name="dateType"
                    label={
                      dateTypeList.find((item) => item.value === dateType)
                        ?.text || ""
                    }
                    initialSelectedItem={dateTypeList.find(
                      (item) => item.value === dateType,
                    )}
                    items={dateTypeList}
                    itemToString={(item) => (item ? item.text : "")}
                    onChange={(item) => {
                      setSearchType(
                        referredOutTestsFormValues.searchTypeValues[0],
                      );
                      setDateType(item.selectedItem.value);
                      setSearchByUnit(false);
                    }}
                  />
                </Column>
                <Column lg={12} md={8} sm={4}>
                  <FormattedMessage id="referral.out.note" />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>

                <Column lg={4} md={8} sm={4}>
                  <CustomDatePicker
                    id={"startDate"}
                    labelText={intl.formatMessage({
                      id: "eorder.date.start",
                      defaultMessage: "Start Date",
                    })}
                    autofillDate={true}
                    value={referredOutTestsFormValues.startDate}
                    className="inputDate"
                    onChange={(date) => {
                      handleDatePickerChangeDate("startDate", date);
                    }}
                  />
                </Column>
                <Column lg={4} md={8} sm={4}>
                  <CustomDatePicker
                    id={"endDate"}
                    labelText={intl.formatMessage({
                      id: "eorder.date.end",
                      defaultMessage: "End Date",
                    })}
                    className="inputDate"
                    autofillDate={true}
                    value={referredOutTestsFormValues.endDate}
                    onChange={(date) => {
                      handleDatePickerChangeDate("endDate", date);
                    }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>

                <Column lg={4} md={8} sm={4}>
                  <FilterableMultiSelect
                    id="testunits"
                    titleText={intl.formatMessage({
                      id: "search.label.testunit",
                      defaultMessage: "Select Test Unit",
                    })}
                    items={testSections}
                    itemToString={(item) => (item ? item.value : "")}
                    onChange={(changes) => {
                      setTestUnits({
                        ...testUnits,
                        testUnits: changes.selectedItems,
                      });
                      setSearchType(
                        referredOutTestsFormValues.searchTypeValues[0],
                      );
                      setSearchByUnit(false);
                    }}
                    selectionFeedback="top-after-reopen"
                  />
                </Column>

                <Column lg={12} md={8} sm={4}>
                  {testUnits.testUnits &&
                    testUnits.testUnits.map((test, index) => (
                      <Tag
                        key={index}
                        filter
                        onClose={() => {
                          var info = { ...testUnits };
                          info["testUnits"].splice(index, 1);
                          setTestUnits(info);
                        }}
                      >
                        {test.value}
                      </Tag>
                    ))}
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>
                <Column lg={4} md={8} sm={4}>
                  <FilterableMultiSelect
                    id="testnames"
                    titleText={intl.formatMessage({
                      id: "search.label.test",
                      defaultMessage: "Select Test Name",
                    })}
                    items={tests}
                    itemToString={(item) => (item ? item.value : "")}
                    onChange={(changes) => {
                      setTestNames({
                        ...testNames,
                        testNames: changes.selectedItems,
                      });
                      setSearchType(
                        referredOutTestsFormValues.searchTypeValues[0],
                      );
                      setSearchByUnit(false);
                    }}
                    selectionFeedback="top-after-reopen"
                  />
                </Column>

                <Column lg={12} md={8} sm={4}>
                  {testNames.testNames &&
                    testNames.testNames.map((test, index) => (
                      <Tag
                        key={index}
                        filter
                        onClose={() => {
                          var info = { ...testNames };
                          info["testNames"].splice(index, 1);
                          setTestNames(info);
                        }}
                      >
                        {test.value}
                      </Tag>
                    ))}
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>

                <Column lg={4} md={8} sm={4}>
                  <Button
                    data-cy="byUnitsAndTests"
                    type="button"
                    disabled={searchByUnit}
                    onClick={handleReferredOutPatient}
                  >
                    <FormattedMessage
                      id="referral.button.unitTestSearch"
                      defaultMessage="Search Referrals By Unit(s) & Test(s)"
                    />
                  </Button>
                </Column>
              </Grid>
              <hr />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <h5>
                    <FormattedMessage id="referral.result.labNumber" />
                  </h5>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>

                <Column lg={8} md={8} sm={4}>
                  <Field name="labNumberInput">
                    {({ field }) => (
                      <CustomLabNumberInput
                        name={field.name}
                        labelText={intl.formatMessage({
                          id: "referral.input",
                          defaultMessage: "Scan OR Enter Manually",
                        })}
                        id={field.name}
                        value={values[field.name]}
                        onChange={(e, rawValue) => {
                          setFieldValue(field.name, rawValue);
                          setSearchType(
                            referredOutTestsFormValues.searchTypeValues[1],
                          );
                          handleLabNumberSearch(e);
                        }}
                      />
                    )}
                  </Field>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>

                <Column lg={4} md={8} sm={4}>
                  <Button
                    data-cy="byLabNumber"
                    type="button"
                    disabled={searchByLabNumber}
                    onClick={handleReferredOutPatient}
                  >
                    <FormattedMessage
                      id="referral.button.labSearch"
                      defaultMessage="Search Referrals By Lab Number"
                    />
                  </Button>
                </Column>
              </Grid>
              <hr />
            </Form>
          )}
        </Formik>
        <br />
        <Grid fullWidth={true}>
          <Column lg={4} md={8} sm={4}>
            <span>
              <FormattedMessage id="referral.matching.search" /> :
            </span>{" "}
          </Column>
          <Column lg={4} md={8} sm={4}>
            <Button
              disabled={selectedRowIds.length === 0}
              kind="tertiary"
              type="button"
              data-cy="print-report"
              onClick={handleReferredOutPatientPrint}
            >
              <FormattedMessage
                id="referral.print.selected.patient.reports"
                defaultMessage="Print Selected Patient Reports"
              />
            </Button>{" "}
          </Column>
          <Column lg={4} md={8} sm={4}>
            {responseDataShow && (
              <Button
                disabled={selectedRowIds.length === responseDataShow.length}
                kind="tertiary"
                type="button"
                data-cy="select-all-button"
                onClick={() => {
                  const currentPageIndexes = responseDataShow
                    .slice((page - 1) * pageSize, page * pageSize)
                    .filter((row) => !row.disabled)
                    .map((_, index) => index);

                  setSelectedRowIds(currentPageIndexes);
                }}
              >
                <FormattedMessage
                  id="referral.print.selected.patient.reports.selectall.button"
                  defaultMessage="Select All"
                />
              </Button>
            )}{" "}
          </Column>
          <Column lg={4} md={8} sm={4}>
            <Button
              disabled={selectedRowIds.length === 0}
              kind="tertiary"
              type="button"
              data-cy="select-none-button"
              onClick={() => setSelectedRowIds([])}
            >
              <FormattedMessage
                id="referral.print.selected.patient.reports.selectnone.button"
                defaultMessage="Select None"
              />
            </Button>
          </Column>
        </Grid>
        <br />
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={16} md={8} sm={4}>
            <br />
            {responseDataShow && (
              <DataTable
                rows={responseDataShow.slice(
                  (page - 1) * pageSize,
                  page * pageSize,
                )}
                headers={[
                  {
                    key: "select",
                    header: intl.formatMessage({
                      id: "organization.type.CI.select",
                    }),
                  },
                  {
                    key: "resultDate",
                    header: intl.formatMessage({
                      id: "referral.search.column.resultDate",
                    }),
                  },
                  {
                    key: "accessionNumber",
                    header: intl.formatMessage({
                      id: "sample.label.labnumber",
                    }),
                  },
                  {
                    key: "referredSendDate",
                    header: intl.formatMessage({
                      id: "referral.search.column.sentDate",
                    }),
                  },
                  {
                    key: "referralStatusDisplay",
                    header: intl.formatMessage({
                      id: "label.filters.status",
                    }),
                  },
                  {
                    key: "patientLastName",
                    header: intl.formatMessage({
                      id: "eorder.name.last",
                    }),
                  },
                  {
                    key: "patientFirstName",
                    header: intl.formatMessage({
                      id: "eorder.name.first",
                    }),
                  },
                  {
                    key: "referringTestName",
                    header: intl.formatMessage({
                      id: "eorder.test.name",
                    }),
                  },
                  {
                    key: "referralResultsDisplay",
                    header: intl.formatMessage({
                      id: "column.name.result",
                    }),
                  },
                  {
                    key: "referenceLabDisplay",
                    header: intl.formatMessage({
                      id: "referral.search.column.referenceLab",
                    }),
                  },
                  {
                    key: "notes",
                    header: intl.formatMessage({
                      id: "column.name.notes",
                    }),
                  },
                ]}
              >
                {({
                  rows,
                  headers,
                  getHeaderProps,
                  getTableProps,
                  getSelectionProps,
                }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          <TableSelectAll
                            id="table-select-all"
                            {...getSelectionProps()}
                            checked={
                              selectedRowIds.length === pageSize &&
                              responseDataShow
                                .slice((page - 1) * pageSize, page * pageSize)
                                .filter(
                                  (row, index) =>
                                    !row.disabled &&
                                    selectedRowIds.includes(index),
                                ).length === pageSize
                            }
                            indeterminate={
                              selectedRowIds.length > 0 &&
                              selectedRowIds.length <
                                responseDataShow
                                  .slice((page - 1) * pageSize, page * pageSize)
                                  .filter((row) => !row.disabled).length
                            }
                            onSelect={() => {
                              const currentPageIds = responseDataShow
                                .slice((page - 1) * pageSize, page * pageSize)
                                .filter((row) => !row.disabled)
                                .map((row, index) => index);
                              if (
                                selectedRowIds.length === pageSize &&
                                currentPageIds.every((index) =>
                                  selectedRowIds.includes(index),
                                )
                              ) {
                                setSelectedRowIds([]);
                              } else {
                                setSelectedRowIds(
                                  currentPageIds.filter(
                                    (index) => !selectedRowIds.includes(index),
                                  ),
                                );
                              }
                            }}
                          />
                          {headers.map(
                            (header) =>
                              header.key !== "select" && (
                                <TableHeader
                                  key={header.key}
                                  {...getHeaderProps({ header })}
                                >
                                  {header.header}
                                </TableHeader>
                              ),
                          )}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <>
                          {rows.map((row, rowIndex) => (
                            <TableRow
                              key={row.id}
                              onClick={() => {
                                const index = responseDataShow.findIndex(
                                  (item) => item.id === row.id,
                                );
                                const isSelected =
                                  selectedRowIds.includes(index);
                                if (isSelected) {
                                  setSelectedRowIds(
                                    selectedRowIds.filter(
                                      (selectedId) => selectedId !== index,
                                    ),
                                  );
                                } else {
                                  setSelectedRowIds([...selectedRowIds, index]);
                                }
                              }}
                            >
                              {row.cells.map((cell) =>
                                renderCell(cell, row, rowIndex),
                              )}
                            </TableRow>
                          ))}
                        </>
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            )}
            {responseDataShow && (
              <Pagination
                onChange={handlePageChange}
                page={page}
                pageSize={pageSize}
                pageSizes={[5, 10, 20]}
                totalItems={responseDataShow.length}
                forwardText={intl.formatMessage({
                  id: "pagination.forward",
                })}
                backwardText={intl.formatMessage({
                  id: "pagination.backward",
                })}
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
            )}
            <br />
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(ReferredOutTests);
