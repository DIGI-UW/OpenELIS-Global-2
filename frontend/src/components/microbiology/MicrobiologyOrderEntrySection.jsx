import React, { useEffect, useMemo } from "react";
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
  isReadOnly = false,
}) => {
  const intl = useIntl();
  const cultureTests = useMemo(
    () =>
      samples.flatMap((sample) =>
        (sample.tests || []).filter((test) => test.cultureWorkflowType),
      ),
    [samples],
  );
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

  const methods = useMemo(() => {
    const byId = new Map();
    cultureTests.forEach((test) =>
      (test.methods || []).forEach((method) =>
        byId.set(String(method.methodId), method),
      ),
    );
    return Array.from(byId.values());
  }, [cultureTests]);

  const existingFields = {
    ...emptyMicrobiologyOrderDetail,
    ...orderFormValues.microbiologyOrderDetail,
  };
  const defaultMethod =
    methods.find((method) => method.isDefault) || methods[0];
  const isBloodCulture = samples.some((sample) =>
    sample.sampleTypeName?.toLowerCase().includes("blood"),
  );
  const isCriticalSite = samples.some((sample) =>
    /blood|cerebrospinal|\bcsf\b|sterile/i.test(sample.sampleTypeName || ""),
  );
  const fields = {
    ...existingFields,
    cultureMethodId:
      existingFields.cultureMethodId || defaultMethod?.methodId || "",
    numberOfSets:
      existingFields.numberOfSets === "" ||
      existingFields.numberOfSets === null ||
      existingFields.numberOfSets === undefined
        ? isBloodCulture
          ? 2
          : 1
        : Number(existingFields.numberOfSets),
    antibioticExposure:
      existingFields.antibioticExposure === true ||
      existingFields.antibioticExposure === "true",
    criticalNotificationPreference:
      existingFields.criticalNotificationPreference === null ||
      existingFields.criticalNotificationPreference === ""
        ? isCriticalSite
        : existingFields.criticalNotificationPreference === true ||
          existingFields.criticalNotificationPreference === "true",
  };

  useEffect(() => {
    if (workflows.length === 0) {
      return;
    }
    const current = orderFormValues.microbiologyOrderDetail || {};
    const changed = Object.entries(fields).some(
      ([key, value]) => current[key] !== value,
    );
    if (changed) {
      setOrderFormValues((previous) => ({
        ...previous,
        microbiologyOrderDetail: fields,
      }));
    }
  }, [
    fields.antibioticExposure,
    fields.clinicalHistory,
    fields.criticalNotificationPreference,
    fields.cultureMethodId,
    fields.numberOfSets,
    fields.patientOrigin,
    setOrderFormValues,
    workflows.length,
  ]);

  if (workflows.length === 0) {
    return null;
  }

  const updateField = (name, value) => {
    setOrderFormValues((previous) => ({
      ...previous,
      microbiologyOrderDetail: {
        ...fields,
        [name]: value,
      },
    }));
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
        methods={methods}
        idPrefix="microbiology-order-entry"
        isReadOnly={isReadOnly}
      />
    </Layer>
  );
};

export default MicrobiologyOrderEntrySection;
