import React, { useState, useEffect } from "react";
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

const VLSection = ({
  projectData,
  observations,
  onInputChange,
  onObservationChange,
  organizationLists,
  dictionaryLists,
  gender,
}) => {
  const intl = useIntl();
  const [showConditionalFields, setShowConditionalFields] = useState({
    pregnancySuckle: { pregnancy: false, suckle: false },
    arvTreatmentFields: false,
    vlReasonOther: false,
    priorVLFields: false,
    underInvestigationComment: false,
  });

  // Get ARV centers from organization lists
  const arvCentersByName =
    organizationLists && organizationLists["ARV_ORGS_BY_NAME"]
      ? organizationLists["ARV_ORGS_BY_NAME"]
      : [];

  const arvCentersByCode =
    organizationLists && organizationLists["ARV_ORGS"]
      ? organizationLists["ARV_ORGS"]
      : [];

  // Get dictionary lists
  const yesNoList =
    dictionaryLists && dictionaryLists["YES_NO"]
      ? dictionaryLists["YES_NO"]
      : [];

  const hivTypesList =
    dictionaryLists && dictionaryLists["HIV_TYPES"]
      ? dictionaryLists["HIV_TYPES"]
      : [];

  const arvRegimeList =
    dictionaryLists && dictionaryLists["ARV_REGIME"]
      ? dictionaryLists["ARV_REGIME"]
      : [];

  const vlReasonList =
    dictionaryLists && dictionaryLists["ARV_REASON_FOR_VL_DEMAND"]
      ? dictionaryLists["ARV_REASON_FOR_VL_DEMAND"]
      : [];

  // Handle ARV center name selection and auto-populate code
  const handleCenterNameChange = (event) => {
    const selectedCenterId = event.target.value;
    onInputChange("ARVcenterName", selectedCenterId);

    // Auto-populate center code based on name selection
    if (selectedCenterId && arvCentersByCode.length > 0) {
      const selectedCenter = arvCentersByCode.find(
        (center) => center.id === selectedCenterId,
      );
      if (selectedCenter) {
        onInputChange("ARVcenterCode", selectedCenter.id);
      }
    }
  };

  // Handle ARV center code selection and auto-populate name
  const handleCenterCodeChange = (event) => {
    const selectedCenterId = event.target.value;
    onInputChange("ARVcenterCode", selectedCenterId);

    // Auto-populate center name based on code selection
    if (selectedCenterId && arvCentersByName.length > 0) {
      const selectedCenter = arvCentersByName.find(
        (center) => center.id === selectedCenterId,
      );
      if (selectedCenter) {
        onInputChange("ARVcenterName", selectedCenter.id);
      }
    }
  };

  // Check gender and show pregnancy/suckle fields if female
  useEffect(() => {
    if (gender) {
      // Find if gender is female
      const isFemale =
        gender.toLowerCase().includes("f") ||
        gender.toLowerCase().includes("femme") ||
        gender.toLowerCase().includes("female");

      setShowConditionalFields((prev) => ({
        ...prev,
        pregnancySuckle: {
          pregnancy: isFemale,
          suckle: isFemale,
        },
      }));
    } else {
      setShowConditionalFields((prev) => ({
        ...prev,
        pregnancySuckle: {
          pregnancy: false,
          suckle: false,
        },
      }));
    }
  }, [gender]);

  // Check if currently on ARV treatment to show related fields
  useEffect(() => {
    if (observations.currentARVTreatment) {
      const yesOption = yesNoList.find(
        (item) =>
          item.id === observations.currentARVTreatment &&
          (item.dictEntry === "Yes" ||
            item.dictEntry === "Oui" ||
            item.localizedName === "Yes"),
      );
      setShowConditionalFields((prev) => ({
        ...prev,
        arvTreatmentFields: !!yesOption,
      }));
    } else {
      setShowConditionalFields((prev) => ({
        ...prev,
        arvTreatmentFields: false,
      }));
    }
  }, [observations.currentARVTreatment, yesNoList]);

  // Check if VL reason is "Other" to show text field
  useEffect(() => {
    if (observations.vlReasonForRequest) {
      const otherOption = vlReasonList.find(
        (item) =>
          item.id === observations.vlReasonForRequest &&
          (item.dictEntry?.toLowerCase().includes("other") ||
            item.dictEntry?.toLowerCase().includes("autre") ||
            item.localizedName?.toLowerCase().includes("other") ||
            item.localizedName?.toLowerCase().includes("autre")),
      );
      setShowConditionalFields((prev) => ({
        ...prev,
        vlReasonOther: !!otherOption,
      }));
    } else {
      setShowConditionalFields((prev) => ({
        ...prev,
        vlReasonOther: false,
      }));
    }
  }, [observations.vlReasonForRequest, vlReasonList]);

  // Check if prior VL benefit to show related fields
  useEffect(() => {
    if (observations.vlBenefit) {
      const yesOption = yesNoList.find(
        (item) =>
          item.id === observations.vlBenefit &&
          (item.dictEntry === "Yes" ||
            item.dictEntry === "Oui" ||
            item.localizedName === "Yes"),
      );
      setShowConditionalFields((prev) => ({
        ...prev,
        priorVLFields: !!yesOption,
      }));
    } else {
      setShowConditionalFields((prev) => ({
        ...prev,
        priorVLFields: false,
      }));
    }
  }, [observations.vlBenefit, yesNoList]);

  // Check if under investigation comment should be shown
  useEffect(() => {
    if (observations.underInvestigation) {
      const yesOption = yesNoList.find(
        (item) =>
          item.id === observations.underInvestigation &&
          (item.dictEntry === "Yes" ||
            item.dictEntry === "Oui" ||
            item.localizedName === "Yes"),
      );
      setShowConditionalFields((prev) => ({
        ...prev,
        underInvestigationComment: !!yesOption,
      }));
    } else {
      setShowConditionalFields((prev) => ({
        ...prev,
        underInvestigationComment: false,
      }));
    }
  }, [observations.underInvestigation, yesNoList]);

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.VL.title"
          defaultMessage="ARV - Viral Load"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* ARV Center Name */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="ARVcenterName"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "sample.entry.project.ARV.centerName",
                  defaultMessage: "ARV Center Name",
                })}
              </>
            }
            value={projectData.ARVcenterName || ""}
            onChange={handleCenterNameChange}
          >
            <SelectItem text="" value="" />
            {arvCentersByName.map((center) => (
              <SelectItem
                key={center.id}
                text={center.organizationName || center.name}
                value={center.id}
              />
            ))}
          </Select>
        </Column>

        {/* ARV Center Code */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="ARVcenterCode"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "patient.project.centerCode",
                  defaultMessage: "Center Code",
                })}
              </>
            }
            value={projectData.ARVcenterCode || ""}
            onChange={handleCenterCodeChange}
          >
            <SelectItem text="" value="" />
            {arvCentersByCode.map((center) => (
              <SelectItem
                key={center.id}
                text={center.doubleName || center.code || center.id}
                value={center.id}
              />
            ))}
          </Select>
        </Column>

        {/* Name of Clinician */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="nameOfDoctor"
            labelText={intl.formatMessage({
              id: "patient.project.nameOfClinician",
              defaultMessage: "Name of Clinician",
            })}
            value={observations.nameOfDoctor || ""}
            onChange={(e) =>
              onObservationChange("nameOfDoctor", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "patient.project.nameOfClinician.placeholder",
              defaultMessage: "Enter clinician name",
            })}
            maxLength={50}
          />
        </Column>

        {/* Name of Sampler */}
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="nameOfSampler"
            labelText={intl.formatMessage({
              id: "patient.project.nameOfSampler",
              defaultMessage: "Name of Sampler",
            })}
            value={observations.nameOfSampler || ""}
            onChange={(e) =>
              onObservationChange("nameOfSampler", e.target.value)
            }
            placeholder={intl.formatMessage({
              id: "patient.project.nameOfSampler.placeholder",
              defaultMessage: "Enter sampler name",
            })}
            maxLength={50}
          />
        </Column>

        {/* Pregnancy (Female only) */}
        {showConditionalFields.pregnancySuckle.pregnancy && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="vlPregnancy"
              labelText={intl.formatMessage({
                id: "sample.project.vlPregnancy",
                defaultMessage: "Pregnant?",
              })}
              value={observations.vlPregnancy || ""}
              onChange={(e) =>
                onObservationChange("vlPregnancy", e.target.value)
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
        )}

        {/* Breastfeeding/Suckling (Female only) */}
        {showConditionalFields.pregnancySuckle.suckle && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="vlSuckle"
              labelText={intl.formatMessage({
                id: "sample.project.vlSuckle",
                defaultMessage: "Breastfeeding?",
              })}
              value={observations.vlSuckle || ""}
              onChange={(e) => onObservationChange("vlSuckle", e.target.value)}
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
        )}

        {/* HIV Type */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="hivStatus"
            labelText={
              <>
                <span className="required-field">*</span>{" "}
                {intl.formatMessage({
                  id: "patient.project.hivType",
                  defaultMessage: "HIV Type",
                })}
              </>
            }
            value={observations.hivStatus || ""}
            onChange={(e) => onObservationChange("hivStatus", e.target.value)}
          >
            <SelectItem text="" value="" />
            {hivTypesList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
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

        {/* Under Investigation Note (Conditional) */}
        {showConditionalFields.underInvestigationComment && (
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
            />
          </Column>
        )}

        {/* ARV Treatment Section */}
        <Column lg={16} md={8} sm={4}>
          <hr style={{ margin: "1.5rem 0" }} />
        </Column>

        {/* Currently on ARV Treatment */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="currentARVTreatment"
            labelText={intl.formatMessage({
              id: "sample.entry.project.arv.treatment",
              defaultMessage: "Currently on ARV Treatment?",
            })}
            value={observations.currentARVTreatment || ""}
            onChange={(e) =>
              onObservationChange("currentARVTreatment", e.target.value)
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

        {/* ARV Treatment Init Date (Conditional) */}
        {showConditionalFields.arvTreatmentFields && (
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="arvTreatmentInitDate"
              labelText={intl.formatMessage({
                id: "sample.entry.project.arv.treatment.initDate",
                defaultMessage: "ARV Treatment Start Date",
              })}
              value={observations.arvTreatmentInitDate || ""}
              onChange={(date) =>
                onObservationChange("arvTreatmentInitDate", date)
              }
              disallowFutureDate={true}
            />
          </Column>
        )}

        {/* ARV Treatment Regime (Conditional) */}
        {showConditionalFields.arvTreatmentFields && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="arvTreatmentRegime"
              labelText={intl.formatMessage({
                id: "sample.entry.project.arv.treatment.therap.line",
                defaultMessage: "Therapeutic Line",
              })}
              value={observations.arvTreatmentRegime || ""}
              onChange={(e) =>
                onObservationChange("arvTreatmentRegime", e.target.value)
              }
            >
              <SelectItem text="" value="" />
              {arvRegimeList.map((item) => (
                <SelectItem
                  key={item.id}
                  text={item.localizedName || item.dictEntry}
                  value={item.id}
                />
              ))}
            </Select>
          </Column>
        )}

        {/* Current ARV Treatment Regimen (Conditional) */}
        {showConditionalFields.arvTreatmentFields && (
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="currentARVTreatmentINNsList"
              labelText={intl.formatMessage({
                id: "sample.entry.project.arv.treatment.regimen",
                defaultMessage: "Current ARV Treatment Regimen (INNs)",
              })}
              value={observations.currentARVTreatmentINNsList || ""}
              onChange={(e) =>
                onObservationChange(
                  "currentARVTreatmentINNsList",
                  e.target.value,
                )
              }
              placeholder={intl.formatMessage({
                id: "sample.entry.project.arv.treatment.regimen.placeholder",
                defaultMessage: "Enter ARV regimen",
              })}
              rows={2}
              maxLength={200}
            />
          </Column>
        )}

        <Column lg={16} md={8} sm={4}>
          <hr style={{ margin: "1.5rem 0" }} />
        </Column>

        {/* Reason for VL Request */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="vlReasonForRequest"
            labelText={intl.formatMessage({
              id: "sample.entry.project.vl.reason",
              defaultMessage: "Reason for VL Request",
            })}
            value={observations.vlReasonForRequest || ""}
            onChange={(e) =>
              onObservationChange("vlReasonForRequest", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {vlReasonList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Other Reason (Conditional) */}
        {showConditionalFields.vlReasonOther && (
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="vlOtherReasonForRequest"
              labelText={intl.formatMessage({
                id: "sample.entry.project.vl.specify",
                defaultMessage: "Specify Other Reason",
              })}
              value={observations.vlOtherReasonForRequest || ""}
              onChange={(e) =>
                onObservationChange("vlOtherReasonForRequest", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "sample.entry.project.vl.specify.placeholder",
                defaultMessage: "Please specify",
              })}
              maxLength={50}
            />
          </Column>
        )}

        <Column lg={16} md={8} sm={4}>
          <hr style={{ margin: "1.5rem 0" }} />
        </Column>

        {/* Initial CD4 Section */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.project.cd4init"
              defaultMessage="Initial CD4"
            />
          </h4>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="initcd4Count"
            labelText={intl.formatMessage({
              id: "sample.project.cd4Count",
              defaultMessage: "CD4 Count",
            })}
            value={observations.initcd4Count || ""}
            onChange={(e) =>
              onObservationChange("initcd4Count", e.target.value)
            }
            placeholder="0"
            maxLength={4}
          />
        </Column>

        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="initcd4Percent"
            labelText={intl.formatMessage({
              id: "sample.project.cd4Percent",
              defaultMessage: "CD4 %",
            })}
            value={observations.initcd4Percent || ""}
            onChange={(e) =>
              onObservationChange("initcd4Percent", e.target.value)
            }
            placeholder="0"
            maxLength={10}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="initcd4Date"
            labelText={intl.formatMessage({
              id: "sample.project.Cd4Date",
              defaultMessage: "CD4 Date",
            })}
            value={observations.initcd4Date || ""}
            onChange={(date) => onObservationChange("initcd4Date", date)}
            disallowFutureDate={true}
          />
        </Column>

        <Column lg={16} md={8} sm={4}>
          <hr style={{ margin: "1.5rem 0" }} />
        </Column>

        {/* CD4 at Demand Section */}
        <Column lg={16} md={8} sm={4}>
          <h4
            style={{
              marginTop: "1rem",
              marginBottom: "1rem",
              fontWeight: "bold",
            }}
          >
            <FormattedMessage
              id="sample.project.cd4demand"
              defaultMessage="CD4 at Demand"
            />
          </h4>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="demandcd4Count"
            labelText={intl.formatMessage({
              id: "sample.project.cd4Count",
              defaultMessage: "CD4 Count",
            })}
            value={observations.demandcd4Count || ""}
            onChange={(e) =>
              onObservationChange("demandcd4Count", e.target.value)
            }
            placeholder="0"
            maxLength={4}
          />
        </Column>

        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="demandcd4Percent"
            labelText={intl.formatMessage({
              id: "sample.project.cd4Percent",
              defaultMessage: "CD4 %",
            })}
            value={observations.demandcd4Percent || ""}
            onChange={(e) =>
              onObservationChange("demandcd4Percent", e.target.value)
            }
            placeholder="0"
            maxLength={10}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="demandcd4Date"
            labelText={intl.formatMessage({
              id: "sample.project.Cd4Date",
              defaultMessage: "CD4 Date",
            })}
            value={observations.demandcd4Date || ""}
            onChange={(date) => onObservationChange("demandcd4Date", date)}
            disallowFutureDate={true}
          />
        </Column>

        <Column lg={16} md={8} sm={4}>
          <hr style={{ margin: "1.5rem 0" }} />
        </Column>

        {/* Prior VL Benefit */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="vlBenefit"
            labelText={intl.formatMessage({
              id: "sample.project.priorVLRequest",
              defaultMessage: "Prior VL Request?",
            })}
            value={observations.vlBenefit || ""}
            onChange={(e) => onObservationChange("vlBenefit", e.target.value)}
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

        {/* Prior VL Lab (Conditional) */}
        {showConditionalFields.priorVLFields && (
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="priorVLLab"
              labelText={intl.formatMessage({
                id: "sample.project.priorVLLab",
                defaultMessage: "Prior VL Lab",
              })}
              value={observations.priorVLLab || ""}
              onChange={(e) =>
                onObservationChange("priorVLLab", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "sample.project.priorVLLab.placeholder",
                defaultMessage: "Enter lab name",
              })}
              maxLength={10}
            />
          </Column>
        )}

        {/* Prior VL Value (Conditional) */}
        {showConditionalFields.priorVLFields && (
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="priorVLValue"
              labelText={intl.formatMessage({
                id: "sample.project.VLValue",
                defaultMessage: "VL Value",
              })}
              value={observations.priorVLValue || ""}
              onChange={(e) =>
                onObservationChange("priorVLValue", e.target.value)
              }
              placeholder="0"
              maxLength={10}
            />
          </Column>
        )}

        {/* Prior VL Date (Conditional) */}
        {showConditionalFields.priorVLFields && (
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="priorVLDate"
              labelText={intl.formatMessage({
                id: "sample.project.VLDate",
                defaultMessage: "VL Date",
              })}
              value={observations.priorVLDate || ""}
              onChange={(date) => onObservationChange("priorVLDate", date)}
              disallowFutureDate={true}
            />
          </Column>
        )}
      </Grid>
    </Section>
  );
};

export default VLSection;
