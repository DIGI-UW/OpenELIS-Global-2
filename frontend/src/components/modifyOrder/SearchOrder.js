import React, { useState, useEffect, useRef } from "react";
import SearchPatientForm from "../patient/SearchPatientForm";
import { Button, Column, TextInput, Grid, Form } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import CustomLabNumberInput from "../common/CustomLabNumberInput";

function SearchOrder() {
  const [selectedPatient, setSelectedPatient] = useState({});
  const componentMounted = useRef(false);
  const [accessionNumber, setAccessionNumber] = useState("");

  const getSelectedPatient = (patient) => {
    setSelectedPatient(patient);
    console.debug("selectedPatient:" + selectedPatient);
  };

  useEffect(() => {
    componentMounted.current = true;
    openPatientResults(selectedPatient.patientPK);

    return () => {
      componentMounted.current = false;
    };
  }, [selectedPatient]);

  const openPatientResults = (patientId) => {
    if (patientId) {
      window.location.href = "/ModifyOrder?patientId=" + patientId;
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    var labNumber = accessionNumber ? accessionNumber.split("-")[0] : "";
    window.location.href = "/ModifyOrder?accessionNumber=" + labNumber;
  };

  return (
    <>
      <div className="orderLegendBody">
        <Form onSubmit={handleSearch}>
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="sample.label.search.labnumber" />
              </h4>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <CustomLabNumberInput
                placeholder={"Enter Lab No"}
                id="labNumber"
                name="labNumber"
                value={accessionNumber}
                onChange={(e, rawVal) =>
                  setAccessionNumber(rawVal ? rawVal : e?.target?.value)
                }
                labelText={<FormattedMessage id="search.label.accession" />}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Button data-cy="submit-button" type="submit">
                <FormattedMessage id="label.button.submit" />
              </Button>
            </Column>
          </Grid>
        </Form>
      </div>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <h4>
              {" "}
              <FormattedMessage id="sample.label.search.patient" />
            </h4>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <SearchPatientForm
              getSelectedPatient={getSelectedPatient}
            ></SearchPatientForm>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default SearchOrder;
