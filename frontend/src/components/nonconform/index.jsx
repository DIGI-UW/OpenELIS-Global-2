import PageBreadCrumb from "../common/PageBreadCrumb";
import { NCECorrectiveAction } from "./common/NCECorrectiveAction";
import { ReportNonConformingEvent } from "./common/ReportNonConformingEvent";
import { ViewNonConformingEvent } from "./common/ViewNonConforming";
import { NceDashboard } from "./common/NceDashboard";

// Each NCE sub-page gets its own page-title breadcrumb in addition to "Home".
// Use the existing menu translation keys so labels stay consistent with the
// sidenav and remain translated.
const NCE_PAGE_CRUMBS = {
  NceDashboard: {
    label: "banner.menu.nonconformity.dashboard",
    link: "/NceDashboard",
  },
  ReportNonConformingEvent: {
    label: "banner.menu.nonconformity.report",
    link: "/ReportNonConformingEvent",
  },
  ViewNonConformingEvent: {
    label: "banner.menu.nonconformity.view",
    link: "/ViewNonConformingEvent",
  },
  NCECorrectiveAction: {
    label: "banner.menu.nonconformity.correctiveActions",
    link: "/NCECorrectiveAction",
  },
};

const NonConformIndex = ({ form }) => {
  const breadcrumbs = [{ label: "home.label", link: "/" }];
  if (NCE_PAGE_CRUMBS[form]) {
    breadcrumbs.push(NCE_PAGE_CRUMBS[form]);
  }

  return (
    <div>
      <br />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="orderLegendBody">
        {form == "NceDashboard" && <NceDashboard />}
        {form == "ReportNonConformingEvent" && <ReportNonConformingEvent />}
        {form == "ViewNonConformingEvent" && <ViewNonConformingEvent />}
        {form == "NCECorrectiveAction" && <NCECorrectiveAction />}
      </div>
    </div>
  );
};

export default NonConformIndex;
