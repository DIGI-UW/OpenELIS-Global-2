import React, { useState, useRef, useCallback, useContext } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Loading,
  TextInput,
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
} from "@carbon/react";
import { UserFollow, TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import CustomDatePicker from "../../common/CustomDatePicker";
import "../workflow/NotebookWorkflow.css";

/**
 * PatientOrderEntryPage - Page 1 of the MedLab workflow.
 * Simplified patient registration page that allows batch registration of multiple patients.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PatientOrderEntryPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Patient registration form state
  const [patientForm, setPatientForm] = useState({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "",
    nationalId: "",
  });

  // Registered patients in this session
  const [registeredPatients, setRegisteredPatients] = useState([]);

  // Loading state
  const [submitting, setSubmitting] = useState(false);

  // Form validation - nationalId is required by default system configuration
  const isFormValid = useCallback(() => {
    return (
      patientForm.firstName.trim() !== "" &&
      patientForm.lastName.trim() !== "" &&
      patientForm.dateOfBirth !== "" &&
      patientForm.gender !== "" &&
      patientForm.nationalId.trim() !== ""
    );
  }, [patientForm]);

  // Clear form
  const handleClearForm = useCallback(() => {
    setPatientForm({
      firstName: "",
      lastName: "",
      dateOfBirth: "",
      gender: "",
      nationalId: "",
    });
  }, []);

  // Clear registered patients list
  const handleClearList = useCallback(() => {
    setRegisteredPatients([]);
  }, []);

  // Register patient handler
  const handleRegisterPatient = useCallback(() => {
    if (!isFormValid()) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "medlab.patient.validation.error",
          defaultMessage: "Please fill in all required fields",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }

    setSubmitting(true);

    const patientData = {
      firstName: patientForm.firstName,
      lastName: patientForm.lastName,
      birthDateForDisplay: patientForm.dateOfBirth,
      gender: patientForm.gender,
      nationalId: patientForm.nationalId,
      patientUpdateStatus: "ADD",
    };

    postToOpenElisServer(
      "/rest/PatientManagement",
      JSON.stringify(patientData),
      (status, response) => {
        if (!componentMounted.current) return;

        setSubmitting(false);

        if (status === 200) {
          // Add to session list
          const newPatient = {
            id: response?.patientPK || `temp-${Date.now()}`,
            firstName: patientForm.firstName,
            lastName: patientForm.lastName,
            birthDateForDisplay: patientForm.dateOfBirth,
            gender: patientForm.gender,
            nationalId: patientForm.nationalId,
          };

          setRegisteredPatients((prev) => [...prev, newPatient]);

          // Clear form for next entry
          handleClearForm();

          // Show success notification
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.patient.created.success",
              defaultMessage: "Patient registered successfully",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);

          // Notify parent of progress update
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.patient.created.error",
              defaultMessage: "Error registering patient",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    patientForm,
    isFormValid,
    handleClearForm,
    intl,
    addNotification,
    setNotificationVisible,
    onProgressUpdate,
  ]);

  // Table headers for registered patients
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
      key: "birthDateForDisplay",
      header: intl.formatMessage({ id: "patient.dob" }),
    },
    {
      key: "gender",
      header: intl.formatMessage({ id: "patient.gender" }),
    },
    {
      key: "nationalId",
      header: intl.formatMessage({
        id: "patient.natioanalid",
        defaultMessage: "National ID",
      }),
    },
  ];

  return (
    <div className="patient-order-entry-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.patientRegistration.title"
            defaultMessage="Patient Registration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.patientRegistration.description"
            defaultMessage="Register patients for the medical lab workflow. Add multiple patients in a single session."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.patient.registeredThisSession"
                  defaultMessage="Registered This Session"
                />
              </span>
              <span className="progress-value">
                {registeredPatients.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Patient Registration Form */}
      <div
        className="patient-registration-form"
        style={{ marginBottom: "1.5rem" }}
      >
        <Grid fullWidth>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="patient-first-name"
              labelText={
                <>
                  {intl.formatMessage({ id: "patient.first.name" })}{" "}
                  <span className="requiredlabel">*</span>
                </>
              }
              value={patientForm.firstName}
              onChange={(e) =>
                setPatientForm((prev) => ({
                  ...prev,
                  firstName: e.target.value,
                }))
              }
              placeholder={intl.formatMessage({
                id: "patient.first.name.placeholder",
                defaultMessage: "Enter first name",
              })}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="patient-last-name"
              labelText={
                <>
                  {intl.formatMessage({ id: "patient.last.name" })}{" "}
                  <span className="requiredlabel">*</span>
                </>
              }
              value={patientForm.lastName}
              onChange={(e) =>
                setPatientForm((prev) => ({
                  ...prev,
                  lastName: e.target.value,
                }))
              }
              placeholder={intl.formatMessage({
                id: "patient.last.name.placeholder",
                defaultMessage: "Enter last name",
              })}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <CustomDatePicker
              id="patient-dob"
              labelText={
                <>
                  {intl.formatMessage({ id: "patient.dob" })}{" "}
                  <span className="requiredlabel">*</span>
                </>
              }
              value={patientForm.dateOfBirth}
              onChange={(date) =>
                setPatientForm((prev) => ({ ...prev, dateOfBirth: date }))
              }
              disallowFutureDate={true}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <RadioButtonGroup
              legendText={
                <>
                  {intl.formatMessage({ id: "patient.gender" })}{" "}
                  <span className="requiredlabel">*</span>
                </>
              }
              name="patient-gender"
              valueSelected={patientForm.gender}
              onChange={(value) =>
                setPatientForm((prev) => ({ ...prev, gender: value }))
              }
            >
              <RadioButton
                labelText={intl.formatMessage({ id: "patient.male" })}
                value="M"
                id="gender-male"
              />
              <RadioButton
                labelText={intl.formatMessage({ id: "patient.female" })}
                value="F"
                id="gender-female"
              />
            </RadioButtonGroup>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="patient-national-id"
              labelText={
                <>
                  {intl.formatMessage({
                    id: "patient.natioanalid",
                    defaultMessage: "National ID",
                  })}{" "}
                  <span className="requiredlabel">*</span>
                </>
              }
              value={patientForm.nationalId}
              onChange={(e) =>
                setPatientForm((prev) => ({
                  ...prev,
                  nationalId: e.target.value,
                }))
              }
              placeholder={intl.formatMessage({
                id: "patient.information.nationalid",
                defaultMessage: "Enter national ID",
              })}
            />
          </Column>
        </Grid>
      </div>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={UserFollow}
          onClick={handleRegisterPatient}
          disabled={submitting || !isFormValid()}
        >
          {submitting ? (
            <Loading small withOverlay={false} />
          ) : (
            <FormattedMessage
              id="medlab.patient.register"
              defaultMessage="Register Patient"
            />
          )}
        </Button>

        <Button kind="tertiary" size="sm" onClick={handleClearForm}>
          <FormattedMessage
            id="medlab.patient.clearForm"
            defaultMessage="Clear Form"
          />
        </Button>

        {registeredPatients.length > 0 && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={TrashCan}
            onClick={handleClearList}
          >
            <FormattedMessage
              id="medlab.patient.clearList"
              defaultMessage="Clear List"
            />
          </Button>
        )}
      </div>

      {/* Registered Patients Table */}
      {registeredPatients.length > 0 && (
        <div style={{ marginTop: "1.5rem" }}>
          <DataTable
            rows={registeredPatients}
            headers={patientHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer
                title={intl.formatMessage({
                  id: "medlab.patient.registeredTitle",
                  defaultMessage: "Registered Patients This Session",
                })}
                description={intl.formatMessage(
                  {
                    id: "medlab.patient.registeredCount",
                    defaultMessage: "{count} patient(s) registered",
                  },
                  { count: registeredPatients.length },
                )}
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
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "gender"
                              ? cell.value === "M"
                                ? intl.formatMessage({ id: "patient.male" })
                                : intl.formatMessage({ id: "patient.female" })
                              : cell.value || "-"}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </div>
      )}

      {/* Empty state - show helpful message when no patients registered */}
      {registeredPatients.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="medlab.patient.empty"
              defaultMessage="No patients registered yet. Fill in the form above and click 'Register Patient' to add patients."
            />
          </p>
        </div>
      )}
    </div>
  );
}

export default PatientOrderEntryPage;
