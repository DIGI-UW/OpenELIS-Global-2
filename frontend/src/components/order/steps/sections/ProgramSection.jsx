import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  ComboBox,
  TextInput,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import Questionnaire from "../../../common/Questionnaire";

/**
 * ProgramSection - Program selection with dynamic additional fields
 *
 * Implements:
 * - ORD-10: Program field typeahead ComboBox
 * - Program-specific additional fields (VL, EID, TB, etc.)
 */

const ProgramSection = ({ orderData, setOrderData, isReadOnly }) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  const [programs, setPrograms] = useState([]);
  const [selectedProgram, setSelectedProgram] = useState(null);
  const [questionnaire, setQuestionnaire] = useState(
    orderData?.sampleOrderItems?.questionnaire || null,
  );
  const [questionnaireResponse, setQuestionnaireResponse] = useState(
    orderData?.sampleOrderItems?.additionalQuestions || null,
  );

  // Track last initialized programId to avoid re-initializing when user manually changes
  const lastInitializedProgramIdRef = useRef(null);

  // Convert questionnaire to response format
  const convertQuestionnaireToResponse = (questionnaireData) => {
    if (!questionnaireData || !questionnaireData.item) {
      return null;
    }

    const items = questionnaireData.item.map((currentItem) => ({
      linkId: currentItem.linkId,
      definition: currentItem.definition,
      text: currentItem.text,
      answer: [],
    }));

    return {
      resourceType: "QuestionnaireResponse",
      id: "",
      questionnaire: "Questionnaire/" + questionnaireData.id,
      status: "in-progress",
      item: items,
    };
  };

  // Fetch programs on mount
  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/user-programs", (response) => {
      if (componentMounted.current && response) {
        setPrograms(response);
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Initialize selected program when orderData.sampleOrderItems.programId changes (e.g., from barcode scan)
  useEffect(() => {
    const currentProgramId = orderData?.sampleOrderItems?.programId;

    // Initialize if:
    // 1. We have a programId in orderData
    // 2. Programs list is loaded
    // 3. Either no program is selected OR the programId has changed from what we last initialized
    if (currentProgramId && programs.length > 0) {
      const programIdToFind = String(currentProgramId);

      // Check if already initialized with this programId
      if (
        String(currentProgramId) === String(lastInitializedProgramIdRef.current)
      ) {
        return;
      }

      const found = programs.find((p) => String(p.id) === programIdToFind);

      if (found) {
        lastInitializedProgramIdRef.current = currentProgramId;
        setSelectedProgram(found);

        // Check if we have saved questionnaire responses from loaded order
        const savedResponses = orderData?.sampleOrderItems?.additionalQuestions;
        if (savedResponses) {
          setQuestionnaireResponse(savedResponses);
          // Still need to fetch the questionnaire structure for rendering
          fetchProgramQuestionnaire(found.id, true); // true = don't overwrite responses
        } else {
          fetchProgramQuestionnaire(found.id);
        }
      }
    }
  }, [orderData?.sampleOrderItems?.programId, programs]);

  // Fetch program-specific questionnaire
  // preserveResponses = true when loading saved order with existing responses
  const fetchProgramQuestionnaire = (programId, preserveResponses = false) => {
    if (!programId) {
      setQuestionnaire(null);
      setQuestionnaireResponse(null);
      return;
    }
    getFromOpenElisServer(
      `/rest/program/${programId}/questionnaire`,
      (response) => {
        if (componentMounted.current && response?.item) {
          setQuestionnaire(response);

          // Only create new blank responses if not preserving existing ones
          if (!preserveResponses) {
            const convertedResponse = convertQuestionnaireToResponse(response);
            setQuestionnaireResponse(convertedResponse);
            setOrderData((prev) => ({
              ...prev,
              sampleOrderItems: {
                ...prev.sampleOrderItems,
                questionnaire: response,
                additionalQuestions: convertedResponse,
              },
            }));
          } else {
            // Just set the questionnaire structure for rendering, keep existing responses
            setOrderData((prev) => ({
              ...prev,
              sampleOrderItems: {
                ...prev.sampleOrderItems,
                questionnaire: response,
              },
            }));
          }
        } else {
          setQuestionnaire(null);
          if (!preserveResponses) {
            setQuestionnaireResponse(null);
          }
        }
      },
    );
  };

  // Handle program selection
  const handleProgramChange = ({ selectedItem }) => {
    setSelectedProgram(selectedItem);

    if (selectedItem) {
      // Track this as user-initiated change so useEffect doesn't override
      lastInitializedProgramIdRef.current = selectedItem.id;
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          programId: selectedItem.id,
        },
      }));
      fetchProgramQuestionnaire(selectedItem.id);
    } else {
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          programId: "",
          questionnaire: null,
          additionalQuestions: null,
        },
      }));
      setQuestionnaire(null);
      setQuestionnaireResponse(null);
    }
  };

  // Get answer for a questionnaire item
  const getAnswer = (linkId) => {
    if (!questionnaireResponse?.item || !questionnaire?.item) {
      return "";
    }

    const responseItem = questionnaireResponse.item.find(
      (item) => item.linkId === linkId,
    );
    const questionnaireItem = questionnaire.item.find(
      (item) => item.linkId === linkId,
    );

    if (!responseItem || !questionnaireItem || !responseItem.answer?.length) {
      return "";
    }

    switch (questionnaireItem.type) {
      case "boolean":
        return responseItem.answer[0]?.valueBoolean ?? "";
      case "decimal":
        return responseItem.answer[0]?.valueDecimal ?? "";
      case "integer":
        return responseItem.answer[0]?.valueInteger ?? "";
      case "date":
        return responseItem.answer[0]?.valueDate ?? "";
      case "time":
        return responseItem.answer[0]?.valueTime ?? "";
      case "string":
      case "text":
        return responseItem.answer[0]?.valueString ?? "";
      case "quantity":
        return responseItem.answer[0]?.valueQuantity ?? "";
      case "choice":
        return responseItem.answer[0]?.valueCoding
          ? responseItem.answer[0].valueCoding.code
          : (responseItem.answer[0]?.valueString ?? "");
      default:
        return "";
    }
  };

  // Handle questionnaire answer change
  const handleAnswerChange = (e) => {
    const { id, value } = e.target;

    if (!questionnaireResponse || !questionnaire) {
      return;
    }

    const updatedQuestionnaireResponse = { ...questionnaireResponse };
    const responseItem = updatedQuestionnaireResponse.item.find(
      (item) => item.linkId === id,
    );
    const questionnaireItem = questionnaire.item.find(
      (item) => item.linkId === id,
    );

    if (!responseItem || !questionnaireItem) {
      return;
    }

    responseItem.answer = [];

    if (value !== "") {
      switch (questionnaireItem.type) {
        case "boolean":
          responseItem.answer.push({ valueBoolean: value });
          break;
        case "decimal":
          responseItem.answer.push({ valueDecimal: value });
          break;
        case "integer":
          responseItem.answer.push({ valueInteger: value });
          break;
        case "date":
          responseItem.answer.push({ valueDate: value });
          break;
        case "time":
          responseItem.answer.push({ valueTime: value });
          break;
        case "string":
        case "text":
          responseItem.answer.push({ valueString: value });
          break;
        case "quantity":
          responseItem.answer.push({ valueQuantity: value });
          break;
        case "choice": {
          // Handle single select and multiselect
          let items = value;
          if (!Array.isArray(items)) {
            items = [{ value: value }];
          }
          for (const item of items) {
            const curValue = item.value || item;
            const option = questionnaireItem.answerOption?.find(
              (opt) => opt?.valueCoding?.code === curValue,
            );
            if (option) {
              responseItem.answer.push({ valueCoding: option.valueCoding });
            } else {
              const stringOption = questionnaireItem.answerOption?.find(
                (opt) => opt.valueString === curValue,
              );
              if (stringOption) {
                responseItem.answer.push({
                  valueString: stringOption.valueString,
                });
              }
            }
          }
          break;
        }
        default:
          break;
      }
    }

    setQuestionnaireResponse(updatedQuestionnaireResponse);
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        additionalQuestions: updatedQuestionnaireResponse,
      },
    }));
  };

  // Simple handler for VL-specific hardcoded fields (flat object structure)
  const [vlFields, setVlFields] = useState(
    orderData?.sampleOrderItems?.vlProgramFields || {},
  );

  const handleVLFieldChange = (field, value) => {
    const updatedVlFields = { ...vlFields, [field]: value };
    setVlFields(updatedVlFields);
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        vlProgramFields: updatedVlFields,
      },
    }));
  };

  // VL Program specific fields (example)
  const renderVLProgramFields = () => (
    <div className="program-fields">
      <Grid>
        <Column lg={5} md={4} sm={4}>
          <Select
            id="arvRegimen"
            labelText={intl.formatMessage({
              id: "vl.arvRegimen",
              defaultMessage: "ARV Regimen",
            })}
            value={vlFields.arvRegimen || ""}
            onChange={(e) => handleVLFieldChange("arvRegimen", e.target.value)}
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "select.regimen",
                defaultMessage: "Select Regimen...",
              })}
            />
            <SelectItem value="TDF/3TC/DTG" text="TDF/3TC/DTG" />
            <SelectItem value="TDF/3TC/EFV" text="TDF/3TC/EFV" />
            <SelectItem value="AZT/3TC/NVP" text="AZT/3TC/NVP" />
            <SelectItem value="ABC/3TC/DTG" text="ABC/3TC/DTG" />
          </Select>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="durationOnARV"
            labelText={intl.formatMessage({
              id: "vl.durationOnARV",
              defaultMessage: "Duration on ARV (months)",
            })}
            placeholder={intl.formatMessage({
              id: "vl.durationOnARV.placeholder",
              defaultMessage: "Enter months on treatment",
            })}
            value={vlFields.durationOnARV || ""}
            onChange={(e) =>
              handleVLFieldChange("durationOnARV", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>
        <Column lg={6} md={4} sm={4}>
          <Select
            id="vlIndication"
            labelText={intl.formatMessage({
              id: "vl.indication",
              defaultMessage: "Indication for VL Test",
            })}
            value={vlFields.vlIndication || ""}
            onChange={(e) =>
              handleVLFieldChange("vlIndication", e.target.value)
            }
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "select",
                defaultMessage: "Select...",
              })}
            />
            <SelectItem value="routine" text="Routine Monitoring" />
            <SelectItem value="targeted" text="Targeted (Clinical Suspicion)" />
            <SelectItem value="confirmatory" text="Confirmatory" />
          </Select>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Select
            id="pregnancyStatus"
            labelText={intl.formatMessage({
              id: "vl.pregnancyStatus",
              defaultMessage: "Pregnancy / Breastfeeding Status",
            })}
            value={vlFields.pregnancyStatus || ""}
            onChange={(e) =>
              handleVLFieldChange("pregnancyStatus", e.target.value)
            }
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "select",
                defaultMessage: "Select...",
              })}
            />
            <SelectItem value="not_applicable" text="Not Applicable" />
            <SelectItem value="pregnant" text="Pregnant" />
            <SelectItem value="breastfeeding" text="Breastfeeding" />
          </Select>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="d/m/Y"
            onChange={(dates) => {
              if (dates && dates[0]) {
                const date = dates[0];
                const formatted = `${date.getDate().toString().padStart(2, "0")}/${(date.getMonth() + 1).toString().padStart(2, "0")}/${date.getFullYear()}`;
                handleVLFieldChange("lastVLDate", formatted);
              }
            }}
          >
            <DatePickerInput
              id="lastVLDate"
              labelText={intl.formatMessage({
                id: "vl.lastVLDate",
                defaultMessage: "Date of Last VL Result",
              })}
              placeholder="dd/mm/yyyy"
              disabled={isReadOnly}
            />
          </DatePicker>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="lastVLResult"
            labelText={intl.formatMessage({
              id: "vl.lastVLResult",
              defaultMessage: "Last VL Result (copies/mL)",
            })}
            placeholder={intl.formatMessage({
              id: "vl.lastVLResult.placeholder",
              defaultMessage: "e.g., 150",
            })}
            value={vlFields.lastVLResult || ""}
            onChange={(e) =>
              handleVLFieldChange("lastVLResult", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>
      </Grid>
    </div>
  );

  // Check if VL program is selected
  const isVLProgram =
    selectedProgram?.value?.toLowerCase().includes("vl") ||
    selectedProgram?.value?.toLowerCase().includes("viral load");

  return (
    <Tile className="order-section program-section">
      <h4 className="section-title">
        <FormattedMessage id="label.program" defaultMessage="Program" />
      </h4>

      <Grid>
        <Column lg={8} md={6} sm={4}>
          <ComboBox
            key={selectedProgram?.id || "empty"}
            id="program"
            titleText={intl.formatMessage({
              id: "label.program",
              defaultMessage: "Program",
            })}
            items={programs}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={selectedProgram}
            onChange={handleProgramChange}
            placeholder={intl.formatMessage({
              id: "program.placeholder",
              defaultMessage: "Type to filter or select from the list",
            })}
            disabled={isReadOnly}
          />
          <p className="helper-text">
            <FormattedMessage
              id="program.helper"
              defaultMessage="Type to filter or select from the list. Selecting a program displays its specific Additional Order Information fields below."
            />
          </p>
        </Column>
      </Grid>

      {/* Additional Order Information - Program Specific */}
      {selectedProgram && (
        <div className="additional-order-info">
          <h5 className="subsection-title">
            <FormattedMessage
              id="order.additionalInfo"
              defaultMessage="Additional Order Information"
            />
            {" — "}
            {selectedProgram.value}
          </h5>
          <p className="helper-text">
            <FormattedMessage
              id="order.additionalInfo.helper"
              defaultMessage="These fields are specific to the selected program and provide additional context needed for this workflow."
            />
          </p>

          {/* Render VL-specific fields or use generic Questionnaire */}
          {isVLProgram ? (
            renderVLProgramFields()
          ) : questionnaire ? (
            <Questionnaire
              questionnaire={questionnaire}
              onAnswerChange={handleAnswerChange}
              getAnswer={getAnswer}
            />
          ) : null}
        </div>
      )}
    </Tile>
  );
};

export default ProgramSection;
