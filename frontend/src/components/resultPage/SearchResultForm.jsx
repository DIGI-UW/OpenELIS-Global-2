import React, { useContext, useEffect, useState, useRef } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  convertAlphaNumLabNumForDisplay,
  Roles,
} from "../utils/Utils";
import {
  Form,
  TextInput,
  TextArea,
  Checkbox,
  Button,
  Grid,
  Column,
  Stack,
  Pagination,
  Select,
  SelectItem,
  Loading,
  Link,
  FileUploader,
} from "@carbon/react";
import { Copy, ArrowLeft, ArrowRight } from "@carbon/icons-react";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import DataTable from "react-data-table-component";
import { Formik, Field } from "formik";
import { jpGet, jpSet } from "../utils/JsonPath";
import SearchResultFormValues from "../formModel/innitialValues/SearchResultFormValues";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import SearchPatientForm from "../patient/SearchPatientForm";
import SendToAnalyzerButton from "../modifyOrder/SendToAnalyzerButton";
import ReferredOutTests from "./resultsReferredOut/ReferredOutTests";
import { ConfigurationContext } from "../layout/Layout";
import config from "../../config.json";
import CustomDatePicker from "../common/CustomDatePicker";
import CustomTimePicker from "../common/CustomTimePicker";
import AsyncAvatar from "../patient/photoManagement/photoAvatar/AyncAvatar";
import CompactFileInput from "./fileUpload/FileInput";
import LocationPickerModal from "../storage/LocationPicker/LocationPickerModal";
import {
  getDeepestLocationSelection,
  positionToCoordinate,
} from "../storage/LocationPicker/locationSelectionMapper";
import { isStorageAssignmentSuccess } from "../storage/LocationPicker/storageAssignmentResponse";
import ResultMultiSelect from "../common/multiSelect";
import CascadingMultiSelect from "../common/cascadingMultiSelect";
import EQABadge from "../eqa/EQABadge";
import InlineNceForm from "../nonconform/common/InlineNceForm";
import { Warning } from "@carbon/icons-react";
import ESignatureButton, {
  SignatureMeaning,
} from "../esignature/ESignatureButton";
import AcceptUnconditionallyGuard from "./AcceptUnconditionallyGuard";

/**
 * Value for `labNumber` on /rest/LogbookResults. Strips only the legacy
 * two-segment pattern {@code BASE-SUFFIX} where SUFFIX is numeric (analysis ordinal).
 * Multi-segment accessions (e.g. harness {@code HARN-QS7-2026-00001}) must stay intact.
 */
function labNumberForLogbookSearch(accessionNumber) {
  if (!accessionNumber) {
    return "";
  }
  const trimmed = accessionNumber.trim();
  const parts = trimmed.split("-");
  if (parts.length === 2 && /^\d+$/.test(parts[1])) {
    return parts[0];
  }
  return trimmed;
}

function ResultSearchPage() {
  const [originalResultForm, setOriginalResultForm] = useState({
    testResult: [],
  });
  const [resultForm, setResultForm] = useState(originalResultForm);
  const [searchBy, setSearchBy] = useState({ type: "", doRange: false });
  const [param, setParam] = useState("&accessionNumber=");

  const setResults = (resultForm) => {
    setOriginalResultForm(resultForm);
    setResultForm(resultForm);
  };

  // Single-accession context → offer LIS-initiated dispatch of this order to an
  // analyzer. Derived from the active accession-number search param (set when the
  // page is loaded/searched by accession, e.g. /AccessionResults?accessionNumber=…);
  // suppressed for range/logbook/patient searches that span multiple accessions.
  const accessionMatch = /accessionNumber=([^&]+)/.exec(param || "");
  const loadedAccession =
    accessionMatch &&
    accessionMatch[1] &&
    !(param || "").includes("upperAccessionNumber")
      ? decodeURIComponent(accessionMatch[1])
      : "";
  const hasResults = (resultForm?.testResult?.length || 0) > 0;

  return (
    <>
      <SearchResultForm
        setParam={setParam}
        setSearchBy={setSearchBy}
        setResults={setResults}
      />
      {loadedAccession && hasResults && (
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <div style={{ margin: "0.5rem 0" }}>
              <SendToAnalyzerButton accessionNumber={loadedAccession} />
            </div>
          </Column>
        </Grid>
      )}
      <SearchResults
        extraParams={param}
        searchBy={searchBy}
        results={resultForm}
        setResultForm={setResultForm}
        refreshOnSubmit={true}
      />
    </>
  );
}

export function SearchResultForm(props) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [tests, setTests] = useState([]);
  const [analysisStatusTypes, setAnalysisStatusTypes] = useState([]);
  const [sampleStatusTypes, setSampleStatusTypes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchBy, setSearchBy] = useState({ type: "", doRange: false });
  const [patient, setPatient] = useState({ patientPK: "" });
  const [testSections, setTestSections] = useState([]);
  const [defaultTestSectionId, setDefaultTestSectionId] = useState("");
  const [defaultTestSectionLabel, setDefaultTestSectionLabel] = useState("");
  const [defaultTestId, setDefaultTestId] = useState("");
  const [defaultTestLabel, setDefaultTestLabel] = useState("");
  const [defaultSampleStatusId, setDefaultSampleStatusId] = useState("");
  const [defaultSampleStatusLabel, setDefaultSampleStatusLabel] = useState("");
  const [defaultAnalysisStatusId, setDefaultAnalysisStatusId] = useState("");
  const [defaultAnalysisStatusLabel, setDefaultAnalysisStatusLabel] =
    useState("");
  const [searchFormValues, setSearchFormValues] = useState(
    SearchResultFormValues,
  );
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [url, setUrl] = useState("");
  const componentMounted = useRef(false);

  const setResultsWithId = (results) => {
    if (results.testResult) {
      var i = 0;
      if (results.testResult) {
        results.testResult.forEach((item) => (item.id = "" + i++));
      }
      props.setResults?.(results);
      setLoading(false);
      if (results.paging) {
        var { totalPages, currentPage } = results.paging;
        if (totalPages > 1) {
          setPagination(true);
          setCurrentApiPage(currentPage);
          setTotalApiPages(totalPages);
          if (parseInt(currentPage) < parseInt(totalPages)) {
            setNextPage(parseInt(currentPage) + 1);
          } else {
            setNextPage(null);
          }
          if (parseInt(currentPage) > 1) {
            setPreviousPage(parseInt(currentPage) - 1);
          } else {
            setPreviousPage(null);
          }
        }
      }
    } else {
      props.setResults?.({ testResult: [] });
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "patient.search.nopatient" }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      setLoading(false);
    }
  };

  const intl = useIntl();

  const loadNextResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(url + "&page=" + nextPage, setResultsWithId);
  };

  const loadPreviousResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(url + "&page=" + previousPage, setResultsWithId);
  };

  const getSelectedPatient = (patient) => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    setPatient(patient);
  };
  useEffect(() => {
    // Only fire a patient-driven search when no URL accession/date params will
    // trigger their own authoritative search from the [searchBy] effect. This
    // prevents a broad (empty-accession) request from overwriting the URL-driven
    // accession search with stale or wider results.
    const urlParams = new URLSearchParams(window.location.search);
    const hasUrlSearch =
      urlParams.get("accessionNumber") ||
      urlParams.get("upperAccessionNumber") ||
      urlParams.get("collectionDate") ||
      urlParams.get("recievedDate") ||
      urlParams.get("selectedTest") ||
      urlParams.get("selectedSampleStatus") ||
      urlParams.get("selectedAnalysisStatus");
    if (!hasUrlSearch) {
      querySearch(searchFormValues);
    }
  }, [patient]);

  const querySearch = (values) => {
    setLoading(true);
    props.setResults({ testResult: [] });

    let accessionNumber =
      values.accessionNumber !== ""
        ? values.accessionNumber
        : values.startLabNo;
    let labNo = labNumberForLogbookSearch(accessionNumber);
    const endLabNo = values.endLabNo ? values.endLabNo : "";
    values.unitType = values.unitType ? values.unitType : "";

    let searchEndPoint =
      "/rest/LogbookResults?" +
      "labNumber=" +
      labNo +
      "&upperRangeAccessionNumber=" +
      endLabNo +
      "&patientPK=" +
      patient.patientPK +
      "&testSectionId=" +
      values.unitType +
      "&collectionDate=" +
      values.collectionDate +
      "&recievedDate=" +
      values.recievedDate +
      "&selectedTest=" +
      values.testName +
      "&selectedSampleStatus=" +
      values.sampleStatusType +
      "&selectedAnalysisStatus=" +
      values.analysisStatus +
      "&doRange=" +
      searchBy.doRange +
      "&finished=" +
      false;
    setUrl(searchEndPoint);
    props.setSearchBy?.(searchBy);
    switch (searchBy.type) {
      case "unit":
        props.setParam("&testSectionId=" + values.unitType);
        break;
      case "patient":
        props.setParam("&patientId=" + patient.patientPK);
        break;
      case "order":
        props.setParam("&accessionNumber=" + labNo);
        break;
      case "date":
        props.setParam(
          "&selectedTest=" +
            values.testName +
            "&selectedSampleStatus=" +
            values.sampleStatusType +
            "&selectedAnalysisStatus=" +
            values.analysisStatus +
            "&collectionDate=" +
            values.collectionDate +
            "&recievedDate=" +
            values.recievedDate,
        );
        break;
      case "range":
        props.setParam(
          "&accessionNumber=" + labNo + "&upperAccessionNumber=" + endLabNo,
        );
        break;
    }

    getFromOpenElisServer(searchEndPoint, setResultsWithId);
  };

  const handleSubmit = (values) => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    querySearch(values);
  };

  const getTests = (tests) => {
    if (componentMounted.current) {
      setTests(tests);
    }
  };

  const getAnalysisStatusTypes = (analysisStatusTypes) => {
    if (componentMounted.current) {
      setAnalysisStatusTypes(analysisStatusTypes);
    }
  };

  const getSampleStatusTypes = (sampleStatusTypes) => {
    if (componentMounted.current) {
      setSampleStatusTypes(sampleStatusTypes);
    }
  };

  const fetchTestSections = (response) => {
    setTestSections(response);
  };

  const submitOnSelect = (e) => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    var values = { unitType: e.target.value };
    handleSubmit(values);
  };

  useEffect(() => {
    componentMounted.current = true;
    let testId = new URLSearchParams(window.location.search).get(
      "selectedTest",
    );
    testId = testId ? testId : "";
    getFromOpenElisServer("/rest/test-list", (fetchedTests) => {
      let test = fetchedTests.find((test) => test.id === testId);
      let testLabel = test ? test.value : "";
      setDefaultTestId(testId);
      setDefaultTestLabel(testLabel);
      getTests(fetchedTests);
    });

    let sampleStatusId = new URLSearchParams(window.location.search).get(
      "selectedSampleStatus",
    );
    sampleStatusId = sampleStatusId ? sampleStatusId : "";
    getFromOpenElisServer(
      "/rest/sample-status-types",
      (fetchedSampleStatusTypes) => {
        let sampleStatus = fetchedSampleStatusTypes.find(
          (sampleStatus) => sampleStatus.id === sampleStatusId,
        );
        let sampleStatusLabel = sampleStatus ? sampleStatus.value : "";
        setDefaultSampleStatusId(sampleStatusId);
        setDefaultSampleStatusLabel(sampleStatusLabel);
        getSampleStatusTypes(fetchedSampleStatusTypes);
      },
    );

    let analysisStatusId = new URLSearchParams(window.location.search).get(
      "selectedAnalysisStatus",
    );
    analysisStatusId = analysisStatusId ? analysisStatusId : "";
    getFromOpenElisServer(
      "/rest/analysis-status-types",
      (fetchedAnalysisStatusTypes) => {
        let analysisStatus = fetchedAnalysisStatusTypes.find(
          (analysisStatus) => analysisStatus.id === analysisStatusId,
        );
        let analysisStatusLabel = analysisStatus ? analysisStatus.value : "";
        setDefaultAnalysisStatusId(analysisStatusId);
        setDefaultAnalysisStatusLabel(analysisStatusLabel);
        getAnalysisStatusTypes(fetchedAnalysisStatusTypes);
      },
    );

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
        setDefaultTestSectionId(testSectionId);
        setDefaultTestSectionLabel(testSectionLabel);
        fetchTestSections(fetchedTestSections);
      },
    );
    if (testSectionId) {
      let values = { unitType: testSectionId };
      querySearch(values);
    }

    var displayFormType = "";
    var doRange = "";
    if (window.location.pathname == "/result") {
      displayFormType = new URLSearchParams(window.location.search).get("type");
      doRange = new URLSearchParams(window.location.search).get("doRange");
    } else if (window.location.pathname == "/LogbookResults") {
      displayFormType = "unit";
      doRange = "false";
    } else if (window.location.pathname == "/PatientResults") {
      displayFormType = "patient";
      doRange = "false";
    } else if (window.location.pathname == "/AccessionResults") {
      displayFormType = "order";
      doRange = "false";
    } else if (window.location.pathname == "/StatusResults") {
      displayFormType = "date";
      doRange = "false";
    } else if (window.location.pathname == "/RangeResults") {
      displayFormType = "range";
      doRange = "true";
    }
    setSearchBy({
      type: displayFormType,
      doRange: doRange,
    });
  }, []);

  useEffect(() => {
    let accessionNumber = new URLSearchParams(window.location.search).get(
      "accessionNumber",
    );
    let upperAccessionNumber = new URLSearchParams(window.location.search).get(
      "upperAccessionNumber",
    );
    if (accessionNumber || upperAccessionNumber) {
      let searchValues = {
        ...searchFormValues,
        accessionNumber: accessionNumber,
        endLabNo: upperAccessionNumber,
      };
      setSearchFormValues(searchValues);
      querySearch(searchValues);
    }
    let collectionDate = new URLSearchParams(window.location.search).get(
      "collectionDate",
    );
    let recievedDate = new URLSearchParams(window.location.search).get(
      "recievedDate",
    );
    let selectedTest = new URLSearchParams(window.location.search).get(
      "selectedTest",
    );
    let selectedSampleStatus = new URLSearchParams(window.location.search).get(
      "selectedSampleStatus",
    );
    let selectedAnalysisStatus = new URLSearchParams(
      window.location.search,
    ).get("selectedAnalysisStatus");

    if (
      collectionDate ||
      recievedDate ||
      selectedTest ||
      selectedSampleStatus ||
      selectedAnalysisStatus
    ) {
      let searchValues = {
        ...searchFormValues,
        collectionDate: collectionDate ? collectionDate : "",
        recievedDate: recievedDate ? recievedDate : "",
        testName: selectedTest ? selectedTest : "",
        sampleStatusType: selectedSampleStatus ? selectedSampleStatus : "",
        analysisStatus: selectedAnalysisStatus ? selectedAnalysisStatus : "",
      };
      setSearchFormValues(searchValues);
      querySearch(searchValues);
    }
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
  }, [searchBy]);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      <Formik
        initialValues={searchFormValues}
        //validationSchema={}
        onSubmit={handleSubmit}
        onChange
        enableReinitialize={true}
      >
        {({
          values,
          //   errors,
          //   touched,
          handleChange,
          setFieldValue,
          //   handleBlur,
          handleSubmit,
        }) => (
          <Form
            onSubmit={handleSubmit}
            onChange={handleChange}
            //onBlur={handleBlur}
          >
            <Stack gap={2}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <h4>
                    <FormattedMessage id="label.button.search" />
                  </h4>
                </Column>
                {searchBy.type === "order" && (
                  <>
                    <Column lg={6} md={4} sm={4}>
                      <Field name="accessionNumber">
                        {({ field }) => (
                          <CustomLabNumberInput
                            placeholder="Enter Accession No."
                            name={field.name}
                            id={field.name}
                            data-cy="enterAccession"
                            value={values[field.name]}
                            labelText={
                              <FormattedMessage id="search.label.accession" />
                            }
                            onChange={(e, rawValue) => {
                              setFieldValue(field.name, rawValue);
                            }}
                          />
                        )}
                      </Field>
                    </Column>
                    <Column lg={10} />
                  </>
                )}

                {searchBy.type === "range" && (
                  <>
                    <Column lg={6} sm={4}>
                      <Field name="startLabNo">
                        {({ field }) => (
                          <CustomLabNumberInput
                            placeholder="Enter Accession No."
                            name={field.name}
                            id={field.name}
                            data-cy="startAccession"
                            value={values[field.name]}
                            labelText={
                              <FormattedMessage id="search.label.fromaccession" />
                            }
                            onChange={(e, rawValue) => {
                              setFieldValue(field.name, rawValue);
                            }}
                          />
                        )}
                      </Field>
                    </Column>
                    <Column lg={6} sm={4}>
                      <Field name="endLabNo">
                        {({ field }) => (
                          <CustomLabNumberInput
                            placeholder="Enter Accession No."
                            name={field.name}
                            id={field.name}
                            data-cy="endAccession"
                            value={values[field.name]}
                            labelText={
                              <FormattedMessage id="search.label.toaccession" />
                            }
                            onChange={(e, rawValue) => {
                              setFieldValue(field.name, rawValue);
                            }}
                          />
                        )}
                      </Field>
                    </Column>
                    <Column lg={4} />
                  </>
                )}

                {searchBy.type === "date" && (
                  <>
                    <Column lg={3} md={4} sm={4}>
                      <Field name="collectionDate">
                        {({ field, form }) => (
                          <CustomDatePicker
                            id={field.name}
                            labelText={intl.formatMessage({
                              id: "search.label.collectiondate",
                            })}
                            value={values[field.name]}
                            onChange={(date) =>
                              form.setFieldValue(field.name, date)
                            }
                            name={field.name}
                            disallowFutureDate={true}
                          />
                        )}
                      </Field>
                    </Column>
                    <Column lg={3} md={4} sm={4}>
                      <Field name="recievedDate">
                        {({ field, form }) => (
                          <CustomDatePicker
                            id={field.name}
                            labelText={intl.formatMessage({
                              id: "search.label.receiveddate",
                            })}
                            value={values[field.name]}
                            onChange={(date) =>
                              form.setFieldValue(field.name, date)
                            }
                            name={field.name}
                            disallowFutureDate={true}
                          />
                        )}
                      </Field>
                    </Column>
                    <Column lg={3} md={4} sm={4}>
                      <Field name="testName">
                        {({ field }) => (
                          <Select
                            labelText={
                              <FormattedMessage id="search.label.test" />
                            }
                            name={field.name}
                            id={field.name}
                          >
                            <SelectItem
                              text={defaultTestLabel}
                              value={defaultTestId}
                            />
                            {tests
                              .filter((item) => item.id !== defaultTestId)
                              .map((test, index) => {
                                return (
                                  <SelectItem
                                    key={index}
                                    text={test.value}
                                    value={test.id}
                                  />
                                );
                              })}
                          </Select>
                        )}
                      </Field>
                    </Column>
                    <Column lg={3} md={4} sm={4}>
                      <Field name="analysisStatus">
                        {({ field }) => (
                          <Select
                            labelText={
                              <FormattedMessage id="search.label.analysis" />
                            }
                            name={field.name}
                            id={field.name}
                          >
                            <SelectItem
                              text={defaultAnalysisStatusLabel}
                              value={defaultAnalysisStatusId}
                            />
                            {analysisStatusTypes
                              .filter(
                                (item) => item.id !== defaultAnalysisStatusId,
                              )
                              .map((test, index) => {
                                return (
                                  <SelectItem
                                    key={index}
                                    text={test.value}
                                    value={test.id}
                                  />
                                );
                              })}
                          </Select>
                        )}
                      </Field>
                    </Column>
                    <Column lg={3} md={4} sm={4}>
                      <Field name="sampleStatusType">
                        {({ field }) => (
                          <Select
                            labelText={
                              <FormattedMessage id="search.label.sample" />
                            }
                            name={field.name}
                            id={field.name}
                          >
                            <SelectItem
                              text={defaultSampleStatusLabel}
                              value={defaultSampleStatusId}
                            />
                            {sampleStatusTypes
                              .filter(
                                (item) => item.id !== defaultSampleStatusId,
                              )
                              .map((test, index) => {
                                return (
                                  <SelectItem
                                    key={index}
                                    text={test.value}
                                    value={test.id}
                                  />
                                );
                              })}
                          </Select>
                        )}
                      </Field>
                    </Column>
                    <Column lg={1} />
                  </>
                )}

                {searchBy.type !== "patient" && searchBy.type !== "unit" && (
                  <Column lg={16} md={8} sm={4}>
                    <Button
                      style={{ marginTop: "16px" }}
                      type="submit"
                      id="searchResults"
                    >
                      <FormattedMessage id="label.button.search" />
                    </Button>
                  </Column>
                )}
              </Grid>
            </Stack>
          </Form>
        )}
      </Formik>
      {searchBy.type === "patient" && (
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <SearchPatientForm
              getSelectedPatient={getSelectedPatient}
            ></SearchPatientForm>
          </Column>
        </Grid>
      )}

      {searchBy.type === "unit" && (
        <>
          <Grid>
            <Column lg={6} md={4} sm={4}>
              <Select
                labelText={intl.formatMessage({ id: "search.label.testunit" })}
                name="unitType"
                id="unitType"
                onChange={submitOnSelect}
              >
                <SelectItem
                  text={defaultTestSectionLabel}
                  value={defaultTestSectionId}
                />
                {testSections
                  .filter((item) => item.id !== defaultTestSectionId)
                  .map((test, index) => {
                    return (
                      <SelectItem
                        key={index}
                        text={test.value}
                        value={test.id}
                      />
                    );
                  })}
              </Select>
            </Column>
            <Column lg={10} />
          </Grid>
        </>
      )}

      {searchBy.type === "ReferredOutTests" && <ReferredOutTests />}

      <>
        {pagination && (
          <Grid>
            <Column lg={16}>
              {" "}
              <br /> <br />
            </Column>
            <Column lg={14} />
            <Column
              lg={2}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "10px",
                width: "110%",
              }}
            >
              <Link>
                {currentApiPage} / {totalApiPages}
              </Link>
              <div style={{ display: "flex", gap: "10px" }}>
                <Button
                  hasIconOnly
                  id="loadpreviousresults"
                  onClick={loadPreviousResultsPage}
                  disabled={previousPage != null ? false : true}
                  renderIcon={ArrowLeft}
                  iconDescription="previous"
                ></Button>
                <Button
                  hasIconOnly
                  id="loadnextresults"
                  onClick={loadNextResultsPage}
                  disabled={nextPage != null ? false : true}
                  renderIcon={ArrowRight}
                  iconDescription="next"
                ></Button>
              </div>
            </Column>
          </Grid>
        )}
      </>
    </>
  );
}

export function SearchResults(props) {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [acceptAsIs, setAcceptAsIs] = useState([]);
  const [referalOrganizations, setReferalOrganizations] = useState([]);
  const [methods, setMethods] = useState([]);
  const [referralReasons, setReferralReasons] = useState([]);
  const [rejectReasons, setRejectReasons] = useState([]);
  const [rejectedItems, setRejectedItems] = useState({});
  const [validationState, setValidationState] = useState({});
  const saveStatus = "";
  const [referTest, setReferTest] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sampleLocations, setSampleLocations] = useState({}); // Track location by analysisId
  const [nceFormOpenRow, setNceFormOpenRow] = useState(null); // Track which row has NCE form open
  // Which analysisId's storage-picker modal is open (one at a time).
  const [storageModalRow, setStorageModalRow] = useState(null);

  const componentMounted = useRef(false);

  useEffect(() => {
    componentMounted.current = true;

    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      loadReferalOrganizations,
    );
    getFromOpenElisServer("/rest/displayList/METHODS", loadMethods);
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_REASONS",
      loadReferalReasons,
    );
    getFromOpenElisServer(
      "/rest/displayList/REJECTION_REASONS",
      loadRejectReasons,
    );
    if (props.results.testResult.length > 0) {
      var defaultRejectedItems = {};
      props.results.testResult.forEach((result) => {
        defaultRejectedItems[result.id] = false;
      });
      setRejectedItems(defaultRejectedItems);
    }
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (props.results.testResult) {
      let newValidationState = { ...validationState };
      props.results.testResult.forEach((row) => {
        if (row.resultType === "N") {
          let value = row.resultValue;
          if (!value) {
            return;
          }
          let validation = (newValidationState[row.id] = validateNumericResults(
            value,
            row,
          ));

          row.resultValue = validation.newValue;
          validation.style = {
            ...validation?.style,
            borderColor: validation.isCritical
              ? "orange"
              : validation.isInvalid
                ? "red"
                : "",
            background: validation.outsideValid
              ? "#ffa0a0"
              : validation.outsideNormal
                ? "#ffffa0"
                : "var(--cds-field)",
          };
        }
      });
      setValidationState(newValidationState);
    }
  }, [props.results]);

  const loadReferalOrganizations = (values) => {
    if (componentMounted.current) {
      setReferalOrganizations(values);
    }
  };

  const loadMethods = (values) => {
    if (componentMounted.current) {
      setMethods(values);
    }
  };

  const loadReferalReasons = (values) => {
    if (componentMounted.current) {
      setReferralReasons(values);
    }
  };

  const loadRejectReasons = (values) => {
    if (componentMounted.current) {
      setRejectReasons(values);
    }
  };

  const downloadFile = (fileName, content, fileType) => {
    var win = window.open();
    win.document.write(
      '<iframe src="' +
        fileType +
        ";base64," +
        content +
        '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
    );
  };

  const addRejectResult = () => {
    const resultColumn = {
      id: "reject",
      name: intl.formatMessage({ id: "column.name.reject" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "8rem",
    };

    if (configurationProperties.allowResultRejection == "true") {
      if (columns) {
        const updatedList = [
          ...columns.slice(0, 8),
          resultColumn,
          ...columns.slice(8),
        ];
        columns = updatedList;
      }
    }
  };

  var columns = [
    {
      id: "sampleInfo",
      name: intl.formatMessage({ id: "column.name.sampleInfo" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      sortable: true,
      selector: (row) => row.accessionNumber,
      width: "16rem",
    },
    {
      id: "testDate",
      name: intl.formatMessage({ id: "column.name.testDate" }),
      cell: (row) => {
        const raw = (row.testDate || "").trim();
        const parts = raw.split(/\s+/);
        const datePart = parts[0] || "";
        const timePart = parts[1] || "";
        const emit = (nextDate, nextTime) => {
          const combined = nextDate
            ? nextTime
              ? `${nextDate} ${nextTime}`
              : nextDate
            : "";
          // Suppress no-op emits (CustomDatePicker fires onChange on mount;
          // would otherwise mark every row isModified at initial table render).
          if (combined === (row.testDate || "")) {
            return;
          }
          row.testDate = combined;
          handleChange(
            {
              target: {
                id: `testDate-${row.id}`,
                name: `testResult[${row.id}].testDate`,
                value: combined,
              },
            },
            row.id,
          );
        };
        return (
          <div
            style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}
          >
            <CustomDatePicker
              id={`testDate-date-${row.id}`}
              labelText=""
              value={datePart}
              disallowFutureDate
              onChange={(next) => emit(next, timePart)}
            />
            <CustomTimePicker
              id={`testDate-time-${row.id}`}
              labelText=""
              value={timePart}
              onChange={(next) => emit(datePart, next)}
            />
          </div>
        );
      },
      selector: (row) => row.testDate,
      sortable: true,
      width: "15rem",
    },

    {
      id: "analyzerResult",
      name: intl.formatMessage({ id: "column.name.analyzerResult" }),
      selector: (row) => row.analysisMethod,
      sortable: true,
      width: "7rem",
    },
    {
      id: "testName",
      name: intl.formatMessage({ id: "column.name.testName" }),
      selector: (row) => row.testName,
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      sortable: true,
      width: "10rem",
    },
    {
      id: "normalRange",
      name: intl.formatMessage({ id: "column.name.normalRange" }),
      selector: (row) => row.normalRange,
      sortable: true,
      width: "8rem",
    },
    {
      id: "accept",
      name: intl.formatMessage({ id: "column.name.accept" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "7rem",
    },
    {
      id: "result",
      name: intl.formatMessage({ id: "column.name.result" }),
      cell: (row, index, column, id) => (
        <div style={{ paddingLeft: "1.5rem", width: "100%" }}>
          {renderCell(row, index, column, id)}
        </div>
      ),
      width: "14rem",
    },
    {
      id: "currentResult",
      name: intl.formatMessage({ id: "column.name.currentResult" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "10rem",
    },
    {
      id: "notes",
      name: intl.formatMessage({ id: "column.name.notes" }),
      cell: (row, index, column, id) => {
        return renderCell(row, index, column, id);
      },
      width: "25rem",
    },
  ];

  const renderCell = (row, index, column, id) => {
    let formatLabNum = configurationProperties.AccessionFormat === "ALPHANUM";
    const fullTestName = row.testName;
    const splitIndex = fullTestName.lastIndexOf("(");
    const testName = fullTestName.substring(0, splitIndex);
    const sampleType = fullTestName.substring(splitIndex);

    console.debug("renderCell: index: " + index + ", id: " + id);
    switch (column.id) {
      case "sampleInfo":
        // return <input id={"results_" + id} type="text" size="6"></input>
        return (
          <>
            <div>
              <Button
                onClick={async () => {
                  if ("clipboard" in navigator) {
                    return await navigator.clipboard.writeText(
                      row.accessionNumber,
                    );
                  } else {
                    return document.execCommand(
                      "copy",
                      true,
                      row.accessionNumber,
                    );
                  }
                }}
                kind="ghost"
                iconDescription={intl.formatMessage({
                  id: "instructions.copy.labnum",
                })}
                hasIconOnly
                renderIcon={Copy}
              />
            </div>
            <div className="sampleInfo">
              <br></br>
              {(formatLabNum
                ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
                : row.accessionNumber) +
                "-" +
                row.sequenceNumber}
              {row.isEqaSample && <EQABadge priority={row.eqaPriority} />}
              <br></br>
              {row.patientName} <br></br>
              {row.patientInfo}
              <br></br>
              <br></br>
            </div>
            <div>
              <AsyncAvatar
                patientId={row.patientId}
                hasPhoto={true}
                patientName={row.patientName || ""}
              />
            </div>
            {row.nonconforming && (
              <picture>
                <img
                  src={config.serverBaseUrl + "/images/nonconforming.gif"}
                  alt="nonconforming"
                  width="20"
                  height="15"
                />
              </picture>
            )}
          </>
        );
      case "testName":
        return (
          <div className="sampleInfo">
            <br></br>
            {testName}
            {row.unitsOfMeasure && (
              <>
                <br></br>
                {row.unitsOfMeasure}
              </>
            )}
            <br></br>
            {sampleType}
          </div>
        );

      case "accept":
        return (
          <div style={{ paddingRight: "2rem", marginRight: "1rem" }}>
            <AcceptUnconditionallyGuard
              rowId={row.id}
              accepted={!!acceptAsIs[row.id]}
              onAccept={(reason) => handleAcceptUnconditionally(row.id, reason)}
              onUnaccept={() => handleUnacceptUnconditionally(row.id)}
            />
          </div>
        );

      case "reject":
        return (
          <div>
            <Field name="reject">
              {() => (
                <Checkbox
                  id={"testResult" + row.id + ".rejected"}
                  name={"testResult[" + row.id + "].rejected"}
                  labelText=""
                  onChange={(e) => handleRejectCheckBoxChange(e, row.id)}
                />
              )}
            </Field>
            <br></br>
            {rejectedItems[row.id] == true && (
              <Select
                id={"rejectReasonId" + row.id}
                name={"testResult[" + row.id + "].rejectReasonId"}
                //noLabel={true}
                labelText={"Reason"}
                onChange={(e) => handleChange(e, row.id)}
              >
                {/* {...updateShadowResult(e, this, param.rowId)} */}
                <SelectItem text="" value="" />
                {rejectReasons.map((reason, reason_index) => (
                  <SelectItem
                    text={reason.value}
                    value={reason.id}
                    key={reason_index}
                  />
                ))}
              </Select>
            )}
          </div>
        );

      case "notes":
        return (
          <>
            <div className="note">
              <TextArea
                id={"testResult" + row.id + ".note"}
                name={"testResult[" + row.id + "].note"}
                //value={props.results.testResult[row.id]?.pastNotes}
                disabled={false}
                type="text"
                labelText=""
                rows={1}
                onChange={(e) => handleChange(e, row.id)}
              ></TextArea>
              <div className="note" style={{ whiteSpace: "pre-wrap" }}>
                {row.pastNotes?.replace(/<br\s*\/?>/gi, "\n")}
              </div>
            </div>
          </>
        );

      case "result":
        switch (row.resultType) {
          case "D":
            return (
              <Select
                className="result"
                id={"resultValue" + row.id}
                name={"testResult[" + row.id + "].resultValue"}
                noLabel={true}
                onChange={(e) => validateResults(e, row.id)}
                value={row.resultValue}
              >
                {/* {...updateShadowResult(e, this, param.rowId)} */}
                <SelectItem text="" value="" />
                {row.dictionaryResults.map(
                  (dictionaryResult, dictionaryResult_index) => (
                    <SelectItem
                      text={dictionaryResult.value}
                      value={dictionaryResult.id}
                      key={dictionaryResult_index}
                    />
                  ),
                )}
              </Select>
            );

          case "M":
            return (
              <ResultMultiSelect
                id={`multiResultValue${row.id}`}
                name={`testResult[${row.id}].multiSelectResultValues`}
                dictionaryValues={row.dictionaryResults}
                value={row.multiSelectResultValues}
                onChange={(e) => handleChange(e, row.id)}
              />
            );

          case "C":
            return (
              <CascadingMultiSelect
                id={`multiResult${row.id}`}
                name={`testResult[${row.id}].multiSelectResultValues`}
                dictionaryValues={row.dictionaryResults}
                value={row.multiSelectResultValues}
                onChange={(e) => handleChange(e, row.id)}
              />
            );

          case "N":
            return (
              <TextInput
                id={"ResultValue" + row.id}
                name={"testResult[" + row.id + "].resultValue"}
                labelText=""
                type="number"
                value={row.resultValue}
                style={validationState[row.id]?.style}
                onBlur={(e) => {
                  if (
                    validationState[row.id]?.isInvalid &&
                    configurationProperties.ALERT_FOR_INVALID_RESULTS
                  ) {
                    addNotification({
                      title: intl.formatMessage({ id: "notification.title" }),
                      message:
                        intl.formatMessage({
                          id: "result.outOfValidRange.msg",
                        }) +
                        " " +
                        row.testName +
                        " : " +
                        row.resultValue,
                      kind: NotificationKinds.error,
                    });
                    setNotificationVisible(true);
                  }
                }}
                onChange={(e) => {
                  handleChange(e, row.id);
                  if (
                    validationState[row.id]?.isInvalid &&
                    configurationProperties.ALERT_FOR_INVALID_RESULTS
                  ) {
                    addNotification({
                      title: intl.formatMessage({ id: "notification.title" }),
                      message:
                        intl.formatMessage({
                          id: "result.outOfValidRange.msg",
                        }) +
                        " " +
                        row.testName +
                        " : " +
                        row.resultValue,
                      kind: NotificationKinds.error,
                    });
                    setNotificationVisible(true);
                  }
                }}
              />
            );

          case "R":
            return (
              <TextArea
                id={"ResultValue" + row.id}
                name={"testResult[" + row.id + "].resultValue"}
                rows={1}
                labelText=""
                onChange={(e) => handleChange(e, row.id)}
                value={row.resultValue}
              />
            );

          case "A":
            return (
              <TextArea
                id={"ResultValue" + row.id}
                name={"testResult[" + row.id + "].resultValue"}
                rows={1}
                labelText=""
                onChange={(e) => handleChange(e, row.id)}
                value={row.resultValue}
              />
            );

          default:
            return row.resultValue;
        }

      case "currentResult":
        switch (row.resultType) {
          case "M":
          case "C":
          case "D":
            return (
              <>
                {
                  row.dictionaryResults.find(
                    (result) => result.id == row.shadowResultValue,
                  )?.value
                }
              </>
            );

          default:
            return row.shadowResultValue;
        }
      default:
        return;
    }
  };

  // Fetch location for a SampleItem when analysis row is expanded
  const fetchSampleLocation = (analysisId, sampleItemId) => {
    // Skip if already fetched or no sampleItemId
    if (!sampleItemId || sampleLocations[analysisId]) {
      return;
    }

    getFromOpenElisServer(
      `/rest/storage/sample-items/${encodeURIComponent(sampleItemId)}`,
      (response) => {
        if (response) {
          const locationPath =
            response.hierarchicalPath || response.location || "";
          setSampleLocations((prev) => ({
            ...prev,
            [analysisId]: {
              locationPath,
              sampleItemId: sampleItemId,
              sampleItemExternalId: response.sampleItemExternalId || null,
              sampleAccessionNumber: response.sampleAccessionNumber || "",
            },
          }));
        } else {
          // SampleItem may not have location assigned yet
          console.debug("No location found for SampleItem:", sampleItemId);
          setSampleLocations((prev) => ({
            ...prev,
            [analysisId]: { locationPath: "", sampleItemId: sampleItemId },
          }));
        }
      },
    );
  };

  // Handle location assignment/movement using shared location mapping rules.
  const handleLocationAssignment = async (
    locationData,
    analysisId,
    sampleItemId,
  ) => {
    // locationData format: { sample, selection, reason?, conditionNotes?, positionCoordinate? }
    const selection =
      locationData?.selection || locationData?.newLocation || {};
    const deepest = getDeepestLocationSelection(selection, {
      requireAssignable: true,
    });

    // Use sampleItemId from parameter or stored location data
    const actualSampleItemId =
      locationData?.sample?.sampleItemId ||
      locationData?.sample?.id ||
      sampleItemId ||
      (sampleLocations[analysisId] &&
      typeof sampleLocations[analysisId] === "object"
        ? sampleLocations[analysisId].sampleItemId
        : null);

    if (!actualSampleItemId || !deepest) {
      console.error("Missing SampleItem ID or location for assignment", {
        sampleItemId: actualSampleItemId,
        selection,
      });
      return;
    }

    const isMovement =
      locationData?.isMovement ||
      Boolean(locationData?.currentLocationPath) ||
      Boolean(
        sampleLocations[analysisId] &&
        typeof sampleLocations[analysisId] === "object" &&
        sampleLocations[analysisId].locationPath,
      );

    try {
      // Call assignment or movement API with SampleItem ID
      const assignmentData = {
        sampleItemId: actualSampleItemId,
        locationId: String(deepest.value.id),
        locationType: deepest.type,
        positionCoordinate:
          locationData.positionCoordinate ||
          positionToCoordinate(locationData.position) ||
          "",
        notes: locationData.conditionNotes || "", // Assignment form uses "notes" field
      };
      if (isMovement) {
        assignmentData.reason =
          locationData.reason || "Reassignment from result entry workflow";
      }

      postToOpenElisServerJsonResponse(
        isMovement
          ? "/rest/storage/sample-items/move"
          : "/rest/storage/sample-items/assign",
        JSON.stringify(assignmentData),
        (response) => {
          const isSuccess = isStorageAssignmentSuccess(response);

          if (isSuccess) {
            // Update local state with location path
            const locationPath =
              response.newHierarchicalPath || response.hierarchicalPath || "";
            const storedData = sampleLocations[analysisId];
            setSampleLocations((prev) => ({
              ...prev,
              [analysisId]:
                storedData && typeof storedData === "object"
                  ? { ...storedData, locationPath }
                  : locationPath,
            }));
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "storage.location.assigned.success",
                defaultMessage: "Location assigned successfully",
              }),
              kind: NotificationKinds.success,
            });
            setNotificationVisible(true);
          } else {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message:
                response?.message ||
                intl.formatMessage({
                  id: "storage.location.assigned.error",
                  defaultMessage: "Failed to assign location",
                }),
              kind: NotificationKinds.error,
            });
            setNotificationVisible(true);
          }
        },
        (error) => {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "storage.location.assigned.error",
              defaultMessage: "Failed to assign location",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        },
      );
    } catch (error) {
      console.error("Error assigning location:", error);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "storage.location.assigned.error",
          defaultMessage: "Failed to assign location",
        }),
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
    }
  };

  const renderReferral = ({ data }) => {
    // Fetch location when row is expanded using sampleItemId
    const analysisId = data.id;
    const sampleItemId = data.sampleItemId;

    if (sampleItemId && !sampleLocations[analysisId]) {
      fetchSampleLocation(analysisId, sampleItemId);
    }

    // Get location path from stored data (keyed by analysisId)
    const locationData = sampleLocations[analysisId];
    const currentLocationPath =
      typeof locationData === "object"
        ? locationData.locationPath || ""
        : locationData || "";

    return (
      <>
        <Grid>
          <Column lg={2}>
            <Select
              id={"testMethod" + data.id}
              name={"testResult[" + data.id + "].testMethod"}
              labelText={intl.formatMessage({
                id: "referral.label.testmethod",
              })}
              onChange={(e) => handleChange(e, data.id)}
              value={data.testMethod}
            >
              <SelectItem text="" value="" />
              {methods.map((method, method_index) => (
                <SelectItem
                  text={method.value}
                  value={method.id}
                  key={method_index}
                />
              ))}
            </Select>
          </Column>
          <Column lg={2}>
            <CompactFileInput
              data={data}
              results={props.results}
              setResultForm={props.setResultForm}
            />

            {data.resultFile && data.resultFile.fileName && (
              <Link
                onClick={() =>
                  downloadFile(
                    data.resultFile.fileName,
                    data.resultFile.content,
                    data.resultFile.fileType,
                  )
                }
                style={{ fontSize: "12px" }}
              >
                {data.resultFile.fileName}
              </Link>
            )}
          </Column>
          <Column lg={2}>
            <span
              className="cds--label"
              aria-hidden="true"
              style={{ display: "block" }}
            >
              &nbsp;
            </span>
            <Checkbox
              labelText={intl.formatMessage({ id: "results.label.refer" })}
              name={"testResult[" + data.id + "].refer"}
              id={"testResult[" + data.id + "].refer"}
              checked={data.refer === "true"}
              disabled={data.referredOut}
              data-cy="referalcheckbox"
              onChange={(e) => {
                e.target.value = e.target.checked;
                handleChange(e, data.id);
              }}
            />
          </Column>
          <Column lg={2}>
            <Select
              id={"referralReason" + data.id}
              name={"testResult[" + data.id + "].referralItem.referralReasonId"}
              // noLabel={true}
              labelText={intl.formatMessage({ id: "referral.label.reason" })}
              onChange={(e) => handleChange(e, data.id)}
              value={data?.referralItem?.referralReasonId}
              disabled={!referTest[data.id]}
            >
              {/* {...updateShadowResult(e, this, param.rowId)} */}
              <SelectItem text="" value="" />
              {referralReasons.map((reason, reason_index) => (
                <SelectItem
                  text={reason.value}
                  value={reason.id}
                  key={reason_index}
                />
              ))}
            </Select>
          </Column>
          <Column lg={2}>
            <Select
              id={"institute" + data.id}
              name={
                "testResult[" + data.id + "].referralItem.referredInstituteId"
              }
              // noLabel={true}
              labelText={intl.formatMessage({ id: "referral.label.institute" })}
              onChange={(e) => handleChange(e, data.id)}
              value={data?.referralItem?.referredInstituteId}
              disabled={!referTest[data.id]}
            >
              {/* {...updateShadowResult(e, this, param.rowId)} */}

              <SelectItem text="" value="" />
              {referalOrganizations.map((org, org_index) => (
                <SelectItem text={org.value} value={org.id} key={org_index} />
              ))}
            </Select>
          </Column>
          <Column lg={3}>
            <Select
              id={"testToPerform" + data.id}
              name={"testResult[" + data.id + "].referralItem.referredTestId"}
              // noLabel={true}
              labelText={intl.formatMessage({
                id: "referral.label.testtoperform",
              })}
              onChange={(e) => handleChange(e, data.id)}
              value={data?.referralItem?.referredTestId}
              disabled={!referTest[data.id]}
            >
              {/* {...updateShadowResult(e, this, param.rowId)} */}

              <SelectItem text={data.testName} value={data.id} />
            </Select>
          </Column>
          <Column lg={2}>
            <CustomDatePicker
              id={"sentDate_" + data.id}
              labelText={intl.formatMessage({
                id: "referral.label.sentdate",
              })}
              onChange={(date) => handleDatePickerChange(date, data.id)}
              name={"testResult[" + data.id + "].referralItem.referredSendDate"}
              value={data?.referralItem?.referredSendDate}
              disabled={!referTest[data.id]}
              disallowFutureDate={true}
            />
          </Column>
        </Grid>
        <Grid style={{ marginTop: "1rem" }}>
          <Column lg={16}>
            <Button
              kind="danger--tertiary"
              size="sm"
              renderIcon={Warning}
              onClick={() =>
                setNceFormOpenRow(nceFormOpenRow === data.id ? null : data.id)
              }
            >
              <FormattedMessage
                id="nce.button.reportNce"
                defaultMessage="Report NCE"
              />
            </Button>
          </Column>
          <Column lg={16} style={{ marginTop: "1rem" }}>
            <div
              className="result-entry-storage-section"
              style={{
                display: "flex",
                alignItems: "center",
                flexWrap: "wrap",
                gap: "0.75rem",
              }}
            >
              <div
                className="result-entry-storage-current"
                style={{ minWidth: 0, flex: "0 1 auto" }}
              >
                <strong>
                  <FormattedMessage
                    id="storage.location.current"
                    defaultMessage="Storage location"
                  />
                  :
                </strong>{" "}
                {currentLocationPath || (
                  <FormattedMessage
                    id="storage.location.unassigned"
                    defaultMessage="Unassigned"
                  />
                )}
              </div>
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => setStorageModalRow(data.id)}
              >
                <FormattedMessage
                  id={
                    currentLocationPath
                      ? "storage.location.move"
                      : "storage.location.assign"
                  }
                  defaultMessage={
                    currentLocationPath
                      ? "Move storage location"
                      : "Assign storage location"
                  }
                />
              </Button>
              <LocationPickerModal
                isOpen={storageModalRow === data.id}
                sample={{
                  id: sampleItemId || data.accessionNumber,
                  sampleAccessionNumber: data.accessionNumber,
                  sampleType: data.sampleType || "",
                  status: data.sampleStatus || "Active",
                }}
                onConfirm={({ selection, position, reason, notes }) => {
                  handleLocationAssignment(
                    {
                      sample: {
                        sampleItemId,
                        sampleAccessionNumber: data.accessionNumber,
                      },
                      isMovement: Boolean(currentLocationPath),
                      currentLocationPath,
                      selection,
                      position,
                      positionCoordinate: positionToCoordinate(position, {
                        emptyValue: null,
                      }),
                      conditionNotes: notes || "",
                      reason: reason || null,
                    },
                    analysisId,
                    sampleItemId,
                  );
                  setStorageModalRow(null);
                }}
                currentLocation={
                  currentLocationPath
                    ? {
                        selection: {},
                        hierarchicalPath: currentLocationPath,
                        position:
                          locationData &&
                          typeof locationData === "object" &&
                          locationData.positionCoordinate
                            ? {
                                mode: "text",
                                value: locationData.positionCoordinate,
                              }
                            : null,
                      }
                    : null
                }
                onCancel={() => setStorageModalRow(null)}
              />
            </div>
          </Column>
        </Grid>
        {/* Report NCE */}

        {nceFormOpenRow === data.id && (
          <InlineNceForm
            resultRow={data}
            onClose={() => setNceFormOpenRow(null)}
            onSubmitSuccess={() => setNceFormOpenRow(null)}
          />
        )}
      </>
    );
  };
  const validateResults = (e, rowId) => {
    console.debug("validateResults:" + e.target.value);
    // e.target.value;
    handleChange(e, rowId);
  };

  const validateNumericResults = (value, row) => {
    //ignore < or > from the analyser on validation
    var greaterThanOrLessThan = "";
    if (("" + value).startsWith("<") || ("" + value).startsWith(">")) {
      greaterThanOrLessThan = value.charAt(0);
    }
    var actualValue = ("" + value).replace(/[<>]/g, "");
    let validation = {
      isInvalid: false,
      outsideNormal: false,
      isCritical: false,
      isBlank: false,
      isNaN: false,
      outsideValid: false,
      newValue: value,
    };
    //commented out for now
    let isSpecialCase = "XXXX" == actualValue.toUpperCase();
    validation = { ...validation, ...validateNumberFormat(value, row) };

    // resultBox.style.borderColor = validFormat ? "" : "red";

    // if( isSpecialCase ){
    //   resultBox.title = "";
    //   value = greaterThanOrLessThan + actualValue.toUpperCase();
    //   resultBox.style.borderColor = "";
    //   resultBox.style.background = "#ffffff";
    //   $("valid_" + row).value = true;
    //   return;
    // }
    if (validation.isNaN) {
      return { ...validation };
    } else if (
      row.lowCritical != row.highCritical &&
      actualValue > row.lowCritical &&
      actualValue < row.highCritical
    ) {
      return { ...validation, isCritical: true };
    } else if (
      row.lowerAbnormalRange != row.upperAbnormalRange &&
      (actualValue < row.lowerAbnormalRange ||
        actualValue > row.upperAbnormalRange)
    ) {
      return { ...validation, isInvalid: true, outsideValid: true };
      // resultBox.style.background = "#ffa0a0";
      // resultBox.title = "En dehors de la plage valide"; //FIXME: Uses hardcoded French labels. Switch to refer to resource file.
      // $("valid_" + row).value = false;
      // if( outOfValidRangeMsg ){
      //   alert( outOfValidRangeMsg);
      // }
    } else if (
      row.lowerNormalRange != row.upperNormalRange &&
      (actualValue < row.lowerNormalRange || actualValue > row.upperNormalRange)
    ) {
      return { ...validation, outsideNormal: true };
      // resultBox.style.background = "#ffffa0";
      // resultBox.title = "En dehors de la plage normale"; //FIXME: Uses hardcoded French labels. Switch to refer to resource file.
      // $("valid_" + row).value = true;
    } else {
      return { ...validation, outsideNormal: false };
      // resultBox.style.background = "#ffffff";
      // resultBox.title = "";
      // $("valid_" + row).value = true;
    }
  };

  const validateNumberFormat = (value, row) => {
    //ignore < or > from the analyser on validation
    var greaterThanOrLessThan = "";
    if (("" + value).startsWith("<") || ("" + value).startsWith(">")) {
      greaterThanOrLessThan = value.charAt(0);
    }
    var actualValue = ("" + value).replace(/[<>]/g, "");

    let validation = { isInvalid: false };
    if (!actualValue) {
      return { ...validation, isInvalid: true, isBlank: true };
      // resultBox.title = "";
      // resultBox.style.background = "#ffffff";
      // $("valid_" + row).value = false;
      // return true;
    }

    if (actualValue.trim() == ".") {
      validation = {
        ...validation,
        newValue: greaterThanOrLessThan + "0.0",
      };
    }

    if (isNaN(actualValue)) {
      return { ...validation, isInvalid: true, isNaN: true };
      // $("valid_" + row).value = false;
      // return false;
    }

    if (!isNaN(row.significantDigits)) {
      const valueStr = actualValue.toString();
      if (valueStr.includes(".")) {
        const decimalPlaces = valueStr.split(".")[1].length;
        if (decimalPlaces > row.significantDigits) {
          actualValue = parseFloat(actualValue).toFixed(row.significantDigits);
        }
      }
      validation = {
        ...validation,
        newValue: greaterThanOrLessThan + actualValue,
      };
    }

    return validation;
  };

  const handleChange = (e, rowId) => {
    const { name, id, value } = e.target;
    console.debug(
      "handleChange:" + id + ":" + name + ":" + value + ":" + rowId,
    );
    // setState({value: e.target.value})
    console.debug("State updated to ", e.target.value);
    var form = { ...props.results };
    jpSet(form, name, value);
    var refer = jpGet(form, "testResult[" + rowId + "].refer");
    var testId = jpGet(form, "testResult[" + rowId + "].testId");
    var referList = { ...referTest };
    referList[rowId] = refer === "true" ? true : false;
    setReferTest(referList);
    if (refer == "true") {
      jpSet(
        form,
        "testResult[" + rowId + "].referralItem.referredTestId",
        testId,
      );
      jpSet(
        form,
        "testResult[" + rowId + "].referralItem.referredSendDate",
        configurationProperties.currentDateAsText,
      );
    } else {
      jpSet(form, "testResult[" + rowId + "].referralItem.referredTestId", "");
      jpSet(
        form,
        "testResult[" + rowId + "].referralItem.referredSendDate",
        "",
      );
    }
    var isModified = "testResult[" + rowId + "].isModified";
    jpSet(form, isModified, "true");
    props.setResultForm(form);
  };

  const handleRejectCheckBoxChange = (e, rowId) => {
    const { name, checked } = e.target;
    var form = props.results;
    jpSet(form, name, checked);
    var shadowRejected = "testResult[" + rowId + "].shadowRejected";
    jpSet(form, shadowRejected, checked);
    var isModified = "testResult[" + rowId + "].isModified";
    jpSet(form, isModified, "true");

    var allrejectedItems = { ...rejectedItems };
    allrejectedItems[rowId] = checked;
    setRejectedItems(allrejectedItems);

    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: "result.reject.warning" }),
      kind: NotificationKinds.warning,
    });
    if (checked) {
      setNotificationVisible(true);
    }
  };

  const handleDatePickerChange = (date, rowId) => {
    var form = { ...props.results };
    if (form.testResult[rowId].referralItem) {
      if (form.testResult[rowId].referralItem.referredSendDate != date) {
        console.debug("handleDatePickerChange:" + date);
        jpSet(
          form,
          "testResult[" + rowId + "].referralItem.referredSendDate",
          date,
        );
        var isModified = "testResult[" + rowId + "].isModified";
        jpSet(form, isModified, "true");
        props.setResultForm(form);
      }
    }
  };

  const handleAcceptUnconditionally = (rowId, reason) => {
    const form = { ...props.results };
    jpSet(form, "testResult[" + rowId + "].forceTechApproval", "true");
    jpSet(form, "testResult[" + rowId + "].forceTechApprovalNote", reason);
    jpSet(form, "testResult[" + rowId + "].isModified", "true");
    props.setResultForm(form);

    const next = [...acceptAsIs];
    next[rowId] = true;
    setAcceptAsIs(next);
  };

  const handleUnacceptUnconditionally = (rowId) => {
    const form = { ...props.results };
    // BE's ResultUtil.isForcedToAcceptance treats non-blank as forced —
    // clearing requires "" / null, not "false".
    jpSet(form, "testResult[" + rowId + "].forceTechApproval", "");
    jpSet(form, "testResult[" + rowId + "].forceTechApprovalNote", "");
    jpSet(form, "testResult[" + rowId + "].isModified", "true");
    props.setResultForm(form);

    const next = [...acceptAsIs];
    next[rowId] = false;
    setAcceptAsIs(next);
  };

  const buildSignContext = () => {
    const results = (props.results && props.results.testResult) || [];
    const count = results.length;
    const accessions = [
      ...new Set(results.map((r) => r.accessionNumber).filter(Boolean)),
    ];
    if (accessions.length === 1) {
      return intl.formatMessage(
        {
          id: "esig.context.saveResults",
          defaultMessage: "Save {count} result(s) for accession {accession}",
        },
        {
          count,
          accession:
            convertAlphaNumLabNumForDisplay(accessions[0]) || accessions[0],
        },
      );
    }
    return intl.formatMessage(
      {
        id: "esig.context.saveResultsMulti",
        defaultMessage:
          "Save {count} result(s) across {accessionCount} accessions",
      },
      { count, accessionCount: accessions.length },
    );
  };

  const getFirstAnalysisId = () => {
    const results = (props.results && props.results.testResult) || [];
    for (const r of results) {
      if (r.analysisId) return Number(r.analysisId);
    }
    return 0;
  };

  const handleSave = () => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    var searchEndPoint = "/rest/LogbookResults";
    props.results.testResult.forEach((result) => {
      result.reportable = result.reportable === "N" ? false : true;
      delete result.result;
    });
    postToOpenElisServerJsonResponse(
      searchEndPoint,
      JSON.stringify(props.results),
      setResponse,
    );
  };

  const setResponse = (resp) => {
    console.debug("setStatus" + JSON.stringify(resp));
    setIsSubmitting(false);
    if (resp) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: createMesssage(resp),
        kind: NotificationKinds.success,
      });
      if (props.refreshOnSubmit) {
        window.location.href =
          "/result?type=" +
          props.searchBy.type +
          "&doRange=" +
          props.searchBy.doRange +
          props.extraParams;
      }
    } else {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.save.msg" }),
        kind: NotificationKinds.error,
      });
    }
    setNotificationVisible(true);
  };

  const createMesssage = (resp) => {
    var message = "";
    if (resp.reflex?.length > 0) {
      message +=
        intl.formatMessage({ id: "reflexTests" }) +
        ": " +
        resp.reflex.join(", ");
    }
    if (resp.calculated?.length > 0) {
      message +=
        intl.formatMessage({ id: "calculatedTests" }) +
        ": " +
        resp.calculated.join(", ");
    }
    if (message === "") {
      message += intl.formatMessage({ id: "success.save.msg" });
    }
    return message;
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }
    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {addRejectResult()}
      <>
        {props.results?.testResult?.length > 0 && (
          <Grid style={{ marginTop: "20px" }} className="gridBoundary">
            <Column lg={3} />
            <Column lg={7} sm={4}>
              <picture>
                <img
                  src={config.serverBaseUrl + "/images/nonconforming.gif"}
                  alt="nonconforming"
                  width="25"
                  height="20"
                />
              </picture>
              <b>
                {" "}
                <FormattedMessage id="validation.label.nonconform" />
              </b>
            </Column>
          </Grid>
        )}
        <Formik
          initialValues={SearchResultFormValues}
          //validationSchema={}
          onSubmit
          onChange
        >
          {({
            // values,
            // errors,
            // touched,
            handleChange,
            //handleBlur,
            // handleSubmit,
          }) => (
            <Form
              onChange={handleChange}
              //onBlur={handleBlur}
            >
              <DataTable
                data={props.results?.testResult?.slice(
                  (page - 1) * pageSize,
                  page * pageSize,
                )}
                columns={columns}
                isSortable
                expandableRows
                expandableRowsComponent={renderReferral}
              ></DataTable>
              <Pagination
                style={{ marginTop: "1.5rem" }}
                onChange={handlePageChange}
                page={page}
                pageSize={pageSize}
                pageSizes={[10, 20, 30, 50, 100]}
                totalItems={props.results?.testResult?.length}
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

              <ESignatureButton
                meaning={SignatureMeaning.AUTHORED}
                context={buildSignContext()}
                recordType="RESULT_BATCH"
                recordId={getFirstAnalysisId()}
                onSign={handleSave}
                disabled={isSubmitting}
                style={{ marginTop: "16px" }}
              >
                <FormattedMessage id="label.button.save" />
              </ESignatureButton>
            </Form>
          )}
        </Formik>
      </>
    </>
  );
}

export default injectIntl(ResultSearchPage);
