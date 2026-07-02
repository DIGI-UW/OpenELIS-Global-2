export interface LabNumberFormValues {
  labNumberType: "ALPHANUM" | "SITEYEARNUM";
  usePrefix: boolean;
  alphanumPrefix: string;
}

const labNumberFormValues: LabNumberFormValues = {
  labNumberType: "ALPHANUM",
  usePrefix: false,
  alphanumPrefix: "",
};

export default labNumberFormValues;
