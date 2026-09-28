const REQUIREMENT_BY_PATH = {
  "patientProperties.gender": "order.save.requirement.patientSex",
  "patientProperties.birthDateForDisplay":
    "order.save.requirement.patientBirthDate",
  "patientProperties.nationalId": "order.save.requirement.nationalId",
  "patientProperties.email": "order.save.requirement.validEmail",
  "patientProperties.patientContact.person.email":
    "order.save.requirement.validEmail",
  "sampleOrderItems.labNo": "order.save.requirement.labNumber",
  sampleOrderItems: "order.save.requirement.site",
  "sampleOrderItems.referringSiteName": "order.save.requirement.site",
  "sampleOrderItems.providerFirstName": "order.save.requirement.provider",
  "sampleOrderItems.providerLastName": "order.save.requirement.provider",
  "sampleOrderItems.providerEmail": "order.save.requirement.validEmail",
  sampleXML: "order.save.requirement.sampleType",
};

/**
 * The Add Order form's validation errors as save requirements, one per field
 * and in form order, so the disabled Submit button can say what is missing even
 * when the field sits on an earlier step. A path with no known label keeps the
 * validator's own message.
 */
export function missingFieldRequirements(validationError) {
  const inner = (validationError && validationError.inner) || [];
  const seen = new Set();
  const requirements = [];
  for (const error of inner) {
    const labelId = REQUIREMENT_BY_PATH[error.path];
    const key = labelId || error.message;
    if (!key || seen.has(key)) {
      continue;
    }
    seen.add(key);
    requirements.push(
      labelId
        ? { met: false, labelId }
        : { met: false, labelId: null, text: error.message },
    );
  }
  return requirements;
}
