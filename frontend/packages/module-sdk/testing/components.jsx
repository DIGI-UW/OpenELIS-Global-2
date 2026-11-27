/**
 * Doubles for `openelis_core/components`: each renders a plain element that
 * carries the same id and label a page would see, and calls the same
 * callbacks, so a page test can type into a date field or pick a patient
 * without the shell's providers, Carbon or the patient search behind it.
 */
import React, { useContext } from "react";
import { NotificationContext } from "./contexts.js";

export const PatientHeader = (props) => (
  <div data-testid="patient-header">
    {props.id
      ? `${props.lastName || ""}, ${props.firstName || ""}`
      : "no patient"}
  </div>
);

/** Renders a button per entry of `testPatients`; clicking one selects it. */
export const testPatients = [];

export const SearchPatientForm = ({ getSelectedPatient, idPrefix = "" }) => (
  <div data-testid={`${idPrefix}-search-patient-form`}>
    {testPatients.map((patient) => (
      <button
        key={patient.patientPK || patient.id}
        type="button"
        onClick={() => getSelectedPatient(patient)}
      >
        {patient.lastName}, {patient.firstName}
      </button>
    ))}
  </div>
);

const labelled = (id, labelText, control) => (
  <div>
    {labelText ? <label htmlFor={id}>{labelText}</label> : null}
    {control}
  </div>
);

export const CustomDatePicker = ({
  id,
  labelText,
  value,
  onChange,
  invalid,
  invalidText,
  disabled,
}) =>
  labelled(
    id,
    labelText,
    <>
      <input
        id={id}
        type="text"
        value={value || ""}
        disabled={disabled}
        aria-invalid={invalid || undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {invalid && invalidText ? <span role="alert">{invalidText}</span> : null}
    </>,
  );

export const CustomTimePicker = ({ id, labelText, value, onChange }) =>
  labelled(
    id,
    labelText,
    <input
      id={id}
      type="text"
      value={value || ""}
      onChange={(event) => onChange(event.target.value)}
    />,
  );

export const CustomLabNumberInput = ({
  id,
  labelText,
  value,
  onChange,
  name,
}) =>
  labelled(
    id,
    labelText,
    <input
      id={id}
      name={name}
      type="text"
      value={value || ""}
      onChange={(event) => onChange(event, event.target.value)}
    />,
  );

export const CustomSelect = ({
  id,
  labelText,
  value,
  onChange,
  options = [],
  placeholder = "Select...",
  disabled,
}) =>
  labelled(
    id,
    labelText,
    <select
      id={id}
      value={value ?? ""}
      disabled={disabled}
      onChange={(event) => onChange && onChange(event.target.value)}
    >
      <option value="">{placeholder}</option>
      {(options || []).map((option) => (
        <option key={option.id} value={option.id}>
          {option.value}
        </option>
      ))}
    </select>,
  );

export const CustomTextInput = ({
  id,
  labelText,
  value,
  defaultValue,
  onChange,
}) =>
  labelled(
    id,
    labelText,
    <input
      id={id}
      type="text"
      defaultValue={value ?? defaultValue ?? ""}
      onChange={(event) => onChange && onChange(event.target.value)}
    />,
  );

export const CustomCheckBox = ({ id, label, checked, onChange }) =>
  labelled(
    id,
    label,
    <input
      id={id}
      type="checkbox"
      checked={!!checked}
      onChange={(event) => onChange(event.target.checked)}
    />,
  );

export const AutoComplete = ({
  id,
  label,
  suggestions = [],
  value,
  onSelect,
}) =>
  labelled(
    id,
    label,
    <select
      id={id}
      value={value ?? ""}
      onChange={(event) => onSelect && onSelect(event.target.value)}
    >
      <option value="" />
      {suggestions.map((suggestion) => (
        <option key={suggestion.id} value={suggestion.id}>
          {suggestion.value}
        </option>
      ))}
    </select>,
  );

export const PageBreadCrumb = ({ breadcrumbs = [] }) => (
  <nav aria-label="breadcrumb">
    {breadcrumbs.map((crumb, index) => (
      <span key={index}>{crumb.text || crumb.label}</span>
    ))}
  </nav>
);

export const ConfirmPopup = ({ isOpen, onClose, onConfirm, messageCode }) =>
  isOpen ? (
    <div role="dialog">
      <p>{messageCode}</p>
      <button type="button" onClick={onConfirm}>
        Confirm
      </button>
      <button type="button" onClick={onClose}>
        Cancel
      </button>
    </div>
  ) : null;

export const Spinner = () => <div role="status">loading</div>;

export const PageTitle = ({ breadcrumbs = [], subtitle }) => (
  <header>
    <h1>
      {breadcrumbs.length ? breadcrumbs[breadcrumbs.length - 1].label : ""}
    </h1>
    {subtitle ? <p>{subtitle}</p> : null}
  </header>
);

export const RouteErrorBoundary = ({ children }) => <>{children}</>;

export const ServerPageArrows = ({
  currentPage,
  totalPages,
  onPrevious,
  onNext,
  previousDisabled,
  nextDisabled,
}) => (
  <div>
    <button type="button" onClick={onPrevious} disabled={previousDisabled}>
      previous
    </button>
    <span>
      {currentPage} / {totalPages}
    </span>
    <button type="button" onClick={onNext} disabled={nextDisabled}>
      next
    </button>
  </div>
);

export const NotificationKinds = Object.freeze({
  info: "info",
  error: "error",
  success: "success",
  warning: "warning",
});

/** Lists the pending notifications of the (double) NotificationContext. */
export const AlertDialog = () => {
  const context = useContext(NotificationContext);
  const notifications = (context && context.notifications) || [];
  return (
    <div data-testid="alert-dialog">
      {notifications.map((notification, index) => (
        <div
          key={notification.id ?? index}
          role="alert"
          data-kind={notification.kind}
        >
          <strong>{notification.title}</strong> {notification.message}
        </div>
      ))}
    </div>
  );
};

export const RequiredMarker = ({ required = true }) =>
  required ? <span aria-hidden="true"> *</span> : null;

export const requiredProps = (required = true) =>
  required ? { "aria-required": true } : {};
