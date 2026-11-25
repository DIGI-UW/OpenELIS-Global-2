/** A profile the Bridge ships, as the setup picker names it. */
export type ShippedProfile = {
  displayName: string;
  profileId: string;
  revision: number;
  /** The lab unit an analyzer of this type works in. */
  labUnit: string;
};

export const GENEXPERT: ShippedProfile = {
  displayName: "Cepheid GeneXpert (ASTM Mode)",
  profileId: "cepheid-genexpert-astm",
  revision: 1,
  labUnit: "Molecular Biology",
};

/** The workbook the mock replays is the Madagascar site's, so its profile is the one the stories set up. */
export const FLUOROCYCLER: ShippedProfile = {
  displayName: "Bruker FluoroCycler XT (Madagascar workbook)",
  profileId: "hain-fluorocycler-xt-madagascar",
  revision: 1,
  labUnit: "Molecular Biology",
};

export const QUANTSTUDIO: ShippedProfile = {
  displayName: "Thermo Fisher QuantStudio QS5/QS7",
  profileId: "thermo-quantstudio",
  revision: 1,
  labUnit: "Molecular Biology",
};

export const BC5380: ShippedProfile = {
  displayName: "Mindray BC-5380",
  profileId: "mindray-bc5380",
  revision: 1,
  labUnit: "Hematology",
};
