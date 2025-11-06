export * from "./test-results";
export * from "./workspace";

export interface DashbardConfig {
  columns: number;
}

export interface DashboardLinkConfig {
  title: string;
}

export interface DashboardConfig extends DashboardLinkConfig {
  slot: string;
  config: DashbardConfig;
}

export interface PatientProgram {
  uuid: string;
  display: string;
  patient: OpenmrsResource;
  program: OpenmrsResource;
  dateEnrolled: string;
  dateCompleted: string;
  location: OpenmrsResource;
}

export interface OpenmrsResource {
  uuid: string;
  display?: string;
  [anythingElse: string]: any;
}

export type PatientUuid = string | null;

export interface FetchResponse<T = any> extends Response {
  data: T;
}
