import React, { useEffect, useState } from "react";
import { Tabs, TabList, Tab } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import Workplan from "./Workplan";

const WORKPLAN_VIEWS = ["test", "panel", "unit", "priority"];

const queryToView = (search) => {
  const params = new URLSearchParams(search || "");
  const candidate = params.get("view");
  return WORKPLAN_VIEWS.includes(candidate) ? candidate : null;
};

const resolveInitialView = (initialType, search) => {
  if (WORKPLAN_VIEWS.includes(initialType)) {
    return initialType;
  }
  const queryView = queryToView(search);
  return queryView || "test";
};

const getViewIndex = (view) => {
  const index = WORKPLAN_VIEWS.indexOf(view);
  return index >= 0 ? index : 0;
};

export default function UnifiedWorkplan({ initialType }) {
  const intl = useIntl();
  const location = useLocation();
  const history = useHistory();
  const [activeView, setActiveView] = useState(
    resolveInitialView(initialType, location.search),
  );

  useEffect(() => {
    setActiveView(resolveInitialView(initialType, location.search));
  }, [initialType, location.search]);

  const updateCanonicalQuery = (nextView) => {
    if (location.pathname !== "/Workplan") {
      return;
    }
    const params = new URLSearchParams(location.search || "");
    if (nextView === "test") {
      params.delete("view");
    } else {
      params.set("view", nextView);
    }
    const nextSearch = params.toString();
    history.replace({
      pathname: location.pathname,
      search: nextSearch ? `?${nextSearch}` : "",
    });
  };

  const onTabChange = ({ selectedIndex }) => {
    const nextView = WORKPLAN_VIEWS[selectedIndex] || "test";
    setActiveView(nextView);
    updateCanonicalQuery(nextView);
  };

  return (
    <>
      <Tabs selectedIndex={getViewIndex(activeView)} onChange={onTabChange}>
        <TabList
          aria-label={intl.formatMessage({
            id: "workplan.filter.mode.ariaLabel",
          })}
        >
          <Tab>
            <FormattedMessage id="workplan.test.types" />
          </Tab>
          <Tab>
            <FormattedMessage id="workplan.panel.types" />
          </Tab>
          <Tab>
            <FormattedMessage id="workplan.unit.types" />
          </Tab>
          <Tab>
            <FormattedMessage id="workplan.priority.list" />
          </Tab>
        </TabList>
      </Tabs>
      <Workplan key={activeView} type={activeView} />
    </>
  );
}
