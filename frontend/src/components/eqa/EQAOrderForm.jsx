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
  // Both lists have answered; the deep link below waits for that.
  const [listsLoaded, setListsLoaded] = useState(0);
  // Imported consignments waiting for reception; the receipt takes delivery of
  // the one chosen here, which is what tells the provider the panel arrived.
  const [inboundBoxes, setInboundBoxes] = useState([]);

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
      if (componentMounted.current) setListsLoaded((n) => n + 1);
    });

    getFromOpenElisServer("/rest/eqa/cycles/mine", (response) => {
      if (componentMounted.current && Array.isArray(response)) {
        setCycles(response);
      }
      if (componentMounted.current) setListsLoaded((n) => n + 1);
    });

    getFromOpenElisServer(
      "/rest/shipping-box/by-state/IN_TRANSIT",
      (response) => {
        if (componentMounted.current && Array.isArray(response)) {
          setInboundBoxes(response);
        }
      },
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // "Receive panel" on My Cycles deep-links here with the cycle in the query
  // string. Once both lists have answered, preselect that cycle and the
  // enrollment whose programme name matches the cycle's scheme; an explicit
  // enrollmentId in the link wins over the name match. Fields the user has
  // already filled are left alone.
  useEffect(() => {
    if (listsLoaded < 2) {
      return;
    }
    const params = new URLSearchParams(window.location.search);
    const linkedCycleId = params.get("cycleId");
    if (!linkedCycleId) {
      return;
    }
    const cycle = cycles.find((c) => String(c.id) === linkedCycleId);
    if (!cycle) {
      return;
    }
    const linkedEnrollmentId = params.get("enrollmentId");
    const enrollment = linkedEnrollmentId
      ? myPrograms.find((prog) => String(prog.id) === linkedEnrollmentId)
      : myPrograms.find((prog) => prog.programName === cycle.schemeName);
    setOrderFormValues((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev?.sampleOrderItems,
        eqaCycleId: prev?.sampleOrderItems?.eqaCycleId || String(cycle.id),
        eqaProgramId:
          prev?.sampleOrderItems?.eqaProgramId ||
          (enrollment ? String(enrollment.id) : ""),
      },
    }));
  }, [listsLoaded]);

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
  // Consignments already linked to the chosen cycle come first; a box imported
  // before the cycle existed, or for another cycle, stays selectable.
  const consignmentOptions = [...inboundBoxes]
    .sort((a, b) => {
      const aMatch = String(a.eqaCycleId) === String(cycleId) ? 0 : 1;
      const bMatch = String(b.eqaCycleId) === String(cycleId) ? 0 : 1;
      return aMatch - bMatch || String(a.boxId).localeCompare(String(b.boxId));
    })
    .map((box) => ({
      id: box.id,
      boxId: box.boxId,
      label:
        box.boxId +
        (String(box.eqaCycleId) === String(cycleId)
          ? ` · ${intl.formatMessage({ id: "eqa.order.receipt.consignment.thisCycle" })}`
          : "") +
        (box.destinationFacilityName
          ? ` — ${box.destinationFacilityName}`
          : ""),
    }));
  const selectedBox = consignmentOptions.find(
    (box) => String(box.id) === String(sampleOrder.eqaShippingBoxId || ""),
  );

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
                  {receiptOnFile.boxCode && (
                    <>
                      {" "}
                      <FormattedMessage
                        id="eqa.order.receipt.referenceRecorded"
                        values={{ code: receiptOnFile.boxCode }}
                      />
                    </>
                  )}
                </p>
              ) : (
                <Grid>
                  <Column lg={16} md={8} sm={4}>
                    <Select
                      id="eqa-consignment"
                      labelText={intl.formatMessage({
                        id: "eqa.order.receipt.consignment",
                      })}
                      helperText={intl.formatMessage({
                        id: "eqa.order.receipt.consignment.help",
                      })}
                      value={sampleOrder.eqaShippingBoxId || ""}
                      onChange={(e) =>
                        updateField("eqaShippingBoxId", e.target.value)
                      }
                    >
                      <SelectItem
                        value=""
                        text={intl.formatMessage({
                          id: "eqa.order.receipt.consignment.none",
                        })}
                      />
                      {consignmentOptions.map((box) => (
                        <SelectItem
                          key={box.id}
                          value={String(box.id)}
                          text={box.label}
                        />
                      ))}
                    </Select>
                  </Column>
                  {selectedBox && (
                    <Column lg={16} md={8} sm={4}>
                      <TextInput
                        id="eqa-shipment-reference"
                        readOnly
                        labelText={intl.formatMessage({
                          id: "eqa.order.receipt.reference",
                        })}
                        value={selectedBox.boxId}
                      />
                    </Column>
                  )}
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
