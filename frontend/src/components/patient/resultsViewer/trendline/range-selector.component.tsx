import React from "react";
import { useIntl } from "react-intl";
import { Tab, Tabs, TabList } from "@carbon/react";
//import styles from './trendline.scss';
import "./trendline.scss";

const RangeSelector: React.FC<{
  setLowerRange: (lowerRange: Date) => void;
  upperRange: Date;
}> = ({ setLowerRange, upperRange }) => {
  const intl = useIntl();
  const label = (id: string) => intl.formatMessage({ id });
  const ranges: Array<[string, () => void]> = [
    [
      label("label.patientHistory.range.oneDay"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 1 * 24 * 3600 * 1000),
        ),
    ],
    [
      label("label.patientHistory.range.fiveDays"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 5 * 24 * 3600 * 1000),
        ),
    ],
    [
      label("label.patientHistory.range.oneMonth"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 30 * 24 * 3600 * 1000),
        ),
    ],
    [
      label("label.patientHistory.range.sixMonths"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 182 * 24 * 3600 * 1000),
        ),
    ],
    [
      label("label.patientHistory.range.oneYear"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 365 * 24 * 3600 * 1000),
        ),
    ],
    [
      label("label.patientHistory.range.fiveYears"),
      () =>
        setLowerRange(
          new Date(
            Date.parse(upperRange.toString()) - 5 * 365 * 24 * 3600 * 1000,
          ),
        ),
    ],
    [label("label.patientHistory.range.all"), () => setLowerRange(new Date(0))],
  ];

  return (
    // The graph opens on the full history, so "All" is the tab that describes
    // it. `selected` is not a Carbon v11 prop, which left "1 day" highlighted
    // over a chart showing every point.
    <Tabs light defaultSelectedIndex={ranges.length - 1} className="range-tabs">
      <TabList aria-label="Trendline range tabs">
        {ranges.map(([label, onClick], index) => (
          <Tab onClick={onClick} key={index}>
            {label}
          </Tab>
        ))}
      </TabList>
    </Tabs>
  );
};

export default React.memo(RangeSelector);
