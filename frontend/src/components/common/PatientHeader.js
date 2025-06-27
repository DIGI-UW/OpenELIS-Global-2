import React from "react";
import { Grid, Column, Section, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
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
  const intl = useIntl();

  const tagStyle = {
    fontSize: "0.8rem",
  };
  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Section>
          <Section>
            {id ? (
              <div className={className}>
                <Grid>
                  <Column lg={1} md={2} sm={1}>
                    <Avatar
                      alt={"Patient avatar"}
                      color="rgba(0,0,0,0)"
                      name={
                        patientName ? patientName : lastName + " " + firstName
                      }
                      src={""}
                      size="56"
                      textSizeRatio={1}
                      style={{
                        backgroundImage: `url('/images/patient-background.svg')`,
                        backgroundRepeat: "round",
                      }}
                    />
                  </Column>
                  <Column lg={15} md={5} sm={3}>
                    <div>
                      <span className="patient-name">
                        {patientName ? patientName : lastName + " " + firstName}
                      </span>
                      <span className="patient-dob">
                        {" "}
                        {gender === "M" ? (
                          <>
                            {" "}
                            ♂
                            <FormattedMessage id="patient.male" />
                          </>
                        ) : (
                          <>
                            {" "}
                            ♀ <FormattedMessage id="patient.female" />
                          </>
                        )}{" "}
                        {age
                          ? age +
                            " " +
                            intl.formatMessage({ id: "patient.yrs" })
                          : dob}
                      </span>
                    </div>
                    {/* <br/> */}
                    <div className="patient-id">
                      {nationalId && (
                        <Tag size="lg" type="blue" style={tagStyle}>
                          <FormattedMessage id="patient.natioanalid" /> :{" "}
                          <strong>{nationalId}</strong>
                        </Tag>
                      )}
                      {subjectNumber && (
                        <Tag size="lg" type="blue" style={tagStyle}>
                          <FormattedMessage id="patient.subject.number" /> :{" "}
                          <strong>{subjectNumber}</strong>
                        </Tag>
                      )}
                      {accesionNumber && (
                        <Tag size="lg" type="blue" style={tagStyle}>
                          <FormattedMessage id="quick.entry.accession.number" />{" "}
                          : <strong>{accesionNumber}</strong>
                        </Tag>
                      )}
                      {orderDate && (
                        <>
                          <Tag size="lg" type="blue" style={tagStyle}>
                            <FormattedMessage id="sample.label.orderdate" /> :{" "}
                            <strong>{orderDate}</strong>
                          </Tag>

                          <Tag size="lg" type="blue" style={tagStyle}>
                            <FormattedMessage id="sample.label.requester" />:{" "}
                            <strong>{requester}</strong>
                          </Tag>
                        </>
                      )}
                      {referringFacility && (
                        <>
                          <Tag size="lg" type="blue" style={tagStyle}>
                            <FormattedMessage id="sample.label.facility" />:{" "}
                            <strong>{referringFacility}</strong>
                          </Tag>

                          <Tag size="lg" type="blue" style={tagStyle}>
                            <FormattedMessage id="sample.label.dept" /> :{" "}
                            <strong>{department}</strong>
                          </Tag>
                        </>
                      )}
                    </div>
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
                      size="56"
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
