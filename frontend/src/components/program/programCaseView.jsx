import React, { useState, useEffect } from "react";
import { useParams, useHistory } from "react-router-dom";
import { Grid, Column, Row, Section } from "@carbon/react";
import PatientHeader from "../common/PatientHeader";
import { getFromOpenElisServer } from "../utils/Utils";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import PageBreadCrumb from "../common/PageBreadCrumb";
import "./programCaseView.css";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.results.order.programmes", link: "/genericProgram" },
];

const ProgramCaseView = () => {
  const { programSampleId } = useParams();
  const history = useHistory();

  const [programSampleData, setProgramSampleData] = useState(null);
  const [questionnaireResponse, setQuestionnaireResponse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!programSampleId) {
      setError("No program sample ID provided");
      setLoading(false);
      return;
    }

    const url = `/rest/programSample/${programSampleId}`;

    getFromOpenElisServer(url, (response) => {
      if (!response) {
        setError("No data returned from server");
        setLoading(false);
        return;
      }

      setProgramSampleData(response);
      setQuestionnaireResponse(response.programQuestionnaireResponse);
      setLoading(false);
    });
  }, [programSampleId]);

  const formatDate = (timestamp) => {
    if (!timestamp) return "";
    try {
      const date = new Date(Number(timestamp));
      return date.toLocaleDateString();
    } catch {
      return "Invalid date";
    }
  };

  if (loading) return null;
  if (error) return <p>{error}</p>;
  if (!programSampleData) return null;

  const patientHeaderProps = {
    id:
      programSampleData.patientPK?.toString() ||
      programSampleData.programSampleId?.toString(),
    firstName: programSampleData.firstName,
    lastName: programSampleData.lastName,
    gender: programSampleData.gender,
    age: programSampleData.age,
    accesionNumber: programSampleData.accessionNumber,
    orderDate: formatDate(programSampleData.receivedDate),
    patientPK: programSampleData.patientPK,
    referringFacility: programSampleData.referringFacility,
    department: programSampleData.department,
    requester: programSampleData.requester,
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2 className="title-title-program">
            {programSampleData.programName}
          </h2>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <Row>
            <Column lg={16} md={8} sm={4}>
              <PatientHeader {...patientHeaderProps} />
            </Column>
          </Row>

          <Column lg={16} md={8} sm={4}>
            <Section>
              <div className="patient-header-2">
                {questionnaireResponse && (
                  <QuestionnaireResponse
                    key={questionnaireResponse.programSampleId}
                    questionnaireResponse={questionnaireResponse}
                  />
                )}
              </div>
            </Section>
          </Column>
        </Column>
      </Grid>
    </>
  );
};

export default ProgramCaseView;
