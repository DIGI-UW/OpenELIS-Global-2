import React from "react";
import { useIntl } from "react-intl";
import ComingSoon from "./ComingSoon";

const RecentActivity = () => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "qa.overview.section.activity" });
  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-rows">
        <ComingSoon
          variant="row"
          titleKey="qa.overview.activity.feed"
          ticket="OGC-694"
        />
      </div>
    </section>
  );
};

export default RecentActivity;
