export type ConceptUuid = string;
export type ObsUuid = string;

export interface ObsRecord {
  members?: Array<ObsRecord>;
  conceptClass: ConceptUuid;
  meta?: ObsMetaInfo;
  effectiveDateTime: string;
  encounter: {
    reference: string;
    type: string;
  };
  [_: string]: any;
}

export interface ObsMetaInfo {
  [_: string]: any;
  assessValue?: (value: string) => OBSERVATION_INTERPRETATION;
}

export interface ConceptRecord {
  uuid: ConceptUuid;
  [_: string]: any;
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
