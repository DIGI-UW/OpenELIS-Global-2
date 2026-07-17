import React, { useEffect, useRef, useState } from "react";
import { Column, Grid, Select, SelectItem, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import CustomDatePicker from "../common/CustomDatePicker";

const EQAOrderForm = ({ orderFormValues, setOrderFormValues }) => {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [myPrograms, setMyPrograms] = useState([]);

  const sampleOrder = orderFormValues?.sampleOrderItems || {};

  useEffect(() => {
    componentMounted.current = true;

    // Fetch self-enrolled programmes (My Programs)
    getFromOpenElisServer("/rest/eqa/my-programs", (response) => {
      if (componentMounted.current && Array.isArray(response)) {
        setMyPrograms(response);
      }
    });

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const updateField = (field, value) => {
    setOrderFormValues((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        [field]: value,
      },
    }));
  };

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="eqa.order.form.title" />
          </h3>

          <Grid>
            {/* Programme — from My Programs (self-enrollments) */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="eqa-program"
                labelText={intl.formatMessage({
                  id: "eqa.order.programme",
                })}
                value={sampleOrder.eqaProgramId || ""}
                onChange={(e) => updateField("eqaProgramId", e.target.value)}
              >
                <SelectItem value="" text="" />
                {myPrograms.map((prog) => (
                  <SelectItem
                    key={prog.id}
                    value={String(prog.id)}
                    text={prog.programName || prog.name}
                  />
                ))}
              </Select>
            </Column>

            {/* Provider Sample ID */}
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="eqa-provider-sample-id"
                labelText={intl.formatMessage({
                  id: "eqa.order.providerSampleId",
                })}
                value={sampleOrder.eqaProviderSampleId || ""}
                onChange={(e) =>
                  updateField("eqaProviderSampleId", e.target.value)
                }
              />
            </Column>

            {/* Result Deadline */}
            <Column lg={8} md={4} sm={4}>
              <CustomDatePicker
                id="eqa-deadline"
                labelText={intl.formatMessage({
                  id: "eqa.order.deadline",
                })}
                value={sampleOrder.eqaDeadline || ""}
                onChange={(date) => updateField("eqaDeadline", date)}
              />
            </Column>

            {/* EQA Priority */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="eqa-priority"
                labelText={intl.formatMessage({
                  id: "eqa.order.priority",
                })}
                value={sampleOrder.eqaPriority || "STANDARD"}
                onChange={(e) => updateField("eqaPriority", e.target.value)}
              >
                <SelectItem value="STANDARD" text="Standard" />
                <SelectItem value="URGENT" text="Urgent" />
                <SelectItem value="CRITICAL" text="Critical" />
              </Select>
            </Column>
          </Grid>
        </div>
      </Column>
    </Grid>
  );
};

export default EQAOrderForm;
