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
    "the established fixture must contain control-identification evidence",
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
    "configDefaults must contain profile-owned instance defaults",
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

function profileSpecificLiterals(fixtures) {
  const values = new Set();
  for (const { profile } of fixtures) {
    const candidates = [
      profile.profileMeta?.id,
      profile.profileMeta?.displayName,
      profile.profileMeta?.manufacturer,
      profile.analyzer_name,
      profile.manufacturer,
      ...(profile.default_test_mappings || []).map(
        (mapping) => mapping.test_code,
      ),
    ];
    for (const candidate of candidates) {
      if (isNonBlank(candidate)) {
        values.add(candidate);
      }
    }
  }
  return [...values];
}

function lineContainsLiteral(line, value) {
  return [
    JSON.stringify(value),
    `'${value.replaceAll("'", "\\'")}'`,
    `\`${value.replaceAll("\`", "\\\`")}\``,
  ].some((literal) => line.includes(literal));
}

export function findHardcodedProfileSpecialCases(source, fixtures) {
  const controlFlow =
    /\bif\s*\(|\bswitch\s*\(|\bcase\s+|={2,3}|\.equals\s*\(|\.contains\s*\(|\.startsWith\s*\(/;
  const violations = [];

  source.split(/\r?\n/).forEach((line, index) => {
    if (!controlFlow.test(line)) {
      return;
    }
    for (const value of profileSpecificLiterals(fixtures)) {
      if (lineContainsLiteral(line, value)) {
        violations.push({ line: index + 1, value });
      }
    }
  });

  return violations;
}

const duplicatedDefaultAuthorities = [
  "DEFAULT_COMMUNICATION_MODE",
  "DEFAULT_PROTOCOL_VERSION",
  "FILE_FORMAT_PATTERNS",
  "PLUGIN_PROTOCOL_DEFAULTS",
];

export function findProfileDefaultAuthorityViolations(source) {
  return duplicatedDefaultAuthorities
    .filter((identifier) => new RegExp(`\\b${identifier}\\b`).test(source))
    .sort();
}

const isTestOrProfileData = (filePath) =>
  /(^|\/)(?:src\/test|test|tests|__tests__)(\/|$)/.test(filePath) ||
  /\.(?:test|spec)\.[^.]+$/.test(filePath) ||
  filePath.startsWith("projects/analyzer-profiles/");

const isOeProductionPath = (filePath) =>
  filePath.startsWith("src/main/java/org/openelisglobal/analyzer") ||
  filePath.startsWith("frontend/src/components/analyzers/") ||
  filePath.startsWith("frontend/src/services/analyzer");

const isAnalyzerProductionPath = (filePath) =>
  isOeProductionPath(filePath) ||
  filePath.startsWith("src/main/java/org/itech/ahb/") ||
  filePath.startsWith("protocols/") ||
  /^(?:api|server|profile_adapter)\.py$/.test(filePath);

export function findAddedProfileBoundaryViolations(diff, fixtures) {
  const violations = [];
  let filePath = "";

  diff.split(/\r?\n/).forEach((line, index) => {
    if (line.startsWith("+++ b/")) {
      filePath = line.slice("+++ b/".length);
      return;
    }
    if (
      !line.startsWith("+") ||
      line.startsWith("+++") ||
      !isAnalyzerProductionPath(filePath) ||
      isTestOrProfileData(filePath)
    ) {
      return;
    }

    const addedSource = line.slice(1);
    const rules = [];
    if (/\bAnalyzerQcRule\b/.test(addedSource)) {
      rules.push("analyzer-qc-rule");
    }
    if (
      isOeProductionPath(filePath) &&
      /\b(?:defaultConfigId|getDefaultConfigs?|applyDefaultConfig)\b|\/data\/analyzer-profiles/.test(
        addedSource,
      )
    ) {
      rules.push("copied-profile-authority");
    }
    if (
      isOeProductionPath(filePath) &&
      /\b(?:WatchService|WatchKey|DirectoryStream|FileSystemWatcher)\b/.test(
        addedSource,
      )
    ) {
      rules.push("oe-file-watcher");
    }
    if (findProfileDefaultAuthorityViolations(addedSource).length > 0) {
      rules.push("hardcoded-profile-default");
    }
    if (findHardcodedProfileSpecialCases(addedSource, fixtures).length > 0) {
      rules.push("hardcoded-profile-special-case");
    }
    if (
      /\b(?:fallbackControl|fallbackQc|defaultQcRules|hardcodedControl)\w*\b/i.test(
        addedSource,
      )
    ) {
      rules.push("hidden-control-classifier-fallback");
    }

    for (const rule of rules) {
      violations.push({ filePath, line: index + 1, rule });
    }
  });

  return violations;
}
