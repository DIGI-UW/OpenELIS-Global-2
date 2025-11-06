import * as Yup from "yup";

const RuleBuilderValidationSchema = Yup.object().shape({
  ruleName: Yup.string().required("Rule Name Required"),
  overall: Yup.string().required("Overall Required"),
  conditions: Yup.array().of(
    Yup.object().shape({
      sample: Yup.string().required("Sample is required"),
      test: Yup.string().required("Test is required"),
      relation: Yup.string().required("Relation is required"),
      value: Yup.string().required("Value is required"),
    }),
  ),
  actions: Yup.array().of(
    Yup.object().shape({
      action: Yup.string().required("Action is required"),
      reflexResult: Yup.string().required("Result is required"),
    }),
  ),
});

export default RuleBuilderValidationSchema;
