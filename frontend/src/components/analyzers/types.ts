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
  pluginTypeId?: string;
  analyzerTypeId?: string;
  protocolVersion?: string;
  communicationMode?: string;
  identifierPattern?: string;
  fileFormat?: string;
  filePattern?: string;
  columnMappings?: Record<string, unknown> | string;
  delimiter?: string;
  hasHeader?: boolean;
  skipRows?: number;
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

export interface AnalyzerType {
  id?: string;
  name?: string;
  description?: string;
  protocol?: AnalyzerProtocol;
  pluginClassName?: string;
  identifierPattern?: string;
  isGenericPlugin?: boolean;
  pluginLoaded?: boolean;
  instanceCount?: number;
  isActive?: boolean;
}

export interface AnalyzerDefaultConfig {
  id?: string;
  analyzerName?: string;
  analyzer_name?: string;
  protocol?: AnalyzerProtocol | { format?: string };
  identifier_pattern?: string;
  category?: string;
  communication_mode?: string;
  supported_extensions?: string[];
  configDefaults?: {
    fileFormat?: string;
    delimiter?: string;
    hasHeader?: boolean;
    skipRows?: number;
  };
  profileMeta?: {
    category?: string;
    displayName?: string;
  };
  communication?: {
    mode?: string;
  };
  column_mapping?: Record<string, unknown>;
  error?: string;
}

export interface AnalyzerField {
  id?: string;
  fieldName?: string;
  astmRef?: string;
  fieldType?: string;
  displayName?: string;
  unit?: string;
  analyzerId?: string;
  analyzer?: Analyzer;
  customFieldType?: {
    displayName?: string;
    typeName?: string;
  };
  isRequired?: boolean;
  [key: string]: unknown;
}

export interface AnalyzerMapping {
  id?: string;
  analyzerFieldId?: string;
  mappingType?: string;
  openElisFieldId?: string;
  openElisFieldName?: string;
  openelisFieldId?: string;
  openelisFieldName?: string;
  openelisFieldType?: string;
  isRequired?: boolean;
  isActive?: boolean;
  validationRules?: Array<{
    id?: string;
    ruleName?: string;
    ruleType?: string;
    errorMessage?: string;
  }>;
  unitMapping?:
    | string
    | {
        analyzerUnit?: string;
        openelisUnit?: string;
        conversionFactor?: number;
      };
  [key: string]: unknown;
}

export interface PendingCode {
  id?: string;
  code?: string;
  analyzerTestName?: string;
  seenCount?: number;
  status?: string;
  [key: string]: unknown;
}

export interface AnalyzerErrorRecord {
  id?: string;
  timestamp?: string;
  createdDate?: string;
  analyzerId?: string;
  analyzerName?: string;
  analyzer?: Analyzer;
  errorType?: string;
  severity?: string;
  errorMessage?: string;
  message?: string;
  status?: string;
  acknowledgedBy?: string;
  acknowledgedDate?: string;
  analyzerLogs?: Array<{
    timestamp?: string;
    level?: string;
    message?: string;
  }>;
  [key: string]: unknown;
}

export interface AnalyzerNotification {
  kind: "error" | "success" | "info" | "warning";
  title: string;
  subtitle?: string;
}
