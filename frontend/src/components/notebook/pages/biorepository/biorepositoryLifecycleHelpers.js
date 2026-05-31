import { formatTransferSourceLab } from "./biorepositoryTransferHelpers";
import { formatQuantityWithUnit } from "./biorepositoryQuantityHelpers";

const PROJECT_PREFIX = /^Project:\s*(.+)$/m;
const REASON_PREFIX = /^Reason:\s*(.+)$/ms;

export function parseStructuredTransferNotes(requestNotes) {
  if (!requestNotes || !requestNotes.trim()) {
    return { projectName: null, transferReason: null };
  }

  const projectMatch = requestNotes.match(PROJECT_PREFIX);
  const reasonMatch = requestNotes.match(REASON_PREFIX);

  if (projectMatch || reasonMatch) {
    return {
      projectName: projectMatch ? projectMatch[1].trim() : null,
      transferReason: reasonMatch ? reasonMatch[1].trim() : null,
    };
  }

  return { projectName: null, transferReason: requestNotes.trim() };
}

export function formatStructuredTransferNotes(projectName, transferReason) {
  const project = (projectName || "").trim();
  const reason = (transferReason || "").trim();
  return `Project: ${project}\nReason: ${reason}`;
}

export function mergeTransferSummary(apiSummary, transferContext) {
  const parsedContextNotes = transferContext?.requestNotes
    ? parseStructuredTransferNotes(transferContext.requestNotes)
    : { projectName: null, transferReason: null };

  return {
    sourceLab:
      apiSummary?.sourceLab ||
      transferContext?.sourceLab ||
      transferContext?.requestSourceLab ||
      null,
    destinationLab: apiSummary?.destinationLab || "BIOREPOSITORY",
    transferStatus: apiSummary?.transferStatus || transferContext?.requestStatus || null,
    itemStatus: apiSummary?.itemStatus || transferContext?.status || null,
    projectName:
      apiSummary?.projectName ||
      parsedContextNotes.projectName ||
      transferContext?.projectName ||
      null,
    transferReason:
      apiSummary?.transferReason ||
      parsedContextNotes.transferReason ||
      transferContext?.transferReason ||
      null,
    requestNotes: apiSummary?.requestNotes || transferContext?.requestNotes || null,
    requestedByName:
      apiSummary?.requestedByName || transferContext?.requestedByName || null,
    requestedTimestamp:
      apiSummary?.requestedTimestamp || transferContext?.requestedTimestamp || null,
    rejectionReason:
      apiSummary?.rejectionReason || transferContext?.rejectionReason || null,
    sampleExternalId:
      apiSummary?.sampleExternalId ||
      transferContext?.externalId ||
      null,
    accessionNumber:
      apiSummary?.accessionNumber ||
      transferContext?.accessionNumber ||
      null,
    sampleType: apiSummary?.sampleType || transferContext?.sampleType || null,
    quantity: apiSummary?.quantity ?? transferContext?.quantity ?? null,
    unitOfMeasure:
      apiSummary?.unitOfMeasure || transferContext?.unitOfMeasure || null,
    sampleCondition:
      apiSummary?.sampleCondition || transferContext?.sampleCondition || null,
    preservationMedium:
      apiSummary?.preservationMedium ||
      transferContext?.preservationMedium ||
      null,
    collectionDate:
      apiSummary?.collectionDate || transferContext?.collectionDate || null,
    biosafetyLevel: apiSummary?.biosafetyLevel || null,
    ethicsApprovalRef: apiSummary?.ethicsApprovalRef || null,
    originLab: apiSummary?.originLab || null,
    principalInvestigator: apiSummary?.principalInvestigator || null,
    projectId: apiSummary?.projectId || null,
  };
}

export function formatSummaryField(label, value, formatter) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const displayValue = formatter ? formatter(value) : value;
  return { label, value: displayValue };
}

export function buildLifecycleSummaryRows(summary) {
  if (!summary) {
    return [];
  }

  const rows = [
    formatSummaryField("Source department", summary.sourceLab, formatTransferSourceLab),
    formatSummaryField("Sample ID", summary.sampleExternalId || summary.accessionNumber),
    formatSummaryField("Accession", summary.accessionNumber && summary.sampleExternalId ? summary.accessionNumber : null),
    formatSummaryField("Sample type", summary.sampleType),
    formatSummaryField("Collection date", summary.collectionDate, formatDateValue),
    formatSummaryField(
      "Volume",
      summary.quantity,
      (value) => formatQuantityWithUnit(value, summary.unitOfMeasure),
    ),
    formatSummaryField("Sample condition", summary.sampleCondition),
    formatSummaryField("Preservative/Medium", summary.preservationMedium),
    formatSummaryField("Project", summary.projectName || summary.projectId),
    formatSummaryField("Transfer reason", summary.transferReason),
    formatSummaryField("Requested by", summary.requestedByName),
    formatSummaryField("Requested at", summary.requestedTimestamp, formatDateValue),
    formatSummaryField("Transfer status", summary.transferStatus),
    formatSummaryField("Item status", summary.itemStatus),
    formatSummaryField("Biosafety level", summary.biosafetyLevel),
    formatSummaryField("Ethics approval", summary.ethicsApprovalRef),
    formatSummaryField("Origin lab", summary.originLab),
    formatSummaryField("Principal investigator", summary.principalInvestigator),
    formatSummaryField("Rejection reason", summary.rejectionReason),
  ];

  return rows.filter(Boolean);
}

export function shouldShowWorkflowTransition(event) {
  return Boolean(event?.fromWorkflowStatus || event?.toWorkflowStatus);
}

export function shouldShowLocationTransition(event) {
  return Boolean(event?.fromLocationDisplay || event?.toLocationDisplay);
}

function formatDateValue(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
