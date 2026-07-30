export type AnalyzerStatus =
  | "INACTIVE"
  | "SETUP"
  | "VALIDATION"
  | "ACTIVE"
  | "ERROR_PENDING"
  | "OFFLINE"
  | "PENDING_REGISTRATION";

export type AnalyzerProtocol = "ASTM" | "HL7" | "FILE" | string;

export interface Analyzer {
  id?: string;
  name?: string;
  analyzerType?: string;
  type?: string;
  ipAddress?: string;
  port?: number | string;
  importDirectory?: string;
  testUnitIds?: Array<string | number>;
  active?: boolean;
  status?: AnalyzerStatus;
  lifecycleStage?: AnalyzerStatus;
  lastModified?: string;
  pluginLoaded?: boolean;
  protocol?: AnalyzerProtocol;
}

export interface AnalyzerApiError {
  error?: string;
  message?: string;
  status?: number;
  statusCode?: number;
  statusText?: string;
  success?: boolean;
  messageKey?: string;
  errorKey?: string;
  messageArgs?: Record<string, unknown>;
  errorArgs?: Record<string, unknown>;
  fieldErrors?: Array<{
    field?: string;
    defaultMessage?: string;
  }>;
}

export type AnalyzerApiResponse = AnalyzerApiError & Record<string, unknown>;
