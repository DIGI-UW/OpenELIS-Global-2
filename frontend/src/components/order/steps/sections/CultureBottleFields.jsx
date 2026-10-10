import React, { useContext, useEffect, useState } from "react";
import { useIntl } from "react-intl";
import {
  Column,
  NumberInput,
  Select,
  SelectItem,
  TextInput,
  TimePicker,
} from "@carbon/react";
import CustomDatePicker from "../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../layout/Layout";
import { getFromOpenElisServer } from "../../../utils/Utils";
import {
  formatIsoDateForBackend,
  formatPickerDateForIso,
} from "../../dateUtils";

/** Shared bottle details on order entry and collection; times are never defaulted. */
export default function CultureBottleFields({
  sample,
  sampleIndex,
  isReadOnly,
  onChange,
  containers,
  includeCollectionTime = false,
}) {
  const intl = useIntl();
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE || "en-US";
  const [loadedContainers, setLoadedContainers] = useState([]);
  useEffect(() => {
    let current = true;
    if (containers === undefined) {
      getFromOpenElisServer(
        "/rest/vector/dictionary/sample-containers",
        (response) => {
          if (current)
            setLoadedContainers(Array.isArray(response) ? response : []);
        },
      );
    }
    return () => {
      current = false;
    };
  }, [containers]);
  const containerTypes = containers ?? loadedContainers;
  return (
    <>
      <Column lg={4} md={4} sm={4}>
        <NumberInput
          id={`cultureSetNumber-${sampleIndex}`}
          min={1}
          step={1}
          label={intl.formatMessage({
            id: "order.cultureSetNumber",
          })}
          value={sample.cultureSetNumber || ""}
          allowEmpty
          disabled={isReadOnly}
          invalid={
            !Number.isInteger(Number(sample.cultureSetNumber)) ||
            Number(sample.cultureSetNumber) < 1
          }
          invalidText={intl.formatMessage({
            id: "order.cultureSetNumber.required",
          })}
          onChange={(_, { value }) => onChange("cultureSetNumber", value)}
        />
      </Column>
      <Column lg={8} md={4} sm={4}>
        <Select
          id={`cultureContainer-${sampleIndex}`}
          labelText={intl.formatMessage({
            id: "microbiology.sets.container",
          })}
          value={sample.container || ""}
          disabled={isReadOnly}
          onChange={(e) => onChange("container", e.target.value)}
        >
          <SelectItem value="" text="" />
          {sample.container &&
            !containerTypes.some(
              (entry) => entry.dictEntry === sample.container,
            ) && (
              <SelectItem value={sample.container} text={sample.container} />
            )}
          {containerTypes.map((entry) => (
            <SelectItem
              key={entry.id}
              value={entry.dictEntry}
              text={entry.dictEntry}
            />
          ))}
        </Select>
      </Column>
      <Column lg={8} md={4} sm={4}>
        <TextInput
          id={`cultureBodySite-${sampleIndex}`}
          maxLength={40}
          disabled={isReadOnly}
          labelText={intl.formatMessage({
            id: "microbiology.sets.bodySite",
          })}
          value={sample.bodySite || ""}
          onChange={(e) => onChange("bodySite", e.target.value)}
        />
      </Column>
      {includeCollectionTime && (
        <>
          <Column lg={4} md={4} sm={4}>
            <CustomDatePicker
              id={`cultureDate-${sampleIndex}`}
              labelText={intl.formatMessage({
                id: "collect.sample.collectionDate",
              })}
              value={formatIsoDateForBackend(
                sample.collectionDate || "",
                dateLocale,
              )}
              updateStateValue
              disallowFutureDate
              disabled={isReadOnly}
              onChange={(value) =>
                onChange(
                  "collectionDate",
                  formatPickerDateForIso(value, dateLocale),
                )
              }
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TimePicker
              id={`cultureTime-${sampleIndex}`}
              labelText={intl.formatMessage({
                id: "collect.sample.collectionTime",
              })}
              value={sample.collectionTime || ""}
              disabled={isReadOnly}
              onChange={(e) => onChange("collectionTime", e.target.value)}
            />
          </Column>
        </>
      )}
    </>
  );
}
