import PageBreadCrumb from "../common/PageBreadCrumb";
import { NCECorrectiveAction } from "./common/NCECorrectiveAction";
import { ReportNonConformingEvent } from "./common/ReportNonConformingEvent";
import { ViewNonConformingEvent } from "./common/ViewNonConforming";

const NonConformIndex = ({ form }) => {
  return (
    <div>
      <br />
      <PageBreadCrumb breadcrumbs={[{ label: "home.label", link: "/" }]} />
      <div className="orderLegendBody">
        {form == "ReportNonConformingEvent" && <ReportNonConformingEvent />}
        {form == "ViewNonConformingEvent" && <ViewNonConformingEvent />}
        {form == "NCECorrectiveAction" && <NCECorrectiveAction />}
      </div>
    </div>
  );
};

export default NonConformIndex;
