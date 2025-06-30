import React, { useEffect, useRef, useState } from "react";
import { Button, Stack, Grid, Column } from "@carbon/react";
import SearchPatientForm from "../patient/SearchPatientForm";
import CreatePatientForm from "../patient/CreatePatientForm";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const PatientInfo = (props) => {
  const { orderFormValues, setOrderFormValues, error } = props;
  const componentMounted = useRef(false);
  const [searchPatientTab, setSearchPatientTab] = useState({
    kind: "primary",
    active: true,
  });
  const [newPatientTab, setNewPatientTab] = useState({
    kind: "tertiary",
    active: false,
  });
  const [selectedPatient, setSelectedPatient] = useState({
    id: "",
    healthRegion: [],
  });

  const getSelectedPatient = (patient) => {
    setSelectedPatient(patient);
    if (orderFormValues) {
      setOrderFormValues({
        ...orderFormValues,
        patientUpdateStatus: "UPDATE",
        patientProperties: patient,
      });
    }
    handleNewPatientTab();
  };

  const handleSearchPatientTab = () => {
    setSearchPatientTab({ kind: "primary", active: true });
    setNewPatientTab({ kind: "tertiary", active: false });
  };

  const handleNewPatientTab = () => {
    setNewPatientTab({ kind: "primary", active: true });
    setSearchPatientTab({ kind: "tertiary", active: false });
  };

  useEffect(() => {
    componentMounted.current = true;

    if (
      orderFormValues.patientProperties.firstName !== "" ||
      orderFormValues.patientProperties.guid !== ""
    ) {
      handleNewPatientTab();
    }
    window.scrollTo(0, 0);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (
      orderFormValues.patientProperties.guid &&
      !orderFormValues.patientProperties.lastName
    ) {
      const searchEndPoint =
        "/rest/patient-search-results?" +
        "guid=" +
        orderFormValues.patientProperties.guid;
      getFromOpenElisServer(searchEndPoint, (searchPatients) => {
        if (searchPatients.patientSearchResults.length > 0) {
          const searchEndPoint =
            "/rest/patient-details?patientID=" +
            searchPatients.patientSearchResults[0].patientID;
          getFromOpenElisServer(searchEndPoint, (patientDetails) => {
            getSelectedPatient(patientDetails);
            handleNewPatientTab();
          });
        }
      });
    }
  }, [orderFormValues.patientProperties.guid]);

  return (
    <>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <h3>
                <FormattedMessage id="banner.menu.patient" />
              </h3>
            </Column>
            <Column lg={4} md={4} sm={2}>
              <Button
                kind={searchPatientTab.kind}
                onClick={handleSearchPatientTab}
              >
                <FormattedMessage id="search.patient.label" />
              </Button>
            </Column>
            <Column lg={4} md={4} sm={2}>
              <Button kind={newPatientTab.kind} onClick={handleNewPatientTab}>
                <FormattedMessage id="new.patient.label" />
              </Button>
            </Column>
            <Column lg={16} md={8} sm={4}>
              {searchPatientTab.active && (
                <SearchPatientForm getSelectedPatient={getSelectedPatient} />
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              {newPatientTab.active && (
                <CreatePatientForm
                  showActionsButton={false}
                  selectedPatient={selectedPatient}
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                  error={error}
                />
              )}
            </Column>
          </Grid>
        </div>
      </Stack>
    </>
  );
};

export default PatientInfo;
