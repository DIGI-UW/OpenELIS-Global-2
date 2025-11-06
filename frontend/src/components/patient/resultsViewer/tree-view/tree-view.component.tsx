import React, { useContext, useState } from "react";
import { AccordionSkeleton, DataTableSkeleton, Button } from "@carbon/react";
import { TreeViewAlt } from "@carbon/react/icons";
import { useLayoutType } from "../commons";
import FilterSet, { FilterContext } from "../filter";
import GroupedTimeline from "../grouped-timeline";
import Trendline from "../trendline/trendline.component";
//import styles from '../results-viewer.styles.scss';
import "../results-viewer.styles.scss";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();

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
            iconDescription={t("showTree", "Show tree")}
          />
        </div>
        {showTreeOverlay && (
          <TabletOverlay
            headerText={t("Tree", "Tree")}
            close={() => setShowTreeOverlay(false)}
            buttonsGroup={
              <>
                <Button
                  kind="secondary"
                  size="xl"
                  onClick={resetTree}
                  disabled={loading}
                >
                  {t("resetTreeText", "Reset tree")}
                </Button>
                <Button
                  kind="primary"
                  size="xl"
                  onClick={() => setShowTreeOverlay(false)}
                  disabled={loading}
                >
                  {`${t("view", "View")} ${
                    !loading && timelineData?.loaded
                      ? timelineData?.data?.rowData?.length
                      : ""
                  } ${t("resultsText", "results")}`}
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
