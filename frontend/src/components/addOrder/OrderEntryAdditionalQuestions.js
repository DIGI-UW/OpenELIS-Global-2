import React, { useEffect, useRef, useState } from "react";
import { Select, SelectItem, Stack } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "../../index.css";
import "../../App.css";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";
import Questionnaire from "../common/Questionnaire";

export const ProgramSelect = ({
  programChange = () => {
    console.debug("default programChange function does nothing");
  },
  orderFormValues,
  editable,
}) => {
  const componentMounted = useRef(false);

  const intl = useIntl();

  const [programs, setPrograms] = useState([]);

  const fetchPrograms = (programsList) => {
    if (componentMounted.current) {
      setPrograms(programsList);
    }
  };

  useEffect(() => {
    if (!orderFormValues?.sampleOrderItems?.programId) {
      programChange({
        target: {
          value: programs.find((program) => {
            return program.value === "Routine Testing";
          })?.id,
        },
      });
    }
  }, [programs]);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/user-programs", fetchPrograms);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  return (
    <>
      <div className="formInlineDiv">
        {programs.length > 0 && (
          <div className="inputText">
            <Select
              id="additionalQuestionsSelect"
              labelText={intl.formatMessage({ id: "label.program" })}
              onChange={programChange}
              value={orderFormValues?.sampleOrderItems?.programId}
              disabled={editable ? editable : false}
            >
              <SelectItem value="" text="" />
              {programs.map((program) => {
                return (
                  <SelectItem
                    key={program.id}
                    value={program.id}
                    text={program.value}
                  />
                );
              })}
            </Select>
          </div>
        )}
      </div>
    </>
  );
};

const OrderEntryAdditionalQuestions = ({
  orderFormValues,
  setOrderFormValues = () => {
    console.debug("default setOrderFormValues change function does nothing");
  },
}) => {
  const [questionnaire, setQuestionnaire] = useState(
    orderFormValues?.sampleOrderItems?.questionnaire,
  );
  const [questionnaireResponse, setQuestionnaireResponse] = useState(
    orderFormValues?.sampleOrderItems?.additionalQuestions,
  );

  const handleProgramSelection = (event) => {
    if (!event?.target?.value) {
      setAdditionalQuestions({});
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: "",
        },
      });
    } else {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: event.target.value,
        },
      });
      getFromOpenElisServer(
        "/rest/program/" + event.target.value + "/questionnaire",
        (res) => setAdditionalQuestions(res, event),
      );
    }
  };

  function convertQuestionnaireToResponse(questionnaire) {
    var items = [];
    if (questionnaire && "item" in questionnaire) {
      for (let i = 0; i < questionnaire.item.length; i++) {
        let currentItem = questionnaire.item[i];
        items.push({
          linkId: currentItem.linkId,
          definition: currentItem.definition,
          text: currentItem.text,
          answer: [],
        });
      }

      var convertedQuestionnaireResponse = {
        resourceType: "QuestionnaireResponse",
        id: "",
        questionnaire: "Questionnaire/" + questionnaire.id,
        status: "in-progress",
        item: items,
      };
      return convertedQuestionnaireResponse;
    }
    return null;
  }

  function setAdditionalQuestions(res, event) {
    console.debug(res);
    if ("item" in res) {
      setQuestionnaire(res);
      var convertedQuestionnaireResponse = convertQuestionnaireToResponse(res);
      setQuestionnaireResponse(convertedQuestionnaireResponse);
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          questionnaire: res,
          programId: event ? event.target.value : "",
          additionalQuestions: convertedQuestionnaireResponse,
        },
      });
    }
  }
  const getAnswer = (linkId) => {
    var responseItem = questionnaireResponse?.item?.find(
      (item) => item.linkId === linkId,
    );
    var questionnaireItem = questionnaire?.item?.find(
      (item) => item.linkId === linkId,
    );
    switch (questionnaireItem.type) {
      case "boolean":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueBoolean
          : "";
      case "decimal":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueDecimal
          : "";
      case "integer":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueInteger
          : "";
      case "date":
        return responseItem?.answer ? responseItem?.answer[0]?.valueDate : "";
      case "time":
        return responseItem?.answer ? responseItem?.answer[0]?.valueTime : "";
      case "string":
      case "text":
        return responseItem?.answer ? responseItem?.answer[0]?.valueString : "";
      case "quantity":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueQuantity
          : "";
      case "choice":
        if (responseItem?.answer) {
          return responseItem?.answer[0]?.valueCoding
            ? responseItem?.answer[0]?.valueCoding.code
            : responseItem?.answer[0]?.valueString;
        }
    }
  };

  const answerChange = (e) => {
    const { id, value } = e.target;

    var updatedQuestionnaireResponse = { ...questionnaireResponse };
    var responseItem = updatedQuestionnaireResponse.item.find(
      (item) => item.linkId === id,
    );
    var questionnaireItem = questionnaire.item.find(
      (item) => item.linkId === id,
    );
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
        case "choice":
          //make single select and multiselect have the same shape to reuse code
          var items = value;
          if (!Array.isArray(items)) {
            items = [{ value: value }];
          }
          for (var i = 0; i < items.length; i++) {
            var curValue = items[i].value;
            var option = questionnaireItem?.answerOption?.find(
              (option) => option?.valueCoding?.code === curValue,
            );
            if (option) {
              responseItem.answer.push({ valueCoding: option.valueCoding });
            } else {
              option = questionnaireItem?.answerOption?.find(
                (option) => option.valueString === curValue,
              );
              if (option) {
                responseItem.answer.push({ valueString: option.valueString });
              } else {
                console.error(
                  "couldn't find a matching questionnaire answer for '" +
                    curValue +
                    "'",
                );
              }
            }
          }
          break;
      }
    }
    setQuestionnaireResponse(updatedQuestionnaireResponse);
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        additionalQuestions: updatedQuestionnaireResponse,
      },
    });
  };

  return (
    <>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="select.program" />
          </h3>
          <ProgramSelect
            programChange={handleProgramSelection}
            orderFormValues={orderFormValues}
          />
          <Questionnaire
            questionnaire={questionnaire}
            onAnswerChange={answerChange}
            getAnswer={getAnswer}
          />
          {questionnaireResponse && (
            <input
              type="hidden"
              name="additionalQuestions"
              value={questionnaireResponse}
            />
          )}
        </div>
      </Stack>
    </>
  );
};

export default OrderEntryAdditionalQuestions;
