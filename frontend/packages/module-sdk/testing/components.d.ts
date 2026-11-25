/**
 * The `openelis_core/components` doubles (components.jsx): the shell's
 * surface, each rendering a plain element with the id and label a page sees.
 */
import type { PatientRecord } from "openelis_core/components";

export * from "openelis_core/components";

/** The patients the SearchPatientForm double offers, one button each; clicking one selects it. */
export const testPatients: PatientRecord[];
