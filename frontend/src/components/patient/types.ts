import type { ReactNode } from "react";

export type Nullable<T> = T | null;

export interface PatientAddress {
  street?: string;
  city?: string;
  [key: string]: unknown;
}

export interface PatientContactPerson {
  firstName?: string;
  lastName?: string;
  primaryPhone?: string;
  email?: string;
}

export interface PatientContact {
  id?: string;
  firstName?: string;
  lastName?: string;
  primaryPhone?: string;
  email?: string;
  person?: PatientContactPerson;
}

export interface PatientDataSummary {
  activeOrders?: number;
  totalOrders?: number;
  totalResults?: number;
  totalSamples?: number;
}

export interface PatientIdentifier {
  identityType?: string;
  identityValue?: string;
}

export interface PatientRecord {
  id?: string;
  patientPK?: string;
  patientID?: string;
  patientId?: string;
  STnumber?: string;
  stNumber?: string;
  subjectNumber?: string;
  nationalId?: string;
  externalId?: string;
  guid?: string;
  lastName?: string;
  firstName?: string;
  aka?: string;
  mothersName?: string;
  mothersInitial?: string;
  streetAddress?: string;
  streetName?: string;
  flatNumberApartmentName?: string;
  city?: string;
  commune?: string;
  county?: string;
  town?: string;
  postalCode?: string;
  address?: string | PatientAddress;
  addressDepartment?: string;
  addressHierarchy?: Record<string, string | null | undefined>;
  gender?: string;
  dob?: string;
  birthDate?: string;
  birthdate?: string;
  birthDateForDisplay?: string;
  patientType?: string;
  insuranceNumber?: string;
  occupation?: string;
  phoneNumber?: string;
  primaryPhone?: string;
  contactPhone?: string;
  email?: string;
  contactEmail?: string;
  healthRegion?: string;
  education?: string;
  maritalStatus?: string;
  maritialStatus?: string;
  nationality?: string;
  healthDistrict?: string;
  otherNationality?: string;
  dataSourceName?: string;
  dataSummary?: PatientDataSummary;
  identifiers?: PatientIdentifier[];
  patientContact?: PatientContact;
  contact?: PatientContact;
  photo?: string;
  patientUpdateStatus?: string;
  [key: string]: unknown;
}

export interface PatientSearchCriteria {
  patientId?: string;
  firstName?: string;
  lastName?: string;
  gender?: string;
  dateOfBirth?: string;
  labNumber?: string;
  guid?: string;
  suppressExternalSearch?: boolean | string;
  crSearch?: boolean;
}

export interface PatientSearchResponse {
  patientSearchResults?: PatientRecord[];
  paging?: {
    totalPages?: string | number;
    currentPage?: string | number;
  };
  nextPage?: string | null;
  previousPage?: string | null;
  currentPage?: number | null;
  totalPages?: number | null;
  [key: string]: unknown;
}

export interface PatientMergeRequest {
  patient1Id?: string;
  patient2Id?: string;
  primaryPatientId?: string | null;
  reason?: string;
}

export interface PatientMergeResult {
  mergedPatientId?: string;
  primaryPatientId?: string;
  auditId?: string;
  [key: string]: unknown;
}

export interface PatientMergeApiError {
  status?: number;
  message?: string;
  [key: string]: unknown;
}

export interface AddressHierarchyLevel {
  id?: string;
  level?: number;
  name?: string;
  typeName?: string;
  bindKey?: string;
  [key: string]: unknown;
}

export interface AddressSearchResult {
  id?: string;
  hierarchyLevels?: AddressHierarchyLevel[];
}

export type PatientSelectHandler = (patient: Nullable<PatientRecord>) => void;

export interface PatientPanelTitleProps {
  title: ReactNode;
}
