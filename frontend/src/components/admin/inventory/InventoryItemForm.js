import React, { useState, useEffect, useContext } from "react";
import {
  Modal,
  Button,
  Form,
  FormGroup,
  TextInput,
  Select,
  SelectItem,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  postToOpenElisServerFullResponse,
  getFromOpenElisServer,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import CustomDatePicker from "../../common/CustomDatePicker.js";

function InventoryItemForm({
  isOpen,
  onClose,
  onSave,
  item,
  kitTypes = [],
  sources = [],
}) {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [formData, setFormData] = useState({
    kitName: "",
    type: "",
    receiveDate: "",
    expirationDate: "",
    lotNumber: "",
    organizationId: "",
    isActive: true,
  });

  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (item) {
      // Editing existing item
      setFormData({
        kitName: item.kitName || "",
        type: item.type || "",
        receiveDate: item.receiveDate || "",
        expirationDate: item.expirationDate || "",
        lotNumber: item.lotNumber || "",
        organizationId: item.organizationId || "",
        isActive: item.isActive !== undefined ? item.isActive : true,
      });
    } else {
      // Creating new item
      setFormData({
        kitName: "",
        type: "",
        receiveDate: "",
        expirationDate: "",
        lotNumber: "",
        organizationId: "",
        isActive: true,
      });
    }
    setErrors({});
    setLoading(false);
    setIsSubmitting(false);
  }, [item, isOpen]);

  const handleChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
    // Clear error for this field
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const validate = () => {
    const newErrors = {};

    if (!formData.kitName || formData.kitName.trim() === "") {
      newErrors.kitName = intl.formatMessage({
        id: "inventory.error.kitName.required",
      });
    }

    if (!formData.organizationId || formData.organizationId === "") {
      newErrors.organizationId = intl.formatMessage({
        id: "inventory.error.source.required",
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = () => {
    if (!validate()) {
      return;
    }

    if (isSubmitting) {
      return; // Prevent double submission
    }

    setIsSubmitting(true);
    setLoading(true);

    const url = item ? `/rest/inventory/${item.id}` : "/rest/inventory";
    const method = item ? "PUT" : "POST";

    postToOpenElisServerFullResponse(
      url,
      JSON.stringify(formData),
      (res) => {
        setLoading(false);
        setIsSubmitting(false);
        if (res && (res.status === 200 || res.status === 201)) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: item
              ? intl.formatMessage({ id: "inventory.update.success" })
              : intl.formatMessage({ id: "inventory.create.success" }),
          });
          setNotificationVisible(true);
          onSave();
        } else {
          const errorMessage =
            res?.data?.error || intl.formatMessage({ id: "server.error.msg" });
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: errorMessage,
          });
          setNotificationVisible(true);
        }
      },
      method,
    );
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      open={isOpen}
      modalHeading={
        item
          ? intl.formatMessage({ id: "inventory.edit.item" })
          : intl.formatMessage({ id: "inventory.testKit.add" })
      }
      primaryButtonText={
        loading
          ? intl.formatMessage({ id: "label.saving" })
          : intl.formatMessage({ id: "label.button.save" })
      }
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      size="md"
      primaryButtonDisabled={loading || isSubmitting}
    >
      <Form>
        <Stack gap={6}>
          <FormGroup>
            <TextInput
              id="kitName"
              labelText={
                <>
                  <FormattedMessage id="inventory.testKit.name" />
                  <span className="requiredlabel">*</span>
                </>
              }
              value={formData.kitName}
              onChange={(e) => handleChange("kitName", e.target.value)}
              invalid={!!errors.kitName}
              invalidText={errors.kitName}
              required
            />
          </FormGroup>

          <FormGroup>
            <Select
              id="type"
              labelText={intl.formatMessage({ id: "inventory.testKit.type" })}
              value={formData.type || ""}
              onChange={(e) => handleChange("type", e.target.value)}
            >
              <SelectItem value="" text="" />
              <SelectItem value="HIV" text="HIV" />
              <SelectItem value="SYPHILIS" text="SYPHILIS" />
            </Select>
          </FormGroup>

          <FormGroup>
            <CustomDatePicker
              id="receiveDate"
              labelText={intl.formatMessage({
                id: "inventory.testKit.receiveDate",
              })}
              value={formData.receiveDate || ""}
              onChange={(date) => handleChange("receiveDate", date)}
            />
          </FormGroup>

          <FormGroup>
            <CustomDatePicker
              id="expirationDate"
              labelText={intl.formatMessage({
                id: "inventory.testKit.expiration",
              })}
              value={formData.expirationDate || ""}
              onChange={(date) => handleChange("expirationDate", date)}
            />
          </FormGroup>

          <FormGroup>
            <TextInput
              id="lotNumber"
              labelText={intl.formatMessage({ id: "inventory.testKit.lot" })}
              value={formData.lotNumber}
              onChange={(e) => handleChange("lotNumber", e.target.value)}
            />
          </FormGroup>

          <FormGroup>
            <Select
              id="organizationId"
              labelText={
                <>
                  <FormattedMessage id="inventory.testKit.source" />
                  <span className="requiredlabel">*</span>
                </>
              }
              value={formData.organizationId}
              onChange={(e) => handleChange("organizationId", e.target.value)}
              invalid={!!errors.organizationId}
              invalidText={errors.organizationId}
              required
            >
              <SelectItem value="" text="" />
              {(sources || []).map((source) => (
                <SelectItem
                  key={source.id}
                  value={source.id}
                  text={source.name}
                />
              ))}
            </Select>
          </FormGroup>
        </Stack>
      </Form>
    </Modal>
  );
}

export default InventoryItemForm;
