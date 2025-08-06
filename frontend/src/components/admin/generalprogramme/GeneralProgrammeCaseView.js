"use client";

import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  Button,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../../utils/Utils";
import QuestionnaireResponse from "../../common/QuestionnaireResponse";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "menu.generalprogramme.label",
    link: "/GeneralProgrammeDashboard",
  },
  {
    label: "program.case.details.label",
    link: "#",
  },
];

function GeneralProgrammeCaseView() {
  const intl = useIntl();
  const { caseId } = useParams();
  const [loading, setLoading] = useState(true);
  const [caseData, setCaseData] = useState(null);

  useEffect(() => {
    if (caseId) {
      fetchCaseData(caseId);
    }
  }, [caseId]);

  const fetchCaseData = (id) => {
    setLoading(true);
    getFromOpenElisServer(`/rest/program/case/${id}`, (data) => {
      setCaseData(data);
      setLoading(false);
    });
  };

  const handleBackToDashboard = () => {
    window.history.back();
  };

  if (loading) {
    return <Loading />;
  }

  if (!caseData) {
    return (
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <p>
                <FormattedMessage id="program.case.not.found" />
              </p>
            </Section>
          </Column>
        </Grid>
      </div>
    );
  }

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "1rem",
                marginBottom: "0rem",
              }}
            >
              <Button kind="secondary" onClick={handleBackToDashboard}>
                <FormattedMessage id="label.button.back" />
              </Button>
              <Heading>
                <FormattedMessage id="program.case.details.label" /> -{" "}
                {caseData.labNumber}
              </Heading>
            </div>
          </Section>
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={8} md={4} sm={4}>
          <Section>
            <Tile style={{ marginBottom: "1rem" }}>
              <h4>
                <FormattedMessage id="sample.information.label" />
              </h4>
              <div style={{ marginTop: "1rem" }}>
                <p>
                  <strong>
                    <FormattedMessage id="sample.label.labnumber" />:
                  </strong>{" "}
                  {caseData.labNumber}
                </p>
                <p>
                  <strong>
                    <FormattedMessage id="patient.label.name" />:
                  </strong>{" "}
                  {caseData.patientName}
                </p>
                <p>
                  <strong>
                    <FormattedMessage id="sample.label.collectiondate" />:
                  </strong>{" "}
                  {caseData.collectionDate}
                </p>
                <p>
                  <strong>
                    <FormattedMessage id="label.status" />:
                  </strong>{" "}
                  {caseData.status}
                </p>
                <p>
                  <strong>
                    <FormattedMessage id="program.name.label" />:
                  </strong>{" "}
                  {caseData.programmeName}
                </p>
              </div>
            </Tile>
          </Section>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Section>
            <Tile>
              <h4>
                <FormattedMessage id="program.captured.information.label" />
              </h4>
              {caseData.questionnaireResponse ? (
                <QuestionnaireResponse
                  questionnaireResponse={caseData.questionnaireResponse}
                  readOnly={true}
                />
              ) : (
                <p style={{ marginTop: "1rem", fontStyle: "italic" }}>
                  <FormattedMessage id="program.no.additional.information" />
                </p>
              )}
            </Tile>
          </Section>
        </Column>
      </Grid>

      {caseData.testResults && caseData.testResults.length > 0 && (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Tile>
                <h4>
                  <FormattedMessage id="test.results.label" />
                </h4>
                <div style={{ marginTop: "1rem" }}>
                  {caseData.testResults.map((result, index) => (
                    <div
                      key={index}
                      style={{
                        padding: "0.5rem",
                        borderBottom: "1px solid #e0e0e0",
                        marginBottom: "0.5rem",
                      }}
                    >
                      <p>
                        <strong>{result.testName}:</strong> {result.result}{" "}
                        {result.unit}
                      </p>
                      {result.referenceRange && (
                        <p style={{ fontSize: "0.875rem", color: "#666" }}>
                          Reference Range: {result.referenceRange}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </Tile>
            </Section>
          </Column>
        </Grid>
      )}
    </div>
  );
}

export default GeneralProgrammeCaseView;
