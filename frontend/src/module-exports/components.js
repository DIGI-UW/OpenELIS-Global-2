/**
 * The core components modules may render (`openelis_core/components`). A
 * module imports them through the federation remote and so renders the same
 * component code, at the same version, as the core pages around it. Additions
 * here are frontend-contract minors; removals and prop changes are majors.
 */
export { default as PatientHeader } from "../components/common/PatientHeader";
export { default as SearchPatientForm } from "../components/patient/SearchPatientForm";
export { default as CustomDatePicker } from "../components/common/CustomDatePicker";
export { default as CustomTimePicker } from "../components/common/CustomTimePicker";
export { default as CustomLabNumberInput } from "../components/common/CustomLabNumberInput";
export { default as CustomSelect } from "../components/common/CustomSelect";
export { default as CustomTextInput } from "../components/common/CustomTextInput";
export { default as CustomCheckBox } from "../components/common/CustomCheckBox";
export { default as AutoComplete } from "../components/common/AutoComplete";
export { default as PageBreadCrumb } from "../components/common/PageBreadCrumb";
export { default as ConfirmPopup } from "../components/common/ConfirmPopup";
export {
  AlertDialog,
  NotificationKinds,
} from "../components/common/CustomNotification";
export { default as Spinner } from "../components/common/Spinner";
export { default as PageTitle } from "../components/common/PageTitle/PageTitle";
export {
  RequiredMarker,
  requiredProps,
} from "../components/common/RequiredMarker";
export { default as RouteErrorBoundary } from "../components/common/RouteErrorBoundary";
export { default as ServerPageArrows } from "../components/common/ServerPageArrows";
