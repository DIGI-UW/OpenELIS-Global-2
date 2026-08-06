import React, { useEffect, useMemo, useState } from "react";
import { InlineNotification, Layer, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import MicrobiologyOrderDetailFields, {
  emptyMicrobiologyOrderDetail,
} from "./MicrobiologyOrderDetailFields";
import { getPatientOrigins } from "./MicrobiologyService";

const MicrobiologyOrderEntrySection = ({
  samples = [],
  orderFormValues,
  setOrderFormValues,
  enabled,
  isReadOnly = false,
}) => {
  const intl = useIntl();
  const [patientOriginOptions, setPatientOriginOptions] = useState([]);
  const [defaultPatientOrigin, setDefaultPatientOrigin] = useState("");
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
  const selectedTests = useMemo(
    () => samples.flatMap((sample) => sample.tests || []),
    [samples],
  );
  const routedWorkflows = workflows.length > 0 ? workflows : ["UNASSIGNED"];
  const sectionEnabled = enabled ?? workflows.length > 0;
  const requestingOrganizationId =
    orderFormValues.sampleOrderItems?.referringSiteDepartmentId ||
    orderFormValues.sampleOrderItems?.referringSiteId ||
    "";
  const methodSourceTests =
    cultureTests.length > 0 ? cultureTests : selectedTests;

  const methods = useMemo(() => {
    const byId = new Map();
    methodSourceTests.forEach((test) =>
      (test.methods || []).forEach((method) =>
        byId.set(String(method.methodId), method),
      ),
    );
    return Array.from(byId.values());
  }, [methodSourceTests]);

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
    patientOrigin: existingFields.patientOrigin || defaultPatientOrigin || "",
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
    if (!sectionEnabled) {
      return undefined;
    }
    let active = true;
    getPatientOrigins(requestingOrganizationId).then((response) => {
      if (!active) {
        return;
      }
      setPatientOriginOptions(response?.options || []);
      setDefaultPatientOrigin(response?.defaultCode || "");
    });
    return () => {
      active = false;
    };
  }, [requestingOrganizationId, sectionEnabled]);

  useEffect(() => {
    if (!sectionEnabled) {
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
    sectionEnabled,
  ]);

  if (!sectionEnabled) {
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
          {routedWorkflows.map((workflow) => (
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
        patientOrigins={patientOriginOptions}
        idPrefix="microbiology-order-entry"
        isReadOnly={isReadOnly}
      />
    </Layer>
  );
};

export default MicrobiologyOrderEntrySection;
