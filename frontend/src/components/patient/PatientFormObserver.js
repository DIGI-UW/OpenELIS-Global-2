import { useEffect } from "react";
import { useFormikContext } from "formik";

const PatientFormObserver = (props) => {
  const { values } = useFormikContext();
  const { orderFormValues, setOrderFormValues, formAction } = props;
  useEffect(() => {
    setOrderFormValues({
      ...orderFormValues,
      patientUpdateStatus: formAction,
      patientProperties: values,
    });
  }, [values]);
  return null;
};

export default PatientFormObserver;
