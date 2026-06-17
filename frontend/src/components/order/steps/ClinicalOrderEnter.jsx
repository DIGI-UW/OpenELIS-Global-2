import React, { useContext, useState, useEffect, useRef } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Stack,
  TextInput,
  Button,
  Tile,
  Accordion,
  AccordionItem,
  Link,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
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

const WORKFLOW_TYPE = "clinical";

const ClinicalOrderEnter = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const componentMounted = useRef(true);
  const {
    orderData,
    setOrderData,
    samples,
    setSamples,
    labNumber,
    saveOrderEntry,
    markStepComplete,
    isReadOnly,
    isEditMode,
    resetOrder,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Initialise empty — populated by the sync effect below after the mount
  // reset guard runs, preventing stale cross-domain lab numbers from bleeding in.
  const [localLabNumber, setLocalLabNumber] = useState("");
  const [isGeneratingLabNo, setIsGeneratingLabNo] = useState(false);
  const [printLabelsExpanded, setPrintLabelsExpanded] = useState(false);
  const [errors, setErrors] = useState({});
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });

  // Reset on mount for new orders. Only skip reset when ?order= is present
  // AND the URL path belongs to this workflow — prevents a stale ?order= from
  // a different domain blocking the reset when switching between workflows.
  useEffect(() => {
    const orderParam = new URLSearchParams(location.search).get("order");
    const pathMatchesWorkflow = location.pathname.startsWith("/order/clinical");
    if (!isEditMode && !(orderParam && pathMatchesWorkflow)) {
      resetOrder();
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Seed workflowType into orderData on mount (or when editing an existing order
  // that already has a workflowType — keep it so it is not reset on re-render).
  useEffect(() => {
    const current =
      orderData?.sampleOrderItems?.environmentalFields?.workflowType;
    if (current !== WORKFLOW_TYPE) {
      setOrderData((prev) => ({
        ...prev,
        patientUpdateStatus:
          prev.patientUpdateStatus !== undefined
            ? prev.patientUpdateStatus
            : "ADD",
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          environmentalFields: {
            ...prev.sampleOrderItems?.environmentalFields,
            workflowType: WORKFLOW_TYPE,
          },
        },
      }));
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Sync local lab number when context changes (e.g., order loaded from dashboard)
  useEffect(() => {
    const contextLabNo = labNumber || orderData?.sampleOrderItems?.labNo;
    const pathMatchesWorkflow = location.pathname.startsWith("/order/clinical");
    if (!pathMatchesWorkflow) return;
    if (contextLabNo && contextLabNo !== localLabNumber) {
      setLocalLabNumber(contextLabNo);
    } else if (!contextLabNo && localLabNumber) {
      setLocalLabNumber("");
    }
  }, [labNumber, orderData?.sampleOrderItems?.labNo, location.pathname]);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

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

  const hasPatientOrSite = !!(
    orderData?.patientProperties?.lastName ||
    orderData?.patientProperties?.nationalId
  );
  const hasSampleTypes = samples.some((s) => s.sampleTypeId);
  const canSave = localLabNumber && hasPatientOrSite && hasSampleTypes;

  const canProceed =
    canSave &&
    Object.values(phoneValidation).every((item) => item.status !== false);

  const handleSave = async () => {
    if (!canSave) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.save.incomplete",
          defaultMessage:
            "Please add a patient and at least one sample type before saving.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    try {
      await saveOrderEntry(false);
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

  const handleSaveAndNext = async () => {
    if (!canSave) return;
    try {
      await saveOrderEntry(false);
      markStepComplete("enter");
      history.push(
        labNumber
          ? `/order/clinical/collect?order=${encodeURIComponent(labNumber)}`
          : "/order/clinical/collect",
      );
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleSaveAsDraft = async () => {
    if (!canSave) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.save.incomplete",
          defaultMessage:
            "Please add a patient and at least one sample type before saving.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    try {
      await saveOrderEntry(true);
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

  return (
    <OrderWorkflowLayout
      title="order.step.enter"
      canProceed={canProceed}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
      extraButtons={
        <Button
          kind="tertiary"
          onClick={handleSaveAsDraft}
          size="md"
          disabled={!canSave}
        >
          <FormattedMessage
            id="button.save.draft"
            defaultMessage="Save as Draft"
          />
        </Button>
      }
    >
      {notificationVisible && <AlertDialog />}

      <Stack gap={7}>
        {/* Lab Number */}
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

        {/* Patient Search */}
        <PatientSearchSection
          orderData={orderData}
          setOrderData={setOrderData}
          setPhoneValidation={setPhoneValidation}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Program Selection */}
        <ProgramSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Clinical Information */}
        <ClinicalInfoSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Requester / Ordering Provider */}
        <RequesterSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          workflowType={WORKFLOW_TYPE}
        />

        {/* Sample & Test Selection */}
        <SampleTestSection
          samples={samples}
          setSamples={setSamples}
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          workflowType={WORKFLOW_TYPE}
        />
      </Stack>
    </OrderWorkflowLayout>
  );
};

export default ClinicalOrderEnter;
