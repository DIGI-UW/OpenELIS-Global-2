import React, { useContext, useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Link as RouterLink } from "react-router-dom";
import {
  Tile,
  Grid,
  Column,
  Checkbox,
  Select,
  SelectItem,
  TextInput,
  InlineNotification,
  Button,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import CustomDatePicker from "../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../layout/Layout";
import { createSampleOrderFormValues } from "../../../formModel/innitialValues/OrderEntryFormValues";
import NewEqaCycleModal from "../../../eqa/MyCycles/NewEqaCycleModal";
import { formatIsoDateForBackend } from "../../dateUtils";

const EQA_PRIORITIES = ["STANDARD", "URGENT"];

/**
 * The two adjacent decisions an order can carry: that it is an EQA
 * proficiency sample, and that it has no patient.
 *
 * EQA used to be a checkbox on the legacy screen that satisfied the
 * patient gate by fabricating one shared sentinel patient — "NULL NULL",
 * male, born 1900 — so every EQA order in the system pointed at the same
 * 126-year-old man and was evaluated against his reference range. Making
 * "no patient" a real, recorded state lets EQA declare it honestly.
 *
 * EQA is orthogonal to domain: a proficiency panel on a water sample is
 * perfectly coherent, so this is a control on the shared form rather than a
 * fourth lane.
 *
 * An EQA sample names the lab's active enrollment it belongs to, and the
 * cycle when the lab tracks it here (OGC-1483). The cycle is optional, since
 * some labs track cycles in other software, but an order without one is not
 * submitted or scored from OpenELIS, and the section says so.
 */
const EqaAndNoPatientSection = ({
  orderData,
  setOrderData,
  isReadOnly,
  patientRequired,
  fieldErrors = {},
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE || "en-US";
  const [enrollments, setEnrollments] = useState(null);
  const [cycles, setCycles] = useState([]);
  const [schemeTouched, setSchemeTouched] = useState(false);
  const [newCycleOpen, setNewCycleOpen] = useState(false);

  const sampleOrderItems = orderData?.sampleOrderItems || {};
  const isEqa = Boolean(sampleOrderItems.isEQASample);
  const noPatient = Boolean(sampleOrderItems.noPatientOverride);
  const hasPatient = Boolean(
    orderData?.patientProperties?.lastName ||
    orderData?.patientProperties?.nationalId,
  );

  const enrollmentId = sampleOrderItems.eqaProgramId || "";
  const cycleId = sampleOrderItems.eqaCycleId || "";
  const selectedEnrollment = (enrollments || []).find(
    (enrollment) => String(enrollment.id) === String(enrollmentId),
  );
  const selectedCycle = cycles.find(
    (cycle) => String(cycle.id) === String(cycleId),
  );
  const panelCodes = selectedCycle?.panelCodes || [];
  const noEnrollments = enrollments !== null && enrollments.length === 0;
  const providerSampleId = sampleOrderItems.eqaProviderSampleId || "";
  const codeMatchesNoPanelSample =
    panelCodes.length > 0 &&
    providerSampleId.trim() !== "" &&
    !panelCodes.includes(providerSampleId.trim());

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/eqa/my-programs?active=true", (data) => {
      if (componentMounted.current) {
        setEnrollments(Array.isArray(data) ? data : []);
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadCycles = (id, then) => {
    if (!id) {
      setCycles([]);
      return;
    }
    getFromOpenElisServer(
      `/rest/eqa/my-programs/${encodeURIComponent(id)}/open-cycles`,
      (data) => {
        if (!componentMounted.current) return;
        const list = Array.isArray(data) ? data : [];
        setCycles(list);
        then?.(list);
      },
    );
  };

  useEffect(() => {
    loadCycles(isEqa ? enrollmentId : "");
  }, [isEqa, enrollmentId]); // eslint-disable-line react-hooks/exhaustive-deps

  const updateOrder = (changes) =>
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, ...changes },
    }));

  // Ticking EQA sets no-patient by default, because for a proficiency sample
  // the absence of a patient is normal rather than exceptional. Unticking
  // withdraws both, along with the EQA detail, so a stale programme cannot
  // outlive the flag that revealed it.
  const handleEqaChange = (_event, { checked }) => {
    if (checked) {
      setOrderData((prev) => ({
        ...prev,
        patientProperties: {
          ...createSampleOrderFormValues().patientProperties,
          patientUpdateStatus: "NO_ACTION",
        },
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          isEQASample: true,
          noPatientOverride: true,
          noPatientReasonCode: "EQA",
          noPatientReason: intl.formatMessage({
            id: "order.noPatient.reason.eqa",
          }),
        },
      }));
      return;
    }
    updateOrder({
      isEQASample: false,
      noPatientOverride: false,
      noPatientReasonCode: "",
      noPatientReason: "",
      eqaProgramId: "",
      eqaCycleId: "",
      eqaProviderSampleId: "",
      eqaDeadline: "",
      eqaPriority: "",
    });
  };

  // Allowed to be unticked under EQA: some schemes do use identified
  // specimens, so EQA sets no-patient by default rather than locking it.
  const handleNoPatientChange = (_event, { checked }) => {
    updateOrder({
      noPatientOverride: checked,
      noPatientReasonCode: checked ? (isEqa ? "EQA" : "MANUAL") : "",
      noPatientReason: checked
        ? isEqa
          ? intl.formatMessage({ id: "order.noPatient.reason.eqa" })
          : sampleOrderItems.noPatientReason || ""
        : "",
    });
  };

  // PatientRequired governs the manual override only. A site that requires
  // patients for clinical work still runs proficiency testing, so EQA can set
  // no-patient even where the manual tick is not on offer.
  const offerManualOverride = !patientRequired && !isEqa;
  const warnAboutNoPatient = noPatient && !isEqa;

  return (
    <Tile className="order-section eqa-no-patient-section">
      <h4 className="section-title">
        <FormattedMessage
          id="order.sampleHandling"
          defaultMessage="Sample Handling"
        />
      </h4>

      <Grid>
        <Column lg={8} md={4} sm={4}>
          <Checkbox
            id="isEQASample"
            labelText={intl.formatMessage({
              id: "order.eqaSample",
              defaultMessage: "External quality assessment (EQA) sample",
            })}
            checked={isEqa}
            onChange={handleEqaChange}
            disabled={isReadOnly}
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <Checkbox
            id="noPatientOverride"
            labelText={intl.formatMessage({
              id: "order.noPatient",
              defaultMessage: "This order has no patient",
            })}
            checked={noPatient || isEqa}
            onChange={handleNoPatientChange}
            disabled={isReadOnly || !offerManualOverride}
          />
        </Column>
      </Grid>

      {warnAboutNoPatient && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "order.noPatient.warning.title",
            defaultMessage:
              "Results will not be evaluated against a reference range",
          })}
          subtitle={intl.formatMessage({
            id: "order.noPatient.warning.subtitle",
            defaultMessage:
              "Without a patient there is no age or sex to select a reference range from, so results on this order will not be flagged normal or abnormal.",
          })}
        />
      )}

      {noPatient && !isEqa && (
        <TextInput
          id="noPatientReason"
          labelText={intl.formatMessage({
            id: "order.noPatient.reason",
            defaultMessage: "Reason for ordering without a patient",
          })}
          value={sampleOrderItems.noPatientReason || ""}
          onChange={(e) => updateOrder({ noPatientReason: e.target.value })}
          disabled={isReadOnly}
        />
      )}

      {noPatient && hasPatient && !isEqa && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "order.noPatient.butPatientChosen",
            defaultMessage:
              "A patient is attached to this order, so the no-patient decision will not be recorded.",
          })}
        />
      )}

      {isEqa && (
        <Grid className="eqa-detail-grid">
          {noEnrollments && (
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "order.eqa.noEnrollments.title",
                })}
              />
              <RouterLink to="/qa/eqa/my-programs">
                <FormattedMessage id="order.eqa.noEnrollments.link" />
              </RouterLink>
            </Column>
          )}
          {!noEnrollments && (
            <Column lg={8} md={4} sm={4}>
              <Select
                id="eqaProgramId"
                labelText={intl.formatMessage({ id: "order.eqa.scheme" })}
                aria-required="true"
                value={enrollmentId}
                invalid={Boolean(
                  fieldErrors["sampleOrderItems.eqaProgramId"] ||
                  (schemeTouched && !enrollmentId),
                )}
                invalidText={intl.formatMessage({
                  id:
                    fieldErrors["sampleOrderItems.eqaProgramId"] &&
                    intl.messages[fieldErrors["sampleOrderItems.eqaProgramId"]]
                      ? fieldErrors["sampleOrderItems.eqaProgramId"]
                      : "order.eqa.schemeRequired",
                })}
                onBlur={() => setSchemeTouched(true)}
                onChange={(e) => {
                  setSchemeTouched(true);
                  updateOrder({
                    eqaProgramId: e.target.value,
                    eqaCycleId: "",
                    ...(selectedCycle?.deadline ? { eqaDeadline: "" } : {}),
                  });
                }}
                disabled={isReadOnly}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "order.eqa.scheme.choose" })}
                />
                {(enrollments || []).map((enrollment) => (
                  <SelectItem
                    key={enrollment.id}
                    value={String(enrollment.id)}
                    text={
                      enrollment.provider
                        ? `${enrollment.programName} (${enrollment.provider})`
                        : enrollment.programName || String(enrollment.id)
                    }
                  />
                ))}
              </Select>
            </Column>
          )}
          {!noEnrollments && (
            <Column lg={8} md={4} sm={4}>
              <Select
                id="eqaCycleId"
                labelText={intl.formatMessage({ id: "order.eqa.cycle" })}
                value={cycleId}
                invalid={Boolean(fieldErrors["sampleOrderItems.eqaCycleId"])}
                invalidText={intl.formatMessage({
                  id: "order.eqa.cycleNotInScheme",
                })}
                onChange={(e) => {
                  const cycle = cycles.find(
                    (candidate) => String(candidate.id) === e.target.value,
                  );
                  updateOrder({
                    eqaCycleId: e.target.value,
                    eqaDeadline: cycle?.deadline
                      ? formatIsoDateForBackend(cycle.deadline, dateLocale)
                      : selectedCycle?.deadline
                        ? ""
                        : sampleOrderItems.eqaDeadline || "",
                  });
                }}
                disabled={isReadOnly || !enrollmentId}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "order.eqa.cycle.none" })}
                />
                {cycles.map((cycle) => (
                  <SelectItem
                    key={cycle.id}
                    value={String(cycle.id)}
                    text={cycle.name}
                  />
                ))}
              </Select>
            </Column>
          )}
          {enrollmentId && !cycleId && (
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: "order.eqa.noCycle.warning",
                })}
              />
              {!isReadOnly && (
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => setNewCycleOpen(true)}
                >
                  <FormattedMessage id="order.eqa.newCycle" />
                </Button>
              )}
            </Column>
          )}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="eqaProviderSampleId"
              labelText={intl.formatMessage({
                id: "order.eqa.providerSampleId",
                defaultMessage: "Provider Sample ID",
              })}
              list={panelCodes.length > 0 ? "eqaPanelCodes" : undefined}
              value={providerSampleId}
              warn={codeMatchesNoPanelSample}
              warnText={intl.formatMessage(
                { id: "order.eqa.providerSampleId.noMatch" },
                { codes: panelCodes.join(", ") },
              )}
              helperText={
                panelCodes.length > 0
                  ? intl.formatMessage(
                      { id: "order.eqa.providerSampleId.codes" },
                      { codes: panelCodes.join(", ") },
                    )
                  : undefined
              }
              onChange={(e) =>
                updateOrder({ eqaProviderSampleId: e.target.value })
              }
              disabled={isReadOnly}
            />
            {panelCodes.length > 0 && (
              <datalist id="eqaPanelCodes">
                {panelCodes.map((code) => (
                  <option key={code} value={code} />
                ))}
              </datalist>
            )}
          </Column>
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="eqaDeadline"
              labelText={intl.formatMessage({
                id: "order.eqa.deadline",
                defaultMessage: "Submission Deadline",
              })}
              value={sampleOrderItems.eqaDeadline || ""}
              updateStateValue={true}
              onChange={(date) => updateOrder({ eqaDeadline: date })}
              disabled={isReadOnly}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="eqaPriority"
              labelText={intl.formatMessage({
                id: "order.eqa.priority",
                defaultMessage: "EQA Priority",
              })}
              value={sampleOrderItems.eqaPriority || "STANDARD"}
              onChange={(e) => updateOrder({ eqaPriority: e.target.value })}
              disabled={isReadOnly}
            >
              {EQA_PRIORITIES.map((priority) => (
                <SelectItem key={priority} value={priority} text={priority} />
              ))}
            </Select>
          </Column>
        </Grid>
      )}

      {newCycleOpen && (
        <NewEqaCycleModal
          schemeName={selectedEnrollment?.programName || ""}
          onClose={() => setNewCycleOpen(false)}
          onCreated={(created) => {
            setNewCycleOpen(false);
            loadCycles(enrollmentId, (list) => {
              const match = list.find(
                (cycle) => String(cycle.id) === String(created?.id),
              );
              if (match) {
                updateOrder({
                  eqaCycleId: String(match.id),
                  ...(match.deadline
                    ? {
                        eqaDeadline: formatIsoDateForBackend(
                          match.deadline,
                          dateLocale,
                        ),
                      }
                    : {}),
                });
              }
            });
          }}
        />
      )}
    </Tile>
  );
};

export default EqaAndNoPatientSection;
