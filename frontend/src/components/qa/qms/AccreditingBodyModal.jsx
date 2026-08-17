import React, { useEffect, useState } from "react";
import {
  Button,
  DatePicker,
  DatePickerInput,
  FileUploader,
  InlineNotification,
  Modal,
  NumberInput,
  RadioButton,
  RadioButtonGroup,
  Stack,
  TextInput,
  Toggle,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  deleteFromOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
  toLocalIsoDate,
} from "../../utils/Utils";
import config from "../../../config.json";

const VISIBILITY_MODES = ["ANY_ACCREDITED_TEST", "PERCENTAGE"];
const MAX_LOGO_BYTES = 500 * 1024;
const LOGO_TYPES = ["image/png", "image/jpeg"];

const blankBody = () => ({
  code: "",
  name: "",
  expiresOn: "",
  logoVisibilityMode: "ANY_ACCREDITED_TEST",
  thresholdPct: 80,
  displayOrder: 0,
  active: true,
});

/**
 * Add/Edit Accrediting Body (OGC-686). The certificate carries one expiry for
 * the whole scope, so expiry lives here and not on the individual test enrollments.
 * `code` is immutable once created — the backend rejects a change with a 400. The
 * logo is uploaded as a separate multipart call after the body exists, since the
 * upload endpoint is keyed by body id.
 */
const AccreditingBodyModal = ({ open, onClose, body, onSaved }) => {
  const intl = useIntl();
  const isEdit = !!(body && body.id);

  const [form, setForm] = useState(blankBody());
  const [pendingFile, setPendingFile] = useState(null);
  const [uploaderKey, setUploaderKey] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    setSaving(false);
    setPendingFile(null);
    setUploaderKey((k) => k + 1);
    setForm(isEdit ? { ...blankBody(), ...body } : blankBody());
  }, [open, body, isEdit]);

  const set = (field, value) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const pickFile = (file) => {
    if (!file) {
      setPendingFile(null);
      return;
    }
    if (!LOGO_TYPES.includes(file.type) || file.size > MAX_LOGO_BYTES) {
      setError(
        intl.formatMessage({ id: "qa.qms.accreditation.body.logoError" }),
      );
      setPendingFile(null);
      // Remount the uploader too: leaving the rejected filename on screen reads
      // as "this file is attached", and saving after dismissing the error would
      // then quietly persist the body with no logo at all.
      setUploaderKey((k) => k + 1);
      return;
    }
    setError(null);
    setPendingFile(file);
  };

  const removeLogo = () =>
    deleteFromOpenElisServer(
      `/rest/accreditation/bodies/${body.id}/logo`,
      (status) => {
        if (status >= 200 && status < 300) {
          set("logoImageId", null);
          onSaved();
        } else {
          setError(
            intl.formatMessage({ id: "qa.qms.accreditation.body.logoError" }),
          );
        }
      },
    );

  // The body row is already persisted by the time this runs, so a failed upload
  // still reports the save upstream — it just keeps the modal open to say so.
  const uploadLogoThenClose = (id) => {
    if (!pendingFile) {
      setSaving(false);
      onSaved();
      onClose();
      return;
    }
    const formData = new FormData();
    formData.append("file", pendingFile);
    postToOpenElisServerFormData(
      `/rest/accreditation/bodies/${id}/logo`,
      formData,
      (status) => {
        setSaving(false);
        onSaved();
        if (status >= 200 && status < 300) {
          onClose();
        } else {
          setPendingFile(null);
          setUploaderKey((k) => k + 1);
          setError(
            intl.formatMessage({ id: "qa.qms.accreditation.body.logoError" }),
          );
        }
      },
    );
  };

  const handleSubmit = () => {
    if (!form.code.trim() || !form.name.trim() || !form.expiresOn) {
      setError(
        intl.formatMessage({ id: "qa.qms.accreditation.body.required" }),
      );
      return;
    }
    setSaving(true);
    setError(null);
    const payload = JSON.stringify({
      code: form.code.trim(),
      name: form.name.trim(),
      expiresOn: form.expiresOn,
      logoVisibilityMode: form.logoVisibilityMode,
      thresholdPct: Number(form.thresholdPct),
      displayOrder: Number(form.displayOrder),
      active: !!form.active,
    });
    // A rejection says exactly what is wrong — bad code pattern, duplicate code,
    // name too long — in {"error": ...}. Show that; the generic string is only the
    // fallback for a response we cannot read.
    const fail = (response) => {
      setSaving(false);
      const generic = intl.formatMessage({
        id: "qa.qms.accreditation.body.saveError",
      });
      if (!response) {
        setError(generic);
        return;
      }
      response
        .json()
        .then((data) => setError(data?.error || generic))
        .catch(() => setError(generic));
    };
    const handle = (response) => {
      if (!response || !response.ok) {
        fail(response);
        return;
      }
      response
        .json()
        .then((saved) => uploadLogoThenClose(saved?.id ?? body.id))
        .catch(() => fail(null));
    };
    if (isEdit) {
      putToOpenElisServerFullResponse(
        `/rest/accreditation/bodies/${body.id}`,
        payload,
        handle,
      );
    } else {
      postToOpenElisServerFullResponse(
        "/rest/accreditation/bodies",
        payload,
        handle,
      );
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({
        id: isEdit
          ? "qa.qms.accreditation.body.modal.editTitle"
          : "qa.qms.accreditation.body.modal.addTitle",
      })}
      primaryButtonText={intl.formatMessage({ id: "button.save" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving}
      size="md"
    >
      <Stack gap={5}>
        <TextInput
          id="accreditation-body-code"
          labelText={intl.formatMessage({
            id: "qa.qms.accreditation.body.field.code",
          })}
          helperText={intl.formatMessage({
            id: "qa.qms.accreditation.body.field.codeHelper",
          })}
          value={form.code}
          disabled={isEdit}
          onChange={(e) => set("code", e.target.value.toUpperCase())}
          invalid={!!error && !form.code.trim()}
        />
        <TextInput
          id="accreditation-body-name"
          labelText={intl.formatMessage({
            id: "qa.qms.accreditation.body.field.name",
          })}
          value={form.name}
          onChange={(e) => set("name", e.target.value)}
          invalid={!!error && !form.name.trim()}
        />
        <DatePicker
          datePickerType="single"
          dateFormat="Y-m-d"
          value={form.expiresOn ? [form.expiresOn] : []}
          onChange={(dates) =>
            set("expiresOn", dates.length ? toLocalIsoDate(dates[0]) : "")
          }
        >
          <DatePickerInput
            id="accreditation-body-expires"
            labelText={intl.formatMessage({
              id: "qa.qms.accreditation.body.field.expiresOn",
            })}
            placeholder="yyyy-mm-dd"
            invalid={!!error && !form.expiresOn}
          />
        </DatePicker>

        <RadioButtonGroup
          legendText={intl.formatMessage({
            id: "qa.qms.accreditation.body.field.visibilityMode",
          })}
          name="accreditation-visibility"
          orientation="vertical"
          valueSelected={form.logoVisibilityMode}
          onChange={(value) => set("logoVisibilityMode", value)}
        >
          {VISIBILITY_MODES.map((mode) => (
            <RadioButton
              key={mode}
              id={`visibility-${mode}`}
              value={mode}
              labelText={intl.formatMessage({
                id: `qa.qms.accreditation.visibility.${mode}`,
              })}
            />
          ))}
        </RadioButtonGroup>

        {form.logoVisibilityMode === "PERCENTAGE" && (
          <NumberInput
            id="accreditation-body-threshold"
            label={intl.formatMessage({
              id: "qa.qms.accreditation.body.field.thresholdPct",
            })}
            min={0}
            max={100}
            value={form.thresholdPct}
            onChange={(_e, { value }) => set("thresholdPct", value)}
          />
        )}

        <NumberInput
          id="accreditation-body-order"
          label={intl.formatMessage({
            id: "qa.qms.accreditation.body.field.displayOrder",
          })}
          min={0}
          value={form.displayOrder}
          onChange={(_e, { value }) => set("displayOrder", value)}
        />

        <Toggle
          id="accreditation-body-active"
          labelText={intl.formatMessage({
            id: "qa.qms.accreditation.body.field.active",
          })}
          labelA={intl.formatMessage({ id: "label.no" })}
          labelB={intl.formatMessage({ id: "label.yes" })}
          toggled={!!form.active}
          onToggle={(checked) => set("active", checked)}
        />

        {isEdit && form.logoImageId && (
          <div>
            <img
              src={`${config.serverBaseUrl}/rest/accreditation/logo/${form.logoImageId}`}
              alt=""
              style={{ height: "3rem" }}
            />
            <Button
              kind="ghost"
              size="sm"
              onClick={removeLogo}
              data-testid="remove-logo-button"
            >
              {intl.formatMessage({
                id: "qa.qms.accreditation.body.logo.remove",
              })}
            </Button>
          </div>
        )}

        <FileUploader
          key={uploaderKey}
          labelTitle={intl.formatMessage({
            id: "qa.qms.accreditation.body.field.logo",
          })}
          buttonLabel={intl.formatMessage({ id: "label.button.uploadfile" })}
          accept={[".png", ".jpg", ".jpeg"]}
          filenameStatus="edit"
          onChange={(e) => pickFile(e.target.files?.[0])}
          onDelete={() => setPendingFile(null)}
        />

        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            onCloseButtonClick={() => setError(null)}
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
          />
        )}
      </Stack>
    </Modal>
  );
};

export default AccreditingBodyModal;
