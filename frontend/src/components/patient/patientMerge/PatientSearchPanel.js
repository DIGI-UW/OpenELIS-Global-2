import React, { useState, useContext, useRef } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Grid,
  Column,
  TextInput,
  Button,
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
  Tag,
  Tile,
} from "@carbon/react";
import { Search } from "@carbon/react/icons";
import { Formik, Field } from "formik";
import CustomDatePicker from "../../common/CustomDatePicker";
import { ConfigurationContext } from "../../layout/Layout";
import { searchPatients, getPatientMergeDetails } from "./patientMergeService";

const patientSearchHeaders = [
  { key: "lastName", header: "Last Name" },
  { key: "firstName", header: "First Name" },
  { key: "gender", header: "Gender" },
  { key: "dob", header: "Date of Birth" },
  { key: "subjectNumber", header: "Unique Health ID" },
  { key: "nationalId", header: "National ID" },
  { key: "dataSourceName", header: "Data Source" },
];

function PatientSearchPanel({
  panelId,
  title,
  selectedPatient,
  onPatientSelect,
  otherSelectedPatient,
}) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);

  const [searchResults, setSearchResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [dob, setDob] = useState("");
  const formikRef = useRef(null);

  const initialValues = {
    patientId: "",
    firstName: "",
    lastName: "",
    gender: "",
    dateOfBirth: "",
    suppressExternalSearch: true,
  };

  const handleSearch = async (values, suppressExternal = true) => {
    setLoading(true);
    setHasSearched(true);
    try {
      const result = await searchPatients({
        ...values,
        dateOfBirth: dob,
        suppressExternalSearch: suppressExternal,
      });

      if (
        result.patientSearchResults &&
        result.patientSearchResults.length > 0
      ) {
        // Add unique id for DataTable and filter out other selected patient
        const processedResults = result.patientSearchResults
          .map((patient) => ({
            ...patient,
            id: patient.patientID,
          }))
          .filter((patient) => {
            // Filter out the other selected patient to prevent selecting same patient twice
            if (
              otherSelectedPatient &&
              patient.patientID === otherSelectedPatient.patientID
            ) {
              return false;
            }
            // Only show OpenElis patients (not external registry patients)
            return patient.dataSourceName === "OpenElis";
          });

        setSearchResults(processedResults);
      } else {
        setSearchResults([]);
      }
    } catch (error) {
      console.error("Search error:", error);
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  };

  const handlePatientSelect = async (patientId) => {
    const patient = searchResults.find((p) => p.patientID === patientId);
    if (patient) {
      // Transform to match expected format with patientPK
      let selectedPatientData = {
        ...patient,
        patientPK: patient.patientID,
      };

      // Fetch detailed data including clinical summary
      try {
        const details = await getPatientMergeDetails(patientId);
        if (details) {
          // Enrich with dataSummary from merge details API
          selectedPatientData = {
            ...selectedPatientData,
            dataSummary: details.dataSummary,
          };
        }
      } catch (error) {
        console.error("Failed to fetch patient details:", error);
        // Continue with basic data if details fetch fails
      }

      onPatientSelect(selectedPatientData);
    }
  };

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
  };

  // Check if form has any search criteria
  const isFormEmpty = (values) => {
    return (
      !values.patientId?.trim() &&
      !values.firstName?.trim() &&
      !values.lastName?.trim() &&
      !values.gender &&
      !dob
    );
  };

  return (
    <div className="patientSelectionSection">
      <h4>{title}</h4>

      {loading && <Loading />}

      <Formik
        innerRef={formikRef}
        initialValues={initialValues}
        onSubmit={(values) => handleSearch(values)}
      >
        {({ values, handleSubmit, setFieldValue }) => (
          <form onSubmit={handleSubmit}>
            <Grid className="searchFormGrid">
              <Column lg={8} md={4} sm={4}>
                <Field name="patientId">
                  {({ field }) => (
                    <TextInput
                      {...field}
                      id={`${panelId}-patientId`}
                      labelText={intl.formatMessage({ id: "patient.id" })}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientId",
                      })}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="firstName">
                  {({ field }) => (
                    <TextInput
                      {...field}
                      id={`${panelId}-firstName`}
                      labelText={intl.formatMessage({
                        id: "patient.first.name",
                      })}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientFirstName",
                      })}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="lastName">
                  {({ field }) => (
                    <TextInput
                      {...field}
                      id={`${panelId}-lastName`}
                      labelText={intl.formatMessage({
                        id: "patient.last.name",
                      })}
                      placeholder={intl.formatMessage({
                        id: "input.placeholder.patientLastName",
                      })}
                    />
                  )}
                </Field>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Field name="gender">
                  {({ field }) => (
                    <RadioButtonGroup
                      legendText={intl.formatMessage({ id: "patient.gender" })}
                      name={`${panelId}-${field.name}`}
                      id={`${panelId}-gender`}
                      onChange={(value) => setFieldValue("gender", value)}
                      valueSelected={values.gender}
                    >
                      <RadioButton
                        id={`${panelId}-male`}
                        labelText={intl.formatMessage({ id: "patient.male" })}
                        value="M"
                      />
                      <RadioButton
                        id={`${panelId}-female`}
                        labelText={intl.formatMessage({ id: "patient.female" })}
                        value="F"
                      />
                    </RadioButtonGroup>
                  )}
                </Field>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <CustomDatePicker
                  id={`${panelId}-dob`}
                  labelText={intl.formatMessage({ id: "patient.dob" })}
                  value={dob}
                  onChange={(date) => setDob(date)}
                  disallowFutureDate={true}
                />
              </Column>
            </Grid>

            <div className="searchButtons">
              <Button
                kind="primary"
                type="submit"
                disabled={isFormEmpty(values)}
              >
                <FormattedMessage id="label.button.search" />
              </Button>
              <Button
                kind="tertiary"
                type="button"
                onClick={() => handleSearch(values, false)}
                disabled={
                  isFormEmpty(values) ||
                  configurationProperties?.UseExternalPatientInfo === "false"
                }
              >
                <FormattedMessage id="label.button.externalsearch" />
              </Button>
            </div>
          </form>
        )}
      </Formik>

      {/* Search Results Table - hidden when patient is selected */}
      {searchResults.length > 0 && !selectedPatient && (
        <div className="patientSearchResults">
          <DataTable
            rows={searchResults}
            headers={patientSearchHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({
                  id: "patient.merge.patientResults",
                })}
              >
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableHeader />
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          <FormattedMessage
                            id={`patient.${header.key === "dob" ? "dob" : header.key === "subjectNumber" ? "subject.number" : header.key === "nationalId" ? "natioanalid" : header.key === "dataSourceName" ? "dataSourceName" : header.key === "firstName" ? "first.name" : header.key === "lastName" ? "last.name" : "gender"}`}
                            defaultMessage={header.header}
                          />
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows
                      .slice((page - 1) * pageSize, page * pageSize)
                      .map((row) => (
                        <TableRow key={row.id}>
                          <TableCell>
                            <RadioButton
                              name={`${panelId}-patient-select`}
                              id={`${panelId}-select-${row.id}`}
                              labelText=""
                              checked={selectedPatient?.patientID === row.id}
                              onClick={() => handlePatientSelect(row.id)}
                            />
                          </TableCell>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "dataSourceName" ? (
                                <Tag
                                  type={
                                    cell.value === "OpenElis" ? "red" : "green"
                                  }
                                >
                                  {cell.value}
                                </Tag>
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <Pagination
            page={page}
            pageSize={pageSize}
            pageSizes={[5, 10, 20, 50]}
            totalItems={searchResults.length}
            onChange={handlePageChange}
          />
        </div>
      )}

      {/* Show "Search for different patient" when patient is selected */}
      {selectedPatient && (
        <div className="changeSelectionButton">
          <Button
            kind="ghost"
            size="sm"
            onClick={() => {
              onPatientSelect(null);
              setSearchResults([]);
              setHasSearched(false);
              setDob("");
              if (formikRef.current) {
                formikRef.current.resetForm();
              }
            }}
          >
            <FormattedMessage id="patient.merge.searchDifferent" />
          </Button>
        </div>
      )}

      {/* Empty State - shown when search was performed but no results */}
      {hasSearched &&
        searchResults.length === 0 &&
        !loading &&
        !selectedPatient && (
          <Tile className="emptySearchResults">
            <div className="emptyStateContent">
              <Search size={48} />
              <p className="emptyStateTitle">
                <FormattedMessage id="patient.search.nopatient" />
              </p>
              <p className="emptyStateDescription">
                <FormattedMessage id="patient.merge.tryDifferentSearch" />
              </p>
            </div>
          </Tile>
        )}
    </div>
  );
}

export default PatientSearchPanel;
