import React from "react";
import useContainerScanner from "../components/microbiology/useContainerScanner";
import workspaceService from "../components/microbiology/CaseWorkspaceService";
import CaseViewShell from "../components/microbiology/CaseViewShell";
import OfflineNotice from "../components/microbiology/OfflineNotice";

const MicrobiologyPage = () => {
  useContainerScanner(workspaceService);
  return (
    <>
      <OfflineNotice />
      <CaseViewShell />
    </>
  );
};
export default MicrobiologyPage;
