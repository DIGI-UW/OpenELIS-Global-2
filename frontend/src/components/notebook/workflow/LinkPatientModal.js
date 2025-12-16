import React, { useState, useCallback, useContext } from "react";
import {
  Modal,
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
  Loading,
  InlineNotification,
  Tag,
  Tile,
} from "@carbon/react";
import { Search, Link as LinkIcon } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { NotificationContext, ConfigurationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import CustomDatePicker from "../../common/CustomDatePicker";

/**
 * LinkPatientModal - Modal component for searching and linking patients to samples.
 * Used in sample collection workflow to associate samples with existing patients.
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback when modal is closed
 * @param {Array} props.selectedSampleIds - Array of selected sample IDs to link
 * @param {function} props.onLinkPatient - Callback when patient is linked (receives patientId, patientInfo)
 */
function LinkPatientModal({
  open,
  onClose,
  selectedSampleIds = [],
  onLinkPatient,
}) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  // Search form state
  const [searchForm, setSearchForm] = useState({
    patientId: "",
    lastName: "",
    firstName: "",
    dateOfBirth: "",
    gender: "",
  });

  // Search results state
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  // Selected patient state
  const [selectedPatient, setSelectedPatient] = useState(null);

  // Validation for name inputs
  const [prevFirstName, setPrevFirstName] = useState("");
  const [prevLastName, setPrevLastName] = useState("");

  // Clear form and results
  const handleClearSearch = useCallback(() => {
    setSearchForm({
      patientId: "",
      lastName: "",
      firstName: "",
      dateOfBirth: "",
      gender: "",
    });
    setSearchResults([]);
    setSelectedPatient(null);
    setHasSearched(false);
  }, []);

  // Handle modal close
  const handleClose = useCallback(() => {
    handleClearSearch();
    onClose();
  }, [handleClearSearch, onClose]);

  // Handle name validation
  const handleFirstNameChange = useCallback(
    (e) => {
      if (configurationProperties?.FIRST_NAME_REGEX) {
        const regex = new RegExp(
          configurationProperties.FIRST_NAME_REGEX,
          "iu",
        );
        if (!regex.test(e.target.value)) {
          e.target.value = prevFirstName;
        }
      }
      setPrevFirstName(e.target.value);
      setSearchForm((prev) => ({ ...prev, firstName: e.target.value }));
    },
    [configurationProperties, prevFirstName],
  );

  const handleLastNameChange = useCallback(
    (e) => {
      if (configurationProperties?.LAST_NAME_REGEX) {
        const regex = new RegExp(configurationProperties.LAST_NAME_REGEX, "iu");
        if (!regex.test(e.target.value)) {
          e.target.value = prevLastName;
        }
      }
      setPrevLastName(e.target.value);
      setSearchForm((prev) => ({ ...prev, lastName: e.target.value }));
    },
    [configurationProperties, prevLastName],
  );

  // Perform patient search
  const handleSearch = useCallback(() => {
    // Validate that at least one field is filled
    const hasSearchCriteria =
      searchForm.patientId.trim() ||
      searchForm.lastName.trim() ||
      searchForm.firstName.trim() ||
      searchForm.dateOfBirth ||
      searchForm.gender;

    if (!hasSearchCriteria) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "medlab.linkPatient.enterCriteria",
          defaultMessage: "Please enter at least one search criterion",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }

    setSearching(true);
    setSelectedPatient(null);

    const searchParams = new URLSearchParams({
      lastName: searchForm.lastName,
      firstName: searchForm.firstName,
      STNumber: searchForm.patientId,
      subjectNumber: searchForm.patientId,
      nationalID: searchForm.patientId,
      labNumber: "",
      guid: "",
      dateOfBirth: searchForm.dateOfBirth,
      gender: searchForm.gender,
      suppressExternalSearch: "true",
    });

    getFromOpenElisServer(
      `/rest/patient-search-results?${searchParams.toString()}`,
      (response) => {
        setSearching(false);
        setHasSearched(true);

        if (response?.patientSearchResults?.length > 0) {
          // Add id field for DataTable
          const results = response.patientSearchResults.map((patient) => ({
            ...patient,
            id: patient.patientID,
          }));
          setSearchResults(results);
        } else {
          setSearchResults([]);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "patient.search.nopatient",
              defaultMessage: "No patients found matching the search criteria",
            }),
            kind: NotificationKinds.info,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [searchForm, intl, addNotification, setNotificationVisible]);

  // Handle patient selection
  const handlePatientSelect = useCallback((patientId) => {
    setSelectedPatient(patientId);
  }, []);

  // Handle link confirmation
  const handleConfirmLink = useCallback(() => {
    if (!selectedPatient) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "medlab.linkPatient.selectPatient",
          defaultMessage: "Please select a patient to link",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }

    const patientInfo = searchResults.find(
      (p) => p.patientID === selectedPatient,
    );
    if (onLinkPatient) {
      onLinkPatient(selectedPatient, patientInfo);
    }
    handleClose();
  }, [
    selectedPatient,
    searchResults,
    onLinkPatient,
    handleClose,
    intl,
    addNotification,
    setNotificationVisible,
  ]);

  // Table headers for search results
  const patientHeaders = [
    {
      key: "lastName",
      header: intl.formatMessage({ id: "patient.last.name" }),
    },
    {
      key: "firstName",
      header: intl.formatMessage({ id: "patient.first.name" }),
    },
    {
      key: "gender",
      header: intl.formatMessage({ id: "patient.gender" }),
    },
    {
      key: "birthdate",
      header: intl.formatMessage({ id: "patient.dob" }),
    },
    {
      key: "nationalId",
      header: intl.formatMessage({ id: "patient.natioanalid" }),
    },
  ];

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "medlab.linkPatient.title",
        defaultMessage: "Link Samples to Patient",
      })}
      primaryButtonText={intl.formatMessage({
        id: "medlab.linkPatient.confirm",
        defaultMessage: "Link to Patient",
      })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      onRequestSubmit={handleConfirmLink}
      primaryButtonDisabled={!selectedPatient}
      size="lg"
    >
      {/* Selected samples info */}
      <Tile className="order-info-tile" style={{ marginBottom: "1rem" }}>
        <Tag type="blue">
          <FormattedMessage
            id="medlab.linkPatient.samplesSelected"
            defaultMessage="{count} sample(s) selected"
            values={{ count: selectedSampleIds.length }}
          />
        </Tag>
        <p
          style={{
            marginTop: "0.5rem",
            color: "#525252",
            fontSize: "0.875rem",
          }}
        >
          <FormattedMessage
            id="medlab.linkPatient.description"
            defaultMessage="Search for an existing patient and link the selected samples to them."
          />
        </p>
      </Tile>

      {/* Search Form */}
      <div style={{ marginBottom: "1.5rem" }}>
        <h5 style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="medlab.linkPatient.searchTitle"
            defaultMessage="Search Patient"
          />
        </h5>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="link-patient-id"
              labelText={intl.formatMessage({
                id: "patient.id",
                defaultMessage: "Patient ID / National ID / Subject Number",
              })}
              value={searchForm.patientId}
              onChange={(e) =>
                setSearchForm((prev) => ({
                  ...prev,
                  patientId: e.target.value,
                }))
              }
              placeholder={intl.formatMessage({
                id: "input.placeholder.patientId",
                defaultMessage: "Enter patient ID",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div style={{ visibility: "hidden", height: "100%" }} />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="link-patient-lastname"
              labelText={intl.formatMessage({ id: "patient.last.name" })}
              value={searchForm.lastName}
              onChange={handleLastNameChange}
              placeholder={intl.formatMessage({
                id: "input.placeholder.patientLastName",
                defaultMessage: "Enter last name",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="link-patient-firstname"
              labelText={intl.formatMessage({ id: "patient.first.name" })}
              value={searchForm.firstName}
              onChange={handleFirstNameChange}
              placeholder={intl.formatMessage({
                id: "input.placeholder.patientFirstName",
                defaultMessage: "Enter first name",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="link-patient-dob"
              labelText={intl.formatMessage({ id: "patient.dob" })}
              value={searchForm.dateOfBirth}
              onChange={(date) =>
                setSearchForm((prev) => ({ ...prev, dateOfBirth: date }))
              }
              disallowFutureDate={true}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({ id: "patient.gender" })}
              name="link-patient-gender"
              valueSelected={searchForm.gender}
              onChange={(value) =>
                setSearchForm((prev) => ({ ...prev, gender: value }))
              }
            >
              <RadioButton
                labelText={intl.formatMessage({ id: "patient.male" })}
                value="M"
                id="link-gender-male"
              />
              <RadioButton
                labelText={intl.formatMessage({ id: "patient.female" })}
                value="F"
                id="link-gender-female"
              />
            </RadioButtonGroup>
          </Column>
        </Grid>
        <div style={{ marginTop: "1rem", display: "flex", gap: "0.5rem" }}>
          <Button
            kind="primary"
            size="sm"
            renderIcon={Search}
            onClick={handleSearch}
            disabled={searching}
          >
            {searching ? (
              <Loading small withOverlay={false} />
            ) : (
              <FormattedMessage id="label.button.search" />
            )}
          </Button>
          <Button kind="tertiary" size="sm" onClick={handleClearSearch}>
            <FormattedMessage
              id="medlab.linkPatient.clearSearch"
              defaultMessage="Clear"
            />
          </Button>
        </div>
      </div>

      {/* Loading state */}
      {searching && <Loading />}

      {/* Search Results */}
      {!searching && hasSearched && (
        <div>
          <h5 style={{ marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="medlab.linkPatient.resultsTitle"
              defaultMessage="Search Results"
            />
          </h5>

          {searchResults.length === 0 ? (
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "patient.search.nopatient",
                defaultMessage: "No patients found",
              })}
              subtitle={intl.formatMessage({
                id: "medlab.linkPatient.tryAgain",
                defaultMessage: "Try adjusting your search criteria",
              })}
              hideCloseButton
              lowContrast
            />
          ) : (
            <DataTable rows={searchResults} headers={patientHeaders} isSortable>
              {({ rows, headers, getHeaderProps, getTableProps }) => (
                <TableContainer>
                  <Table {...getTableProps()} size="sm">
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
                        const patient = searchResults.find(
                          (p) => p.patientID === row.id,
                        );
                        const isSelected = selectedPatient === row.id;
                        return (
                          <TableRow
                            key={row.id}
                            onClick={() => handlePatientSelect(row.id)}
                            style={{
                              cursor: "pointer",
                              backgroundColor: isSelected
                                ? "#e0f0ff"
                                : "transparent",
                            }}
                          >
                            <TableCell>
                              <RadioButton
                                name="patient-selection"
                                labelText=""
                                id={`select-patient-${row.id}`}
                                checked={isSelected}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handlePatientSelect(row.id);
                                }}
                              />
                            </TableCell>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "gender"
                                  ? cell.value === "M"
                                    ? intl.formatMessage({ id: "patient.male" })
                                    : cell.value === "F"
                                      ? intl.formatMessage({
                                          id: "patient.female",
                                        })
                                      : cell.value || "-"
                                  : cell.value || "-"}
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
          )}

          {/* Selected patient confirmation */}
          {selectedPatient && (
            <Tile
              style={{
                marginTop: "1rem",
                backgroundColor: "#e0f0ff",
                padding: "1rem",
              }}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <LinkIcon size={20} />
                <span>
                  <FormattedMessage
                    id="medlab.linkPatient.selectedConfirm"
                    defaultMessage="Selected patient: {name}"
                    values={{
                      name: (() => {
                        const p = searchResults.find(
                          (pt) => pt.patientID === selectedPatient,
                        );
                        return p
                          ? `${p.firstName || ""} ${p.lastName || ""}`.trim()
                          : selectedPatient;
                      })(),
                    }}
                  />
                </span>
              </div>
            </Tile>
          )}
        </div>
      )}
    </Modal>
  );
}

export default LinkPatientModal;
