import React, { useContext, useState } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import {
  Form,
  TextInput,
  Button,
  Grid,
  Column,
  Select,
  SelectItem,
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
  Tag,
} from "@carbon/react";
import { Person } from "@carbon/react/icons";
import { patientSearchHeaderData } from "../data/PatientResultsTableHeaders";
import { Formik, Field } from "formik";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import CreatePatientFormValues from "../formModel/innitialValues/CreatePatientFormValues";

function PatientEditByProjext() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const [patientSearchResults, setPatientSearchResults] = useState([]);
  const [importStatus, setImportStatus] = useState({});
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [loading, setLoading] = useState(false);
  const [url, setUrl] = useState("");

  // Initial search form values
  const initialSearchFormValues = {
    searchType: "lastName",
    identifierType: "RTN",
    searchValue: "",
  };

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
    setLoading(true);

    // Construct the search endpoint based on the selected search type
    let searchEndPoint = "/rest/patient-search-results?";

    switch (values.searchType) {
      case "lastName":
        searchEndPoint += "lastName=" + values.searchValue;
        break;
      case "firstName":
        searchEndPoint += "firstName=" + values.searchValue;
        break;
      case "labNumber":
        searchEndPoint += "labNumber=" + values.searchValue;
        break;
      default:
        // For identifier types (RTN, EID, etc)
        searchEndPoint +=
          values.searchType +
          "=" +
          values.searchValue +
          "&identifierType=" +
          values.identifierType;
        break;
    }

    getFromOpenElisServer(searchEndPoint, fetchPatientResults);
    setUrl(searchEndPoint);
  };

  const fetchPatientResults = (res) => {
    let patientsResults = res.patientSearchResults;
    if (patientsResults && patientsResults.length > 0) {
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
    setLoading(false);
  };

  const fetchPatientDetails = (patientDetails) => {
    console.log("Patient details fetched:", patientDetails);
  };

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

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading />}
      <Formik
        initialValues={initialSearchFormValues}
        enableReinitialize={true}
        onSubmit={handleSubmit}
      >
        {({
          values,
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
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={8} md={8} sm={16}>
                <Field name="searchType">
                  {({ field }) => (
                    <Select
                      id="search-type-select"
                      labelText="Search By"
                      defaultValue="lastName"
                      onChange={(e) =>
                        setFieldValue("searchType", e.target.value)
                      }
                    >
                      <SelectItem value="firstName" text="First Name" />
                      <SelectItem value="lastName" text="Last Name" />
                      <SelectItem
                        value="patientIdentificationCode"
                        text="Patient Identification Code"
                      />
                      <SelectItem value="labNumber" text="Lab Number" />
                    </Select>
                  )}
                </Field>
              </Column>

              {values.searchType === "patientIdentificationCode" && (
                <Column lg={8} md={8} sm={16}>
                  <Field name="identifierType">
                    {({ field }) => (
                      <Select
                        id="identifier-type-select"
                        labelText="Identifier Type"
                        defaultValue="initialARV"
                        onChange={(e) =>
                          setFieldValue("identifierType", e.target.value)
                        }
                      >
                        <SelectItem value="initialARV" text="Initial ARV" />
                        <SelectItem value="followUpARV" text="Follow-up ARV" />
                        <SelectItem value="RTN" text="RTN" />
                        <SelectItem
                          value="ARVViralLoad"
                          text="ARV - Viral Load"
                        />
                        <SelectItem value="EID" text="EID" />
                        <SelectItem
                          value="recencyTesting"
                          text="Recency Testing"
                        />
                      </Select>
                    )}
                  </Field>
                </Column>
              )}

              <Column
                lg={values.searchType === "patientIdentificationCode" ? 16 : 8}
                md={16}
                sm={16}
              >
                <Field name="searchValue">
                  {({ field }) => (
                    <TextInput
                      name={field.name}
                      value={values[field.name]}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.search",
                        defaultMessage: "Type search here...",
                      })}
                      labelText="Search"
                      id={field.name}
                      onChange={(e) =>
                        setFieldValue(field.name, e.target.value)
                      }
                    />
                  )}
                </Field>
              </Column>

              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>

              <Column lg={4} md={4} sm={2}>
                <Button
                  id="search_button"
                  kind="primary"
                  type="submit"
                  data-cy="searchPatientButton"
                >
                  <FormattedMessage
                    id="label.button.search"
                    defaultMessage="Search"
                  />
                </Button>
              </Column>

              <Column lg={16}>
                <br />
                <br />
              </Column>
            </Grid>
          </Form>
        )}
      </Formik>

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

                    return (
                      <TableRow key={row.id}>
                        <TableCell>
                          {dataSourceName === "OpenElis" ? (
                            <input
                              type="radio"
                              data-cy="radioButton"
                              name="radio-group"
                              onClick={patientSelected}
                              id={row.id}
                            />
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
        forwardText={intl.formatMessage({
          id: "pagination.forward",
          defaultMessage: "Next",
        })}
        backwardText={intl.formatMessage({
          id: "pagination.backward",
          defaultMessage: "Previous",
        })}
        itemsPerPageText={intl.formatMessage({
          id: "pagination.items-per-page",
          defaultMessage: "Items per page:",
        })}
      />
    </>
  );
}

export default injectIntl(PatientEditByProjext);
