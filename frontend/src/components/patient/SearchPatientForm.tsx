import React, { useContext, useState, useEffect, useRef } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import {
  serverPageArrowsProps,
  serverPageSizeOf,
  serverPaginationProps,
} from "../utils/serverPaging";
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
} from "@carbon/react";
import ServerPageArrows from "../common/ServerPageArrows";
import { Person } from "@carbon/react/icons";
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
import type {
  Nullable,
  PatientRecord,
  PatientSearchCriteria,
  PatientSearchResponse,
} from "./types";

interface SearchPatientFormProps {
  getSelectedPatient?: (patient: PatientRecord) => void;
  setOrderFormValues?: React.Dispatch<
    React.SetStateAction<Record<string, unknown>>
  >;
  orderFormValues?: Record<string, unknown>;
  showPatientSearch?: boolean;
  patientSearchStatus?: boolean;
  /** Prefix for every element id, so two search forms can share one page. */
  idPrefix?: string;
  /** Patients (by patientID) left out of the results, e.g. one already chosen elsewhere on the page. */
  excludePatientIds?: string[];
  [key: string]: unknown;
}

type ImportStatus = Record<string, boolean>;

function SearchPatientForm(props: SearchPatientFormProps) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const intl = useIntl();
  const fieldId = (name: string) =>
    props.idPrefix ? `${props.idPrefix}-${name}` : name;

  const [dob, setDob] = useState("");
  const [patientSearchResults, setPatientSearchResults] = useState<
    PatientRecord[]
  >([]);
  const [importStatus, setImportStatus] = useState<ImportStatus>({});
  // The server's page announcement for the list shown, and the rows a full
  // server page holds; Carbon's items per page is pinned to the latter so
  // Carbon's page is the server's page.
  const [paging, setPaging] = useState<{
    currentPage?: string | number;
    totalPages?: string | number;
  }>();
  const [serverPageSize, setServerPageSize] = useState<number | undefined>();
  const [loading, setLoading] = useState(false);
  const [isToggled, setIsToggled] = useState(false);
  const [url, setUrl] = useState("");
  const [searchFormValues, setSearchFormValues] = useState(
    SearchPatientFormValues,
  );
  const [prevfirstName, setPrevfirstName] = useState("");
  const [prevlastName, setPrevlastName] = useState("");
  // Bumped by Clear: remounting the form is what empties the uncontrolled
  // inputs and the gender radios along with Formik's values.
  const [formInstance, setFormInstance] = useState(0);
  // When a lab-number deep link drives the search, auto-select the matched
  // patient once results arrive (so the user lands on the patient page, not the
  // search results). Manual searches leave this false and just list results.
  const autoSelectOnResults = useRef(false);

  const handlePatientImport = (patientId: string) => {
    const patientSelected = patientSearchResults.find(
      (patient) => patient.patientID === patientId,
    );

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

  const handlePost = (status: number, patientId: string) => {
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

  const handleSubmit = (values: PatientSearchCriteria) => {
    setPatientSearchResults([]);
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

  /** One server page, the same request for the arrows and for Carbon. */
  const loadResultsPage = (pageNumber: number | string | null) => {
    setLoading(true);
    getFromOpenElisServer(url + "&page=" + pageNumber, fetchPatientResults);
  };
  const arrows = serverPageArrowsProps({
    paging,
    onPageRequest: loadResultsPage,
  });

  const toggle = () => {
    setIsToggled((prev) => !prev);
  };

  /** Back to a blank search: every criterion, the date, the CR toggle and the results. */
  const clearSearch = () => {
    setSearchFormValues({ ...SearchPatientFormValues });
    setFormInstance((instance) => instance + 1);
    setDob("");
    setIsToggled(false);
    setPrevfirstName("");
    setPrevlastName("");
    setPatientSearchResults([]);
    setPaging(undefined);
    setUrl("");
  };

  const fetchPatientResults = (res: PatientSearchResponse) => {
    if (!res || !res.patientSearchResults) {
      setPatientSearchResults([]);
      return;
    }
    let patientsResults = res.patientSearchResults;
    // Filter out the EQA placeholder patient (NULL/NULL)
    patientsResults = patientsResults.filter(
      (p) => !(p.lastName === "NULL" && p.firstName === "NULL"),
    );
    if (patientsResults.length > 0) {
      patientsResults.forEach((item) => (item.id = item.patientID));
      setPatientSearchResults(patientsResults);
      if (autoSelectOnResults.current) {
        autoSelectOnResults.current = false;
        const localPatient =
          patientsResults.find((p) => p.dataSourceName === "OpenElis") ||
          patientsResults[0];
        if (localPatient) {
          getFromOpenElisServer(
            "/rest/patient-details?patientID=" + localPatient.patientID,
            fetchPatientDetails,
          );
        }
      }
    } else {
      setPatientSearchResults([]);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "patient.search.nopatient" }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
    }
    setPaging(res.paging);
    setServerPageSize((previous) =>
      serverPageSizeOf(res.paging, patientsResults.length, previous),
    );
    setLoading(false);
  };

  const fetchPatientDetails = (patientDetails: PatientRecord) => {
    // Hand the patient on from inside the callback: the consumer seeds its form
    // from this object once, so a photo attached later never reaches it.
    getFromOpenElisServer(
      `/rest/patient-photos/${patientDetails.patientPK}/${false}`,
      (response) => {
        props.getSelectedPatient!({
          ...patientDetails,
          photo: response && response.data ? response.data : "",
        });
      },
    );
  };

  const handleDatePickerChange = (date: string) => {
    setDob(date);
  };

  function handleFirstNameChange(event: React.ChangeEvent<HTMLInputElement>) {
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

  function handleLastNameChange(event: React.ChangeEvent<HTMLInputElement>) {
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

  const patientSelected = (patientId: string) => {
    const searchEndPoint = "/rest/patient-details?patientID=" + patientId;
    getFromOpenElisServer(searchEndPoint, fetchPatientDetails);
  };

  const excludedPatientIds = props.excludePatientIds || [];
  const visibleResults = excludedPatientIds.length
    ? patientSearchResults.filter(
        (patient) => !excludedPatientIds.includes(String(patient.patientID)),
      )
    : patientSearchResults;

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const patientId = params.get("patientId");
    if (patientId) {
      const searchEndPoint = "/rest/patient-details?patientID=" + patientId;
      getFromOpenElisServer(searchEndPoint, fetchPatientDetails);
      return;
    }
    // Deep link from elsewhere (e.g. the Validation page) — prefill the lab
    // number and run the search so the matching patient surfaces immediately.
    const labNumber = params.get("labNumber");
    if (labNumber) {
      autoSelectOnResults.current = true;
      setSearchFormValues({ ...SearchPatientFormValues, labNumber });
      handleSubmit({ ...SearchPatientFormValues, labNumber });
    }
  }, []);
  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <Formik
        key={formInstance}
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
                  <input
                    type="hidden"
                    name={field.name}
                    id={fieldId(field.name)}
                  />
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
                      id={fieldId(field.name)}
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
                      id={fieldId(field.name)}
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
                      id={fieldId(field.name)}
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
                      id={fieldId(field.name)}
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
                      id={fieldId("date-picker-default-id")}
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
                      id={fieldId("search_patient_gender")}
                    >
                      <RadioButton
                        id={fieldId("search-radio-1")}
                        labelText={intl.formatMessage({
                          id: "patient.male",
                          defaultMessage: "Male",
                        })}
                        value="M"
                      />
                      <RadioButton
                        id={fieldId("search-radio-2")}
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
                  id={fieldId("local_search")}
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
                  id={fieldId("external_search")}
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
              <Column lg={4} md={4} sm={2}>
                <Button
                  id={fieldId("clear_search")}
                  type="button"
                  kind="ghost"
                  data-cy="clearPatientSearchButton"
                  onClick={clearSearch}
                >
                  <FormattedMessage id="label.button.clear" />
                </Button>
              </Column>
              {configurationProperties.ENABLE_CLIENT_REGISTRY === "true" && (
                <Column lg={4} md={4} sm={2}>
                  <Toggle
                    labelText="Client Registry Search"
                    labelA="false"
                    labelB="true"
                    id={fieldId("toggle-cr")}
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
      {arrows.show && <ServerPageArrows {...arrows} />}
      <DataTable
        rows={visibleResults}
        headers={patientSearchHeaderData}
        isSortable
      >
        {({ rows, headers, getHeaderProps, getTableProps }) => (
          <TableContainer data-cy="patientResultsTable">
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
                {rows.map((row) => {
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
                  const sourcePatient = patientSearchResults.find(
                    (p) => p.patientID === row.id,
                  );
                  const isMerged = sourcePatient?.isMerged === true;
                  const mergedIntoLabel =
                    sourcePatient?.mergedIntoNationalId ||
                    sourcePatient?.mergedIntoPatientId;

                  return (
                    <TableRow
                      key={row.id}
                      data-cy={`patient-result-row-${row.id}`}
                    >
                      <TableCell>
                        {dataSourceName === "OpenElis" ? (
                          <div
                            style={{ display: "flex", flexDirection: "row" }}
                          >
                            <RadioButton
                              data-cy="radioButton"
                              name={fieldId("radio-group")}
                              onClick={() => patientSelected(String(row.id))}
                              labelText=""
                              id={fieldId(String(row.id))}
                            />
                            <AsyncAvatar
                              patientId={row.id}
                              hasPhoto={true}
                              patientName={patientName}
                            />
                            {isMerged && (
                              <Tag
                                type="magenta"
                                size="sm"
                                title={
                                  mergedIntoLabel
                                    ? `Merged into ${mergedIntoLabel}`
                                    : "Merged"
                                }
                                style={{ marginLeft: "0.5rem" }}
                              >
                                <FormattedMessage
                                  id="patient.search.merged.tag"
                                  defaultMessage="Merged"
                                />
                              </Tag>
                            )}
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
                                      &nbsp;&nbsp;Patient Imported Successfully
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
        {...serverPaginationProps({
          paging,
          rowsOnPage: visibleResults.length,
          pageSize: serverPageSize,
          onPageRequest: loadResultsPage,
          intl,
        })}
      />
    </>
  );
}

export default injectIntl(SearchPatientForm);
