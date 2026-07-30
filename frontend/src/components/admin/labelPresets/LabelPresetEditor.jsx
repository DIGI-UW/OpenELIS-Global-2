import React, { useState, useEffect } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Checkbox,
  Column,
  ComposedModal,
  Form,
  FormGroup,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  ModalBody,
  ModalFooter,
  ModalHeader,
  NumberInput,
  RadioButton,
  RadioButtonGroup,
  Section,
  TextInput,
  Toggle,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { normalizeName } from "./helpers";

const BARCODE_TYPES = ["CODE_128", "QR", "DATAMATRIX"];

const EMPTY_FORM = {
  name: "",
  heightMm: 20,
  widthMm: 40,
  barcodeType: "CODE_128",
  printsPerOrder: false,
  printsPerSample: true,
  defaultPerOrder: 0,
  maxPerOrder: 10,
  defaultPerSample: 1,
  maxPerSample: 10,
  isActive: true,
  fields: [],
};

function LabelPresetEditor({ preset, onClose }) {
  const intl = useIntl();
  const isEdit = preset != null;

  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState(null);

  useEffect(() => {
    if (preset) {
      setForm({
        name: preset.name ?? "",
        heightMm: preset.heightMm ?? 20,
        widthMm: preset.widthMm ?? 40,
        barcodeType: preset.barcodeType ?? "CODE_128",
        printsPerOrder: preset.printsPerOrder ?? false,
        printsPerSample: preset.printsPerSample ?? true,
        defaultPerOrder: preset.defaultPerOrder ?? 0,
        maxPerOrder: preset.maxPerOrder ?? 10,
        defaultPerSample: preset.defaultPerSample ?? 1,
        maxPerSample: preset.maxPerSample ?? 10,
        isActive: preset.isActive ?? true,
        fields: preset.fields ?? [],
      });
    } else {
      setForm(EMPTY_FORM);
    }
    setErrors({});
    setApiError(null);
  }, [preset]);

  const setField = (name, value) => {
    setForm((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const validate = () => {
    const errs = {};
    if (!form.name || !form.name.trim()) {
      errs.name = intl.formatMessage({
        id: "admin.labelPresets.validation.name.required",
      });
    } else if (form.name.trim().length > 120) {
      errs.name = intl.formatMessage({
        id: "admin.labelPresets.validation.name.toolong",
      });
    }
    if (!form.heightMm || form.heightMm < 5 || form.heightMm > 200) {
      errs.heightMm = intl.formatMessage({
        id: "admin.labelPresets.validation.dimension.range",
      });
    }
    if (!form.widthMm || form.widthMm < 5 || form.widthMm > 200) {
      errs.widthMm = intl.formatMessage({
        id: "admin.labelPresets.validation.dimension.range",
      });
    }
    if (!form.barcodeType) {
      errs.barcodeType = intl.formatMessage({
        id: "admin.labelPresets.validation.barcodeType.required",
      });
    }
    if (!form.printsPerOrder && !form.printsPerSample) {
      errs.scope = intl.formatMessage({
        id: "admin.labelPresets.validation.scope.required",
      });
    }
    if (form.printsPerOrder && form.maxPerOrder < form.defaultPerOrder) {
      errs.maxPerOrder = intl.formatMessage({
        id: "admin.labelPresets.validation.max.lte",
      });
    }
    if (form.printsPerSample && form.maxPerSample < form.defaultPerSample) {
      errs.maxPerSample = intl.formatMessage({
        id: "admin.labelPresets.validation.max.lte",
      });
    }
    return errs;
  };

  const handleSubmit = () => {
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setSubmitting(true);
    setApiError(null);

    const payload = {
      ...form,
      name: normalizeName(form.name),
    };

    const handleResponse = (response) => {
      setSubmitting(false);
      if (response && (response.status === 200 || response.status === 201)) {
        onClose(true);
      } else if (response && response.status === 422) {
        response.json().then((body) => {
          if (body && body.fieldErrors) {
            const serverErrs = {};
            body.fieldErrors.forEach((fe) => {
              serverErrs[fe.field] = fe.defaultMessage;
            });
            setErrors(serverErrs);
          }
          if (body && body.globalErrors && body.globalErrors.length > 0) {
            setApiError(body.globalErrors.join("; "));
          }
        });
      } else {
        setApiError(
          intl.formatMessage({ id: "admin.labelPresets.saveFailed" }),
        );
      }
    };

    if (isEdit) {
      putToOpenElisServerFullResponse(
        `/api/labelPresets/${preset.id}`,
        JSON.stringify(payload),
        handleResponse,
      );
    } else {
      postToOpenElisServerFullResponse(
        "/api/labelPresets",
        JSON.stringify(payload),
        handleResponse,
      );
    }
  };

  return (
    <ComposedModal
      open
      onClose={() => onClose(false)}
      size="lg"
      data-testid="label-preset-editor"
    >
      <ModalHeader>
        <Heading>
          {isEdit ? (
            <FormattedMessage id="admin.labelPresets.editor.titleEdit" />
          ) : (
            <FormattedMessage id="admin.labelPresets.editor.titleAdd" />
          )}
        </Heading>
      </ModalHeader>
      <ModalBody>
        {apiError && (
          <InlineNotification
            kind="error"
            title={apiError}
            lowContrast
            style={{ marginBottom: "1rem" }}
          />
        )}
        {submitting && <Loading small withOverlay={false} />}
        <Form>
          {/* Section 1: Basic Info */}
          <Section>
            <Heading>
              <FormattedMessage id="admin.labelPresets.editor.section.basicInfo" />
            </Heading>
            <Grid>
              <Column lg={8}>
                <TextInput
                  id="preset-name"
                  labelText={intl.formatMessage({
                    id: "admin.labelPresets.field.name",
                  })}
                  value={form.name}
                  onChange={(e) => setField("name", e.target.value)}
                  invalid={!!errors.name}
                  invalidText={errors.name}
                  disabled={isEdit && preset?.isSystem}
                />
              </Column>
              <Column lg={4}>
                <Checkbox
                  id="preset-isActive"
                  labelText={intl.formatMessage({
                    id: "admin.labelPresets.field.isActive",
                  })}
                  checked={form.isActive}
                  onChange={(_, { checked }) => setField("isActive", checked)}
                />
              </Column>
            </Grid>
          </Section>

          {/* Section 2: Dimensions */}
          <Section style={{ marginTop: "1.5rem" }}>
            <Heading>
              <FormattedMessage id="admin.labelPresets.editor.section.dimensions" />
            </Heading>
            <Grid>
              <Column lg={4}>
                <NumberInput
                  id="preset-heightMm"
                  label={intl.formatMessage({
                    id: "admin.labelPresets.field.heightMm",
                  })}
                  value={form.heightMm}
                  min={5}
                  max={200}
                  onChange={(e, { value }) => setField("heightMm", value)}
                  invalid={!!errors.heightMm}
                  invalidText={errors.heightMm}
                />
              </Column>
              <Column lg={4}>
                <NumberInput
                  id="preset-widthMm"
                  label={intl.formatMessage({
                    id: "admin.labelPresets.field.widthMm",
                  })}
                  value={form.widthMm}
                  min={5}
                  max={200}
                  onChange={(e, { value }) => setField("widthMm", value)}
                  invalid={!!errors.widthMm}
                  invalidText={errors.widthMm}
                />
              </Column>
            </Grid>
          </Section>

          {/* Section 3: Barcode Settings */}
          <Section style={{ marginTop: "1.5rem" }}>
            <Heading>
              <FormattedMessage id="admin.labelPresets.editor.section.barcodeSettings" />
            </Heading>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "admin.labelPresets.field.barcodeType",
              })}
              name="barcodeType"
              valueSelected={form.barcodeType}
              onChange={(value) => setField("barcodeType", value)}
            >
              {BARCODE_TYPES.map((type) => (
                <RadioButton
                  key={type}
                  labelText={type}
                  value={type}
                  id={`barcode-type-${type}`}
                />
              ))}
            </RadioButtonGroup>
            {errors.barcodeType && (
              <p
                style={{ color: "var(--cds-text-error)", fontSize: "0.75rem" }}
              >
                {errors.barcodeType}
              </p>
            )}
          </Section>

          {/* Section 4: Print Scope & Quantities */}
          <Section style={{ marginTop: "1.5rem" }}>
            <Heading>
              <FormattedMessage id="admin.labelPresets.editor.section.printScope" />
            </Heading>
            {errors.scope && (
              <p
                style={{ color: "var(--cds-text-error)", fontSize: "0.75rem" }}
              >
                {errors.scope}
              </p>
            )}
            <Grid>
              <Column lg={8}>
                <FormGroup legendText="">
                  <Checkbox
                    id="scope-order"
                    labelText={intl.formatMessage({
                      id: "admin.labelPresets.scope.order",
                    })}
                    checked={form.printsPerOrder}
                    onChange={(_, { checked }) =>
                      setField("printsPerOrder", checked)
                    }
                  />
                  {form.printsPerOrder && (
                    <Grid style={{ marginTop: "0.5rem" }}>
                      <Column lg={4}>
                        <NumberInput
                          id="defaultPerOrder"
                          label={intl.formatMessage({
                            id: "admin.labelPresets.field.defaultPerOrder",
                          })}
                          value={form.defaultPerOrder}
                          min={0}
                          onChange={(e, { value }) =>
                            setField("defaultPerOrder", value)
                          }
                        />
                      </Column>
                      <Column lg={4}>
                        <NumberInput
                          id="maxPerOrder"
                          label={intl.formatMessage({
                            id: "admin.labelPresets.field.maxPerOrder",
                          })}
                          value={form.maxPerOrder}
                          min={0}
                          onChange={(e, { value }) =>
                            setField("maxPerOrder", value)
                          }
                          invalid={!!errors.maxPerOrder}
                          invalidText={errors.maxPerOrder}
                        />
                      </Column>
                    </Grid>
                  )}
                </FormGroup>
                <FormGroup legendText="" style={{ marginTop: "1rem" }}>
                  <Checkbox
                    id="scope-sample"
                    labelText={intl.formatMessage({
                      id: "admin.labelPresets.scope.sample",
                    })}
                    checked={form.printsPerSample}
                    onChange={(_, { checked }) =>
                      setField("printsPerSample", checked)
                    }
                  />
                  {form.printsPerSample && (
                    <Grid style={{ marginTop: "0.5rem" }}>
                      <Column lg={4}>
                        <NumberInput
                          id="defaultPerSample"
                          label={intl.formatMessage({
                            id: "admin.labelPresets.field.defaultPerSample",
                          })}
                          value={form.defaultPerSample}
                          min={0}
                          onChange={(e, { value }) =>
                            setField("defaultPerSample", value)
                          }
                        />
                      </Column>
                      <Column lg={4}>
                        <NumberInput
                          id="maxPerSample"
                          label={intl.formatMessage({
                            id: "admin.labelPresets.field.maxPerSample",
                          })}
                          value={form.maxPerSample}
                          min={0}
                          onChange={(e, { value }) =>
                            setField("maxPerSample", value)
                          }
                          invalid={!!errors.maxPerSample}
                          invalidText={errors.maxPerSample}
                        />
                      </Column>
                    </Grid>
                  )}
                </FormGroup>
              </Column>
            </Grid>
          </Section>
        </Form>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={() => onClose(false)}>
          <FormattedMessage id="label.button.cancel" />
        </Button>
        <Button kind="primary" onClick={handleSubmit} disabled={submitting}>
          <FormattedMessage id="label.button.save" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}

export default LabelPresetEditor;
