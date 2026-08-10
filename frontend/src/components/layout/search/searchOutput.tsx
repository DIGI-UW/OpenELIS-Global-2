import React from "react";
import { Grid, Column, Section, Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import AsyncAvatar from "../../patient/photoManagement/photoAvatar/AyncAvatar";
import { openPatientResults, type PatientSearchResult } from "./searchService";

interface SearchOutputProps {
  patientData: PatientSearchResult[];
  loading?: boolean;
  className?: string;
}

const SearchOutput: React.FC<SearchOutputProps> = ({
  patientData,
  className = "patientHead",
}) => {
  return (
    <div>
      {patientData.map((patient) => {
        return (
          <Column lg={16} md={8} sm={4} key={patient.id ?? patient.patientID}>
            <Section>
              <div>
                <Grid
                  className={className}
                  onClick={() => openPatientResults(patient.patientID)}
                >
                  <Column lg={2} md={1}>
                    <div role="img">
                      <AsyncAvatar
                        patientId={patient.patientID ?? patient.id}
                        hasPhoto={Boolean(patient.patientID ?? patient.id)}
                        patientName={`${patient.lastName ?? ""} ${
                          patient.firstName ?? ""
                        }`}
                        size={patient.referringFacility ? 50 : 40}
                      />
                    </div>
                  </Column>
                  <Column lg={14} md={7} sm={3}>
                    <div className="tags">
                      <span className="patient-name-search">
                        <b>{`${patient.lastName ?? ""} ${
                          patient.firstName ?? ""
                        }`}</b>
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
