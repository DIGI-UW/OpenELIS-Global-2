import React, { useEffect, useRef, useState } from "react";
import {
  FilterableMultiSelect,
  Select,
  SelectItem,
  TextInput,
  Stack,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "../../index.css";
import "../../App.css";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";
import { ProgramSelect } from "../addOrder/OrderEntryAdditionalQuestions";
import Questionnaire from "../common/Questionnaire";

const EditOrderEntryAdditionalQuestions = ({
  orderFormValues,
  setOrderFormValues = () => {
    console.debug("default setOrderFormValues change function does nothing");
  },
}) => {
  const [questionnaire, setQuestionnaire] = useState({});
  const [questionnaireResponse, setQuestionnaireResponse] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (orderFormValues?.sampleOrderItems?.programId) {
      getFromOpenElisServer(
        "/rest/program/" +
          orderFormValues.sampleOrderItems.programId +
          "/questionnaire",
        setDefaultAdditionalQuestions,
      );
    }
    if (orderFormValues?.sampleOrderItems?.labNo) {
      if (!orderFormValues.sampleOrderItems.programId) {
        setLoading(false);
      }
    }
  }, [orderFormValues]);

  const handleProgramSelection = (event) => {
    if (!event.target.value) {
      setAdditionalQuestions(null);
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: "",
        },
      });
    } else {
      getFromOpenElisServer(
        "/rest/program/" + event.target.value + "/questionnaire",
        setAdditionalQuestions,
      );
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: event.target.value,
        },
      });
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

  function setAdditionalQuestions(res) {
    console.debug(res);
    setQuestionnaire(res);
    var convertedQuestionnaireResponse = convertQuestionnaireToResponse(res);
    setQuestionnaireResponse(convertedQuestionnaireResponse);
  }

  function setDefaultAdditionalQuestions(res) {
    console.debug(res);
    setQuestionnaire(res);
    setQuestionnaireResponse(
      orderFormValues.sampleOrderItems.additionalQuestions,
    );
    if (loading) {
      setLoading(false);
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
        return responseItem?.answer ? responseItem?.answer[0].valueBoolean : "";
      case "decimal":
        return responseItem?.answer ? responseItem?.answer[0].valueDecimal : "";
      case "integer":
        return responseItem?.answer ? responseItem?.answer[0].valueInteger : "";
      case "date":
        return responseItem?.answer ? responseItem?.answer[0].valueDate : "";
      case "time":
        return responseItem?.answer ? responseItem?.answer[0].valueTime : "";
      case "string":
      case "text":
        return responseItem?.answer ? responseItem?.answer[0].valueString : "";
      case "quantity":
        return responseItem?.answer
          ? responseItem?.answer[0].valueQuantity
          : "";
      case "choice":
        if (responseItem?.answer) {
          return responseItem.answer[0].valueCoding
            ? responseItem?.answer[0].valueCoding.code
            : responseItem?.answer[0].valueString;
        }
    }
  };

  return (
    <>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="label.program" />
          </h3>
          <ProgramSelect
            orderFormValues={orderFormValues}
            programChange={handleProgramSelection}
            editable={true}
          />
          <Questionnaire questionnaire={questionnaire} getAnswer={getAnswer} />
          {questionnaireResponse && (
            <input
              type="hidden"
              name="additionalQuestions"
              value={questionnaireResponse}
            />
          )}
          {loading && <InlineLoading />}
        </div>
      </Stack>
    </>
  );
};

export default EditOrderEntryAdditionalQuestions;
