export type AnalyzerProtocol = "ASTM" | "HL7" | "FILE" | string;

export interface Analyzer {
  id?: string;
  name?: string;
  analyzerType?: string;
  analyzerTypeId?: string;
  type?: string;
  pluginTypeId?: string;
  ipAddress?: string | null;
  port?: number | string | null;
  protocol?: AnalyzerProtocol;
  protocolVersion?: string | null;
  communicationMode?: string | null;
  testUnitIds?: string[];
  status?: string;
  identifierPattern?: string;
  importDirectory?: string | null;
  fileFormat?: string | null;
  filePattern?: string | null;
  columnMappings?: Record<string, unknown> | string | null;
  delimiter?: string | null;
  hasHeader?: boolean | null;
  skipRows?: number | null;
}

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

export interface AnalyzerApiResponse {
  error?: string;
  message?: string;
  status?: number | string;
  statusCode?: number;
  [key: string]: unknown;
}
