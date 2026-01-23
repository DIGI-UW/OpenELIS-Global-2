import React, {
  useState,
  useRef,
  useCallback,
  useContext,
  useEffect,
} from "react";
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
  Checkbox,
  Search,
  Tag,
  InlineNotification,
  Tabs,
  Tab,
  TabList,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import {
  UserFollow,
  TrashCan,
  Add,
  Search as SearchIcon,
  ShoppingCart,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import CustomDatePicker from "../../common/CustomDatePicker";
import BulkOrderModal from "../workflow/BulkOrderModal";
import "../workflow/NotebookWorkflow.css";

// Helper to format date as MM/dd/yyyy (backend expected format)
const formatDateForBackend = (date = new Date()) => {
  const d = new Date(date);
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const year = d.getFullYear();
  return `${month}/${day}/${year}`;
};

/**
 * PatientOrderEntryPage - Page 1 of the MedLab workflow.
 * Patient registration AND lab order entry with test requirements display (FR-006, FR-007).
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

  // Bulk order selection state
  const [selectedPatientsForBulk, setSelectedPatientsForBulk] = useState([]);
  const [bulkOrderModalOpen, setBulkOrderModalOpen] = useState(false);

  // Loading state
  const [submitting, setSubmitting] = useState(false);

  // ========== NEW: Lab Order State (FR-006, FR-007) ==========
  const [activeTab, setActiveTab] = useState(0);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [patientSearchQuery, setPatientSearchQuery] = useState("");
  const [patientSearchResults, setPatientSearchResults] = useState([]);
  const [searchingPatients, setSearchingPatients] = useState(false);

  // Test selection and requirements
  const [availableTests, setAvailableTests] = useState([]);
  const [selectedTests, setSelectedTests] = useState([]);
  const [testRequirements, setTestRequirements] = useState([]);
  const [loadingTests, setLoadingTests] = useState(false);
  const [loadingRequirements, setLoadingRequirements] = useState(false);

  // Lab order form
  const [labOrderForm, setLabOrderForm] = useState({
    labNo: "",
    requestDate: formatDateForBackend(),
    receivedDate: formatDateForBackend(),
    priority: "ROUTINE",
  });
  const [createdOrders, setCreatedOrders] = useState([]);

  // Load existing orders for this notebook page (persisted across refresh)
  // Note: getOrdersForPage filters by pageId, so this only returns orders for THIS page (Stage 1),
  // NOT manifest-imported samples from other pages (Stage 2)
  const loadOrdersForPage = useCallback(() => {
    if (!pageData?.id) return;
    getFromOpenElisServer(
      `/rest/medlab/page/${pageData.id}/orders`,
      (response) => {
        if (componentMounted.current && response) {
          const orders = Array.isArray(response) ? response : [];
          // Transform backend order data to match frontend display format
          const formattedOrders = orders.map((order) => ({
            id: order.id,
            labNo: order.labNo,
            patientId: order.patientId,
            patientName: order.patientName || "Unknown",
            testCount: order.testCount || 0,
            createdAt: order.receivedDate
              ? new Date(order.receivedDate).toLocaleTimeString()
              : "",
            status: order.status || "PENDING",
          }));
          setCreatedOrders(formattedOrders);
        }
      },
    );
  }, [pageData?.id]);

  // Load available tests and existing orders on mount
  useEffect(() => {
    if (componentMounted.current) {
      loadAvailableTests();
      loadOrdersForPage();
    }
    return () => {
      componentMounted.current = false;
    };
  }, [loadOrdersForPage]);

  // Load test requirements when tests are selected
  useEffect(() => {
    if (selectedTests.length > 0) {
      loadTestRequirements(selectedTests);
    } else {
      setTestRequirements([]);
    }
  }, [selectedTests]);

  // Load available tests from API
  const loadAvailableTests = useCallback(() => {
    setLoadingTests(true);
    getFromOpenElisServer("/rest/test-list", (response) => {
      if (componentMounted.current && response) {
        setAvailableTests(Array.isArray(response) ? response : []);
      }
      setLoadingTests(false);
    });
  }, []);

  // Load test requirements for selected tests (FR-007)
  const loadTestRequirements = useCallback((testIds) => {
    if (testIds.length === 0) return;
    setLoadingRequirements(true);
    const idsParam = testIds.join(",");
    getFromOpenElisServer(
      `/rest/medlab/test-requirements?testIds=${idsParam}`,
      (response) => {
        if (componentMounted.current && response) {
          setTestRequirements(Array.isArray(response) ? response : []);
        }
        setLoadingRequirements(false);
      },
    );
  }, []);

  // Patient search handler - searches by name (lastName OR firstName)
  const handlePatientSearch = useCallback(() => {
    if (!patientSearchQuery.trim()) return;
    setSearchingPatients(true);
    const query = encodeURIComponent(patientSearchQuery.trim());
    // Search by lastName first, if no results then try firstName
    const searchEndPoint =
      `/rest/patient-search-results?` +
      `lastName=${query}` +
      `&firstName=` +
      `&STNumber=` +
      `&subjectNumber=` +
      `&nationalID=` +
      `&labNumber=` +
      `&guid=` +
      `&dateOfBirth=` +
      `&gender=` +
      `&suppressExternalSearch=true`;
    getFromOpenElisServer(searchEndPoint, (response) => {
      if (componentMounted.current) {
        const results = response?.patientSearchResults || [];
        if (results.length === 0) {
          // No results by lastName, try firstName
          const firstNameEndPoint =
            `/rest/patient-search-results?` +
            `lastName=` +
            `&firstName=${query}` +
            `&STNumber=` +
            `&subjectNumber=` +
            `&nationalID=` +
            `&labNumber=` +
            `&guid=` +
            `&dateOfBirth=` +
            `&gender=` +
            `&suppressExternalSearch=true`;
          getFromOpenElisServer(firstNameEndPoint, (firstNameResponse) => {
            if (componentMounted.current) {
              setPatientSearchResults(
                firstNameResponse?.patientSearchResults || [],
              );
            }
            setSearchingPatients(false);
          });
        } else {
          setPatientSearchResults(results);
          setSearchingPatients(false);
        }
      } else {
        setSearchingPatients(false);
      }
    });
  }, [patientSearchQuery]);

  // Select a patient from search results
  const handleSelectPatient = useCallback((patient) => {
    setSelectedPatient(patient);
    setPatientSearchResults([]);
    setPatientSearchQuery("");
  }, []);

  // Toggle test selection
  const handleTestToggle = useCallback((testId) => {
    setSelectedTests((prev) => {
      if (prev.includes(testId)) {
        return prev.filter((id) => id !== testId);
      } else {
        return [...prev, testId];
      }
    });
  }, []);

  // Create lab order handler
  const handleCreateLabOrder = useCallback(() => {
    if (!selectedPatient) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "medlab.order.noPatient",
          defaultMessage: "Please select a patient first",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }

    if (selectedTests.length === 0) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "medlab.order.noTests",
          defaultMessage: "Please select at least one test",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }

    if (!labOrderForm.labNo.trim()) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "medlab.order.noLabNo",
          defaultMessage: "Please enter a lab number",
        }),
        kind: NotificationKinds.warning,
      });
      setNotificationVisible(true);
      return;
    }

    setSubmitting(true);

    const orderData = {
      patientId: selectedPatient.patientID,
      labNo: labOrderForm.labNo,
      requestDate: labOrderForm.requestDate,
      receivedDate: labOrderForm.receivedDate,
      priority: labOrderForm.priority,
      testIds: selectedTests,
      notebookEntryId: entryId,
      notebookPageId: pageData?.id,
    };

    // Use postToOpenElisServerJsonResponse to properly parse JSON response body
    postToOpenElisServerJsonResponse(
      "/rest/medlab/patient-order",
      JSON.stringify(orderData),
      (response) => {
        if (!componentMounted.current) return;
        setSubmitting(false);

        // Check for success - postToOpenElisServerJsonResponse returns parsed JSON
        // HTTP errors have numeric statusCode (400, 500, etc.), successful responses have string status ("PENDING")
        const isHttpError = typeof response?.statusCode === "number";
        if (response?.success && !isHttpError) {
          // Add to created orders list (response now returns orderId instead of sampleId)
          setCreatedOrders((prev) => [
            ...prev,
            {
              id: response.orderId || `order-${Date.now()}`,
              labNo: labOrderForm.labNo,
              patientId: selectedPatient.patientID,
              patientName: `${selectedPatient.lastName}, ${selectedPatient.firstName}`,
              testCount: selectedTests.length,
              status: response.status || "PENDING_COLLECTION",
              createdAt: new Date().toLocaleTimeString(),
            },
          ]);

          // Reset form
          setSelectedPatient(null);
          setSelectedTests([]);
          setTestRequirements([]);
          setLabOrderForm({
            labNo: "",
            requestDate: formatDateForBackend(),
            receivedDate: formatDateForBackend(),
            priority: "ROUTINE",
          });

          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.order.created.success",
              defaultMessage: "Lab order created successfully",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);

          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              response?.error ||
              intl.formatMessage({
                id: "medlab.order.created.error",
                defaultMessage: "Error creating lab order",
              }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedPatient,
    selectedTests,
    labOrderForm,
    entryId,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    onProgressUpdate,
  ]);

  // Form validation - nationalId is required by default system configuration
  const isFormValid = useCallback(() => {
    return (
      patientForm.firstName.trim() !== "" &&
      patientForm.lastName.trim() !== "" &&
      patientForm.dateOfBirth !== "" &&
      patientForm.gender !== ""
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
    setSelectedPatientsForBulk([]);
  }, []);

  // ========== Bulk Order Selection Handlers ==========
  const handleTogglePatientSelection = useCallback((patient) => {
    setSelectedPatientsForBulk((prev) => {
      const patientId = patient.id || patient.patientID;
      const isSelected = prev.some((p) => (p.id || p.patientID) === patientId);
      if (isSelected) {
        return prev.filter((p) => (p.id || p.patientID) !== patientId);
      } else {
        return [...prev, patient];
      }
    });
  }, []);

  const handleSelectAllPatients = useCallback(() => {
    if (selectedPatientsForBulk.length === registeredPatients.length) {
      setSelectedPatientsForBulk([]);
    } else {
      setSelectedPatientsForBulk([...registeredPatients]);
    }
  }, [registeredPatients, selectedPatientsForBulk.length]);

  const isPatientSelected = useCallback(
    (patient) => {
      const patientId = patient.id || patient.patientID;
      return selectedPatientsForBulk.some(
        (p) => (p.id || p.patientID) === patientId,
      );
    },
    [selectedPatientsForBulk],
  );

  const handleBulkOrderSuccess = useCallback(
    (orders) => {
      // Add the newly created orders to the list with timestamps
      if (orders && orders.length > 0) {
        const currentTime = new Date().toLocaleTimeString();
        const ordersWithTimestamp = orders.map((order) => ({
          ...order,
          createdAt: order.createdAt || currentTime,
        }));
        setCreatedOrders((prev) => [...prev, ...ordersWithTimestamp]);
      }
      // Clear selection after successful bulk order
      setSelectedPatientsForBulk([]);
      // Reload orders from server to ensure consistency
      loadOrdersForPage();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    },
    [loadOrdersForPage, onProgressUpdate],
  );

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

    postToOpenElisServerJsonResponse(
      "/rest/PatientManagement",
      JSON.stringify(patientData),
      (response) => {
        if (!componentMounted.current) return;

        setSubmitting(false);

        if (response?.success && response?.patientPK) {
          // Add to session list with the real patient ID from the backend
          const newPatient = {
            id: response.patientPK,
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
          // Show error - either request failed or patient ID was not returned
          const errorMessage =
            response?.error ||
            intl.formatMessage({
              id: "medlab.patient.created.error",
              defaultMessage: "Error registering patient",
            });
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: errorMessage,
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

  // Helper to get order count for a patient (matches by patient ID)
  const getPatientOrderCount = useCallback(
    (patient) => {
      const patientId = patient.id || patient.patientID;
      if (!patientId) return 0;
      return createdOrders.filter(
        (order) => String(order.patientId) === String(patientId),
      ).length;
    },
    [createdOrders],
  );

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
    {
      key: "orders",
      header: intl.formatMessage({
        id: "medlab.patient.orders",
        defaultMessage: "Orders",
      }),
    },
  ];

  return (
    <div className="patient-order-entry-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.patientOrderEntry.title"
            defaultMessage="Patient & Lab Order"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.patientOrderEntry.description"
            defaultMessage="Register patient (with inline search), create lab order. Orders drive all downstream sample collection, processing, and testing activities."
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
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.order.createdThisSession"
                  defaultMessage="Orders Created"
                />
              </span>
              <span className="progress-value">{createdOrders.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Tabs for Patient Registration vs Lab Order Entry */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Patient and Order Entry Tabs">
          <Tab>
            <FormattedMessage
              id="medlab.tab.patientRegistration"
              defaultMessage="Register New Patient"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="medlab.tab.labOrderEntry"
              defaultMessage="Create Lab Order"
            />
          </Tab>
        </TabList>
        <TabPanels>
          {/* Tab 1: Patient Registration (existing functionality) */}
          <TabPanel>
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
                    updateStateValue={true}
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
                        })}
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
                {/* Bulk Order Action Bar */}
                <div className="patient-selection-actions">
                  <Checkbox
                    id="select-all-patients"
                    labelText={intl.formatMessage({
                      id: "medlab.patient.selectAll",
                      defaultMessage: "Select All",
                    })}
                    checked={
                      registeredPatients.length > 0 &&
                      selectedPatientsForBulk.length ===
                        registeredPatients.length
                    }
                    indeterminate={
                      selectedPatientsForBulk.length > 0 &&
                      selectedPatientsForBulk.length < registeredPatients.length
                    }
                    onChange={handleSelectAllPatients}
                  />
                  <span className="patient-selection-count">
                    {selectedPatientsForBulk.length > 0 && (
                      <FormattedMessage
                        id="medlab.patient.selectedCount"
                        defaultMessage="{count} selected"
                        values={{ count: selectedPatientsForBulk.length }}
                      />
                    )}
                  </span>
                  <Button
                    kind="primary"
                    size="sm"
                    renderIcon={ShoppingCart}
                    disabled={selectedPatientsForBulk.length === 0}
                    onClick={() => setBulkOrderModalOpen(true)}
                  >
                    <FormattedMessage
                      id="medlab.patient.createBulkOrders"
                      defaultMessage="Create Orders for Selected ({count})"
                      values={{ count: selectedPatientsForBulk.length }}
                    />
                  </Button>
                </div>

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
                            <TableHeader style={{ width: "50px" }}>
                              {/* Checkbox column header - empty */}
                            </TableHeader>
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
                            const patient = registeredPatients.find(
                              (p) => p.id === row.id,
                            );
                            return (
                              <TableRow key={row.id}>
                                <TableCell>
                                  <Checkbox
                                    id={`select-patient-${row.id}`}
                                    labelText=""
                                    hideLabel
                                    checked={
                                      patient
                                        ? isPatientSelected(patient)
                                        : false
                                    }
                                    onChange={() =>
                                      patient &&
                                      handleTogglePatientSelection(patient)
                                    }
                                  />
                                </TableCell>
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.info.header === "gender"
                                      ? cell.value === "M"
                                        ? intl.formatMessage({
                                            id: "patient.male",
                                          })
                                        : intl.formatMessage({
                                            id: "patient.female",
                                          })
                                      : cell.info.header === "orders"
                                        ? (() => {
                                            const orderCount = patient
                                              ? getPatientOrderCount(patient)
                                              : 0;
                                            return orderCount > 0 ? (
                                              <Tag type="green" size="sm">
                                                {orderCount}
                                              </Tag>
                                            ) : (
                                              <Tag type="gray" size="sm">
                                                0
                                              </Tag>
                                            );
                                          })()
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
          </TabPanel>

          {/* Tab 2: Lab Order Entry (FR-006, FR-007) */}
          <TabPanel>
            {/* Patient Search Section */}
            <div
              className="lab-order-section"
              style={{ marginBottom: "1.5rem" }}
            >
              <h5>
                <FormattedMessage
                  id="medlab.order.selectPatient"
                  defaultMessage="1. Select Patient"
                />
              </h5>
              <Grid fullWidth>
                <Column lg={8} md={6} sm={4}>
                  <Search
                    id="patient-search"
                    labelText={intl.formatMessage({
                      id: "medlab.order.searchPatient",
                      defaultMessage: "Search patient by name",
                    })}
                    placeholder={intl.formatMessage({
                      id: "medlab.order.searchPlaceholder",
                      defaultMessage: "Enter patient name...",
                    })}
                    value={patientSearchQuery}
                    onChange={(e) => setPatientSearchQuery(e.target.value)}
                    onKeyDown={(e) =>
                      e.key === "Enter" && handlePatientSearch()
                    }
                  />
                </Column>
                <Column lg={4} md={2} sm={4}>
                  <Button
                    kind="secondary"
                    size="md"
                    renderIcon={SearchIcon}
                    onClick={handlePatientSearch}
                    disabled={searchingPatients || !patientSearchQuery.trim()}
                    style={{ marginTop: "1.5rem" }}
                  >
                    {searchingPatients ? (
                      <Loading small withOverlay={false} />
                    ) : (
                      <FormattedMessage
                        id="button.label.search"
                        defaultMessage="Search"
                      />
                    )}
                  </Button>
                </Column>
              </Grid>

              {/* Patient Search Results */}
              {patientSearchResults.length > 0 && (
                <div style={{ marginTop: "1rem" }}>
                  <p>
                    <FormattedMessage
                      id="medlab.order.selectFromResults"
                      defaultMessage="Select a patient from results:"
                    />
                  </p>
                  {patientSearchResults.map((patient) => (
                    <Tile
                      key={patient.patientID}
                      className="patient-result-tile"
                      style={{
                        cursor: "pointer",
                        marginBottom: "0.5rem",
                        padding: "0.75rem",
                      }}
                      onClick={() => handleSelectPatient(patient)}
                    >
                      <strong>
                        {patient.lastName}, {patient.firstName}
                      </strong>{" "}
                      - {patient.birthDateForDisplay} (
                      {patient.gender === "M"
                        ? intl.formatMessage({ id: "patient.male" })
                        : intl.formatMessage({ id: "patient.female" })}
                      )
                    </Tile>
                  ))}
                </div>
              )}

              {/* Selected Patient Display */}
              {selectedPatient && (
                <InlineNotification
                  kind="info"
                  title={intl.formatMessage({
                    id: "medlab.order.selectedPatient",
                    defaultMessage: "Selected Patient",
                  })}
                  subtitle={`${selectedPatient.lastName}, ${selectedPatient.firstName} - ${selectedPatient.birthDateForDisplay}`}
                  hideCloseButton={false}
                  onCloseButtonClick={() => setSelectedPatient(null)}
                  style={{ marginTop: "1rem" }}
                />
              )}
            </div>

            {/* Test Selection Section (FR-007) */}
            <div
              className="lab-order-section"
              style={{ marginBottom: "1.5rem" }}
            >
              <h5>
                <FormattedMessage
                  id="medlab.order.selectTests"
                  defaultMessage="2. Select Tests"
                />
              </h5>
              {loadingTests ? (
                <Loading
                  small
                  description={intl.formatMessage({
                    id: "loading.tests",
                    defaultMessage: "Loading tests...",
                  })}
                />
              ) : (
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <div
                      style={{
                        maxHeight: "200px",
                        overflowY: "auto",
                        border: "1px solid #e0e0e0",
                        padding: "0.5rem",
                      }}
                    >
                      {availableTests.slice(0, 50).map((test) => (
                        <Checkbox
                          key={test.id || test.value}
                          id={`test-${test.id || test.value}`}
                          labelText={
                            test.value || test.name || test.localizedTestName
                          }
                          checked={selectedTests.includes(
                            test.id || test.value,
                          )}
                          onChange={() =>
                            handleTestToggle(test.id || test.value)
                          }
                        />
                      ))}
                      {availableTests.length === 0 && (
                        <p>
                          <FormattedMessage
                            id="medlab.order.noTestsAvailable"
                            defaultMessage="No tests available"
                          />
                        </p>
                      )}
                    </div>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    {/* Test Requirements Display (FR-007 - Critical) */}
                    <div
                      className="test-requirements-panel"
                      style={{
                        border: "1px solid #0f62fe",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                      }}
                    >
                      <h6>
                        <FormattedMessage
                          id="medlab.order.sampleRequirements"
                          defaultMessage="Sample Collection Requirements"
                        />
                      </h6>
                      {loadingRequirements ? (
                        <Loading small />
                      ) : testRequirements.length > 0 ? (
                        <div>
                          {testRequirements.map((req, idx) => (
                            <div
                              key={idx}
                              style={{
                                marginBottom: "0.5rem",
                                padding: "0.5rem",
                                backgroundColor: "#fff",
                                borderLeft: "3px solid #0f62fe",
                              }}
                            >
                              <div>
                                <strong>
                                  <FormattedMessage
                                    id="medlab.order.containerType"
                                    defaultMessage="Container"
                                  />
                                  :
                                </strong>{" "}
                                {req.containerType || "-"}
                              </div>
                              <div>
                                <strong>
                                  <FormattedMessage
                                    id="medlab.order.volumeRequired"
                                    defaultMessage="Volume"
                                  />
                                  :
                                </strong>{" "}
                                {req.volumeRequiredMl
                                  ? `${req.volumeRequiredMl} mL`
                                  : "-"}
                              </div>
                              <div>
                                <strong>
                                  <FormattedMessage
                                    id="medlab.order.handling"
                                    defaultMessage="Handling"
                                  />
                                  :
                                </strong>{" "}
                                {req.handlingRequirements || "-"}
                              </div>
                              {req.storageTemperature && (
                                <div>
                                  <strong>
                                    <FormattedMessage
                                      id="medlab.order.storage"
                                      defaultMessage="Storage"
                                    />
                                    :
                                  </strong>{" "}
                                  {req.storageTemperature}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      ) : selectedTests.length > 0 ? (
                        <p>
                          <FormattedMessage
                            id="medlab.order.noRequirements"
                            defaultMessage="No specific requirements configured for selected tests"
                          />
                        </p>
                      ) : (
                        <p>
                          <FormattedMessage
                            id="medlab.order.selectTestsToSeeRequirements"
                            defaultMessage="Select tests to see sample requirements"
                          />
                        </p>
                      )}
                    </div>
                  </Column>
                </Grid>
              )}
              {selectedTests.length > 0 && (
                <div style={{ marginTop: "0.5rem" }}>
                  <Tag type="blue">
                    {selectedTests.length}{" "}
                    <FormattedMessage
                      id="medlab.order.testsSelected"
                      defaultMessage="test(s) selected"
                    />
                  </Tag>
                </div>
              )}
            </div>

            {/* Order Details Section */}
            <div
              className="lab-order-section"
              style={{ marginBottom: "1.5rem" }}
            >
              <h5>
                <FormattedMessage
                  id="medlab.order.orderDetails"
                  defaultMessage="3. Order Details"
                />
              </h5>
              <Grid fullWidth>
                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="lab-number"
                    labelText={
                      <>
                        <FormattedMessage
                          id="medlab.order.labNumber"
                          defaultMessage="Lab Number"
                        />{" "}
                        <span className="requiredlabel">*</span>
                      </>
                    }
                    value={labOrderForm.labNo}
                    onChange={(e) =>
                      setLabOrderForm((prev) => ({
                        ...prev,
                        labNo: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "medlab.order.labNumberPlaceholder",
                      defaultMessage: "Enter lab number",
                    })}
                  />
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <CustomDatePicker
                    id="request-date"
                    labelText={
                      <FormattedMessage
                        id="medlab.order.requestDate"
                        defaultMessage="Request Date"
                      />
                    }
                    value={labOrderForm.requestDate}
                    onChange={(date) =>
                      setLabOrderForm((prev) => ({
                        ...prev,
                        requestDate: date,
                      }))
                    }
                  />
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={
                      <FormattedMessage
                        id="medlab.order.priority"
                        defaultMessage="Priority"
                      />
                    }
                    name="order-priority"
                    valueSelected={labOrderForm.priority}
                    onChange={(value) =>
                      setLabOrderForm((prev) => ({ ...prev, priority: value }))
                    }
                  >
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: "medlab.order.routine",
                        defaultMessage: "Routine",
                      })}
                      value="ROUTINE"
                      id="priority-routine"
                    />
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: "medlab.order.urgent",
                        defaultMessage: "Urgent",
                      })}
                      value="URGENT"
                      id="priority-urgent"
                    />
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: "medlab.order.stat",
                        defaultMessage: "STAT",
                      })}
                      value="STAT"
                      id="priority-stat"
                    />
                  </RadioButtonGroup>
                </Column>
              </Grid>
            </div>

            {/* Create Order Button */}
            <div className="page-actions-bar">
              <Button
                kind="primary"
                size="sm"
                renderIcon={Add}
                onClick={handleCreateLabOrder}
                disabled={
                  submitting ||
                  !selectedPatient ||
                  selectedTests.length === 0 ||
                  !labOrderForm.labNo.trim()
                }
              >
                {submitting ? (
                  <Loading small withOverlay={false} />
                ) : (
                  <FormattedMessage
                    id="medlab.order.createOrder"
                    defaultMessage="Create Lab Order"
                  />
                )}
              </Button>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Created Orders Table - Global section visible across all tabs */}
      {createdOrders.length > 0 && (
        <div className="created-orders-section" style={{ marginTop: "1.5rem" }}>
          <h5>
            <FormattedMessage
              id="medlab.order.createdOrders"
              defaultMessage="Created Orders This Session"
            />
          </h5>
          <DataTable
            rows={createdOrders}
            headers={[
              {
                key: "labNo",
                header: intl.formatMessage({
                  id: "medlab.order.labNumber",
                  defaultMessage: "Lab Number",
                }),
              },
              {
                key: "patientName",
                header: intl.formatMessage({
                  id: "patient.name",
                  defaultMessage: "Patient",
                }),
              },
              {
                key: "testCount",
                header: intl.formatMessage({
                  id: "medlab.order.testCount",
                  defaultMessage: "Tests",
                }),
              },
              {
                key: "createdAt",
                header: intl.formatMessage({
                  id: "medlab.order.createdAt",
                  defaultMessage: "Created",
                }),
              },
            ]}
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer>
                <Table {...getTableProps()} size="sm">
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
                          <TableCell key={cell.id}>{cell.value}</TableCell>
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

      {/* Bulk Order Modal */}
      <BulkOrderModal
        open={bulkOrderModalOpen}
        onClose={() => setBulkOrderModalOpen(false)}
        selectedPatients={selectedPatientsForBulk}
        notebookEntryId={entryId}
        notebookPageId={pageData?.id}
        onSuccess={handleBulkOrderSuccess}
      />
    </div>
  );
}

export default PatientOrderEntryPage;
