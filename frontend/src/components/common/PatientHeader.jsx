import React from "react";
import { Grid, Column, Section, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import AsyncAvatar from "../patient/photoManagement/photoAvatar/AyncAvatar";

/**
 * The patient band every screen puts above a sample or a case.
 *
 * statusTag and assignedStaff exist because the anatomic-pathology case views
 * (pathology, immunohistochemistry and cytology) were each drawing their own
 * header band to show the case's state and who is working on it. Both are
 * optional and inert when absent, so the screens that only need the patient
 * render exactly what they rendered before. The band stays display-only:
 * reassignment happens on the module dashboard, not here, and statusTag is a
 * rendered node rather than a description of a badge, because the band owns no
 * badge vocabulary of its own. Because it is a node rather than a descriptor,
 * statusTag can technically be given interactive content; callers are expected
 * to pass a non-interactive status element only, since this band is specified
 * as display-only.
 */
const PatientHeader = (props) => {
  const {
    id,
    lastName,
    firstName,
    gender,
    dob,
    age = null,
    patientName = null,
    patientId = null,
    subjectNumber = null,
    nationalId = null,
    accesionNumber = null,
    orderDate = null,
    referringFacility = null,
    department = null,
    requester = null,
    isOrderPage = false,
    statusTag = null,
    assignedStaff = [],
    className = "patient-header",
  } = props;
  const intl = useIntl();

  const tagStyle = {
    fontSize: "0.8rem",
  };
  // A nullable list on a server DTO arrives here as an explicit null, which a
  // destructuring default does not cover, so normalise once.
  const staff = Array.isArray(assignedStaff) ? assignedStaff : [];
  const hasCaseState = Boolean(statusTag) || staff.length > 0;
  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Section>
          <Section>
            {id ? (
              <div className={className}>
                <Grid>
                  <Column lg={1} md={2} sm={1}>
                    <AsyncAvatar
                      patientId={String(id)}
                      hasPhoto={true}
                      patientName={
                        patientName ? patientName : lastName + " " + firstName
                      }
                      size={56}
                      gender={gender}
                    />
                  </Column>
                  <Column lg={hasCaseState ? 11 : 15} md={5} sm={3}>
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
                    <br />
                    <div className="patient-id">
                      {patientId && (
                        <Tag size="lg" type="blue" style={tagStyle}>
                          <FormattedMessage id="patient.id" /> :{" "}
                          <strong>{patientId}</strong>
                        </Tag>
                      )}
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
                        <Tag size="lg" type="blue" style={tagStyle}>
                          <FormattedMessage id="sample.label.orderdate" /> :{" "}
                          <strong>{orderDate}</strong>
                        </Tag>
                      )}
                      {requester && (
                        <Tag size="lg" type="blue" style={tagStyle}>
                          <FormattedMessage id="sample.label.requester" />:{" "}
                          <strong>{requester}</strong>
                        </Tag>
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
                  {hasCaseState && (
                    <Column lg={4} md={8} sm={4}>
                      {statusTag}
                      {staff.map(
                        (entry, index) =>
                          // A malformed entry is dropped rather than rendered:
                          // this band sits above a patient's identity on every
                          // screen that shows it, and a missing roleKey would
                          // otherwise reach formatMessage as an undefined id
                          // and print the literal string "undefined" here.
                          entry.name &&
                          entry.roleKey && (
                            <div
                              key={`${entry.roleKey}-${index}`}
                              className="cds--type-helper-text-01"
                              data-testid="case-assigned-staff"
                            >
                              {intl.formatMessage(
                                { id: "caseView.label.assignedStaff" },
                                {
                                  role: intl.formatMessage({
                                    id: entry.roleKey,
                                  }),
                                  name: entry.name,
                                },
                              )}
                            </div>
                          ),
                      )}
                    </Column>
                  )}
                </Grid>
              </div>
            ) : (
              <div className={className}>
                <Grid>
                  <Column lg={4} md={2} sm={1}>
                    <AsyncAvatar
                      patientId={null}
                      hasPhoto={false}
                      patientName={"!"}
                      size={56}
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
