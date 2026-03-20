import React, { useState, useEffect } from "react";
import {
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Toggle,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer, putToOpenElisServer } from "../utils/Utils";

const WestgardRulesDisplay = ({ testTypeId }) => {
  const intl = useIntl();
  const [rules, setRules] = useState([]);

  useEffect(() => {
    if (testTypeId) {
      getFromOpenElisServer(
        `/rest/qc/westgard-rules/config/${testTypeId}`,
        (data) => {
          if (data && Array.isArray(data)) {
            setRules(data);
          }
        },
      );
    }
  }, [testTypeId]);

  const handleToggle = (ruleCode, enabled) => {
    putToOpenElisServer(
      `/rest/qc/westgard-rules/config/${testTypeId}`,
      JSON.stringify({ ruleCode, enabled }),
      (data) => {
        if (data && Array.isArray(data)) {
          setRules(data);
        }
      },
    );
  };

  return (
    <div>
      <h4>{intl.formatMessage({ id: "qc.westgard.rules" })}</h4>
      <p>{intl.formatMessage({ id: "qc.westgard.rules.description" })}</p>

      <StructuredListWrapper>
        <StructuredListHead>
          <StructuredListRow head>
            <StructuredListCell head>Rule</StructuredListCell>
            <StructuredListCell head>Description</StructuredListCell>
            <StructuredListCell head>Status</StructuredListCell>
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          {rules.map((rule) => (
            <StructuredListRow key={rule.ruleCode}>
              <StructuredListCell>
                <strong>{rule.ruleCode}</strong>
              </StructuredListCell>
              <StructuredListCell>{rule.description}</StructuredListCell>
              <StructuredListCell>
                <Toggle
                  id={`toggle-${rule.ruleCode}`}
                  size="sm"
                  labelA={intl.formatMessage({
                    id: "qc.westgard.rule.disabled",
                  })}
                  labelB={intl.formatMessage({
                    id: "qc.westgard.rule.enabled",
                  })}
                  toggled={rule.enabled}
                  onToggle={(toggled) => handleToggle(rule.ruleCode, toggled)}
                />
              </StructuredListCell>
            </StructuredListRow>
          ))}
        </StructuredListBody>
      </StructuredListWrapper>
    </div>
  );
};

export default WestgardRulesDisplay;
