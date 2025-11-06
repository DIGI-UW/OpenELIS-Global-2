import React from "react";
import { useTranslation } from "react-i18next";
import { Tab, Tabs, TabList } from "@carbon/react";
//import styles from './trendline.scss';
import "./trendline.scss";

const RangeSelector: React.FC<{
  setLowerRange: (lowerRange: Date) => void;
  upperRange: Date;
}> = ({ setLowerRange, upperRange }) => {
  const { t } = useTranslation();
  const ranges: Array<[string, () => void]> = [
    [
      t("1 day", "1 day"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 1 * 24 * 3600 * 1000),
        ),
    ],
    [
      t("5 day", "5 days"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 5 * 24 * 3600 * 1000),
        ),
    ],
    [
      t("1 month", "1 month"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 30 * 24 * 3600 * 1000),
        ),
    ],
    [
      t("6 month", "6 months"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 182 * 24 * 3600 * 1000),
        ),
    ],
    [
      t("1 year", "1 year"),
      () =>
        setLowerRange(
          new Date(Date.parse(upperRange.toString()) - 365 * 24 * 3600 * 1000),
        ),
    ],
    [
      t("5 years", "5 years"),
      () =>
        setLowerRange(
          new Date(
            Date.parse(upperRange.toString()) - 5 * 365 * 24 * 3600 * 1000,
          ),
        ),
    ],
    [t("All", "All"), () => setLowerRange(new Date(0))],
  ];

  return (
    <Tabs light selected={6} className="range-tabs">
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
