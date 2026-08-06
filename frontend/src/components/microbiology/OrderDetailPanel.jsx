import React, { useState } from "react";
import { Button } from "@carbon/react";
import { useIntl } from "react-intl";
import MicrobiologyService from "./MicrobiologyService";
import MicrobiologyOrderDetailFields, {
  emptyMicrobiologyOrderDetail,
} from "./MicrobiologyOrderDetailFields";

const OrderDetailPanel = ({
  caseId,
  orderDetail,
  service = MicrobiologyService,
  onSaved,
}) => {
  const intl = useIntl();
  const [fields, setFields] = useState({
    ...emptyMicrobiologyOrderDetail,
    ...orderDetail,
  });
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
        setFields({
          ...emptyMicrobiologyOrderDetail,
          ...detail.orderDetail,
        });
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
        <MicrobiologyOrderDetailFields
          fields={fields}
          onChange={(name, value) => setField(name)(value)}
          showCultureMethod={false}
        />
        <div>
          <Button onClick={save} disabled={saving}>
            {intl.formatMessage({ id: "microbiology.orderDetail.save" })}
          </Button>
        </div>
      </div>
    </section>
  );
};

export default OrderDetailPanel;
