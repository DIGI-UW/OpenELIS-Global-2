import React, { useEffect, useState, useRef } from "react";
import { Button, InlineNotification, Stack, Tile } from "@carbon/react";
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
  isReadOnly = false,
  showCulturePurpose = true,
  requiredFields = [],
}) => {
  const intl = useIntl();
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  const [fields, setFields] = useState({
    ...emptyMicrobiologyOrderDetail,
    ...Object.fromEntries(
      Object.entries(orderDetail || {}).filter(([, value]) => value != null),
    ),
  });
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [patientOrigins, setPatientOrigins] = useState([]);

  useEffect(() => {
    let active = true;
    if (!service.getPatientOrigins) {
      return undefined;
    }
    service
      .getPatientOrigins()
      .then((response) => {
        if (active) {
          setPatientOrigins(response?.options || []);
        }
      })
      .catch(() => {
        if (active) setError(true);
      });
    return () => {
      active = false;
    };
  }, [service]);

  const setField = (name) => (value) =>
    setFields((current) => ({ ...current, [name]: value }));

  const save = () => {
    setSaving(true);
    setError(false);
    const payload = {
      cultureMethodId: fields.cultureMethodId,
      culturePurpose: fields.culturePurpose,
      patientOrigin: fields.patientOrigin,
      admissionDate: fields.admissionDate || null,
      clinicalHistory: fields.clinicalHistory,
      antibioticExposure: fields.antibioticExposure,
      numberOfSets:
        fields.numberOfSets === "" ? null : Number(fields.numberOfSets),
    };
    service
      .saveOrderDetail(caseId, payload)
      .then((detail) => {
        if (!mounted.current) return;
        if (
          !detail ||
          detail.error ||
          (typeof detail.status === "number" &&
            (detail.status === 0 || detail.status >= 400))
        )
          throw new Error("save failed");
        if (detail && detail.orderDetail) {
          setFields({
            ...emptyMicrobiologyOrderDetail,
            ...Object.fromEntries(
              Object.entries(detail.orderDetail).filter(
                ([, value]) => value != null,
              ),
            ),
          });
        }
        if (onSaved) {
          onSaved(detail);
        }
      })
      .catch(() => {
        if (mounted.current) setError(true);
      })
      .finally(() => {
        if (mounted.current) setSaving(false);
      });
  };

  return (
    <Tile data-testid="microbiology-order-detail-card">
      <Stack gap={4}>
        <div>
          <div>
            <h3 id="microbiology-order-detail-heading">
              {intl.formatMessage({ id: "microbiology.orderDetail.title" })}
            </h3>
            <p>{intl.formatMessage({ id: "microbiology.orderDetail.hint" })}</p>
          </div>
        </div>
        <Stack gap={4}>
          <MicrobiologyOrderDetailFields
            fields={fields}
            onChange={(name, value) => setField(name)(value)}
            showCultureMethod={false}
            showCulturePurpose={showCulturePurpose}
            patientOrigins={patientOrigins}
            isReadOnly={isReadOnly || saving}
          />
          {requiredFields.length > 0 && (
            <p>
              {intl.formatMessage(
                { id: "microbiology.case.requiredBeforeFinal" },
                {
                  fields: requiredFields
                    .map((field) =>
                      intl.formatMessage({
                        id: `microbiology.m6.field.${field}`,
                      }),
                    )
                    .join(", "),
                },
              )}
            </p>
          )}
          {error && (
            <InlineNotification
              kind="error"
              hideCloseButton
              title={intl.formatMessage({
                id: "microbiology.case.contextSaveError",
              })}
            />
          )}
          {!isReadOnly && (
            <div>
              <Button onClick={save} disabled={saving}>
                {intl.formatMessage({ id: "microbiology.orderDetail.save" })}
              </Button>
            </div>
          )}
        </Stack>
      </Stack>
    </Tile>
  );
};

export default OrderDetailPanel;
