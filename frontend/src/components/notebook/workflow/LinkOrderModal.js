import React, { useState, useCallback, useRef, useEffect } from "react";
import {
  Modal,
  InlineNotification,
  Loading,
  Tag,
  Checkbox,
  Button,
  Column,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  TextInput,
  ClickableTile,
} from "@carbon/react";
import { Add } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import config from "../../../config.json";
import "../workflow/NotebookWorkflow.css";

/**
 * LinkOrderModal - Modal for linking samples to lab orders in Stage 2 (Sample Collection).
 * Two-tab workflow:
 * 1. Select from available orders (Stage 1 Patient Order Entry) - filtered by patient if sample has one
 * 2. Create new test order on-the-fly (anonymous orders supported)
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback when modal is closed
 * @param {Object} props.sample - The sample to link (sampleId, sampleItemId, accessionNumber, patientId)
 * @param {number} props.orderEntryPageId - The Patient Order Entry page ID (Stage 1)
 * @param {function} props.onLinkSuccess - Callback when linking is successful
 */
function LinkOrderModal({
  open,
  onClose,
  sample,
  orderEntryPageId,
  onLinkSuccess,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Track component mount state
  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // State for available orders from Stage 1
  const [availableOrders, setAvailableOrders] = useState([]);
  const [loadingOrders, setLoadingOrders] = useState(false);

  // State for available tests (for creating new orders)
  const [availableTests, setAvailableTests] = useState([]);
  const [loadingTests, setLoadingTests] = useState(false);

  // State for selected order
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [orderTests, setOrderTests] = useState([]);
  const [selectedTestIds, setSelectedTestIds] = useState([]);

  // State for creating new order
  const [selectedNewOrderTests, setSelectedNewOrderTests] = useState([]);
  const [newOrderLabNo, setNewOrderLabNo] = useState("");
  const [creatingOrder, setCreatingOrder] = useState(false);
  const [createOrderError, setCreateOrderError] = useState(null);

  // State for linking
  const [isLinking, setIsLinking] = useState(false);
  const [linkError, setLinkError] = useState(null);

  // State for active tab
  const [activeTab, setActiveTab] = useState(0);

  // Generate random 6-digit lab number
  const generateLabNumber = useCallback(() => {
    const randomNumber = Math.floor(100000 + Math.random() * 900000);
    return `ORD-${randomNumber}`;
  }, []);

  /**
   * Load available orders from Stage 1 (Patient Order Entry page)
   * Filters:
   * 1. Only PENDING_COLLECTION orders (no samples yet)
   * 2. If sample has patient, only show that patient's orders
   */
  const loadAvailableOrders = useCallback(async () => {
    if (!orderEntryPageId) return;

    setLoadingOrders(true);
    getFromOpenElisServer(
      `/rest/medlab/page/${orderEntryPageId}/orders`,
      (response) => {
        if (componentMounted.current && response) {
          const orders = Array.isArray(response) ? response : [];

          // Filter orders:
          // 1. Only PENDING_COLLECTION (no samples collected yet)
          // 2. If sample has patient, only show that patient's orders
          const filteredOrders = orders.filter((order) => {
            // Must be pending collection (no sample yet)
            if (order.status !== "PENDING_COLLECTION") return false;

            // If sample is linked to a patient, only show orders for that patient
            if (
              sample?.patientId &&
              String(order.patientId) !== String(sample.patientId)
            ) {
              return false;
            }

            return true;
          });

          setAvailableOrders(filteredOrders);
        }
        setLoadingOrders(false);
      },
    );
  }, [orderEntryPageId, sample?.patientId]);

  // Load available tests for creating new orders
  const loadAvailableTests = useCallback(() => {
    setLoadingTests(true);
    getFromOpenElisServer("/rest/test-list", (response) => {
      if (componentMounted.current && response) {
        setAvailableTests(Array.isArray(response) ? response : []);
      }
      setLoadingTests(false);
    });
  }, []);

  // Load data when modal opens
  useEffect(() => {
    if (open) {
      if (orderEntryPageId) {
        loadAvailableOrders();
      }
      loadAvailableTests();
    }
  }, [open, orderEntryPageId, loadAvailableOrders, loadAvailableTests]);

  // Reset state when modal opens
  useEffect(() => {
    if (open) {
      setSelectedOrder(null);
      setOrderTests([]);
      setSelectedTestIds([]);
      setLinkError(null);
      setSelectedNewOrderTests([]);
      setNewOrderLabNo(generateLabNumber());
      setCreateOrderError(null);
      setActiveTab(0);
    }
  }, [open, generateLabNumber]);

  // Select an order from available orders list
  const handleSelectOrder = useCallback((order) => {
    console.log("Order selected", { order });
    setSelectedOrder(order);
    setSelectedTestIds([]);

    // Extract tests from order
    if (order.tests && Array.isArray(order.tests)) {
      setOrderTests(
        order.tests.map((test) => ({
          id: test.testId || test.id,
          name: test.testName || test.name,
          sampleType: test.sampleType,
        })),
      );
    } else {
      setOrderTests([]);
    }
  }, []);

  // Handle test selection for existing order
  const handleTestSelectionChange = useCallback((testId, selected) => {
    setSelectedTestIds((prev) => {
      if (selected) {
        return [...prev, testId];
      } else {
        return prev.filter((id) => id !== testId);
      }
    });
  }, []);

  // Handle select all tests
  const handleSelectAllTests = useCallback(
    (selected) => {
      if (selected) {
        setSelectedTestIds(orderTests.map((t) => t.id));
      } else {
        setSelectedTestIds([]);
      }
    },
    [orderTests],
  );

  /**
   * Create new test order on-the-fly
   * - Uses sample's patientId if it exists
   * - Otherwise creates anonymous order (NULL patient)
   */
  const handleCreateOrder = () => {
    if (selectedNewOrderTests.length === 0) {
      setCreateOrderError("Please select at least one test");
      return;
    }

    if (!newOrderLabNo.trim()) {
      setCreateOrderError("Lab number is required");
      return;
    }

    setCreatingOrder(true);
    setCreateOrderError(null);

    const testIds = selectedNewOrderTests.map((t) => t.id);
    const today = new Date().toISOString().split("T")[0];

    // Use sample's patientId if it exists, otherwise null (anonymous order)
    const orderData = {
      patientId: sample?.patientId || null,
      labNo: newOrderLabNo.trim(),
      testIds,
      requestDate: today,
      receivedDate: today,
      priority: "ROUTINE",
    };

    // Call createPatientOrder endpoint
    postToOpenElisServerJsonResponse(
      "/rest/medlab/patient-order",
      JSON.stringify(orderData),
      (result) => {
        if (componentMounted.current) {
          if (result.success) {
            // Order created, now select it
            const newOrder = {
              id: result.orderId,
              orderId: result.orderId,
              labNo: result.labNo || `ORDER-${result.orderId}`,
              patientId: sample?.patientId,
              patientName: sample?.patientName || "Anonymous",
              tests: selectedNewOrderTests.map((t) => ({
                id: t.id,
                testId: t.id,
                name: t.value || t.name || t.testName,
                testName: t.value || t.name || t.testName,
              })),
              status: "PENDING_COLLECTION",
            };

            handleSelectOrder(newOrder);
            // Auto-select all tests
            setSelectedTestIds(testIds);
            // Switch to first tab to show the selected order
            setActiveTab(0);
            setCreateOrderError(null);
          } else {
            setCreateOrderError(result.error || "Failed to create order");
          }
          setCreatingOrder(false);
        }
      },
    );
  };

  // Link sample to order
  const handleLink = async () => {
    if (!sample || !selectedOrder) {
      setLinkError("Please select an order first");
      return;
    }

    if (selectedTestIds.length === 0) {
      setLinkError("Please select at least one test");
      return;
    }

    setIsLinking(true);
    setLinkError(null);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/medlab/samples/${sample.sampleId}/link-order`;
      const response = await fetch(endpoint, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: JSON.stringify({
          orderId: selectedOrder.orderId || selectedOrder.id,
          labNo: selectedOrder.labNo,
          sampleItemId: sample.sampleItemId,
          testIds: selectedTestIds,
        }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        if (onLinkSuccess) {
          onLinkSuccess({
            sample,
            order: selectedOrder,
            testsLinked: selectedTestIds.length,
            ...data,
          });
        }
        handleClose();
      } else {
        setLinkError(data.error || "Error linking sample to order");
      }
    } catch (error) {
      console.error("LinkOrderModal: Link error:", error);
      setLinkError(error.message);
    } finally {
      setIsLinking(false);
    }
  };

  const handleClose = () => {
    setSelectedOrder(null);
    setOrderTests([]);
    setSelectedTestIds([]);
    setLinkError(null);
    setSelectedNewOrderTests([]);
    setCreateOrderError(null);
    onClose();
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "medlab.collection.linkOrder.title",
        defaultMessage: "Link Sample to Order",
      })}
      primaryButtonText={intl.formatMessage({
        id: "medlab.collection.linkOrder",
        defaultMessage: "Link to Order",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "medlab.manifest.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={handleLink}
      primaryButtonDisabled={
        !selectedOrder || selectedTestIds.length === 0 || isLinking
      }
      size="lg"
    >
      <div className="link-order-modal">
        {/* Sample info */}
        {sample && (
          <div className="sample-info" style={{ marginBottom: "1rem" }}>
            <Tag type="blue">
              Sample ID: {sample.accessionNumber || sample.sampleId}
            </Tag>
            {sample.sampleType && <Tag type="gray">{sample.sampleType}</Tag>}
            {sample.patientName && (
              <Tag type="green">Patient: {sample.patientName}</Tag>
            )}
            {!sample.patientId && <Tag type="warm-gray">Anonymous Sample</Tag>}
          </div>
        )}

        <Tabs
          selectedIndex={activeTab}
          onChange={(e) => setActiveTab(e.selectedIndex)}
        >
          <TabList aria-label="Order selection options">
            <Tab>
              <FormattedMessage
                id="medlab.collection.linkOrder.availableOrders"
                defaultMessage="Available Orders"
              />
              {availableOrders.length > 0 && (
                <Tag type="cyan" size="sm" style={{ marginLeft: "0.5rem" }}>
                  {availableOrders.length}
                </Tag>
              )}
            </Tab>
            <Tab>
              <FormattedMessage
                id="medlab.collection.linkOrder.createOrder"
                defaultMessage="Create New Order"
              />
            </Tab>
          </TabList>
          <TabPanels>
            {/* Tab 1: Available Orders from Stage 1 */}
            <TabPanel>
              {loadingOrders ? (
                <Loading description="Loading available orders..." />
              ) : availableOrders.length > 0 ? (
                <div style={{ marginTop: "1rem" }}>
                  <p style={{ marginBottom: "1rem", color: "#525252" }}>
                    <FormattedMessage
                      id="medlab.collection.linkOrder.selectFromList"
                      defaultMessage="Select an order from the Patient Order Entry page (Stage 1):"
                    />
                  </p>
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      gap: "0.5rem",
                      maxHeight: "400px",
                      overflowY: "auto",
                    }}
                  >
                    {availableOrders.map((order) => (
                      <ClickableTile
                        key={order.id}
                        onClick={() => {
                          console.log("Tile clicked", order);
                          handleSelectOrder(order);
                        }}
                        style={{
                          backgroundColor:
                            selectedOrder?.id === order.id
                              ? "#e0e0e0"
                              : "transparent",
                          border:
                            selectedOrder?.id === order.id
                              ? "2px solid #0f62fe"
                              : "1px solid #e0e0e0",
                          padding: "1rem",
                        }}
                      >
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            gap: "1rem",
                          }}
                        >
                          <div style={{ flex: "1" }}>
                            <strong style={{ fontSize: "1rem" }}>
                              {order.labNo}
                            </strong>
                          </div>
                          <div style={{ flex: "1", color: "#525252" }}>
                            {order.patientName || "Anonymous"}
                          </div>
                          <div style={{ flex: "0 0 auto", color: "#525252" }}>
                            {order.testCount || 0}{" "}
                            <FormattedMessage
                              id="medlab.order.tests"
                              defaultMessage="tests"
                            />
                          </div>
                          <div style={{ flex: "0 0 auto" }}>
                            <Tag type="cyan" size="sm">
                              {order.status}
                            </Tag>
                          </div>
                        </div>
                      </ClickableTile>
                    ))}
                  </div>
                </div>
              ) : (
                <InlineNotification
                  kind="info"
                  title={
                    sample?.patientId
                      ? intl.formatMessage({
                          id: "medlab.collection.linkOrder.noOrdersForPatient",
                          defaultMessage: "No pending orders for this patient",
                        })
                      : intl.formatMessage({
                          id: "medlab.collection.linkOrder.noAvailableOrders",
                          defaultMessage:
                            "No pending orders in Patient Order Entry",
                        })
                  }
                  subtitle={intl.formatMessage({
                    id: "medlab.collection.linkOrder.tryCreate",
                    defaultMessage: "Try creating a new order",
                  })}
                  hideCloseButton
                  lowContrast
                  style={{ marginTop: "1rem" }}
                />
              )}
            </TabPanel>

            {/* Tab 2: Create New Test Order */}
            <TabPanel>
              <div style={{ marginTop: "1rem" }}>
                <p style={{ marginBottom: "1rem", color: "#525252" }}>
                  <FormattedMessage
                    id="medlab.collection.linkOrder.createNewDescription"
                    defaultMessage="Create a new test order for this sample:"
                  />
                  {sample?.patientId
                    ? " (will be linked to patient)"
                    : " (will be anonymous order)"}
                </p>

                <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                  <TextInput
                    id="new-order-lab-no"
                    labelText={intl.formatMessage({
                      id: "medlab.order.labNumber",
                      defaultMessage: "Lab Number",
                    })}
                    value={newOrderLabNo}
                    onChange={(e) => setNewOrderLabNo(e.target.value)}
                    placeholder="ORD-123456"
                    invalid={!newOrderLabNo.trim() && createOrderError}
                    invalidText={
                      !newOrderLabNo.trim()
                        ? intl.formatMessage({
                            id: "medlab.order.labNumberRequired",
                            defaultMessage: "Lab number is required",
                          })
                        : ""
                    }
                  />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <fieldset>
                    <legend className="cds--label">
                      <FormattedMessage
                        id="medlab.order.selectTests"
                        defaultMessage="Select Tests"
                      />
                    </legend>
                    {loadingTests ? (
                      <Loading description="Loading tests..." />
                    ) : (
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns:
                            "repeat(auto-fill, minmax(200px, 1fr))",
                          gap: "0.5rem",
                          marginTop: "0.5rem",
                          maxHeight: "300px",
                          overflowY: "auto",
                          padding: "0.5rem",
                          border: "1px solid #e0e0e0",
                          borderRadius: "4px",
                        }}
                      >
                        {availableTests.map((test) => (
                          <Checkbox
                            key={test.id}
                            id={`new-test-${test.id}`}
                            labelText={test.value || test.name || test.testName}
                            checked={selectedNewOrderTests.some(
                              (t) => t.id === test.id,
                            )}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedNewOrderTests((prev) => [
                                  ...prev,
                                  test,
                                ]);
                              } else {
                                setSelectedNewOrderTests((prev) =>
                                  prev.filter((t) => t.id !== test.id),
                                );
                              }
                            }}
                          />
                        ))}
                      </div>
                    )}
                  </fieldset>
                </Column>

                <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
                  <Button
                    kind="primary"
                    renderIcon={Add}
                    onClick={handleCreateOrder}
                    disabled={
                      creatingOrder || selectedNewOrderTests.length === 0
                    }
                  >
                    {creatingOrder
                      ? intl.formatMessage({
                          id: "label.creating",
                          defaultMessage: "Creating...",
                        })
                      : intl.formatMessage({
                          id: "medlab.order.createOrder",
                          defaultMessage: "Create Order",
                        })}
                  </Button>
                </Column>

                {createOrderError && (
                  <InlineNotification
                    kind="error"
                    title={createOrderError}
                    hideCloseButton
                    lowContrast
                    style={{ marginTop: "1rem" }}
                  />
                )}
              </div>
            </TabPanel>
          </TabPanels>
        </Tabs>

        {/* Selected order details */}
        {selectedOrder && (
          <div
            className="order-details"
            style={{
              marginTop: "1.5rem",
              borderTop: "1px solid #e0e0e0",
              paddingTop: "1rem",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <h5>
                <FormattedMessage
                  id="medlab.collection.linkOrder.selectedOrder"
                  defaultMessage="Selected Order"
                />
              </h5>
              <Button
                kind="ghost"
                size="sm"
                onClick={() => {
                  setSelectedOrder(null);
                  setOrderTests([]);
                  setSelectedTestIds([]);
                }}
              >
                <FormattedMessage
                  id="label.button.change"
                  defaultMessage="Change"
                />
              </Button>
            </div>
            <div className="order-summary" style={{ marginBottom: "1rem" }}>
              <Tag type="blue">{selectedOrder.labNo}</Tag>
              <Tag type="gray">{selectedOrder.patientName || "Anonymous"}</Tag>
            </div>

            {/* Test selection */}
            <h5>
              <FormattedMessage
                id="medlab.collection.linkOrder.assignTests"
                defaultMessage="Assign Tests"
              />
              {selectedTestIds.length > 0 && (
                <Tag type="green" style={{ marginLeft: "0.5rem" }}>
                  <FormattedMessage
                    id="medlab.collection.linkOrder.testsSelected"
                    defaultMessage="{count} test(s) selected"
                    values={{ count: selectedTestIds.length }}
                  />
                </Tag>
              )}
            </h5>

            {orderTests.length > 0 ? (
              <div className="test-selection">
                <Checkbox
                  id="select-all-tests"
                  labelText={intl.formatMessage({
                    id: "label.selectAll",
                    defaultMessage: "Select All",
                  })}
                  checked={
                    selectedTestIds.length === orderTests.length &&
                    orderTests.length > 0
                  }
                  indeterminate={
                    selectedTestIds.length > 0 &&
                    selectedTestIds.length < orderTests.length
                  }
                  onChange={(e) => handleSelectAllTests(e.target.checked)}
                  style={{ marginBottom: "0.5rem" }}
                />
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.5rem",
                  }}
                >
                  {orderTests.map((test) => (
                    <div
                      key={test.id}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        padding: "0.5rem",
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                      }}
                    >
                      <Checkbox
                        id={`test-${test.id}`}
                        labelText={test.name}
                        checked={selectedTestIds.includes(test.id)}
                        onChange={(e) =>
                          handleTestSelectionChange(test.id, e.target.checked)
                        }
                      />
                      {test.sampleType && (
                        <Tag type="cool-gray" size="sm">
                          {test.sampleType}
                        </Tag>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "medlab.collection.linkOrder.noTestsAvailable",
                  defaultMessage: "No tests available for this order",
                })}
                hideCloseButton
                lowContrast
              />
            )}
          </div>
        )}

        {linkError && (
          <InlineNotification
            kind="error"
            title={linkError}
            hideCloseButton
            lowContrast
            style={{ marginTop: "1rem" }}
          />
        )}

        {isLinking && (
          <Loading
            withOverlay={false}
            description={intl.formatMessage({
              id: "medlab.collection.linkOrder.linking",
              defaultMessage: "Linking sample to order...",
            })}
          />
        )}
      </div>
    </Modal>
  );
}

export default LinkOrderModal;
