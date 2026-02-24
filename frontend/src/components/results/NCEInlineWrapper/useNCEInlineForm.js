import { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import {
  createNCEFromResult,
  escalateDeltaCheckAlert,
} from "../../../services/NCEIntegrationService";

/** Category → subcategory options mapping */
export const SUBCATEGORY_OPTIONS = {
  Analytical: [
    "Result Discrepancy",
    "Calibration Failure",
    "QC Out of Range",
    "Instrument Malfunction",
    "Reagent Issue",
  ],
  PreAnalytical: [
    "Sample Collection Error",
    "Labeling Error",
    "Transport Issue",
    "Sample Integrity",
  ],
  PostAnalytical: [
    "Transcription Error",
    "Reporting Delay",
    "Result Delivery Failure",
  ],
};

const baseFormState = {
  category: "Analytical",
  subcategory: "Result Discrepancy",
  severity: "",
  title: "",
  description: "",
  immediateAction: "",
  sampleAction: "CONTINUE_WITH_FLAG",
};

function deriveSeverity(context) {
  return context?.suggestedSeverity ?? "";
}

function deriveDescription(context, intl) {
  if (!context) return "";
  const parts = [];
  if (context.testName)
    parts.push(
      intl.formatMessage(
        { id: "nce.inline.description.prefix.test" },
        { value: context.testName },
      ),
    );
  if (context.resultValue)
    parts.push(
      intl.formatMessage(
        { id: "nce.inline.description.prefix.result" },
        { value: context.resultValue },
      ),
    );
  if (context.labNumber)
    parts.push(
      intl.formatMessage(
        { id: "nce.inline.description.prefix.labNumber" },
        { value: context.labNumber },
      ),
    );
  if (context.qualityFlags?.length)
    parts.push(
      intl.formatMessage(
        { id: "nce.inline.description.prefix.flags" },
        { value: context.qualityFlags.join(", ") },
      ),
    );
  return parts.join(". ");
}

/**
 * Hook for managing inline NCE form state and submission.
 * Pre-populates category, subcategory, severity, and description from context.
 * When alertId is provided, routes submission to the delta check escalation
 * endpoint instead of the standard NCE creation endpoint.
 * @param {string} resultId - The result ID to associate the NCE with
 * @param {Object} options - { context, onSuccess, onError, alertId }
 */
const useNCEInlineForm = (resultId, options = {}) => {
  const intl = useIntl();
  const {
    context,
    onSuccess,
    onError,
    alertId,
    sourceType,
    triggerType,
    triggerAction,
  } = options;
  const contextSeverity = deriveSeverity(context);
  const contextDescription = deriveDescription(context, intl);
  const [formData, setFormData] = useState(() => ({
    ...baseFormState,
    severity: contextSeverity,
    description: contextDescription,
  }));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  const validate = useCallback(() => {
    const newErrors = {};
    if (!formData.severity) {
      newErrors.severity = "nce.inline.validation.severityRequired";
    }
    if (!formData.sampleAction) {
      newErrors.sampleAction = "nce.inline.validation.sampleActionRequired";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData.severity, formData.sampleAction]);

  const updateField = useCallback((field, value) => {
    setFormData((prev) => {
      const next = { ...prev, [field]: value };
      if (field === "category") {
        const subs = SUBCATEGORY_OPTIONS[value] || [];
        next.subcategory = subs[0] || "";
      }
      return next;
    });
    setErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }, []);

  const resetForm = useCallback(() => {
    setFormData({
      ...baseFormState,
      severity: contextSeverity,
      description: contextDescription,
    });
    setErrors({});
  }, [contextSeverity, contextDescription]);

  const closeForm = useCallback(() => {
    resetForm();
  }, [resetForm]);

  const submitForm = useCallback(() => {
    if (!validate()) return;

    setIsSubmitting(true);

    if (alertId) {
      // Delta check escalation: map inline form fields to escalation format
      const escalateData = {
        nceTitle: formData.title,
        nceDescription: formData.description,
        category: formData.category,
        subcategory: formData.subcategory,
        severity: formData.severity,
        immediateAction: formData.immediateAction,
        sampleAction: formData.sampleAction,
      };
      escalateDeltaCheckAlert(alertId, escalateData, (response) => {
        setIsSubmitting(false);
        if (response?.error) {
          onError?.(response);
        } else {
          closeForm();
          onSuccess?.(response);
        }
      });
    } else {
      const payload = {
        ...formData,
        sourceType: sourceType || "results_entry",
        triggerType: triggerType || "manual",
        triggerAction: triggerAction || undefined,
      };
      createNCEFromResult(resultId, payload, (response) => {
        setIsSubmitting(false);
        if (response?.error) {
          onError?.(response);
        } else {
          closeForm();
          onSuccess?.(response);
        }
      });
    }
  }, [
    resultId,
    alertId,
    formData,
    validate,
    closeForm,
    onSuccess,
    onError,
    sourceType,
    triggerType,
    triggerAction,
  ]);

  return {
    formData,
    updateField,
    errors,
    isSubmitting,
    closeForm,
    submitForm,
  };
};

export default useNCEInlineForm;
