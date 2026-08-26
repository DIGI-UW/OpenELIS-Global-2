export type AnalyzerStatus =
  | "INACTIVE"
  | "SETUP"
  | "VALIDATION"
  | "ACTIVE"
  | "ERROR_PENDING"
  | "OFFLINE";

export type AnalyzerProtocol = "ASTM" | "HL7" | "FILE" | string;

export interface AnalyzerProfileRef {
  profileId: string;
  revision: number;
  fingerprint: string;
}

export interface AnalyzerConnectionField {
  key: string;
  labelKey: string;
  helpTextKey?: string;
  inputKind: "TEXT" | "NUMBER" | "SELECT" | "BOOLEAN" | "SECRET" | "FILE_PATH";
  required: boolean;
  defaultValue: unknown;
  currentValue?: unknown;
  isSet?: boolean;
  maskedValue?: string;
  choices: Array<{ value: string; labelKey: string }>;
  visibleWhen?: {
    fieldKey: string;
    operator: "EQUALS" | "NOT_EQUALS" | "IN" | "NOT_IN";
    value: unknown;
  };
  validationErrors: string[];
}

export interface AnalyzerConnection {
  schemaVersion: "1.0";
  connectionId: string;
  clientAnalyzerId: string;
  displayName: string;
  profileRef: AnalyzerProfileRef;
  configRevision: number;
  configFingerprint: string;
  fields: AnalyzerConnectionField[];
  readiness: {
    ready: boolean;
    blockers: Array<{
      key: string;
      messageKey: string;
      fieldKeys?: string[];
    }>;
  };
  latestProbe?: {
    requestId: string;
    configRevision: number;
    status: string;
    completedAt: string;
  } | null;
  desiredRuntimeState: string;
  actualRuntimeState: string;
  updatedAt: string;
}

export interface Analyzer {
  id?: string;
  name?: string;
  analyzerType?: string;
  type?: string;
  testUnitIds?: Array<string | number>;
  active?: boolean;
  status?: AnalyzerStatus;
  lifecycleStage?: AnalyzerStatus;
  lastModified?: string;
  pluginLoaded?: boolean;
  protocol?: AnalyzerProtocol;
  profileId?: string | null;
  profileRevision?: number | null;
  profileFingerprint?: string | null;
  profileBindingStatus?: "PINNED" | "UNBOUND" | string;
  bridgeConnectionId?: string | null;
  connected?: boolean;
  connection?: AnalyzerConnection;
  connectionErrorKey?: string;
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
