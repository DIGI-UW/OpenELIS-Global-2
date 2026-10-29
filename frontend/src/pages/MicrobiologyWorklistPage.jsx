import React from "react";
import useContainerScanner from "../components/microbiology/useContainerScanner";
import workspaceService from "../components/microbiology/CaseWorkspaceService";
import { useLocation } from "react-router-dom";
import CaseWorklist from "../components/microbiology/CaseWorklist";
import MicrobiologyWorklist from "../components/microbiology/MicrobiologyWorklist";

const MicrobiologyWorklistPage = () => {
  const location = useLocation();
  useContainerScanner(workspaceService);
  const grain = new URLSearchParams(location.search).get("grain");
  return ["cultures", "ast"].includes(grain) ? (
    <MicrobiologyWorklist />
  ) : (
    <CaseWorklist />
  );
};
export default MicrobiologyWorklistPage;
