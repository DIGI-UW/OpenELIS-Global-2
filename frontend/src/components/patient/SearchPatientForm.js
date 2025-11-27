import React, { useContext, useState, useEffect } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import {
  Form,
  TextInput,
  Button,
  Grid,
  Column,
  RadioButton,
  RadioButtonGroup,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Loading,
  Toggle,
  Tag,
  Link,
} from "@carbon/react";
import { Person, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import { patientSearchHeaderData } from "../data/PatientResultsTableHeaders";
import { Formik, Field } from "formik";
import SearchPatientFormValues from "../formModel/innitialValues/SearchPatientFormValues";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import CustomDatePicker from "../common/CustomDatePicker";
import { ConfigurationContext } from "../layout/Layout";
import CreatePatientFormValues from "../formModel/innitialValues/CreatePatientFormValues";
import AsyncAvatar from "./photoManagement/photoAvatar/AyncAvatar";

function SearchPatientForm(props) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();

  const [dob, setDob] = useState("");
  const [patientSearchResults, setPatientSearchResults] = useState([]);
  const [importStatus, setImportStatus] = useState({});
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [loading, setLoading] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [isToggled, setIsToggled] = useState(false);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [url, setUrl] = useState("");
  const [searchFormValues, setSearchFormValues] = useState(
    SearchPatientFormValues,
  );
  const [prevfirstName, setPrevfirstName] = useState("");
  const [prevlastName, setPrevlastName] = useState("");

  const handlePatientImport = (patientId) => {
    console.log("Import button clicked, patientId:", patientId);

    const patientSelected = patientSearchResults.find(
      (patient) => patient.patientID === patientId,
    );
    console.log("Patient selected:", patientSelected);

    if (!patientSelected) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.no.patient.data" }),
        kind: NotificationKinds.error,
      });
      return;
    }

    const dataToSend = {
      ...CreatePatientFormValues,
      patientPK: "",
      nationalId: patientSelected.nationalId || "",
      subjectNumber: "",
      lastName: patientSelected.lastName || "",
      firstName: patientSelected.firstName || "",
      streetAddress: patientSelected.address?.street || "",
      city: patientSelected.address?.city || "",
      primaryPhone: patientSelected.contactPhone || "",
      gender: patientSelected.gender || "",
      birthDateForDisplay: patientSelected.birthdate || "",
      commune: patientSelected.commune || "",
      education: patientSelected.education || "",
      maritialStatus: patientSelected.maritalStatus || "",
      nationality: patientSelected.nationality || "",
      healthDistrict: patientSelected.healthDistrict || "",
      healthRegion: patientSelected.healthRegion || "",
      otherNationality: patientSelected.otherNationality || "",
      patientContact: {
        person: {
          firstName: patientSelected.contact?.firstName || "",
          lastName: patientSelected.contact?.lastName || "",
          primaryPhone: patientSelected.contact?.primaryPhone || "",
          email: patientSelected.contact?.email || "",
        },
      },
    };

    console.log("Data to send:", dataToSend);

    postToOpenElisServer(
      "/rest/PatientManagement",
      JSON.stringify(dataToSend),
      (status) => {
        handlePost(status, patientId);
      },
    );
  };

  const handlePost = (status, patientId) => {
    setNotificationVisible(true);
    if (status === 200) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.import.patient" }),
        kind: NotificationKinds.success,
      });
      setImportStatus((prevStatus) => ({
        ...prevStatus,
        [patientId]: true,
      }));
    } else {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.import.patient" }),
        kind: NotificationKinds.error,
      });
    }
  };

  const handleSubmit = (values) => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    setLoading(true);
    values.dateOfBirth = dob;
    let searchEndPoint =
      "/rest/patient-search-results?" +
      "lastName=" +
      values.lastName +
      "&firstName=" +
      values.firstName +
      "&STNumber=" +
      values.patientId +
      "&subjectNumber=" +
      values.patientId +
      "&nationalID=" +
      values.patientId +
      "&labNumber=" +
      values.labNumber +
      "&guid=" +
      values.guid +
      "&dateOfBirth=" +
      values.dateOfBirth +
      "&gender=" +
      values.gender +
      "&suppressExternalSearch=" +
      values.suppressExternalSearch;

    if (values.crSearch === true) {
      searchEndPoint += "&crSearch=true";
    }

    getFromOpenElisServer(searchEndPoint, fetchPatientResults);
    setUrl(searchEndPoint);
  };

  const loadNextResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(url + "&page=" + nextPage, fetchPatientResults);
  };

  const loadPreviousResultsPage = () => {
    setLoading(true);
    getFromOpenElisServer(url + "&page=" + previousPage, fetchPatientResults);
  };

  const toggle = () => {
    setIsToggled((prev) => !prev);
  };

  const fetchPatientResults = (res) => {
    let patientsResults = res.patientSearchResults;
    if (patientsResults.length > 0) {
      patientsResults.forEach((item) => (item.id = item.patientID));
      setPatientSearchResults(patientsResults);
    } else {
      setPatientSearchResults([]);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "patient.search.nopatient" }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
    }
    if (res.paging) {
      var { totalPages, currentPage } = res.paging;
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
    setLoading(false);
  };

  const fetchPatientDetails = (patientDetails) => {
    getFromOpenElisServer(
      `/rest/patient-photos/${patientDetails.patientPK}/${false}`,
      (response) => {
        if (response && response.data) {
          patientDetails.photo = response.data;
        }
      },
    );
    props.getSelectedPatient(patientDetails);
  };

  const handleDatePickerChange = (date) => {
    setDob(date);
  };

  function handleFirstNameChange(event) {
    const regexFlags = "iu";
    const regex = new RegExp(
      configurationProperties.FIRST_NAME_REGEX,
      regexFlags,
    );
    const value = event.target.value;
    if (!regex.test(value)) {
      event.target.value = prevfirstName;
    }
    setPrevfirstName(event.target.value);
  }

  function handleLastNameChange(event) {
    const regexFlags = "iu";
    const regex = new RegExp(
      configurationProperties.LAST_NAME_REGEX,
      regexFlags,
    );
    const value = event.target.value;
    if (!regex.test(value)) {
      event.target.value = prevlastName;
    }
    setPrevlastName(event.target.value);
  }

  const patientSelected = (e) => {
    const patientSelected = patientSearchResults.find((patient) => {
      return patient.patientID == e.target.id;
    });
    const searchEndPoint =
      "/rest/patient-details?patientID=" + patientSelected.patientID;
    getFromOpenElisServer(searchEndPoint, fetchPatientDetails);
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };
  useEffect(() => {
    let patientId = new URLSearchParams(window.location.search).get(
      "patientId",
    );
    if (patientId) {
      const searchEndPoint = "/rest/patient-details?patientID=" + patientId;
      getFromOpenElisServer(searchEndPoint, fetchPatientDetails);
    }
  }, []);
  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <Formik
        initialValues={searchFormValues}
        enableReinitialize={true}
        // validationSchema={}
        onSubmit={handleSubmit}
        onChange
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
            <Grid>
              <Field name="guid">
                {({ field }) => (
                  <input type="hidden" name={field.name} id={field.name} />
                )}
              </Field>
              <Column lg={16} md={8} sm={4}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="patientId">
                  {({ field }) => (
                    <TextInput
                      name={field.name}
                      value={values[field.name]}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientId",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.id",
                        defaultMessage: "Patient Id",
                      })}
                      id={field.name}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="labNumber">
                  {({ field }) => (
                    <CustomLabNumberInput
                      name={field.name}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.prevLabNumber",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.prev.lab.no",
                        defaultMessage: "Previous Lab Number",
                      })}
                      id={field.name}
                      value={values[field.name]}
                      onChange={(e, rawValue) => {
                        setFieldValue(field.name, rawValue);
                      }}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="lastName">
                  {({ field }) => (
                    <TextInput
                      name={field.name}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientLastName",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.last.name",
                        defaultMessage: "Last Name",
                      })}
                      id={field.name}
                      onChange={(e) => handleLastNameChange(e)}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="firstName">
                  {({ field }) => (
                    <TextInput
                      name={field.name}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientFirstName",
                      })}
                      labelText={intl.formatMessage({
                        id: "patient.first.name",
                        defaultMessage: "First Name",
                      })}
                      id={field.name}
                      onChange={(e) => handleFirstNameChange(e)}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="dateOfBirth">
                  {({ field }) => (
                    <CustomDatePicker
                      id={"date-picker-default-id"}
                      labelText={intl.formatMessage({
                        id: "patient.dob",
                        defaultMessage: "Date of Birth",
                      })}
                      autofillDate={true}
                      value={values.birthDateForDisplay || ""}
                      onChange={(date) => handleDatePickerChange(date)}
                      name={field.name}
                      disallowFutureDate={true}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="gender">
                  {({ field }) => (
                    <RadioButtonGroup
                      defaultSelected=""
                      legendText={intl.formatMessage({
                        id: "patient.gender",
                        defaultMessage: "Gender",
                      })}
                      name={field.name}
                      id="search_patient_gender"
                    >
                      <RadioButton
                        id="search-radio-1"
                        labelText={intl.formatMessage({
                          id: "patient.male",
                          defaultMessage: "Male",
                        })}
                        value="M"
                      />
                      <RadioButton
                        id="search-radio-2"
                        labelText={intl.formatMessage({
                          id: "patient.female",
                          defaultMessage: "Female",
                        })}
                        value="F"
                      />
                    </RadioButtonGroup>
                  )}
                </Field>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {" "}
                <br />{" "}
              </Column>
              <Column lg={4} md={4} sm={2}>
                <Button
                  id="local_search"
                  kind="tertiary"
                  type="submit"
                  data-cy="searchPatientButton"
                  onClick={() => setFieldValue("suppressExternalSearch", true)}
                >
                  <FormattedMessage id="label.button.search" />
                </Button>
              </Column>
              <Column lg={4} md={4} sm={2}>
                <Button
                  id="external_search"
                  type="submit"
                  disabled={
                    configurationProperties.UseExternalPatientInfo === "false"
                  }
                  kind="tertiary"
                  onClick={() => setFieldValue("suppressExternalSearch", false)}
                >
                  <FormattedMessage
                    id="label.button.externalsearch"
                    defaultMessage="External Search"
                  />
                </Button>
              </Column>
              {configurationProperties.ENABLE_CLIENT_REGISTRY === "true" && (
                <Column lg={4} md={4} sm={2}>
                  <Toggle
                    labelText="Client Registry Search"
                    labelA="false"
                    labelB="true"
                    id="toggle-cr"
                    toggled={isToggled}
                    onClick={() => {
                      toggle();
                      setFieldValue("crSearch", !isToggled);
                    }}
                  />
                </Column>
              )}
              <Column lg={16}>
                {" "}
                <br />
                <br />
              </Column>
            </Grid>
          </Form>
        )}
      </Formik>
      {pagination && (
        <Grid>
          <Column lg={8}>
            {" "}
            <div></div>
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
      <DataTable
        rows={patientSearchResults}
        headers={patientSearchHeaderData}
        isSortable
      >
        {({ rows, headers, getHeaderProps, getTableProps }) => (
          <TableContainer title="Patient Results">
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  <TableHeader />
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
                {rows
                  .slice((page - 1) * pageSize, page * pageSize)
                  .map((row) => {
                    const dataSourceName = row.cells.find(
                      (cell) => cell.info.header === "dataSourceName",
                    )?.value;
                    const firstName =
                      row.cells.find((cell) => cell.info.header === "firstName")
                        ?.value || "";
                    const lastName =
                      row.cells.find((cell) => cell.info.header === "lastName")
                        ?.value || "";
                    const patientName =
                      `${firstName} ${lastName}`.trim() || "Patient";

                    return (
                      <TableRow key={row.id}>
                        <TableCell>
                          {dataSourceName === "OpenElis" ? (
                            <div
                              style={{ display: "flex", flexDirection: "row" }}
                            >
                              <RadioButton
                                data-cy="radioButton"
                                name="radio-group"
                                onClick={patientSelected}
                                labelText=""
                                id={row.id}
                              />
                              <AsyncAvatar
                                patientId={row.id}
                                hasPhoto={true}
                                patientName={patientName}
                              />
                            </div>
                          ) : (
                            <span></span>
                          )}
                        </TableCell>

                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "dataSourceName" ? (
                              <>
                                <Tag
                                  type={
                                    cell.value === "OpenElis"
                                      ? "red"
                                      : cell.value === "Open Client Registry"
                                        ? "green"
                                        : "gray"
                                  }
                                >
                                  {cell.value}
                                </Tag>
                                &nbsp;&nbsp; &nbsp;&nbsp; &nbsp;&nbsp;
                                {dataSourceName === "Open Client Registry" ? (
                                  <Button
                                    id={row.id}
                                    kind="tertiary"
                                    onClick={() => handlePatientImport(row.id)}
                                    size="md"
                                    disabled={importStatus[row.id]}
                                  >
                                    <Person size={16} />
                                    {importStatus[row.id] ? (
                                      <span>
                                        &nbsp;&nbsp;Patient Imported
                                        Successfully
                                      </span>
                                    ) : (
                                      <span>&nbsp;&nbsp;Import Patient</span>
                                    )}
                                  </Button>
                                ) : (
                                  <span></span>
                                )}
                              </>
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    );
                  })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
      <Pagination
        onChange={handlePageChange}
        page={page}
        pageSize={pageSize}
        pageSizes={[10, 20, 30, 50, 100]}
        totalItems={patientSearchResults.length}
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
          intl.formatMessage({ id: "pagination.item" }, { min: min, max: max })
        }
        pageNumberText={intl.formatMessage({
          id: "pagination.page-number",
        })}
        pageRangeText={(_current, total) =>
          intl.formatMessage({ id: "pagination.page-range" }, { total: total })
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
}

export default injectIntl(SearchPatientForm);
