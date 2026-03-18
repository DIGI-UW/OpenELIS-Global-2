import React, { useContext, useState, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Stack,
  TextInput,
  Button,
  Tile,
  ContentSwitcher,
  Switch,
  Accordion,
  AccordionItem,
  InlineNotification,
  Link,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext, ConfigurationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { getFromOpenElisServer } from "../../utils/Utils";
import PatientSearchSection from "./sections/PatientSearchSection";
import ProgramSection from "./sections/ProgramSection";
import ClinicalInfoSection from "./sections/ClinicalInfoSection";
import RequesterSection from "./sections/RequesterSection";
import SampleTestSection from "./sections/SampleTestSection";
import "../order-workflow.scss";

/**
 * OrderEnter - Step 1: Enter Order (Clinical Workflow)
 *
 * Full implementation based on FRS v1.0 and UI mockups.
 *
 * Sections:
 * 1. Lab Number (auto-generate or manual) + Print Labels
 * 2. Sample Category Toggle (Clinical / Environmental)
 * 3. Patient Search (clinical) / Location (environmental)
 * 4. Program Selection with dynamic additional fields
 * 5. Clinical Information (diagnosis, payment status)
 * 6. Requester / Ordering Provider (site + provider search)
 * 7. Sample & Test Selection
 */

const OrderEnter = () => {
  const intl = useIntl();
  const history = useHistory();
  const componentMounted = useRef(true);
  const {
    orderData,
    setOrderData,
    samples,
    setSamples,
    labNumber,
    saveOrder,
    markStepComplete,
    isReadOnly,
    isEditMode,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  // Local state
  const [localLabNumber, setLocalLabNumber] = useState(
    labNumber || orderData?.sampleOrderItems?.labNo || "",
  );
  const [workflowType, setWorkflowType] = useState("clinical"); // "clinical" | "environmental"
  const [labUnitConfig, setLabUnitConfig] = useState(null);
  const [isGeneratingLabNo, setIsGeneratingLabNo] = useState(false);
  const [printLabelsExpanded, setPrintLabelsExpanded] = useState(false);
  const [errors, setErrors] = useState({});

  // Phone validation state for patient form
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });

  // Fetch lab unit configuration
  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/labUnit/config", (response) => {
      if (componentMounted.current && response) {
        setLabUnitConfig(response);
        // Set default workflow type based on lab unit config
        if (response.workflowType === "Environmental") {
          setWorkflowType("environmental");
        } else if (response.workflowType === "Clinical") {
          setWorkflowType("clinical");
        }
        // If "Both", keep default "clinical"
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Generate lab number
  const handleGenerateLabNumber = () => {
    setIsGeneratingLabNo(true);
    getFromOpenElisServer(
      "/rest/SampleEntryGenerateScanProvider",
      (response) => {
        if (componentMounted.current) {
          setIsGeneratingLabNo(false);
          if (response?.body) {
            const newLabNo = response.body;
            setLocalLabNumber(newLabNo);
            setOrderData({
              ...orderData,
              sampleOrderItems: {
                ...orderData.sampleOrderItems,
                labNo: newLabNo,
              },
            });
          }
        }
      },
    );
  };

  // Handle lab number change
  const handleLabNumberChange = (e) => {
    const newLabNo = e.target.value;
    setLocalLabNumber(newLabNo);
    setOrderData({
      ...orderData,
      sampleOrderItems: {
        ...orderData.sampleOrderItems,
        labNo: newLabNo,
      },
    });
  };

  // Handle workflow type switch
  const handleWorkflowTypeChange = (index) => {
    setWorkflowType(index === 0 ? "clinical" : "environmental");
  };

  // Validation check
  const canProceed =
    localLabNumber &&
    Object.values(phoneValidation).every((item) => item.status !== false);

  // Save handler - uses orderEntryOnly=true since this is Step 1 (no samples yet)
  const handleSave = async () => {
    try {
      await saveOrder(false, true); // silent=false, orderEntryOnly=true
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  // Save and navigate to next step
  const handleSaveAndNext = async () => {
    try {
      await saveOrder(false, true); // silent=false, orderEntryOnly=true
      markStepComplete("enter");
      history.push("/order/collect");
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  // Save as draft handler
  const handleSaveAsDraft = async () => {
    try {
      await saveOrder(true, true); // silent=true, orderEntryOnly=true
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.saved.draft",
          defaultMessage: "Order saved as draft",
        }),
      });
      setNotificationVisible(true);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  // Check if lab unit supports both workflow types
  const showWorkflowToggle =
    labUnitConfig?.workflowType === "Both" ||
    configurationProperties?.LAB_WORKFLOW_TYPE === "Both";

  return (
    <OrderWorkflowLayout
      currentStep={0}
      title="order.step.enter"
      canProceed={canProceed}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
      extraButtons={
        <Button kind="tertiary" onClick={handleSaveAsDraft} size="md">
          <FormattedMessage
            id="button.save.draft"
            defaultMessage="Save as Draft"
          />
        </Button>
      }
    >
      {notificationVisible && <AlertDialog />}

      <Stack gap={7}>
        {/* Section 1: Lab Number */}
        <Tile className="order-section">
          <h4 className="section-title">
            <FormattedMessage
              id="order.labNumber"
              defaultMessage="Lab Number"
            />
          </h4>

          <Grid>
            <Column lg={12} md={6} sm={4}>
              <div className="lab-number-field">
                <TextInput
                  id="labNumber"
                  labelText={
                    <span>
                      <FormattedMessage
                        id="order.labNumber"
                        defaultMessage="Lab Number"
                      />
                      <span className="required-indicator"> *</span>
                    </span>
                  }
                  value={localLabNumber}
                  onChange={handleLabNumberChange}
                  placeholder={intl.formatMessage({
                    id: "order.labNumber.placeholder",
                    defaultMessage: "Enter or generate lab number",
                  })}
                  disabled={isReadOnly && !isEditMode}
                />
                <Link
                  className="generate-link"
                  onClick={handleGenerateLabNumber}
                  disabled={isGeneratingLabNo || (isReadOnly && !isEditMode)}
                >
                  {isGeneratingLabNo ? (
                    <FormattedMessage
                      id="generating"
                      defaultMessage="Generating..."
                    />
                  ) : (
                    <FormattedMessage
                      id="order.labNumber.generate"
                      defaultMessage="Generate"
                    />
                  )}
                </Link>
              </div>
              <p className="helper-text">
                <FormattedMessage
                  id="order.labNumber.helper"
                  defaultMessage="Auto-generated per existing lab number rules. Assigned here to enable tracking across all steps."
                />
              </p>
            </Column>
          </Grid>

          {/* Print Labels Accordion */}
          <Accordion>
            <AccordionItem
              title={
                <span className="print-labels-title">
                  <Printer size={16} />
                  <FormattedMessage
                    id="order.printLabels"
                    defaultMessage="Print Labels"
                  />
                </span>
              }
              open={printLabelsExpanded}
              onHeadingClick={() =>
                setPrintLabelsExpanded(!printLabelsExpanded)
              }
            >
              <div className="print-labels-content">
                <p className="helper-text">
                  <FormattedMessage
                    id="order.printLabels.info"
                    defaultMessage="Labels can be printed here or from Step 3 (Label & Store)."
                  />
                </p>
                <div className="label-buttons">
                  <Button kind="tertiary" size="sm" disabled={!localLabNumber}>
                    <FormattedMessage
                      id="label.order"
                      defaultMessage="Order Label"
                    />
                  </Button>
                  <Button kind="tertiary" size="sm" disabled>
                    <FormattedMessage
                      id="label.sample"
                      defaultMessage="Sample Label"
                    />
                  </Button>
                  <Button kind="tertiary" size="sm" disabled={!localLabNumber}>
                    <FormattedMessage
                      id="label.slide"
                      defaultMessage="Slide Label"
                    />
                  </Button>
                  <Button kind="tertiary" size="sm" disabled={!localLabNumber}>
                    <FormattedMessage
                      id="label.block"
                      defaultMessage="Block Label"
                    />
                  </Button>
                  <Button kind="tertiary" size="sm" disabled={!localLabNumber}>
                    <FormattedMessage
                      id="label.freezer"
                      defaultMessage="Freezer Label"
                    />
                  </Button>
                </div>
              </div>
            </AccordionItem>
          </Accordion>
        </Tile>

        {/* Section 2: Sample Category Toggle */}
        {showWorkflowToggle && (
          <Tile className="order-section">
            <h4 className="section-title">
              <FormattedMessage
                id="order.sampleCategory"
                defaultMessage="Sample Category"
              />
            </h4>
            <ContentSwitcher
              onChange={({ index }) => handleWorkflowTypeChange(index)}
              selectedIndex={workflowType === "clinical" ? 0 : 1}
            >
              <Switch name="clinical">
                <FormattedMessage
                  id="workflow.clinical"
                  defaultMessage="Clinical"
                />
              </Switch>
              <Switch name="environmental">
                <FormattedMessage
                  id="workflow.environmental"
                  defaultMessage="Environmental / Other"
                />
              </Switch>
            </ContentSwitcher>
            <p className="helper-text">
              <FormattedMessage
                id="order.sampleCategory.helper"
                defaultMessage="This toggle appears only when the lab unit is configured for 'Both' workflow types."
              />
            </p>
          </Tile>
        )}

        {/* Section 3: Patient Search (Clinical) */}
        {workflowType === "clinical" && (
          <PatientSearchSection
            orderData={orderData}
            setOrderData={setOrderData}
            setPhoneValidation={setPhoneValidation}
            isReadOnly={isReadOnly && !isEditMode}
          />
        )}

        {/* Section 3: Location (Environmental) - Placeholder */}
        {workflowType === "environmental" && (
          <Tile className="order-section">
            <h4 className="section-title">
              <FormattedMessage id="order.location" defaultMessage="Location" />
            </h4>
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "environmental.coming.soon",
                defaultMessage: "Environmental workflow",
              })}
              subtitle={intl.formatMessage({
                id: "environmental.location.info",
                defaultMessage:
                  "Location hierarchy fields (Region/District/Town-Village) and GPS capture will be available here.",
              })}
              hideCloseButton
              lowContrast
            />
          </Tile>
        )}

        {/* Section 4: Program Selection */}
        <ProgramSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Section 5: Clinical Information (Clinical workflow only) */}
        {workflowType === "clinical" && (
          <ClinicalInfoSection
            orderData={orderData}
            setOrderData={setOrderData}
            isReadOnly={isReadOnly && !isEditMode}
          />
        )}

        {/* Section 6: Requester / Ordering Provider */}
        <RequesterSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Section 7: Sample & Test Selection */}
        <SampleTestSection
          samples={samples}
          setSamples={setSamples}
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />
      </Stack>
    </OrderWorkflowLayout>
  );
};

export default OrderEnter;
