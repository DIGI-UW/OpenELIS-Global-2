const isRecord = (value) =>
  value !== null && typeof value === "object" && !Array.isArray(value);

const isNonBlank = (value) =>
  typeof value === "string" && value.trim().length > 0;

const hasEntries = (value) => isRecord(value) && Object.keys(value).length > 0;

function addViolation(violations, condition, message) {
  if (!condition) {
    violations.push(message);
  }
}

function validateMappings(profile, violations) {
  const mappings = profile.default_test_mappings;
  addViolation(
    violations,
    Array.isArray(mappings) && mappings.length > 0,
    "default_test_mappings must describe the emitted fixed vocabulary",
  );
  if (!Array.isArray(mappings)) {
    return;
  }

  const codes = new Set();
  for (const mapping of mappings) {
    addViolation(
      violations,
      isRecord(mapping) && isNonBlank(mapping.test_code),
      "each result definition must have a nonblank test_code",
    );
    if (!isRecord(mapping) || !isNonBlank(mapping.test_code)) {
      continue;
    }
    addViolation(
      violations,
      !codes.has(mapping.test_code),
      `duplicate test_code: ${mapping.test_code}`,
    );
    codes.add(mapping.test_code);
  }
}

function validateRecognition(profile, violations) {
  const recognition = profile.controlResultRecognition;
  if (isRecord(recognition)) {
    const hasRules =
      recognition.mode === "RULES" &&
      ((Array.isArray(recognition.rules) && recognition.rules.length > 0) ||
        hasEntries(recognition.rules));
    const affirmedNone =
      recognition.mode === "NONE" &&
      recognition.affirmedNoControlResults === true &&
      !recognition.rules;
    addViolation(
      violations,
      hasRules || affirmedNone,
      "controlResultRecognition must be RULES or affirmed NONE",
    );
    return;
  }

  addViolation(
    violations,
    Array.isArray(profile.configDefaults?.qcRules) &&
      profile.configDefaults.qcRules.length > 0,
    "the source profile must contain control-identification evidence for curation",
  );
}

function validateSocketProfile(profile, violations) {
  addViolation(
    violations,
    isNonBlank(profile.protocol.version),
    "socket protocol version is required",
  );
  addViolation(
    violations,
    Array.isArray(profile.transport) && profile.transport.length > 0,
    "socket transports are required",
  );
  addViolation(
    violations,
    hasEntries(profile.communication),
    "socket communication direction/capability is required",
  );
  addViolation(
    violations,
    isNonBlank(profile.configDefaults.connectionRole),
    "socket connectionRole default is required",
  );
}

function validateFileProfile(profile, violations) {
  addViolation(
    violations,
    isNonBlank(profile.protocol.format),
    "FILE format is required",
  );
  addViolation(
    violations,
    Array.isArray(profile.supported_extensions) &&
      profile.supported_extensions.length > 0,
    "FILE extensions are required",
  );
  addViolation(
    violations,
    hasEntries(profile.column_mapping),
    "FILE column mappings are required",
  );
  addViolation(
    violations,
    isNonBlank(profile.configDefaults.fileFormat),
    "FILE format default is required",
  );
}

export function findProfileCompatibilityViolations(profile) {
  const violations = [];

  addViolation(violations, isRecord(profile), "profile must be an object");
  if (!isRecord(profile)) {
    return violations;
  }

  addViolation(
    violations,
    isRecord(profile.profileMeta) &&
      isNonBlank(profile.profileMeta.id) &&
      isNonBlank(profile.profileMeta.displayName),
    "profileMeta must identify the analyzer type",
  );
  addViolation(
    violations,
    isRecord(profile.protocol) && isNonBlank(profile.protocol.name),
    "protocol must remain a typed object",
  );
  addViolation(
    violations,
    hasEntries(profile.configDefaults),
    "configDefaults must contain profile-owned new-connection defaults",
  );

  if (!isRecord(profile.protocol) || !hasEntries(profile.configDefaults)) {
    return violations;
  }

  if (profile.protocol.name === "ASTM" || profile.protocol.name === "HL7") {
    validateSocketProfile(profile, violations);
  } else if (profile.protocol.name === "FILE") {
    validateFileProfile(profile, violations);
  } else {
    violations.push("protocol family must be ASTM, HL7, or FILE");
  }

  validateMappings(profile, violations);
  validateRecognition(profile, violations);

  return violations;
}
