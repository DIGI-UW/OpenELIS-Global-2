import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Grid,
  Column,
} from "@carbon/react";
import CustomDatePicker from "../../common/CustomDatePicker";

const IndeterminateSection = ({
  projectData,
  observations,
  onInputChange,
  onObservationChange,
  organizationLists,
  dictionaryLists,
}) => {
  const intl = useIntl();

  // Get IND sites from organization lists (uses EID_ORGS)
  const indSites =
    organizationLists && organizationLists["EID_ORGS"]
      ? organizationLists["EID_ORGS"]
      : [];

  // Get YES_NO dictionary for under investigation
  const yesNoList =
    dictionaryLists && dictionaryLists["YES_NO"]
      ? dictionaryLists["YES_NO"]
      : [];

  // Handle IND site selection
  const handleSiteChange = (event) => {
    const selectedSiteId = event.target.value;
    onInputChange("INDsiteName", selectedSiteId);
    onInputChange("INDsiteCode", selectedSiteId);
  };

  // Check if under investigation comment should be shown
  const shouldShowInvestigationComment = () => {
    const underInvestigation = observations.underInvestigation;
    if (!underInvestigation) return false;

    const yesOption = yesNoList.find(
      (item) =>
        item.id === underInvestigation &&
        (item.dictEntry === "Yes" ||
          item.dictEntry === "Oui" ||
          item.localizedName === "Yes"),
    );

    return !!yesOption;
  };

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.indeterminate.title"
          defaultMessage="Indeterminate Results"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* Site Name */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="INDsiteName"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "sample.entry.project.siteName",
                  defaultMessage: "Site Name",
                })}
              </>
            }
            value={projectData.INDsiteName || ""}
            onChange={handleSiteChange}
          >
            <SelectItem text="" value="" />
            {indSites.map((site) => (
              <SelectItem
                key={site.id}
                text={site.doubleName || site.organizationName || site.name}
                value={site.id}
              />
            ))}
          </Select>
        </Column>

        {/* Address */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="address"
            labelText={intl.formatMessage({
              id: "sample.entry.project.address",
              defaultMessage: "Address",
            })}
            value={projectData.address || ""}
            onChange={(e) => onInputChange("address", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.project.address.placeholder",
              defaultMessage: "Enter address",
            })}
          />
        </Column>

        {/* Phone Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="phoneNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.project.phoneNumber",
              defaultMessage: "Phone Number",
            })}
            value={projectData.phoneNumber || ""}
            onChange={(e) => onInputChange("phoneNumber", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.project.phoneNumber.placeholder",
              defaultMessage: "Enter phone number",
            })}
          />
        </Column>

        {/* Fax Number */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="faxNumber"
            labelText={intl.formatMessage({
              id: "sample.entry.project.faxNumber",
              defaultMessage: "Fax Number",
            })}
            value={projectData.faxNumber || ""}
            onChange={(e) => onInputChange("faxNumber", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.project.faxNumber.placeholder",
              defaultMessage: "Enter fax number",
            })}
          />
        </Column>

        {/* Email */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="email"
            labelText={intl.formatMessage({
              id: "sample.entry.project.email",
              defaultMessage: "Email",
            })}
            value={projectData.email || ""}
            onChange={(e) => onInputChange("email", e.target.value)}
            placeholder={intl.formatMessage({
              id: "sample.entry.project.email.placeholder",
              defaultMessage: "Enter email address",
            })}
            type="email"
          />
        </Column>

        {/* First Test Section Header */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1.5rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.entry.project.firstTest"
              defaultMessage="First Test"
            />
          </h4>
        </Column>

        {/* First Test Date */}
        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="indFirstTestDate"
            labelText={intl.formatMessage({
              id: "sample.entry.project.date",
              defaultMessage: "Date",
            })}
            value={observations.indFirstTestDate || ""}
            onChange={(date) => onObservationChange("indFirstTestDate", date)}
            disallowFutureDate={true}
          />
        </Column>

        {/* First Test Name */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="indFirstTestName"
            labelText={intl.formatMessage({
              id: "sample.entry.project.testName",
              defaultMessage: "Test Name",
            })}
            value={observations.indFirstTestName || ""}
            onChange={(e) =>
              onObservationChange("indFirstTestName", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.project.testName.placeholder",
              defaultMessage: "Enter test name",
            })}
          />
        </Column>

        {/* First Test Result */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="indFirstTestResult"
            labelText={intl.formatMessage({
              id: "sample.entry.project.result",
              defaultMessage: "Result",
            })}
            value={observations.indFirstTestResult || ""}
            onChange={(e) =>
              onObservationChange("indFirstTestResult", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.project.result.placeholder",
              defaultMessage: "Enter result",
            })}
          />
        </Column>

        {/* Second Test Section Header */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1.5rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.entry.project.secondTest"
              defaultMessage="Second Test"
            />
          </h4>
        </Column>

        {/* Second Test Date */}
        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="indSecondTestDate"
            labelText={intl.formatMessage({
              id: "sample.entry.project.date",
              defaultMessage: "Date",
            })}
            value={observations.indSecondTestDate || ""}
            onChange={(date) => onObservationChange("indSecondTestDate", date)}
            disallowFutureDate={true}
          />
        </Column>

        {/* Second Test Name */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="indSecondTestName"
            labelText={intl.formatMessage({
              id: "sample.entry.project.testName",
              defaultMessage: "Test Name",
            })}
            value={observations.indSecondTestName || ""}
            onChange={(e) =>
              onObservationChange("indSecondTestName", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.project.testName.placeholder",
              defaultMessage: "Enter test name",
            })}
          />
        </Column>

        {/* Second Test Result */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="indSecondTestResult"
            labelText={intl.formatMessage({
              id: "sample.entry.project.result",
              defaultMessage: "Result",
            })}
            value={observations.indSecondTestResult || ""}
            onChange={(e) =>
              onObservationChange("indSecondTestResult", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.project.result.placeholder",
              defaultMessage: "Enter result",
            })}
          />
        </Column>

        {/* Final Result of Site */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="indSiteFinalResult"
            labelText={intl.formatMessage({
              id: "sample.entry.project.finalResultOfSite",
              defaultMessage: "Final Result of Site",
            })}
            value={observations.indSiteFinalResult || ""}
            onChange={(e) =>
              onObservationChange("indSiteFinalResult", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "sample.entry.project.finalResultOfSite.placeholder",
              defaultMessage: "Enter final result",
            })}
          />
        </Column>

        {/* Under Investigation */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="underInvestigation"
            labelText={intl.formatMessage({
              id: "patient.project.underInvestigation",
              defaultMessage: "Under Investigation?",
            })}
            value={observations.underInvestigation || ""}
            onChange={(e) =>
              onObservationChange("underInvestigation", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {yesNoList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Under Investigation Comment (Conditional) */}
        {shouldShowInvestigationComment() && (
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="underInvestigationNote"
              labelText={
                <>
                  <span className="required-field">*</span>{" "}
                  {intl.formatMessage({
                    id: "patient.project.underInvestigationComment",
                    defaultMessage: "Investigation Notes",
                  })}
                </>
              }
              value={projectData.underInvestigationNote || ""}
              onChange={(e) =>
                onInputChange("underInvestigationNote", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "patient.project.underInvestigationComment.placeholder",
                defaultMessage: "Enter investigation notes",
              })}
              rows={3}
              maxLength={1000}
            />
          </Column>
        )}
      </Grid>
    </Section>
  );
};

export default IndeterminateSection;
