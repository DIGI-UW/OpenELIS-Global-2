export interface LabNumberFormValues {
  labNumberType: "ALPHANUM" | "SITEYEARNUM";
  usePrefix: boolean;
  alphanumPrefix: string;
}

export default {
  labNumberType: "ALPHANUM",
  usePrefix: false,
  alphanumPrefix: "",
} as LabNumberFormValues;
