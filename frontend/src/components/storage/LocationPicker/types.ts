import type { ReactNode } from "react";

export const LEVEL_ORDER = ["room", "device", "shelf", "rack", "box"] as const;

export type LocationLevel = (typeof LEVEL_ORDER)[number];
export type PickerMode = "search" | "create";

export interface StorageLocationOption {
  id: string | number;
  name?: string;
  label?: string;
  hierarchicalPath?: string;
  type?: LocationLevel | string;
  [key: string]: unknown;
}

export interface SelectedLocation {
  id: string | number;
  name: string;
}

export type LocationSelection = Partial<
  Record<LocationLevel, SelectedLocation>
>;

export type TextPosition = {
  mode: "text";
  value: string;
};

export type GridPosition = {
  mode: "grid";
  row: string | number;
  column: string | number;
};

export type LocationPosition = TextPosition | GridPosition | null;

export interface StorageAssignment {
  selection: LocationSelection;
  position?: LocationPosition;
  hierarchicalPath?: string;
}

export interface PickerState {
  mode: PickerMode;
  selection: LocationSelection;
  position: LocationPosition;
  searchQuery: string;
  searchResults: StorageLocationOption[];
  initialAssignment: StorageAssignment | null;
  reason: string;
  notes: string;
  capacityWarning: string | null;
}

export type PickerAction =
  | { type: "SET_LEVEL"; level: LocationLevel; value?: SelectedLocation }
  | { type: "SET_POSITION"; position: LocationPosition }
  | { type: "SET_MODE"; mode: PickerMode }
  | { type: "SET_SEARCH_QUERY"; query: string }
  | { type: "SET_SEARCH_RESULTS"; results: StorageLocationOption[] }
  | { type: "SET_REASON"; reason: string }
  | { type: "SET_NOTES"; notes: string }
  | {
      type: "PRELOAD";
      selection: LocationSelection;
      position?: LocationPosition;
    }
  | {
      type: "REPLACE_SELECTION";
      selection: LocationSelection;
      position?: LocationPosition;
    }
  | { type: "RESET"; initialAssignment?: StorageAssignment | null };

export interface PickerCommit {
  selection: LocationSelection;
  position: LocationPosition;
  reason: string;
  notes: string;
}

export interface SampleSummary {
  sampleAccessionNumber?: string;
  sampleType?: string;
  status?: string;
  [key: string]: unknown;
}

export interface LocationPickerPageProps {
  sample: SampleSummary;
  currentLocation?: StorageAssignment | null;
  breadcrumb?: ReactNode;
  onSave: (state: PickerCommit) => void;
  onCancel: () => void;
}

export interface LocationPickerModalProps {
  isOpen: boolean;
  sample: SampleSummary;
  currentLocation?: StorageAssignment | null;
  onConfirm: (state: PickerCommit) => void;
  onCancel: () => void;
}

export interface LocationPickerInlineProps {
  initialSelection?: LocationSelection;
  initialPosition?: string | null;
  onChange?: (state: PickerState) => void;
  allowCreate?: boolean;
}

export interface ReplaceSelectionAction {
  type: "REPLACE_SELECTION";
  selection: LocationSelection;
}
