import React, { useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Grid,
  Column,
  Checkbox,
  Select,
  SelectItem,
  TextInput,
  InlineNotification,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import CustomDatePicker from "../../../common/CustomDatePicker";

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
 */
const EqaAndNoPatientSection = ({
  orderData,
  setOrderData,
  isReadOnly,
  patientRequired,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const [programs, setPrograms] = useState([]);

  const sampleOrderItems = orderData?.sampleOrderItems || {};
  const isEqa = Boolean(sampleOrderItems.isEQASample);
  const noPatient = Boolean(sampleOrderItems.noPatientOverride);
  const hasPatient = Boolean(
    orderData?.patientProperties?.lastName ||
    orderData?.patientProperties?.nationalId,
  );

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/eqa/programs", (data) => {
      if (componentMounted.current && Array.isArray(data)) {
        setPrograms(data);
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

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
      updateOrder({
        isEQASample: true,
        noPatientOverride: true,
        noPatientReasonCode: "EQA",
        noPatientReason: intl.formatMessage({
          id: "order.noPatient.reason.eqa",
        }),
      });
      return;
    }
    updateOrder({
      isEQASample: false,
      noPatientOverride: false,
      noPatientReasonCode: "",
      noPatientReason: "",
      eqaProgramId: "",
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
  const offerManualOverride = !patientRequired || isEqa;
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
            checked={noPatient}
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

      {noPatient && hasPatient && (
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
          <Column lg={8} md={4} sm={4}>
            <Select
              id="eqaProgramId"
              labelText={intl.formatMessage({
                id: "order.eqa.program",
                defaultMessage: "EQA Programme",
              })}
              value={sampleOrderItems.eqaProgramId || ""}
              onChange={(e) => updateOrder({ eqaProgramId: e.target.value })}
              disabled={isReadOnly}
            >
              <SelectItem value="" text="" />
              {programs.map((program) => (
                <SelectItem
                  key={program.id}
                  value={String(program.id)}
                  text={
                    program.name || program.programName || String(program.id)
                  }
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="eqaProviderSampleId"
              labelText={intl.formatMessage({
                id: "order.eqa.providerSampleId",
                defaultMessage: "Provider Sample ID",
              })}
              value={sampleOrderItems.eqaProviderSampleId || ""}
              onChange={(e) =>
                updateOrder({ eqaProviderSampleId: e.target.value })
              }
              disabled={isReadOnly}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="eqaDeadline"
              labelText={intl.formatMessage({
                id: "order.eqa.deadline",
                defaultMessage: "Submission Deadline",
              })}
              value={sampleOrderItems.eqaDeadline || ""}
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
    </Tile>
  );
};

export default EqaAndNoPatientSection;
