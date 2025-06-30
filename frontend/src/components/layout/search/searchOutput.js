import React from "react";
import { Grid, Column, Section, Tag, Tile } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import Avatar from "react-avatar";
import { openPatientResults } from "./searchService";

const SearchOutput = ({ patientData, className = "patientHead" }) => {
  return (
    <div>
      {patientData.map((patient) => {
        return (
          <Column lg={16} md={8} sm={4} key={patient.id}>
            <Section>
              <div>
                <Grid
                  className="patientHead"
                  onClick={() => openPatientResults(patient.patientID)}
                >
                  <Column lg={2} md={1}>
                    <div role="img">
                      <Avatar
                        alt="Patient avatar"
                        color="rgba(0,0,0,0)"
                        name={`${patient.lastName} ${patient.firstName}`}
                        src={""}
                        size={patient.referringFacility ? "50" : "40"}
                        textSizeRatio={2}
                        style={{
                          backgroundImage: `url('/images/patient-background.svg')`,
                          marginTop: "5px",
                        }}
                      />
                    </div>
                  </Column>
                  <Column lg={14} md={7} sm={3}>
                    <div className="tags">
                      <span className="patient-name-search">
                        <b>{`${patient.lastName} ${patient.firstName}`}</b>
                      </span>
                      <span>
                        {" "}
                        {patient.gender === "M" ? (
                          <>
                            ♂ <FormattedMessage id="patient.male" />
                          </>
                        ) : (
                          <>
                            ♀ <FormattedMessage id="patient.female" />
                          </>
                        )}{" "}
                        {patient.age || patient.dob}
                      </span>
                    </div>
                    <div className="tags">
                      <Tag size="md" type="blue">
                        <FormattedMessage id="patient.natioanalid" /> :{" "}
                        <strong>{patient.nationalId}</strong>
                      </Tag>
                      {/* <Tag size="md" type="blue">
                        <FormattedMessage id="patient.subject.number" /> :{" "}
                        <strong>{patient.subjectNumber}</strong>
                      </Tag> */}
                    </div>
                  </Column>
                </Grid>
              </div>
            </Section>
          </Column>
        );
      })}
    </div>
  );
};

export default SearchOutput;
