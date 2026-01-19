import React, { useState, useEffect, useCallback, useRef } from "react";
import {
  Modal,
  InlineNotification,
  Loading,
  Checkbox,
  RadioButtonGroup,
  RadioButton,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  TextInput,
  Button,
  Tag,
  ClickableTile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

/**
 * Modal for bulk linking multiple samples to orders.
 * Supports two modes:
 * 1. Shared Order Mode: Link all samples to ONE order (existing or new)
 * 2. Independent Orders Mode: Create separate order for each sample with sequential lab numbers
 */
function BulkLinkOrderModal({
  open,
  onClose,
  samples = [],
  orderEntryPageId,
  onLinkSuccess,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Mode state: 'SHARED' or 'INDEPENDENT'
  const [mode, setMode] = useState("SHARED");

  // Shared mode state
  const [availableOrders, setAvailableOrders] = useState([]);
  const [loadingOrders, setLoadingOrders] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedTestIds, setSelectedTestIds] = useState([]);
  const [activeTab, setActiveTab] = useState(0);

  // Create new order state (shared mode, tab 2)
  const [newOrderLabNo, setNewOrderLabNo] = useState("");
  const [selectedNewOrderTests, setSelectedNewOrderTests] = useState([]);
  const [creatingOrder, setCreatingOrder] = useState(false);
  const [createOrderError, setCreateOrderError] = useState(null);

  // Independent mode state
  const [labNumberPrefix, setLabNumberPrefix] = useState("ORD-");
  const [previewNumbers, setPreviewNumbers] = useState([]);
  const [selectedIndependentTests, setSelectedIndependentTests] = useState([]);

  // Available tests
  const [availableTests, setAvailableTests] = useState([]);
  const [loadingTests, setLoadingTests] = useState(false);

  // Linking state
  const [isLinking, setIsLinking] = useState(false);
  const [linkError, setLinkError] = useState(null);

  // Determine if all samples have same patient
  const uniquePatientIds = Array.from(
    new Set(samples.map((s) => s.patientId).filter(Boolean)),
  );
  const allSamePatient = uniquePatientIds.length === 1;
  const commonPatientId = allSamePatient ? uniquePatientIds[0] : null;

  // Generate random lab number
  const generateLabNumber = useCallback(() => {
    const randomNumber = Math.floor(100000 + Math.random() * 900000);
    return `ORD-${randomNumber}`;
  }, []);

  // Load available tests
  useEffect(() => {
    if (!open) return;

    setLoadingTests(true);
    getFromOpenElisServer("/rest/test-list", (response) => {
      if (componentMounted.current && response) {
        // Transform test list to have consistent format
        const tests = (response.tests || response || []).map((test) => ({
          id: test.id,
          testId: test.id,
          name: test.value || test.testName || test.name,
          testName: test.value || test.testName || test.name,
          value: test.value || test.testName || test.name,
        }));
        setAvailableTests(tests);
        setLoadingTests(false);
      }
    });
  }, [open]);

  // Load available orders (shared mode)
  const loadAvailableOrders = useCallback(() => {
    if (!orderEntryPageId) return;

    setLoadingOrders(true);
    getFromOpenElisServer(
      `/rest/notebook-page/${orderEntryPageId}/samples`,
      (result) => {
        if (componentMounted.current) {
          // Filter for PENDING_COLLECTION orders
          // If all samples have same patient, only show that patient's orders
          let orders = (result?.samples || []).filter(
            (s) => s.status === "PENDING_COLLECTION",
          );

          if (commonPatientId) {
            orders = orders.filter((o) => o.patientId === commonPatientId);
          }

          setAvailableOrders(orders);
          setLoadingOrders(false);
        }
      },
    );
  }, [orderEntryPageId, commonPatientId]);

  // Load available orders when in shared mode
  useEffect(() => {
    if (open && mode === "SHARED") {
      loadAvailableOrders();
    }
  }, [open, mode, loadAvailableOrders]);

  // Preview lab numbers for independent mode
  useEffect(() => {
    if (mode === "INDEPENDENT" && samples.length > 0) {
      // Generate preview: prefix + sequential numbers
      const previews = samples.map((sample, index) => ({
        sampleId: sample.accessionNumber || sample.sampleId,
        labNo: `${labNumberPrefix}${String(index + 1).padStart(3, "0")}`,
      }));
      setPreviewNumbers(previews);
    }
  }, [mode, samples, labNumberPrefix]);

  // Reset state when modal opens
  useEffect(() => {
    if (open) {
      setMode("SHARED");
      setSelectedOrder(null);
      setSelectedTestIds([]);
      setActiveTab(allSamePatient ? 0 : 1); // If mixed patients, go straight to "Create New"
      setNewOrderLabNo(generateLabNumber());
      setSelectedNewOrderTests([]);
      setCreateOrderError(null);
      setLabNumberPrefix("ORD-");
      setSelectedIndependentTests([]);
      setLinkError(null);
    }
  }, [open, allSamePatient, generateLabNumber]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Select an order from available orders list (shared mode)
  const handleSelectOrder = useCallback((order) => {
    setSelectedOrder(order);
    setSelectedTestIds([]);
  }, []);

  // Toggle test selection (shared mode, existing order)
  const handleToggleTest = useCallback((testId) => {
    setSelectedTestIds((prev) =>
      prev.includes(testId)
        ? prev.filter((id) => id !== testId)
        : [...prev, testId],
    );
  }, []);

  // Create new order (shared mode, tab 2)
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

    // Use common patient ID if all samples have same patient
    const orderData = {
      patientId: commonPatientId || null,
      labNo: newOrderLabNo.trim(),
      testIds,
      requestDate: today,
      receivedDate: today,
      priority: "ROUTINE",
    };

    postToOpenElisServerJsonResponse(
      "/rest/medlab/patient-order",
      JSON.stringify(orderData),
      (result) => {
        if (componentMounted.current) {
          if (result.success) {
            // Order created, select it and switch to tab 1
            const newOrder = {
              id: result.orderId,
              orderId: result.orderId,
              labNo: result.labNo || `ORDER-${result.orderId}`,
              patientId: commonPatientId,
              patientName: samples[0]?.patientName || "Anonymous",
              tests: selectedNewOrderTests.map((t) => ({
                id: t.id,
                testId: t.id,
                name: t.value || t.name || t.testName,
                testName: t.value || t.name || t.testName,
              })),
              status: "PENDING_COLLECTION",
            };

            // Add the new order to the available orders list
            setAvailableOrders((prev) => [newOrder, ...prev]);

            handleSelectOrder(newOrder);
            setSelectedTestIds(testIds);
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

  // Handle link submission
  const handleLink = () => {
    if (mode === "SHARED") {
      handleLinkShared();
    } else {
      handleLinkIndependent();
    }
  };

  // Link all samples to shared order
  const handleLinkShared = () => {
    if (!selectedOrder) {
      setLinkError("Please select an order");
      return;
    }

    if (selectedTestIds.length === 0) {
      setLinkError("Please select at least one test");
      return;
    }

    setIsLinking(true);
    setLinkError(null);

    const requestData = {
      sampleIds: samples.map((s) => s.sampleId),
      orderId: selectedOrder.orderId || selectedOrder.id,
      testIds: selectedTestIds,
    };

    postToOpenElisServerJsonResponse(
      "/rest/medlab/samples/bulk-link-shared-order",
      JSON.stringify(requestData),
      (result) => {
        if (componentMounted.current) {
          setIsLinking(false);
          if (result.success) {
            onLinkSuccess({
              mode: "SHARED",
              samplesLinked: result.samplesLinked,
              linksCreated: result.linksCreated,
            });
            onClose();
          } else {
            setLinkError(result.error || "Failed to link samples");
          }
        }
      },
    );
  };

  // Create independent orders for each sample
  const handleLinkIndependent = () => {
    if (!labNumberPrefix.trim()) {
      setLinkError("Lab number prefix is required");
      return;
    }

    if (selectedIndependentTests.length === 0) {
      setLinkError("Please select at least one test");
      return;
    }

    setIsLinking(true);
    setLinkError(null);

    const requestData = {
      sampleIds: samples.map((s) => s.sampleId),
      labNumberPrefix: labNumberPrefix.trim(),
      testIds: selectedIndependentTests.map((t) => t.id),
    };

    postToOpenElisServerJsonResponse(
      "/rest/medlab/samples/bulk-link-independent-orders",
      JSON.stringify(requestData),
      (result) => {
        if (componentMounted.current) {
          setIsLinking(false);
          if (result.success) {
            onLinkSuccess({
              mode: "INDEPENDENT",
              ordersCreated: result.ordersCreated,
              linksCreated: result.linksCreated,
            });
            onClose();
          } else {
            setLinkError(result.error || "Failed to create orders");
          }
        }
      },
    );
  };

  // Determine if link button should be disabled
  const isLinkDisabled = () => {
    if (isLinking) return true;

    if (mode === "SHARED") {
      return !selectedOrder || selectedTestIds.length === 0;
    } else {
      return !labNumberPrefix.trim() || selectedIndependentTests.length === 0;
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "medlab.bulkLinkOrder.modal.title",
        defaultMessage: "Link Samples to Orders",
      })}
      primaryButtonText={
        isLinking
          ? intl.formatMessage({
              id: "label.linking",
              defaultMessage: "Linking...",
            })
          : intl.formatMessage({
              id: "label.link",
              defaultMessage: "Link",
            })
      }
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={handleLink}
      primaryButtonDisabled={isLinkDisabled()}
      size="lg"
    >
      {/* Sample count */}
      <div style={{ marginBottom: "1rem" }}>
        <Tag type="blue">
          <FormattedMessage
            id="medlab.bulkLinkOrder.samplesSelected"
            defaultMessage="{count} sample(s) selected"
            values={{ count: samples.length }}
          />
        </Tag>
      </div>

      {/* Patient mismatch warning */}
      {!allSamePatient && samples.length > 0 && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "medlab.bulkLinkOrder.patientMismatch.warning",
            defaultMessage:
              "Selected samples belong to different patients. You can only create a new order.",
          })}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Mode selection */}
      <RadioButtonGroup
        legendText={intl.formatMessage({
          id: "medlab.bulkLinkOrder.mode.label",
          defaultMessage: "Linking Mode",
        })}
        name="linking-mode"
        valueSelected={mode}
        onChange={setMode}
        style={{ marginBottom: "1.5rem" }}
      >
        <RadioButton
          labelText={intl.formatMessage({
            id: "medlab.bulkLinkOrder.mode.shared",
            defaultMessage: "Link all samples to same order",
          })}
          value="SHARED"
          id="mode-shared"
        />
        <RadioButton
          labelText={intl.formatMessage({
            id: "medlab.bulkLinkOrder.mode.independent",
            defaultMessage: "Create separate order for each sample",
          })}
          value="INDEPENDENT"
          id="mode-independent"
        />
      </RadioButtonGroup>

      {/* Error message */}
      {linkError && (
        <InlineNotification
          kind="error"
          title={linkError}
          lowContrast
          onCloseButtonClick={() => setLinkError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Shared Order Mode */}
      {mode === "SHARED" && (
        <>
          {allSamePatient ? (
            <Tabs
              selectedIndex={activeTab}
              onChange={(e) => setActiveTab(e.selectedIndex)}
            >
              <TabList aria-label="Order selection tabs">
                <Tab>
                  <FormattedMessage
                    id="medlab.collection.linkOrder.selectExisting"
                    defaultMessage="Select Existing Order"
                  />
                </Tab>
                <Tab>
                  <FormattedMessage
                    id="medlab.collection.linkOrder.createNew"
                    defaultMessage="Create New Order"
                  />
                </Tab>
              </TabList>

              <TabPanels>
                {/* Tab 1: Select Existing Order */}
                <TabPanel>
                  {loadingOrders ? (
                    <Loading
                      description="Loading orders..."
                      withOverlay={false}
                    />
                  ) : availableOrders.length > 0 ? (
                    <div>
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          gap: "0.5rem",
                          maxHeight: "400px",
                          overflowY: "auto",
                          marginBottom: "1rem",
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
                                  {order.labNo || order.externalId}
                                </strong>
                              </div>
                              <div style={{ flex: "1", color: "#525252" }}>
                                {order.patientName || "Anonymous"}
                              </div>
                              <div
                                style={{
                                  flex: "1",
                                  color: "#525252",
                                  fontSize: "0.875rem",
                                }}
                              >
                                {order.tests
                                  ?.map((t) => t.testName)
                                  .join(", ") || "N/A"}
                              </div>
                            </div>
                          </ClickableTile>
                        ))}
                      </div>

                      {/* Test selection */}
                      {selectedOrder && selectedOrder.tests && (
                        <div style={{ marginTop: "1rem" }}>
                          <fieldset>
                            <legend className="cds--label">
                              <FormattedMessage
                                id="medlab.bulkLinkOrder.selectTests"
                                defaultMessage="Select Tests (applied to all samples)"
                              />
                            </legend>
                            <div
                              style={{
                                display: "grid",
                                gridTemplateColumns:
                                  "repeat(auto-fill, minmax(200px, 1fr))",
                                gap: "0.5rem",
                                marginTop: "0.5rem",
                              }}
                            >
                              {selectedOrder.tests.map((test) => (
                                <Checkbox
                                  key={test.testId || test.id}
                                  id={`test-${test.testId || test.id}`}
                                  labelText={test.testName || test.name}
                                  checked={selectedTestIds.includes(
                                    test.testId || test.id,
                                  )}
                                  onChange={() =>
                                    handleToggleTest(test.testId || test.id)
                                  }
                                />
                              ))}
                            </div>
                          </fieldset>
                        </div>
                      )}
                    </div>
                  ) : (
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "medlab.collection.linkOrder.noAvailableOrders",
                        defaultMessage: "No pending orders available",
                      })}
                      subtitle={intl.formatMessage({
                        id: "medlab.collection.linkOrder.createNewSuggestion",
                        defaultMessage:
                          'Switch to "Create New Order" tab to create an order',
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                </TabPanel>

                {/* Tab 2: Create New Order */}
                <TabPanel>
                  <p style={{ marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="medlab.collection.linkOrder.createNewDescription"
                      defaultMessage="Create a new test order for this sample:"
                    />
                    {commonPatientId
                      ? " (will be linked to patient)"
                      : " (will be anonymous order)"}
                  </p>

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
                    style={{ marginBottom: "1rem" }}
                  />

                  {loadingTests ? (
                    <Loading
                      description="Loading tests..."
                      withOverlay={false}
                    />
                  ) : (
                    <fieldset>
                      <legend className="cds--label">
                        <FormattedMessage
                          id="medlab.order.selectTests"
                          defaultMessage="Select Tests"
                        />
                      </legend>
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
                              console.log(
                                "Checkbox clicked:",
                                test.name,
                                e.target.checked,
                              );
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
                    </fieldset>
                  )}

                  {createOrderError && (
                    <InlineNotification
                      kind="error"
                      title={createOrderError}
                      lowContrast
                      onCloseButtonClick={() => setCreateOrderError(null)}
                      style={{ marginTop: "1rem" }}
                    />
                  )}

                  <Button
                    kind="primary"
                    onClick={handleCreateOrder}
                    disabled={
                      creatingOrder || selectedNewOrderTests.length === 0
                    }
                    style={{ marginTop: "1rem" }}
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
                </TabPanel>
              </TabPanels>
            </Tabs>
          ) : (
            // When samples have different patients, only show create new order option
            <div>
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "medlab.bulkLinkOrder.patientMismatch.warning",
                  defaultMessage:
                    "Selected samples belong to different patients. You can only create a new order.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />

              <TextInput
                id="new-order-lab-no"
                labelText={intl.formatMessage({
                  id: "medlab.order.labNumber",
                  defaultMessage: "Lab Number",
                })}
                value={newOrderLabNo}
                onChange={(e) => setNewOrderLabNo(e.target.value)}
                placeholder="ORD-123456"
                style={{ marginBottom: "1rem" }}
              />

              {loadingTests ? (
                <Loading description="Loading tests..." withOverlay={false} />
              ) : (
                <fieldset>
                  <legend className="cds--label">
                    <FormattedMessage
                      id="medlab.order.selectTests"
                      defaultMessage="Select Tests"
                    />
                  </legend>
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
                        id={`new-test-diff-${test.id}`}
                        labelText={test.value || test.name || test.testName}
                        checked={selectedNewOrderTests.some(
                          (t) => t.id === test.id,
                        )}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedNewOrderTests((prev) => [...prev, test]);
                          } else {
                            setSelectedNewOrderTests((prev) =>
                              prev.filter((t) => t.id !== test.id),
                            );
                          }
                        }}
                      />
                    ))}
                  </div>
                </fieldset>
              )}

              {createOrderError && (
                <InlineNotification
                  kind="error"
                  title={createOrderError}
                  lowContrast
                  onCloseButtonClick={() => setCreateOrderError(null)}
                  style={{ marginTop: "1rem" }}
                />
              )}

              <Button
                kind="secondary"
                onClick={handleCreateOrder}
                disabled={
                  !newOrderLabNo.trim() ||
                  selectedNewOrderTests.length === 0 ||
                  creatingOrder
                }
                style={{ marginTop: "1rem" }}
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
            </div>
          )}
        </>
      )}

      {/* Independent Orders Mode */}
      {mode === "INDEPENDENT" && (
        <div>
          {/* Lab number prefix */}
          <TextInput
            id="lab-number-prefix"
            labelText={intl.formatMessage({
              id: "medlab.bulkLinkOrder.labNumberPrefix",
              defaultMessage: "Lab Number Prefix",
            })}
            value={labNumberPrefix}
            onChange={(e) => setLabNumberPrefix(e.target.value)}
            placeholder="ORD-"
            style={{ marginBottom: "1rem" }}
          />

          {/* Preview lab numbers */}
          <div style={{ marginBottom: "1rem" }}>
            <h5 className="cds--label">
              <FormattedMessage
                id="medlab.bulkLinkOrder.preview.title"
                defaultMessage="Preview Lab Numbers"
              />
            </h5>
            <div
              style={{
                maxHeight: "150px",
                overflowY: "auto",
                padding: "0.5rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                marginTop: "0.5rem",
              }}
            >
              {previewNumbers.map((preview, index) => (
                <div key={index} style={{ padding: "0.25rem 0" }}>
                  {preview.sampleId} → <strong>{preview.labNo}</strong>
                </div>
              ))}
            </div>
          </div>

          {/* Test selection */}
          {loadingTests ? (
            <Loading description="Loading tests..." withOverlay={false} />
          ) : (
            <fieldset>
              <legend className="cds--label">
                <FormattedMessage
                  id="medlab.bulkLinkOrder.selectTests"
                  defaultMessage="Select Tests (applied to all samples)"
                />
              </legend>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
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
                    id={`independent-test-${test.id}`}
                    labelText={test.value || test.name || test.testName}
                    checked={selectedIndependentTests.some(
                      (t) => t.id === test.id,
                    )}
                    onChange={(e) => {
                      console.log(
                        "Independent checkbox clicked:",
                        test.name,
                        e.target.checked,
                      );
                      if (e.target.checked) {
                        setSelectedIndependentTests((prev) => [...prev, test]);
                      } else {
                        setSelectedIndependentTests((prev) =>
                          prev.filter((t) => t.id !== test.id),
                        );
                      }
                    }}
                  />
                ))}
              </div>
            </fieldset>
          )}
        </div>
      )}
    </Modal>
  );
}

export default BulkLinkOrderModal;
