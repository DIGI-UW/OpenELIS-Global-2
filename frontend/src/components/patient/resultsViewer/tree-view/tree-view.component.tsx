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

  const { timelineData, resetTree } = useContext(FilterContext);

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
        {!tablet && window.location.href.includes("#trendline") ? (
          <Trendline
            patientUuid={patientUuid}
            conceptUuid={window.location.href.split("#trendline/")[1]}
            basePath={basePath}
            showBackToTimelineButton
          />
        ) : !loading || window.location.href.endsWith("#groupedtimeline") ? (
          <GroupedTimeline />
        ) : (
          <DataTableSkeleton />
        )}
      </div>
    </>
  );
};

export default TreeView;
