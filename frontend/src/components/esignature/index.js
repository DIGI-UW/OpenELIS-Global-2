/**
 * Electronic Signature Components
 *
 * Provides reusable components for 21 CFR Part 11 compliant electronic signatures.
 *
 * Usage:
 * ```jsx
 * import { ESignatureButton, SignatureMeaning } from "../esignature";
 *
 * <ESignatureButton
 *   meaning={SignatureMeaning.AUTHORED}
 *   context="Sign 3 result(s) as authored"
 *   recordType="RESULT"
 *   recordId={123}
 *   onSign={(signature) => handleResultSigned(signature)}
 *   label="Save"
 * />
 * ```
 */

export { default as ESignatureButton } from "./ESignatureButton";
export { default as ESignatureModal } from "./ESignatureModal";
export { default as useESign } from "./useESign";
export {
  SignatureMeaning,
  AuthMethod,
  DEFAULT_CERTIFICATION_TEXT,
  isEsigEnabled,
  isUserCertified,
  getSessionStatus,
  getSignaturesForRecord,
  executeSignature,
  certifyUser,
  getAllCertifications,
  revokeCertification,
  ESignatureError,
} from "./api";
