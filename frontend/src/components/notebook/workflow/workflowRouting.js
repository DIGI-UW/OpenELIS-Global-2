import BacteriologyWorkflowTab from "./BacteriologyWorkflowTab";
import BioanalyticalWorkflowTab from "./BioanalyticalWorkflowTab";
import BioequivalenceWorkflowTab from "./BioequivalenceWorkflowTab";
import BiorepositoryWorkflowTab from "./BiorepositoryWorkflowTab";
import GBDWorkflowTab from "./GBDWorkflowTab";
import ImmunologyWorkflowTab from "./ImmunologyWorkflowTab";
import MedLabWorkflowTab from "./MedLabWorkflowTab";
import MNTDWorkflowTab from "./MNTDWorkflowTab";
import NotebookWorkflowTab from "./NotebookWorkflowTab";
import PathologyWorkflowTab from "./PathologyWorkflowTab";
import PharmaceuticalWorkflowTab from "./PharmaceuticalWorkflowTab";
import TBWorkflowTab from "./TBWorkflowTab";
import TraditionalMedicineWorkflowTab from "./TraditionalMedicineWorkflowTab";
import ViralVaccineWorkflowTab from "./ViralVaccineWorkflowTab";
import VirologyLabWorkflowTab from "./VirologyLabWorkflowTab";
import { normalizeWorkflowType } from "../../../constants/ahriWorkflowRegistry";

const WORKFLOW_TAB_BY_TYPE = {
  bacteriology: BacteriologyWorkflowTab,
  bioanalytical: BioanalyticalWorkflowTab,
  bioequivalence: BioequivalenceWorkflowTab,
  biorepository: BiorepositoryWorkflowTab,
  gbd: GBDWorkflowTab,
  genomics: GBDWorkflowTab,
  immunology: ImmunologyWorkflowTab,
  medlab: MedLabWorkflowTab,
  medical_laboratory: MedLabWorkflowTab,
  mntd: MNTDWorkflowTab,
  pathology: PathologyWorkflowTab,
  pharmaceutical: PharmaceuticalWorkflowTab,
  pharmaceuticals: PharmaceuticalWorkflowTab,
  traditional_medicine: TraditionalMedicineWorkflowTab,
  tuberculosis: TBWorkflowTab,
  tb: TBWorkflowTab,
  viral_vaccine: ViralVaccineWorkflowTab,
  virology: VirologyLabWorkflowTab,
};

/**
 * Resolves workflowType from notebook metadata only (never from title).
 * @param {object} notebook
 * @returns {string}
 */
export function resolveWorkflowType(notebook) {
  const explicit = normalizeWorkflowType(notebook?.workflowType);
  if (explicit) {
    return explicit;
  }
  if (notebook?.title && process.env.NODE_ENV === "development") {
    console.warn(
      `[workflowRouting] Missing workflowType for notebook "${notebook.title}"; using generic tab`,
    );
  }
  return "";
}

/**
 * @param {object} notebook
 * @returns {React.ComponentType|null}
 */
export function resolveWorkflowTabComponent(notebook) {
  const workflowType = resolveWorkflowType(notebook);
  if (workflowType && WORKFLOW_TAB_BY_TYPE[workflowType]) {
    return WORKFLOW_TAB_BY_TYPE[workflowType];
  }
  if (workflowType) {
    console.warn(
      `[workflowRouting] Unknown workflowType "${workflowType}" for "${notebook?.title}"; using generic tab`,
    );
  } else if (notebook?.title) {
    console.warn(
      `[workflowRouting] No workflowType for "${notebook.title}"; using generic tab`,
    );
  }
  return NotebookWorkflowTab;
}

export { WORKFLOW_TAB_BY_TYPE };
