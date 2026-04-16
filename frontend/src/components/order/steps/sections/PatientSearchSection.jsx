import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  Button,
  TextInput,
  DatePicker,
  DatePickerInput,
  RadioButtonGroup,
  RadioButton,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Tag,
  Link,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import CreatePatientForm from "../../../patient/CreatePatientForm";

/**
 * PatientSearchSection - Patient search with results table and selection card
 *
 * Implements:
 * - ORD-2: Patient search (local + Client Registry)
 * - ORD-9: Selected patient summary card
 * - XC-2: Unified search pattern
 */

const PatientSearchSection = ({
  orderData,
  setOrderData,
  setPhoneValidation,
  isReadOnly,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Tab state
  const [activeTab, setActiveTab] = useState("search"); // "search" | "new"

  // Search fields
  const [searchFields, setSearchFields] = useState({
    patientId: "",
    previousLabNumber: "",
    lastName: "",
    firstName: "",
    dateOfBirth: "",
    gender: "",
  });

  // Results state
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [totalItems, setTotalItems] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);

  // Selected patient
  const [selectedPatient, setSelectedPatient] = useState(null);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Update selectedPatient when orderData.patientProperties changes (e.g., from barcode scan)
  useEffect(() => {
    if (orderData?.patientProperties?.patientPK) {
      setSelectedPatient(orderData.patientProperties);
      setActiveTab("search"); // Stay on search but show selected
    }
  }, [orderData?.patientProperties?.patientPK]);

  // Handle search field changes
  const handleFieldChange = (field, value) => {
    setSearchFields((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Execute search
  const handleSearch = () => {
    setIsSearching(true);
    setSearchResults([]);

    // Build search endpoint with proper parameter names (matching SearchPatientForm.js pattern exactly)
    const searchEndpoint =
      "/rest/patient-search-results?" +
      "lastName=" +
      (searchFields.lastName || "") +
      "&firstName=" +
      (searchFields.firstName || "") +
      "&STNumber=" +
      (searchFields.patientId || "") +
      "&subjectNumber=" +
      (searchFields.patientId || "") +
      "&nationalID=" +
      (searchFields.patientId || "") +
      "&labNumber=" +
      (searchFields.previousLabNumber || "") +
      "&guid=" +
      "&dateOfBirth=" +
      (searchFields.dateOfBirth || "") +
      "&gender=" +
      (searchFields.gender || "") +
      "&suppressExternalSearch=true";

    getFromOpenElisServer(searchEndpoint, (response) => {
      if (componentMounted.current) {
        setIsSearching(false);
        if (response?.patientSearchResults) {
          // Map results to ensure each has an 'id' field for DataTable
          const mappedResults = response.patientSearchResults.map((p) => ({
            ...p,
            id: p.patientID || p.id,
            dataSource: "Local",
          }));
          setSearchResults(mappedResults);
          setTotalItems(mappedResults.length);
        }
      }
    });
  };

  // External search (Client Registry)
  const handleExternalSearch = () => {
    setIsSearching(true);
    setSearchResults([]);

    const params = new URLSearchParams();
    if (searchFields.lastName) params.append("lastName", searchFields.lastName);
    if (searchFields.firstName)
      params.append("firstName", searchFields.firstName);
    if (searchFields.dateOfBirth)
      params.append("dateOfBirth", searchFields.dateOfBirth);
    if (searchFields.gender) params.append("gender", searchFields.gender);

    const searchEndpoint = `/rest/patient-search/external?${params.toString()}`;

    getFromOpenElisServer(searchEndpoint, (response) => {
      if (componentMounted.current) {
        setIsSearching(false);
        if (response?.patientSearchResults) {
          // Mark results as from external source and ensure 'id' field
          const externalResults = response.patientSearchResults.map((p) => ({
            ...p,
            id: p.patientID || p.id,
            dataSource: "Client Registry",
          }));
          setSearchResults(externalResults);
          setTotalItems(externalResults.length);
        }
      }
    });
  };

  // Clear search
  const handleClear = () => {
    setSearchFields({
      patientId: "",
      previousLabNumber: "",
      lastName: "",
      firstName: "",
      dateOfBirth: "",
      gender: "",
    });
    setSearchResults([]);
    setTotalItems(0);
  };

  // Select patient
  const handleSelectPatient = (patient) => {
    // Get patientID from either field
    const patientId = patient.patientID || patient.id;
    if (!patientId) {
      console.error("No patient ID found");
      return;
    }

    // Fetch full patient details
    getFromOpenElisServer(
      `/rest/patient-details?patientID=${patientId}`,
      (response) => {
        if (componentMounted.current && response) {
          setSelectedPatient(response);
          // IMPORTANT: patientUpdateStatus must be INSIDE patientProperties for backend to recognize it
          setOrderData((prev) => ({
            ...prev,
            patientUpdateStatus: "UPDATE",
            patientProperties: {
              ...response,
              patientUpdateStatus: "UPDATE", // Backend reads this from patientProperties
            },
          }));
        }
      },
    );
  };

  // Clear selection
  const handleClearSelection = () => {
    setSelectedPatient(null);
    setOrderData((prev) => ({
      ...prev,
      patientUpdateStatus: "",
      patientProperties: {
        patientPK: "",
        guid: "",
        firstName: "",
        lastName: "",
        birthDateForDisplay: "",
        gender: "",
        nationalId: "",
      },
    }));
  };

  // Handle new patient tab
  const handleNewPatient = () => {
    setActiveTab("new");
    // Clear any existing selection for new patient entry
  };

  // Table headers
  const headers = [
    {
      key: "lastName",
      header: intl.formatMessage({
        id: "patient.lastName",
        defaultMessage: "Last Name",
      }),
    },
    {
      key: "firstName",
      header: intl.formatMessage({
        id: "patient.firstName",
        defaultMessage: "First Name",
      }),
    },
    {
      key: "gender",
      header: intl.formatMessage({
        id: "patient.gender",
        defaultMessage: "Gender",
      }),
    },
    {
      key: "birthDateForDisplay",
      header: intl.formatMessage({
        id: "patient.dob",
        defaultMessage: "Date of Birth",
      }),
    },
    {
      key: "nationalId",
      header: intl.formatMessage({
        id: "patient.natId",
        defaultMessage: "Unique Health ID",
      }),
    },
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "patient.externalId",
        defaultMessage: "National ID",
      }),
    },
    {
      key: "dataSource",
      header: intl.formatMessage({
        id: "patient.dataSource",
        defaultMessage: "Data Source",
      }),
    },
    { key: "actions", header: "" },
  ];

  return (
    <Tile className="order-section patient-search-section">
      <h4 className="section-title">
        <FormattedMessage id="banner.menu.patient" defaultMessage="Patient" />
      </h4>

      {/* Tab Buttons */}
      <div className="section-tabs">
        <Button
          kind={activeTab === "search" ? "primary" : "tertiary"}
          size="md"
          onClick={() => setActiveTab("search")}
        >
          <FormattedMessage
            id="search.patient.label"
            defaultMessage="Search for Patient"
          />
        </Button>
        <Button
          kind={activeTab === "new" ? "primary" : "tertiary"}
          size="md"
          onClick={handleNewPatient}
          disabled={isReadOnly}
        >
          <FormattedMessage
            id="new.patient.label"
            defaultMessage="New Patient"
          />
        </Button>
      </div>

      {/* Search Tab Content */}
      {activeTab === "search" && (
        <div className="search-content">
          <Grid>
            {/* Search Fields Row 1 */}
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="patientId"
                labelText={intl.formatMessage({
                  id: "patient.id",
                  defaultMessage: "Patient Id",
                })}
                placeholder={intl.formatMessage({
                  id: "patient.id.placeholder",
                  defaultMessage: "Enter Patient Id",
                })}
                value={searchFields.patientId}
                onChange={(e) => handleFieldChange("patientId", e.target.value)}
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="previousLabNumber"
                labelText={intl.formatMessage({
                  id: "order.previousLabNumber",
                  defaultMessage: "Previous Lab Number",
                })}
                placeholder={intl.formatMessage({
                  id: "order.previousLabNumber.placeholder",
                  defaultMessage: "Enter Previous Lab Number",
                })}
                value={searchFields.previousLabNumber}
                onChange={(e) =>
                  handleFieldChange("previousLabNumber", e.target.value)
                }
                disabled={isReadOnly}
              />
            </Column>

            {/* Search Fields Row 2 */}
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="lastName"
                labelText={intl.formatMessage({
                  id: "patient.lastName",
                  defaultMessage: "Last Name",
                })}
                placeholder={intl.formatMessage({
                  id: "patient.lastName.placeholder",
                  defaultMessage: "Enter Patient's Last Name",
                })}
                value={searchFields.lastName}
                onChange={(e) => handleFieldChange("lastName", e.target.value)}
                disabled={isReadOnly}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="firstName"
                labelText={intl.formatMessage({
                  id: "patient.firstName",
                  defaultMessage: "First Name",
                })}
                placeholder={intl.formatMessage({
                  id: "patient.firstName.placeholder",
                  defaultMessage: "Enter Patient's First Name",
                })}
                value={searchFields.firstName}
                onChange={(e) => handleFieldChange("firstName", e.target.value)}
                disabled={isReadOnly}
              />
            </Column>

            {/* Search Fields Row 3 */}
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                dateFormat="d/m/Y"
                onChange={(dates) => {
                  if (dates && dates[0]) {
                    const date = dates[0];
                    const formatted = `${date.getDate().toString().padStart(2, "0")}/${(date.getMonth() + 1).toString().padStart(2, "0")}/${date.getFullYear()}`;
                    handleFieldChange("dateOfBirth", formatted);
                  }
                }}
              >
                <DatePickerInput
                  id="dateOfBirth"
                  labelText={intl.formatMessage({
                    id: "patient.dob",
                    defaultMessage: "Date of Birth",
                  })}
                  placeholder="dd/mm/yyyy"
                  disabled={isReadOnly}
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "patient.gender",
                  defaultMessage: "Gender",
                })}
                name="gender"
                valueSelected={searchFields.gender}
                onChange={(value) => handleFieldChange("gender", value)}
              >
                <RadioButton
                  id="gender-male"
                  labelText={intl.formatMessage({
                    id: "patient.male",
                    defaultMessage: "Male",
                  })}
                  value="M"
                  disabled={isReadOnly}
                />
                <RadioButton
                  id="gender-female"
                  labelText={intl.formatMessage({
                    id: "patient.female",
                    defaultMessage: "Female",
                  })}
                  value="F"
                  disabled={isReadOnly}
                />
              </RadioButtonGroup>
            </Column>

            {/* Search Buttons */}
            <Column lg={16} md={8} sm={4}>
              <div className="search-buttons">
                <Button
                  kind="primary"
                  size="md"
                  onClick={handleSearch}
                  disabled={isSearching || isReadOnly}
                >
                  <FormattedMessage
                    id="label.button.search"
                    defaultMessage="Search"
                  />
                </Button>
                <Button
                  kind="secondary"
                  size="md"
                  onClick={handleExternalSearch}
                  disabled={isSearching || isReadOnly}
                >
                  <FormattedMessage
                    id="patient.externalSearch"
                    defaultMessage="External Search"
                  />
                </Button>
                <Button kind="ghost" size="md" onClick={handleClear}>
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Button>
              </div>
            </Column>
          </Grid>

          {/* Selected Patient Card */}
          {selectedPatient && (
            <div className="selected-entity-card">
              <div className="selected-card-header">
                <Tag type="green" size="sm">
                  <FormattedMessage id="selected" defaultMessage="Selected" />
                </Tag>
                <Link onClick={handleClearSelection}>
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Link>
              </div>
              <div className="selected-card-content">
                <h5>
                  {selectedPatient.firstName} {selectedPatient.lastName}
                </h5>
                <p>
                  {selectedPatient.birthDateForDisplay &&
                    `DOB: ${selectedPatient.birthDateForDisplay}`}
                  {selectedPatient.gender && ` · ${selectedPatient.gender}`}
                  {selectedPatient.nationalId &&
                    ` · ID: ${selectedPatient.nationalId}`}
                </p>
              </div>
            </div>
          )}

          {/* Results Table */}
          {searchResults.length > 0 && !selectedPatient && (
            <div className="search-results">
              <h5 className="results-title">
                <FormattedMessage
                  id="patient.results"
                  defaultMessage="Patient Results"
                />
              </h5>
              <DataTable rows={searchResults} headers={headers} isSortable>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
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
                      {rows.map((row) => {
                        const patient = searchResults.find(
                          (p) => p.patientID === row.id || p.id === row.id,
                        );
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "actions") {
                                return (
                                  <TableCell key={cell.id}>
                                    <Button
                                      kind="primary"
                                      size="sm"
                                      onClick={() =>
                                        handleSelectPatient(patient)
                                      }
                                    >
                                      <FormattedMessage
                                        id="label.button.select"
                                        defaultMessage="Select"
                                      />
                                    </Button>
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "dataSource") {
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value || "Local"}
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
              <Pagination
                totalItems={totalItems}
                backwardText={intl.formatMessage({
                  id: "pagination.previous",
                  defaultMessage: "Previous page",
                })}
                forwardText={intl.formatMessage({
                  id: "pagination.next",
                  defaultMessage: "Next page",
                })}
                pageSize={pageSize}
                pageSizes={[25, 50, 100]}
                itemsPerPageText={intl.formatMessage({
                  id: "pagination.itemsPerPage",
                  defaultMessage: "Items per page:",
                })}
                onChange={({ page, pageSize: newPageSize }) => {
                  setCurrentPage(page);
                  setPageSize(newPageSize);
                }}
              />
            </div>
          )}

          {/* No Results */}
          {searchResults.length === 0 && !isSearching && !selectedPatient && (
            <div className="no-results">
              <p>
                <FormattedMessage
                  id="patient.noResults"
                  defaultMessage="0-0 of 0 items"
                />
              </p>
            </div>
          )}
        </div>
      )}

      {/* New Patient Tab Content */}
      {activeTab === "new" && (
        <div className="new-patient-content">
          <CreatePatientForm
            showActionsButton={false}
            selectedPatient={
              selectedPatient || {
                id: "",
                healthRegion: [],
                nationalId: "",
                subjectNumber: "",
              }
            }
            orderFormValues={orderData}
            setOrderFormValues={setOrderData}
            error={() => null}
            setPhoneValidation={setPhoneValidation}
          />
        </div>
      )}
    </Tile>
  );
};

export default PatientSearchSection;
