import { parseQuantityValue } from "../biorepository/biorepositoryQuantityHelpers";
import { validateRequestReferenceRows } from "./biorepoRequestReferenceHelpers";
import {
  buildContactInfo,
  isBiorepositoryLabUnit,
  resolveRequesterLabUnit,
} from "./biorepoRequesterLabUnitHelpers";

export {
  buildContactInfo,
  isBiorepositoryLabUnit,
  resolveRequesterLabUnit,
} from "./biorepoRequesterLabUnitHelpers";

/** @deprecated use buildRequestorSessionDefaults + resolveRequesterLabUnit */
export function buildSessionFormDefaults(userSessionDetails) {
  const requesterLabUnit = resolveRequesterLabUnit({ session: userSessionDetails });
  return buildRequestorSessionDefaults(userSessionDetails, requesterLabUnit);
}

export function buildRequestorSessionDefaults(
  userSessionDetails,
  requesterLabUnit = "",
) {
  const displayName =
    `${userSessionDetails?.firstName || ""} ${userSessionDetails?.lastName || ""}`.trim();
  const requestorName = displayName || userSessionDetails?.loginName || "";

  return {
    requestorName,
    requesterLabUnit,
    requesterContactInfo: buildContactInfo(userSessionDetails, requesterLabUnit),
  };
}

export function deriveDestinationType(samplesWillBeDestroyed) {
  if (samplesWillBeDestroyed === true) {
    return "INTERNAL_LAB";
  }
  if (samplesWillBeDestroyed === false) {
    return "ANALYSIS_RETURN";
  }
  return "ANALYSIS_RETURN";
}

export function validateBrf02RequestForm(formData, selectedSamples) {
  const errors = [];

  if (!selectedSamples || selectedSamples.length === 0) {
    errors.push("Please add at least one requested sample");
  }

  if (!formData.requestorName?.trim()) {
    errors.push("Requestor name is required");
  }

  if (!formData.requesterLabUnit?.trim()) {
    errors.push("Lab unit is required");
  }

  if (!formData.principalInvestigator?.trim()) {
    errors.push("Principal investigator is required");
  }

  if (!formData.projectTitle?.trim()) {
    errors.push("Project title is required");
  }

  if (!formData.requesterContactInfo?.trim()) {
    errors.push("Contact information is required");
  }

  if (!formData.intendedUseDescription?.trim()) {
    errors.push("Please provide a description for intended use");
  }

  if (
    formData.samplesWillBeDestroyed !== true &&
    formData.samplesWillBeDestroyed !== false
  ) {
    errors.push("Please indicate whether samples will be destroyed after use");
  }

  if (
    formData.samplesWillBeDestroyed === false &&
    !formData.estimatedReturnDate
  ) {
    errors.push("Estimated return date is required when samples will be returned");
  }

  errors.push(...validateRequestReferenceRows(selectedSamples));

  return errors;
}

export function mergePrefillFields(currentFormData, nextValues) {
  const merged = { ...currentFormData };
  Object.entries(nextValues).forEach(([field, value]) => {
    if (!value) {
      return;
    }
    if (field === "requesterLabUnit") {
      const current = merged.requesterLabUnit?.trim() || "";
      if (current && !isBiorepositoryLabUnit(current)) {
        return;
      }
      if (isBiorepositoryLabUnit(value) && current) {
        return;
      }
    }
    if (!merged[field]?.trim()) {
      merged[field] = value;
    }
  });
  return merged;
}
