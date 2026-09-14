import React, { useContext, useState, useEffect, useCallback } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Stack,
  Button,
  Tile,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Printer, Warning } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import SaveFailureNotice from "../SaveFailureNotice";
import InlineNceForm from "../../nonconform/common/InlineNceForm";
import { useOrderContext } from "../OrderContext";
import { useNewOrderReset } from "../useNewOrderReset";
import { describeUnmetRequirements } from "../saveRequirements";
import { NotificationContext } from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import LabNumberField from "./sections/LabNumberField";
import OrderAttachmentsSection from "./sections/OrderAttachmentsSection";
import VectorSection from "./sections/VectorSection";
import CollectionConditionsSection from "./sections/CollectionConditionsSection";
import ProgramSection from "./sections/ProgramSection";
import RequesterSection from "./sections/RequesterSection";
import SampleTestSection from "./sections/SampleTestSection";
import ComplianceStandardsSection from "./sections/ComplianceStandardsSection";
import { currentLocalTime, todayLocalIso } from "../dateUtils";
import "../order-workflow.scss";

const WORKFLOW_TYPE = "environmental";
const WORKFLOW_PREFIX = "/order/environmental";

const EnvironmentalOrderEnter = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const {
    orderData,
    setOrderData,
    samples,
    setSamples,
    labNumber,
    saveOrder,
    fieldErrors,
    markStepComplete,
    isReadOnly,
    isEditMode,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const isNewOrder = useNewOrderReset(WORKFLOW_PREFIX);

  // Initialise empty — the sync effect below populates from context only after
  // the mount reset has run, preventing stale cross-domain lab numbers from a
  // prior workflow session from pre-filling this field.
  const [localLabNumber, setLocalLabNumber] = useState("");
  const [printLabelsExpanded, setPrintLabelsExpanded] = useState(false);
  const [errors, setErrors] = useState({});
  const [showNceForm, setShowNceForm] = useState(false);

  // Seed workflowType + clear patient status on mount.
  useEffect(() => {
    const current =
      orderData?.sampleOrderItems?.environmentalFields?.workflowType;
    if (current !== WORKFLOW_TYPE) {
      setOrderData((prev) => ({
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
    const pathMatchesWorkflow = location.pathname.startsWith(WORKFLOW_PREFIX);
    if (!pathMatchesWorkflow) return;
    if (contextLabNo && contextLabNo !== localLabNumber) {
      setLocalLabNumber(contextLabNo);
    } else if (!contextLabNo && localLabNumber) {
      // Context was reset (mismatch wipe) — clear the local field too.
      setLocalLabNumber("");
    }
  }, [labNumber, orderData?.sampleOrderItems?.labNo, location.pathname]);

  const handleLabNumberChange = useCallback(
    (newLabNo) => {
      setLocalLabNumber(newLabNo);
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          labNo: newLabNo,
        },
      }));
    },
    [setOrderData],
  );

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
  const saveRequirements = [
    {
      met: Boolean(localLabNumber),
      labelId: "order.save.requirement.labNumber",
    },
    { met: hasPatientOrSite, labelId: "order.save.requirement.samplingSite" },
    { met: hasSampleTypes, labelId: "order.save.requirement.sampleType" },
    {
      met: hasSampleTypes && allSamplesHaveTests,
      labelId: "order.save.requirement.testsPerSample",
    },
  ];
  const canSave = saveRequirements.every((requirement) => requirement.met);
  const canProceed = canSave;

  // Stamp collection date/time on samples that don't already have one.
  // Environmental collects date per-sample in the manifest; fall back to now
  // so the backend always receives a valid collection date.
  const buildStampedSamples = () => {
    const now = new Date();
    const todayIso = todayLocalIso(now);
    const currentTime = currentLocalTime(now);
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
        message: describeUnmetRequirements(intl, saveRequirements),
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
        message: describeUnmetRequirements(intl, saveRequirements),
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
      <SaveFailureNotice inlineFields={["sampleOrderItems.labNo"]} />

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
              <LabNumberField
                value={localLabNumber}
                onLabNumberChange={handleLabNumberChange}
                disabled={isReadOnly && !isEditMode}
                autoGenerate={isNewOrder}
                invalid={Boolean(fieldErrors?.["sampleOrderItems.labNo"])}
                invalidText={fieldErrors?.["sampleOrderItems.labNo"]}
              />
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
        {/* T: order attachments existed on the legacy screen with an
            unchanged REST API; only the new lanes had no way in. */}
        <OrderAttachmentsSection
          labNumber={localLabNumber}
          isReadOnly={isReadOnly && !isEditMode}
        />
      </Stack>
    </OrderWorkflowLayout>
  );
};

export default EnvironmentalOrderEnter;
