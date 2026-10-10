import React from "react";
import { useLocation } from "react-router-dom";
import CaseWorklist from "../components/microbiology/CaseWorklist";
import MicrobiologyWorklist from "../components/microbiology/MicrobiologyWorklist";

const MicrobiologyWorklistPage = () => {
  const location = useLocation();
  const grain = new URLSearchParams(location.search).get("grain");
  return ["cultures", "ast"].includes(grain) ? (
    <MicrobiologyWorklist />
  ) : (
    <CaseWorklist />
  );
};
export default MicrobiologyWorklistPage;
