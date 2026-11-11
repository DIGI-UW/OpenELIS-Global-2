import React from "react";
import useContainerScanner from "../components/microbiology/useContainerScanner";
import workspaceService from "../components/microbiology/CaseWorkspaceService";
import CaseViewShell from "../components/microbiology/CaseViewShell";

const MicrobiologyPage = () => {
  useContainerScanner(workspaceService);
  return <CaseViewShell />;
};
export default MicrobiologyPage;
