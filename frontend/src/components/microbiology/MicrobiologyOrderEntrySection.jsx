import React, { useMemo } from "react";
import { InlineNotification, Layer, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import MicrobiologyOrderDetailFields, {
  emptyMicrobiologyOrderDetail,
} from "./MicrobiologyOrderDetailFields";

const MicrobiologyOrderEntrySection = ({
  samples = [],
  orderFormValues,
  setOrderFormValues,
}) => {
  const intl = useIntl();
  const workflows = useMemo(
    () =>
      Array.from(
        new Set(
          samples.flatMap((sample) =>
            (sample.tests || [])
              .map((test) => test.cultureWorkflowType)
              .filter(Boolean),
          ),
        ),
      ),
    [samples],
  );

  if (workflows.length === 0) {
    return null;
  }

  const fields = {
    ...emptyMicrobiologyOrderDetail,
    ...orderFormValues.microbiologyOrderDetail,
  };
  const updateField = (name, value) => {
    setOrderFormValues({
      ...orderFormValues,
      microbiologyOrderDetail: {
        ...fields,
        [name]: value,
      },
    });
  };

  return (
    <Layer
      className="microbiology-card"
      data-testid="microbiology-order-entry-section"
    >
      <div className="microbiology-card__header">
        <div>
          <h3>{intl.formatMessage({ id: "microbiology.orderEntry.title" })}</h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.orderEntry.hint" })}
          </p>
        </div>
        <div>
          {workflows.map((workflow) => (
            <Tag key={workflow} type="blue">
              {formatMicrobiologyEnum(workflow)}
            </Tag>
          ))}
        </div>
      </div>
      <InlineNotification
        kind="info"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({
          id: "microbiology.orderEntry.routingTitle",
        })}
        subtitle={intl.formatMessage({
          id: "microbiology.orderEntry.routingMessage",
        })}
      />
      <MicrobiologyOrderDetailFields
        fields={fields}
        onChange={updateField}
        idPrefix="microbiology-order-entry"
      />
    </Layer>
  );
};

export default MicrobiologyOrderEntrySection;
