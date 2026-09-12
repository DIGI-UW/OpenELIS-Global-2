import React, {
  useContext,
  useState,
  useEffect,
  useCallback,
  useMemo,
} from "react";
import { useHistory } from "react-router-dom";
import { useWorkflowPrefix } from "../OrderContext";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Accordion,
  AccordionItem,
  StructuredListWrapper,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Tag,
  Loading,
  Button,
} from "@carbon/react";
import { Checkmark, Warning } from "@carbon/icons-react";
import InlineNceForm from "../../nonconform/common/InlineNceForm";
import SampleAcceptanceReview from "./sections/SampleAcceptanceReview";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import SaveFailureNotice from "../SaveFailureNotice";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import { getAcceptanceGate, getEnforcement } from "../api/sampleAcceptanceApi";

/**
 * OrderQA - Step 4: QA Review
 *
 * Final quality assurance review before order submission. Shows the complete
 * order summary and the per-specimen Sample Acceptance Checklist, which is
 * the single acceptance review for the order; submission is gated by that
 * checklist's per-domain enforcement.
 */

const OrderQA = () => {
  const intl = useIntl();
  const history = useHistory();
  const workflowPrefix = useWorkflowPrefix();
  const {
    orderId,
    orderData,
    samples,
    resetOrder,
    labNumber,
    markStepComplete,
  } = useOrderContext();

  const workflowType =
    orderData?.sampleOrderItems?.environmentalFields?.workflowType ||
    "clinical";
  const isVectorWorkflow = workflowType === "vector";

  const fallbackSampleName = intl.formatMessage({
    id: "sample.fallback.name",
    defaultMessage: "Sample",
  });

  const poolGroups = useMemo(() => {
    if (!isVectorWorkflow) return [];
    const visible = (samples || []).filter((s) => !s?.voided);
    const groups = new Map();
    visible.forEach((sample, index) => {
      // Prefer the stable vectorPoolId — two pools of the same animal must
      // not collapse into one group. Fall back to sampleTypeId only for
      // pre-fan-out specimens that don't yet have a pool id.
      const key =
        sample?.vectorPoolId ||
        sample?.typeOfSampleId ||
        sample?.sampleTypeId ||
        `unknown-${index}`;
      if (!groups.has(key)) {
        groups.set(key, {
          key,
          name: sample?.sampleTypeName || sample?.name || fallbackSampleName,
          specimens: [],
        });
      }
      groups.get(key).specimens.push(sample);
    });
    return Array.from(groups.values()).map((g) => {
      const withTests = g.specimens.find(
        (s) =>
          (s.panels && s.panels.length > 0) || (s.tests && s.tests.length > 0),
      );
      const testSource = withTests || g.specimens[0] || {};
      return {
        ...g,
        count: g.specimens.length,
        panels: testSource.panels || [],
        tests: testSource.tests || [],
      };
    });
  }, [samples, isVectorWorkflow, fallbackSampleName]);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [isSubmitted, setIsSubmitted] = useState(false);
  const [showNceForm, setShowNceForm] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  // S-09 FR-08: true while any live specimen's intake acceptance is unsatisfied
  // under MANDATORY enforcement — gates the QA submit (server /gate is the backstop).
  const [acceptanceBlocked, setAcceptanceBlocked] = useState(false);
  // S-09 FR-08: when this order's domain enforcement is OFF, the whole Intake
  // Acceptance section is hidden (only the existing NCE button remains) and the
  // acceptance gate never applies. Default false → fail open (section shown).
  const [acceptanceOff, setAcceptanceOff] = useState(false);

  useEffect(() => {
    let active = true;
    getEnforcement().then((modes) => {
      if (!active) return;
      const off = (modes?.[workflowType] || "").toUpperCase() === "OFF";
      setAcceptanceOff(off);
      // The section is hidden under OFF, so its gate must never apply.
      if (off) setAcceptanceBlocked(false);
    });
    return () => {
      active = false;
    };
  }, [workflowType]);

  const displayLabNumber =
    labNumber || orderData?.sampleOrderItems?.labNo || "";

  // Records that the order reached and passed QA review. The dashboard reads
  // the presence of this record to mark the order complete, so it is written
  // even though intake acceptance — not a second checklist — is what is
  // actually reviewed on this step.
  const recordQaReview = useCallback(async () => {
    if (!displayLabNumber) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/qa-checklist",
        JSON.stringify({ labNumber: displayLabNumber }),
        (response) => {
          if (response && response.success) {
            resolve(response);
          } else {
            reject(new Error(response?.error || "Failed to record QA review"));
          }
        },
      );
    });
  }, [displayLabNumber]);

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await recordQaReview();
      markStepComplete("qa");
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      console.error("Error recording QA review:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    } finally {
      setIsSaving(false);
    }
  };

  const handleSubmit = async () => {
    // S-09 FR-08: intake acceptance is gated per specimen here in QA. Block while
    // the client sees an unsatisfied specimen, then confirm with the server /gate
    // (authoritative — it skips voided/rejected). A network error must not block.
    if (acceptanceBlocked) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "sampleAcceptance.gate.blocked",
          defaultMessage:
            "Intake acceptance is mandatory for this domain — accept every specimen (or report an NCE / resample) before this order can proceed.",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    setIsSaving(true);
    // Skip the acceptance gate entirely when the domain enforcement is OFF.
    if (orderId && !acceptanceOff) {
      let gate = { blocked: false };
      try {
        gate = await getAcceptanceGate(orderId);
      } catch (gateError) {
        console.error("Error checking acceptance gate:", gateError);
      }
      if (gate.blocked) {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "sampleAcceptance.gate.blocked",
            defaultMessage:
              "Intake acceptance is mandatory for this domain — accept every specimen (or report an NCE / resample) before this order can proceed.",
          }),
        });
        setNotificationVisible(true);
        setIsSaving(false);
        return;
      }
    }
    try {
      await recordQaReview();
      markStepComplete("qa");
      setIsSubmitted(true);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.submitted.success.msg",
          defaultMessage: "Order submitted successfully",
        }),
      });
      setNotificationVisible(true);
      if (workflowPrefix === "/order/vector") {
        const target = displayLabNumber
          ? `${workflowPrefix}/complete?labNumber=${encodeURIComponent(displayLabNumber)}`
          : `${workflowPrefix}/complete`;
        history.push(target);
      }
    } catch (error) {
      console.error("Error submitting order:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    } finally {
      setIsSaving(false);
    }
  };

  const handleStartNewOrder = () => {
    resetOrder();
    history.push(`${workflowPrefix}/enter`);
  };

  const patientName = orderData?.patientProperties
    ? `${orderData.patientProperties.firstName || ""} ${orderData.patientProperties.lastName || ""}`.trim()
    : "---";

  const isEnvOrVector =
    workflowType === "environmental" || workflowType === "vector";

  if (isSubmitted) {
    return (
      <OrderWorkflowLayout title="order.step.qa" showSaveButtons={false}>
        <Tile className="qa-success-tile">
          <div className="success-content">
            <Checkmark size={48} className="success-icon" />
            <h3>
              <FormattedMessage
                id="order.submit.success"
                defaultMessage="Order Submitted Successfully"
              />
            </h3>
            <p>
              <FormattedMessage
                id="order.submit.labNumber"
                defaultMessage="Lab Number: {labNumber}"
                values={{ labNumber: displayLabNumber || "---" }}
              />
            </p>
            <button
              className="cds--btn cds--btn--primary"
              onClick={handleStartNewOrder}
            >
              <FormattedMessage
                id="order.start.new"
                defaultMessage="Start New Order"
              />
            </button>
          </div>
        </Tile>
      </OrderWorkflowLayout>
    );
  }

  return (
    <OrderWorkflowLayout
      title="order.step.qa"
      canProceed={!acceptanceBlocked}
      onSave={handleSave}
      onSaveAndNext={handleSubmit}
      extraButtons={
        displayLabNumber && (
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
        )
      }
    >
      {notificationVisible && <AlertDialog />}
      <SaveFailureNotice />
      {isSaving && <Loading withOverlay description="Saving..." />}

      <div className="qa-review-container">
        {/* S-09 (OGC-580) Intake Acceptance — per-specimen master/detail table.
            Acceptance is recorded per sample_item; shared across Clinical /
            Environmental / Vector via the domain-resolved checklist. Hidden
            entirely when this order's domain enforcement is OFF (FR-08).
            This is the order's only acceptance checklist: the separate
            built-in QA checklist that used to render below it was an
            unguarded duplicate with its own state, config load and POST. */}
        {!acceptanceOff && (
          <SampleAcceptanceReview
            orderId={orderId}
            labNumber={displayLabNumber}
            samples={samples}
            onBlockedChange={setAcceptanceBlocked}
          />
        )}

        {/* Order Summary */}
        <Accordion>
          {isEnvOrVector ? (
            <AccordionItem
              title={intl.formatMessage({
                id: "qa.summary.samplingSite",
                defaultMessage: "Sampling Site",
              })}
              open
            >
              {(() => {
                const ef =
                  orderData?.sampleOrderItems?.environmentalFields || {};
                const rows = [
                  {
                    labelId: "vector.admin.samplingSite.name",
                    defaultMsg: "Site Name",
                    value: ef.samplingSiteName || ef.vecCollectionSiteName,
                  },
                  {
                    labelId: "vector.admin.samplingSite.code",
                    defaultMsg: "Code",
                    value: ef.samplingSiteCode || ef.vecCollectionSiteCode,
                  },
                  {
                    labelId: "vector.admin.samplingSite.type",
                    defaultMsg: "Site Type",
                    value: ef.siteType || ef.vecCollectionSiteType,
                  },
                  {
                    labelId: "vector.admin.samplingSite.region",
                    defaultMsg: "Subtype",
                    value: ef.siteSubtype || ef.vecCollectionSiteSubtype,
                  },
                  {
                    labelId: "vector.admin.samplingSite.environmentalZone",
                    defaultMsg: "Environmental Zone",
                    value: ef.environmentalZone || ef.vecCollectionSiteZone,
                  },
                  {
                    labelId: "vector.admin.samplingSite.contactName",
                    defaultMsg: "Contact Person",
                    value:
                      ef.samplingSiteContact || ef.vecCollectionSiteContact,
                  },
                  {
                    labelId: "vector.admin.samplingSite.contactPhone",
                    defaultMsg: "Contact Phone",
                    value: ef.samplingSitePhone || ef.vecCollectionSitePhone,
                  },
                  {
                    labelId: "vector.admin.samplingSite.gpsLatitude",
                    defaultMsg: "GPS",
                    value: (() => {
                      const lat = ef.samplingSiteGpsLat || ef.vecGpsLatitude;
                      const lon = ef.samplingSiteGpsLon || ef.vecGpsLongitude;
                      return lat && lon ? `${lat}, ${lon}` : lat || lon || null;
                    })(),
                  },
                  {
                    labelId: "vector.admin.samplingSite.description",
                    defaultMsg: "Description",
                    value:
                      ef.samplingSiteDesc || ef.vecCollectionSiteDescription,
                  },
                ].filter((r) => r.value);
                return (
                  <StructuredListWrapper isCondensed>
                    <StructuredListBody>
                      {rows.length > 0 ? (
                        rows.map((r) => (
                          <StructuredListRow key={r.labelId}>
                            <StructuredListCell>
                              <FormattedMessage
                                id={r.labelId}
                                defaultMessage={r.defaultMsg}
                              />
                            </StructuredListCell>
                            <StructuredListCell>{r.value}</StructuredListCell>
                          </StructuredListRow>
                        ))
                      ) : (
                        <StructuredListRow>
                          <StructuredListCell>---</StructuredListCell>
                        </StructuredListRow>
                      )}
                    </StructuredListBody>
                  </StructuredListWrapper>
                );
              })()}
            </AccordionItem>
          ) : (
            <AccordionItem
              title={intl.formatMessage({
                id: "qa.summary.patient",
                defaultMessage: "Patient Information",
              })}
              open
            >
              <StructuredListWrapper isCondensed>
                <StructuredListBody>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage id="order.summary.patientName" />
                    </StructuredListCell>
                    <StructuredListCell>{patientName}</StructuredListCell>
                  </StructuredListRow>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage
                        id="patient.dob"
                        defaultMessage="Date of Birth"
                      />
                    </StructuredListCell>
                    <StructuredListCell>
                      {orderData?.patientProperties?.birthDateForDisplay ||
                        "---"}
                    </StructuredListCell>
                  </StructuredListRow>
                  <StructuredListRow>
                    <StructuredListCell>
                      <FormattedMessage
                        id="patient.gender"
                        defaultMessage="Gender"
                      />
                    </StructuredListCell>
                    <StructuredListCell>
                      {orderData?.patientProperties?.gender || "---"}
                    </StructuredListCell>
                  </StructuredListRow>
                </StructuredListBody>
              </StructuredListWrapper>
            </AccordionItem>
          )}

          <AccordionItem
            title={intl.formatMessage({
              id: "qa.summary.samples",
              defaultMessage: "Samples & Tests",
            })}
            open
          >
            {isVectorWorkflow && poolGroups.length > 0 ? (
              // Each pool is its own collapsible row inside a nested
              // Accordion. Closed by default so a multi-pool order doesn't
              // open the page on a wall of tests; reviewer expands a pool to
              // verify its specific test configuration.
              <Accordion size="sm" className="qa-pool-accordion">
                {poolGroups.map((pool, index) => {
                  const panelNames = (pool.panels || []).map((p) => p.name);
                  const panelTestIds = new Set(
                    (pool.panels || []).flatMap((p) =>
                      p.testIds
                        ? p.testIds.split(",").map((id) => id.trim())
                        : [],
                    ),
                  );
                  const standaloneTests = (pool.tests || []).filter(
                    (t) => !panelTestIds.has(String(t.id)),
                  );
                  const poolTitle = intl.formatMessage(
                    {
                      id: "qa.summary.poolOf",
                      defaultMessage: "Pool of {count} {animal}",
                    },
                    { count: pool.count, animal: pool.name },
                  );
                  return (
                    <AccordionItem key={pool.key || index} title={poolTitle}>
                      <ul className="qa-test-list">
                        {(pool.panels || []).map((p, i) => {
                          const memberIdSet = new Set(
                            p.testIds
                              ? p.testIds
                                  .split(",")
                                  .map((id) => id.trim())
                                  .filter(Boolean)
                              : [],
                          );
                          const memberTests = (pool.tests || []).filter((t) =>
                            memberIdSet.has(String(t.id)),
                          );
                          return (
                            <React.Fragment key={`panel-${i}`}>
                              <li
                                style={{
                                  display: "flex",
                                  alignItems: "baseline",
                                  gap: "0.375rem",
                                }}
                              >
                                <Tag
                                  type="green"
                                  size="sm"
                                  style={{ flexShrink: 0 }}
                                >
                                  <FormattedMessage
                                    id="qa.summary.panel"
                                    defaultMessage="Panel"
                                  />
                                </Tag>
                                <span>{p.name}</span>
                              </li>
                              {memberTests.map((t, j) => (
                                <li
                                  key={`panel-${i}-test-${j}`}
                                  style={{
                                    paddingLeft: "1.5rem",
                                    color: "var(--cds-text-secondary, #525252)",
                                    fontSize: "0.8125rem",
                                  }}
                                >
                                  ↳ {t.name}
                                </li>
                              ))}
                            </React.Fragment>
                          );
                        })}
                        {standaloneTests.map((test, testIndex) => (
                          <li key={`standalone-${testIndex}`}>{test.name}</li>
                        ))}
                        {(pool.panels || []).length === 0 &&
                          standaloneTests.length === 0 && (
                            <li>
                              <p className="qa-no-tests">
                                <FormattedMessage
                                  id="qa.summary.noTests"
                                  defaultMessage="No tests selected"
                                />
                              </p>
                            </li>
                          )}
                      </ul>
                    </AccordionItem>
                  );
                })}
              </Accordion>
            ) : samples && samples.length > 0 ? (
              samples.map((sample, index) => {
                const panelNames = (sample.panels || []).map((p) => p.name);
                const panelTestIds = new Set(
                  (sample.panels || []).flatMap((p) =>
                    p.testIds
                      ? p.testIds.split(",").map((id) => id.trim())
                      : [],
                  ),
                );
                const standaloneTests = (sample.tests || []).filter(
                  (t) => !panelTestIds.has(String(t.id)),
                );
                const qcType = sample.qcMetadata?.qcType;
                const qcTagType =
                  qcType === "BLANK"
                    ? "blue"
                    : qcType === "DUPLICATE"
                      ? "teal"
                      : qcType === "CONTROL"
                        ? "purple"
                        : null;
                return (
                  <div key={index} className="qa-sample-item">
                    <Tag type="blue" size="sm">
                      {sample.name ||
                        sample.sampleTypeName ||
                        `Sample ${index + 1}`}
                    </Tag>
                    {qcType && (
                      <Tag type={qcTagType} size="sm">
                        <FormattedMessage
                          id={`qc.type.${qcType.toLowerCase()}`}
                          defaultMessage={`QC: ${qcType}`}
                        />
                      </Tag>
                    )}
                    {panelNames.length > 0 && (
                      <ul className="qa-test-list">
                        {panelNames.map((name, i) => (
                          <li key={`panel-${i}`}>
                            <Tag type="green" size="sm">
                              <FormattedMessage
                                id="qa.summary.panel"
                                defaultMessage="Panel"
                              />
                            </Tag>{" "}
                            {name}
                          </li>
                        ))}
                      </ul>
                    )}
                    {standaloneTests.length > 0 && (
                      <ul className="qa-test-list">
                        {standaloneTests.map((test, testIndex) => (
                          <li key={testIndex}>{test.name}</li>
                        ))}
                      </ul>
                    )}
                    {panelNames.length === 0 &&
                      standaloneTests.length === 0 && (
                        <p className="qa-no-tests">
                          <FormattedMessage
                            id="qa.summary.noTests"
                            defaultMessage="No tests selected"
                          />
                        </p>
                      )}
                  </div>
                );
              })
            ) : (
              <p>
                <FormattedMessage
                  id="qa.summary.noSamples"
                  defaultMessage="No samples added"
                />
              </p>
            )}
          </AccordionItem>

          <AccordionItem
            title={intl.formatMessage({
              id: "qa.summary.order",
              defaultMessage: "Order Details",
            })}
          >
            <StructuredListWrapper isCondensed>
              <StructuredListBody>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="order.summary.accessionNumber" />
                  </StructuredListCell>
                  <StructuredListCell>
                    {displayLabNumber || "---"}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage
                      id="sample.collection.date"
                      defaultMessage="Collection Date"
                    />
                  </StructuredListCell>
                  <StructuredListCell>
                    {samples?.[0]?.sampleXML?.collectionDate ||
                      orderData?.sampleOrderItems?.collectionDate ||
                      "---"}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage
                      id="order.requester"
                      defaultMessage="Requester"
                    />
                  </StructuredListCell>
                  <StructuredListCell>
                    {orderData?.sampleOrderItems?.providerFirstName || ""}{" "}
                    {orderData?.sampleOrderItems?.providerLastName || "---"}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListBody>
            </StructuredListWrapper>
          </AccordionItem>
        </Accordion>

        {showNceForm && displayLabNumber && (
          <InlineNceForm
            accessionNumber={displayLabNumber}
            onClose={() => setShowNceForm(false)}
            onSubmitSuccess={() => setShowNceForm(false)}
          />
        )}
      </div>
    </OrderWorkflowLayout>
  );
};

export default OrderQA;
