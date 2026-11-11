import React from "react";
import { useIntl } from "react-intl";
import { EmptyState, ErrorState } from "../commons";
import { FilterProvider } from "../filter/filter-context";
import TreeView from "./tree-view.component";
import { useGetManyObstreeData } from "../grouped-timeline";

interface TreeViewWrapperProps {
  patientUuid: string;
  basePath: string;
  testUuid: string;
  expanded: boolean;
  type: string;
}

const TreeViewWrapper: React.FC<TreeViewWrapperProps> = (props) => {
  //const conceptUuids = config?.concepts?.map((c) => c.conceptUuid) ?? [];
  const { roots, loading, error } = useGetManyObstreeData(props.patientUuid);
  const intl = useIntl();

  if (error)
    return (
      <ErrorState
        error={error}
        headerTitle={intl.formatMessage({
          id: "label.patientHistory.dataLoadError",
        })}
      />
    );

  if (roots?.length) {
    return (
      <FilterProvider key={props.patientUuid} roots={!loading ? roots : []}>
        <TreeView {...props} loading={loading} />
      </FilterProvider>
    );
  }

  return (
    <EmptyState
      headerTitle={intl.formatMessage({ id: "label.test.results" })}
      displayText={intl.formatMessage({ id: "label.test.resultsData" })}
    />
  );
};

export default TreeViewWrapper;
