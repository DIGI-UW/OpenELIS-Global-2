/**
 * Parse a quantity value that may include units (e.g. "5.0 mL").
 */
export function parseQuantityValue(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : null;
  }
  const str = String(value).trim();
  const match = str.match(/^([\d.]+)/);
  if (!match) {
    return null;
  }
  const parsed = parseFloat(match[1]);
  return Number.isFinite(parsed) ? parsed : null;
}

/**
 * Format quantity with optional unit for display.
 */
export function formatQuantityWithUnit(quantity, unit) {
  const parsed = parseQuantityValue(quantity);
  if (parsed === null) {
    return unit ? `- ${unit}` : "-";
  }
  const qtyText = Number.isInteger(parsed) ? String(parsed) : String(parsed);
  return unit ? `${qtyText} ${unit}` : qtyText;
}

/**
 * Compare two numeric quantities safely.
 */
export function isQuantityWithinAvailable(requested, available) {
  const req = parseQuantityValue(requested);
  const avail = parseQuantityValue(available);
  if (req === null || avail === null) {
    return false;
  }
  return req > 0 && req <= avail;
}

/**
 * Validate per-sample retrieval quantity before submit.
 */
export function validateRetrievalQuantity(sample, sampleId = sample?.id) {
  const errors = [];
  const id = sampleId || "unknown";
  const requested = parseQuantityValue(sample?.quantityRequested);
  const available = parseQuantityValue(
    sample?.remainingQuantity ?? sample?.quantity,
  );

  if (requested === null || requested <= 0) {
    errors.push(`Sample ${id}: quantity requested must be greater than zero`);
  } else if (available !== null && requested > available) {
    errors.push(
      `Sample ${id}: quantity requested (${requested}) exceeds available (${available})`,
    );
  }

  return errors;
}

/**
 * Build retrieval items payload for create request API.
 */
export function buildRetrievalItemsPayload(selectedSamples) {
  return (selectedSamples || []).map((sample) => ({
    bioSampleId: sample.id,
    quantityRequested: parseQuantityValue(sample.quantityRequested),
    unitOfMeasure: sample.unitOfMeasure || null,
    remark: sample.remark || null,
  }));
}
