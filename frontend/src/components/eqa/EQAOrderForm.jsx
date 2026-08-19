import React, { useEffect, useRef, useState } from "react";
import {
  Checkbox,
  Column,
  Grid,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import CustomDatePicker from "../common/CustomDatePicker";

const EQAOrderForm = ({ orderFormValues, setOrderFormValues }) => {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [myPrograms, setMyPrograms] = useState([]);
  const [cycles, setCycles] = useState([]);
  const [existingReceipt, setExistingReceipt] = useState(null);

  const sampleOrder = orderFormValues?.sampleOrderItems || {};
  const cycleId = sampleOrder.eqaCycleId || "";
  const enrollmentId = sampleOrder.eqaProgramId || "";

  useEffect(() => {
    componentMounted.current = true;

    // Fetch self-enrolled programmes (My Programs)
    getFromOpenElisServer("/rest/eqa/my-programs", (response) => {
      if (componentMounted.current && Array.isArray(response)) {
        setMyPrograms(response);
      }
    });

    getFromOpenElisServer("/rest/eqa/cycles/mine", (response) => {
      if (componentMounted.current && Array.isArray(response)) {
        setCycles(response);
      }
    });

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // A receipt already on file makes this a read-only view: saving again is a
  // no-op server-side, so re-offering the fields would only mislead.
  useEffect(() => {
    if (!cycleId || !enrollmentId) {
      return;
    }
    getFromOpenElisServer(
      `/rest/eqa/cycles/${cycleId}/receipt?labEnrollmentId=${enrollmentId}`,
      (response) => {
        if (componentMounted.current) {
          setExistingReceipt(response?.id ? response : null);
        }
      },
    );
  }, [cycleId, enrollmentId]);

  // Ignore a receipt still held from a previously picked cycle.
  const receiptOnFile =
    existingReceipt && String(existingReceipt.cycleId) === String(cycleId)
      ? existingReceipt
      : null;

  const updateField = (field, value) => {
    setOrderFormValues((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        [field]: value,
      },
    }));
  };

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="eqa.order.form.title" />
          </h3>

          <Grid>
            {/* Programme — from My Programs (self-enrollments) */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="eqa-program"
                labelText={intl.formatMessage({
                  id: "eqa.order.programme",
                })}
                value={sampleOrder.eqaProgramId || ""}
                onChange={(e) => updateField("eqaProgramId", e.target.value)}
              >
                <SelectItem value="" text="" />
                {myPrograms.map((prog) => (
                  <SelectItem
                    key={prog.id}
                    value={String(prog.id)}
                    text={prog.programName || prog.name}
                  />
                ))}
              </Select>
            </Column>

            {/* Provider Sample ID */}
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="eqa-provider-sample-id"
                labelText={intl.formatMessage({
                  id: "eqa.order.providerSampleId",
                })}
                value={sampleOrder.eqaProviderSampleId || ""}
                onChange={(e) =>
                  updateField("eqaProviderSampleId", e.target.value)
                }
              />
            </Column>

            {/* Result Deadline */}
            <Column lg={8} md={4} sm={4}>
              <CustomDatePicker
                id="eqa-deadline"
                labelText={intl.formatMessage({
                  id: "eqa.order.deadline",
                })}
                value={sampleOrder.eqaDeadline || ""}
                onChange={(date) => updateField("eqaDeadline", date)}
              />
            </Column>

            {/* EQA Priority */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="eqa-priority"
                labelText={intl.formatMessage({
                  id: "eqa.order.priority",
                })}
                value={sampleOrder.eqaPriority || "STANDARD"}
                onChange={(e) => updateField("eqaPriority", e.target.value)}
              >
                <SelectItem value="STANDARD" text="Standard" />
                <SelectItem value="URGENT" text="Urgent" />
                <SelectItem value="CRITICAL" text="Critical" />
              </Select>
            </Column>

            {/* Cycle — optional; uncycled orders stay visible in My Cycles */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="eqa-cycle"
                labelText={intl.formatMessage({ id: "eqa.order.cycle" })}
                value={cycleId}
                onChange={(e) => updateField("eqaCycleId", e.target.value)}
              >
                <SelectItem value="" text="" />
                {cycles.map((cycle) => (
                  <SelectItem
                    key={cycle.id}
                    value={String(cycle.id)}
                    text={cycle.cycleName || `#${cycle.cycleNumber}`}
                  />
                ))}
              </Select>
            </Column>
          </Grid>

          {cycleId && enrollmentId && (
            <>
              <h4>
                <FormattedMessage id="eqa.order.receipt.title" />
              </h4>
              {receiptOnFile ? (
                <p>
                  <FormattedMessage
                    id="eqa.order.receipt.recorded"
                    values={{ date: receiptOnFile.receivedDate }}
                  />
                </p>
              ) : (
                <Grid>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="eqa-received-temp"
                      type="number"
                      labelText={intl.formatMessage({
                        id: "eqa.order.receipt.tempC",
                      })}
                      value={sampleOrder.eqaReceivedTempC || ""}
                      onChange={(e) =>
                        updateField("eqaReceivedTempC", e.target.value)
                      }
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Checkbox
                      id="eqa-integrity-ok"
                      labelText={intl.formatMessage({
                        id: "eqa.order.receipt.integrityOk",
                      })}
                      checked={sampleOrder.eqaIntegrityOk !== false}
                      onChange={(e, { checked }) =>
                        updateField("eqaIntegrityOk", checked)
                      }
                    />
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <TextInput
                      id="eqa-integrity-notes"
                      labelText={intl.formatMessage({
                        id: "eqa.order.receipt.notes",
                      })}
                      value={sampleOrder.eqaIntegrityNotes || ""}
                      onChange={(e) =>
                        updateField("eqaIntegrityNotes", e.target.value)
                      }
                    />
                  </Column>
                </Grid>
              )}
            </>
          )}
        </div>
      </Column>
    </Grid>
  );
};

export default EQAOrderForm;
