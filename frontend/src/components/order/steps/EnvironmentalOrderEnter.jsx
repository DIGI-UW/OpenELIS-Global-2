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
import { Printer, Warning } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import InlineNceForm from "../../nonconform/common/InlineNceForm";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { getFromOpenElisServer } from "../../utils/Utils";
import VectorSection from "./sections/VectorSection";
import CollectionConditionsSection from "./sections/CollectionConditionsSection";
import ProgramSection from "./sections/ProgramSection";
import RequesterSection from "./sections/RequesterSection";
import SampleTestSection from "./sections/SampleTestSection";
import ComplianceStandardsSection from "./sections/ComplianceStandardsSection";
import "../order-workflow.scss";

const WORKFLOW_TYPE = "environmental";

const EnvironmentalOrderEnter = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const componentMounted = useRef(true);
  const {
    orderData,
    setOrderData,
    setOrderDataSilent,
    samples,
    setSamples,
    labNumber,
    saveOrder,
    markStepComplete,
    isReadOnly,
    isEditMode,
    resetOrder,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Initialise empty — the sync effect below populates from context only after
  // the mount reset guard has run, preventing stale cross-domain lab numbers
  // from a prior workflow session from pre-filling this field.
  const [localLabNumber, setLocalLabNumber] = useState("");
  const [isGeneratingLabNo, setIsGeneratingLabNo] = useState(false);
  const [printLabelsExpanded, setPrintLabelsExpanded] = useState(false);
  const [errors, setErrors] = useState({});
  const [showNceForm, setShowNceForm] = useState(false);

  // Reset on mount for new orders. Only skip reset when ?order= is present
  // AND the URL path belongs to this workflow.
  useEffect(() => {
    const orderParam = new URLSearchParams(location.search).get("order");
    const pathMatchesWorkflow = location.pathname.startsWith(
      "/order/environmental",
    );
    if (!isEditMode && !(orderParam && pathMatchesWorkflow)) {
      resetOrder();
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Seed workflowType + clear patient status on mount.
  useEffect(() => {
    const current =
      orderData?.sampleOrderItems?.environmentalFields?.workflowType;
    if (current !== WORKFLOW_TYPE) {
      setOrderDataSilent((prev) => ({
        ...prev,
        patientUpdateStatus: "NO_ACTION",
        patientProperties: {
          ...prev.patientProperties,
          patientUpdateStatus: "NO_ACTION",
        },
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

  // Sync local lab number when context changes.
  useEffect(() => {
    const contextLabNo = labNumber || orderData?.sampleOrderItems?.labNo;
    const pathMatchesWorkflow = location.pathname.startsWith(
      "/order/environmental",
    );
    if (!pathMatchesWorkflow) return;
    if (contextLabNo && contextLabNo !== localLabNumber) {
      setLocalLabNumber(contextLabNo);
    } else if (!contextLabNo && localLabNumber) {
      // Context was reset (mismatch wipe) — clear the local field too.
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

  const envFields = orderData?.sampleOrderItems?.environmentalFields || {};
  const hasPatientOrSite = !!(
    envFields.samplingSiteId ||
    envFields.samplingSiteName ||
    envFields.vecCollectionSiteId ||
    envFields.vecCollectionSiteName
  );
  const hasSampleTypes = samples.some((s) => s.sampleTypeId);
  const allSamplesHaveTests = samples
    .filter((s) => s.sampleTypeId)
    .every((s) => (s.tests?.length || 0) + (s.panels?.length || 0) > 0);
  const canSave =
    localLabNumber && hasPatientOrSite && hasSampleTypes && allSamplesHaveTests;
  const canProceed = canSave;

  // Stamp collection date/time on samples that don't already have one.
  // Environmental collects date per-sample in the manifest; fall back to now
  // so the backend always receives a valid collection date.
  const buildStampedSamples = () => {
    const now = new Date();
    const todayIso = now.toISOString().slice(0, 10);
    const currentTime = now.toTimeString().slice(0, 5);
    const stamped = samples.map((s) =>
      s.sampleTypeId
        ? {
            ...s,
            collectionDate: s.collectionDate || todayIso,
            collectionTime: s.collectionTime || currentTime,
            receivedDate: s.receivedDate || todayIso,
            receivedTime: s.receivedTime || currentTime,
          }
        : s,
    );
    setSamples(stamped);
    return stamped;
  };

  const handleSave = async () => {
    if (!canSave) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.save.incomplete",
          defaultMessage:
            "Please add a sampling site, at least one sample type, and at least one test or panel per sample before saving.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    const stamped = buildStampedSamples();
    try {
      await saveOrder(false, false, stamped);
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
    const stamped = buildStampedSamples();
    try {
      await saveOrder(false, false, stamped);
      markStepComplete("enter");
      history.push(
        labNumber
          ? `/order/environmental/label?order=${encodeURIComponent(labNumber)}`
          : "/order/environmental/label",
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
            "Please add a sampling site, at least one sample type, and at least one test or panel per sample before saving.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    const stamped = buildStampedSamples();
    try {
      await saveOrder(true, false, stamped);
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
        <>
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
          {labNumber && (
            <Button
              kind="danger--tertiary"
              size="md"
              renderIcon={Warning}
              onClick={() => setShowNceForm((v) => !v)}
            >
              <FormattedMessage
                id="nce.button.reportNce"
                defaultMessage="Report NCE"
              />
            </Button>
          )}
        </>
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
                    defaultMessage="Labels can be printed here or from Step 2 (Label & Store)."
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

        {/* Sampling Site */}
        <VectorSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          workflowType={WORKFLOW_TYPE}
        />

        {/* Collection Conditions */}
        <CollectionConditionsSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Program Selection */}
        <ProgramSection
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

        {/* Applicable Compliance Standards */}
        <ComplianceStandardsSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
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

        {showNceForm && labNumber && (
          <InlineNceForm
            accessionNumber={labNumber}
            onClose={() => setShowNceForm(false)}
            onSubmitSuccess={() => setShowNceForm(false)}
          />
        )}
      </Stack>
    </OrderWorkflowLayout>
  );
};

export default EnvironmentalOrderEnter;
