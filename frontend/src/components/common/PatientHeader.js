import React from "react";
import { Grid, Column, Section, Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import Avatar from "react-avatar";

const PatientHeader = (props) => {
  const {
    id,
    lastName,
    firstName,
    gender,
    dob,
    age = null,
    patientName = null,
    subjectNumber = null,
    nationalId = null,
    accesionNumber = null,
    orderDate = null,
    referringFacility = null,
    department = null,
    requester = null,
    isOrderPage = false,
    className = "patient-header",
  } = props;
  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Section>
          <Section>
            {id ? (
              <div className={className}>
                <Grid>
                  <Column lg={4} md={2} sm={1}>
                    <div
                      className={
                        referringFacility ? "patientAvatar2" : "patientAvatar"
                      }
                      role="img"
                    >
                      <Avatar
                        alt={"Patient avatar"}
                        color="rgba(0,0,0,0)"
                        name={
                          patientName ? patientName : lastName + " " + firstName
                        }
                        src={""}
                        size={referringFacility ? "150" : "120"}
                        textSizeRatio={2}
                        style={{
                          backgroundImage: `url('/images/patient-background.svg')`,
                          backgroundRepeat: "round",
                        }}
                      />
                    </div>
                  </Column>
                  <Column lg={10} md={5} sm={3}>
                    <div className="patient-name">
                      {patientName ? patientName : lastName + " " + firstName}
                    </div>
                    <div className="patient-dob">
                      {" "}
                      <Tag type="blue">
                        <FormattedMessage id="patient.label.sex" /> :
                      </Tag>
                      {gender === "M" ? (
                        <FormattedMessage id="patient.male" />
                      ) : (
                        <FormattedMessage id="patient.female" />
                      )}{" "}
                      <Tag type="blue">
                        {age ? (
                          <FormattedMessage id="patient.label.age" />
                        ) : (
                          <FormattedMessage id="patient.dob" />
                        )}{" "}
                        :
                      </Tag>{" "}
                      {age ? age : dob}
                    </div>
                    {nationalId && (
                      <div className="patient-id">
                        <Tag type="blue">
                          <FormattedMessage id="patient.natioanalid" /> :
                        </Tag>
                        {nationalId}
                      </div>
                    )}
                    {subjectNumber && (
                      <div className="patient-id">
                        <Tag type="blue">
                          <FormattedMessage id="patient.subject.number" /> :
                        </Tag>
                        {subjectNumber}{" "}
                      </div>
                    )}
                    {accesionNumber && (
                      <div className="patient-id">
                        <Tag type="blue">
                          <FormattedMessage id="quick.entry.accession.number" />{" "}
                          :
                        </Tag>
                        {accesionNumber}{" "}
                      </div>
                    )}
                    {orderDate && (
                      <div className="patient-id">
                        <div className="patient-id">
                          <Tag type="blue">
                            <FormattedMessage id="sample.label.orderdate" /> :
                          </Tag>
                          {orderDate}

                          <Tag type="blue">
                            <FormattedMessage id="sample.label.requester" />: :
                          </Tag>
                          {requester}
                        </div>
                      </div>
                    )}
                    {referringFacility && (
                      <div className="patient-id">
                        <Tag type="blue">
                          <FormattedMessage id="sample.label.facility" />:
                        </Tag>
                        {referringFacility}{" "}
                        <Tag type="blue">
                          <FormattedMessage id="sample.label.dept" /> :
                        </Tag>{" "}
                        {department}
                      </div>
                    )}
                  </Column>
                </Grid>
              </div>
            ) : (
              <div className={className}>
                <Grid>
                  <Column lg={4} md={2} sm={1}>
                    <Avatar
                      alt={"Patient avatar"}
                      color="rgba(0,0,0,0)"
                      name={"!"}
                      src={""}
                      size={referringFacility ? "150" : "120"}
                      textSizeRatio={2}
                      style={{
                        backgroundImage: `url('/images/patient-background.svg')`,
                        backgroundRepeat: "round",
                      }}
                    />
                  </Column>
                  <Column lg={8}>
                    <div className="patient-name">
                      {" "}
                      {isOrderPage ? (
                        <FormattedMessage id="patient.label.nopatientid" />
                      ) : (
                        <FormattedMessage id="patient.label.nopatientid" />
                      )}
                    </div>
                  </Column>
                </Grid>
              </div>
            )}
          </Section>
        </Section>
      </Column>
    </Grid>
  );
};

export default PatientHeader;
