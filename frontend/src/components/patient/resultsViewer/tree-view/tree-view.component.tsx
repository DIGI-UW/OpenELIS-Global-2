import React, { useContext, useState } from "react";
import { AccordionSkeleton, DataTableSkeleton, Button } from "@carbon/react";
import { TreeViewAlt } from "@carbon/react/icons";
import { useLayoutType } from "../commons";
import FilterSet, { FilterContext } from "../filter";
import GroupedTimeline from "../grouped-timeline";
import Trendline from "../trendline/trendline.component";
//import styles from '../results-viewer.styles.scss';
import "../results-viewer.styles.scss";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { parseTrendHash } from "../trendline/trendKey";
import TabletOverlay from "../tablet-overlay";

interface TreeViewProps {
  patientUuid: string;
  basePath: string;
  testUuid: string;
  loading: boolean;
  expanded: boolean;
  type: string;
}

const TreeView: React.FC<TreeViewProps> = ({
  patientUuid,
  basePath,
  testUuid,
  loading,
  expanded,
  type,
}) => {
  const tablet = useLayoutType() === "tablet";
  const [showTreeOverlay, setShowTreeOverlay] = useState(false);
  const intl = useIntl();
  const location = useLocation();
  const history = useHistory();

  const { timelineData, resetTree } = useContext(FilterContext);

  // Reading the hash off the router (rather than window.location) is what
  // makes clicking a test in the timeline swap the view: a hash the component
  // only sampled at render time never told React anything had changed.
  const trendKey = parseTrendHash(location.hash);
  const backToTimeline = () => history.push({ hash: "" });

  if (tablet) {
    return (
      <>
        <div>{!loading ? <GroupedTimeline /> : <DataTableSkeleton />}</div>
        <div className="floatingTreeButton">
          <Button
            renderIcon={TreeViewAlt}
            hasIconOnly
            onClick={() => setShowTreeOverlay(true)}
            iconDescription={intl.formatMessage({
              id: "label.patientHistory.showTree",
            })}
          />
        </div>
        {showTreeOverlay && (
          <TabletOverlay
            headerText={intl.formatMessage({ id: "label.patientHistory.tree" })}
            close={() => setShowTreeOverlay(false)}
            buttonsGroup={
              <>
                <Button
                  kind="secondary"
                  size="xl"
                  onClick={resetTree}
                  disabled={loading}
                >
                  <FormattedMessage id="label.patientHistory.resetTree" />
                </Button>
                <Button
                  kind="primary"
                  size="xl"
                  onClick={() => setShowTreeOverlay(false)}
                  disabled={loading}
                >
                  <FormattedMessage
                    id="label.patientHistory.viewResults"
                    values={{
                      count:
                        !loading && timelineData?.loaded
                          ? timelineData?.data?.rowData?.length
                          : "",
                    }}
                  />
                </Button>
              </>
            }
          >
            {!loading ? (
              <FilterSet hideFilterSetHeader />
            ) : (
              <AccordionSkeleton open count={4} align="start" />
            )}
          </TabletOverlay>
        )}
      </>
    );
  }

  return (
    <>
      {!tablet && (
        <div id="treeview" className="leftSection">
          {!loading ? (
            <FilterSet />
          ) : (
            <AccordionSkeleton open count={4} align="start" />
          )}
        </div>
      )}
      <div className="rightSection">
        {!tablet && trendKey ? (
          <Trendline
            patientUuid={patientUuid}
            trendKey={trendKey}
            basePath={basePath}
            showBackToTimelineButton
            onBackToTimeline={backToTimeline}
          />
        ) : !loading ? (
          <GroupedTimeline />
        ) : (
          <DataTableSkeleton />
        )}
      </div>
    </>
  );
};

export default TreeView;
