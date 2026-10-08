import React, { useContext, useState, useEffect, useCallback } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import { Grid, Column, Stack, Tile } from "@carbon/react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import SaveFailureNotice, { saveFailureMessage } from "../SaveFailureNotice";
import { useOrderContext } from "../OrderContext";
import { useNewOrderReset } from "../useNewOrderReset";
import { describeUnmetRequirements } from "../saveRequirements";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
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
import MicroOrderPreview from "./sections/MicroOrderPreview";
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
    seedOrderData,
    samples,
    setSamples,
    labNumber,
    saveOrderEntry,
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
    seedOrderData((prev) => ({
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
      seedOrderData((prev) => ({
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

  // A generated lab number is a default the form set for itself, not a change
  // the user made, so it does not mark the order dirty.
  const handleLabNumberChange = useCallback(
    (newLabNo, { generated = false } = {}) => {
      setLocalLabNumber(newLabNo);
      (generated ? seedOrderData : setOrderData)((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          labNo: newLabNo,
        },
      }));
    },
    [setOrderData, seedOrderData],
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
  // Two levels of required (FR-A7, FR-B13). Save and exit needs the save
  // level: a lab number, a patient (or the recorded no-patient decision) and
  // a sample type. Save and next needs the complete level as well: the
  // provider where the deployment requires one. The site setting only marks
  // the field (OGC-1201 K: no server validation reads it), so it never holds
  // the step. Each missing item is listed in the To continue checklist with a
  // link to its field.
  const saveRequirements = [
    {
      met: Boolean(localLabNumber),
      labelId: "order.save.requirement.labNumber",
      itemId: "order.continue.item.labNumber",
      targetId: "labNumber",
    },
    {
      met: hasPatient || noPatientOverride || !patientRequired,
      labelId: "order.save.requirement.patient",
      itemId: "order.continue.item.patient",
      targetId: "order-patient-search-lastName",
    },
    {
      met: hasSampleTypes,
      labelId: "order.save.requirement.sampleType",
      itemId: "order.continue.item.sampleType",
      targetId: "sampleType-0",
    },
  ];
  const completeRequirements = [
    {
      met: hasProvider || !providerRequired,
      labelId: "order.save.requirement.provider",
      itemId: "order.continue.item.provider",
      targetId: "providerName",
    },
  ];
  const canSave = saveRequirements.every((requirement) => requirement.met);
  const toContinue = [...saveRequirements, ...completeRequirements]
    .filter((requirement) => !requirement.met)
    .map((requirement) => ({
      id: requirement.itemId,
      label: intl.formatMessage({ id: requirement.itemId }),
      targetId: requirement.targetId,
    }));

  const canProceed =
    canSave &&
    completeRequirements.every((requirement) => requirement.met) &&
    Object.values(phoneValidation).every((item) => item.status !== false);

  const handleSave = async () => {
    if (!canSave) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: describeUnmetRequirements(intl, saveRequirements),
      });
      setNotificationVisible(true);
      return false;
    }
    try {
      await saveOrderEntry();
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
      return true;
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: saveFailureMessage(intl, error),
      });
      setNotificationVisible(true);
      return false;
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
        message: saveFailureMessage(intl, error),
      });
      setNotificationVisible(true);
    }
  };

  return (
    <OrderWorkflowLayout
      title="order.step.enter"
      canProceed={canProceed}
      canSave={canSave}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
      toContinue={toContinue}
    >
      {notificationVisible && <AlertDialog />}
      <SaveFailureNotice inlineFields={["sampleOrderItems.labNo"]} />

      <Stack gap={7}>
        {/* 1. Order: the lab number, with the EQA and no-patient decisions
            (FR-B1, FR-B3). The dead Print Labels accordion is gone; labels
            print from Prepare Samples. */}
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
        </Tile>

        {/* AL and W: the two adjacent decisions — EQA, and no patient. */}
        <EqaAndNoPatientSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          patientRequired={patientRequired}
        />

        {/* 2. Patient */}
        <PatientSearchSection
          orderData={orderData}
          setOrderData={setOrderData}
          setPhoneValidation={setPhoneValidation}
          isReadOnly={isReadOnly && !isEditMode}
          required={patientRequired && !noPatientOverride}
        />

        {/* 3. Requester, before the request details, as on the paper form */}
        <RequesterSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          workflowType={WORKFLOW_TYPE}
          siteRequired={siteRequired}
          providerRequired={providerRequired}
        />

        {/* 4. Request details: program, then clinical information */}
        <ProgramSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          domain="CLINICAL"
        />

        <ClinicalInfoSection
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* 5. Tests */}
        <SampleTestSection
          samples={samples}
          setSamples={setSamples}
          orderData={orderData}
          setOrderData={setOrderData}
          isReadOnly={isReadOnly && !isEditMode}
          workflowType={WORKFLOW_TYPE}
        />
        <MicroOrderPreview
          samples={samples}
          savedOrder={isReadOnly || isEditMode}
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
