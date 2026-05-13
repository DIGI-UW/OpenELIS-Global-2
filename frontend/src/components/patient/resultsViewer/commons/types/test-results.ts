export type ConceptUuid = string;
export type ObsUuid = string;

export interface ObsRecord {
  members?: Array<ObsRecord>;
  hasMember?: Array<{ reference: string }>;
  conceptClass: ConceptUuid;
  meta?: ObsMetaInfo;
  effectiveDateTime: string;
  issued?: string;
  encounter: {
    reference: string;
    type: string;
  };
  value?: string | number;
  valueQuantity?: { value: number | string };
  valueCodeableConcept?: { coding: Array<{ display: string }> };
  name?: string;
  id?: ObsUuid;
  [_: string]: unknown;
}

export interface ObsMetaInfo {
  range?: string;
  [_: string]: unknown;
  assessValue?: (value: string) => OBSERVATION_INTERPRETATION;
}

export interface ConceptRecord {
  uuid: ConceptUuid;
  display?: string;
  conceptClass?: { name: string; display?: string };
  datatype?: { display: string };
  [_: string]: unknown;
}

export interface PatientData {
  [_: string]: {
    entries: Array<ObsRecord>;
    type: "LabSet" | "Test";
    uuid: string;
  };
}

export type OBSERVATION_INTERPRETATION =
  | "NORMAL"
  | "HIGH"
  | "CRITICALLY_HIGH"
  | "OFF_SCALE_HIGH"
  | "LOW"
  | "CRITICALLY_LOW"
  | "OFF_SCALE_LOW"
  | "--";

export interface ExternalOverviewProps {
  patientUuid: string;
  filter: (filterProps: PanelFilterProps) => boolean;
}

export type PanelFilterProps = [ObsRecord, string, string, string];
