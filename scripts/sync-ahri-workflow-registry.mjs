#!/usr/bin/env node
/**
 * Regenerates frontend/src/constants/ahriWorkflowRegistry.js from
 * volume/configuration/backend/workflow-registry/ahri-workflows.csv
 *
 * Usage:
 *   node scripts/sync-ahri-workflow-registry.mjs          # write JS file
 *   node scripts/sync-ahri-workflow-registry.mjs --check  # exit 1 if drift
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");
const csvPath = path.join(
  root,
  "volume/configuration/backend/workflow-registry/ahri-workflows.csv",
);
const jsPath = path.join(root, "frontend/src/constants/ahriWorkflowRegistry.js");

function parseCsvLine(line) {
  const fields = [];
  let current = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') {
      inQuotes = !inQuotes;
    } else if (c === "," && !inQuotes) {
      fields.push(current.trim());
      current = "";
    } else {
      current += c;
    }
  }
  fields.push(current.trim());
  return fields;
}

function loadRegistryFromCsv() {
  const text = fs.readFileSync(csvPath, "utf8");
  const lines = text.split(/\r?\n/).filter((l) => l.trim());
  const headers = parseCsvLine(lines[0]);
  const idx = Object.fromEntries(headers.map((h, i) => [h, i]));
  const registry = {};
  for (let i = 1; i < lines.length; i++) {
    const cols = parseCsvLine(lines[i]);
    const workflowType = cols[idx.workflowType].trim().toLowerCase().replace(/\s+/g, "_");
    const entry = {
      stageOrder: Number.parseInt(cols[idx.stageOrder], 10),
      stageId: cols[idx.stageId].trim(),
      pageKey: (cols[idx.pageKey] || cols[idx.stageId]).trim(),
      stageTitle: cols[idx.stageTitle].trim(),
      requiredActions: cols[idx.requiredActions]
        .split("|")
        .map((s) => s.trim())
        .filter(Boolean),
      allowedPersonas: cols[idx.allowedPersonas]
        .split("|")
        .map((s) => s.trim())
        .filter(Boolean),
    };
    if (!registry[workflowType]) {
      registry[workflowType] = [];
    }
    registry[workflowType].push(entry);
  }
  for (const stages of Object.values(registry)) {
    stages.sort((a, b) => a.stageOrder - b.stageOrder);
  }
  return registry;
}

function generateJs(registry) {
  const body = JSON.stringify(registry, null, 2);
  return `/** Auto-synced from volume/configuration/backend/workflow-registry/ahri-workflows.csv */
export const NOTEBOOK_STAGE_ACTIONS = {
  VIEW: "VIEW",
  EDIT: "EDIT",
  COMPLETE: "COMPLETE",
};

const REGISTRY_BY_WORKFLOW_TYPE = ${body};

export function normalizeWorkflowType(workflowType) {
  if (!workflowType) return "";
  const normalized = String(workflowType).trim().toLowerCase().replace(/\\s+/g, "_");
  if (
    normalized === "histopathology_biopsy_tissue" ||
    normalized === "histopathology" ||
    normalized === "histopathology/biopsy" ||
    normalized === "histopathology_biopsy" ||
    normalized === "peripheral_smear_bone_marrow_morphology" ||
    normalized === "peripheral_smear" ||
    normalized === "bone_marrow" ||
    normalized === "peripheral_smear_bone_marrow" ||
    normalized === "fnac" ||
    normalized === "cytology_liquid_based_pap_smear" ||
    normalized === "cytology" ||
    normalized === "liquid_based_pap_smear" ||
    normalized === "pap_smear"
  ) {
    return "pathology";
  }
  return normalized;
}

export function getRegistryStages(workflowType) {
  const key = normalizeWorkflowType(workflowType);
  return REGISTRY_BY_WORKFLOW_TYPE[key] || [];
}

export function resolvePageKey(page) {
  if (!page) return "";
  if (page.pageId) return String(page.pageId).trim();
  if (page.pageKey) return String(page.pageKey).trim();
  const order = page?.pageOrder ?? page?.order ?? 0;
  return order > 0 ? \`stage-\${order}\` : "";
}

export function findRegistryStage(workflowType, page) {
  const key = resolvePageKey(page);
  const order = page?.pageOrder ?? page?.order ?? 0;
  const stages = getRegistryStages(workflowType);
  return (
    stages.find((s) => s.pageKey === key || s.stageId === key) ||
    stages.find((s) => s.stageOrder === order) ||
    null
  );
}

export function isActionPermitted(workflowType, page, action) {
  const stage = findRegistryStage(workflowType, page);
  if (!stage) return false;
  const actions = stage.requiredActions || [];
  return actions.includes(action);
}

export function resolvePageAllowedRoles(workflowType, page, action = null) {
  const stage = findRegistryStage(workflowType, page);
  if (stage) {
    if (action && !isActionPermitted(workflowType, page, action)) {
      return [];
    }
    return stage.allowedPersonas || [];
  }
  const explicit = page?.allowedRoles
    ? (Array.isArray(page.allowedRoles) ? page.allowedRoles : Array.from(page.allowedRoles))
    : [];
  if (explicit.length > 0) {
    return explicit;
  }
  return [];
}

export function enrichPagesWithRegistryRoles(workflowType, pages) {
  if (!pages || !Array.isArray(pages)) return [];
  return pages.map((page) => {
    const allowedRoles = resolvePageAllowedRoles(workflowType, page);
    return { ...page, pageKey: resolvePageKey(page), allowedRoles };
  });
}

export { REGISTRY_BY_WORKFLOW_TYPE };
`;
}

const checkOnly = process.argv.includes("--check");
const registry = loadRegistryFromCsv();
const expected = generateJs(registry);

if (checkOnly) {
  const current = fs.readFileSync(jsPath, "utf8");
  if (current !== expected) {
    console.error(
      "ahriWorkflowRegistry.js is out of sync with ahri-workflows.csv. Run: node scripts/sync-ahri-workflow-registry.mjs",
    );
    process.exit(1);
  }
  console.log("Registry JS matches CSV.");
  process.exit(0);
}

fs.writeFileSync(jsPath, expected, "utf8");
console.log(`Wrote ${jsPath}`);
