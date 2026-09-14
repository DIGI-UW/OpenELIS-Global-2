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
import RequesterSection from "./sections/RequesterSection";
import ProgramSection from "./sections/ProgramSection";
import SampleTestSection from "./sections/SampleTestSection";
import "../order-workflow.scss";

const WORKFLOW_TYPE = "vector";
const WORKFLOW_PREFIX = "/order/vector";

const VectorOrderEnter = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const {
    orderData,
    setOrderData,
    samples,
    setSamples,
    labNumber,
    saveOrderEntry,
    isSubmitting,
    fieldErrors,
    markStepComplete,
    isReadOnly,
    isEditMode,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const isNewOrder = useNewOrderReset(WORKFLOW_PREFIX);

  // Initialise empty — populated by the sync effect below after the mount
  // reset runs, preventing stale cross-domain lab numbers from bleeding in.
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
  const hasCollectionSite = !!(
    envFields.vecCollectionSiteId ||
    envFields.vecCollectionSiteName ||
    envFields.vecOrganismGroupId
  );
  const hasSampleTypes = samples.some((s) => s.sampleTypeId);
  const saveRequirements = [
    {
      met: Boolean(localLabNumber),
      labelId: "order.save.requirement.labNumber",
    },
    {
      met: hasCollectionSite,
      labelId: "order.save.requirement.collectionSite",
    },
    { met: hasSampleTypes, labelId: "order.save.requirement.sampleType" },
  ];
  const canSave = saveRequirements.every((requirement) => requirement.met);
  const canProceed = canSave;

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
    try {
      await saveOrderEntry();
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

  // Vector skips Collect — goes directly to Label.
  const handleSaveAndNext = async () => {
    if (!canSave) return;
    try {
      await saveOrderEntry();
      markStepComplete("enter");
      history.push(
        labNumber
          ? `/order/vector/label?order=${encodeURIComponent(labNumber)}`
          : "/order/vector/label",
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
    try {
      await saveOrderEntry();
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
            disabled={isSubmitting || !canSave}
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

        {/* Vector Collection Site */}
        <VectorSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />

        <RequesterSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          workflowType={WORKFLOW_TYPE}
        />

        {/* Program Selection */}
        <ProgramSection
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

export default VectorOrderEnter;
