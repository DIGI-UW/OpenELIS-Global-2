import React, { useState } from "react";
import { Button, NumberInput, TextArea, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import MicrobiologyService from "./MicrobiologyService";

const emptyDetail = {
  patientOrigin: "",
  numberOfSets: "",
  clinicalHistory: "",
  antibioticExposure: "",
  criticalNotificationPreference: "",
};

const OrderDetailPanel = ({
  caseId,
  orderDetail,
  service = MicrobiologyService,
  onSaved,
}) => {
  const intl = useIntl();
  const [fields, setFields] = useState({ ...emptyDetail, ...orderDetail });
  const [saving, setSaving] = useState(false);

  const setField = (name) => (value) =>
    setFields((current) => ({ ...current, [name]: value }));

  const save = () => {
    setSaving(true);
    const payload = {
      ...fields,
      numberOfSets:
        fields.numberOfSets === "" ? null : Number(fields.numberOfSets),
    };
    service.saveOrderDetail(caseId, payload).then((detail) => {
      setSaving(false);
      if (detail && detail.orderDetail) {
        setFields({ ...emptyDetail, ...detail.orderDetail });
      }
      if (onSaved) {
        onSaved(detail);
      }
    });
  };

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-order-detail-card"
      aria-labelledby="microbiology-order-detail-heading"
    >
      <div className="microbiology-card__header">
        <div>
          <h3 id="microbiology-order-detail-heading">
            {intl.formatMessage({ id: "microbiology.orderDetail.title" })}
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.orderDetail.hint" })}
          </p>
        </div>
      </div>
      <div className="microbiology-card__body">
        <div className="microbiology-form-grid">
          <TextInput
            id="microbiology-order-detail-patient-origin"
            labelText={intl.formatMessage({
              id: "microbiology.orderDetail.patientOrigin",
            })}
            value={fields.patientOrigin}
            onChange={(event) => setField("patientOrigin")(event.target.value)}
          />
          <NumberInput
            id="microbiology-order-detail-number-of-sets"
            label={intl.formatMessage({
              id: "microbiology.orderDetail.numberOfSets",
            })}
            value={fields.numberOfSets}
            min={0}
            allowEmpty
            onChange={(event) => setField("numberOfSets")(event.target.value)}
          />
          <div className="microbiology-form-grid__wide">
            <TextArea
              id="microbiology-order-detail-clinical-history"
              labelText={intl.formatMessage({
                id: "microbiology.orderDetail.clinicalHistory",
              })}
              value={fields.clinicalHistory}
              onChange={(event) =>
                setField("clinicalHistory")(event.target.value)
              }
            />
          </div>
          <div className="microbiology-form-grid__wide">
            <TextArea
              id="microbiology-order-detail-antibiotic-exposure"
              labelText={intl.formatMessage({
                id: "microbiology.orderDetail.antibioticExposure",
              })}
              value={fields.antibioticExposure}
              onChange={(event) =>
                setField("antibioticExposure")(event.target.value)
              }
            />
          </div>
          <TextInput
            id="microbiology-order-detail-critical-notification-preference"
            labelText={intl.formatMessage({
              id: "microbiology.orderDetail.criticalNotificationPreference",
            })}
            value={fields.criticalNotificationPreference}
            onChange={(event) =>
              setField("criticalNotificationPreference")(event.target.value)
            }
          />
          <div>
            <Button onClick={save} disabled={saving}>
              {intl.formatMessage({ id: "microbiology.orderDetail.save" })}
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
};

export default OrderDetailPanel;
