import React from "react";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();

  if (error)
    return (
      <ErrorState
        error={error}
        headerTitle={t("dataLoadError", "Data Load Error")}
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
      headerTitle={t("testResults", "Test Results")}
      displayText={t("testResultsData", "Test results data")}
    />
  );
};

export default TreeViewWrapper;
