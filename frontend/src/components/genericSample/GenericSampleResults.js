import React, { useState, useContext, useRef, useEffect } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import {
  Grid,
  Column,
  Section,
  Heading,
  Form,
  Button,
  Loading,
  TextInput,
  TextArea,
  Checkbox,
  Select,
  SelectItem,
  Pagination,
} from "@carbon/react";
import { Copy } from "@carbon/icons-react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  convertAlphaNumLabNumForDisplay,
} from "../utils/Utils";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import DataTable from "react-data-table-component";
import { Formik } from "formik";
import config from "../../config.json";

/**
 * GenericSampleResults - Result Entry page for Generic Sample menu.
 *
 * Features:
 * - Search by accession number
 * - Display test results in a data table
 * - Enter and save results
 * - Similar layout to AccessionResults but simplified
 *
 * Related: Feature 001-sample-management
 */
function GenericSampleResults() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const componentMounted = useRef(true);

  // Breadcrumb navigation
  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "sample.label.generic", link: "/GenericSample/Order" },
    { label: "result.entry.label" },
  ];

  // Search state
  const [accessionNumber, setAccessionNumber] = useState("");
  const [loading, setLoading] = useState(false);

  // Results state
  const [results, setResults] = useState({ testResult: [] });

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Form state
  const [acceptAsIs, setAcceptAsIs] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  /**
   * Search for results by accession number.
   */
  const handleSearch = () => {
    if (!accessionNumber || !accessionNumber.trim()) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "result.entry.error.accessionRequired",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }

    setLoading(true);
    setResults({ testResult: [] });

    // Use the full accession number as-is
    // Note: We don't filter by finished status so users can see and edit results after saving
    const labNumber = accessionNumber.trim();
    const searchEndPoint =
      "/rest/LogbookResults?" +
      "labNumber=" +
      encodeURIComponent(labNumber) +
      "&doRange=false" +
      "&patientPK=" +
      "&collectionDate=" +
      "&recievedDate=" +
      "&selectedTest=" +
      "&selectedSampleStatus=" +
      "&selectedAnalysisStatus=" +
      "&testSectionId=" +
      "&upperRangeAccessionNumber=";

    getFromOpenElisServer(searchEndPoint, (data) => {
      setLoading(false);
      if (data && data.testResult && data.testResult.length > 0) {
        // Add IDs to results
        let i = 0;
        data.testResult.forEach((item) => (item.id = "" + i++));
        setResults(data);
      } else {
        setResults({ testResult: [] });
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage(
            { id: "result.entry.noResults" },
            { accessionNumber },
          ),
          kind: NotificationKinds.info,
        });
        setNotificationVisible(true);
      }
    });
  };

  /**
   * Handle result value change.
   */
  const handleResultChange = (e, rowId) => {
    const { name, value } = e.target;
    const form = { ...results };
    const jp = require("jsonpath");
    jp.value(form, name, value);
    // Also update pastNotes when note changes for display
    if (name.includes(".note")) {
      const pastNotesPath = "testResult[" + rowId + "].pastNotes";
      jp.value(form, pastNotesPath, value);
    }
    const isModified = "testResult[" + rowId + "].isModified";
    jp.value(form, isModified, "true");
    setResults(form);
  };

  /**
   * Handle accept as is checkbox.
   */
  const handleAcceptAsIsChange = (e, rowId) => {
    handleResultChange(e, rowId);
    if (acceptAsIs[rowId] === undefined) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "result.acceptasis.warning" }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
    }
    const newAcceptAsIs = [...acceptAsIs];
    newAcceptAsIs[rowId] = !acceptAsIs[rowId];
    setAcceptAsIs(newAcceptAsIs);
  };

  /**
   * Save results.
   */
  const handleSave = () => {
    if (isSubmitting) return;
    setIsSubmitting(true);

    const searchEndPoint = "/rest/LogbookResults";
    const dataToSend = { ...results };
    dataToSend.testResult.forEach((result) => {
      result.reportable = result.reportable === "N" ? false : true;
      delete result.result;
    });

    postToOpenElisServerJsonResponse(
      searchEndPoint,
      JSON.stringify(dataToSend),
      (resp) => {
        setIsSubmitting(false);
        if (resp) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "success.save.msg" }),
            kind: NotificationKinds.success,
          });
          // Refresh results
          handleSearch();
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "error.save.msg" }),
            kind: NotificationKinds.error,
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  /**
   * Handle pagination change.
   */
  const handlePageChange = (pageInfo) => {
    if (page !== pageInfo.page) setPage(pageInfo.page);
    if (pageSize !== pageInfo.pageSize) setPageSize(pageInfo.pageSize);
  };

  /**
   * Table columns configuration.
   */
  const columns = [
    {
      id: "sampleInfo",
      name: intl.formatMessage({ id: "column.name.sampleInfo" }),
      cell: (row) => {
        const formatLabNum =
          configurationProperties?.AccessionFormat === "ALPHANUM";
        return (
          <div className="sampleInfo">
            <Button
              kind="ghost"
              size="sm"
              hasIconOnly
              renderIcon={Copy}
              iconDescription={intl.formatMessage({
                id: "instructions.copy.labnum",
              })}
              onClick={async () => {
                await navigator.clipboard.writeText(row.accessionNumber);
              }}
            />
            <br />
            {row.sampleItemExternalId ||
              (formatLabNum
                ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
                : row.accessionNumber) +
                "-" +
                row.sequenceNumber}
            <br />
            {row.patientName}
            <br />
            {row.patientInfo}
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
          </div>
        );
      },
      sortable: true,
      selector: (row) => row.accessionNumber,
      width: "14rem",
    },
    {
      id: "testDate",
      name: intl.formatMessage({ id: "column.name.testDate" }),
      selector: (row) => row.testDate,
      sortable: true,
      width: "7rem",
    },
    {
      id: "testName",
      name: intl.formatMessage({ id: "column.name.testName" }),
      cell: (row) => {
        const fullTestName = row.testName || "";
        const splitIndex = fullTestName.lastIndexOf("(");
        const testName =
          splitIndex > 0 ? fullTestName.substring(0, splitIndex) : fullTestName;
        const sampleType =
          splitIndex > 0 ? fullTestName.substring(splitIndex) : "";
        return (
          <div className="sampleInfo">
            {testName}
            <br />
            {sampleType}
          </div>
        );
      },
      sortable: true,
      selector: (row) => row.testName,
      width: "12rem",
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
      cell: (row) => (
        <Checkbox
          id={"testResult" + row.id + ".forceTechApproval"}
          name={"testResult[" + row.id + "].forceTechApproval"}
          labelText=""
          onChange={(e) => handleAcceptAsIsChange(e, row.id)}
        />
      ),
      width: "5rem",
    },
    {
      id: "result",
      name: intl.formatMessage({ id: "column.name.result" }),
      cell: (row) => {
        if (
          row.resultType === "D" ||
          row.resultType === "M" ||
          row.resultType === "C"
        ) {
          // Dictionary/Multi/Cascading - dropdown
          return (
            <Select
              id={"testResult" + row.id + ".resultValue"}
              name={"testResult[" + row.id + "].resultValue"}
              labelText=""
              hideLabel
              defaultValue={row.resultValue || ""}
              onChange={(e) => handleResultChange(e, row.id)}
            >
              <SelectItem value="" text="" />
              {row.dictionaryResults?.map((dict, idx) => (
                <SelectItem key={idx} value={dict.id} text={dict.value} />
              ))}
            </Select>
          );
        } else {
          // Numeric/Text/etc - text input
          return (
            <TextInput
              id={"testResult" + row.id + ".resultValue"}
              name={"testResult[" + row.id + "].resultValue"}
              labelText=""
              hideLabel
              size="sm"
              defaultValue={row.resultValue || ""}
              onChange={(e) => handleResultChange(e, row.id)}
            />
          );
        }
      },
      width: "10rem",
    },
    {
      id: "currentResult",
      name: intl.formatMessage({ id: "column.name.currentResult" }),
      cell: (row) => {
        // For dictionary/coded results, show the display text instead of the ID
        if (
          (row.resultType === "D" ||
            row.resultType === "M" ||
            row.resultType === "C") &&
          row.resultValue &&
          row.dictionaryResults
        ) {
          const dictItem = row.dictionaryResults.find(
            (dict) => dict.id === row.resultValue,
          );
          return dictItem ? dictItem.value : row.resultValue;
        }
        return row.resultValue || "";
      },
      width: "8rem",
    },
    {
      id: "notes",
      name: intl.formatMessage({ id: "column.name.notes" }),
      cell: (row) => (
        <TextArea
          id={"testResult" + row.id + ".note"}
          name={"testResult[" + row.id + "].note"}
          labelText=""
          hideLabel
          rows={2}
          defaultValue={row.pastNotes || ""}
          onChange={(e) => handleResultChange(e, row.id)}
        />
      ),
      width: "15rem",
    },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="result.entry.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>

      {notificationVisible && <AlertDialog />}
      {loading && <Loading />}

      <div className="orderLegendBody">
        {/* Search Form */}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <h4>
              <FormattedMessage id="label.button.search" />
            </h4>
          </Column>
        </Grid>

        <Grid fullWidth={true}>
          <Column lg={6} md={4} sm={4}>
            <CustomLabNumberInput
              id="accessionNumber"
              name="accessionNumber"
              labelText={<FormattedMessage id="search.label.accession" />}
              placeholder={intl.formatMessage({
                id: "result.entry.search.accessionPlaceholder",
              })}
              value={accessionNumber}
              onChange={(e, rawValue) => {
                // rawValue is provided for ALPHANUM format, otherwise use e.target.value
                const value =
                  rawValue !== undefined ? rawValue : e.target.value;
                setAccessionNumber(value);
              }}
            />
          </Column>
          <Column lg={2} md={2} sm={2}>
            <Button
              type="button"
              onClick={handleSearch}
              disabled={loading}
              style={{ marginTop: "1.5rem" }}
            >
              <FormattedMessage id="label.button.search" />
            </Button>
          </Column>
        </Grid>

        {/* Results Table */}
        {results.testResult && results.testResult.length > 0 && (
          <>
            <Grid fullWidth={true} style={{ marginTop: "2rem" }}>
              <Column lg={16} md={8} sm={4}>
                <h4>
                  <FormattedMessage id="result.entry.results.title" />
                </h4>
              </Column>
            </Grid>

            <Formik initialValues={{}} onSubmit={() => {}} onChange={() => {}}>
              {({ handleChange }) => (
                <Form onChange={handleChange}>
                  <DataTable
                    data={results.testResult.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )}
                    columns={columns}
                    isSortable
                  />
                  <Pagination
                    onChange={handlePageChange}
                    page={page}
                    pageSize={pageSize}
                    pageSizes={[10, 20, 30, 50, 100]}
                    totalItems={results.testResult.length}
                    forwardText={intl.formatMessage({
                      id: "pagination.forward",
                    })}
                    backwardText={intl.formatMessage({
                      id: "pagination.backward",
                    })}
                    itemRangeText={(min, max, total) =>
                      intl.formatMessage(
                        { id: "pagination.item-range" },
                        { min, max, total },
                      )
                    }
                    itemsPerPageText={intl.formatMessage({
                      id: "pagination.items-per-page",
                    })}
                    itemText={(min, max) =>
                      intl.formatMessage(
                        { id: "pagination.item" },
                        { min, max },
                      )
                    }
                    pageNumberText={intl.formatMessage({
                      id: "pagination.page-number",
                    })}
                    pageRangeText={(_current, total) =>
                      intl.formatMessage(
                        { id: "pagination.page-range" },
                        { total },
                      )
                    }
                    pageText={(page, pagesUnknown) =>
                      intl.formatMessage(
                        { id: "pagination.page" },
                        { page: pagesUnknown ? "" : page },
                      )
                    }
                  />
                  <Button
                    type="button"
                    onClick={handleSave}
                    disabled={isSubmitting}
                    style={{ marginTop: "1rem" }}
                  >
                    <FormattedMessage id="label.button.save" />
                  </Button>
                </Form>
              )}
            </Formik>
          </>
        )}
      </div>
    </>
  );
}

export default injectIntl(GenericSampleResults);
