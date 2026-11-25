/**
 * The shell's surface as a module sees it: the five remote entry points of
 * `openelis_core`, declared as ambient modules so a module keeps its import
 * paths and `tsc` checks the names and props it uses (docs/modules.md).
 *
 * The runtime is JavaScript under frontend/src/module-exports; a core test
 * keeps every runtime export declared here. Types marked "(inferred)" are read
 * from the component's use of its props rather than from a TypeScript source.
 */

declare module "openelis_core/api" {
  import type { UseQueryResult } from "@tanstack/react-query";

  export type LegacyApiResponse = any;
  export type RequestPayload = BodyInit | Record<string, unknown> | null;

  /** What `hasRole` and friends read from a session. */
  export interface RoleBearer {
    roles?: string[];
    permissions?: string[];
  }

  /**
   * The body a JSON write resolves with when the server refuses it (the error
   * body plus the status fields), or the network-error form with status 0.
   */
  export interface ApiFailure {
    error?: string;
    message?: string;
    status: number;
    statusCode?: number;
    statusText?: string;
    fieldErrors?: Array<{
      field?: string;
      defaultMessage?: string;
      message?: string;
    }>;
    [key: string]: unknown;
  }

  /** The callback receives undefined on a failed, non-JSON or aborted read. */
  export function getFromOpenElisServer<T = LegacyApiResponse>(
    endPoint: string,
    callback: (response: T | undefined) => void,
    signal?: AbortSignal | null,
  ): void;
  /** Rejects on a non-2xx answer or a non-JSON body. */
  export function fetchFromOpenElisServer<T>(
    endPoint: string,
    signal?: AbortSignal,
  ): Promise<T>;
  /** The callback receives the HTTP status: 0 on a network error, 401 when answered with the login page. */
  export function postToOpenElisServer<TExtra = unknown>(
    endPoint: string,
    payLoad: RequestPayload,
    callback: (status: number, extraParams?: TExtra) => void,
    extraParams?: TExtra,
  ): void;
  /** Never rejects: a refusal arrives as an ApiFailure in the callback. */
  export function postToOpenElisServerJsonResponse<
    T = LegacyApiResponse,
    TExtra = unknown,
  >(
    endPoint: string,
    payLoad: RequestPayload,
    callback: (
      response: T | ApiFailure | undefined,
      extraParams?: TExtra,
    ) => void,
    extraParams?: TExtra,
  ): void;
  export function postToOpenElisServerFullResponse<TExtra = unknown>(
    endPoint: string,
    payLoad: RequestPayload,
    callback: (response: Response | undefined, extraParams?: TExtra) => void,
    extraParams?: TExtra,
  ): void;
  export function putToOpenElisServer(
    endPoint: string,
    payLoad: RequestPayload | undefined,
    callback: (status: number) => void,
  ): void;
  export function putToOpenElisServerJsonResponse<TExtra = unknown>(
    endPoint: string,
    payLoad: RequestPayload,
    callback: (json: any, extraParams?: TExtra) => void,
    extraParams?: TExtra,
  ): void;
  export function deleteFromOpenElisServer(
    endPoint: string,
    callback: (status: number) => void,
  ): void;
  export function hasRole(
    userSessionDetails: RoleBearer | null | undefined,
    role: string,
  ): boolean;
  export function hasPermission(
    userSessionDetails: RoleBearer | null | undefined,
    permission: string | null | undefined,
  ): boolean;
  export function hasPermissionOrGlobalAdmin(
    userSessionDetails: RoleBearer | null | undefined,
    permission: string,
  ): boolean;
  export const Roles: {
    readonly GLOBAL_ADMIN: "Global Administrator";
    readonly USER_ACCOUNT_ADMIN: "User Account Administrator";
    readonly AUDIT_TRAIL: "Audit Trail";
    readonly ANALYSER_IMPORT: "Analyser Import";
    readonly CYTOPATHOLOGIST: "Cytopathologist";
    readonly PATHOLOGIST: "Pathologist";
    readonly RECEPTION: "Reception";
    readonly RESULTS: "Results";
    readonly VALIDATION: "Validation";
    readonly REPORTS: "Reports";
  };
  export type RoleName = (typeof Roles)[keyof typeof Roles];

  export function fetchFromServer<T>(
    endPoint: string,
    signal?: AbortSignal,
  ): Promise<T>;
  export function serverQuery<T>(
    key: unknown[],
    endPoint: string,
  ): {
    queryKey: unknown[];
    queryFn: (context: { signal?: AbortSignal }) => Promise<T>;
  };
  /** Disabled while the endpoint is falsy; keeps the previous data while it refetches. */
  export function useServerData<T>(
    endPoint: string | null | undefined,
  ): UseQueryResult<T, unknown>;
  /** Without an endpoint, invalidates every useServerData read. */
  export function useInvalidateServerData(): (
    endPoint?: string,
  ) => Promise<void>;

  export const config: {
    title: string;
    loginRedirect: string;
    serverBaseUrl: string;
  };
}

declare module "openelis_core/hooks" {
  import type { MouseEvent } from "react";

  export type ClickLike = Pick<
    MouseEvent,
    | "defaultPrevented"
    | "button"
    | "metaKey"
    | "ctrlKey"
    | "shiftKey"
    | "altKey"
    | "preventDefault"
  >;
  /** Returns a click handler that pushes the href in the app's history on a plain left click. */
  export function useInAppNavigation(): (
    href: string,
  ) => (event?: ClickLike) => void;

  /** (inferred) An array default is a multi-value filter, comma-joined in the URL. */
  export function useUrlFilters<D extends Record<string, unknown>>(
    defaults: D,
  ): {
    values: {
      [K in keyof D]: D[K] extends unknown[] ? string[] | D[K] : string | D[K];
    };
    hasParams: boolean;
    setUrlFilters: (next: Record<string, unknown>) => void;
  };
  /** (inferred) Runs once on mount when shouldRun was true at mount. */
  export function useUrlFilterAutoRun(
    shouldRun: boolean,
    run: () => void,
  ): void;

  export interface ActiveLocale {
    id: string;
    localeCode: string;
    displayName: string;
    active: boolean;
    fallback: boolean;
    sortOrder: number;
  }
  /** (inferred) Empty until loaded, then sorted by sortOrder. */
  export function useActiveLocales(): ActiveLocale[];

  /** (inferred) Empty until loaded. */
  export function useDomains(): Array<{
    id: "CLINICAL" | "ENVIRONMENTAL" | "VECTOR";
    labelKey: string;
  }>;
}

declare module "openelis_core/contexts" {
  import type { Context, Dispatch, ReactNode, SetStateAction } from "react";

  export type LoginMethod = "FORM" | "SAML" | "OAUTH" | "CERT" | "NONE";
  /** (inferred) The /session answer; every field is optional because the value starts as {}. */
  export interface UserSessionDetails {
    authenticated?: boolean;
    loginMethod?: LoginMethod;
    userId?: string;
    loginName?: string;
    firstName?: string;
    lastName?: string;
    roles?: string[];
    permissions?: string[];
    userLabRolesMap?: Record<string, string[]>;
    csrf?: string;
    loginLabUnit?: string;
  }
  /** (inferred) What App.jsx provides. */
  export interface UserSessionContextValue {
    userSessionDetails: UserSessionDetails;
    errorLoadingSessionDetails: boolean;
    isCheckingLogin: () => boolean;
    logout: () => void;
    refresh: (callback?: () => void) => Promise<void>;
  }
  export const UserSessionDetailsContext: Context<UserSessionContextValue>;

  export interface Notification {
    kind: "info" | "error" | "success" | "warning";
    title: string;
    message: ReactNode;
    subtitle?: ReactNode;
    id?: number;
  }
  export interface NotificationContextValue {
    notificationVisible: boolean;
    setNotificationVisible: Dispatch<SetStateAction<boolean>>;
    notifications: Notification[];
    addNotification: (notification: Notification) => void;
    removeNotification: (id: number) => void;
    notificationShown?: (notification: Notification) => void;
  }
  /** Null outside the shell's Layout. */
  export const NotificationContext: Context<NotificationContextValue | null>;

  export interface ConfigurationContextValue {
    configurationProperties: Record<string, unknown>;
    reloadConfiguration: () => void;
    supportedLocales: Array<{
      id: string;
      localeCode: string;
      displayName: string;
      active: boolean;
      fallback: boolean;
      sortOrder: number;
    }>;
    enabledLanguages: Record<string, { label: string; fallback?: boolean }>;
  }
  /** Null outside the shell's Layout. */
  export const ConfigurationContext: Context<ConfigurationContextValue | null>;
}

declare module "openelis_core/sdk" {
  import type { ComponentType, ReactNode } from "react";
  import type { ApiFailure } from "openelis_core/api";
  import type {
    NotificationContextValue,
    UserSessionContextValue,
  } from "openelis_core/contexts";

  /** The frontend contract version the shell publishes; the SDK package carries the same number. */
  export const FRONTEND_CONTRACT_VERSION: string;

  export type ExtensionSlotName =
    | "home.dashboard.cards"
    | "patient.header.actions"
    | (string & {});
  /** Renders every started module's contribution to the slot, each with {...context, slot}. */
  export interface ExtensionSlotProps {
    name: ExtensionSlotName;
    context?: Record<string, unknown>;
    wrapper?: ComponentType<{ children?: ReactNode }>;
  }
  export function ExtensionSlot(props: ExtensionSlotProps): JSX.Element | null;

  /** (inferred) */
  export interface ModuleErrorBoundaryProps {
    moduleId?: string;
    title?: string;
    silent?: boolean;
    onError?: (error: unknown) => void;
    onRetry?: () => void;
    children?: ReactNode;
  }
  export const ModuleErrorBoundary: ComponentType<ModuleErrorBoundaryProps>;

  export function useSession(): UserSessionContextValue;
  /** The full permission key, `mod.<id>.<key>` for one of the manifest's or a core key; the shell does not expand it. */
  export function usePermission(key: string): boolean;
  export function useRole(role: string): boolean;
  /** {} outside the shell's Layout. */
  export function useConfigurationProperties(): Record<string, unknown>;
  export function useNotifications(): NotificationContextValue | null;

  /** (inferred) The same object on every render. */
  export interface OpenElisApi {
    /** Rejects on a non-2xx answer or a non-JSON body. */
    get<T = any>(endpoint: string, signal?: AbortSignal): Promise<T>;
    /**
     * Never rejects: a 2xx resolves with the parsed body, or {} when the body
     * is empty (a 201 or 204); a refusal resolves with an ApiFailure
     * (statusCode set), a network error with status 0.
     */
    post<T = any>(endpoint: string, body?: unknown): Promise<T | ApiFailure>;
    put<T = any>(endpoint: string, body?: unknown): Promise<T | ApiFailure>;
    /** Resolves with the HTTP status: 0 on a network error, 401 on the login page. */
    del(endpoint: string): Promise<number>;
  }
  export function useOpenElisApi(): OpenElisApi;

  /** The module's configuration rows (every value a string); {} while loading, on error or without an id. */
  export function useModuleConfig(
    moduleId: string | null | undefined,
  ): Record<string, string>;

  /** (inferred) */
  export interface ModuleRouteDeclaration {
    path: string;
    exact?: boolean;
    expose: string;
    permission?: string;
    role?: string;
  }
  export interface ModuleSlotDeclaration {
    slot: string;
    expose: string;
    order?: number;
    permission?: string;
  }
  export interface LoadedModule {
    id: string;
    name: string;
    version: string;
    kind: "provider" | "module";
    state: "STARTED";
    reason: string | null;
    description?: string;
    vendor?: { name?: string; url?: string; contact?: string };
    requires: {
      openelis?: string;
      coreApi?: string;
      spi?: string;
      frontendContract: string;
      modules?: Array<{ id: string; version: string }>;
    };
    permissions?: Array<{
      key: string;
      description?: string;
      defaultRoles?: string[];
    }>;
    menu?: Array<{
      elementId: string;
      parent?: string;
      displayKey: string;
      toolTipKey?: string;
      actionURL?: string;
      presentationOrder?: number;
      icon?: string;
      permission?: string;
    }>;
    frontend: {
      entry: string;
      messages?: string;
      routes?: ModuleRouteDeclaration[];
      slots?: ModuleSlotDeclaration[];
    };
    restPrefix?: string;
    routePrefix?: string;
    assetBase: string;
    remoteName: string;
    permissionPrefix: string;
    messages: Record<string, Record<string, string>>;
  }
  /** A started, contract-compatible module by id. */
  export function useModule(moduleId: string): LoadedModule | undefined;
}

declare module "openelis_core/components" {
  import type {
    ChangeEvent,
    ComponentType,
    Dispatch,
    ReactElement,
    ReactNode,
    SetStateAction,
  } from "react";

  /** (inferred) Without an id it renders the "no patient" line. */
  export interface PatientHeaderProps {
    id?: string | number;
    lastName?: string;
    firstName?: string;
    gender?: "M" | "F" | string;
    dob?: string;
    age?: string | number | null;
    patientName?: string | null;
    patientId?: string | null;
    subjectNumber?: string | null;
    nationalId?: string | null;
    /** Spelt as the component spells it. */
    accesionNumber?: string | null;
    orderDate?: string | null;
    referringFacility?: string | null;
    department?: string | null;
    requester?: string | null;
    isOrderPage?: boolean;
    statusTag?: ReactNode;
    assignedStaff?: Array<{ name: string; roleKey: string }> | null;
    className?: string;
  }
  export const PatientHeader: ComponentType<PatientHeaderProps>;

  export interface PatientRecord {
    id?: string;
    patientPK?: string;
    patientID?: string;
    patientId?: string;
    firstName?: string;
    lastName?: string;
    gender?: string;
    dob?: string;
    birthDateForDisplay?: string;
    nationalId?: string;
    subjectNumber?: string;
    STnumber?: string;
    photo?: string;
    [key: string]: unknown;
  }
  export interface SearchPatientFormProps {
    getSelectedPatient: (patient: PatientRecord) => void;
    setOrderFormValues?: Dispatch<SetStateAction<Record<string, unknown>>>;
    orderFormValues?: Record<string, unknown>;
    showPatientSearch?: boolean;
    patientSearchStatus?: boolean;
    /** Prefixes every element id, so two forms on one page stay apart. */
    idPrefix?: string;
    excludePatientIds?: string[];
    renderNotifications?: boolean;
    followUrlLabNumber?: boolean;
    nameMatch?: "prefix";
    [key: string]: unknown;
  }
  /** Needs the shell's providers (ConfigurationContext, NotificationContext, intl). */
  export const SearchPatientForm: ComponentType<SearchPatientFormProps>;

  /** (inferred) Value and onChange carry the site's display format (dd/MM/yyyy or MM/dd/yyyy). */
  export interface CustomDatePickerProps {
    id: string;
    onChange: (value: string) => void;
    value?: string;
    /** Follow `value` after the first render. */
    updateStateValue?: boolean;
    className?: string;
    labelText?: ReactNode;
    helperText?: ReactNode;
    invalid?: boolean;
    invalidText?: ReactNode;
    disallowFutureDate?: boolean;
    disallowPastDate?: boolean;
    futureDateText?: string;
    pastDateText?: string;
    disabled?: boolean;
    "aria-required"?: boolean;
  }
  export const CustomDatePicker: ComponentType<CustomDatePickerProps>;

  /** (inferred) onChange receives "HH:mm" or "". */
  export interface CustomTimePickerProps {
    id: string;
    onChange: (value: string) => void;
    value?: string;
    labelText?: ReactNode;
    className?: string;
  }
  export const CustomTimePicker: ComponentType<CustomTimePickerProps>;

  /** (inferred) Every other prop is passed to Carbon's TextInput. */
  export interface CustomLabNumberInputProps {
    id: string;
    name?: string;
    value?: string;
    labelText: ReactNode;
    onChange: (event: ChangeEvent<HTMLInputElement>, value?: string) => void;
    [carbonTextInputProp: string]: unknown;
  }
  export const CustomLabNumberInput: ComponentType<CustomLabNumberInputProps>;

  export interface SelectOption {
    id: string | number;
    value: string;
  }
  /** (inferred) */
  export interface CustomSelectProps {
    id: string;
    onChange?: (value: string) => void;
    value?: string | number;
    labelText?: ReactNode;
    placeholder?: string;
    options?: SelectOption[] | null;
    defaultSelect?: SelectOption;
    disabled?: boolean;
    invalid?: boolean;
    invalidText?: ReactNode;
    "aria-label"?: string;
    "aria-required"?: boolean;
  }
  export const CustomSelect: ComponentType<CustomSelectProps>;

  /** (inferred) `value` is the initial value only. */
  export interface CustomTextInputProps {
    id: string;
    labelText: ReactNode;
    onChange?: (value: string) => void;
    value?: string;
    defaultValue?: string;
  }
  export const CustomTextInput: ComponentType<CustomTextInputProps>;

  /** (inferred) */
  export interface CustomCheckBoxProps {
    id: string;
    label: ReactNode;
    onChange: (checked: boolean) => void;
    checked?: boolean;
  }
  export const CustomCheckBox: ComponentType<CustomCheckBoxProps>;

  /** (inferred) */
  export interface AutoCompleteProps {
    id: string;
    name?: string;
    label?: ReactNode;
    class?: string;
    suggestions: Array<{ id: string | number; value: string }>;
    value?: string | number;
    onSelect?: (id: string | number) => void;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    allowFreeText?: boolean;
    required?: boolean;
    invalidText?: ReactNode;
  }
  export const AutoComplete: ComponentType<AutoCompleteProps>;

  /** (inferred) `label` is a message id; `text` overrides it with literal text. */
  export interface BreadcrumbEntry {
    label?: string;
    text?: string;
    link?: string;
    isCurrentPage?: boolean;
  }
  export const PageBreadCrumb: ComponentType<{
    breadcrumbs: BreadcrumbEntry[];
  }>;

  /** (inferred) */
  export const ConfirmPopup: ComponentType<{
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    messageCode: string;
  }>;

  export const NotificationKinds: {
    readonly info: "info";
    readonly error: "error";
    readonly success: "success";
    readonly warning: "warning";
  };
  export type NotificationKind =
    (typeof NotificationKinds)[keyof typeof NotificationKinds];
  /** Shows the shell's pending notifications; needs NotificationContext. */
  export const AlertDialog: () => ReactElement;

  /** (inferred) */
  export const Spinner: ComponentType<{
    className?: string;
    path?: { fill?: string; className?: string };
    circle?: {
      fill?: string;
      className?: string;
      stroke?: string;
      cx?: string | number;
      cy?: string | number;
      r?: string | number;
      strokeWidth?: string | number;
    };
  }>;

  export interface PageTitleBreadcrumb {
    label: string;
    link?: string;
  }
  export interface PageTitleProps {
    breadcrumbs: PageTitleBreadcrumb[];
    showBackArrow?: boolean;
    onBack?: () => void;
    subtitle?: string;
  }
  export const PageTitle: (props: PageTitleProps) => JSX.Element;

  export const RequiredMarker: (props: {
    required?: boolean;
    announce?: boolean;
  }) => JSX.Element | null;
  export const requiredProps: (required?: boolean) => {
    "aria-required"?: true;
  };

  export const RouteErrorBoundary: ComponentType<{
    children?: ReactNode;
    titleKey?: string;
    messageKey?: string;
    onReload?: () => void;
  }>;

  export interface ServerPageArrowsProps {
    currentPage: number | string | null;
    totalPages: number | string | null;
    onPrevious: () => void;
    onNext: () => void;
    previousDisabled: boolean;
    nextDisabled: boolean;
  }
  export const ServerPageArrows: ComponentType<ServerPageArrowsProps>;
}
