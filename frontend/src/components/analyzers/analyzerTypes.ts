import type {
  Analyzer as SharedAnalyzer,
  AnalyzerApiResponse as SharedAnalyzerApiResponse,
  AnalyzerProtocol,
} from "./types";

export type { AnalyzerProtocol } from "./types";

export type Analyzer = SharedAnalyzer & {
  analyzerTypeId?: string;
  pluginTypeId?: string;
  protocolVersion?: string | null;
  communicationMode?: string | null;
  identifierPattern?: string;
  fileFormat?: string | null;
  filePattern?: string | null;
  columnMappings?: Record<string, unknown> | string | null;
  delimiter?: string | null;
  hasHeader?: boolean | null;
  skipRows?: number | null;
};

export interface AnalyzerPluginType {
  id: string;
  name: string;
  protocol: AnalyzerProtocol;
  isGenericPlugin?: boolean;
}

export interface AnalyzerDefaultConfig {
  id: string;
  analyzerName?: string;
  protocol?: AnalyzerProtocol;
}

export interface AnalyzerFormValues {
  name: string;
  analyzerType: string;
  pluginTypeId: string;
  ipAddress: string;
  port: string;
  protocolVersion: string;
  communicationMode: string;
  testUnitIds: string[];
  status: string;
  identifierPattern: string;
  importDirectory: string;
  fileFormat: string;
  filePattern: string;
  columnMappings: string;
  delimiter: string;
  hasHeader: boolean;
  skipRows: number;
}

export type AnalyzerFormErrors = Partial<
  Record<keyof AnalyzerFormValues, string>
>;

export interface AnalyzerNotification {
  kind: "error" | "info" | "success" | "warning";
  title: string;
  subtitle?: string;
}

export interface AnalyzerField {
  id: string;
  fieldName?: string;
  astmRef?: string;
  unit?: string;
  [key: string]: unknown;
}

export interface FieldMappingRecord {
  id?: string;
  analyzerFieldId?: string;
  mappingType?: string;
  isRequired?: boolean;
  [key: string]: unknown;
}

export interface PendingCode {
  id?: string;
  code?: string;
  status?: string;
  [key: string]: unknown;
}

export interface SerialPortConfiguration {
  id?: string;
  analyzerId: string | number | null;
  portName: string;
  baudRate: number;
  dataBits: number;
  stopBits: string;
  parity: string;
  flowControl: string;
  active: boolean;
}

export type AnalyzerApiResponse = SharedAnalyzerApiResponse;
