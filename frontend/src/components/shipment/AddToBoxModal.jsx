import {
  InlineNotification,
  Loading,
  Modal,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
} from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";

const AddToBoxModal = ({ open, onClose, sample, onSuccess }) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [mode, setMode] = useState("existing"); // "existing" or "new"
  const [availableBoxes, setAvailableBoxes] = useState([]);
  const [selectedBoxId, setSelectedBoxId] = useState("");
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open && sample) {
      fetchAvailableBoxes();
    }
  }, [open, sample]);

  const fetchAvailableBoxes = () => {
    if (!sample?.destinationFacilityId) {
      setError(
        intl.formatMessage({ id: "shipment.error.noDestinationFacility" }),
      );
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/shipping-box/by-facility/${sample.destinationFacilityId}`,
      (response) => {
        if (response) {
          // Filter to only show DRAFT boxes
          const draftBoxes = response.filter((box) => box.state === "DRAFT");
          setAvailableBoxes(draftBoxes);

          if (draftBoxes.length === 0) {
            setMode("new");
          } else if (draftBoxes.length === 1) {
            setSelectedBoxId(draftBoxes[0].id.toString());
          }
        }
        setLoading(false);
      },
      (error) => {
        setError(intl.formatMessage({ id: "shipment.error.fetchBoxes" }));
        setLoading(false);
      },
    );
  };

  const handleSubmit = () => {
    if (mode === "existing" && !selectedBoxId) {
      setError(intl.formatMessage({ id: "shipment.error.selectBox" }));
      return;
    }

    setSubmitting(true);
    setError(null);

    if (mode === "existing") {
      // Assign sample item to existing box using new SampleItem-based API
      postToOpenElisServerFullResponse(
        "/rest/box-sample/items",
        JSON.stringify({
          shippingBoxId: Number.parseInt(selectedBoxId),
          sampleItemId: sample.sampleItemId || sample.id,
        }),
        async (response) => {
          try {
            if (response.ok) {
              addNotification({
                kind: "success",
                title: intl.formatMessage({ id: "notification.success" }),
                message: intl.formatMessage({
                  id: "shipment.notification.sampleAssignedToBox",
                }),
              });

              if (onSuccess) {
                onSuccess();
              }
              onClose();
            } else {
              const errorText = await response.text();
              let errorMessage;
              if (errorText && errorText.includes("already assigned")) {
                errorMessage = intl.formatMessage({
                  id: "shipment.error.sampleAlreadyAssigned",
                });
              } else {
                errorMessage =
                  errorText ||
                  intl.formatMessage({ id: "shipment.error.assignToBox" });
              }

              setError(errorMessage);
              addNotification({
                kind: "error",
                title: intl.formatMessage({ id: "notification.error" }),
                message: errorMessage,
              });
            }
          } catch (error) {
            const errorMessage = intl.formatMessage({
              id: "shipment.error.assignToBox",
            });
            setError(errorMessage);
            addNotification({
              kind: "error",
              title: intl.formatMessage({ id: "notification.error" }),
              message: errorMessage,
            });
          } finally {
            setSubmitting(false);
          }
        },
      );
    } else {
      // Create new box - redirect to box creation page with sample item info
      const facilityParam = sample.destinationFacilityId
        ? `facilityId=${sample.destinationFacilityId}&`
        : "";
      const sampleParam = sample.sampleItemId || sample.id || "";
      window.location.href = `/SampleShipment/create-box?${facilityParam}sampleItemId=${sampleParam}`;
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({ id: "shipment.modal.addToBox.title" })}
      primaryButtonText={
        mode === "new"
          ? intl.formatMessage({ id: "shipment.action.createBox" })
          : intl.formatMessage({ id: "shipment.action.addToBox" })
      }
      secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
      primaryButtonDisabled={
        submitting || loading || (mode === "existing" && !selectedBoxId)
      }
    >
      {loading ? (
        <Loading description={intl.formatMessage({ id: "loading.boxes" })} />
      ) : (
        <>
          {error && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({ id: "notification.error" })}
              subtitle={error}
              lowContrast
              hideCloseButton
            />
          )}

          <div style={{ marginBottom: "1rem" }}>
            <p>
              <strong>
                <FormattedMessage id="sample.label.accessionNumber" />:
              </strong>{" "}
              {sample?.accessionNumber}
            </p>
            {sample?.typeOfSample && (
              <p>
                <strong>
                  <FormattedMessage id="sample.label.typeOfSample" />:
                </strong>{" "}
                {sample.typeOfSample}
              </p>
            )}
            <p>
              <strong>
                <FormattedMessage id="shipment.label.tests" />:
              </strong>{" "}
              {sample?.referralTests
                ? sample.referralTests.map((t) => t.testName).join(", ")
                : "-"}
            </p>
          </div>

          {availableBoxes.length > 0 && (
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "shipment.modal.addToBox.mode",
              })}
              name="box-mode"
              valueSelected={mode}
              onChange={(value) => setMode(value)}
            >
              <RadioButton
                labelText={intl.formatMessage({
                  id: "shipment.modal.addToBox.existingBox",
                })}
                value="existing"
                id="mode-existing"
              />
              <RadioButton
                labelText={intl.formatMessage({
                  id: "shipment.modal.addToBox.newBox",
                })}
                value="new"
                id="mode-new"
              />
            </RadioButtonGroup>
          )}

          {mode === "existing" && availableBoxes.length > 0 && (
            <Select
              id="box-select"
              labelText={intl.formatMessage({
                id: "shipment.modal.addToBox.selectBox",
              })}
              value={selectedBoxId}
              onChange={(e) => setSelectedBoxId(e.target.value)}
              disabled={submitting}
            >
              <SelectItem
                text={intl.formatMessage({ id: "label.select" })}
                value=""
              />
              {availableBoxes.map((box) => (
                <SelectItem
                  key={box.id}
                  text={`${box.boxId} (${box.sampleCount || 0} ${intl.formatMessage({ id: "shipment.label.samples" })})`}
                  value={box.id.toString()}
                />
              ))}
            </Select>
          )}

          {mode === "new" && (
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "shipment.modal.addToBox.newBoxInfo.title",
              })}
              subtitle={intl.formatMessage({
                id: "shipment.modal.addToBox.newBoxInfo.message",
              })}
              lowContrast
              hideCloseButton
            />
          )}

          {availableBoxes.length === 0 && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "shipment.modal.addToBox.noDraftBoxes.title",
              })}
              subtitle={intl.formatMessage({
                id: "shipment.modal.addToBox.noDraftBoxes.message",
              })}
              lowContrast
              hideCloseButton
            />
          )}
        </>
      )}
    </Modal>
  );
};

export default AddToBoxModal;
