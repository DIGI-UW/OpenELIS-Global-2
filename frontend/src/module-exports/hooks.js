/**
 * Core hooks modules may use (`openelis_core/hooks`). Additions are
 * frontend-contract minors; removals and signature changes are majors.
 */
export { default as useInAppNavigation } from "../components/common/useInAppNavigation";
export {
  useUrlFilters,
  useUrlFilterAutoRun,
} from "../components/common/useUrlFilters";
export { default as useActiveLocales } from "../components/common/useActiveLocales";
export { default as useDomains } from "../components/common/useDomains";
