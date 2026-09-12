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
import { Printer } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import SaveFailureNotice from "../SaveFailureNotice";
import { useOrderContext } from "../OrderContext";
import { useNewOrderReset } from "../useNewOrderReset";
import { describeUnmetRequirements } from "../saveRequirements";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import LabNumberField from "./sections/LabNumberField";
import EqaAndNoPatientSection from "./sections/EqaAndNoPatientSection";
import OrderAttachmentsSection from "./sections/OrderAttachmentsSection";
import PatientSearchSection from "./sections/PatientSearchSection";
import ProgramSection from "./sections/ProgramSection";
import ClinicalInfoSection from "./sections/ClinicalInfoSection";
import RequesterSection from "./sections/RequesterSection";
import SampleTestSection from "./sections/SampleTestSection";
import "../order-workflow.scss";

const WORKFLOW_TYPE = "clinical";
const WORKFLOW_PREFIX = "/order/clinical";

const ClinicalOrderEnter = () => {
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

  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const patientRequired = configurationProperties.PatientRequired !== "false";
  const siteRequired =
    configurationProperties.SampleEntryReferralSiteNameRequired === "true";
  const providerRequired =
    configurationProperties.REQUESTER_REQUIRED === "true";

  const isNewOrder = useNewOrderReset(WORKFLOW_PREFIX);

  // Initialise empty — populated by the sync effect below after the mount
  // reset runs, preventing stale cross-domain lab numbers from bleeding in.
  const [localLabNumber, setLocalLabNumber] = useState("");
  const [printLabelsExpanded, setPrintLabelsExpanded] = useState(false);
  const [errors, setErrors] = useState({});
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });

  // A caller can pre-set the EQA control, which is what makes the override a
  // recorded decision rather than something only a human click can produce:
  // the EQA worklist links straight in here with it already on.
  useEffect(() => {
    if (!isNewOrder) {
      return;
    }
    if (new URLSearchParams(location.search).get("eqa") !== "true") {
      return;
    }
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        isEQASample: true,
        noPatientOverride: true,
        noPatientReasonCode: "EQA",
      },
    }));
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

  const hasPatient = !!(
    orderData?.patientProperties?.lastName ||
    orderData?.patientProperties?.nationalId
  );
  // A recorded decision, not a silent fallthrough: an order may go without a
  // patient when the user (or EQA) has said so and why.
  const noPatientOverride = Boolean(
    orderData?.sampleOrderItems?.noPatientOverride,
  );
  const hasSampleTypes = samples.some((s) => s.sampleTypeId);
  const hasProvider = Boolean(
    orderData?.sampleOrderItems?.providerPersonId ||
    orderData?.sampleOrderItems?.providerId,
  );
  // These settings have always existed; the lanes just never read them.
  // SampleEntryReferralSiteNameRequired marks the field — that is all it has
  // ever done, in the legacy screen it was written for — so it drives the
  // asterisk that regressed, not a save gate. REQUESTER_REQUIRED is a real
  // validation gate in the legacy flow, and PatientRequired is honoured with
  // the recorded no-patient override as its escape hatch.
  const saveRequirements = [
    {
      met: Boolean(localLabNumber),
      labelId: "order.save.requirement.labNumber",
    },
    {
      met: hasPatient || noPatientOverride || !patientRequired,
      labelId: "order.save.requirement.patient",
    },
    {
      met: hasProvider || !providerRequired,
      labelId: "order.save.requirement.provider",
    },
    { met: hasSampleTypes, labelId: "order.save.requirement.sampleType" },
  ];
  const canSave = saveRequirements.every((requirement) => requirement.met);

  const canProceed =
    canSave &&
    Object.values(phoneValidation).every((item) => item.status !== false);

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

  const handleSaveAndNext = async () => {
    if (!canSave) return;
    try {
      await saveOrderEntry();
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

        {/* AL and W: the two adjacent decisions — EQA, and no patient. */}
        <EqaAndNoPatientSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          patientRequired={patientRequired}
        />

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
          samples={samples}
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
          siteRequired={siteRequired}
          providerRequired={providerRequired}
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

export default ClinicalOrderEnter;
